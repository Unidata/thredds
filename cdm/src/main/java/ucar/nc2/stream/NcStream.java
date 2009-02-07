package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.Range;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;


public class NcStream {
  // CDFS ecceada0
  static final byte[] MAGIC_HEADER = new byte[]{0x43, 0x44, 0x46, 0x53, (byte) 0xec, (byte) 0xce, (byte) 0xad, (byte) 0xa0};
  static final byte[] MAGIC_DATA = new byte[]{(byte) 0xa1, (byte) 0x1a, (byte) 0xad, (byte) 0xa0};

  private NetcdfFile ncfile;
  private NcStreamProto.Stream proto;

  public NcStream(NetcdfFile ncfile) throws InvalidProtocolBufferException {
    this.ncfile = ncfile;
    NcStreamProto.Group.Builder rootBuilder = NcStreamProto.Group.newBuilder();
    rootBuilder.setName("");

    for (Dimension dim : ncfile.getDimensions()) {
      rootBuilder.addDims(makeDim(dim));
    }

    for (Attribute att : ncfile.getGlobalAttributes()) {
      rootBuilder.addAtts(makeAtt(att));
    }

    for (Variable var : ncfile.getVariables()) {
      rootBuilder.addVars(makeVar(var));
    }

    NcStreamProto.Stream.Builder streamBuilder = NcStreamProto.Stream.newBuilder();
    streamBuilder.setName(ncfile.getLocation());
    streamBuilder.setRoot(rootBuilder);
    streamBuilder.setIndexPos(0);

    proto = streamBuilder.build();
  }

  private NcStreamProto.Attribute.Builder makeAtt(Attribute att) {
    NcStreamProto.Attribute.Builder attBuilder = NcStreamProto.Attribute.newBuilder();
    attBuilder.setName(att.getName());
    attBuilder.setType(convertType(att.getDataType()));
    attBuilder.setLen(att.getLength());
    attBuilder.setData(getAttData(att));
    return attBuilder;
  }

  private NcStreamProto.Dimension.Builder makeDim(Dimension dim) {
    NcStreamProto.Dimension.Builder dimBuilder = NcStreamProto.Dimension.newBuilder();
    dimBuilder.setName(dim.getName());
    dimBuilder.setLength(dim.getLength());
    if (!dim.isShared()) dimBuilder.setIsPrivate(true);
    if (dim.isVariableLength()) dimBuilder.setIsVlen(true);
    if (dim.isUnlimited()) dimBuilder.setIsUnlimited(true);
    return dimBuilder;
  }

  private NcStreamProto.Variable.Builder makeVar(Variable var) {
    NcStreamProto.Variable.Builder builder = NcStreamProto.Variable.newBuilder();
    builder.setName(var.getShortName());
    builder.setDataType(convertDataType(var.getDataType()));

    for (Dimension dim : var.getDimensions()) {
      builder.addShape(makeDim(dim));
    }

    for (Attribute att : var.getAttributes()) {
      builder.addAtts(makeAtt(att));
    }
    return builder;
  }

  private NcStreamProto.Data makeDataProto(Variable var) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(var.getName());
    builder.setDataType(convertDataType(var.getDataType()));

    NcStreamProto.Section.Builder sbuilder = NcStreamProto.Section.newBuilder();
    for (Range r : var.getShapeAsSection().getRanges()) {
      NcStreamProto.Range.Builder rbuilder = NcStreamProto.Range.newBuilder();
      rbuilder.setSize(r.length());
      sbuilder.addRange(rbuilder);
    }
    builder.setSection(sbuilder);
    return builder.build();
  }

  void show(NcStreamProto.Stream proto) throws InvalidProtocolBufferException {
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

  private com.google.protobuf.ByteString getAttData(Attribute att) {
    if (att.getDataType().isString()) {
      String val = att.getStringValue();
      return ByteString.copyFromUtf8(val);
    }

    Array data = att.getValues();
    ByteBuffer bb = data.getDataAsByteBuffer();

    return new ByteString(bb.array());
  }

  ////////////////////////////////////////////////////////////

  public void stream(WritableByteChannel wbc) throws IOException {
    OutputStream out = Channels.newOutputStream(wbc);
    long size = 0;

    //// header message
    size += writeBytes(out, MAGIC_HEADER); // magic
    byte[] b = proto.toByteArray();
    size += writeVInt(out, b.length); // len
    System.out.println("Header len="+b.length);
    // payload
    size += writeBytes(out, b);

    // data messages
    for (Variable v : ncfile.getVariables()) {
      // magic
      size += writeBytes(out, MAGIC_DATA); // magic
      NcStreamProto.Data dataProto= makeDataProto(v);
      byte[] datab = dataProto.toByteArray();
      size += writeVInt(out, datab.length); // proto len
      size += writeBytes(out, datab); //proto
      System.out.println(v.getName()+" proto len="+datab.length);

      long len = v.getSize() * v.getElementSize();
      size += writeVInt(out, (int) len); // data len
      System.out.println(v.getName()+" data len="+len);
      out.flush();

      long readCount = ncfile.readToByteChannel(v, wbc);
      assert readCount == len;
      size += len;
    }
    System.out.println(" total size="+size);
  }

  private int writeByte(OutputStream out, byte b) throws IOException {
    out.write(b);
    return 1;
  }

  private int writeBytes(OutputStream out, byte[] b, int offset, int length) throws IOException {
    out.write(b, offset, length);
    return length;
  }

  private int writeBytes(OutputStream out, byte[] b) throws IOException {
    return writeBytes(out, b, 0, b.length);
  }

  private int writeVInt(OutputStream out, int i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte(out, (byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte(out, (byte) i);
    return count + 1;
  }

  /**
   * Writes an long in a variable-length format.  Writes between one and five
   * bytes.  Smaller values take fewer bytes.  Negative numbers are not
   * supported.
   */
  private int writeVLong(OutputStream out, long i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte(out, (byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte(out, (byte) i);
    return count + 1;
  }

  ////////////////////////////////////////////////////////////
  public NetcdfFile readStream(InputStream is) throws IOException {
    assert readAndTest(is, MAGIC_HEADER);

    int msize = readVInt(is);
    System.out.println("READ header len= "+msize);

    byte[] m = new byte[ msize];
    is.read(m);
    proto = NcStreamProto.Stream.parseFrom(m);
    ncfile = proto2nc( proto);

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    while (is.available() > 0) {
      assert readAndTest(is, MAGIC_DATA);

      int psize = readVInt(is);
      System.out.println(" dproto len= "+psize);
      byte[] dp = new byte[ psize];
      is.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      System.out.println(" dproto = "+dproto);

      int dsize = readVInt(is);
      System.out.println(" data len= "+dsize);
      is.skip(dsize);
    }

    return ncfile;

  }

  private int readVInt(InputStream is) throws IOException {
    byte b = (byte) is.read();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = (byte) is.read();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  private boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    is.read(b);

    if (b.length != test.length) return false;
    for (int i=0; i<b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  public NetcdfFile proto2nc(NcStreamProto.Stream proto) throws InvalidProtocolBufferException {
    NetcdfFile ncfile = new NetcdfFileStream();
    ncfile.setLocation( proto.getName());

    NcStreamProto.Group root = proto.getRoot();

    for (NcStreamProto.Dimension dim : root.getDimsList()) {
      ncfile.addDimension( null, makeDim( dim));
    }

    for (NcStreamProto.Attribute att : root.getAttsList()) {
      ncfile.addAttribute( null, makeAtt( att));
    }

    for (NcStreamProto.Variable var : root.getVarsList()) {
      ncfile.addVariable( null, makeVar( ncfile, var));
    }

    return ncfile;
  }

  private Dimension makeDim(NcStreamProto.Dimension dim) {
    return new Dimension( dim.getName(), (int) dim.getLength(), !dim.getIsPrivate(), dim.getIsUnlimited(), dim.getIsVlen());
  }

  private Attribute makeAtt(NcStreamProto.Attribute att) {
    ByteString bs = att.getData();
    if (att.getType() == ucar.nc2.stream.NcStreamProto.Attribute.Type.STRING) {
      return new Attribute(att.getName(), bs.toStringUtf8());
    }

    ByteBuffer bb = ByteBuffer.wrap( bs.toByteArray());
    return new Attribute(att.getName(), Array.factory( convertType(att.getType()), null,  bb));
  }

  private Variable makeVar(NetcdfFile ncfile , NcStreamProto.Variable var) {
    Variable ncvar = new Variable(ncfile, null, null, var.getName());
    ncvar.setDataType(convertDataType(var.getDataType()));

    StringBuilder sbuff = new StringBuilder();
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : var.getShapeList()) {
      sbuff.append(dim.getName());
      sbuff.append(" ");
    }
    ncvar.setDimensions(sbuff.toString());

    for (ucar.nc2.stream.NcStreamProto.Attribute att : var.getAttsList()) {
      ncvar.addAttribute(makeAtt(att));
    }

    return ncvar;
  }

  private class NetcdfFileStream extends NetcdfFile {

  }


  ////////////////////////////////////////////////////////////////

  private ucar.nc2.stream.NcStreamProto.Attribute.Type convertType(DataType dtype) {
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

  private ucar.nc2.stream.NcStreamProto.DataType convertDataType(DataType dtype) {
    switch (dtype) {
      case CHAR:
      case STRING:
        return ucar.nc2.stream.NcStreamProto.DataType.STRING;
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
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  private DataType convertType(ucar.nc2.stream.NcStreamProto.Attribute.Type dtype) {
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

  private DataType convertDataType(ucar.nc2.stream.NcStreamProto.DataType dtype) {
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
    throw new IllegalStateException("illegal data type " + dtype);
  }

}
