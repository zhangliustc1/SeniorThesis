package edu.washington.cs.uei.merge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.util.GeneralUtility;

public abstract class AbstractClusterMergePairClassifier extends
		AbstractMergePairClassifier implements MergePairClassifier {

	protected int minSharedTuples = 1;
	private double objectThreshold = 1.0;
	private double relationThreshold = 1.0;
	
	protected GeneralUtility gu = new GeneralUtility();
	private String objectFile;
	private HashMap<String, LinkedList<String []>> clusterToStringMap;
	
	public AbstractClusterMergePairClassifier(String objectFile)
	{
		super();
		this.objectFile = objectFile;
	}
	
	
	protected LinkedList<String []> getCluster(String clusterID) {
		if(clusterToStringMap==null) {
			clusterToStringMap = gu.clusterHashMap(this.objectFile);
		}
		if(!clusterToStringMap.containsKey(clusterID)) {
			//System.out.println("Error!:  Couldn't find cluster for ID " + clusterID);
			return null;
		}
		return clusterToStringMap.get(clusterID);
	}
	
	protected LinkedList<String []> getCluster(int clusterIndex, String [] compTuple) {
		String [] obj = new String[4];
		obj[0] = compTuple[clusterIndex-1];
		obj[1] = compTuple[clusterIndex];
		obj[2] = compTuple[clusterIndex+1];
		obj[3] = compTuple[clusterIndex+2];
		
		LinkedList<String []> ret = new LinkedList<String[]>();
		ret.add(obj);
		return ret;
	}
	
	
	public abstract double scoreMergePair(LinkedList tuples, int c1Index, int c2Index,
			boolean isEntity);


	public boolean getClassification(double score, boolean isEntity) {
		return ((isEntity && score>objectThreshold) ||
				(!isEntity && score>relationThreshold));
	}
}
