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


package opendap.dap;

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

//    /** A <code>Vector</code> of OPeNDAP BaseTypes to be used by my children */
//    protected Vector vars;

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
    public void printXML(PrintWriter pw, String pad, boolean constrained) {

        Enumeration e = getAttributeNames();
        Enumeration ve = getVariables();

        boolean hasAttributes = e.hasMoreElements();
        boolean hasVariables = ve.hasMoreElements();

        pw.print(pad + "<" + getTypeName());
        if (getName() != null) {
            pw.print(" name=\"" +
                    opendap.dap.XMLparser.DDSXMLParser.normalizeToXML(getClearName()) + "\"");
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



