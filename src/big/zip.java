/**
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-07-12T09:55:42Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: zip.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text>
 * 
 * Compress files on disk to the Zip format using the Apache Common Compress
 * library. When included inside the BIG archive, files are compressed using
 * the standard zip algorithm. In practice, it is possible to extract the data
 * for each binary block, place the result on a file and rename it with extension
 * .zip to open with any standard tooling.
 * 
 * The code is placed on this class to ease identifying and improving the code
 * related to this compression/decompression phase inside BIG archives.
 * 
 * Each binary block (file) is compressed using its own zip file. There is
 * an increase of size due to the overhead requiring for creating many small
 * zip files and a loss of size-saving that could be won by merging multiple
 * files inside a zip archive. However, we need each file to be independent in
 * order to reach them on-demand and the overall result is still better than
 * simply storing a non-compressed text file, which is our target file to store.
 * 
 * One positive side effect of using a zip container is that we retain the
 * file information such as original file name and time stamp.
 * </text> 
 */


package big;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

/**
 *
 * @author Nuno Brito, 12th of July 2014 in Darmstadt, Germany
 */
public class zip {

    /**
     * 
     * @param fileToCompress    The file that we want to compress
     * @param fileToOutput      The zip file containing the compressed file
     * @return  True if the file was compressed and created, false when something
     * went wrong
     */
    public static boolean compress(final File fileToCompress, final File fileToOutput){
        if(fileToOutput.exists()){
            // do the first run to delete this file
            fileToOutput.delete();
            // did this worked?
            if(fileToOutput.exists()){
                // something went wrong, the file is still here
                System.out.println("ZIP59 - Failed to delete output file: "
                + fileToOutput.getAbsolutePath());
                return false;
            }
        }
        // does our file to compress exist?
        if(fileToCompress.exists() == false){
            // we have a problem here
            System.out.println("ZIP66 - Didn't found the file to compress: "
            + fileToCompress.getAbsolutePath());
            return false;
        }
        // all checks are done, now it is time to do the compressing
        try{
            final OutputStream outputStream = new FileOutputStream(fileToOutput);
            ArchiveOutputStream archive = new ArchiveStreamFactory()
                .createArchiveOutputStream("zip", outputStream);
            archive.putArchiveEntry(new ZipArchiveEntry(fileToCompress.getName()));
            // create the input file stream and copy it over to the archive
            FileInputStream inputStream = new FileInputStream(fileToCompress);
            IOUtils.copy(inputStream, archive);
            // close the archive
            archive.closeArchiveEntry();
            archive.flush();
            archive.close();
            // now close the input file stream
            inputStream.close();
            // and close the output file stream too
            outputStream.flush();
            outputStream.close();
       
        } catch (FileNotFoundException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ArchiveException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } 
        
     return true;
    }
 
    /**
     * Pick a file on disk, compress it using the ZIP algorithm and write the
     * results directly at another file that was already open for writing.
     * @param fileToCompress    The file to compress
     * @param outputStream      The file stream that was already open 
     * @param inputStream 
     * @return  True if the compressed file was written in the target file,
     * false if something went wrong
     */
    public static boolean compress(final File fileToCompress, 
            OutputStream outputStream, FileInputStream inputStream){
        // does our file to compress exist?
        if(fileToCompress.exists() == false){
            // we have a problem here
            System.out.println("ZIP66 - Didn't found the file to compress: "
            + fileToCompress.getAbsolutePath());
            return false;
        }
        // all checks are done, now it is time to do the compressing
        try{
            ArchiveOutputStream archive = new ArchiveStreamFactory()
                .createArchiveOutputStream("zip", outputStream);
//            archive.putArchiveEntry(new ZipArchiveEntry(fileToCompress.getName()));
//            // create the input file stream and copy it over to the archive
//            IOUtils.copy(inputStream, archive);
//            // close the archive
//            archive.closeArchiveEntry();
//            archive.flush();
            archive.close();
            // and flush the output file stream but keep it open for other usage
            outputStream.flush();
       
        } catch (FileNotFoundException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ArchiveException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(zip.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } 
        
     return true;
    }
    
    
    /**
     * Do a simple test with the compression and decompression, please verify
     * that the files exist on the location where this program is running or
     * change the values here accordingly.
     * @param args
     * @throws IOException
     * @throws ArchiveException 
     */
    public static void main(String[] args) throws IOException, ArchiveException {
         File file1 = new File("test.big");
         File file2 = new File("test.zip");
         compress(file1, file2);
     }
   
    
    
}
