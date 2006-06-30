package ucar.nc2.dt.grid;

import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Feb 19, 2006
 * Time: 2:10:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ForecastGridCoordSys {

  public ProjectionRect getBoundingBox();
  public LatLonRect getLatLonBoundingBox();

  public ProjectionImpl getProjection();
  public VerticalTransform getVerticalTransform();

  public CoordinateAxis1D getVerticalAxis();
  public CoordinateAxis1D getTimeAxis();
  public CoordinateAxis getXHorizAxis(); // require 1D ??
  public CoordinateAxis getYHorizAxis(); // require 1D ??

  public ArrayList getLevels(); // NamedObject
  public ArrayList getTimes(); // NamedObject
  public String getLevelName(int index);
  public String getTimeName(int index);
  public boolean isDate();
  public Date[] getTimeDates();

  public int findTimeCoordElement(Date p0);
  public int[] findXYCoordElement(double xpos, double ypos, int[] result);

  //public boolean isGridCoordSys(StringBuffer p0, CoordinateSystem p1);
  //public boolean isLatLon();

  public boolean isZPositive();

}
