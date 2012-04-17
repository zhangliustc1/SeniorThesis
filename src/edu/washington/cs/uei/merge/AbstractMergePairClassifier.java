package edu.washington.cs.uei.merge;

import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.util.GeneralUtility;

public abstract class AbstractMergePairClassifier implements
		MergePairClassifier {

	
	private static final int compClusterIndex = 16;
	private static final int c2Index = 13;
	
	private GeneralUtility gu = new GeneralUtility();

	public int getCompClusterIndex() { return compClusterIndex;	}
	public int getC2Index() { return c2Index; }
	
	private void emitMergePair(
			String clusterX,
			String clusterY,
			String xCount,
			String yCount,
			double score,
			String xStr,
			String yStr,
			BasicDiskTable out) {
		
		String [] outline = {
			clusterX,
			clusterY,
			xCount,
			yCount,
			String.valueOf(score),
			xStr,
			yStr,
			String.valueOf(getNumSharedProperties())
		};
		
		out.println(outline);
	}
	
	protected int getNumSharedProperties()
	{
		return 0;
	}
	
	
	private void emitMergePair(
			String clusterX,
			String clusterY,
			String xCount,
			String yCount,
			double score,
			String xStr,
			String yStr,
			String allScoresStr,
			String allCountsStr,
			BasicDiskTable out) {
		
		String [] outline = {
			clusterX,
			clusterY,
			xCount,
			yCount,
			String.valueOf(score),
			xStr,
			yStr,
			allScoresStr,
			allCountsStr
		};
		
		out.println(outline);
	}

	
	public abstract double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity);
	
	public double scoreMergePair(
			LinkedList tuples,
			int c1Index,
			int c2Index,
			boolean isEntity,
			String [] [] string1s,
			String [] [] string2s,
			double [] [] scores)
	{
		return -1;
	}

	public abstract boolean getClassification(double score, boolean isEntity);

	private String [] getTuplesForClusters(
			String clusterID1,
			int c1Index,
			String clusterID2,
			int c2Index,
			BasicDiskTable tuples,
			LinkedList<String []> tupleList)
	{
		String [] tuple = tuples.readLine();

		while(tuple!=null && 
				clusterID1.equals(tuple[c1Index]) &&
				clusterID2.equals(tuple[c2Index])) {
			tupleList.add(tuple);
			tuple = tuples.readLine();
		}

		return tuple;
	}
	
	
	
	public void emitMergePairsFullInformation(
			BasicDiskTable in, 
			BasicDiskTable out,
			boolean append) {
		
		in.open();
		out.openForWriting(append);
		
		String [] tupleIn = in.readLine();
		int compareCluster = Integer.parseInt(tupleIn[compClusterIndex]);
		int c1Index = 4 * compareCluster + 1;
		boolean isEntity = c1Index==1;
		String clusterID1 = null;
		String clusterID2 = null;
		String c1Count = null;
		String c2Count = null;
		String c1Str = null;
		String c2Str = null;
		StringBuffer scoreStringBuffer = null;
		StringBuffer countStringBuffer = null;
		LinkedList<String []> tuplesForClusters = new LinkedList<String []>();
		
		int clusterPairCount = 0;
		
		while(tupleIn!=null) {
			clusterID1 = tupleIn[c1Index];
			clusterID2 = tupleIn[c2Index];
			c1Count    = tupleIn[c1Index+2];
			c2Count    = tupleIn[c2Index+2];
			c1Str      = tupleIn[c1Index-1];
			c2Str      = tupleIn[c2Index-1];
			tuplesForClusters.clear();
			tuplesForClusters.add(tupleIn);
			tupleIn = getTuplesForClusters(
					clusterID1, 
					c1Index, 
					clusterID2, 
					c2Index, 
					in, 
					tuplesForClusters);
			double [][] scores = new double[2][];
			String [][] s1s    = new String[1][];
			String [][] s2s    = new String[1][];
			
			double score = scoreMergePair(
					tuplesForClusters, 
					c1Index, c2Index, 
					isEntity,
					s1s,
					s2s,
					scores);
			c1Str = gu.join(s1s[0], "; ", 0, s1s[0].length-1);
			c2Str = gu.join(s2s[0], "; ", 0, s2s[0].length-1);
			scoreStringBuffer = new StringBuffer(String.valueOf(scores[0][0]));
			countStringBuffer = new StringBuffer(String.valueOf(scores[1][0]));
			for(int i=1; i<scores[0].length; i++) {
				scoreStringBuffer.append("; ");
				scoreStringBuffer.append(String.valueOf(scores[0][i]));
				countStringBuffer.append("; ");
				countStringBuffer.append(String.valueOf(scores[1][i]));
			}
			if(getClassification(score, isEntity)) {
				emitMergePair(
						clusterID1, 
						clusterID2, 
						c1Count,
						c2Count,
						score,
						c1Str,
						c2Str,
						scoreStringBuffer.toString(),
						countStringBuffer.toString(),
						out);
			}
			
			clusterPairCount++;
			if(clusterPairCount % 10000 == 0) {
				System.out.println("cluster pair count = " + clusterPairCount);
				System.out.flush();
			}
		}
		
		in.close();
		out.closeForWriting();
	}
	
	public void emitMergePairs(
			BasicDiskTable in, 
			BasicDiskTable out,
			boolean append) {
		
		in.open();
		out.openForWriting(append);
		
		String [] tupleIn = in.readLine();
		int compareCluster = Integer.parseInt(tupleIn[compClusterIndex]);
		int c1Index = 4 * compareCluster + 1;
		boolean isEntity = c1Index==1;
		String clusterID1 = null;
		String clusterID2 = null;
		String c1Count = null;
		String c2Count = null;
		String c1Str = null;
		String c2Str = null;
		LinkedList<String []> tuplesForClusters = new LinkedList<String []>();
		
		int clusterPairCount = 0;
		
		while(tupleIn!=null) {
			clusterID1 = tupleIn[c1Index];
			clusterID2 = tupleIn[c2Index];
			c1Count    = tupleIn[c1Index+2];
			c2Count    = tupleIn[c2Index+2];
			c1Str      = tupleIn[c1Index-1];
			c2Str      = tupleIn[c2Index-1];
			tuplesForClusters.clear();
			tuplesForClusters.add(tupleIn);
			tupleIn = getTuplesForClusters(
					clusterID1, 
					c1Index, 
					clusterID2, 
					c2Index, 
					in, 
					tuplesForClusters);
			
			double score = scoreMergePair(
					tuplesForClusters, 
					c1Index, c2Index, 
					isEntity);

			if(getClassification(score, isEntity)) {
				emitMergePair(
						clusterID1, 
						clusterID2, 
						c1Count,
						c2Count,
						score,
						c1Str,
						c2Str,
						out);
			}
			
			clusterPairCount++;
			if(clusterPairCount % 10000 == 0) {
				System.out.println("cluster pair count = " + clusterPairCount);
				System.out.flush();
			}
		}
		
		in.close();
		out.closeForWriting();
	}
}