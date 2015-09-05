package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.Index;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;

import java.util.*;

/**
 * The independent axes are used to iterate.
 * Attached dependent axes are used to generate values also. Two cases handled so far:
 *   1) ConstantForecast: runtime (ind), timeOffset (dep), time (scalar)
 *   1) Best: time (ind), runtime (dep)
 *
 *  Grib: If theres a time offset, then there must be a runtime coordinate, and the time offset is reletive to that.
 */
@Immutable
public class CoordsSet implements Iterable<Map<String, Object>> {
  public static final String runDate = "runDate";                 // CalendarDate
  public static final String timeOffsetCoord = "timeOffsetCoord";   // Double or double[] (intv)
  public static final String timeOffsetDate = "timeOffsetDate";   // CalendarDate (validation only)
  // public static final String timeCoord = "timeCoord";   // Double or double[] (intv)
  public static final String vertCoord = "vertCoord";   // Double or double[] (intv)
  public static final String ensCoord = "ensCoord";   // Double

  public static CoordsSet factory(boolean constantForecast, List<CoverageCoordAxis1D> axes) {
    return new CoordsSet(constantForecast, axes);
  }

  ///////////////////////////////////////////////////////
  private final boolean constantForecast;
  private final List<CoverageCoordAxis1D> axes;    // all axes
  private final int[] shape;                       // only independent

  private CoordsSet(boolean constantForecast, List<CoverageCoordAxis1D> axes) {
    this.constantForecast = constantForecast;
    List<CoverageCoordAxis1D> indAxes = new ArrayList<>();

    for (CoverageCoordAxis1D axis : axes) {
      if (axis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent) { // independent or scalar
        indAxes.add( axis);
      }
    }
    this.axes = indAxes;

    this.shape = new int[indAxes.size()];
    int count = 0;
    for (CoverageCoordAxis1D axis : indAxes) {
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
  public Iterator<Map<String, Object>> iterator() {
    return new CoordIterator();
  }

  private class CoordIterator implements Iterator<Map<String, Object>> {
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

    public Map<String, Object> next() {
      Map<String, Object> next = currentElement();
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

    private Map<String, Object> currentElement() {
      Map<String, Object> result = new HashMap<>();
      int odoIndex = 0;
      CalendarDate runtime = null;
      for (CoverageCoordAxis1D axis : axes) {
        int coordIdx = (axis.getDependenceType() == CoverageCoordAxis.DependenceType.scalar) ? 0 : odo[odoIndex];
        Object coord = axis.getCoordObject(coordIdx); // CalendarDate (runtime), Double or double{} (interval)

        if (axis.getAxisType() == AxisType.RunTime) {
          runtime = (CalendarDate) coord;
          result.put(runDate, runtime);

          if (constantForecast) {
            CoverageCoordAxis1D timeOffsetCF = axis.getDependent();
            if (timeOffsetCF != null && timeOffsetCF.getAxisType() == AxisType.TimeOffset) {
              addAdjustedTimeCoords(result, timeOffsetCF, coordIdx, runtime);
            }
          }

        } else if (axis.getAxisType() == AxisType.Time) {

          CoverageCoordAxis1D runtimeForBest = axis.getDependent();
          if (runtimeForBest != null && runtimeForBest.getAxisType() == AxisType.RunTime) {
            runtime = (CalendarDate) runtimeForBest.getCoordObject(coordIdx); // CalendarDate
            result.put(runDate, runtime);
          }

          addAdjustedTimeCoords(result, axis, coordIdx, runtime);
        }

        else if (axis.getAxisType().isVert())
          result.put(vertCoord, coord);

        else if (axis.getAxisType() == AxisType.Ensemble)
          result.put(ensCoord, coord);

        else if (!constantForecast && axis.getAxisType() == AxisType.TimeOffset) {
          result.put(timeOffsetCoord, coord);
          double val = axis.isInterval() ? (axis.getCoordEdge1(coordIdx) + axis.getCoordEdge2(coordIdx)) / 2.0  : axis.getCoord(coordIdx);
          result.put(timeOffsetDate, axis.makeDateInTimeUnits(runtime, val)); // validation
        }

        if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
          odoIndex++;
      }
      return result;
    }

  }

  private void addAdjustedTimeCoords(Map<String, Object> result, CoverageCoordAxis1D axis, int coordIdx, CalendarDate runtime) {
    // this must be adjusted to be offset from the runtime.
    double adjust = axis.getOffsetInTimeUnits(axis.getRefDate(), runtime);
    if (axis.isInterval()) {
      double[] adjustVal = new double[] {axis.getCoordEdge1(coordIdx)+adjust, axis.getCoordEdge2(coordIdx)+adjust};
      result.put(timeOffsetCoord, adjustVal);
      double mid = (adjustVal[0]+adjustVal[1]) / 2.0;
      result.put(timeOffsetDate, axis.makeDateInTimeUnits(runtime, mid)); // validation
    } else {
      double adjustVal = axis.getCoord(coordIdx) + adjust;
      result.put(timeOffsetCoord, adjustVal);
      result.put(timeOffsetDate, axis.makeDateInTimeUnits(runtime, adjustVal)); // validation
    }
  }
}
