/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Vector;
import dods.dap.Server.InvalidParameterException;

/**
 * This class is used to hold arrays of other DODS data. The elements of the
 * array can be simple or compound data types. There is no limit on the
 * number of dimensions an array can have, or on the size of each dimension.
 * <p>
 * If desired, the user can give each dimension of an array a name. You can,
 * for example, have a 360x180 array of temperatures, covering the whole
 * globe with one-degree squares. In this case, you could name the first
 * dimension "Longitude" and the second dimension "Latitude". This can
 * help prevent a great deal of confusion.
 * <p>
 * The <code>DArray</code> is used as part of the <code>DGrid</code> class,
 * where the dimension names are crucial to its structure. The dimension names
 * correspond to "Map" vectors, holding the actual values for that column of
 * the array.
 * <p>
 * Each array dimension carries with it its own projection information. The
 * projection inforamtion takes the form of three integers: the start, stop,
 * and stride values. This is clearest with an example. Consider a
 * one-dimensional array 10 elements long. If the start value of the
 * dimension constraint is 3, then the constrained array appears to be seven
 * elements long. If the stop value is changed to 7, then the array appears
 * to be five elements long. If the stride is changed to two, the array will
 * appear to be 3 elements long. Array constraints are written as
 * <code>[start:stride:stop]</code>.
 *
 * <code><pre>
 * A = [1 2 3 4 5 6 7 8 9 10]
 * A[3::] = [4 5 6 7 8 9 10]
 * A[3::7] = [4 5 6 7 8]
 * A[3:2:7] = [4 6 8]
 * A[0:3:9] = [1 4 7 10]
 * </pre></code>
 *
 * NB: DODS uses zero-based indexing.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see DGrid
 * @see DVector
 * @see BaseType
 */
public class DArray extends DVector implements Cloneable {
    /** A Vector of DArrayDimension information (i.e. the shape) */
    private Vector dimVector;

    /** Constructs a new <code>DArray</code>. */
    public DArray() {
        this(null);
    }

    /**
    * Constructs a new <code>DArray</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public DArray(String n) {
        super(n);
        dimVector = new Vector();
    }

    /**
    * Returns a clone of this <code>DArray</code>.  A deep copy is performed
    * on all data inside the variable.
    *
    * @return a clone of this <code>DArray</code>.
    */
    public Object clone() {
        DArray a = (DArray)super.clone();
        a.dimVector = new Vector();
        for(int i=0; i<dimVector.size(); i++) {
              DArrayDimension d = (DArrayDimension)dimVector.elementAt(i);
              a.dimVector.addElement(d.clone());
        }
        return a;
    }

    /**
    * Returns the DODS type name of the class instance as a <code>String</code>.
    * @return the DODS type name of the class instance as a <code>String</code>.
    */
    public String getTypeName() {
        return "Array";
    }

    /**
    * Checks for internal consistency.  For <code>DArray</code>, verify that
    * the dimension vector is not empty.
    *
    * @param all for complex constructor types, this flag indicates whether to
    *    check the semantics of the member variables, too.
    * @exception BadSemanticsException if semantics are bad, explains why.
    * @see BaseType#checkSemantics(boolean)
    */
    public void checkSemantics(boolean all)
        throws BadSemanticsException {
        super.checkSemantics(all);

        if (dimVector.isEmpty())
            throw new BadSemanticsException("An array variable must have dimensions");
    }



    /**
    * Write the variable's declaration in a C-style syntax. This
    * function is used to create textual representation of the Data
    * Descriptor Structure (DDS).  See <em>The DODS User Manual</em> for
    * information about this structure.
    *
    * @param os The <code>PrintWriter</code> on which to print the
    *    declaration.
    * @param space Each line of the declaration will begin with the
    *    characters in this string.  Usually used for leading spaces.
    * @param print_semi a boolean value indicating whether to print a
    *    semicolon at the end of the declaration.
    *
    * @see BaseType#printDecl(PrintWriter, String, boolean)
    */
    public void printDecl(PrintWriter os, String space, boolean print_semi,
			  boolean constrained) {
		
        // BEWARE! Since printDecl()is (multiplely) overloaded in BaseType and
        // all of the different signatures of printDecl() in BaseType lead to
        // one signature, we must be careful to override that SAME signature
        // here. That way all calls to printDecl() for this object lead to
        // this implementation.



	    //os.println("DArray.printDecl()");

        getPrimitiveVector().printDecl(os, space, false, constrained);
        for(Enumeration e = dimVector.elements(); e.hasMoreElements(); ) {
            DArrayDimension d = (DArrayDimension)e.nextElement();
            os.print("[");
            if(d.getName() != null)
                os.print(d.getName() + " = ");
            os.print(d.getSize() + "]");
        }
        if(print_semi)
            os.println(";");
    }

    /**
    * Prints the value of the variable, with its declaration.  This
    * function is primarily intended for debugging DODS applications and
    * text-based clients such as geturl.
    *
    * @param os the <code>PrintWriter</code> on which to print the value.
    * @param space this value is passed to the <code>printDecl</code> method,
    *    and controls the leading spaces of the output.
    * @param print_decl_p a boolean value controlling whether the
    *    variable declaration is printed as well as the value.
    * @see BaseType#printVal(PrintWriter, String, boolean)
    */
    public void printVal(PrintWriter os, String space, boolean print_decl_p) {
        // print the declaration if print decl is true.
        // for each dimension,
        //   for each element, 
        //     print the array given its shape, number of dimensions.
        // Add the `;'

        if (print_decl_p) {
            printDecl(os, space, false);
            os.print(" = ");
        }

        int dims = numDimensions();
        int shape[] = new int[dims];
        int i = 0;
        for (Enumeration e = dimVector.elements(); e.hasMoreElements(); ) {
            DArrayDimension d = (DArrayDimension)e.nextElement();
            shape[i++] = d.getSize();
        }

        printArray(os, 0, dims, shape, 0);

        if (print_decl_p)
            os.println(";");
    }

    /**
    * Print an array. This is a private member function.
    * @param os is the stream used for writing
    * @param index is the index of VEC to start printing
    * @param dims is the number of dimensions in the array
    * @param shape holds the size of the dimensions of the array.
    * @param offset holds the current offset into the dimension array.
    * @return the number of elements written.
    */
    private int printArray(PrintWriter os, int index, int dims, int shape[],
			   int offset) { 
        if (dims == 1) {
            os.print("{");
            for(int i=0; i<shape[offset]-1; i++) {
                getPrimitiveVector().printSingleVal(os, index++);
                os.print(", ");
            }
            getPrimitiveVector().printSingleVal(os, index++);
            os.print("}");
            return index;
        }
        else {
            os.print("{");
            for(int i=0; i<shape[offset]-1; i++) {
                index = printArray(os, index, dims-1, shape, offset+1);
                os.print(",");
            }
            index = printArray(os, index, dims-1, shape, offset+1);
            os.print("}");
            return index;
        }
    }

    /**
    * Given a size and a name, this function adds a dimension to the
    * array.  For example, if the <code>DArray</code> is already 10 elements
    * long, calling <code>appendDim</code> with a size of 5 will transform the
    * array into a 10x5 matrix.  Calling it again with a size of 2 will
    * create a 10x5x2 array, and so on.
    *
    * @param size the size of the desired new dimension.
    * @param name the name of the new dimension.
    */
    public void appendDim(int size, String name) {
        DArrayDimension newDim = new DArrayDimension(size, name);
        dimVector.addElement(newDim);
    }

    /**
    * Add a dimension to the array.  Same as <code>appendDim(size, null)</code>.
    * @param size the size of the desired new dimension.
    * @see DArray#appendDim(int, String)
    */
    public void appendDim(int size) {
        appendDim(size, null);
    }

    /**
    * Returns an <code>Enumeration</code> of <code>DArrayDimension</code>s
    *   in this array.
    * @return an <code>Enumeration</code> of <code>DArrayDimension</code>s
    *   in this array.
    */
    public final Enumeration getDimensions() {
        return dimVector.elements();
    }

    /**
    * Returns the number of dimensions in this array.
    * @return the number of dimensions in this array.
    */
    public final int numDimensions() {
        return dimVector.size();
    }
  
  
  
    /** Returns the <code>DArrayDimension</code> object for 
    * the dimension requested. It makes sure that the dimension requested
    * exists. 
    */
    public DArrayDimension getDimension(int dimension) throws InvalidParameterException {

	
		// QC the passed dimension
        if (dimension < dimVector.size() )
            return((DArrayDimension)dimVector.get(dimension));
        else
            throw new InvalidParameterException("DArray.getDimension(): Bad dimension request: dimension > # of dimensions");    
    }
    
    /** Returns the <code>DArrayDimension</code> object for 
    * the first dimension. 
    */
    public DArrayDimension getFirstDimension(){
        return((DArrayDimension)dimVector.get(0));
    }
    

}
