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
package ucar.ma2;

import java.io.IOException;

/**
 * ArraySequence is the way to contain the data for a Sequence, using a StructureDataIterator.
 * A Sequence is a one-dimensional Structure with indeterminate length.
 * The only data access is through getStructureIterator().
 * So an ArraySequence is really a wrapper around a StructureDataIterator, adapting it to an Array.
 * 
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

  /**                                                             n
   * @return StructureDataIterator.class
   */
  @Override
  public Class getElementType() {
    return StructureDataIterator.class;
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
  public long getSizeBytes() {
    return count * members.getStructureSize(); // LOOK we may not know the count ???
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

        // LOOK SEQUENCE, OPAQUE ??
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "Seq@"+hashCode();
  }

}
