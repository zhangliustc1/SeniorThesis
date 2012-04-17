package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;

public class PropertyWeightedUrnMergePairClassifier extends
		AbstractClusterMergePairClassifier {

	private static final int propertyInDegreeIndex = 17;
	
	private int minSharedTuples = 1;
	private int entityPropertyMultiple = 30;
	private int relationPropertyMultiple = 500;
	private double threshold = 1.0;
	
	private String objectFile;
	
	public PropertyWeightedUrnMergePairClassifier(double mergeThreshold, String objectFile)
	{
		super(objectFile);
		threshold = mergeThreshold;
		this.objectFile = objectFile;
	}


	public void setEntityPropertyMultiple(int entityPM)
	{
		entityPropertyMultiple = entityPM;
		System.out.println("Entity Property Multiple set to " + entityPropertyMultiple);
	}

	public void setRelationPropertyMultiple(int relationPM)
	{
		relationPropertyMultiple = relationPM;
		System.out.println("Relation Property Multiple set to " + relationPropertyMultiple);
	}
	
	
	private long choose(int n, int k) {
		
		if(n<0||k>n||k<0) {
			return 0;
		}
		if(n==0||n==k) {
			return 1;
		}
		long ret = 1;
		int min = Math.min(k, n-k);
		for(int i=1; i<=min; i++) {
			ret*=n-i+1;
			ret/=i;
		}
		
		return ret;
	}
	
	private double logChoose(int n, int k)
	{
		double ret = 0;
		
		if(k>n || k<0) {
			System.out.println("Error:  choosing too many or negative number from a set:" + n + ", " + k);
			return 0;
		}
		
		int min = Math.min(k, n-k);
		
		for(int i=1; i<=min; i++) {
			ret += Math.log(n-i+1);
			ret -= Math.log(min-i+1);
		}
		
		//System.out.println("log choose(" + n + ", " + k + ") = " + ret);
		
		return ret;
	}
	
	private double logFactorial(int n) {
		double ret = 0;
		for(int i=1; i<=n; i++) {
			ret += Math.log(i);
		}
		return ret;
	}
	
	private double stirlingLogFactorial(int n) {
		return 0.5 * Math.log(Math.PI * (2*n +1/3)) + n * (Math.log(n)-1);
	}

	private double stirlingLogFactorial(double n) {
		return 0.5 * Math.log(Math.PI * (2*n +1/3)) + n * (Math.log(n)-1);
	}

	
	private double stirlingLogChoose(int n, int k) 
	{
		final int approxBoundary = 50;
		
		if(k>n||k<0) {
			System.out.println("Error:  choosing too many or negative number from a set:" + n + ", " + k);
			return 0;
		}
		
		if(k==0 || k==n) {
			return 0;
		}
		
		if(k==1 || k==n-1) {
			return Math.log(n);
		}
		
		int min = Math.min(k, n-k);
		int max = Math.max(k, n-k);
		
		double ret = 0;
		if(n<approxBoundary) {
			return logChoose(n,k);
		}
		ret = stirlingLogFactorial(n);
		if(max<approxBoundary) {
			return ret - logFactorial(max) - logFactorial(min);
		}
		ret -= stirlingLogFactorial(max);
		if(min<approxBoundary) {
			return ret - logFactorial(min);
		}
		return ret - stirlingLogFactorial(min);
		
	}
	
	private double stirlingLogChoose(double n, double k) {
		if(k>n||k<0) {
			System.out.println("Error:  choosing too many or negative number from a set:" + n + ", " + k);
			return 0;
		}
		
		if(k==0 || k==n) {
			return 0;
		}
		
		return stirlingLogFactorial(n) - stirlingLogFactorial(k) - stirlingLogFactorial(n-k);
	}
	

	
	double urnLikelihood(int k, int n1, int n2, int P1, int P2, int P12)
	{
		int minP = Math.min(P1,P2);

		if(P12>minP) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12>min(P1,P2)");
			return 0;
		}
		if(P12<k) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12<k");
			return 0;
		}
		
		int rstart = Math.max(0,P12-P2+n2-k);
		int rend   = Math.min(n2-k, Math.min(P12-k, P1-n1));
		
		double ret = 0;
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2,n2);
		
		System.out.println("logCh_P12_k = " + logCh_P12_k);
		System.out.println("denom = " + denom);
		

		for(int r=rstart; r<=rend; r++) {
		
			logprob = logCh_P12_k +
					  stirlingLogChoose(P12-k, r) +
					  stirlingLogChoose(P2-P12,n2-k-r) +
					  stirlingLogChoose(P1-k-r, n1-k) -
					  denom;
				
			System.out.println();
			System.out.println("" + r);
			System.out.println("choose(P12-k, r) = " + stirlingLogChoose(P12-k, r));
			System.out.println("choose(P2-P12, n2-k-r) = " + stirlingLogChoose(P2-P12, n2-k-r));
			System.out.println("choose(P1-k-r, n1-k) = " + stirlingLogChoose(P1-k-r, n1-k));
			System.out.println("logProb = " + logprob);
			ret += Math.exp(logprob);
		}
		
		//System.out.println(ret);
		
		//ret += Math.exp(logprob);
		
		return ret;
	}
	
	
	// assume n1>=n2, P1>=2*n1
	// returns logLikelihood that urns 1 and 2 share all potential properties (P12 = min(P1, P2) = P2)
	double urnLogLikelihoodAllSameProperties(int k, int n1, int n2, int P1, int P2)
	{
		int P12 = Math.min(P1,P2);
		assert P12 == P2;

		if(P12<k) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12<k");
			return 0;
		}
		
		int r = n2-k;
		
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2,n2);
		
		logprob = logCh_P12_k +
				  stirlingLogChoose(P12-k, r) +
				  stirlingLogChoose(P2-P12,n2-k-r) +
				  stirlingLogChoose(P1-k-r, n1-k) -
				  denom;
		
		return logprob;
	}
	
	// assume n1>=n2, P1>=2*n1
	// returns logLikelihood that urns 1 and 2 share all potential properties (P12 = min(P1, P2) = P2)
	double urnLogLikelihoodAllSameProperties(double k, int n1, int n2, int P1, int P2)
	{
		int P12 = Math.min(P1,P2);
		assert P12 == P2;

		if(P12<k) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12<k");
			return 0;
		}
		
		double r = n2-k;
		
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2,n2);
		
		logprob = logCh_P12_k +
				  stirlingLogChoose(P12-k, r) +
				  stirlingLogChoose(P2-P12,n2-k-r) +
				  stirlingLogChoose(P1-k-r, n1-k) -
				  denom;
		
		return logprob;
	}
	
	double urnLikelihood(double k, int n1, int n2, int P1, int P2, int P12)
	{
		int minP = Math.min(P1,P2);

		if(P12>minP) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12>min(P1,P2)");
			return 0;
		}
		if(P12<k) {
			System.err.println("ERROR:  Trying to find Urn Likelihood, but P12<k");
			return 0;
		}
		
		int rstart = (int)Math.max(0,P12-P2+n2-k);
		int rend   = (int)Math.min(n2-k, Math.min(P12-k, P1-n1));
		
		double ret = 0;
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2,n2);
 
		

		for(int r=rstart; r<=rend; r++) {
		
			logprob = logCh_P12_k +
					  stirlingLogChoose(P12-k, r) +
					  stirlingLogChoose(P2-P12,n2-k-r) +
					  stirlingLogChoose(P1-k-r, n1-k) -
					  denom;
				
			ret += Math.exp(logprob);
		}
		
		//System.out.println(ret);
		
		//ret += Math.exp(logprob);
		
		return ret;
	}
	

	// assume P2<= P1
	double urnLikelihoodAllP12(int k, int n1, int n2, int P1, int P2) {
		double ret = 0;
		double logProb = 0;
		
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2, n2);
		double numer = stirlingLogChoose(P2+1,n2+1);
		double addend = numer - denom;
		
		//System.out.println("denom = " + denom);
		
		for(int r=k; r<=Math.min(n2, P1-n1+k); r++) {
			logProb = stirlingLogChoose(r,k) + stirlingLogChoose(P1-r, n1-k);
			/*
			System.out.println(r);
			System.out.println("choose(r,k) = " + stirlingLogChoose(r,k));
			System.out.println("choose(P1-r, n1-k) = " + stirlingLogChoose(P1-r, n1-k));
			System.out.println("logProb = " + (logProb+addend));
			System.out.println("prob = " + Math.exp(logProb+addend));
			*/
			ret += Math.exp(logProb+addend);
		}
		
		//return ret / ((double)(Math.min(P1,P2)+1));
		return ret;
	}
	
	
	// assume P2<= P1
	double urnLikelihoodAllP12(double k, int n1, int n2, int P1, int P2) {
		double ret = 0;
		double logProb = 0;
		
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2, n2);
		double numer = stirlingLogChoose(P2+1,n2+1);
		double addend = numer - denom;
		
		//System.out.println("denom = " + denom);
		//System.out.println("numer = " + numer);

		for(int r=(int)k+1; r<=Math.min(n2, P1-n1+k); r++) {
			logProb = stirlingLogChoose(r,k) + stirlingLogChoose(P1-r, n1-k);
			//System.out.println("prob = " + Math.exp(logProb+addend));
			ret += Math.exp(logProb+addend);
		}
		
		//System.exit(1);
		
		//return ret / ((double)(Math.min(P1,P2)+1));
		return ret;
	}
	
	
	double urnLikelihood(int k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		double ret = 0;
		
		for(int P12 = minP12; P12<=maxP12; P12++) {
			ret += urnLikelihood(k,n1,n2,P1,P2,P12);
		}
	
		return ret;
	}
	
	
	double urnLikelihood(double k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		double ret = 0;
		
		for(int P12 = minP12; P12<=maxP12; P12++) {
			ret += urnLikelihood(k,n1,n2,P1,P2,P12);
		}
		
		return ret;
	}
	

	private String getKey(String s1, String s2) {
		return s1 + " :::: " + s2;
	}
	
	private int getPotentialPropertyCount(int strCount, boolean isEntity)
	{
		//return (int)(Math.pow(strCount, 1.3));
		//return (int)(3*strCount * Math.log(strCount));
		int propMult = isEntity ? entityPropertyMultiple : relationPropertyMultiple;
		return propMult * strCount;
	}
	
	public double getPriorProbability(int s1Count, int s2Count, boolean isEntity) {
		
		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);
		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		int maxP12 = Math.min(P1, P2);
		//return priorMergeProbability;
		
		return 1.0 / ((double)maxP12+1);
		
		//return 1.0 / ((double) numProperties);
	}
	
	// to avoid underflow errors, combine urnLikelihood calculations
	private double urnProbabilityAllSameProperties(int k, int n1, int n2, int P1, int P2)
	{
		double logLikelihoodAllSameProperties = urnLogLikelihoodAllSameProperties(k, n1, n2, P1, P2);
		
		double ret = 0;
		double logProb = 0;

		// use trick to avoid underflow:
		// exp(x) / sum_i exp(y_i) = 1 / sum_i [exp(y_i) / exp(x)] = 1 / sum_i exp(y_i-x) 
		
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2, n2);
		double numer = stirlingLogChoose(P2+1,n2+1);
		double addend = numer - denom;
		
		for(int r=k; r<=Math.min(n2, P1-n1+k); r++) {
			logProb = stirlingLogChoose(r,k) + stirlingLogChoose(P1-r, n1-k);
			ret += Math.exp(logProb+addend-logLikelihoodAllSameProperties);
		}
		
		return 1.0 / ret;
		
	}
	
	// to avoid underflow errors, combine urnLikelihood calculations
	private double urnProbabilityAllSameProperties(double k, int n1, int n2, int P1, int P2)
	{
		double logLikelihoodAllSameProperties = urnLogLikelihoodAllSameProperties(k, n1, n2, P1, P2);
		
		double ret = 0;
		double logProb = 0;

		// use trick to avoid underflow:
		// exp(x) / sum_i exp(y_i) = 1 / sum_i [exp(y_i) / exp(x)] = 1 / sum_i exp(y_i-x) 
		
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2, n2);
		double numer = stirlingLogChoose(P2+1,n2+1);
		double addend = numer - denom;
		
		for(double r=k; r<=Math.min(n2, P1-n1+k); r++) {
			logProb = stirlingLogChoose(r,k) + stirlingLogChoose(P1-r, n1-k);
			ret += Math.exp(logProb+addend-logLikelihoodAllSameProperties);
		}
		
		return 1.0 / ret;
		
	}
	
	private double getPropertyWeight(int propertyMultiplicity)
	{
		return 1 / ((double) propertyMultiplicity);
	}
	
	public double getProbabilityStringCooccurrence(
			double sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{
		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);
		
		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		return urnProbabilityAllSameProperties(sharedCount, n1, n2, P1, P2);
	}
	
	public double getProbabilityStringCooccurrence(
			int sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{
		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);
		
		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		return urnProbabilityAllSameProperties(sharedCount, n1, n2, P1, P2);
	}
	
	public double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity) 
	{
		if(tuples==null||tuples.size()<minSharedTuples) {
			return 0;
		}
				
		String [] tuple = (String [])tuples.get(0);
		
		String c1 = tuple[c1Index];
		String c2 = tuple[c2Index];
		
		LinkedList<String[]> s1s = getCluster(c1);
		LinkedList<String[]> s2s = getCluster(c2);
		
		if(s1s==null) {
			s1s = getCluster(c1Index, tuple);
		}
		if(s2s==null) {
			s2s = getCluster(c2Index, tuple);
		}
		/*
		System.out.println("cluster 1 = " + c1);
		System.out.println("cluster 2 = " + c2);
		System.out.println("num shared tuples = " + tuples.size());
		*/
		HashMap<String, double []> stringPairCoocValues = new HashMap<String, double []>();
		String s1, s2, key;
		double [] val;
		int propertyMultiplicity;
		
		for(Iterator it = tuples.iterator(); it.hasNext(); ) {
			tuple = (String [])it.next();
			
			s1 = tuple[c1Index-1];
			s2 = tuple[c2Index-1];
			key = getKey(s1,s2);
			propertyMultiplicity = Integer.parseInt(tuple[propertyInDegreeIndex]);
			//System.out.println("property in-degree = " + propertyMultiplicity);
			
			if(stringPairCoocValues.containsKey(key)) {
				val = stringPairCoocValues.get(key);
				val[2] += getPropertyWeight(propertyMultiplicity);
			}
			else {
				val = new double[3];
				val[0] = Integer.parseInt(tuple[c1Index+1]);
				val[1] = Integer.parseInt(tuple[c2Index+1]);
				val[2] = getPropertyWeight(propertyMultiplicity);
				stringPairCoocValues.put(key, val);
			}
		}

		double ret = 1;
		double probCooc;
		String [] t1, t2;
		int n1, n2;
		double [] scores = new double[s1s.size()*s2s.size()];
		
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
					if(((int)val[0])!=n1 || ((int)val[1])!=n2) {
						System.out.println("Error!:  cooc values don't match object file:  n1 = " + n1 + ", n2 = " + n2 + ", val[0] = " + val[0] + ", val[1] = " + val[1]);					
					}
					//System.out.println(s1 + ", " + s2 + ":  k=" + val[2] + ", prob=" + probCooc);
				}
				else {
					probCooc = getProbabilityStringCooccurrence(0, n1, n2, isEntity);
					//System.out.println(s1 + ", " + s2 + ":  k=0, prob=" + probCooc);
				}
				
				//double priorProb = priorMergeProbability;
				double priorProb = getPriorProbability(n1, n2, isEntity);
				//priorProbs[i] = getPriorProbability(n1, n2, isEntity);
				//i++;
				scores[i] = probCooc / (1-probCooc) * (1-priorProb) / priorProb;
				i++;
				//ret = ret * probCooc / (1-probCooc) *
				//	  (1-priorProb) / priorProb;
				//ret = ret * probCooc / (1-probCooc);
			}
		}
		
		//double priorProb = gu.geometricMean(priorProbs);
		//ret = ret * (1-priorProb) / priorProb;
		
		ret = gu.generalizedMean(scores, 1);
		//System.out.println("ret = " + ret);
		//System.exit(1);
		return ret;
	}
	
	public double scoreMergePairCluster(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity) 
	{
		if(tuples==null) {
			return 0;
		}
		int sharedCnt = tuples.size();
		if(sharedCnt<minSharedTuples) {
			return 0;
		}

		String [] tuple = (String [])tuples.get(0);

		int c1Cnt = Integer.parseInt(tuple[c1Index+2]);
		int c2Cnt = Integer.parseInt(tuple[c2Index+2]);
		
		double probCooc = getProbabilityStringCooccurrence(sharedCnt, c1Cnt, c2Cnt, isEntity);
		double priorProb = getPriorProbability(c1Cnt, c2Cnt, isEntity);
		double ret = probCooc / (1-probCooc) * (1-priorProb) / priorProb;

		return ret;
	}
	
	public double scoreMergePair(
			LinkedList tuples, 
			int c1Index, 
			int c2Index, 
			boolean isEntity,
			String [][] s1Strings,
			String [][] s2Strings,
			double [][] retStats) 
	{
		if(tuples==null||tuples.size()<minSharedTuples) {
			return 0;
		}
		
		//System.out.println("num shared tuples = " + tuples.size());
		
		String [] tuple = (String [])tuples.get(0);
		
		String c1 = tuple[c1Index];
		String c2 = tuple[c2Index];
		int c1Cnt = Integer.parseInt(tuple[c1Index+2]);
		int c2Cnt = Integer.parseInt(tuple[c2Index+2]);
		int sharedCnt = 0;
		
		LinkedList<String[]> s1s = getCluster(c1);
		LinkedList<String[]> s2s = getCluster(c2);
		
		if(s1s==null) {
			s1s = getCluster(c1Index, tuple);
		}
		if(s2s==null) {
			s2s = getCluster(c2Index, tuple);
		}
		
		s1Strings[0] = new String[s1s.size()];
		int j = 0;
		for(Iterator<String []> it = s1s.iterator(); it.hasNext(); ) {
			s1Strings[0][j] = it.next()[0];
			j++;
		}
		s2Strings[0] = new String[s2s.size()];
		j = 0;
		for(Iterator<String []> it = s2s.iterator(); it.hasNext(); ) {
			s2Strings[0][j] = it.next()[0];
			j++;
		}
		
		HashMap<String, int []> stringPairCoocValues = new HashMap<String, int []>();
		String s1, s2, key;
		int [] val;
		
		for(Iterator it = tuples.iterator(); it.hasNext(); ) {
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
		retStats[0] = new double[s1s.size()*s2s.size()];
		retStats[1] = new double[s1s.size()*s2s.size()];
		
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
							val[2], val[0], val[1], isEntity);
					retStats[1][i] = val[2];
					sharedCnt     += val[2];
					if(val[0]!=n1 || val[1] != n2) {
						System.out.println("Error!:  cooc values don't match object file:  n1 = " + n1 + ", n2 = " + n2 + ", val[0] = " + val[0] + ", val[1] = " + val[1]);					
					}
				}
				else {
					probCooc = getProbabilityStringCooccurrence(0, n1, n2, isEntity);
					retStats[1][i] = 0;
				}
				
				//double priorProb = priorMergeProbability;
				double priorProb = getPriorProbability(n1, n2, isEntity);
				//priorProbs[i] = getPriorProbability(n1, n2, isEntity);
				//i++;
				retStats[0][i] = probCooc / (1-probCooc) * (1-priorProb) / priorProb;
				i++;
				ret = ret * probCooc / (1-probCooc) *
					  (1-priorProb) / priorProb;
				//ret = ret * probCooc / (1-probCooc);
			}
		}
		
		//double priorProb = gu.geometricMean(priorProbs);
		//ret = ret * (1-priorProb) / priorProb;
		
		//ret = gu.geometricMean(retStats[0]);
		
		probCooc = getProbabilityStringCooccurrence(sharedCnt, c1Cnt, c2Cnt, isEntity);
		double priorProb = getPriorProbability(c1Cnt, c2Cnt, isEntity);
		ret = probCooc / (1-probCooc) * (1-priorProb) / priorProb;
		
		return ret;	
	}
	
	
	
	public boolean getClassification(double score, boolean isEntity) {
		return score > threshold;
	}

	
	public int findEvenLikelihoodThreshold(int n1, int n2) {
		double probCooc, priorProb, score;
		priorProb = getPriorProbability(n1, n2, true);
		int ret = Math.min(n1, n2) / 1000 * 25;
		for(; ret<=Math.min(n1, n2); ret++) {
			probCooc = getProbabilityStringCooccurrence(ret, n1, n2, true);
			//score = probCooc / priorProb;
			score = probCooc / (1-probCooc) * (1-priorProb) / priorProb;
			//System.out.println("" + ret + ": " + score);
			if(score>1) {
				return ret;
			}
		}
		return Math.min(n1, n2);
	}
	
	
	public static void main(String[] args) {
		/*
		UrnMergePairClassifier test = new UrnMergePairClassifier();
		test.runTest();
		System.exit(1);
		*/
		
		/*
		String _workspace_ = 
			"C:\\Documents and Settings\\ayates\\My Documents\\textrunner\\corpus\\small_test_hist_sci\\under_1000\\";
		int n1 = 25;
		int n2 = 20;
		String outfile = _workspace_ + "ESP_" + n1 + "_" + n2 + ".txt";
		UrnMergePairClassifier test = new UrnMergePairClassifier(0.02, "");
		BasicDiskTable outTest = new BasicDiskTable(new File(outfile));
		test.testESPModel(n1, n2, outTest);
		System.exit(1);
		*/
		
		/*
		PropertyWeightedUrnMergePairClassifier test = new PropertyWeightedUrnMergePairClassifier(0, "");
		test.setEntityPropertyMultiple(30);
		int n1 = 2418;
		int n2 = 25;
		//int k = 1156+454+487+200;
		//double k = 1.0;
		int k = 1;
		double prob = test.getProbabilityStringCooccurrence(k, n1, n2, true);
		double priorProb = test.getPriorProbability(n1, n2, true);
		System.out.println("probability = " + prob);
		System.out.println("prior prob  = " + priorProb);
		double likelihood = prob / (1-prob) * (1-priorProb) / priorProb;
		System.out.println("likelihood  = " + likelihood);
		System.exit(1);
		*/
		/*
		UrnMergePairClassifier test = new UrnMergePairClassifier(0, "");
		test.entityPropertyMultiple = 30;
		int n1 = 50000;
		int n2 = 60000;
		test.findEvenLikelihoodThreshold(n1, n2);
		//test.testUrnMPCScores();
		System.exit(1);
		*/
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
		
		PropertyWeightedUrnMergePairClassifier pwumpc = 
			new PropertyWeightedUrnMergePairClassifier(threshold, objectFile);
	
		pwumpc.setEntityPropertyMultiple(entityPM);
		pwumpc.setRelationPropertyMultiple(relationPM);

		pwumpc.emitMergePairs(in, out, append);
	}
}
