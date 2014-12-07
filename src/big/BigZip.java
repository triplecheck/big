/**
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-07T13:49:34Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: BigZip.java  
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import utils.files;
import tools.header;

/**
 *
 * @author Nuno Brito, 7th of July 2014 in Darmstadt, Germany
 */
public class BigZip {

    // settings
    private int 
            // how big a file do we accept for storing?
            maxFileSize = 1000000 * 100; // default max size = 100Mb 
    
    // variables
    private Boolean isReady = false;
    private OutputStream outputStream = null;
    private BufferedWriter writer = null;
    // the main file associated to this object
    private File 
            fileLogBIG = null,
            fileMainBIG = null,
            fileIndexBIG = null;
    
    private long 
            currentPosition = 0,
            getNextFileCounter = 0;
    
    // defines the magic number and recovery trigger for each stored file
    private final String magicSignature = "BIG81nb";
    
    private String basePath = "";
    
    // variables used during the "next file" iterator
    private BufferedReader readerNextFile;
    private FileReader fileReaderNext;
    private long currentGetNextPosition = 0;

    private String 
            readerNextFileName,
            lastReadLine,
            currentLine;
    
      /**
     * Initialises a BIG archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param fileTarget    the file that we want to open 
     */
    public BigZip(final File fileTarget) {
        Start(fileTarget, false);
    }
  
    /**
     * Initialises a BIG archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param fileTarget    the file that we want to open 
     * @param silent        No initialisation messages are output
     */
    public BigZip(final File fileTarget, boolean silent) {
        Start(fileTarget, silent);
    }
    
    /**
     * Initialises a BIG archive. If the archive file doesn't exist yet then 
     * it will be created. You should check the isReady() method to verify
     * that the archive is ready to be used.
     * @param fileTarget    the file that we want to open 
     * @param silent        No initialisation messages are output
     */
    private void Start(final File fileTarget, boolean silent) {
        // do the proper assignments
        this.fileMainBIG = fileTarget;
        this.fileLogBIG = getNewFile("log");
        this.fileIndexBIG = getNewFile("index");
                
        // ensure these files exist        
        existOrTouch(fileMainBIG, "");
        existOrTouch(fileLogBIG, "log");
        existOrTouch(fileIndexBIG, "index");
        
        // prepare the initial message
        String message = "Archive is ready to be used: " 
                + fileMainBIG.getName();
        
        // shall we add the file size if above a given value?
        if(fileMainBIG.length() > 0){
            // add the size then
            message += " ("
                    + utils.files.humanReadableSize(fileMainBIG.length())
                    + ")";
        }
        // output the message
        if(silent == false){
            System.out.println(message);
        }
        // alld done
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
        // first check for the folder
        File folder = file.getParentFile();
        // do we have a folder defined?
        if(folder == null){
            folder = new File(".");
        }
        
        // does it exist?
        if(folder.exists() == false){
            // then create one
            utils.files.mkdirs(folder);
        }


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
                System.err.println("BIG201 - Error creating file: "
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
     * Add all files from a given folder inside our archive
     * @param fileToAdd The file we want to add
     */
    public void addFile(final File fileToAdd) {
        // preflight checks
        if(isReady == false){
            System.err.println("BIG241 - Error, Archive is not ready");
            return;
        }
        // open the index files
        operationStart(fileToAdd);
        // call the iteration to go through all files
        addFile(fileToAdd, fileToAdd); 
        // now close all the pointers
        operationEnd();
    }
    
    
    
    /**
     * Opens the BIG file and respective index
     */
    private void operationStart(final File folderToAdd){
      try {
            // if the base path is already set, don't change it
            if(basePath.isEmpty()){
                basePath = folderToAdd.getAbsolutePath();
            }
          
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
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Checks if we have a restore point that hasn't terminated with success
     * on a previous operation. If something went wrong on the previous run,
     * we will try to clean up things now. Results without success are discarded
     * from the BIG archive.
     */
    private void pointRestoreAndSave(final File folderToAdd){
        // get the last line from our log textWeird file
        String lastLine = utils.files.getLastLine(fileLogBIG);
        // are we detecting that something went wrong?
        if((lastLine.isEmpty() == false) && (lastLine.startsWith("start:"))){
            System.out.println("BIG290 Something went wrong last time, we need to restore the last saved point!");
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
        }
        // now add a line to record what we are doing
        utils.files.addTextToFile(fileLogBIG, "\n"
                + "start: "
                + utils.files.getPrettyFileSize(currentPosition) 
                + " "
                + utils.time.getDateTimeISO()
                + "->"
                + folderToAdd.getName()
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
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
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
    private boolean addFile(final File baseFolder, final File fileToCopy){
        
        // avoid files with size above our limits
        if(fileToCopy.length() > maxFileSize){
            // we reserve the false flag for exceptions
            return true;
        }
        
    // declare
        FileInputStream inputStream = null;
    try {
        // create the place holder for the zip file
        File fileZip = new File("temp.zip");
        // this file can't exist
        if(fileZip.exists()){
            fileZip.delete();
            // this file really can't exist
            if(fileZip.exists()){
                System.out.println("BIG305 - Failed to delete " + fileZip.getName());
                return false;
            }
        }

        // compress the file
        zip.compress(fileToCopy, fileZip);
        // use the zip file as inputstream
        inputStream = new FileInputStream(fileZip);
        
        byte[] buffer = new byte[8192];
        int length;
        // add the magic number to this file block
        outputStream.write(magicSignature.getBytes());
        // now copy the whole file into the BIG archive
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        // if there is something else to be flushed, do it now
        outputStream.flush();
        
        
        // calculate the base path
        final String resultingPath = fileToCopy.getAbsolutePath().replace(basePath, "");
        
        // calculate the SHA1 signature
        final String output = utils.thirdparty.Checksum.generateFileChecksum("SHA-1", fileToCopy);
        
        // write a new line in our index file
        writer.write("\n" 
                + utils.files.getPrettyFileSize(currentPosition)
                + " "
                + output
                + " " 
                + resultingPath
        );
        // increase the position counter
        currentPosition += fileZip.length() + magicSignature.length();
        
        // delete the zip file, we don't need it anymore
        fileZip.delete();
        
        
    } catch(IOException e){
        System.err.println("BIG346 - Error copying file: " + fileToCopy.getAbsolutePath());
        return false;
    }  
    
    finally {
        if(inputStream != null){
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    return true;
}

    /**
     * Define basePath, this is useful for cases where we want to index
     * files with several sublevels of folders to preserve URL information.
     * @param basePath 
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    /**
     * Looks inside our BIG archive to extract a specific file using the
     * path/name portion
     * @param fileToExtract The full path and name of the file
     * @param targetFile    The where we will be writing the result
     * @return True if we created a new file, false if we didn't found one or
     * something else went wrong.
     */
    public boolean getFile(final String fileToExtract, final File targetFile){
        // get the line where the file is located on our index
        long[] coordinates = getFileCoordinates(fileIndexBIG, fileToExtract);
        // did we found something?
        if(coordinates == null){
            return false;
        }
        // now extract the mentioned bytes from our BIG archive
        boolean result = extractBytes(targetFile, coordinates[0], coordinates[1]);
        System.out.println("BIG 489: Grabbing file from "
            + coordinates[0]
            + " to "
            + coordinates[1]
        );
        // all done
        return result;
    }
    
    
    /**
     * Given a position inside our knowledge base, retrieve the data up to
     * the next file indicator.
     * @param targetFile    The new file that will be created
     * @param startPosition The position from where we start to read the data
     * @param endPosition
     * @return 
     */
    public boolean extractBytes(final File targetFile, final long startPosition,
            final Long endPosition){
        /**
         * This is a tricky method. We will be extracting data from a the BIG
         * archive onto a new file somewhere on disk. The biggest challenge here
         * is to find exactly when the data for the file ends and still do the
         * file copy with a wonderful performance.
         */
        try {
            // enable random access to the BIG file (fast as heck)
            RandomAccessFile dataBIG = new RandomAccessFile(fileMainBIG, "r");
            // if the target file exists, try to delete it
            if(targetFile.exists()){
                targetFile.delete();
                if(targetFile.exists()){
                    // we failed completely
                    System.out.println("BIG405 - Failed to delete: " + targetFile.getAbsolutePath());
                    return false;
                }
            }
            // we need to create a temporary zip file holder
            File fileZip = new File("temp.zip");
            // delete the zip file if it already exists
            if(fileZip.exists()){
                fileZip.delete();
                if(fileZip.exists()){
                    // we failed completely
                    System.out.println("BIG416 - Failed to delete: " + fileZip.getAbsolutePath());
                    return false;
                }
            }
            
            // create a new file
            RandomAccessFile dataNew = new RandomAccessFile(fileZip, "rw");
            // jump directly to the position where the file is positioned
            dataBIG.seek(startPosition);
            // now we start reading bytes during the mentioned interval
            while(dataBIG.getFilePointer() < endPosition){
                // read a byte from our BIG archive
                int data = dataBIG.read();
                // write the same byte on the target file
                dataNew.write(data);
            }

            // close the file streams
            dataBIG.close();
            dataNew.close();
            
            // extract the file
            zip.extract(fileZip, new File("."));
            // delete the temp zip file
            fileZip.delete();
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
          
        return true;
    }
    
    
    /**
     * Version 2 that permits to extract the text from a compressed file without
     * creating any file on the disk.
     * @param startPosition Offset where the file begins
     * @param endPosition   Offset where the file ends
     * @return      The source code of the compressed file
     */
    public String extractBytesToRAM(final long startPosition, final Long endPosition){
        
        String result = null;
        
        try {
            // enable random access to the BIG file (fast as heck)
            RandomAccessFile dataBIG = new RandomAccessFile(fileMainBIG, "r");
            // jump directly to the position where the file is positioned
            dataBIG.seek(startPosition);
            // create a byte array
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

            //= new ByteArrayInputStream();
            // now we start reading bytes during the mentioned interval
            while(dataBIG.getFilePointer() < endPosition){
                // read a byte from our BIG archive
                int data = dataBIG.read();
                byteOutput.write(data);
            }
            // flush data at this point
            byteOutput.flush();
            // now convert the stream from input into an output (to feed the zip stream)
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            // where we place the decompressed bytes
            ByteArrayOutputStream textOutput = new ByteArrayOutputStream();
            // create the zip streamer
            final ArchiveInputStream archiveStream;
            archiveStream = new ArchiveStreamFactory().createArchiveInputStream("zip", byteInput);
            final ZipArchiveEntry entry = (ZipArchiveEntry) archiveStream.getNextEntry();
            // copy all bytes from one location to the other (and decompress the data)
            IOUtils.copy(archiveStream, textOutput);
            // flush the results
            textOutput.flush();
            // we've got the result right here!
            result = textOutput.toString();
            // now close all the streams that we have open
            dataBIG.close();
            byteOutput.close();
            byteInput.close();
            textOutput.close();
            archiveStream.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (ArchiveException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
          
        return result;
    }
    
    
    /**
     * Version 2 that permits to extract the text from a compressed file without
     * creating any file on the disk.
     * @param filePosition
     * @return      The source code of the compressed file
     */
    public String extractBytesToRAM(final long filePosition){
        
        String result = null;
        
        try {
            
            // add the signature bytes to our start position
            long startPosition = filePosition + magicSignature.length();
            
            // enable random access to the BIG file (fast as heck)
            RandomAccessFile dataBIG = new RandomAccessFile(fileMainBIG, "r");
            // jump directly to the position where the file is positioned
            dataBIG.seek(startPosition);
            // create a byte array
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

            // get the end of this file entry (by brute-force)
            char test = 0;
            long endPosition = -1;
            while(test != -1){
                test = dataBIG.readChar();
                // if the magic devil number was found..
                if(test == 66){
                    // read the next value for confirmation
                    byte value = dataBIG.readByte();
                    if(value != 73){
                        continue;
                    }
                    // we found the next entry
                    endPosition = dataBIG.getFilePointer() - 1;
                    break;
                }
            }
            
            // rewind back to the start position
            dataBIG.seek(startPosition);
            
            // now we start reading bytes during the mentioned interval
            while(dataBIG.getFilePointer() < endPosition){
                // read a byte from our BIG archive
                int data = dataBIG.read();
                byteOutput.write(data);
            }
            // flush data at this point
            byteOutput.flush();
            // now convert the stream from input into an output (to feed the zip stream)
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            // where we place the decompressed bytes
            ByteArrayOutputStream textOutput = new ByteArrayOutputStream();
            // create the zip streamer
            final ArchiveInputStream archiveStream;
            archiveStream = new ArchiveStreamFactory().createArchiveInputStream("zip", byteInput);
            final ZipArchiveEntry entry = (ZipArchiveEntry) archiveStream.getNextEntry();
            // copy all bytes from one location to the other (and decompress the data)
            IOUtils.copy(archiveStream, textOutput);
            // flush the results
            textOutput.flush();
            // we've got the result right here!
            result = textOutput.toString();
            // now close all the streams that we have open
            dataBIG.close();
            byteOutput.close();
            byteInput.close();
            textOutput.close();
            archiveStream.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (ArchiveException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
          
        return result;
    }
    
    /**
     * Looks inside a textWeird file to discover the line that contains a given
 keyword. When the line is discovered then it returns an array where
     * the first long represents the start of data and the second represents
     * its end.
     * @param file      A file on disk
     * @param keyword   A keyword that must be present on the file
     * @return          An array with the start and end position of a given
     * file inside our BIG archive. If we don't have a match, the result is NULL
     */
    private long[] getFileCoordinates(final File file, 
            final String keyword){
        // what we provide as answer
        long[] result = null;
        BufferedReader reader;
        try {
            FileReader fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            String line = "";
            while (line != null) {
                // do we have a match? Yes, let's proceed
                if(line.endsWith(keyword)){
                    // an example of what we are reading:
                    // 000000000180411 3f1f0990b8200b5e9b5de461a7fa7f7640ae16f7 /C/HappyNuno.txt
                    final String startValue = line.substring(0, 15);
                    // get the coordinate and ignore the magic signature size to get the raw binary contents
                    final long val1 = Long.parseLong(startValue) + magicSignature.length();
                    // now read the next line to get the end value
                    line = reader.readLine();
                    final String endValue = line.substring(0, 15);
                    final long val2 = Long.parseLong(endValue);
                    // deliver the value
                    result = new long[]{val1, val2};
                    break;
                }
                line = reader.readLine();
            }
            fileReader.close();
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(files.class.getName()).log(Level.SEVERE, null, ex);
        }
        // all done    
        return result;
    }

    /**
     * Given a specific SHA1 signature, go through the BIG archive and
     * file the files that have a matching value. This method will search
     * across the whole knowledge base. If more than one match is found, it will
     * be included on the list.
     * @param idSHA1    The SHA1 identifier to find
     * @return          A list of files found with this SHA1
     */
    public ArrayList<String> findFilesWithSpecificSHA1(final String idSHA1){
        // prepare the variable where we place the results
        ArrayList<String> result = new ArrayList();
        
        // open the file for reading
        getNextFileInitiate();
        
        String line;
        try {
            while( (line = readerNextFile.readLine()) !=  null){ 
                // get the SHA1 signature
                final String SHA1 = line.substring(16, 56);
                // no need to continue if no match exists
                if(utils.text.equals(SHA1, idSHA1)==false){
                    continue;
                }
                // ge the file name details after coordinate 57
                final String fileName = line.substring(57);
                // add this data to our result list
                result.add(fileName);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // close the files for reading
            getNextFileConclude();    
        }
        //System.out.println("BIG662 SHA1 search concluded");
        // all done
       return result;
    }
    
    
        
    public String findFileWithSpecificSHA1(final String signatureSHA1) {
        // open the file for reading
        getNextFileInitiate();
        String result = null;
        String line;
        try {
            while( (line = readerNextFile.readLine()) !=  null){ 
                // get the SHA1 signature
                final String SHA1 = line.substring(16, 56);
                // no need to continue if no match exists
                if(utils.text.equals(SHA1, signatureSHA1)==false){
                    continue;
                }
                // ge the file name details after coordinate 57
                final String fileName = line.substring(57);
                // add this data to our result list
                result = fileName;
                break;
            }
            
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // close the files for reading
            getNextFileConclude();    
        }
        // all done
       return result;
    } 
    
    /**
     * Prepares this archive to iterate all files sequentially
     */
    public void getNextFileInitiate(){
        // we start by initiating the file readers
        try {
            fileReaderNext = new FileReader(fileIndexBIG);
            readerNextFile = new BufferedReader(fileReaderNext);
            // avoid the header line
            readerNextFile.readLine();
            // now avoid the first file because we know its offset is 0000
            lastReadLine = readerNextFile.readLine();
            currentLine = lastReadLine;
            readerNextFileName = getFileNameOutOfLine(lastReadLine);
            
            } catch (FileNotFoundException ex) {
            Logger.getLogger(files.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Releases the allocated resources required for running this operation
     */
    public void getNextFileConclude(){
        // closes the streams previously open
        try {
            if(fileReaderNext != null)
                fileReaderNext.close();
            if(readerNextFile != null)
                readerNextFile.close();
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
    
    /**
     * Starting from the first file, this method permits to iterate over all
     * the files inside a big archive.
     * @return a pointer to the extracted file on disk
     * @throws java.io.IOException when the file had some error 
     */
    public File getNextFile() throws IOException {
            lastReadLine = currentLine;
            // now get the next line
            currentLine = readerNextFile.readLine();
            // increase the counter
            getNextFileCounter++;
            // get the new coordinate
            final long newValue = getValueOutOfLine(currentLine);
            
            // define the file pointer that we will be using
            final File file = new File(readerNextFileName);
            // now extract the mentioned bytes from our BIG archive
            extractBytes(file, currentGetNextPosition 
                    + magicSignature.length(), newValue);
        
            // now update the marker for the present offset
            currentGetNextPosition = newValue;
            readerNextFileName = getFileNameOutOfLine(currentLine);
            // all done
       return file;
    }

    /**
     * Returns the last line that was read while iterating the files inside
     * a big archive in sequential mode
     * @return The full line as available on the textWeird file
     */
    public String getLastLine() {
        return lastReadLine;
    }

    /**
     * How many files were indexed with this sequential processing?
     * @return 
     */
    public long getGetNextFileCounter() {
        return getNextFileCounter;
    }
    
    
    
    /**
     * Given a line describing a file, get the file name portion
     * @param line  A line from our index file
     * @return      The file name. Errors are ignored intentionally to permit
     *              scale and faster processing speed.
     */
    private String getFileNameOutOfLine(final String line){
        // get the last path indicator
        final int i1 = line.lastIndexOf("/");
        // provide the name portion of the file
        return line.substring(i1+1);
    }
    
    /**
     * Given a line describing a file in our big archive, get the coordinate value
     * @param line
     * @return      A long with the value where the data can be found
     */
    private long getValueOutOfLine(final String line){
        // get the first values with the coordinate
        final String startValue = line.substring(0, 15);
        // get the coordinate and ignore the magic signature
        return Long.parseLong(startValue);
    }
    
    /**
     * Tries to jump directly to the last position from where processing took place
     * @param offsetPosition
     * @param linesProcessed 
     */
    public void moveToOffsetPosition(final long offsetPosition, final long linesProcessed){
         try {
            // attempt to skip a given number of bytes
            readerNextFile.skip(offsetPosition);
            getNextFileCounter = linesProcessed;
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }
    
    /**
     * Skip a given number of files until we get the next pointer to reader.
     * @param currentLine   The line number that was counted up to that point
     */
    public void moveToLinePosition(final long currentLine){
//        try {
//            // attempt to skip a given number of bytes
//            readerNextFile.skip(nextPosition);
//            getNextFileCounter = currentLine;
//            this.currentGetNextPosition = currentGetNextPosition;
//        } catch (IOException ex) {
//            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        try {
            // ignore all these lines until we can resume
            while(getNextFileCounter < currentLine){
                   emptyLineRead();
            }
        } catch (IOException ex) {
            Logger.getLogger(BigZip.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Does a simple line read to advance the counters
     */
    private void emptyLineRead() throws IOException{
        readerNextFile.readLine();
        getNextFileCounter++;
    }
    
    /**
     * Starting from the first file, this method permits to iterate over all
     * the files inside a big archive.
     * @return a pointer to the extracted file on disk
     * @throws java.io.IOException when the file had some error 
     */
    public String getNextSourceCodeFile() throws IOException {
            lastReadLine = currentLine;
            // now get the next line
            currentLine = readerNextFile.readLine();
            // increase the counter
            getNextFileCounter++;
            // get the new coordinate
            final long newValue = getValueOutOfLine(currentLine);
            
            // now extract the mentioned bytes from our BIG archive
            final String result = extractBytesToRAM(currentGetNextPosition 
                    + magicSignature.length(), newValue);
        
            // now update the marker for the present offset
            currentGetNextPosition = newValue;
            readerNextFileName = getFileNameOutOfLine(currentLine);
            // all done
       return result;
    }

    /**
     * Close the big archive and all open files associated with it
     */
    public void close() {
        getNextFileConclude();
    }

    public File getFileLog() {
        return fileLogBIG;
    }

    public File getFile() {
        return fileMainBIG;
    }

    public File getFileIndex() {
        return fileIndexBIG;
    }
   
    public long getCurrentGetNextPosition() {
        return currentGetNextPosition;
    }

    public String getCurrentLine() {
        return currentLine;
    }

    /**
     * Sets the maximum size accepted as a file for storage.
     * @param maxFileSizeBigZip 
     */
    public void setFileSizeLimit(final int maxFileSizeBigZip) {
        maxFileSize = maxFileSizeBigZip;
    }
    
}
