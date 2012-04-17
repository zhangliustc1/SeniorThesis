package edu.washington.cs.uei.model;

import java.util.HashSet;
import java.util.Iterator;


public class ZipfDistribution {
	private int numDistinctElements;
	private double probFirstElement;
	private double zipfParam;
	
	private static final double gamma = 0.577215664901532860606512090082402431042159335;
	private static final int    exactHarmonicNumberLimit = 1000;
	
	/*
	private class HarmonicIntegrand implements RealFunctionOfOneVariable 
	{
		int harmonicNum = 1;
		public HarmonicIntegrand(int n) {
			
		}
		public double eval(double t) {
			return (1-Math.pow(t, harmonicNum)) / (1-t);
		}
	};*/
	
	public ZipfDistribution(int numElements, double zipfParameter)
	{
		numDistinctElements = numElements;
		zipfParam = zipfParameter;
		
		init();
	}
	
	public ZipfDistribution(int numElements) {
		numDistinctElements = numElements;
		zipfParam = 1;
		
		init();
	}
	
	private void init() {
		double probMass = 0;
		for(int i=1; i<=numDistinctElements; i++) {
			probMass += 1 / (Math.pow((double) i, zipfParam));
		}
		probFirstElement = 1 / probMass;		
	}
	
	public int getNumDistinctElements() {
		return numDistinctElements;
	}
	
	public double getZipfParameter() {
		return zipfParam;
	}
	
	public double getElementProbability(int el) {
		if(el<1 || el>numDistinctElements) {
			return 0;
		}
		return probFirstElement / Math.pow(el, zipfParam);
	}
	
	public int generateElement() {
		double probChoice = Math.random();
		return findClosestHarmonicNumberByBinarySearch(probChoice/probFirstElement);
		/*
		int curChoice = 1;
		double curProb = getElementProbability(curChoice);
		while(curProb<probChoice && curChoice<numDistinctElements) {
			curChoice++;
			curProb += getElementProbability(curChoice);
		}
		
		return curChoice;
		*/
	}
	
	public int generateDistinctElement(HashSet<Integer> leaveOutEls, double leaveOutProbMass)
	{
		int choice = generateElement();
		while(leaveOutEls.contains(new Integer(choice))) {
			choice = generateElement();
		}
		return choice;
		/*
		double rebalance = 1 / (1-leaveOutProbMass);
		double probChoice = Math.random();
		//System.out.println("Zipf.genDistinct:  rebalance = " + rebalance + ", prob choice = " + probChoice);
		Integer curChoice = new Integer(1);
		double curProb = (leaveOutEls.contains(curChoice)) ? 
				0 : getElementProbability(curChoice) * rebalance;
		while(curProb<probChoice && curChoice<numDistinctElements) {
			curChoice++;
			curProb += (leaveOutEls.contains(curChoice)) ? 
					0 : getElementProbability(curChoice) * rebalance;
		}
		//System.out.println("Zipf.genDistinct:  out = " + curChoice);
		return curChoice;
		*/
	}
	
	public int findClosestHarmonicNumberByBinarySearch(double val)
	{
		int low = 1;
		int high = numDistinctElements;
		double H;
		int middle = 1;
		while(high-low>0) {
			//System.out.println("high = " + high + ", low = " + low);
			middle = (high+low) / 2;
			H = harmonicNumber(middle);
			if(H>val) {
				high = middle - 1;
			}
			else if(H+1/((double)middle+1) <= val) {
				low = middle + 1;
			}
			else {
				return middle;
			}
		}
		return low;
	}
	
	/*
	public double harmonicNumberByIntegration(int n) 
	{
		HarmonicIntegrand h = new HarmonicIntegrand(n);
		//return RungeKuttaFehlbergIntegrator.integrate(h, 0, 1);
		return BulirschStoerIntegrator.integrate(h, 0, 1);
	}*/
	
	public static double harmonicNumberBySum(int n) 
	{
		double ret = 0;
		for(int i=1; i<=n; i++) {
			ret += 1 / ((double) i);
		}
		return ret;
	}
	
	public static double harmonicNumberByLogApproximation(int n) 
	{
		return gamma + Math.log(n) + 1 / ((double) 2*n) - 1 / ((double) 12*n*n); 
		//return gamma + Math.log(n) + 1 / ((double) 2*n);
	}
	
	public static double harmonicNumber(int n) {
		if(n<exactHarmonicNumberLimit) {
			return harmonicNumberBySum(n);
		}
		return harmonicNumberByLogApproximation(n);
	}
	
	public static void main(String [] args) 
	{
		ZipfDistribution zd = new ZipfDistribution(30000000);
		HashSet<Integer> leaveOuts = new HashSet<Integer>();
		for(int i=0; i<100; i++) {
			int choice = zd.generateDistinctElement(leaveOuts, 0);
			leaveOuts.add(new Integer(choice));
		}
		for(Iterator<Integer> it = leaveOuts.iterator(); it.hasNext(); ) {
			System.out.println(it.next());
		}
		System.out.println(leaveOuts.size());
		
		/*
		ZipfDistribution zd = new ZipfDistribution(30000000);
		for(int i=1; i<zd.numDistinctElements; i *= 10) {
			System.out.println("element = " + i);
			System.out.println("found element = " + zd.findClosestHarmonicNumberByBinarySearch(harmonicNumber(i)));
		}*/
		/*
		for(int i = exactHarmonicNumberLimit; i<2*exactHarmonicNumberLimit; i++) {
			double h1 = harmonicNumberBySum(i);
			double h2 = harmonicNumberByLogApproximation(i);
			System.out.println(i);
			System.out.println("real:  " + h1);
			System.out.println("approx:  " + h2);
			System.out.println("error in log approximation:  " + (h2-h1));
		}*/
	}
}
