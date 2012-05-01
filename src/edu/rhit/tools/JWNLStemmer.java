package edu.rhit.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;
import net.didion.jwnl.dictionary.MorphologicalProcessor;


// taken from: http://tipsandtricks.runicsoft.com/Other/JavaStemmer.html

public class JWNLStemmer {

	//private int MaxWordLength = 50;
	private Dictionary dict;
	private MorphologicalProcessor morph;
	private boolean isInitialized = false;  
	public HashMap<String, String> allWords = null;
	
	public static final String NON_VERB = "NON_VERB";

	/**
	 * establishes connection to the WordNet database
	 */
	public JWNLStemmer ()
	{
		this.allWords = new HashMap<String, String>();
		
		try
		{
			JWNL.initialize(new FileInputStream("JWNLproperties.xml"));
			this.dict = Dictionary.getInstance();
			this.morph = this.dict.getMorphologicalProcessor();
			// ((AbstractCachingDictionary)dic).
			//	setCacheCapacity (10000);
			this.isInitialized = true;
		}
		catch ( FileNotFoundException e )
		{
			System.out.println ( "Error initializing Stemmer: JWNLproperties.xml not found" );
		}
		catch ( JWNLException e )
		{
			System.out.println ( "Error initializing Stemmer: " + e.toString() );
		} 
		
	}

	public void Unload ()
	{ 
		dict.close();
		Dictionary.uninstall();
		JWNL.shutdown();
	}

	/**
	 * stems a word with wordnet
	 * @param word word to stem
	 * @return the stemmed word or null if it was not found in WordNet
	 */
	public String stemWordWithWordNet ( String word )
	{
		if ( !isInitialized )
			return word;
		if ( word == null ) return null;
		if ( morph == null ) morph = dict.getMorphologicalProcessor();
		
		IndexWord w;
		try
		{
			w = morph.lookupBaseForm( POS.VERB, word );
			if ( w != null )
				return w.getLemma().toString ();
			//System.out.println("word is not verb!");
			
//			w = morph.lookupBaseForm( POS.NOUN, word );
//			if ( w != null )
//				return w.getLemma().toString();
//			w = morph.lookupBaseForm( POS.ADJECTIVE, word );
//			if ( w != null )
//				return w.getLemma().toString();
//			w = morph.lookupBaseForm( POS.ADVERB, word );
//			if ( w != null )
//				return w.getLemma().toString();
		} 
		catch ( JWNLException e )
		{
			System.out.println("Problem with stemming: " + e);
		}
		return null;
	}

	/**
	 * Stem a single word
	 * tries to look up the word in the allWords HashMap
	 * If the word is not found it is stemmed with WordNet
	 * and put into allWords
	 * 
	 * @param word word to be stemmed
	 * @return stemmed word
	 */
	public String stem( String word )
	{
		// check if we already know the word
		String stemmedword = allWords.get( word );
		if ( stemmedword != null )
			return stemmedword; // return it if we already know it
		
		// don't check words with digits in them
//		if ( containsNumbers(word) == true )
//			stemmedword = null;
		// else
		
		// unknown word: try to stem it
		stemmedword = stemWordWithWordNet (word);
		
		if ( stemmedword != null )
		{
			// word was recognized and stemmed with wordnet:
			// add it to hashmap and return the stemmed word
			this.allWords.put( word, stemmedword );
			return stemmedword;
		}
		// word could not be stemmed by wordnet, 
		// thus it is no correct english word
		// just add it to the list of known words so 
		// we won't have to look it up again
		allWords.put(word, NON_VERB);
		return NON_VERB;
	}

	/**
	 * performs Stem on each element in the given Vector
	 * 
	 */
	public ArrayList<String> stem ( ArrayList<String> words )
	{
		if ( !isInitialized )
			return words;
		
		for ( int i = 0; i < words.size(); i++ )
		{
			words.set( i, stem( (String)words.get( i ) ) );
		}
		return words;		
	}
	
	public ArrayList<String> filterAndStem(String phrase){
		ArrayList<String> words = new ArrayList<String>(Arrays.asList(phrase.split(" ")));
		ArrayList<String> ret = new ArrayList<String>();
		
		if ( !isInitialized )
			return words;
		
		for (int i = 0; i < words.size(); i++)
		{
			// something about checking for verbs
			String s = this.stem(words.get(i));
			if (s != NON_VERB){
				ret.add(s);
			}
		}
		return ret;	
	}
	
	
	public static void main(String[] args){
		JWNLStemmer t = new JWNLStemmer();
		
		String s = "presented him with";
				
		System.out.println(t.filterAndStem(s));
		
	}
}