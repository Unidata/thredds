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
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;

import java.util.List;
import java.util.ArrayList;
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
 * @deprecated use ucar.nc2.dataset.StructurePseudoDS
 * @author caron
 */
public class StructurePseudo extends Structure {
  private static boolean debugRecord = false;
  private List<Variable> orgVariables =  new ArrayList<Variable>(); // the underlying original variables

  /** Make a Structure out of all Variables with the named dimension as their outermost dimension.
   *
   * @param ncfile part of this file
   * @param group part of this group
   * @param shortName short name of this Structure
   * @param dim the existing dimension
   */
  public StructurePseudo( NetcdfFile ncfile, Group group, String shortName, Dimension dim) {
    super (ncfile, group, null, shortName); // cant do this for nested structures
    this.dataType = DataType.STRUCTURE;
    setDimensions( dim.getName());

    if (group == null)
      group = ncfile.getRootGroup();

    // find all variables in this group that has this as the outer dimension
    List<Variable> vars = group.getVariables();
    for (Variable orgV : vars) {
      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(dim)) {
        Variable memberV = new Variable(ncfile, group, this, orgV.getShortName());
        memberV.setDataType(orgV.getDataType());
        memberV.setSPobject(orgV.getSPobject()); // ??
        memberV.attributes.addAll(orgV.getAttributes());

        List<Dimension> dims = new ArrayList<Dimension>(orgV.dimensions);
        dims.remove(0); //remove outer dimension
        memberV.setDimensions(dims);

        addMemberVariable(memberV);
        orgVariables.add(orgV);
      }
    }

    calcElementSize();
  }

  /** Make a Structure out of named Variables, each has the same named outermost dimension.
   *
   * @param ncfile part of this file
   * @param group part of this group
   * @param shortName short name of this Structure
   * @param varNames limited to these variables. all must have dim as outer dimension.
   * @param dim the existing dimension
   */
  public StructurePseudo( NetcdfFile ncfile, Group group, String shortName, List<String> varNames, Dimension dim) {
    super (ncfile, group, null, shortName); // cant do this for nested structures
    this.dataType = DataType.STRUCTURE;
    setDimensions( dim.getName());

    if (group == null)
      group = ncfile.getRootGroup();

    // use the list of varnames that were passed in
    for (String name : varNames) {
      Variable orgV = group.findVariable(name);
      if (orgV == null) {
        log.warn("StructurePseudo cannot find variable "+name);
        continue; // skip - should log message
      }

      Dimension dim0 = orgV.getDimension(0);
      if (!dim0.equals(dim)) throw new IllegalArgumentException("Variable "+orgV.getNameAndDimensions()+" must have outermost dimension="+dim);

      Variable memberV = new Variable(ncfile, group, this, orgV.getShortName());
      memberV.setDataType(orgV.getDataType());
      memberV.setSPobject(orgV.getSPobject()); // ??
      memberV.attributes.addAll(orgV.getAttributes());

      List<Dimension> dims = new ArrayList<Dimension>(orgV.dimensions);
      dims.remove(0); //remove outer dimension
      memberV.setDimensions(dims);

      addMemberVariable(memberV);
      orgVariables.add(orgV);
    }

    calcElementSize();
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
