/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.io.IOException;

/**
 * Make a collection of variables with the same outer dimension into a fake Structure.
 * Its fake because the variables are not stored contiguously.
 * <pre>
 *  so
 *   var1(dim, other);
 *   var2(dim, other);
 *   var3(dim, other);
 * becomes
 *   struct {
 *     var1(other);
 *     var2(other);
 *     var3(other);
 *   } name(dim);
 * </pre>
 * @author caron
 */
public class StructurePseudoDS extends StructureDS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructurePseudoDS.class);
  private static boolean debugRecord = false;
  protected static final Set<NetcdfDataset.Enhance> enhanceScaleMissing = EnumSet.of(NetcdfDataset.Enhance.ScaleMissing);


  private List<Variable> orgVariables =  new ArrayList<Variable>(); // the underlying original variables

  protected StructurePseudoDS( NetcdfDataset ncfile, Group group, String shortName) {
    super (ncfile, group, shortName);
  }

  /**
   * Make a Structure out of all Variables with the named dimension as their outermost dimension, or from a list
   *  named Variables, each has the same named outermost dimension.
   *
   * @param ncfile part of this file
   * @param group part of this group
   * @param shortName short name of this Structure
   * @param varNames limited to these variables, all must have dim as outer dimension. If null, use all Variables
   *   with that outer dimension
   * @param outerDim existing, outer dimension
   */
  public StructurePseudoDS(NetcdfDataset ncfile, Group group, String shortName, List<String> varNames, Dimension outerDim) {
    super(ncfile, group, shortName); // cant do this for nested structures
    setDimensions(outerDim.getName());

    if (group == null)
      group = ncfile.getRootGroup();

    if (varNames == null) {
      List<Variable> vars = group.getVariables();
      varNames = new ArrayList<String>(vars.size());
      for (Variable orgV : vars) {
        if (orgV.getDataType() == DataType.STRUCTURE) continue;
        
        Dimension dim0 = orgV.getDimension(0);
        if ((dim0 != null) && dim0.equals(outerDim))
          varNames.add(orgV.getShortName());
      }
    }

    for (String name : varNames) {
      Variable orgV = group.findVariable(name);
      if (orgV == null) {
        log.warn("StructurePseudoDS cannot find variable " + name);
        continue;
      }

      Dimension dim0 = orgV.getDimension(0);
      if (!outerDim.equals(dim0))
        throw new IllegalArgumentException("Variable " + orgV.getNameAndDimensions() + " must have outermost dimension=" + outerDim);

      VariableDS memberV = new VariableDS(ncfile, group, this, orgV.getShortName(), orgV.getDataType(), null,
          orgV.getUnitsString(), orgV.getDescription());
      memberV.setSPobject(orgV.getSPobject()); // ??
      memberV.getAttributes().addAll(orgV.getAttributes());

      List<Dimension> dims = new ArrayList<Dimension>(orgV.getDimensions());
      dims.remove(0); //remove outer dimension
      memberV.setDimensions(dims);

      memberV.enhance(enhanceScaleMissing);

      addMemberVariable(memberV);
      orgVariables.add(orgV);
    }

    calcElementSize();
  }

  @Override
  protected Variable copy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Structure select(List<String> memberNames) {
    StructurePseudoDS result = new StructurePseudoDS((NetcdfDataset) ncfile, group, getShortName(), memberNames, getDimension(0));   
    result.isSubset = true;
    return result;
  }

  @Override
  public boolean removeMemberVariable( Variable v) {
    if (super.removeMemberVariable(v)) {
      java.util.Iterator<Variable> iter = orgVariables.iterator();
      while (iter.hasNext()) {
        Variable mv =  iter.next();
        if (mv.getShortName().equals(v.getShortName())) {
          iter.remove();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
    if (debugRecord) System.out.println(" read all psuedo records ");
    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA( smembers, getShape());

    for (Variable v : orgVariables) {
      Array data = v.read();
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }

  @Override
  public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException  {
    if (null == section)
      return _read();

    if (debugRecord) System.out.println(" read psuedo records "+ section.getRange(0));

    String err = section.checkInRange(getShape());
    if (err != null)
      throw new InvalidRangeException(err);

    Range r = section.getRange(0);

    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA( smembers, section.getShape());

    for (Variable v : orgVariables) {
      List<Range> vsection =  new ArrayList<Range>(v.getRanges());
      vsection.set(0, r);
      Array data = v.read(vsection); // LOOK should these be flattened ??
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }


}
