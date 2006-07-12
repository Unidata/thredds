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
import java.io.*;

/**
 * This class holds a one-dimensional array of DODS data types.
 * It is the parent of both <code>DList</code> and <code>DArray</code>.
 * This class uses a <code>PrimitiveVector</code> to hold the data and
 * deserialize it, thus allowing more efficient storage to be used for the
 * primitive types.
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 * @see DArray
 * @see DList
 * @see PrimitiveVector
 */
abstract public class DVector extends BaseType implements ClientIO {
    /**
    * The values in this <code>DVector</code>, stored in a
    * <code>PrimitiveVector</code>.
    */
    private PrimitiveVector vals;

    /** Constructs a new <code>DVector</code>. */
    public DVector() {
        super();
        }

    /**
    * Returns the DODS type name of the class instance as a <code>String</code>.
    * @return the DODS type name of the class instance as a <code>String</code>.
    */
    public String getTypeName() {
        return "Vector";
    }
    /**
    * Constructs a new <code>DVector</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public DVector(String n) { super(n); }

    /**
    * Returns a clone of this <code>DVector</code>.  A deep copy is performed on
    * all variables inside the <code>DVector</code>.
    *
    * @return a clone of this <code>DVector</code>.
    */
    public Object clone() {
        DVector v = (DVector)super.clone();
        v.vals = (PrimitiveVector)vals.clone();
        return v;
    }

    /**
    * Returns the number of elements in the vector.
    * @return the number of elements in the vector.
    */
    public int getLength() {
        if (vals == null)
            return 0;
        else
            return vals.getLength();
    }

    /**
    * Sets the number of elements in the vector.  Allocates a new
    * array of the desired size.  Note that if this is called multiple times,
    * the old array and its contents will be lost!
    * <p>
    * Only called inside of <code>deserialize</code> method or in derived
    * classes on server.
    *
    * @param len the number of elements in the array.
    */
    public void setLength(int len) {
        vals.setLength(len);
    }

    /**
    * Adds a variable to the container.
    * @param v the variable to add.
    */
    public void addVariable(BaseType v) {
        vals = v.newPrimitiveVector();
        setName(v.getName());
    }

    /**
    * Returns the <code>PrimitiveVector</code> for this vector.  This can be
    * cast to the appropriate type and used by a DODS client to read or set
    * individual values in the vector.
    *
    * @return the attached <code>PrimitiveVector</code>.
    */
    public PrimitiveVector getPrimitiveVector() {
        return vals;
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
    public void printDecl(PrintWriter os, String space,
                                     boolean print_semi, boolean constrained) {

        // BEWARE! Since printDecl()is (multiple) overloaded in BaseType
        // and all of the different signatures of printDecl() in BaseType
        // lead to one signature, we must be careful to override that
        // SAME signature here. That way all calls to printDecl() for
        // this object lead to this implementation.


        //os.println("DVector.printDecl()");
        os.print(space + getTypeName());
        vals.printDecl(os, " ", print_semi,constrained);
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

        if (print_decl_p) {
            printDecl(os, space, false);
            os.print(" = ");
        }

        os.print("{ ");
        vals.printVal(os, "");

        if (print_decl_p)
            os.println("};");
        else
            os.print("}");
    }

    /**
    * Reads data from a <code>DataInputStream</code>. This method is only used
    * on the client side of the DODS client/server connection.
    *
    * @param source a <code>DataInputStream</code> to read from.
    * @param sv the <code>ServerVersion</code> returned by the server.
    * @param statusUI the <code>StatusUI</code> object to use for GUI updates
    *    and user cancellation notification (may be null).
    * @exception EOFException if EOF is found before the variable is completely
    *     deserialized.
    * @exception IOException thrown on any other InputStream exception.
    * @exception DataReadException if an unexpected value was read.
    * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
    */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
                                         throws IOException,
                                         EOFException,
                                         DataReadException {

        // Because arrays of primitive types (ie int32, float32, byte, etc) are
        // handled in the C++ core using the XDR package we must read the
        // length twice for those types. For BaseType vectors, we should read
        // it only once. This is in effect a work around for a bug in the C++
        // core as the C++ core does not consume 2 length values for the
        // BaseType vectors. Bummer...

        int length = source.readInt();

        if(!(vals instanceof BaseTypePrimitiveVector)){
            // because both XDR and DODS write the length, we must read it twice
            int length2 = source.readInt();
            //System.out.println("array1 length read: "+getName()+" "+length+ " -- "+length2);
            //System.out.println("  array type = : "+vals.getClass().getName());

            // QC the second length
            if(length != length2) {
              throw new DataReadException("Inconsistent array length read: "+length+ " != "+length2);
            }
        } /* else {
          System.out.println("array2 length read: "+getName()+" "+length);
          System.out.println("  array type = : "+vals.getClass().getName());
        } */

        if(length < 0)
            throw new DataReadException("Negative array length read.");
        if(statusUI != null)
            statusUI.incrementByteCount(8);
        vals.setLength(length);
        vals.deserialize(source, sv, statusUI);
    }

    /**
    * Writes data to a <code>DataOutputStream</code>. This method is used
    * primarily by GUI clients which need to download DODS data, manipulate
    * it, and then re-save it as a binary file.
    *
    * @param sink a <code>DataOutputStream</code> to write to.
    * @exception IOException thrown on any <code>OutputStream</code>
    * exception.
    */
    public void externalize(DataOutputStream sink) throws IOException {

        // Because arrays of primitive types (ie int32, float32, byte, etc) are
        // handled in the C++ core using the XDR package we must write the
        // length twice for those types. For BaseType vectors, we should write
        // it only once. This is in effect a work around for a bug in the C++
        // core as the C++ core does not consume 2 length values for thge
        // BaseType vectors. Bummer...
        int length = vals.getLength();
        sink.writeInt(length);

        if(!(vals instanceof BaseTypePrimitiveVector)){
            // because both XDR and DODS write the length, we must write it twice
            sink.writeInt(length);
        }

        vals.externalize(sink);
    }

}
