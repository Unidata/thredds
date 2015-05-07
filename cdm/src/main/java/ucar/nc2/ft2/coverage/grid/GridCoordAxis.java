/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 5/4/2015
 */
public class GridCoordAxis {
  public enum Type {X, Y, Z, T}

  String name;
  DataType dataType;
  AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  long nvalues;
  String units, description;
  boolean isRegular;
  double startValue;
  double endValue;
  double resolution;
  double[] values;   // may be null

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
      resolution = (endValue - startValue) / (nvalues-1);
    return resolution;
  }

  public void setResolution(double resolution) {
    this.resolution = resolution;
  }

  public double getStartValue() {
    return startValue;
  }

  public void setStartValue(double startValue) {
    this.startValue = startValue;
  }

  public double getEndValue() {
    return endValue;
  }

  public void setEndValue(double endValue) {
    this.endValue = endValue;
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

  // to override
  public double[] readValues() throws IOException {
    return values;
  }

  public double[] getValues() {
    return values;
  }

  public void setValues(double[] values) {
    this.values = values;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordAxis '%s' axisType=%s dataType=%s", indent, name, axisType, dataType);
    f.format(" npts: %d [%f,%f] '%s' isRegular=%s", nvalues, startValue, endValue, units, isRegular);
    if (resolution != 0.0)
      f.format(" resolution=%f", resolution);
    f.format("%n");

    if (values != null) {
      f.format("%nvalues=");
      for (double v : values)
        f.format("%f,", v);
      f.format("%n");
    }

    indent.decr();
  }

  public double getCoordEdge(int index) {
    return startValue + (index - .5) * resolution;
  }

  public double getValue(int index) {
    if (values != null)
      return values[index];
    else
      return startValue + index* resolution;
  }
}
