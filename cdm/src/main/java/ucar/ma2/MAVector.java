// $Id: MAVector.java,v 1.2 2004/07/12 23:40:15 caron Exp $
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
 * Abstraction for vector operations.
 * A vector is a rank-1 Array.
 * All operations done in double precision (??)
 *
 * @author @caron
 * @version $Revision: 1.2 $ $Date: 2004/07/12 23:40:15 $
 */

public class MAVector {
  private Array a;
  private int nelems;
  private Index ima;

  /**
   * Create an MAVector out of a double array
   */
  public MAVector( double[] values) {
    this( Array.factory( values));
  }

  /**
   * Create an MAVector of the given length.
   */
  public MAVector( int nelems) {
    this.a = new ArrayDouble.D1(nelems);
    this.nelems = nelems;
    ima = a.getIndex();
  }

  /**
   * Create an MAVector using the given rank-1 array.
   * @param a rank-1 array
   * @exception IllegalArgumentException is a is not rank 1
   */
  public MAVector( Array a) {
    this.a = a;
    if (a.getRank() != 1)
      throw new IllegalArgumentException("rank != 1, instead = "+ a.getRank());
    nelems = a.getShape()[0];
    ima = a.getIndex();
  }

  public int getNelems() { return nelems; }
  public double getDouble(int i) { return a.getDouble(ima.set(i)); }
  public void setDouble(int i, double val) { a.setDouble(ima.set(i), val); }

  /**
   * Create a new MAVector that is the same as this one, with a copy of the backing store.
   */
  public MAVector copy() {
    return new MAVector( a.copy());
  }

  /**
   * Cos between two vectors = dot(v) / norm() * norm(v)
   * @param v cosine with this vector
   * @return double result: cos between this and v
   * @exception IllegalArgumentException if nelems != v.getNelems().
   */
  public double cos(MAVector v) {

    if (nelems != v.getNelems())
      throw new IllegalArgumentException("MAVector.cos "+nelems+" != "+ v.getNelems());

    double norm = norm();
    double normV = v.norm();
    if ((norm == 0.0) || (normV == 0.0))
      return 0.0;
    else
      return dot(v)/(norm*normV);
  }

  /**
   * Dot product of 2 vectors
   * @param v dot product with this vector
   * @return double result: dot product
   * @exception IllegalArgumentException if nelems != v.getNelems().
   */
  public double dot(MAVector v) {

    if (nelems != v.getNelems())
      throw new IllegalArgumentException("MAVector.dot "+nelems+" != "+ v.getNelems());

    double sum = 0.0;
    for (int k=0; k<nelems; k++)
      sum += getDouble(k) * v.getDouble(k);

    return sum;
  }

  /**
   * Get the L2 norm of this vector.
   * @return double norm
   */
  public double norm() {

    double sum = 0.0;
    for (int k=0; k<nelems; k++) {
      double val = getDouble(k);
      sum += val * val;
    }

    return Math.sqrt(sum);
  }

  /**
   * Normalize this vector, so it has norm = 1.0.
   */
  public void normalize() {
    double norm = norm();
    if (norm <= 0.0)
      return;

    for (int k=0; k<nelems; k++) {
      double val = getDouble(k);
      setDouble(k, val/norm);
    }
  }

}

/*
 * $Log: MAVector.java,v $
 * Revision 1.2  2004/07/12 23:40:15  caron
 * 2.2 alpha 1.0 checkin
 *
 * Revision 1.1.1.1  2003/12/04 21:05:27  caron
 * checkin 2.2
 *
 * Revision 1.3  2003/06/03 20:06:02  caron
 * fix javadocs
 *
 * Revision 1.2  2003/04/08 15:42:15  caron
 * version for netcdf 2.1
 *
 * Revision 1.1  2000/10/24 21:30:19  caron
 * add MAMatrix, MAVector
 *
 */
