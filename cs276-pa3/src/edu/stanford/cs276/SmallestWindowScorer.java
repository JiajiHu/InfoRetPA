package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.cs276.util.Pair;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
//public class SmallestWindowScorer extends BM25Scorer {
public class SmallestWindowScorer extends CosineSimilarityScorer {

  // ///smallest window specific hyperparameters////////
  private final double B = 5;
  private final double BOOST_MOD = -1;
  private final boolean subLinear = false;

  public SmallestWindowScorer(Map<String, Double> idfs,
      Map<Query, Map<String, Document>> queryDict) {
    // super(idfs, queryDict);
    super(idfs);
    handleSmallestWindow();
  }

  public void handleSmallestWindow() {
    /*
     * @//TODO : Your code here
     */
  }

  public double checkWindow(Query q, String docstr, double curSmallestWindow) {
    String[] str = docstr.split("\\s+");
    Map<String, Double> target = getQueryFreqs(q, false);
    Map<String, Double> current = new HashMap<String, Double>();
    double window = curSmallestWindow;
    for (String word : target.keySet()) {
      current.put(word, 0.0);
    }
    int left = -1;
    int right = 0;
    int full = q.queryWords.size();
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

  public double checkBodyWindow(Query q, Map<String, List<Integer>> body,
      double curSmallestWindow) {
    double window = curSmallestWindow;

    Map<String, Double> target = getQueryFreqs(q, false);
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
    int full = q.queryWords.size();
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

  @Override
  public double getSimScore(Document d, Query q) {
    Map<Field, Map<String, Double>> tfs = this.getDocTermFreqs(d, q, subLinear);
    this.normalizeTFs(tfs, d, q);
    Map<String, Double> tfQuery = getQueryFreqs(q, subLinear);
    double score = getNetScore(tfs, q, tfQuery, d);

    double window = -1.0;
    if (d.url != null)
      window = checkWindow(q, join(d.url.split("\\W+"), " "), window);
    if (d.title != null)
      window = checkWindow(q, d.title, window);
    if (d.headers != null)
      for (String header : d.headers)
        window = checkWindow(q, header, window);
    if (d.anchors != null)
      for (String anchor : d.anchors.keySet())
        window = checkWindow(q, anchor, window);
    if (d.body_hits != null)
      window = checkBodyWindow(q, d.body_hits, window);

    // System.out.println("\nQuery: "+q.queryWords);
    // System.out.print(d);
    // System.out.println("window size: "+window);

    if (window == -1)
      return score;
    // TODO: IDEA: normalize also with body_length?
    // TODO: Try other method using window size
    return score * Math.pow(B, (q.queryWords.size()) / window);
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

}
