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


package opendap.dap;

import java.util.Enumeration;

import opendap.dap.parsers.DDSXMLParser;
import opendap.util.SortedTable;
import opendap.util.Debug;

import java.io.*;

/**
 * An <code>AttributeTable</code> stores a set of names and, for each name,
 * an <code>Attribute</code> object.  For more information on the types of
 * data which can be stored in an attribute, including aliases and other
 * <code>AttributeTable</code> objects, see the documentation for
 * <code>Attribute</code>.
 * <p/>
 * The attribute tables have a standard printed representation.  There is a
 * <code>print</code> method for writing this form and a <code>parse</code>
 * method for reading the printed form.
 * <p/>
 * An <code>AttributeTable</code>'s print representation might look like:
 * <pre>
 *   String long_name "Weekly Means of Sea Surface Temperature";
 * </pre>
 * or
 * <pre>
 *   actual_range {
 *       Float64 min -1.8;
 *       Float64 max 35.09;
 *   }
 * </pre>
 * or
 * <pre>
 *   String Investigators "Cornillon", "Fleirl", "Watts";
 * </pre>
 * or
 * <pre>
 *   Alias New_Attribute Old_Attribute;
 * </pre>
 * Here, <em>long_name</em> and <em>Investigators</em> are
 * simple attributes, <em>actual_range</em> is a container attribute, and
 * <em>New_Attribute</em> is an alias pointing to <em>Old_Attribute</em>.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see DAS
 * @see Attribute
 *
 * Modified 1/9/2011 Dennis Heimbigner
 * - Make subclass of BaseType for uniformity
 */

public class AttributeTable extends DAPNode
{
    /**
     * A table of Attributes with their names as a key
     */
    private SortedTable _attr;

    /**
     * Create a new empty <code>AttributeTable</code>.
     *
     * @deprecated Use constructor that takes the name of the table.
     */
    public AttributeTable() {
        _attr = new SortedTable();
    }

    /**
     * Create a new empty <code>AttributeTable</code>.
     */
    public AttributeTable(String clearname) {
        super(clearname);
        _attr = new SortedTable();
    }

    /**
     *
     * @return the # of contained attributes
     */
    public int size()
    {
        return (_attr == null ? 0 : _attr.size());
    }

    /**
     * Returns an <code>Enumeration</code> of the attribute names in this
     * <code>AttributeTable</code>.
     * Use the <code>getAttribute</code> method to get the
     * <code>Attribute</code> for a given name.
     *
     * @return an <code>Enumeration</code> of <code>String</code>.
     * @see AttributeTable#getAttribute(String)
     */
    public final Enumeration getNames() {
        return _attr.keys();
    }

    /**
     * Returns the <code>Attribute</code> which matches name.
     *
     * @param clearname the name of the <code>Attribute</code> to return.
     * @return the <code>Attribute</code> with the specified name, or null
     *         if there is no matching <code>Attribute</code>.
     * @see Attribute
     */
    public final Attribute getAttribute(String clearname) { //throws NoSuchAttributeException {
        Attribute a = (Attribute) _attr.get(clearname);
        return (a);
    }

    /**
     * Returns the <code>Attribute</code> which matches name.
     *
     * @param clearname the name of the <code>Attribute</code> to return.
     * @return True if an Attribute with named 'name' exists, False otherwise.
     * @see Attribute
     */
    public final boolean hasAttribute(String clearname) {
        Attribute a = (Attribute) _attr.get(clearname);

        if (a == null) {
            return (false);
        }
        return (true);
    }

    /**
     * Adds an attribute to the table.  If the given name already
     * refers to an attribute, and the attribute has a vector value,
     * the given value is appended to the attribute vector.  Calling
     * this function repeatedly is the way to create an attribute
     * vector.
     * <p/>
     * The function throws an exception if the attribute is a
     * container, or if the type of the input value does not match the
     * existing attribute's type and the <code>check</code> parameter
     * is true.  Use the <code>appendContainer</code> method to add container
     * attributes.
     *
     * @param clearname  The name of the attribute to add or modify.
     * @param type  The type code of the attribute to add or modify.
     * @param value The value to add to the attribute table.
     * @param check Check the validity of the attribute's value?
     * @throws AttributeExistsException   thrown if an Attribute with the same
     *                                    name, but a different type was previously defined.
     * @throws AttributeBadValueException thrown if the value is not a legal
     *                                    member of type
     * @see AttributeTable#appendContainer(String)
     */
    public final void appendAttribute(String clearname, int type, String value,
                                      boolean check) throws DASException {

        Attribute a = (Attribute) _attr.get(clearname);

        if (a != null && (type != a.getType())) {

            // type mismatch error
            throw new AttributeExistsException("The Attribute `" + clearname
                    + "' was previously defined with a different type.");

        } else if (a != null) {

            a.appendValue(value, check);

        } else {

            a = new Attribute(type, clearname, value, check);
            _attr.put(clearname, a);
        }
    }

    /**
     * Adds an attribute to the table.  If the given name already
     * refers to an attribute, and the attribute has a vector value,
     * the given value is appended to the attribute vector.  Calling
     * this function repeatedly is the way to create an attribute
     * vector.
     * <p/>
     * The function throws an exception if the attribute is a
     * container, or if the type of the input value does not match the
     * existing attribute's type.  Use the <code>appendContainer</code>
     * method to add container attributes.
     *
     * @param clearname  The name of the attribute to add or modify.
     * @param type  The type code of the attribute to add or modify.
     * @param value The value to add to the attribute table.
     * @throws AttributeExistsException   thrown if an Attribute with the same
     *                                    name, but a different type was previously defined.
     * @throws AttributeBadValueException thrown if the value is not a legal
     *                                    member of type
     * @see AttributeTable#appendContainer(String)
     */
    public final void appendAttribute(String clearname, int type, String value)
            throws DASException {
        appendAttribute(clearname, type, value, true);
    }

    /**
     * Create and append an attribute container to the table.
     * A container is another <code>AttributeTable</code> object.
     *
     * @param clearname the name of the container to add.
     * @return A pointer to the new <code>AttributeTable</code> object, or null
     *         if a container by that name already exists.
     */
    public final AttributeTable appendContainer(String clearname) {
        // return null if clearname already exists
        // FIXME! THIS SHOULD RETURN AN EXCEPTION!
        if (_attr.get(clearname) != null)
            return null;

        AttributeTable at = new AttributeTable(clearname);
        Attribute a = new Attribute(clearname, at);
        _attr.put(clearname, a);
        return at;
    }

    /**
     * Create and append an attribute container to the table.
     * A container is another <code>AttributeTable</code> object.
     *
     * @param clearname the name of the container to add.
     *             if a container by that name already exists.
     */
    public final void addContainer(String clearname, AttributeTable at) throws AttributeExistsException {

        // return null if name already exists
        if (_attr.get(clearname) != null) {
            throw new AttributeExistsException("The Attribute '" + clearname +
                    "' already exists in the container '" +
                    getEncodedName() + "'");
        }

        Attribute a = new Attribute(clearname, at);
        _attr.put(clearname, a);
    }

    /**
     * Add an alias to the current table.
     * This method is used by the DAS parser to build Aliases
     * for the DAS. And the DDSXMLParser to add them to the DDX
     * <p/>
     * The new (9/26/02) DDS requires the use of <code>
     * addAlias(String, String, String)</code> and is the preffered
     * way of representing the DAS information.
     *
     * @param alias         The alias to insert into the attribute table.
     * @param attributeName The normalized name of the attribute to which
     *                      the alias will refer.
     * @throws NoSuchAttributeException thrown if the existing attribute
     *                                  could not be found.
     * @throws AttributeExistsException thrown if the new alias has the same
     *                                  name as an existing attribute.
     */
    public final void addAlias(String alias, String attributeName)
            throws NoSuchAttributeException, AttributeExistsException {

        // complain if alias name already exists in this AttributeTable.
        if (_attr.get(alias) != null) {
            throw new AttributeExistsException("Could not alias `" + alias +
                    "' to `" + attributeName + "'. " +
                    "It is a duplicat name in this AttributeTable");
        }
        if (Debug.isSet("AttributTable")) {
            log.debug("Adding alias '" + alias + "' to AttributeTable '" + getClearName() + "'");
        }

        Alias newAlias = new Alias(alias, attributeName);
        _attr.put(alias, newAlias);
    }

    /**
     * Delete the attribute named <code>name</code>.
     *
     * @param clearname The name of the attribute to delete.  This can be an
     *             attribute of any type, including containers.
     */
    public final void delAttribute(String clearname) {
        _attr.remove(clearname);
    }

    /**
     * Delete the attribute named <code>name</code>.  If the attribute has a
     * vector value, delete the <code>i</code>'th element of the vector.
     *
     * @param clearname The name of the attribute to delete.  This can be an
     *             attribute of any type, including containers.
     * @param i    If the named attribute is a vector, and <code>i</code> is
     *             non-negative, the <code>i</code>'th entry in the vector is deleted.
     *             If <code>i</code> equals -1, the entire attribute is deleted.
     * @see AttributeTable#delAttribute(String)
     */
    public final void delAttribute(String clearname, int i) throws DASException {

        if (i == -1) {  // delete the whole attribute

            _attr.remove(clearname);

        } else {

            Attribute a = (Attribute) _attr.get(clearname);

            if (a != null) {

                if (a.isContainer()) {

                    _attr.remove(clearname);  // delete the entire container

                } else {

                    a.deleteValueAt(i);

                }
            }
        }
    }

    /**
     * Print the attribute table on the given <code>PrintWriter</code>.
     *
     * @param os  the <code>PrintWriter</code> to use for output.
     * @param pad the number of spaces to indent each line.
     */
    public void print(PrintWriter os, String pad) {

        if (Debug.isSet("AttributTable")) os.println("Entered AttributeTable.print()");

        os.println(pad + getEncodedName() + " {");
        for (Enumeration e = getNames(); e.hasMoreElements();) {

            String name = (String) e.nextElement();
            Attribute a = getAttribute(name);
            if (a != null)
                a.print(os, pad + "    ");


        }
        os.println(pad + "}");
        if (Debug.isSet("AttributTable")) os.println("Leaving AttributeTable.print()");
        os.flush();
    }

    /**
     * Print the attribute table on the given <code>OutputStream</code>.
     *
     * @param os  the <code>OutputStream</code> to use for output.
     * @param pad the number of spaces to indent each line.
     */
    public final void print(OutputStream os, String pad) {
        print(new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8))), pad);
    }

    /**
     * Print the attribute table on the given <code>PrintWriter</code> with
     * four spaces of indentation.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public final void print(PrintStream os) {
        print(os, "");
    }

    /**
     * Print the attribute table on the given <code>PrintWriter</code> with
     * four spaces of indentation.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public final void print(PrintWriter os) {
        print(os, "");
    }

    /**
     * Print the attribute table on the given <code>OutputStream</code> with
     * four spaces of indentation.
     *
     * @param os the <code>OutputStream</code> to use for output.
     */
    public final void print(OutputStream os) {
        print(os, "");
    }


    /**
     *
     * @param os
     * @opendap.ddx.experimental
     */
    public void printXML(OutputStream os) {
        printXML(os, "");
    }


    /**
     *
     * @param os
     * @param pad
     * @opendap.ddx.experimental
     */
    public void printXML(OutputStream os, String pad) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printXML(pw, pad);
        pw.flush();
    }


    /**
     *
     * @param pw
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw) {
        printXML(pw, "");
    }


    /**
     *
     * @param pw
     * @param pad
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw, String pad) {
        printXML(pw, pad, false);
    }


    /**
     *
     * @param pw
     * @param pad
     * @param constrained
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw, String pad, boolean constrained) {

        if (Debug.isSet("AttributTable")) pw.println("Entered AttributeTable.print()");

        pw.println(pad + "<Attribute name=\"" +
                DDSXMLParser.normalizeToXML(getEncodedName()) +
                "\" type=\"Container\">");

        Enumeration e = getNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Attribute a = getAttribute(name);
            if (a != null)
                a.printXML(pw, pad + "\t", constrained);

        }
        pw.println(pad + "</Attribute>");
        if (Debug.isSet("AttributTable")) pw.println("Leaving AttributeTable.print()");
        pw.flush();


    }

    /**
     * Returns a clone of this <code>AttributeTable</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this <code>Attribute</code>.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
            AttributeTable at = (AttributeTable) super.cloneDAG(map);
            at._attr = new SortedTable();
            for (int i = 0; i < _attr.size(); i++) {
                String key = (String) _attr.getKey(i);
                Attribute element = (Attribute) _attr.elementAt(i);
                // clone element (don't clone key because it's a read-only String)
                at._attr.put(key, (Attribute)cloneDAG(map,element));
            }
            return at;
    }

}

// $Log: AttributeTable.java,v $
// Revision 1.3  2003/09/02 17:49:34  ndp
// *** empty log message ***
//
// Revision 1.2  2003/09/02 15:06:25  ndp
// *** empty log message ***
//
// Revision 1.1  2003/08/12 23:51:25  ndp
// Mass check in to begin Java-OPeNDAP development work
//
// Revision 1.12 2011/01/09 Dennis Heimbigner
//  - Make subclass of BaseType for uniformity
//
// Revision 1.11  2003/04/07 22:12:32  jchamber
// added serialization
//
// Revision 1.10  2003/02/12 16:41:15  ndp
// *** empty log message ***
//
// Revision 1.9  2002/10/10 18:12:31  ndp
// Fixed bugs in DDS.getDAS(), Updated testDataset and sqlDataset
//
// Revision 1.8  2002/10/08 21:59:18  ndp
// Added XML functionality to the core. This includes the new DDS code (aka DDX)
// for parsing XML representations of the dataset description ( that's a DDX)
// Also BaseType has been modified to hold Attributes and methods added to DDS
// to ingest DAS's (inorder to add Attributes to variables) and to get the DAS
// object from the DDS. Geturl and DConnect hav been modified to provide client
// access to this new set of functionalites. ndp 10/8/2002
//
// Revision 1.7  2002/05/30 23:25:57  jimg
// I added methods that provide a way to add attribues without the usual
// type/value checking. See today's log in Attribute.java for more info.
//


