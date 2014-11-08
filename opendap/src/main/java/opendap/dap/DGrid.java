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

import opendap.servers.SDArray;
import opendap.dap.parsers.DDSXMLParser;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class holds a <code>DArray</code> and a set of "Map"
 * vectors.  The Map vectors are one-dimensional arrays corresponding
 * to each dimension of the central <code>Array</code>.  Using this scheme, a
 * <code>Grid</code> can represent, in a rectilinear array, data which is not
 * in reality rectilinear.  An example will help make this clear.
 * <p/>
 * Assume that the following array contains measurements of some real
 * quantity, conducted at nine different points in space:
 * <p/>
 * <code><pre>
 * A = [ 1  2  3  4 ]
 *     [ 2  4  6  8 ]
 *     [ 3  6  9  12]
 * </pre></code>
 * <p/>
 * To locate this <code>Array</code> in the real world, we could note the
 * location of one corner of the grid, and the grid spacing.  This would allow
 * us to calculate the location of any of the other points of the
 * <code>Array</code>.
 * <p/>
 * This approach will not work, however, unless the grid spacing is
 * precisely regular.  If the distance between Row 1 and Row 2 is not
 * the same as the distance between Row 2 and Row 3, the scheme will
 * break down.  The solution is to equip the <code>Array</code> with two
 * <code>Map</code> vectors that define the location of each row or column of
 * the array:
 * <p/>
 * <code><pre>
 *       A = [ 1  2  3  4 ] Row = [ 0 ]
 *           [ 2  4  6  8 ]       [ 3 ]
 *           [ 3  6  9  12]       [ 8 ]
 * <p/>
 *  Column = [ 0  2  8  27]
 * </pre></code>
 * <p/>
 * The real location of the point in the first row and column of the
 * array is now exactly fixed at (0,0), and the point in the last row
 * and last column is at (8,27).
 *
 * @author jehamby
 * @version $Revision: 19676 $
 * @see BaseType
 * @see DArray
 */
public class DGrid extends DConstructor implements ClientIO {
    /**
     * The <code>Array</code> part of the <code>DGrid</code>
     */
    public static final int ARRAY = 1;

    /**
     * The Map part of the <code>DGrid</code>
     */
    public static final int MAPS = 2;

    /**
     * The Array component of this <code>DGrid</code>.
     */
    protected DArray arrayVar;

    /**
     * The Map component of this <code>DGrid</code>.
     */
    protected Vector mapVars;

    /**
     * Constructs a new <code>DGrid</code>.
     */
    public DGrid() {
        this(null);
    }

    /**
     * Constructs a new <code>DGrid</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public DGrid(String n) {
        super(n);
        mapVars = new Vector();
    }

    /**
     * Returns the OPeNDAP type name of the class instance as a <code>String</code>.
     *
     * @return the OPeNDAP type name of the class instance as a <code>String</code>.
     */
    public String getTypeName() {
        return "Grid";
    }

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
        if (!leaves)
            return mapVars.size() + 1; // Number of Maps plus 1 Array component
        else {
            int count = 0;
            for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
                BaseType bt = (BaseType) e.nextElement();
                count += bt.elementCount(leaves);
            }
            count += arrayVar.elementCount(leaves);
            return count;
        }
    }

    /**
     * Adds a variable to the container.
     *
     * @param v    the variable to add.
     * @param part the part of the <code>DGrid</code> to be modified.  Allowed
     *             values are <code>ARRAY</code> or <code>MAPS</code>.
     * @throws IllegalArgumentException if an invalid part was given.
     */
    public void addVariable(BaseType v, int part) {

        if (!(v instanceof DArray)) throw new IllegalArgumentException(
                "Grid `" + getEncodedName() +
                        "'s' member `" + arrayVar.getEncodedName() +
                        "' must be an array");

        v.setParent(this);

        switch (part) {

            case ARRAY:
                arrayVar = (DArray) v;
                return;

            case MAPS:
                mapVars.addElement(v);
                return;

            default:
                throw new IllegalArgumentException("addVariable(): Unknown Grid part");
        }
    }

    /**
     * Returns the named variable.
     *
     * @param name the name of the variable.
     * @return the named variable.
     * @throws NoSuchVariableException if the named variable does not
     *                                 exist in this container.
     */
    public BaseType getVariable(String name) throws NoSuchVariableException {

        int dotIndex = name.indexOf('.');

        if (dotIndex != -1) {  // name contains "."

            String aggregate = name.substring(0, dotIndex);
            String field = name.substring(dotIndex + 1);

            BaseType aggRef = getVariable(aggregate);
            if (aggRef instanceof DConstructor)
                return ((DConstructor) aggRef).getVariable(field);  // recurse
            else
                ; // fall through to throw statement
        } else {
            if (arrayVar.getEncodedName().equals(name))
                return arrayVar;

            for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
                BaseType v = (BaseType) e.nextElement();
                if (v.getEncodedName().equals(name))
                    return v;
            }
        }
        throw new NoSuchVariableException("DGrid.getVariable() No such variable: '" + name + "'");
    }

    /**
     * Gets the indexed variable. For a DGrid the index 0 returns the <code>DArray</code> and
     * indexes 1 and higher return the associated map <code>Vector</code>s.
     *
     * @param index the index of the variable in the <code>Vector</code> Vars.
     * @return the indexed variable.
     * @throws NoSuchVariableException if the named variable does not
     *                                 exist in this container.
     */
    public BaseType getVar(int index)
            throws NoSuchVariableException
    {
        if (index == 0) {
            return (arrayVar);
        } else {
            int i = index - 1;
            if (i < mapVars.size())
                return ((BaseType) mapVars.elementAt(i));
            else
                throw new NoSuchVariableException("DGrid.getVariable() No Such variable: " + index + " - 1");
        }
    }
    

    /**
     * Get the number of contained variables (for use with getVar()
     * @return the number of contained variables
     */
    public int getVarCount()
    {
	return mapVars.size() + 1;
    }

    /**
     * Private class for implemantation of the Enumeration. Because
     * DStructure and DSequence are simpler classes and use a single Vector,
     * their implementations of getVariables aren't as fancy.
     */
    class EnumerateDGrid implements Enumeration {
        boolean array;
        Enumeration e;

        EnumerateDGrid() {
            array = false;    // true when the array is/has being/been
            // visited
            e = mapVars.elements();
        }

        public boolean hasMoreElements() {
            return (array == false) || e.hasMoreElements();
        }

        public Object nextElement() {
            if (!array) {
                array = true;
                return arrayVar;
            } else {
                return e.nextElement();
            }
        }
    }

    /**
     * Return an Enumeration that can be used to iterate over the members of
     * a Structure. This implementation provides access to the elements of
     * the Structure. Each Object returned by the Enumeration can be cast to
     * a BaseType.
     *
     * @return An Enumeration
     */
    public Enumeration getVariables() {
        return new EnumerateDGrid();
    }
    /**
     * Checks for internal consistency.  For <code>DGrid</code>, verify that
     * the map variables have unique names and match the number of dimensions
     * of the array variable.
     *
     * @param all for complex constructor types, this flag indicates whether to
     *            check the semantics of the member variables, too.
     * @throws BadSemanticsException if semantics are bad, explains why.
     * @see BaseType#checkSemantics(boolean)
     */
    public void checkSemantics(boolean all) throws BadSemanticsException {
        super.checkSemantics(all);

        Util.uniqueNames(mapVars, getEncodedName(), getTypeName());

        if (arrayVar == null)
            throw new BadSemanticsException("DGrid.checkSemantics(): Null grid base array in `"
                    + getEncodedName() + "'");

        // check semantics of array variable
        arrayVar.checkSemantics(all);

        // enough maps?
        if (mapVars.size() != arrayVar.numDimensions())
            throw new BadSemanticsException("DGrid.checkSemantics(): The number of map variables for grid `"
                    + getEncodedName() + "' does not match the number of dimensions of `"
                    + arrayVar.getEncodedName() + "'");

        //----- I added this next test 12/3/99. As soon as I did I questioned whether or not
        //----- it adds any value. ie: Can it ever happen that this test fails? I don't think
        //----- so now that I have written it...  ndp 12/3/99

        // Is the size of the maps equal to the size of the cooresponding dimensions?
        Enumeration emap = mapVars.elements();

        Enumeration edims = arrayVar.getDimensions();
        int dim = 0;
        while (emap.hasMoreElements() && edims.hasMoreElements()) {

            DArray thisMapArray = (DArray) emap.nextElement();
            Enumeration ema = thisMapArray.getDimensions();
            DArrayDimension thisMapDim = (DArrayDimension) ema.nextElement();

            DArrayDimension thisArrayDim = (DArrayDimension) edims.nextElement();

            if (thisMapDim.getSize() != thisArrayDim.getSize()) {

                throw new BadSemanticsException("In grid '" +
                        getEncodedName() +
                        " The size of dimension " +
                        dim + " in the array component '" +
                        arrayVar.getEncodedName() +
                        "is not equal to the size of the coresponding map vector '" +
                        thisMapArray.getEncodedName() + ".");
            }
            dim++;
        }
        //----- end  ndp 12/3/99

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
     * @see BaseType#printDecl(PrintWriter, String, boolean)
     */
    //Coverity[CALL_SUPER]
    public void printDecl(PrintWriter os, String space, boolean print_semi, boolean constrained) {
        os.println(space + getTypeName() + " {");
        os.println(space + " ARRAY:");
        arrayVar.printDecl(os, space + "    ", true);
        os.println(space + " MAPS:");
        for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
            BaseType bt = (BaseType) e.nextElement();
            bt.printDecl(os, space + "    ", true);
        }
        os.print(space + "} " + getEncodedName());
        if (print_semi)
            os.println(";");
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
     * @see BaseType#printVal(PrintWriter, String, boolean)
     */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {

        if (print_decl_p) {
            printDecl(os, space, false);
            os.print(" = ");
        }

        os.print("{ ARRAY: ");
        arrayVar.printVal(os, "", false);

        os.print(" MAPS: ");
        for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
            BaseType bt = (BaseType) e.nextElement();
            bt.printVal(os, "", false);
            if (e.hasMoreElements())
                os.print(", ");
        }
        os.print(" }");

        if (print_decl_p)
            os.println(";");
    }

    /**
     * Reads data from a <code>DataInputStream</code>. This method is only used
     * on the client side of the OPeNDAP client/server connection.
     *
     * @param source   a <code>DataInputStream</code> to read from.
     * @param sv       the <code>ServerVersion</code> returned by the server.
     * @param statusUI the <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @throws EOFException      if EOF is found before the variable is completely
     *                           deserialized.
     * @throws IOException       thrown on any other InputStream exception.
     * @throws DataReadException if an unexpected value was read.
     * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
     */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
            throws IOException, DataReadException {
        arrayVar.deserialize(source, sv, statusUI);
        for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
            if (statusUI != null && statusUI.userCancelled())
                throw new DataReadException("DGrid.deserialize(): User cancelled");
            ClientIO bt = (ClientIO) e.nextElement();
            bt.deserialize(source, sv, statusUI);
        }
    }

    /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download OPeNDAP data, manipulate
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @throws IOException thrown on any <code>OutputStream</code>
     *                     exception.
     */
    public void externalize(DataOutputStream sink) throws IOException {
        arrayVar.externalize(sink);
        for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
            ClientIO bt = (ClientIO) e.nextElement();
            bt.externalize(sink);
        }
    }


    /**
     *
     * @param pw
     * @param pad
     * @param constrained
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw, String pad, boolean constrained) {


        pw.print(pad + "<Grid");
        if (getEncodedName() != null) {
            pw.print(" name=\"" +
                    DDSXMLParser.normalizeToXML(getClearName()) + "\"");
        }
        pw.println(">");

        Enumeration e = getAttributeNames();

        while (e.hasMoreElements()) {
            String aName = (String) e.nextElement();


           Attribute a = getAttribute(aName);
            if(a!=null)
                a.printXML(pw, pad + "\t", constrained);

        }

        arrayVar.printXML(pw, pad + "\t", constrained);


        e = mapVars.elements();
        while (e.hasMoreElements()) {
            DArray map = (DArray) e.nextElement();

            map.printAsMapXML(pw, pad + "\t", constrained);

        }


        pw.println(pad + "</Grid>");
    }

    // Export for testing
    public DArray getArray() {return arrayVar;}
    public Vector<DArrayDimension> getArrayDims() {return arrayVar.dimVector;}

    /**
         * How many prohected components of this Grid object?
         *
         * @return The number of projected components.
         */
        public int projectedComponents(boolean constrained) {
            int comp;

            if (constrained) {
                comp = ((DArray)arrayVar).isProject() ? 1 : 0;

                Enumeration e = mapVars.elements();

                while (e.hasMoreElements()) {
                    if (((DArray) e.nextElement()).isProject())
                        comp++;
                }
            } else {
                comp = 1 + mapVars.size();
            }

            return comp;
        }


    /**
     * When projected (using whatever the current constraint provides in the way
     * of a projection) am I still a Grid?
     *
     * @return True if projected grid is still a grid. False otherwise.
     */

    public boolean projectionYieldsGrid(boolean constrained) {

        if(!constrained) return true;
        
        // For each dimension in the Array part, check the corresponding Map
        // vector to make sure it is present in the projected Grid. If for each
        // projected dimension in the Array component, there is a matching Map
        // vector, then the Grid is valid.
        boolean valid = true;

        // Don't bother checking if the Array component is not included.
        if (!((SDArray) arrayVar).isProject())
            return false;

        int nadims = arrayVar.numDimensions();
        int nmaps =  getVarCount()-1;
        //Enumeration aDims = arrayVar.getDimensions();
        //Enumeration e = mapVars.elements();
        //while (valid && e.hasMoreElements() && aDims.hasMoreElements()) {

        if(nadims != nmaps)
            valid = false;
        else for(int d=0;d<nadims;d++) {
            try {

            DArrayDimension thisDim = arrayVar.getDimension(d); //(DArrayDimension) aDims.nextElement();
            SDArray mapArray = (SDArray)getVar(d+1); //e.nextElement();
            DArrayDimension mapDim = mapArray.getFirstDimension();

            if (thisDim.getSize() > 0) {
                // LogStream.out.println("Dimension Contains Data.");

                if (mapArray.isProject()) { // This map vector better be projected!
                    // LogStream.out.println("Map Vector Projected, checking projection image...");

                    // Check the matching Map vector; the Map projection must equal
                    // the Array dimension projection

                    // wrong: valid at this point might have been false: valid = true;
                    valid = valid && mapDim.getStart() == thisDim.getStart();
                    valid = valid && mapDim.getStop() == thisDim.getStop();
                    valid = valid && mapDim.getStride() == thisDim.getStride();
                } else {
                    // LogStream.out.println("Map Vector not Projected.");
                    valid = false;
                }

            } else {
                // LogStream.out.println("Dimension empty. Verifying corresponding Map vector not projected...");
                // Corresponding Map vector must be excluded from the
                // projection or it's not a grid.
                valid = !mapArray.isProject();
            }

            }  catch(Exception e) {
                Util.check(e);
                valid=false;
                break;
            }


        }

        //if (e.hasMoreElements() != aDims.hasMoreElements()) valid = false;

        return valid;
    }


    /**
     * Returns a clone of this <code>DGrid</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this object.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        DGrid g = (DGrid) super.cloneDAG(map);
        g.arrayVar = (DArray) cloneDAG(map,arrayVar);
        g.mapVars = new Vector();
        for (int i = 0; i < mapVars.size(); i++) {
            BaseType bt = (BaseType) mapVars.elementAt(i);
	        BaseType btclone = (BaseType)cloneDAG(map,bt);
            g.mapVars.addElement(btclone);
        }
        return g;
    }

}


