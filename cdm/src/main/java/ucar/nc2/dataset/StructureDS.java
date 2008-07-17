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
import ucar.ma2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;

/**
 * An "enhanced" Structure.
 *
 * @see NetcdfDataset
 * @author john caron
 */

public class StructureDS extends ucar.nc2.Structure implements VariableEnhanced {
  private EnhancementsImpl proxy;

  protected Structure orgVar; // wrap this Variable
  private String orgName; // in case Variable wwas renamed, and we need the original name for aggregation

  /**
   * Constructor when theres no underlying variable. You better set the values too!
   *
   * @param ds              the containing NetcdfDataset.
   * @param group           the containing group; if null, use rootGroup
   * @param parentStructure parent Structure, may be null
   * @param shortName       variable shortName, must be unique within the Group
   * @param dims            list of dimension names, space delimited
   * @param units           unit string (may be null)
   * @param desc            description (may be null)
   */
  public StructureDS(NetcdfDataset ds, Group group, Structure parentStructure, String shortName,
                     String dims, String units, String desc) {

    super(ds, group, parentStructure, shortName);
    setDimensions(dims);
    this.proxy = new EnhancementsImpl(this, units, desc);

    if (units != null)
      addAttribute(new Attribute("units", units));
    if (desc != null)
      addAttribute(new Attribute("long_name", desc));
  }

  /**
   * Create a StructureDS thats wraps a Structure
   * @param g parent group
   * @param orgVar original Structure
   */
  public StructureDS(Group g, ucar.nc2.Structure orgVar) { // , boolean reparent) {
    super(orgVar);
    this.group = g;
    this.orgVar = orgVar;
    this.proxy = new EnhancementsImpl(this);

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    this.preReader = null;
    this.postReader = null;
    createNewCache();

    if (orgVar instanceof StructureDS)
      return;

    // all member variables must be wrapped, reparented
    List<Variable> newList = new ArrayList<Variable>(members.size());
    for (Variable v : members) {
      Variable newVar = (v instanceof Structure) ? new StructureDS(g, (Structure) v) : new VariableDS(g, v, false);
      newVar.setParentStructure(this);
      newList.add(newVar);
    }
    setMemberVariables(newList);
  }

  // for section and slice
  @Override
  protected Variable copy() {
    return new StructureDS(null, this);
  }

  @Override
  public Structure select( List<Variable> members) {
    StructureDS result = new StructureDS(group, orgVar);
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * Create a subset of the Structure consisting only of the one member variable
   * @param v Variable
   * @return subsetted Structure
   */
  @Override
  public Structure select( Variable v) {
    StructureDS result = new StructureDS(group, orgVar);
    List<Variable> members = new ArrayList<Variable>(1);
    members.add(v);
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
  * A StructureDS may wrap another Structure.
  * @return original Structure or null
  */
  public ucar.nc2.Variable getOriginalVariable() {
    return orgVar;
  }

  /**
   * Set the Structure to wrap.
   *
   * @param orgVar original Variable, must be a Structure
   */
  public void setOriginalVariable(ucar.nc2.Variable orgVar) {
    if (!(orgVar instanceof Structure))
      throw new IllegalArgumentException("STructureDS must wrap a Structure; name=" + orgVar.getName());
    this.orgVar = (Structure) orgVar;
  }

  /**
   * When this wraps another Variable, get the original Variable's DataType.
   *
   * @return original Variable's DataType
   */
  public DataType getOriginalDataType() {
    return DataType.STRUCTURE;
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

  /**
   * Set the proxy reader.
   *
   * @param proxyReader set to this
   */
  public void setProxyReader(ProxyReader proxyReader) {
    this.postReader = proxyReader;
  }

  /**
   * Get the proxy reader, or null.
   *
   * @return return the proxy reader, if any
   */
  public ProxyReader getProxyReader() {
    return this.postReader;
  }

  // regular Variables.
  @Override
  protected Array _read() throws IOException {
    Array result;

    // LOOK caching ??

    if (hasCachedData())
      result = super._read();
    else if (postReader != null)
      result = postReader.read(this, null);
    else if (orgVar != null)
      result = orgVar.read();
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), getShape(), data);
    }

    return convert(result);
  }

  // section of regular Variable
  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException {
    Array result;

    if (hasCachedData())
      result = super._read(section);
    else if (postReader != null)
      result = postReader.read(this, section, null);
    else if (orgVar != null)
      result = orgVar.read(section);
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    // correct the member info
    ArrayStructure as = (ArrayStructure) result;
    StructureMembers sm = as.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      Variable v = findVariable(m.getName());
      if (v != null)
        m.setVariableInfo(v.getShortName(), v.getUnitsString(), v.getDescription());
    }

    return convert(result);
  }

  ///////////////////////////////////////

  protected Array convert(Array data) throws IOException {
    ArrayStructure as = (ArrayStructure) data;
    if (!convertNeeded(this, as)) return data;
    as = ArrayStructureMA.factoryMA( as); // LOOK! converting to ArrayStructureMA

    StructureMembers sm = as.getStructureMembers(); // these are from orgVar - may have been renamed
    for (StructureMembers.Member m : sm.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      if ((v2 == null) && (orgVar != null))
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) continue;

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        if (vds.getNeedScaleOffsetMissing()) {
          Array mdata = as.extractMemberArray(m);
          mdata = vds.convertScaleOffsetMissing(mdata);
          as.setMemberArray(m, mdata);

        } else if (vds.getNeedEnumConversion()) {
          Array mdata = as.extractMemberArray(m);
          System.out.print(" convert "+vds.getName()+" array="+mdata.hashCode());
          mdata = vds.convertEnums(mdata);
          as.setMemberArray(m, mdata);
          System.out.println(" to  array="+mdata.hashCode());
        }
      }

      // recurse into sub-structures
      if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        if (innerStruct.convertNeeded(innerStruct, null)) {

          if (innerStruct.getDataType() == DataType.SEQUENCE) {
            ArrayObject.D1 seqArray = (ArrayObject.D1) as.extractMemberArray(m);
            for (int i=0; i<seqArray.getSize(); i++) {
              ArraySequence innerSeq =  (ArraySequence) seqArray.get(i);
              innerStruct.convert( innerSeq); // modify each innerSequence as needed
            }

          // non-Sequence Structures
          } else {
            Array mdata = as.extractMemberArray(m);
            mdata = innerStruct.convert(mdata);
            as.setMemberArray(m, mdata);
          }
        }
      }
      m.setVariableInfo(v2.getShortName(), v2.getUnitsString(), v2.getDescription());

    }
    return as;
  }

  protected boolean convertNeeded(Structure s, ArrayStructure as) {

    if (as == null) { // check everything
      for (Variable v : s.getVariables()) {

        if (v instanceof VariableDS) {
          VariableDS vds = (VariableDS) v;
          if (vds.getNeedScaleOffsetMissing() || vds.getNeedEnumConversion())
            return true;
        }

        if (v instanceof StructureDS) {
          StructureDS nested = (StructureDS) v;
          if (convertNeeded(nested, null))
            return true;
        }
      }
      return false;
    }

    // only check the ones present in the ArrayStructure
    for (StructureMembers.Member m : as.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) s.findVariable(m.getName());
      if ((v2 == null) && (orgVar != null)) // tricky stuff in case NcML renamed the variable
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) continue;

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        if (vds.getNeedScaleOffsetMissing() || vds.getNeedEnumConversion())
          return true;
      }

      if (v2 instanceof StructureDS) {
        StructureDS nested = (StructureDS) v2;
        if (convertNeeded(nested, null))
          return true;
      }
    }
    return false;
  }

  private VariableEnhanced findVariableFromOrgName(String shortName) {
    for (Variable vTop : getVariables()) {
      Variable v = vTop;
      while (v instanceof VariableEnhanced) {
        Variable org = ((VariableEnhanced) v).getOriginalVariable();
        if (org.getShortName().equals(shortName))
          return (VariableEnhanced) vTop;
        v = org;
      }
    }
    return null;
  }

  /* private void convertNested(ArrayStructure as, StructureMembers.Member outerM, StructureDS innerStruct) {
    StructureMembers innerMembers = outerM.getStructureMembers(); // these are from orgVar - may have been renamed

    for (StructureMembers.Member innerM : innerMembers.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) innerStruct.findVariable( innerM.getName());
      assert v2 != null;

      if (v2.hasScaleOffset() || (v2.hasMissing() && v2.getUseNaNs())) {
        Array mdata = as.getMemberArray(innerM);
        mdata = v2.convert(mdata);
        as.setMemberArray(innerM, mdata);
      }

      // recurse into sub-structures
      // recurse into sub-structures
      if (v2 instanceof StructureDS) {
        StructureDS inner = (StructureDS) v2;
        if (inner.needsConverting(null)) {
          convertNested(as, innerM, inner);
        }
      }
      innerM.setVariableInfo(v2);
    }
  } */

  /**
   * DO NOT USE DIRECTLY. public by accident.
   * recalc any enhancement info
   */
  public void enhance(EnumSet<NetcdfDataset.Enhance> mode) {
    for (Variable v : getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      ve.enhance(mode);
    }
  }

  public void addCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.addCoordinateSystem(p0);
  }

  public void removeCoordinateSystem(ucar.nc2.dataset.CoordinateSystem p0) {
    proxy.removeCoordinateSystem(p0);
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

  /* public double getValidMax() {
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
    return smProxy.isFillValue(p0);
  }

  public boolean isInvalidData(double p0) {
    return smProxy.isInvalidData(p0);
  }

  public boolean isMissing(double val) {
    return smProxy.isMissing( val);
  }

  public boolean isMissingFast(double val) {
    return smProxy.isMissingFast( val);
  }

  public boolean isMissingValue(double p0) {
    return smProxy.isMissingValue(p0);
  }

  public void setFillValueIsMissing(boolean p0) {
    smProxy.setFillValueIsMissing(p0);
  }

  public void setInvalidDataIsMissing(boolean p0) {
    smProxy.setInvalidDataIsMissing(p0);
  }

  public void setMissingDataIsMissing(boolean p0) {
    smProxy.setMissingDataIsMissing(p0);
  }

  public void setUseNaNs(boolean useNaNs) {
    smProxy.setUseNaNs(useNaNs);
  }

  public boolean getUseNaNs() {
    return smProxy.getUseNaNs();
  }

  public double convertScaleOffsetMissing(byte value) {
    return smProxy.convertScaleOffsetMissing(value);
  }

  public double convertScaleOffsetMissing(short value) {
    return smProxy.convertScaleOffsetMissing(value);
  }

  public double convertScaleOffsetMissing(int value) {
    return smProxy.convertScaleOffsetMissing(value);
  }

  public double convertScaleOffsetMissing(long value) {
    return smProxy.convertScaleOffsetMissing(value);
  }

  public double convertScaleOffsetMissing(double value) {
    return smProxy.convertScaleOffsetMissing(value);
  } */

}
