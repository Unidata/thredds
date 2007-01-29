// $Id:TypedDatasetImpl.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt;

import ucar.nc2.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;

/**
 * Superclass for implementations of TypedDataset.
 *
 * @author John Caron
 * @version $Id:TypedDatasetImpl.java 51 2006-07-12 17:13:13Z caron $
 */

public abstract class TypedDatasetImpl implements TypedDataset {
  protected NetcdfFile ncfile;
  protected String title, desc, location;
  protected Date startDate, endDate;
  protected LatLonRect boundingBox;
  protected ArrayList dataVariables = new ArrayList(); // VariableSimpleIF
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

  /** Construtor when theres a NetcdfFile underneath */
  public TypedDatasetImpl(NetcdfFile ncfile) {
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
  public List getGlobalAttributes() {
    if (ncfile == null) return new ArrayList();
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

    sbuff.append("  location= "+getLocationURI()+"\n");
    sbuff.append("  title= "+getTitle()+"\n");
    sbuff.append("  desc= "+getDescription()+"\n");
    sbuff.append("  start= "+formatter.toDateTimeString( getStartDate())+"\n");
    sbuff.append("  end  = "+formatter.toDateTimeString( getEndDate())+"\n");
    sbuff.append("  bb   = "+getBoundingBox()+"\n");

    sbuff.append("  has netcdf = "+(getNetcdfFile() != null)+"\n");
    List ga = getGlobalAttributes();
    if (ga.size() > 0) {
      sbuff.append("  Attributes\n");
      for (int i = 0; i < ga.size(); i++) {
        Attribute a = (Attribute) ga.get(i);
         sbuff.append("    "+a+"\n");
      }
    }

    List vars = getDataVariables();
    sbuff.append("  Variables ("+vars.size()+")\n");
    for (int i = 0; i < vars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) vars.get(i);
      sbuff.append("    name='"+v.getShortName()+"' desc='"+v.getDescription()+"' units='"+v.getUnitsString()+"' type="+v.getDataType()+"\n");
    }

    sbuff.append("\nparseInfo=\n");
    sbuff.append(parseInfo);
    sbuff.append("\n");

    return sbuff.toString();
  }

  public Date getStartDate() { return startDate; }
  public Date getEndDate() { return endDate; }
  public LatLonRect getBoundingBox() { return boundingBox; }

  public List getDataVariables() {return dataVariables; }
  public VariableSimpleIF getDataVariable( String shortName) {
    for (int i = 0; i < dataVariables.size(); i++) {
      VariableSimpleIF s = (VariableSimpleIF) dataVariables.get(i);
      String ss = s.getShortName();
      if (shortName.equals( ss)) return s;
    }
    return null;
  }
}