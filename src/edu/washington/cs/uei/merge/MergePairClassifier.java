package edu.washington.cs.uei.merge;

import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;

public interface MergePairClassifier {
	
	/* Input tuples have form:
	 * 21 items
	 * str_x0
	 * cluster_id0
	 * str_count_0
	 * cluster_count_0
	 * str_x1
	 * cluster_id1
	 * str_count_1
	 * cluster_count_1
	 * str_x2
	 * cluster_id2
	 * str_count_2
	 * cluster_count_2
	 * 
	 * str_y
	 * cluster_id_y
	 * str_count_y
	 * cluster_count_y
	 * cluster_compare_arg (0, 1, 2)
	 * 
	 * cluster_tuple_count_x
	 * cluster_tuple_count_y
	 * likelihood_mass(cluster_id_x)
	 * likelihood_mass(cluster_id_y)
	 * 
	 * Output merge table has columns:
	 * 5 required items, 2 other items
	 * cluster1
	 * cluster2
	 * cluster1 count
	 * cluster2 count
	 * merge score
	 * cluster1 string rep
	 * cluster2 string rep
	 * 
	 */
	
	public double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity);
	
	public boolean getClassification(double score, boolean isEntity);
	
	public void emitMergePairs(BasicDiskTable in, BasicDiskTable out, boolean append); 
		
}
