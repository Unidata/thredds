package ucar.nc2.stream;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * IOSP to read ncStream file (RandomAccessFile), into a NetcdfFile.
 */
public class NcStreamIosp extends AbstractIOServiceProvider {
  private static final boolean debug = false;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    if (!readAndTest(raf, NcStream.MAGIC_START)) return false; // must start with these 4 bytes
    byte[] b = new byte[4];
    raf.read(b);
    return test(b, NcStream.MAGIC_HEADER) || test(b, NcStream.MAGIC_DATA); // immed followed by one of these
  }

  public String getFileTypeId() {
    return "ncstream";
  }

  public String getFileTypeDescription() {
    return "netCDF streaming protocol";
  }

  //////////////////////////////////////////////////////////////////////

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    try {
      this.raf = raf;
      raf.seek(0);
      if (!readAndTest(raf, NcStream.MAGIC_START))
        throw new IOException("Data corrupted on "+ncfile.getLocation());

      // assume for the moment its always starts with one header message
      if(!readAndTest(raf, NcStream.MAGIC_HEADER))
        throw new IOException("Data corrupted on "+ncfile.getLocation());

      int msize = readVInt(raf);
      if (debug) System.out.printf("READ header len= %d%n", msize);

      byte[] m = new byte[msize];
      raf.read(m);
      NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);

      NcStreamProto.Group root = proto.getRoot();
      NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
      ncfile.finish();

      // LOOK why doesnt this work ?
      //CodedInputStream cis = CodedInputStream.newInstance(is);
      //cis.setSizeLimit(msize);
      //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

      while (!raf.isAtEndOfFile()) {
        if (debug) System.out.printf("READ message at = %d%n", raf.getFilePointer());
        byte[] b = new byte[4];
        raf.read(b);
        if (test(b, NcStream.MAGIC_END))
          break;
        else if (!test(b, NcStream.MAGIC_DATA))
          throw new IllegalStateException("bad format");

        int psize = readVInt(raf);
        if (debug) System.out.println(" dproto len= " + psize);
        byte[] dp = new byte[psize];
        raf.read(dp);
        NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
        if (debug) System.out.println(" dproto = " + dproto);

        int dsize = readVInt(raf);
        if (debug) System.out.println(" data len= " + dsize);

        DataSection dataSection = new DataSection();
        dataSection.size = dsize;
        dataSection.filePos = raf.getFilePointer();
        dataSection.section = NcStream.decodeSection(dproto.getSection());

        Variable v = ncfile.getRootGroup().findVariable(dproto.getVarName());
        v.setSPobject(dataSection);

        raf.skipBytes(dsize);
      }

    } catch (Throwable t) {
      throw new RuntimeException("NcStreamIosp: "+t.getMessage(), t);
    }
  }


  private class DataSection {
    int size;
    long filePos;
    Section section;

    @Override
    public String toString() {
      return "DataSection{" +
              "size=" + size +
              ", filePos=" + filePos +
              ", section=" + section +
              '}';
    }
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    DataSection dataSection = (DataSection) v.getSPobject();

    raf.seek(dataSection.filePos);
    byte[] data = new byte[dataSection.size];
    raf.read(data);

    Array dataArray = Array.factory(v.getDataType(), v.getShape(), ByteBuffer.wrap(data));
    return dataArray.section(section.getRanges());
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
    return test(b, test);
  }

  private boolean test(byte[] bread, byte[] test) throws IOException {
    if (bread.length != test.length) return false;
    for (int i = 0; i < bread.length; i++)
      if (bread[i] != test[i]) return false;
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////////
 // lower level interface for debugging

  public List<NcsMess> open(RandomAccessFile raf, NetcdfFile ncfile) throws IOException {
    this.raf = raf;
    raf.seek(0);
    if (!readAndTest(raf, NcStream.MAGIC_START))
      throw new IOException("Data corrupted on "+ncfile.getLocation());

    // assume for the moment its always starts with one header message
    if (!readAndTest(raf, NcStream.MAGIC_HEADER))
      throw new IOException("Data corrupted on "+ncfile.getLocation());

    int msize = readVInt(raf);
    if (debug) System.out.println("READ header len= " + msize);

    byte[] m = new byte[msize];
    raf.read(m);
    NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);

    NcStreamProto.Group root = proto.getRoot();
    NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();

    List<NcsMess> ncm = new ArrayList<NcsMess>();
    ncm.add(new NcsMess(msize, proto));

    while (!raf.isAtEndOfFile()) {
      if (debug) System.out.printf("READ message at = %d%n", raf.getFilePointer());
      byte[] b = new byte[4];
      raf.read(b);
      if (test(b,NcStream.MAGIC_END)) break;
      if (!test(b, NcStream.MAGIC_DATA))
        System.out.println("HEY");

      int psize = readVInt(raf);
      if (debug) System.out.printf(" dproto len= %d%n", psize);
      byte[] dp = new byte[psize];
      raf.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      if (debug) System.out.printf("%s", dproto);
      ncm.add(new NcsMess(psize, dproto));

      int dsize = readVInt(raf);
      if (debug) System.out.println(" data len= " + dsize);
      if (dsize == 1)
        System.out.println("HEY");

      DataSection dataSection = new DataSection();
      dataSection.size = dsize;
      dataSection.filePos = raf.getFilePointer();
      dataSection.section = NcStream.decodeSection(dproto.getSection());

      ncm.add(new NcsMess(dsize, dataSection));

      Variable v = ncfile.getRootGroup().findVariable(dproto.getVarName());
      v.setSPobject(dataSection);

      raf.skipBytes(dsize);
    }

    return ncm;
  }

  static public class NcsMess {
    public int len;
    public Object what;
    public NcsMess(int len, Object what) {
      this.len = len;
      this.what = what;
    }
  }

}
