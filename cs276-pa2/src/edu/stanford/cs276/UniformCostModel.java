package edu.stanford.cs276;

import edu.stanford.cs276.util.Pair;

public class UniformCostModel implements EditCostModel {
	
  static double CORRECT_PROB = 0.95;
  static double EDIT_PROB = 0.03;

  @Override
	public double editProbability(String original, String R, Pair<String, Integer> pair) {
    int distance = pair.getSecond();
	  if (distance == 0){
	    return CORRECT_PROB;
	  }
	  return Math.pow(EDIT_PROB, distance);
	  }
}
