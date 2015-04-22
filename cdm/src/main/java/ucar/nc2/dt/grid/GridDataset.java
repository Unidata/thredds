/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Make a NetcdfDataset into a collection of GeoGrids with Georeferencing coordinate systems.
 * <p/>
 * <p/>
 * A variable will be made into a GeoGrid if it has a Georeferencing coordinate system,
 * using GridCoordSys.isGridCoordSys(), and it has no extra dimensions, ie
 * GridCoordSys.isComplete( var) is true.
 * If it has multiple Georeferencing coordinate systems, any one that is a product set will be given preference.
 * <p/>
 * Example:
 * <p/>
 * <pre>
 * GridDataset gridDs = GridDataset.open (uriString);
 * List grids = gridDs.getGrids();
 * for (int i=0; i&lt;grids.size(); i++) {
 *   GeoGrid grid = (Geogrid) grids.get(i);
 * }
 * </pre>
 *
 * @author caron
 */

public class GridDataset implements ucar.nc2.dt.GridDataset, ucar.nc2.ft.FeatureDataset {
  private NetcdfDataset ncd;
  private ArrayList<GeoGrid> grids = new ArrayList<>();
  private Map<String, Gridset> gridsetHash = new HashMap<>();

  /**
   * Open a netcdf dataset, using NetcdfDataset.defaultEnhanceMode plus CoordSystems
   * and turn into a GridDataset.
   *
   * @param location netcdf dataset to open, using NetcdfDataset.acquireDataset().
   * @return GridDataset
   * @throws java.io.IOException on read error
   * @see ucar.nc2.dataset.NetcdfDataset#acquireDataset
   */
  static public GridDataset open(String location) throws java.io.IOException {
    return open(location, NetcdfDataset.getDefaultEnhanceMode());
  }

  /**
   * Open a netcdf dataset, using NetcdfDataset.defaultEnhanceMode plus CoordSystems
   * and turn into a GridDataset.
   *
   * @param location netcdf dataset to open, using NetcdfDataset.acquireDataset().
   * @param enhanceMode open netcdf dataset with this enhanceMode
   * @return GridDataset
   * @throws java.io.IOException on read error
   * @see ucar.nc2.dataset.NetcdfDataset#acquireDataset
   */
  static public GridDataset open(String location, Set<NetcdfDataset.Enhance> enhanceMode) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(null, location, enhanceMode, -1, null, null);
    return new GridDataset(ds, null);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ncd underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @throws java.io.IOException on read error
   */
  public GridDataset(NetcdfDataset ncd) throws IOException {
    this(ncd, null);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ncd underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @param parseInfo put parse info here, may be null
   * @throws java.io.IOException on read error
   */
  public GridDataset(NetcdfDataset ncd, Formatter parseInfo) throws IOException {
    this.ncd = ncd;
    // ds.enhance(EnumSet.of(NetcdfDataset.Enhance.CoordSystems));
    Set<Enhance> enhance = ncd.getEnhanceMode();
    if(enhance == null || enhance.isEmpty()) enhance = NetcdfDataset.getDefaultEnhanceMode(); 
    ncd.enhance(enhance);

    // look for geoGrids
    if (parseInfo != null) parseInfo.format("GridDataset look for GeoGrids%n");
    List<Variable> vars = ncd.getVariables();
    for (Variable var : vars) {
      VariableEnhanced varDS = (VariableEnhanced) var;
      constructCoordinateSystems(ncd, varDS, parseInfo);
    }
  }

  private void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v, Formatter parseInfo) {

    if (v instanceof StructureDS) {
      StructureDS s = (StructureDS) v;
      List<Variable> members = s.getVariables();
      for (Variable nested : members) {
        // LOOK flatten here ??
        constructCoordinateSystems(ds, (VariableEnhanced) nested, parseInfo);
      }
    } else {

      // see if it has a GridCS
      // LOOK: should add geogrid it multiple times if there are multiple geoCS ??
      GridCoordSys gcs = null;
      List<CoordinateSystem> csys = v.getCoordinateSystems();
      for (CoordinateSystem cs : csys) {
        GridCoordSys gcsTry = GridCoordSys.makeGridCoordSys(parseInfo, cs, v);
        if (gcsTry != null) {
          gcs = gcsTry;
          if (gcsTry.isProductSet()) break;
        }
      }

      if (gcs != null)
        addGeoGrid((VariableDS) v, gcs, parseInfo);
    }

  }

  private LatLonRect llbbMax = null;
  private CalendarDateRange dateRangeMax = null;

  private void makeRanges() {

    for (ucar.nc2.dt.GridDataset.Gridset gset : getGridsets()) {
      GridCoordSystem gcs = gset.getGeoCoordSystem();

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      if (llbbMax == null)
        llbbMax = llbb;
      else
        llbbMax.extend(llbb);

      CalendarDateRange dateRange = gcs.getCalendarDateRange();
      if (dateRange != null) {
        if (dateRangeMax == null)
          dateRangeMax = dateRange;
        else
          dateRangeMax.extend(dateRange);
      }
    }
  }

  // stuff to satisfy ucar.nc2.dt.TypedDataset
  public String getTitle() {
    String title = ncd.getTitle();
    if (title == null)
      title = ncd.findAttValueIgnoreCase(null, CDM.TITLE, null);
    if (title == null)
      title = getName();
    return title;
  }

  public String getDescription() {
    String desc = ncd.findAttValueIgnoreCase(null, "description", null);
    if (desc == null)
      desc = ncd.findAttValueIgnoreCase(null, CDM.HISTORY, null);
    return (desc == null) ? getName() : desc;
  }

  public String getLocation() {
    return ncd.getLocation();
  }

  /**
   * @deprecated use getCalendarDateRange
   */
  public DateRange getDateRange() {
    CalendarDateRange cdr = getCalendarDateRange();
    return (cdr != null) ? cdr.toDateRange() : null;
  }

  /**
   * @deprecated use getStartCalendarDate
   */
  public Date getStartDate() {
    DateRange dr = getDateRange();
    return (dr != null) ? dr.getStart().getDate() : null;
  }

  /**
   * @deprecated use getEndCalendarDate
   */
  public Date getEndDate() {
    DateRange dr = getDateRange();
    return (dr != null) ? dr.getEnd().getDate() : null;
  }

  public CalendarDateRange getCalendarDateRange() {
    if (dateRangeMax == null) makeRanges();
    return dateRangeMax;
  }

  public CalendarDate getCalendarDateStart() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getStart();
  }

  public CalendarDate getCalendarDateEnd() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getEnd();
  }

  public LatLonRect getBoundingBox() {
    if (llbbMax == null) makeRanges();
    return llbbMax;
  }

  public void calcBounds() throws java.io.IOException {
    // not needed
  }

  public List<Attribute> getGlobalAttributes() {
    return ncd.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ncd.findGlobalAttributeIgnoreCase(name);
  }

  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> result = new ArrayList<>( grids.size());
    for (GridDatatype grid : getGrids()) {
      if (grid.getVariable() != null) // LOOK could make Adaptor if no variable
        result.add( grid.getVariable());
    }
    return result;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    return ncd.getRootGroup().findVariable(shortName);
  }

  public NetcdfFile getNetcdfFile() {
    return ncd;
  }

  private void addGeoGrid(VariableDS varDS, GridCoordSys gcs, Formatter parseInfo) {
    Gridset gridset;
    if (null == (gridset = gridsetHash.get(gcs.getName()))) {
      gridset = new Gridset(gcs);
      gridsetHash.put(gcs.getName(), gridset);
      if (parseInfo != null) parseInfo.format(" -make new GridCoordSys= %s%n",gcs.getName());
      gcs.makeVerticalTransform(this, parseInfo); // delayed until now LOOK why for each grid ??
    }

    GeoGrid geogrid = new GeoGrid(this, varDS, gridset.gcc);
    grids.add(geogrid);
    gridset.add(geogrid);
  }

  /**
   * the name of the dataset is the last part of the location
   * @return the name of the dataset
   */
  public String getName() {
    String loc = ncd.getLocation();
    int pos = loc.lastIndexOf('/');
    if (pos < 0)
      pos = loc.lastIndexOf('\\');
    return (pos < 0) ? loc : loc.substring(pos+1);
  }

  /**
   * @return the underlying NetcdfDataset
   */
  public NetcdfDataset getNetcdfDataset() {
    return ncd;
  }

  /**
   * @return the list of GeoGrid objects contained in this dataset.
   */
  public List<GridDatatype> getGrids() {
    return new ArrayList<GridDatatype>(grids);
  }

  public GridDatatype findGridDatatype(String name) {
    return findGridByName(name);
  }

  /**
   * Return GridDatatype objects grouped by GridCoordSys. All GridDatatype in a Gridset
   * have the same GridCoordSystem.
   *
   * @return List of type ucar.nc2.dt.GridDataset.Gridset
   */
  public List<ucar.nc2.dt.GridDataset.Gridset> getGridsets() {
    return new ArrayList<ucar.nc2.dt.GridDataset.Gridset>(gridsetHash.values());
  }

  /**
   * find the named GeoGrid.
   *
   * @param fullName find this GeoGrid by full name
   * @return the named GeoGrid, or null if not found
   */
  public GeoGrid findGridByName(String fullName) {
    for (GeoGrid ggi : grids) {
      if (fullName.equals(ggi.getFullName()))
        return ggi;
    }
    return null;
  }
  
  /**
   * find the named GeoGrid.
   *
   * @param shortName find this GeoGrid by short name
   * @return the named GeoGrid, or null if not found
   */
  public GeoGrid findGridByShortName(String shortName) {
    for (GeoGrid ggi : grids) {
      if (shortName.equals(ggi.getShortName()))
        return ggi;
    }
    return null;
  }

  public GeoGrid findGridDatatypeByAttribute(String attName, String attValue) {
    for (GeoGrid ggi : grids) {
      for (Attribute att : ggi.getAttributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue()))
          return ggi;
    }
    return null;
  }
  
  /**
   * Get Details about the dataset.
   */
  public String getDetailInfo() {
    Formatter buff = new Formatter();
    getDetailInfo(buff);
    return buff.toString();
  }

  public void getDetailInfo(Formatter buff) {
    getInfo(buff);
    buff.format("%n%n----------------------------------------------------%n");
    try (NetcdfDatasetInfo info = new NetcdfDatasetInfo(ncd)) {
      buff.format("%s", info.getParseInfo());
    } catch (IOException e) {
      buff.format("NetcdfDatasetInfo failed");
    }
    buff.format("%n%n----------------------------------------------------%n");
    buff.format("%s", ncd.toString());
    buff.format("%n%n----------------------------------------------------%n");
  }

  /**
   * Show Grids and coordinate systems.
   * @param buf put info here
   */
  private void getInfo(Formatter buf) {
    int countGridset = 0;

    for (Gridset gs : gridsetHash.values()) {
      GridCoordSystem gcs = gs.getGeoCoordSystem();
      buf.format("%nGridset %d  coordSys=%s", countGridset,  gcs);
      buf.format(" LLbb=%s ", gcs.getLatLonBoundingBox());
      if ((gcs.getProjection() != null)  && !gcs.getProjection().isLatLon())
        buf.format(" bb= %s", gcs.getBoundingBox());
      buf.format("%n");
      buf.format("Name__________________________Unit__________________________hasMissing_Description%n");
      for (GridDatatype grid : gs.getGrids()) {
        buf.format("%s%n", grid.getInfo());
      }
      countGridset++;
      buf.format("%n");
    }

    buf.format("%nGeoReferencing Coordinate Axes%n");
    buf.format("Name__________________________Units_______________Type______Description%n");
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      if (axis.getAxisType() == null) continue;
      axis.getInfo(buf);
      buf.format("%n");
    }
  }

  /**
   * This is a set of GeoGrids with the same GeoCoordSys.
   */
  public static class Gridset implements ucar.nc2.dt.GridDataset.Gridset {

    private GridCoordSys gcc;
    private List<GridDatatype> grids = new ArrayList<>();

    private Gridset(GridCoordSys gcc) {
      this.gcc = gcc;
    }

    private void add(GeoGrid grid) {
      grids.add(grid);
    }

    /**
     * Get list of GeoGrid objects
     */
    public List<GridDatatype> getGrids() {
      return grids;
    }

    /**
     * all GridDatatype point to this GridCoordSystem
     */
    public GridCoordSystem getGeoCoordSystem() {
      return gcc;
    }

    /**
     * all GeoGrids point to this GeoCoordSysImpl.
     *
     * @deprecated use getGeoCoordSystem() if possible.
     */
    public GridCoordSys getGeoCoordSys() {
      return gcc;
    }

  }

  ////////////////////////////
  // for ucar.nc2.ft.FeatureDataset

  public FeatureType getFeatureType() {
    return FeatureType.GRID;
  }

  public String getImplementationName() {
    return ncd.getConventionUsed();
  }

  //////////////////////////////////////////////////
  //  FileCacheable

  @Override
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this)) return;
    }

    try {
      if (ncd != null) ncd.close();
    } finally {
      ncd = null;
      }
  }

        // release any resources like file handles
  public void release() throws IOException {
    if (ncd != null) ncd.release();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    if (ncd != null) ncd.reacquire();
  }

  @Override
  public long getLastModified() {
    return (ncd != null) ? ncd.getLastModified() : 0;
  }

  protected FileCacheIF fileCache;

  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }


  /////////////////////////////
  // deprecated


  /**
   * Open a netcdf dataset, parse Conventions, find all the geoGrids, return a GridDataset.
   *
   * @deprecated : use GridDataset.open().
   */
  static public GridDataset factory(String netcdfFileURI) throws java.io.IOException {
    return open(netcdfFileURI);
  }

}