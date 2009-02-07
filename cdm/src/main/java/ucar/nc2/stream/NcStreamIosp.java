package ucar.nc2.stream;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.protobuf.ByteString;

public class NcStreamIosp extends AbstractIOServiceProvider {

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    return readAndTest(raf, NcStream.MAGIC_HEADER);
  }

  private RandomAccessFile raf;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    raf.seek(0);
    assert readAndTest(raf, NcStream.MAGIC_HEADER);

    int msize = readVInt(raf);
    System.out.println("READ header len= " + msize);

    byte[] m = new byte[msize];
    raf.read(m);
    NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(m);

    NcStreamProto.Group root = proto.getRoot();

    for (NcStreamProto.Dimension dim : root.getDimsList()) {
      ncfile.addDimension(null, makeDim(dim));
    }

    for (NcStreamProto.Attribute att : root.getAttsList()) {
      ncfile.addAttribute(null, makeAtt(att));
    }

    for (NcStreamProto.Variable var : root.getVarsList()) {
      ncfile.addVariable(null, makeVar(ncfile, var));
    }

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    while (!raf.isAtEndOfFile()) {
      assert readAndTest(raf, NcStream.MAGIC_DATA);

      int psize = readVInt(raf);
      System.out.println(" dproto len= " + psize);
      byte[] dp = new byte[psize];
      raf.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      System.out.println(" dproto = " + dproto);

      int dsize = readVInt(raf);
      System.out.println(" data len= " + dsize);

      DataSection dataSection = new DataSection();
      dataSection.size = dsize;
      dataSection.filePos = raf.getFilePointer();
      dataSection.section = makeSection( dproto.getSection());

      Variable v = ncfile.getRootGroup().findVariable( dproto.getVarName());
      v.setSPobject(dataSection);

      raf.skipBytes(dsize);
    }
  }

  private class DataSection {
    int size;
    long filePos;
    Section section;
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    DataSection dataSection = (DataSection) v.getSPobject();

    raf.seek(dataSection.filePos);
    byte[] data = new byte[ dataSection.size];
    raf.read(data);

    Array dataArray = Array.factory( v.getDataType(), v.getShape(), ByteBuffer.wrap( data));
    return dataArray.section( section.getRanges());
  }

  public void close() throws IOException {
    raf.close();
  }

  private int readVInt(RandomAccessFile raf) throws IOException {
    byte b = (byte) raf.read();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = (byte) raf.read();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  private boolean readAndTest(RandomAccessFile raf, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    raf.read(b);

    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  private Dimension makeDim(NcStreamProto.Dimension dim) {
    return new Dimension( dim.getName(), (int) dim.getLength(), !dim.getIsPrivate(), dim.getIsUnlimited(), dim.getIsVlen());
  }

  private Attribute makeAtt(NcStreamProto.Attribute att) {
    ByteString bs = att.getData();
    if (att.getType() == ucar.nc2.stream.NcStreamProto.Attribute.Type.STRING) {
      return new Attribute(att.getName(), bs.toStringUtf8());
    }

    ByteBuffer bb = ByteBuffer.wrap(bs.toByteArray());
    return new Attribute(att.getName(), Array.factory(convertType(att.getType()), null, bb));
  }

  private Variable makeVar(NetcdfFile ncfile, NcStreamProto.Variable var) {
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

  private Section makeSection(NcStreamProto.Section proto) {
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
