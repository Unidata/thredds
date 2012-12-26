package ucar.nc2.ft.grid.impl;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.grid.CoverageCS;
import ucar.nc2.ft.grid.Subset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.vertical.VerticalTransform;

import java.util.*;

/**
 * Coverage Coordinate System implementation.
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageCSImpl implements CoverageCS {
  protected NetcdfDataset ds;
  protected CoordinateSystem cs;
  protected CoverageCSFactory fac;

  protected CoverageCSImpl(NetcdfDataset ds, CoordinateSystem cs, CoverageCSFactory fac) {
    this.ds = ds;
    this.cs = cs;
    this.fac = fac;
  }

  @Override
  public String getName() {
    return cs.getName();
  }

  @Override
  public List<Dimension> getDomain() {
    return cs.getDomain();
  }

  @Override
  public List<CoordinateAxis> getCoordinateAxes() {
    return fac.standardAxes;
  }

  @Override
  public List<CoordinateAxis> getOtherCoordinateAxes() {
    return fac.otherAxes;
  }

  @Override
  public boolean isProductSet() {
    return cs.isProductSet();
  }

  @Override
  public List<CoordinateTransform> getCoordinateTransforms() {
    return cs.getCoordinateTransforms();
  }

  @Override
  public CoordinateAxis getXHorizAxis() {
    return cs.isLatLon() ? cs.getLonAxis() : cs.getXaxis();
  }

  @Override
  public CoordinateAxis getYHorizAxis() {
    return cs.isLatLon() ? cs.getLatAxis() : cs.getYaxis();
  }

  @Override
  public boolean isLatLon() {
    return cs.isLatLon();
  }

  @Override
  public LatLonRect getLatLonBoundingBox() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ProjectionRect getBoundingBox() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ProjectionCT getProjectionCT() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public ProjectionImpl getProjection() {
    return cs.getProjection();
  }

  @Override
  public Subset makeSubsetFromLatLonRect(LatLonRect llbb) throws InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public CoordinateAxis getVerticalAxis() {
    return fac.vertAxis;
  }

  @Override
  public boolean isZPositive() {
    CoordinateAxis vertZaxis = getVerticalAxis();
    if (vertZaxis == null) return false;
    if (vertZaxis.getPositive() != null) {
      return vertZaxis.getPositive().equalsIgnoreCase(ucar.nc2.constants.CF.POSITIVE_UP);
    }
    if (vertZaxis.getAxisType() == AxisType.Height) return true;
    if (vertZaxis.getAxisType() == AxisType.Pressure) return false;
    return true; // default
  }


  @Override
  public VerticalCT getVerticalCT() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public VerticalTransform getVerticalTransform() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean hasTimeAxis() {
    return getTimeAxis() != null;
  }

  @Override
  public CoordinateAxis getTimeAxis() {
    return cs.getTaxis();
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    if (!hasTimeAxis()) return null;

    CoordinateAxis timeAxis = getTimeAxis();
    if (timeAxis instanceof CoordinateAxis1DTime)
      return ((CoordinateAxis1DTime)timeAxis).getCalendarDateRange();

    // bail out for now
    return null;
  }


  @Override
  public String toString() {
    return fac.toString();
  }

  @Override
  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n%n", getName());
  }

}
