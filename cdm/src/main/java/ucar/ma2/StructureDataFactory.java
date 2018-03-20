/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Convenience routines for constructing one-off StructureData objects
 *
 * @author caron
 * @since Jan 19, 2009
 */
public class StructureDataFactory {

  /* static public StructureData make(String name, String value) {
    StructureMembers members = new StructureMembers("");
    StructureMembers.Member m = members.addMember(name, null, null, DataType.STRING, new int[]{1});
    StructureDataW sw = new StructureDataW(members);
    Array dataArray = Array.factory(DataType.STRING, new int[]{1});
    dataArray.setObject(dataArray.getIndex(), value);
    sw.setMemberData(m, dataArray);
    return sw;
  } */

  static public StructureData make(String name, Object value) {
    StructureMembers members = new StructureMembers("");
    DataType dtype = DataType.getType(value.getClass(), false);  // LOOK unsigned
    StructureMembers.Member m = members.addMember(name, null, null, dtype, new int[]{1});
    StructureDataW sw = new StructureDataW(members);
    Array dataArray = Array.factory(dtype, new int[]{1});
    dataArray.setObject(dataArray.getIndex(), value);
    sw.setMemberData(m, dataArray);
    return sw;
  }

  static public StructureData make(StructureData s1, StructureData s2) {
    return make(new StructureData[]{s1, s2});
  }

  static public StructureData make(StructureData[] sdatas) {
    if (sdatas.length == 1) return sdatas[0];

    // look for sole
    int count = 0;
    StructureData result = null;
    for (StructureData sdata : sdatas) {
      if (sdata != null) {
        count++;
        result = sdata;
      }
    }
    if (count == 1) return result;

    // combine
    StructureDataComposite result2 = new StructureDataComposite();
    for (StructureData sdata : sdatas) {
      if (sdata != null)
        result2.add(sdata);
    }
    return result2;
  }


}
