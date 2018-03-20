/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Specialization of Index for rank 1 arrays.
 *
 * @see Index
 * @author caron
 */
public class Index1D extends Index {

  /** current element's indices */
  private int curr0;
  /** array strides */
  private int stride0;
  /** array shapes */
  private int shape0;

  Index1D() { super(1);}
  public Index1D( int[] shape) {
    super(shape);
    precalc();
  }

   public String toString() {
     return Integer.toString(curr0);
   }

  protected void precalc() {
    shape0 = shape[0];
    stride0 = stride[0];
    curr0 = current[0];
  }

 public int [] getCurrentCounter() {
    current[0] = curr0;
    return current.clone();
  }

  public int currentElement() {
    return offset + curr0*stride0;
  }

  public int incr() {
    if (++curr0 >= shape0)
      curr0 = 0;    // rollover !
    return offset + curr0*stride0;
  }


  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr0 = value;
  }

  public Index set0(int v) {
    if (v < 0 || v >= shape0)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr0 = v;
    return this;
  }

  public Index set(int v0) {
    set0(v0);
    return this;
  }

  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();
    set0(index[0]);
    return this;
  }

 public Object clone() {
   return super.clone();
 }

 int setDirect(int v0) {
    if (v0 < 0 || v0 >= shape0)
      throw new ArrayIndexOutOfBoundsException();
    return offset + v0*stride0;
 }

}
