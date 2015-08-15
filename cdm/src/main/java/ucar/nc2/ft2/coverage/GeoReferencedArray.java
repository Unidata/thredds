/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IsMissingEvaluator;
import ucar.nc2.constants.AxisType;

import java.util.Formatter;
import java.util.List;

/**
 * GeoReferencedArray
 *
 * @author caron
 * @since 7/11/2015
 */
public class GeoReferencedArray implements IsMissingEvaluator{
  private String coverageName;
  private DataType dataType;
  private Array data;
  private CoverageCoordSys csSubset;
  private List<CoverageCoordAxis> axes;

  public GeoReferencedArray(String coverageName, DataType dataType, Array data, List<CoverageCoordAxis> axes) {
    this.coverageName = coverageName;
    this.dataType = dataType;
    this.data = data;
    this.axes = axes;
  }

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

  public List<CoverageCoordAxis> getCoordinatesForData() {
    return axes;
  }

  public CoverageCoordSys getCoordSysForData() {
    return csSubset;
  }

  public CoverageCoordAxis getAxis(AxisType want) {
    for (CoverageCoordAxis axis : axes)
    if (axis.getAxisType() == want) return axis;
    return null;
  }


  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("GeoReferencedArray {%n");
    f.format(" coverageName='%s'%n", coverageName);
    f.format(" dataType=%s%n", dataType);
    f.format(" csSubset=%s%n", csSubset);
    for (CoverageCoordAxis axis : axes)
      f.format("%n%s", axis);
    f.format("}");
    return f.toString();
  }
}
