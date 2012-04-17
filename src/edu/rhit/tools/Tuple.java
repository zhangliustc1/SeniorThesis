package edu.rhit.tools;

public class Tuple implements Comparable<Tuple> {

	public String s1;
	public String s2;
	
	public Tuple(){
		
	}
	
	public Tuple(String a, String b){
		s1 = a;
		s2 = b;
	}

	@Override
	public String toString() {
		return "( " + s1 + ", " + s2 + " )";		
	}

	@Override
	public int compareTo(Tuple arg0) {
		Tuple other = (Tuple) arg0;
		String thiscstring = this.s1 + this.s2;
		return thiscstring.compareTo(other.s1 + other.s2);
	}
	
	
}
