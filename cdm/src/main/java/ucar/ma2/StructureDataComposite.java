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

import java.util.Map;
import java.util.HashMap;

/**
 * A composite of other StructureData.
 * If multiple of same name, first one is used
 *
 * @author caron
 * @since Jan 21, 2009
 */
public class StructureDataComposite extends StructureData {
  protected Map<StructureMembers.Member, StructureData> proxy = new HashMap<StructureMembers.Member,StructureData>(32);

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

  public Array getArray(StructureMembers.Member m) {
    StructureData sdata = proxy.get(m);
    return sdata.getArray(m);
  }

  public float convertScalarFloat(StructureMembers.Member m) {
    return proxy.get(m).convertScalarFloat(m);
  }

  public double convertScalarDouble(StructureMembers.Member m) {
    return proxy.get(m).convertScalarDouble(m);
  }

  public int convertScalarInt(StructureMembers.Member m) {
    return proxy.get(m).convertScalarInt(m);
  }

  public double getScalarDouble(StructureMembers.Member m) {
    return proxy.get(m).getScalarDouble(m);
  }

  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayDouble(m);
  }

  public float getScalarFloat(StructureMembers.Member m) {
    return proxy.get(m).getScalarFloat(m);
  }

  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayFloat(m);
  }

  public byte getScalarByte(StructureMembers.Member m) {
    return proxy.get(m).getScalarByte(m);
  }

  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayByte(m);
  }

  public int getScalarInt(StructureMembers.Member m) {
    return proxy.get(m).getScalarInt(m);
  }

  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayInt(m);
  }

  public short getScalarShort(StructureMembers.Member m) {
    return proxy.get(m).getScalarShort(m);
  }

  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayShort(m);
  }

  public long getScalarLong(StructureMembers.Member m) {
    return proxy.get(m).getScalarLong(m);
  }

  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayLong(m);
  }

  public char getScalarChar(StructureMembers.Member m) {
    return proxy.get(m).getScalarChar(m);
  }

  public char[] getJavaArrayChar(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayChar(m);
  }

  public String getScalarString(StructureMembers.Member m) {
    return proxy.get(m).getScalarString(m);
  }

  public String[] getJavaArrayString(StructureMembers.Member m) {
    return proxy.get(m).getJavaArrayString(m);
  }

  public StructureData getScalarStructure(StructureMembers.Member m) {
    return proxy.get(m).getScalarStructure(m);
  }

  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return proxy.get(m).getArrayStructure(m);
  }

  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return proxy.get(m).getArraySequence(m);
  }

}
