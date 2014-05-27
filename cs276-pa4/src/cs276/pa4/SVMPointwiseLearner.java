package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.classifiers.functions.LibSVM;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class SVMPointwiseLearner extends Learner {

  private LibSVM model;
  // NOTE: sublinear = true quite a bit better!
  private final boolean sublinear = true;
  // NOTE: len_normalize a lot better!
  private final boolean len_normalize = true;
  private final double nor_len = 500;

  public SVMPointwiseLearner(boolean isLinearKernel) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    model.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));

    if (isLinearKernel) {
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
          LibSVM.TAGS_KERNELTYPE));
    }
  }

  public SVMPointwiseLearner(double C, double gamma, boolean isLinearKernel) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    model.setCost(C);
    model.setGamma(gamma); // only matter for RBF kernel
    model.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));

    if (isLinearKernel) {
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
          LibSVM.TAGS_KERNELTYPE));
    }
  }

  @Override
  public Instances extract_train_features(String train_data_file,
      String train_rel_file, Map<String, Double> idfs) {

    Instances dataset = null;
    Map<Query, List<Document>> trainData = new HashMap<Query, List<Document>>();
    Map<String, Map<String, Double>> relData = new HashMap<String, Map<String, Double>>();
    try {
      trainData = Util.loadTrainData(train_data_file);
      relData = Util.loadRelData(train_rel_file);
    } catch (Exception e) {
      e.printStackTrace();
    }

    /* Build attributes list */
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    attributes.add(new Attribute("relevance_score"));
    dataset = new Instances("train_dataset", attributes, 0);

    /* Add data */
    for (Query q : trainData.keySet()) {
      Map<String, Double> tfQuery = Util.getQueryFreqs(q, sublinear);
      Map<String, Double> rel = relData.get(q.query.trim());
      for (Document d : trainData.get(q)) {
        double[] instance = new double[6];
        Map<Field, Map<String, Double>> tfs = Util.getDocTermFreqs(d, q,
            sublinear);
        Field[] fields = Field.values();
        for (int i = 0; i < fields.length; i++) {
          double temp = 0;
          Field field = fields[i];
          for (String word : tfQuery.keySet()) {
            double idf_score;
            if (idfs.containsKey(word))
              idf_score = idfs.get(word);
            else
              idf_score = idfs.get("unseen term");
            temp += tfQuery.get(word) * tfs.get(field).get(word) * idf_score;
          }
          // instance[i] = temp;
          // if (!len_normalize)
          if (!len_normalize || i != 2)
            instance[i] = temp;
          else
            instance[i] = temp / (double) (d.body_length + nor_len);
        }

        instance[5] = rel.get(d.url.trim());
        Instance inst = new DenseInstance(1.0, instance);
        dataset.add(inst);
      }
    }

    Instances new_data = null;
    Standardize filter = new Standardize();
    try {
      filter.setInputFormat(dataset);
      new_data = Filter.useFilter(dataset, filter);
    } catch (Exception e) {
      e.printStackTrace();
    }

    /* Set last attribute as target */
    new_data.setClassIndex(new_data.numAttributes() - 1);

    return new_data;
  }

  @Override
  public Classifier training(Instances dataset) {
    try {
      model.buildClassifier(dataset);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return model;
  }

  @Override
  public TestFeatures extract_test_features(String test_data_file,
      Map<String, Double> idfs) {
    Instances dataset = null;
    Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
    int index = 0;
    TestFeatures testFeatures = new TestFeatures();

    Map<Query, List<Document>> testData = new HashMap<Query, List<Document>>();
    try {
      testData = Util.loadTrainData(test_data_file);
    } catch (Exception e) {
      e.printStackTrace();
    }

    /* Build attributes list */
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    attributes.add(new Attribute("relevance_score"));
    dataset = new Instances("test_dataset", attributes, 0);

    /* Add data */
    for (Query q : testData.keySet()) {
      map.put(q.query, new HashMap<String, Integer>());
      Map<String, Double> tfQuery = Util.getQueryFreqs(q, sublinear);
      for (Document d : testData.get(q)) {
        double[] instance = new double[6];
        Map<Field, Map<String, Double>> tfs = Util.getDocTermFreqs(d, q,
            sublinear);
        Field[] fields = Field.values();
        for (int i = 0; i < fields.length; i++) {
          double temp = 0;
          Field field = fields[i];
          for (String word : tfQuery.keySet()) {
            double idf_score;
            if (idfs.containsKey(word))
              idf_score = idfs.get(word);
            else
              idf_score = idfs.get("unseen term");
            temp += tfQuery.get(word) * tfs.get(field).get(word) * idf_score;
          }
          // instance[i] = temp;
          // if (!len_normalize)
          if (!len_normalize || i != 2)
            instance[i] = temp;
          else
            instance[i] = temp / (double) (d.body_length + nor_len);
        }
        instance[5] = 0.0;
        Instance inst = new DenseInstance(1.0, instance);
        dataset.add(inst);
        map.get(q.query).put(d.url, index);
        index++;
      }
    }

    Instances new_data = null;
    Standardize filter = new Standardize();
    try {
      filter.setInputFormat(dataset);
      new_data = Filter.useFilter(dataset, filter);
    } catch (Exception e) {
      e.printStackTrace();
    }
    new_data.setClassIndex(new_data.numAttributes() - 1);

    testFeatures.features = new_data;
    testFeatures.index_map = map;

    return testFeatures;
  }

  @Override
  public Map<String, List<String>> testing(TestFeatures tf, Classifier model) {
    Map<String, List<String>> results = new HashMap<String, List<String>>();
    Instances test_dataset = tf.features; /* The dataset you built in Step 3 */
    Map<String, Map<String, Integer>> map = tf.index_map;

    for (String query : map.keySet()) {
      // loop through urls for query, getting scores
      List<Pair<String, Double>> urlAndScores = new ArrayList<Pair<String, Double>>(
          map.get(query).size());
      for (String url : map.get(query).keySet()) {
        int index = map.get(query).get(url);
        double score = 0;
        try {
          score = model.classifyInstance(test_dataset.instance(index));
        } catch (Exception e) {
          e.printStackTrace();
        }
        urlAndScores.add(new Pair<String, Double>(url, score));
      }

      // sort urls for query based on scores
      Collections.sort(urlAndScores, new Comparator<Pair<String, Double>>() {
        @Override
        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
          if (o1.getSecond() < o2.getSecond())
            return 1;
          else if (o1.getSecond() > o2.getSecond())
            return -1;
          return 0;
        }
      });

      // put completed rankings into map
      List<String> curRankings = new ArrayList<String>();
      for (Pair<String, Double> urlAndScore : urlAndScores)
        curRankings.add(urlAndScore.getFirst());

      results.put(query, curRankings);
    }
    return results;
  }

}
