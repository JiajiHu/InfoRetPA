package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class PairwiseLearner extends Learner {
  private LibSVM model;
  // NOTE: sublinear = true quite a bit better!
  private final boolean sublinear = true;

  public PairwiseLearner(boolean isLinearKernel) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (isLinearKernel) {
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
          LibSVM.TAGS_KERNELTYPE));
    }
  }

  public PairwiseLearner(double C, double gamma, boolean isLinearKernel) {
    try {
      model = new LibSVM();
    } catch (Exception e) {
      e.printStackTrace();
    }

    model.setCost(C);
    model.setGamma(gamma); // only matter for RBF kernel

    if (isLinearKernel) {
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR,
          LibSVM.TAGS_KERNELTYPE));
    }
  }

  @Override
  public Instances extract_train_features(String train_data_file,
      String train_rel_file, Map<String, Double> idfs) {
    int count_pos = 0;
    int count_neg = 0;
    boolean pos = true;
    Instances dataset = null;
    Map<Query, List<Document>> trainData = new HashMap<Query, List<Document>>();
    Map<String, Map<String, Double>> relData = new HashMap<String, Map<String, Double>>();
    try {
      trainData = Util.loadTrainData(train_data_file);
      relData = Util.loadRelData(train_rel_file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
    int index = 0;

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    ArrayList<String> labels = new ArrayList<String>();
    labels.add("0");
    labels.add("1");
    attributes.add(new Attribute("pos_neg", labels));

    /* Build attributes list */
    dataset = new Instances("pretrain_dataset", attributes, 0);

    for (Query q : trainData.keySet()) {
      map.put(q.query, new HashMap<String, Integer>());
      Map<String, Double> tfQuery = Util.getQueryFreqs(q, sublinear);
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
          instance[i] = temp;
        }
        instance[5] = 0;
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
    Instances trainset = null;
    trainset = new Instances("train_dataset", attributes, 0);
    for (Query q : trainData.keySet()) {
      Map<String, Double> rel = relData.get(q.query.trim());
      List<Pair<List<Double>, Double>> docs = new ArrayList<Pair<List<Double>, Double>>();
      for (Document d : trainData.get(q)) {
        int doc_index = map.get(q.query).get(d.url);
        Instance doc_feat = new_data.get(doc_index);
        List<Double> feat_list = new ArrayList<Double>();
        for (int i = 0; i < 5; i++)
          feat_list.add(doc_feat.value(i));
        docs.add(new Pair<List<Double>, Double>(feat_list,
            rel.get(d.url.trim())));
      }

      for (int i = 0; i < docs.size(); i++) {
        for (int j = i + 1; j < docs.size(); j++) {
          double rel_i = docs.get(i).getSecond();
          double rel_j = docs.get(j).getSecond();
          if (rel_i == rel_j)
            continue;
          double[] point = new double[6];
          for (int k = 0; k < 5; k++)
            point[k] = docs.get(i).getFirst().get(k)
                - docs.get(j).getFirst().get(k);
          if (rel_i < rel_j) {
            if (pos) {
              for (int k = 0; k < 5; k++)
                point[k] = -point[k];
              point[5] = 1;
              count_pos++;
            } else {
              point[5] = 0;
              count_neg++;
            }
          } else {
            if (!pos) {
              for (int k = 0; k < 5; k++)
                point[k] = -point[k];
              point[5] = 0;
              count_neg++;
            } else {
              point[5] = 1;
              count_pos++;
            }
          }
          Instance inst = new DenseInstance(1.0, point);
          trainset.add(inst);
          pos = !pos;
        }
      }
    }
    /* Set last attribute as target */
    trainset.setClassIndex(trainset.numAttributes() - 1);
    System.err.println("positive: " + count_pos);
    System.err.println("negative: " + count_neg);

    return trainset;
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

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    ArrayList<String> labels = new ArrayList<String>();
    labels.add("0");
    labels.add("1");
    attributes.add(new Attribute("pos_neg", labels));

    /* Build attributes list */
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
          instance[i] = temp;
        }
        instance[5] = 0;
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
  public Map<String, List<String>> testing(TestFeatures tf,
      final Classifier model) {
    Map<String, List<String>> results = new HashMap<String, List<String>>();
    Instances test_dataset = tf.features;
    Map<String, Map<String, Integer>> map = tf.index_map;

    for (String query : map.keySet()) {
      List<Pair<String, Instance>> urlAndInstances = new ArrayList<Pair<String, Instance>>();
      for (String url : map.get(query).keySet()) {
        int index = map.get(query).get(url);
        urlAndInstances.add(new Pair<String, Instance>(url, test_dataset
            .get(index)));
      }
      // sort urls for query based on instances
      Collections.sort(urlAndInstances,
          new Comparator<Pair<String, Instance>>() {
            @Override
            public int compare(Pair<String, Instance> o1,
                Pair<String, Instance> o2) {
              double res = compareInstance(o1.getSecond(), o2.getSecond(),
                  model);
              if (res == 0)
                return 1;
              else
                return -1;
            }
          });

      // put completed rankings into map
      List<String> curRankings = new ArrayList<String>();
      for (Pair<String, Instance> urlAndInstance : urlAndInstances)
        curRankings.add(urlAndInstance.getFirst());

      results.put(query, curRankings);
    }
    return results;
  }

  public double compareInstance(Instance first, Instance second,
      Classifier model) {
    double[] attr = new double[first.numAttributes()];
    for (int i = 0; i < first.numAttributes(); i++) {
      attr[i] = first.value(i) - second.value(i);
    }
    attr[5] = 0;

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("url_w"));
    attributes.add(new Attribute("title_w"));
    attributes.add(new Attribute("body_w"));
    attributes.add(new Attribute("header_w"));
    attributes.add(new Attribute("anchor_w"));
    ArrayList<String> labels = new ArrayList<String>();
    labels.add("0");
    labels.add("1");
    attributes.add(new Attribute("pos_neg", labels));

    Instances dataset = new Instances("test_one", attributes, 0);
    Instance inst = new DenseInstance(1.0, attr);
    dataset.add(inst);
    dataset.setClassIndex(dataset.numAttributes() - 1);

    try {
      return model.classifyInstance(dataset.get(0));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

}
