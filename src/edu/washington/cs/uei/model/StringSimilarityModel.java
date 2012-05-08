package edu.washington.cs.uei.model;

/*
 * Provides a score in [0,1) for how similar a pair of strings are
 */

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.washington.cs.uei.util.GeneralUtility;

import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;
import uk.ac.shef.wit.simmetrics.similaritymetrics.NeedlemanWunch;
import uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWaterman;
import uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWatermanGotoh;

public class StringSimilarityModel {

	private final static Pattern _acroPattern_ = Pattern.compile("[A-Z.]*");
	
	private NeedlemanWunch nw = new NeedlemanWunch(0.5f);
	private SmithWaterman sw = null;
	private Levenshtein lev = new Levenshtein();
	private SmithWatermanGotoh swg = new SmithWatermanGotoh();
	private MongeElkan me = new MongeElkan();
	private GeneralUtility gu = new GeneralUtility();
	
	private static final double Beta = 2.0;
	
	
	public double getDistanceSim(String s, String t, boolean isEntity){
		if (isEntity){
			return this.me.getSimilarity(s, t);
		}
		else{
			return lev.getSimilarity(s, t);
		}
	}
	
	public double getRelationSimilarity(String s, String t) 
	{
		if(s.contains(" not ") || s.contains(" no ") || t.contains(" not ") || t.contains(" no ")) {
			return 0.01;
		}
		return nw.getSimilarity(s, t);
	}
	
	public double getSimilarity(String s, String t, boolean compRelations)
	{
		if(s.equalsIgnoreCase(t)) {
			return 0.99;
		}
		if(compRelations) {
			return getRelationSimilarity(s, t);
			//return getSimilarity(s, t);
		}
		else {
			return getTokenSequenceSimilarity(s, t);
		}
	}
	
	public double getSimilarity(String s, String t) 
	{
		//return getSubsequenceSimilarity(s, t);
		return getNormalizedSubsequenceSimilarity(s, t);
	}
	
	
	// the size of the longest subsequence between shorter and longer
	public int getLongestSubsequenceSize(String shorter, String longer) 
	{
		int [][] subSeqSize = new int[shorter.length()+1][longer.length()+1];
		
		for(int i=0; i<shorter.length()+1; i++) {
			subSeqSize[i][0] = 0;
		}
		for(int j=0; j<longer.length(); j++) {
			subSeqSize[0][j] = 0;
		}
		
		for(int i=1; i<shorter.length()+1; i++) {
			for(int j=1; j<longer.length()+1; j++) {
				int nextMax = subSeqSize[i-1][j];
				if(subSeqSize[i][j-1]>nextMax) {
					nextMax = subSeqSize[i][j-1];
				}
				
				int match =0;
				if(shorter.charAt(i-1)==longer.charAt(j-1)) {
					match = 1;
				}
				
				if(subSeqSize[i-1][j-1] + match > nextMax) {
					nextMax = subSeqSize[i-1][j-1] + 1;
				}
				
				subSeqSize[i][j] = nextMax;
			}
		}
		
		/*
		for(int i=0; i<shorter.length()+1; i++) {
			for(int j=0; j<longer.length()+1; j++) {
				System.out.print(subSeqSize[i][j]);
				System.out.print(" ");
			}
			System.out.println("");
		}
		*/
		
		
		return subSeqSize[shorter.length()][longer.length()];
	}
	
	
	// the size of the longest subsequence between shorter and longer
	// subtract penalties from length of subsequence for gaps
	public double getLongestNonconsecutivePenalizedSubsequenceSize(String shorter, String longer) 
	{
		final double nonconsecPenalty = 1.0;
		final double nonconsecAcrossWordsPenalty = 1.0;
		
		
		
		double [][] subSeqSize = new double[shorter.length()+1][longer.length()+1];
		
		for(int i=0; i<shorter.length()+1; i++) {
			subSeqSize[i][0] = 0;
		}
		for(int j=0; j<longer.length(); j++) {
			subSeqSize[0][j] = 0;
		}
		
		for(int i=1; i<shorter.length()+1; i++) {
			for(int j=1; j<longer.length()+1; j++) {
				double nextMax = subSeqSize[i-1][j];
				if(subSeqSize[i][j-1]>nextMax) {
					nextMax = subSeqSize[i][j-1];
				}
				
				double match =0;
				if(shorter.charAt(i-1)==longer.charAt(j-1)) {
					if(i-2<0 || j-2<0 || shorter.charAt(i-2)==longer.charAt(j-2)) {
						match = 1;
					}
					else {
						match = 1-nonconsecPenalty;
					}
				}
				
				if(subSeqSize[i-1][j-1] + match > nextMax) {
					nextMax = subSeqSize[i-1][j-1] + match;
				}
				
				subSeqSize[i][j] = nextMax;
			}
		}
		
		/*
		for(int i=0; i<shorter.length()+1; i++) {
			for(int j=0; j<longer.length()+1; j++) {
				System.out.print(subSeqSize[i][j]);
				System.out.print(" ");
			}
			System.out.println("");
		}
		*/
		
		
		return subSeqSize[shorter.length()][longer.length()];
	}
	
	public double getTokenSequenceSimilarity(String s, String t)
	{
		String [] splitS, splitT;
		boolean acronymS = isAcronym(s);
		if(acronymS) {
			splitS = splitAcronym(s);
		}
		else {
			splitS = s.split(" ");
		}
		
		boolean acronymT = isAcronym(t);
		if(acronymT) {
			splitT = splitAcronym(t);
		}
		else {
			splitT = t.split(" ");
		}
		
		double subseqSize = getPerTokenSubsequenceSize(splitS, splitT);
		
		double score = subseqSize / (subseqSize +
									 Beta * (splitS.length-subseqSize) + 
									 Beta * (splitT.length-subseqSize));
		
		return score;
	}
	
	public boolean isAcronym(String s) {
		return _acroPattern_.matcher(s).matches(); 
	}
	
	public String [] splitAcronym(String s) {
		LinkedList<String> ret = new LinkedList<String>();
		for(int i=0; i<s.length(); i++) {
			if(s.charAt(i)=='.') {
				continue;
			}
			ret.add("" + s.charAt(i));
		}
		String [] retArray = new String [ret.size()];
		int i=0;
		for(Iterator<String> it = ret.iterator(); it.hasNext(); i++) {
			retArray[i] = it.next();
		}
		return retArray;
	}
	
	public double getPerTokenSubsequenceSize(String [] s1, String [] s2) {
		double [][] D = new double [s1.length+1][s2.length+1];

		for(int i=0; i<s1.length+1; i++) {
			/*if(i<s1.length) {
				System.out.println("token " + i + ":  " + s1[i]);
			}*/
			D[i][0] = 0;
		}
		for(int j=0; j<s2.length+1; j++) {
			/*if(j<s2.length) {
				System.out.println("token " + j + ":  " + s2[j]);
			}*/
			D[0][j] = 0;
		}

		for(int i=1; i<s1.length+1; i++) {
			for(int j=1; j<s2.length+1; j++) {
				double nextMax = D[i-1][j];
				if(D[i][j-1]>nextMax) {
					nextMax = D[i][j-1];
				}
				
				//double match = getSubsequenceSimilarity(s1[i-1],s2[j-1]);
				double match = s1[i-1].equalsIgnoreCase(s2[j-1]) ? 1.0 : 0;
				
				if(D[i-1][j-1] + match > nextMax) {
					nextMax = D[i-1][j-1] + match;
				}
				
				D[i][j] = nextMax;
			}
		}
		
		/*
		for(int i=0; i<s1.length+1; i++) {
			for(int j=0; j<s2.length+1; j++) {
				System.out.print(D[i][j]);
				System.out.print(" ");
			}
			System.out.println("");
		}*/
		
		return D[s1.length][s2.length];
	}
	
	
	public double getPerWordSubsequenceSimilarity(String s, String t) 
	{
		String [] splitS, splitT;
		int shorterSize;
		boolean acronymS = isAcronym(s);
		if(acronymS) {
			splitS = splitAcronym(s);
		}
		else {
			splitS = s.split(" ");
		}
		
		boolean acronymT = isAcronym(t);
		if(acronymT) {
			splitT = splitAcronym(t);
		}
		else {
			splitT = t.split(" ");
		}
		
		shorterSize = splitS.length;
		if(splitT.length<shorterSize && !acronymS) {
			shorterSize = splitT.length;
		}
		else if(splitT.length>shorterSize && acronymT) {
			shorterSize = splitT.length;
		}
		
		return getPerTokenSubsequenceSize(splitS, splitT) / shorterSize;
	}
	
	// the size of the longest subsequence that s and t have in common, 
	// divided by the length of the smaller one
	public double getSubsequenceSimilarity(String s, String t) 
	{
		
		String shorter = s.replaceAll("\\.", "");
		String longer = t.replaceAll("\\.", "");
		if(shorter.length()>longer.length()) {
			shorter = longer;
			longer = s.replaceAll("\\.", "");
		}
		
		int subSeqSize = getLongestSubsequenceSize(shorter, longer);
		//double subSeqSize = getLongestNonconsecutivePenalizedSubsequenceSize(shorter, longer);
		
		return (subSeqSize+1) / (subSeqSize + Beta * ((double) shorter.length()-subSeqSize) + 2);
	}
	
	
	public double getAcronymSize(String s, String t) {
		if(t==null) {
			return 0;
		}
		String [] words = t.split(" ");
		if(words==null || words.length<=0) {
			return 0;
		}
		
		StringBuffer initials = new StringBuffer("");
		for(int i=0; i<words.length; i++) {
			if(words[i]!=null && words[i].length()>0) {
				initials.append(words[i].charAt(0));
			}
		}
		
		String in = initials.toString();
		if(in.length()>=s.length()) {
			return getLongestNonconsecutivePenalizedSubsequenceSize(s, in);
		}
		else {
			return getLongestNonconsecutivePenalizedSubsequenceSize(in, s);
		}
	}
	
	
	public double getNormalizedSubsequenceSimilarity(String s, String t) {
		// Remove dots
		String shorter = s.replaceAll("\\.", "");
		String longer = t.replaceAll("\\.", "");
		if(shorter.length()>longer.length()) {
			shorter = longer;
			longer = s.replaceAll("\\.", "");
		}
		
		// Get the longest subsequence shared between them
		//int subSeqSize = getLongestSubsequenceSize(shorter, longer);
		double subSeqSize = getLongestNonconsecutivePenalizedSubsequenceSize(shorter, longer);
		
		//double normalization = gu.geometricMean(shorter.length(), longer.length());
		double normalization = shorter.length();
		if(isAcronym(shorter) && isAcronym(longer)) {
			normalization = longer.length();
		}
		
		//normalization += (shorter.length() - subSeqSize) * (Beta-1);
		//normalization += (normalization - subSeqSize) * (Beta-1);
		//normalization += Math.pow((normalization - subSeqSize)+0.415, Beta) +
		normalization += Math.pow(Beta, normalization-subSeqSize + 0.1) +
						 ((subSeqSize<normalization) ? normalization : 0);
		
		double score = (subSeqSize + 1) / (normalization + 2);
		
		if(isAcronym(shorter) && shorter.length()>1 && !isAcronym(longer)) {
			String [] words = longer.split(" ");
			if(words==null || words.length<=0) {
				return 0;
			}
			
			StringBuffer initials = new StringBuffer("");
			for(int i=0; i<words.length; i++) {
				if(words[i]!=null && words[i].length()>0) {
					char init = words[i].charAt(0);
					if('A'<=init && init<='Z') {
						initials.append(init);
					}
				}
			}
			
			if(initials.length()<=0) {
				return score;
			}
			
			double acroScore = getNormalizedSubsequenceSimilarity(shorter, initials.toString());
			if(acroScore > score) {
				return acroScore;
			}
		}
		/*
		else {
			normalization = gu.geometricMean(shorter.length(), longer.length());
		}*/
		
		return score;
	}
	
	
	public static void main(String [] args) 
	{
		NeedlemanWunch nw = new NeedlemanWunch(0.5f);
		SmithWatermanGotoh swg = new SmithWatermanGotoh();
		MongeElkan me = new MongeElkan();
		StringSimilarityModel ssm = new StringSimilarityModel();
		
		String [] s1 = {"U.S.", "Amnesty International", "Microsoft Corp.", "Microsoft", "State", "Jesus", "United States", "Britain", "World Bank", "North Carolina", "BAA", "XML-RPC", "West Virginia", "Environmental Protection Agency"};
		String [] s2 = {"US", "Center", "Microsoft", "Mac", "United States", "Messiah", "U.S.", "Spain", "World Trade Organization", "South Carolina", "Almanac", "DOM", "Virginia", "EPA"};
		
		for(int i=0; i<s1.length && i<s2.length; i++) {
			System.out.println(s1[i] + ", " + s2[i]);
			System.out.println(nw.getSimilarity(s1[i],s2[i]));
			System.out.println(swg.getSimilarity(s1[i], s2[i]));
			System.out.println(me.getSimilarity(s1[i], s2[i]));
			System.out.println(ssm.getSubsequenceSimilarity(s1[i], s2[i]));
			System.out.println(ssm.getNormalizedSubsequenceSimilarity(s1[i], s2[i]));
			System.out.println(ssm.getDistanceSim(s1[i], s2[i], false));
			//System.out.println(ssm.getPerWordSubsequenceSimilarity(s1[i],s2[i]));
			System.out.println();
		}
		
	}

}
