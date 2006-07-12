// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.adde;

import ucar.ma2.*;
import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Concrete implementation of Array specialized for StructureData.
 * Data storage is in 1D java array of bytes, which is converted to member data on the fly.
 *
 * @author caron
 * @version $Revision$ $Date$
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