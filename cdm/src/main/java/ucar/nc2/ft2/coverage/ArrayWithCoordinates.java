/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;

/**
 * Describe
 *
 * @author caron
 * @since 7/11/2015
 */
public class ArrayWithCoordinates {
  private Array data;
  private CoverageCoordSys coordSys;

  public ArrayWithCoordinates(Array data, CoverageCoordSys coordSys) {
    this.data = data;
    this.coordSys = coordSys;
  }

  public Array getData() {
    return data;
  }

  public CoverageCoordAxis getXaxis() {
    return null;
  }

  public CoverageCoordAxis getYaxis() {
    return null;
  }

  /*
    public class Value {
    double val;
  }

  public Iterator<Value> getIterator() {
    return null;
  }

  public Value getValue(Index ima) {
    return null;
  }
   */
}
