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

import opendap.dap.parsers.DDSXMLParser;

import java.io.*;
import java.util.Enumeration;

/**
 * This abstract class defines the basic data type features for the OPeNDAP data
 * access protocol (DAP) data types. All of the DAP type classes
 * (<code>DFloat64</code>, <code>DArray</code>, etc.) subclass it or one of
 * its two abstract descendents, <code>DVector</code> or
 * <code>DConstructor</code>.
 * <p/>
 * These classes and their methods give a user the capacity to set up
 * sophisticated data types. They do <em>not</em> provide sophisticated ways to
 * access and use this data. On the server side, in many cases, the class
 * instances will have no data in them at all until the <code>serialize</code>
 * method is called to send data to the client. On the client side, most OPeNDAP
 * application programs will unpack the data promptly into whatever local
 * data structure the programmer deems the most useful.
 * <p/>
 * Descendents of this class should implement the <code>ClientIO</code>
 * interface.  That interface defines a <code>deserialize</code> method used
 * by a OPeNDAP client to retrieve the variable's declaration and value(s) from
 * a OPeNDAP server.
 *
 * @author jehamby
 * @version $Revision: 19676 $
 * @see DDS
 * @see ClientIO
 */
public abstract class BaseType extends DAPNode
{

    /**
     * The Attribute Table used to contain attributes specific to this
     * instance of a BaseType variable. This is the repository for
     * "Semantic Metadata"
     */
    private Attribute _attr;
    private AttributeTable _attrTbl;

    /**
     * Constructs a new <code>BaseType</code> with no name.
     */
    public BaseType() {
        this(null);
    }

    /**
     * Constructs a new <code>BaseType</code> with name <code>n</code>.
     * Name is always assumed to be dap encoded
     * @param clearname the name of the variable.
     */
    public BaseType(String clearname)
    {
	    super();
        _attrTbl = new AttributeTable(getClearName());
        _attr = new Attribute(getClearName(), _attrTbl);
        setClearName(clearname);
    }

    /**
     * Sets the unencoded name of the class instance.
     *
     * @param clearname the unencoded name of the class instance.
     */
    @Override
    public void setClearName(String clearname)
    {
	    super.setClearName(clearname);
        if(_attr != null) _attr.setClearName(clearname);
        if(_attrTbl !=  null) _attrTbl.setClearName(clearname);
    }


    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    abstract public String getTypeName();

    /**
     * Returns the number of variables contained in this object. For simple and
     * vector type variables, it always returns 1. To count the number
     * of simple-type variable in the variable tree rooted at this variable, set
     * <code>leaves</code> to <code>true</code>.
     *
     * @param leaves If true, count all the simple types in the `tree' of
     *               variables rooted at this variable.
     * @return the number of contained variables.
     */
    public int elementCount(boolean leaves) {
        return 1;
    }

    /**
     * Returns the number of variables contained in this object. For simple and
     * vector type variables, it always returns 1.
     *
     * @return the number of contained variables.
     */
    public final int elementCount() {
        return elementCount(false);
    }


    /**
     * Write the variable's declaration in a C-style syntax. This
     * function is used to create textual representation of the Data
     * Descriptor Structure (DDS).  See <em>The OPeNDAP User Manual</em> for
     * information about this structure.
     *
     * @param os          The <code>PrintWriter</code> on which to print the
     *                    declaration.
     * @param space       Each line of the declaration will begin with the
     *                    characters in this string.  Usually used for leading spaces.
     * @param print_semi  a boolean value indicating whether to print a
     *                    semicolon at the end of the declaration.
     * @param constrained a boolean value indicating whether to print
     *                    the declartion dependent on the projection information. <b>This
     *                    is only used by Server side code.</b>
     * @see DDS
     */
    public void printDecl(PrintWriter os, String space,
                          boolean print_semi, boolean constrained) {

        //LogStream.out.println("BaseType.printDecl()...");
        os.print(space + getTypeName() + " " + getEncodedName());
        if (print_semi)
            os.println(";");
    }

    /**
     * Write the variable's declaration in a C-style syntax. This
     * function is used to create textual representation of the Data
     * Descriptor Structure (DDS).  See <em>The OPeNDAP User Manual</em> for
     * information about this structure.
     *
     * @param os         The <code>PrintWriter</code> on which to print the
     *                   declaration.
     * @param space      Each line of the declaration will begin with the
     *                   characters in this string.  Usually used for leading spaces.
     * @param print_semi a boolean value indicating whether to print a
     *                   semicolon at the end of the declaration.
     * @see DDS
     */
    public void printDecl(PrintWriter os, String space,
                          boolean print_semi) {

        printDecl(os, space, print_semi, false);

    }

    /**
     * Print the variable's declaration.  Same as
     * <code>printDecl(os, space, true)</code>.
     *
     * @param os    The <code>PrintWriter</code> on which to print the
     *              declaration.
     * @param space Each line of the declaration will begin with the
     *              characters in this string.  Usually used for leading spaces.
     * @see DDS#print(PrintWriter)
     */
    public final void printDecl(PrintWriter os, String space) {
        printDecl(os, space, true, false);
    }

    /**
     * Print the variable's declaration.  Same as
     * <code>printDecl(os, "    ", true)</code>.
     *
     * @param os The <code>PrintWriter</code> on which to print the
     *           declaration.
     * @see DDS#print(PrintWriter)
     */
    public final void printDecl(PrintWriter os) {
        printDecl(os, "    ", true, false);
    }

    /**
     * Print the variable's declaration using <code>OutputStream</code>.
     *
     * @param os         The <code>OutputStream</code> on which to print the
     *                   declaration.
     * @param space      Each line of the declaration will begin with the
     *                   characters in this string.  Usually used for leading spaces.
     * @param print_semi a boolean value indicating whether to print a
     *                   semicolon at the end of the declaration.
     * @see DDS#print(PrintWriter)
     */
    public final void printDecl(OutputStream os, String space,
                                boolean print_semi, boolean constrained) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printDecl(pw, space, print_semi, constrained);
        pw.flush();
    }

    /**
     * Print the variable's declaration using <code>OutputStream</code>.
     *
     * @param os         The <code>OutputStream</code> on which to print the
     *                   declaration.
     * @param space      Each line of the declaration will begin with the
     *                   characters in this string.  Usually used for leading spaces.
     * @param print_semi a boolean value indicating whether to print a
     *                   semicolon at the end of the declaration.
     * @see DDS#print(PrintWriter)
     */
    public final void printDecl(OutputStream os, String space,
                                boolean print_semi) {
        printDecl(os, space, print_semi, false);
    }

    /**
     * Print the variable's declaration.  Same as
     * <code>printDecl(os, space, true)</code>.
     *
     * @param os    The <code>OutputStream</code> on which to print the
     *              declaration.
     * @param space Each line of the declaration will begin with the
     *              characters in this string.  Usually used for leading spaces.
     * @see DDS#print(PrintWriter)
     */
    public final void printDecl(OutputStream os, String space) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printDecl(pw, space);
        pw.flush();
    }

    /**
     * Print the variable's declaration.  Same as
     * <code>printDecl(os, "    ", true)</code>.
     *
     * @param os The <code>OutputStream</code> on which to print the
     *           declaration.
     * @see DDS#print(PrintWriter)
     */
    public  void printDecl(OutputStream os) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printDecl(pw);
        pw.flush();
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     */
    abstract public void printVal(PrintWriter os, String space,
                                  boolean print_decl_p);

    /**
     * Print the variable's value.  Same as
     * <code>printVal(os, space, true)</code>.
     *
     * @param os    the <code>PrintWriter</code> on which to print the value.
     * @param space this value is passed to the <code>printDecl</code> method,
     *              and controls the leading spaces of the output.
     * @see DataDDS#printVal(PrintWriter)
     */
    public  void printVal(PrintWriter os, String space) {
        printVal(os, space, true);
    }

    /**
     * Print the variable's value using <code>OutputStream</code>.
     *
     * @param os           the <code>OutputStream</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     * @see DataDDS#printVal(PrintWriter)
     */
    public  void printVal(OutputStream os, String space,
                               boolean print_decl_p) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printVal(pw, space, print_decl_p);
        pw.flush();
    }

    /**
     * Print the variable's value using <code>OutputStream</code>.
     *
     * @param os    the <code>OutputStream</code> on which to print the value.
     * @param space this value is passed to the <code>printDecl</code> method,
     *              and controls the leading spaces of the output.
     * @see DataDDS#printVal(PrintWriter)
     */
    public void printVal(OutputStream os, String space) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,Util.UTF8)));
        printVal(pw, space);
        pw.flush();
    }

    /**
     * Checks for internal consistency.  This is important to check for complex
     * constructor classes.
     * For example, an <code>DInt32</code> instance would return false if it had
     * no name defined.  A <code>DGrid</code> instance might return false for
     * more complex reasons, such as having Map arrays of the wrong
     * size or shape.
     * <p/>
     * This method is used by the <code>DDS</code> class, and will rarely, if
     * ever, be explicitly called by a OPeNDAP application program.  A
     * variable must pass this test before it is sent, but there may be
     * many other stages in a retrieve operation where it would fail.
     *
     * @param all For complex constructor types (
     *            <code>DGrid</code>, <code>DSequence</code>, <code>DStructure</code>),
     *            this flag indicates whether to check the
     *            semantics of the member variables, too.
     * @throws BadSemanticsException if semantics are bad, explains why.
     * @see DDS#checkSemantics(boolean)
     */
    public void checkSemantics(boolean all)
            throws BadSemanticsException {
        if (_nameClear == null)
            throw new BadSemanticsException("BaseType.checkSemantics(): Every variable must have a name");
    }

    /**
     * Check semantics.  Same as <code>checkSemantics(false)</code>.
     *
     * @throws BadSemanticsException if semantics are bad, explains why.
     * @see BaseType#checkSemantics(boolean)
     */
    public final void checkSemantics() throws BadSemanticsException {
        checkSemantics(false);
    }

    /**
     * Constructs a new <code>PrimitiveVector</code> object optimized for the
     * particular data type of this <code>BaseType</code>.  For example, a
     * <code>DByte</code> class would create a new
     * <code>BytePrimitiveVector</code> in this call.  This allows for a very
     * optimized, yet type-safe, implementation of <code>DVector</code>
     * functionality.  For non-primitive types, such as
     * <code>DArray</code>, <code>DGrid</code>, <code>DSequence</code>, and
     * <code>DStructure</code>, the default implementation returns a
     * <code>BaseTypePrimitiveVector</code> object which can
     * deserialize an array of complex types.
     *
     * @return a new <code>PrimitiveVector</code> object for the variable type.
     */
    public PrimitiveVector newPrimitiveVector() {
        return new BaseTypePrimitiveVector(this);
    }



    public String getLongName() {

        boolean done = false;

        BaseType parent = (BaseType)getParent();

        String longName = _nameEncoded;

        while (parent != null && !(parent instanceof DDS)) {
            longName = parent.getEncodedName() + "." + longName;
            parent = (BaseType)parent.getParent();
        }
        return (longName);
    }

    // **************************************************************
    //
    // Attribute Table Methods
    //
    // ..............................................................


    public boolean hasAttributes() {


        Enumeration e = _attrTbl.getNames();

        if (e.hasMoreElements())
            return (true);

        return (false);


    }


    public Attribute getAttribute() {
        return _attr;
    }

    public AttributeTable getAttributeTable() {
        return _attrTbl;
    }

    public void addAttributeAlias(String alias, String attributeName)
            throws DASException {
        _attrTbl.addAlias(alias, attributeName);
    }

    public void appendAttribute(String clearname, int type, String value, boolean check)
            throws DASException {
        _attrTbl.appendAttribute(clearname, type, value, check);
    }

    public void appendAttribute(String clearname, int type, String value)
            throws DASException {
        _attrTbl.appendAttribute(clearname, type, value);
    }

    public void addAttributeContainer(AttributeTable at)
            throws AttributeExistsException {
        _attrTbl.addContainer(at.getClearName(), at);
    }

    public AttributeTable appendAttributeContainer(String clearname) {
        return (_attrTbl.appendContainer(clearname));
    }

    public void delAttribute(String clearname) {
        _attrTbl.delAttribute(clearname);
    }

    public void delAttribute(String clearname, int i) throws DASException {
        _attrTbl.delAttribute(clearname, i);
    }

    public Attribute getAttribute(String clearname) {
        return (_attrTbl.getAttribute(clearname));
    }

    public Enumeration getAttributeNames() {
        return (_attrTbl.getNames());
    }

    public void printAttributes(OutputStream os) {
        _attrTbl.print(os);
    }

    public void printAttributes(OutputStream os, String pad) {
        _attrTbl.print(os, pad);
    }

    public void printAttributes(PrintWriter pw) {
        _attrTbl.print(pw);
    }

    public void printAttributes(PrintWriter pw, String pad) {
        _attrTbl.print(pw, pad);
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
        printXML(pw, pad, false);
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

        pw.print(pad + "<" + getTypeName());
        if (_nameClear != null) {
            pw.print(" name=\"" +
                    DDSXMLParser.normalizeToXML(_nameClear) + "\"");
        }

        Enumeration e = getAttributeNames();
        if (e.hasMoreElements()) {
            pw.println(">");
            while (e.hasMoreElements()) {
                String aName = (String) e.nextElement();

                Attribute a = getAttribute(aName);
                if(a!=null)
                    a.printXML(pw, pad + "\t", constrained);

            }
            pw.println(pad + "</" + getTypeName() + ">");
        } else {
            pw.println("/>");
        }


    }

    public void printConstraint(PrintWriter os)
    {
        BaseType parent = (BaseType)getParent();
        BaseType array =  null;
        if(parent != null) {
            if(parent instanceof DArray) {
            array = parent;
            parent = (BaseType)parent.getParent();
            }
        }
        if(array != null)
            array.printConstraint(os);
            else  {
                if(parent != null && !(parent instanceof DDS)) {
                parent.printConstraint(os);
                os.print(".");
            }
                os.print(getEncodedName());
            }
    }


    /**
     * Returns a clone of this <code>BaseType</code>.
     * See DAPNode.cloneDAG.
     *
     * @param map The set of already cloned nodes.
     * @return a clone of this <code>BaseType</code>.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        BaseType bt = (BaseType)super.cloneDAG(map);
        if(this._attrTbl != null)
  	        bt._attrTbl = (AttributeTable) cloneDAG(map,this._attrTbl);
        if(this._attr != null)
	        bt._attr = new Attribute(getClearName(), bt._attrTbl);
        return bt;
    }


}


