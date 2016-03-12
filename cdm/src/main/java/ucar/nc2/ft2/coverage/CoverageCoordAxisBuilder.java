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

import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.RangeComposite;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Misc;
import ucar.unidata.util.StringUtil2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable builder object for CoverageCoordAxis
 *
 * @author caron
 */

public class CoverageCoordAxisBuilder {

  public String name;
  public String description;
  public DataType dataType;
  public AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  public AttributeContainer attributes;
  public CoverageCoordAxis.DependenceType dependenceType;
  public List<String> dependsOn;

  public int ncoords;            // number of coordinates (not values)
  public CoverageCoordAxis.Spacing spacing;
  public double startValue;
  public double endValue;
  public double resolution;
  public CoordAxisReader reader;
  public boolean isSubset;

  public TimeHelper timeHelper; // AxisType = Time, RunTime only
  public String units;

  public double[] values;

  // 1D only
  public Range range; // set when its a subset
  public RangeComposite crange;

  //int minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  //public int stride = 1;
  // public boolean isTime2D;

  // 2d only
  public int[] shape;
  public Object userObject;

  public CoverageCoordAxisBuilder() {
  }

  public CoverageCoordAxisBuilder(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer atts,
                                  CoverageCoordAxis.DependenceType dependenceType, String dependsOnS, CoverageCoordAxis.Spacing spacing,
                                  int ncoords, double startValue, double endValue, double resolution, double[] values, CoordAxisReader reader) {
    this.name = name;
    this.units = units;
    this.description = description;
    this.dataType = dataType;
    this.axisType = axisType;
    this.attributes = atts;
    this.dependenceType = dependenceType;
    this.setDependsOn(dependsOnS);
    this.spacing = spacing;
    this.ncoords = ncoords;
    this.startValue = startValue;
    this.endValue = endValue;
    this.resolution = resolution;
    this.values = values;
    this.reader = reader;
  }

  CoverageCoordAxisBuilder(CoverageCoordAxis from) {
    this.name = from.name;
    this.units = from.units;
    this.description = from.description;
    this.dataType = from.dataType;
    this.axisType = from.axisType;
    this.attributes = from.attributes;
    this.dependenceType = from.dependenceType;
    this.spacing = from.spacing;
    this.values = from.values;
    this.reader = from.reader; // used only if values == null
    this.dependsOn = from.dependsOn;
    this.startValue = from.startValue;
    this.endValue = from.endValue;
    this.resolution = from.resolution;

    this.ncoords = from.ncoords;
    this.isSubset = from.isSubset;
    this.timeHelper = from.timeHelper;

    if (from instanceof LatLonAxis2D) {
      LatLonAxis2D latlon = (LatLonAxis2D) from;
      this.shape = latlon.getShape();
      this.userObject = latlon.getUserObject();
    }
  }

  public void setIsSubset(boolean isSubset) {
    this.isSubset = isSubset;
  }

  public CoverageCoordAxisBuilder setDependsOn(String dependsOn) {
    if (dependsOn != null && dependsOn.trim().length() > 0) {
      List<String> temp = new ArrayList<>();
      Collections.addAll(temp, StringUtil2.splitString(dependsOn));
      this.dependsOn = Collections.unmodifiableList(temp);
    } else {
      this.dependsOn = Collections.emptyList();
    }
    return this;
  }

  ///////////////////////////////////////////////////////////////////////
  //// this could be moved into grib ncx calculation
  // for point: values are the points, values[npts]
  // for intervals: values are the edges, values[2*npts]: low0, high0, low1, high1

  public void setSpacingFromValues(boolean isInterval) {
    if (isInterval)
      setSpacingFromIntervalValues();
    else
      setSpacingFromPointValues();
  }

  private void setSpacingFromPointValues() {
    assert (values.length == ncoords);

    this.startValue = values[0];
    this.endValue = values[ncoords - 1];
    this.resolution = (ncoords == 1) ? 0.0 : (endValue - startValue) / (ncoords - 1);

    if (ncoords == 1) {
      this.spacing = CoverageCoordAxis.Spacing.regularPoint;
      values = null;
      return;
    }

    this.resolution = (endValue - startValue) / (ncoords - 1);

    Counters.Counter resol = new Counters.Counter("resol");
    for (int i = 0; i < values.length - 1; i++) {
      double diff = values[i + 1] - values[i];
      resol.count(diff);
    }

    Comparable resolMode = resol.getMode();
    if (resolMode != null)
      this.resolution = ((Number) resolMode).doubleValue();

    boolean isRegular = isRegular(resol);
    this.spacing = isRegular ? CoverageCoordAxis.Spacing.regularPoint : CoverageCoordAxis.Spacing.irregularPoint;
    if (isRegular) values = null;
  }

  private void setSpacingFromIntervalValues() {
    assert (values.length == 2 * ncoords);

    this.startValue = values[0];
    this.endValue = values[values.length - 1];
    this.resolution = (endValue - startValue) / ncoords;

    Counters.Counter resol = new Counters.Counter("resol");
    boolean isContiguous = true;
    for (int i = 0; i < values.length - 2; i += 2) {
      double diff = values[i + 2] - values[i];  // difference of consecutive starting interval values // LOOK roundoff
      resol.count(diff);
      if (isContiguous && !Misc.closeEnough(values[i+1], values[i+2])) // difference of this ending interval values with next starting value
        isContiguous = false;
    }

    Comparable resolMode = resol.getMode();
    if (resolMode != null) {
      double modeValue = ((Number) resolMode).doubleValue();
      if (modeValue != 0.0)
        this.resolution = modeValue;
    }

    boolean regular = isRegular(resol);

    if (regular && isContiguous) {
      this.spacing = CoverageCoordAxis.Spacing.regularInterval;
      this.values = null;

    } else if (isContiguous) {
      this.spacing = CoverageCoordAxis.Spacing.contiguousInterval;
      double[] contValues = new double[ncoords+ 1];
      int count = 0;
      for (int i = 0; i < values.length; i += 2)
        contValues[count++] = values[i]; // starting interval
      contValues[count] = values[values.length - 1]; // ending interval
      this.values = contValues;

    } else {
      this.spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
    }
  }

  private static final double missingTolerence = .05;

  private boolean isRegular(Counters.Counter resol) {
    if (resol.getUnique() == 1) return true; // all same resolution, or n == 1

    Comparable mode = resol.getMode();
    Number modeNumber = (Number) mode;
    if (modeNumber == null || modeNumber.intValue() == 0)
      return false;

    int modeCount = 0;
    int nonModeCount = 0;
    for (Comparable value : resol.getValues()) {
      if (value.compareTo(mode) == 0)
        modeCount = resol.getCount(value);
      else {
        Number valueNumber = (Number) value;
        // non mode must be a multiple of mode - means there are some missing values
        int rem = (valueNumber.intValue() % modeNumber.intValue());
        if (rem != 0)
          return false;
        int multiple = (valueNumber.intValue() / modeNumber.intValue());
        nonModeCount += (multiple - 1) * resol.getCount(value);
      }
    }
    if (modeCount == 0) return true; // cant happen i think

    // only tolerate these many missing values
    double ratio = (nonModeCount / (double) modeCount);
    return ratio < missingTolerence;
  }

  ////////////////////////////////////////

  CoverageCoordAxisBuilder subset(String dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double[] values) {
    assert values != null;
    if (dependsOn != null) {
      this.dependenceType = CoverageCoordAxis.DependenceType.dependent;
      setDependsOn(dependsOn);
    }
    this.spacing = spacing;
    this.ncoords = ncoords;
    this.reader = null;
    this.values = values;
    this.isSubset = true;

    return this;
  }

  CoverageCoordAxisBuilder subset(int ncoords, double startValue, double endValue, double resolution, double[] values) {
    this.ncoords = ncoords;
    this.startValue = startValue;
    this.endValue = endValue;
    this.resolution = resolution;
    this.values = values;
    this.isSubset = true;

    return this;
  }

  /* CoverageCoordAxisBuilder setIndexRange(int minIndex, int maxIndex, int stride) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.stride = stride;
    this.isSubset = true;
    return this;
  } */

  CoverageCoordAxisBuilder setRange(Range r) {
    this.range = r;
    this.isSubset = true;
    return this;
  }

  CoverageCoordAxisBuilder setCompositeRange(RangeComposite cr) {
    this.crange = cr;
    this.isSubset = true;
    return this;
  }

  void setReferenceDate(CalendarDate refDate) {
    this.timeHelper = timeHelper.setReferenceDate(refDate);
    this.units = timeHelper.getUdUnit();
    if (attributes != null) {
      attributes.addAttribute(new Attribute(CDM.UNITS, this.units));
    }
  }

}