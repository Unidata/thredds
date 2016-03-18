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
 *
 */
package ucar.nc2.ft2.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Optional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Coverage CoordAxis abstract superclass
 * Immutable with (possible) lazy eval of values
 *
 * @author caron
 * @since 7/11/2015
 */
abstract public class CoverageCoordAxis implements Comparable<CoverageCoordAxis> {
  static private final Logger logger = LoggerFactory.getLogger(CoverageCoordAxis.class);

  public enum Spacing {
    regularPoint,          // regularly spaced points (start, end, npts), start and end are pts, edges halfway between coords, resol = (start - end) / (npts-1)
    irregularPoint,        // irregular spaced points (values, npts), edges halfway between coords
    regularInterval,       // regular contiguous intervals (start, end, npts), start and end are edges, resol = (start - end) / npts
    contiguousInterval,    // irregular contiguous intervals (values, npts), values are the edges, values[npts+1], coord halfway between edges
    discontiguousInterval  // irregular discontiguous spaced intervals (values, npts), values are the edges, values[2*npts]: low0, high0, low1, high1, ...
  }

  public enum DependenceType {
    independent,             // has its own dimension, is a coordinate variable, eg x(x)
    dependent,               // aux coordinate, eg reftime(time) or time_bounds(time);
    scalar,                  // eg reftime
    twoD,                    // lat(x,y)
    fmrcReg,                 // time(reftime, hourOfDay)
    dimension                // swath(scan, scanAcross)
  }

  protected final String name;
  protected final String description;
  protected final DataType dataType;
  protected final AxisType axisType;        // ucar.nc2.constants.AxisType ordinal
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final List<String> dependsOn;  // independent axes or dimensions

  protected final int ncoords;            // number of coordinates (not always same as values)
  protected final Spacing spacing;
  protected final double startValue;
  protected final double endValue;
  protected final double resolution;
  protected final CoordAxisReader reader;
  protected final boolean isSubset;

  protected final TimeHelper timeHelper; // AxisType = Time, RunTime only
  protected final String units;

  // may be lazy eval
  protected double[] values;     // null if isRegular, or use CoordAxisReader for lazy eval

  protected CoverageCoordAxis(CoverageCoordAxisBuilder builder) {
    this.name = builder.name;
    this.units = builder.units;
    this.description = builder.description;
    this.dataType = builder.dataType;
    this.axisType = builder.axisType;
    this.attributes = builder.attributes;
    this.dependenceType = builder.dependenceType;
    this.spacing = builder.spacing;
    this.values = builder.values;
    this.reader = builder.reader; // used only if values == null
    this.dependsOn = builder.dependsOn == null ? Collections.emptyList() : builder.dependsOn;

    this.startValue = builder.startValue;
    this.endValue = builder.endValue;
    this.resolution = builder.resolution;

    this.ncoords = builder.ncoords;
    this.isSubset = builder.isSubset;

    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      if (axisType == AxisType.Time || axisType == AxisType.RunTime)
        timeHelper = TimeHelper.factory(units, attributes);
      else if (axisType == AxisType.TimeOffset)
        timeHelper = TimeHelper.factory(null, attributes);
      else
        timeHelper = null;
    }
  }

  // called after everything is wired in the dataset
  protected void setDataset(CoordSysContainer dataset) {
    // NOOP
  }

  @Override
  public int compareTo(CoverageCoordAxis o) {
    return axisType.axisOrder() - o.axisType.axisOrder();
  }

  // create a copy of this axis
  abstract public CoverageCoordAxis copy();

  // create a subset of this axis based on the SubsetParams. return copy if no subset requested, or params = null
  abstract public Optional<CoverageCoordAxis> subset(SubsetParams params);

  // called from HorizCoordSys
  abstract public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) throws InvalidRangeException;

  // called only on dependent axes. pass in independent axis
  abstract public Optional<CoverageCoordAxis> subsetDependent(CoverageCoordAxis1D dependsOn);

  abstract public Array getCoordsAsArray();

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

  public Attribute findAttribute(String attName) {
    return attributes.findAttribute(attName);
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
    return (spacing == Spacing.regularPoint) || (spacing == Spacing.regularInterval);
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

  public boolean isInterval() {
    return spacing == Spacing.regularInterval || spacing == Spacing.contiguousInterval || spacing == Spacing.discontiguousInterval;
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
    return new int[]{ncoords};
  }

  public Range getRange() {
    if (getDependenceType() == CoverageCoordAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  public RangeIterator getRangeIterator() {
    if (getDependenceType() == CoverageCoordAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  public void toString(Formatter f, Indent indent) {
    f.format("%sCoordAxis '%s' (%s) ", indent, name, getClass().getName());
    indent.incr();

    f.format("%s", getDependenceType());
    if (dependsOn.size() > 0) {
      f.format(" :");
      for (String s : dependsOn) f.format(" %s", s);
    }
    f.format("%n");

    f.format("%saxisType=%s dataType=%s units='%s' desc='%s'", indent, axisType, dataType, units, description);
    if (timeHelper != null) f.format(" refDate=%s", timeHelper.getRefDate());
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
    f.format("start=%f end=%f %s %s resolution=%f", startValue, endValue, units, spacing, resolution);
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

  public double getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return timeHelper.getOffsetInTimeUnits(start, end);
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return timeHelper.makeDateInTimeUnits(start, addTo);
  }

  public CalendarDate getRefDate() {
    return timeHelper.getRefDate();
  }

  public Calendar getCalendar() {
    return timeHelper.getCalendar();
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return timeHelper.getCalendarDateUnit();
  }

  ///////////////////////////////////////////////

  private boolean valuesLoaded;

  protected void loadValuesIfNeeded() {
    if (isRegular() || valuesLoaded) return;
    synchronized (this) {
      if (values == null && reader != null)
        try {
          values = reader.readCoordValues(this);
        } catch (IOException e) {
          logger.error("Failed to read " + name, e);
        }
      valuesLoaded = true;
    }
  }

  // will return null when isRegular, otherwise reads values if needed
  public double[] getValues() {
    loadValuesIfNeeded();
    return values == null ? null : Arrays.copyOf(values, values.length); // cant allow values array to escape, must be immutable
  }
}
