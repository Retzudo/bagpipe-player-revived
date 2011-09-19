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
        Pattern p;
        Matcher m;
        
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
            if(line.length()>0) cleanBwwBuilder.append(line).append('\n');
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
             * First find the time signature (e.g. 4/4 or 6/8) and flats (but we won't
             * use them since bagpipes are always in A Mixolydian).
             */
            p=Pattern.compile("&.*(sharp[a-g])\\s+(\\d_\\d|C{1})\\s+?", Pattern.CASE_INSENSITIVE);
            m=p.matcher(cleanBww[i]);
            
            if(m.find()) {
                // Replace the _ with / and append it to the ABC string
                setMetronom(m.group(2).replace('_', '/'));
                // extract every symbol that remains in this line
                p=Pattern.compile("((?<=\\d_\\d\\s)[\\w'!\\^].*)", Pattern.CASE_INSENSITIVE);
                m=p.matcher(cleanBww[i]);
                if(m.find()) {
                    // append them to the string
                    appendToBwwMusic(m.group());
                }
                // then skip to the next cycle so that the information we just extracted
                // doesn't get into the BWW string
                continue;
            }
            
            // apparently this line contains no information besides music so append it
            appendToBwwMusic(cleanBww[i]);
        }
        
        computeAbcFromBww();
        
        /*
         * Always append 'K:' at the and of the head before music. Why? Dunno!
         */
        raw_abc.append("K:A Mixolydian\n");
    }
    
    /**
     * Adds the string s to the BWW string of the class and strips it of all
     * superfluous whitespaces before, after and between the usefull symbols
     * @param s String containing one or more symbols
     */
    private void appendToBwwMusic(String s) {
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
    
    /**
     * @return Returns the parsed BWW file as Tune
     */
    public Tune getTune(){
        return new TuneParser().parse(raw_abc.toString());
    }
    
    /**
     * @return Return the parsed BWW file as an ABC string
     */
    public String getABC(){
        return raw_abc.toString();
    }
}
