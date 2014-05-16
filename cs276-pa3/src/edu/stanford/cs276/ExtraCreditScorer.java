package edu.stanford.cs276;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.cs276.util.Pair;

public class ExtraCreditScorer extends SmallestWindowScorer {

  private final double lambda = 0.05;// higher good for dev, bad for train
  private final double lambda2 = 0.15;// higher good for train, bad for dev
  private final double lambda3 = 0.0;// no real impact
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
    score -= lambda2 * getUnseenQuery(tfs, tfQuery);
    Pair<Double, Double> leastMostFreq = getLeastMostFreq(tfs, tfQuery);
    double leastSeenFreq = leastMostFreq.getFirst();
    double mostSeenFreq = leastMostFreq.getSecond();
    double l = Math.abs(leastSeenFreq - 1.0 / tfQuery.keySet().size());
    double m = Math.abs(mostSeenFreq - 1.0 / tfQuery.keySet().size());
    double freq = Math.max(l, m);
    score -= lambda3 * freq;
    score += lambda4 * Math.log(d.body_length + 300);
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

  public Pair<Double, Double> getLeastMostFreq(
      Map<Field, Map<String, Double>> tfs, Map<String, Double> qtf) {
    Map<String, Double> seen = new HashMap<String, Double>();
    for (String qword : qtf.keySet()) {
      seen.put(qword, 0.0);
      for (Field field : Field.values()) {
        seen.put(qword, seen.get(qword) + tfs.get(field).get(qword));
      }
    }
    double total = getSum(seen.values());
    double smallest = 1;
    double largest = 0;
    for (String qword : seen.keySet()) {
      if (seen.get(qword) / total < smallest)
        smallest = seen.get(qword) / total;
      if (seen.get(qword) / total > largest)
        largest = seen.get(qword) / total;
    }
    return new Pair<Double, Double>(smallest, largest);
  }

  public double getUnseenQuery(Map<Field, Map<String, Double>> tfs,
      Map<String, Double> qtf) {
    double unseen = 0;
    for (String qword : qtf.keySet()) {
      boolean flag = false;
      for (Field field : Field.values()) {
        if (tfs.get(field).get(qword) > 0) {
          flag = true;
          break;
        }
      }
      if (!flag)
        unseen += 1;
    }
    return unseen;
  }

}
