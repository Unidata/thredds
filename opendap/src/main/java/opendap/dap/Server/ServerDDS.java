/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////



package opendap.dap.Server;

import java.io.*;
import java.util.Enumeration;

import opendap.dap.*;

/**
 * ServerDDS is a specialization of DDS for the server-side of OPeNDAP. This
 * class includes methods used to distinguish synthesized variables
 * (variables added to the DDS by a constraint expression function), methods
 * for CE function management and methods used to return a `constrained DDS'
 * as part of a OPeNDAP data document.
 * <p/>
 * All of the variables contained by a ServerDDS <em>must</em> implement the
 * Projection interface.
 *
 * @author jhrg
 * @version $Revision: 15901 $
 * @see DDS
 * @see CEEvaluator
 */
public class ServerDDS extends DDS implements Cloneable {

    protected ServerDDS() {
        super();
    }

    /**
     * Creates an empty <code>Server DDS</code> with the given dataset name.
     *
     * @param n the dataset name
     */
    protected ServerDDS(String n) {
        super(n);
    }

    /**
     * Creates an empty <code>ServerDDS</code> with the given
     * <code>BaseTypeFactory</code>. This will be used for OPeNDAP servers
     * which need to construct subclasses of the various
     * <code>BaseType</code> objects to hold additional server-side
     * information.
     *
     * @param factory the server <code>BaseTypeFactory</code> object.
     */
    public ServerDDS(BaseTypeFactory factory) {
        super(factory);
    }

    /**
     * Creates an empty <code>ServerDDS</code> with the given dataset name
     * and <code>BaseTypeFactory</code>. This will be used for OPeNDAP servers
     * which need to construct subclasses of the various
     * <code>BaseType</code> objects to hold additional server-side
     * information.
     *
     * @param n       the dataset name
     * @param factory the server <code>BaseTypeFactory</code> object.
     */
    public ServerDDS(String n, BaseTypeFactory factory) {
        super(n, factory);
    }

    /**
     * Creates an empty <code>DDS</code> with the given dataset name and
     * <code>BaseTypeFactory</code>.  This will be used for OPeNDAP servers which
     * need to construct subclasses of the various <code>BaseType</code> objects
     * to hold additional server-side information.
     *
     * @param n       the dataset name
     * @param factory the server <code>BaseTypeFactory</code> object.
     * @param schema  the URL where the parser can find an instance of the
     *                OPeNDAP namespace schema.
     */
    public ServerDDS(String n, BaseTypeFactory factory, String schema) {
        super(n, factory, schema);
    }


    /**
     * Return a clone of the <code>ServerDDS</code>. A deep copy is
     * performed on this object and those it contains.
     *
     * @return a ServerDDS object.
     */
    public Object clone() {
        ServerDDS d = (ServerDDS) super.clone();
        return (d);
    }

    /**
     * Set the filename of the dataset. This must be passed to the
     * <code>read()</code> method of the FileIO interface. The filename of
     * the dataset may be a real filename or may be any other string that
     * can be used to identify for the <code>read</code> method how to
     * access the data-store of which a particular variable is a member.
     *
     * @param n The name of the dataset.
     * @see ServerMethods#read(String, Object) ServerMethods.read()
     */
    public void setDatasetFilename(String n) {
        setName(n);
    }

    /**
     * Get the dataset filename.
     *
     * @return The filename of the dataset.
     * @see #setDatasetFilename(String)
     */
    public String getDatasetFilename() {
        String s = getName();
        System.out.println(s);
        return (s);
    }

    /**
     * Print the constrained <code>DDS</code> on the given
     * <code>PrintWriter</code>.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void printConstrained(PrintWriter os) {
        os.println("Dataset {");
        for (Enumeration e = getVariables(); e.hasMoreElements();) {
            BaseType bt = (BaseType) e.nextElement();
            if (((ServerMethods) bt).isProject())
                bt.printDecl(os, "    ", true, true);
        }
        os.print("} ");
        if (getName() != null)
            os.print(getName());
        os.println(";");
    }

    /**
     * Print the constrained <code>DDS</code> on the given
     * <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see DDS#print(PrintWriter)
     */
    public final void printConstrained(OutputStream os) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
        printConstrained(pw);
        pw.flush();
    }


    /**
     * Print the constrained <code>DDS</code> on the given
     * <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see DDS#print(PrintWriter)
     * @opendap.ddx.experimental
     */
    public final void printConstrainedXML(OutputStream os) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
        printConstrained(pw);
        pw.flush();
    }

    /**
     * Print the constrained <code>DDS</code> on the given
     * <code>PrintWriter</code>.
     *
     * @param pw the <code>PrintWriter</code> to use for output.
     * @opendap.ddx.experimental
     */
    public void printConstrainedXML(PrintWriter pw) {
        printXML(pw, "", true);
    }

}



