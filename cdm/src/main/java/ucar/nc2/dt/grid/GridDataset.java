/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt.grid;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

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

public class GridDataset implements ucar.nc2.dt.GridDataset {
  private NetcdfDataset ds;
  private ArrayList<GeoGrid> grids = new ArrayList<GeoGrid>();
  private Map<String, Gridset> gridsetHash = new HashMap<String, Gridset>();

  /**
   * Open a netcdf dataset, parse Conventions, find all the geoGrids, return a GridDataset.
   *
   * @param netcdfFileURI netcdf dataset to open. May have a dods:, http: or file: prefix,
   *                      or just a local filename. If it ends with ".xml", its assumed to be a NetcdfDataset Definition XML file
   * @return GridDataset
   * @throws java.io.IOException on read error
   * @see ucar.nc2.dataset.NetcdfDataset#open
   */
  static public GridDataset open(String netcdfFileURI) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(netcdfFileURI, null);
    return new GridDataset(ds);
  }

  /**
   * Create a GridDataset from a NetcdfDataset.
   *
   * @param ds underlying NetcdfDataset.
   */
  public GridDataset(NetcdfDataset ds) {
    this.ds = ds;

    // look for geoGrids
    parseInfo.append("GridDataset look for GeoGrids\n");
    List<Variable> vars = ds.getVariables();
    for (Variable var : vars) {
      VariableEnhanced varDS = (VariableEnhanced) var;
      constructCoordinateSystems(ds, varDS);
    }

  }

  private void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v) {

    if (v instanceof StructureDS) {
      StructureDS s = (StructureDS) v;
      List<Variable> members = s.getVariables();
      for (Variable nested : members) {
        // LOOK flatten here ??
        constructCoordinateSystems(ds, (VariableEnhanced) nested);
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
        addGeoGrid((VariableDS) v, gcs);
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

  public List<Attribute> getGlobalAttributes() {
    return ds.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ds.findGlobalAttributeIgnoreCase(name);
  }

  public List<VariableSimpleIF> getDataVariables() {
    return new ArrayList<VariableSimpleIF>(ds.getVariables());
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    return ds.findTopVariable(shortName);
  }

  public NetcdfFile getNetcdfFile() {
    return ds;
  }

  /**
   * Close all resources associated with this dataset.
   */
  public void close() throws java.io.IOException {
    ds.close();
  }

  private void addGeoGrid(VariableDS varDS, GridCoordSys gcs) {
    Gridset gridset;
    if (null == (gridset = gridsetHash.get(gcs.getName()))) {
      gridset = new Gridset(gcs);
      gridsetHash.put(gcs.getName(), gridset);
      parseInfo.append(" -make new GridCoordSys= ").append(gcs.getName()).append("\n");
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
   * Show Grids and coordinate systems.
   *
   * @return info about this GridDataset
   */
  public String getInfo() {
    StringBuilder buf = new StringBuilder(20000);
    int countGridset = 0;
    buf.setLength(0);

    for (Gridset gs : gridsetHash.values()) {
      GridCoordSystem gcs = gs.getGeoCoordSystem();
      buf.append("\nGridset ").append(countGridset).append(" coordSys=").append(gcs);
      buf.append(" LLbb=").append(gcs.getLatLonBoundingBox());
      if ((gcs.getProjection() != null)  && !gcs.getProjection().isLatLon())
        buf.append(" bb=").append(gcs.getBoundingBox());
      buf.append("\n");
      buf.append("Name___________Unit___________hasMissing_____Description\n");
      for (GeoGrid grid : grids) {
        buf.append(grid.getInfo());
        buf.append("\n");
      }
      countGridset++;
      buf.append("\n");
    }

    buf.append("\nGeoReferencing Coordinate Axes\n");
    buf.append("Name___________Len__Unit________________Type___Description\n");
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (axis.getAxisType() == null) continue;
      buf.append(axis.getInfo());
      buf.append("\n");
    }
    return buf.toString();
  }

  private StringBuilder parseInfo = new StringBuilder(); // debugging

  public StringBuilder getParseInfo() {
    return parseInfo;
  }

  /**
   * Get Details about the dataset.
   */
  public String getDetailInfo() {
    StringBuilder buff = new StringBuilder(5000);
    buff.append(getInfo());
    buff.append("\n\n----------------------------------------------------\n");
    NetcdfDatasetInfo info = null;
    try {
      info = new NetcdfDatasetInfo( ds.getLocation());
      buff.append(info.getParseInfo());
    } catch (IOException e) {
      buff.append("NetcdfDatasetInfo failed");
    } finally {
      if (info != null) try { info.close(); } catch (IOException ee) {} // do nothing      
    }
    buff.append("\n\n----------------------------------------------------\n");
    buff.append(parseInfo.toString());
    buff.append(ds.toString());
    buff.append("\n\n----------------------------------------------------\n");

    return buff.toString();
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

    // convert to NetcdfDataset with enhance
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