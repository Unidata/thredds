// $Id: Index6D.java,v 1.4 2005/12/15 00:29:09 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * Specialization of Index for rank 6 arrays.
 *
 * @see Index
 * @author caron
 * @version $Revision: 1.4 $ $Date: 2005/12/15 00:29:09 $
 */
public class Index6D extends Index {

  /** current element's indices */
  private int curr0, curr1, curr2, curr3, curr4, curr5;
  /** array strides */
  private int stride0, stride1, stride2, stride3, stride4, stride5;
  /** array shapes */
  private int shape0, shape1, shape2, shape3, shape4, shape5;

  Index6D() { super(6); }
  public Index6D( int[] shape) {
    super(shape);
    precalc();
  }

  protected void precalc() {
    shape0 = shape[0];
    shape1 = shape[1];
    shape2 = shape[2];
    shape3 = shape[3];
    shape4 = shape[4];
    shape5 = shape[5];

    stride0 = stride[0];
    stride1 = stride[1];
    stride2 = stride[2];
    stride3 = stride[3];
    stride4 = stride[4];
    stride5 = stride[5];

    curr0 = current[0];
    curr1 = current[1];
    curr2 = current[2];
    curr3 = current[3];
    curr4 = current[4];
    curr5 = current[5];
  }

   public String toString() {
     return curr0+","+curr1+","+curr2+","+curr3+","+curr4+","+curr5;
   }

  public int [] getCurrentCounter() {
    current[0] = curr0;
    current[1] = curr1;
    current[2] = curr2;
    current[3] = curr3;
    current[4] = curr4;
    current[5] = curr5;
    return (int []) current.clone();
  }

  public int currentElement() {
    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2 +
        + curr3*stride3 + curr4*stride4 + curr5*stride5;
  }

  /* public int element(int [] index) {
    int val0 = index[0];
    if (val0 < 0 || val0 >= shape0)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    int val1 = index[1];
    if (val1 < 0 || val1 >= shape1)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    int val2 = index[2];
    if (val2 < 0 || val2 >= shape2)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    int val3 = index[3];
    if (val3 < 0 || val3 >= shape3)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    int val4 = index[4];
    if (val4 < 0 || val4 >= shape4)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    int val5 = index[5];
    if (val5 < 0 || val5 >= shape5)  // check index here
      throw new ArrayIndexOutOfBoundsException();

    return offset + val0*stride0 + val1*stride1 + val2*stride2
        + val3*stride3 + val4*stride4 + val5*stride5;
  } */

  protected int incr() {

      if (++curr5 >= shape5) {
        curr5 = 0;
        if (++curr4 >= shape4) {
          curr4 = 0;
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
          }
        }


    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2 +
        + curr3*stride3 + curr4*stride4 + curr5*stride5;
  }

  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();

    if (dim == 5)
      curr5 = value;
    else if (dim == 4)
      curr4 = value;
    else if (dim == 3)
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

  public Index set4(int v) {
    if (v < 0 || v >= shape4)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr4 = v;
    return this;
  }

  public Index set5(int v) {
    if (v < 0 || v >= shape5)  // check index here
      throw new ArrayIndexOutOfBoundsException();
    curr5 = v;
    return this;
  }

  public Index set(int v0, int v1, int v2, int v3, int v4, int v5) {
    set0(v0);
    set1(v1);
    set2(v2);
    set3(v3);
    set4(v4);
    set5(v5);
    return this;
  }

  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();
    set0(index[0]);
    set1(index[1]);
    set2(index[2]);
    set3(index[3]);
    set4(index[4]);
    set5(index[5]);
    return this;
  }

  public Object clone() {
    return super.clone();
  }

    //experimental : should be package private
  int setDirect(int v0, int v1, int v2, int v3, int v4, int v5) {
    return offset + v0*stride0 + v1*stride1 + v2*stride2 + v3*stride3 +
        v4*stride4 + v5*stride5;
  }


}
