package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class EmpiricalCostModel implements EditCostModel{
	int numErrorType;
	List<Map<Character, Map<Character, Integer> >> count;	// count of error frequency, list[0]:del, list[1]:ins, 
															// list[2]:sub, list[3]:trans
	List<Map<Character, Integer>> totalCount; 		// total count of a char in mistake
	static double CORRECT_PROB = 0.95;
	static double EDIT_PROB = 0.05;
	int numChar;

	public EmpiricalCostModel(String editsFile) throws IOException {
		// initialize
		System.out.println("initialize EmpiricalCostModel");
		numErrorType = 4;
		count = new ArrayList<Map<Character, Map<Character, Integer> >>();
		totalCount = new ArrayList<Map<Character, Integer>>();
		numChar = CandidateGenerator.alphanum.length;
		Map<Character, Map<Character, Integer> > map;
		Map<Character, Integer> mapCnt;
		
		for(int i=0; i<numErrorType; i++){
			map = new HashMap<Character, Map<Character, Integer>>();
			count.add(map);
			mapCnt = new HashMap<Character, Integer> ();
			totalCount.add(mapCnt);
		}
		
		// count error from data
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
			int lenNoisy = noisy.length();
			int lenClean = clean.length();
			Error error = detectDistanceOne(clean, noisy);
			
			int errorType = error.errorType;
			char pre = error.pre;
			char post = error.post;
			map = count.get(errorType);		// counter for corresponding error type
			mapCnt = totalCount.get(errorType);	// map from char2 to count
			
			Map<Character, Integer> temp;
			
			if(map.containsKey(pre)){
				mapCnt.put(pre, mapCnt.get(pre)+1);
				
				temp = map.get(pre);
				if(temp.containsKey(post)){
					temp.put(post, temp.get(post)+1);
				}else{
					temp.put(post, 1);
				}
			}else{
				mapCnt.put(pre, 1);
				temp = new HashMap<Character, Integer>();
				temp.put(post, 1);
				map.put(pre, temp);
			}
			
		}

		input.close();
		System.out.println("size of del keySet: "+count.get(0).keySet().size() );
		for(char c: count.get(0).keySet()){
			System.out.print("pre: "+c);
			System.out.print(" ");
			System.out.print("setsize: "+count.get(0).get(c).keySet().size() );
		}
		System.out.println("Done.");
	}
	
	// You need to update this to calculate the proper empirical cost
	@Override
	public double editProbability(String original, String R, int distance) {
		/*
		 * Your code here
		 */
		// System.out.println("run editProbability");
		
		if(distance == 0){
			// System.out.println("dis == 0");
			return CORRECT_PROB;
		}
		
		Error error;
		int errorType;
		Map<Character, Map<Character, Integer> > counter;
		char pre, post;
		
		if(distance == 1){
			// System.out.println("dis == 1");
			System.out.println("O: "+original + " C: "+R);
			error = detectDistanceOne (original, R);
			errorType = error.errorType;
			pre = error.pre;
			post = error.post;

			double num=0.0, den=0.0;
			Map<Character, Map<Character, Integer>> map = count.get(errorType);
			Map<Character, Integer> mapCnt = totalCount.get(errorType);
			Map<Character, Integer> temp;
			


			if(map.containsKey(pre)){
				den = 0.0 + mapCnt.get(pre) + numChar;
				temp = map.get(pre);
				if(temp.containsKey(post)){
					num = 1.0+temp.get(post);
				}else{
					num = 1.0;
				}
				// System.out.println("map containsKey: "+pre);
				// System.out.println("num: "+num+" den: "+den);
				
				System.out.println("1 prob="+num/den);
				return Math.pow(EDIT_PROB, distance)*num/den;
			}else{
				// System.out.println("return 1.0/numChar, numChar: "+numChar);
				System.out.println("2 prob="+1/numChar);
				return Math.pow(EDIT_PROB, distance)/numChar;
			}

		}else if(distance == 2){

			// System.out.println("dis == 2");			
			return 0.5;
		}else{
			assert(distance <= 2): "can't process distance more than 2";
			return 0.5;
		}
	}
	
	private Error detectDistanceOne (String original, String R){
		int errorType;
		int lenOriginal = original.length();
		int lenR = R.length();
		int i;
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
			
			// return new Error(errorType, pre, post); 
				
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
			
			// return new Error(errorType, pre, post);
			
		}else{	// sub OR trans
			assert(lenOriginal == lenR): "original and wrong doesn't have edit distance 1";
			i=0;
			while(i<lenOriginal-1 && original.charAt(i) == R.charAt(i) )
				i++;
			if(i != lenOriginal-1 && original.charAt(i) == R.charAt(i+1) && original.charAt(i+1) == R.charAt(i)){
				errorType = 3;
				pre = original.charAt(i);
				post = R.charAt(i);
				
				// return new Error(errorType, pre, post);
			}else{
				errorType = 2;
				pre = original.charAt(i);
				post = R.charAt(i);
				
				// return new Error(errorType, pre, post);	
			}
		}
		// System.out.println("errorType: "+errorType + " pre: "+pre +" post: "+post );
		return new Error(errorType, pre, post);
	}
}
class Error {
	int errorType;		// 0:del 1:ins 2:sub 3:trans
	char pre;
	char post;
	
	public Error(){
		errorType = -1;
		pre = ' ';
		post = ' ';
	}
	public Error(int et, char _pre, char _post){
		errorType = et;
		pre = _pre;
		post = _post;
	}
}

