/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
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
 * Describe
 *
 * @author caron
 * @since 10/30/2015.
 */
public class NcStreamDataCol {

  /*
  message Data2 {
    string fullName = 1;
    DataType dataType = 2;
    Section section = 3;
    bool bigend = 4;
    uint32 version = 5;
    bool isVlen = 7;
    uint32 nelems = 9;

    // oneof
    bytes primdata = 10;              // rectangular, primitive array
    repeated string stringdata = 11;  // string dataType
    repeated uint32 vlens = 12;       // isVlen true
    repeated bytes opaquedata = 13;   // opaque dataType

    // structures
    ArrayStructureCol structdata = 14;  // structure/seq dataType
  }

  message ArrayStructureCol {
    repeated Data2 memberData = 1;
  }

  primdata has nelems * sizeof(dataType) bytes, turn into multidim array of primitives with section info and bigend
  stringdata has nelems strings, turn into multidim array of String with section info
  vlens has section.size array lengths; section does not include the last (vlen) dimension; data in primdata
  opaquedata has nelems opaque objects, turn into multidim array of Opaque with section info
  structdata has nelems StructureData objects, turn into multidim array of StructureData with section info and bigend

*/

  public NcStreamProto.Data2 encodeData2(String name, boolean isVlen, Section section, Array data) {
    NcStreamProto.Data2.Builder builder = NcStreamProto.Data2.newBuilder();
    encodeData2(builder, name, isVlen, section, data);
    return builder.build();
  }

  void encodeData2(NcStreamProto.Data2.Builder builder, String name, boolean isVlen, Section section, Array data) {
    DataType dataType = data.getDataType();

    builder.setName(name);
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
        Section ssection = section.removeRange(section.getRank()-1);
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
      builder.setStructdata(encodeStructureData( data));

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

  void encodeVlenData(NcStreamProto.Data2.Builder builder, Section section, Array data) {
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
    Section ssection = section.removeRange(section.getRank() - 1);
    builder.setSection(NcStream.encodeSection(ssection));
    assert ssection.computeSize() == count;

    int nbytes = count * data.getDataType().getSize();
    ByteBuffer bb = ByteBuffer.allocate(nbytes);

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

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // Structures

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
    List<MemberData> members;

    public MemberData(StructureMembers.Member member, int nelems) {
      this.member = member;
      this.nelems = nelems;
      this.dtype = member.getDataType();
      this.isVlen = member.isVariableLength();

      if (isVlen) {
        vlenList = new ArrayList<>(nelems);
      } else if (dtype == DataType.STRING) {
        stringList = new ArrayList<>(nelems * member.getSize());
      } else if (dtype == DataType.OPAQUE) {
        opaqueList = new ArrayList<>(nelems * member.getSize());

      } else if (dtype == DataType.STRUCTURE) { // LOOK not doing sequences yet
        members = new ArrayList<>();
        for (StructureMembers.Member m : member.getStructureMembers().getMembers()) {
          members.add( new MemberData(m, nelems * m.getSize()));
        }
      } else
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

  NcStreamProto.ArrayStructureCol.Builder encodeStructureData(Array data) {
    assert data instanceof ArrayStructure;
    ArrayStructure as = (ArrayStructure) data;
    int nelems = (int) as.getSize();

    // create MemberData to hold extracted data
    List<MemberData> memberDataList = new ArrayList<>();
    StructureMembers sm = as.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      memberDataList.add( new MemberData(m, nelems));
    }

    // data extraction
    for (int recno=0; recno<nelems; recno++) {
      for (MemberData md : memberDataList) {
        if (md.member.isVariableLength()) {
          md.vlenList.add(as.getArray(recno, md.member));
        } else {
          extractData(as, recno, md);
        }
      }
    }

    // construct the result recursively
    return buildNestedStructureData( memberDataList);
  }

  NcStreamProto.ArrayStructureCol.Builder buildNestedStructureData(List<MemberData> mdataList) {
    NcStreamProto.ArrayStructureCol.Builder result = NcStreamProto.ArrayStructureCol.newBuilder();

    for (MemberData nestedMemberData : mdataList) {
      NcStreamProto.Data2.Builder nestedBuilder = NcStreamProto.Data2.newBuilder();
      nestedBuilder.setName(nestedMemberData.member.getName());
      nestedBuilder.setDataType(NcStream.convertDataType(nestedMemberData.member.getDataType()));
      nestedBuilder.setNelems(nestedMemberData.nelems);

      if (nestedMemberData.member.isVariableLength()) {
        nestedMemberData.completeVlens();
        nestedBuilder.addAllVlens (nestedMemberData.vlens);
        nestedBuilder.setPrimdata(ByteString.copyFrom(nestedMemberData.bb));

      } else if (nestedMemberData.member.getDataType() == DataType.STRING) {
        nestedBuilder.addAllStringdata(nestedMemberData.stringList);

      } else if (nestedMemberData.member.getDataType() == DataType.OPAQUE) {
        nestedBuilder.addAllOpaquedata(nestedMemberData.opaqueList);

      } else if (nestedMemberData.member.getDataType() == DataType.STRUCTURE) {
        nestedBuilder.setStructdata(buildNestedStructureData( nestedMemberData.members)); // recurse

      } else {
        nestedBuilder.setPrimdata(ByteString.copyFrom(nestedMemberData.bb));
      }

      result.addMemberData(nestedBuilder);
    }

    return result;
  }

  ///////////////////////////////////////////////////////////
  // extract data from ArrayStructure and put into MemberData

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

      else if (md.dtype == DataType.STRUCTURE)
        extractStructureData(md, as.getScalarStructure(recno, m));

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

      } else if (md.dtype == DataType.STRUCTURE) {
        ArrayStructure nested = as.getArrayStructure(recno, m);
        for (int i=0; i<nested.getSize(); i++) extractStructureData(md, nested.getStructureData(i));
      }
    }
  }

  void extractStructureData(MemberData md, StructureData sdata) {

    for (MemberData nested : md.members) {
      StructureMembers.Member m = nested.member;
      ByteBuffer bb = nested.bb;
      Class classType = nested.dtype.getPrimitiveClassType();

      if (m.isScalar()) {
        if (classType == double.class)
          bb.putDouble(sdata.getScalarDouble( m));

        else if (classType == float.class)
          bb.putFloat(sdata.getScalarFloat( m));

        else if (classType == byte.class)
          bb.put(sdata.getScalarByte( m));

        else if (classType == short.class)
          bb.putShort(sdata.getScalarShort( m));

        else if (classType == int.class)
          bb.putInt(sdata.getScalarInt( m));

        else if (classType == long.class)
          bb.putLong(sdata.getScalarLong( m));

        else if (md.dtype == DataType.CHAR)
          bb.put((byte) sdata.getScalarChar( m));

        else if (md.dtype == DataType.STRING)
          md.stringList.add(sdata.getScalarString( m));

        else if (md.dtype == DataType.OPAQUE)
          md.opaqueList.add( ByteString.copyFrom((ByteBuffer) sdata.getScalarObject( m)));

        else if (md.dtype == DataType.STRUCTURE)
          extractStructureData(md, sdata.getScalarStructure( m));

      } else {
        if (classType == double.class) {
          double[] data = sdata.getJavaArrayDouble( m);
          for (double aData : data) bb.putDouble(aData);

        } else if (classType == float.class) {
          float[] data = sdata.getJavaArrayFloat( m);
          for (float aData : data) bb.putFloat(aData);

        } else if (classType == byte.class) {
          byte[] data = sdata.getJavaArrayByte( m);
          for (byte aData : data) bb.put(aData);

        } else if (classType == short.class) {
          short[] data = sdata.getJavaArrayShort( m);
          for (short aData : data) bb.putShort(aData);

        } else if (classType == int.class) {
          int[] data = sdata.getJavaArrayInt( m);
          for (int aData : data) bb.putInt(aData);

        } else if (classType == long.class) {
          long[] data = sdata.getJavaArrayLong( m);
          for (long aData : data) bb.putLong(aData);

        } else if (md.dtype == DataType.CHAR) {
          char[] data = sdata.getJavaArrayChar( m);
          for (char aData : data) bb.put((byte) aData);

        } else if (md.dtype == DataType.STRING) {
          String[] data = sdata.getJavaArrayString( m);
          Collections.addAll(md.stringList, data);

        } else if (md.dtype == DataType.OPAQUE) {
          Array ao = sdata.getArray( m);
          while (ao.hasNext()) md.opaqueList.add(ByteString.copyFrom((ByteBuffer) ao.next()));

        } else if (md.dtype == DataType.STRUCTURE) {
          ArrayStructure nestedAS = sdata.getArrayStructure( m);
          for (int i=0; i<nestedAS.getSize(); i++) extractStructureData(md, nestedAS.getStructureData(i));
        }
      }
    }

  }



}
