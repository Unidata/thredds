// $Id: Index3D.java,v 1.4 2005/12/15 00:29:09 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * Specialization of Index for rank 3 arrays.
 *
 * @see Index
 * @author caron
 * @version $Revision: 1.4 $ $Date: 2005/12/15 00:29:09 $
 */
public class Index3D extends Index {

  /** current element's indices */
  private int curr0, curr1, curr2;
  /** array strides */
  private int stride0, stride1, stride2;
  /** array shapes */
  private int shape0, shape1, shape2;

  Index3D() { super(3); }
  public Index3D( int[] shape) {
    super(shape);
    precalc();
  }

  protected void precalc() {
    shape0 = shape[0];
    shape1 = shape[1];
    shape2 = shape[2];

    stride0 = stride[0];
    stride1 = stride[1];
    stride2 = stride[2];

    curr0 = current[0];
    curr1 = current[1];
    curr2 = current[2];
  }

  public int [] getCurrentCounter() {
    current[0] = curr0;
    current[1] = curr1;
    current[2] = curr2;
    return (int []) current.clone();
  }

   public String toString() {
     return curr0+","+curr1+","+curr2;
   }

  public int currentElement() {
    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2;
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

    return offset + val0*stride0 + val1*stride1 + val2*stride2;
  } */

  protected int incr() {
    if (++curr2 >= shape2) {
      curr2 = 0;
      if (++curr1 >= shape1) {
        curr1 = 0;
        if (++curr0 >= shape0) {
          curr0 = 0;    // rollover !
        }
      }
    }
    return offset + curr0*stride0 + curr1*stride1 + curr2*stride2;
  }

  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();
    if (dim == 2)
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

  public Index set(int v0, int v1, int v2) {
    set0(v0);
    set1(v1);
    set2(v2);
    return this;
  }

  public Index set(int[] index){
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();
    set0(index[0]);
    set1(index[1]);
    set2(index[2]);
    return this;
  }

  public Object clone() {
    return super.clone();
  }

    //experimental
  int setDirect(int v0, int v1, int v2) {
    if (v0 < 0 || v0 >= shape0)
      throw new ArrayIndexOutOfBoundsException();
    if (v1 < 0 || v1 >= shape1)
      throw new ArrayIndexOutOfBoundsException();
    if (v2 < 0 || v2 >= shape2)
      throw new ArrayIndexOutOfBoundsException();
    return offset + v0*stride0 + v1*stride1 + v2*stride2;
  }


}

/* Change History:
   $Log: Index3D.java,v $
   Revision 1.4  2005/12/15 00:29:09  caron
   *** empty log message ***

   Revision 1.3  2005/12/09 04:24:34  caron
   Aggregation
   caching
   sync

   Revision 1.2  2004/07/12 23:40:14  caron
   2.2 alpha 1.0 checkin

 */
