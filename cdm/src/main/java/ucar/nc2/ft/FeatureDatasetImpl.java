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
package ucar.nc2.ft;

import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of FeatureDataset.
 * Subclass must implement getFeatureClass(), and add specific functionality.
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class FeatureDatasetImpl implements FeatureDataset {
  protected NetcdfDataset netcdfDataset;
  protected String title, desc, location;
  protected List<VariableSimpleIF> dataVariables;
  protected Formatter parseInfo = new Formatter();
  protected CalendarDateRange dateRange;
  protected LatLonRect boundingBox;
  protected FileCacheIF fileCache;

  // for subsetting
  protected FeatureDatasetImpl(FeatureDatasetImpl from) {
    this.netcdfDataset = from.netcdfDataset;
    this.title = from.title;
    this.desc = from.desc;
    this.location = from.location;
    this.dataVariables = new ArrayList<>( from.dataVariables);
    this.parseInfo = new Formatter();
    String fromInfo = from.parseInfo.toString().trim();
    if (fromInfo.length() > 0)
      parseInfo.format("%s%n", fromInfo);
    this.parseInfo.format("Subsetted from original%n");
  }

  /** No-arg constuctor */
  public FeatureDatasetImpl() {}

  /** Constructor when theres no NetcdfFile underneath.
   *
   * @param title title of the dataset.
   * @param description description of the dataset.
   * @param location URI of the dataset
   */
  public FeatureDatasetImpl(String title, String description, String location) {
    this.title = title;
    this.desc = description;
    this.location = location;
  }

  /** Constructor when theres a NetcdfFile underneath
   * @param netcdfDataset adapt this NetcdfDataset
   */
  public FeatureDatasetImpl(NetcdfDataset netcdfDataset) {
    this.netcdfDataset = netcdfDataset;
    this.location = netcdfDataset.getLocation();

    this.title = netcdfDataset.getTitle();
    if (title == null)
      title = netcdfDataset.findAttValueIgnoreCase(null, "title", null);
    if (desc == null)
      desc = netcdfDataset.findAttValueIgnoreCase(null, "description", null);
  }

  protected void setTitle( String title) { this.title = title; }
  protected void setDescription( String desc) { this.desc = desc; }
  protected void setLocationURI( String location) {this.location = location; }
  public void setDateRange(CalendarDateRange dateRange) { this.dateRange = dateRange; }
  public void setBoundingBox(LatLonRect boundingBox) { this.boundingBox = boundingBox; }

  /* protected void removeDataVariable( String varName) {
    if (dataVariables == null) return;
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getName().equals( varName) )
        iter.remove();
    }
  } */

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() { return netcdfDataset; }
  public String getTitle() { return title; }
  public String getDescription() { return desc; }
  public String getLocation() {return location; }
  public List<Attribute> getGlobalAttributes() {
    if (netcdfDataset == null) return new ArrayList<>();
    return netcdfDataset.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase( String name ) {
    if (netcdfDataset == null) return null;
    return netcdfDataset.findGlobalAttributeIgnoreCase( name);
  }

  public void getDetailInfo( java.util.Formatter sf) {

    sf.format("FeatureDataset on location= %s%n", getLocation());
    sf.format("  featureType= %s%n",getFeatureType());
    sf.format("  title= %s%n",getTitle());
    sf.format("  desc= %s%n",getDescription());
    sf.format("  range= %s%n",getCalendarDateRange());
    sf.format("  start= %s%n", getCalendarDateEnd());
    sf.format("  end  = %s%n",getCalendarDateEnd());
    LatLonRect bb = getBoundingBox();
    sf.format("  bb   = %s%n", bb);
    if (bb != null)
      sf.format("  bb   = %s%n",getBoundingBox().toString2());

    sf.format("  has netcdf = %b%n", (getNetcdfFile() != null));
    List<Attribute> ga = getGlobalAttributes();
    if (ga.size() > 0) {
      sf.format("  Attributes%n");
      for (Attribute a : ga)
        sf.format("    %s%n",a);
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sf.format("%n  Data Variables (%d)%n",vars.size());
    for (VariableSimpleIF v : vars) {
      sf.format("    name='%s' desc='%s' units=%s' type='%s' dims=(", v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType());
      for (Dimension d : v.getDimensions()) sf.format("%s ", d);
      sf.format(")%n");
    }

    if (parseInfo.toString().length() > 0)
      sf.format("%nparseInfo=%n%s%n", parseInfo);
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    return dateRange;
  }

  @Override
  public CalendarDate getCalendarDateStart() {
    return (dateRange == null) ? null : dateRange.getStart();
  }

  @Override
  public CalendarDate getCalendarDateEnd() {
    return (dateRange == null) ? null : dateRange.getEnd();
  }

  public DateRange getDateRange() { return (dateRange == null) ? null : dateRange.toDateRange(); }
  public Date getStartDate() { return (getDateRange() == null) ? null : getDateRange().getStart().getDate(); }
  public Date getEndDate() { return (getDateRange() == null) ? null : getDateRange().getEnd().getDate(); }
  public LatLonRect getBoundingBox() { return boundingBox; }

  public List<VariableSimpleIF> getDataVariables() {
    return (dataVariables == null) ? new ArrayList<VariableSimpleIF>() : dataVariables;
  }
  public VariableSimpleIF getDataVariable( String shortName) {
    for (VariableSimpleIF s : getDataVariables()) {
      String ss = s.getShortName();
      if (shortName.equals(ss)) return s;
    }
    return null;
  }


  public String getImplementationName() {
    return getClass().getName();
  }

  //////////////////////////////////////////////////
  //  FileCacheable

  @Override
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this)) return;
    }

    try {
      if (netcdfDataset != null) netcdfDataset.close();
    } finally {
      netcdfDataset = null;
    }
  }

      // release any resources like file handles
  public void release() throws IOException {
    if (netcdfDataset != null) netcdfDataset.release();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    if (netcdfDataset != null) netcdfDataset.reacquire();
  }

  @Override
  public long getLastModified() {
    return (netcdfDataset != null) ? netcdfDataset.getLastModified() : 0;
  }

  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }

}
