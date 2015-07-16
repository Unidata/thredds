/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * GeoReferencedArray
 *
 * @author caron
 * @since 7/11/2015
 */
public class GeoReferencedArray {
  private String coverageName;
  private DataType dataType;
  private Array data;
  private CoverageCoordSys csSubset;

  public GeoReferencedArray(String coverageName, DataType dataType, Array data, CoverageCoordSys csSubset) {
    this.coverageName = coverageName;
    this.dataType = dataType;
    this.data = data;
    this.csSubset = csSubset;
  }

  public String getCoverageName() {
    return coverageName;
  }

  public DataType getDataType() {
    return dataType;
  }

  public Array getData() {
    return data;
  }

  public CoverageCoordSys getCoordSysForData() {
    return csSubset;
  }
}
