/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Indent;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 5/4/2015
 */
public class GridCoordAxis {
  
  String name;
  DataType dataType;
  AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  long nvalues;
  String units, description;
  boolean isRegular;
  double min;        // ??
  double max;
  double resolution;


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public void setAxisType(int axisTypeOrdinal) {
    this.axisType = AxisType.values()[axisTypeOrdinal];
  }

  public void setAxisType(AxisType type) {
    this.axisType = type;
  }

  public long getNvalues() {
    return nvalues;
  }

  public void setNvalues(long nvalues) {
    this.nvalues = nvalues;
  }

  public boolean isRegular() {
    return isRegular;
  }

  public void setIsRegular(boolean isRegular) {
    this.isRegular = isRegular;
  }

  public double getResolution() {
    if (resolution == 0.0 && isRegular && nvalues > 0)
      resolution = (max - min) / (nvalues + 1);    // n+1 assumes min and max are the edges. might be a bad assumption
    return resolution;
  }

  public void setResolution(double resolution) {
    this.resolution = resolution;
  }

  public double getMin() {
    return min;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordAxis '%s' axisType=%s dataType=%s", indent, name, axisType, dataType);
    f.format(" npts: %d [%f,%f] '%s' isRegular=%s", nvalues, min, max, units, isRegular);
    if (resolution != 0.0)
      f.format(" resolution=%f", resolution);
    f.format("%n");

    indent.decr();
  }
}
