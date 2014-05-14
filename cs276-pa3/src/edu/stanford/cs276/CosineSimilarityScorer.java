package edu.stanford.cs276;

import java.util.Map;

public class CosineSimilarityScorer extends AScorer {
  public CosineSimilarityScorer(Map<String, Double> idfs) {
    super(idfs);
  }

  private final double URL_WEIGHT = 2.9;
  private final double TITLE_WEIGHT = 3.5;
  private final double BODY_WEIGHT = 0.8;
  private final double HEADER_WEIGHT = 1.6;
  private final double ANCHOR_WEIGHT = 0.5;
  private final double[] WEIGHTS = { URL_WEIGHT, TITLE_WEIGHT, BODY_WEIGHT,
      HEADER_WEIGHT, ANCHOR_WEIGHT };
  private final double SMOOTHING_BODY_LENGTH = 500;
  private final boolean subLinear = false;

  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q,
      Map<String, Double> tfQuery, Document d) {
    double score = 0.0;
    Field[] fields = Field.values();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      double weight = WEIGHTS[i];
      double temp = 0.0;
      for (String word : tfQuery.keySet()) {
        double idf_score;
        if (idfs.containsKey(word))
          idf_score = idfs.get(word);
        else
          idf_score = idfs.get("unseen term");
        temp += tfQuery.get(word) * tfs.get(field).get(word) * idf_score;
      }
      score += temp * weight;
    }
    return score;
  }

  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d,
      Query q) {
    double bodyLen = d.body_length;
    double norm = bodyLen + SMOOTHING_BODY_LENGTH; // normalize factor
    for (Field field : Field.values()) {
      Map<String, Double> map = tfs.get(field);
      for (String word : map.keySet()) {
        map.put(word, map.get(word) / norm);
      }
    }
  }

  @Override
  public double getSimScore(Document d, Query q) {
    Map<Field, Map<String, Double>> tfs = this.getDocTermFreqs(d, q, subLinear);
    this.normalizeTFs(tfs, d, q);
    Map<String, Double> tfQuery = getQueryFreqs(q, subLinear);
    return getNetScore(tfs, q, tfQuery, d);
  }
}