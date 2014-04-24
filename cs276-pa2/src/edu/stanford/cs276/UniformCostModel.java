package edu.stanford.cs276;

public class UniformCostModel implements EditCostModel {
	
  static double CORRECT_PROB = 0.95;
  static double EDIT_PROB = 0.0005;

  @Override
	public double editProbability(String original, String R, int distance) {
		/*
		 * Your code here
		 */
	  /*******************************/
	  if (distance == 0){
	    return CORRECT_PROB;
	  }
	  return Math.pow(EDIT_PROB, distance);
	  /*******************************/
    }
}
