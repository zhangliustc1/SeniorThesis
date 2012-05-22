package edu.rhit.tools;

import org.apache.commons.codec.EncoderException;
//import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.RefinedSoundex;
 
import uk.ac.shef.wit.simmetrics.similaritymetrics.Soundex;;


public class SoundexTest {
	public static void main(String[] args){
		Soundex s = new Soundex();
		RefinedSoundex rs = new RefinedSoundex();
		try {
//			System.out.println(s.difference("Stephen", "Baclkbhekj"));
//			
//			System.out.println(s.encode("lacks the"));
//			System.out.println(s.encode("lacks te"));
			//System.out.println(s.getSimilarityExplained("the rain in spain falls mainly on the plain", "the rine in spine falls minly on the pline"));
			
			
			System.out.println(rs.difference("Stephen", "Baclkbhekj"));
			System.out.println(rs.difference("the rain in spain falls mainly on the plain", "the rine in spine falls minly on the pline"));
			
			System.out.println(rs.encode("lacks the"));
			System.out.println(rs.encode("lacks te"));
			
			System.out.println(rs.encode("Astounding"));
			System.out.println(rs.encode("Astunding"));
			

			System.out.println(rs.soundex("the rain in spain falls mainly on the plain"));
			System.out.println(rs.soundex("the rine in spin falls minly on the plin"));
			
		} catch (EncoderException e) {
			e.printStackTrace();
		}
	}
}
