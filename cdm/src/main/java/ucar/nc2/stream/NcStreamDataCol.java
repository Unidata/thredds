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
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DataCol message encoding.
 *
 * @author caron
 * @since 10/30/2015.
 */
public class NcStreamDataCol {

  /*
  message DataCol {
    string name = 1;      // fullname for top, shortname for member
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
    repeated DataCol memberData = 1;
  }

  primdata has nelems * sizeof(dataType) bytes, turn into multidim array of primitives with section info and bigend
  stringdata has nelems strings, turn into multidim array of String with section info
  vlens has section.size array lengths; section does not include the last (vlen) dimension; data in primdata
  opaquedata has nelems opaque objects, turn into multidim array of Opaque with section info
  structdata has nelems StructureData objects, turn into multidim array of StructureData with section info and bigend

*/

  public NcStreamProto.DataCol encodeData2(String name, boolean isVlen, Section section, Array data) {
    NcStreamProto.DataCol.Builder builder = NcStreamProto.DataCol.newBuilder();
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
        ArrayChar cdata = (ArrayChar) data;
        for (String s : cdata)
          builder.addStringdata(s);
        Section ssection = section.removeLast();
        builder.setSection(NcStream.encodeSection(ssection));

      } else if (data instanceof ArrayObject) {
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext())
          builder.addStringdata((String) iter.next());
      } else {
        throw new IllegalStateException("Unknown class for STRING =" + data.getClass().getName());
      }

    } else if (dataType == DataType.OPAQUE) {
      if (data instanceof ArrayObject) {
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          ByteBuffer bb = (ByteBuffer) iter.next();

          // Need to use duplicate so that internal state of bb isn't affected
          builder.addOpaquedata(ByteString.copyFrom(bb.duplicate()));
        }
      } else {
        throw new IllegalStateException("Unknown class for OPAQUE =" + data.getClass().getName());
      }

    } else if (dataType == DataType.STRUCTURE) {
      builder.setStructdata(encodeStructureData(data));

    } else if (dataType == DataType.SEQUENCE) {
      throw new UnsupportedOperationException("Not implemented yet SEQUENCE =" + data.getClass().getName());

    } else { // normal case
      builder.setPrimdata(copyArrayToByteString(data));
    }

    return builder.build();
  }

  void encodeVlenData(NcStreamProto.DataCol.Builder builder, Section section, Array data) {
    if (data instanceof ArrayObject) {
      IndexIterator iter = data.getIndexIterator();
      int count = 0;
      int nelems = 0;
      while (iter.hasNext()) {
        Array varray = (Array) iter.next();
        int vlensize = (int) varray.getSize();
        builder.addVlens(vlensize);
        nelems += vlensize;
        count++;
      }
      builder.setNelems(nelems);
      Section ssection = section.removeVlen();
      builder.setSection(NcStream.encodeSection(ssection));
      assert ssection.computeSize() == count;

      int nbytes = nelems * data.getDataType().getSize();
      ByteBuffer bb = ByteBuffer.allocate(nbytes);
      bb.order(ByteOrder.nativeOrder());

      iter = data.getIndexIterator();
      while (iter.hasNext()) {
        Array varray = (Array) iter.next();
        copyArrayToBB(varray, true, bb);
      }
      bb.flip();
      builder.setPrimdata(ByteString.copyFrom(bb));

      // If the vlen is rank one, the data array may just be an Array of the appropriate type, since the ArrayObject is not needed:
    } else  {
      builder.setPrimdata(copyArrayToByteString(data));

      // returning as a regular array, not vlen
      builder.setIsVlen(false);
      builder.setNelems((int) data.getSize());
      builder.setSection(NcStream.encodeSection(new Section(data.getShape())));
    }

    // otherwise WTF ?
    // throw new IllegalStateException("Unknown class for OPAQUE =" + data.getClass().getName());
  }

  public static ByteString copyArrayToByteString(Array data) {
    int nbytes = (int) data.getSizeBytes();
    if (nbytes < 0) {
      System.out.printf("copyArrayToByteString neg byte size %d dataType = %d data size %d shape = %s%n",
              nbytes, data.getDataType().getSize(), data.getSize(), Misc.showInts(data.getShape()));
    }
    ByteBuffer bb = ByteBuffer.allocate(nbytes);
    bb.order(ByteOrder.nativeOrder());
    copyArrayToBB(data, false, bb);
    bb.flip();
    return ByteString.copyFrom(bb);
  }

  public static void copyArrayToBB(Array data, boolean isVlen, ByteBuffer out) {
    IndexIterator iterA = data.getIndexIterator();

    // VLEN
    if (isVlen && data instanceof ArrayObject) {
      while (iterA.hasNext()) {
        Object inner = iterA.next();
        assert (inner instanceof Array);
        copyArrayToBB((Array) inner, isVlen, out);
      }
      return;
    }

    Class classType = data.getElementType();

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
    Section section;
    DataType dtype;
    boolean isVlen;
    int nelems;

    ByteBuffer bb;
    List<String> stringList;
    List<ByteString> opaqueList;
    List<Array> vlenList;
    List<Integer> vlens;
    List<MemberData> members;

    public MemberData(StructureMembers.Member member, int[] parent) {
      this.member = member;
      this.section = new Section(parent);
      try {
        int[] mshape = member.getShape();
        //if (mshape.length == 0) // scalar
        //  this.section.appendRange(Range.ONE);
        //else
        // compose with the parent
        for (int s : mshape) {
          if (s < 0) continue;
          this.section.appendRange(s);
        }
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }

      this.dtype = member.getDataType();
      this.isVlen = member.isVariableLength();
      this.nelems = (int) section.computeSize();

      if (isVlen) {
        vlenList = new ArrayList<>(nelems);
      } else if (dtype == DataType.STRING) {
        stringList = new ArrayList<>(nelems * member.getSize());
      } else if (dtype == DataType.OPAQUE) {
        opaqueList = new ArrayList<>(nelems * member.getSize());

      } else if (dtype == DataType.STRUCTURE) { // LOOK not doing sequences yet
        members = new ArrayList<>();
        for (StructureMembers.Member m : member.getStructureMembers().getMembers()) {
          members.add(new MemberData(m, section.getShape()));
        }
      } else {
        bb = ByteBuffer.allocate(nelems * member.getSizeBytes());
        bb.order(ByteOrder.nativeOrder());
      }
    }

    int addVlens(Array va) {
      int total = 0;
      if (va instanceof ArrayObject) {
        while (va.hasNext()) {
          Object inner = va.next();
          assert (inner instanceof Array);
          total += addVlens((Array) inner);
        }
      } else {
        vlens.add((int) va.getSize());
        total += va.getSize();
      }
      return total;
    }

    int finishVlens() {
      vlens = new ArrayList<>(nelems*member.getSize());
      int total = 0;
      for (Array va : vlenList) {
        total += addVlens(va);
      }
      this.nelems = total;

      bb = ByteBuffer.allocate(total * member.getSizeBytes());
      bb.order(ByteOrder.nativeOrder());
      for (Array va : vlenList) {
        copyArrayToBB(va, true, bb);
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
      memberDataList.add(new MemberData(m, as.getShape()));
    }

    // data extraction
    for (int recno = 0; recno < nelems; recno++) {
      for (MemberData md : memberDataList) {
        if (md.member.isVariableLength()) {
          md.vlenList.add( as.getArray(recno, md.member));
        } else {
          extractData(as, recno, md);
        }
      }
    }

    // construct the result recursively
    return buildNestedStructureData(memberDataList);
  }

  NcStreamProto.ArrayStructureCol.Builder buildNestedStructureData(List<MemberData> mdataList) {
    NcStreamProto.ArrayStructureCol.Builder result = NcStreamProto.ArrayStructureCol.newBuilder();

    for (MemberData nestedMemberData : mdataList) {
      NcStreamProto.DataCol.Builder nestedBuilder = NcStreamProto.DataCol.newBuilder();
      nestedBuilder.setName(nestedMemberData.member.getName());
      nestedBuilder.setDataType(NcStream.convertDataType(nestedMemberData.member.getDataType()));
      nestedBuilder.setNelems(nestedMemberData.nelems);
      nestedBuilder.setSection(NcStream.encodeSection(nestedMemberData.section));

      if (nestedMemberData.member.isVariableLength()) {
        nestedMemberData.finishVlens();
        nestedBuilder.addAllVlens(nestedMemberData.vlens);
        nestedMemberData.bb.flip();
        nestedBuilder.setPrimdata(ByteString.copyFrom(nestedMemberData.bb));
        nestedBuilder.setNelems(nestedMemberData.nelems);
        nestedBuilder.setIsVlen(true);

      } else if (nestedMemberData.member.getDataType() == DataType.STRING) {
        nestedBuilder.addAllStringdata(nestedMemberData.stringList);

      } else if (nestedMemberData.member.getDataType() == DataType.OPAQUE) {
        nestedBuilder.addAllOpaquedata(nestedMemberData.opaqueList);

      } else if (nestedMemberData.member.getDataType() == DataType.STRUCTURE) {
        nestedBuilder.setStructdata(buildNestedStructureData(nestedMemberData.members)); // recurse

      } else {
        nestedMemberData.bb.flip();
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
        bb.put((byte) as.getScalarChar(recno, m)); // look this just truncates to first byte

      else if (md.dtype == DataType.STRING)
        md.stringList.add(as.getScalarString(recno, m));

      else if (md.dtype == DataType.OPAQUE)
        md.opaqueList.add(ByteString.copyFrom((ByteBuffer) as.getScalarObject(recno, m)));

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
        for (int i = 0; i < nested.getSize(); i++) extractStructureData(md, nested.getStructureData(i));
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
          bb.putDouble(sdata.getScalarDouble(m));

        else if (classType == float.class)
          bb.putFloat(sdata.getScalarFloat(m));

        else if (classType == byte.class)
          bb.put(sdata.getScalarByte(m));

        else if (classType == short.class)
          bb.putShort(sdata.getScalarShort(m));

        else if (classType == int.class)
          bb.putInt(sdata.getScalarInt(m));

        else if (classType == long.class)
          bb.putLong(sdata.getScalarLong(m));

        else if (md.dtype == DataType.CHAR)
          bb.put((byte) sdata.getScalarChar(m));

        else if (md.dtype == DataType.STRING)
          md.stringList.add(sdata.getScalarString(m));

        else if (md.dtype == DataType.OPAQUE)
          md.opaqueList.add(ByteString.copyFrom((ByteBuffer) sdata.getScalarObject(m)));

        else if (md.dtype == DataType.STRUCTURE)
          extractStructureData(md, sdata.getScalarStructure(m));

      } else {
        if (classType == double.class) {
          double[] data = sdata.getJavaArrayDouble(m);
          for (double aData : data) bb.putDouble(aData);

        } else if (classType == float.class) {
          float[] data = sdata.getJavaArrayFloat(m);
          for (float aData : data) bb.putFloat(aData);

        } else if (classType == byte.class) {
          byte[] data = sdata.getJavaArrayByte(m);
          for (byte aData : data) bb.put(aData);

        } else if (classType == short.class) {
          short[] data = sdata.getJavaArrayShort(m);
          for (short aData : data) bb.putShort(aData);

        } else if (classType == int.class) {
          int[] data = sdata.getJavaArrayInt(m);
          for (int aData : data) bb.putInt(aData);

        } else if (classType == long.class) {
          long[] data = sdata.getJavaArrayLong(m);
          for (long aData : data) bb.putLong(aData);

        } else if (md.dtype == DataType.CHAR) {
          char[] data = sdata.getJavaArrayChar(m);
          for (char aData : data) bb.put((byte) aData);

        } else if (md.dtype == DataType.STRING) {
          String[] data = sdata.getJavaArrayString(m);
          Collections.addAll(md.stringList, data);

        } else if (md.dtype == DataType.OPAQUE) {
          Array ao = sdata.getArray(m);
          while (ao.hasNext()) md.opaqueList.add(ByteString.copyFrom((ByteBuffer) ao.next()));

        } else if (md.dtype == DataType.STRUCTURE) {
          ArrayStructure nestedAS = sdata.getArrayStructure(m);
          for (int i = 0; i < nestedAS.getSize(); i++) extractStructureData(md, nestedAS.getStructureData(i));
        }
      }
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  public Array decode(NcStreamProto.DataCol dproto, Section parentSection) throws IOException {

    ByteOrder bo = dproto.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

    DataType dataType = NcStream.convertDataType(dproto.getDataType());
    Section section = (dataType == DataType.SEQUENCE) ? new Section() : NcStream.decodeSection(dproto.getSection());
    if (!dproto.getIsVlen()) {
      assert dproto.getNelems() == section.computeSize();
    }

    // special cases
    if (dproto.getIsVlen()) {
      if (parentSection == null)
        return decodeVlenData(dproto);
      else
        return decodeVlenData(dproto, parentSection);

    } else if (dataType == DataType.STRING) {
      Array data = Array.factory(dataType, section.getShape());
      IndexIterator ii = data.getIndexIterator();
      for (String s : dproto.getStringdataList()) {
        ii.setObjectNext(s);
      }
      return data;

    } else if (dataType == DataType.STRUCTURE) {
      return decodeStructureData(dproto, parentSection);

    } else if (dataType == DataType.OPAQUE) {
      Array data = Array.factory(dataType, section.getShape());
      IndexIterator ii = data.getIndexIterator();
      for (ByteString s : dproto.getOpaquedataList()) {
        ii.setObjectNext(s.asReadOnlyByteBuffer());
      }
      return data;

    } else { // common case
      ByteBuffer bb = dproto.getPrimdata().asReadOnlyByteBuffer();
      bb.order(bo);
      return Array.factory(dataType, section.getShape(), bb);
    }

  }

  // top level vlen
  public Array decodeVlenData(NcStreamProto.DataCol dproto) throws IOException {
    DataType dataType = NcStream.convertDataType(dproto.getDataType());
    ByteBuffer bb = dproto.getPrimdata().asReadOnlyByteBuffer();
    ByteOrder bo = dproto.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    bb.order(bo);
    Array alldata = Array.factory(dataType, new int[]{dproto.getNelems()}, bb); // flat array
    IndexIterator all = alldata.getIndexIterator();

    Section section = NcStream.decodeSection(dproto.getSection());
    Array[] data = new Array[(int) section.computeSize()];

    // divide the primitive data into variable length arrays
    int count = 0;
    for (int len : dproto.getVlensList()) {
      Array primdata = Array.factory(dataType, new int[]{len});
      IndexIterator prim = primdata.getIndexIterator();
      for (int i=0; i<len; i++) {
        prim.setObjectNext( all.getObjectNext()); // generic
      }
      data[count++] = primdata;
    }

    // return Array.makeObjectArray(dataType, data[0].getClass(), section.getShape(), data);
    return Array.makeVlenArray(section.getShape(), data);
  }

  // vlen inside a Structure
  private Array decodeVlenData(NcStreamProto.DataCol dproto, Section parentSection) throws IOException {
    DataType dataType = NcStream.convertDataType(dproto.getDataType());
    ByteBuffer bb = dproto.getPrimdata().asReadOnlyByteBuffer();
    ByteOrder bo = dproto.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    bb.order(bo);
    Array alldata = Array.factory(dataType, new int[]{dproto.getNelems()}, bb); // 1D array
    IndexIterator all = alldata.getIndexIterator();

    int psize = (int) parentSection.computeSize();

    Section section = NcStream.decodeSection(dproto.getSection());
    Section vsection = section.removeFirst(parentSection);
    int vsectionSize = (int) vsection.computeSize(); // the # of varlen Arrays at the inner structure
    // LOOK check for scalar

    // divide the primitive data into variable length arrays
    int countInner = 0;
    Array[] pdata = new Array[psize];
    for (int pCount=0; pCount<psize; pCount++) {
      Array[] vdata = new Array[vsectionSize];
      for (int vCount=0; vCount<vsectionSize; vCount++) {
        int vlen = dproto.getVlens(countInner++);
        Array primdata = Array.factory(dataType, new int[]{vlen});
        IndexIterator prim = primdata.getIndexIterator();
        for (int i = 0; i < vlen; i++) {
          prim.setObjectNext(all.getObjectNext()); // generic
        }
        vdata[vCount] = primdata;
      }
      pdata[pCount] = Array.makeVlenArray(vsection.getShape(), vdata);
    }

    // ArrayObject(parentShape)
    return Array.makeVlenArray(parentSection.getShape(), pdata);
  }


  private Array decodeStructureData(NcStreamProto.DataCol dproto, Section parentSection) throws IOException {
    NcStreamProto.ArrayStructureCol structData = dproto.getStructdata();
    Section section = NcStream.decodeSection(dproto.getSection());
    int nelems = dproto.getNelems();
    assert nelems == section.computeSize();

    // accumulate parent sections
    parentSection = section.prepend(parentSection);

    StructureMembers members = new StructureMembers(dproto.getName());
    for (NcStreamProto.DataCol memberData : structData.getMemberDataList()) {
      decodeMemberData(members, memberData, parentSection);
    }

    return new ArrayStructureMA(members, section.getShape());
  }

  private void decodeMemberData(StructureMembers members, NcStreamProto.DataCol memberData, Section parentSection) throws IOException {
    String name = memberData.getName();
    DataType dataType = NcStream.convertDataType(memberData.getDataType());
    Section section = NcStream.decodeSection(memberData.getSection());
    if (!memberData.getIsVlen()) {
      assert memberData.getNelems() == section.computeSize();
    }
    // the dproto section includes parents, remove them
    Section msection = section.removeFirst(parentSection);
    if (memberData.getIsVlen())
      msection = msection.appendRange(Range.VLEN);

    StructureMembers.Member result = members.addMember(name, null, null, dataType, msection.getShape());
    Array data = decode(memberData, parentSection);
    result.setDataArray(data);
  }

}
