/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.ma2;

/**
 * Specialization of Index for rank 2 arrays.
 *
 * @see Index
 * @author caron
 */
public class Index2D extends Index {

  /** current element's indices */
  private int curr0, curr1;
  /** array strides */
  private int stride0, stride1;
  /** array shapes */
  private int shape0, shape1;

  Index2D() { super(2); }
  public Index2D( int[] shape) {
    super(shape);
    precalc();
  }

  protected void precalc() {
    shape0 = shape[0];
    shape1 = shape[1];

    stride0 = stride[0];
    stride1 = stride[1];

    curr0 = current[0];
    curr1 = current[1];
  }

  public int [] getCurrentCounter() {
    current[0] = curr0;
    current[1] = curr1;
    return current.clone();
  }

   public String toString() {
     return curr0+","+curr1;
   }

  public int currentElement() {
    return offset + curr0*stride0 + curr1*stride1;
  }

  protected int incr() {
      if (++curr1 >= shape1) {
        curr1 = 0;
        if (++curr0 >= shape0) {
          curr0 = 0;    // rollover !
        }
      }
    return offset + curr0*stride0 + curr1*stride1;
  }


  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();
    if (dim == 1)
      curr1 = value;
    else
      curr0 = value;
  }

  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();
    set0(index[0]);
    set1(index[1]);
    return this;
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

  public Index set(int v0, int v1) {
    set0(v0);
    set1(v1);
    return this;
  }

  public Object clone() {
    return super.clone();
  }

  int setDirect(int v0, int v1) {
    if (v0 < 0 || v0 >= shape0)
      throw new ArrayIndexOutOfBoundsException();
    if (v1 < 0 || v1 >= shape1)
      throw new ArrayIndexOutOfBoundsException();
    return offset + v0*stride0 + v1*stride1;
  }

}
