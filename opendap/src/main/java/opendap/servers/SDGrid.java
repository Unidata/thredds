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


package opendap.servers;

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

import opendap.dap.*;
import opendap.dap.parsers.DDSXMLParser;

/**
 * Holds a OPeNDAP Server <code>Grid</code> value.
 *
 * @author ndp
 * @version $Revision: 19676 $
 * @see BaseType
 */
public abstract class SDGrid extends DGrid implements ServerArrayMethods, RelOps {
    private boolean Synthesized;
    private boolean ReadMe;
    Vector AP;

    /**
     * Constructs a new <code>SDGrid</code>.
     */
    public SDGrid() {
        super();
        Synthesized = false;
        ReadMe = false;
    }

    /**
     * Constructs a new <code>SDGrid</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public SDGrid(String n) {
        super(n);
        Synthesized = false;
        ReadMe = false;
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
    public void printDecl(PrintWriter os, String space, boolean print_semi, boolean constrained) {

        // BEWARE! Since printDecl()is (multiple) overloaded in BaseType
        // and all of the different signatures of printDecl() in BaseType
        // lead to one signature, we must be careful to override that
        // SAME signature here. That way all calls to printDecl() for
        // this object lead to this implementation.


        boolean isSingle = false;
        boolean isStructure = false;
        boolean isGrid = false;
        boolean psemi = true;

        //os.println("The grid contains "+projectedComponents(true)+" projected components");

        if (constrained && projectedComponents(true) == 0)
            return;

        // If we are printing the declaration of a constrained Grid then check for
        // the case where the projection removes all but one component; the
        // resulting object is a simple array.
        // 2013-2-26: Heimbigner : this is incorrect, even single
        // projected components should be in a structure.

        /* Wrong
            if (constrained && projectedComponents(true) == 1) {
            //os.println("It's a single Array.");
            isSingle = true;
            psemi = print_semi;
        } */

        // If there are M (< N) components (Array and Maps combined) in a N
        // component Grid, send the M components as elements of a Structure.
        // This will preserve the grouping without violating the rules for a
        // Grid.
        else if (constrained && !projectionYieldsGrid(true)) {
            //os.println("It's a Structure.");
            isStructure = true;
        } else {
            // The number of elements in the (projected) Grid must be such that
            // we have a valid Grid object; send it as such.
            //os.println("It's a Grid.");
            isGrid = true;
        }

        if (isGrid) os.println(space + getTypeName() + " {");
        if (isGrid) os.println(space + " ARRAY:");

        if (isStructure) os.println(space + "Structure {");

        ((SDArray) arrayVar).printDecl(os, space + "    ", psemi, constrained);

        if (isGrid) os.println(space + " MAPS:");
        for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
            SDArray sda = (SDArray) e.nextElement();
            sda.printDecl(os, space + "    ", psemi, constrained);
        }

        if (isStructure || isGrid) {
            os.print(space + "} " + getEncodedName());
            if (print_semi)
                os.println(";");
        }

        return;
    }


    /**
     * Prints the value of the variable, with its declaration.  This
     * function is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     * <p/>
     * <h2> Important Note</h2>
     * This method overrides the BaseType method of the same name and
     * type signature and it significantly changes the behavior for all versions
     * of <code>printVal()</code> for this type:
     * <b><i> All the various versions of printVal() will only
     * print a value, or a value with declaration, if the variable is
     * in the projection.</i></b>
     * <br>
     * <br>In other words, if a call to
     * <code>isProject()</code> for a particular variable returns
     * <code>true</code> then <code>printVal()</code> will print a value
     * (or a declaration and a value).
     * <br>
     * <br>If <code>isProject()</code> for a particular variable returns
     * <code>false</code> then <code>printVal()</code> is basically a No-Op.
     * <br>
     * <br>
     *
     * @param os           the <code>PrintWriter</code> on which to print the value.
     * @param space        this value is passed to the <code>printDecl</code> method,
     *                     and controls the leading spaces of the output.
     * @param print_decl_p a boolean value controlling whether the
     *                     variable declaration is printed as well as the value.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     * @see ServerMethods#isProject()
     */

    public void printVal(PrintWriter os, String space, boolean print_decl_p) {

        if (!isProject())
            return;

        //System.out.println("\nSome Part of this object is projected...");

        if (print_decl_p) {
            printDecl(os, space, false, true);
            os.print(" = ");
        }

        boolean isStillGrid = projectionYieldsGrid(true);

        os.print("{ ");
        if (isStillGrid) os.print("ARRAY: ");

        if (((SDArray) arrayVar).isProject())
            arrayVar.printVal(os, "", false);

        if (isStillGrid) os.print(" MAPS: ");

        boolean firstPass = true;
        Enumeration e = mapVars.elements();
        while (e.hasMoreElements()) {

            SDArray sda = (SDArray) e.nextElement();

            if (((SDArray) sda).isProject()) {
                if (!firstPass) os.print(", ");
                sda.printVal(os, "", false);
                firstPass = false;
            }

        }
        os.print(" }");

        if (print_decl_p)
            os.println(";");

    }


    /**
     * Adds a variable to the container. This overrides the same method in
     * the parent class <code>DGrid</code> in order to add array projection
     * functionality.
     *
     * @param v    the variable to add.
     * @param part the part of the <code>DGrid</code> to be modified.  Allowed
     *             values are <code>ARRAY</code> or <code>MAPS</code>.
     * @throws IllegalArgumentException if an invalid part was given.
     */
    public void addVariable(BaseType v, int part) {
        super.addVariable(v, part);
    }

    // --------------- Projection Interface

    /**
     * Set the state of this variable's projection. <code>true</code> means
     * that this variable is part of the current projection as defined by
     * the current constraint expression, otherwise the current projection
     * for this variable should be <code>false</code>.
     *
     * @param state <code>true</code> if the variable is part of the current
     *              projection, <code>false</code> otherwise.
     * @param all   If <code>true</code>, set the Project property of all the
     *              members (and their children, and so on).
     * @see CEEvaluator
     */
    @Override
    public void setProject(boolean state, boolean all) {
        setProjected(state);
        if (all) {
            // System.out.println("SDGrid:setProject: Blindly setting Project");
            ((SDArray) arrayVar).setProject(state);

            for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
                ServerMethods sm = (ServerMethods) e.nextElement();
                sm.setProject(state);
            }
        }
    }


// --------------- RelOps Interface

    /**
     * The RelOps interface defines how each type responds to relational
     * operators. Most (all?) types will not have sensible responses to all of
     * the relational operators (e.g. DGrid won't know how to match a regular
     * expression but DString will). For those operators that are nonsensical a
     * class should throw InvalidOperatorException.
     */

    public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Equals (=) operator does not work with the type SDGrid!");
    }

    public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Not Equals (!=) operator does not work with the type SDGrid!");
    }

    public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Greater Than (>)operator does not work with the type SDGrid!");
    }

    public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("GreaterThan or equals (<=) operator does not work with the type SDGrid!");
    }

    public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("LessThan (<) operator does not work with the type SDGrid!");
    }

    public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("LessThan oe equals (<=) operator does not work with the type SDGrid!");
    }

    public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
        throw new InvalidOperatorException("Regular Expression's don't work with the type SDGrid!");
    }

// --------------- FileIO Interface

    /**
     * Set the Synthesized property.
     *
     * @param state If <code>true</code> then the variable is considered a
     *              synthetic variable and no part of OPeNDAP will ever try to read it from a
     *              file, otherwise if <code>false</code> the variable is considered a
     *              normal variable whose value should be read using the
     *              <code>read()</code> method. By default this property is false.
     * @see #isSynthesized()
     * @see #read(String, Object)
     */
    public void setSynthesized(boolean state) {
        Synthesized = state;
    }

    /**
     * Get the value of the Synthesized property.
     *
     * @return <code>true</code> if this is a synthetic variable,
     *         <code>false</code> otherwise.
     */
    public boolean isSynthesized() {
        return (Synthesized);
    }

    /**
     * Set the Read property. A normal variable is read using the
     * <code>read()</code> method. Once read the <em>Read</em> property is
     * <code>true</code>. Use this function to manually set the property
     * value. By default this property is false.
     *
     * @param state <code>true</code> if the variable has been read,
     *              <code>false</code> otherwise.
     * @see #isRead()
     * @see #read(String, Object)
     */
    public void setRead(boolean state) {
        ReadMe = state;
    }

    /**
     * Get the value of the Read property.
     *
     * @return <code>true</code> if the variable has been read,
     *         <code>false</code> otherwise.
     * @see #read(String, Object)
     * @see #setRead(boolean)
     */
    public boolean isRead() {
        return (ReadMe);
    }

    /**
     * Read a value from the named dataset for this variable.
     *
     * @param datasetName String identifying the file or other data store
     *                    from which to read a vaue for this variable.
     * @param specialO    This <code>Object</code> is a goody that is used by Server implementations
     *                    to deliver important, and as yet unknown, stuff to the read method. If you
     *                    don't need it, make it a <code>null</code>.
     * @return <code>true</code> if more data remains to be read, otherwise
     *         <code>false</code>. This is an abtsract method that must be implemented
     *         as part of the installation/localization of a OPeNDAP server.
     * @throws IOException
     * @throws EOFException
     */
    public abstract boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException, EOFException;


    /**
     * Server-side serialization for OPeNDAP variables (sub-classes of
     * <code>BaseType</code>).
     * This does not send the entire class as the Java <code>Serializable</code>
     * interface does, rather it sends only the binary data values. Other software
     * is responsible for sending variable type information (see <code>DDS</code>).
     * <p/>
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * on the server side of the OPeNDAP client/server connection, and possibly
     * by GUI clients which need to download OPeNDAP data, manipulate it, and
     * then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @throws IOException thrown on any <code>OutputStream</code> exception.
     * @see BaseType
     * @see DDS
     * @see ServerDDS
     */
    public void serialize(String dataset, DataOutputStream sink, CEEvaluator ce, Object specialO)
            throws NoSuchVariableException, DAP2ServerSideException, IOException {

        if (!isRead())
            read(dataset, specialO);

        if (ce.evalClauses(specialO)) {
            if (((ServerMethods) arrayVar).isProject())
                ((ServerMethods) arrayVar).serialize(dataset, sink, ce, specialO);

            for (Enumeration e = mapVars.elements(); e.hasMoreElements();) {
                ServerMethods sm = (ServerMethods) e.nextElement();
                if (sm.isProject())
                    sm.serialize(dataset, sink, ce, specialO);
            }
        }
    }

//---------------------------------------------------------------------------------
//------------------------- Array Projection Interface ----------------------------
//..................................................................................

    /**
     * Set the projection information for this dimension. The internal <code>DArray</code> is
     * retrieved and then the <code>DArrayDimension</code>
     * associated with the <code>dimension</code> specified is retrieved and the <code>start</code>
     * <code>stride</code> and <code>stop</code> parameters are passed to its
     * <code>setProjection()</code> method.
     *
     * @param dimension The dimension that is to be modified.
     * @param start     The starting point for the projection of this <code>DArrayDimension</code>.
     * @param stride    The size of the stride for the projection of this <code>DArrayDimension</code>.
     * @param stop      The stopping point for the projection of this <code>DArrayDimension</code>.
     * @see DArray
     * @see DArrayDimension
     */
    public void setProjection(int dimension, int start, int stride, int stop)
            throws InvalidDimensionException, SBHException {
        try {
            DArray a = (DArray) getVar(0);
            DArrayDimension d = a.getDimension(dimension);
            d.setProjection(start, stride, stop);

            DArray map = (DArray) getVar(dimension + 1);
            DArrayDimension mapD = map.getDimension(0);
            mapD.setProjection(start, stride, stop);
        }
        catch (NoSuchVariableException e) {
            throw new InvalidDimensionException("SDGrid.setProjection(): Bad Value for dimension!: "
                    + e.getMessage());
        }
    }


    /**
     * Gets the <b>start</b> value for the projection of the
     * <code>dimension</code> indicated. The parameter
     * <code>dimension</code> is checked against the instance of the
     * <code>SDArray</code> for bounds violation.
     *
     * @param dimension The dimension from whose projection to retrieve the
     *                  <code>start</code> value.
     */
    public int getStart(int dimension) throws InvalidDimensionException {
        try {
            DArray a = (DArray) getVar(0);
            DArrayDimension d = a.getDimension(dimension);
            return (d.getStart());
        }
        catch (NoSuchVariableException e) {
            throw new InvalidDimensionException("SDGrid.getStart(): Bad Value for dimension!: "
                    + e.getMessage());
        }
    }


    /**
     * Gets the <b>stride</b> value for the projection of the
     * <code>dimension</code> indicated. The parameter
     * <code>dimension</code> is checked against the instance of the
     * <code>SDArray</code> for bounds violation.
     *
     * @param dimension The dimension from whose projection to retrieve the
     *                  <code>stride</code> value.
     */
    public int getStride(int dimension) throws InvalidDimensionException {
        try {
            DArray a = (DArray) getVar(0);
            DArrayDimension d = a.getDimension(dimension);
            return (d.getStride());
        }
        catch (NoSuchVariableException e) {
            throw new InvalidDimensionException("SDGrid.getStride(): Bad Value for dimension!: "
                    + e.getMessage());
        }
    }


    /**
     * Gets the <b>stop</b> value for the projection of the
     * <code>dimension</code> indicated. The parameter
     * <code>dimension</code> is checked against the instance of the
     * <code>SDArray</code> for bounds violation.
     *
     * @param dimension The dimension from whose projection to retrieve the
     *                  <code>stop</code> value.
     */
    public int getStop(int dimension) throws InvalidDimensionException {
        try {
            DArray a = (DArray) getVar(0);
            DArrayDimension d = a.getDimension(dimension);
            return (d.getStop());
        }
        catch (NoSuchVariableException e) {
            throw new InvalidDimensionException("SDGrid.getStop(): Bad Value for dimension!: "
                    + e.getMessage());
        }
    }


    /**
     * Write the variable's declaration in a C-style syntax. This
     * function is used to create textual representation of the Data
     * Descriptor Structure (DDS).  See <em>The OPeNDAP User Manual</em> for
     * information about this structure.
     *
     * @see BaseType#printDecl(PrintWriter, String, boolean)
     * @opendap.ddx.experimental
     */
    public void printXML(PrintWriter pw, String pad, boolean constrained) {

        // BEWARE! Since printDecl()is (multiple) overloaded in BaseType
        // and all of the different signatures of printDecl() in BaseType
        // lead to one signature, we must be careful to override that
        // SAME signature here. That way all calls to printDecl() for
        // this object lead to this implementation.


        boolean isSingle = false;
        boolean isStructure = false;
        boolean isGrid = false;
        boolean psemi = true;

        //os.println("The gird contains "+projectedComponents(true)+" projected components");

        if (constrained && projectedComponents(true) == 0)
            return;

        // If we are printing the declaration of a constrained Grid then check for
        // the case where the projection removes all but one component; the
        // resulting object is a simple array.

        if (constrained && projectedComponents(true) == 1) {
            //os.println("It's a single Array.");
            isSingle = true;
        }
        // If there are M (< N) componets (Array and Maps combined) in a N
        // component Grid, send the M components as elements of a Struture.
        // This will preserve the grouping without violating the rules for a
        // Grid.
        else if (constrained && !projectionYieldsGrid(true)) {
            //os.println("It's a Structure.");
            isStructure = true;
        } else {
            // The number of elements in the (projected) Grid must be such that
            // we have a valid Grid object; send it as such.
            //os.println("It's a Grid.");
            isGrid = true;
        }

        if (isGrid) {
            pw.print(pad + "<Grid ");
            if (getEncodedName() != null) {
                pw.print(" name=\"" +
                        DDSXMLParser.normalizeToXML(getEncodedName()) + "\"");
            }
            pw.println(">");
        }

        if (isStructure) {

            pw.print(pad + "<Structure");
            if (getEncodedName() != null) {
                pw.print(" name=\"" +
                        DDSXMLParser.normalizeToXML(getEncodedName()) + "\"");
            }
            pw.println(">");

        }

        Enumeration e = getAttributeNames();

        while (e.hasMoreElements()) {
            String aName = (String) e.nextElement();
            Attribute a = getAttribute(aName);
            if(a!=null)
                a.printXML(pw, pad + "\t", constrained);
        }

        ((SDArray) arrayVar).printXML(pw, pad + (isSingle ? "" : "\t"), constrained);

        if (isGrid) {

            e = mapVars.elements();
            while (e.hasMoreElements()) {
                SDArray map = (SDArray) e.nextElement();
                //Coverity[DEADCODE]
                map.printAsMapXML(pw, pad + (isSingle ? "" : "\t"), constrained);
            }

        } else {

            e = mapVars.elements();
            while (e.hasMoreElements()) {
                SDArray sda = (SDArray) e.nextElement();
                sda.printXML(pw, pad + (isSingle ? "" : "\t"), constrained);
            }

        }


        if (isStructure) {
            pw.println(pad + "</Structure>");
        } else if (isGrid) {
            pw.println(pad + "</Grid>");
        }

        return;
    }

/*

	if(isGrid){


            os.println(space + getTypeName() + " {" );
            os.println(space + " ARRAY:");

	    ((SDArray)arrayVar).printDecl(os, space + (isSingle ? "":"    ") , psemi, constrained);

            os.println(space + " MAPS:");

            for(Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
                SDArray sda = (SDArray)e.nextElement();
                sda.printDecl(os, space + (isSingle ? "":"    ") , psemi, constrained);
            }

            os.print(space + "} " + getName());
            os.println(";");

 	}

        if(isStructure){

            os.println(space + "Structure {");
            ((SDArray)arrayVar).printDecl(os, space + (isSingle ? "":"    ") , psemi, constrained);


            for(Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
                SDArray sda = (SDArray)e.nextElement();
                sda.printDecl(os, space + (isSingle ? "":"    ") , psemi, constrained);
            }

            os.print(space + "} " + getName());
            os.println(";");

	}

	if(isSingle){


            ((SDArray)arrayVar).printDecl(os, space + (isSingle ? "":"    ") , psemi, constrained);

            for(Enumeration e = mapVars.elements(); e.hasMoreElements(); ) {
                SDArray sda = (SDArray)e.nextElement();
                sda.printDecl(os, space , psemi, constrained);
            }

	}

*/

}
