package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.InvalidRangeException;

import java.util.ArrayList;
import java.util.List;

/**
 * A TimeOffsetAxis and its associated runtime axis.
 * These have to be subset together.
 * Each CoordSys has 0 or 1, there may be many in a Dataset.
 *
 * @author John
 * @since 8/14/2015
 */
@Immutable
public class Time2DCoordSys {
  public final CoverageCoordAxis1D runAxis;
  public final TimeOffsetAxis timeOffset;

  public Time2DCoordSys(TimeOffsetAxis timeOffset) {
    this.runAxis = timeOffset.getRunAxis();
    this.timeOffset = timeOffset;
  }

  public List<CoverageCoordAxis> getCoordAxes() throws InvalidRangeException {
    List<CoverageCoordAxis> result = new ArrayList<>();
    result.add(runAxis);
    result.add(timeOffset);
    return result;
  }

  public Time2DCoordSys subset(SubsetParams params) {
    CoverageCoordAxis1D runAxisSubset = (CoverageCoordAxis1D) runAxis.subset(params);
    TimeOffsetAxis timeOffsetSubset = (TimeOffsetAxis) timeOffset.subset(params, runAxisSubset);
    timeOffsetSubset.setRunAxis(runAxisSubset);
    return new Time2DCoordSys( timeOffsetSubset);
  }

}
