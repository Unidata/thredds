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
package ucar.nc2.dataset;

import ucar.nc2.*;
import java.util.*;

/**
 * Implementation of Enhancements for coordinate systems and standard attribute handling.
 * Factored out so that it can be used as a 'mixin' in VariablesDS and StructureDS.
 * 
 * @author caron
 */
class EnhancementsImpl implements Enhancements {
  private Variable forVar;
  private String desc, units;
  private List<CoordinateSystem> coordSys; // dont allocate unless its used

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
  public List<CoordinateSystem> getCoordinateSystems() {
    return (coordSys == null) ? new ArrayList<CoordinateSystem>(0) : coordSys;
  }

  /** Add a CoordinateSystem to the dataset. */
  public void addCoordinateSystem( CoordinateSystem cs){
    if (cs == null)
      throw new RuntimeException("Attempted to add null CoordinateSystem to var " + forVar.getName());

    if (coordSys == null) coordSys = new ArrayList<CoordinateSystem>(5);
    coordSys.add(cs);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    if (coordSys != null)
      coordSys.remove( p0);
  }

  /** Set the Description for this Variable.
   * @param desc description
   */
  public void setDescription(String desc) { this.desc = desc; }

  /** Get the description of the Variable.
   *  Default is to look for attributes in this order: "long_name", "description", "title", "standard_name".
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