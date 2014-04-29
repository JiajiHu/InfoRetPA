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
	
	Map<String, Integer> pairCount;		// count of char pair count
	Map<Character, Integer> charCount; 	// count of char count 
	
	static double CORRECT_PROB = 0.95;
	static double EDIT_PROB = 0.05;
	int numChar;

	public EmpiricalCostModel(String editsFile) throws IOException {
		// initialize
		System.out.println("initialize EmpiricalCostModel");
		numErrorType = 4;
		count = new ArrayList<Map<Character, Map<Character, Integer> >>();
		totalCount = new ArrayList<Map<Character, Integer>>();
		pairCount = new HashMap<String, Integer>();
		charCount = new HashMap<Character, Integer>();
		
		numChar = CandidateGenerator.alphanum.length;
		Map<Character, Map<Character, Integer> > map;
		Map<Character, Integer> mapCnt;
		Map<Character, Integer> temp;
		
		for(int i=0; i<numErrorType; i++){
			map = new HashMap<Character, Map<Character, Integer>>();
			count.add(map);
			mapCnt = new HashMap<Character, Integer> ();
			totalCount.add(mapCnt);
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
			char c, p;
			int lenNoisy = noisy.length();
			int lenClean = clean.length();
			
			for(int i=0; i<lenClean; i++){
				c = clean.charAt(i);
				if(charCount.containsKey(c)){
					charCount.put(c, charCount.get(c)+1);
				}else{
					charCount.put(c, 1);
				}
			}
			
			for(int i=0; i<lenClean; i++){
				c = clean.charAt(i);
				if(i == 0){
					p = '*';
				}else{
					p = clean.charAt(i-1);
				}
				String pair = ""+p+""+c;
			}
			
			Error error = detectDistanceOne(clean, noisy);
			int errorType = error.errorType;
			char pre = error.pre;
			char post = error.post;
			map = count.get(errorType);		// counter for corresponding error type
			mapCnt = totalCount.get(errorType);	// map from char2 to count
			// System.out.println("O: "+clean+" W: "+noisy);
			// printError(error);
			
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
		original = original.trim();
		R = R.trim();
		Error error;
		Error[] errors;
		int errorType;
		Map<Character, Map<Character, Integer> > counter;
		char pre, post;

		if(distance == 1){
			// System.out.println("dis == 1");
			// System.out.println("O: "+original + " CE: "+R);
			error = detectDistanceOne (original, R);
			errorType = error.errorType;
			pre = error.pre;
			post = error.post;
			// System.out.println("pre: "+pre + " post: " +post);
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
				// System.out.println("map containsKey: "+pre); // System.out.println("num: "+num+" den: "+den);
				// System.out.println("1 prob="+num/den);
				return Math.pow(EDIT_PROB, distance)*num/den;
			}else{
				// System.out.println("2 prob="+1/numChar);
				return Math.pow(EDIT_PROB, distance)/numChar;
			}

		}else if(distance == 2){
			// errors = detectDistanceTwo (original, R); 
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
		return new Error(errorType, pre, post);
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
		str = str + "pre: "+e.pre + " post: "+e.post +" ";
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
	char pre;
	char post;
	public Error(){
		errorType = -1;
		pre = '@';
		post = '@';
	}
	public Error(int et, char _pre, char _post){
		errorType = et;
		pre = _pre;
		post = _post;
	}
}

