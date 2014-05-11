package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.cs276.util.Pair;

public class BM25Scorer extends AScorer {
  Map<Query, Map<String, Document>> queryDict;
  Map<String, Double> weights;
  Map<String, Double> bWeights;
  
  // weights
  private final double URL_WEIGHT = -1;
  private final double TITLE_WEIGHT = -1;
  private final double BODY_WEIGHT = -1;
  private final double HEADER_WEIGHT = -1;
  private final double ANCHOR_WEIGHT = -1;
  
  // bm25 specific weights
  private final double B_URL = -1;
  private final double B_TITLE = -1;
  private final double B_HEADER = -1;
  private final double B_BODY = -1;
  private final double B_ANCHOR = -1;

  private final double K1 = -1;
  private final double PR_Lambda = -1;
  private final double PR_LambdaPrime = -1;
  private final boolean subLinear = false;
  
  Map<Document, Map<String, Double>> lengths;	// doc->field->length
  Map<String, Double> avgLengths;	// field->length
  Map<Document, Double> pagerankScores;	// doc->pagerank

  // ////////////////////////////////////////

  public BM25Scorer(Map<String, Double> idfs, Map<Query, Map<String, Document>> queryDict) {
	  super(idfs);
	  this.queryDict = queryDict;
	  this.calcAverageLengths();
	  this.weights = new HashMap<String, Double>();
	  this.bWeights = new HashMap<String, Double>();
	  
	  weights.put("URL", URL_WEIGHT);
	  weights.put("TITLE", TITLE_WEIGHT);
	  weights.put("BODY", BODY_WEIGHT);
	  weights.put("HEADER", HEADER_WEIGHT);
	  weights.put("ANCHOR", ANCHOR_WEIGHT);
	  
	  bWeights.put("URL", B_URL);
	  bWeights.put("TITLE", B_TITLE);
	  bWeights.put("BODY", B_BODY);
	  bWeights.put("HEADER", B_HEADER);
	  bWeights.put("ANCHOR", B_ANCHOR);	  
  }
  
  // sets up average lengths for bm25, also handles pagerank
  public void calcAverageLengths() {
    lengths = new HashMap<Document, Map<String, Double>>();
    avgLengths = new HashMap<String, Double>();
    pagerankScores = new HashMap<Document, Double>();

    /****************************************/
    int numField = Field.values().length;
    List<Double> count = new ArrayList<Double>();	// Store as the follwoing order: URL,TITLE,BODY,HEADER,ANCHOR 
    List<Double> sum = new ArrayList<Double>();
    for(int i=0; i<numField; i++){
    	count.set(i, 0.0);
    	sum.set(i, 0.0);
    }
    
    // loop through queries
    for (Query query : queryDict.keySet()) {
    	Map<String, Document> mapUrl = queryDict.get(query);
    	for (String url : queryDict.get(query).keySet()) {
        	Document doc = mapUrl.get(url);
        	pagerankScores.put(doc, (double) doc.page_rank);
        	
        	// 0: url
        	Map<String, Double> temp = parseURL(doc.url);
        	sum.set(0, sum.get(0)+temp.values().size());
        	count.set(0, count.get(0)+1.0);
        	
        	// 1: title
        	temp = parseTitle(doc.title);
        	sum.set(1, sum.get(1)+temp.values().size());
        	count.set(1, count.get(1)+1.0);
        	
        	// 2: body
        	sum.set(2, sum.get(2)+doc.body_length);
        	count.set(2, count.get(2)+1.0);
        	
        	// 3: header
        	temp = parseHeader(doc.headers);
        	sum.set(3, sum.get(3)+temp.values().size());
        	count.set(3, count.get(3)+doc.headers.size());
        	
        	// 4: anchor
        	for(String anchor: doc.anchors.keySet()){
        		String[] splited = anchor.split("\\s+");
        		sum.set(4, sum.get(4)+splited.length);
        		count.set(4, count.get(4)+1.0);
        	}
        	
        }
    }
    
    // calculate average length
    Field[] fields = Field.values();
    System.out.println("check: URL,TITLE,BODY,HEADER,ANCHOR == "+fields);
    
    for(int i=0; i<numField; i++){
    	String field = fields[i].name();
    	avgLengths.put(field, sum.get(i)/count.get(i));
    }
    /****************************************/
  } 
  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q,
      Map<String, Double> tfQuery, Document d) {
    double score = 0.0;

    /****************************************/
    
    /****************************************/
    
    return score;
  }

  // do bm25 normalization
  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d, Query q) {
	  /****************************************/
	    
	  /****************************************/  
  }

  @Override
  public double getSimScore(Document d, Query q) {

    Map<Field, Map<String, Double>> tfs = this.getDocTermFreqs(d, q, subLinear);

    this.normalizeTFs(tfs, d, q);

    Map<String, Double> tfQuery = getQueryFreqs(q,subLinear);

    return getNetScore(tfs, q, tfQuery, d);
  }
}
