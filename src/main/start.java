/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-07T13:17:11Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: start.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text> The starter for the TDF (TripleCheck Data Format)
    handler. </text> 
 */

package main;

import java.io.File;
import big.ArchiveBIG;

/**
 *
 * @author Nuno Brito, 7th of July 2014 in Darmstadt, Germany
 */
public class start {

    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final String testFolderLocation = "/media/nuno/main/core/code/source/7z922/";
        final File testFolder = new File(testFolderLocation);
        
        final String testArchive = "test.big";
        final File file = new File(testArchive);
        
        ArchiveBIG folder = new ArchiveBIG(file);
        // now add some files
        System.out.println("Adding files to " + testArchive);
        folder.addFolder(testFolder);
        
//        System.out.println("Extracting file");
//        File test = new File("test.txt");
//        folder.getFile("/C/HappyNuno.txt", test);
        
        System.out.println("Done");
        
    }

    
   
    
}
