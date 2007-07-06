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
 * Abstraction for matrix operations.
 * A matrix is a rank-2 Array: m[rows, cols].
 * All operations done in double precision (??)
 *
 * @author @caron
 */

public class MAMatrix {
  private Array a;
  private int nrows, ncols;
  private Index ima;

  /**
   * Create an MAMatrix of the given shape.
   * @param nrows number of rows
   * @param ncols number of cols
   */
  public MAMatrix( int nrows, int ncols) {
    this.a = new ArrayDouble.D2(nrows, ncols);
    this.nrows = nrows;
    this.ncols = ncols;
    ima = a.getIndex();
  }
  /**
   * Create an MAMatrix using the given rank-2 array.
   * @param a rank-2 array
   * @exception IllegalArgumentException is a is not rank 2
   */
  public MAMatrix( Array a) {
    this.a = a;
    if (a.getRank() != 2)
      throw new IllegalArgumentException("rank != 2, instead = "+ a.getRank());
    nrows = a.getShape()[0];
    ncols = a.getShape()[1];
    ima = a.getIndex();
  }

  public int getNrows() { return nrows; }
  public int getNcols() { return ncols; }
  public double getDouble(int i, int j) { return a.getDouble(ima.set(i,j)); }
  public void setDouble(int i, int j, double val) { a.setDouble(ima.set(i,j), val); }

  /**
   * Create a new MAMatrix that is the same as this one, with a copy of the backing store.
   */
  public MAMatrix copy() {
    return new MAMatrix( a.copy());
  }

  /**
   * Create a MAMatrix that is the transpose of this one, with the same backing store.
   * Use copy() to get a copy.
   */
  public MAMatrix transpose() {
    return new MAMatrix( a.transpose(0,1));
  }

  /**
   * Get the jth column, return as a MAVector: same backing store.
   */
  public MAVector column(int j) {
    return new MAVector( a.slice(1,j));
  }

  /**
   * Get the ith row, return as a MAVector: same backing store.
   */
  public MAVector row(int i) {
    return new MAVector( a.slice(0,i));
  }

  /**
   * Dot product of matrix and vector: return M dot v
   * @param v dot product with this vector
   * @return MAVector result: new vector
   * @exception IllegalArgumentException if ncols != v.getSize().
   */
  public MAVector dot(MAVector v) {

    if (ncols != v.getNelems())
      throw new IllegalArgumentException("MAMatrix.dot "+ncols+" != "+ v.getNelems());

    ArrayDouble.D1 result = new ArrayDouble.D1(nrows);
    Index imr = result.getIndex();

    for (int i=0; i<nrows; i++) {
      double sum = 0.0;
      for (int k=0; k<ncols; k++)
        sum += getDouble(i, k) * v.getDouble(k);
      result.setDouble( imr.set(i), sum);
    }

    return new MAVector( result);
  }

  /**
   * Matrix multiply: return m1 * m2.
   * @param m1 left matrix
   * @param m2 right matrix
   * @return MAMatrix result: new matrix
   * @exception IllegalArgumentException if m1.getNcols() != m2.getNrows().
   */
  static public MAMatrix multiply(MAMatrix m1, MAMatrix m2) {

    if (m1.getNcols() != m2.getNrows())
      throw new IllegalArgumentException("MAMatrix.multiply "+m1.getNcols()+" != "+ m2.getNrows());
    int kdims = m1.getNcols();

    ArrayDouble.D2 result = new ArrayDouble.D2(m1.getNrows(), m2.getNcols());
    Index imr = result.getIndex();

    for (int i=0; i<m1.getNrows(); i++) {
      for (int j=0; j<m2.getNcols(); j++) {
        double sum = 0.0;
        for (int k=0; k<kdims; k++)
          sum += m1.getDouble(i, k) * m2.getDouble(k, j);
        result.setDouble( imr.set(i,j), sum);
      }
    }

    return new MAMatrix( result);
  }

  /**
   * Matrix multiply by a diagonal matrix, store result in this: this = this * diag
   * @param diag diagonal matrix stored as a Vector
   * @exception IllegalArgumentException if ncols != diag.getNelems().
   */
  public void postMultiplyDiagonal(MAVector diag) {

    if (ncols != diag.getNelems())
      throw new IllegalArgumentException("MAMatrix.postMultiplyDiagonal "+ncols+" != "+ diag.getNelems());

    for (int i=0; i<nrows; i++) {
      for (int j=0; j<ncols; j++) {
        double val = a.getDouble( ima.set(i,j)) * diag.getDouble(j);
        a.setDouble( ima, val);
      }
    }
  }

  /**
   * Matrix multiply by a diagonal matrix, store result in this: this = diag * this
   * @param diag diagonal matrix stored as a Vector
   * @exception IllegalArgumentException if nrows != diag.getNelems().
   */
  public void preMultiplyDiagonal(MAVector diag) {

    if (nrows != diag.getNelems())
      throw new IllegalArgumentException("MAMatrix.preMultiplyDiagonal "+nrows+" != "+ diag.getNelems());

    for (int i=0; i<nrows; i++) {
      for (int j=0; j<ncols; j++) {
        double val = a.getDouble( ima.set(i,j)) * diag.getDouble(i);
        a.setDouble( ima, val);
      }
    }
  }

}
