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
	List<Map<String, Integer>> count;	// count of error frequency, list[0]:del, list[1]:ins, list[2]:sub, list[3]:trans
	//List<Map<Character, Integer>> totalCount; 		// total count of a char in mistake	
	Map<String, Integer> pairCount;		// count of char pair count
	Map<String, Integer> singleCount; 	// count of char count 
	
	static double CORRECT_PROB = 0.95;
	//static double EDIT_PROB = 0.05;
	int numChar;

	public EmpiricalCostModel(String editsFile) throws IOException {
		// initialize
		System.out.println("initialize EmpiricalCostModel");
		numErrorType = 4;
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
			
			for(int i=0; i<lenClean; i++){
				c = clean.substring(i,i+1);
				if(singleCount.containsKey(c)){
					singleCount.put(c, singleCount.get(c)+1);
				}else{
					singleCount.put(c, 1);
				}
			}
			
			for(int i=0; i<lenClean; i++){
				if(i == 0){
					c = "*"+clean.charAt(i);
				}else{
					c = clean.substring(i-1,i+1);
				}
				if(pairCount.containsKey(c)){
					pairCount.put(c, pairCount.get(c)+1);
				}else{
					pairCount.put(c, 1);
				}
			}
			
			Error error = detectDistanceOne(clean, noisy);
			int errorType = error.errorType;
			String errorPair = error.errorPair;
			map = count.get(errorType);		// counter for corresponding error type
			
			// TODO: modify as dictionary type to make code clean!
			if(map.containsKey(errorPair)){
				map.put(errorPair, map.get(errorPair)+1);
			}else{
				map.put(errorPair, 1);
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
		// System.out.println("run editProbability");
		if(distance == 0){
			// System.out.println("dis == 0");
			return CORRECT_PROB;
		}
		//original = original.trim();
		//R = R.trim();
		
		Error error;
		Error[] errors = new Error[2];
		double prob;
		
		if(distance == 1){
			// System.out.println("dis == 1");
			// System.out.println("O: "+original + " CE: "+R);
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
			assert(distance <= 2): "can't process distance more than 2";
			return 1e-4;
		}
	}
	private double calculateProb (Error error){
		int errorType = error.errorType;
		String errorPair = error.errorPair;
		Map<String, Integer> map = count.get(errorType);
		double num=1.0, den=numChar;
		
		if( errorType == 0 || errorType == 3){	// del or trans
			assert(errorPair.length() == 2): "wrong errorPair length";
			if(map.containsKey(errorPair))
				num += map.get(errorPair);
			if(pairCount.containsKey(errorPair))
				den += pairCount.get(errorPair);
		}else{	// ins or sub
			assert(errorType == 1 || errorType == 2): "wrong errorType";
			if(map.containsKey(errorPair))
				num += map.get(errorPair);
			if(singleCount.containsKey(errorPair.substring(0,1) ))
				den += singleCount.get(errorPair.substring(0,1) );
		}
		return num/den;

	}
	private Error detectDistanceOne (String original, String R){
		int errorType;
		int lenOriginal = original.length();
		int lenR = R.length();
		int i;
		String errorPair;
		char pre, post;
		
		// System.out.println("original: "+original+" corrupted: "+R);
		if(lenOriginal == lenR+1){	// del
			i=0;
			errorType = 0;
			while(i<lenR && original.charAt(i) == R.charAt(i) )
				i++;
			if(i == 0){
				pre = '*'; // use '*' to represent beginning of a sentence
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
				pre = '*'; // use '*' to represent head
				post = R.charAt(0);	// char been inserted
			}else{
				pre = R.charAt(i-1);
				post =R.charAt(i);
			}
		}else{	// sub OR trans
			assert(lenOriginal == lenR): "original and wrong doesn't have edit distance 1";
			i=0;
			System.out.println ("O: "+original + "W: "+R);
			System.out.println ("O len: "+lenOriginal + "W len: "+lenR);
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
		// System.out.println("errorType: "+errorType + " pre: "+pre +" post: "+post );
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
	int errorType;		// 0:del 1:ins 2:sub 3:trans
	String errorPair;
	public Error(){
		errorType = -1;
		errorPair = null;
	}
	public Error(int et, String str){
		errorType = et;
		errorPair = str;
	}
}

