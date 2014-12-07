/*
 * SPDXVersion: SPDX-1.1
 * Creator: Person: Nuno Brito (nuno.brito@triplecheck.de)
 * Creator: Organization: TripleCheck (contact@triplecheck.de)
 * Created: 2014-05-27T11:13:10Z
 * LicenseName: EUPL-1.1-without-appendix
 * FileName: Header.java  
 * FileType: SOURCE
 * FileCopyrightText: <text> Copyright 2014 Nuno Brito, TripleCheck </text>
 * FileComment: <text> Provides a standardized header for each index file</text> 
 */

package tools;

/**
 *
 * @author Nuno Brito, 27th of May 2014 in Paris, France
 */
public class header {

    // change these settings to match your desired values
    static String 
            license = "",
            copyright = "TripleCheck at http://github.com/triplecheck";
    
    /**
     * Creates the first line with the copyright header information
     * @return A single line with the header for this index file
     */
    public static String create(){
        
        // add the license header
        String licenseText = "";
        if(license.isEmpty() == false){
            licenseText = license
                    + " license"
                    + " |";
        }
        
        // add the license header
        String copyrightText = "";
        if(copyright.isEmpty() == false){
            licenseText = copyright;
        }
        
        String header = "Since " 
                    + utils.time.getDateTimeISO()
                    + " | "
                    + licenseText
                    + " Copyright (c) "
                    + utils.time.getCurrentYear()
                    + " "
                    + copyrightText
                ;
        return header;
    }
    
    /**
     * Creates the first line with the copyright header information
     * @param designation   In which type of file is this header being placed
     * @param owner         To whom does the copyright apply?
     * @return A single line with the header for this index file
     */
    public static String create(final String designation, final String owner){
        String header = designation
                    + " | "
                    + utils.time.getDateTimeISO()
                    + " | Copyright (C) "
                    + utils.time.getCurrentYear()
                    + " "
                    + owner
                ;
        return header;
    }

}
