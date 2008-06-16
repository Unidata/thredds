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

/**
 * An wrapper around a Variable, creating an "enhanced" Variable.
 * The original Variable is used for the I/O.
 * There are several distinct uses:
 * <ol>
 * <li>  1) handle scale/offset/missing values; this can change DataType and data values
 * <li>  2) container for coordinate system information
 * <li>  3) NcML modifications to underlying Variable
 * </ol>
 * The default case is for VariableDS to be opened in enhanced mode, which checks for scale/offset/missing values and
 *   converts the data automatically. For double/floats, missing data is replaced with NaN's (you can turn off NaN
 *   replacement with NetcdfDataset.setUseNaNs(false)).
 *   This is done whenever the NetcdfDataset.open is called with NetcdfDataset.EnhanceMode equal to All or ScaleOffset.
 *
 * Automatic conversion has a lot of overhead, and if you need maximum performance, but still want to use
 *   scale/offset/missing value handling, open the NetcdfDataset with EnhanceMode.ScaleOffsetDefer. The variableDs
 *   data type is not promoted, and the data is not converted on a read, but you can call the convertScaleOffset()
 *   routines which will do the conversion on a point-by-point basis, or convertArray() which will convert the entire
 *   Array.
 * 
 * @author caron
 */

public class VariableDS extends ucar.nc2.Variable implements VariableEnhanced {
  private EnhancementsImpl proxy;
  private EnhanceScaleMissingImpl smProxy;
  private NetcdfDataset.EnhanceMode enhanceMode;

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

    this.proxy = new EnhancementsImpl(this, units, desc);
    this.smProxy = new EnhanceScaleMissingImpl(); // ??
  }

  /**
   * Wrap the given Variable, making it into a VariableDS.
   * Delegate data reading to the original variable.
   * Does not share cache, iosp.
   *
   * @param g logical container, if null use orgVar's group
   * @param orgVar the original Variable to wrap. The original Variable is not modified.
   *    Must not be a Structure, use StructureDS instead.
   * @param enhance if true, handle scale/offset/missing values; this can change DataType and data values.
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
      this.proxy = ncVarDS.proxy;
      this.smProxy = ncVarDS.smProxy;
      this.enhanceMode = ncVarDS.enhanceMode;

    } else {
      this.proxy = new EnhancementsImpl( this);
      if (enhance) {
        enhance(NetcdfDataset.defaultEnhanceMode);
      } else {
        this.smProxy = new EnhanceScaleMissingImpl();
      }
    }
  }

  /**
   * Copy constructor, for subclasses.
   * @param vds copy from here.
   */
  protected VariableDS( VariableDS vds) {
    super(vds);

    this.enhanceMode = vds.enhanceMode;
    this.orgVar = vds.orgVar;
    this.orgDataType = vds.orgDataType;
    this.smProxy = vds.smProxy;

    //decouple coordinate systems
    this.proxy = new EnhancementsImpl( this);
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new VariableDS( this);
  }

  /**
   * public by accident.
   * Calculate scale/offset/missing value info. This may change the DataType.
   */
  public void enhance(NetcdfDataset.EnhanceMode mode) {
    if (orgVar instanceof VariableDS) return; // LOOK ????

    if (mode == NetcdfDataset.EnhanceMode.All)
      mode = NetcdfDataset.EnhanceMode.ScaleMissing;
    else if (mode == NetcdfDataset.EnhanceMode.AllDefer)
      mode = NetcdfDataset.EnhanceMode.ScaleMissingDefer;

    this.smProxy = new EnhanceScaleMissingImpl( this);

    // see if we need to promote the data type
    if ((mode == NetcdfDataset.EnhanceMode.ScaleMissing) &&
         smProxy.hasScaleOffset() && (smProxy.getConvertedDataType() != getDataType()))
      setDataType( smProxy.getConvertedDataType());

    this.enhanceMode = mode;
  }

  /** If this Variable has been "enhanced", ie processed for scale/offset/missing value
   * @return EnhanceMode
   */
  NetcdfDataset.EnhanceMode getEnhanceMode() { return enhanceMode; }

  public boolean isCoordinateVariable() {
    return (this instanceof CoordinateAxis) || super.isCoordinateVariable();
  }

  // Enhancements interface

  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.addCoordinateSystem( p0);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.removeCoordinateSystem( p0);
  }

  public java.util.List<CoordinateSystem> getCoordinateSystems() {
    return proxy.getCoordinateSystems();
  }

  public java.lang.String getDescription() {
    return proxy.getDescription();
  }

  public java.lang.String getUnitsString() {
    return proxy.getUnitsString();
  }

  public void setUnitsString( String units) {
    proxy.setUnitsString(units);
  }

  // EnhanceScaleMissing interface
  public Array convert(Array data) {
    return smProxy.convert( data);
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

  public boolean isMissing(double val) {
    return smProxy.isMissing( val);
  }

  public boolean isMissingFast(double val) {
    return smProxy.isMissingFast( val);
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

  public boolean getUseNaNs() {
    return smProxy.getUseNaNs() ;
  }

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
  public ProxyReader getProxyReader() { return this.postReader; }

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
      Object data = smProxy.getFillValue( getDataType());
      return Array.factoryConstant( dataType.getPrimitiveClassType(), getShape(), data);
    }

    // LOOK not caching
    return (enhanceMode ==  NetcdfDataset.EnhanceMode.ScaleMissing) ? convert(result) : result;
  }

  // section of regular Variable
  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException  {
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
      Object data = smProxy.getFillValue( getDataType());
      return Array.factoryConstant( dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    return (enhanceMode ==  NetcdfDataset.EnhanceMode.ScaleMissing) ? convert(result) : result;
  }

  // structure-member Variables.
  /*
  protected Array _readMemberData(Section section, boolean flatten) throws IOException, InvalidRangeException  {
    Array result;
    //if (agg != null)
    //  result = agg.readMemberData( this, null);
    //else
    /* if (ioVar != null)
      result = ioVar.readAllStructures(section, flatten);
    else /
      result = super._readMemberData(section, flatten);

    // look should do recursively
    /* if (smProxy.hasScaleOffset())
      result = smProxy.convertScaleOffset( result);
    else if (smProxy.hasMissing() && smProxy.getUseNaNs())
      result = smProxy.convertMissing( result); /

    return result;
  }  */

}