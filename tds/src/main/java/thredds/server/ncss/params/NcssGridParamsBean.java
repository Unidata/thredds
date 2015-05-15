/* Copyright */
package thredds.server.ncss.params;

import thredds.server.ncss.validation.NcssGridRequestConstraint;
import ucar.nc2.ft2.coverage.grid.GridSubset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.ProjectionRect;

/**
 * Parameters specific to ncss grid
 *
 * @author caron
 * @since 4/29/2015
 */
@NcssGridRequestConstraint
public class NcssGridParamsBean extends NcssParamsBean {

    //// projection rectangle
  private Double minx;

 	private Double maxx;

 	private Double miny;

 	private Double maxy;

 	private boolean addLatLon;

 	private Integer horizStride = 1;

 	private Integer timeStride = 1;

  private Double vertCoord;

  public Double getMinx() {
    return minx;
  }

  public void setMinx(Double minx) {
    this.minx = minx;
  }

  public Double getMaxx() {
    return maxx;
  }

  public void setMaxx(Double maxx) {
    this.maxx = maxx;
  }

  public Double getMiny() {
    return miny;
  }

  public void setMiny(Double miny) {
    this.miny = miny;
  }

  public Double getMaxy() {
    return maxy;
  }

  public void setMaxy(Double maxy) {
    this.maxy = maxy;
  }

  public boolean isAddLatLon() {
    return addLatLon;
  }

  public void setAddLatLon(boolean addLatLon) {
    this.addLatLon = addLatLon;
  }

  public Integer getHorizStride() {
    return horizStride;
  }

  public void setHorizStride(Integer horizStride) {
    this.horizStride = horizStride;
  }

  public Integer getTimeStride() {
    return timeStride;
  }

  public void setTimeStride(Integer timeStride) {
    this.timeStride = timeStride;
  }

  public boolean hasProjectionBB() { // need to validate
    return minx != null && miny != null && maxx != null && maxy != null;
  }

  public ProjectionRect getProjectionBB() {
    return new ProjectionRect(minx, miny, maxx, maxy);
  }

  public Double getVertCoord() {
    return vertCoord;
  }

  public void setVertCoord(Double vertCoord) {
    this.vertCoord = vertCoord;
  }

  public GridSubset makeSubset(Calendar cal) {
    // construct the subset
    GridSubset subset = new GridSubset();
    if (hasProjectionBB())
      subset.set(GridSubset.projBB, getProjectionBB());
    else if (hasLatLonBB())
      subset.set(GridSubset.latlonBB, getLatLonBoundingBox());
    if (horizStride != null)
      subset.set(GridSubset.horizStride, horizStride);

    if (vertCoord != null)
      subset.set(GridSubset.vertCoord, vertCoord);

    CalendarDate date = getRequestedDate(cal);
    CalendarDateRange dateRange = getCalendarDateRange(cal);
    if (isAllTimes()) {
      subset.set(GridSubset.allTimes, true);
      if (timeStride != null)
        subset.set(GridSubset.timeStride, timeStride);

    } else if (date != null) {
      subset.set(GridSubset.date, date);
      if (timeWindow != null)
        subset.set(GridSubset.timeWindow, timeWindow);

    } else if (dateRange != null) {
      subset.set(GridSubset.dateRange, dateRange);
      if (timeStride != null)
        subset.set(GridSubset.latlonBB, timeStride);

    } else {
      subset.set(GridSubset.latestTime, true);
    }

    return subset;
  }

}
