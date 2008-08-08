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
package ucar.ma2;

import java.io.IOException;

/**
 * ArraySequence is the default way to contain the data for a Sequence, using a StructureDataIterator.
 * A Sequence is a one-dimensional Structure with indeterminate length.
 * The only data access is through getStructureIterator().
 * @author caron
 * @since Feb 27, 2008
 */
public class ArraySequence extends ArrayStructure {
  private StructureDataIterator iter;
  private int count;

  protected ArraySequence(StructureMembers sm, int[] shape) {
    super(sm, shape);
  }

  public ArraySequence(StructureMembers members, StructureDataIterator iter, int count) {
    super(members, new int[] {0});
    this.iter = iter;
    this.count = count;
  }

  @Override
  public Class getElementType() {
    return ArraySequence.class;
  }

  @Override
  public StructureDataIterator getStructureDataIterator() throws java.io.IOException {
    iter = iter.reset();
    return iter;
  }

  public int getStructureDataCount() {
    return count;
  }

  @Override
  protected StructureData makeStructureData(ArrayStructure as, int index) {
    throw new UnsupportedOperationException("Cannot subset a Sequence");
  }

  @Override
  public Array extractMemberArray(StructureMembers.Member m) throws IOException {
    if (m.getDataArray() != null)
      return m.getDataArray();

    DataType dataType = m.getDataType();
    boolean isScalar = (m.getSize() == 1) || (dataType == DataType.SEQUENCE);

    // combine the shapes
    int[] mshape = m.getShape();
    int rrank = 1 + mshape.length;
    int[] rshape = new int[rrank];
    rshape[0] = count;
    System.arraycopy(mshape, 0, rshape, 1, mshape.length);

    // create an empty array to hold the result
    Array result;
    if (dataType == DataType.STRUCTURE) {
      StructureMembers membersw = new StructureMembers(m.getStructureMembers()); // no data arrays get propagated
      result = new ArrayStructureW(membersw, rshape);
    } else {
      result = Array.factory(dataType.getPrimitiveClassType(), rshape);
    }

    StructureDataIterator sdataIter = getStructureDataIterator();
    IndexIterator resultIter = result.getIndexIterator();

    while (sdataIter.hasNext()) {
      StructureData sdata = sdataIter.next();

      if (isScalar) {
        if (dataType == DataType.DOUBLE)
          resultIter.setDoubleNext(sdata.getScalarDouble(m));

        else if (dataType == DataType.FLOAT)
          resultIter.setFloatNext(sdata.getScalarFloat(m));

        else if ((dataType == DataType.BYTE) || (dataType == DataType.ENUM1))
          resultIter.setByteNext(sdata.getScalarByte(m));

        else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2))
          resultIter.setShortNext(sdata.getScalarShort(m));

        else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4))
          resultIter.setIntNext(sdata.getScalarInt(m));

        else if (dataType == DataType.LONG)
          resultIter.setLongNext(sdata.getScalarLong(m));

        else if (dataType == DataType.CHAR)
          resultIter.setCharNext(sdata.getScalarChar(m));

        else if (dataType == DataType.STRING)
          resultIter.setObjectNext(sdata.getScalarString(m));

        else if (dataType == DataType.STRUCTURE)
          resultIter.setObjectNext( sdata.getScalarStructure(m));

        else if (dataType == DataType.SEQUENCE)
          resultIter.setObjectNext( sdata.getArraySequence(m));

    } else {
        if (dataType == DataType.DOUBLE) {
          double[] data = sdata.getJavaArrayDouble(m);
          for (double aData : data) resultIter.setDoubleNext(aData);

        } else if (dataType == DataType.FLOAT) {
          float[] data = sdata.getJavaArrayFloat(m);
          for (float aData : data) resultIter.setFloatNext(aData);

        } else if ((dataType == DataType.BYTE) || (dataType == DataType.ENUM1)) {
          byte[] data = sdata.getJavaArrayByte(m);
          for (byte aData : data) resultIter.setByteNext(aData);

        } else if ((dataType == DataType.SHORT)|| (dataType == DataType.ENUM2)) {
          short[] data = sdata.getJavaArrayShort(m);
          for (short aData : data) resultIter.setShortNext(aData);

        } else if ((dataType == DataType.INT)|| (dataType == DataType.ENUM4)) {
          int[] data = sdata.getJavaArrayInt(m);
          for (int aData : data) resultIter.setIntNext(aData);

        } else if (dataType == DataType.LONG) {
          long[] data = sdata.getJavaArrayLong(m);
          for (long aData : data) resultIter.setLongNext(aData);

        } else if (dataType == DataType.CHAR) {
          char[] data = sdata.getJavaArrayChar(m);
          for (char aData : data) resultIter.setCharNext(aData);

        } else if (dataType == DataType.STRING) {
          String[] data = sdata.getJavaArrayString(m);
          for (String aData : data) resultIter.setObjectNext(aData);

        } else if (dataType == DataType.STRUCTURE) {
          ArrayStructure as = sdata.getArrayStructure(m);
          StructureDataIterator innerIter = as.getStructureDataIterator();
          while (innerIter.hasNext())
            resultIter.setObjectNext( innerIter.next());

        }
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "Seq@"+hashCode();
  }

}
