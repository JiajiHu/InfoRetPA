package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import edu.stanford.cs276.util.BinDictionary;
import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Pair;


public class LanguageModel implements Serializable {

	private static LanguageModel lm_;

	public Dictionary unaryFreq = new Dictionary();
	public BinDictionary binaryFreq = new BinDictionary();
  
	// Do not call constructor directly since this is a Singleton
	private LanguageModel(String corpusFilePath) throws Exception {
		constructDictionaries(corpusFilePath);
	}

  public double findUnaryProb(String word){
	  return (unaryFreq.count(word)+0.0)/unaryFreq.termCount();
	}
	
	public double findBinaryProb(Pair<String,String> words){
    return (binaryFreq.count(words)+0.0)/unaryFreq.count(words.getFirst());
  }
	
	public double interpolationProb(Pair<String,String> words, double lambda, int mode){
	  if (mode == 0)
	    return lambda*findUnaryProb(words.getSecond()) + (1.0-lambda)*(findBinaryProb(words));
	  else if (mode == 1){
	    if (binaryFreq.count(words) < 1)
	      return findUnaryProb(words.getSecond());
	    else
	      return lambda*findBinaryProb(words);
	  }
	  return 0;
	}
	
	public double getLMScore(String current, double lambda, int smooth_mode){
	  double score;
	  String[] q_words = current.trim().split("\\s+");
      score = Math.log(findUnaryProb(q_words[0]));
      for (int i=1; i<q_words.length;i++){
        score = score + Math.log(interpolationProb(new Pair<String,String> (q_words[i-1],q_words[i]), lambda, smooth_mode));
      }
	  return score;
	}

	public void constructDictionaries(String corpusFilePath)
			throws Exception {

		System.out.println("Constructing dictionaries...");
		File dir = new File(corpusFilePath);
		for (File file : dir.listFiles()) {
			if (".".equals(file.getName()) || "..".equals(file.getName())) {
				continue; // Ignore the self and parent aliases.
			}
			System.out.printf("Reading data file %s ...\n", file.getName());
			BufferedReader input = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = input.readLine()) != null) {
			  String[] tokens = line.trim().split("\\s+");
			  for (int i = 0; i < tokens.length; i++) {
			    unaryFreq.add(tokens[i]);
			    if (i>0){
			      Pair<String,String> seq = new Pair<String,String> (tokens[i-1],tokens[i]);
			      binaryFreq.add(seq);
			    }
			  }
			}
			input.close();
		}
		System.out.println("Done.");
	}
	
	// Loads the object (and all associated data) from disk
	public static LanguageModel load() throws Exception {
		try {
			if (lm_==null){
				FileInputStream fiA = new FileInputStream(Config.languageModelFile);
				ObjectInputStream oisA = new ObjectInputStream(fiA);
				lm_ = (LanguageModel) oisA.readObject();
			}
		} catch (Exception e){
			throw new Exception("Unable to load language model.  You may have not run build corrector");
		}
		return lm_;
	}
	
	// Saves the object (and all associated data) to disk
	public void save() throws Exception{
		FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
		ObjectOutputStream save = new ObjectOutputStream(saveFile);
		save.writeObject(this);
		save.close();
	}
	
	// Creates a new lm object from a corpus
	public static LanguageModel create(String corpusFilePath) throws Exception {
		if(lm_ == null ){
			lm_ = new LanguageModel(corpusFilePath);
		}
		return lm_;
	}
}
