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
 * Joins a parent Table to one child Table
 *
 * @author caron
 * @since Apr 21, 2008
 */
public abstract class Join {


  public static Join factory(TableConfig.JoinConfig config) {
    switch (config.joinType) {

      case NestedStructure:
        return new JoinNested();

      case ForwardLinkedList:
      case BackwardLinkedList:
        return new JoinLinked(config.start, config.next);

      case ContiguousList:
        return new JoinContiguousList(config.start, config.numRecords);

      case MultiDim:
        return new JoinMultiDim();

      case ParentIndex:
        return new JoinParentIndex(config.parentIndex);

      default:
        throw new IllegalStateException("Unimplemented join type = " + config.joinType);

    }

  }

  ///////////////////////////////////////////////////////////////////////////////
  protected Table parent, child;

  abstract public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException;

  // for subclasses
  protected Join(){}

  public void joinTables(Table parent, Table child) {
    assert parent != null;
    assert child != null;
    this.parent = parent;
    this.child = child;
    child.join2parent = this;
    child.parent = parent;
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append(getClass().getName());
    if (parent != null)
      sbuff.append(" from ").append(parent.getName());
    if (child != null)
      sbuff.append(" to ").append(child.getName());
    return sbuff.toString();
  }

  private static class JoinNested extends Join {

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {

      String name = child.getName();
      StructureMembers members = parentStruct.getStructureMembers();
      StructureMembers.Member m = members.findMember(name);
      if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence seq = parentStruct.getArraySequence(m);
        return seq.getStructureDataIterator();

      } else if (m.getDataType() == DataType.STRUCTURE) {
        ArrayStructure as = parentStruct.getArrayStructure(m);
        return as.getStructureDataIterator();
      }

      return null;
    }
  }

  private static class JoinLinked extends Join {
    private String start, next;
    private Table.TableStructure myChild;

    JoinLinked(String start, String next) {
      this.start = start;
      this.next = next;
    }

    @Override
    public void joinTables(Table parent, Table child) {
      super.joinTables(parent, child);
      myChild = (Table.TableStructure) child;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      int firstRecno = parentStruct.getScalarInt(start);
      return new StructureDataIteratorLinked(myChild.struct, firstRecno, -1, next);
    }
  }

  private static class JoinContiguousList extends Join {
    private String start, numRecords;
    private Table.TableStructure myChild;

    JoinContiguousList(String start, String numRecords) {
      this.start = start;
      this.numRecords = numRecords;
    }

    @Override
    public void joinTables(Table parent, Table child) {
      super.joinTables(parent, child);
      myChild = (Table.TableStructure) child;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      int firstRecno = parentStruct.getScalarInt(start);
      int n = parentStruct.getScalarInt(numRecords);
      return new StructureDataIteratorLinked(myChild.struct, firstRecno, n, null);
    }
  }

  private static class JoinMultiDim extends Join {
    private Table.TableMultDim myChild;

    @Override
    public void joinTables(Table parent, Table child) {
      super.joinTables(parent, child);
      myChild = (Table.TableMultDim) child;
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      ArrayStructureMA asma = new ArrayStructureMA(myChild.sm, new int[]{myChild.dim.getLength()});
      for (VariableSimpleIF v : child.cols) {
        Array data = parentStruct.getArray(v.getShortName());
        StructureMembers.Member childm = myChild.sm.findMember(v.getShortName());
        childm.setDataArray(data);
      }
      return asma.getStructureDataIterator();
    }
  }

  public static class JoinParentIndex extends Join {
    private Table.TableStructure myChild;
    private ArrayStructure parentData;
    private String parentIndex;

    JoinParentIndex(String parentIndex) {
      this.parentIndex = parentIndex;
    }

    @Override
    public void joinTables(Table parent, Table child) {
      super.joinTables(parent, child);
      myChild = (Table.TableStructure) child;

      try {
        parentData = (ArrayStructure) myChild.struct.read(); // cache entire station table  LOOK
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public StructureData getJoin(StructureData obsdata) {
      int index = obsdata.getScalarInt(parentIndex);
      return parentData.getStructureData(index);
    }

    public StructureDataIterator getStructureDataIterator(StructureData parentStruct, int bufferSize) throws IOException {
      return null;
    }
  }

}

