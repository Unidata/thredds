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

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;
import java.util.EnumSet;

/**
 * An wrapper around a Variable, creating an "enhanced" Variable.
 * The original Variable is used for the I/O.
 * There are several distinct uses:
 * <ol>
 * <li>  1) handle scale/offset/missing values/enum conversion; this can change DataType and data values
 * <li>  2) container for coordinate system information
 * <li>  3) NcML modifications to underlying Variable
 * </ol>
 *
 * @see NetcdfDataset
 * @author caron
 */

public class VariableDS extends ucar.nc2.Variable implements VariableEnhanced, EnhanceScaleMissing {
  private EnhancementsImpl enhanceProxy;
  private EnhanceScaleMissingImpl scaleMissingProxy;
  private EnumSet<NetcdfDataset.Enhance> enhanceMode;
  private boolean needScaleOffsetMissing = false;
  private boolean needEnumConversion = false;

  protected Variable orgVar; // wrap this Variable
  private DataType orgDataType; // keep seperate for the case where there is no ioVar.
  private String orgName; // in case Variable wwas renamed, and we need the original name for aggregation

  /**
   * Constructor when there's no underlying variable.
   * You must also set the values by doing one of:<ol>
   * <li>set the values with setCachedData()
   * <li>set a proxy reader with setProxyReader()
   * </ol>
   * Otherwise, it is assumed to have constant values (using the fill value)
   *
   * @param ds the containing dataset
   * @param group the containing group
   * @param parentStructure the containing Structure (may be null)
   * @param shortName the (short) name
   * @param dataType the data type
   * @param dims list of dimension names, these must already exist in the Group; empty String = scalar
   * @param units String value of units, may be null
   * @param desc  String value of description, may be null
   */
  public VariableDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName,
      DataType dataType, String dims, String units, String desc) {

    super(ds, group, parentStructure, shortName);
    setDataType(dataType);
    setDimensions( dims);
    this.orgDataType = dataType;

    if (dataType == DataType.STRUCTURE)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+shortName);

    if (units != null)
      addAttribute( new Attribute("units", units));
    if (desc != null)
      addAttribute( new Attribute("long_name", desc));

    this.enhanceProxy = new EnhancementsImpl(this, units, desc);
    this.scaleMissingProxy = new EnhanceScaleMissingImpl(); // gets replaced later, in enhance()
  }

  /**
   * Wrap the given Variable, making it into a VariableDS.
   * Delegate data reading to the original variable.
   * Does not share cache, iosp.
   *
   * @param g logical container, if null use orgVar's group
   * @param orgVar the original Variable to wrap. The original Variable is not modified.
   *    Must not be a Structure, use StructureDS instead.
   * @param enhance if true, use NetcdfDataset.defaultEnhanceMode to define what enhancements are made.
   *   Note that this can change DataType and data values.
   *   You can also call enhance() later. If orgVar is VariableDS, then enhance is inherited from there,
   *   and this parameter is ignored.
   */
  public VariableDS( Group g, Variable orgVar, boolean enhance) {
    super(orgVar);

    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+orgVar.getName());

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    this.preReader = null;
    this.postReader = null;
    createNewCache();

    this.orgVar = orgVar;
    this.orgDataType = orgVar.getDataType();
    if (g != null) this.group = g; // otherwise super() sets group; this affects the long name.

    if (orgVar instanceof VariableDS) {
      VariableDS ncVarDS = (VariableDS) orgVar;
      this.enhanceProxy = ncVarDS.enhanceProxy;
      this.scaleMissingProxy = ncVarDS.scaleMissingProxy;
      this.enhanceMode = ncVarDS.enhanceMode;

    } else {
      this.enhanceProxy = new EnhancementsImpl( this);
      if (enhance) {
        enhance(NetcdfDataset.defaultEnhanceMode);
      } else {
        this.scaleMissingProxy = new EnhanceScaleMissingImpl();
      }
    }
  }

  /**
   * Wrap the given Variable, making it into a VariableDS.
   * Delegate data reading to the original variable.
   * Does not share cache, iosp.
   * This is for NcML explicit mode
   *
   * @param group     the containing group; may not be null
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param orgVar the original Variable to wrap. The original Variable is not modified.
   *    Must not be a Structure, use StructureDS instead.
   */
  public VariableDS(Group group, Structure parent, String shortName, Variable orgVar) {
    super(null, group, parent, shortName);

    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+orgVar.getName());

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    this.preReader = null;
    this.postReader = null;
    createNewCache();

    this.orgVar = orgVar;
    this.orgDataType = orgVar.getDataType();

    this.enhanceProxy = new EnhancementsImpl( this);
    this.scaleMissingProxy = new EnhanceScaleMissingImpl();
  }

  /**
   * Copy constructor, for subclasses.
   * @param vds copy from here.
   */
  protected VariableDS( VariableDS vds) {
    super(vds);

    this.orgVar = vds.orgVar;
    this.orgDataType = vds.orgDataType;
    this.orgName = vds.orgName;

    this.scaleMissingProxy = vds.scaleMissingProxy;
    this.enhanceProxy = new EnhancementsImpl( this); //decouple coordinate systems

    // LOOK not sure of this
    this.enhanceMode = vds.enhanceMode;
    this.needScaleOffsetMissing = vds.needScaleOffsetMissing;
    this.needEnumConversion = vds.needEnumConversion;
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new VariableDS( this);
  }

  /**
   * DO NOT USE DIRECTLY. public by accident.
   * Calculate scale/offset/missing value info. This may change the DataType.
   */
  public void enhance(EnumSet<NetcdfDataset.Enhance> mode) {
    this.enhanceMode = EnumSet.copyOf(mode);
    boolean alreadyScaleOffsetMissing = false;
    boolean alreadyEnumConversion = false;

    // see if underlying variable has enhancements already applied
    if (orgVar != null && orgVar instanceof VariableDS) {
      VariableDS orgVarDS = (VariableDS) orgVar;
      EnumSet<NetcdfDataset.Enhance> orgEnhanceMode = orgVarDS.getEnhanceMode();
      if (orgEnhanceMode != null) {
        if (orgEnhanceMode.contains(NetcdfDataset.Enhance.ScaleMissing)) {
          alreadyScaleOffsetMissing = true;
          this.enhanceMode.add(NetcdfDataset.Enhance.ScaleMissing); // Note: promote the enhancement to the wrapped variable
        }
        if (orgEnhanceMode.contains(NetcdfDataset.Enhance.ConvertEnums)) {
          alreadyEnumConversion = true;
          this.enhanceMode.add(NetcdfDataset.Enhance.ConvertEnums); // Note: promote the enhancement to the wrapped variable
        }
      }
    }

    // do we need to calculate the ScaleMissing ?
    if (!alreadyScaleOffsetMissing && dataType.isNumeric() && mode.contains(NetcdfDataset.Enhance.ScaleMissing) || mode.contains(NetcdfDataset.Enhance.ScaleMissingDefer)) {
      this.scaleMissingProxy = new EnhanceScaleMissingImpl( this);

      // promote the data type if ScaleMissing is set
      if (mode.contains(NetcdfDataset.Enhance.ScaleMissing) &&
          scaleMissingProxy.hasScaleOffset() && (scaleMissingProxy.getConvertedDataType() != getDataType())) {
        setDataType( scaleMissingProxy.getConvertedDataType());
        removeAttribute("_unsigned");
      }

      // do we need to actually convert data ?
      needScaleOffsetMissing = mode.contains(NetcdfDataset.Enhance.ScaleMissing) &&
          (scaleMissingProxy.hasScaleOffset() || scaleMissingProxy.getUseNaNs());
    }

    // do we need to do enum conversion ?
    if (!alreadyEnumConversion && mode.contains(NetcdfDataset.Enhance.ConvertEnums) && dataType.isEnum()) {
      this.needEnumConversion = true;

      // LOOK promote data type to STRING ????
      setDataType( DataType.STRING);
      removeAttribute("_unsigned");
    }

  }

  boolean getNeedScaleOffsetMissing() { return needScaleOffsetMissing; }
  boolean getNeedEnumConversion() { return needEnumConversion; }

  /** Get the enhancement mode
   * @return the enhancement mode
   */
  EnumSet<NetcdfDataset.Enhance> getEnhanceMode() { return enhanceMode; }

  public boolean isCoordinateVariable() {
    return (this instanceof CoordinateAxis) || super.isCoordinateVariable();
  }

  // Enhancements interface

  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    enhanceProxy.addCoordinateSystem( p0);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    enhanceProxy.removeCoordinateSystem( p0);
  }

  public java.util.List<CoordinateSystem> getCoordinateSystems() {
    return enhanceProxy.getCoordinateSystems();
  }

  public java.lang.String getDescription() {
    return enhanceProxy.getDescription();
  }

  public java.lang.String getUnitsString() {
    return enhanceProxy.getUnitsString();
  }

  public void setUnitsString( String units) {
    enhanceProxy.setUnitsString(units);
  }

  // EnhanceScaleMissing interface
  public Array convertScaleOffsetMissing(Array data) {
    return scaleMissingProxy.convertScaleOffsetMissing( data);
  }

  public double getValidMax() {
    return scaleMissingProxy.getValidMax();
  }

  public double getValidMin() {
    return scaleMissingProxy.getValidMin();
  }

  public boolean hasFillValue() {
    return scaleMissingProxy.hasFillValue();
  }

  public boolean hasInvalidData() {
    return scaleMissingProxy.hasInvalidData();
  }

  public boolean hasMissing() {
    return scaleMissingProxy.hasMissing();
  }

  public boolean hasMissingValue() {
    return scaleMissingProxy.hasMissingValue();
  }

  public boolean hasScaleOffset() {
    return scaleMissingProxy.hasScaleOffset();
  }

  public boolean isFillValue(double p0) {
    return scaleMissingProxy.isFillValue( p0);
  }

  public boolean isInvalidData(double p0) {
    return scaleMissingProxy.isInvalidData( p0);
  }

  public boolean isMissing(double val) {
    return scaleMissingProxy.isMissing( val);
  }

  public boolean isMissingFast(double val) {
    return scaleMissingProxy.isMissingFast( val);
  }

  public boolean isMissingValue(double p0) {
    return scaleMissingProxy.isMissingValue( p0);
  }

  public void setFillValueIsMissing(boolean p0) {
    scaleMissingProxy.setFillValueIsMissing( p0);
  }

  public void setInvalidDataIsMissing(boolean p0) {
    scaleMissingProxy.setInvalidDataIsMissing( p0);
  }

  public void setMissingDataIsMissing(boolean p0) {
    scaleMissingProxy.setMissingDataIsMissing( p0);
  }

  public void setUseNaNs(boolean useNaNs) {
    scaleMissingProxy.setUseNaNs( useNaNs);
  }

  public boolean getUseNaNs() {
    return scaleMissingProxy.getUseNaNs() ;
  }

  public double convertScaleOffsetMissing(byte value) {
    return scaleMissingProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(short value) {
    return scaleMissingProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(int value) {
    return scaleMissingProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(long value) {
    return scaleMissingProxy.convertScaleOffsetMissing( value);
  }
  public double convertScaleOffsetMissing(double value) {
    return scaleMissingProxy.convertScaleOffsetMissing( value);
  } 

  /**
   * A VariableDS usually wraps another Variable.
   * @return original Variable or null
   */
  public ucar.nc2.Variable getOriginalVariable() {
    return orgVar;
  }

  /**
   * Set the Variable to wrap. Used by NcML explicit mode.
   * @param orgVar original Variable, must not be a Structure
   */
  public void setOriginalVariable(ucar.nc2.Variable orgVar) {
    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+orgVar.getName());
    this.orgVar = orgVar;
  }

  /**
   * When this wraps another Variable, get the original Variable's DataType.
   * @return original Variable's DataType
   */
  public DataType getOriginalDataType() {
    return orgDataType;
  }

  /**
   * When this wraps another Variable, get the original Variable's DataType.
   * @return original Variable's DataType
   */
  public String getOriginalName() {
    return orgName;
  }

  @Override
  public void setName(String newName) {
    this.orgName = shortName;
    super.setName(newName);
  }

  /** Set the proxy reader.
   * @param proxyReader set to this
   */
  public void setProxyReader( ProxyReader proxyReader) {
    // LOOK: interactions with smProxy ??
    this.postReader = proxyReader;
  }

  /** Get the proxy reader, or null.
   * @return return the proxy reader, if any
   */
  public ProxyReader getProxyReader() {
    return this.postReader;
  }

  public String toStringDebug() {
    return (orgVar != null) ? orgVar.toStringDebug() : "";
  }

  // regular Variables.
  @Override
  protected Array _read() throws IOException {
    Array result;

    // has a pre-reader proxy
    if (preReader != null)
      return preReader.read(this, null);
    else if (hasCachedData())
      result = super._read();
    else if (postReader != null)
      result = postReader.read( this, null);
    else if (orgVar != null)
      result = orgVar.read();
    else { // return fill value in a "constant array"; this allow NcML to act as ncgen
      Object data = scaleMissingProxy.getFillValue( getDataType());
      return Array.factoryConstant( dataType.getPrimitiveClassType(), getShape(), data);
    }

    // LOOK not caching
    if (needScaleOffsetMissing)
      return convertScaleOffsetMissing(result);
    else if (needEnumConversion)
      return convertEnums(result);
    else
      return result;
  }

  // section of regular Variable
  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException  {    
    // really a full read
    if ((null == section) || section.computeSize() == getSize())
      return _read();
    
    Array result;
    // has a pre-reader proxy
    if (preReader != null)
      return preReader.read(this, section, null);
    else if (hasCachedData())
      result = super._read(section);
    else if (postReader != null)
      result = postReader.read( this, section, null);
    else if (orgVar != null)
      result = orgVar.read(section);
    else  { // return fill value in a "constant array"
      Object data = scaleMissingProxy.getFillValue( getDataType());
      return Array.factoryConstant( dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    if (needScaleOffsetMissing)
      return convertScaleOffsetMissing(result);
    else if (needEnumConversion)
      return convertEnums(result);
    else
      return result;
  }

  protected Array convertEnums(Array values) {
    DataType dt = DataType.getType(values.getElementType());
    if (!dt.isNumeric())
      System.out.println("HEY");

    Array result = Array.factory(DataType.STRING, values.getShape());
    IndexIterator ii = result.getIndexIterator();
    values.resetLocalIterator();
    while (values.hasNext()) {
      String sval = lookupEnumString(values.nextInt());
      ii.setObjectNext(sval);
    }
    return result;
  }


}