package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.model.StringSimilarityModel;
import edu.washington.cs.uei.util.GeneralUtility;

public class ProbabilisticSimilarityMPC 
	extends AbstractMergePairClassifier
	implements MergePairClassifier 
{
	private int    minSharedTuples             = 1;
	private double objectSimilarityThreshold   = 1.0;
	private double relationSimilarityThreshold = 1.0;
	
	private double simWeight                   = 18.0;
	private double simPriorDenom               = 2.0;
	
	private double priorMergeProbability       = 0.5;

	private StringSimilarityModel ssm = new StringSimilarityModel();
	private GeneralUtility gu = new GeneralUtility();
	
	public ProbabilisticSimilarityMPC(
			int minSharedTupleCount,
			double mergeThresh)
	{
		super();
		minSharedTuples = minSharedTupleCount;
		//priorMergeProbability = priorMergeProb;
		objectSimilarityThreshold = mergeThresh;
		relationSimilarityThreshold = mergeThresh;
	}
	
	
	private void getStringSets(
			LinkedList<String []> tuples,
			int c1Index,
			int c2Index,
			HashSet<String> s1s,
			HashSet<String> s2s)
	{
		for(Iterator<String []> it = tuples.iterator(); it.hasNext(); ) {
			String [] line = it.next();
			s1s.add(line[c1Index-1]);
			s2s.add(line[c2Index-1]);
		}
	}

	
	public double getProbabilityStringSimilarity(String s1, String s2, boolean isEntity)
	{
		int IDindex = s1.indexOf("+");
		if(IDindex>=0) {
			s1 = s1.substring(IDindex+1);
		}
		else {
			//System.err.println("string in ProbSSM has no + sign:  " + s1);
		}
		IDindex = s2.indexOf("+");
		if(IDindex>=0) {
			s2 = s2.substring(IDindex+1);
		}
		else {
			//System.err.println("string in ProbSSM has no + sign:  " + s2);
		}
		double sim = ssm.getSimilarity(s1, s2, !isEntity);
		return (simWeight * sim + 1) /
			   (simWeight + simPriorDenom);
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
		
		HashSet<String> string1s = new HashSet<String>();
		HashSet<String> string2s = new HashSet<String>();
		
		getStringSets(
				tuples,
				c1Index,
				c2Index,
				string1s, 
				string2s);
		
		double probRatio = 1;
		
		//boolean printProb = string1s.size()>1 || string2s.size()>1;
		boolean printProb = false;
		if(printProb) {
			System.out.println();
		}
			
		double [] scores = new double[string1s.size()*string2s.size()];
		int i=0;
		
		for(Iterator<String> it1 = string1s.iterator(); it1.hasNext(); ) {
			String s1 = it1.next();
			
			for(Iterator<String> it2 = string2s.iterator(); it2.hasNext(); ) {
				String s2 = it2.next();
				
				if(printProb) {
					System.out.println(s1 + " :::: " + s2);
				}
				
				double probSim = getProbabilityStringSimilarity(s1, s2, isEntity); 
				scores[i] = probSim / (1-probSim) * (1-priorMergeProbability) / priorMergeProbability;
				i++;
				//probRatio *= probSim / (1-probSim) * 
				//			 (1-priorMergeProbability) / priorMergeProbability;
			}
		}
		
		probRatio = gu.generalizedMean(scores, 1);
		
		if(printProb) {
			System.out.println(probRatio);
		}
		
		return probRatio;
	
	}
	

	public boolean getClassification(double score, boolean isEntity) {
		return (isEntity  && score>objectSimilarityThreshold) || 
			   (!isEntity && score>relationSimilarityThreshold);
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String workspace = args[1];

		String objectFile = args[2];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[3]));
		
		double mergeThreshold = Double.parseDouble(args[4]);
		
		boolean append = false;
		if(args.length>5 && args[5]!=null && args[5].equals("-append")) {
			append = true;
		}
		
		ProbabilisticSimilarityMPC psmpc = 
			new ProbabilisticSimilarityMPC(1, mergeThreshold);
		
		psmpc.emitMergePairs(in, out, append);
	}
}
