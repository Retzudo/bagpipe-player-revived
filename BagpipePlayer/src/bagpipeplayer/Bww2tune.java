package bagpipeplayer;

import abc.notation.BarLine;
import abc.notation.Decoration;
import abc.notation.EndOfStaffLine;
import abc.notation.KeySignature;
import abc.notation.MeasureRepeat;
import abc.notation.Note;
import abc.notation.NotesSeparator;
import abc.notation.RepeatBarLine;
import abc.notation.RepeatEnd;
import abc.notation.TimeSignature;
import abc.notation.Tune;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Christoph Willinger
 */
public class Bww2tune {
    
    private String raw_bww;
    private ArrayList<String> bwwMusic = new ArrayList();
    //private ArrayList<Note> abcMusic = new ArrayList();
    private Tune abcMusic = new Tune();
    private boolean metronomSet=false;
    private String voice = "Bagpipe";
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
        String [] cleanBww = null;
        int lineWithFirstQuote=0;
        
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
        t.addTitle(cleanBww[lineWithFirstQuote].substring(cleanBww[lineWithFirstQuote].indexOf('"')+1, cleanBww[lineWithFirstQuote].lastIndexOf('"')));
        
        /*
         * same for the rythm (reel, jig etc.)
         */
        t.setRhythm(cleanBww[lineWithFirstQuote+1].substring(cleanBww[lineWithFirstQuote+1].indexOf('"')+1, cleanBww[lineWithFirstQuote+1].lastIndexOf('"')));
        
        /*
         * and the Composer
         */
        t.addComposer(cleanBww[lineWithFirstQuote+2].substring(cleanBww[lineWithFirstQuote+2].indexOf('"')+1, cleanBww[lineWithFirstQuote+2].lastIndexOf('"')));
        
        /*
         * and lastly the history.
         * BagpipePlayer takes information like "arr. by yadda yadda" as history information.
         */
        t.addHistory(cleanBww[lineWithFirstQuote+3].substring(cleanBww[lineWithFirstQuote+3].indexOf('"')+1, cleanBww[lineWithFirstQuote+3].lastIndexOf('"')));
        
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
                        addToBwwMusic(m.group(3));
                    }
                }
            } else {
                // apparently this line contains no information besides music so just append it
                addToBwwMusic(cleanBww[i]);
            }
        } 
        /*
         * 
         * Always append 'K:' at the and of the head before music. Why? Dunno!
         */
        t.getMusic().addElement(voice, new KeySignature(Note.A, KeySignature.MIXOLYDIAN));
        t.getMusic().addElement(voice, new BarLine(BarLine.REPEAT_OPEN));
        Note bla = new Note(Note.G);
        bla.setStrictDuration(Note.HALF);
        t.getMusic().addElement(voice, bla);
        t.getMusic().addElement(voice, new Note(Note.G));
        t.getMusic().addElement(voice, new Note(Note.A));
        t.getMusic().addElement(voice, new Note(Note.B));
        t.getMusic().addElement(voice, new Note(Note.c));
        t.getMusic().addElement(voice, new BarLine());
        t.getMusic().addElement(voice, new Note(Note.d));
        t.getMusic().addElement(voice, new Note(Note.e));
        t.getMusic().addElement(voice, new Note(Note.f));
        t.getMusic().addElement(voice, new Note(Note.g));
        t.getMusic().addElement(voice, new Note(Note.a));
        t.getMusic().addElement(voice, new BarLine(BarLine.REPEAT_CLOSE));
        t.getMusic().addElement(voice, new EndOfStaffLine());
        replaceAndAddBwwSymbol("LG_8");
        replaceAndAddBwwSymbol("LAr_16");
        replaceAndAddBwwSymbol("'la");
        replaceAndAddBwwSymbol("Bl_32");
        
        // replaceAndAddBwwSymbol();
    }
    
    /**
     * Adds the string s to the BWW string of the class and strips it of all
     * superfluous whitespaces before, after and between the usefull symbols
     * @param s String containing one or more symbols
     */
    private void addToBwwMusic(String s) {
        if(s.matches("\n")) {
            bwwMusic.add(s);
            return;
        }
        String clean="";
        clean = s.trim();
        clean = clean.replaceAll("\\s{2,}", " ");
        clean = clean.replaceAll("\\t", " ");
        bwwMusic.add(clean+" ");
    }
    
    
    /**
     * @return Returns the musical information gathered from the BWW file as a String
     */
    
    public String getBwwMusicAsString() {
        StringBuilder s = new StringBuilder();
        for(String e : bwwMusic) {
            s.append(e).append(" ");
        }
        return s.toString().trim();
    }
    
    /**
     * Sets the metronom once (ger. "Takt")
     * @param m String containing the measure (e.g. 4/4, 6/8 or C for Common Time)
     */
    private void setMetronom(String m) {
        if(!metronomSet) {
            t.getMusic().addElement(voice, TimeSignature.SIGNATURE_4_4);
        }
        metronomSet=true;
    }
    
    /**
     * Replaces a BWW symbol with a ABC symbol
     * @param s String containing the BWW symbol
     * @return Returns an ABC symbol
     */
    private void replaceAndAddBwwSymbol(String s) {
        Note n;
        Pattern p = Pattern.compile("([HL]?[ABCDEFG])(r|l)?_(\\d+)"); // Matches any BWW note
        Matcher m = p.matcher(s);
        short noteDuration = 0;
        // If this is a note
        System.out.println("Current symbol: "+s);
        if(m.find()) {
            /*
             * Get the duration of the note
             */
            System.out.println("Symbol must be a note.");
            noteDuration = Short.parseShort(m.group(3));
            
            /*
             * Note replacement
             */
            n = new Note(Note.c);
            if(m.group(1).matches("LG")) n = new Note(Note.G);
            if(m.group(1).matches("LA")) n = new Note(Note.A);
            if(m.group(1).matches("B")) n = new Note(Note.B);
            if(m.group(1).matches("C")) n = new Note(Note.c);
            if(m.group(1).matches("D")) n = new Note(Note.d);
            if(m.group(1).matches("E")) n = new Note(Note.e);
            if(m.group(1).matches("F")) n = new Note(Note.f);
            if(m.group(1).matches("HG")) n =  new Note(Note.g);
            if(m.group(1).matches("HA")) n = new Note(Note.a);
            
            switch(noteDuration) {
                case 32: n.setStrictDuration(Note.THIRTY_SECOND); break;
                case 16: n.setStrictDuration(Note.SIXTEENTH); break;
                case 8: n.setStrictDuration(Note.EIGHTH); break;
                case 4: n.setStrictDuration(Note.QUARTER); break;
                case 2: n.setStrictDuration(Note.HALF); break;
                case 1: n.setStrictDuration(Note.WHOLE); break;
            }
            
            t.getMusic().addElement(voice, n);
            
            /*
             * See of l or r was in the note's string and add a note seperator if not
             */
            if(m.group(2)==null) {
                System.out.println("Adding separator!");
                t.getMusic().addElement(voice, new NotesSeparator());
            }
            
        } else if(s.startsWith("'")) {
            byte dots=1;
            /*
             * If it is a dot for the last note get the last note from the
             * Tune, add one or two dots and append it again.
             */
            if(s.charAt(1)=='\'') dots++;
            t.getMusic().getVoice(voice).getLastNote().setDotted(dots);
        } else if(s.startsWith("^")) {
            /*
             * If is a group or a tie see what exactly it is.
             * 1) It could be a triplet in the "old format" (according to the Bagpipe Player documentation)
             *    which comes after the three notes it stands for containing
             *    the highest note it has to go over (e.g. ^3hg means "the last 3 notes were a triplet with
             *    an High G at its peak").
             * 2) It could be a "new format" group which starts with ^3s and ends with ^3e. Thpse two symbols
             *    enclose their notes which they stand for, e.g. ^3s LA_8 LA_8 LA_8 ^3e.
             * 3) It could be a tie which is in between two tied notes. E.g. LG_2 ^tlg LG_2.
             */
        }
        
        
        /*
         * RAW CONVERSIONS
         */
        
        // repeated part
        if(s.matches("I!''")) t.getMusic().addElement(voice, new MeasureRepeat(1));
        // end of repeated part
        if(s.matches("''!I")) t.getMusic().addElement(voice, new RepeatEnd());
        // bar line
        if(s.matches("!")) t.getMusic().addElement(voice, new BarLine());
        // end of line
        if(s.matches("!t")) t.getMusic().addElement(voice, new EndOfStaffLine());
        // a 1/16 c 
        
        System.out.println();
    }
    
    /**
     * @return Returns the parsed BWW file as Tune
     */
    public Tune getTune(){
        //return new TuneParser().parse(raw_abc.toString());
        return t;
    }
}
