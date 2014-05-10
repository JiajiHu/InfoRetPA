package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BM25Scorer extends AScorer {
  Map<Query, Map<String, Document>> queryDict;

  public BM25Scorer(Map<String, Double> idfs,
      Map<Query, Map<String, Document>> queryDict) {
    super(idfs);
    this.queryDict = queryDict;
    this.calcAverageLengths();
  }

  // /////////////weights///////////////////////////
  private final double URL_WEIGHT = -1;
  private final double TITLE_WEIGHT = -1;
  private final double BODY_WEIGHT = -1;
  private final double HEADER_WEIGHT = -1;
  private final double ANCHOR_WEIGHT = -1;

  // /////bm25 specific weights///////////////
  private final double B_URL = -1;
  private final double B_TITLE = -1;
  private final double B_HEADER = -1;
  private final double B_BODY = -1;
  private final double B_ANCHOR = -1;

  private final double K1 = -1;
  private final double PR_Lambda = -1;
  private final double PR_LambdaPrime = -1;
  private final boolean subLinear = false;
  // ////////////////////////////////////////

  // //////////bm25 data structures--feel free to modify ////////

  Map<Document, Map<String, Double>> lengths;
  Map<String, Double> avgLengths;
  Map<Document, Double> pagerankScores;

  // ////////////////////////////////////////

  // sets up average lengths for bm25, also handles pagerank
  public void calcAverageLengths() {
    lengths = new HashMap<Document, Map<String, Double>>();
    avgLengths = new HashMap<String, Double>();
    pagerankScores = new HashMap<Document, Double>();

    /*
     * @//TODO : Your code here
     */

    // normalize avgLengths
    for (Field field : Field.values()) {
      /*
       * @//TODO : Your code here
       */
    }

  }

  // //////////////////////////////////

  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q,
      Map<String, Double> tfQuery, Document d) {
    double score = 0.0;

    /*
     * @//TODO : Your code here
     */

    return score;
  }

  // do bm25 normalization
  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d,
      Query q) {
    /*
     * @//TODO : Your code here
     */
  }

  @Override
  public double getSimScore(Document d, Query q) {

    Map<Field, Map<String, Double>> tfs = this.getDocTermFreqs(d, q, subLinear);

    this.normalizeTFs(tfs, d, q);

    Map<String, Double> tfQuery = getQueryFreqs(q);

    return getNetScore(tfs, q, tfQuery, d);
  }

}
