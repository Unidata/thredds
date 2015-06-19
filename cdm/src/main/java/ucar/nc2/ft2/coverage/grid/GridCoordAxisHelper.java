/* Copyright */
package ucar.nc2.ft2.coverage.grid;

/**
 * Describe
 *
 * @author caron
 * @since 6/1/2015
 */
public class GridCoordAxisHelper {
  enum Mode {min, max, closest} // values is a min, max or closest

  GridCoordAxis axis;
  GridCoordAxisHelper(GridCoordAxis axis) {
    this.axis = axis;
  }

  /*
    private void subsetValues(GridCoordAxis result, double minValue, double maxValue) {
    double[] subsetValues;
    double resolution = getResolution();
    int minIndex, maxIndex;
    double subsetStart, subsetEnd;
    int count=0, count2=0;

    switch (spacing) {

      case irregularPoint:
        minIndex = 0;
        maxIndex = 0;
        for (double val : getValues()) {  // LOOK linear
          if (val > maxValue) break;
          if (val < minValue) minIndex++;
          maxIndex++;
        }
        count = maxIndex - minIndex;
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");

        subsetValues = new double[count];
        for (int i=minIndex; i<maxIndex; i++) {
          subsetValues[count2++] = getCoord(i);
        }

        result.setNvalues(count);
        result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex-1); // need closed interval
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length - 1]);
        break;

      case contiguousInterval:
        minIndex = -1;
        count = 0;
        for (int i=0; i<values.length; i++) {                       // these are edges
          if (values[i] <= maxValue && values[i+1] >= minValue) {   // count number of intervals that pass
            count++;
            if (minIndex < 0) minIndex = i;  // record first one that passes
          }
        }
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");
        maxIndex = minIndex+count-1;

        subsetValues = new double[count+1];            // need npts+1
        for (int i=minIndex; i<=maxIndex; i++)
          subsetValues[count2++] = values[i];

        result.setNvalues(count);
        result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length-1]);
        break;

      case discontiguousInterval:
        minIndex = -1;
        count = 0;
        for (int i=0; i<values.length; i+=2) {                       // these are bounds (low, high)1, (low,high)2, ...
          if (values[i] <= maxValue && values[i+1] >= minValue) {
            count++;     // count number of intervals that pass
            if (minIndex < 0) minIndex = i;  // record first one that passes
          }
        }
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");
        maxIndex = minIndex+count-1;

        subsetValues = new double[2*count];                   // need 2*npts
        for (int i=minIndex; i<=maxIndex; i+=2) {
          subsetValues[count2++] = values[i];
          subsetValues[count2++] = values[i+1];
        }

        result.setNvalues(count);
        result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length-1]);
        break;
    }
  }
   */

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
    throw new IllegalStateException("unknown spacing"+axis.getSpacing());
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
      throw new IllegalStateException("unknown spacing"+axis.getSpacing());
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
        if ((index>0) && (index < n) && axis.getCoord(index) < coordValue) index++; // result >= coordVal
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
   *    * values[i] <= target < values[i+1] (if values are ascending)
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
      while (high > low + 1) {
        mid = (low + high) / 2;                           // binary search
        double midVal = axis.getCoordEdge1(mid);
        if (midVal == target) return mid;                // look double compare
        else if (midVal < target) low = mid;
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
        double midVal = axis.getCoordEdge1(mid);
        if (midVal == target) return mid;
        else if (midVal < target) high = mid;
        else low = mid;
      }

      return high - 1;
    }
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * Only use if isContiguous() == false
   * This algorithm does a linear search in the bound1[] amd bound2[] array.
   * <p/>
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

  private int findCoordElementDiscontiguousInterval(double target, boolean bounded) {
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

}
