package edu.rhit.tools;

import org.apache.commons.codec.language.RefinedSoundex;

public class Sndx implements Comparable<Sndx> {
	private String actual;
	private String soundex;
	
	public Sndx(String s){
		RefinedSoundex rs = new RefinedSoundex();
		actual = s;
		soundex = rs.encode(s);
	}

	public String getActual() {
		return actual;
	}

	public String getSoundex() {
		return soundex;
	}

	@Override
	public int compareTo(Sndx arg0) {

		return soundex.compareTo(arg0.getSoundex());
	}

	@Override
	public String toString() {
		return soundex;
	}
	
	
}
