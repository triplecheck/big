/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-12-10T08:31:00Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: BigZipUnitTest.java
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2013 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Basic tests to the software functionality. </text>
 */

import big.BigZip;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Nuno Brito, 10th of January 2014 in Darmstadt, Germany.
 */
public class bigZipUnitTest {
    
    static File folderTest = new File("testing");

    static BigZip db;
    
    File 
        folderWithFiles = utils.files.getCanonicalFile(new File("../lib")),
        fileSingle = utils.files.getCanonicalFile(new File("../LICENSE")),

        fileZip = new File(folderTest, "zipTest.big"),
        fileZipLog = new File(folderTest, "zipTest.big-log"),
        fileZipIndex = new File(folderTest, "zipTest.big-index")
        ;
    
    public bigZipUnitTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("[START] Starting tests");
        System.out.println("Creating the test folder: " + folderTest.getAbsolutePath());
        utils.files.mkdirs(folderTest);
    }
    
    @AfterClass
    public static void tearDownClass() {
        System.out.println("[DONE] Finished tests");
    }
    
    @Before
    public void setUp() {
        // delete any old files
        utils.files.deleteDir(folderTest);
        // create the new folder
        utils.files.mkdirs(folderTest);
        db = new BigZip(fileZip);
    }
    
    @After
    public void tearDown() {
        db.close();
        //utils.files.deleteDir(folderTest);
    }

    @Test
    public void testAddingSingleFile() throws IOException {
        System.out.println("[TEST] Adding a single file");
        System.out.println("Source: " + fileSingle.getAbsolutePath());
        
        // operation of adding a single file
        db.addFile(fileSingle, fileSingle.getParentFile().getAbsolutePath());
        
        // test the contents of the log files
        final String contentIndex = utils.files.readAsString(fileZipLog);
        assert(contentIndex.contains(fileSingle.getName()));
        assert(contentIndex.contains("start: 00000"));
        assert(contentIndex.contains("ended: 00000"));
        
        // test the data file
        final String contentZip = utils.files.readAsString(fileZip);
        assert(contentZip.contains("BIG81nbPK"));
        assert(contentZip.contains(fileSingle.getName()));
        
        // test the data file
        final String contentZipIndex = utils.files.readAsString(fileZipIndex);
        assert(contentZipIndex.contains("000000000000000"));
        assert(contentZipIndex.contains(fileSingle.getName()));
       
        System.out.println("\n\n");
    }

    @Test
    public void testAddingSingleFileQuick() throws IOException{
        System.out.println("[TEST] Quickly adding a single file");
        System.out.println("Source: " + fileSingle.getAbsolutePath());
        
        db.quickStart();
        
        // add some files for our testing purposes
        for(final File file : new File("..").listFiles()){
            if(file.isDirectory()){
                continue;
            }
            final String hash = 
                utils.hashing.checksum.generateFileChecksum("SHA-1", file);
            final String path = "./" + file.getName();
            db.quickWrite(file, hash, path);
        }
        
        
        db.quickEnd();
        
        // test the contents of the log files
        final String contentIndex = utils.files.readAsString(fileZipLog);
        assert(contentIndex.contains(fileSingle.getName()));
        assert(contentIndex.contains("start: 00000"));
        assert(contentIndex.contains("ended: 00000"));
        
        // test the data file
        final String contentZip = utils.files.readAsString(fileZip);
        assert(contentZip.contains("BIG81nbPK"));
        assert(contentZip.contains(fileSingle.getName()));
        
        // test the data file
        final String contentZipIndex = utils.files.readAsString(fileZipIndex);
        assert(contentZipIndex.contains("000000000000000"));
        assert(contentZipIndex.contains(fileSingle.getName()));
        
        // was a file added with contents recovered?
        String content = db.extractBytesToRAM(0);
        assert(content.isEmpty() == false);
        
        // find the license file, verify if it was decompressed correctly
        content = db.getFileAsText("./LICENSE");
        System.out.println("Testing content extraction");
        assert(content.contains("The original code"));
        
    }
    
    
    @Test
    public void testAddingSingleFileQuickString() throws IOException{
        System.out.println("[TEST] Quickly adding a single text");
        System.out.println("Source: " + fileSingle.getAbsolutePath());
        
        // calculate the SHA1 signature
        final String SHA1 = 
                utils.hashing.checksum.generateFileChecksum("SHA-1", fileSingle);
        
        
        final String content = utils.files.readAsString(fileSingle);
        
        db.quickStart();
        // operation of adding a single file
        db.quickWrite(content, SHA1, 
                fileSingle.getAbsolutePath());
        db.quickEnd();
        
        // test the contents of the log files
        final String contentIndex = utils.files.readAsString(fileZipLog);
        assert(contentIndex.contains(fileSingle.getName()));
        assert(contentIndex.contains("start: 00000"));
        assert(contentIndex.contains("ended: 00000"));
        
        // test the data file
        final String contentZip = utils.files.readAsString(fileZip);
        assert(contentZip.contains("BIG81nbPK"));
        assert(contentZip.contains(fileSingle.getName()));
        
        // test the data file
        final String contentZipIndex = utils.files.readAsString(fileZipIndex);
        assert(contentZipIndex.contains("000000000000000"));
        assert(contentZipIndex.contains(fileSingle.getName()));
    }
    
    
}
