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
 * @author John Caron
 */

public abstract class TypedDatasetImpl implements TypedDataset {
  protected NetcdfDataset ncfile;
  protected String title, desc, location;
  protected Date startDate, endDate;
  protected LatLonRect boundingBox;
  protected List<VariableSimpleIF> dataVariables = new ArrayList<VariableSimpleIF>(); // VariableSimpleIF
  protected StringBuffer parseInfo = new StringBuffer();

  /** No-arg constuctor */
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
   * @param ncfile adapt this NetcdfDataset
   */
  public TypedDatasetImpl(NetcdfDataset ncfile) {
    this.ncfile = ncfile;
    this.location = ncfile.getLocation();

    this.title = ncfile.getTitle();
    if (title == null)
      title = ncfile.findAttValueIgnoreCase(null, "title", null);
    if (desc == null)
      desc = ncfile.findAttValueIgnoreCase(null, "description", null);
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
      if (v.getName().equals( varName) )
        iter.remove();
    }
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() { return ncfile; }
  public String getTitle() { return title; }
  public String getDescription() { return desc; }
  public String getLocationURI() {return location; }
  public String getLocation() {return location; }
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

  public String getDetailInfo() {
    DateFormatter formatter = new DateFormatter();
    StringBuffer sbuff = new StringBuffer();

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