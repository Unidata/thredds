/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.IndexIterator;
import ucar.ma2.Array;

import java.util.*;
import java.io.IOException;

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
  private NetcdfDataset ds;
  private ArrayList<GeoGrid> grids = new ArrayList<GeoGrid>();
  private Map<String, Gridset> gridsetHash = new HashMap<String, Gridset>();

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
    return open(location, NetcdfDataset.getCoordSysEnhanceMode());
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
    return new GridDataset(ds);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ds underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @throws java.io.IOException on read error
   */
  public GridDataset(NetcdfDataset ds) throws IOException {
    this(ds, null);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ds underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @param parseInfo put parse info here, may be null
   * @throws java.io.IOException on read error
   */
  public GridDataset(NetcdfDataset ds, Formatter parseInfo) throws IOException {
    this.ds = ds;
    ds.enhance(EnumSet.of(NetcdfDataset.Enhance.CoordSystems));

    // look for geoGrids
    if (parseInfo != null) parseInfo.format("GridDataset look for GeoGrids\n");
    List<Variable> vars = ds.getVariables();
    for (Variable var : vars) {
      VariableEnhanced varDS = (VariableEnhanced) var;
      constructCoordinateSystems(ds, varDS, parseInfo);
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
  private DateRange dateRangeMax = null;

  private void makeRanges() {

    for (ucar.nc2.dt.GridDataset.Gridset gset : getGridsets()) {
      GridCoordSystem gcs = gset.getGeoCoordSystem();

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      if (llbbMax == null)
        llbbMax = llbb;
      else
        llbbMax.extend(llbb);

      DateRange dateRange = gcs.getDateRange();
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
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    return (title == null) ? getName() : title;
  }

  public String getDescription() {
    String desc = ds.findAttValueIgnoreCase(null, "description", null);
    if (desc == null)
      desc = ds.findAttValueIgnoreCase(null, "history", null);
    return (desc == null) ? getName() : desc;
  }

  public String getLocation() {
    return ds.getLocation();
  }

  public String getLocationURI() {
    return ds.getLocation();
  }

  public Date getStartDate() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getStart().getDate();
  }

  public Date getEndDate() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getEnd().getDate();
  }

  public LatLonRect getBoundingBox() {
    if (llbbMax == null) makeRanges();
    return llbbMax;
  }

  public void calcBounds() throws java.io.IOException {
    // not needed
  }

  public List<Attribute> getGlobalAttributes() {
    return ds.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ds.findGlobalAttributeIgnoreCase(name);
  }

  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> result = new ArrayList<VariableSimpleIF>( grids.size());
    for (GridDatatype grid : getGrids()) {
      if (grid.getVariable() != null) // LOOK could make Adaptor if no variable
        result.add( grid.getVariable());
    }
    return result;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    return ds.findTopVariable(shortName);
  }

  public NetcdfFile getNetcdfFile() {
    return ds;
  }

  private void addGeoGrid(VariableDS varDS, GridCoordSys gcs, Formatter parseInfo) {
    Gridset gridset;
    if (null == (gridset = gridsetHash.get(gcs.getName()))) {
      gridset = new Gridset(gcs);
      gridsetHash.put(gcs.getName(), gridset);
      if (parseInfo != null) parseInfo.format(" -make new GridCoordSys= %s\n",gcs.getName());
      gcs.makeVerticalTransform(this, parseInfo); // delayed until now
    }

    GeoGrid geogrid = new GeoGrid(this, varDS, gridset.gcc);
    grids.add(geogrid);
    gridset.add(geogrid);
  }

  /**
   * @return the name of the dataset
   */
  public String getName() {
    return ds.getLocation();
  }

  /**
   * @return the underlying NetcdfDataset
   */
  public NetcdfDataset getNetcdfDataset() {
    return ds;
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
   * @param name find this GeoGrid by name
   * @return the named GeoGrid, or null if not found
   */
  public GeoGrid findGridByName(String name) {
    for (GeoGrid ggi : grids) {
      if (name.equals(ggi.getName()))
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
    buff.format("\n\n----------------------------------------------------\n");
    NetcdfDatasetInfo info = null;
    try {
      info = new NetcdfDatasetInfo( ds.getLocation());
      buff.format("%s", info.getParseInfo());
    } catch (IOException e) {
      buff.format("NetcdfDatasetInfo failed");
    } finally {
      if (info != null) try { info.close(); } catch (IOException ee) {} // do nothing
    }
    buff.format("\n\n----------------------------------------------------\n");
    buff.format("%s", ds.toString());
    buff.format("\n\n----------------------------------------------------\n");
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

    buf.format("\nGeoReferencing Coordinate Axes\n");
    buf.format("Name__________________________Units_______________Type______Description\n");
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (axis.getAxisType() == null) continue;
      axis.getInfo(buf);
      buf.format("\n");
    }
  }

  /**
   * This is a set of GeoGrids with the same GeoCoordSys.
   */
  public class Gridset implements ucar.nc2.dt.GridDataset.Gridset {

    private GridCoordSys gcc;
    private List<GridDatatype> grids = new ArrayList<GridDatatype>();

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

  public DateRange getDateRange() {
    if (dateRangeMax == null) makeRanges();
    return dateRangeMax;
  }

  public String getImplementationName() {
    return ds.getConventionUsed();
  }

  //////////////////////////////////////////////////
  //  FileCacheable

  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else {
      try {
        if (ds != null) ds.close();
      } finally {
        ds = null;
      }
    }
  }

  public boolean sync() throws IOException {
    return (ds != null) ? ds.sync() : false;
  }

  protected FileCache fileCache;
  public void setFileCache(FileCache fileCache) {
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


  /////////////////////////////////////////////////////////////////////////////////////////////

  // test dataset handler
  static private ucar.nc2.dt.grid.GridDataset openGridDataset(String filename) throws IOException {

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = NetcdfDataset.acquireFile(filename, null);

    if (ncfile == null) return null;

    NetcdfDataset ncd;
    if (ncfile instanceof NetcdfDataset) {
      ncd = (NetcdfDataset) ncfile;
      //if (ncd.getEnhanceMode() == NetcdfDataset.EnhanceMode.None) // LOOK
      ncd.enhance();
    } else {
      ncd = new NetcdfDataset(ncfile, true);
    }

    // convert to a GridDataset
    return new ucar.nc2.dt.grid.GridDataset(ncd);
  }

  /**
   * testing
   */
  public static void main(String arg[]) {
    String defaultFilename = "R:/testdata/grid/netcdf/cf/mississippi.nc";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;
    try {
      GridDataset gridDs = openGridDataset(filename);
      //GridDataset gridDs = GridDataset.open(filename);
      //System.out.println(gridDs.getDetailInfo());

      String outFilename = "C:/data/writeGrid.nc";
      GeoGrid gg = gridDs.findGridByName("latent");
      assert gg != null;
      gg.writeFile(outFilename);

      gridDs = GridDataset.open(outFilename);
      System.out.println(gridDs.getDetailInfo());
    } catch (Exception ioe) {
      ioe.printStackTrace();
    }

  }

}