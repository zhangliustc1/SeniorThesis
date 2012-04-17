package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;

public class ThresholdCombinationMergePairClassifier 
	extends	AbstractMergePairClassifier 
	implements MergePairClassifier 
{
	private int    minSharedTuples               = 2;
	private double objectCooccurrenceThreshold   = 0.000005;
	private double relationCooccurrenceThreshold = 0.000005;
	private double objectSimilarityThreshold     = 0.9;
	private double relationSimilarityThreshold   = 0.7;

	private CooccurrenceMergePairClassifier cmpc = null;
	private SimilarityMergePairClassifier   smpc = null;

	
	ThresholdCombinationMergePairClassifier(
			int minSharedTupleCount,
			double cooccurrenceThreshold,
			double simThreshold,
			String objectFile)
	{
		minSharedTuples = minSharedTupleCount;
		objectCooccurrenceThreshold = cooccurrenceThreshold; 
		relationCooccurrenceThreshold = cooccurrenceThreshold;
		objectSimilarityThreshold = simThreshold;
		relationSimilarityThreshold = simThreshold;
		
		cmpc = new CooccurrenceMergePairClassifier(minSharedTuples, cooccurrenceThreshold, objectFile);
		smpc = new SimilarityMergePairClassifier(minSharedTuples, simThreshold, objectFile);
	}

	
	public double scoreMergePair(LinkedList tuples, int c1Index, int c2Index,
			boolean isEntity) {
		return smpc.scoreMergePair(tuples, c1Index, c2Index, isEntity) +
			   cmpc.scoreMergePair(tuples, c1Index, c2Index, isEntity);
	}

	public boolean getClassification(double score, boolean isEntity) {
		return false;
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String objectFile = args[1];

		BasicDiskTable out = new BasicDiskTable(new File(args[2]));

		
		double coocThresh = Double.parseDouble(args[3]);
		double simThresh  = Double.parseDouble(args[4]);
		
		boolean append = false;
		if(args.length>5 && args[5]!=null && args[5].equals("-append")) {
			append = true;
		}
		
		ThresholdCombinationMergePairClassifier tcmpc = 
			new ThresholdCombinationMergePairClassifier(
					2, 
					coocThresh,
					simThresh,
					objectFile);
		
		tcmpc.emitMergePairs(in, out, append);

	}
}
