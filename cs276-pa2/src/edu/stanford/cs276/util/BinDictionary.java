package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.HashMap;

public class BinDictionary implements Serializable {

	private HashMap<Pair<String,String>, Integer> map;


	public BinDictionary() {
		map = new HashMap<Pair<String,String>, Integer>();
	}

	public void add(Pair<String,String> term) {

		if (map.containsKey(term)) {
			map.put(term, map.get(term) + 1);
		} else {
			map.put(term, 1);
		}
	}

	public int count(Pair<String,String> term) {

		if (map.containsKey(term)) {
			return map.get(term);
		} else {
			return 0;
		}
	}
}
