package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.model.StringSimilarityModel;
import edu.washington.cs.uei.util.GeneralUtility;

public class SimilarityMergePairClassifier 
	extends AbstractMergePairClassifier
	implements MergePairClassifier 
	{

	private static int    minSharedTuples               = 1;
	private static double objectSimilarityThreshold     = 0.6;
	private static double relationSimilarityThreshold   = 0.6;
	
	private GeneralUtility gu = new GeneralUtility();
	private HashMap<String, LinkedList<String []>> clusters;
	private String objectFile;
	
	
	public SimilarityMergePairClassifier(
			int minSharedTupleCount, 
			double similarityThreshold, 
			String objectFile)
    {
		super();
		minSharedTuples = minSharedTupleCount;
		objectSimilarityThreshold = similarityThreshold; 
		relationSimilarityThreshold = similarityThreshold;
		this.objectFile = objectFile;
		//clusters = gu.clusterHashMap(objectFile);
    }
	
	private StringSimilarityModel ssm = new StringSimilarityModel();
	
	private LinkedList<String []> getCluster(String clusterID) {
		if(clusters==null) {
			clusters = gu.clusterHashMap(this.objectFile);
		}
		if(!clusters.containsKey(clusterID)) {
			System.out.println("Error!:  couldn't find cluster for ID " + clusterID);
			return null;
		}
		return clusters.get(clusterID);
	}
	
	public double scoreMergePair(LinkedList tuples, int c1Index, int c2Index, boolean isEntity) 
	{
		// look at all tuples these two clusters share, and decide if it's worth merging them
		//double term1 = 0;
		
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
		
		LinkedList<String[]> cxStrs = getCluster(cxID);
		LinkedList<String[]> cyStrs = getCluster(cyID);

		double similarityScore = 0;
		double tempSim = 0;
		
		String [] cxStr;
		String [] cyStr;
		for(Iterator<String []> itXStrs=cxStrs.iterator(); itXStrs.hasNext(); ) 
		{
			cxStr = itXStrs.next();
			for(Iterator<String []> itYStrs=cyStrs.iterator(); itYStrs.hasNext(); )
			{
				cyStr = itYStrs.next();
				
				tempSim = ssm.getSimilarity(cxStr[0],cyStr[0], !isEntity);
				if(tempSim>similarityScore) {
					similarityScore = tempSim;
				}
			}
		}
		
		return similarityScore;
	}

	public boolean getClassification(double score, boolean isEntity) {
		if(isEntity) {
			return score >= objectSimilarityThreshold; 
		}
		return score >= relationSimilarityThreshold;
	}

	public static void main(String[] args) {
		BasicDiskTable in = new BasicDiskTable(new File(args[0]));

		String objectFile = args[1];
		
		BasicDiskTable out = new BasicDiskTable(new File(args[2]));
		
		double simThreshold = Double.parseDouble(args[3]);
		
		boolean append = false;
		if(args.length>4 && args[4]!=null && args[4].equals("-append")) {
			append = true;
		}
		
		SimilarityMergePairClassifier smpc = 
			new SimilarityMergePairClassifier(2, simThreshold, objectFile);
		
		smpc.emitMergePairs(in, out, append);
	}
}
