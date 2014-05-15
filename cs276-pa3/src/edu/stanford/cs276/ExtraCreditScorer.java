package edu.stanford.cs276;

import java.util.HashMap;
import java.util.Map;

public class ExtraCreditScorer extends SmallestWindowScorer {

  private final double lambda = 0.05;// higher good for dev, bad for train
  private final double lambda2 = 0;// only negative impact
  private final double lambda3 = 0.0;
  private final double lambda4 = 0.0;

  public ExtraCreditScorer(Map<String, Double> idfs,
      Map<Query, Map<String, Document>> queryDict) {
    super(idfs, queryDict);
  }

  @Override
  public double getSimScore(Document d, Query q) {
    Map<Field, Map<String, Double>> tfs = super.getDocTermFreqs(d, q, false);
    Map<String, Double> tfQuery = getQueryFreqs(q, false);

    double score;
    score = super.getSimScore(d, q);
    double fieldCount = getFieldCount(tfs);
    score += lambda * fieldCount;
    double leastSeenFreq = getLeastSeenFreq(tfs, tfQuery);
    score += lambda2 * Math.log(0.1+leastSeenFreq);
    score += lambda3 * Math.log(d.body_length+300);
    score -= lambda4 * getUnseenQuery(tfs, tfQuery);
    return score;
  }

  public double getFieldCount(Map<Field, Map<String, Double>> tfs) {
    double count = 0;
    for (Field field : Field.values()) {
      Map<String, Double> vector = tfs.get(field);
      if (getSum(vector.values()) > 0)
        count += 1;
    }
    return count;
  }

  public double getLeastSeenFreq(Map<Field, Map<String, Double>> tfs,
      Map<String, Double> qtf) {
    Map<String, Double> seen = new HashMap<String, Double>();
    for (String qword : qtf.keySet()) {
      seen.put(qword, 0.0);
      for (Field field : Field.values()) {
        seen.put(qword, seen.get(qword) + tfs.get(field).get(qword));
      }
    }
    double total = getSum(seen.values());
    double smallest = 1;
    for (String qword : seen.keySet()) {
      if (seen.get(qword) / total < smallest)
        smallest = seen.get(qword) / total;
    }
    return smallest;
  }
  
  public double getUnseenQuery(Map<Field, Map<String, Double>> tfs,
      Map<String, Double> qtf){
    double unseen = 0;
    
    return unseen;
  }

}
