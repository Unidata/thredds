/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, University of Rhode Island
// ALL RIGHTS RESERVED.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: James Gallagher <jgallagher@gso.uri.edu>
//
/////////////////////////////////////////////////////////////////////////////

package dods.dap.Server;

import java.io.*;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;
import java.util.Map;

import dods.dap.*;

/** ServerDDS is a specialization of DDS for the server-side of DODS. This
    class includes methods used to distinguish synthesized variables
    (variables added to the DDS by a constraint expression function), methods
    for CE function management and methods used to return a `constrained DDS'
    as part of a DODS data document.
    <p>
    All of the variables contained by a ServerDDS <em>must</em> implement the
    Projection interface.
    @version $Revision: 51 $
    @author jhrg
    @see DDS
    @see CEEvaluator */
public class ServerDDS extends DDS implements Cloneable {

    protected ServerDDS() {
	super();
    }

    /** Creates an empty <code>Server DDS</code> with the given dataset name.
	@param n the dataset name */
    protected ServerDDS(String n) {
	super(n);
    }

    /** Creates an empty <code>ServerDDS</code> with the given
	<code>BaseTypeFactory</code>. This will be used for DODS servers
	which need to construct subclasses of the various
	<code>BaseType</code> objects to hold additional server-side
	information.
	@param factory the server <code>BaseTypeFactory</code> object. */
    public ServerDDS(BaseTypeFactory factory) {
	this(null, factory);
    }

    /** Creates an empty <code>ServerDDS</code> with the given dataset name
	and <code>BaseTypeFactory</code>. This will be used for DODS servers
	which need to construct subclasses of the various
	<code>BaseType</code> objects to hold additional server-side
	information.
	@param n the dataset name
	@param factory the server <code>BaseTypeFactory</code> object. */
    public ServerDDS(String n, BaseTypeFactory factory) {
	super(n, factory);
    }

    /** Return a clone of the <code>ServerDDS</code>. A deep copy is
	performed on this object and those it contains. 
	@return a ServerDDS object. */
    public Object clone() {
    	ServerDDS d = (ServerDDS)super.clone();
	return(d);
    }

    /** Set the filename of the dataset. This must be passed to the
	<code>read()</code> method of the FileIO interface. The filename of
	the dataset may be a real filename or may be any other string that
	can be used to identify for the <code>read</code> method how to
	access the data-store of which a particular variable is a member.
	@param n The name of the dataset.
	@see ServerMethods#read(String, Object) ServerMethods.read()
    */
    public void setDatasetFilename(String n) {
	name = n;
    }

    /** Get the dataset filename.
	@return The filename of the dataset.
	@see #setDatasetFilename(String) */
    public String getDatasetFilename() {
	String s = name;
    	System.out.println(s);
	return(s);
    }

    /** Print the constrained <code>DDS</code> on the given
	<code>PrintWriter</code>.
	@param os the <code>PrintWriter</code> to use for output. */
    public void printConstrained(PrintWriter os) {
	os.println("Dataset {");
	for (Enumeration e = getVariables(); e.hasMoreElements();) {
	    BaseType bt = (BaseType)e.nextElement();
	    if (((ServerMethods)bt).isProject())
		bt.printDecl(os, "    ", true,true);
	}
	os.print("} ");
	if (name != null)
	    os.print(name);
	os.println(";");
    }

    /** Print the constrained <code>DDS</code> on the given
	<code>OutputStream</code>.
	@param os the <code>OutputStream</code> to use for output.
	@see DDS#print(PrintWriter) */
    public final void printConstrained(OutputStream os) {
	PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
	printConstrained(pw);
	pw.flush();
    }

}

