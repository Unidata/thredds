/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Specialization of Index for rank 4 arrays.
 *
 * @see Index
 * @author caron
 */
public class Index4D extends Index {

  /** current element's indices */
  private int curr0, curr1, curr2, curr3;
  /** array strides */
  private int stride0, stride1, stride2, stride3;
  /** array shapes */
  private int shape0, shape1, shape2, shape3;

  Index4D() { super(4); }
  public Index4D( int[] shape) {
    super(shape);
    precalc();
  }

  protected void precalc() {
    shape0 = shape[0];
    shape1 = shape[1];
    shape2 = shape[2];
    shape3 = shape[3];

    stride0 = stride[0];
    stride1 = stride[1];
    stride2 = stride[2];
    stride3 = stride[3];

    curr0 = current[0];
    curr1 = current[1];
    curr2 = current[2];
    curr3 = current[3];
  }

   public String toString() {
     return curr0+","+curr1+","+curr2+","+curr3;
   }

  public int [] getCurrentCounter() {
    current[0] = curr0;
    current[1] = curr1;
    current[2] = curr2;
    current[3] = curr3;
    return current.clone();
  }

  public int currentElement() {
    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2 +
        + curr3*stride3;
  }

  public int incr() {
    if (++curr3 >= shape3) {
      curr3 = 0;
      if (++curr2 >= shape2) {
        curr2 = 0;
        if (++curr1 >= shape1) {
          curr1 = 0;
          if (++curr0 >= shape0) {
            curr0 = 0;    // rollover !
          }
        }
      }
    }

    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2 + curr3*stride3;
  }

  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();

    if (dim == 3)
      curr3 = value;
    else if (dim == 2)
      curr2 = value;
    else if (dim == 1)
      curr1 = value;
    else
      curr0 = value;
  }

  public Index set0(int v) {
    if (v < 0 || v >= shape0)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr0 = v;
    return this;
  }

  public Index set1(int v) {
    if (v < 0 || v >= shape1)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr1 = v;
    return this;
  }

  public Index set2(int v) {
    if (v < 0 || v >= shape2)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr2 = v;
    return this;
  }

  public Index set3(int v) {
    if (v < 0 || v >= shape3)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr3 = v;
    return this;
  }

  public Index set(int v0, int v1, int v2, int v3) {
    set0(v0);
    set1(v1);
    set2(v2);
    set3(v3);
    return this;
  }

  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();
    set0(index[0]);
    set1(index[1]);
    set2(index[2]);
    set3(index[3]);
    return this;
  }

  public Object clone() {
    return super.clone();
  }

    //experimental : should be package private
  int setDirect(int v0, int v1, int v2, int v3) {
    return offset + v0*stride0 + v1*stride1 + v2*stride2 + v3*stride3;
  }


}