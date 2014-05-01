package edu.stanford.cs276.util;

import java.io.Serializable;

public class Triple<A, B, C> implements Serializable{
    private A first;
    private B second;
    private C third;

    public Triple(A first, B second, C third) {
    	super();
    	this.first = first;
    	this.second = second;
    	this.third = third;
    }

    @Override
	public int hashCode() {
    	int hashFirst = first != null ? first.hashCode() : 0;
    	int hashSecond = second != null ? second.hashCode() : 0;
      int hashThird = third != null ? third.hashCode() : 0;

    	return ((hashFirst + hashSecond + hashThird) * hashThird + hashSecond) * hashSecond + hashFirst;
    }

    @Override
	public boolean equals(Object other) {
    	if (other instanceof Triple) {
    		Triple otherTri = (Triple) other;
    		return 
    		((  this.first == otherTri.first ||
          ( this.first != null && otherTri.first != null &&
            this.first.equals(otherTri.first))) &&
            (  this.third == otherTri.third ||
            ( this.third != null && otherTri.third != null &&
              this.third.equals(otherTri.third))) &&
    		 (	this.second == otherTri.second ||
    			( this.second != null && otherTri.second != null &&
    			  this.second.equals(otherTri.second))) );
    	}

    	return false;
    }

    @Override
	public String toString()
    { 
           return "(" + first + ", " + second +", " + third + ")"; 
    }

    public A getFirst() {
    	return first;
    }

    public void setFirst(A first) {
    	this.first = first;
    }

    public B getSecond() {
    	return second;
    }

    public void setSecond(B second) {
    	this.second = second;
    }

    public C getThird() {
      return third;
    }

    public void setThird(C third) {
      this.third = third;
    }


}