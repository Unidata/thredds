/* Copyright */
package thredds.server.ncss.params;

import thredds.server.ncss.validation.NcssGridRequestConstraint;
import ucar.nc2.ft2.coverage.SubsetParams;
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

  public SubsetParams makeSubset(Calendar cal) {
    // construct the subset
    SubsetParams subset = new SubsetParams();
    if (hasProjectionBB())
      subset.set(SubsetParams.projBB, getProjectionBB());
    else if (hasLatLonBB())
      subset.set(SubsetParams.latlonBB, getLatLonBoundingBox());
    if (horizStride != null)
      subset.set(SubsetParams.horizStride, horizStride);

    if (vertCoord != null)
      subset.set(SubsetParams.vertCoord, vertCoord);

    CalendarDate date = getRequestedDate(cal);
    CalendarDateRange dateRange = getCalendarDateRange(cal);
    if (isAllTimes()) {
      subset.set(SubsetParams.allTimes, true);
      if (timeStride != null)
        subset.set(SubsetParams.timeStride, timeStride);

    } else if (date != null) {
      subset.set(SubsetParams.time, date);
      if (timeWindow != null)
        subset.set(SubsetParams.timeWindow, timeWindow);

    } else if (dateRange != null) {
      subset.set(SubsetParams.timeRange, dateRange);
      if (timeStride != null)
        subset.set(SubsetParams.timeStride, timeStride);

    } else {
      subset.set(SubsetParams.latestTime, true);
    }

    return subset;
  }

}
