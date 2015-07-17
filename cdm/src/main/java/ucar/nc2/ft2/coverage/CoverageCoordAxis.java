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
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedObject;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage CoordAxis abstract superclass
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
abstract public class CoverageCoordAxis {
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

  protected final String name;
  protected final String units, description;
  protected final DataType dataType;
  protected final AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final String dependsOn;

  protected final int ncoords;            // number of coordinates (not values)
  protected final Spacing spacing;
  protected final double startValue;
  protected final double endValue;
  protected final double resolution;
  final CoordAxisReader reader;

  protected final TimeHelper timeHelper;

  // maybe lazy eval
  protected double[] values;     // null if isRegular, CoordAxisReader for lazy eval

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

    if (axisType == AxisType.Time || axisType == AxisType.RunTime)
      timeHelper = new TimeHelper(units, attributes);
    else
      timeHelper = null;

    if (resolution == 0.0 && ncoords > 1)
      this.resolution = (endValue - startValue) / (ncoords - 1);
    else
      this.resolution = resolution;

    this.ncoords = ncoords;
  }

  abstract public void toString(Formatter f, Indent indent);
  abstract public CoverageCoordAxis copy(CoordAxisReader reader);
  abstract public CoverageCoordAxis subset(SubsetParams params);
  abstract public CoverageCoordAxis subset(double minValue, double maxValue);
  abstract public Array getCoordsAsArray();
  abstract public List<NamedObject> getCoordValueNames();

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

  ///////////////////////////////////////////////
  // time coords only

  public double convert(CalendarDate date) {
    return timeHelper.convert(date);
  }

  public CalendarDate makeDate(double value) {
    return timeHelper.makeDate(value);
  }

  public CalendarDateRange getDateRange() {
    return timeHelper.getDateRange(startValue, endValue);
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
