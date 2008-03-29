/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract superclass for implementations of FeatureDataset
 * Subclass must implement getFeatureClass(), and add specific functionality.
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class FeatureDatasetImpl implements FeatureDataset {
  protected NetcdfDataset ncfile;
  protected String title, desc, location;
  protected List<VariableSimpleIF> dataVariables = new ArrayList<VariableSimpleIF>();
  protected StringBuffer parseInfo = new StringBuffer();
  protected DateRange dateRange;
  protected LatLonRect boundingBox;

  // for subsetting
  protected FeatureDatasetImpl(FeatureDatasetImpl from) {
    this.ncfile = from.ncfile;
    this.title = from.title;
    this.desc = from.desc;
    this.location = from.location;
    this.dataVariables = new ArrayList<VariableSimpleIF>( from.dataVariables);
    this.parseInfo = new StringBuffer(from.parseInfo);
    this.parseInfo.append("Subsetted from original\n");
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

  /** Construtor when theres a NetcdfFile underneath
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
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getName().equals( varName) )
        iter.remove();
    }
  }

  public void appendParseInfo( String info) {
    parseInfo.append(info);
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() { return ncfile; }
  public String getTitle() { return title; }
  public String getDescription() { return desc; }
  public String getLocationURI() {return location; }
  public List<Attribute> getGlobalAttributes() {
    if (ncfile == null) return new ArrayList<Attribute>();
    return ncfile.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase( String name ) {
    if (ncfile == null) return null;
    return ncfile.findGlobalAttributeIgnoreCase( name);
  }

  public void close() throws java.io.IOException {
    if (ncfile != null) ncfile.close();
  }

  public void getDetailInfo( java.util.Formatter sf) {
    DateFormatter formatter = new DateFormatter();

    sf.format("FeatureDataset on location= %s\n", getLocationURI());
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

  public List<VariableSimpleIF> getDataVariables() {return dataVariables; }
  public VariableSimpleIF getDataVariable( String shortName) {
    for (VariableSimpleIF s : dataVariables) {
      String ss = s.getShortName();
      if (shortName.equals(ss)) return s;
    }
    return null;
  }

}
