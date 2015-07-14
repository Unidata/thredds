/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.Calendar;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage CoordAxis
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoverageCoordAxis {
  static private final Logger logger = LoggerFactory.getLogger(CoverageCoordAxis.class);

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

  public static CoverageCoordAxis factory(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes,
                             DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                             double[] values, CoordAxisReader reader, ucar.nc2.time.Calendar cal) {

    if (axisType == AxisType.Time)
      return new CoverageCoordAxisTime(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values, reader, cal);
    else
      return new CoverageCoordAxis(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
            ncoords, startValue, endValue, resolution, values, reader);
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
  private final double resolution;
  final CoordAxisReader reader;

  // maybe lazy eval
  private double[] values;     // null if isRegular, CoordAxisReader for lazy eval

  // subset ??
  private long minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  private int stride = 1;

  protected CoverageCoordAxis(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes,
                           DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader) {
    this.name = name;
    this.units = units;
    this.description = description;
    this.dataType = dataType;
    this.axisType = axisType;
    this.attributes = new AttributeContainerHelper( name, attributes);
    this.dependenceType = dependenceType;
    this.dependsOn = dependsOn;
    this.spacing = spacing;
    this.startValue = startValue;
    this.endValue = endValue;
    this.values = values;
    this.reader = reader; // used only if values == null

    if (resolution == 0.0 && ncoords > 1)
      this.resolution = (endValue - startValue) / (ncoords - 1);
    else
      this.resolution = resolution;

    this.ncoords = ncoords;
    this.minIndex = 0;
    this.maxIndex = ncoords-1;
  }

  // ??
  void setIndexRange(int minIndex, int maxIndex, int stride) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.stride = stride;
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

  public long getMaxIndex() {
    return maxIndex;
  }

  public int getStride() {
    return stride;
  }

  public Spacing getSpacing() {
    return spacing;
  }

  public boolean isRegular() {
    return (spacing == Spacing.regular);
  }

  public double getResolution() {
    return resolution;
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

  public CoverageCoordAxis subset(double minValue, double maxValue) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    return helper.subset(minValue, maxValue);
  }

  public CoverageCoordAxis copy(Calendar cal) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    return helper.copy(cal);
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
    getValues();  // read in if needed
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

  // will return null when isRegular
  public double[] getValues() {
    synchronized (this) {
      if (values == null && !isRegular() && reader != null)
        try {
          values = reader.readValues(this);
        } catch (IOException e) {
          logger.error("Failed to read "+name, e);
        }
    }
    return values;
  }
}
