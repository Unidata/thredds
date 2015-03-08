package ucar.nc2.ft.cover.impl;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.ft.cover.Coverage;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.CoverageDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * CoverageDataset Implementation
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageDatasetImpl implements CoverageDataset {

  private NetcdfDataset ds;
  private CoverageCS.Type type;
  private List<Coverage> coverages;
  private List<CoverageSet> coverageSets = new ArrayList<>();

  public CoverageDatasetImpl(NetcdfDataset ds, Formatter parseInfo) {
    this.ds = ds;

    CoverageCSFactory fac = new CoverageCSFactory();
    type =  fac.classify(ds, parseInfo);

    coverageSets = new ArrayList<> (ds.getVariables().size());
    for (CoordinateSystem cs : ds.getCoordinateSystems())  {
      CoverageCS ccs = CoverageCSFactory.make(ds, cs, parseInfo);
      if (ccs == null) continue;
      CoverageSet cset = new CoverageSetImpl(ccs);
      coverageSets.add(cset);
    }

    coverages = new ArrayList<> (ds.getVariables().size());
    for (Variable v : ds.getVariables())  {
      VariableEnhanced ve = (VariableEnhanced) v;
      List<CoordinateSystem> css = ve.getCoordinateSystems();
      if (css.size() == 0) continue;
      Collections.sort(css, new Comparator<CoordinateSystem>() { // take one with most axes
        public int compare(CoordinateSystem o1, CoordinateSystem o2) {
          return o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size() ;
        }
      });
      CoordinateSystem cs = css.get(0);
      CoverageSetImpl cset = findCoverageSet(cs);
      if (cset == null) continue;
      CoverageImpl ci = new CoverageImpl(ds, cset.ccs, ve);
      cset.coverages.add(ci);
      coverages.add(ci);
    }

  }

  private CoverageSetImpl findCoverageSet( CoordinateSystem cs) {
    for (CoverageSet ccs : coverageSets) {
      if (ccs.getCoverageCS().getName().equals(cs.getName()))
        return (CoverageSetImpl) ccs;
    }
    return null;
  }

  private LatLonRect llbbMax = null;
  private CalendarDateRange dateRangeMax = null;

  private void makeRanges() {

    for (CoverageSet cset : getCoverageSets()) {
      CoverageCS ccs = cset.getCoverageCS();

      LatLonRect llbb = ccs.getLatLonBoundingBox();
      if (llbbMax == null)
        llbbMax = llbb;
      else
        llbbMax.extend(llbb);

      CalendarDateRange dateRange = ccs.getCalendarDateRange();
      if (dateRange != null) {
        if (dateRangeMax == null)
          dateRangeMax = dateRange;
        else
          dateRangeMax.extend(dateRange);
      }
    }
  }

  @Override
  public List<Coverage> getCoverages() {
    return coverages;
  }

  @Override
  public Coverage findCoverage(String name) {
    for (Coverage c : coverages) {
      if (c.getFullName().equals(name)) return c;
    }
    return null;
  }

  public CoverageCS.Type getType() {
    return type;
  }

  @Override
  public List<CoverageSet> getCoverageSets() {
    return coverageSets;
  }

  @Override
  public FeatureType getFeatureType() {
    switch (type) {
      case Coverage:
      case Curvilinear:
      case Grid:
        return FeatureType.GRID;
      case Fmrc:
        return FeatureType.FMRC;
      case Swath:
        return FeatureType.SWATH;
    }
    return null;
  }

  @Override
  public String getTitle() {
    return ds.getTitle();
  }

  @Override
  public String getDescription() {
    String desc = ds.findAttValueIgnoreCase(null, "description", null);
    if (desc == null)
      desc = ds.findAttValueIgnoreCase(null, CDM.HISTORY, null);
    return (desc == null) ? getLocation() : desc;
  }

  @Override
  public String getLocation() {
    return ds.getLocation();
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    if (dateRangeMax == null) makeRanges();
    return dateRangeMax;
  }

  @Override
  public CalendarDate getCalendarDateStart() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getStart();
  }

  @Override
  public CalendarDate getCalendarDateEnd() {
    if (dateRangeMax == null) makeRanges();
    return (dateRangeMax == null) ? null : dateRangeMax.getEnd();
  }

  @Override
  public LatLonRect getBoundingBox() {
    if (llbbMax == null) makeRanges();
    return llbbMax;
  }

  @Override
  public void calcBounds() throws IOException {
    // not needed
  }

  @Override
  public List<Attribute> getGlobalAttributes() {
    return ds.getGlobalAttributes();
  }

  @Override
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ds.findGlobalAttribute(name);
  }

  @Override
  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> datav = new ArrayList<>(coverages.size());
    for (Coverage c : coverages) datav.add(c);
    return datav;
  }

  @Override
  public VariableSimpleIF getDataVariable(String shortName) {
    for (Coverage c : coverages) {
      if (c.getShortName().equals(shortName)) return c;
    }
    return null;
  }

  @Override
  public NetcdfFile getNetcdfFile() {
    return ds;
  }

  @Override
  public void close() throws IOException {
    ds.close();
  }

  ////////////////////////////////////////
  // FileCacheable

      // release any resources like file handles
  public void release() throws IOException {
    ds.release();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    ds.reacquire();
  }

  @Override
  public long getLastModified() {
    return ds.getLastModified();
  }

  @Override
  public void setFileCache(FileCacheIF fileCache) {
    ds.setFileCache(fileCache);
  }

  @Override
  public void getDetailInfo(Formatter sf) {
    ds.getDetailInfo();
}

  @Override
  public String getImplementationName() {
    return null;
  }

  @Override
  public DateRange getDateRange() {
    return getCalendarDateRange().toDateRange();
  }

  @Override
  public Date getStartDate() {
    CalendarDate cdate = getCalendarDateStart();
    return (cdate == null) ? null : cdate.toDate();
  }

  @Override
  public Date getEndDate() {
    CalendarDate cdate = getCalendarDateEnd();
    return (cdate == null) ? null : cdate.toDate();  }

  private static class CoverageSetImpl implements CoverageSet {
    CoverageCS ccs;
    List<Coverage> coverages = new ArrayList<>();

    CoverageSetImpl(CoverageCS ccs) {
      this.ccs = ccs;
    }

    @Override
    public List<Coverage> getCoverages() {
      return coverages;
    }

    @Override
    public CoverageCS getCoverageCS() {
      return ccs;
    }
  }
}
