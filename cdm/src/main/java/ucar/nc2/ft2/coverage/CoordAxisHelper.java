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

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Misc;

import javax.annotation.Nonnull;
import java.util.Arrays;

import ucar.nc2.util.Optional;

/**
 * Helper class for CoverageCoordAxis1D for subsetting and searching.
 *
 * @author caron
 * @since 7/13/2015
 */
class CoordAxisHelper {
  private final CoverageCoordAxis1D axis;

  CoordAxisHelper(CoverageCoordAxis1D axis) {
    this.axis = axis;
  }

  /**
   * Given a coordinate interval, find what grid element matches it.
   * @param target  interval in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area
   */
  int findCoordElement(double[] target, boolean bounded) {
    switch (axis.getSpacing()) {
      case regularInterval:
        // can use midpoint
        return findCoordElementRegular((target[0]+target[1])/2, bounded);
      case contiguousInterval:
        // can use midpoint
        return findCoordElementContiguous((target[0]+target[1])/2, bounded);
      case discontiguousInterval:
        // cant use midpoint
        return findCoordElementDiscontiguousInterval(target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + axis.getSpacing());
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * This means that
   * <pre>
   * edge[i] <= target < edge[i+1] (if values are ascending)
   * edge[i] > target >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target  position in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area
   */
  int findCoordElement(double target, boolean bounded) {
    switch (axis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        return findCoordElementRegular(target, bounded);
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(target, bounded);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval(target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + axis.getSpacing());
  }

  // same contract as findCoordElement()
  private int findCoordElementRegular(double coordValue, boolean bounded) {
    int n = axis.getNcoords();
    if (n == 1 && bounded) return 0;

    double distance = coordValue - axis.getCoordEdge1(0);
    double exactNumSteps = distance / axis.getResolution();
    //int index = (int) Math.round(exactNumSteps); // ties round to +Inf
    int index = (int) exactNumSteps; // truncate down

    if (bounded && index < 0) return 0;
    if (bounded && index >= n) return n - 1;

    // check that found point is within interval
    if (index >= 0 && index < n) {
      double lower = axis.getCoordEdge1(index);
      double upper = axis.getCoordEdge2(index);
      if (axis.isAscending()) {
        assert lower <= coordValue : lower + " should be le " + coordValue;
        assert upper >= coordValue : upper + " should be ge " + coordValue;
      } else {
        assert lower >= coordValue : lower + " should be ge " + coordValue;
        assert upper <= coordValue : upper + " should be le " + coordValue;
      }
    }

    return index;
  }

  /**
   * Performs a binary search to find the index of the element of the array whose value is contained in the contiguous intervals.
   * irregularPoint,    // irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval, // irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * <p>
   * same contract as findCoordElement()
   */
  private int findCoordElementContiguous(double target, boolean bounded) {
    int n = axis.getNcoords();
    //double resolution = (values[n-1] - values[0]) / (n - 1);
    //int startGuess = (int) Math.round((target - values[0]) / resolution);

    int low = 0;
    int high = n - 1;
    if (axis.isAscending()) {
      // Check that the point is within range
      if (target < axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target > axis.getCoordEdgeLast())
        return bounded ? n - 1 : n;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;                           // binary search
        if (contains(target, mid, true)) return mid;
        else if (axis.getCoordEdge2(mid) < target) low = mid;
        else high = mid;
      }
      return contains(target, low, true) ? low : high;

    } else {  // descending

      // Check that the point is within range
      if (target > axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target < axis.getCoordEdgeLast())
        return bounded ? n - 1 : n;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;         // binary search
        if (contains(target, mid, false)) return mid;
        else if (axis.getCoordEdge2(mid) < target) high = mid;
        else low = mid;
      }
      return contains(target, low, false) ? low : high;
    }
  }

  private boolean contains(double target, int coordIdx, boolean ascending) {
    double midVal1 = axis.getCoordEdge1(coordIdx);
    double midVal2 = axis.getCoordEdge2(coordIdx);
    if (ascending)
      return (midVal1 <= target && target <= midVal2);
    else
      return (midVal1 >= target && target >= midVal2);
  }

  private boolean contains(double target, int coordIdx) {
    double midVal1 = axis.getCoordEdge1(coordIdx);
    double midVal2 = axis.getCoordEdge2(coordIdx);
    if (midVal1 < midVal2)
      return (midVal1 <= target && target <= midVal2);
    else
      return (midVal1 >= target && target >= midVal2);
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not contained in any interval
  // LOOK not using bounded
  private int findCoordElementDiscontiguousInterval(double target, boolean bounded) {
    int idx = findSingleHit(target);
    if (idx >= 0) return idx;
    if (idx == -1) return -1; // no hits

    // multiple hits = choose closest to the midpoint
    return findClosest(target);
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not found
  // LOOK not using bounded
  private int findCoordElementDiscontiguousInterval(double[] target, boolean bounded) {
    for (int i = 0; i < axis.getNcoords(); i++) {
      double edge1 = axis.getCoordEdge1(i);
      double edge2 = axis.getCoordEdge2(i);
      if (Misc.closeEnough(edge1, target[0]) && Misc.closeEnough(edge2, target[1]))
        return i;
    }
    return -1;
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(double target) {
    int hits = 0;
    int idxFound = -1;
    int n = axis.getNcoords();
    for (int i = 0; i < n; i++) {
      if (contains(target, i)) {
        hits++;
        idxFound = i;
      }
    }
    if (hits == 1) return idxFound;
    if (hits == 0) return -1;
    return -hits;
  }

  // return index of closest value to target
  // if its a tie, use the larger one
  private int findClosest(double target) {
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;
    int idxFound = -1;
    for (int i = 0; i < axis.getNcoords(); i++) {
      double coord = axis.getCoordMidpoint(i);
      double diff = Math.abs(coord - target);
      if (diff < minDiff || (diff == minDiff && coord > useValue)) {
        minDiff = diff;
        idxFound = i;
        useValue = coord;
      }
    }
    return idxFound;
  }

  //////////////////////////////////////////////////////////////

  public Optional<CoverageCoordAxisBuilder> subset(double minValue, double maxValue, int stride) {
    return subsetValues(minValue, maxValue, stride);
  }

  @Nonnull
  public CoverageCoordAxisBuilder subsetClosest(double want) {
    return subsetValuesClosest(want);
  }

  @Nonnull
  public CoverageCoordAxisBuilder subsetLatest() {
    return subsetValuesLatest();
  }

  @Nonnull
  public CoverageCoordAxisBuilder subsetClosest(CalendarDate date) {
    double want = axis.convert(date);
    return subsetValuesClosest(want);
  }

  @Nonnull
  public CoverageCoordAxisBuilder subsetClosest(CalendarDate[] date) {
    double[] want = new double[2];
    want[0] = axis.convert(date[0]);
    want[1] = axis.convert(date[1]);
    return subsetValuesClosest(want);
  }

  public Optional<CoverageCoordAxisBuilder> subset(CalendarDateRange dateRange, int stride) {
    double min = axis.convert(dateRange.getStart());
    double max = axis.convert(dateRange.getEnd());
    return subsetValues(min, max, stride);
  }

  // look could specialize when only one point
  // look must handle discon interval different
  private Optional<CoverageCoordAxisBuilder> subsetValues(double minValue, double maxValue, int stride) {
    if (axis.getSpacing() == CoverageCoordAxis.Spacing.discontiguousInterval)
      return subsetValuesDiscontinuous(minValue, maxValue, stride);

    double lower = axis.isAscending() ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = axis.isAscending() ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = findCoordElement(lower, false);
    int maxIndex = findCoordElement(upper, false);

    if (minIndex >= axis.getNcoords())
      return Optional.empty(String.format("no points in subset: lower %f > end %f", lower, axis.getEndValue()));
    if (maxIndex < 0)
      return Optional.empty(String.format("no points in subset: upper %f < start %f", upper, axis.getStartValue()));

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= axis.getNcoords())
      maxIndex = axis.getNcoords() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    try {
      return Optional.of(subsetByIndex(new Range(minIndex, maxIndex, stride)));
    } catch (InvalidRangeException e) {
      return Optional.empty(e.getMessage());
    }
  }

  public Optional<RangeIterator> makeRange(double minValue, double maxValue, int stride) {
    //if (axis.getSpacing() == CoverageCoordAxis.Spacing.discontiguousInterval)
    //  return subsetValuesDiscontinuous(minValue, maxValue, stride);

    double lower = axis.isAscending() ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = axis.isAscending() ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = findCoordElement(lower, false);
    int maxIndex = findCoordElement(upper, false);

    if (minIndex >= axis.getNcoords())
      return Optional.empty(String.format("no points in subset: lower %f > end %f", lower, axis.getEndValue()));
    if (maxIndex < 0)
      return Optional.empty(String.format("no points in subset: upper %f < start %f", upper, axis.getStartValue()));

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= axis.getNcoords())
      maxIndex = axis.getNcoords() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0)
      return Optional.empty("no points in subset");

    try {
      return Optional.of(new Range(minIndex, maxIndex, stride));
    } catch (InvalidRangeException e) {
      return Optional.empty(e.getMessage());
    }
  }

  private Optional<CoverageCoordAxisBuilder> subsetValuesDiscontinuous(double minValue, double maxValue, int stride) {
    return Optional.empty("subsetValuesDiscontinuous not done yet"); // LOOK
  }

  // Range must be contained in this range
  @Nonnull
  CoverageCoordAxisBuilder subsetByIndex(Range range) throws InvalidRangeException {
    int ncoords = range.length();
    if (range.last() >= axis.getNcoords())
      throw new InvalidRangeException("range.last() >= axis.getNcoords()");

    double resolution = 0.0;

    int count2 = 0;
    double[] values = axis.getValues();  // will be null for regular
    double[] subsetValues = null;
    switch (axis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        resolution = range.stride() * axis.getResolution();
        break;

      case irregularPoint:
        subsetValues = new double[ncoords];
        for (int i : range)
          subsetValues[count2++] = values[i];
        break;

      case contiguousInterval:
        subsetValues = new double[ncoords + 1];            // need npts+1
        for (int i : range)
          subsetValues[count2++] = values[i];
        subsetValues[count2] = values[range.last() + 1];
        break;

      case discontiguousInterval:
        subsetValues = new double[2 * ncoords];            // need 2*npts
        for (int i : range) {
          subsetValues[count2++] = values[2 * i];
          subsetValues[count2++] = values[2 * i + 1];
        }
        break;
    }

    // subset(int ncoords, double start, double end, double[] values)
    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(axis);
    builder.subset(ncoords, axis.getCoordMidpoint(range.first()), axis.getCoordMidpoint(range.last()), resolution, subsetValues);
    builder.setRange(range);
    return builder;
  }

  @Nonnull
  private CoverageCoordAxisBuilder subsetValuesClosest(double[] want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index
    if (closest_index < 0)
      findCoordElement(want, true);
    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(axis);

    if (axis.spacing == CoverageCoordAxis.Spacing.regularInterval) {
      double val1 = axis.getCoordEdge1(closest_index);
      double val2 = axis.getCoordEdge2(closest_index);
      builder.subset(1, val1, val2, val2-val1, null);

    } else {
      builder.subset(1, 0, 0, 0.0, makeValues(closest_index));
    }

    try {
      builder.setRange(new Range(closest_index, closest_index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  @Nonnull
  private CoverageCoordAxisBuilder subsetValuesClosest(double want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index
    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(axis);

    if (axis.spacing == CoverageCoordAxis.Spacing.regularPoint) {
      double val = axis.getCoordMidpoint(closest_index);
      builder.subset(1, val, val, 0.0, null);

    } else if (axis.spacing == CoverageCoordAxis.Spacing.regularInterval) {
      double val1 = axis.getCoordEdge1(closest_index);
      double val2 = axis.getCoordEdge2(closest_index);
      builder.subset(1, val1, val2, val2-val1, null);

    } else {
      builder.subset(1, 0, 0, 0.0, makeValues(closest_index));
    }

    try {
      builder.setRange(new Range(closest_index, closest_index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  Optional<CoverageCoordAxisBuilder> subsetContaining(double want) {
    int index = findCoordElement(want, false); // not bounded, may not be valid index
    if (index < 0 || index >= axis.getNcoords())
      return Optional.empty(String.format("value %f not in axis %s", want, axis.getName()));

    double val = axis.getCoordMidpoint(index);

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(axis);
    builder.subset(1, val, val, 0.0, makeValues(index));
    try {
      builder.setRange(new Range(index, index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return Optional.of(builder);
  }

  @Nonnull
  private CoverageCoordAxisBuilder subsetValuesLatest() {
    int last = axis.getNcoords() - 1;
    double val = axis.getCoordMidpoint(last);

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(axis);
    builder.subset(1, val, val, 0.0, makeValues(last));
    try {
      builder.setRange(new Range(last, last));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  private double[] makeValues(int index) {
    double[] subsetValues = null; // null for regular

    switch (axis.getSpacing()) {
      case irregularPoint:
        subsetValues = new double[1];
        subsetValues[0] = axis.getCoordMidpoint(index);
        break;

      case discontiguousInterval:
      case contiguousInterval:
        subsetValues = new double[2];
        subsetValues[0] = axis.getCoordEdge1(index);
        subsetValues[1] = axis.getCoordEdge2(index);
        break;
    }
    return subsetValues;
  }

  int search(double want) {
    if (axis.getNcoords() == 1) {
      return Misc.closeEnough(want, axis.getStartValue()) ? 0 : -1;
    }
    if (axis.isRegular()) {
      double fval = (want - axis.getStartValue()) / axis.getResolution();
      double ival = Math.rint(fval);
      return Misc.closeEnough(fval, ival) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return Arrays.binarySearch(axis.getValues(), want);
  }
}
