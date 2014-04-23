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
  /*************************************/
	public static CandidateGenerator candidateGen;
  /*************************************/

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
	  /*************************************/
		candidateGen = CandidateGenerator.get();
		/*************************************/
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(new File(queryFilePath)));
		nsm.setProbabilityType(uniformOrEmpirical);
		
		int totalCount = 0;
		int yourCorrectCount = 0;
		String query = null;
	  /**************************************/
    double mu = 1;
    double lambda = 0.1;
	  /**************************************/
    
		/*
		 * Each line in the file represents one query.  We loop over each query and find
		 * the most likely correction
		 */
		while ((query = queriesFileReader.readLine()) != null) {
			
			/*
			 * Your code here
			 */
      String correctedQuery = query;
			/**************************************/
			double highscore = Double.NEGATIVE_INFINITY;
			
			double score;
			HashMap<String,Integer> candidates = candidateGen.getCandidates(query,languageModel.unaryFreq);
			for(String current: candidates.keySet()){
			  
			  String[] q_words = current.trim().split("\\s+");
        score = Math.log(languageModel.findUnaryProb(q_words[0]));
        for (int i=1; i<q_words.length;i++){
          score = score + Math.log(languageModel.interpolationProb(new Pair<String,String> (q_words[i-1],q_words[i]), lambda));
        }
        score = score + Math.log(nsm.ecm_.editProbability(query, current, candidates.get(current)));
        if (score > highscore){
          highscore = score;
          correctedQuery = current;
        }
			}
      /**************************************/			
			if ("extra".equals(extra)) {
				/*
				 * If you are going to implement something regarding to running the corrector, 
				 * you can add code here. Feel free to move this code block to wherever 
				 * you think is appropriate. But make sure if you add "extra" parameter, 
				 * it will run code for your extra credit and it will run you basic 
				 * implementations without the "extra" parameter.
				 */	
			}
			

			// If a gold file was provided, compare our correction to the gold correction
			// and output the running accuracy
			if(!query.equals(correctedQuery)){
			  System.out.println("Changed: "+query);
	      System.out.println("To: "+correctedQuery);	  
			}
			if (goldFileReader != null) {
				String goldQuery = goldFileReader.readLine();
				if (goldQuery.equals(correctedQuery)) {
					yourCorrectCount++;
				}
				else{
		      System.out.println("Original: "+query);
				  System.out.println("Corrected: "+correctedQuery);
				  System.out.println("Gold: "+goldQuery);
				}
				totalCount++;				
			}
		}
		queriesFileReader.close();
	  
		System.out.println("Correct "+ Integer.toString(yourCorrectCount));
    System.out.println("Total "+ Integer.toString(totalCount));
    System.out.println("Percentage "+ Double.toString((yourCorrectCount+0.0)/totalCount));
    
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		// System.out.println("RUNNING TIME: "+totalTime/1000+" seconds ");
	}
}
