/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//         
/////////////////////////////////////////////////////////////////////////////

package dods.util;

import java.io.*;
import java.util.*;


/**
 * This class encapsulates the old .ini file functionality
 * that we used to see (and still do) in the Microsoft Operating
 * Systems. This is a handy way of delivering configuration information
 * to software at runtime. The .ini file structure is as follows:
 * <p>
 * <b>[SectionName]</b><br>
 * <b>PropertyName = Property</b><br>
 * <b>NextPropertyName = Property2</b><br>
 * <b>AnotherPropertyName = Property3</b><br>
 * <p>
 * <b>[AnotherSectionName]</b><br>
 * <b>PropertyName = Property</b><br>
 * <b>NextPropertyName = Property2</b><br>
 * <b>AnotherPropertyName = Property3</b><br>
 * <p>
 * This class opens and parses the iniFile it's constructor. If
 * the file isn't accesible or is unparsable then the class will
 * not contain any usable configuration information.
 *
 * @version $Revision: 1.1 $
 * @author ndp
 */

public class iniFile {

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
    protected iniFile(){
    }
    //************************************************************************
    
    
    
    
    /*************************************************************************
    * Create a <code>iniFile</code> object from the file named in the
    * parameter <code>fname</code>. The object will get the append to the 
    * file name to the path returned by the call <code>
    * System.getProperty("user.home")</code>.
    *
    * @param fname A <code>String</code> containing the name of the .ini file.
    */
    public iniFile(String fname) {
    
	this(null,fname,false);
    

    }
    //************************************************************************
    
    
    
    /*************************************************************************
    * Create a <code>iniFile</code> object from the file named in the
    * parameter <code>fname</code>, and found on the parameter<code> path</code>
    * @param path A <code>String</code> containing the path to the .ini file.
    * @param fname A <code>String</code> containing the name of the .ini file.
    */
    public iniFile(String path ,String fname)  {

    
	this(path,fname,false);    
   
    }
    //************************************************************************


    /*************************************************************************
    * Create a <code>iniFile</code> object from the file named in the
    * parameter <code>fname</code>, and found on the parameter<code> path</code>
    * @param path A <code>String</code> containing the path to the .ini file.
    * @param fname A <code>String</code> containing the name of the .ini file.
    * @param dbg A <code>boolean</code> that toggles debugging output.
    */
    public iniFile(String path ,String fname, boolean dbg)  {

    
	Debug = dbg;
	
	if(path == null)
	    path = System.getProperty("user.home");

	    
        String fileSeperator = System.getProperty("file.separator");
    
        iniFile = path + fileSeperator + fname;
	errMsg = "The file: \""+iniFile+"\" did not contain recognizable init information.";

        currentSection = -1;    
	sectionNames = null;
	sectionProperties = null;
        parseFile();

        if(sectionNames == null)
	        System.err.println(errMsg);
    
    
    }
    //************************************************************************



    /*************************************************************************
    * Get the name of the .ini file that was used to create this object.
    *
    * returns A <code>String</code> containing the name of the .ini file
    * that was opened a parsed when this object was instantiated.
    */
    public String getFileName(){
        return(iniFile);
    }
    //************************************************************************



    /*************************************************************************
    * Parse the .ini file indicated by the <code>private String iniFile</code>.
    */
    private void parseFile()  {

    
	try {    
            BufferedReader fp = new BufferedReader( new InputStreamReader( new FileInputStream(iniFile)));
	
	    boolean done = false;
	    while(!done) {
	
	        String thisLine = fp.readLine();
	    
	        if(thisLine != null) {
	            if(Debug) System.out.println("Read: \""+thisLine+"\"");
                
                    thisLine = thisLine.trim();
	
	            if (thisLine.startsWith(";") || thisLine.equalsIgnoreCase("")){
	                // Do nothing, it's a comment
		        if(Debug) System.out.println("Ignoring comment or blank line...");
	            }
	            else {	    
	                int cindx = thisLine.indexOf(";");
		
		        if(cindx > 0)
		            thisLine = thisLine.substring(0,cindx);
			
                        if(Debug) System.out.println("Comments removed: \""+thisLine+"\"");
	    
	                thisLine = thisLine.trim();
	    
	                if(thisLine.startsWith("[")  && thisLine.endsWith("]")){
	    
	                    String sname = thisLine.substring(1,thisLine.length()-1).trim();
		    
			    if(Debug) System.out.println("Found Section Name: "+sname);
			
			    if(sectionNames == null)
			        sectionNames = new Vector();
			    
			    sectionNames.add(sname);
			
			    if(sectionProperties == null)
			        sectionProperties = new Vector();
			    
			    sectionProperties.add(new Vector());
			
			
			
                        }
                        else if(sectionNames!= null && sectionProperties!=null){
		            int eqidx = thisLine.indexOf("=");
		    
		            if(eqidx != -1){
		                String pair[] = new String[2];
		                pair[0] = thisLine.substring(0,eqidx).trim();
		                pair[1] = thisLine.substring(eqidx+1,thisLine.length()).trim();
		
			        if(Debug) System.out.println("pair[0]: \""+pair[0]+"\"   pair[1]: \""+pair[1]+"\"");
			    
			        // Add the pair to the current property list, which is the
			        // last element in the sectionProperties vector.
			        ((Vector)sectionProperties.lastElement()).add(pair);			    
		            }
                        }
	            }
	        }
	        else {
	           done = true;
	        }
            }
     	    fp.close();
        }
	catch (FileNotFoundException e) {
	        System.err.println("Could Not Find ini File: \""+iniFile+"\"");
	}
	catch (IOException e) {
	        System.err.println("Could Not Read ini File: \""+iniFile+"\"");
	}
     
    }
    //************************************************************************
    
    
    
    
    /*************************************************************************
    * Get the list of properties for the section <code>sectionName</code>.
    *
    * @param sectionName A <code>String</code> containing the name of the
    * section whose property list is desired.
    *
    * @returns An enumeration of the properties in the <code>sectionName</code>
    * Returns <code>null</code> if the section name doesn't exist or there are
    * no properties for the section.
    */
    public Enumeration getPropList(String sectionName){
    
        if(sectionNames == null){
	        System.err.println(errMsg);
	        return(null);
	}
    
        int sectionIndex = 0;
	
	Enumeration e = sectionNames.elements();
	
	boolean done = false;
	while(!done && e.hasMoreElements()){
	    String thisName = (String) e.nextElement();
	    if(sectionName.equalsIgnoreCase(thisName))
	        done = true;
	    else
	        sectionIndex++;
	}
	
	if(!done)
	    return(null);
	    
	return(((Vector)sectionProperties.elementAt(sectionIndex)).elements());
    
    }
    //************************************************************************
   

    
	    
    /*************************************************************************
    * Get the named property from the current section.
    *
    * @param propertyName The name of the desired property.
    *
    * @returns A <code>String</code> containing the value of property of the 
    * passed property name. Returns null if the property name doesn't exist
    * or is not set.
    */
    public String getProperty(String propertyName){
    
        if(currentSection < 0){
	        String msg = "You must use the setSection() method before you can use getProperty().";
	        System.err.println(msg);
	        return(msg);
	}
        String pair[] = null;
	
        Enumeration e = ((Vector)sectionProperties.elementAt(currentSection)).elements();
	boolean done = false;
	while(!done && e.hasMoreElements()){
	
	    pair = (String[]) e.nextElement();
	    
	    if(pair[0].equalsIgnoreCase(propertyName))
		    done = true;
	}
	
	if(done)
	    return(pair[1]);

    
        return(null);
	    
    }
    //************************************************************************
    
    
    
    
    
    /*************************************************************************
    * Get the list of Sections of this .ini File
    *
    * @returns An enumeration of the sections in iniFile
    */
    public Enumeration getSectionList(){
    
        if(sectionNames == null)
	    return(null);

        return(sectionNames.elements());

    }
    //************************************************************************
    
    
    
    
    
    /*************************************************************************
    * Prints the iniFile.
    *
    * @param ps The <code>PrintStream</code> to which to print.
    */
    public void printProps(PrintStream ps){
    
    
        Enumeration se = getSectionList();
	
	if(se == null){
	     ps.println(errMsg);
	}
	else {
	
	    while(se.hasMoreElements()){
	
	        String sname = (String) se.nextElement();
	            
                setSection(sname);
	    
	        ps.println("["+sname+"]");
	    
                Enumeration pe = getPropList(sname);
	    
	        while(pe.hasMoreElements()){
	        
		    String pair[] = (String[]) pe.nextElement();
		
		    String prop = pair[0];
		
		    String valu = getProperty(prop);
		
		    ps.println("    \""+prop+"\" = \""+valu+"\"");

	        }
	    }
	}
	    
    }
    //************************************************************************
    
    
    
    
    
    /*************************************************************************
    * Set the section of the iniFile that you wish to work with. This is 
    * persistent for the life of the object, or until it's set again.
    *
    * @param sectionName A <code>String</code> containing the name of the 
    * section that is desired.
    *
    * @returns true if the section exists and the operation was successful, 
    * false otherwise.
    */
    public boolean setSection(String sectionName){
    
        if(sectionNames == null){
	        System.err.println(errMsg);
	        return(false);
	}
    
        int sectionIndex = 0;
	
	Enumeration e = sectionNames.elements();
	
	boolean done = false;
	while(!done && e.hasMoreElements()){
	    String thisName = (String) e.nextElement();
	    if(sectionName.equalsIgnoreCase(thisName))
	        done = true;
	    else
	        sectionIndex++;
	}
	
	if(!done)
	    return(false);
	    
        currentSection = sectionIndex;	    
	    
	return(true);
    }
    //************************************************************************
    
           
    
}


