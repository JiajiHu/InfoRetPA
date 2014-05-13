package edu.stanford.cs276;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class BM25Scorer extends AScorer {
  Map<Query, Map<String, Document>> queryDict;
  Map<Document, Map<Field, Double>> lengths; // doc->field->length
  Map<Field, Double> avgLengths; // field->length
  Map<Document, Double> pagerankScores; // doc->pagerank

  // weights
  private final double URL_WEIGHT = 1;
  private final double TITLE_WEIGHT = 1;
  private final double BODY_WEIGHT = 1;
  private final double HEADER_WEIGHT = 1;
  private final double ANCHOR_WEIGHT = 1;
  private final double[] WEIGHTS = { URL_WEIGHT, TITLE_WEIGHT, BODY_WEIGHT,
      HEADER_WEIGHT, ANCHOR_WEIGHT };

  // bm25 specific weights
  private final double B_URL = 1;
  private final double B_TITLE = 1;
  private final double B_HEADER = 1;
  private final double B_BODY = 1;
  private final double B_ANCHOR = 1;
  private final double[] B_WEIGHTS = { B_URL, B_TITLE, B_BODY, B_HEADER,
      B_ANCHOR };

  private final double K1 = 1;
  private final int V_NUM = 0;
  private final double PR_Lambda = 1;
  private final double PR_LambdaPrime = 1;
  private final double PR_LambdaPrime2 = 1; // for the 3rd type of V function

  private final boolean subLinear = false;

  // ////////////////////////////////////////

  public BM25Scorer(Map<String, Double> idfs,
      Map<Query, Map<String, Document>> queryDict) {
    super(idfs);
    this.queryDict = queryDict;
    this.calcAverageLengths();
  }

  // sets up average lengths for bm25, also handles pagerank
  public void calcAverageLengths() {
    lengths = new HashMap<Document, Map<Field, Double>>();
    avgLengths = new HashMap<Field, Double>();
    pagerankScores = new HashMap<Document, Double>();
    // field name -> num of field over all docs
    Map<Field, Double> count = new HashMap<Field, Double>();
    // field name -> len of field over all docs
    Map<Field, Double> sum = new HashMap<Field, Double>();

    // initialize
    for (Field field : Field.values()) {
      count.put(field, 0.0);
      sum.put(field, 0.0);
    }

    // loop through queries to populate lengths, avgLengths, pagerankScores
    for (Query query : queryDict.keySet()) {
      Map<String, Document> mapUrl = queryDict.get(query);
      for (String url : mapUrl.keySet()) {
        Document doc = mapUrl.get(url);
        pagerankScores.put(doc, (double) doc.page_rank);
        // field name -> len
        Map<Field, Double> fieldLen = new HashMap<Field, Double>();
        // 0: url
        Map<String, Double> temp = parseURL(doc.url);
        double len = getSum(temp.values());
        fieldLen.put(Field.URL, len);
        sum.put(Field.URL, sum.get(Field.URL) + len);
        count.put(Field.URL, count.get(Field.URL) + 1.0);

        // 1: title
        temp = parseTitle(doc.title);
        len = getSum(temp.values());
        fieldLen.put(Field.TITLE, len);
        sum.put(Field.TITLE, sum.get(Field.TITLE) + len);
        count.put(Field.TITLE, count.get(Field.TITLE) + 1.0);

        // 2: body
        len = doc.body_length;
        fieldLen.put(Field.BODY, len);
        sum.put(Field.BODY, sum.get(Field.BODY) + len);
        count.put(Field.BODY, count.get(Field.BODY) + 1.0);

        // 3: header
        if (doc.headers != null) {
          temp = parseHeader(doc.headers);
          len = getSum(temp.values());
          fieldLen.put(Field.HEADER, len);
          sum.put(Field.HEADER, sum.get(Field.HEADER) + len);
          count.put(Field.HEADER, count.get(Field.HEADER) + doc.headers.size());
        } else {
          fieldLen.put(Field.HEADER, 0.0);
        }

         // 4: anchor
         if (doc.anchors != null) {
         temp = parseAnchor(doc.anchors);
         len = getSum(temp.values());
         fieldLen.put(Field.ANCHOR, len);
         sum.put(Field.ANCHOR, sum.get(Field.ANCHOR) + len);
         count.put(Field.ANCHOR,
         count.get(Field.ANCHOR) + getISum(doc.anchors.values()));
         } else {
         fieldLen.put(Field.ANCHOR, 0.0);
         }

//        // 4: anchor
//        if (doc.anchors != null) {
//          for (String anchor : doc.anchors.keySet()) {
//            len = (double) anchor.split("\\s+").length;
//            sum.put(Field.ANCHOR, sum.get(Field.ANCHOR) + len);
//            count.put(Field.ANCHOR, count.get(Field.ANCHOR) + 1.0);
//            fieldLen.put(Field.ANCHOR, len);
//          }
//        } else {
//          fieldLen.put(Field.ANCHOR, 0.0);
//        }

        lengths.put(doc, fieldLen);
      }
    }

    // calculate average length
    for (Field field : Field.values()) {
      avgLengths.put(field, sum.get(field) / count.get(field));
//      System.out.println(field.name()+" average size: "+sum.get(field) / count.get(field));
    }
  }

  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q,
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

    score += PR_Lambda * functionV(V_NUM, pagerankScores.get(d));
    return score;
  }

  private double functionV(int select, double f) {
    if (select == 0) {
      return Math.log(PR_LambdaPrime + f);
    } else if (select == 1) {
      return f / (PR_LambdaPrime + f);
    } else if (select == 2) {
      return 1 / (PR_LambdaPrime + Math.exp(-f * PR_LambdaPrime2));
    }
    return 0.0;
  }

  // do bm25 normalization
  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d,
      Query q) {
    Field[] fields = Field.values();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      double len = lengths.get(d).get(field);
      if (len == 0)
        continue;
      double b_weight = B_WEIGHTS[i];
      double avgLen = avgLengths.get(field);
      double norm = 1 + b_weight * (len / avgLen - 1);

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

  private double getSum(Collection<Double> c) {
    double res = 0.0;
    for (double d : c) {
      res += d;
    }
    return res;
  }

  private double getISum(Collection<Integer> c) {
    double res = 0.0;
    for (int i : c) {
      res += i;
    }
    return (double) res;
  }

}
