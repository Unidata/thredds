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
 */
package ucar.nc2.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

/**
 * Defines the ncstream format, along with ncStream.proto.
 * <pre>
 * To regenerate ncStreamProto.java from ncStream.proto:
 * cd c:/dev/tds4.2/thredds/cdm/src/main/java
 * protoc --proto_path=. --java_out=. ucar/nc2/stream/ncStream.proto
 * </pre>
 *
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/stream/NcStream.html"
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/stream/NcstreamGrammer.html"
 */
public class NcStream {
  //  must start with this "CDFS"
  static public final byte[] MAGIC_START = new byte[]{0x43, 0x44, 0x46, 0x53};

  static public final byte[] MAGIC_HEADER = new byte[]{(byte) 0xad, (byte) 0xec, (byte) 0xce, (byte) 0xda};
  static public final byte[] MAGIC_DATA = new byte[]{(byte) 0xab, (byte) 0xec, (byte) 0xce, (byte) 0xba};
  static public final byte[] MAGIC_DATA2 = new byte[]{(byte) 0xab, (byte) 0xeb, (byte) 0xbe, (byte) 0xba};
  static public final byte[] MAGIC_VDATA = new byte[]{(byte) 0xab, (byte) 0xef, (byte) 0xfe, (byte) 0xba};
  static public final byte[] MAGIC_VEND = new byte[]{(byte) 0xed, (byte) 0xef, (byte) 0xfe, (byte) 0xda};

  static public final byte[] MAGIC_HEADERCOV = new byte[]{(byte) 0xad, (byte) 0xed, (byte) 0xde, (byte) 0xda};
  static public final byte[] MAGIC_DATACOV = new byte[]{(byte) 0xab, (byte) 0xed, (byte) 0xde, (byte) 0xba};

  static public final byte[] MAGIC_ERR = new byte[]{(byte) 0xab, (byte) 0xad, (byte) 0xba, (byte) 0xda};
  static public final byte[] MAGIC_END = new byte[]{(byte) 0xed, (byte) 0xed, (byte) 0xde, (byte) 0xde};

  static public final int ncstream_data_version = 3;

  static NcStreamProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    NcStreamProto.Group.Builder groupBuilder = NcStreamProto.Group.newBuilder();
    groupBuilder.setName(g.getShortName());

    for (Dimension dim : g.getDimensions())
      groupBuilder.addDims(NcStream.encodeDim(dim));

    for (Attribute att : g.getAttributes())
      groupBuilder.addAtts(NcStream.encodeAtt(att));

    for (EnumTypedef enumType : g.getEnumTypedefs())
      groupBuilder.addEnumTypes(NcStream.encodeEnumTypedef(enumType));

    for (Variable var : g.getVariables()) {
      if (var instanceof Structure)
        groupBuilder.addStructs(NcStream.encodeStructure((Structure) var));
      else
        groupBuilder.addVars(NcStream.encodeVar(var, sizeToCache));
    }

    for (Group ng : g.getGroups())
      groupBuilder.addGroups(encodeGroup(ng, sizeToCache));

    return groupBuilder;
  }


  static public NcStreamProto.Attribute.Builder encodeAtt(Attribute att) {
    NcStreamProto.Attribute.Builder attBuilder = NcStreamProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setDataType(convertDataType(att.getDataType()));
    attBuilder.setLen(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        for (int i = 0; i < att.getLength(); i++)
          attBuilder.addSdata(att.getStringValue(i));

      } else {
        Array data = att.getValues();
        ByteBuffer bb = data.getDataAsByteBuffer();
        attBuilder.setData(ByteString.copyFrom(bb.array()));
      }
    }

    return attBuilder;
  }

  static NcStreamProto.Dimension.Builder encodeDim(Dimension dim) {
    NcStreamProto.Dimension.Builder dimBuilder = NcStreamProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  static NcStreamProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) throws IOException {
    NcStreamProto.EnumTypedef.Builder builder = NcStreamProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    Map<Integer, String> map = enumType.getMap();
    NcStreamProto.EnumTypedef.EnumType.Builder b2 = NcStreamProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMap(b2);
    }
    return builder;
  }

  static NcStreamProto.Variable.Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    NcStreamProto.Variable.Builder builder = NcStreamProto.Variable.newBuilder();
    builder.setName(var.getShortName());
    builder.setDataType(convertDataType(var.getDataType()));
    if (var.getDataType().isEnum()) {
      EnumTypedef enumType = var.getEnumTypedef();
      if (enumType != null)
        builder.setEnumType(enumType.getShortName());
    }

    for (Dimension dim : var.getDimensions()) {
      builder.addShape(encodeDim(dim));
    }

    for (Attribute att : var.getAttributes()) {
      builder.addAtts(encodeAtt(att));
    }

    // put small amounts of data in header "immediate mode"
    if (var.isCaching() && var.getDataType().isNumeric()) {
      if (var.isCoordinateVariable() || var.getSize() * var.getElementSize() < sizeToCache) {
        Array data = var.read();
        ByteBuffer bb = data.getDataAsByteBuffer();
        builder.setData(ByteString.copyFrom(bb.array()));
      }
    }

    return builder;
  }

  static NcStreamProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    NcStreamProto.Structure.Builder builder = NcStreamProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setDataType(convertDataType(s.getDataType()));

    for (Dimension dim : s.getDimensions())
      builder.addShape(encodeDim(dim));

    for (Attribute att : s.getAttributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(NcStream.encodeStructure((Structure) v));
      else
        builder.addVars(NcStream.encodeVar(v, -1));
    }

    return builder;
  }

  static public NcStreamProto.Error encodeErrorMessage(String message) {
    NcStreamProto.Error.Builder builder = NcStreamProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  static NcStreamProto.Data encodeDataProto(Variable var, Section section, NcStreamProto.Compress compressionType,
                                            ByteOrder bo, int uncompressedLength) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(var.getFullNameEscaped());
    builder.setDataType(convertDataType(var.getDataType()));
    builder.setSection(encodeSection(section));
    builder.setCompress(compressionType);
    if (compressionType != NcStreamProto.Compress.NONE) {
      builder.setUncompressedSize(uncompressedLength);
    }
    builder.setVdata(var.isVariableLength());
    builder.setBigend(bo == ByteOrder.BIG_ENDIAN);
    builder.setVersion(ncstream_data_version);
    return builder.build();
  }

  static public NcStreamProto.Data encodeDataProto(String varname, DataType datatype, Section section, boolean deflate, int uncompressedLength) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(varname);
    builder.setDataType(convertDataType(datatype));
    builder.setSection(encodeSection(section));
    if (deflate) {
      builder.setCompress(NcStreamProto.Compress.DEFLATE);
      builder.setUncompressedSize(uncompressedLength);
    }
    builder.setVersion(ncstream_data_version);
    return builder.build();
  }

  static public NcStreamProto.Section encodeSection(Section section) {
    NcStreamProto.Section.Builder sbuilder = NcStreamProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      NcStreamProto.Range.Builder rbuilder = NcStreamProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRange(rbuilder);
    }
    return sbuilder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////


  static void show(NcStreamProto.Header proto) throws InvalidProtocolBufferException {
    NcStreamProto.Group root = proto.getRoot();

    for (NcStreamProto.Dimension dim : root.getDimsList()) {
      System.out.println("dim= " + dim);
    }

    for (NcStreamProto.Attribute att : root.getAttsList()) {
      System.out.println("att= " + att);
    }

    for (NcStreamProto.Variable var : root.getVarsList()) {
      System.out.println("var= " + var);
    }
  }

  ////////////////////////////////////////////////////////////

  static int writeByte(OutputStream out, byte b) throws IOException {
    out.write(b);
    return 1;
  }

  static int writeBytes(OutputStream out, byte[] b, int offset, int length) throws IOException {
    out.write(b, offset, length);
    return length;
  }

  static public int writeBytes(OutputStream out, byte[] b) throws IOException {
    return writeBytes(out, b, 0, b.length);
  }

  static public int writeVInt(OutputStream out, int value) throws IOException {
    int count = 0;

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
    while (true) {
      if ((value & ~0x7F) == 0) {
        writeByte(out, (byte) value);
        break;
      } else {
        writeByte(out, (byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }

  static public int writeVInt(RandomAccessFile out, int value) throws IOException {
    int count = 0;

    while (true) {
      if ((value & ~0x7F) == 0) {
        out.write((byte) value);
        break;
      } else {
        out.write((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }


  static public int writeVInt(WritableByteChannel wbc, int value) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(8);

    while (true) {
      if ((value & ~0x7F) == 0) {
        bb.put((byte) value);
        break;
      } else {
        bb.put((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    bb.flip();
    wbc.write(bb);
    return bb.limit();
  }

  static public int writeVLong(OutputStream out, long i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte(out, (byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte(out, (byte) i);
    return count + 1;
  }

  static public int readVInt(InputStream is) throws IOException {
    int ib = is.read();
    if (ib == -1) return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = is.read();
      if (ib == -1) return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  static public int readVInt(RandomAccessFile raf) throws IOException {
    int ib = raf.read();
    if (ib == -1) return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = raf.read();
      if (ib == -1) return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  static public int readFully(InputStream is, byte[] b) throws IOException {
    int done = 0;
    int want = b.length;
    while (want > 0) {
      int bytesRead = is.read(b, done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  static public boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    readFully(is, b);

    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  static public boolean readAndTest(RandomAccessFile raf, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    raf.readFully(b);

    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  static public boolean test(byte[] b, byte[] test) throws IOException {
    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  static public String decodeErrorMessage(NcStreamProto.Error err) {
    return err.getMessage();
  }

  static Dimension decodeDim(NcStreamProto.Dimension dim) {
    String name = (dim.getName().length() == 0 ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return new Dimension(name, dimLen, !dim.getIsPrivate(), dim.getIsUnlimited(), dim.getIsVlen());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static void readGroup(NcStreamProto.Group proto, NetcdfFile ncfile, Group g) throws InvalidProtocolBufferException {

    for (NcStreamProto.Dimension dim : proto.getDimsList())
      g.addDimension(NcStream.decodeDim(dim));

    for (NcStreamProto.Attribute att : proto.getAttsList())
      g.addAttribute(NcStream.decodeAtt(att));

    for (NcStreamProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumeration(NcStream.decodeEnumTypedef(enumType));

    for (NcStreamProto.Variable var : proto.getVarsList())
      g.addVariable(NcStream.decodeVar(ncfile, g, null, var));

    for (NcStreamProto.Structure s : proto.getStructsList())
      g.addVariable(NcStream.decodeStructure(ncfile, g, null, s));

    for (NcStreamProto.Group gp : proto.getGroupsList()) {
      Group ng = new Group(ncfile, g, gp.getName());
      g.addGroup(ng);
      readGroup(gp, ncfile, ng);
    }
  }

  static EnumTypedef decodeEnumTypedef(NcStreamProto.EnumTypedef enumType) {
    List<NcStreamProto.EnumTypedef.EnumType> list = enumType.getMapList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (NcStreamProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    return new EnumTypedef(enumType.getName(), map);
  }

  static public Attribute decodeAtt(NcStreamProto.Attribute attp) {
    // BARF LOOK
    DataType dtOld = decodeAttributeType(attp.getType());
    DataType dtNew = convertDataType(attp.getDataType());
    DataType dtUse;
    if (dtNew != DataType.CHAR) dtUse = dtNew;
    else if (dtOld != DataType.STRING) dtUse = dtOld;
    else if (attp.getSdataCount() > 0) dtUse = DataType.STRING;
    else dtUse = DataType.CHAR;

    int len = attp.getLen();
    if (len == 0) // deal with empty attribute
      return new Attribute(attp.getName(), dtUse);

    if (dtUse == DataType.STRING) {
      int lenp = attp.getSdataCount();
      if (lenp != len)
        System.out.println("HEY lenp != len");
      if (lenp == 1)
        return new Attribute(attp.getName(), attp.getSdata(0));
      else {
        Array data = Array.factory(dtUse, new int[]{lenp});
        for (int i = 0; i < lenp; i++) data.setObject(i, attp.getSdata(i));
        return new Attribute(attp.getName(), data);
      }
    } else {
      ByteString bs = attp.getData();
      ByteBuffer bb = ByteBuffer.wrap(bs.toByteArray());
      return new Attribute(attp.getName(), Array.factory(dtUse, (int[]) null, bb)); // if null, then use int[]{bb
      // .limit()}
    }
  }

  static Variable decodeVar(NetcdfFile ncfile, Group g, Structure parent, NcStreamProto.Variable var) {
    Variable ncvar = new Variable(ncfile, g, parent, var.getName());
    DataType varType = convertDataType(var.getDataType());
    ncvar.setDataType(convertDataType(var.getDataType()));

    if (varType.isEnum()) {
      String enumName = var.getEnumType();
      EnumTypedef enumType = g.findEnumeration(enumName);
      if (enumType != null)
        ncvar.setEnumTypedef(enumType);
    }

    List<Dimension> dims = new ArrayList<>(6);
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : var.getShapeList()) {
      Dimension ncdim = decodeDim(dim);
      if (!ncdim.isShared())
        dims.add(ncdim);
      else {
        Dimension d = g.findDimension(ncdim.getShortName());
        if (d == null)
          throw new IllegalStateException("Can find shared dimension " + dim.getName());
        dims.add(d);
      }
    }
    ncvar.setDimensions(dims);

    for (ucar.nc2.stream.NcStreamProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (!var.getData().isEmpty()) {
      // LOOK may mess with ability to change var size later.
      ByteBuffer bb = ByteBuffer.wrap(var.getData().toByteArray());
      Array data = Array.factory(varType, ncvar.getShape(), bb);
      ncvar.setCachedData(data, false);
    }

    return ncvar;
  }

  static Structure decodeStructure(NetcdfFile ncfile, Group g, Structure parent, NcStreamProto.Structure s) {
    Structure ncvar = (s.getDataType() == ucar.nc2.stream.NcStreamProto.DataType.SEQUENCE) ?
            new Sequence(ncfile, g, parent, s.getName()) : new Structure(ncfile, g, parent, s.getName());

    ncvar.setDataType(convertDataType(s.getDataType()));

    List<Dimension> dims = new ArrayList<>(6);
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : s.getShapeList()) {
      Dimension ncdim = decodeDim(dim);
      if (!ncdim.isShared())
        dims.add(ncdim);
      else {
        Dimension d = g.findDimension(ncdim.getShortName());
        if (d == null)
          throw new IllegalStateException("Can find shared dimension " + dim.getName());
        dims.add(d);
      }
    }
    ncvar.setDimensions(dims);

    for (ucar.nc2.stream.NcStreamProto.Attribute att : s.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    for (ucar.nc2.stream.NcStreamProto.Variable vp : s.getVarsList())
      ncvar.addMemberVariable(decodeVar(ncfile, g, ncvar, vp));

    for (NcStreamProto.Structure sp : s.getStructsList())
      ncvar.addMemberVariable(decodeStructure(ncfile, g, ncvar, sp));

    return ncvar;
  }

  @Nonnull
  static public Section decodeSection(NcStreamProto.Section proto) {
    Section section = new Section();

    for (ucar.nc2.stream.NcStreamProto.Range pr : proto.getRangeList()) {
      try {
        long stride = pr.getStride();
        if (stride == 0) stride = 1; // default in protobuf2 was 1, but protobuf3 is 0, luckily 0 is illegal
        if (pr.getSize() == 0)
          section.appendRange(Range.EMPTY); // used for scalars LOOK really used ??
        else {
          // this.last = first + (this.length-1) * stride;
          section.appendRange((int) pr.getStart(), (int) (pr.getStart() + (pr.getSize() - 1) * stride), (int) stride);
        }

      } catch (InvalidRangeException e) {
        throw new RuntimeException("Bad Section in ncstream", e);
      }
    }
    return section;
  }

  /* decodeDataByteOrder

    proto2:
    message Data {
      required string varName = 1;          // full escaped name.
      required DataType dataType = 2;
      optional Section section = 3;         // not required for SEQUENCE
      optional bool bigend = 4 [default = true];
      optional uint32 version = 5 [default = 0];
      optional Compress compress = 6 [default = NONE];
      optional bool vdata = 7 [default = false];
      optional uint32 uncompressedSize = 8;
    }

    problem is that bigend default is true, but in proto3 it must be false. so we need to detect if the value is set or not.
    thanks to Simon (Vsevolod) Ilyushchenko <simonf@google.com>, workaround is:

    proto3:
    message Data {
      string varName = 1;          // full escaped name.
      DataType dataType = 2;
      Section section = 3;         // not required for SEQUENCE
      oneof bigend_present {
        bool bigend = 4;           // [default=true] in proto2
      }
      uint32 version = 5;          // < 3 for proto2, = 3 for proto3 (v5.0+)
      Compress compress = 6;
      bool vdata = 7;
      uint32 uncompressedSize = 8;
    }

    which is wire-compatible and allows us to detect if value is set or not.
  */
  static ByteOrder decodeDataByteOrder(NcStreamProto.Data pData) {
    boolean isMissing = pData.getBigendPresentCase() == NcStreamProto.Data.BigendPresentCase.BIGENDPRESENT_NOT_SET;
    if (isMissing) {
      int version = pData.getVersion();
      return (version < 3) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }
    return pData.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
  }

  ////////////////////////////////////////////////////////////////

  static public ucar.nc2.stream.NcStreamProto.DataType convertDataType(DataType dtype) {
    switch (dtype) {
      case CHAR:
        return ucar.nc2.stream.NcStreamProto.DataType.CHAR;
      case BYTE:
        return ucar.nc2.stream.NcStreamProto.DataType.BYTE;
      case SHORT:
        return ucar.nc2.stream.NcStreamProto.DataType.SHORT;
      case INT:
        return ucar.nc2.stream.NcStreamProto.DataType.INT;
      case LONG:
        return ucar.nc2.stream.NcStreamProto.DataType.LONG;
      case FLOAT:
        return ucar.nc2.stream.NcStreamProto.DataType.FLOAT;
      case DOUBLE:
        return ucar.nc2.stream.NcStreamProto.DataType.DOUBLE;
      case STRING:
        return ucar.nc2.stream.NcStreamProto.DataType.STRING;
      case STRUCTURE:
        return ucar.nc2.stream.NcStreamProto.DataType.STRUCTURE;
      case SEQUENCE:
        return ucar.nc2.stream.NcStreamProto.DataType.SEQUENCE;
      case ENUM1:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM1;
      case ENUM2:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM2;
      case ENUM4:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM4;
      case OPAQUE:
        return ucar.nc2.stream.NcStreamProto.DataType.OPAQUE;
      case UBYTE:
        return ucar.nc2.stream.NcStreamProto.DataType.UBYTE;
      case USHORT:
        return ucar.nc2.stream.NcStreamProto.DataType.USHORT;
      case UINT:
        return ucar.nc2.stream.NcStreamProto.DataType.UINT;
      case ULONG:
        return ucar.nc2.stream.NcStreamProto.DataType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  static public DataType convertDataType(ucar.nc2.stream.NcStreamProto.DataType dtype) {
    switch (dtype) {
      case CHAR:
        return DataType.CHAR;
      case BYTE:
        return DataType.BYTE;
      case SHORT:
        return DataType.SHORT;
      case INT:
        return DataType.INT;
      case LONG:
        return DataType.LONG;
      case FLOAT:
        return DataType.FLOAT;
      case DOUBLE:
        return DataType.DOUBLE;
      case STRING:
        return DataType.STRING;
      case STRUCTURE:
        return DataType.STRUCTURE;
      case SEQUENCE:
        return DataType.SEQUENCE;
      case ENUM1:
        return DataType.ENUM1;
      case ENUM2:
        return DataType.ENUM2;
      case ENUM4:
        return DataType.ENUM4;
      case OPAQUE:
        return DataType.OPAQUE;
      case UBYTE:
        return DataType.UBYTE;
      case USHORT:
        return DataType.USHORT;
      case UINT:
        return DataType.UINT;
      case ULONG:
        return DataType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  /////////////////////
  // < 5.0

  static DataType decodeAttributeType(ucar.nc2.stream.NcStreamProto.Attribute.Type dtype) {
    switch (dtype) {
      case STRING:
        return DataType.STRING;
      case BYTE:
        return DataType.BYTE;
      case SHORT:
        return DataType.SHORT;
      case INT:
        return DataType.INT;
      case LONG:
        return DataType.LONG;
      case FLOAT:
        return DataType.FLOAT;
      case DOUBLE:
        return DataType.DOUBLE;
    }
    throw new IllegalStateException("illegal att type " + dtype);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static long encodeArrayStructure(ArrayStructure as, ByteOrder bo, OutputStream os) throws java.io.IOException {
    long size = 0;

    ArrayStructureBB dataBB = StructureDataDeep.copyToArrayBB(as, bo, true);  // force canonical packing
    List<String> ss = new ArrayList<>();
    List<Object> heap = dataBB.getHeap();
    List<Integer> count = new ArrayList<>();
    if (heap != null) {
      for (Object ho : heap) {
        if (ho instanceof String) {
          count.add(1);
          ss.add((String) ho);
        } else if (ho instanceof String[]) {
          String[] hos = (String[]) ho;
          count.add(hos.length);
          for (String s : hos)
            ss.add(s);
        }
      }
    }

    // LOOK optionally compress
    StructureMembers sm = dataBB.getStructureMembers();
    ByteBuffer bb = dataBB.getByteBuffer();
    NcStreamProto.StructureData proto = NcStream.encodeStructureDataProto(bb.array(), count, ss, (int) as.getSize(), sm.getStructureSize());
    byte[] datab = proto.toByteArray();
    size += NcStream.writeVInt(os, datab.length); // proto len
    os.write(datab); // proto
    size += datab.length;
    // System.out.printf("encodeArrayStructure write sdata size= %d%n", datab.length);

    return size;
  }

  static NcStreamProto.StructureData encodeStructureDataProto(byte[] fixed, List<Integer> count, List<String> ss, int nrows, int rowLength) {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.setData(ByteString.copyFrom(fixed));
    builder.setNrows(nrows);
    builder.setRowLength(rowLength);
    for (Integer c : count)
      builder.addHeapCount(c);
    for (String s : ss)
      builder.addSdata(s);
    return builder.build();
  }

  public static ArrayStructureBB decodeArrayStructure(StructureMembers sm, int shape[], byte[] proto) throws java.io.IOException {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.mergeFrom(proto);
    long size = 0;

    ByteBuffer bb = ByteBuffer.wrap(builder.getData().toByteArray());
    ArrayStructureBB dataBB = new ArrayStructureBB(sm, shape, bb, 0);

    List<String> ss = builder.getSdataList();
    List<Integer> count = builder.getHeapCountList();

    int scount = 0;
    for (Integer c : count) {
      if (c == 1) {
        dataBB.addObjectToHeap(ss.get(scount++));
      } else {
        String[] hos = new String[c];
        for (int i = 0; i < c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB;
  }

  public static StructureData decodeStructureData(StructureMembers sm, ByteOrder bo, byte[] proto) throws java.io.IOException {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.mergeFrom(proto);

    ByteBuffer bb = ByteBuffer.wrap(builder.getData().toByteArray());
    bb.order(bo);
    ArrayStructureBB dataBB = new ArrayStructureBB(sm, new int[]{1}, bb, 0);

    List<String> ss = builder.getSdataList();
    List<Integer> count = builder.getHeapCountList();

    int scount = 0;
    for (Integer c : count) {
      if (c == 1) {
        dataBB.addObjectToHeap(ss.get(scount++));
      } else {
        String[] hos = new String[c];
        for (int i = 0; i < c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB.getStructureData(0);
  }

}
