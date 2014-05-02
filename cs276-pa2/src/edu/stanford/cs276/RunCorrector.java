package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.cs276.util.Pair;

public class RunCorrector {

	public static LanguageModel languageModel;
	public static NoisyChannelModel nsm;
  public static CandidateGenerator candidateGen;
  
	public static void main(String[] args) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		// Parse input arguments
		String uniformOrEmpirical = null;
		String queryFilePath = null;
		String goldFilePath = null;
		String extra = null;
		BufferedReader goldFileReader = null;
		if (args.length == 2) {
			// Run without extra and comparing to gold
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
		}
		else if (args.length == 3) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			if (args[2].equals("extra")) {
				extra = args[2];
			} else {
				goldFilePath = args[2];
			}
		} 
		else if (args.length == 4) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			extra = args[2];
			goldFilePath = args[3];
		}
		else {
			System.err.println(
					"Invalid arguments.  Argument count must be 2, 3 or 4" +
					"./runcorrector <uniform | empirical> <query file> \n" + 
					"./runcorrector <uniform | empirical> <query file> <gold file> \n" +
					"./runcorrector <uniform | empirical> <query file> <extra> \n" +
					"./runcorrector <uniform | empirical> <query file> <extra> <gold file> \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt data/gold.txt \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt extra \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt extra data/gold.txt \n");
			return;
		}
		
		if (goldFilePath != null ){
			goldFileReader = new BufferedReader(new FileReader(new File(goldFilePath)));
		}
		
		// Load models from disk
		languageModel = LanguageModel.load();
		nsm = NoisyChannelModel.load();
		candidateGen = CandidateGenerator.get();
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(new File(queryFilePath)));
		nsm.setProbabilityType(uniformOrEmpirical);
		
		int totalCount = 0;
		int yourCorrectCount = 0;
		String query = null;
	
		/**************************************/
		int totalCand = 0;
	  int wrong_unchanged = 0;
	  int w_changed_wrong = 0;
	  int w_changed_right = 0;
	  int right_unchanged = 0;
	  int right_changed_wrong = 0;
	  
	  int smooth_mode = 0;
    double mu = 0.7;
	  if(uniformOrEmpirical.equals("uniform")){
	    // actually mu = 0.7 works best, almost as good as empirical
	    mu = 1.3;
	  }else{
	    mu = 1.3;
	  }
		double lambda = 0.05;
		/**************************************/
    
		/*
		 * Each line in the file represents one query.  We loop over each query and find
		 * the most likely correction
		 */
		while ((query = queriesFileReader.readLine()) != null) {
			
      String correctedQuery = query;
			double highscore = Double.NEGATIVE_INFINITY;
			double score;
      
      HashMap<String,Pair<String,Integer>> candidates = candidateGen.getCandidates(query,languageModel.unaryVals);
		  totalCand = totalCand + candidates.size();
			for(String current: candidates.keySet()){
			  score = languageModel.getLMScore(current, lambda, smooth_mode);
			  score = mu * score + Math.log(nsm.ecm_.editProbability(query, current, candidates.get(current)));
		    if (score > highscore){
		      highscore = score;
		      correctedQuery = current;
		    }
			}
			

			// If a gold file was provided, compare our correction to the gold correction
			// and output the running accuracy
			if (goldFileReader != null) {
				String goldQuery = goldFileReader.readLine();
				if (goldQuery.equals(correctedQuery)) {
					if (query.equals(correctedQuery))
					  	right_unchanged++;
					else
					  	w_changed_right++;
				  	yourCorrectCount++;
				}
				else{
					if (query.equals(correctedQuery))
				  		wrong_unchanged++;
          		  	else{
            			if (query.equals(goldQuery))
                			right_changed_wrong++;
            			else
              				w_changed_wrong++;
         		}
//					System.out.println("\nOriginal:  "+query);
//		      System.out.println("Corrected: "+correctedQuery);
//					System.out.println("Gold:      "+goldQuery);
				}
				totalCount++;				
			}
	    System.out.println(correctedQuery);
		}
		queriesFileReader.close();

//		System.out.println("\n***********************************************");
//	    System.out.println("Correct "+ Integer.toString(yourCorrectCount));
//	    System.out.println("Total "+ Integer.toString(totalCount));
//	    System.out.println("Percentage "+ Double.toString((yourCorrectCount+0.0)/totalCount));
//
//	    System.out.println("Correct unchanged "+ Integer.toString(right_unchanged));
//	    System.out.println("Wrong to right "+ Integer.toString(w_changed_right));
//	    System.out.println("Wrong to wrong "+ Integer.toString(w_changed_wrong));
//	    System.out.println("Wrong unchanged "+ Integer.toString(wrong_unchanged));
//	    System.out.println("Correct to wrong "+ Integer.toString(right_changed_wrong));
//	    
//	    System.out.println("Total candidates generated: "+Integer.toString(totalCand));

		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
//		 System.out.println("RUNNING TIME: "+totalTime/1000+" seconds ");
	}
}
