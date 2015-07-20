/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

/**
 * Describe
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
   */

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
  }

  private int findCoordElementDiscontiguousInterval(double target, boolean bounded) {
    ;
    int n = axis.getNcoords();

    if (axis.isAscending()) {
      // Check that the point is within range
      if (target < axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target > axis.getCoordEdge2(n - 1))
        return bounded ? n - 1 : -1;

      int idx = findSingleHit(target, true);
      if (idx >= 0) return idx;

      // multiple hits = choose closest to the midpoint i guess
      return findClosest(target);

    } else {

      // Check that the point is within range
      if (target > axis.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target < axis.getCoordEdge2(n-1))
        return bounded ? n - 1 : -1;

      int idx = findSingleHit(target, false);
      if (idx >= 0) return idx;

      // multiple hits = choose closest to the midpoint i guess
      return findClosest(target);
    }
  }

  // return index if only one match, else -1
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
    return hits == 1 ? idxFound : -1;
  }

  // return index of closest value to target
  private int findClosest(double target) {
    double minDiff =  Double.MAX_VALUE;
    int idxFound = -1;
    for (int i = 0; i < axis.getNcoords(); i++) {
      double diff =  Math.abs(axis.getCoord(i)-target);
      if (diff < minDiff) {
        minDiff = diff;
        idxFound = i;
      }
    }
    return idxFound;
  }

  //////////////////////////////////////////////////////////////

  public CoverageCoordAxis subset(double minValue, double maxValue) {
    return subsetValues(minValue, maxValue);
  }

  public CoverageCoordAxis subsetClosest(double want) {
    return subsetValuesClosest( want);
  }

  public CoverageCoordAxis subsetLatest() {
    return subsetValuesLatest();
  }

  public CoverageCoordAxis subset(CalendarDate date) {
    double want = axis.convert(date);
    return subsetValuesClosest(want);
  }

  public CoverageCoordAxis subset(CalendarDateRange dateRange) {
    double min = axis.convert(dateRange.getStart());
    double max = axis.convert(dateRange.getEnd());
    return subsetValues(min, max);
  }

  // look does min < max when !isAscending ?
  // look specialize when only one point
  private CoverageCoordAxis subsetValues(double minValue, double maxValue) {
    double[] subsetValues = null;
    int minIndex, maxIndex;
    int count2 = 0;

    minIndex = findCoordElementBounded(minValue, Mode.min);
    maxIndex = findCoordElementBounded(maxValue, Mode.max);
    int count = maxIndex - minIndex + 1;

    if (minIndex < 0)
      throw new IllegalArgumentException("no points in subset: min > end");
    if (maxIndex < 0)
      throw new IllegalArgumentException("no points in subset: max < start");
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    double[] values = axis.getValues();  // will be null for regular
    switch (axis.getSpacing()) {

      case irregularPoint:
        subsetValues = new double[count];
        for (int i = minIndex; i <= maxIndex; i++)
          subsetValues[count2++] = values[i];
        break;

      case contiguousInterval:
        subsetValues = new double[count + 1];            // need npts+1
        for (int i = minIndex; i <= maxIndex + 1; i++)
          subsetValues[count2++] = values[i];
        break;

      case discontiguousInterval:
        subsetValues = new double[2 * count];            // need 2*npts
        for (int i = minIndex; i <= maxIndex; i += 2) {
          subsetValues[count2++] = values[i];
          subsetValues[count2++] = values[i + 1];
        }
        break;
    }

    CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
            axis.getAttributes(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
            count, axis.getCoord(minIndex), axis.getCoord(maxIndex), axis.getResolution(), subsetValues, axis.reader);
    result.setIndexRange(minIndex, maxIndex, 1);
    return result;
  }

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */
  private CoverageCoordAxis subsetValuesClosest(double want) {
    double[] subsetValues = null;

    int want_index = findCoordElement(want, Mode.closest);

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

    CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
            axis.getAttributes(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
            1, axis.getCoord(want_index), axis.getCoord(want_index), axis.getResolution(), subsetValues, axis.reader);
    result.setIndexRange(want_index, want_index, 1);
    return result;
  }

  private CoverageCoordAxis subsetValuesLatest() {
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

    CoverageCoordAxis1D result = new CoverageCoordAxis1D(axis.getName(), axis.getUnits(), axis.getDescription(), axis.getDataType(), axis.getAxisType(),
            axis.getAttributes(), axis.getDependenceType(), axis.getDependsOnList(), axis.getSpacing(),
            1, start, end, axis.getResolution(), subsetValues, axis.reader);
    result.setIndexRange(last, last, 1);
    return result;
  }
}
