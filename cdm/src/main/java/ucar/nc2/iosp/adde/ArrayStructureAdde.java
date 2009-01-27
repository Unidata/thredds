// $Id:ArrayStructureAdde.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.iosp.adde;

import ucar.ma2.*;
import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Concrete implementation of Array specialized for StructureData.
 * Data storage is in 1D java array of bytes, which is converted to member data on the fly.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see Array
 */
public class ArrayStructureAdde extends ArrayStructure {
  private int MISSING_VALUE_INT = -9999; // ??
  private double MISSING_VALUE_DOUBLE = Double.NaN;

  protected int[][] data;
  protected double[] scaleFactor;

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * dimensions.length determines the rank of the new Array.
   *
   * @param members a description of the structure members
   * @param shape       the shape of the Array.
   */
  public ArrayStructureAdde(StructureMembers members, int[] shape, int[][] data, double[] scaleFactor) {
    super(members, shape);
    this.data = data;
    this.scaleFactor = scaleFactor;
  }


  public Array createView(Index index) {
    return new ArrayStructureAdde(members, index, nelems, sdata, data, scaleFactor);
  }

  protected StructureData makeStructureData(ArrayStructure as, int index) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory. Trusted package private.
   *
   * @param members     a description of the structure members
   * @param ima         use this IndexArray as the index
   * @param storage     use this as the backing storage.
   */
  ArrayStructureAdde(StructureMembers members, Index ima, int nelems, StructureData[] sdata, int[][] storage, double[] scaleFactor) {
    super(members, ima);
    this.nelems = nelems;
    this.sdata = sdata;
    this.data = storage;
    this.scaleFactor = scaleFactor;
  }

  public Object getStorage() { return data; }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    int param = m.getDataParam();

    int v = data[param][recnum];

    if (v == McIDASUtil.MCMISSING)
      return MISSING_VALUE_DOUBLE;

    if (scaleFactor[param] != 0.0)
        return v * scaleFactor[param];

    return (double) v;

  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    double[] data = new double[1];
    data[0] = getScalarDouble(recnum, m);
    return data;
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    int param = m.getDataParam();

    int v = data[param][recnum];
    if (v == McIDASUtil.MCMISSING)
      return MISSING_VALUE_INT;
    return v;
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    int[] data = new int[1];
    data[0] = getScalarInt(recnum, m);
    return data;
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    throw new IllegalArgumentException("Unsupported type");
  }

    public String getScalarString(int recnum, StructureMembers.Member m) {
    if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING)) {
      int param = m.getDataParam();
      return McIDASUtil.intBitsToString(data[param][recnum]);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}