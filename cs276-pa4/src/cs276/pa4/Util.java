package cs276.pa4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;

public class Util {
  public static Map<Query, List<Document>> loadTrainData(
      String feature_file_name) throws Exception {
    Map<Query, List<Document>> result = new HashMap<Query, List<Document>>();

    File feature_file = new File(feature_file_name);
    if (!feature_file.exists()) {
      System.err.println("Invalid feature file name: " + feature_file_name);
      return null;
    }

    BufferedReader reader = new BufferedReader(new FileReader(feature_file));
    String line = null, anchor_text = null;
    Query query = null;
    Document doc = null;
    int numQuery = 0;
    int numDoc = 0;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(":", 2);
      String key = tokens[0].trim();
      String value = tokens[1].trim();

      if (key.equals("query")) {
        query = new Query(value);
        numQuery++;
        result.put(query, new ArrayList<Document>());
      } else if (key.equals("url")) {
        doc = new Document();
        doc.url = new String(value);
        result.get(query).add(doc);
        numDoc++;
      } else if (key.equals("title")) {
        doc.title = new String(value);
      } else if (key.equals("header")) {
        if (doc.headers == null)
          doc.headers = new ArrayList<String>();
        doc.headers.add(value);
      } else if (key.equals("body_hits")) {
        if (doc.body_hits == null)
          doc.body_hits = new HashMap<String, List<Integer>>();
        String[] temp = value.split(" ", 2);
        String term = temp[0].trim();
        List<Integer> positions_int;

        if (!doc.body_hits.containsKey(term)) {
          positions_int = new ArrayList<Integer>();
          doc.body_hits.put(term, positions_int);
        } else
          positions_int = doc.body_hits.get(term);

        String[] positions = temp[1].trim().split(" ");
        for (String position : positions)
          positions_int.add(Integer.parseInt(position));

      } else if (key.equals("body_length"))
        doc.body_length = Integer.parseInt(value);
      else if (key.equals("pagerank"))
        doc.page_rank = Integer.parseInt(value);
      else if (key.equals("anchor_text")) {
        anchor_text = value;
        if (doc.anchors == null)
          doc.anchors = new HashMap<String, Integer>();
      } else if (key.equals("stanford_anchor_count"))
        doc.anchors.put(anchor_text, Integer.parseInt(value));
    }

    reader.close();
    System.err
        .println("# Signal file " + feature_file_name + ": number of queries="
            + numQuery + ", number of documents=" + numDoc);

    return result;
  }

  public static Map<String, Double> loadDFs(String dfFile) throws IOException {
    Map<String, Double> dfs = new HashMap<String, Double>();
    double totalDocCount = 98998;
    BufferedReader br = new BufferedReader(new FileReader(dfFile));
    String line;
    while ((line = br.readLine()) != null) {
      line = line.trim();
      if (line.equals(""))
        continue;
      String[] tokens = line.split("\\s+");
      dfs.put(tokens[0], Double.parseDouble(tokens[1]));
    }
    br.close();

    for (String term : dfs.keySet()) {
      dfs.put(term, Math.log((totalDocCount + 1) / (dfs.get(term) + 1.0)));
    }
    dfs.put("unseen term", Math.log((totalDocCount + 1.0)));

    return dfs;
  }

  /* query -> (url -> score) */
  public static Map<String, Map<String, Double>> loadRelData(
      String rel_file_name) throws IOException {
    Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();

    File rel_file = new File(rel_file_name);
    if (!rel_file.exists()) {
      System.err.println("Invalid feature file name: " + rel_file_name);
      return null;
    }

    BufferedReader reader = new BufferedReader(new FileReader(rel_file));
    String line = null, query = null, url = null;
    int numQuery = 0;
    int numDoc = 0;
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split(":", 2);
      String key = tokens[0].trim();
      String value = tokens[1].trim();

      if (key.equals("query")) {
        query = value;
        result.put(query, new HashMap<String, Double>());
        numQuery++;
      } else if (key.equals("url")) {
        String[] tmps = value.split(" ", 2);
        url = tmps[0].trim();
        double score = Double.parseDouble(tmps[1].trim());
        result.get(query).put(url, score);
        numDoc++;
      }
    }
    reader.close();
    System.err.println("# Rel file " + rel_file_name + ": number of queries="
        + numQuery + ", number of documents=" + numDoc);

    return result;
  }

  public static void main(String[] args) {
    try {
      System.out.print(loadRelData(args[0]));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, Double> getQueryFreqs(Query q, boolean subLinear) {
    Map<String, Double> tfQuery = new HashMap<String, Double>();
    for (String word : q.words) {
      if (tfQuery.containsKey(word)) {
        tfQuery.put(word.toLowerCase(), tfQuery.get(word.toLowerCase()) + 1.0);
      } else {
        tfQuery.put(word.toLowerCase(), 1.0);
      }
    }
    if (subLinear) {
      for (String word : tfQuery.keySet()) {
        tfQuery.put(word.toLowerCase(),
            1.0 + Math.log(tfQuery.get(word.toLowerCase())));
      }
    }
    return tfQuery;
  }

  public static Map<String, Double> parseURL(String url) {
    Map<String, Double> u_tf = new HashMap<String, Double>();
    if (url == null)
      return u_tf;
    String[] tokens = url.toLowerCase().trim().split("[^a-z0-9]");
    for (int i = 1; i < tokens.length; i++) {
      String token = tokens[i];
      if (u_tf.containsKey(token))
        u_tf.put(token, u_tf.get(token) + 1.0);
      else
        u_tf.put(token, 1.0);
    }
    return u_tf;
  }

  public static Map<String, Double> parseTitle(String title) {
    Map<String, Double> t_tf = new HashMap<String, Double>();
    if (title == null)
      return t_tf;
    String[] tokens = title.toLowerCase().trim().split("\\s+");
    for (String token : tokens) {
      if (t_tf.containsKey(token))
        t_tf.put(token, t_tf.get(token) + 1.0);
      else
        t_tf.put(token, 1.0);
    }
    return t_tf;
  }

  public static Map<String, Double> parseHeader(List<String> header) {
    Map<String, Double> h_tf = new HashMap<String, Double>();
    if (header == null)
      return h_tf;
    for (String head : header) {
      String[] tokens = head.toLowerCase().trim().split("\\s+");
      for (String token : tokens) {
        if (h_tf.containsKey(token))
          h_tf.put(token, h_tf.get(token) + 1.0);
        else
          h_tf.put(token, 1.0);
      }
    }
    return h_tf;
  }

  public static Map<String, Double> parseAnchor(Map<String, Integer> anchor) {
    Map<String, Double> a_tf = new HashMap<String, Double>();
    if (anchor == null)
      return a_tf;
    for (String anchor_text : anchor.keySet()) {
      for (String token : anchor_text.toLowerCase().split("\\s+")) {
        if (a_tf.containsKey(token))
          a_tf.put(token, (double) (anchor.get(anchor_text) + a_tf.get(token)));
        else
          a_tf.put(token, (double) anchor.get(anchor_text));
      }
    }
    return a_tf;
  }

  public static Map<String, Double> parseBody(Map<String, List<Integer>> body) {
    Map<String, Double> b_tf = new HashMap<String, Double>();
    if (body == null)
      return b_tf;
    for (String token : body.keySet()) {
      b_tf.put(token, (double) body.get(token).size());
    }
    return b_tf;
  }

  public static Map<Field, Map<String, Double>> getDocTermFreqs(Document d,
      Query q, boolean subLinear) {
    Map<Field, Map<String, Double>> tfs = new HashMap<Field, Map<String, Double>>();
    Map<Field, Map<String, Double>> q_tfs = new HashMap<Field, Map<String, Double>>();

    tfs.put(Field.URL, parseURL(d.url));
    tfs.put(Field.TITLE, parseTitle(d.title));
    tfs.put(Field.HEADER, parseHeader(d.headers));
    tfs.put(Field.ANCHOR, parseAnchor(d.anchors));
    tfs.put(Field.BODY, parseBody(d.body_hits));

    for (Field field : Field.values()) {
      q_tfs.put(field, new HashMap<String, Double>());
    }
    // loop through query terms increasing relevant tfs
    for (String queryWord : q.words) {
      for (Field field : Field.values()) {
        if (tfs.get(field).containsKey(queryWord)) {
          if (subLinear)
            q_tfs.get(field).put(queryWord,
                1.0 + Math.log(tfs.get(field).get(queryWord)));
          else
            q_tfs.get(field).put(queryWord, tfs.get(field).get(queryWord));
        } else
          q_tfs.get(field).put(queryWord, 0.0);
      }
    }
    return q_tfs;
  }

  // Helper functions for smallest window
  public static double checkWindow(Query q, String docstr,
      Map<String, Double> target, double curSmallestWindow) {
    String[] str = docstr.split("[^a-z0-9]");
    Map<String, Double> current = new HashMap<String, Double>();
    double window = curSmallestWindow;
    for (String word : target.keySet()) {
      current.put(word, 0.0);
    }
    int left = -1;
    int right = 0;
    int full = q.words.size();
    int count = 0;
    while (right < str.length) {
      if (target.containsKey(str[right])) {
        current.put(str[right], current.get(str[right]) + 1.0);
        if (current.get(str[right]) <= target.get(str[right])) {
          count += 1;
          if (count == full) {
            while (count == full) {
              left++;
              if (target.containsKey(str[left])) {
                current.put(str[left], current.get(str[left]) - 1.0);
                if (current.get(str[left]) < target.get(str[left])) {
                  count--;
                }
              }
            }
            if (window < 0 || right - left + 1 < window)
              window = right - left + 1;
          }
        }
      }
      right++;
    }
    return window;
  }

  public static double checkBodyWindow(Query q, Map<String, Double> target,
      Map<String, List<Integer>> body, double curSmallestWindow) {
    double window = curSmallestWindow;

    Map<String, Double> current = new HashMap<String, Double>();
    for (String word : target.keySet()) {
      current.put(word, 0.0);
    }

    List<Pair<String, Integer>> body_hits = new ArrayList<Pair<String, Integer>>();
    for (String word : body.keySet()) {
      if (body.get(word).size() < target.get(word))
        return window;
      for (int i = 0; i < body.get(word).size(); i++)
        body_hits.add(new Pair<String, Integer>(word, body.get(word).get(i)));
    }
    Collections.sort(body_hits, new Comparator<Pair<String, Integer>>() {
      public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
        if (o1.getSecond() < o2.getSecond())
          return -1;
        return 1;
      }
    });
    int left = -1;
    int right = 0;
    int full = q.words.size();
    int count = 0;
    while (right < body_hits.size()) {
      current.put(body_hits.get(right).getFirst(),
          current.get(body_hits.get(right).getFirst()) + 1.0);
      if (current.get(body_hits.get(right).getFirst()) <= target.get(body_hits
          .get(right).getFirst())) {
        count += 1;
        if (count == full) {
          while (count == full) {
            left++;
            if (target.containsKey(body_hits.get(left).getFirst())) {
              current.put(body_hits.get(left).getFirst(),
                  current.get(body_hits.get(left).getFirst()) - 1.0);
              if (current.get(body_hits.get(left).getFirst()) < target
                  .get(body_hits.get(left).getFirst())) {
                count--;
              }
            }
          }
          if (window < 0
              || body_hits.get(right).getSecond()
                  - body_hits.get(left).getSecond() + 1 < window)
            window = body_hits.get(right).getSecond()
                - body_hits.get(left).getSecond() + 1;
        }
      }
      right++;
    }
    return window;
  }

  public static String join(String[] list, String delim) {
    StringBuilder sb = new StringBuilder();
    String loopDelim = "";
    for (String s : list) {
      sb.append(loopDelim);
      sb.append(s);
      loopDelim = delim;
    }
    return sb.toString();
  }

  // Helper functions for BM25
  public static void normalizeBM25TFs(Map<Field, Map<String, Double>> tfs,
      Document d, Query q, double[] lengths, double[] B_WEIGHTS,
      double[] avgLengths) {
    Field[] fields = Field.values();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      double len = lengths[i];
      if (len == 0)
        continue;
      double b_weight = B_WEIGHTS[i];
      double avgLen = avgLengths[i];
      double norm = 1 + b_weight * (len / avgLen - 1);

      Map<String, Double> map = tfs.get(field);
      for (String word : map.keySet()) {
        map.put(word, map.get(word) / norm);
      }
    }
  }

  public static double getNetScore(Map<String, Double> idfs, double[] WEIGHTS,
      int V_NUM, double K1, double PR_Lambda, double PR_LambdaPrime,
      double PR_LambdaPrime2, Map<Field, Map<String, Double>> tfs, Query q,
      Map<String, Double> tfQuery, Document d) {

    double score = 0.0;
    Field[] fields = Field.values();
    for (String term : tfQuery.keySet()) {
      double idf_score;
      if (idfs.containsKey(term)) {
        idf_score = idfs.get(term);
      } else {
        idf_score = idfs.get("unseen term");
      }
      double wdt = 0;
      for (int i = 0; i < fields.length; i++) {
        wdt += WEIGHTS[i] * tfs.get(fields[i]).get(term);
      }
      score += (wdt / (K1 + wdt)) * idf_score;
    }

    return score;
  }

  public static double functionV(int select, double f, double PR_LambdaPrime,
      double PR_LambdaPrime2) {
    if (select == 0) {
      return Math.log(PR_LambdaPrime + f);
    } else if (select == 1) {
      return f / (PR_LambdaPrime + f);
    } else if (select == 2) {
      return 1 / (PR_LambdaPrime + Math.exp(-f * PR_LambdaPrime2));
    }
    return 0.0;
  }

  public static double functionW(int select, double window, double qsize) {
    if (window == -1.0)
      return 0;
    switch (select) {
    case 0:
      return 0;
    case 1:
      return 1 / (window - qsize + 1);
    case 2:
      return Math.exp(qsize - window);
    default:
      return 0;
    }
  }

  // get map sum
  public static double mapSum(Map<String, Double> map) {
    double result = 0.0;
    for (String key : map.keySet()) {
      result += map.get(key);
    }
    return result;
  }

  public static Pair<Instances, ArrayList<Pair<Double, Double>>> standardizeInstances(
      Instances input, ArrayList<Attribute> attributes) {
    Instances output = new Instances("train_dataset", attributes, 0);
    ArrayList<Pair<Double, Double>> meanAndStdvar = new ArrayList<Pair<Double, Double>>();
    double[] sum = new double[attributes.size() - 1];
    double[] var = new double[attributes.size() - 1];

    for (int i = 0; i < input.size(); i++) {
      double[] ins = input.get(i).toDoubleArray();
      for (int j = 0; j < attributes.size() - 1; j++) {
        sum[j] += ins[j];
      }
    }
    for (int j = 0; j < attributes.size() - 1; j++) {
      sum[j] = sum[j]/input.size();
    }
    for (int i = 0; i < input.size(); i++) {
      double[] ins = input.get(i).toDoubleArray();
      for (int j = 0; j < attributes.size() - 1; j++) {
        var[j] += Math.pow(ins[j]-sum[j],2);
      }
    }
    for (int j = 0; j < attributes.size() - 1; j++) {
      var[j] = Math.sqrt(var[j]/input.size());
    }
    for (int j = 0; j < attributes.size() - 1; j++) {
      meanAndStdvar.add(new Pair<Double,Double>(sum[j],var[j]));
    }
    for (int i = 0; i < input.size(); i++) {
      double[] ins = input.get(i).toDoubleArray();
      double[] instance = new double[attributes.size()];
      for (int j = 0; j < attributes.size() - 1; j++) {
        instance[j] = (ins[j]-sum[j])/var[j];
      }
      instance[attributes.size()-1] = ins[attributes.size()-1];
      Instance inst = new DenseInstance(1.0, instance);
      output.add(inst);
    }    
  
    output.setClassIndex(output.numAttributes() - 1);

    return new Pair<Instances, ArrayList<Pair<Double, Double>>>(output,
        meanAndStdvar);
  }

  public static Instances standardizeWithFilter(Instances input, ArrayList<Attribute> attributes,
      ArrayList<Pair<Double, Double>> meanAndStdvar) {
    Instances output = new Instances("test_dataset", attributes, 0);
    for (int i = 0; i < input.size(); i++) {
      double[] ins = input.get(i).toDoubleArray();
      double[] instance = new double[attributes.size()];
      for (int j = 0; j < attributes.size() - 1; j++) {
        instance[j] = (ins[j]-meanAndStdvar.get(j).getFirst())/meanAndStdvar.get(j).getSecond();
      }
      instance[attributes.size()-1] = ins[attributes.size()-1];
      Instance inst = new DenseInstance(1.0, instance);
      output.add(inst);
    }    
  
    output.setClassIndex(output.numAttributes() - 1);

    return output;
  }
  
  public static double getSeenQuery(Map<Field, Map<String, Double>> tfs,
      Map<String, Double> qtf) {
    double seen = 0;
    for (String qword : qtf.keySet()) {
      for (Field field : Field.values()) {
        if (tfs.get(field).get(qword) > 0) {
          seen += 1;
          break;
        }
      }
    }
    return seen;
  }



}
