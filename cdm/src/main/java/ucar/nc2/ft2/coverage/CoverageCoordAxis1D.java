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

import net.jcip.annotations.Immutable;
import ucar.ma2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.nc2.util.Optional;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * Coverage CoordAxis 1D case
 *
 * @author caron
 * @since 7/15/2015
 */
@Immutable
public class CoverageCoordAxis1D extends CoverageCoordAxis { // implements Iterable<Object> {

  // does this really describe all subset possibilities? what about RangeScatter, composite ??
  protected final Range range;            // for subset, tracks the indexes in the original
  protected final RangeComposite crange;

  public CoverageCoordAxis1D(CoverageCoordAxisBuilder builder) {
    super(builder);

    if (axisType == null && builder.dependenceType == DependenceType.independent)
      throw new IllegalArgumentException("independent axis must have type");

    // make sure range has axisType as the name
    String rangeName = (axisType != null) ? axisType.toString() : null;
    if (builder.range != null) {
      this.range = (rangeName != null) ? builder.range.setName(rangeName) : builder.range;
    } else {
      this.range = Range.make(rangeName, getNcoords());
    }
    this.crange = builder.crange;
  }

  @Override
  public RangeIterator getRangeIterator() {
    return crange != null ? crange : range;
  }

  @Override
  public Range getRange() {
    return range;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s range=%s isSubset=%s", indent, range, isSubset());
    f.format("%n");
  }

  @Override
  public String getSummary() {
    if (axisType != AxisType.RunTime)
      return super.getSummary();

    if (ncoords < 7) {
      Formatter f = new Formatter();
      for (int i = 0; i < ncoords; i++) {
        CalendarDate cd = makeDate(getCoordMidpoint(i));
        if (i > 0) f.format(", ");
        f.format("%s", cd);
      }
      return f.toString();
    }

    Formatter f = new Formatter();
    CalendarDate start = makeDate(getStartValue());
    f.format("start=%s", start);
    CalendarDate end = makeDate(getEndValue());
    f.format(", end=%s", end);
    f.format(" (npts=%d spacing=%s)", getNcoords(), getSpacing());

    return f.toString();
  }


  ///////////////////////////////////////////////////////////////////
  // Spacing

  public boolean isAscending() {
    loadValuesIfNeeded();
    switch (spacing) {
      case regularInterval:
      case regularPoint:
        return getResolution() > 0;

      case irregularPoint:
        return values[0] <= values[ncoords - 1];

      case contiguousInterval:
        return values[0] <= values[ncoords];

      case discontiguousInterval:
        return values[0] <= values[2 * ncoords - 1];
    }
    throw new IllegalStateException("unknown spacing" + spacing);
  }

  public double getCoordMidpoint(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);
    loadValuesIfNeeded();

    switch (spacing) {
      case regularPoint:
        return startValue + index * getResolution();

      case irregularPoint:
        return values[index];

      case regularInterval:
        return startValue + (index + .5) * getResolution();

      case contiguousInterval:
      case discontiguousInterval:
        return (getCoordEdge1(index) + getCoordEdge2(index)) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge1(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);
    loadValuesIfNeeded();

    switch (spacing) {
      case regularPoint:
        return startValue + (index - .5) * getResolution();

      case regularInterval:
        return startValue + index * getResolution();

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
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);
    loadValuesIfNeeded();

    switch (spacing) {
      case regularPoint:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + (index + .5) * getResolution();

      case regularInterval:
        return startValue + (index+1) * getResolution();

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
    return getCoordEdge2(ncoords - 1);
  }

  @Override
  public Array getCoordsAsArray() {
    Array result;
    switch (dependenceType) {
      case scalar:
        result = Array.factory(getDataType(), new int[0]);
        break;
      default:
        result = Array.factory(getDataType(), new int[]{ncoords});
        break;
    }

    for (int i = 0; i < ncoords; i++)
      result.setDouble(i, getCoordMidpoint(i));
    return result;
  }

  @Override
  public Array getCoordBoundsAsArray() {
    Array result = Array.factory(getDataType(), new int[]{ncoords, 2});

    int count = 0;
    for (int i = 0; i < ncoords; i++) {
      result.setDouble(count++, getCoordEdge1(i));
      result.setDouble(count++, getCoordEdge2(i));
    }
    return result;
  }

  @Override
  public Optional<CoverageCoordAxis> subset(double minValue, double maxValue, int stride) throws InvalidRangeException {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    Optional<CoverageCoordAxisBuilder> buildero = helper.subset(minValue, maxValue, stride);
    return !buildero.isPresent() ? Optional.empty(buildero.getErrorMessage()) : Optional.of(new CoverageCoordAxis1D(buildero.get()));
  }

  // CalendarDate, double[2], or Double
  public Object getCoordObject(int index) {
    if (axisType == AxisType.RunTime)
      return makeDate(getCoordMidpoint(index));
    if (isInterval())
      return new double[]{getCoordEdge1(index), getCoordEdge2(index)};
    return getCoordMidpoint(index);
  }

  public List<NamedObject> getCoordValueNames() {
    loadValuesIfNeeded();
    if (timeHelper != null)
      return timeHelper.getCoordValueNames(this);

    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < ncoords; i++) {
      Object value = null;
      switch (spacing) {
        case regularPoint:
        case irregularPoint:
          value = Format.d(getCoordMidpoint(i), 3);
          break;

        case regularInterval:
        case contiguousInterval:
        case discontiguousInterval:
          value = new CoordInterval(getCoordEdge1(i), getCoordEdge2(i), 3);
          break;
      }
      result.add(new NamedAnything(value, value + " " + getUnits()));
    }

    return result;
  }

  @Override
  public CoverageCoordAxis copy() {
    return new CoverageCoordAxis1D(new CoverageCoordAxisBuilder(this));
  }

  @Override
  public Optional<CoverageCoordAxis> subset(SubsetParams params) {
    Optional<CoverageCoordAxisBuilder> buildero = subsetBuilder(params);
    return !buildero.isPresent() ? Optional.empty(buildero.getErrorMessage()) : Optional.of(new CoverageCoordAxis1D(buildero.get()));
  }

  // only for longitude, only for regular (do we need a subclass for longitude 1D coords ??
  public Optional<CoverageCoordAxis> subsetByIntervals(List<MAMath.MinMax> lonIntvs, int stride) {
    if (axisType != AxisType.Lon)
      return Optional.empty("subsetByIntervals only for longitude");
    if (!isRegular())
      return Optional.empty("subsetByIntervals only for regular longitude");

    CoordAxisHelper helper = new CoordAxisHelper(this);

    double start = Double.NaN;
    boolean first = true;
    List<RangeIterator> ranges = new ArrayList<>();
    for (MAMath.MinMax lonIntv : lonIntvs) {
      if (first) start = lonIntv.min;
      first = false;

      Optional<RangeIterator> opt = helper.makeRange(lonIntv.min, lonIntv.max, stride);
      if (!opt.isPresent())
        return Optional.empty(opt.getErrorMessage());
      ranges.add(opt.get());
    }

    try {
      RangeComposite compositeRange = new RangeComposite(AxisType.Lon.toString(), ranges);
      int npts = compositeRange.length();
      double end = start + npts * resolution;

      CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(this); // copy
      builder.subset(npts, start, end, resolution, null);
      builder.setRange(null);
      builder.setCompositeRange(compositeRange);

      return Optional.of(new CoverageCoordAxis1D(builder));
    } catch (InvalidRangeException e) {
      return Optional.empty(e.getMessage());
    }
  }

  public Optional<CoverageCoordAxis> subsetByIndex(Range range) {
    try {
      CoordAxisHelper helper = new CoordAxisHelper(this);
      CoverageCoordAxisBuilder builder = helper.subsetByIndex(range);
      return Optional.of(new CoverageCoordAxis1D(builder));
    } catch (InvalidRangeException e) {
      return Optional.empty(e.getMessage());
    }
  }

  // LOOK  incomplete handling of subsetting params
  protected Optional<CoverageCoordAxisBuilder> subsetBuilder(SubsetParams params) {
    if (params == null)
      return Optional.of(new CoverageCoordAxisBuilder(this));

    CoordAxisHelper helper = new CoordAxisHelper(this);

    switch (getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = params.getDouble(SubsetParams.vertCoord);
        if (dval != null)
          return Optional.of(helper.subsetClosest(dval));
        double[] vertRange = params.getVertRange(); // used by WCS
        if (vertRange != null)
          return helper.subset(vertRange[0], vertRange[1], 1);

        // default is all
        break;

      case Ensemble:
        Double eval = params.getDouble(SubsetParams.ensCoord);
        if (eval != null) {
          return Optional.of(helper.subsetClosest(eval));
        }
        // default is all
        break;

      // x,y get seperately subsetted
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        throw new IllegalArgumentException();
        // return null; // LOOK heres a case where null is "correct"

      case Time:
        if (params.isTrue(SubsetParams.timePresent))
          return Optional.of(helper.subsetLatest());

        CalendarDate date = (CalendarDate) params.get(SubsetParams.time);
        if (date != null)
          return Optional.of(helper.subsetClosest(date));

        Integer stride = (Integer) params.get(SubsetParams.timeStride);
        if (stride == null || stride < 0) stride = 1;

        CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
        if (dateRange != null)
          return helper.subset(dateRange, stride);

        // If no time range or time point, a timeOffset can be used to specify the time point.
        Double timeOffset = params.getDouble(SubsetParams.timeOffset);
        if (timeOffset != null)
          return Optional.of(helper.subsetClosest(timeOffset));

        if (stride != 1)
          try {
            return Optional.of(helper.subsetByIndex(getRange().setStride(stride)));
          } catch (InvalidRangeException e) {
            return Optional.empty(e.getMessage());
          }

        // default is all
        break;

      case RunTime:
        CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
        if (rundate != null)
          return Optional.of(helper.subsetClosest(rundate));

/*        CalendarDateRange rundateRange = (CalendarDateRange) params.get(SubsetParams.runtimeRange);
        if (rundateRange != null)
          return helper.subset(rundateRange, 1); */

        if (params.isTrue(SubsetParams.runtimeAll))
          break;

        // default is latest
        return Optional.of(helper.subsetLatest());

      case TimeOffset:
        Double oval = params.getDouble(SubsetParams.timeOffset);
        if (oval != null) {
          return Optional.of(helper.subsetClosest(oval));
        }

        if (params.isTrue(SubsetParams.timeOffsetFirst)) {
          try {
            return Optional.of(helper.subsetByIndex(new Range(1)));
          } catch (InvalidRangeException e) {
            return Optional.empty(e.getMessage());
          }
        }
        // default is all
        break;
    }

    // otherwise return copy the original axis
    return Optional.of(new CoverageCoordAxisBuilder(this));
  }

  @Override
  public Optional<CoverageCoordAxis> subsetDependent(CoverageCoordAxis1D dependsOn) {
    CoverageCoordAxisBuilder builder;
    try {
      builder = new CoordAxisHelper(this).subsetByIndex(dependsOn.getRange()); // LOOK Other possible subsets?
    } catch (InvalidRangeException e) {
      return Optional.empty(e.getMessage());
    }
    return Optional.of(new CoverageCoordAxis1D(builder));
  }

 // @Override
  public Iterator<Object> iterator() {
    return new MyIterator();
  }

  // Look what about intervals ??
  private class MyIterator implements java.util.Iterator<Object> {
    private int current = 0;
    private int ncoords = getNcoords();

    public boolean hasNext() {
      return current < ncoords;
    }

    public Object next() {
      return getCoordMidpoint(current++);
    }
  }

}

