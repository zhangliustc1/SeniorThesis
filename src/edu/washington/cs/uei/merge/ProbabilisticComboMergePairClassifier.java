package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;


public class ProbabilisticComboMergePairClassifier 
	extends	AbstractMergePairClassifier 
	implements MergePairClassifier 
{

	private int    minSharedTuples               = 2;
	private double objectCooccurrenceThreshold   = 0.005;
	private double relationCooccurrenceThreshold = 0.0002;
	private double objectSimilarityThreshold     = 0.9;
	private double relationSimilarityThreshold   = 0.7;
	private double priorProbabilityOfMerge       = 0.5;
	private double similaritySteepness           = 20;
	private double cooccurrenceSteepness         = 200;
	private double mergeThreshold                = 0.5;

	private CooccurrenceMergePairClassifier cmpc = null;
	private SimilarityMergePairClassifier   smpc = null;
	
	
	ProbabilisticComboMergePairClassifier(
			int minSharedTupleCount,
			double mergeThresh,
			double simThresh,
			double coocThresh,
			double simScale,
			double coocScale,
			String objectFile)
	{
		super();
		minSharedTuples               = minSharedTupleCount;
		mergeThreshold                = mergeThresh;
		objectSimilarityThreshold     = simThresh;
		relationSimilarityThreshold   = simThresh;
		objectCooccurrenceThreshold   = coocThresh; 
		relationCooccurrenceThreshold = coocThresh;
		similaritySteepness           = simScale;
		cooccurrenceSteepness         = coocScale;
		
		cmpc = new CooccurrenceMergePairClassifier(minSharedTuples, coocThresh, objectFile);
		smpc = new SimilarityMergePairClassifier(minSharedTuples, simThresh, objectFile);
	}


	private double probGivenSimilarity(double sim, boolean isObject)
	{
		/*
		double simThreshold = objectSimilarityThreshold;
		if(!isObject) {
			simThreshold = relationSimilarityThreshold;
		}
		return 1 / ((double) 1.0 + Math.pow(2, -similaritySteepness * (sim-simThreshold)));
		*/
		
		return sim;
	}
	
	private double probGivenCooccurrence(double cooc, boolean isObject) 
	{
		/*
		double coocThreshold = objectCooccurrenceThreshold;
		if(!isObject) {
			coocThreshold = relationCooccurrenceThreshold;
		}
		return 1 / ((double) 1.0 + Math.pow(2, -cooccurrenceSteepness * (cooc-coocThreshold)));
		*/
		return cooc;
	}
	
	private double probGivenSimAndCooc(double sim, double cooc, boolean isObject)
	{
		double ps = probGivenSimilarity(sim, isObject);
		double pc = probGivenCooccurrence(cooc, isObject);
		double px = priorProbabilityOfMerge;
		
		return ps * pc * (1-px) / 
		((1-px) * ps * pc + px * (1-ps) * (1-pc));
	}

	
	public double scoreMergePair(LinkedList tuples, int c1Index, int c2Index,
			boolean isEntity) {
		double sim  = smpc.scoreMergePair(tuples, c1Index, c2Index, isEntity);
		double cooc = cmpc.scoreMergePair(tuples, c1Index, c2Index, isEntity);
		return probGivenSimAndCooc(sim, cooc, isEntity);
	}

	public boolean getClassification(double score, boolean isEntity) {
		return score >= mergeThreshold;
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String objectFile  = args[1];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[2]));
		
		
		double mergeThresh = Double.parseDouble(args[3]);
		double simThresh   = Double.parseDouble(args[4]);
		double coocThresh  = Double.parseDouble(args[5]);
		double simScale    = Double.parseDouble(args[6]);
		double coocScale   = Double.parseDouble(args[7]);
		
		boolean append = false;
		if(args.length>8 && args[8]!=null && args[8].equals("-append")) {
			append = true;
		}
		
		ProbabilisticComboMergePairClassifier pcmpc = 
			new ProbabilisticComboMergePairClassifier(2, 
					mergeThresh,
					simThresh,
					coocThresh,
					simScale,
					coocScale,
					objectFile);
	
		pcmpc.emitMergePairs(in, out, append);

	}
}
