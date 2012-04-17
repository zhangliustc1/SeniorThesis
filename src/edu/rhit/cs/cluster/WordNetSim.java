package edu.rhit.cs.cluster;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;

import edu.sussex.nlp.jws.*;

public class WordNetSim {
	JWS ws;
	
	public WordNetSim() {
		String dir = "/home/stephen/Documents/Classes/Fall2011/NLP/resolver-export/MyReverbData/dummywordnet";
		this.ws = new JWS(dir, "3.0");
	}

	// Given a string (which should have separable spaces in it)
	// this returns a list that has only the important words left.
	public static List<String> parseKeywords(String keywords) {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_29);
		
		List<String> result = new ArrayList<String>();
		TokenStream stream = analyzer.tokenStream("", new StringReader(keywords));

		try {
			while (stream.incrementToken()) {
				result.add(((TermAttribute) stream
						.getAttribute(TermAttribute.class)).term());
			}
		} catch (IOException e) {
			// not thrown b/c we're using a string reader...
		}

		return result;
	}
	
	
	private double wordDifference(String s1, String s2){
		JiangAndConrath jcn = ws.getJiangAndConrath();
		
		// All the different options
//		ws.getAdaptedLesk();
//		ws.getAdaptedLeskTanimoto();
//		ws.getHirstAndStOnge();
//		ws.getJiangAndConrath();
//		ws.getLeacockAndChodorow();
//		ws.getLin();
//		ws.getPath();
//		ws.getResnik();
//		ws.getWuAndPalmer();
		
		return jcn.max(s1, s2, "v");
		
	}
	
	// This assumes that both input strings are longer than one word. 
	// One word should also be fine though. 
	// Assume that all words are verbs. Hmm. 
	private double phraseDifference(String s1, String s2){
		List<String> l1 = parseKeywords(s1); 
		List<String> l2 = parseKeywords(s2);
		
		// now somehow get difference between each element in l1 and l2
		
		
		return 0.0;
	}

	public static void main(String[] args) {

		List<String> l = parseKeywords(",, you are cool MAN");
		System.out.println(l);
		
		WordNetSim w = new WordNetSim();
		
		System.out.println(w.wordDifference("carry", "lift"));

	}
}
