package ucar.nc2.ft.grid.impl;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.grid.FmrcCS;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class FmrcCSImpl extends CoverageCSImpl implements FmrcCS {
  private CoordinateAxis1DTime[] timeAxisForRun;

  protected FmrcCSImpl(NetcdfDataset ds, CoordinateSystem cs, CoverageCSFactory fac) {
    super(ds, cs, fac);
  }

  @Override
  public CoordinateAxis1DTime getRunTimeAxis() {
    return (CoordinateAxis1DTime) cs.findAxis(AxisType.RunTime);
  }

  @Override
  public CoordinateAxis1DTime getTimeAxisForRun(CalendarDate runTime) {
    CoordinateAxis1DTime runTimeAxis = getRunTimeAxis();
    int runIndex = runTimeAxis.findTimeIndexFromCalendarDate(runTime);

    int nruns = (int) runTimeAxis.getSize();
    if ((runIndex < 0) || (runIndex >= nruns))
      throw new IllegalArgumentException("getTimeAxisForRun index out of bounds= " + runIndex);

    if (timeAxisForRun == null)
      timeAxisForRun = new CoordinateAxis1DTime[nruns];

    if (timeAxisForRun[runIndex] == null)
      timeAxisForRun[runIndex] = makeTimeAxisForRun(runIndex);

    return timeAxisForRun[runIndex];
  }

  private CoordinateAxis1DTime makeTimeAxisForRun(int run_index) {
    CoordinateAxis tAxis = getTimeAxis();
    VariableDS section;
    try {
      section = (VariableDS) tAxis.slice(0, run_index);
      return CoordinateAxis1DTime.factory(ds, section, null);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

}
