package edu.rhit.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.language.RefinedSoundex;

import edu.rhit.cs.cluster.ESPModel;
import edu.rhit.cs.cluster.WordNetSim;
import edu.washington.cs.uei.disktable.BasicDiskTable;
import edu.washington.cs.uei.model.StringSimilarityModel;
import edu.washington.cs.uei.scoring.LinearPrecisionRecallCalculator;
import edu.washington.cs.uei.util.GeneralUtility;

public class MyResolver {

	

        public static String workdir = "/home/stephen/Documents/Classes/Fall2011/NLP/resolver-export/MyReverbData/";
    //public static String workdir = "/Users/stu2009/mayhewsw/SeniorThesis/";

	// This variable decides on the data set. It is necessary
	// because there are various changes to the code if it 
	// is set
	private static boolean artificial = false;
	
	public static String infile = artificial ? workdir + "justgoldArtificial.txt" : workdir + "justgold.txt";

	public static String objhypfile = workdir + "hyp_obj_new_clusters7.txt";
	public static String relhypfile = workdir + "hyp_rel_new_clusters7.txt";

	public static String objgoldfile = artificial ? workdir + "object_scoring_clusters_artificial.txt" : workdir + "yates_gold_objects.txt";
	public static String relgoldfile = artificial ? workdir + "relation_scoring_clusters_artificial.txt" : workdir + "yates_gold_relations.txt";

	public WordNetSim wns;
	
	public static String sep = " :::: ";

	public static int Max = 50;
	
	private HashSet<String> Objects;
	private HashSet<String> Relations;

	public String method;
	public boolean Soundex;
	
	public boolean mutrec = true;

	private int numMerges = 4;
	private boolean stopwordRemove = false;
	
	// This should be a number between 0 and 1
	// public static double threshold = 0.3;

	public MyResolver(String method, boolean Soundex) {
		this.method = method;
		this.Soundex = Soundex;
	}

	public ArrayList<String[]> getData(String clusterFilename) {
		BasicDiskTable clusters = new BasicDiskTable(new File(clusterFilename));
		clusters.open();

		String[] line = clusters.readLine();
		ArrayList<String[]> lines = new ArrayList<String[]>();

		while (line != null) {
			lines.add(line);
			line = clusters.readLine();
		}

		return lines;
	}

	/*
	 * This will return all pairs given a list of id's. For example: in: {3, 5,
	 * 8} out: {(3, 5), (3, 8), (5, 8)} except each integer in each pair is the
	 * associated string as decided by the elements in Cluster
	 */
	private ArrayList<Tuple> getPairs(ArrayList<Integer> propList,
			TreeMap<Integer, ArrayList<String>> Elements) {
		ArrayList<Tuple> allPairs = new ArrayList<Tuple>();
		for (int i = 0; i < propList.size(); i++) {
			for (int j = i + 1; j < propList.size(); j++) {

				// String a = getKeyFromValue(Cluster, propList.get(i));
				// String b = getKeyFromValue(Cluster, propList.get(j));

				// NOTE NOTE NOTE: use the first string in the array for
				// comparisons.
				// Is this wise? I'm not sure
				String a = Elements.get(propList.get(i)).get(0);
				String b = Elements.get(propList.get(j)).get(0);
				// System.out.println(a + ", " + aa);
				// System.out.println(b + ", " + bb);

				allPairs.add(new Tuple(a, b));
			}

		}
		return allPairs;
	}

	// This creates a data structure of the form:
	// [ s:{prop of s, prop of s, prop of s}, ... ]
	public HashMap<String, HashSet<String>> getPropApply(ArrayList<String[]> E) {
		
		HashMap<String, HashSet<String>> props = new HashMap<String, HashSet<String>>();

		for (String[] extraction : E) {
			for (int i = 0; i < 3; i++) {

				String [] cleanExtraction = new String[3];
				cleanExtraction[0] = cleanText(extraction[0]);
				cleanExtraction[1] = cleanText(extraction[1]);
				cleanExtraction[2] = cleanText(extraction[2]);
				
				HashSet<String> s;
				if(props.containsKey(cleanExtraction[i])){
					s = props.get(cleanExtraction[i]);
				}else{
					s = new HashSet<String>();
				}
				String propString = getPropstring(i, cleanExtraction);
				s.add(propString);
				props.put(cleanExtraction[i], s); // is this step necessary? not sure

			}
		}
	
		return props;
	}
	
	private String getPropstring(int i, String [] extraction){
		if(i == 0){
			return extraction[1] + " :::: " + extraction[2];
		}else if(i == 1){
			return extraction[0] + " :::: " + extraction[2];
		}else if(i == 2){
			return extraction[0] + " :::: " + extraction[1];
		}else{
			System.out.println("Error: bad value for i in getPropstring");
			return "ERROR";
			
		}
	}

	// This is copied almost exactly out of the paper. pp269.
	public int ClusterAlgorithm(double threshold) {
		
		// Set up wordnetsim
		wns = new WordNetSim();
		
		// E is all assertions. Array? Arraylist? of String [] ? Only need to
		// iterate. No insert or delete
		ArrayList<String[]> E = getData(infile);

		// S is each unique relation or object. Hashset<String>?
		HashSet<String> S = new HashSet<String>();

		this.Objects = new HashSet<String>();
		this.Relations = new HashSet<String>();

		HashMap<String, String> cleanToDirty = new HashMap<String, String>();
		
		// Iterate over E, and put each element into S. Hashset will maintain
		// uniqueness.
		for (String[] extraction : E) {
			if (extraction.length < 3) {
				System.out.println("Non conforming line. Starts with: "
						+ extraction[0]);
				continue;
			}
			
			// Clean text.
			String c0 = cleanText(extraction[0]);
			String c1 = cleanText(extraction[1]);
			String c2 = "";
			if(! artificial){
				c2 = cleanText(extraction[2]);
			}
			
			cleanToDirty.put(c0, extraction[0]);
			cleanToDirty.put(c1, extraction[1]);
			if(! artificial){
				cleanToDirty.put(c2, extraction[2]);
			}

			// Each extraction is of the form (obj, rel, obj)
			S.add(c0);
			S.add(c1);
			if(! artificial){
				S.add(c2);
			}


			// So we have a record of what is what.
			Objects.add(c0);
			if(! artificial){
				Objects.add(c2);
			}

			Relations.add(c1);

		}

		TreeMap<String, Integer> Cluster = new TreeMap<String, Integer>();
		TreeMap<Integer, ArrayList<String>> Elements = new TreeMap<Integer, ArrayList<String>>();
		
		// This is the initial clusterID
		int clusterid = 100;

		// 1. For each s in S: (I hate Java. What a stupid way to use lists.)
		System.out.println("Step 1");
		for (String s : S) {
			// System.out.println(s + ", " + clusterid);
			Cluster.put(s, clusterid);
			clusterid++;
			ArrayList<String> elList = new ArrayList<String>();
			elList.add(s);
			Elements.put(Cluster.get(s), elList);
		}

		// Steps 2 - 4
//		HashMap<Tuple, Double> Scores = calculateScores(Cluster, E, Elements);
		TreeMap<Tuple, Double> Scores = calculateScores(Cluster, E, Elements);

		// This number is consistent finally!
		System.out.println(Scores.size());

		// 5. Repeat until no merges can be performed.
		System.out.print("\nStep 5");
		boolean moreMerges = true;
		int mergeiter = 1;
		while (moreMerges) {
			System.out.println("\nRunning merge iteration " + mergeiter);
			mergeiter++;
			// Sort scores. I wonder if this is not accurate.
			ArrayList<Tuple> sortedScores = sortByValueArray(Scores);

			HashSet<Integer> usedclusters = new HashSet<Integer>();

//			 for (int y = 0; y < 10; y++){
//				 System.out.println(sortedScores.get(y) + ", " +
//						 	Scores.get(sortedScores.get(y)));
//			 }

			Tuple firstTup = sortedScores.get(0);
			double currScore;
			int count = 0;
			
			int cutoff = 0;
			
			for (Tuple currTup : sortedScores) {

				// Get top pair (currTup)
				currScore = Scores.get(currTup);

				// Condition for NOT merging
				if (currScore < threshold) {
					// This means that no merges have happened
					// and no merges will happen.
					if (firstTup == currTup) {
						moreMerges = false;
					}
					break;
				}

				// Now do merging of two strings in currTup.
				String c1String = currTup.s1;
				String c2String = currTup.s2;

				int c1 = Cluster.get(c1String);
				int c2 = Cluster.get(c2String);

				// if not in usedclusters...
				if (!usedclusters.contains(c1) && !usedclusters.contains(c2)) {
					// Merging c1 and c2 (both cluster IDs)
					
					// update elements
					ArrayList<String> c1elems = Elements.get(c1); // reference here...
					ArrayList<String> c2elems = Elements.get(c2);

					c1elems.addAll(c2elems);
					
					// Replacing, not adding
					Elements.put(c1, c1elems); // may not be necessary, but nice to be explicit

					// Update clusters (loop)
					for (String s : c2elems) {
						// This is replacing, not adding
						Cluster.put(s, c1);
					}

					// delete c2 from Elements
					Elements.remove(c2);

					// Update used clusters
					usedclusters.add(c1);
					usedclusters.add(c2);
					
					// If mutual recursion...
					if (this.mutrec){
						// Merge properties containing c1 and c2
						// There should be fewer properties after this step
						
					}
				}
				count++;
			}
			
			// Repeat steps 2 through 4 to recalculate scores.
			// Even if all the sizes are the same, scores sizes is different
			Scores = calculateScores(Cluster, E, Elements);
			
			System.out.println("\nSizes: " + Cluster.size() + ", " + E.size() + ", " + Elements.size());
			// FIXME: This is not consistent between runs.
			System.out.println("Scores: " + Scores.size());
			
			if (Scores.size() == 0 || mergeiter == this.numMerges  + 1) {
				moreMerges = false;
			}

		}

		HashSet<Cluster> objclusters = new HashSet<Cluster>();
		HashSet<Cluster> relclusters = new HashSet<Cluster>();

		for (Entry<Integer, ArrayList<String>> e : Elements.entrySet()) {
			Cluster c = new Cluster();
			for (String s : e.getValue()) {
				// Here is where we would access the dictionary
				String s_dirty = cleanToDirty.get(s);
				c.addStringNoConvert(s_dirty);
			}

			String repr_string = e.getValue().get(0);

			if (Objects.contains(repr_string)) {
				objclusters.add(c);
			} else {
				relclusters.add(c);
			}
		}

		produceOutput(objclusters, objhypfile);
		produceOutput(relclusters, relhypfile);

		return 0;
	}

	private String cleanText(String s) {
		// remove spaces, and punctuation
		// set to lower
		
		// this is just simple replacement stuff.
		s = s.replaceAll("[^A-Za-z \\d]", "");
		s = s.toLowerCase();
		s = s.trim();
		
		String newS = s;
		// more potent stopword removal etc.
		if (this.stopwordRemove){
			newS = GeneralUtility.join(WordNetSim.parseKeywords(s), " ");
		}	
		
		return newS;
	}

	/*
	 * This is steps 2 - 4
	 */
	public TreeMap<Tuple, Double> calculateScores(
			TreeMap<String, Integer> Cluster, ArrayList<String[]> E,
			TreeMap<Integer, ArrayList<String>> Elements) {
		// This accepts Cluster and element. It changes neither of them.
		// Returns scores
		// 2. Scores is a list, (HashMap?)
		System.out.println("Step 2");
		TreeMap<Tuple, Double> Scores = new TreeMap<Tuple, Double>();

		// Index is a HashMap
		TreeMap<String, ArrayList<Integer>> Index = new TreeMap<String, ArrayList<Integer>>();

		// For tracking progress
		float elen = E.size();
		int prog = 1;
		int lastPercent = 0;
		int step = 2;

		// 3. For each extraction in E:
		System.out.println("Step 3 "
				+ "===========================================");
		for (String[] extraction : E) {
			// To allow for blank lines. Flexibility is nice...
			if (extraction.length < 3) {
				continue;
			}

			int percent = (int) ((prog / elen) * 100);
			if (percent - lastPercent >= step) {
				System.out.print("=");
				lastPercent = percent;
			}
			prog++;

			// For this section, we just need all combinations of the first two
			// integers passed in.

			String [] cleanExtraction = new String[3];
			cleanExtraction[0] = cleanText(extraction[0]);
			cleanExtraction[1] = cleanText(extraction[1]);
			cleanExtraction[2] = cleanText(extraction[2]);
			
//			Runtime r = Runtime.getRuntime();
//			System.out.println(r.totalMemory());
			
			// Part 1
			putIndexProperty(Index, Cluster, cleanExtraction, 1, 2, 0);

			// Part 2
			if (! artificial){
				putIndexProperty(Index, Cluster, cleanExtraction, 0, 1, 2);
			}
				
			// Part 3
			putIndexProperty(Index, Cluster, cleanExtraction, 0, 2, 1);
		}
		
		System.out.println("\nGetting propcounts...");
		HashMap<String, HashSet<String>> propCounts = getPropApply(E);

		int count = 0;
		float iline = Index.entrySet().size();
		lastPercent = 0;

		// 4. For each property p in Index
		System.out.println("\nStep 4 " + "===========================================");
		// This loop is too slow
		for (Map.Entry<String, ArrayList<Integer>> entry : Index.entrySet()) {
			// String property = entry.getKey();

			// This is a list of clusterIDs
			ArrayList<Integer> propList = entry.getValue();
			Collections.sort(propList);

			// For tracking progress
			int percent = (int) ((count / iline) * 100);
			if (percent - lastPercent >= step) {
				System.out.print("=");
				lastPercent = percent;
			}
			count++;

			if (propList.size() < Max) {
				ArrayList<Tuple> allPairs = getPairs(propList, Elements);
				// For each pair in propList
				// System.out.println(allPairs.size());

				// Get the strings in that pair, get similarity
				// Store it in Scores
				for (Tuple t : allPairs) {
					double getScore = similarity(t, propCounts);
					Scores.put(t, getScore);
				}
			}else{
				// Entries with more than Max properties go here.
				//System.out.println("Gone over max: " + entry + ", " + propList.size());
			}
		}

		return Scores;
	}

	/*
	 * This is the meat of step 3. a and b are the indices that will become the
	 * property. For example, if the extraction is: (mars, lacks, ozone layer)
	 * and a = 1, b = 2, then the property would be (lacks, ozone layer).
	 */
	private void putIndexProperty(TreeMap<String, ArrayList<Integer>> Index,
			TreeMap<String, Integer> Cluster, String[] extraction, int a,
			int b, int c) {
		TreeSet<Integer> val1 = new TreeSet<Integer>();
		String property1 = extraction[a] + sep + extraction[b];
		
		// Add Soundex stuff here.
		if (this.Soundex){
			RefinedSoundex rs = new RefinedSoundex();
			property1 = rs.encode(property1); // convert prop 1 to a soundex string
		}
		
		val1.add(Cluster.get(extraction[c]));

		// Also add whatever was already in Index[property]
		ArrayList<Integer> ip = Index.get(property1);
		if (ip != null) {
			val1.addAll(ip);
		}

		Index.put(property1, new ArrayList<Integer>(val1));
	}

	
	public int produceOutput(HashSet<Cluster> clusters, String outfile) {
		// This will write the lines to a file.
		try {
			PrintWriter out = new PrintWriter(new FileWriter(outfile));

			for (Iterator<Cluster> it = clusters.iterator(); it.hasNext();) {
				Cluster t = it.next();
				String string_clust = t.toString();
				out.println(string_clust);
			}

			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	// This was taken from
	// http://stackoverflow.com/questions/7965132/java-sort-hashmap-by-value
	// With some tweaking.
	public static Map<Tuple, Double> sortByValue(HashMap<Tuple, Double> map) {
		List<Map.Entry<Tuple, Double>> list = new LinkedList<Map.Entry<Tuple, Double>>(
				map.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<Tuple, Double>>() {

			public int compare(Map.Entry<Tuple, Double> m1,
					Map.Entry<Tuple, Double> m2) {
				boolean ascending = true;
				if (ascending) {
					return (m1.getValue()).compareTo(m2.getValue());
				} else {
					return (m2.getValue()).compareTo(m1.getValue());
				}
			}
		});

		Map<Tuple, Double> result = new LinkedHashMap<Tuple, Double>();
		for (Map.Entry<Tuple, Double> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/*
	 * This takes a HashMap<Tuple, Float> and sorts it (descending) and returns
	 * an arraylist which has all the keys (Tuples) in the correct sorted order.
	 */
	public static ArrayList<Tuple> sortByValueArray(TreeMap<Tuple, Double> map) {
		List<Map.Entry<Tuple, Double>> list = new LinkedList<Map.Entry<Tuple, Double>>(
				map.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<Tuple, Double>>() {

			public int compare(Map.Entry<Tuple, Double> m1,
					Map.Entry<Tuple, Double> m2) {
				boolean ascending = false;
				if (ascending) {
					return (m1.getValue()).compareTo(m2.getValue());
				} else {
					return (m2.getValue()).compareTo(m1.getValue());
				}
			}
		});

		ArrayList<Tuple> result = new ArrayList<Tuple>();
		for (Map.Entry<Tuple, Double> entry : list) {
			result.add(entry.getKey());
		}
		return result;
	}

	private double similarity(Tuple t, HashMap<String, HashSet<String>> propCounts) {
		// SSM
		//int d = SimpleSSM.LevenshteinDistance(t.s1, t.s2);
		StringSimilarityModel ssm = new StringSimilarityModel();
		double sim2 = ssm.getSimilarity(t.s1, t.s2);

		// ESP
		HashSet<String> props1 = new HashSet<String>();	
		props1.addAll(propCounts.get(t.s1));
		HashSet<String> props2 = new HashSet<String>();
		props2.addAll(propCounts.get(t.s2));
				
		int n1 = props1.size();
		int n2 = props2.size();
		if (n1 < 0 || n2 < 0){
			System.out.println("Problem!");
		}
					
		props1.retainAll(props2); // note that props1 may not be used after this.
		int k = props1.size();  //intersection of props1 and props2
		
		ESPModel esp = new ESPModel();
		boolean isEntity = true;
		
		// Either contains s1 or s2. One should be enough though.
		if (this.Relations.contains(t.s1)){
			isEntity = false;
		}
		
		double prob = esp.getProbabilityStringCooccurrenceNormalized(k, n1, n2, isEntity);
		
		if(this.method == "ssm"){
			// From yates code
			return sim2;
		}else if(this.method  == "esp"){
			return prob;	
		}else if(this.method == "comb"){
			
			double prior = 0.5;
			double num =  prob * sim2 * (1 - prior);
			double denom = num + (1-prob)*(1-sim2)*(prior);
			return num / denom;
			
		}else if (this.method == "max"){
			
			return Math.max(sim2,  prob);
		}else if(this.method == "wn"){
			// Wordnet similarity only works for relations. 
			if (isEntity){
				return sim2;
			}else{
				return this.wns.similarity(t.s1, t.s2);
			}
		}else{
		
			System.out.println("Warning: running default scorer!");
			return 0.0;
		}
		
		
	}

	public static void runMyResolver(double thresh, String method, boolean Soundex){
		System.out.println("===== With Threshold: " + thresh + " ========");
		System.out.print("Using method: " + method);
		MyResolver m = new MyResolver(method, Soundex);
		if(m.Soundex){
			System.out.print(" + Soundex");
		}
		
		if(m.stopwordRemove){
			System.out.print(" + Stopword removal");
		}else{
			System.out.println();
		}
		
		// This produces two files: objhypfile, relhypfile
		m.ClusterAlgorithm(thresh);

		System.out.println("Done clustering");

		System.out.println("Calculating score...");
		// Run test
		System.out.println("\n==== Object Scores ====");
		LinearPrecisionRecallCalculator prc = new LinearPrecisionRecallCalculator();
		prc.printResults(objgoldfile, objhypfile);

		System.out.println("\n==== Relation Scores ====");
		prc = new LinearPrecisionRecallCalculator();

		prc.printResults(relgoldfile, relhypfile);
	}
	
	

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		boolean soundex = false;

		
		double t = 0.5;
		runMyResolver(t, "wn", soundex);
//		runMyResolver(t+0.1, "ssm", soundex);
		
//		for (double thresh = 0.1; thresh < 1; thresh += 0.1) {
//			runMyResolver(thresh, "wn", soundex);
//		}

	}

}