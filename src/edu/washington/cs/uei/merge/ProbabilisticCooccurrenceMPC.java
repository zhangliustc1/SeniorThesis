package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.util.GeneralUtility;
import edu.washington.cs.uei.util.HashCounter;

public class ProbabilisticCooccurrenceMPC 
	extends AbstractMergePairClassifier
	implements MergePairClassifier 
{

	private int    minSharedTuples               = 2;
	//private double objectCooccurrenceThreshold   = 0.012;
	//private double relationCooccurrenceThreshold = 0.001;
	private double objectCooccurrenceThreshold   = 1.0;
	private double relationCooccurrenceThreshold = 1.0;
	
	private double priorMergeProbability = 0.01;
	
	private GeneralUtility gu = new GeneralUtility();
	
	private HashMap<String, LinkedList<String[]>> clusterToStringMap;

	
	public ProbabilisticCooccurrenceMPC(
			int minSharedTupleCount,
			double priorMergeProb,
			String objectFile)
	{
		super();
		minSharedTuples = minSharedTupleCount;
		//objectCooccurrenceThreshold = cooccurrenceThreshold; 
		//relationCooccurrenceThreshold = cooccurrenceThreshold;
		priorMergeProbability = priorMergeProb;
		
		clusterToStringMap = gu.clusterHashMap(objectFile);
	}
	
	
	private void getStringAndStringPairCounts(
			LinkedList<String []> tuples,
			int c1Index,
			int c2Index,
			HashMap<String, Integer> s1Counts,
			HashMap<String, Integer> s2Counts,
			HashCounter sPairCounts)
	{
		String s1, s2;
		for(Iterator<String []> it = tuples.iterator(); it.hasNext(); ) {
			String [] line = it.next();
			s1 = line[c1Index-1];
			s2 = line[c2Index-1];
			
			if(!s1Counts.containsKey(s1)) {
				s1Counts.put(s1, new Integer(Integer.parseInt(line[c1Index+1])));
			}
			if(!s2Counts.containsKey(s2)) {
				s2Counts.put(s2, new Integer(Integer.parseInt(line[c2Index+1])));
			}
			sPairCounts.add(getKey(s1,s2));
		}
	}
	

	private String getKey(String s1, String s2) {
		return s1 + " :::: " + s2;
	}
	
	
	public double getProbabilityStringCooccurrence(
			int sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{
		int possibleP = Math.min(s1Count, s2Count) - sharedCount;
		double posteriorWeight = 5 * possibleP;
		
		if(isEntity) {
			return (posteriorWeight * sharedCount + 1) / 
					(posteriorWeight * gu.geometricMean(s1Count, s2Count) + possibleP);
		}
		else {
			return (posteriorWeight * sharedCount + 1) / 
				    (posteriorWeight * gu.generalizedMean(s1Count, s2Count, 0.5) + possibleP); 
		}
	}
	
	public double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity) 
	{
		if(tuples==null||tuples.size()<minSharedTuples) {
			return 0;
		}
		
		String [] tuple = (String [])tuples.get(0);
		
		String c1 = tuple[c1Index];
		String c2 = tuple[c2Index];
		
		LinkedList<String[]> s1s = clusterToStringMap.get(c1);
		LinkedList<String[]> s2s = clusterToStringMap.get(c2);
		
		HashMap<String, int []> stringPairCoocValues = new HashMap<String, int []>();
		String s1, s2, key;
		int [] val;
		
		for(Iterator it = tuples.iterator(); it.hasNext(); ) {
			tuple = (String [])it.next();
			
			s1 = tuple[c1Index-1];
			s2 = tuple[c2Index-1];
			key = getKey(s1,s2);
			
			if(stringPairCoocValues.containsKey(key)) {
				val = stringPairCoocValues.get(key);
				val[2]++;
			}
			else {
				val = new int[3];
				val[0] = Integer.parseInt(tuple[c1Index+1]);
				val[1] = Integer.parseInt(tuple[c2Index+1]);
				val[2] = 1;
				stringPairCoocValues.put(key, val);
			}
			
		}

		double ret = 1;
		double probCooc;
		String [] t1, t2;
		int n1, n2;
		
		for(Iterator<String[]> it1=s1s.iterator(); it1.hasNext(); ) {
			t1 = it1.next();
			s1 = t1[0];
			n1 = Integer.parseInt(t1[2]);
			
			for(Iterator<String []> it2=s2s.iterator(); it2.hasNext(); ) {
				t2 = it2.next();
				s2 = t2[0];
				n2 = Integer.parseInt(t2[2]);
				
				key = getKey(s1, s2);
				
				if(stringPairCoocValues.containsKey(key)) {
					val = stringPairCoocValues.get(key);
					probCooc = getProbabilityStringCooccurrence(
							val[2], val[0], val[1], isEntity);
					if(val[0]!=n1 || val[1] != n2) {
						System.out.println("Error!:  cooc values don't match object file:  n1 = " + n1 + ", n2 = " + n2 + ", val[0] = " + val[0] + ", val[1] = " + val[1]);					
					}
				}
				else {
					probCooc = getProbabilityStringCooccurrence(0, n1, n2, isEntity);
				}
				
				ret = ret * probCooc / (1-probCooc) *
							(1-priorMergeProbability) / priorMergeProbability;
			}
		}
		
		return ret;
		
		/*
		int size = tuples.size();
		if(size<minSharedTuples) {
			return 0;
		}
		
		Iterator it = tuples.iterator();
		if(!it.hasNext()) {
			return 0;
		}
		
		HashMap<String, Integer> string1Freqs = new HashMap<String, Integer>();
		HashMap<String, Integer> string2Freqs = new HashMap<String, Integer>();
		HashCounter stringPairCoocs = new HashCounter();
		
		getStringAndStringPairCounts(
				tuples,
				c1Index,
				c2Index,
				string1Freqs, 
				string2Freqs, 
				stringPairCoocs);
		
		double probRatio = 1;
		
		LinkedList<String []>c1Strings = clusterToStringMap.get(c1);
		LinkedList<String []>c2Strings = clusterToStringMap.get(c2);
		
		for(Iterator<String> it1 = string1Freqs.keySet().iterator(); it1.hasNext(); ) {
			String s1 = it1.next();
			Integer count1 = string1Freqs.get(s1);
			
			for(Iterator<String> it2 = string2Freqs.keySet().iterator(); it2.hasNext(); ) {
				String s2 = it2.next();
				Integer count2 = string2Freqs.get(s2);
				
				int sharedCount = stringPairCoocs.getCount(getKey(s1,s2));
				
				double probCooc = getProbabilityStringCooccurrence(
						sharedCount, 
						count1.intValue(),
						count2.intValue(),
						isEntity);
				
				probRatio *= probCooc / (1-probCooc) * 
							 priorMergeProbability / (1-priorMergeProbability);
			}
		}
		
		return probRatio;
		*/
	}
	

	public boolean getClassification(double score, boolean isEntity) {
		return (isEntity  && score>objectCooccurrenceThreshold) || 
			   (!isEntity && score>relationCooccurrenceThreshold);
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String objectFile = args[1];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[2]));
		
		double priorMergeProb = Double.parseDouble(args[3]);
		
		boolean append = false;
		if(args.length>4 && args[4]!=null && args[4].equals("-append")) {
			append = true;
		}
		
		ProbabilisticCooccurrenceMPC pcmpc = 
			new ProbabilisticCooccurrenceMPC(2, priorMergeProb, objectFile);
		
		pcmpc.emitMergePairs(in, out, append);
	}
}
