package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import edu.stanford.cs276.util.Pair;

public class EmpiricalCostModel implements EditCostModel{
	int numErrorType;
	List<Map<String, Integer>> count;	// error frequency count -- list[0]:del, list[1]:ins, list[2]:sub, list[3]:trans
	Map<String, Integer> pairCount;		// char pair count
	Map<String, Integer> singleCount; // char count 
	
	static double CORRECT_PROB = 0.95;
	static double ERROR_PROB = 1e-6;
	int numChar;

	public EmpiricalCostModel(String editsFile) throws IOException {
		// initialize
		System.out.println("initialize EmpiricalCostModel");
		numErrorType = 5;
		count = new ArrayList<Map<String, Integer>>();
		pairCount = new HashMap<String, Integer>();
		singleCount = new HashMap<String, Integer>();
		
		numChar = CandidateGenerator.alphanum.length;
		Map<String, Integer> map;
		
		for(int i=0; i<numErrorType; i++){
			map = new HashMap<String, Integer>();
			count.add(map);
		}
		
		// collect statistics from data
		BufferedReader input = new BufferedReader(new FileReader(editsFile));
		System.out.println("Constructing edit distance map...");
		String line = null;
		
		while ((line = input.readLine()) != null) {
			Scanner lineSc = new Scanner(line);
			lineSc.useDelimiter("\t");
			String noisy = lineSc.next();
			String clean = lineSc.next();
			
			// Determine type of error and record probability
			/*
			 * Your code here
			 */
			int lenClean = clean.length();
			String c;
			
			for(int i=0; i<lenClean-1; i++){
				c = clean.substring(i,i+1);
				if(singleCount.containsKey(c)){
					singleCount.put(c, singleCount.get(c)+1);
				}else{
					singleCount.put(c, 1);
				}
			}
			
			for(int i=0; i<lenClean; i++){
				if(i == 0){
					c = " "+clean.charAt(i);
				}else{
					c = clean.substring(i-1,i+1);
				}
				if(pairCount.containsKey(c)){
					pairCount.put(c, pairCount.get(c)+1);
				}else{
					pairCount.put(c, 1);
				}
			}
			
			try {
			  Error error = detectDistanceOne(clean, noisy);
	      int errorType = error.errorType;
	      String errorPair = error.errorPair;
	      map = count.get(errorType);   // counter for corresponding error type
	      
	      if(map.containsKey(errorPair)){
	        map.put(errorPair, map.get(errorPair)+1);
	      }else{
	        map.put(errorPair, 1);
	      }
			} catch (Exception e) {
			}
						
		}

		input.close();
		System.out.println("Done.");
	}

	// You need to update this to calculate the proper empirical cost
	public double editProbability(String original, String R, Pair<String, Integer> pair) {
		/*
		 * Your code here
		 */
		int distance = pair.getSecond();
		String prev = pair.getFirst();
		if(distance == 0){
			return CORRECT_PROB;
		}
		
		Error error;
		Error[] errors = new Error[2];
		double prob;
		
		try {
	    if(distance == 1){
	      error = detectDistanceOne (original, R);
	      prob = calculateProb (error);
	      return prob;
	    }else if(distance == 2){
	      double prob1, prob2;
	      errors[0] = detectDistanceOne (original, prev);
	      errors[1] = detectDistanceOne (prev, R);
	      prob1 = calculateProb (errors[0]);
	      prob2 = calculateProb (errors[1]);
	      prob = prob1*prob2;
	      return prob;
	    }else{
	      return ERROR_PROB;
	    }
		} catch (Exception e){
		  return ERROR_PROB;
		}

	}
	private double calculateProb (Error error){
		int errorType = error.errorType;
		String errorPair = error.errorPair;
		Map<String, Integer> map = count.get(errorType);
		double num = 1;
		double den=num;
		if(errorType == 1 || errorType == 2)
	    den = den * numChar;
		if( errorType == 0 || errorType == 3){	// del or trans
			assert(errorPair.length() == 2): "wrong errorPair length";
			if(map.containsKey(errorPair))
				num += map.get(errorPair);
			if(pairCount.containsKey(errorPair))
				den += pairCount.get(errorPair);
		}else if (errorType == 1 || errorType == 2){	// ins or sub
			if(map.containsKey(errorPair))
				num += map.get(errorPair);
			if(singleCount.containsKey(errorPair.substring(0,1) ))
				den += singleCount.get(errorPair.substring(0,1) );
		}
		else
		  return 1e-6;
		return num/den;

	}
	private Error detectDistanceOne (String original, String R) throws Exception{
		int errorType;
		int lenOriginal = original.length();
		int lenR = R.length();
		int i;
		String errorPair;
		char pre, post;
		
		if(lenOriginal == lenR+1){	// del
			i=0;
			errorType = 0;
			while(i<lenR && original.charAt(i) == R.charAt(i) )
				i++;
			if(i == 0){
				pre = ' '; // use ' ' to represent beginning of a sentence
				post = original.charAt(0);
			}else{
				pre = original.charAt(i-1);
				post = original.charAt(i);
			}	
		}else if(lenOriginal == lenR-1){	// ins
			i=0;
			errorType = 1;
			while(i<lenOriginal && original.charAt(i) == R.charAt(i) )
				i++;
			if(i == 0){
				pre = ' ';
				post = R.charAt(0);
			}else{
				pre = R.charAt(i-1);
				post =R.charAt(i);
			}
		}else if (lenOriginal == lenR){	// sub OR trans
			i=0;
			while(i<lenOriginal-1 && original.charAt(i) == R.charAt(i) )
				i++;
			if(i != lenOriginal-1 && original.charAt(i) == R.charAt(i+1) && original.charAt(i+1) == R.charAt(i)){
				errorType = 3;
				pre = original.charAt(i);
				post = R.charAt(i);
			}else{
				errorType = 2;
				pre = original.charAt(i);
				post = R.charAt(i);
			}
		}
		else{
		  return new Error();
		}
		return new Error(errorType, ""+pre+post);
	}

	private void printMap (Map<Character, Integer> map) {
		if(map == null){
			System.out.println("empty map");
		}else{
			String str = "";
			for(char c: map.keySet() ){
				str = str + c+":"+map.get(c)+" ";
			}
			System.out.println(str);
		}
	}
	private void printError (Error e){
		String str = "";
		int type = e.errorType;
		switch(type){
			case 0: str = str+"del ";
					break;
			case 1: str = str+"ins ";
					break;
			case 2: str = str+"sub ";
					break;
			case 3: str = str+"trans ";
					break;
			default: 
		}
		str = str + e.errorPair;
		System.out.println(str);
	}
}
class Error {
	int errorType;		// 0:del 1:ins 2:sub 3:trans 4:ERROR
	String errorPair;
	public Error(){
		errorType = 4;
		errorPair = null;
	}
	public Error(int et, String str){
		errorType = et;
		errorPair = str;
	}
}

