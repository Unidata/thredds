/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import ucar.ma2.DataType;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/16/2015
 */
class GeoArrayResponse {

  String coverageName;
  DataType dataType;

  public boolean bigend;
  public boolean deflate;
  public long uncompressedSize;
  public int[] shape;
  public List<String> axisName;
  public String coordSysName;

}
