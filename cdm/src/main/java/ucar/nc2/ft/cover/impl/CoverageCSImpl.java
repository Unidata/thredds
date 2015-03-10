package ucar.nc2.ft.cover.impl;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.vertical.VerticalTransform;

import java.io.IOException;
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
  protected ProjectionImpl projection;
  protected ProjectionRect mapArea;

  protected CoverageCSImpl(NetcdfDataset ds, CoordinateSystem cs, CoverageCSFactory fac) {
    this.ds = ds;
    this.cs = cs;
    this.fac = fac;

    // set canonical area
    ProjectionImpl projOrig = cs.getProjection();
    if (projOrig != null) {
      projection = projOrig.constructCopy();
      projection.setDefaultMapArea(getBoundingBox());  // LOOK too expensive for 2D
    }
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
    if (mapArea == null) makeBoundingBox();
    return mapArea;
  }

  private void makeBoundingBox() {

    // x,y may be 2D
    if (!(getXHorizAxis() instanceof CoordinateAxis1D) || !(getYHorizAxis() instanceof CoordinateAxis1D)) {
      CoordinateAxis xaxis = getXHorizAxis();
      CoordinateAxis yaxis = getYHorizAxis();


      /*  could try to optimize this - just get cord=ners or something
      CoordinateAxis2D xaxis2 = (CoordinateAxis2D) horizXaxis;
      CoordinateAxis2D yaxis2 = (CoordinateAxis2D) horizYaxis;
      MAMath.MinMax
      */

      mapArea = new ProjectionRect(xaxis.getMinValue(), yaxis.getMinValue(),
              xaxis.getMaxValue(), yaxis.getMaxValue());

    } else {

      CoordinateAxis1D xaxis1 = (CoordinateAxis1D) getXHorizAxis();
      CoordinateAxis1D yaxis1 = (CoordinateAxis1D) getYHorizAxis();

      /* add one percent on each side if its a projection. WHY?
        double dx = 0.0, dy = 0.0;
        if (!isLatLon()) {
        dx = .01 * (xaxis1.getCoordEdge((int) xaxis1.getSize()) - xaxis1.getCoordEdge(0));
        dy = .01 * (yaxis1.getCoordEdge((int) yaxis1.getSize()) - yaxis1.getCoordEdge(0));
        }

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0) - dx, yaxis1.getCoordEdge(0) - dy,
          xaxis1.getCoordEdge((int) xaxis1.getSize()) + dx,
          yaxis1.getCoordEdge((int) yaxis1.getSize()) + dy); */

      mapArea = new ProjectionRect(xaxis1.getCoordEdge(0), yaxis1.getCoordEdge(0),
              xaxis1.getCoordEdge((int) xaxis1.getSize()),
              yaxis1.getCoordEdge((int) yaxis1.getSize()));
    }
  }

  @Override
  public ProjectionImpl getProjection() {
    return projection;
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
    return fac.timeAxis;
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    if (!hasTimeAxis()) return null;

    CoordinateAxis timeAxis = getTimeAxis();
    if (timeAxis instanceof CoordinateAxis1DTime)
      return ((CoordinateAxis1DTime) timeAxis).getCalendarDateRange();

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

  ///////////////////

  @Override
  public Subset makeSubsetFromLatLonRect(LatLonRect llbb) throws InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Subset getSubset() {
    return new SubsetImpl();
  }

  static class SubsetImpl implements Subset {
    int level = -1;
    int time = -1;

    @Override
    public void setLevel(int idx) {
      this.level = idx;
    }

    @Override
    public void setTime(int idx) {
      this.time = idx;
    }

    // kludge
    Array readData(VariableEnhanced ve) throws IOException, InvalidRangeException {
      int n = ve.getRank();
      int[] origin = new int[n];
      int[] shape = new int[n];
      System.arraycopy(ve.getShape(), 0, shape, 0, n);

      // assume canonical ordering
      if (level >= 0) {
      }

      Array result = ve.read(origin, shape);
      return result.reduce();
    }
  }

}
