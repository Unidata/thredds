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
package ucar.nc2.ft;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;
import java.io.IOException;

/**
 * Abstract superclass for implementations of FeatureDataset.
 * Subclass must implement getFeatureClass(), and add specific functionality.
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class FeatureDatasetImpl implements FeatureDataset {
  protected NetcdfDataset ncfile;
  protected String title, desc, location;
  protected List<VariableSimpleIF> dataVariables;
  protected Formatter parseInfo = new Formatter();
  protected DateRange dateRange;
  protected LatLonRect boundingBox;

  // for subsetting
  protected FeatureDatasetImpl(FeatureDatasetImpl from) {
    this.ncfile = from.ncfile;
    this.title = from.title;
    this.desc = from.desc;
    this.location = from.location;
    this.dataVariables = new ArrayList<VariableSimpleIF>( from.dataVariables);
    this.parseInfo = new Formatter();
    String fromInfo = from.parseInfo.toString().trim();
    if (fromInfo.length() > 0)
      parseInfo.format("%s\n", fromInfo);
    this.parseInfo.format("Subsetted from original\n");
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
   * @param ncfile adapt this NetcdfDataset
   */
  public FeatureDatasetImpl(NetcdfDataset ncfile) {
    this.ncfile = ncfile;
    this.location = ncfile.getLocation();

    this.title = ncfile.getTitle();
    if (title == null)
      title = ncfile.findAttValueIgnoreCase(null, "title", null);
    if (desc == null)
      desc = ncfile.findAttValueIgnoreCase(null, "description", null);
  }

  protected void setTitle( String title) { this.title = title; }
  protected void setDescription( String desc) { this.desc = desc; }
  protected void setLocationURI( String location) {this.location = location; }
  protected void setDateRange(DateRange dateRange) { this.dateRange = dateRange; }
  protected void setBoundingBox(LatLonRect boundingBox) { this.boundingBox = boundingBox; }

  protected void removeDataVariable( String varName) {
    if (dataVariables == null) return;
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getName().equals( varName) )
        iter.remove();
    }
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() { return ncfile; }
  public String getTitle() { return title; }
  public String getDescription() { return desc; }
  public String getLocation() {return location; }
  public List<Attribute> getGlobalAttributes() {
    if (ncfile == null) return new ArrayList<Attribute>();
    return ncfile.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase( String name ) {
    if (ncfile == null) return null;
    return ncfile.findGlobalAttributeIgnoreCase( name);
  }

  public void getDetailInfo( java.util.Formatter sf) {
    DateFormatter formatter = new DateFormatter();

    sf.format("FeatureDataset on location= %s\n", getLocation());
    sf.format("  featureType= %s\n",getFeatureType());
    sf.format("  title= %s\n",getTitle());
    sf.format("  desc= %s\n",getDescription());
    sf.format("  range= %s\n",getDateRange());
    sf.format("  start= %s\n", formatter.toDateTimeString(getStartDate()));
    sf.format("  end  = %s\n",formatter.toDateTimeString(getEndDate()));
    LatLonRect bb = getBoundingBox();
    sf.format("  bb   = %s\n", bb);
    if (bb != null)
      sf.format("  bb   = %s\n",getBoundingBox().toString2());

    sf.format("  has netcdf = %b\n", (getNetcdfFile() != null));
    List<Attribute> ga = getGlobalAttributes();
    if (ga.size() > 0) {
      sf.format("  Attributes\n");
      for (Attribute a : ga)
        sf.format("    %s\n",a);
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sf.format("  Data Variables (%d)\n",vars.size());
    for (VariableSimpleIF v : vars)
      sf.format("    name='%s' desc='%s' units=%s' type='%s'\n",v.getName(),v.getDescription(),v.getUnitsString(),v.getDataType());

    sf.format("\nparseInfo=\n%s\n", parseInfo);
  }

  public DateRange getDateRange() { return dateRange; }
  public Date getStartDate() { return (dateRange == null) ? null : dateRange.getStart().getDate(); }
  public Date getEndDate() { return (dateRange == null) ? null : dateRange.getEnd().getDate(); }
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

  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else {
      try {
        if (ncfile != null) ncfile.close();
      } finally {
        ncfile = null;
      }
    }
  }

  public boolean sync() throws IOException {
    return false;
  }

  protected FileCache fileCache;
  public void setFileCache(FileCache fileCache) {
    this.fileCache = fileCache;
  }

}
