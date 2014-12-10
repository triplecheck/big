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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        System.out.println();
    }
    
    @AfterClass
    public static void tearDownClass() {
        System.out.println("\n[DONE] Finished tests");
        
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
        utils.files.deleteDir(folderTest);
    }

    @Test
    public void testAddingSingleFile() {
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
    public void testAddingSingleFileQuick(){
        System.out.println("[TEST] Quickly adding a single file");
        System.out.println("Source: " + fileSingle.getAbsolutePath());
        
        // calculate the SHA1 signature
        final String SHA1 = 
                utils.thirdparty.Checksum.generateFileChecksum("SHA-1", fileSingle);
        
        
        db.quickStart();
        // operation of adding a single file
        db.quickWrite(fileSingle, SHA1, fileSingle.getParentFile().getAbsolutePath());
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
