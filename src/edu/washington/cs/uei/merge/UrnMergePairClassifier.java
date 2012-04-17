package edu.washington.cs.uei.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import edu.washington.cs.uei.disktable.BasicDiskTable;

public class UrnMergePairClassifier 
	extends AbstractClusterMergePairClassifier
	implements MergePairClassifier 
{
	private int minSharedTuples = 1;
	private int entityPropertyMultiple = 10;
	private int relationPropertyMultiple = 500;
	//private int numProperties = 8432;
	private double threshold = 1.0;
	
	private int P12SampleSize = 100;
	private int rSampleSize   = 50;
	
	private String objectFile;
	
	public UrnMergePairClassifier(double mergeThreshold, String objectFile)
	{
		super(objectFile);
		//priorMergeProbability = priorMergeProb;
		threshold = mergeThreshold;
		this.objectFile = objectFile;
		//clusterToStringMap = gu.clusterHashMap(objectFile);
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
		
		if(k<=1 || k>=n-1) {
			return Math.log(n);
		}
		
		return stirlingLogFactorial(n) - stirlingLogFactorial(k) - stirlingLogFactorial(n-k);
	}
	
	// Same as equation (1)?
	double urnLikelihoodSample(int k, int n1, int n2, int P1, int P2, int P12)
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
		double rdiff  = ((rend-rstart) / ((double)rSampleSize));
		
		//System.out.println("Using rdiff = " + rdiff);
		
		
		double ret = 0;
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		

		if(rdiff<1) {
			for(int r=rstart; r<=rend; r++) {
				//System.out.println("Trying r = " + r);
				logprob = logCh_P12_k +
						  stirlingLogChoose(P12-k, r) +
						  stirlingLogChoose(P2-P12,n2-k-r) +
						  stirlingLogChoose(P1-k-r, n1-k) -
						  stirlingLogChoose(P1,n1) -
						  stirlingLogChoose(P2,n2);
					
				ret += Math.exp(logprob);
			}
			//System.out.println(ret);
			return ret;
		}
		int index = 0;
		for(double r=rstart; r<rend; r+=rdiff) {
			//System.out.println("Trying r = " + r);
			index = (int)Math.round(r);
			logprob = logCh_P12_k +
					  stirlingLogChoose(P12-k, index) +
					  stirlingLogChoose(P2-P12,n2-k-index) +
					  stirlingLogChoose(P1-k-index, n1-k) -
					  stirlingLogChoose(P1,n1) -
					  stirlingLogChoose(P2,n2);
				
			ret += Math.exp(logprob);
		}
		//System.out.println("Trying r = " + rend);
		logprob = logCh_P12_k +
		  stirlingLogChoose(P12-k, rend) +
		  stirlingLogChoose(P2-P12,n2-k-rend) +
		  stirlingLogChoose(P1-k-rend, n1-k) -
		  stirlingLogChoose(P1,n1) -
		  stirlingLogChoose(P2,n2);
		
		//System.out.println(ret);
		
		ret += Math.exp(logprob);
		
		ret = ret *(rend-rstart) / ((double)rSampleSize+1);
		
		return ret;
	}
	
	
	double urnLikelihoodSample(double k, int n1, int n2, int P1, int P2, int P12)
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
		
		int rstart = (int)Math.ceil(Math.max(0,P12-P2+n2-k));
		int rend   = (int)Math.min(n2-k, Math.min(P12-k, P1-n1));
		double rdiff  = ((rend-rstart) / ((double)rSampleSize));
		
		//System.out.println("Using rdiff = " + rdiff);
		
		
		double ret = 0;
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		

		if(rdiff<1) {
			for(int r=rstart; r<=rend; r++) {
				//System.out.println("Trying r = " + r);
				logprob = logCh_P12_k +
						  stirlingLogChoose(P12-k, r) +
						  stirlingLogChoose(P2-P12,n2-k-r) +
						  stirlingLogChoose(P1-k-r, n1-k) -
						  stirlingLogChoose(P1,n1) -
						  stirlingLogChoose(P2,n2);
					
				ret += Math.exp(logprob);
			}
			//System.out.println(ret);
			return ret;
		}
		int index = 0;
		for(double r=rstart; r<rend; r+=rdiff) {
			//System.out.println("Trying r = " + r);
			index = (int)Math.round(r);
			logprob = logCh_P12_k +
					  stirlingLogChoose(P12-k, index) +
					  stirlingLogChoose(P2-P12,n2-k-index) +
					  stirlingLogChoose(P1-k-index, n1-k) -
					  stirlingLogChoose(P1,n1) -
					  stirlingLogChoose(P2,n2);
				
			ret += Math.exp(logprob);
		}
		//System.out.println("Trying r = " + rend);
		logprob = logCh_P12_k +
		  stirlingLogChoose(P12-k, rend) +
		  stirlingLogChoose(P2-P12,n2-k-rend) +
		  stirlingLogChoose(P1-k-rend, n1-k) -
		  stirlingLogChoose(P1,n1) -
		  stirlingLogChoose(P2,n2);
		
		ret += Math.exp(logprob);
		
		//System.out.println(ret);
		
		ret = ret *(rend-rstart) / ((double)rSampleSize+1);
		
		return ret;
	}
	
	
	// Equation 2 pp.263
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
		
		//int rstart = Math.max(0,P12-P2+n2-k);
		//int rend   = Math.min(n2-k, Math.min(P12-k, P1-n1));
		int r = n2-k;
		
		//double ret = 0;
		double logprob = 0;
		double logCh_P12_k = stirlingLogChoose(P12,k);
		double denom = stirlingLogChoose(P1,n1) + stirlingLogChoose(P2,n2);
		
		//for(int r=rstart; r<=rend; r++) {
		
		logprob = logCh_P12_k +
				  stirlingLogChoose(P12-k, r) +
				  stirlingLogChoose(P2-P12,n2-k-r) +
				  stirlingLogChoose(P1-k-r, n1-k) -
				  denom;
			
		//System.out.println();
		//System.out.println("" + r);
		//System.out.println("choose(P12-k, r) = " + stirlingLogChoose(P12-k, r));
		//System.out.println("choose(P2-P12, n2-k-r) = " + stirlingLogChoose(P2-P12, n2-k-r));
		//System.out.println("choose(P1-k-r, n1-k) = " + stirlingLogChoose(P1-k-r, n1-k));
		//System.out.println("logProb = " + logprob);
			//ret += Math.exp(logprob);
		//}
		
		//System.out.println(ret);
		
		//ret += Math.exp(logprob);
		
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
	
	
	double urnLikelihoodSample(int k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		//System.out.println("calculating urnLikelihood:  " + k + ", " + n1 + ", " + n2 + ", " + P1 + ", " + P2 + ", " + minP12 + ", " + maxP12);
		
		double ret = 0;
		
		double Pdiff = ((maxP12-k) / ((double)P12SampleSize));
		
		//System.out.println("Using Pdiff = " + Pdiff);
		
		if(Pdiff<1) {
			for(; minP12 <=maxP12; minP12++) {
				//System.out.println("Trying P12 = " + minP12);
				ret += urnLikelihoodSample(k, n1, n2, P1, P2, minP12);
			}
			//ret /= ((double) Math.min(P1,P2) + 1);
			//System.out.println(ret);
			return ret;
		}
		
		int index = 0;
		int count = 0;
		for(double P12 = minP12; P12<maxP12-0.5; P12+=Pdiff) {
			index = (int)Math.round(P12);
			//System.out.println("Trying P12 = " + P12);
			ret += urnLikelihoodSample(k,n1,n2,P1,P2,index);
			count++;
		}
		//System.out.println("Trying P12 = " + maxP12);
		ret += urnLikelihoodSample(k, n1, n2, P1, P2, maxP12);
		
		ret = ret * (maxP12-minP12+1) / ((double)count+1);
		
		//adjust for P(P12)
		//ret /= ((double)Math.min(P1,P2) + 1);
		
		//System.out.println(ret);
		
		return ret;
	}
	
	
	double urnLikelihoodSample(double k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		//System.out.println("calculating urnLikelihood:  " + k + ", " + n1 + ", " + n2 + ", " + P1 + ", " + P2 + ", " + minP12 + ", " + maxP12);
		
		double ret = 0;
		
		double Pdiff = ((maxP12-k) / ((double)P12SampleSize));
		
		//System.out.println("Using Pdiff = " + Pdiff);
		
		if(Pdiff<1) {
			for(; minP12 <=maxP12; minP12++) {
				//System.out.println("Trying P12 = " + minP12);
				ret += urnLikelihoodSample(k, n1, n2, P1, P2, minP12);
			}
			ret /= ((double) Math.min(P1,P2)- k + 1);
			//System.out.println(ret);
			return ret;
		}
		
		int index = 0;
		int count = 0;
		for(double P12 = minP12; P12<maxP12-0.5; P12+=Pdiff) {
			index = (int)Math.round(P12);
			//System.out.println("Trying P12 = " + P12);
			ret += urnLikelihoodSample(k,n1,n2,P1,P2,index);
			count++;
		}
		//System.out.println("Trying P12 = " + maxP12);
		ret += urnLikelihoodSample(k, n1, n2, P1, P2, maxP12);
		
		ret = ret * (maxP12-minP12+1) / ((double)count+1);
		
		//adjust for P(P12)
		//ret /= ((double)Math.min(P1,P2) + 1);
		
		//System.out.println(ret);
		
		return ret;
	}
	
	// This is equation 3?
	double urnLikelihood(int k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		//System.out.println("calculating urnLikelihood:  " + k + ", " + n1 + ", " + n2 + ", " + P1 + ", " + P2 + ", " + minP12 + ", " + maxP12);
		
		double ret = 0;
		
		for(int P12 = minP12; P12<=maxP12; P12++) {
			ret += urnLikelihood(k,n1,n2,P1,P2,P12);
		}
		
		//adjust for P(P12)
		//ret /= ((double)Math.min(P1,P2)+1);
		
		//System.out.println(ret);
		
		return ret;
	}
	
	
	double urnLikelihood(double k, int n1, int n2, int P1, int P2, int minP12, int maxP12)
	{
		//System.out.println("calculating urnLikelihood:  " + k + ", " + n1 + ", " + n2 + ", " + P1 + ", " + P2 + ", " + minP12 + ", " + maxP12);
		
		double ret = 0;
		
		for(int P12 = minP12; P12<=maxP12; P12++) {
			ret += urnLikelihood(k,n1,n2,P1,P2,P12);
		}
		
		//adjust for P(P12)
		//ret /= ((double)Math.min(P1,P2)+1);
		
		//System.out.println(ret);
		
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
		//int propMult = (int)(2+Math.log(strCount));
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
			ret += Math.exp(logProb+addend-logLikelihoodAllSameProperties);
		}
		
		//return ret / ((double)(Math.min(P1,P2)+1));
		return 1.0 / ret;
		
	}
	
	public double getProbabilityStringCooccurrence(
			int sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{
		//if(isEntity) {
			int n1 = Math.max(s1Count, s2Count);
			int n2 = Math.min(s1Count, s2Count);
			
			int P1 = getPotentialPropertyCount(n1, isEntity);
			int P2 = getPotentialPropertyCount(n2, isEntity);
			//int P1 = numProperties;
			//int P2 = numProperties;
			/*
			System.out.println("n1 = " + n1);
			System.out.println("n2 = " + n2);
			System.out.println("P1 = " + P1);
			System.out.println("P2 = " + P2);
			System.out.println("n1 mult = " + (P1 / ((double)n1)));
			System.out.println("n2 mult = " + (P2 / ((double)n2)));
			*/
			//int minP12 = (int) (sharedPropertyCutoff * maxP12);
			/*
			int P1 = numProperties;
			int P2 = numProperties;
			int maxP12 = numProperties;
			*/
			//return urnLikelihood(sharedCount, n1, n2, P1, P2, minP12, maxP12) /
			//return urnLikelihood(sharedCount, n1, n2, P1, P2, maxP12) / 
			//		urnLikelihoodAllP12(sharedCount, n1, n2, P1, P2);
			return urnProbabilityAllSameProperties(sharedCount, n1, n2, P1, P2);
		//}
		//else {
		//	return Math.min(sharedCount / gu.generalizedMean(s1Count, s2Count, 0.5), 0.99999); 
		//}
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
		
		//System.out.println("num shared tuples = " + tuples.size());
		
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
							val[2], val[0], val[1], isEntity);
					if(val[0]!=n1 || val[1] != n2) {
						System.out.println("Error!:  cooc values don't match object file:  n1 = " + n1 + ", n2 = " + n2 + ", val[0] = " + val[0] + ", val[1] = " + val[1]);					
					}
				}
				else {
					probCooc = getProbabilityStringCooccurrence(0, n1, n2, isEntity);
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
	
	
	public double scoreMergePairTemp(
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
	
	private double getExpProb(int ind)
	{
		final double alpha = 0.4;
		double ret = 1-alpha;
		for(int i=0; i<ind; i++) {
			ret *= alpha;
		}
		return ret;
	}
	
	public void testUrnMPCScores()
	{
		
		int maxSyn = 10;
		int [][] evenLikelihoodThresholds = new int [maxSyn][maxSyn];
		double [][] umpcScores = new double[maxSyn][maxSyn];
		
		final int totalMass = 100000;
		final double repRate = 0.1;
		
		double probCooc, priorProb;
		double probJ = 0;
		double probI = 0;
		int ni = 0;
		int nj = 0;
		int k  = 0;
		for(int i=1; i<maxSyn-1; i++) {
			probI = getExpProb(i);
			ni = (int) (probI * totalMass);
			for(int j=i+1; j<maxSyn; j++) {
				probJ = getExpProb(j);
				nj = (int) (probJ * totalMass);
				k  = (int) (probI * probJ * repRate * totalMass);
				probCooc = getProbabilityStringCooccurrence(k, ni, nj, true);
				priorProb = getPriorProbability(ni, nj, true);
				umpcScores[i][j] = probCooc / (1-probCooc) * (1-priorProb)/ priorProb;
				//umpcScores[i][j] = Math.log(probCooc);
				evenLikelihoodThresholds[i][j] = findEvenLikelihoodThreshold(ni, nj);
				//System.out.println("" + i + ", " + j + ": " + umpcScores[i][j] + ", " + evenLikelihoodThresholds[i][j]);
				System.out.println("" + i + ", " + j + ": (" + ni + ", " + nj + ", " + k + "): " + umpcScores[i][j] + ", " + evenLikelihoodThresholds[i][j]);
			}
		}
	}
	
	private void runTest()
	{
		
		this.entityPropertyMultiple = 50;
		/*
		int shared = 469;
		int x = 2203;
		int y = 4039;
		*/
		
		int shared = 206;
		int x = 1032;
		int y = 8014;
		
		//this.entityPropertyMultiple = (int)(2+Math.log(Math.min(x, y)));
		System.out.println("prop Mult = " + this.entityPropertyMultiple);
		
		System.out.println("prob of co-occurrence if shared = " + shared);
		System.out.println(" and x = " + x + ", y = " + y + " is:");
		
		double result = this.getProbabilityStringCooccurrence(shared, x, y, true);
		double prior = this.getPriorProbability(x, y, true);
		System.out.println(result + ", prior = " + prior);
		System.out.println("(odds = " + (result * (1-prior) / (1-result) / prior) + ")");
	}
	
	
	public static void main(String[] args) {
		/*
		UrnMergePairClassifier test = new UrnMergePairClassifier(1.0, "");
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
		UrnMergePairClassifier test = new UrnMergePairClassifier(0, "");
		//test.entityPropertyMultiple = 15;
		int n1 = 106061;
		int n2 = 29266;
		//int k = 1156+454+487+200;
		int k = 726+277+296+123;
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
		
		UrnMergePairClassifier umpc = 
			new UrnMergePairClassifier(threshold, objectFile);
	
		umpc.setEntityPropertyMultiple(entityPM);
		umpc.setRelationPropertyMultiple(relationPM);

		umpc.emitMergePairs(in, out, append);
	}
}
