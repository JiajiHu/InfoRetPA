package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TriDictionary implements Serializable {

	private int termCount;
  private int behindTermCount;
	private HashMap<String, Triple<Integer,Integer,Integer>> map;

	public int termCount() {
		return termCount;
	}

	public int behindTermCount() {
    return behindTermCount;
  }

	public TriDictionary() {
		termCount = 0;
	  behindTermCount = 0;
		map = new HashMap<String, Triple<Integer,Integer,Integer>>();
	}

	public void addFirst(String term) {

		termCount++;
		if (map.containsKey(term)) {
			map.put(term, new Triple<Integer,Integer,Integer>(map.get(term).getFirst() + 1,map.get(term).getSecond(),map.get(term).getThird()));
		} else {
			map.put(term, new Triple<Integer,Integer,Integer>(1,0,0));
		}
	}
  public void addSecond(String term) {

    behindTermCount++;
    if (map.containsKey(term)) {
      map.put(term, new Triple<Integer,Integer,Integer>(map.get(term).getFirst(),map.get(term).getSecond() + 1,map.get(term).getThird()));
    } else {
      map.put(term, new Triple<Integer,Integer,Integer>(0,1,0));
    }
  }

	 public void addThird(String term) {

	    if (map.containsKey(term)) {
	      map.put(term, new Triple<Integer,Integer,Integer>(map.get(term).getFirst(),map.get(term).getSecond(),map.get(term).getThird()+1));
	    } else {
	      map.put(term, new Triple<Integer,Integer,Integer>(0,0,1));
	    }
	  }

	public int countFirst(String term) {

		if (map.containsKey(term)) {
			return map.get(term).getFirst();
		} else {
			return 0;
		}
	}

	public int countSecond(String term) {

    if (map.containsKey(term)) {
      return map.get(term).getSecond();
    } else {
      return 0;
    }
  }
	
	public int countThird(String term) {

    if (map.containsKey(term)) {
      return map.get(term).getThird();
    } else {
      return 0;
    }
  }
	
}
