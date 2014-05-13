package edu.stanford.cs276;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    for (String word : q.queryWords) {
      if (tfQuery.containsKey(word)) {
        tfQuery.put(word, tfQuery.get(word) + 1.0);
      } else {
        tfQuery.put(word, 1.0);
      }
    }
    if (subLinear) {
      for (String word : tfQuery.keySet()) {
        tfQuery.put(word, 1.0 + Math.log(tfQuery.get(word)));
      }
    }
    return tfQuery;
  }

  // //////////////////Initialization/Parsing Methods/////////////////////

  public Map<String, Double> parseURL(String url) {
    Map<String, Double> u_tf = new HashMap<String, Double>();
    if (url == null)
      return u_tf;
    String[] tokens = url.trim().split("\\W+");
    for (int i = 1; i < tokens.length; i++) {
      String token = tokens[i];
      if (u_tf.containsKey(token))
        u_tf.put(token, u_tf.get(token) + 1.0);
      else
        u_tf.put(token, 1.0);
    }
    return u_tf;
  }

  public Map<String, Double> parseTitle(String title) {
    Map<String, Double> t_tf = new HashMap<String, Double>();
    if (title == null)
      return t_tf;
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
    if (header == null)
      return h_tf;
    for (String head : header) {
      String[] tokens = head.trim().split("\\s+");
      for (String token : tokens) {
        if (h_tf.containsKey(token))
          h_tf.put(token, h_tf.get(token) + 1.0);
        else
          h_tf.put(token, 1.0);
      }
    }
    return h_tf;
  }

  public Map<String, Double> parseAnchor(Map<String, Integer> anchor) {
    Map<String, Double> a_tf = new HashMap<String, Double>();
    if (anchor == null)
      return a_tf;
    for (String anchor_text : anchor.keySet()) {
      for (String token : anchor_text.split("\\s+")) {
        if (a_tf.containsKey(token))
          a_tf.put(token, (double) (anchor.get(anchor_text) + a_tf.get(token)));
        else
          a_tf.put(token, (double) anchor.get(anchor_text));
      }
    }
    return a_tf;
  }

  public Map<String, Double> parseBody(Map<String, List<Integer>> body) {
    Map<String, Double> b_tf = new HashMap<String, Double>();
    if (body == null)
      return b_tf;
    for (String token : body.keySet()) {
      b_tf.put(token, (double) body.get(token).size());
    }
    return b_tf;
  }

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

    tfs.put(Field.URL, parseURL(d.url));
    tfs.put(Field.TITLE, parseTitle(d.title));
    tfs.put(Field.HEADER, parseHeader(d.headers));
    tfs.put(Field.ANCHOR, parseAnchor(d.anchors));
    tfs.put(Field.BODY, parseBody(d.body_hits));

    // ////////handle counts//////

    for (Field field : Field.values()) {
      q_tfs.put(field, new HashMap<String, Double>());
    }
    // loop through query terms increasing relevant tfs
    for (String queryWord : q.queryWords) {
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
}
