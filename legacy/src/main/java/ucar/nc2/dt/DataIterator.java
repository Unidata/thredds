/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

/**
 * Just like java.util.Iterator, but may throw IOException on nextData() call.
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */
public interface DataIterator extends java.util.Iterator {

  /** true if another "DataType" object is available */
  public boolean hasNext();

  /** Returns the next "DataType" object
   * @return the next "DataType" object
   */
  public Object nextData() throws java.io.IOException;

  /** Returns the next "DataType" object.
   * @throws RuntimeException (unchecked) instead of IOException.
   * @deprecated use nextData()
   */
  public Object next();

}
