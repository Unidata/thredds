/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    discontiguousInterval   // irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1..
  }


  public enum DependenceType {
    independent,             // has its own dimension, is a coordinate variable, eg x(x)
    dependent,               // aux coordinate, reftime(time) or time_bounds(time);
    scalar,                  // reftime
    twoD                     // time(reftime, time), lat(x,y)
  }

  protected final String name;
  protected final String description;
  protected final DataType dataType;
  protected final AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final List<String> dependsOn;

  protected final int ncoords;            // number of coordinates (not values)
  protected final Spacing spacing;
  protected final double startValue;
  protected final double endValue;
  protected final double resolution;
  protected final CoordAxisReader reader;
  private final boolean isSubset;

  // Look not final see setReferenceDate()
  protected TimeHelper timeHelper; // AxisType = Time, RunTime only
  protected String units;
  protected CoverageCoordAxis1D dependent;

  // may be lazy eval
  protected double[] values;     // null if isRegular, CoordAxisReader for lazy eval

  protected CoverageCoordAxis(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer atts,
                              DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                              double[] values, CoordAxisReader reader, boolean isSubset) {
    this.name = name;
    this.units = units;
    this.description = description;
    this.dataType = dataType;
    this.axisType = axisType;
    this.attributes = atts;
    this.dependenceType = dependenceType;
    this.spacing = spacing;
    this.values = values;
    this.reader = reader; // used only if values == null
    if (dependsOn != null && dependsOn.trim().length() > 0) {
      List<String> temp = new ArrayList<>();
      Collections.addAll(temp, StringUtil2.splitString(dependsOn));
      this.dependsOn = Collections.unmodifiableList(temp);
    } else {
      this.dependsOn = Collections.emptyList();
    }

    if (values == null) {
      this.startValue = startValue;
      this.endValue = endValue;
    }  else {
      this.startValue = values[0];
      this.endValue = values[values.length-1];
      // could also check if regular, and change spacing
    }

    if (resolution == 0.0 && ncoords > 1)
      this.resolution = (endValue - startValue) / (ncoords - 1);
    else
      this.resolution = resolution;

    this.ncoords = ncoords;
    this.isSubset = isSubset;

    if (axisType == AxisType.Time || axisType == AxisType.RunTime)
      timeHelper = TimeHelper.factory(units, atts);
    else if (axisType == AxisType.TimeOffset)
      timeHelper = TimeHelper.factory(null, atts);
    else
      timeHelper = null;
  }

  // called after everything is wired in the dataset
  protected void setDataset(CoordSysContainer dataset) {
    // NOOP
  }

  // create a subset of this axis based on the SubsetParams. return this if no subset requested
  abstract public CoverageCoordAxis subset(SubsetParams params);

  // called only on dependent axes. pass in what if depends on
  abstract public CoverageCoordAxis subsetDependent(CoverageCoordAxis1D dependsOn);

  // called only on CoverageCoordAxis1D
  abstract public CoverageCoordAxis subset(double minValue, double maxValue);

  abstract public Array getCoordsAsArray() throws IOException;

  abstract public Array getCoordBoundsAsArray();

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

  public AttributeContainer getAttributeContainer() {
    return new AttributeContainerHelper(name, attributes.getAttributes());
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

  public boolean isScalar() {
    return dependenceType == DependenceType.scalar;
  }

  public String getDependsOn() {
    Formatter result = new Formatter();
    for (String name : dependsOn)
      result.format("%s ", name);
    return result.toString().trim();
  }

  public List<String> getDependsOnList() {
    return dependsOn;
  }

  public boolean getHasData() {
    return values != null;
  }

  public boolean isSubset() {
    return isSubset;
  }

  public boolean isTime2D() {
     return false;
   }

  public boolean isInterval() {
    return spacing == Spacing.contiguousInterval ||  spacing == Spacing.discontiguousInterval;
  }

   @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public int[] getShape() {
    if (getDependenceType() == CoverageCoordAxis.DependenceType.scalar)
      return new int[0];
    return new int[] {ncoords};
  }

  public RangeIterator getRange() {
    if (getDependenceType() == CoverageCoordAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(name, 0, ncoords-1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  public void toString(Formatter f, Indent indent) {
    f.format("%sCoordAxis '%s' (%s)%n", indent, name, getClass().getName());
    indent.incr();
    f.format("%saxisType=%s dataType=%s units='%s' desc='%s'", indent, axisType, dataType, units, description);
    if (timeHelper != null) f.format(" refDate=%s", timeHelper.getRefDate());
    f.format("%n");

    f.format("%s%s", indent, getDependenceType());
    if (dependsOn.size() > 0) f.format(" :");
    for (String s : dependsOn)
      f.format(" %s", s);
    f.format("%n");

    AttributeContainerHelper.show(attributes, indent, f);

    f.format("%snpts: %d [%f,%f] spacing=%s", indent, ncoords, startValue, endValue, spacing);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
    f.format("%n");

    if (values != null) {
      int n = values.length;
      switch (spacing) {
        case irregularPoint:
        case contiguousInterval:
          f.format("%scontiguous values (%d)=", indent, n);
          for (double v : values)
            f.format("%f,", v);
          f.format("%n");
          break;

        case discontiguousInterval:
          f.format("%sdiscontiguous values (%d)=", indent, n);
          for (int i = 0; i < n; i += 2)
            f.format("(%f,%f) ", values[i], values[i + 1]);
          f.format("%n");
          break;
      }
    }
    indent.decr();
  }

  public String getSummary() {
    Formatter f = new Formatter();
    f.format("start=%f end=%f %s %s", startValue, endValue, units, spacing);
    if (ncoords > 1) {
      switch (spacing) {
        case regular:
          f.format(" spacing=%f", resolution);
          break;
        case contiguousInterval:
        case irregularPoint:
          if (axisType.isTime())
            f.format(", typical spacing=%f", resolution);
          break;
      }
    }
    f.format(" (npts=%d)", ncoords);
    return f.toString();
  }

  ///////////////////////////////////////////////
  // time coords only

  public double convert(CalendarDate date) {
    return timeHelper.offsetFromRefDate(date);
  }

  public CalendarDate makeDate(double value) {
    return timeHelper.makeDate(value);
  }

  public CalendarDateRange getDateRange() {
    return timeHelper.getDateRange(startValue, endValue);
  }

  public double getOffsetInTimeUnits(CalendarDate convertFrom, CalendarDate convertTo) {
    return timeHelper.getOffsetInTimeUnits(convertFrom, convertTo);
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return timeHelper.makeDateInTimeUnits(start, addTo);
  }

  public CalendarDate getRefDate() {
    return timeHelper.getRefDate();
  }

  void setReferenceDate(CalendarDate refDate) {
    // munge the unit
    this.timeHelper = timeHelper.setReferenceDate(refDate);
    this.units = timeHelper.getUdUnit();
  }

  public CoverageCoordAxis1D getDependent() {
    return dependent;
  }

  public void setDependent(CoverageCoordAxis1D dependent) {
    this.dependent = dependent;
  }

  ///////////////////////////////////////////////

  // will return null when isRegular
  public double[] getValues() {
    synchronized (this) {
      if (values == null && !isRegular() && reader != null)
        try {
          values = reader.readCoordValues(this);
        } catch (IOException e) {
          logger.error("Failed to read " + name, e);
        }
    }
    return values;
  }
}
