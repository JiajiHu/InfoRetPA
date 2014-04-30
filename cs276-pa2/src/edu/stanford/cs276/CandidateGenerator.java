package edu.stanford.cs276;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.lang.Object;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Pair;

public class CandidateGenerator implements Serializable {

	private static CandidateGenerator cg_;
	
	// Don't use the constructor since this is a Singleton instance
	private CandidateGenerator() {}
	
	public static CandidateGenerator get() throws Exception{
		if (cg_ == null ){
			cg_ = new CandidateGenerator();
		}
		return cg_;
	}
	
	
		public static final Character[] alphanum = {
    'a','b','c','d','e','f','g','h','i','j','k','l','m','n',
    'o','p','q','r','s','t','u','v','w','x','y','z',
    '0','1','2','3','4','5','6','7','8','9',
    ' ',','};
		
		public static final Character[] alphabet = {
	    'a','b','c','d','e','f','g','h','i','j','k','l','m','n',
	    'o','p','q','r','s','t','u','v','w','x','y','z',
	    ' ','\''};
	    
	// Generate all candidates for the target query
	public HashMap<String,Pair<String,Integer>> getCandidates(String query, Dictionary dict) throws Exception {
    boolean genAll = true;
	  HashMap<String,Pair<String,Integer>> candidates = new HashMap<String, Pair<String,Integer>>();	
		boolean flag = false;
	  candidates.put(query, new Pair<String,Integer>(null,0));
	  for(String possible: editDistanceOne(query, dict, false)){
	    if (isValid(possible,dict) && !candidates.containsKey(possible)){
	      candidates.put(possible, new Pair<String,Integer>(null,1));
	      flag = true;
	    }
	  }
	  if (!flag || genAll){
//	    for(String possible: editDistanceOne(query, dict, true && !genAll))
	    for(String possible: editDistanceOne(query, dict, true))
	      if (numInvalid(possible,dict) < 2)
  	      for(String two: editDistanceOne(possible, dict, false))
  	        if (isValid(two,dict) && !candidates.containsKey(two))
  	          candidates.put(two, new Pair<String,Integer>(possible,2));
	  }
	  return candidates;
	}
	
	public Set<String> editDistanceOne(String query, Dictionary dict, boolean tolerate){
	  Set<String> possibles = new HashSet<String>();
	  if (!query.trim().equals(query))
	      return possibles;
	  String[] qwords = query.split("\\s+");
    String front = "";
    for (int i=0;i<qwords.length; i++){
      String back = "";
      //check front and back: if contains non-dictionary words, don't put in candidates (if not tolerate)
      boolean flag = true;
      for (int j=i+2;j<qwords.length;j++){
        if (dict.count(qwords[j])==0){
          flag = false;
          if (!tolerate)
            break;
        }
        back = back + " " + qwords[j];
      }
      if (!tolerate && !flag){
        front = front + qwords[i] + " ";
        continue;
      }
      //try merging words`
      if (i+1<qwords.length){
        if(tolerate || dict.count(qwords[i]+qwords[i+1])>0){
          possibles.add(front + qwords[i] + qwords[i+1] + back);
        }
        back = " " + qwords[i+1] + back;        
      }
      
      Set<String> words = editDistanceOneWords(qwords[i],dict,tolerate);
      for(String word: words){
        possibles.add(front + word + back);
      }
      if (!tolerate && dict.count(qwords[i])==0)
        break;
      front = front + qwords[i] + " ";
    }
	  
	  return possibles;
	}
	
	
  //return strings of edit distance one, including splits, excluding merges
  public Set<String> editDistanceOneWords(String qword, Dictionary dict, boolean tolerate){
    // use rules or not: need rules to avoid stupid mistakes
    boolean rules = false;
    Character[] using = alphabet;
    
	  Set<String> possibles = new HashSet<String>();
	  Set<Pair<String,String>> splits = new HashSet<Pair<String,String>>();
	  // splits
	  for (int i=0; i<=qword.length(); i++){
	    splits.add( new Pair<String,String>(qword.substring(0,i),qword.substring(i)));
	  }
	  for (Pair<String,String> split:splits){
	    if (split.getFirst().isEmpty() || split.getSecond().isEmpty())
	      continue;
	    if (tolerate || (dict.count(split.getFirst())!=0 && dict.count(split.getFirst())!=0))
	      possibles.add((split.getFirst()+" "+split.getSecond()));
	  }
	  // deletes
    for (Pair<String,String> split:splits){
      if (split.getSecond().length()>0){
        // never delete a single letter word
        if (rules && qword.length() == 1)
          continue;
        // never delete a number
        if (rules && Character.isDigit(split.getSecond().charAt(0)))
          continue;
        if (tolerate || (dict.count(split.getFirst()+split.getSecond().substring(1))!=0))
          possibles.add(split.getFirst()+split.getSecond().substring(1));
      }
    }      
    // transposes
	  for (Pair<String,String> split:splits){
      if (split.getSecond().length()>1 && (tolerate || dict.count(split.getFirst()+split.getSecond().charAt(1)+split.getSecond().charAt(0)+split.getSecond().substring(2)) != 0))
        possibles.add(split.getFirst()+split.getSecond().charAt(1)+split.getSecond().charAt(0)+split.getSecond().substring(2));
    }
	  // replaces
	  for (Pair<String,String> split:splits){
      if (split.getSecond().length()>0){
        for (Character c : using){
          // do not allow replacing with ' '
          if (c == ' ')
            continue;
          //never replace a number
          if (rules && Character.isDigit(split.getSecond().charAt(0)))
            continue;
          if (c != ' ' && (tolerate ||dict.count(split.getFirst()+ c +split.getSecond().substring(1))!=0))
            possibles.add(split.getFirst()+ c +split.getSecond().substring(1));
        }
      }
    }
	  // inserts
	  for (Pair<String,String> split:splits){
      for (Character c: using){
        // inserting ' ' is same as split
        if (c == ' ')
          continue;
        if (c != ' ' && (tolerate || dict.count(split.getFirst()+c+split.getSecond()) != 0))
          possibles.add(split.getFirst()+c+split.getSecond());
      }
    }
	  return possibles;
	}
  
  //decide if there are invalid words in a query
  public boolean isValid(String query, Dictionary dict){
    String[] chars = query.split("\\s+");
    if (!join(chars," ").equals(query))
      return false;
    for (int i=0; i<chars.length; i++){
      if (dict.count(chars[i])==0){
        return false;
      }
    }
    return true;
  }
  // count number of invalid words in query
  public int numInvalid(String query, Dictionary dict){
    String[] chars = query.split("\\s+");
    int ret = 0;
    for (int i=0; i<chars.length; i++){
      if (dict.count(chars[i])==0){
        ret++;
      }
    }
    return ret;
  }
  
  public static String join(String[] list, String delim) {

    StringBuilder sb = new StringBuilder();

    String loopDelim = "";

    for(String s : list) {
        sb.append(loopDelim);
        sb.append(s);            
        loopDelim = delim;
    }

    return sb.toString();
}
  
}
