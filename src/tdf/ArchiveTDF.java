/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-07T13:49:34Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: ArchiveTDF.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Defines a TripleCheck Data Format file. </text> 
 */


package tdf;

import java.io.File;
import utils.files;

/**
 *
 * @author Nuno Brito, 7th of July 2014 in Darmstadt, Germany
 */
public class ArchiveTDF {

    private Boolean isReady = false;
    
    /**
     * Initialises a TDF archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param file 
     */
    public ArchiveTDF(final File file) {
        
        // does our archive already exists?
        if(file.exists() == false){
            // then create a new one
            files.touch(file);
            // did this worked?
            if(file.exists() == false){
                // we failed to create our file
                System.err.println("FTDF42 - Error creating empty archive: "
                 + file.getAbsolutePath());
                return;
            }
        }
        System.out.println("ATDF47 Archive open: " + file.getName());
        isReady = true;
    }

    /**
     * Is this object initialised and ready to be used?
     * @return True if ready, return False when something went wrong 
     */
    public Boolean isReady() {
        return isReady;
    }

    /**
     * Add all files from a given folder inside our archive
     * @param folderToAdd The folder whose files we want to add
     */
    public void addFolder(final File folderToAdd) {
        // preflight checks
        if(isReady == false){
            System.err.println("ATDF66 - Error, Archive is not ready");
            return;
        }
        // call the iteration to go through all files
        
        
    }
    
    
    
}
