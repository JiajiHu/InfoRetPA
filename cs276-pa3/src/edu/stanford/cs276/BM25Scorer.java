package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.cs276.util.Pair;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

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
  private final double PR_LambdaPrime2 = -1;	// for the 3rd type of V function
  
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
    Map<String, Double> count = new HashMap<String, Double>();	// field name -> num of field over all docs
    Map<String, Double> sum = new HashMap<String, Double>();	// field name -> len of field over all docs
    
    // initialize
    for(Field field: Field.values()){
    	count.put(field.name(), 0.0);
    	sum.put(field.name(), 0.0);
    	avgLengths.put(field.name(), 0.0);
    }
    
    // loop through queries to populate lengths, avgLengths, pagerankScores
    for (Query query : queryDict.keySet()) {
    	Map<String, Document> mapUrl = queryDict.get(query);
    	for (String url : queryDict.get(query).keySet()) {
        	Document doc = mapUrl.get(url);
        	pagerankScores.put(doc, (double) doc.page_rank);
        	Map<String, Double> fieldLen = new HashMap<String, Double>();	// field name -> len
        	
        	// 0: url
        	String fieldName = "URL";
        	Map<String, Double> temp = parseURL(doc.url);
        	double len = temp.values().size();
        	fieldLen.put(fieldName, len);
        	sum.put(fieldName, sum.get(fieldName)+len);
        	count.put(fieldName, count.get(fieldName)+1.0);
        	
        	// 1: title
        	fieldName = "TITLE";
        	temp = parseTitle(doc.title);
        	len = temp.values().size();
        	fieldLen.put(fieldName, len);
        	sum.put(fieldName, sum.get(fieldName)+len);
        	count.put(fieldName, count.get(fieldName)+1.0);
        	
        	// 2: body
        	fieldName = "BODY";
        	len = doc.body_length;
        	fieldLen.put(fieldName, len);
        	sum.put(fieldName, sum.get(fieldName)+len);
        	count.put(fieldName, count.get(fieldName)+1.0);
        	
        	// 3: header
        	fieldName = "HEADER";
        	if(doc.headers != null){
        		temp = parseHeader(doc.headers);
            	len = getSum(temp.values());
            	fieldLen.put(fieldName, len);
            	sum.put(fieldName, sum.get(fieldName)+len);
            	count.put(fieldName, count.get(fieldName)+doc.headers.size());
        	}else{
        		fieldLen.put(fieldName, 0.0);
        	}
        	
        	// 4: anchor
        	fieldName = "ANCHOR";
        	if(doc.anchors != null){
        		len = 0.0;
        		for(String anchor: doc.anchors.keySet()){
            		String[] splited = anchor.split("\\s+");
            		len += splited.length;
            		sum.put(fieldName, sum.get(fieldName)+splited.length);
            		count.put(fieldName, count.get(fieldName)+1.0);
            	}
        		fieldLen.put(fieldName, len);
        	}else{
        		fieldLen.put(fieldName, 0.0);
        	}
        	lengths.put(doc, fieldLen);
        }
    }
    
    // calculate average length
    for(Field field: Field.values()){
    	String fieldName = field.name();
    	avgLengths.put(fieldName, sum.get(fieldName)/count.get(fieldName));
    }
    /****************************************/
  } 
  private double getSum (Collection<Double> c){
	  double res = 0.0;
	  for(double d: c){
		  res += d;
	  }
	  return res;
  }
  public double getNetScore(Map<Field, Map<String, Double>> tfs, Query q, Map<String, Double> tfQuery, Document d) {
	  double score = 0.0;
	  /****************************************/
	  for(Field field: Field.values()){
		  Map<String, Double> map = tfs.get(field);
		  String fieldName = field.name();
		  double weight = weights.get(fieldName);
		  for(String word: map.keySet()){
			  if(idfs.containsKey(word)){
				  score += map.get(word)*weight*idfs.get(word)/(K1+map.get(word));
			  }else{	
				  score += map.get(word)*weight*idfs.get("unseen term")/(K1+map.get(word));
			  }  
		  }
	  }
	  // TODO: make sure how V(f) works?
	  score += PR_Lambda*pagerankScores.get(d);
	  /****************************************/
	  return score;
  }
  private double functionV(int select, double f){
	  // TODO: meaning of lambda
	  double res = 0.0;
	  if(select == 0){
		  res = Math.log(PR_LambdaPrime+f);
	  }else if(select == 1){
		  res = f/(PR_LambdaPrime+f);
	  }else{
		  res = 1/(PR_LambdaPrime+Math.exp(-f*PR_LambdaPrime2));
	  }
	  return res;
  }
  
  // do bm25 normalization
  public void normalizeTFs(Map<Field, Map<String, Double>> tfs, Document d, Query q) {
	  /****************************************/
	  for(Field field: Field.values()){
		  Map<String, Double> map = tfs.get(field);
		  String fieldName = field.name();
		  double b_weight = bWeights.get(fieldName);
		  double avgLen = avgLengths.get(fieldName);
		  double len = avgLen;	// TODO: how to deal with length >
		  
		  if(lengths.containsKey(d)){
			  len = lengths.get(d).get(fieldName);
		  }
		  
		  double norm = 1+b_weight*(len/avgLen-1);
		  
		  if(Math.abs(avgLen) < 1e-4){
			  System.out.println("shouldn't happen in this data set");
			  for(String word: map.keySet()){
				  map.put(word, 0.0);
			  }
		  }else{
			  for(String word: map.keySet()){
				  map.put(word, map.get(word)/norm);
			  }
		  }  
	  } 
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
