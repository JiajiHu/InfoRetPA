package edu.stanford.cs276;

import java.util.Map;

public class CosineSimilarityScorer extends AScorer {
  public CosineSimilarityScorer(Map<String, Double> idfs) {
    super(idfs);
  }

  // /////////////weights///////////////////////////
  private final double URL_WEIGHT = 1;
  private final double TITLE_WEIGHT = 1;
  private final double BODY_WEIGHT = 1;
  private final double HEADER_WEIGHT = 1;
  private final double ANCHOR_WEIGHT = 1;
  private final double[] WEIGHTS = { URL_WEIGHT, TITLE_WEIGHT, BODY_WEIGHT,
      HEADER_WEIGHT, ANCHOR_WEIGHT };
  private final double SMOOTHING_BODY_LENGTH = 500;
  private final boolean subLinear = false;

  
  // HJJ NOTE: NDCG score seems to drop by 0.01 when using idf normalization, 
  // might be because of bad weights - May.10
  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q,
      Map<String, Double> tfQuery, Document d) {
    /******************************************/
    double score = 0.0;
    int count = 0;
    for (Field field : Field.values()) {
      double weight = WEIGHTS[count];
      double temp = 0.0;
      Map<String, Double> tfs_field = tfs.get(field);
      for (String word : q.queryWords) {
        double idf_val;
        if(idfs.containsKey(word))
          idf_val = idfs.get(word);
        else
          idf_val = idfs.get("unseen term");
        temp += tfQuery.get(word) * tfs_field.get(word) * idf_val;
      }
      score += temp * weight;
      count++;
    }
    return score;
    /******************************************/
  }

  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d,
      Query q) {
    /******************************************/
    double bodyLen = d.body_length;
    double norm = bodyLen + SMOOTHING_BODY_LENGTH; // normalize factor
    for (Field field : Field.values()) {
      Map<String, Double> map = tfs.get(field);
      for (String word : map.keySet()) {
        map.put(word, map.get(word) / (norm));
      }
    }
    /******************************************/
  }

  @Override
  public double getSimScore(Document d, Query q) {
    Map<Field, Map<String, Double>> tfs = this.getDocTermFreqs(d, q, subLinear);
    this.normalizeTFs(tfs, d, q);
    Map<String, Double> tfQuery = getQueryFreqs(q, subLinear);
    return getNetScore(tfs, q, tfQuery, d);
  }
}