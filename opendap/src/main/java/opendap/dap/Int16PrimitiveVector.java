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

import java.io.*;

/**
 * A vector of shorts. (as in 16bit ints :)
 *
 * @author npotter
 * @version $Revision: 15901 $
 * @see PrimitiveVector
 */
public class Int16PrimitiveVector extends PrimitiveVector
{
    /**
     * the array of <code>short</code> values.
     */
    private short vals[];

    /**
     * Constructs a new <code>Int16PrimitiveVector</code>.
     *
     * @param var the template <code>BaseType</code> to use.
     */
    public Int16PrimitiveVector(BaseType var) {
        super(var);
    }

    /**
     * Returns the number of elements in the array.
     *
     * @return the number of elements in the array.
     */
    public int getLength() {
        return vals.length;
    }

    /**
     * Sets the number of elements in the array.  Allocates a new primitive
     * array of the desired size.  Note that if this is called multiple times,
     * the old array and its contents will be lost.
     * <p/>
     * Only called inside of <code>deserialize</code> method or in derived
     * classes on server.
     *
     * @param len the number of elements in the array.
     */
    public void setLength(int len) {
        vals = new short[len];
    }

    /**
     * Return the i'th value as a <code>short</code>.
     *
     * @param i the index of the value to return.
     * @return the i'th value.
     */
    public final short getValue(int i) {
        return vals[i];
    }

    /**
     * Set the i'th value of the array.
     *
     * @param i      the index of the value to set.
     * @param newVal the new value.
     */
    public final void setValue(int i, short newVal) {
        vals[i] = newVal;
    }

    /**
     * Prints the value of all variables in this vector.  This
     * method is primarily intended for debugging OPeNDAP applications and
     * text-based clients such as geturl.
     *
     * @param os    the <code>PrintWriter</code> on which to print the value.
     * @param space this value is passed to the <code>printDecl</code> method,
     *              and controls the leading spaces of the output.
     * @see BaseType#printVal(PrintWriter, String, boolean)
     */
    public  void printVal(PrintWriter os, String space) {
        int len = vals.length;
        for (int i = 0; i < len - 1; i++) {
            os.print(vals[i]);
            os.print(", ");
        }
        // print last value, if any, without trailing comma
        if (len > 0)
            os.print(vals[len - 1]);
    }

    /**
     * Prints the value of a single variable in this vector.
     * method is used by <code>DArray</code>'s <code>printVal</code> method.
     *
     * @param os    the <code>PrintWriter</code> on which to print the value.
     * @param index the index of the variable to print.
     * @see DArray#printVal(PrintWriter, String, boolean)
     */
    public void printSingleVal(PrintWriter os, int index) {
        os.print(vals[index]);
    }

    /**
     * Reads data from a <code>DataInputStream</code>. This method is only used
     * on the client side of the OPeNDAP client/server connection.
     *
     * @param source   a <code>DataInputStream</code> to read from.
     * @param sv       The <code>ServerVersion</code> returned by the server.
     *                 (used by <code>DSequence</code> to determine which protocol version was
     *                 used).
     * @param statusUI The <code>StatusUI</code> object to use for GUI updates
     *                 and user cancellation notification (may be null).
     * @throws DataReadException when invalid data is read, or if the user
     *                           cancels the download.
     * @throws EOFException      if EOF is found before the variable is completely
     *                           deserialized.
     * @throws IOException       thrown on any other InputStream exception.
     * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
     */
    public synchronized void deserialize(DataInputStream source,
                                         ServerVersion sv,
                                         StatusUI statusUI)
            throws IOException, EOFException, DataReadException {
        for (int i = 0; i < vals.length; i++) {
            vals[i] = (short) source.readInt();
            if (statusUI != null) {
                statusUI.incrementByteCount(4);
                if (statusUI.userCancelled())
                    throw new DataReadException("User cancelled");
            }
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
        for (int i = 0; i < vals.length; i++) {
            sink.writeInt((int) (vals[i]));
        }
    }

    /**
     * Write a subset of the data to a <code>DataOutputStream</code>.
     *
     * @param sink    a <code>DataOutputStream</code> to write to.
     * @param start  starting index (i=start)
     * @param stop   ending index (i<=stop)
     * @param stride index stride (i+=stride)
     * @throws IOException thrown on any <code>OutputStream</code> exception.
     */
    public void externalize(DataOutputStream sink, int start, int stop, int stride) throws IOException {
        for (int i = start; i <= stop; i += stride)
            sink.writeInt((int) vals[i]);
    }

    /**
     * Returns (a reference to) the internal storage for this PrimitiveVector
     * object.
     * <h2>WARNING:</h2>
     * Because this method breaks encapsulation rules the user must beware!
     * If we (the OPeNDAP prgramming team) choose to change the internal
     * representation(s) of these types your code will probably break.
     * <p/>
     * This method is provided as an optimization to eliminate massive
     * copying of data.
     *
     * @return The internal array of shorts.
     */
    public Object getInternalStorage() {
        return (vals);
    }

    /**
     * Set the internal storage for PrimitiveVector.
     * <h2><i>WARNING:</i></h2>
     * Because this method breaks encapsulation rules the user must beware!
     * If we (the OPeNDAP prgramming team) choose to change the internal
     * representation(s) of these types your code will probably break.
     * <p/>
     * This method is provided as an optimization to eliminate massive
     * copying of data.
     */
    public void setInternalStorage(Object o) {
        vals = (short []) o;
    }

    /**
     * Create a new primitive vector using a subset of the data.
     *
     * @param start  starting index (i=start)
     * @param stop   ending index (i<=stop)
     * @param stride index stride (i+=stride)
     * @return new primitive vector, of type Int16PrimitiveVector.
     */
    public PrimitiveVector subset(int start, int stop, int stride) {
        Int16PrimitiveVector n = new Int16PrimitiveVector(getTemplate());
        stride = Math.max(stride, 1);
        stop = Math.max(start, stop);
        int length = 1 + (stop - start) / stride;
        n.setLength(length);

        int count = 0;
        for (int i = start; i <= stop; i += stride) {
            n.setValue(count, vals[i]);
            count++;
        }
        return n;
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
   public void printVal(PrintWriter os, String space,
                                boolean print_decl_p) {}

    /**
     * Returns a clone of this <code>Int16PrimitiveVector</code>.
     * See DAPNode.cloneDag()
     *
     * @param map track previously cloned nodes
     * @return a clone of this <code>Int16PrimitiveVector</code>.
     */
    public DAPNode cloneDAG(CloneMap map)
        throws CloneNotSupportedException
    {
        Int16PrimitiveVector v = (Int16PrimitiveVector) super.cloneDAG(map);
        if (vals != null) {
            v.vals = new short[vals.length];
            System.arraycopy(vals, 0, v.vals, 0, vals.length);
        }
        return v;
    }


}


