package edu.rhit.cs.cluster;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.HunspellDictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemmer;
import org.apache.lucene.analysis.hunspell.HunspellStemmer.Stem;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;

import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.JiangAndConrath;

public class WordNetSim {
	JWS ws;
	HunspellStemmer hStemmer;
	
	
	public WordNetSim() {
		String dir = "/home/stephen/Documents/Classes/Fall2011/NLP/resolver-export/MyReverbData/dummywordnet";
		this.ws = new JWS(dir, "3.0");
		
		InputStream aff;
		InputStream dic;
		try {
			aff = new FileInputStream(new File("/usr/share/hunspell/en_US.aff"));
			dic = new FileInputStream(new File("/usr/share/hunspell/en_US.dic"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		HunspellDictionary hd;
		try {
			hd = new HunspellDictionary(aff, dic);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		
		this.hStemmer = new HunspellStemmer(hd);
		
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
	
	// Returns empty  if the stem is not good.
	// Not always guaranteed to return the correct stem.
	// Probably 90% accurate.
	private String getStem(String s){
		List<Stem> ls = this.hStemmer.stem(s);
		
		if (ls.size() == 0){
			return "[No Stem]";
		}

		String last = ls.get(ls.size()-1).getStemString();
		int getMe = 2;
		while(!last.startsWith(s.substring(0,1))){
			if(getMe >= ls.size()){
				break;
			}
			last = ls.get(ls.size()-getMe).getStemString();
			getMe++;
		}
		
		// Include some logic about it being a verb.
		
	
		return last;
	}
	
	// Get the largest element
	private String getLongest(List<String> l){
		String largest = "";
		int size = -1;
		for (String s : l){
			if (s.length() > size){
				size = s.length();
				largest = s;
			}
		}
		return largest;
	}

	public double similarity(String s1, String s2){
		// Remove stopwords
		List<String> l1 = parseKeywords(s1); 
		List<String> l2 = parseKeywords(s2);
				
		// Get the largest word and lemmatize it
		// A lemmatized word is more likely to be
		// in WordNet. 
		// The idea behind getting the longest
		// word is that long words are usually
		// more meaningful than short words.
		// TODO: make this smarter instead of just getting longest
		s1 = this.getStem(this.getLongest(l1));
		s2 = this.getStem(this.getLongest(l2));
		
		System.out.println(s1 + ", " + s2);
		
		
		// Finally, preprocessing out of
		// the way, do WordNet similarity
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
	
	public static void main(String[] args) {

//		List<String> l = parseKeywords(",, you are cool MAN");
//		System.out.println(l);
//		
		WordNetSim w = new WordNetSim();
//		
		String s = "is implemented in";
		String t = "as performed by";
		
		
		
		
		System.out.println(w.similarity(s, t));


		
			
//		try {
//			// Open the file that is the first
//			// command line parameter
//			FileInputStream fstream = new FileInputStream(
//					"/home/stephen/Documents/Classes/Fall2011/NLP/resolver-export/MyReverbData/yates_gold_relations.txt");
//			// Get the object of DataInputStream
//			DataInputStream in = new DataInputStream(fstream);
//			BufferedReader br = new BufferedReader(new InputStreamReader(in));
//			String strLine;
//			// Read File Line By Line
//			while ((strLine = br.readLine()) != null) {
//				// Print the content on the console		
//				String justText = strLine.split(" :::: ")[0];
//				
//				List<String> l2 = parseKeywords(justText);
//				if (l2.size() == 0){
//					continue;
//				}
//				//System.out.println(l2);
//				
//				System.out.println(w.getStem(l2.get(l2.size()-1)));
//			}
//			// Close the input stream
//			in.close();
//		} catch (Exception e) {// Catch exception if any
//			System.out.println(e);
//			System.err.println("Error: " + e.getMessage());
//		}	
		
		
		
	
	}
}
