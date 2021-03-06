package edu.rhit.tools;

import java.util.TreeMap;
import java.text.*;
import edu.sussex.nlp.jws.*;

// 'TestExamples': how to use Java WordNet::Similarity
// David Hope, 2008
public class JWSTest
{
 	public static void main(String[] args)
	{

// 1. SET UP:
//   Let's make it easy for the user. So, rather than set pointers in 'Environment Variables' etc. let's allow the user to define exactly where they have put WordNet(s)
		//String dir = "C:/Program Files/WordNet";
		//String dir = "/usr/share/wordnet";
		String dir = "/home/stephen/Documents/Classes/Fall2011/NLP/resolver-export/MyReverbData/dummywordnet";
//   That is, you may have version 3.0 sitting in the above directory e.g. C:/Program Files/WordNet/3.0/dict
//   The corresponding IC files folder should be in this same directory e.g. C:/Program Files/WordNet/3.0/WordNet-InfoContent-3.0

//   Option 1  (Perl default): specify the version of WordNet you want to use (assuming that you have a copy of it) and use the default IC file [ic-semcor.dat]
		JWS	ws = new JWS(dir, "3.0");
//   Option 2 : specify the version of WordNet you want to use and the particular IC file that you wish to apply
		//JWS ws = new JWS(dir, "3.0", "ic-bnc-resnik-add1.dat");


// 2. EXAMPLES OF USE:

// 2.1 [JIANG & CONRATH MEASURE]
		JiangAndConrath jcn = ws.getJiangAndConrath();
		System.out.println("Jiang & Conrath\n");
// all senses
		TreeMap<String, Double> 	scores1	=	jcn.jcn("apple", "banana", "n");			// all senses
		//TreeMap<String, Double> 	scores1	=	jcn.jcn("apple", 1, "banana", "n"); 	// fixed;all
		//TreeMap<String, Double> 	scores1	=	jcn.jcn("apple", "banana", 2, "n"); 	// all;fixed
		for(String s : scores1.keySet())
			System.out.println(s + "\t" + scores1.get(s));
// specific senses
		System.out.println("\nspecific pair\t=\t" + jcn.jcn("apple", 1, "banana", 1, "n") + "\n");
// max.
		System.out.println("\nhighest score\t=\t" + jcn.max("apple", "banana", "n") + "\n\n\n");


// 2.2 [LIN MEASURE]
		Lin lin = ws.getLin();
		System.out.println("Lin\n");
// all senses
//		String s1 = "consider";
//		String s2 = "think";
		String s1 = "is published in";
		String s2 = "is implemented in";
		TreeMap<String, Double> 	scores2	=	lin.lin(s1, s2, "v");			// all senses
		//TreeMap<String, Double> 	scores2	=	lin.lin("apple", 1, "banana", "n"); 	// fixed;all
		//TreeMap<String, Double> 	scores2	=	lin.lin("apple", "banana", 2, "n"); 	// all;fixed
		for(String s : scores2.keySet())
			System.out.println(s + "\t" + scores2.get(s));
				
// specific senses
		System.out.println("\nspecific pair\t=\t" + lin.lin(s1, 1, s2, 2, "v") + "\n");
// max.
		System.out.println("\nhighest score\t=\t" + lin.max(s1, s2, "v") + "\n\n\n");

// ... and so on for any other measure
		

		

		
		
	}
} // eof