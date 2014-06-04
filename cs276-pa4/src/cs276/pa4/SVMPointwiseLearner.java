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
  private ArrayList<Attribute> attributes;
  private final double nor_len = 500;
  private boolean isStd;

  public SVMPointwiseLearner(boolean isLinearKernel, boolean std) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    isStd = std;
    model.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));

    if (isLinearKernel) {
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
          LibSVM.TAGS_KERNELTYPE));
    }
  }

  public SVMPointwiseLearner(double C, double gamma, boolean isLinearKernel,
      boolean std) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    isStd = std;
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
    attributes = setAttributes();
    dataset = new Instances("train_dataset", attributes, 0);

    /* Add data */
    for (Query q : trainData.keySet()) {
      Map<String, Double> tfQuery = Util.getQueryFreqs(q, sublinear);
      Map<String, Double> rel = relData.get(q.query.trim());
      for (Document d : trainData.get(q)) {
        double[] instance = computeAttributes(d, q, idfs, tfQuery);
        instance[attributes.size() - 1] = rel.get(d.url.trim());
        Instance inst = new DenseInstance(1.0, instance);
        dataset.add(inst);
      }
    }

    Instances new_data = dataset;
    if (isStd) {
      Standardize filter = new Standardize();
      try {
        filter.setInputFormat(dataset);
        new_data = Filter.useFilter(dataset, filter);
      } catch (Exception e) {
        e.printStackTrace();
      }
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
    attributes = setAttributes();
    dataset = new Instances("test_dataset", attributes, 0);

    /* Add data */
    for (Query q : testData.keySet()) {
      map.put(q.query, new HashMap<String, Integer>());
      Map<String, Double> tfQuery = Util.getQueryFreqs(q, sublinear);
      for (Document d : testData.get(q)) {
        double[] instance = computeAttributes(d, q, idfs, tfQuery);
        instance[attributes.size() - 1] = 0.0;
        Instance inst = new DenseInstance(1.0, instance);
        dataset.add(inst);
        map.get(q.query).put(d.url, index);
        index++;
      }
    }

    Instances new_data = dataset;
    if (isStd) {
      Standardize filter = new Standardize();
      try {
        filter.setInputFormat(dataset);
        new_data = Filter.useFilter(dataset, filter);
      } catch (Exception e) {
        e.printStackTrace();
      }
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

  public static ArrayList<Attribute> setAttributes() {
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    attributes.add(new Attribute("bm25"));
    attributes.add(new Attribute("pagerank"));
    attributes.add(new Attribute("smallest_window"));
    // attributes.add(new Attribute("log_body_length"));
    // attributes.add(new Attribute("numfields"));
    // attributes.add(new Attribute("numqueries"));
    // // attributes.add(new Attribute("log_url_len"));
    attributes.add(new Attribute("url_num_words"));
    // // attributes.add(new Attribute("url_num_segments"));
    attributes.add(new Attribute("url_~"));
    attributes.add(new Attribute("url_?"));
    attributes.add(new Attribute("url_="));
    attributes.add(new Attribute("pdf"));
    attributes.add(new Attribute("ppt"));
    attributes.add(new Attribute("relevance_score"));
    return attributes;
  }

  public double[] computeAttributes(Document d, Query q,
      Map<String, Double> idfs, Map<String, Double> tfQuery) {
    double[] weights = new double[attributes.size()];
    Map<Field, Map<String, Double>> tfs = Util.getDocTermFreqs(d, q, sublinear);
    Field[] fields = Field.values();

    // Add tf-idf weights
    int i = 0;
    for (; i < fields.length; i++) {
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
      if (!len_normalize || i != 2)
        weights[i] = temp;
      else
        weights[i] = temp / (double) (d.body_length + nor_len);
    }

    // Add bm25 weights
//    double[] WEIGHTS = { 3.3, 5.2, 0.9, 2.85, 3.45 };
  double[] WEIGHTS = { 2.26, 3.39, 0, 1.51, 1.20 }; // from task 1
//    double[] WEIGHTS = { 1.24, 3.767, 0.15, 2.245, 3.98 }; // from task 2
    double[] B_WEIGHTS = { 0.0, 0.2, 0.8, 0.5, 0.0 };
    double[] len = { Util.mapSum(Util.parseURL(d.url)),
        Util.mapSum(Util.parseTitle(d.title)), (double) d.body_length,
        Util.mapSum(Util.parseHeader(d.headers)),
        Util.mapSum(Util.parseAnchor(d.anchors)) };
    double[] avrLen = { 10.759, 6.101, 3621.364, 14.813, 401.709 };
    double K1 = 4.9;
    int V_NUM = 2;
    int W_NUM = 2;
    double PR_LambdaPrime = 0.05;
    double PR_LambdaPrime2 = 0.1;
    Util.normalizeBM25TFs(tfs, d, q, len, B_WEIGHTS, avrLen);
    double bm25 = Util.getNetScore(idfs, WEIGHTS, V_NUM, K1, tfs, q, tfQuery, d);
    weights[i] = bm25;
    i++;

    // Add pagerank score
    weights[i] = Util.functionV(V_NUM, d.page_rank, PR_LambdaPrime,
        PR_LambdaPrime2);
    i++;
    //
    // Add smallest window
    double window = -1.0;
    if (d.url != null)
      window = Util.checkWindow(q, Util.join(d.url.split("[^a-z0-9]"), " "),
          tfQuery, window);
    if (d.title != null)
      window = Util.checkWindow(q, d.title, tfQuery, window);
    if (d.headers != null)
      for (String header : d.headers)
        window = Util.checkWindow(q, header, tfQuery, window);
    if (d.anchors != null)
      for (String anchor : d.anchors.keySet())
        window = Util.checkWindow(q, anchor, tfQuery, window);
    if (d.body_hits != null)
      window = Util.checkBodyWindow(q, tfQuery, d.body_hits, window);

    weights[i] = Util.functionW(W_NUM, window, q.words.size());
    i++;
    //
    // // body length
    // weights[i] = Math.log(nor_len + d.body_length);
    // i++;
    //
    // // number of fields seen
    // double field = 0.0;
    // for (int j = 0; j < 5; j++) {
    // if (weights[j] != 0)
    // field += 1.0;
    // }
    // weights[i] = field;
    // i++;

    // // number of query terms not seen
    // double qs = Util.getSeenQuery(tfs, tfQuery);
    // weights[i] = qs;
    // i++;
    //
    // // // url length -- not good
    // // double url_len = Math.log(d.url.length());
    // // weights[i] = url_len;
    // // i++;
    //
    // url num words
    double url_words = d.url.split("[^a-z0-9]").length;
    weights[i] = Math.log(Math.max(url_words - 4.5, 1));
    i++;
    //
    // // // url num segments -- no difference, may need tweaking
    // // double url_segs = d.url.split("/").length;
    // // weights[i] = Math.pow(Math.abs(url_segs-1),2);
    // // i++;
    //
    String url;
    // url number of ~
    url = d.url;
    double tilde = url.length() - url.replace("~", "").length();
    weights[i] = tilde;
    i++;

    // url number of ?
    url = d.url;
    double qmark = url.length() - url.replace("?", "").length();
    weights[i] = qmark;
    i++;

    // url number of =
    url = d.url;
    double eq = url.length() - url.replace("=", "").length();
    weights[i] = eq;
    i++;

    // is pdf file
    url = d.url;
    double pdf = 0.0;
    if (url.endsWith("pdf"))
      pdf = 1;
    weights[i] = pdf;
    i++;

    // is ppt file
    url = d.url;
    double ppt = 0.0;
    if (url.endsWith("ppt"))
      ppt = 1;
    weights[i] = ppt;
    i++;

    return weights;
  }

}
