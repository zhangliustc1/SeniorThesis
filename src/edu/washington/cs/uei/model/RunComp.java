package edu.washington.cs.uei.model;

// Stanley's string comparison metric

import java.io.*;
import com.wcohen.ss.Levenstein;
import com.wcohen.ss.MongeElkan;
import com.wcohen.ss.JaroWinklerTFIDF;
import com.wcohen.ss.TFIDF;
import com.wcohen.ss.SoftTFIDF;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.tokens.SimpleTokenizer;
import com.wcohen.ss.api.Token;

public class RunComp
{
	private JaroWinklerTFIDF comp = new JaroWinklerTFIDF();
	private boolean isJWTFIDF = true;
	private double ACRONYM_MULT = 0.9;
	
    private static String getCapInitialAsWord(String str)
    {
        char[] charArr = new char[str.length()+1];
        int cnt = 0;
        for (int i = 0; i < str.length(); i++)
        {
            if (i == 0 || str.charAt(i-1)==' ')
            {
                    
                if (Character.isUpperCase( str.charAt(i) ))
                {
                    
                    if (i+1 < str.length() && 
                        !Character.isUpperCase( str.charAt(i+1) ))
                    {
                        charArr[cnt++] = str.charAt(i);
                        while(str.charAt(i++) != ' ' && i < str.length());
                        i--;
                    }
                    else
                    {
                        if (i > 0) charArr[cnt++] = ' ';
                        charArr[cnt++] = str.charAt(i);
                        while(str.charAt(i++) != ' ' && i < str.length())
                            charArr[cnt++] = str.charAt(i);
                        i--;
                    }
                        
                }
            }
        }
        return new String(charArr, 0, cnt);
    }

    private static String removePeriod(String str)
    {
        str = str.trim();
        char[] charArr = new char[str.length()];
        int cnt = 0;
        for (int i = 0; i < str.length(); i++)
            if (str.charAt(i) != '.')
                charArr[cnt++] = str.charAt(i);
        return new String(charArr, 0, cnt);
    }
    
    private static boolean isOneWordAllCaps(String str)
    {
        str = str.trim();
        for (int i = 0; i < str.length(); i++)
            if (!Character.isUpperCase(str.charAt(i))) 
                return false;
        return true;
    }
    
    public double getSimilarity(String string0, String string1, boolean isEntity) {
        String str0 = removePeriod(string0);
        String str1 = removePeriod(string1);
        boolean oneWordAllCaps0 = isOneWordAllCaps(string0);
        boolean oneWordAllCaps1 = isOneWordAllCaps(string1);

        boolean matchAcronym = false;
        if ( (oneWordAllCaps0 && !oneWordAllCaps1) ||
             (!oneWordAllCaps0 && oneWordAllCaps1) )
        {
            matchAcronym = true;
            if (oneWordAllCaps0)
            {
                str0 = string0;
                str1 = getCapInitialAsWord(str1);
            }
            else
            if (oneWordAllCaps1)
            {
                str0 = getCapInitialAsWord(str0);
                str1 = string1;
            }
        }

        StringWrapper s0 = comp.prepare(str0);
        StringWrapper s1 = comp.prepare(str1);
        double score = comp.score(s0,s1);                      

        if (matchAcronym) score *= ACRONYM_MULT;

          //NOTE: this overcomes a bug in SecondString
        if (isJWTFIDF && score > 1)  
        {
            score = comp.score(s1,s0);
            assert(score <= 1);
        }
        
        return score;
    }


    public static void main(String[] args) throws Exception
    {
        if (args.length != 3)
        {
            String s = "usage: RunComp <in pair file> <out pair file> ";
            s += " <adjust score>";
            System.out.println(s);
            System.exit(-1);
        }
        
        double ACRONYM_MULT = 0.9;
        String strPairFile = args[0];
        String strPairScoreFile = args[1];
        boolean adjust = (args[2].compareTo("true")==0);
        //String strPairFile = "/homes/gws/koks/mrc/arg0Pair.txt";
        //String strPairScoreFile = "arg0PairScore-adj.txt";
        //boolean adjust = true;

        //MongeElkan comp = new MongeElkan();
        //comp.setScaling(true);
        //Levenstein comp = new Levenstein();
        //boolean isJWTFIDF = false;
        //TFIDF comp = new TFIDF();
        JaroWinklerTFIDF comp = new JaroWinklerTFIDF();
        boolean isJWTFIDF = true;
        boolean score0To1 = true;

        /*boolean ignorePunc = true, ignoreCase = false;
        SimpleTokenizer tk = new SimpleTokenizer(ignorePunc, ignoreCase);
        JaroWinkler jw = new JaroWinkler();
        double thresh = 0.9; //0.95
        SoftTFIDF comp = new SoftTFIDF(tk, jw, thresh);//*/


        BufferedReader in = new BufferedReader(new FileReader(strPairFile));
        BufferedWriter out=new BufferedWriter(new FileWriter(strPairScoreFile));
        String line;
        while ((line = in.readLine()) != null)
        {
            //System.out.println(line);
            if (line.trim().length() == 0) continue;
            int idx = line.indexOf("::::");
            String string0 = line.substring(0,idx-1).trim();
            String string1 = line.substring(idx+4,line.length()).trim();

            String str0 = removePeriod(string0);
            String str1 = removePeriod(string1);
            boolean oneWordAllCaps0 = isOneWordAllCaps(string0);
            boolean oneWordAllCaps1 = isOneWordAllCaps(string1);

            boolean matchAcronym = false;
            if ( (oneWordAllCaps0 && !oneWordAllCaps1) ||
                 (!oneWordAllCaps0 && oneWordAllCaps1) )
            {
                matchAcronym = true;
                if (oneWordAllCaps0)
                {
                    str0 = string0;
                    str1 = getCapInitialAsWord(str1);
                }
                else
                if (oneWordAllCaps1)
                {
                    str0 = getCapInitialAsWord(str0);
                    str1 = string1;
                }
            }

            StringWrapper s0 = comp.prepare(str0);
            StringWrapper s1 = comp.prepare(str1);
            double score = comp.score(s0,s1);                      

            if (matchAcronym) score *= ACRONYM_MULT;

              //NOTE: this overcomes a bug in SecondString
            if (isJWTFIDF && score > 1)  
            {
                score = comp.score(s1,s0);
                assert(score <= 1);
            }
            if (adjust) score = (20*score+1)/(20+5);
            boolean output = true;

            if (score0To1)
            {
                if (score > 1.0) score = 1.0;
                if (score > 0)
                {
                    String outStr = line + " :::: " + score + "\n";
                    out.write(outStr);
                }
            }
        }
        in.close();
        out.close();
    }
}
