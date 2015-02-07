/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-11-26T23:29:05Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: sanitize.java
 * FileCopyrightText: <text> Copyright 2013 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Creates a new copy of a big zip. If any inconsistencies
    are found, tries to solve them.</text>
 */

package tools;

import java.io.File;
import utils.ReadWrite.deprecated.FileReadLinesBatch;

/**
 *
 * @author Nuno Brito, 11th of November 2014 in Darmstadt, Germany
 */
public class sanitize extends FileReadLinesBatch{

    // settings
    static private final String fileLocationBig = "../../kb/storage/Java.big-index";
    
    // internal variables
    FileReadLinesBatch fileRead;

    public sanitize(File textFileTarget) {
        super(textFileTarget);
    }

     @Override
    public void processTextLine(String sourceCode) {
        // soon to have here our code to handle the text line
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        sanitize sane = new sanitize(new File(fileLocationBig));
        sane.processTextFile();
    }

    @Override
    public void monitorMessage() {
        // get the counter of lines
        long counterLines = getCurrentLine();
        // get the number properly formatted
        final String valueLines 
            = utils.text.convertToHumanNumbers(counterLines);
        // output the number of lines already read
        System.out.println(valueLines + " lines");
    }
    
}
