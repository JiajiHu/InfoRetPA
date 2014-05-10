package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarityScorer extends AScorer
{
	public CosineSimilarityScorer(Map<String,Double> idfs)
	{
		super(idfs);
	}
	
	///////////////weights///////////////////////////
    private final double URL_WEIGHT = -1;
    private final double TITLE_WEIGHT  = -1;
    private final double BODY_WEIGHT = -1;
    private final double HEADER_WEIGHT = -1;
    private final double ANCHOR_WEIGHT = -1;
    private final double[] WEIGHTS = {URL_WEIGHT, TITLE_WEIGHT, BODY_WEIGHT, HEADER_WEIGHT, ANCHOR_WEIGHT};
    private final double SMOOTHING_BODY_LENGTH = -1;
    private final boolean subLinear = false;
    
	public double getNetScore(Map<Field,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		/******************************************/
		double score = 0.0;
		int count = 0;
		for(Field field: Field.values()){
			double weight = WEIGHTS[count];
			double temp = 0.0;
			Map<String, Double> tfs_field = tfs.get(field);
			for(String word: q.queryWords){
				temp += tfQuery.get(word) * tfs_field.get(word);
			}
			score += temp * weight;
			count++;
		}
		return score;
		/******************************************/
	}

	
	public void normalizeTFs(Map<Field,Map<String, Double>> tfs,Document d, Query q)
	{
		/******************************************/
		double bodyLen = d.body_length;
		double norm = bodyLen + 500; // normalize factor
		for(Field field: Field.values()){
			Map<String, Double> map = tfs.get(field);
			for(String word: map.keySet()){
				map.put(word, map.get(word)/(500.0+bodyLen));
			}
		}
		/******************************************/
	}

	
	@Override
	public double getSimScore(Document d, Query q) 
	{
		Map<Field,Map<String, Double>> tfs = this.getDocTermFreqs(d,q,subLinear);
		this.normalizeTFs(tfs, d, q);
		Map<String,Double> tfQuery = getQueryFreqs(q,subLinear);
        return getNetScore(tfs,q,tfQuery,d);
	}
	
	private double dotProduct (Map<String, Double> v1, Map<String, Double> v2){
		double res = 0.0;
		for(String word: v1.keySet()){
			if(v2.containsKey(word)){
				res += v2.get(word);
			}
		}
		return res;
	}
}
