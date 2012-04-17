package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.util.GeneralUtility;

public class CooccurrenceMergePairClassifier 
	extends	AbstractClusterMergePairClassifier 
	implements MergePairClassifier 
{
	
	//private static final double generalizedMeanPower = 0.5;

	private int    minSharedTuples               = 1;
	//private double objectCooccurrenceThreshold   = 0.012;
	//private double relationCooccurrenceThreshold = 0.001;
	private double cooccurrenceThreshold   = 1.0;
	//private double relationCooccurrenceThreshold = 1.0;
	//private double priorMergeProbability         = 0.5; 
	
	private GeneralUtility gu = new GeneralUtility();

	public CooccurrenceMergePairClassifier(
			int minSharedTupleCount,
			double threshold,
			String objectFile)
	{
		super(objectFile);
		minSharedTuples = minSharedTupleCount;
		cooccurrenceThreshold = threshold;
		//objectCooccurrenceThreshold = cooccurrenceThreshold; 
		//relationCooccurrenceThreshold = cooccurrenceThreshold;
	}
	

	public double getProbabilityStringCooccurrence(
			int cooc, 
			int n1, 
			int n2, 
			boolean isEntity) 
	{
		if(isEntity) {
			return (cooc) / (gu.geometricMean(n1, n2));
		}
		else {
			//return (cooc) / (gu.generalizedMean(n1, n2, generalizedMeanPower));
			return (cooc) / (gu.geometricMean(n1, n2));
		}
		
	}
	
	private String getKey(String s1, String s2) {
		return s1 + " :::: " + s2;
	}

	
	
	public double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity) 
	{
		int size = tuples.size();
		if(size<minSharedTuples) {
			return 0;
		}
		
		Iterator it = tuples.iterator();
		if(!it.hasNext()) {
			return 0;
		}
		
		String [] tuple = (String []) it.next();
		String cxID = tuple[c1Index];
		String cyID = tuple[c2Index];
		
		LinkedList<String[]> s1s = getCluster(cxID);
		LinkedList<String[]> s2s = getCluster(cyID);

		if(s1s==null) {
			s1s = getCluster(c1Index, tuple);
		}
		if(s2s==null) {
			s2s = getCluster(c2Index, tuple);
		}
		
		HashMap<String, int []> stringPairCoocValues = new HashMap<String, int []>();
		String s1, s2, key;
		int [] val;
		
		for(it = tuples.iterator(); it.hasNext(); ) {
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
		double [] scores = new double [s1s.size() * s2s.size()];
		int i=0;
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
							val[2], n1, n2, isEntity);
					if(val[0]!=n1 || val[1] != n2) {
						System.out.println("Error!:  cooc values don't match object file:  n1 = " + n1 + ", n2 = " + n2 + ", val[0] = " + val[0] + ", val[1] = " + val[1]);					
					}
				}
				else {
					probCooc = getProbabilityStringCooccurrence(0, n1, n2, isEntity);
				}
				
				scores[i] = probCooc;
				i++;
				//ret = ret * probCooc / (1-probCooc) *
				//			(1-priorMergeProbability) / priorMergeProbability;
			}
		}
		
		return gu.generalizedMean(scores, 1);
		//return ret;
	}
	

	public boolean getClassification(double score, boolean isEntity) {
		return score>cooccurrenceThreshold;
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String workspace  = args[1];
		
		String objectFile = args[2];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[3]));
		
		double threshold = Double.parseDouble(args[4]);
		
		boolean append = false;
		if(args.length>5 && args[5]!=null && args[5].equals("-append")) {
			append = true;
		}
		
		CooccurrenceMergePairClassifier cmpc = 
			new CooccurrenceMergePairClassifier(1, threshold, objectFile);
		
		cmpc.emitMergePairs(in, out, append);
	}
}
