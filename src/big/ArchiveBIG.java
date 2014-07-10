/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-07T13:49:34Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: ArchiveBIG.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Provides access to a Big Information Gathered archive. </text> 
 */


package big;

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
public class ArchiveBIG {

    private int maxFileSize = 1000000 * 100; // max size = 100Mb 
    private Boolean isReady = false;
    private OutputStream outputStream = null;
    private BufferedWriter writer = null;
    // the main file associated to this object
    private File 
            fileTempBIG = null,
            fileMainBIG = null,
            fileIndexBIG = null;
    
    
    long currentPosition = 0;
    
    /**
     * Initialises a BIG archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param fileTarget    the file that we want to open 
     */
    public ArchiveBIG(final File fileTarget) {
        // do the proper assignments
        this.fileMainBIG = fileTarget;
        this.fileTempBIG = getNewFile("temp");
        this.fileIndexBIG = getNewFile("index");
                
        // ensure these files exist        
        existOrTouch(fileMainBIG);
        existOrTouch(fileTempBIG);
        existOrTouch(fileIndexBIG);

        
        System.out.println("ATDF47 Archive is ready to be used: " + fileMainBIG.getName());
        isReady = true;
    }

    /**
     * Provides a new file based on the main BIG file
     * @param name  The name to append on the extension
     * @return      A file pointer
     */
    private File getNewFile(final String name){
        return new File(fileMainBIG.getParentFile(), fileMainBIG.getName() + "-" + name);
    }
    
    /**
     * Check if a file exists, if doesn't exist try to create one.
     * @param file  the file to create
     * @return      true if it exists or was created, false if we fail to create one
     */
    private boolean existOrTouch(final File file){
        // does our archive already exists?
        if(file.exists() == false){
            // then create a new one
            files.touch(file);
            // did this worked?
            if(file.exists() == false){
                // we failed to create our file
                System.err.println("FTDF42 - Error creating empty archive: "
                 + file.getAbsolutePath());
                return false;
            }
        }
        return true;
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
        
        // open the index files
        filesOpen();
        // call the iteration to go through all files
        addFiles(folderToAdd, folderToAdd, 25); 
        // now close all the pointers
        filesClose();
    }
    
    /**
     * Opens the BIG file and respective index
     */
    private void filesOpen(){
      try {
            // open our archive file
            outputStream = new FileOutputStream(fileTempBIG, true);
            // open the respective index file
            writer = new BufferedWriter(
                new FileWriter(fileIndexBIG, true), 8192);
            
            // get other variables
            currentPosition = fileTempBIG.length();
            
        } catch (IOException ex) {
            Logger.getLogger(ArchiveBIG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Closes the pointers of our work files
     */
    private void filesClose(){
        try {
            
            outputStream.flush();
            writer.flush();
            outputStream.close();
            writer.close();
            
        } catch (IOException ex) {
            Logger.getLogger(ArchiveBIG.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        
        // calculate the SHA1 signature
        final String output = utils.thirdparty.Checksum.generateFileChecksum("SHA-1", fileToCopy);
        
        // write a new line in our index file
        writer.write(currentPosition + " "
                + output
                + " " + resultingPath + "\n");
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
                Logger.getLogger(ArchiveBIG.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
     
}
