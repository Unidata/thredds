package ucar.nc2.stream;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.Layout;
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
        Variable v = ncfile.getRootGroup().findVariable(dproto.getVarName());
        if (debug) System.out.printf(" dproto = %s for %s%n", dproto, v.getShortName());

        if (!dproto.getVdata()) { // regular data
          int dsize = readVInt(raf);
          if (debug) System.out.println(" data len= " + dsize);

          DataStorage dataSection = new DataStorage();
          dataSection.size = dsize;
          dataSection.filePos = raf.getFilePointer();
          dataSection.section = NcStream.decodeSection(dproto.getSection());
          v.setSPobject(dataSection);
          raf.skipBytes(dsize);

        } else {
          DataStorage dataStorage = new DataStorage();
          dataStorage.filePos = raf.getFilePointer();
          int nelems = readVInt(raf);
          int totalSize = 0;
          for (int i=0; i<nelems; i++) {
            int dsize= readVInt(raf);
            totalSize += dsize;
            raf.skipBytes(dsize);
          }
          dataStorage.isVlen = true;
          dataStorage.nelems = nelems;
          dataStorage.size = totalSize;
          dataStorage.section = NcStream.decodeSection(dproto.getSection());

          v.setSPobject(dataStorage);
        }
      }

    } catch (Throwable t) {
      throw new RuntimeException("NcStreamIosp: "+t.getMessage() +" on " + raf.getLocation(), t);
    }
  }


  private class DataStorage {
    int size;
    long filePos;
    Section section;
    boolean isVlen;
    int nelems;

    @Override
    public String toString() {
      return  "size=" + size +
              ", filePos=" + filePos +
              ", section=" + section +
              ", nelems=" + nelems ;
    }
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    DataStorage dataStorage = (DataStorage) v.getSPobject();
    if (dataStorage.isVlen)
      return readVlenData(v, section, dataStorage);

    raf.seek(dataStorage.filePos);
    byte[] data = new byte[dataStorage.size];
    raf.read(data);

    Array dataArray = Array.factory(v.getDataType(), v.getShape(), ByteBuffer.wrap(data));
    return dataArray.section(section.getRanges());
  }

  public Array readVlenData(Variable v, Section section, DataStorage dataStorage) throws IOException, InvalidRangeException {
    raf.seek(dataStorage.filePos);
    int nelems = readVInt(raf);
    Object[] result = new Object[nelems];

    for (int elem=0; elem<nelems; elem++) {
      int dsize= readVInt(raf);
      byte[] data = new byte[dsize];
      raf.read(data);
      Array dataArray = Array.factory(v.getDataType(), null, ByteBuffer.wrap(data));
      result[elem] = dataArray;
    }
    return new ArrayObject(result[0].getClass(), new int[] {nelems}, result);
    //return dataArray.section(section.getRanges());
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
 // read in all messages, return as List<NcMess>

  public List<NcsMess> open(RandomAccessFile raf, NetcdfFile ncfile) throws IOException {
    List<NcsMess> ncm = new ArrayList<NcsMess>();

    this.raf = raf;
    raf.seek(0);
    long pos = raf.getFilePointer();

    if (!readAndTest(raf, NcStream.MAGIC_START)) {
      ncm.add(new NcsMess(pos, 0, "MAGIC_START missing - abort"));
      return ncm;
    }
    // assume for the moment its always starts with one header message
    pos = raf.getFilePointer();
    if (!readAndTest(raf, NcStream.MAGIC_HEADER)) {
      ncm.add(new NcsMess(pos, 0, "MAGIC_HEADER missing - abort"));
      return ncm;
    }

    int msize = readVInt(raf);
    byte[] m = new byte[msize];
    raf.read(m);
    NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);
    ncm.add(new NcsMess(pos, msize, proto));

    NcStreamProto.Group root = proto.getRoot();
    NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();

    while (!raf.isAtEndOfFile()) {
      pos = raf.getFilePointer();
      byte[] b = new byte[4];
      raf.read(b);
      if (test(b,NcStream.MAGIC_END)) {
        ncm.add(new NcsMess(pos, 4, "MAGIC_END"));
        break;
      }

      if (test(b,NcStream.MAGIC_ERR)) {
        int esize = readVInt(raf);
        byte[] dp = new byte[esize];
        raf.read(dp);
        NcStreamProto.Error error = NcStreamProto.Error.parseFrom(dp);
        ncm.add(new NcsMess(pos, esize, error.getMessage()));
        continue;
      }

      if (!test(b, NcStream.MAGIC_DATA)) {
        ncm.add(new NcsMess(pos, 4, "MAGIC_DATA missing - abort"));
        break;
      }

      // data messages
      int psize = readVInt(raf);
      byte[] dp = new byte[psize];
      raf.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      ncm.add(new NcsMess(pos, psize, dproto));

      pos = raf.getFilePointer();
      if (!dproto.getVdata()) { // regular data
        int dsize = readVInt(raf);
        DataStorage dataStorage = new DataStorage();
        dataStorage.size = dsize;
        dataStorage.filePos = pos;
        dataStorage.section = NcStream.decodeSection(dproto.getSection());
        dataStorage.nelems = (int) dataStorage.section.computeSize();
        ncm.add(new NcsMess(dataStorage.filePos, dsize, dataStorage));
        raf.skipBytes(dsize);

      } else {
        DataStorage dataStorage = new DataStorage();
        dataStorage.filePos = pos;
        int nelems = readVInt(raf);
        int totalSize = 0;
        for (int i=0; i<nelems; i++) {
          int dsize= readVInt(raf);
          totalSize += dsize;
          raf.skipBytes(dsize);
        }
        dataStorage.nelems = nelems;
        dataStorage.size = totalSize;
        dataStorage.section = NcStream.decodeSection(dproto.getSection());
        ncm.add(new NcsMess(dataStorage.filePos ,totalSize, dataStorage));
      }

      //Variable v = ncfile.getRootGroup().findVariable(dproto.getVarName());
      //v.setSPobject(dataStorage);

    }

    return ncm;
  }

  static public class NcsMess {
    public long filePos;
    public int len;
    public int nelems;
    public Object what;
    public NcsMess(long filePos, int len, Object what) {
      this.filePos = filePos;
      this.len = len;
      this.what = what;
      if (what instanceof DataStorage)
        this.nelems = ((DataStorage) what).nelems;
    }
  }

}
