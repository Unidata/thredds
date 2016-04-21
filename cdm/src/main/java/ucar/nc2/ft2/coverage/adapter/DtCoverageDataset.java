/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft2.coverage.adapter;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

/**
 * fork ucar.nc2.dt.grid for adaption to Coverage (this class can evolve as needed and not worry about backwards compatibility).
 * uses NetcdfDataset / CoordinateSystem.
 *
 * @see DtCoverageAdapter
 * @author caron
 * @since 5/26/2015
 */
public class DtCoverageDataset implements Closeable {

  /**
   * Open a netcdf dataset, using NetcdfDataset.defaultEnhanceMode plus CoordSystems
   * and turn into a DtCoverageDataset.
   *
   * @param location netcdf dataset to open, using NetcdfDataset.acquireDataset().
   * @return GridDataset
   * @throws java.io.IOException on read error
   * @see ucar.nc2.dataset.NetcdfDataset#acquireDataset
   */
  static public DtCoverageDataset open(String location) throws java.io.IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return open(durl, NetcdfDataset.getDefaultEnhanceMode());
  }

  static public DtCoverageDataset open(DatasetUrl durl) throws java.io.IOException {
    return open(durl, NetcdfDataset.getDefaultEnhanceMode());
  }

  /**
   * Open a netcdf dataset, using NetcdfDataset.defaultEnhanceMode plus CoordSystems
   * and turn into a DtCoverageDataset.
   *
   * @param durl    netcdf dataset to open, using NetcdfDataset.acquireDataset().
   * @param enhanceMode open netcdf dataset with this enhanceMode
   * @return GridDataset
   * @throws java.io.IOException on read error
   * @see ucar.nc2.dataset.NetcdfDataset#acquireDataset
   */
  static public DtCoverageDataset open(DatasetUrl durl, Set<NetcdfDataset.Enhance> enhanceMode) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(null, durl, enhanceMode, -1, null, null);
    return new DtCoverageDataset(ds, null);
  }

  static public DtCoverageDataset open(NetcdfDataset ds) throws java.io.IOException {
    return new DtCoverageDataset(ds, null);
  }

  ////////////////////////////////////////

  private NetcdfDataset ncd;
  private FeatureType coverageType;

  private ArrayList<DtCoverage> grids = new ArrayList<>();
  private Map<String, Gridset> gridsetHash = new HashMap<>();
  private List<Gridset> gridSets = new ArrayList<>();

  private LatLonRect llbbMax = null;
  private CalendarDateRange dateRangeMax = null;
  private ProjectionRect projBB = null;

  /**
   * Create a DtCoverageDataset from a NetcdfDataset.
   *
   * @param ncd underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @throws java.io.IOException on read error
   */
  public DtCoverageDataset(NetcdfDataset ncd) throws IOException {
    this(ncd, null);
  }

  /**
   * Create a DtCoverageDataset from a NetcdfDataset.
   *
   * @param ncd       underlying NetcdfDataset, will do Enhance.CoordSystems if not already done.
   * @param parseInfo put parse info here, may be null
   * @throws java.io.IOException on read error
   */
  public DtCoverageDataset(NetcdfDataset ncd, Formatter parseInfo) throws IOException {
    this.ncd = ncd;

    // ds.enhance(EnumSet.of(NetcdfDataset.Enhance.CoordSystems));
    Set<NetcdfDataset.Enhance> enhance = ncd.getEnhanceMode();
    if (enhance == null || enhance.isEmpty()) enhance = NetcdfDataset.getDefaultEnhanceMode();
    ncd.enhance(enhance);

    DtCoverageCSBuilder facc = DtCoverageCSBuilder.classify(ncd, parseInfo);
    if (facc != null)
      this.coverageType = facc.type;

    Map<String, Gridset> csHash = new HashMap<>();
    for (CoordinateSystem cs : ncd.getCoordinateSystems()) {
      DtCoverageCSBuilder fac = new DtCoverageCSBuilder(ncd, cs, parseInfo);
      if (fac.type == null) continue;
      DtCoverageCS ccs = fac.makeCoordSys();
      if (ccs == null) continue;
      Gridset cset = new Gridset(ccs);
      gridSets.add(cset);
      gridsetHash.put(ccs.getName(), cset);
      csHash.put(cs.getName(), cset);
    }

    for (Variable v : ncd.getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      List<CoordinateSystem> css = ve.getCoordinateSystems();
      if (css.size() == 0) continue;
      Collections.sort(css, (o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
      CoordinateSystem cs = css.get(0);    // the largest one
      Gridset cset = csHash.get(cs.getName());
      if (cset == null) continue;
      DtCoverage ci = new DtCoverage(this, cset.gcc, (VariableDS) ve);
      cset.grids.add(ci);
      grids.add(ci);
    }
  }

  private void makeHorizRanges() {

    for (Gridset gset : getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();

      ProjectionRect bb = gcs.getBoundingBox();
      if (projBB == null)
        projBB = bb;
      else if (bb != null)
        projBB.add(bb);

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      if (llbbMax == null)
        llbbMax = llbb;
      else if (llbb != null)
        llbbMax.extend(llbb);
    }
  }

  private void makeTimeRanges() {

    for (Gridset gset : getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      CalendarDateRange dateRange = gcs.getCalendarDateRange();
      if (dateRange != null) {
        if (dateRangeMax == null)
          dateRangeMax = dateRange;
        else
          dateRangeMax = dateRangeMax.extend(dateRange);
      }
    }
  }

  public FeatureType getCoverageType() {
    return coverageType;
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

  public CalendarDateRange getCalendarDateRange() {
    if (dateRangeMax == null) makeTimeRanges();
    return dateRangeMax;
  }

  public CalendarDate getCalendarDateStart() {
    if (dateRangeMax == null) makeTimeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getStart();
  }

  public CalendarDate getCalendarDateEnd() {
    if (dateRangeMax == null) makeTimeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getEnd();
  }

  public LatLonRect getBoundingBox() {
    if (llbbMax == null) makeHorizRanges();
    return llbbMax;
  }

  public ProjectionRect getProjBoundingBox() {
    if (llbbMax == null) makeHorizRanges();
    return projBB;
  }

  public void calcBounds() throws java.io.IOException {
    // not needed
  }

  public List<Attribute> getGlobalAttributes() {
    return ncd.getGlobalAttributes();
  }

  public Dimension findDimension(String name) {
    return ncd.findDimension(name);
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ncd.findGlobalAttributeIgnoreCase(name);
  }

  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> result = new ArrayList<>(grids.size());
    for (DtCoverage grid : getGrids()) {
      if (grid.getVariable() != null) // LOOK could make Adaptor if no variable
        result.add(grid.getVariable());
    }
    return result;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    return ncd.getRootGroup().findVariable(shortName);
  }

  public NetcdfFile getNetcdfFile() {
    return ncd;
  }

  /**
   * the name of the dataset is the last part of the location
   *
   * @return the name of the dataset
   */
  public String getName() {
    String loc = ncd.getLocation();
    int pos = loc.lastIndexOf('/');
    if (pos < 0)
      pos = loc.lastIndexOf('\\');
    return (pos < 0) ? loc : loc.substring(pos + 1);
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
  public List<DtCoverage> getGrids() {
    return new ArrayList<>(grids);
  }

  public DtCoverage findGridDatatype(String name) {
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
  public DtCoverage findGridByName(String fullName) {
    for (DtCoverage ggi : grids) {
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
  public DtCoverage findGridByShortName(String shortName) {
    for (DtCoverage ggi : grids) {
      if (shortName.equals(ggi.getShortName()))
        return ggi;
    }
    return null;
  }

  public DtCoverage findGridByFullName(String fullName) {
    for (DtCoverage ggi : grids) {
      if (fullName.equals(ggi.getFullName()))
        return ggi;
    }
    return null;
  }

  public DtCoverage findGridDatatypeByAttribute(String attName, String attValue) {
    for (DtCoverage ggi : grids) {
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
   *
   * @param buf put info here
   */
  private void getInfo(Formatter buf) {
    int countGridset = 0;

    for (Gridset gs : gridsetHash.values()) {
      DtCoverageCS gcs = gs.getGeoCoordSystem();
      buf.format("%nGridset %d  coordSys=%s", countGridset, gcs);
      buf.format(" LLbb=%s ", gcs.getLatLonBoundingBox());
      if ((gcs.getProjection() != null) && !gcs.getProjection().isLatLon())
        buf.format(" bb= %s", gcs.getBoundingBox());
      buf.format("%n");
      buf.format("Name__________________________Unit__________________________hasMissing_Description%n");
      for (DtCoverage grid : gs.getGrids()) {
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

    private DtCoverageCS gcc;
    private List<DtCoverage> grids = new ArrayList<>();

    private Gridset(DtCoverageCS gcc) {
      this.gcc = gcc;
    }

    private void add(DtCoverage grid) {
      grids.add(grid);
    }

    /**
     * Get list of GeoGrid objects
     */
    public List<DtCoverage> getGrids() {
      return grids;
    }

    /**
     * all GeoGrid point to this GeoGridCoordSys
     */
    public DtCoverageCS getGeoCoordSystem() {
      return gcc;
    }

    /**
     * all GeoGrids point to this GeoCoordSysImpl.
     *
     * @deprecated use getGeoCoordSystem() if possible.
     */
    public DtCoverageCS getGeoCoordSys() {
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
