/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.CancelTask;
import ucar.ma2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An "enhanced" Structure.
 *
 * @author john caron
 * @see NetcdfDataset
 */

public class StructureDS extends ucar.nc2.Structure implements VariableEnhanced {
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDS.class);

  private EnhancementsImpl proxy; // API relies that this cant be null

  protected Structure orgVar; // wrap this Variable
  private String orgName; // in case Variable wwas renamed, and we need the original name for aggregation

  protected StructureDS(NetcdfFile ncfile, Group group, String shortName) {
    super(ncfile, group, null, shortName);
    this.proxy = new EnhancementsImpl(this);
  }

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
      addAttribute(new Attribute(CDM.UNITS, units));
    if (desc != null)
      addAttribute(new Attribute(CDM.LONG_NAME, desc));
  }

  /**
   * Create a StructureDS thats wraps a Structure
   *
   * @param g      parent group
   * @param orgVar original Structure
   */
  public StructureDS(Group g, ucar.nc2.Structure orgVar) { // , boolean reparent) {
    super(orgVar);
    setParentGroup(g);
    this.orgVar = orgVar;
    this.proxy = new EnhancementsImpl(this);

    // dont share cache, iosp : all IO is delegated
    this.ncfile = null;
    this.spiObject = null;
    createNewCache();

    if (orgVar instanceof StructureDS)
      return;

    // all member variables must be wrapped, reparented
    List<Variable> newList = new ArrayList<>(members.size());
    for (Variable v : members) {
      Variable newVar = convertVariable(g, v);
      newVar.setParentStructure(this);
      newList.add(newVar);
    }
    setMemberVariables(newList);
  }

  private Variable convertVariable(Group g, Variable v) {
    Variable newVar;
    if (v instanceof Sequence) {
      newVar = new SequenceDS(g, (Sequence) v);
    } else if (v instanceof Structure) {
      newVar = new StructureDS(g, (Structure) v);
    } else {
      newVar = new VariableDS(g, v, false); // enhancement done later
    }
    return newVar;
  }

  /**
   * Wrap the given Structure, making it into a StructureDS.
   * Delegate data reading to the original variable.
   * Does not share cache, iosp.
   * This is for NcML explicit mode
   *
   * @param ds        the containing NetcdfDataset.
   * @param group     the containing group; may not be null
   * @param parent    parent Structure, may be null
   * @param shortName variable shortName, must be unique within the Group
   * @param orgVar    the original Structure to wrap.
   */
  public StructureDS(NetcdfDataset ds, Group group, Structure parent, String shortName, Structure orgVar) {
    super(ds, group, parent, shortName);

    // dont share cache, iosp : all IO is delegated
    // this.ncfile = null;
    this.spiObject = null;
    createNewCache();

    this.orgVar = orgVar;
    this.proxy = new EnhancementsImpl(this);
  }

  // for section and slice and select

  @Override
  protected Variable copy() {
    return new StructureDS(getParentGroup(), this);
  }

  // copy() doesnt work because convert gets called twice

  @Override
  public Structure select(List<String> memberNames) {
    StructureDS result = new StructureDS(getParentGroup(), orgVar);
    List<Variable> members = new ArrayList<>();
    for (String name : memberNames) {
      Variable m = findVariable(name);
      if (null != m) members.add(m);
    }
    result.setMemberVariables(members);
    result.isSubset = true;
    return result;
  }

  /**
   * A StructureDS may wrap another Structure.
   *
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
      throw new IllegalArgumentException("StructureDS must wrap a Structure; name=" + orgVar.getFullName());
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
   *
   * @return original Variable's DataType
   */
  public String getOriginalName() {
    return orgName;
  }

  @Override
  public String setName(String newName) {
    this.orgName = getShortName();
    setShortName(newName);
    return newName;
  }

  // regular Variables.

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Array result;

    if (hasCachedData())
      result = super.reallyRead(client, cancelTask);
    else if (orgVar != null)
      result = orgVar.read();
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), getShape(), data);
    }

    return convert(result, null);
  }

  // section of regular Variable

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    if (section.computeSize() == getSize())
      return _read();

    Array result;
    if (hasCachedData())
      result = super.reallyRead(client, section, cancelTask);
    else if (orgVar != null)
      result = orgVar.read(section);
    else {
      throw new IllegalStateException("StructureDS has no way to get data");
      //Object data = smProxy.getFillValue(getDataType());
      //return Array.factoryConstant(dataType.getPrimitiveClassType(), section.getShape(), data);
    }

    // do any needed conversions (enum/scale/offset/missing/unsigned, etc)
    return convert(result, section);
  }

  ///////////////////////////////////////

  // is conversion needed?

  private boolean convertNeeded(StructureMembers smData) {

    for (Variable v : getVariables()) {

      if (v instanceof VariableDS) {
        VariableDS vds = (VariableDS) v;
        if (vds.needConvert())
          return true;
      } else if (v instanceof StructureDS) {
        StructureDS nested = (StructureDS) v;
        if (nested.convertNeeded(null))
          return true;
      }

      // a variable with no data in the underlying smData
      if ((smData != null) && !varHasData(v, smData))
        return true;
    }

    return false;
  }

  // possible things needed:
  //   1) enum/scale/offset/missing/unsigned conversion
  //   2) name, info change
  //   3) variable with cached data added to StructureDS through NcML

  protected ArrayStructure convert(Array data, Section section) throws IOException {
    ArrayStructure orgAS = (ArrayStructure) data;
    if (!convertNeeded(orgAS.getStructureMembers())) {
      // name, info change only
      convertMemberInfo(orgAS.getStructureMembers());
      return orgAS;
    }

    // LOOK! converting to ArrayStructureMA
    // do any enum/scale/offset/missing/unsigned conversions
    ArrayStructure newAS = ArrayStructureMA.factoryMA(orgAS);
    for (StructureMembers.Member m : newAS.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      if ((v2 == null) && (orgVar != null)) // these are from orgVar - may have been renamed
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) continue;

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        if (vds.needConvert()) {
          Array mdata = newAS.extractMemberArray(m);
          // mdata has not yet been enhanced, but vds would *think* that it has been if we used the 1-arg version of
          // VariableDS.convert(). So, we use the 2-arg version to explicitly request enhancement.
          mdata = vds.convert(mdata, vds.getEnhanceMode());
          newAS.setMemberArray(m, mdata);
        }

      } else if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        if (innerStruct.convertNeeded(null)) {

          if (innerStruct.getDataType() == DataType.SEQUENCE) {
            ArrayObject.D1 seqArray = (ArrayObject.D1) newAS.extractMemberArray(m);
            ArrayObject.D1 newSeq = (ArrayObject.D1) Array.factory(DataType.SEQUENCE, new int[] {(int) seqArray.getSize()});
            m.setDataArray(newSeq); // put back into member array

            // wrap each Sequence
            for (int i = 0; i < seqArray.getSize(); i++) {
              ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
              newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
            }

            // non-Sequence Structures
          } else {
            Array mdata = newAS.extractMemberArray(m);
            mdata = innerStruct.convert(mdata, null);
            newAS.setMemberArray(m, mdata);
          }

        }

        // always convert the inner StructureMembers
        innerStruct.convertMemberInfo(m.getStructureMembers());
      }
    }

    StructureMembers sm = newAS.getStructureMembers();
    convertMemberInfo(sm);

    // check for variables that have been added by NcML
    for (Variable v : getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = getParentGroup().findVariable(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(section);
          StructureMembers.Member m = sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          newAS.setMemberArray(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return newAS;
  }

  /* convert original structureData to one that conforms to this Structure */

  protected StructureData convert(StructureData orgData, int recno) throws IOException {
    if (!convertNeeded(orgData.getStructureMembers())) {
      // name, info change only
      convertMemberInfo(orgData.getStructureMembers());
      return orgData;
    }

    // otherwise we create a new StructureData and convert to it. expensive
    StructureMembers smResult = new StructureMembers(orgData.getStructureMembers());
    StructureDataW result = new StructureDataW(smResult);

    for (StructureMembers.Member m : orgData.getMembers()) {
      VariableEnhanced v2 = (VariableEnhanced) findVariable(m.getName());
      if ((v2 == null) && (orgVar != null))     // why ?
        v2 = findVariableFromOrgName(m.getName());
      if (v2 == null) {
        findVariableFromOrgName(m.getName());  // debug
        // log.warn("StructureDataDS.convert Cant find member " + m.getName());
        continue;
      }
      StructureMembers.Member mResult = smResult.findMember(m.getName());

      if (v2 instanceof VariableDS) {
        VariableDS vds = (VariableDS) v2;
        Array mdata = orgData.getArray(m);

        if (vds.needConvert())
          // mdata has not yet been enhanced, but vds would *think* that it has been if we used the 1-arg version of
          // VariableDS.convert(). So, we use the 2-arg version to explicitly request enhancement.
          mdata = vds.convert(mdata, vds.getEnhanceMode());

        result.setMemberData(mResult, mdata);
      }

      // recurse into sub-structures
      if (v2 instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v2;
        // if (innerStruct.convertNeeded(null)) {

        if (innerStruct.getDataType() == DataType.SEQUENCE) {
          Array a = orgData.getArray(m);

          if (a instanceof ArrayObject.D1) { // LOOK when does this happen vs ArraySequence?
            ArrayObject.D1 seqArray = (ArrayObject.D1) a;
            ArrayObject.D1 newSeq = (ArrayObject.D1) Array.factory(DataType.SEQUENCE, new int[] {(int) seqArray.getSize()});
            mResult.setDataArray(newSeq); // put into result member array

            for (int i = 0; i < seqArray.getSize(); i++) {
              ArraySequence innerSeq = (ArraySequence) seqArray.get(i); // get old ArraySequence
              newSeq.set(i, new SequenceConverter(innerStruct, innerSeq)); // wrap in converter
            }

          } else {
            ArraySequence seqArray = (ArraySequence) a;
            result.setMemberData(mResult, new SequenceConverter(innerStruct, seqArray)); // wrap in converter
          }

          // non-Sequence Structures
        } else {
          Array mdata = orgData.getArray(m);
          mdata = innerStruct.convert(mdata, null);
          result.setMemberData(mResult, mdata);
        }
        //}

        // always convert the inner StructureMembers
        innerStruct.convertMemberInfo(mResult.getStructureMembers());
      }
    }

    StructureMembers sm = result.getStructureMembers();
    convertMemberInfo(sm);

    // check for variables that have been added by NcML
    for (Variable v : getVariables()) {
      if (!varHasData(v, sm)) {
        try {
          Variable completeVar = getParentGroup().findVariable(v.getShortName()); // LOOK BAD
          Array mdata = completeVar.read(new Section().appendRange(recno, recno));
          StructureMembers.Member m = sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), v.getShape());
          result.setMemberData(m, mdata);
        } catch (InvalidRangeException e) {
          throw new IOException(e.getMessage());
        }
      }
    }

    return result;
  }

  // the wrapper StructureMembers must be converted to correspond to the wrapper Structure

  private void convertMemberInfo(StructureMembers wrapperSm) {
    for (StructureMembers.Member m : wrapperSm.getMembers()) {
      Variable v = findVariable(m.getName());
      if ((v == null) && (orgVar != null)) // may have been renamed
        v = (Variable) findVariableFromOrgName(m.getName());

      if (v != null) { // a section will have missing variables LOOK wrapperSm probably wrong in that case
        //  log.error("Cant find " + m.getName());
        //else
        m.setVariableInfo(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType());
      }

      // nested structures
      if (v instanceof StructureDS) {
        StructureDS innerStruct = (StructureDS) v;
        innerStruct.convertMemberInfo(m.getStructureMembers());
      }

    }
  }

  // look for the top variable that has an orgVar with the wanted orgName

  private VariableEnhanced findVariableFromOrgName(String orgName) {
    for (Variable vTop : getVariables()) {
      Variable v = vTop;
      while (v instanceof VariableEnhanced) {
        VariableEnhanced ve = (VariableEnhanced) v;
        if ((ve.getOriginalName() != null) && (ve.getOriginalName().equals(orgName)))
          return (VariableEnhanced) vTop;
        v = ve.getOriginalVariable();
      }
    }
    return null;
  }

  // verify that the variable has data in the data array

  private boolean varHasData(Variable v, StructureMembers sm) {
    if (sm.findMember(v.getShortName()) != null) return true;
    while (v instanceof VariableEnhanced) {
      VariableEnhanced ve = (VariableEnhanced) v;
      if (sm.findMember(ve.getOriginalName()) != null) return true;
      v = ve.getOriginalVariable();
    }
    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class SequenceConverter extends ArraySequence {
    StructureDS orgStruct;
    ArraySequence orgSeq;

    SequenceConverter(StructureDS orgStruct, ArraySequence orgSeq) {
      super(orgSeq.getStructureMembers(), orgSeq.getShape());
      this.orgStruct = orgStruct;
      this.orgSeq = orgSeq;
      this.nelems = orgSeq.getStructureDataCount();

      // copy and convert the members
      members = new StructureMembers(orgSeq.getStructureMembers());
      orgStruct.convertMemberInfo(members);
    }

    @Override
    public StructureDataIterator getStructureDataIterator() { // throws java.io.IOException {
      return new StructureDataConverter(orgStruct, orgSeq.getStructureDataIterator());
    }
  }

  private static class StructureDataConverter implements StructureDataIterator {
    private StructureDataIterator orgIter;
    private StructureDS newStruct;
    private int count = 0;

    StructureDataConverter(StructureDS newStruct, StructureDataIterator orgIter) {
      this.newStruct = newStruct;
      this.orgIter = orgIter;
    }

    @Override
    public boolean hasNext() throws IOException {
      return orgIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      StructureData sdata = orgIter.next();
      return newStruct.convert(sdata, count++);
    }

    @Override
    public void setBufferSize(int bytes) {
      orgIter.setBufferSize(bytes);
    }

    @Override
    public StructureDataIterator reset() {
      orgIter = orgIter.reset();
      return (orgIter == null) ? null : this;
    }

    @Override
    public int getCurrentRecno() {
      return orgIter.getCurrentRecno();
    }

    @Override
    public void close() {
      orgIter.close();
    }
  }

  /* public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    StructureDataIterator iter = orgVar.getStructureIterator(bufferSize);
    return new StructureDataConverter(this, iter);
  } */

  /* public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
     //StructureDataIterator iter = ncfile.getStructureIterator(this, bufferSize);
     //return (iter != null) ? iter : new Structure.Iterator(bufferSize);
     return new Structure.Iterator(bufferSize);
   } */

  /* Experimental - convert entire original array at once, to avoid one-by-one StructureData conversion.
private class Iterator implements StructureDataIterator {
  private int count = 0;
  private int recnum = (int) getSize();
  private int readStart = 0;
  private int readCount = 0;
  private int readAtaTime;
  private ArrayStructure as = null;
  private Index ii = null;

  protected Iterator(int bufferSize) {
    setBufferSize( bufferSize);
    reset();
  }

  public boolean hasNext() { return count < recnum; }

  public StructureDataIterator reset() {
    count = 0;
    readStart = 0;
    readCount = 0;
    ii = Index.factory(shape); // convert to nD index

    return this;
  }

  public StructureData next() throws IOException {
    if (count >= readStart) {
      if (getRank() == 1) readNextRank1();
      else readNextGeneralRank();
    }

    count++;
    return as.getStructureData( readCount++);
  }

  private void readNextRank1() throws IOException {
    int left = Math.min(recnum, readStart+readAtaTime); // dont go over recnum
    int need = left - readStart; // how many to read this time
    try {
      // System.out.println(" read start= "+readStart+" count= "+need);
      as = readStructure( readStart, need);

    } catch (InvalidRangeException e) {
      log.error("Structure.Iterator.readNext() ",e);
      throw new IllegalStateException("Structure.Iterator.readNext() ",e);
    } // cant happen

    readStart += need;
    readCount = 0;
  }

  private void readNextGeneralRank() throws IOException {
   ii.setCurrentCounter(index);
   int[] origin = ii.getCurrentCounter();
   section = new Section();
   for (int i=0;i<origin.length;i++)
     section.appendRange(origin[i], origin[i]);
  }

  public void setBufferSize(int bytes) {
    if (count > 0) return; // too late
    int structureSize = calcStructureSize();
    if (bytes <= 0)
      bytes = defaultBufferSize;
    readAtaTime = Math.max( 10, bytes / structureSize);
  }

}  */

  ///////////////////////////////////////////////////////////

  /**
   * DO NOT USE DIRECTLY. public by accident.
   * recalc any enhancement info
   */
  public void enhance(Set<NetcdfDataset.Enhance> mode) {
    for (Variable v : getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      ve.enhance(mode);
    }
  }

  public void clearCoordinateSystems() {
    this.proxy = new EnhancementsImpl(this, getUnitsString(), getDescription());
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

  public void setUnitsString(String units) {
    proxy.setUnitsString(units);
  }

}
