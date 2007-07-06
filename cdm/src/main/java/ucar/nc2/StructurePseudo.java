/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Make a collection of variables with the same outer dimension into a fake Structure.
 * Its fake because the variables are not stored contiguously.
 * @author caron
 */
public class StructurePseudo extends Structure {
  private List<Variable> orgVariables =  new ArrayList<Variable>();
  private boolean debugRecord = false;

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
        Variable memberV = new Variable(ncfile, group, this, orgV.getName());
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

  ///////////////
  // internal reads: all other calls go through these.
  // subclasses must override, so that NetcdfDataset wrapping will work.

  @Override
  protected Array _read() throws IOException {
    if (debugRecord) System.out.println(" read all psuedo records ");
    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA( smembers, getShape());

    for (Variable v : orgVariables) {
      Array data = v.read();
      StructureMembers.Member m = smembers.findMember(v.getName());
      m.setDataObject(data);
    }

    return asma;
  }

  // section of non-structure-member Variable
  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException  {
    if (null == section)
      return _read();

    if (debugRecord) System.out.println(" read psuedo records "+ section.getRange(0));

    String err = section.checkInRange(getShape());
    if (err != null)
      throw new InvalidRangeException(err);

    Range r = section.getRange(0);

    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA( smembers, getShape());

    for (Variable v : orgVariables) {
      List<Range> vsection =  v.getRanges();
      vsection.set(0, r);
      Array data = v.read(vsection);
      StructureMembers.Member m = smembers.findMember(v.getName());
      m.setDataObject(data);
    }

    return asma;
  }

  /**
   * Not allowed.
   * @throws InvalidRangeException
   */
  protected Array _readMemberData(List section, boolean flatten) throws IOException, InvalidRangeException  {
    throw new UnsupportedOperationException();
  }

}
