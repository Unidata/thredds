/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;


/**
 * This class encapsulates the old .ini file functionality
 * that we used to see (and still do) in the Microsoft Operating
 * Systems. This is a handy way of delivering configuration information
 * to software at runtime. The .ini file structure is as follows:
 * <p/>
 * <b>[SectionName]</b><br>
 * <b>PropertyName = Property</b><br>
 * <b>NextPropertyName = Property2</b><br>
 * <b>AnotherPropertyName = Property3</b><br>
 * <p/>
 * <b>[AnotherSectionName]</b><br>
 * <b>PropertyName = Property</b><br>
 * <b>NextPropertyName = Property2</b><br>
 * <b>AnotherPropertyName = Property3</b><br>
 * <p/>
 * This class opens and parses the iniFile it's constructor. If
 * the file isn't accesible or is unparsable then the class will
 * not contain any usable configuration information.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */

public class iniFile
{

    private boolean Debug = false;

    private String iniFile;
    private Vector sectionNames;
    private Vector sectionProperties;
    private int currentSection;

    private String errMsg;

    //************************************************************************

    /**
     * We don't want this to get used so we made it protected...
     */
    protected iniFile()
    {
    }
    //************************************************************************


    /**
     * **********************************************************************
     * Create a <code>iniFile</code> object from the file named in the
     * parameter <code>fname</code>. The object will get the append to the
     * file name to the path returned by the call <code>
     * System.getProperty("user.home")</code>.
     *
     * @param fname A <code>String</code> containing the name of the .ini file.
     */
    public iniFile(String fname)
    {

        this(null, fname, false);


    }
    //************************************************************************


    /**
     * **********************************************************************
     * Create a <code>iniFile</code> object from the file named in the
     * parameter <code>fname</code>, and found on the parameter<code> path</code>
     *
     * @param path  A <code>String</code> containing the path to the .ini file.
     * @param fname A <code>String</code> containing the name of the .ini file.
     */
    public iniFile(String path, String fname)
    {


        this(path, fname, false);

    }
    //************************************************************************


    /**
     * **********************************************************************
     * Create a <code>iniFile</code> object from the file named in the
     * parameter <code>fname</code>, and found on the parameter<code> path</code>
     *
     * @param path  A <code>String</code> containing the path to the .ini file.
     * @param fname A <code>String</code> containing the name of the .ini file.
     * @param dbg   A <code>boolean</code> that toggles debugging output.
     */
    public iniFile(String path, String fname, boolean dbg)
    {


        Debug = dbg;

        if(path == null)
            path = System.getProperty("user.home");


        String fileSeperator = System.getProperty("file.separator");

        iniFile = path + fileSeperator + fname;
        errMsg = "The file: \"" + iniFile + "\" did not contain recognizable init information.";

        currentSection = -1;
        sectionNames = null;
        sectionProperties = null;
        parseFile();

        if(sectionNames == null)
            System.err.println(errMsg);


    }
    //************************************************************************


    /**
     * **********************************************************************
     * Get the name of the .ini file that was used to create this object.
     * <p/>
     * returns A <code>String</code> containing the name of the .ini file
     * that was opened a parsed when this object was instantiated.
     */
    public String getFileName()
    {
        return (iniFile);
    }
    //************************************************************************


    /**
     * **********************************************************************
     * Parse the .ini file indicated by the <code>private String iniFile</code>.
     */
    private void parseFile()
    {

        try {
            try (
                BufferedReader fp = new BufferedReader(new InputStreamReader(new FileInputStream(iniFile), Charset.forName("UTF-8")));
            ) {
                boolean done = false;
                while(!done) {
                    String thisLine = fp.readLine();
		            if(thisLine != null && thisLine.trim().length() == 0)
			            thisLine = null;
                    if(thisLine != null) {
                        if(Debug) System.out.println("Read: \"" + thisLine + "\"");

                        if(thisLine.startsWith(";") || thisLine.equalsIgnoreCase("")) {
                            // Do nothing, it's a comment
                            if(Debug) System.out.println("Ignoring comment or blank line...");
                        } else {
                            int cindx = thisLine.indexOf(";");

                            if(cindx > 0)
                                thisLine = thisLine.substring(0, cindx).trim();

                            if(Debug) System.out.println("Comments removed: \"" + thisLine + "\"");


                            if(thisLine.startsWith("[") && thisLine.endsWith("]")) {

                                String sname = thisLine.substring(1, thisLine.length() - 1).trim();

                                if(Debug) System.out.println("Found Section Name: " + sname);

                                if(sectionNames == null)
                                    sectionNames = new Vector();

                                sectionNames.add(sname);

                                if(sectionProperties == null)
                                    sectionProperties = new Vector();

                                sectionProperties.add(new Vector());


                            } else if(sectionNames != null && sectionProperties != null) {
                                int eqidx = thisLine.indexOf("=");

                                if(eqidx != -1) {
                                    String pair[] = new String[2];
                                    pair[0] = thisLine.substring(0, eqidx).trim();
                                    pair[1] = thisLine.substring(eqidx + 1, thisLine.length()).trim();

                                    if(Debug)
                                        System.out.println("pair[0]: \"" + pair[0] + "\"   pair[1]: \"" + pair[1] + "\"");

                                    // Add the pair to the current property list, which is the
                                    // last element in the sectionProperties vector.
                                    ((Vector) sectionProperties.lastElement()).add(pair);
                                }
                            }
                        }
                    } else {
                        done = true;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could Not Find ini File: \"" + iniFile + "\"");
        } catch (IOException e) {
            System.err.println("Could Not Read ini File: \"" + iniFile + "\"");
        }
    }

    //************************************************************************


    /**
     * **********************************************************************
     * Get the list of properties for the section <code>sectionName</code>.
     *
     * @param sectionName A <code>String</code> containing the name of the
     *                    section whose property list is desired.
     * @return An enumeration of the properties in the <code>sectionName</code>
     * Returns <code>null</code> if the section name doesn't exist or there are
     * no properties for the section.
     */
    public Enumeration getPropList(String sectionName)
    {

        if(sectionNames == null) {
            System.err.println(errMsg);
            return (null);
        }

        int sectionIndex = 0;

        Enumeration e = sectionNames.elements();

        boolean done = false;
        while(!done && e.hasMoreElements()) {
            String thisName = (String) e.nextElement();
            if(sectionName.equalsIgnoreCase(thisName))
                done = true;
            else
                sectionIndex++;
        }
        if(!done)
            return (null);
        return (((Vector) sectionProperties.elementAt(sectionIndex)).elements());

    }
    //************************************************************************


    /**
     * **********************************************************************
     * Get the named property from the current section.
     *
     * @param propertyName The name of the desired property.
     * @return A <code>String</code> containing the value of property of the
     * passed property name. Returns null if the property name doesn't exist
     * or is not set.
     */
    public String getProperty(String propertyName)
    {

        if(currentSection < 0) {
            String msg = "You must use the setSection() method before you can use getProperty().";
            System.err.println(msg);
            return (msg);
        }
        String pair[] = null;

        Enumeration e = ((Vector) sectionProperties.elementAt(currentSection)).elements();
        boolean done = false;
        while(!done && e.hasMoreElements()) {

            pair = (String[]) e.nextElement();

            if(pair[0].equalsIgnoreCase(propertyName))
                done = true;
        }

        if(done)
            return (pair[1]);


        return (null);

    }
    //************************************************************************


    /**
     * **********************************************************************
     * Get the list of Sections of this .ini File
     *
     * @return An enumeration of the sections in iniFile
     */
    public Enumeration getSectionList()
    {

        if(sectionNames == null)
            return (null);

        return (sectionNames.elements());

    }
    //************************************************************************


    /**
     * **********************************************************************
     * Prints the iniFile.
     *
     * @param ps The <code>PrintStream</code> to which to print.
     */
    public void printProps(PrintStream ps)
    {


        Enumeration se = getSectionList();

        if(se == null) {
            ps.println(errMsg);
        } else {

            while(se.hasMoreElements()) {
                String sname = (String) se.nextElement();
                setSection(sname);
                ps.println("[" + sname + "]");
                Enumeration pe = getPropList(sname);
                while(pe != null && pe.hasMoreElements()) {
                    String pair[] = (String[]) pe.nextElement();
                    String prop = pair[0];
                    String valu = getProperty(prop);
                    ps.println("    \"" + prop + "\" = \"" + valu + "\"");
                }
            }
        }

    }
    //************************************************************************


    /**
     * **********************************************************************
     * Set the section of the iniFile that you wish to work with. This is
     * persistent for the life of the object, or until it's set again.
     *
     * @param sectionName A <code>String</code> containing the name of the
     *                    section that is desired.
     * @return true if the section exists and the operation was successful,
     * false otherwise.
     */
    public boolean setSection(String sectionName)
    {

        if(sectionNames == null) {
            System.err.println(errMsg);
            return (false);
        }

        int sectionIndex = 0;

        Enumeration e = sectionNames.elements();

        boolean done = false;
        while(!done && e.hasMoreElements()) {
            String thisName = (String) e.nextElement();
            if(sectionName.equalsIgnoreCase(thisName))
                done = true;
            else
                sectionIndex++;
        }

        if(!done)
            return (false);

        currentSection = sectionIndex;

        return (true);
    }
    //************************************************************************


    /**
     * @param args
     */
    public static void main(String args[])
    {
        boolean dbgFlag = true;
        iniFile inf = null;

        switch (args.length) {
        case 1:
            inf = new iniFile(null, args[0], dbgFlag);
            break;

        case 2:
            inf = new iniFile(args[0], args[1], dbgFlag);
            break;

        case 3:
            if(args[2].equalsIgnoreCase("false"))
                dbgFlag = false;
            inf = new iniFile(args[0], args[1], dbgFlag);
            break;

        default:
            System.err.println("Usage: test_iniFile [path] filename.ini [false]");
            System.exit(1);
        }


        inf.printProps(System.out);


    }


}




