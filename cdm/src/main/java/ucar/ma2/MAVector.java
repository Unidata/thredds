/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

/**
 * Abstraction for vector operations.
 * A vector is a rank-1 Array.
 * All operations done in double precision
 *
 * @author @caron
 */

public class MAVector {
  private Array a;
  private int nelems;
  private Index ima;

  /**
   * Create an MAVector out of a double array
   */
  public MAVector( double[] values) {
    this( Array.makeFromJavaArray(values, false));
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

