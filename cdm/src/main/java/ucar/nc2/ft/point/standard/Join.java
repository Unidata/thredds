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
package ucar.nc2.ft.point.standard;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.point.StructureDataIteratorLinked;
import ucar.ma2.*;

import java.io.IOException;

/**
 * @author caron
 * @since Apr 21, 2008
 */
public class Join implements Comparable<Join> {

  public enum Type {
    ContiguousList, ForwardLinkedList, BackwardLinkedList, MultiDim, NestedStructure, Identity, Index, Singleton
  }

  protected TableConfig.JoinConfig config;
  protected NestedTable.Table parent, child;

  public Join(TableConfig.JoinConfig config) {
    this.config = config;
  }

  public void setTables(NestedTable.Table parent, NestedTable.Table child) {
    assert parent != null;
    assert child != null;
    this.parent = parent;
    this.child = child;
  }

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append( config.joinType);
    if (parent != null)
      sbuff.append(" from ").append(parent.getName());
    if (child != null)
      sbuff.append(" to ").append(child.getName());

    return sbuff.toString();
  }

  public int compareTo(Join o) {
    return config.joinType.compareTo(o.config.joinType);
  }

  // get the StructureDataIterator for the child table contained by the given parent
  public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {

    switch (config.joinType) {
      case NestedStructure: {
        String name = child.getName();
        StructureMembers members = parentStruct.getStructureMembers();
        StructureMembers.Member m = members.findMember(name);
        if (m.getDataType() == DataType.SEQUENCE) {
          ArraySequence2 seq = parentStruct.getArraySequence(m);
          return seq.getStructureDataIterator();

        } else if (m.getDataType() == DataType.STRUCTURE) {
          ArrayStructure as = parentStruct.getArrayStructure(m);
          return as.getStructureDataIterator();
        }

        return null;
      }

      case ForwardLinkedList:
      case BackwardLinkedList: {
        int firstRecno = parentStruct.getScalarInt( config.start);
        return new StructureDataIteratorLinked(child.struct, firstRecno, -1, config.next);
      }

      case ContiguousList: {
        int firstRecno = parentStruct.getScalarInt( config.start);
        int n = parentStruct.getScalarInt( config.numRecords);
        return new StructureDataIteratorLinked(child.struct, firstRecno, n, null);
      }

      case MultiDim: {
        ArrayStructureMA asma = new ArrayStructureMA( child.sm, new int[] {child.config.dim.getLength()});
        for (VariableSimpleIF v : child.cols) {
          Array data = parentStruct.getArray( v.getShortName());
          StructureMembers.Member childm = child.sm.findMember(v.getShortName());
          childm.setDataArray(data);
        }

        return asma.getStructureDataIterator();
      }

    }

    throw new IllegalStateException("Join type = "+config.joinType);
  }

}

