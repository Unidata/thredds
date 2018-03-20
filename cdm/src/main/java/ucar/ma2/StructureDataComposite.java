/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.util.Map;
import java.util.HashMap;

/**
 * A composite of other StructureData.
 * If multiple members of same name exist, the first one added is used
 *
 * @author caron
 * @since Jan 21, 2009
 */
public class StructureDataComposite extends StructureData {
  protected Map<StructureMembers.Member, StructureData> proxy = new HashMap<>(32);

  public StructureDataComposite() {
    super(new StructureMembers(""));
  }

  public void add(StructureData sdata) {
    for (StructureMembers.Member m : sdata.getMembers()) {
      if (this.members.findMember(m.getName()) == null) {
        this.members.addMember(m);
        proxy.put(m, sdata);
      }
    }
  }

  public void add(int pos, StructureData sdata) {
    for (StructureMembers.Member m : sdata.getMembers()) {
      if (this.members.findMember(m.getName()) == null) {
        this.members.addMember(pos++, m);
        proxy.put(m, sdata);
      }
    }
  }

  public Array getArray(StructureMembers.Member m) {
    StructureData sdata = proxy.get(m);
    return sdata.getArray(m.getName());
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return proxy.get(m).convertScalarFloat(m.getName());
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return proxy.get(m).convertScalarDouble(m.getName());
  }

  public int convertScalarInt(StructureMembers.Member m) {
    return proxy.get(m).convertScalarInt(m.getName());
  }

  public long convertScalarLong(StructureMembers.Member m) {
    return proxy.get(m).convertScalarLong(m.getName());
  }

  public double getScalarDouble(StructureMembers.Member m) {
    return proxy.get(m).getScalarDouble(m.getName());
  }

  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayDouble(m.getName());
  }

  public float getScalarFloat(StructureMembers.Member m) {
    return proxy.get(m).getScalarFloat(m.getName());
  }

  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayFloat(m.getName());
  }

  public byte getScalarByte(StructureMembers.Member m) {
    return proxy.get(m).getScalarByte(m.getName());
  }

  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayByte(m.getName());
  }

  public int getScalarInt(StructureMembers.Member m) {
    return proxy.get(m).getScalarInt(m.getName());
  }

  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayInt(m.getName());
  }

  public short getScalarShort(StructureMembers.Member m) {
    return proxy.get(m).getScalarShort(m.getName());
  }

  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayShort(m.getName());
  }

  public long getScalarLong(StructureMembers.Member m) {
    return proxy.get(m).getScalarLong(m.getName());
  }

  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayLong(m.getName());
  }

  public char getScalarChar(StructureMembers.Member m) {
    return proxy.get(m).getScalarChar(m.getName());
  }

  public char[] getJavaArrayChar(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayChar(m.getName());
  }

  public String getScalarString(StructureMembers.Member m) {
    return proxy.get(m).getScalarString(m.getName());
  }

  public String[] getJavaArrayString(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayString(m.getName());
  }

  public StructureData getScalarStructure(StructureMembers.Member m) {
    return proxy.get(m).getScalarStructure(m.getName());
  }

  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return proxy.get(m).getArrayStructure(m.getName());
  }

  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return proxy.get(m).getArraySequence(m.getName());
  }

  public Object getScalarObject( StructureMembers.Member m) {
    return proxy.get(m).getScalarObject(m.getName());
  }  

}
