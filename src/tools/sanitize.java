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
import java.io.IOException;
import utils.model.FileReadLines;

/**
 *
 * @author Nuno Brito, 11th of November 2014 in Darmstadt, Germany
 */
public class sanitize extends FileReadLines{

    // settings
    static private final String fileLocationBig = "../../kb/storage/Java.big";
    
    // internal variables
    FileReadLines fileRead;

    public sanitize(File textFileTarget) {
        super(textFileTarget);
    }

     @Override
    public void processSourceCode(String sourceCode) {
    }
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        sanitize sane = new sanitize(new File(fileLocationBig));
        sane.processArchive();
    }

   
    
}
