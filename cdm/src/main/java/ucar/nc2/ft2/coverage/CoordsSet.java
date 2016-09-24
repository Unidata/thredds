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

import ucar.ma2.Index;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An iterator over the coordinates in a set of CoverageCoordAxis.
 * The independent axes are used to iterate.
 * Attached dependent axes are used to generate values also. Two cases handled so far:
 *   1) ConstantForecast: runtime (ind), timeOffset (dep), time (scalar)
 *   1) Best: time (ind), runtime (dep)
 *
 *  Grib: If theres a time offset, then there must be a runtime coordinate, and the time offset is reletive to that.
 *  LOOK only used by Grib
 */
@Immutable
public class CoordsSet implements Iterable<SubsetParams> {
  /* public static final String runDate = "runDate";                 // CalendarDate
  public static final String timeOffsetCoord = "timeOffsetCoord";   // Double or double[] (intv)
  public static final String timeOffsetDate = "timeOffsetDate";   // CalendarDate (validation only)
  // public static final String timeCoord = "timeCoord";   // Double or double[] (intv)
  public static final String vertCoord = "vertCoord";   // Double or double[] (intv)
  public static final String ensCoord = "ensCoord";   // Double */

  public static CoordsSet factory(boolean constantForecast, List<CoverageCoordAxis> axes) {
    return new CoordsSet(constantForecast, axes);
  }

  ///////////////////////////////////////////////////////
  private final boolean constantForecast;
  private final List<CoverageCoordAxis> axes;     // all axes
  private final int[] shape;                      // only independent

  private CoordsSet(boolean constantForecast, List<CoverageCoordAxis> axes) {
    this.constantForecast = constantForecast;
    List<CoverageCoordAxis1D> indAxes = new ArrayList<>();
    int rank = 0;

    for (CoverageCoordAxis axis : axes) {
      if (axis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent)  // independent or scalar
        indAxes.add( (CoverageCoordAxis1D) axis);
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
        rank++;
    }
    this.axes = axes;

    this.shape = new int[rank];
    int count = 0;
    for (CoverageCoordAxis1D axis : indAxes) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
        shape[count++] = axis.getNcoords();
    }
  }

  public int[] getShape() {
    return shape;
  }

  public int[] getShape(RangeIterator y, RangeIterator x) {
    int[] result = new int[getRank() + 2];
    System.arraycopy(shape, 0, result, 0, shape.length);
    result[shape.length] = y.length();
    result[shape.length+1] = x.length();
    return result;
  }

  public int getRank() {
    return shape.length;
  }

  @Override
  public Iterator<SubsetParams> iterator() {
    return new CoordIterator();
  }

  private class CoordIterator implements Iterator<SubsetParams> {
    private int[] odo = new int[getRank()];
    private int[] shape = getShape();
    private long done, total;

    CoordIterator() {
      done = 0;
      total = Index.computeSize(shape); // total number of elements
    }

    public boolean hasNext() {
      return done < total;
    }

    public SubsetParams next() {
      SubsetParams next = currentElement();
      done++;
      if (done < total) incr(); // increment for next call
      return next;
    }

    private void incr() {
      int digit = getRank() - 1;
      while (digit >= 0) {
        odo[digit]++;
        if (odo[digit] < shape[digit])
          break; // normal exit

        // else, carry to next digit in the odometer
        odo[digit] = 0;
        digit--;
      }
    }

    private SubsetParams currentElement() {
      SubsetParams result = new SubsetParams();
      int odoIndex = 0;
      CalendarDate runtime = null;
      for (CoverageCoordAxis axis : axes) {
        if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.dependent) continue;
        CoverageCoordAxis1D axis1D = (CoverageCoordAxis1D) axis;

        int coordIdx = (axis.getDependenceType() == CoverageCoordAxis.DependenceType.scalar) ? 0 : odo[odoIndex];
        Object coord = axis1D.getCoordObject(coordIdx); // CalendarDate (runtime), Double or double{} (interval)

        if (axis.getAxisType() == AxisType.RunTime) {
          runtime = (CalendarDate) coord;
          result.setRunTime(runtime);

          if (constantForecast) {
            CoverageCoordAxis1D timeOffsetCF = findDependent(axis, AxisType.TimeOffset);
            if (timeOffsetCF != null) {
              addAdjustedTimeCoords(result, timeOffsetCF, coordIdx, runtime);
            }
          }

        } else if (axis.getAxisType() == AxisType.Time) {
          CoverageCoordAxis1D runtimeForBest = findDependent(axis, AxisType.RunTime);
          if (runtimeForBest != null) {
            runtime = (CalendarDate) runtimeForBest.getCoordObject(coordIdx); // CalendarDate
            result.setRunTime(runtime);
          }
          assert runtime != null;
          addAdjustedTimeCoords(result, axis1D, coordIdx, runtime);
        }

        else if (axis.getAxisType().isVert()) {
          if (coord instanceof Double)
            result.setVertCoord( (Double) coord);
          else if (coord instanceof double[])
            result.setVertCoordIntv((double[]) coord);
          else
            throw new IllegalStateException("unknow vert coord type "+coord.getClass().getName());
        }

        else if (axis.getAxisType() == AxisType.Ensemble)
          result.setEnsCoord((Double) coord);

        else if (!constantForecast && axis.getAxisType() == AxisType.TimeOffset) {
          if (coord instanceof Double)
            result.setTimeOffset( (Double) coord);
          else if (coord instanceof double[])
            result.setTimeOffsetIntv((double[]) coord);
          else
            throw new IllegalStateException("unknow time coord type "+coord.getClass().getName());

          double val = axis.isInterval() ? (axis1D.getCoordEdge1(coordIdx) + axis1D.getCoordEdge2(coordIdx)) / 2.0  : axis1D.getCoordMidpoint(coordIdx);
          assert runtime != null;
          result.set(SubsetParams.timeOffsetDate, axis.makeDateInTimeUnits(runtime, val)); // validation
          result.set(SubsetParams.timeOffsetUnit, axis.getCalendarDateUnit()); // validation
        }

        if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
          odoIndex++;
      }
      return result;
    }

  }

  private void addAdjustedTimeCoords(SubsetParams result, CoverageCoordAxis1D axis, int coordIdx, CalendarDate runtime) {
    // this must be adjusted to be offset from the runtime.
    // adjust = end - start
    // axisCoordOffset + axis.reftime = offset + runtime
    // offset = axisCoordOffset + axis.reftime - runtime
    // offset = axisCoordOffset + adjust
    // offset = axisCoordOffset + end - start = axisCoordOffset + axis.reftime - runtime
    // therefore: end = reftime, start = runtime
    double adjust = axis.getOffsetInTimeUnits(runtime, axis.getRefDate());
    if (axis.isInterval()) {
      double[] adjustVal = new double[] {axis.getCoordEdge1(coordIdx)+adjust, axis.getCoordEdge2(coordIdx)+adjust};
      result.setTimeOffsetIntv(adjustVal);
      double mid = (adjustVal[0]+adjustVal[1]) / 2.0;
      result.set(SubsetParams.timeOffsetUnit, axis.makeDateInTimeUnits(runtime, mid)); // validation
      result.set(SubsetParams.timeOffsetUnit, axis.getCalendarDateUnit()); // validation
    } else {
      double adjustVal = axis.getCoordMidpoint(coordIdx) + adjust;
      result.setTimeOffset(adjustVal);
      result.set(SubsetParams.timeOffsetDate, axis.makeDateInTimeUnits(runtime, adjustVal)); // validation
      result.set(SubsetParams.timeOffsetUnit, axis.getCalendarDateUnit()); // validation
    }
  }

  // find the dependent axis that depend on independentAxis
  private CoverageCoordAxis1D  findDependent( CoverageCoordAxis independentAxis, AxisType axisType) {
    for (CoverageCoordAxis axis : axes) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.dependent) {
        for (String axisName : axis.dependsOn) {
          if (axisName.equalsIgnoreCase(independentAxis.getName()) && axis.getAxisType() == axisType)
            return (CoverageCoordAxis1D) axis;
        }
      }
    }
    return null;
  }
}
