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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.files;

/**
 *
 * @author Nuno Brito, 7th of July 2014 in Darmstadt, Germany
 */
public class ArchiveTDF {

    private int maxFileSize = 1000000 * 100; // max size = 100Mb 
    private Boolean isReady = false;
    OutputStream outputStream = null;
    BufferedWriter writer = null;
    
    long currentPosition = 0;
    
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
         // does our index file already exists?
        File fileIndex = new File(file.getParentFile(), file.getName() + "-index");
        if(fileIndex.exists() == false){
            // then create a new one
            files.touch(fileIndex);
            // did this worked?
            if(fileIndex.exists() == false){
                // we failed to create our file
                System.err.println("FTDF66 - Error creating empty index archive: "
                 + fileIndex.getAbsolutePath());
                return;
            }
        }
        
        
        
        try {
            // open our archive file
            outputStream = new FileOutputStream(file, true);
            // open the respective index file
            writer = new BufferedWriter(
                new FileWriter(fileIndex, true), 8192);
            
            // get other variables
            currentPosition = file.length();
            
        } catch (IOException ex) {
            Logger.getLogger(ArchiveTDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("ATDF47 An archive was open: " + file.getName());
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
       addFiles(folderToAdd, folderToAdd, 25); 
    }
    
    
    /**
     * Find all files in a given folder and respective sub-folders
     * @param where A file object of the start folder
     * @param maxDeep How deep is the crawl allowed to proceed
     * @return An array containing all the found files, returns null if none is
     * found
     */
     private void addFiles(final File baseFolder, final File where, int maxDeep){
        // list the files on the current directory 
        File[] files = where.listFiles();
        // no need to continue if nothing was found
        if(files == null){
            return;
        }
        // go through each file
        for (File file : files) {
            if (file.isFile()){
                // Add the file to our archive
                addFile(baseFolder, file);
            }
            else
                if ( (file.isDirectory())
                        &&( maxDeep-1 > 0 ) ){
                    // do the recursive crawling
                    addFiles(baseFolder, file, maxDeep-1);
                }
        }
     }
    
     
    /**
     * Copies one file into the big archive
     */ 
    private void addFile(final File baseFolder, final File fileToCopy){
    // declare
        FileInputStream inputStream = null;
    try {
        inputStream = new FileInputStream(fileToCopy);
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        // calculate the base path
        final String basePath = baseFolder.getAbsolutePath();
        final String resultingPath = fileToCopy.getAbsolutePath().replace(basePath, "");
        
        // write a new line in our index file
        writer.write(currentPosition + " " + resultingPath + "\n");
        // increase the position counter
        currentPosition += fileToCopy.length();
        
    } catch(IOException e){
        System.err.println("ATDF134 - Error copying file: " + fileToCopy.getAbsolutePath());
        System.exit(1);
    }
    
    finally {
        if(inputStream != null){
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(ArchiveTDF.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
     
}
