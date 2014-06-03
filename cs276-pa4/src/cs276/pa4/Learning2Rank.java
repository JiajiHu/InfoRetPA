package cs276.pa4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;

public class Learning2Rank {

  // std determines whether to use the right standardization
  static boolean std = false;
  static boolean t2isLinearKernel = true;
  static double t2C = 2000;
  static double t2G = 0.0002;
  static boolean t3isLinearKernel = true;
  static double t3C = 2000.0;
  static double t3G = 0.00002;
  static boolean t4isLinearKernel = true;
  static double t4C = 1.0;
  static double t4G = 0.2;

  public static Pair<Classifier, ArrayList<Pair<Double, Double>>> train(
      String train_data_file, String train_rel_file, int task,
      Map<String, Double> idfs) {
    System.err.println("## Training with feature_file =" + train_data_file
        + ", rel_file = " + train_rel_file + " ... \n");
    Classifier model = null;
    Learner learner = null;

    if (task == 1) {
      learner = new PointwiseLearner();
    } else if (task == 2) {
      learner = new PairwiseLearner(t2isLinearKernel, false, !std);
//       learner = new PairwiseLearner(t2C, t2G, t2isLinearKernel, false, !std);
    } else if (task == 3 || task == 4) { // still use task3 for extra credit competition
     learner = new PairwiseLearner(t3isLinearKernel, true, !std);
//      learner = new PairwiseLearner(t3C, t3G, t3isLinearKernel, true, !std);

    } else if (task == 5) {
//      learner = new SVMPointwiseLearner(t4C, t4G, t4isLinearKernel, !std);
       learner = new SVMPointwiseLearner(t4isLinearKernel, !std);
    }

    /* Step (1): construct your feature matrix here */
    Instances data = learner.extract_train_features(train_data_file,
        train_rel_file, idfs);
    Instances traindata = data;
    ArrayList<Pair<Double, Double>> meanAndStdvar = new ArrayList<Pair<Double, Double>>();
    if (task != 1 && std) {
      ArrayList<Attribute> attributes = new ArrayList<Attribute>();
      if (task == 2)
        attributes = PairwiseLearner.setAttributes();
      else if (task == 3)
        attributes = PairwiseLearner.setAttributesTask3();
      else
        attributes = SVMPointwiseLearner.setAttributes();
      Pair<Instances, ArrayList<Pair<Double, Double>>> afterStandard = Util
          .standardizeInstances(data, attributes);
      traindata = afterStandard.getFirst();
      meanAndStdvar = afterStandard.getSecond();
    }
    /* Step (2): implement your learning algorithm here */
    model = learner.training(traindata);

    return new Pair<Classifier, ArrayList<Pair<Double, Double>>>(model,
        meanAndStdvar);
  }

  public static Map<String, List<String>> test(String test_data_file,
      Classifier model, int task, Map<String, Double> idfs,
      ArrayList<Pair<Double, Double>> meanAndStdvar) {
    System.err.println("## Testing with feature_file=" + test_data_file
        + " ... \n");
    Map<String, List<String>> ranked_queries = new HashMap<String, List<String>>();
    Learner learner = null;
    if (task == 1) {
      learner = new PointwiseLearner();
    } else if (task == 2) {
      learner = new PairwiseLearner(t2isLinearKernel, false, !std);
      // learner = new PairwiseLearner(t2C, t2G, isLinearKernel, false, !std);
    } else if (task == 3 || task == 4) {
      learner = new PairwiseLearner(t3isLinearKernel, true, !std);
      // learner = new PairwiseLearner(t3C, t3G, isLinearKernel, true, !std);
    } else if (task == 5) {
      learner = new SVMPointwiseLearner(t4isLinearKernel, !std);
      // learner = new SVMPointwiseLearner(t4C, t4G, t4isLinearKernel, !std);
    }

    /* Step (1): construct your test feature matrix here */
    TestFeatures tf = learner.extract_test_features(test_data_file, idfs);

    if (task != 1 && std) {
      ArrayList<Attribute> attributes = new ArrayList<Attribute>();
      if (task == 2)
        attributes = PairwiseLearner.setAttributes();
      else if (task == 3)
        attributes = PairwiseLearner.setAttributesTask3();
      else
        attributes = SVMPointwiseLearner.setAttributes();
      tf.features = Util.standardizeWithFilter(tf.features, attributes,
          meanAndStdvar);
    }

    /* Step (2): implement your prediction and ranking code here */
    ranked_queries = learner.testing(tf, model);

    return ranked_queries;
  }

  /* This function output the ranking results in expected format */
  public static void writeRankedResultsToFile(
      Map<String, List<String>> ranked_queries, PrintStream ps) {
    for (String query : ranked_queries.keySet()) {
      ps.println("query: " + query.toString());

      for (String url : ranked_queries.get(query)) {
        ps.println("  url: " + url);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 4 && args.length != 5) {
      System.err.println("Input arguments: " + Arrays.toString(args));
      System.err
          .println("Usage: <train_data_file> <train_data_file> <test_data_file> <task> [ranked_out_file]");
      System.err
          .println("  ranked_out_file (optional): output results are written into the specified file. "
              + "If not, output to stdout.");
      return;
    }

    String train_data_file = args[0];
    String train_rel_file = args[1];
    String test_data_file = args[2];
    int task = Integer.parseInt(args[3]);
    String ranked_out_file = "";
    if (args.length == 5) {
      ranked_out_file = args[4];
    }

    /* Populate idfs */
    String dfFile = "df.txt";
    Map<String, Double> idfs = null;
    try {
      idfs = Util.loadDFs(dfFile);
    } catch (IOException e) {
      e.printStackTrace();
    }

    /* Train & test */
    System.err.println("### Running task" + task + "...");
    Pair<Classifier, ArrayList<Pair<Double, Double>>> model = train(
        train_data_file, train_rel_file, task, idfs);

    /* performance on the training data */
    Map<String, List<String>> trained_ranked_queries = test(train_data_file,
        model.getFirst(), task, idfs, model.getSecond());
    String trainOutFile = "tmp.train.ranked";
    writeRankedResultsToFile(trained_ranked_queries, new PrintStream(
        new FileOutputStream(trainOutFile)));
    NdcgMain ndcg = new NdcgMain(train_rel_file);
    System.err.println("# Trained NDCG=" + ndcg.score(trainOutFile));
    (new File(trainOutFile)).delete();

    Map<String, List<String>> ranked_queries = test(test_data_file,
        model.getFirst(), task, idfs, model.getSecond());

    /* Output results */
    if (ranked_out_file.equals("")) { /* output to stdout */
      writeRankedResultsToFile(ranked_queries, System.out);
    } else { /* output to file */
      try {
        writeRankedResultsToFile(ranked_queries, new PrintStream(
            new FileOutputStream(ranked_out_file)));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }
}
