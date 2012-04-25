package edu.rhit.cs.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.HunspellDictionary;
import org.apache.lucene.analysis.hunspell.HunspellStemmer;
import org.apache.lucene.analysis.hunspell.HunspellStemmer.Stem;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;

import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.JiangAndConrath;

public class WordNetSim {
	JWS ws;
	HunspellStemmer hStemmer;

	private static final PrintStream SYSTEM_OUT = System.out;
	private static final PrintStream NULL_OUT = new PrintStream(new NullOutputStream());
	
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
				String s = ((TermAttribute) stream.getAttribute(TermAttribute.class)).term();				
				result.add(s);
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
	
		return last;
	}
	
	// Get the largest element
//	private String getLongest(List<String> l){
//		String largest = "";
//		int size = -1;
//		for (String s : l){
//			if (s.length() > size){
//				size = s.length();
//				largest = s;
//			}
//		}
//		return largest;
//	}
	
	// This finds the first string that has 
	// a meaningful stem, [and has the possibility
	// of being a verb]
	private String getBest(List<String> l){
		String s = this.getStem(l.get(0));
		
		// TODO: add logic that checks for verbishness
		
		int ind = 1;
		boolean bad = s == "[No Stem]";
		while(bad){
			if (ind >= l.size()-1){
				break;
			}
			s = this.getStem(l.get(ind));
			
			bad = s == "[No Stem]";
			ind++;
		}
		return s;
	}

	public double similarity(String s1, String s2){
		// Remove stopwords
		List<String> l1 = parseKeywords(s1); 
		List<String> l2 = parseKeywords(s2);
		
		if(l1.size() == 0 || l2.size() == 0){
			return 0.0;
		}
		
		// Compares so the sort is descending
		class LenComparator implements Comparator<String>{
		    @Override
		    public int compare(String o1, String o2) {  
		      if (o1.length() < o2.length()) {
		         return 1;
		      } else if (o1.length() > o2.length()) {
		         return -1;
		      } else { 
		         return 0;
		      }
		    }
		}
		
		LenComparator lc = new LenComparator();
		Collections.sort(l1, lc);
		Collections.sort(l2, lc);
		
		// Get the largest word and lemmatize it
		// A lemmatized word is more likely to be
		// in WordNet. 
		// The idea behind getting the longest
		// word is that long words are usually
		// more meaningful than short words.
		// TODO: make this smarter instead of just getting longest
		s1 = this.getBest(l1);
		s2 = this.getBest(l2);
		
		//System.out.println("S1: " + s1 + ", S2: " + s2);
		
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
		
		if (s1.equals(s2)){
			return 1;
		}
		
		System.setOut(NULL_OUT);
		double d = jcn.max(s1, s2, "v"); 
		System.setOut(SYSTEM_OUT);
		
		return d;
	}
	
	public static void main(String[] args) {

//		List<String> l = parseKeywords(",, you are cool MAN");
//		System.out.println(l);
//		
		WordNetSim w = new WordNetSim();
//		
		String s = "fff";
		String t = "was implemented";
			
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
	
	private static class NullOutputStream extends OutputStream {
	    @Override
	    public void write(int b){
	         return;
	    }
	    @Override
	    public void write(byte[] b){
	         return;
	    }
	    @Override
	    public void write(byte[] b, int off, int len){
	         return;
	    }
	    public NullOutputStream(){
	    }
	}
	
	
}


