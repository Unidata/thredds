/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

import ucar.nc2.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;

/**
 * Superclass for implementations of TypedDataset.
 *
 * @deprecated use ucar.nc2.ft.*
 * @author John Caron
 */

public abstract class TypedDatasetImpl implements TypedDataset {
  protected NetcdfDataset netcdfDataset;
  protected String title, desc, location;
  protected Date startDate, endDate;
  protected LatLonRect boundingBox;
  protected List<VariableSimpleIF> dataVariables = new ArrayList<>();
  protected StringBuffer parseInfo = new StringBuffer();

  /** No-arg constructor */
  public TypedDatasetImpl() {}

  /** Constructor when theres no NetcdfFile underneath.
   *
   * @param title title of the dataset.
   * @param description description of the dataset.
   * @param location URI of the dataset
   */
  public TypedDatasetImpl(String title, String description, String location) {
    this.title = title;
    this.desc = description;
    this.location = location;
  }

  /** Construtor when theres a NetcdfFile underneath
   * @param netcdfDataset adapt this NetcdfDataset
   */
  public TypedDatasetImpl(NetcdfDataset netcdfDataset) {
    this.netcdfDataset = netcdfDataset;
    this.location = netcdfDataset.getLocation();

    this.title = netcdfDataset.getTitle();
    if (title == null)
      title = netcdfDataset.findAttValueIgnoreCase(null, "title", null);
    if (desc == null)
      desc = netcdfDataset.findAttValueIgnoreCase(null, "description", null);
  }

  public void setTitle( String title) { this.title = title; }
  public void setDescription( String desc) { this.desc = desc; }
  public void setLocationURI( String location) {this.location = location; }

  protected abstract void setStartDate(); // reminder for subclasses to set this
  protected abstract void setEndDate(); // reminder for subclasses to set this
  protected abstract void setBoundingBox(); // reminder for subclasses to set this

  protected void removeDataVariable( String varName) {
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getShortName().equals( varName) )
        iter.remove();
    }
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() { return netcdfDataset; }
  public String getTitle() { return title; }
  public String getDescription() { return desc; }
  public String getLocationURI() {return location; }
  public String getLocation() {return location; }
  public List<Attribute> getGlobalAttributes() {
    if (netcdfDataset == null) return new ArrayList<>();
    return netcdfDataset.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase( String name ) {
    if (netcdfDataset == null) return null;
    return netcdfDataset.findGlobalAttributeIgnoreCase( name);
  }

  public void close() throws java.io.IOException {
    if (netcdfDataset != null) netcdfDataset.close();
  }

  public String getDetailInfo() {
    DateFormatter formatter = new DateFormatter();
    StringBuilder sbuff = new StringBuilder();

    sbuff.append("  location= ").append(getLocation()).append("\n");
    sbuff.append("  title= ").append(getTitle()).append("\n");
    sbuff.append("  desc= ").append(getDescription()).append("\n");
    sbuff.append("  start= ").append(formatter.toDateTimeString(getStartDate())).append("\n");
    sbuff.append("  end  = ").append(formatter.toDateTimeString(getEndDate())).append("\n");
    sbuff.append("  bb   = ").append(getBoundingBox()).append("\n");
    if (getBoundingBox() != null )
      sbuff.append("  bb   = ").append(getBoundingBox().toString2()).append("\n");

    sbuff.append("  has netcdf = ").append(getNetcdfFile() != null).append("\n");
    List<Attribute> ga = getGlobalAttributes();
    if (ga.size() > 0) {
      sbuff.append("  Attributes\n");
      for (Attribute a : ga) {
        sbuff.append("    ").append(a).append("\n");
      }
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sbuff.append("  Variables (").append(vars.size()).append(")\n");
    for (VariableSimpleIF v : vars) {
      sbuff.append("    name='").append(v.getShortName()).append("' desc='").append(v.getDescription()).append("' units='").append(v.getUnitsString()).append("' type=").append(v.getDataType()).append("\n");
    }

    sbuff.append("\nparseInfo=\n");
    sbuff.append(parseInfo);
    sbuff.append("\n");

    return sbuff.toString();
  }

  public Date getStartDate() { return startDate; }
  public Date getEndDate() { return endDate; }
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