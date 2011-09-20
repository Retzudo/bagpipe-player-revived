package bagpipeplayer;

import abc.notation.Tune;
import abc.parser.TuneParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Christoph Willinger
 */
public class Bww2tune {
    
    private String raw_bww;
    private StringBuilder raw_abc;
    private StringBuilder bwwMusic = new StringBuilder();;
    private boolean metronomSet=false;
    Tune t;
    
    /**
     * Constructor for BWW files as a File class
     * @param file BWW file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public Bww2tune(File file) throws FileNotFoundException, IOException {
        t = new Tune();
        FileInputStream stream = new FileInputStream(file);
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        raw_bww = Charset.defaultCharset().decode(bb).toString();
        stream.close();
        parse();
    }
    
    /**
     * Constructor for already read BWW files including linesbreaks and whatnot
     * @param string String containing the whole BWW file
     */
    public Bww2tune(String string) {
        t = new Tune();
        raw_bww=string;
        parse();
    }
    
    /**
     * Parses the BWW file into an ABC file
     */
    private void parse() {
        raw_abc = new StringBuilder();
        String [] cleanBww = null;
        int lineWithFirstQuote=0;
        
        /*
         * Append index number to ABC string. Bagpipe Player assumes there is only one tune per tunebook.
         */
        raw_abc.append("X:1\n");
        
        /*
         * Split the BWW file by lines for parsing
         */
        String [] lines = raw_bww.split("\\r?\\n");
        
        /*
         * Remove any empty lines
         */
        StringBuilder cleanBwwBuilder = new StringBuilder();
        for(String line : lines) {
            if(line.length()>0) cleanBwwBuilder.append(line.trim()).append('\n');
        }
        cleanBww = cleanBwwBuilder.toString().split("\\n");
        
        /*
         * Find the first appearance of an " because this is where the information we want starts
         */
        for(int l=0; l<cleanBww.length; l++) {
            if(cleanBww[l].charAt(0)=='"') {
                lineWithFirstQuote=l;
                break;
            }
        }
        
        
        /*
         * Get the title of the tune and discord the formatting information stored in the BWW file because we won't need it.
         * The procedure assumes that this information is stored always at the same line numger (i.e. line 9)
         */
        raw_abc.append("T:").append(cleanBww[lineWithFirstQuote].substring(cleanBww[lineWithFirstQuote].indexOf('"')+1, cleanBww[lineWithFirstQuote].lastIndexOf('"'))).append('\n');
        
        /*
         * same for the rythm (reel, jig etc.)
         */
        raw_abc.append("R:").append(cleanBww[lineWithFirstQuote+1].substring(cleanBww[lineWithFirstQuote+1].indexOf('"')+1, cleanBww[lineWithFirstQuote+1].lastIndexOf('"'))).append('\n');
        
        /*
         * and the Composer
         */
        raw_abc.append("C:").append(cleanBww[lineWithFirstQuote+2].substring(cleanBww[lineWithFirstQuote+2].indexOf('"')+1, cleanBww[lineWithFirstQuote+2].lastIndexOf('"'))).append('\n');
        
        /*
         * and lastly the history.
         * BagpipePlayer takes information like "arr. by yadda yadda" as history information.
         */
        raw_abc.append("H:").append(cleanBww[lineWithFirstQuote+3].substring(cleanBww[lineWithFirstQuote+3].indexOf('"')+1, cleanBww[lineWithFirstQuote+3].lastIndexOf('"'))).append('\n');
        
        /*
         * Now parse every line with musical information starting at the line after the "" lines
         */
        for(int i=lineWithFirstQuote+4; i<cleanBww.length; i++) {
            /*
             * Select every line beginning with an & (a clef in BWW language)
             */
            if(cleanBww[i].startsWith("&")) {
                Pattern p = null;
                Matcher m = null;
                /*
                 * Ok if the line beginns with a clef there probably is a beat and
                 * maybe already some music information. So we disect the string.
                 * ^& +(sharp[a-z]+ *)* --> matches the beginning of the string and
                 *                            those "sharps"
                 * (\\d_\\d|C)? * --> matches the beat which can be here.
                 * (.*)?$ --> matches everything else (if it is there) until the end of the line.
                 */
                p=Pattern.compile("^& +(sharp[a-z]+ *)*(\\d_\\d|C)? *(.*)?$");
                m=p.matcher(cleanBww[i]);
                
                if(m.find()) {
                    //System.out.println("group 1: ---"+m.group(1)+"---");
                    //System.out.println("group 2: ---"+m.group(2)+"---");
                    //System.out.println("group 3: ---"+m.group(3)+"---");
                    
                    /*
                     * If this line contains a beat, set it (only possible once).
                     */
                    if(m.group(2)!=null) {
                        setMetronom(m.group(2).replace('_', '/'));
                    }
                    
                    /*
                     * If this line contains anything else, assume that it is music
                     * and add it.
                     */
                    if(m.group(3)!=null) {
                        appendToBwwMusic(m.group(3));
                        appendToBwwMusic("\n");
                    }
                }
            } else {
                // apparently this line contains no information besides music so just append it
                appendToBwwMusic(cleanBww[i]);
                appendToBwwMusic("\n");
            }
        }
        
        computeAbcFromBww();
        
        /*
         * Always append 'K:' at the and of the head before music. Why? Dunno!
         */
        raw_abc.append("L:1/16\n");
        raw_abc.append("K:A Mixolydian\n");
    }
    
    /**
     * Adds the string s to the BWW string of the class and strips it of all
     * superfluous whitespaces before, after and between the usefull symbols
     * @param s String containing one or more symbols
     */
    private void appendToBwwMusic(String s) {
        if(s.matches("\n")) {
            bwwMusic.append(s);
            return;
        }
        String clean="";
        clean = s.trim();
        clean = clean.replaceAll("\\s{2,}", " ");
        clean = clean.replaceAll("\\t", " ");
        bwwMusic.append(clean).append(' ');
    }
    
    /**
     * @return Returns the musical information gathered from the BWW file as a String
     */
    public String getBwwMusic() {
        return bwwMusic.toString().trim();
    }
    
    /**
     * Sets the metronom once (ger. "Takt")
     * @param m String containing the measure (e.g. 4/4, 6/8 or C for Common Time)
     */
    private void setMetronom(String m) {
        if(!metronomSet) {
            raw_abc.append("M:").append(m).append("\n");
        }
        metronomSet=true;
    }
    
    private void computeAbcFromBww(){
        
    }
    
    private String replaceBwwSymbol(String s) {
        //High g grace note
        if(s.matches("|!''")) return "[|: ";
        if(s.matches("Cr_16")) return " c";
        if(s.matches("Bl_16")) return "B ";
        if(s.matches("gg")) return "{a}";
        return "";
    }
    
    private void appendToAbcMusic(String s) {
        bwwMusic.append(s);
    }
    
    /**
     * @return Returns the parsed BWW file as Tune
     */
    public Tune getTune(){
        return new TuneParser().parse(raw_abc.toString());
    }
    
    /**
     * @return Return the parsed BWW file as an ABC string
     */
    public String getAbc(){
        return raw_abc.toString();
    }
}
