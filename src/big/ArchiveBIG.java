/**
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-07T13:49:34Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: ArchiveBIG.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text>
 * 
 * This class provides the methods to add and read files from a BIG archive.
 * There is a common set of variables that are used for opening the stream of
 * data globally across the class. This is more efficient than opening the BIG
 * file every time we need to add something new but at the same time brings more
 * complexity in ensuring that each related file is ready for operation.
 * 
 * The risk of data corruption is very high. One single byte misplaced and the
 * whole archive is lost. Therefore, we write a line on the log stating that an
 * operation is in course. When the operation ends then another line on the log
 * will signal that everything was done with success. It will happen that
 * some time the process is interrupted before completing. When this is the case
 * then on the next operation will be noted that existed a pending operation.
 * The pending operation will be discarded completely to ensure that the archive
 * remains usable.
 * 
 * A second measure to prevent data corruption is the magic signature that also
 * serves as individual file separator. On one hand it permits other tools to
 * identify the type of data stored in the archive. On the other hand, if the
 * index is not available then you lose the path/name information of the files
 * but the data remains usable.
 * 
 * </text> 
 */


package big;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.files;
import utils.header;

/**
 *
 * @author Nuno Brito, 7th of July 2014 in Darmstadt, Germany
 */
public class ArchiveBIG {

    private final int maxFileSize = 1000000 * 100; // max size = 100Mb 
    private Boolean isReady = false;
    private OutputStream outputStream = null;
    private BufferedWriter writer = null;
    // the main file associated to this object
    private File 
            fileLogBIG = null,
            fileMainBIG = null,
            fileIndexBIG = null;
    
    long currentPosition = 0;
    
    // defines the magic number and recovery trigger for each stored file
    private final String magicSignature = "BIG81nb";
    
    /**
     * Initialises a BIG archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param fileTarget    the file that we want to open 
     */
    public ArchiveBIG(final File fileTarget) {
        // do the proper assignments
        this.fileMainBIG = fileTarget;
        this.fileLogBIG = getNewFile("log");
        this.fileIndexBIG = getNewFile("index");
                
        // ensure these files exist        
        existOrTouch(fileMainBIG, "");
        existOrTouch(fileLogBIG, "log");
        existOrTouch(fileIndexBIG, "index");
        
        System.out.println("BIG88 Archive is ready to be used: " + fileMainBIG.getName());
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
    private boolean existOrTouch(final File file, final String designation){
        // does our archive already exists?
        if(file.exists() == false){
            // then create a new one
            if(designation.isEmpty()){
                files.touch(file);
            }else{
                utils.files.SaveStringToFile(file, header.create(magicSignature
                    + "-" + designation
                    , "TripleCheck at http://github.com/triplecheck"));
            }
            // did this worked?
            if(file.exists() == false){
                // we failed to create our file
                System.err.println("BIG88 - Error creating file: "
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
            System.err.println("BIG137 - Error, Archive is not ready");
            return;
        }
        
        // open the index files
        operationStart(folderToAdd);
        // call the iteration to go through all files
        addFiles(folderToAdd, folderToAdd, 25); 
        // now close all the pointers
        operationEnd();
    }
    
    /**
     * Opens the BIG file and respective index
     */
    private void operationStart(final File folderToAdd){
      try {
            // open the BIG file where the binary data is stored
            currentPosition = fileMainBIG.length();
            // do we have any operation left incomplete?
            pointRestoreAndSave(folderToAdd);
            // open our archive file
            outputStream = new FileOutputStream(fileMainBIG, true);
            // open the file where we list the data, signatures and positions
            writer = new BufferedWriter(
                new FileWriter(fileIndexBIG, true), 8192);
            
            
        } catch (IOException ex) {
            Logger.getLogger(ArchiveBIG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Checks if we have a restore point that hasn't terminated with success
     * on a previous operation. If something went wrong on the previous run,
     * we will try to clean up things now. Results without success are discarded
     * from the BIG archive.
     */
    private void pointRestoreAndSave(final File folderToAdd){
        // get the last line from our log text file
        String lastLine = utils.files.getLastLine(fileLogBIG);
        // are we detecting that something went wrong?
        if((lastLine.isEmpty() == false) && (lastLine.startsWith("start:"))){
            System.out.println("BIG190 Something went wrong last time, we need to restore the last saved point!");
            // we need to restore the last saved point
            final String snippet = lastLine.substring(lastLine.indexOf(" ")+1);
            final String number = snippet.substring(0, snippet.indexOf(" "));
            long lastPosition = Long.parseLong(number);
            
            // try to return our knowledge base to the previous state
            utils.files.changeSize(fileMainBIG, lastPosition);
            if(lastPosition != fileMainBIG.length()){
                System.out.println("BIG197 - Failed to restore last saved point");
                System.exit(-1);
            }
            // we had success so, time to delete this info from the index
            deleteIndexDataAfterPosition(lastPosition);
            // update our index
            currentPosition = lastPosition;
            System.exit(-1);
        }
        // now add a line to record what we are doing
        utils.files.addTextToFile(fileLogBIG, "\n"
                + "start: "
                + utils.files.getPrettyFileSize(currentPosition) 
                + " "
                + utils.time.getDateTimeISO()
                + " -> "
                + folderToAdd.getAbsolutePath()
        );
    }
    
    /**
     * Looks at the data inside the index file, when we reach a file that
     * is bigger than the value specified as last position then we delete
     * all lines that come after that position, effectively deleting them.
     */
    private void deleteIndexDataAfterPosition(final long lastPosition){
        // prepare the keyword that we want to delete
        final String prettyNumber = utils.files.getPrettyFileSize(lastPosition);
        // cut the log file after the mentioned position
        utils.files.cutTextFileAfter(fileLogBIG, "start: " + prettyNumber);
        // cut the index file after the mentioned position
        utils.files.cutTextFileAfter(fileIndexBIG, prettyNumber + " ");
   }
    
    /**
     * Closes the pointers of our work files
     */
    private void operationEnd(){
        try {
            // flush all the remaining data onto the files
            outputStream.flush();
            writer.flush();
            
            // close the streams
            outputStream.close();
            writer.close();
            
            
             // now add a line to record what we are doing
            utils.files.addTextToFile(fileLogBIG, "\n"
                + "ended: "
                + utils.files.getPrettyFileSize(currentPosition) 
                + " "
                + utils.time.getDateTimeISO()
            );
            
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
        // add the magic number to this file block
        outputStream.write(magicSignature.getBytes());
        // now copy the whole file
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        // if there is something else to be flushed, do it now
        outputStream.flush();
        
        // calculate the base path
        final String basePath = baseFolder.getAbsolutePath();
        final String resultingPath = fileToCopy.getAbsolutePath().replace(basePath, "");
        
        // calculate the SHA1 signature
        final String output = utils.thirdparty.Checksum.generateFileChecksum("SHA-1", fileToCopy);
        
        // write a new line in our index file
        writer.write("\n" 
                + utils.files.getPrettyFileSize(currentPosition)
                + " "
                + output
                + " " + resultingPath);
        // increase the position counter
        currentPosition += fileToCopy.length() + magicSignature.length();
        
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
