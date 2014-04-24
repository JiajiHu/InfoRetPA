package edu.stanford.cs276;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
	    ' ',','};
	    
	// Generate all candidates for the target query
	public HashMap<String,Integer> getCandidates(String query, Dictionary dict) throws Exception {
	  HashMap<String,Integer> candidates = new HashMap<String, Integer>();	
		/****************************/
	  candidates.put(query, 0);
	  for(String possible: editDistanceOne(query, dict, true)){
	    if (isValid(possible,dict))
	      candidates.put(possible, 1);
//	    for(String two: editDistanceOne(possible,dict, false)){
//	      candidates.put(two, 2);
//	    }
	  }
		return candidates;
	}
	
	public Set<String> editDistanceOne(String query, Dictionary dict, boolean tolerate){
	  Set<String> possibles = new HashSet<String>();
	  String[] qwords = query.trim().split("\\s+");
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
        if(dict.count(qwords[i]+qwords[i+1])>0){
          possibles.add(front + qwords[i] + qwords[i+1] + back);
        }
        back = " " + qwords[i+1] + back;        
      }
      
      Set<String> words = editDistanceOneWords(qwords[i],dict);
      for(String word: words){
        possibles.add((front + word + back).trim());
      }
      if (!tolerate && dict.count(qwords[i])==0)
        break;
      front = front + qwords[i] + " ";
    }
	  
	  return possibles;
	}
	
	//decide if there are invalid words in a query
	public boolean isValid(String query, Dictionary dict){
	  String[] chars = query.trim().split("\\s+");
	  for (int i=0; i<chars.length; i++){
	    if (dict.count(chars[i])==0){
        return false;
	    }
	  }
	  return true;
	}
	
  //return strings of edit distance one, including splits, excluding merges
  public Set<String> editDistanceOneWords(String qword, Dictionary dict){
    // use rules or not: need rules to avoid stupid mistakes
    boolean rules = true;
    Character[] using = alphabet;
    
	  Set<String> possibles = new HashSet<String>();
	  Set<Pair<String,String>> splits = new HashSet<Pair<String,String>>();
	  // splits
	  for (int i=0; i<qword.length(); i++){
	    splits.add( new Pair<String,String>(qword.substring(0,i),qword.substring(i)));
	  }
	  for (Pair<String,String> split:splits){
	    if (dict.count(split.getFirst())!=0 && dict.count(split.getFirst())!=0)
	    possibles.add((split.getFirst()+" "+split.getSecond()).trim());
	  }
	  // deletes
    for (Pair<String,String> split:splits){
      if (rules && split.getSecond().length()>0){
        // never delete a single letter word
        if (rules && qword.length() == 1)
          continue;
        // never delete a number
        if (rules && Character.isDigit(split.getSecond().charAt(0)))
          continue;
        // never take out a trailing s
        if (rules && split.getSecond().length()==1 && split.getSecond().charAt(0)=='s')
          continue;
        if (dict.count(split.getFirst()+split.getSecond().substring(1))!=0)
          possibles.add(split.getFirst()+split.getSecond().substring(1));
      }
    }      
    // transposes
	  for (Pair<String,String> split:splits){
      if (split.getSecond().length()>1 && dict.count(split.getFirst()+split.getSecond().charAt(1)+split.getSecond().charAt(0)+split.getSecond().substring(2)) != 0)
        possibles.add(split.getFirst()+split.getSecond().charAt(1)+split.getSecond().charAt(0)+split.getSecond().substring(2));
    }
	  // replaces
	  for (Pair<String,String> split:splits){
      if (split.getSecond().length()>0){
        for (Character c : using){
          //never replace a number
          if (rules && Character.isDigit(split.getSecond().charAt(0)))
            continue;
          //never delete a single letter word
          if (rules && qword.length() == 1 && c == ' ')
            continue;
          //never delete a trailing s
          if (rules && split.getSecond().length()==1 && split.getSecond().charAt(0)=='s' && c ==' ')
            continue;
          if (c != ' ' && dict.count(split.getFirst()+ c +split.getSecond().substring(1))!=0)
            possibles.add(split.getFirst()+ c +split.getSecond().substring(1));
          if (c == ' ' && dict.count((split.getFirst()).trim()) != 0 && dict.count((split.getSecond().substring(1)).trim()) != 0)
            possibles.add((split.getFirst()+ c +split.getSecond().substring(1).trim())); 
        }
      }
    }
	  // inserts
	  for (Pair<String,String> split:splits){
      for (Character c: using){
        if (c != ' ' && dict.count(split.getFirst()+c+split.getSecond()) != 0)
          possibles.add(split.getFirst()+c+split.getSecond());
        if (c == ' ' && dict.count(split.getFirst()) != 0 && dict.count(split.getSecond()) != 0)
          possibles.add((split.getFirst()+c+split.getSecond()).trim());
      }
    }
	  return possibles;
	}
  /****************************/

}
