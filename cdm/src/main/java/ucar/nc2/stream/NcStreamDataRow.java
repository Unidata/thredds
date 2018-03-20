/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import com.google.protobuf.ByteString;
import ucar.ma2.*;
import ucar.nc2.iosp.IospHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LOOK Not Done Yet
 *
 * @author caron
 * @since 10/26/2015.
 */
public class NcStreamDataRow {

  /*
  message DataRow {
    string fullName = 1;
    DataType dataType = 2;
    Section section = 3;
    bool bigend = 4;
    uint32 version = 5;
    bool isVlen = 7;
    uint32 nelems = 9;

    // oneof
    bytes primarray = 10;        // rectangular, primitive array # <1>
    repeated string stringdata = 11;  // string dataType # <2>
    ArrayStructureRow structdata = 12;  // structure/seq dataType # <3>
    repeated uint32 vlens = 13;  // isVlen true # <4>
    repeated bytes opaquedata = 14;  // opaque dataType # <5>
  }
  primarray has nelems * sizeof(dataType) bytes, turn into multidim array of primitives with section info and bigend
  stringdata has nelems strings, turn into multidim array of String with section info
  structdata has nelems StructureData objects, turn into multidim array of StructureData with section info and bigend
  vlens has section.size array lengths
  opaquedata has nelems opaque objects, turn into multidim array of Opaque with section info
*/

  public NcStreamProto.DataRow encodeData3(String name, boolean isVlen, Section section, Array data) {
    NcStreamProto.DataRow.Builder builder = NcStreamProto.DataRow.newBuilder();
    encodeData3(builder, name, isVlen, section, data);
    return builder.build();
  }

  void encodeData3(NcStreamProto.DataRow.Builder builder, String name, boolean isVlen, Section section, Array data) {
    DataType dataType = data.getDataType();

    builder.setFullName(name);
    builder.setDataType(NcStream.convertDataType(data.getDataType()));
    builder.setBigend(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
    builder.setVersion(NcStream.ncstream_data_version);

    if (!isVlen) {
      builder.setNelems((int) data.getSize());
      builder.setSection(NcStream.encodeSection(section));
    }

    if (isVlen) {
      builder.setIsVlen(true);
      encodeVlenData(builder, section, data);

    } else if (dataType == DataType.STRING) {
      if (data instanceof ArrayChar) { // is this possible ?
        ArrayChar cdata =(ArrayChar) data;
        for (String s:cdata)
          builder.addStringdata(s);
        Section ssection = section.removeLast();
        builder.setSection(NcStream.encodeSection(ssection));

      } else if (data instanceof ArrayObject) {
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext())
          builder.addStringdata( (String) iter.next());
      } else {
        throw new IllegalStateException("Unknown class for STRING ="+ data.getClass().getName());
      }

    } else if (dataType == DataType.OPAQUE) {
      if (data instanceof ArrayObject) {
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          ByteBuffer bb = (ByteBuffer) iter.next();
          builder.addOpaquedata(ByteString.copyFrom(bb));
        }
      } else {
        throw new IllegalStateException("Unknown class for OPAQUE ="+ data.getClass().getName());
      }

    } else if (dataType == DataType.STRUCTURE) {
      builder.setStructdata( encodeStructureData(name, data));

    } else if (dataType == DataType.SEQUENCE) {
      throw new UnsupportedOperationException("Not implemented yet SEQUENCE ="+ data.getClass().getName());

    } else { // normal case
      int nbytes = (int) data.getSizeBytes();
      ByteBuffer bb = ByteBuffer.allocate(nbytes);
      copyArrayToBB(data, bb);
      bb.flip();
      builder.setPrimdata(ByteString.copyFrom(bb));
    }

  }

  void encodeVlenData(NcStreamProto.DataRow.Builder builder, Section section, Array data) {
    if (!(data instanceof ArrayObject))
      throw new IllegalStateException("Unknown class for OPAQUE =" + data.getClass().getName());

    IndexIterator iter = data.getIndexIterator();
    int count = 0;
    while (iter.hasNext()) {
      Array varray = (Array) iter.next();
      int vlensize = (int) varray.getSize();
      builder.addVlens(vlensize);
      count += vlensize;
    }
    builder.setNelems(count);
    Section ssection = section.removeLast();
    builder.setSection(NcStream.encodeSection(ssection));
    assert section.computeSize() == count;

    int nbytes = count * data.getDataType().getSize();
    ByteBuffer bb = ByteBuffer.allocate(nbytes);

    // copyArrayToBB(data, bb);
    iter = data.getIndexIterator();
    while (iter.hasNext()) {
      Array varray = (Array) iter.next();
      copyArrayToBB(varray, bb);
    }
  }

  void copyArrayToBB(Array data, ByteBuffer out) {
    Class classType = data.getElementType();
    IndexIterator iterA = data.getIndexIterator();

    if (classType == double.class) {
      while (iterA.hasNext())
        out.putDouble(iterA.getDoubleNext());

    } else if (classType == float.class) {
      while (iterA.hasNext())
        out.putFloat(iterA.getFloatNext());

    } else if (classType == long.class) {
      while (iterA.hasNext())
        out.putLong(iterA.getLongNext());

    } else if (classType == int.class) {
      while (iterA.hasNext())
        out.putInt(iterA.getIntNext());

    } else if (classType == short.class) {
      while (iterA.hasNext())
        out.putShort(iterA.getShortNext());

    } else if (classType == char.class) {
      byte[] pa = IospHelper.convertCharToByte((char[]) data.get1DJavaArray(DataType.CHAR));
      out.put(pa, 0, pa.length);

    } else if (classType == byte.class) {
      while (iterA.hasNext())
        out.put(iterA.getByteNext());

    } else
      throw new UnsupportedOperationException("Class type = " + classType.getName());

  }

  private class MemberData {
    StructureMembers.Member member;
    int nelems;
    DataType dtype;
    boolean isVlen;

    ByteBuffer bb;
    List<String> stringList;
    List<ByteString> opaqueList;
    List<Array> vlenList;
    List<Integer> vlens;

    public MemberData(StructureMembers.Member member, int nelems) {
      this.member = member;
      this.nelems = nelems;
      this.dtype = member.getDataType();
      this.isVlen = member.isVariableLength();

      if (isVlen)
        vlenList = new ArrayList<>(nelems);
      else if (dtype == DataType.STRING)
        stringList = new ArrayList<>(nelems * member.getSize());
      else if (dtype == DataType.OPAQUE)
        opaqueList = new ArrayList<>(nelems * member.getSize());
      else if (dtype == DataType.STRUCTURE);
      else
        bb = ByteBuffer.allocate(nelems * member.getSizeBytes());
    }

    int completeVlens() {
      vlens = new ArrayList<>(nelems);
      int total = 0;
      for (Array va : vlenList) {
        vlens.add((int) va.getSize());
        total += va.getSize();
      }
      bb = ByteBuffer.allocate(nelems * member.getSizeBytes());
      for (Array va : vlenList) {
        copyArrayToBB(va, bb);
      }
      return total;
    }
  }

/*
  message Member {
    string shortName = 1;
    DataType dataType = 2;
    repeated uint32 shape = 3;  // or section?
    bool isVlen = 4;
  }

  message ArrayStructureRow {
    repeated Member members = 1;
    uint64 nrows = 5;      // number of rows in this message
    uint32 rowLength = 6;  // length in bytes of each row

    bytes fixdata = 10;            // fixed data
    repeated string stringdata = 11;  // string dataType
    repeated bytes bytedata = 13;  // opaque dataType and vlens
    repeated ArrayStructureRow structdata = 14;  // structure/seq dataType
  }
  */

  NcStreamProto.ArrayStructureRow.Builder encodeStructureData(String structName, Array data) {
    assert data instanceof ArrayStructure;
    ArrayStructure as = (ArrayStructure) data;
    int nelems = (int) as.getSize();

    List<MemberData> memberData = new ArrayList<>();
    StructureMembers sm = as.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      memberData.add( new MemberData(m, nelems));
    }

    // use most efficient form of data extraction
    for (int recno=0; recno<nelems; recno++) {
      for (MemberData md : memberData) {
        if (md.member.isVariableLength()) {
          md.vlenList.add(as.getArray(recno, md.member));
        } else {
          extractData(as, recno, md);
        }
      }
    }

    NcStreamProto.ArrayStructureRow.Builder builder = NcStreamProto.ArrayStructureRow.newBuilder();
    for (MemberData md : memberData) {
      NcStreamProto.Member.Builder member = NcStreamProto.Member.newBuilder();
      member.setShortName(md.member.getName());
      member.setDataType(NcStream.convertDataType(md.member.getDataType()));
      /* LOOK
      member.setNelems(md.nelems);
      if (md.member.isVariableLength()) {
        md.completeVlens();
        nested.addAllVlens (md.vlens);
        nested.setPrimdata(ByteString.copyFrom(md.bb));

      } else if (md.member.getDataType() == DataType.STRING)
        nested.addAllStringdata(md.stringList);
      else if (md.member.getDataType() == DataType.OPAQUE)
        nested.addAllOpaquedata(md.opaqueList);
      else
        nested.setPrimdata(ByteString.copyFrom(md.bb)); */

      builder.addMembers(member);
    }

    return builder;
  }

  void extractData(ArrayStructure as, int recno, MemberData md) {
    StructureMembers.Member m = md.member;
    ByteBuffer bb = md.bb;
    Class classType = md.dtype.getPrimitiveClassType();

    if (m.isScalar()) {
      if (classType == double.class)
        bb.putDouble(as.getScalarDouble(recno, m));

      else if (classType == float.class)
        bb.putFloat(as.getScalarFloat(recno, m));

      else if (classType == byte.class)
        bb.put(as.getScalarByte(recno, m));

      else if (classType == short.class)
        bb.putShort(as.getScalarShort(recno, m));

      else if (classType == int.class)
        bb.putInt(as.getScalarInt(recno, m));

      else if (classType == long.class)
        bb.putLong(as.getScalarLong(recno, m));

      else if (md.dtype == DataType.CHAR)
        bb.put((byte) as.getScalarChar(recno, m));

      else if (md.dtype == DataType.STRING)
        md.stringList.add(as.getScalarString(recno, m));

      else if (md.dtype == DataType.OPAQUE)
        md.opaqueList.add( ByteString.copyFrom((ByteBuffer) as.getScalarObject(recno, m)));

    } else {
      if (classType == double.class) {
        double[] data = as.getJavaArrayDouble(recno, m);
        for (double aData : data) bb.putDouble(aData);

      } else if (classType == float.class) {
        float[] data = as.getJavaArrayFloat(recno, m);
        for (float aData : data) bb.putFloat(aData);

      } else if (classType == byte.class) {
        byte[] data = as.getJavaArrayByte(recno, m);
        for (byte aData : data) bb.put(aData);

      } else if (classType == short.class) {
        short[] data = as.getJavaArrayShort(recno, m);
        for (short aData : data) bb.putShort(aData);

      } else if (classType == int.class) {
        int[] data = as.getJavaArrayInt(recno, m);
        for (int aData : data) bb.putInt(aData);

      } else if (classType == long.class) {
        long[] data = as.getJavaArrayLong(recno, m);
        for (long aData : data) bb.putLong(aData);

      } else if (md.dtype == DataType.CHAR) {
        char[] data = as.getJavaArrayChar(recno, m);
        for (char aData : data) bb.put((byte) aData);

      } else if (md.dtype == DataType.STRING) {
        String[] data = as.getJavaArrayString(recno, m);
        Collections.addAll(md.stringList, data);

      } else if (md.dtype == DataType.OPAQUE) {
        ArrayObject ao = as.getArrayObject(recno, m);
        while (ao.hasNext()) md.opaqueList.add(ByteString.copyFrom((ByteBuffer) ao.next()));
      }
    }
  }
}
