/* Copyright */
package ucar.nc2.ft2.coverage.grid.adapter;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 5/26/2015
 */
public class GeoGridDataset {
  
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
  static public GeoGridDataset open(String location) throws java.io.IOException {
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
  static public GeoGridDataset open(String location, Set<NetcdfDataset.Enhance> enhanceMode) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(null, location, enhanceMode, -1, null, null);
    return new GeoGridDataset(ds, null);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ncd underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @throws java.io.IOException on read error
   */
  public GeoGridDataset(NetcdfDataset ncd) throws IOException {
    this(ncd, null);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ncd underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @param parseInfo put parse info here, may be null
   * @throws java.io.IOException on read error
   */
  public GeoGridDataset(NetcdfDataset ncd, Formatter parseInfo) throws IOException {
    this.ncd = ncd;
    // ds.enhance(EnumSet.of(NetcdfDataset.Enhance.CoordSystems));
    Set<NetcdfDataset.Enhance> enhance = ncd.getEnhanceMode();
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
      GeoGridCoordSys gcs = null;
      List<CoordinateSystem> csys = v.getCoordinateSystems();
      for (CoordinateSystem cs : csys) {
        GeoGridCoordSys gcsTry = GeoGridCoordSys.makeGridCoordSys(parseInfo, cs, v);
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
  private ProjectionRect projBB = null;

  private void makeRanges() {

    for (Gridset gset : getGridsets()) {
      GeoGridCoordSys gcs = gset.getGeoCoordSystem();

      ProjectionRect bb = gcs.getBoundingBox();
      if (projBB == null)
        projBB = bb;
      else
        projBB.add(bb);

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

  public ProjectionRect getProjBoundingBox() {
    if (llbbMax == null) makeRanges();
    return projBB;
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
    for (GeoGrid grid : getGrids()) {
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

  private void addGeoGrid(VariableDS varDS, GeoGridCoordSys gcs, Formatter parseInfo) {
    Gridset gridset;
    if (null == (gridset = gridsetHash.get(gcs.getName()))) {
      gridset = new Gridset(gcs);
      gridsetHash.put(gcs.getName(), gridset);
      if (parseInfo != null) parseInfo.format(" -make new GeoGridCoordSys= %s%n",gcs.getName());
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
  public List<GeoGrid> getGrids() {
    return new ArrayList<>(grids);
  }

  public GeoGrid findGridDatatype(String name) {
    return findGridByName(name);
  }

  /**
   * Return GeoGrid objects grouped by GeoGridCoordSys. All GeoGrid in a Gridset
   * have the same GeoGridCoordSys.
   *
   * @return List of type ucar.nc2.dt.GridDataset.Gridset
   */
  public List<Gridset> getGridsets() {
    return new ArrayList<>(gridsetHash.values());
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
      GeoGridCoordSys gcs = gs.getGeoCoordSystem();
      buf.format("%nGridset %d  coordSys=%s", countGridset,  gcs);
      buf.format(" LLbb=%s ", gcs.getLatLonBoundingBox());
      if ((gcs.getProjection() != null)  && !gcs.getProjection().isLatLon())
        buf.format(" bb= %s", gcs.getBoundingBox());
      buf.format("%n");
      buf.format("Name__________________________Unit__________________________hasMissing_Description%n");
      for (GeoGrid grid : gs.getGrids()) {
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
  public static class Gridset {

    private GeoGridCoordSys gcc;
    private List<GeoGrid> grids = new ArrayList<>();

    private Gridset(GeoGridCoordSys gcc) {
      this.gcc = gcc;
    }

    private void add(GeoGrid grid) {
      grids.add(grid);
    }

    /**
     * Get list of GeoGrid objects
     */
    public List<GeoGrid> getGrids() {
      return grids;
    }

    /**
     * all GeoGrid point to this GeoGridCoordSys
     */
    public GeoGridCoordSys getGeoCoordSystem() {
      return gcc;
    }

    /**
     * all GeoGrids point to this GeoCoordSysImpl.
     *
     * @deprecated use getGeoCoordSystem() if possible.
     */
    public GeoGridCoordSys getGeoCoordSys() {
      return gcc;
    }
  }

  public synchronized void close() throws java.io.IOException {

    try {
      if (ncd != null) ncd.close();
    } finally {
      ncd = null;
      }
  }
}
