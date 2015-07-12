/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage CoordAxis
 *
 * @author caron
 * @since 7/11/2015
 */
public class CoverageCoordAxis {

  public enum Spacing {
    regular,                // regularly spaced points or intervals (start, end, npts), edges halfway between coords
    irregularPoint,         // irregular spaced points (values, npts), edges halfway between coords
    contiguousInterval,     // irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
    discontiguousInterval } // irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...

  public enum DependenceType {
    independent,             // time(time)
    dependent,               // reftime(time), lat(x,y)
    scalar,                  // reftime
    twoD }                   // time(reftime, time)

  public static CoverageCoordAxis factory(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                             DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                             double[] values, ucar.nc2.time.Calendar cal) {

    if (axisType == AxisType.Time)
      return new CoverageCoordAxisTime(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values, cal);
    else
      return new CoverageCoordAxis(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
            ncoords, startValue, endValue, resolution, values);
  }


  private final String name;
  private final String units, description;
  private final DataType dataType;
  private final AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  private final AttributeContainer attributes;
  private final DependenceType dependenceType;
  private final String dependsOn;

  private final int ncoords;            // number of coordinates (not values)
  private final Spacing spacing;
  private final double startValue;
  private final double endValue;

  private double resolution;
  private double[] values;     // null if isRegular,

  // subset
  private long minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  private int stride = 1;

  public CoverageCoordAxis(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                           DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values) {
    this.name = name;
    this.units = units;
    this.description = description;
    this.dataType = dataType;
    this.axisType = axisType;
    this.attributes = attributes;
    this.dependenceType = dependenceType;
    this.dependsOn = dependsOn;
    this.spacing = spacing;
    this.startValue = startValue;
    this.endValue = endValue;
    this.resolution = resolution;
    this.values = values;

    this.ncoords = ncoords;
    this.minIndex = 0;
    this.maxIndex = ncoords-1;
  }

  public String getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public List<Attribute> getAttributes() {
    return attributes.getAttributes();
  }

  public int getNcoords() {
    return ncoords;
  }

  public long getMinIndex() {
    return minIndex;
  }

  public void setMinIndex(long minIndex) {
    this.minIndex = minIndex;
  }

  public long getMaxIndex() {
    return maxIndex;
  }

  public void setMaxIndex(long maxIndex) {
    this.maxIndex = maxIndex;
  }

  public int getStride() {
    return stride;
  }

  public void setStride(int stride) {
    this.stride = stride;
  }

  public Spacing getSpacing() {
    return spacing;
  }

  public boolean isRegular() {
    return (spacing == Spacing.regular);
  }

  public double getResolution() {
    if (resolution == 0.0 && isRegular() && ncoords > 1)    // LOOK calc resolution for non regular ??
      resolution = (endValue - startValue) / (ncoords - 1);
    return resolution;
  }

  public void setResolution(double resolution) {
    this.resolution = resolution;
  }

  public double getStartValue() {
    return startValue;
  }

  public double getEndValue() {
    return endValue;
  }

  public String getUnits() {
    return units;
  }

  public String getDescription() {
    return description;
  }

  public DependenceType getDependenceType() {
    return dependenceType;
  }

  public boolean isScalar() { return dependenceType == DependenceType.scalar; }

  public String getDependsOn() {
    return dependsOn;
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
    f.format(" npts: %d [%f,%f] '%s' spacing=%s", ncoords, startValue, endValue, units, spacing);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
    f.format(" %s", getDependenceType());
    if (getDependsOn() != null)
      f.format(": %s", getDependsOn());
    f.format("%n");

    if (values != null) {
      int n = values.length;
      switch (spacing) {
        case irregularPoint:
        case contiguousInterval:
          f.format("%ncontiguous (%d)=", n);
          for (double v : values)
            f.format("%f,", v);
          f.format("%n");
          break;

        case discontiguousInterval:
          f.format("%ndiscontiguous (%d)=", n);
          for (int i = 0; i < n; i += 2)
            f.format("(%f,%f) ", values[i], values[i + 1]);
          f.format("%n");
          break;
      }
    }

    indent.decr();
  }

    ///////////////////////////////////////////////////////////////////
  // Spacing

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */

  // LOOK question: is this applicable to anyD or only 1D ??

  public boolean isAscending() {
    switch (spacing) {
      case regular:
        return getResolution() > 0;

      case irregularPoint:
        return values[0] <= values[ncoords - 1];

      case contiguousInterval:
        return values[0] <= values[ncoords];

      case discontiguousInterval:
        return values[0] <= values[2*ncoords-1];
    }
    throw new IllegalStateException("unknown spacing"+spacing);
  }

  public double getCoord(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + index * getResolution();

      case irregularPoint:
        return values[index];

      case contiguousInterval:
        return (values[index] + values[index + 1]) / 2;

      case discontiguousInterval:
        return (values[2 * index] + values[2 * index + 1]) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge1(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + (index - .5) * getResolution();

      case irregularPoint:
        if (index > 0)
          return (values[index - 1] + values[index]) / 2;
        else
          return values[0] - (values[1] - values[0]) / 2;

      case contiguousInterval:
        return values[index];

      case discontiguousInterval:
        return values[2 * index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge2(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + (index + .5) * getResolution();

      case irregularPoint:
        if (index < ncoords - 1)
          return (values[index] + values[index + 1]) / 2;
        else
          return values[index] + (values[index] - values[index - 1]) / 2;

      case contiguousInterval:
        return values[index + 1];

      case discontiguousInterval:
        return values[2 * index + 1];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdgeLast() {
    return getCoordEdge2( ncoords - 1);
  }

  public Array getCoordsAsArray() {
    getValues();
    Array result;
    switch (dependenceType) {
      case scalar:
        result = Array.factory(getDataType(), new int[0]);
        break;
      case twoD:
        result = Array.factory(getDataType(), new int[0]);
        break;
      default:
        result = Array.factory(getDataType(), new int[] { ncoords});
        break;
    }

    for (int i=0; i< ncoords; i++)
      result.setDouble(i, getCoord(i));
    return result;
  }

 /* public Array getCoordEdge1() {
    getValues();
    double[] vals = new double[ ncoords];
    for (int i=0; i< ncoords; i++)
      vals[i] = getCoordEdge1(i);
    return Array.makeFromJavaArray(vals);
  }

  public Array getCoordEdge2() {
    getValues();
    double[] vals = new double[ ncoords];
    for (int i=0; i< ncoords; i++)
      vals[i] = getCoordEdge2(i);
    return Array.makeFromJavaArray(vals);
  } */

  public List<NamedObject> getCoordValueNames() {
    getValues();
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < ncoords; i++) {
      String valName = "";
      switch (spacing) {
        case regular:
        case irregularPoint:
          valName = Format.d(getCoord(i), 3);
          break;

        case contiguousInterval:
        case discontiguousInterval:
          valName = Format.d(getCoordEdge1(i), 3) + "," + Format.d(getCoordEdge2(i), 3);
          break;

      }
      result.add(new NamedAnything(valName, valName + " " + getUnits()));
    }

    return result;
  }

  ///////////////////////////////////////////////

  protected double[] readValues() { return null; } //LOOK

  public double[] getValues() {
    if (values == null) values = readValues();
    return values;
  }
}
