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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Make a collection of variables with the same 2 outer dimensions into a fake 2D Structure(outer,inner)
 *
 * @author caron
 * @since Oct 21, 2009
 */


public class StructurePseudo2Dim extends StructurePseudoDS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructurePseudo2Dim.class);
  private List<Variable> orgVariables = new ArrayList<Variable>();
  private boolean debugRecord = false;

  /** Make a Structure out of named Variables which have var(outer, inner, ...)
   *
   * @param ncfile the contaning file
   * @param group the containing group, if null use root group
   * @param shortName short name of this Structure
   * @param varNames limited to these variables. all must var(outer, inner, ...). If null, then find all such variables.
   * @param outer the outer dimension, may not be null
   * @param inner the inner dimension, may not be null
   */
  public StructurePseudo2Dim( NetcdfDataset ncfile, Group group, String shortName, List<String> varNames, Dimension outer, Dimension inner) {
    super (ncfile, group, shortName);
    this.dataType = DataType.STRUCTURE;
    ArrayList<Dimension> dims = new ArrayList<Dimension>(2);
    dims.add(outer);
    dims.add(inner);
    setDimensions( dims);

    if (group == null)
      group = ncfile.getRootGroup();

    // find all variables in this group that has this as the outer dimension
    if (varNames == null) {
      List<Variable> vars = group.getVariables();
      varNames = new ArrayList<String>(vars.size());
      for (Variable orgV : vars) {
        if (orgV.getRank() < 2) continue;
        if (outer.equals(orgV.getDimension(0)) && inner.equals(orgV.getDimension(1)))
          varNames.add(orgV.getShortName());
      }
    }

    for (String name : varNames) {
      Variable orgV = group.findVariable(name);
      if (orgV == null) {
        log.warn("StructurePseudo2Dim cannot find variable "+name);
        continue;
      }

      if (!outer.equals(orgV.getDimension(0)))
        throw new IllegalArgumentException("Variable "+orgV.getNameAndDimensions()+" must have outermost dimension="+outer);
      if (!inner.equals(orgV.getDimension(1)))
        throw new IllegalArgumentException("Variable "+orgV.getNameAndDimensions()+" must have 2nd dimension="+inner);

      VariableDS memberV = new VariableDS(ncfile, group, this, orgV.getShortName(), orgV.getDataType(), null,
          orgV.getUnitsString(), orgV.getDescription());
       memberV.setDataType(orgV.getDataType());
      memberV.setSPobject(orgV.getSPobject()); // ??
      memberV.getAttributes().addAll(orgV.getAttributes());

      List<Dimension> dimList = new ArrayList<Dimension>(orgV.getDimensions());
      memberV.setDimensions( dimList.subList(2, dimList.size())); // remove first 2 dimensions
      memberV.enhance(enhanceScaleMissing);

      addMemberVariable(memberV);
      orgVariables.add(orgV);
    }

    calcElementSize();
  }

  @Override
  public Structure select(List<String> memberNames) {
    StructurePseudo2Dim result = new StructurePseudo2Dim((NetcdfDataset) ncfile, group, getShortName(), memberNames, getDimension(0), getDimension(1));
    result.isSubset = true;
    return result;
  }

  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException  {
    if (null == section)
      return _read();

    if (debugRecord) System.out.println(" read psuedo records "+ section.getRange(0));

    String err = section.checkInRange(getShape());
    if (err != null)
      throw new InvalidRangeException(err);

    Range outerRange = section.getRange(0);
    Range innerRange = section.getRange(1);

    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA( smembers, section.getShape());

    for (Variable v : orgVariables) {
      List<Range> vsection =  new ArrayList<Range>(v.getRanges());
      vsection.set(0, outerRange);
      vsection.set(1, innerRange);
      Array data = v.read(vsection); // LOOK should these be flattened ??
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }


}
