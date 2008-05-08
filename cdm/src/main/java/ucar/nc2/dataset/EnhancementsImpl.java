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
package ucar.nc2.dataset;

import ucar.nc2.*;
import java.util.*;

/**
 * Implementation of Enhancements for coordinate systems and standard attribute handling.
 * @author caron
 */
class EnhancementsImpl implements Enhancements {
  private Variable forVar;
  private String desc, units;
  private List<CoordinateSystem> coordSys = new ArrayList<CoordinateSystem>();

  /**
   * Constructor when there's no underlying, existing Variable.
   * You can access units, description and coordSys.
   * All missing and scale/offset flags are false.
   * @param forVar the Variable to decorate.
   * @param units set unit string.
   * @param desc set description.
   */
  public EnhancementsImpl( Variable forVar, String units, String desc) {
    this.forVar = forVar;
    this.units = units;
    this.desc = desc;
  }

  /**
   * Constructor.
   * @param forVar the Variable to decorate.
   */
  public EnhancementsImpl( Variable forVar) {
    this.forVar = forVar;
  }

  /**
   * Get the list of Coordinate Systems for this variable.
   * Normally this is empty unless you use ucar.nc2.dataset.NetcdfDataset.
   * @return list of type ucar.nc2.dataset.CoordinateSystem; may be empty not null.
   */
  public List<CoordinateSystem> getCoordinateSystems() {return coordSys; }

  /** Add a CoordinateSystem to the dataset. */
  public void addCoordinateSystem( CoordinateSystem cs){
    coordSys.add(cs);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    coordSys.remove( p0);
  }

  /** Set the Description for this Variable.
   * @param desc description
   */
  public void setDescription(String desc) { this.desc = desc; }

  /** Get the description of the Variable.
   *  Default is to look for attriutes in this order: "long_name", "description", "title", "standard_name".
   */
  public String getDescription() {
    if ((desc == null) && (forVar != null)) {
      Attribute att = forVar.findAttributeIgnoreCase( "long_name");
      if ((att != null) && att.isString())
        desc = att.getStringValue().trim();

      if (desc == null) {
        att = forVar.findAttributeIgnoreCase( "description");
        if ((att != null) && att.isString())
          desc = att.getStringValue().trim();
      }

      if (desc == null) {
        att = forVar.findAttributeIgnoreCase( "title");
        if ((att != null) && att.isString())
          desc = att.getStringValue().trim();
      }

      if (desc == null) {
        att = forVar.findAttributeIgnoreCase( "standard_name");
        if ((att != null) && att.isString())
          desc = att.getStringValue().trim();
      }

    }

    return (desc == null) ? "" : desc;
  }

  /** Set the Unit String for this Variable. Default is to use the "units" attribute.
   * @param units  unit string
   */
  public void setUnitsString( String units) {
    this.units = units;
    forVar.addAttribute( new Attribute("units", units));
  }

  /**
   * Get the Unit String for the Variable. May be set explicitly, else look for attribute "units".
   * @return the Unit String for the Variable, or null if none.
   */
  public String getUnitsString() {
    if ((units == null) && (forVar != null)) {
      Attribute att = forVar.findAttributeIgnoreCase( "units");
      if ((att != null) && att.isString())
        return att.getStringValue().trim();
    }
    return units;
  }
}