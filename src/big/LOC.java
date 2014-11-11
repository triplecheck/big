/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-11-11T17:18:05Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: LOC.java
 * FileCopyrightText: <text> Copyright 2013 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Counts the LOC inside a BIG zip.</text>
 */

package big;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Nuno Brito, 11th of November 2014 in Darmstadt, Germany
 */
public class LOC {

    static BigZip big;
    static long counter = 0;
    static boolean debug = false;
    
    /**
     * We expect as argument the location of the big archive
     * @param args 
     * @throws java.io.IOException 
     */
    public static void main(String[] args) throws IOException{
        
        String fileLocationBig;
        
        if(debug){
            fileLocationBig = "../../kb/storage/Java.big";
        }else{
            if(args.length == 0){
                System.err.println("Error, missing parameter to indicate location of big zip.");
                return;
            }
            fileLocationBig = args[0];
        }
        
        // transform into a file
        final File fileTemp = new File(fileLocationBig);
        // get the canonical version
        fileLocationBig = utils.files.getCanonical(fileTemp);
        // get the final version in clean state
        final File file = new File(fileLocationBig);

        // does the file exist?
        if(file.exists() == false){
            System.err.println("Error, couldn't find: " + file.getAbsolutePath());
            return;
        }

        // open the big archive
        big = new BigZip(file);
        // initialize the file iterator
        big.getNextFileInitiate();
        // get some output about the processing progress
        launchMonitoringThread();
        // now get to read the source code files in sequence
        processFiles(big);
        // conclude operations
        big.getNextFileConclude();
    }

    /**
     * Go through all the files on the archive
     * @param big           The bigzip we want to process
     * @throws IOException  If something went wrong
     */
    private static void processFiles(BigZip big) throws IOException {
        String sourceCode;
        // iterate all files inside the archive
        while((sourceCode = big.getNextSourceCodeFile()) != null){
            processSourceCode(sourceCode);
        }
      
    }
    
    /**
     * Do the line counting
     * @param sourceCode    The code to process
     * @throws IOException  Error when something didn't work as expected
     */
    private static void processSourceCode(final String sourceCode) throws IOException {
        // count lines, including empty ones
        counter += utils.text.getLOC(sourceCode);
    }

    
    
        /**
     * Launch a thread that will check how the indexing is progressing
     */
    static void launchMonitoringThread(){
                Thread thread = new Thread(){
                @Override
                public void run(){
                    utils.time.wait(3);
                    while(true){
                        long counterFiles = big.getGetNextFileCounter();
                        
                        // get the number properly formatted
                        final String valueLines 
                                = utils.text.convertToHumanNumbers(counter);
                        final String valueFiles 
                                = utils.text.convertToHumanNumbers(counterFiles);
                        
                        // only show after we indexed some results
                        if(counterFiles > 1){
                            System.out.println(valueFiles + " files: "
                                    + valueLines
                                    + " lines");
                        }
                        // just keep waiting
                        utils.time.wait(5);
                    }
                }
                };
            thread.start();
    }
    
}
