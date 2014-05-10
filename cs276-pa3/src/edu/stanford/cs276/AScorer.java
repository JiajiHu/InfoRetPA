package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AScorer {

  Map<String, Double> idfs;

  public AScorer(Map<String, Double> idfs) {
    this.idfs = idfs;
  }

  // scores each document for each query
  public abstract double getSimScore(Document d, Query q);

  // handle the query vector
  public Map<String, Double> getQueryFreqs(Query q, boolean subLinear) {
    Map<String, Double> tfQuery = new HashMap<String, Double>();

    /******************************************/
    for (String word : q.queryWords) {
      if (tfQuery.containsKey(word))
        tfQuery.put(word, 1.0);
      else {
        tfQuery.put(word, tfQuery.get(word) + 1.0);
      }
    }
    if (subLinear) {
      for (String word : tfQuery.keySet()) {
        tfQuery.put(word, 1.0 + Math.log(tfQuery.get(word)));
      }
    }
    /******************************************/
    return tfQuery;
  }

  // //////////////////Initialization/Parsing Methods/////////////////////
  /******************************************/
  public Map<String, Double> parseURL(String url) {
    Map<String, Double> u_tf = new HashMap<String, Double>();
    String[] tokens = url.trim().split("\\W+");
    for (String token : tokens) {
      if (u_tf.containsKey(token))
        u_tf.put(token, u_tf.get(token) + 1.0);
      else
        u_tf.put(token, 1.0);
    }
    return u_tf;
  }

  public Map<String, Double> parseTitle(String title) {
    Map<String, Double> t_tf = new HashMap<String, Double>();
    String[] tokens = title.trim().split("\\s+");
    for (String token : tokens) {
      if (t_tf.containsKey(token))
        t_tf.put(token, t_tf.get(token) + 1.0);
      else
        t_tf.put(token, 1.0);
    }
    return t_tf;
  }

  public Map<String, Double> parseHeader(List<String> header) {
    Map<String, Double> h_tf = new HashMap<String, Double>();
    for (String token : header) {
      if (h_tf.containsKey(token))
        h_tf.put(token, h_tf.get(token) + 1.0);
      else
        h_tf.put(token, 1.0);
    }
    return h_tf;
  }

  public Map<String, Double> parseAnchor(Map<String, Integer> anchor) {
    Map<String, Double> a_tf = new HashMap<String, Double>();
    for (String token : anchor.keySet()) {
      a_tf.put(token, (double) anchor.get(token));
    }
    return a_tf;
  }

  public Map<String, Double> parseBody(Map<String, List<Integer>> body) {
    Map<String, Double> b_tf = new HashMap<String, Double>();
    for (String token : body.keySet()) {
      b_tf.put(token, (double) body.get(token).size());
    }
    return b_tf;
  }

  /******************************************/

  /*
   * / Creates the various kinds of term frequences (url, title, body, header,
   * and anchor) You can override this if you'd like, but it's likely that your
   * concrete classes will share this implementation
   */
  public Map<Field, Map<String, Double>> getDocTermFreqs(Document d, Query q,
      boolean subLinear) {
    // map from tf type -> queryWord -> score
    Map<Field, Map<String, Double>> tfs = new HashMap<Field, Map<String, Double>>();
    Map<Field, Map<String, Double>> q_tfs = new HashMap<Field, Map<String, Double>>();

    /******************************************/
    tfs.put(Field.URL, parseURL(d.url));
    tfs.put(Field.TITLE, parseTitle(d.title));
    tfs.put(Field.HEADER, parseHeader(d.headers));
    tfs.put(Field.ANCHOR, parseAnchor(d.anchors));
    tfs.put(Field.BODY, parseBody(d.body_hits));

    // ////////handle counts//////

    Set<String> seen = new HashSet<String>();
    // loop through query terms increasing relevant tfs
    for (String queryWord : q.queryWords) {
      if (seen.contains(queryWord))
        continue;
      for (Field field : Field.values()) {
        if (tfs.get(field).containsKey(queryWord)) {
          if (subLinear)
            q_tfs.get(field).put(queryWord,
                1.0 + Math.log(tfs.get(field).get(queryWord)));
          else
            q_tfs.get(field).put(queryWord, tfs.get(field).get(queryWord));
        }
      }
      seen.add(queryWord);
    }
    return q_tfs;
    /******************************************/
  }
}
