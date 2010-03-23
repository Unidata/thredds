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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

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

  protected Variable orgVar; // wrap this Variable : use it for the I/O
  protected DataType orgDataType; // keep seperate for the case where there is no ioVar.
  protected String orgName; // in case Variable was renamed, and we need to keep track of the original name

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
    // this.orgDataType = dataType;

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
   * Does not share cache, iosp, proxies.
   *
   * @param g logical container, if null use orgVar's group
   * @param orgVar the original Variable to wrap. The original Variable is not modified.
   *    Must not be a Structure, use StructureDS instead.
   * @param enhance if true, use NetcdfDataset.defaultEnhanceMode to define what enhancements are made.
   *   Note that this can change DataType and data values.
   *   You can also call enhance() later. If orgVar is VariableDS, then enhance is inherited from there,
   *   and this parameter is ignored.
   */
  public VariableDS(Group g, Variable orgVar, boolean enhance) {
    super(orgVar);
    if (g != null) this.group = g; // otherwise super() sets group; this affects the long name and the dimensions.
    setDimensions( getDimensionsString()); // reset the dimensions

    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+orgVar.getName());

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    createNewCache();

    this.orgVar = orgVar;
    this.orgDataType = orgVar.getDataType();

    if (orgVar instanceof VariableDS) {
      VariableDS ncVarDS = (VariableDS) orgVar;
      this.enhanceProxy = ncVarDS.enhanceProxy;
      this.scaleMissingProxy = ncVarDS.scaleMissingProxy;
      this.enhanceMode = ncVarDS.enhanceMode;

    } else {
      this.enhanceProxy = new EnhancementsImpl( this);
      if (enhance) {
        enhance(NetcdfDataset.getDefaultEnhanceMode());
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
    setDimensions( getDimensionsString()); // reset the dimensions

    if (orgVar instanceof Structure)
      throw new IllegalArgumentException("VariableDS must not wrap a Structure; name="+orgVar.getName());

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
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
   * Remove coordinate system info.
   */
  public void clearCoordinateSystems() {
    this.enhanceProxy = new EnhancementsImpl( this, getUnitsString(), getDescription());
  }

  /**
   * DO NOT USE DIRECTLY. public by accident.
   * Calculate scale/offset/missing value info. This may change the DataType.
   */
  public void enhance(Set<NetcdfDataset.Enhance> mode) {
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
    if (!alreadyScaleOffsetMissing && (dataType.isNumeric() || dataType == DataType.CHAR) && 
        mode.contains(NetcdfDataset.Enhance.ScaleMissing) || mode.contains(NetcdfDataset.Enhance.ScaleMissingDefer)) {
      this.scaleMissingProxy = new EnhanceScaleMissingImpl( this);

      // promote the data type if ScaleMissing is set
      if (mode.contains(NetcdfDataset.Enhance.ScaleMissing) &&
          scaleMissingProxy.hasScaleOffset() && (scaleMissingProxy.getConvertedDataType() != getDataType())) {
        setDataType( scaleMissingProxy.getConvertedDataType());
        removeAttributeIgnoreCase("_Unsigned");
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
      removeAttributeIgnoreCase("_Unsigned");
    }

  }

  boolean getNeedScaleOffsetMissing() { return needScaleOffsetMissing; }
  boolean getNeedEnumConversion() { return needEnumConversion; }

  /** Get the enhancement mode
   * @return the enhancement mode
   */
  public EnumSet<NetcdfDataset.Enhance> getEnhanceMode() { return enhanceMode; }

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
   * @return original Variable's DataType, or current data type if it doesnt wrap anothe rvariable
   */
  public DataType getOriginalDataType() {
    return orgDataType != null ? orgDataType : getDataType();
  }

  /**
   * When this wraps another Variable, get the original Variable's name.
   * @return original Variable's name
   */
  public String getOriginalName() {
    return orgName;
  }

  public String lookupEnumString(int val) {
    if (dataType.isEnum())
      return super.lookupEnumString(val);
    return orgVar.lookupEnumString(val);
  }

  @Override
  public void setName(String newName) {
    this.orgName = shortName;
    super.setName(newName);
  }

  public String toStringDebug() {
    return (orgVar != null) ? orgVar.toStringDebug() : "";
  }

  // regular Variables.
  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Array result;

    if (hasCachedData()) // ??
      result = super.reallyRead(client, cancelTask);
    else if (orgVar != null)
      result = orgVar.read();
    else if ((proxyReader != null) && (proxyReader != this))
      result = proxyReader.reallyRead(this, cancelTask);
    else
      return getMissingDataArray(shape);

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
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException  {
    // see if its really a full read
    if ((null == section) || section.computeSize() == getSize())
      return _read();
    
    Array result;
    if (hasCachedData()) // ??
      result = super.reallyRead(client, section, cancelTask);
    else if (orgVar != null)
      result = orgVar.read(section);
    else if ((proxyReader != null) && (proxyReader != this))
      result = proxyReader.reallyRead(this, section, cancelTask);
    else
      return getMissingDataArray(section.getShape());

    if (needScaleOffsetMissing)
      return convertScaleOffsetMissing(result);
    else if (needEnumConversion)
      return convertEnums(result);
    else
      return result;
  }

  /**
   * Return Array with missing data
   * @param shape of this shape
   * @return Array with given shape
   */
  public Array getMissingDataArray(int[] shape) {
    Object data = scaleMissingProxy.getFillValue( getDataType());
    return Array.factoryConstant( dataType.getPrimitiveClassType(), shape, data);
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