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
 * A vector of doubles.
 *
 * @version $Revision$
 * @author jehamby
 * @see PrimitiveVector
 */
public class Float64PrimitiveVector extends PrimitiveVector implements Cloneable {
  /** the array of <code>double</code> values. */
  private double vals[];

  /**
   * Constructs a new <code>Float64PrimitiveVector</code>.
   * @param var the template <code>BaseType</code> to use.
   */
  public Float64PrimitiveVector(BaseType var) {
    super(var);
  }

  /**
   * Returns a clone of this <code>Float64PrimitiveVector</code>.  A deep
   * copy is performed on all data inside the variable.
   *
   * @return a clone of this <code>Float64PrimitiveVector</code>.
   */
  public Object clone() {
    Float64PrimitiveVector v = (Float64PrimitiveVector)super.clone();
    if (vals != null) {
      v.vals = new double[vals.length];
      System.arraycopy(vals, 0, v.vals, 0, vals.length);
    }
    return v;
  }

  /**
   * Returns the number of elements in the array.
   * @return the number of elements in the array.
   */
  public int getLength() {
    return vals.length;
  }

  /**
   * Sets the number of elements in the array.  Allocates a new primitive
   * array of the desired size.  Note that if this is called multiple times,
   * the old array and its contents will be lost.
   * <p>
   * Only called inside of <code>deserialize</code> method or in derived
   * classes on server.
   *
   * @param len the number of elements in the array.
   */
  public void setLength(int len) {
    vals = new double[len];
  }

  /**
   * Return the i'th value as a <code>double</code>.
   * @param i the index of the value to return.
   * @return the i'th value.
   */
  public final double getValue(int i) {
    return vals[i];
  }

  /**
   * Set the i'th value of the array.
   * @param i the index of the value to set.
   * @param newVal the new value.
   */
  public final void setValue(int i, double newVal) {
    vals[i] = newVal;
  }

  /**
   * Prints the value of all variables in this vector.  This
   * method is primarily intended for debugging DODS applications and
   * text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *    and controls the leading spaces of the output.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  public void printVal(PrintWriter os, String space) {
    int len = vals.length;
    for(int i=0; i<len-1; i++) {
      os.print(vals[i]);
      os.print(", ");
    }
    // print last value, if any, without trailing comma
    if(len > 0)
      os.print(vals[len-1]);
  }

  /**
   * Prints the value of a single variable in this vector.
   * method is used by <code>DArray</code>'s <code>printVal</code> method.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param index the index of the variable to print.
   * @see DArray#printVal(PrintWriter, String, boolean)
   */
  public void printSingleVal(PrintWriter os, int index) {
    os.print(vals[index]);
  }

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used
   * on the client side of the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv The <code>ServerVersion</code> returned by the server.
   *    (used by <code>DSequence</code> to determine which protocol version was
   *    used).
   * @param statusUI The <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @exception DataReadException when invalid data is read, or if the user
   *     cancels the download.
   * @exception EOFException if EOF is found before the variable is completely
   *     deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  public synchronized void deserialize(DataInputStream source,
                                       ServerVersion sv,
                                       StatusUI statusUI)
       throws IOException, EOFException, DataReadException {
    for(int i=0; i<vals.length; i++) {
      vals[i] = source.readDouble();
      if (statusUI != null) {
        statusUI.incrementByteCount(8);
        if (statusUI.userCancelled())
          throw new DataReadException("User cancelled");
      }
    }
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
    for(int i=0; i<vals.length; i++) {
      sink.writeDouble(vals[i]);
    }
  }

  /**
   * Write a subset of the data to a <code>DataOutputStream</code>.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @param start: starting index (i=start)
   * @param stop: ending index (i<=stop)
   * @param stride: index stride (i+=stride)
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  public void externalize(DataOutputStream sink, int start, int stop, int stride) throws IOException {
    for (int i=start; i<=stop; i+=stride)
      sink.writeDouble(vals[i]);
  }

    /**
    * Returns (a reference to) the internal storage for this PrimitiveVector
    * object.
    * <h2>WARNING:</h2>
    * Because this method breaks encapsulation rules the user must beware!
    * If we (the DODS prgramming team) choose to change the internal
    * representation(s) of these types your code will probably break.
    * <p>
    * This method is provided as an optimization to eliminate massive
    * copying of data.
    *
    * @return The internal array of doubles.
    */
    public Object getInternalStorage() {
        return(vals);
    }

    /**
    * Set the internal storage for PrimitiveVector.
    * <h2><i>WARNING:</i></h2>
    * Because this method breaks encapsulation rules the user must beware!
    * If we (the DODS prgramming team) choose to change the internal
    * representation(s) of these types your code will probably break.
    * <p>
    * This method is provided as an optimization to eliminate massive
    * copying of data.
    */
    public void setInternalStorage(Object o) {
      vals = (double []) o;
    }

  /**
   * Create a new primitive vector using a subset of the data.
   *
   * @param start: starting index (i=start)
   * @param stop: ending index (i<=stop)
   * @param stride: index stride (i+=stride)
   * @return new primitive vector, of type Float64PrimitiveVector.
   */
  public PrimitiveVector subset( int start, int stop, int stride) {
    Float64PrimitiveVector n = new Float64PrimitiveVector(getTemplate());
    stride = Math.max( stride, 1);
    stop = Math.max( start, stop);
    int length = 1 + (stop - start) / stride;
    n.setLength( length);

    int count=0;
    for (int i=start; i<=stop; i+=stride) {
      n.setValue(count, vals[i]);
      count++;
    }
    return n;
  }




}
