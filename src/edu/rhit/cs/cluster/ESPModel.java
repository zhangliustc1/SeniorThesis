package edu.rhit.cs.cluster;

public class ESPModel {
	
	private int entityPropertyMultiple = 30;
	private int relationPropertyMultiple = 500;
	
	public ESPModel(){
		
	}
	
	double inefficientUrnModel(int k, int n1, int n2, boolean isEntity){
		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		
		// this is equation 3
		int minP = Math.min(P1, P2);
		
		double num = getP(k, n1, n2, P1, P2, minP);
		
		double denom = 0;
		
		for (int Sij = k; Sij <= minP; Sij++){
			denom += getP(k, n1, n2, P1, P2, Sij);
		}
		
		return num / denom;
	}
	
	double getP(int k, int n1, int n2, int P1, int P2, int S12){
		
		double num = getCount(k, n1, n2, P1, P2, S12);
		double denom = Math.exp( logChoose(P1, n1) +  logChoose(P2, n2));
		
		return num / denom;
	}
	
	double getCount(int k, int n1, int n2, int P1, int P2, int S12){
		
		// choose r and s
		int rstart = Math.max(0, n2 - k - P2 + S12);
		int sstart = Math.max(0, n1 - k - P1 + S12);
		int rend = Math.min(n2-k, Math.min(S12 - k, P1 - n1));
		int send = Math.min(n1 - k, S12 - k - (n1 - k));
		
		if ( rend + send > (S12 - k)){
			System.out.println("Boundaries for r and s are bad.");
		}
		
		double outsideterm = Math.exp(stirlingLogChoose(S12, k));
		double sum = 0;
		
		for(int r = rstart; r <= rend; r++){
			for(int s = sstart; s <= send; s++){
				double term1 = stirlingLogChoose(S12 - k, r + s);
				double term2 = stirlingLogChoose(r +s, s);
				double term3 = stirlingLogChoose(P2 - S12, n2 - (k + r));
				double term4 = stirlingLogChoose(P1 - S12, n1 - (k + s));
				sum += Math.exp(term1 + term2 + term3 + term4);
			}
		}
		
		return outsideterm * sum;
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
		
//		System.out.println("logCh_P12_k = " + logCh_P12_k);
//		System.out.println("denom = " + denom);
		

		for(int r=rstart; r<=rend; r++) {
		
			logprob = logCh_P12_k +
					  stirlingLogChoose(P12-k, r) +
					  stirlingLogChoose(P2-P12,n2-k-r) +
					  stirlingLogChoose(P1-k-r, n1-k) -
					  denom;
				
//			System.out.println();
//			System.out.println("r: " + r);
//			System.out.println("choose(P12-k, r) = " + stirlingLogChoose(P12-k, r));
//			System.out.println("choose(P2-P12, n2-k-r) = " + stirlingLogChoose(P2-P12, n2-k-r));
//			System.out.println("choose(P1-k-r, n1-k) = " + stirlingLogChoose(P1-k-r, n1-k));
//			System.out.println("logProb = " + logprob);
			ret += Math.exp(logprob);
		}
		
		//System.out.println(ret);
		
		//ret += Math.exp(logprob);
		
		return ret;
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
	
	public double getProbabilityStringCooccurrenceNormalizedPrior(
			int sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{

		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);

		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		
		double prob = urnProbabilityAllSameProperties(sharedCount, n1, n2, P1, P2);
		double prior = getPriorProbability(n1, n2, isEntity);
		
		double bestProb = urnProbabilityAllSameProperties(Math.min(n1, n2), n1, n2, P1, P2);
		
		return calculateWithPrior(prob, prior)/ calculateWithPrior(bestProb, prior);
		
		//return prob/bestProb;
	}
	
	public double getProbabilityStringCooccurrenceNormalized(
			int sharedCount, 
			int s1Count,
			int s2Count,
			boolean isEntity)
	{

		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);

		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		
		double prob = urnProbabilityAllSameProperties(sharedCount, n1, n2, P1, P2);
		double prior = getPriorProbability(n1, n2, isEntity);
		
		double bestProb = urnProbabilityAllSameProperties(Math.min(n1, n2), n1, n2, P1, P2);
		
		return prob/bestProb;
	}
	
	private double calculateWithPrior(double score, double prior){
		return score / (1 - score) * (1 - prior)/ prior;
	}
		
		
	private int getPotentialPropertyCount(int strCount, boolean isEntity)
	{
		//return (int)(Math.pow(strCount, 1.3));
		//return (int)(3*strCount * Math.log(strCount));
		int propMult = isEntity ? entityPropertyMultiple : relationPropertyMultiple;
		//int propMult = (int)(2+Math.log(strCount));
		return propMult * strCount;
	}
	
	// This is perhaps the final thing???
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
	
	public double getPriorProbability(int s1Count, int s2Count, boolean isEntity) {

		int n1 = Math.max(s1Count, s2Count);
		int n2 = Math.min(s1Count, s2Count);
		int P1 = getPotentialPropertyCount(n1, isEntity);
		int P2 = getPotentialPropertyCount(n2, isEntity);
		int minP12 = Math.min(P1, P2);
		// return priorMergeProbability;

		return 1.0 / ((double) minP12 + 1);

		// return 1.0 / ((double) numProperties);
	}
	
	// Find the smallest min(n1, n2)/40 <= k <= min(n1, n2) such that
	// the prob / (1-prob) * (1-prior)/prior term is greater than 1
	// For normalization?
	public int findEvenLikelihoodThreshold(int n1, int n2) {
		double probCooc, priorProb, score;
		priorProb = getPriorProbability(n1, n2, true);
		int ret = Math.min(n1, n2) / 1000 * 25; // same as min/40. Why this?
		for (; ret <= Math.min(n1, n2); ret++) {
			probCooc = getProbabilityStringCooccurrence(ret, n1, n2, true);
			// score = probCooc / priorProb;
			score = probCooc / (1 - probCooc) * (1 - priorProb) / priorProb;
			// System.out.println("" + ret + ": " + score);
			if (score > 1) {
				// System.out.println(score);
				return ret;
			}
		}
		return Math.min(n1, n2);
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
	
	public void myUrnTest() {
		System.out.println();


		double probCooc, priorProb, normProb, normProbPrior;
		int ni = 0;
		int nj = 0;
		int k = 0;

		for (ni = 300; ni < 320; ni += 10) {

			for (nj = 200; nj < 230; nj += 2) {
				for (k = 1; k < nj; k++) {

					normProbPrior = getProbabilityStringCooccurrenceNormalizedPrior(k, ni, nj, true);
					normProb = getProbabilityStringCooccurrenceNormalized(k, ni, nj, true);
					probCooc = getProbabilityStringCooccurrence(k, ni, nj, true);

					priorProb = getPriorProbability(ni, nj, true);

					double score = probCooc / (1 - probCooc) * (1 - priorProb)/ priorProb;
					int even = findEvenLikelihoodThreshold(ni, nj);
					
					//String s = String.format("(%d, %d, %3d): %8.4f,  %d, normProb = %.2f, norm with prior = %.2f", ni, nj, k, score, even, normProb, normProbPrior );
					String s = String.format("(%d, %d, %3d): %8.4f,  %d, normProb = %.2f", ni, nj, k, score, even, probCooc );
					System.out.println(s);
					//System.out.println("(" + ni + ", " + nj + ", " + k + "): " + score + ", " + even + ", normProb = " + normProb  + ", norm with prior = " + normProbPrior);
					
				}
			}
		}
	}
	
	public static void main(String[] args){
		ESPModel esp = new ESPModel();

		// num extracted shared properties
		int k = 30;
		// num assertions containing s1
		int n1 = 70;
		// num assertions containing s2
		int n2 = 50;

		boolean isEntity = true;
		//entityPropertyMultiple : relationPropertyMultiple;
		
		
		//System.out.println(esp.inefficientUrnModel(k, n1, n2, isEntity));
		//System.out.println(esp.getProbabilityStringCooccurrenceNormalized(k, n1, n2, isEntity));
		esp.myUrnTest();

		
		
	}
	
	
}
