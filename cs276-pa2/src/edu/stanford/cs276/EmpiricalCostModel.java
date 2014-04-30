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
		original = original.trim();
		R = R.trim();
		
		Error error;
		//Error[] errors;
		int errorType;
		String errorPair, c;
		Map<String, Integer> map;
		
		if(distance == 1){
			// System.out.println("dis == 1");
			// System.out.println("O: "+original + " CE: "+R);
			error = detectDistanceOne (original, R);
			errorType = error.errorType;
			errorPair = error.errorPair;
			map = count.get(errorType);
			
			// System.out.println("pre: "+pre + " post: " +post);
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

		}else if(distance == 2){
			// errors = detectDistanceTwo (original, R); 
			// System.out.println("dis == 2");			
			return 0.0001;
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
	
	
	/*
		private Error[] detectDistanceTwo (String original, String R){
		Error[] errors = new Error[2];
		int lenOriginal = original.length();
		int lenR = R.length();
		int i;
		char pre, post;
		String mid;

		if(lenOriginal == lenR + 2){	// 2 deletion
			i = 0;
			while(i<lenR && original.charAt(i) == R.charAt(i) )
				i++;
			if(i == 0){
				pre = ' ';
				post = original.charAt(0);
			}else{
				pre = original.charAt(i-1);
				post = original.charAt(i);
			}
			errors[0] = new Error(0, pre, post);
			mid = new StringBuilder(R).insert(i, original.charAt(i) ).toString();
			errors[1] = detectDistanceOne(original, mid);
			assert(errors[1].errorType == 0): "wrong error type in double deletion in detectDistanceTwo";

		}else if(lenOriginal == lenR - 2){	// 2 insertion
			i = 0;
			while(i<lenOriginal && original.charAt(i) == R.charAt(i) )
				i++;
			if(i == 0){
				pre = ' ';
				post = R.charAt(0);
			}else{
				pre = R.charAt(i-1);
				post = R.charAt(i);
			}
			errors[0] = new Error(1, pre, post);
			mid = new StringBuilder(R).deleteCharAt(i).toString();
			errors[1] = detectDistanceOne(original, mid);
			assert(error[1].errorType == 1): "wrong error type in double insertion in detectDistanceTwo";

		}else if(lenOriginal == lenR + 1){	// 1 deletion + 1 sub/trans
			// 1. detect deletion: inter-word deletion OR intra-word deletion
			String[] originalWords = original.split("\\s+"); 
			String[] RWords = R.split("\\s+");
			int j=0, index1=0, index2=-1;	// index1: word of del, index2: word of sub/trans

			if(originalWords.length == RWords.length){
				// 1.1 must be intra-word deletion		
				j = 0; 
				while(j<RWords.length && originalWords[i].length() == RWords.length()){
					if(!originalWords[j].equals(RWords[j]) ){
						index2 = j;
					}
					j++;
				}
				assert(j!=RWords.length);	// must exist at least one word with different length
				index1 = j;
				
				j++; // start from the next word of the different word
				while(j<RWords.length && index2 == -1){
					assert(originalWords[j].length() == RWords[j].length() ): "only exist one word with different length and should be found already";
					if(!originalWords[j].equals(RWords[j]) ){
						index2 = j;
						break;
					}	
					j++;
				}

				if(index2 != -1){
					// 1.1.1 sub/trans happens in a different word
					assert(index1 != index2);
					errors[0] = detectDistanceOne(originalWords[index1], RWords[inedex2]);
					assert(errors[0].errorType == 0);
					errors[1] = detectDistanceOne(originalWords[index2], RWords[index2]);
					assert(errors[1].errorType == 2 || errors[1].errorType == 3);
					
				}else{
					// 1.1.2 sub/trans happens in the same word
					errors[0] = detectDistanceOne(originalWords[j], RWords[j]);
				}
			}else{	
				// 1.2 must be inter-word deltetion (merge)
				j = 0;
				while(j<RWords.length && )

			}

			// 2. detect 

		}else if(lenOriginal == lenR - 1){	// 1 insertion + 1 sub/trans
			
		}else{
			assert(lenOriginal == lenR): "original and wrong doesn't have edit distance 2";

		}
		return errors;
	}
	 */
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

