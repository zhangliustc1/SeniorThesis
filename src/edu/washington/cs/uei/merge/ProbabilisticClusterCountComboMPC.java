package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.util.GeneralUtility;

public class ProbabilisticClusterCountComboMPC 
	extends AbstractClusterMergePairClassifier
	implements MergePairClassifier 
{

	private static final int relArg = 4;
	private static final int arg2Arg = 8;

	private int    minSharedTuples               = 1;
	private double priorProbabilityOfMerge       = 0.5;
	private double mergeThreshold                = 1.0;

	//private ProbabilisticCooccurrenceMPC pcmpc = null;
	private UrnMergePairClassifier       umpc  = null;
	//private WeightedCooccurrenceMergePairClassifier wcmpc = null;
	//private WeightedUrnMergePairClassifier wumpc = null;
	private ProbabilisticSimilarityMPC   psmpc = null;
	
	private GeneralUtility gu = new GeneralUtility();
	
	//private HashMap<String, LinkedList<String[]>> clusterToStringMap;

	// local variable to store the number of shared properties between two clusters
	private int numSharedProps = 0;
	
	
	ProbabilisticClusterCountComboMPC(
			int minSharedTupleCount,
			double mergeThresh,
			/*
			double simThresh,
			double coocThresh,
			double simScale,
			double coocScale,*/
			String workspace,
			String objectFile)
	{
		super(objectFile);
		minSharedTuples               = minSharedTupleCount;
		mergeThreshold                = mergeThresh;
		//priorProbabilityOfMerge       = priorMergeProb;
		/*
		objectSimilarityThreshold     = simThresh;
		relationSimilarityThreshold   = simThresh;
		objectCooccurrenceThreshold   = coocThresh; 
		relationCooccurrenceThreshold = coocThresh;
		similaritySteepness           = simScale;
		cooccurrenceSteepness         = coocScale;
		*/
		
		//clusterToStringMap = gu.clusterHashMap(objectFile);
		
		umpc  = new UrnMergePairClassifier(mergeThresh, objectFile);
		//wcmpc = new WeightedCooccurrenceMergePairClassifier(priorMergeProb, workspace, objectFile);
		//wumpc = new WeightedUrnMergePairClassifier(priorMergeProb, workspace, objectFile);
		psmpc = new ProbabilisticSimilarityMPC(minSharedTuples, mergeThresh);
	}


	protected int getNumSharedProperties()
	{
		return numSharedProps;
	}
	
	
	private String getKey(String s1, String s2) {
		return s1 + " :::: " + s2;
	}
	
	public double scoreMergePair(LinkedList tuples, int c1Index, int c2Index,
			boolean isEntity) 
	{
		
		if(tuples==null||tuples.size()<minSharedTuples) {
			return 0;
		}
		
		String [] tuple = (String [])tuples.get(0);
		
		String c1 = tuple[c1Index];
		String c2 = tuple[c2Index];
		
		//LinkedList<String[]> s1s = clusterToStringMap.get(c1);
		//LinkedList<String[]> s2s = clusterToStringMap.get(c2);
		LinkedList<String[]> s1s = getCluster(c1);
		LinkedList<String[]> s2s = getCluster(c2);
		
		if(s1s==null) {
			s1s = getCluster(c1Index, tuple);
		}
		if(s2s==null) {
			s2s = getCluster(c2Index, tuple);
		}
		
		
		HashMap<String, LinkedList<String>> stringPairRelSets = 
			new HashMap<String, LinkedList<String>>();
		LinkedList<String> relSet;
		
		
		//HashMap<String, double []> stringPairCoocValues = new HashMap<String, double []>();
		HashMap<String, int []> stringPairCoocValues = new HashMap<String, int []>();
		String s1, s2, key;
		//double [] val;
		int [] val;
		
		HashSet<String> sharedProperties = new HashSet<String>();
		
		HashSet<String> strings1 = new HashSet<String>();
		HashSet<String> strings2 = new HashSet<String>();
		
		for(Iterator it = tuples.iterator(); it.hasNext(); ) {
			tuple = (String [])it.next();
			
			/*
			s1 = tuple[c1Index-1];
			s2 = tuple[c2Index-1];
			key = getKey(s1,s2);
			
			strings1.add(s1.substring(s1.indexOf("+")+1));
			strings2.add(s2.substring(s1.indexOf("+")+1));
			
			if(stringPairCoocValues.containsKey(key)) {
				val = stringPairCoocValues.get(key);
				//val[2] += 1 / wcmpc.inverseFunctionNess(tuple[relArg]);;
				val[2]++;
			}
			else {
				//val = new double[3];
				val = new int[3];
				val[0] = Integer.parseInt(tuple[c1Index+1]);
				val[1] = Integer.parseInt(tuple[c2Index+1]);
				//val[2] = 1 / wcmpc.inverseFunctionNess(tuple[relArg]);
				val[2] = 1;
				stringPairCoocValues.put(key, val);
			}
			
			if(stringPairRelSets.containsKey(key)) {
				relSet = stringPairRelSets.get(key);
				relSet.add(tuple[relArg]);
			}
			else {
				relSet = new LinkedList<String>();
				relSet.add(tuple[relArg]);
				stringPairRelSets.put(key, relSet);
			}
			*/
			
			sharedProperties.add(tuple[arg2Arg]);
		}
		
		/*
		boolean testCluster = false;
		String [] testStrings = {"Clinton", "Starr", "Bush", "George W. Bush", "George Bush", "Governor Bush", "Lewinsky", "Gore", "Griffey"};
		//String [] testStrings = {"Clinton"};
		for(int i=0; i<testStrings.length; i++) {
			if(strings1.contains(testStrings[i]) || strings2.contains(testStrings[i])) {
				testCluster = true;
				break;
			}
		}
		if(!testCluster) {
			return 0;
		}*/

		/*
		double [] simScores = new double[strings1.size()*strings2.size()];
		int index = 0;
		for(Iterator<String> it1 = strings1.iterator(); it1.hasNext(); ) {
			s1 = it1.next();
			
			for(Iterator<String> it2 = strings2.iterator(); it2.hasNext(); ) {
				s2 = it2.next();
				simScores[index] = psmpc.getProbabilityStringSimilarity(s1, s2, isEntity);
				simScores[index] = simScores[index] / (1 - simScores[index]);
				index++;
			}
		}*/
		
		double ret = 1;
		double probSim, probCooc;
		double priorProb = 0;
		//double [] priorProbs = new double[s1s.size()* s2s.size()];
		
		double [] scores = new double[s1s.size()*s2s.size()];
		double [] simScores = new double[s1s.size()*s2s.size()];
		int i = 0;
		String [] t1, t2;
		int n1, n2;
		int c1Cnt = 0;
		int c2Cnt = 0;
		
		for(Iterator<String[]> it1=s1s.iterator(); it1.hasNext(); ) {
			t1 = it1.next();
			s1 = t1[0];
			n1 = Integer.parseInt(t1[2]);
			c1Cnt += n1;
			
			for(Iterator<String []> it2=s2s.iterator(); it2.hasNext(); ) {
				t2 = it2.next();
				s2 = t2[0];
				n2 = Integer.parseInt(t2[2]);
				if(c1Cnt==n1) {
					c2Cnt += n2;
				}

				probSim = psmpc.getProbabilityStringSimilarity(s1, s2, isEntity);
				simScores[i] = probSim / (1-probSim);
				/*
				key = getKey(s1, s2);
				int sharedCount = 0;
				if(stringPairCoocValues.containsKey(key)) {
					val = stringPairCoocValues.get(key);
					sharedCount = val[2];
				}
				
				if(stringPairRelSets.containsKey(key)) {
					relSet = stringPairRelSets.get(key);
				}
				else {
					relSet = new LinkedList<String>();
				}
				
				probCooc = 
					umpc.getProbabilityStringCooccurrence(
							sharedCount, 
							n1, n2, 
							//relSet,
							isEntity);
				
				priorProb = umpc.getPriorProbability(n1, n2, isEntity);
				scores[i] = // probSim / (1-probSim) *
							probCooc / (1-probCooc) *
							(1-priorProb) / priorProb;
				*/
				i++;
			}
		}
		
		this.numSharedProps = sharedProperties.size();
		
		probCooc = 
			umpc.getProbabilityStringCooccurrence(
					this.numSharedProps, 
					c1Cnt, c2Cnt, 
					isEntity);
		
		priorProb = umpc.getPriorProbability(c1Cnt, c2Cnt, isEntity);
		
		//ret = gu.geometricMean(scores);
		ret = (probCooc / (1-probCooc) * (1-priorProb) / priorProb);
		ret *= gu.geometricMean(simScores);
		
		/*
		if(ret>1) {
			displayScores(scores, s1s, s2s, stringPairCoocValues, ret);
			System.out.println("Shared Properties (" + sharedProperties.size() + " out of " + c1Cnt + ", " + c2Cnt + "): ");
			for(Iterator<String> it = sharedProperties.iterator(); it.hasNext(); ) {
				System.out.println(it.next());
			}
			probCooc = 
				umpc.getProbabilityStringCooccurrence(
						sharedProperties.size(), 
						c1Cnt, c2Cnt, 
						isEntity);
			
			priorProb = umpc.getPriorProbability(c1Cnt, c2Cnt, isEntity);
			double score = (probCooc / (1-probCooc) * 
					(1-priorProb) / priorProb);
			System.out.println("Cluster cooccurrence odds = " + score);
			System.out.println("Cluster overall merge odds = " + 
					(score * gu.geometricMean(simScores)));
		}*/
		
		return ret;
	}
	
	private void displayScores(
			double [] coocScores, 
			LinkedList<String []> c1strings,
			LinkedList<String []> c2strings,
			HashMap<String, int []> coocValues,
			double overallScore)
	{
		String [] t1, t2;
		String s1 = null, s2 = null;
		String cluster1 = null;
		String cluster2 = null;
		int index = 0;
		
		for(Iterator<String []> it1 = c1strings.iterator(); it1.hasNext(); ) {
			t1 = it1.next();
			s1 = t1[0];
			cluster1 = t1[1];
			
			for(Iterator<String []> it2 = c2strings.iterator(); it2.hasNext(); ) {
				t2 = it2.next();
				s2 = t2[0];
				break;
				/*
				cluster2 = t2[1];
				displayScore(s1, s2, cluster1, cluster2, coocScores[index], 
						psmpc.getProbabilityStringSimilarity(s1, s2, true),
						coocValues.get(getKey(s1,s2)));
				index++;*/
			}
			break;
		}
		displayScore(s1, s2, cluster1, cluster2, gu.geometricMean(coocScores), overallScore, null);
	}
	
	private void displayScore(String s1, String s2, String cluster1, String cluster2, 
			double coocScore, double simScore, int [] nums) {
		if(nums==null) {
			System.out.println(cluster1 + " :::: " + cluster2 + " :::: " + s1 + " :::: " + s2 + " :::: " + coocScore + " :::: " + simScore);
		}
		else {
			System.out.println(cluster1 + " :::: " + cluster2 + " :::: " + s1 + " :::: " + s2 + " :::: " + coocScore + " :::: " + simScore + " :::: " + nums[0] + ", " + nums[1] + ", " + nums[2]);
		}
	}

	public boolean getClassification(double score, boolean isEntity) {
		return score > mergeThreshold;
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String workspace = args[1];
		
		String objectFile  = args[2];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[3]));
		
		double threshold = Double.parseDouble(args[4]);

		int entityPM = Integer.parseInt(args[5]);
		int relationPM = Integer.parseInt(args[6]);
		
		boolean append = false;
		if(args.length>7 && args[7]!=null && args[7].equals("-append")) {
			append = true;
		}
		
		ProbabilisticClusterCountComboMPC pccmpc = 
			new ProbabilisticClusterCountComboMPC(1, 
					threshold,
					workspace,
					objectFile);

		pccmpc.umpc.setEntityPropertyMultiple(entityPM);
		pccmpc.umpc.setRelationPropertyMultiple(relationPM);
	
		pccmpc.emitMergePairs(in, out, append);

	}

}
