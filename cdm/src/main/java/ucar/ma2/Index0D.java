/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Specialization of Index for rank 0 arrays, ie scalars.
 *
 * @see Index
 * @author caron
 */
public class Index0D extends Index {

  Index0D() {
    super(0);
    this.size = 1;
    this.offset = 0;
  }

  public Index0D( int[] shape) {
    super(shape);
  }

  public int currentElement() {
    return offset;
  }

  public int incr() {
    return offset;
  }

  public Object clone() {
    return super.clone();
  }

  public Index set() {
      return this;
  }
}
