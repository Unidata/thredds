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

import java.util.Enumeration;
import java.io.PrintWriter;

/**
 * Contains methods used only by the OPeNDAP constructor classes
 * (<code>DStructure</code>, <code>DSequence</code>, <code>DGrid</code>, and
 * <code>DList</code>).
 *
 * @author jehamby
 * @version $Revision: 19676 $
 * @see DStructure
 * @see DSequence
 * @see DGrid
 */
abstract public class DConstructor extends BaseType {

    /**
     * Constructs a new <code>DConstructor</code>.
     */
    public DConstructor() {
        super();
    }

    /**
     * Constructs a new <code>DConstructor</code> with the given name.
     *
     * @param n The name of the variable.
     */
    public DConstructor(String n) {
        super(n);
    }

    /**
     * Adds a variable to the container.
     *
     * @param v    the variable to add.
     * @param part The part of the constructor data to be modified.
     */
    abstract public void addVariable(BaseType v, int part);

    /**
     * Adds a variable to the container.  Same as <code>addVariable(v, 0)</code>.
     *
     * @param v the variable to add.
     */
    public final void addVariable(BaseType v) {
        addVariable(v, 0);
    }

    /**
     * Gets the named variable.
     *
     * @param name the name of the variable.
     * @return the named variable.
     * @throws NoSuchVariableException if the named variable does not
     *                                 exist in this container.
     */
    abstract public BaseType getVariable(String name)
            throws NoSuchVariableException;

    /**
     * Gets the indexed variable. For a DGrid the index 0 returns the <code>DArray</code> and
     * indexes 1 and higher return the associated map <code>Vector</code>s.
     *
     * @param index the index of the variable in the <code>Vector</code> Vars.
     * @return the named variable.
     * @throws NoSuchVariableException if the named variable does not
     *                                 exist in this container.
     */
    abstract public BaseType getVar(int index)
            throws NoSuchVariableException;

    /**
     * Get the number of contained variables (for use with getVar()
     * @return the number of contained variables
     */
    abstract public int getVarCount();


    /**
     * Return an Enumeration that can be used to iterate over all of the
     * members of the class. Each implementation must define what this means.
     * The intent of this method is to support operations on all members of a
     * Structure, Seqeunce or Grid that can be performed equally. So it is
     * not necessary that this methods be usable, for example, when the
     * caller needs to know that it s dealing with the Array part of a grid.
     *
     * @return An Enumeration object.
     */
    abstract public Enumeration getVariables();


    /**
     *
     * @param bt The BasType object to search.
     * @return true if some child of the passed BaseType has attributes
     * @opendap.ddx.experimental
     */
    protected boolean someChildHasAttributes(BaseType bt) {

        boolean foundit = false;

        if (bt.hasAttributes())
            return (true);

        if (bt instanceof DConstructor) {

            Enumeration e = ((DConstructor) bt).getVariables();

            while (e.hasMoreElements()) {
                BaseType thisBT = (BaseType) e.nextElement();

                foundit = foundit || someChildHasAttributes(thisBT);
            }
        }

        return (foundit);
    }

    /**
     *
     * @param pw Where to print
     * @param pad Padding for iondentation (makes the output easier for humans
     * to read).
     * @param constrained If true then only projected variables (and their
     * Attributes) will be printed.
     * @opendap.ddx.experimental
     */
    //Coverity[CALL_SUPER]
    public void printXML(PrintWriter pw, String pad, boolean constrained) {

        Enumeration e = getAttributeNames();
        Enumeration ve = getVariables();

        boolean hasAttributes = e.hasMoreElements();
        boolean hasVariables = ve.hasMoreElements();

        pw.print(pad + "<" + getTypeName());
        if (getEncodedName() != null) {
            pw.print(" name=\"" +
                    DDSXMLParser.normalizeToXML(getClearName()) + "\"");
        }


        if (hasAttributes || hasVariables) {

            pw.println(">");

            while (e.hasMoreElements()) {
                String aName = (String) e.nextElement();

                Attribute a = getAttribute(aName);
                if(a!=null)
                    a.printXML(pw, pad + "\t", constrained);

            }

            while (ve.hasMoreElements()) {
                BaseType bt = (BaseType) ve.nextElement();
                bt.printXML(pw, pad + "\t", constrained);
            }

            pw.println(pad + "</" + getTypeName() + ">");

        } else {

            pw.println("/>");

        }

    }


}



