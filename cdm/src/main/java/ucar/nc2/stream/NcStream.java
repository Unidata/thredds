package ucar.nc2.stream;

import ucar.nc2.*;
import ucar.ma2.*;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import ucar.nc2.iosp.IospHelper;

/**
 * Defines the ncstream format, along with ncStream.proto.
 * <pre>
 * To regenerate ncStreamProto.java from ncStream.proto:
 cd c:/dev/tds4.2/thredds/cdm/src/main/java
 protoc --proto_path=. --java_out=. ucar/nc2/stream/ncStream.proto
 * </pre>
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/stream/NcStream.html"
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/stream/NcstreamGrammer.html"
 */
public class NcStream {
  //  must start with this "CDFS"
  static public final byte[] MAGIC_START = new byte[]{0x43, 0x44, 0x46, 0x53};

  static public final byte[] MAGIC_HEADER = new byte[]{(byte) 0xad, (byte) 0xec, (byte) 0xce, (byte) 0xda};
  static public final byte[] MAGIC_DATA =   new byte[]{(byte) 0xab, (byte) 0xec, (byte) 0xce, (byte) 0xba};
  static public final byte[] MAGIC_VDATA =  new byte[]{(byte) 0xab, (byte) 0xef, (byte) 0xfe, (byte) 0xba};    
  static public final byte[] MAGIC_VEND =  new byte[]{(byte) 0xed, (byte) 0xef, (byte) 0xfe, (byte) 0xda};

  static public final byte[] MAGIC_ERR = new byte[]{(byte) 0xab, (byte) 0xad, (byte) 0xba, (byte) 0xda};
  static public final byte[] MAGIC_END = new byte[]{(byte) 0xed, (byte) 0xed, (byte) 0xde, (byte) 0xde};

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
      groupBuilder.addGroups( encodeGroup(ng, sizeToCache));

    return groupBuilder;
  }


  static NcStreamProto.Attribute.Builder encodeAtt(Attribute att) {
    NcStreamProto.Attribute.Builder attBuilder = NcStreamProto.Attribute.newBuilder();
    attBuilder.setName(att.getName());
    attBuilder.setType(encodeAttributeType(att.getDataType()));
    attBuilder.setLen(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        for (int i=0; i<att.getLength(); i++) 
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
    dimBuilder.setName(dim.getName() == null ? "" : dim.getName());
    dimBuilder.setLength(dim.getLength());
    if (!dim.isShared()) dimBuilder.setIsPrivate(true);
    if (dim.isVariableLength()) dimBuilder.setIsVlen(true);
    if (dim.isUnlimited()) dimBuilder.setIsUnlimited(true);
    return dimBuilder;
  }

  static NcStreamProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) throws IOException {
    NcStreamProto.EnumTypedef.Builder builder = NcStreamProto.EnumTypedef.newBuilder();

    builder.setName( enumType.getName());
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
    builder.setDataType(encodeDataType(var.getDataType()));
    if (var.isUnsigned())
      builder.setUnsigned(true);
    if (var.getDataType().isEnum()) {
      EnumTypedef enumType = var.getEnumTypedef();
      if (enumType != null)
        builder.setEnumType(enumType.getName());
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
    builder.setDataType(encodeDataType(s.getDataType()));

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

  static NcStreamProto.Data encodeDataProto(Variable var, Section section) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(var.getFullNameEscaped());
    builder.setDataType(encodeDataType(var.getDataType()));
    builder.setSection(encodeSection(section));
    builder.setVersion(1);
    return builder.build();
  }

  static public NcStreamProto.Section encodeSection(Section section) {
    NcStreamProto.Section.Builder sbuilder = NcStreamProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      NcStreamProto.Range.Builder rbuilder = NcStreamProto.Range.newBuilder();
      rbuilder.setSize(r.length());
      sbuilder.addRange(rbuilder);
    }
    return sbuilder.build();
  }

  public static long encodeArrayStructure(ArrayStructure as, OutputStream os) throws java.io.IOException {
    long size = 0;

    ArrayStructureBB dataBB = IospHelper.makeArrayBB(as);
    List<String>  ss = new ArrayList<String>();
    List<Object> heap = dataBB.getHeap();
    List<Integer> count = new ArrayList<Integer>();
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

    ByteBuffer bb = dataBB.getByteBuffer();
    NcStreamProto.StructureData proto = NcStream.encodeStructureDataProto(bb.array(), count, ss);
    byte[] datab = proto.toByteArray();
    size += NcStream.writeVInt(os, datab.length); // proto len
    os.write(datab); // proto
    size += datab.length;
    // System.out.printf("encodeArrayStructure write sdata size= %d%n", datab.length);

    return size;
  }

  static NcStreamProto.StructureData encodeStructureDataProto(byte[] fixed, List<Integer> count, List<String> ss) {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.setData(ByteString.copyFrom(fixed));
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
        for (int i=0; i<c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB;
  }

  public static StructureData decodeStructureData(StructureMembers sm, byte[] proto) throws java.io.IOException {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.mergeFrom(proto);
    long size = 0;

    ByteBuffer bb = ByteBuffer.wrap(builder.getData().toByteArray());
    ArrayStructureBB dataBB = new ArrayStructureBB(sm, new int[] {1}, bb, 0);

    List<String> ss = builder.getSdataList();
    List<Integer> count = builder.getHeapCountList();

    int scount = 0;
    for (Integer c : count) {
      if (c == 1) {
        dataBB.addObjectToHeap(ss.get(scount++));
      } else {
        String[] hos = new String[c];
        for (int i=0; i<c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB.getStructureData(0);
  }



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

  static public int writeVInt(WritableByteChannel wbc, int value) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(8);

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
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

  static int writeVLong(OutputStream out, long i) throws IOException {
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

  static public String decodeErrorMessage(NcStreamProto.Error err) {
    return err.getMessage();
  }

  static Dimension decodeDim(NcStreamProto.Dimension dim) {
    String name = (dim.getName() == null || dim.getName().length() == 0 ? null : dim.getName());
    return new Dimension(name, (int) dim.getLength(), !dim.getIsPrivate(), dim.getIsUnlimited(), dim.getIsVlen());
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
      g.addGroup( ng);
      readGroup(gp, ncfile, ng);
    }
  }

  static EnumTypedef decodeEnumTypedef(NcStreamProto.EnumTypedef enumType) {
    List<NcStreamProto.EnumTypedef.EnumType> list = enumType.getMapList();
    Map<Integer, String> map = new HashMap<Integer, String>( 2 * list.size());
    for (NcStreamProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    return new EnumTypedef( enumType.getName(), map);
   }

  static Attribute decodeAtt(NcStreamProto.Attribute attp) {
    int len = attp.getLen();
    if (len == 0) // deal with empty attribute
      return new Attribute(attp.getName(), decodeAttributeType(attp.getType()));

    DataType dt = decodeAttributeType(attp.getType());

    if (dt == DataType.STRING) {
      int lenp = attp.getSdataCount();
      if (lenp != len)
        System.out.println("HEY lenp != len");
      if (lenp == 1)
        return new Attribute(attp.getName(), attp.getSdata(0));
      else {
        Array data = Array.factory(dt, new int[] {lenp});
        for (int i=0; i<lenp; i++) data.setObject(i, attp.getSdata(i));
        return new Attribute(attp.getName(), data);
      }
    } else {
      ByteString bs = attp.getData();
      ByteBuffer bb = ByteBuffer.wrap(bs.toByteArray());
      return new Attribute(attp.getName(), Array.factory(decodeAttributeType(attp.getType()), null, bb));
    }
  }

   static Variable decodeVar(NetcdfFile ncfile, Group g, Structure parent, NcStreamProto.Variable var) {
    Variable ncvar = new Variable(ncfile, g, parent, var.getName());
    DataType varType = decodeDataType(var.getDataType());
    ncvar.setDataType(decodeDataType(var.getDataType()));

    if (varType.isEnum()) {
      String enumName = var.getEnumType();
      EnumTypedef enumType = g.findEnumeration(enumName);
      if (enumType != null)
        ncvar.setEnumTypedef(enumType);
    }

    List<Dimension> dims = new ArrayList<Dimension>(6);
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : var.getShapeList()) {
      if (dim.getIsPrivate())
        dims.add(new Dimension(dim.getName(), (int) dim.getLength(), false, dim.getIsUnlimited(), dim.getIsVlen()));
      else {
        Dimension d = g.findDimension(dim.getName());
        if (d == null)
          throw new IllegalStateException("Can find shared dimension "+dim.getName());
        dims.add(d);
      }
    }
    ncvar.setDimensions(dims);

    for (ucar.nc2.stream.NcStreamProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (var.getUnsigned())
      ncvar.addAttribute(new Attribute("_Unsigned", "true"));

    if (var.hasData()) {
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

    ncvar.setDataType(decodeDataType(s.getDataType()));

    List<Dimension> dims = new ArrayList<Dimension>(6);
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : s.getShapeList()) {
      if (dim.getIsPrivate() || dim.getIsVlen())
        dims.add(new Dimension(dim.getName(), (int) dim.getLength(), false, dim.getIsUnlimited(), dim.getIsVlen()));
      else {
        Dimension d = g.findDimension(dim.getName());
        if (d == null)
          throw new IllegalStateException("Can find shared dimension "+dim.getName());
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

  static public Section decodeSection(NcStreamProto.Section proto) {
    Section section = new Section();

    for (ucar.nc2.stream.NcStreamProto.Range pr : proto.getRangeList()) {
      try {
        section.appendRange((int) pr.getStart(), (int) (pr.getStart() + pr.getSize() - 1));
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }
    }
    return section;
  }

  ////////////////////////////////////////////////////////////////

  static ucar.nc2.stream.NcStreamProto.Attribute.Type encodeAttributeType(DataType dtype) {
    switch (dtype) {
      case CHAR:
      case STRING:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.STRING;
      case BYTE:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.BYTE;
      case SHORT:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.SHORT;
      case INT:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.INT;
      case LONG:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.LONG;
      case FLOAT:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.FLOAT;
      case DOUBLE:
        return ucar.nc2.stream.NcStreamProto.Attribute.Type.DOUBLE;
    }
    throw new IllegalStateException("illegal att type " + dtype);
  }

  static public ucar.nc2.stream.NcStreamProto.DataType encodeDataType(DataType dtype) {
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
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

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

  static public DataType decodeDataType(ucar.nc2.stream.NcStreamProto.DataType dtype) {
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
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

}
