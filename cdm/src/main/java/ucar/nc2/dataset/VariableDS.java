// $Id: VariableDS.java,v 1.27 2006/03/25 00:20:11 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;
import java.util.List;

/**
 * An "enhanced" Variable.
 * @author John Caron
 * @version $Revision: 1.27 $ $Date: 2006/03/25 00:20:11 $
 */

public class VariableDS extends ucar.nc2.Variable implements VariableEnhanced {
  private EnhancementsImpl proxy;
  private EnhanceScaleMissingImpl smProxy;
  private boolean isEnhanced;
  private DataType orgDataType;
  private ucar.nc2.ncml.Aggregation agg = null; // for NcML

  /** Constructor when theres no underlying variable.  */
  public VariableDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName,
      DataType dataType, String dims, String units, String desc) {

    super(ds, group, parentStructure, shortName);
    setDataType(dataType);
    setDimensions( dims);
    this.orgDataType = dataType;

    if (units != null)
      addAttribute( new Attribute("units", units));
    if (desc != null)
      addAttribute( new Attribute("long_name", desc));

    this.proxy = new EnhancementsImpl(this, units, desc);
    this.smProxy = new EnhanceScaleMissingImpl(); // ??
  }

  /**
   * Wrap the given Variable, making it into an enhanced one.
   * @param ncVar the original Variable to wrap.
   */
  public VariableDS( Group g, Variable ncVar, boolean enhance) {
    super(ncVar);
    if (g != null) this.group = g; // otherwise super() sets group
    this.orgDataType = ncVar.getDataType();

    if (ncVar instanceof VariableDS) {
      VariableDS ncVarDS = (VariableDS) ncVar;
      this.agg = ncVarDS.agg;
    }

    this.proxy = new EnhancementsImpl( this);
    if (enhance) {
      enhance();
    } else {
      this.smProxy = new EnhanceScaleMissingImpl();
    }
  }

  // override to keep section a VariableDS
  public Variable section(List section) throws InvalidRangeException  {
    Variable vs = new VariableDS( this.group, this, isEnhanced);
    makeSection( vs, section);
    return vs;
  }

  /** recalc any enhancement info */
  public void enhance() {
    this.smProxy = new EnhanceScaleMissingImpl( this);
    if (smProxy.hasScaleOffset() && (smProxy.getConvertedDataType() != getDataType()))
      setDataType( smProxy.getConvertedDataType());
    this.isEnhanced = true;
  }

  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.addCoordinateSystem( p0);
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

  /**
   * When this wraps another Variabloe, get the original Variable's DataType.
   */
  public DataType getOriginalDataType() {
    return orgDataType;
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  public void setUnitsString( String units) {
    proxy.setUnitsString(units);
  }

  /** @deprecated use getUnitsString()*/
  public java.lang.String getUnitString() {
    return getUnitsString();
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

  /** If its an NcML aggregation, it has an Aggregation object associated. */
  public void setAggregation( ucar.nc2.ncml.Aggregation agg) {this.agg = agg; }

  /** If this Variable has been "enhanced", ie processed for scale/offset/missing value */
  public boolean isEnhanced() { return isEnhanced; }

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

  // regular Variables.
  protected Array _read() throws IOException {
    Array result;
    if (agg != null)
     result = agg.read( this, null);
    else if (hasCachedData())
      result = super._read();
    else if (orgVar != null)
      result = orgVar.read();
    else { // return fill value in a "constant array"; this allow NcML to act as ncgen
      Object data = smProxy.getFillValue( getDataType());
      Array array = Array.factoryConstant( dataType.getPrimitiveClassType(), getShape(), data);
      return array;
    }

    if (smProxy.hasScaleOffset())
      result = smProxy.convertScaleOffset( result);
    else if (smProxy.hasMissing() && smProxy.getUseNaNs())
      result = smProxy.convertMissing( result);

    return result;
  }

  // section of regular Variable
  protected Array _read(java.util.List section) throws IOException, InvalidRangeException  {
    Array result;
    if (agg != null)
      result = agg.read( this, null, section);
    else if (hasCachedData())
      result = super._read(section);
    else if (orgVar != null)
      result = orgVar.read(section);
    else { // return fill value in a "constant array"
      Object data = smProxy.getFillValue( getDataType());
      Array array = Array.factoryConstant( dataType.getPrimitiveClassType(), Range.getShape(section), data);
      return array;
    }

    if (smProxy.hasScaleOffset())
      result = smProxy.convertScaleOffset( result);
    else if (smProxy.hasMissing() && smProxy.getUseNaNs())
      result = smProxy.convertMissing( result);

    return result;
  }

  // structure-member Variables.
  protected Array _readMemberData(java.util.List section, boolean flatten) throws IOException, InvalidRangeException  {
    Array result;
    //if (agg != null)
    //  result = agg.readMemberData( this, null);
    //else
    if (orgVar != null)
      result = orgVar.readAllStructures(section, flatten);
    else
      result = super._readMemberData(section, flatten);

    // LOOK should do recursively
    /* if (smProxy.hasScaleOffset())
      result = smProxy.convertScaleOffset( result);
    else if (smProxy.hasMissing() && smProxy.getUseNaNs())
      result = smProxy.convertMissing( result); */

    return result;
  }

}