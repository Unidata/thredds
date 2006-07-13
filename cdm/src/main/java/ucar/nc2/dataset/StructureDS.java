// $Id:StructureDS.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureMembers;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * An "enhanced" Structure.
 * @author john caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class StructureDS extends ucar.nc2.Structure implements VariableEnhanced {
  private EnhancementsImpl proxy;
  private EnhanceScaleMissingImpl smProxy;

  /** Constructor when theres no underlying variable. You better set the values too! */
  public StructureDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName,
      String dims, String units, String desc) {

    super(ds, group, parentStructure, shortName);
    setDimensions( dims);
    this.proxy = new EnhancementsImpl(this, units, desc);
    this.smProxy = new EnhanceScaleMissingImpl();

    /* if (units != null)
      addAttribute( new Attribute("units", units));
    if (desc != null)
      addAttribute( new Attribute("long_name", desc)); */
  }

  public StructureDS(Group g, ucar.nc2.Structure orgVar, boolean reparent) {
    super(orgVar, reparent);
    this.group = g;
    this.proxy = new EnhancementsImpl( this);
    this.smProxy = new EnhanceScaleMissingImpl(); // scale/offset not applied to Structure
  }

    /** Override so it returns a Structure */
  public Variable section(List section) throws InvalidRangeException  {
    Variable vs = new StructureDS( this.group, this, false);
    makeSection( vs, section);
    return vs;
  }

  public StructureMembers makeStructureMembers() {
    StructureMembers members = new StructureMembers( getName());
    java.util.Iterator viter = getVariables().iterator();
    while (viter.hasNext()) {
      VariableDS v2 = (VariableDS) viter.next();
      StructureMembers.Member m = new StructureMembers.Member( v2);
      members.addMember( m);
    }
    return members;
  }

  protected Array _read() throws IOException {
    ArrayStructure as = (ArrayStructure) super._read();
    enhanceMembers( as.getStructureMembers());
    return as;
  }

  protected Array _read(List section) throws IOException, InvalidRangeException {
    ArrayStructure as = (ArrayStructure) super._read( section);
    enhanceMembers( as.getStructureMembers());
    return as;
  }

    // non-structure-member Variables.
  private void enhanceMembers(StructureMembers members) {
    java.util.Iterator iter = members.getMembers().iterator();
    while (iter.hasNext()) {
      StructureMembers.Member m = (StructureMembers.Member) iter.next();
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      m.setVariableSimple( v2);
    }
  } 

  ///////////////////////////////////////

  // VariableEnhanced implementation
  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.addCoordinateSystem( p0);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.removeCoordinateSystem( p0);
  }

  public java.util.List getCoordinateSystems() {
    return proxy.getCoordinateSystems();
  }

  public java.lang.String getDescription() {
    return proxy.getDescription();
  }

  public ucar.nc2.Variable getOriginalVariable() {
    return proxy.getOriginalVariable();
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  public double getValidMax() {
    return smProxy.getValidMax();
  }

  public double getValidMin() {
    return smProxy.getValidMin();
  }

  public boolean hasFillValue() {
    return smProxy.hasFillValue();
  }

  public boolean hasInvalidData() {
    return smProxy.hasInvalidData();
  }

  public boolean hasMissing() {
    return smProxy.hasMissing();
  }

  public boolean hasMissingValue() {
    return smProxy.hasMissingValue();
  }

  public boolean hasScaleOffset() {
    return smProxy.hasScaleOffset();
  }

  public boolean isFillValue(double p0) {
    return smProxy.isFillValue( p0);
  }

  public boolean isInvalidData(double p0) {
    return smProxy.isInvalidData( p0);
  }

  public boolean isMissing(double p0) {
    return smProxy.isMissing( p0);
  }

  public boolean isMissingValue(double p0) {
    return smProxy.isMissingValue( p0);
  }

  public void setFillValueIsMissing(boolean p0) {
    smProxy.setFillValueIsMissing( p0);
  }

  public void setInvalidDataIsMissing(boolean p0) {
    smProxy.setInvalidDataIsMissing( p0);
  }

  public void setMissingDataIsMissing(boolean p0) {
    smProxy.setMissingDataIsMissing( p0);
  }

  public void setUseNaNs(boolean useNaNs) {
    smProxy.setUseNaNs( useNaNs);
  }

  /**
   * Convert data if hasScaleOffset, using scale and offset.
   * Also if useNaNs = true, return NaN if value is missing data.
   * @param value data to convert
   * @return converted data.
   */
  public double convertScaleOffsetMissing(byte value) {
    return smProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(short value) {
    return smProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(int value) {
    return smProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(long value) {
    return smProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(double value) {
    return smProxy.convertScaleOffsetMissing( value);
  }
}
