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

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Misc;

import java.util.Arrays;

/**
 * Helper class for CoverageCoordAxis1D.
 * Mostly subsetting.
 *
 * @author caron
 * @since 7/13/2015
 */
class CoordAxisHelper {
  enum Mode {min, max, closest} // values is a min, max or closest

  private final CoverageCoordAxis1D axis;

  CoordAxisHelper(CoverageCoordAxis1D axis) {
    this.axis = axis;
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * This means that
   * <pre>
   * edge[i] <= pos < edge[i+1] (if values are ascending)
   * edge[i] > pos >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target position in this coordinate system
   * @return index of grid point containing it, or -1 if outside grid area
   */
  public int findCoordElement(double target, Mode mode) {
    switch (axis.getSpacing()) {
      case regular:
        return findCoordElementRegular(target, mode, false);
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(target, false);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval(target, false);
    }
    throw new IllegalStateException("unknown spacing" + axis.getSpacing());
  }

  /**
   * Given a coordinate position, find what grid element contains it, or is closest to it.
   *
   * @param target position in this coordinate system
   * @return index of grid point containing it, or best estimate of closest grid interval.
   */
  public int findCoordElementBounded(double target, Mode mode) {
    switch (axis.getSpacing()) {
      case regular:
        return findCoordElementRegular(target, mode, true);
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(target, true);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval(target, true);
    }
    throw new IllegalStateException("unknown spacing" + axis.getSpacing());
  }

  //////////////////////////////////////////////////////////////////
  // following is from Jon Blower's ncWMS
  // faster routines for coordValue -> index search
  // significantly modified

  /**
   * Optimize the regular case
   * Gets the index of the given point. Uses index = (value - start) / stride,
   * hence this is faster than an exhaustive search.
   * from jon blower's ncWMS.
   *
   * @param coordValue The value along this coordinate axis
   * @param bounded    if false and not in range, return -1, else nearest index
   * @return the index that is nearest to this point, or -1 if the point is
   * out of range for the axis
   */
  private int findCoordElementRegular(double coordValue, Mode mode, boolean bounded) {
    int n = axis.getNcoords();

    double distance = coordValue - axis.getStartValue();
    double exactNumSteps = distance / axis.getResolution();
    int index = -1;

    switch (mode) {
      case min:
        index = (int) exactNumSteps;
        if ((index > 0) && (index < n) && axis.getCoord(index) < coordValue) index++; // result >= coordVal
        break;
      case max:
        index = (int) exactNumSteps;  // result <= coordVal
        break;
      case closest:
        index = (int) Math.round(exactNumSteps);
        break;
    }

    if (index < 0)
      return bounded ? 0 : -1;
    else if (index >= n)
      return bounded ? n - 1 : -1;


    return index;
  }

  /**
   * Performs a binary search to find the index of the element of the array
   * whose value is contained in the interval, so must be contiguous.
   * irregularPoint,    // irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval, // irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   *
   * @param target  The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target, or -1 if the target is out of range
   * * values[i] <= target < values[i+1] (if values are ascending)
   * values[i] > target >= values[i+1] (if values are descending)
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
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low+1) {
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
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low+1) {
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

  /**
   * Given a coordinate position, find what grid element contains it.
   * Only use if isContiguous() == false
   * This algorithm does a linear search in the bound1[] amd bound2[] array.
   * <p>
   * This means that
   * <pre>
   * edge[i] <= pos < edge[i+1] (if values are ascending)
   * edge[i] > pos >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target  The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target,
   * or -1 if the target is out of range
   *

  private int findCoordElementDiscontiguousIntervalOld(double target, boolean bounded) {
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
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;                           // binary search
        if ((axis.getCoordEdge1(mid) <= target) && (target <= axis.getCoordEdge2(mid)))
          return mid;
        else if (axis.getCoordEdge2(mid) < target) low = mid;
        else high = mid;
      }

      return low;

    } else {  // descending

      // Check that the point is within range
      if (target > axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target < axis.getCoordEdgeLast())
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;         // binary search
        if ((axis.getCoordEdge1(mid) >= target) && (target >= axis.getCoordEdge2(mid)))
          return mid;
        else if (axis.getCoordEdge1(mid) > target) high = mid;
        else low = mid;
      }

      return high - 1;
    }
  } */

  private int findCoordElementDiscontiguousInterval(double target, boolean bounded) {
    int n = axis.getNcoords();

    if (axis.isAscending()) {
      // Check that the point is within range
      if (target < axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target > axis.getCoordEdge2(n - 1))
        return bounded ? n - 1 : -1;

      int idx = findSingleHit(target, true);
      if (idx >= 0) return idx;
      if (idx == -1) return -1; // no hits

      // multiple hits = choose closest to the midpoint
      return findClosest(target);

    } else {

      // Check that the point is within range
      if (target > axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target < axis.getCoordEdge2(n-1))
        return bounded ? n - 1 : -1;

      int idx = findSingleHit(target, false);
      if (idx >= 0) return idx;
      if (idx == -1) return -1; // no hits

      // multiple hits = choose closest to the midpoint
      return findClosest(target);
    }
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(double target, boolean ascending) {
    int hits = 0;
    int idxFound = -1;
    int n = axis.getNcoords();
    for (int i = 0; i < n; i++) {
      if (contains(target, i, ascending)) {
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
    double minDiff =  Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;
    int idxFound = -1;
    for (int i = 0; i < axis.getNcoords(); i++) {
      double coord = axis.getCoord(i);
      double diff =  Math.abs(coord-target);
      if (diff < minDiff || (diff == minDiff && coord > useValue)) {
        minDiff = diff;
        idxFound = i;
        useValue = coord;
      }
    }
    return idxFound;
  }

  //////////////////////////////////////////////////////////////

  public CoverageCoordAxis1D subset(double minValue, double maxValue) {
    return subsetValues(minValue, maxValue);
  }

  public CoverageCoordAxis1D subsetClosest(double want) {
    return subsetValuesClosest(want);
  }

  public CoverageCoordAxis1D subsetLatest() {
    return subsetValuesLatest();
  }

  public CoverageCoordAxis1D subset(CalendarDate date) {
    double want = axis.convert(date);
    return subsetValuesClosest(want);
  }

  public CoverageCoordAxis1D subset(CalendarDateRange dateRange) {
    double min = axis.convert(dateRange.getStart());
    double max = axis.convert(dateRange.getEnd());
    return subsetValues(min, max);
  }

  // look does min < max when !isAscending ?
  // look could specialize when only one point
  private CoverageCoordAxis1D subsetValues(double minValue, double maxValue) {

    int minIndex = findCoordElementBounded(minValue, Mode.min);
    int maxIndex = findCoordElementBounded(maxValue, Mode.max);
    int count = maxIndex - minIndex + 1;

    if (minIndex < 0)
      throw new IllegalArgumentException("no points in subset: min > end");
    if (maxIndex < 0)
      throw new IllegalArgumentException("no points in subset: max < start");
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    return subsetValues(minIndex, maxIndex);
  }

  public CoverageCoordAxis1D subsetValues(int minIndex, int maxIndex) {
    double[] subsetValues = null;
    int ncoords = maxIndex - minIndex + 1;

    int count2 = 0;
    double[] values = axis.getValues();  // will be null for regular
    switch (axis.getSpacing()) {

      case irregularPoint:
        subsetValues = new double[ncoords];
        for (int i = minIndex; i <= maxIndex; i++)
          subsetValues[count2++] = values[i];
        break;

      case contiguousInterval:
        subsetValues = new double[ncoords + 1];            // need npts+1
        for (int i = minIndex; i <= maxIndex + 1; i++)
          subsetValues[count2++] = values[i];
        break;

      case discontiguousInterval:
        subsetValues = new double[2 * ncoords];            // need 2*npts
        for (int i = minIndex; i <= maxIndex; i += 2) {
          subsetValues[count2++] = values[i];
          subsetValues[count2++] = values[i + 1];
        }
        break;
    }

   // CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
   //         axis.getAttributeContainer(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
   //         count, axis.getCoord(minIndex), axis.getCoord(maxIndex), axis.getResolution(), subsetValues, axis.reader, true);
    CoverageCoordAxis1D result = axis.subset(ncoords, axis.getCoord(minIndex), axis.getCoord(maxIndex), subsetValues);
    result.setIndexRange(minIndex, maxIndex, 1);
    return result;
  }


  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */
  private CoverageCoordAxis1D subsetValuesClosest(double want) {
    double[] subsetValues = null;

    int want_index = findCoordElementBounded(want, Mode.closest);  // LOOK bounded
    /* if (want_index < 0) {
      System.out.println("HEY");
      findCoordElementBounded(want, Mode.closest);
    } */

    switch (axis.getSpacing()) {
      case contiguousInterval:
      case irregularPoint:
        if (axis.getSpacing() == CoverageCoordAxis.Spacing.irregularPoint) {
          subsetValues = new double[1];
          subsetValues[0] = axis.getCoord(want_index);
        } else {
          subsetValues = new double[2];
          subsetValues[0] = axis.getCoordEdge1(want_index);
          subsetValues[1] = axis.getCoordEdge2(want_index);
        }
        break;

      case discontiguousInterval:
        subsetValues = new double[2];
        subsetValues[0] = axis.getCoordEdge1(want_index);
        subsetValues[1] = axis.getCoordEdge2(want_index);
        break;
    }

   // CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
   //         axis.getAttributeContainer(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
   //         1, axis.getCoord(want_index), axis.getCoord(want_index), axis.getResolution(), subsetValues, axis.reader, true);
    CoverageCoordAxis1D result = axis.subset(1, axis.getCoord(want_index), axis.getCoord(want_index), subsetValues);
    result.setIndexRange(want_index, want_index, 1);
    return result;
  }

  private CoverageCoordAxis1D subsetValuesLatest() {
    double[] subsetValues = null;

    int last = axis.getNcoords()-1;
    double start = axis.getCoord(last);
    double end = axis.getCoord(last);

    switch (axis.getSpacing()) {
      case irregularPoint:
        subsetValues = new double[1];
        subsetValues[0] = axis.getCoord(last);
        break;

      case discontiguousInterval:
      case contiguousInterval:
        subsetValues = new double[2];
        start = subsetValues[0] = axis.getCoordEdge1(last);
        end = subsetValues[1] = axis.getCoordEdge2(last);
        break;
    }

    //CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
    //        axis.getAttributeContainer(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
    //        1, start, end, axis.getResolution(), subsetValues, axis.reader, true);
    CoverageCoordAxis1D result = axis.subset(1, start, end, subsetValues);
    result.setIndexRange(last, last, 1);
    return result;
  }

  public int search(double want) {
    if (axis.getNcoords() == 1) {
      return Misc.closeEnough(want, axis.getStartValue()) ? 0 : -1;
    }
    if (axis.isRegular()) {
      double fval = (want - axis.getStartValue()) / axis.getResolution();
      double ival = Math.rint(fval);
      return Misc.closeEnough(fval, ival) ? (int) ival : (int) -ival-1; // LOOK
    }

    // otherwise do a binary searcg
    return Arrays.binarySearch( axis.getValues(), want);
  }
}
