// $Id:ArrayStructureW.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.ma2;

/**
 * Concrete implementation of ArrayStructure, with data storage in individual Arrays in StructureData objects.
 * The ArrayStructure data accessors thus defer to the accessors in the StructureData objects.
 * Using this and StructureDataW is often the easiest to construct, but not very efficient for large arrays
 *  of Structures due to excessive object creation.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see Array
 */
public class ArrayStructureW extends ArrayStructure {

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * You must completely construct by calling setStructureData()
   *
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   */
  public ArrayStructureW(StructureMembers members, int[] shape) {
    super(members, shape);
    this.sdata = new StructureData[nelems];
  }

  /**
   * Create a new Array of type StructureData and the given members, shape, and array of StructureData.
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   * @param sdata   StructureData array, must be
   */

  public ArrayStructureW(StructureMembers members, int[] shape, StructureData[] sdata) {
    super(members, shape);
    if (nelems != sdata.length)
      throw new IllegalArgumentException("StructureData length= "+sdata.length+"!= shape.length="+nelems);
    this.sdata = sdata;
  }

  /**
   * Set one of the StructureData of this ArrayStructure.
   * @param sd set it to this StructureData.
   * @param index which one to set, as an index into 1D backing store.
   */
  public void setStructureData(StructureData sd, int index) {
    sdata[index] = sd;
  }

  protected StructureData makeStructureData( ArrayStructure as, int index) {
    return new StructureDataW( as.getStructureMembers());
  }

  /* public Array createView(Index index) {
    return new ArrayStructureW(members, index, sdata);
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory. Trusted package private.
   *
   * @param members a description of the structure members
   * @param ima     use this IndexArray as the index
   * @param sdata   StructureData array.
   *
  ArrayStructureW(StructureMembers members, Index ima, StructureData[] sdata) {
    super(members, ima);
    this.nelems = sdata.length;
    this.sdata = sdata;
  }   */

  /**
   * Get underlying StructureData primitive array storage. CAUTION! You may invalidate your warrentee!
   */
  public Object getStorage() { return sdata; }

  public Array getArray(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getArray(m);
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarDouble(m);
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayDouble(m);
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarFloat(m);
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayFloat(m);
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarByte(m);
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayByte(m);
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarShort(m);
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayShort(m);
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarInt(m);
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayInt(m);
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarLong(m);
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayLong(m);
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarChar(m);
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayChar(m);
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarString(m);
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getJavaArrayString(m);
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getScalarStructure(m);
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    StructureData sd = getStructureData(recnum);
    return sd.getArrayStructure(m);
  }

  /* public class StructureDataW extends StructureData {
    protected HashMap memberData = new HashMap(); // Members

    /**
     * Constructor.
     *
    public StructureDataW() {
      super(ArrayStructureW.this, 0);
    }

    public void setMemberData(StructureMembers.Member m, Array data) {
      if (data == null)
        throw new IllegalArgumentException("data cant be null");

      memberData.put(m, data);
    }

    public void setMemberData(String memberName, Array data) {
      StructureMembers.Member m = sa.members.findMember(memberName);
      if (m == null)
        throw new IllegalArgumentException("illegal member name =" + memberName);
      setMemberData(m, data);
    }

    public Array getMemberData(StructureMembers.Member m) {
      return (Array) memberData.get(m);
    }

    public Array getMemberData(String memberName) {
      StructureMembers.Member m = sa.members.findMember(memberName);
      if (m == null) throw new IllegalArgumentException("illegal member name =" + memberName);
      return getMemberData(m);
    }

    public Array getArray(StructureMembers.Member m) {
      return getMemberData(m);
    }

    public double getScalarDouble(String memberName) {
      Array data = getMemberData(memberName);
      return data.getDouble(Array.scalarIndex);
    }

    public double getScalarDouble(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getDouble(Array.scalarIndex);
    }

    public double[] getArrayDouble(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (double []) data.getStorage();
    }


    public float getScalarFloat(String memberName) {
      Array data = getMemberData(memberName);
      return data.getFloat(Array.scalarIndex);
    }

    public float getScalarFloat(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getFloat(Array.scalarIndex);
    }

    public float[] getArrayFloat(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (float []) data.getStorage();
    }

    public float convertScalarFloat(StructureMembers.Member m) {
      return getScalarFloat(m);
    }

    public double convertScalarDouble(StructureMembers.Member m) {
      return getScalarDouble(m);
    }

    public byte getScalarByte(String memberName) {
      Array data = getMemberData(memberName);
      return data.getByte(Array.scalarIndex);
    }

    public byte getScalarByte(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getByte(Array.scalarIndex);
    }

    public byte[] getArrayByte(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (byte []) data.getStorage();
    }


    public int getScalarInt(String memberName) {
      Array data = getMemberData(memberName);
      return data.getInt(Array.scalarIndex);
    }

    public int getScalarInt(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getInt(Array.scalarIndex);
    }

    public int[] getArrayInt(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (int []) data.getStorage();
    }


    public short getScalarShort(String memberName) {
      Array data = getMemberData(memberName);
      return data.getShort(Array.scalarIndex);
    }

    public short getScalarShort(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getShort(Array.scalarIndex);
    }

    public short[] getArrayShort(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (short []) data.getStorage();
    }


    public long getScalarLong(String memberName) {
      Array data = getMemberData(memberName);
      return data.getLong(Array.scalarIndex);
    }

    public long getScalarLong(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getLong(Array.scalarIndex);
    }

    public long[] getArrayLong(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (long []) data.getStorage();
    }


    public char getScalarChar(String memberName) {
      Array data = getMemberData(memberName);
      return data.getChar(Array.scalarIndex);
    }

    public char getScalarChar(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return data.getChar(Array.scalarIndex);
    }

    public char[] getArrayChar(StructureMembers.Member m) {
      Array data = getMemberData(m);
      return (char []) data.getStorage();
    }

    public String getScalarString(StructureMembers.Member m) {
      if (m.getDataType() == DataType.STRING) {
        Array data = getMemberData(m);
        return (String) data.getObject(0);
      } else {
        byte[] ba = getArrayByte(m);
        int count = 0;
        for (int i = 0; i < ba.length; i++) {
          if (0 == ba[i]) break;
          count++;
       }
        return new String(ba, 0, count);
      }
    }

    /*
     * Get structureData value, from rank 0 member array.
     *
     * @param memberName name of member Variable.
     * @throws IllegalArgumentException if name is not legal member name.
     *
    public StructureData getScalarStructure(String memberName) {
      ArrayObject.D0 a = (ArrayObject.D0) findMemberArray(memberName);
      if (a == null) throw new IllegalArgumentException("illegal member name =" + memberName);
      Object data = a.get();
      assert (data instanceof StructureData) : data.getClass().getName();
      return (StructureData) data;
    } */

    /**
     * Get String value, from rank 0 String or rank 1 char member array.
     *
     * @param memberName name of member Variable.
     * @throws IllegalArgumentException if name is not legal member name.
     *
    public String getScalarString(String memberName) {
      return getScalarString( members.findMember(memberName));
    } 

  } */

}