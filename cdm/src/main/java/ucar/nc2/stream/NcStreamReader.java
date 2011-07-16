/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 * 
 * Portions of this software were developed by the Unidata Program at the 
 * University Corporation for Atmospheric Research.
 * 
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 * 
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.ma2.*;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Read an ncStream InputStream into a NetcdfFile.
 * Used by CdmRemote
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamReader {

  private static final boolean debug = false;

  public NetcdfFile readStream(InputStream is, NetcdfFile ncfile) throws IOException {
    byte[] b = new byte[4];
    NcStream.readFully(is, b);

    // starts with MAGIC_START, MAGIC_HEADER or just MAGIC_HEADER
    if (test(b, NcStream.MAGIC_START)) {
      if (!readAndTest(is, NcStream.MAGIC_HEADER))
        throw new IOException("Data corrupted on "+ncfile.getLocation());

    } else {
      if (!test(b, NcStream.MAGIC_HEADER))
        throw new IOException("Data corrupted on "+ncfile.getLocation());
    }

    // header
    int msize = NcStream.readVInt(is);
    byte[] m = new byte[msize];
    NcStream.readFully(is, m);

    if (debug) {
      System.out.println("READ header len= " + msize);
      //System.out.println("READ header= " + new String(m, "UTF-8"));
    }

    NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);
    ncfile = proto2nc(proto, ncfile);
    if (debug) System.out.printf("  proto= %s%n", proto);

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    while (is.available() > 0) {
      readData(is, ncfile);
    }

    return ncfile;

  }

  class DataResult {
    String varNameFullEsc;
    Section section;
    Array data;

    DataResult(String varName, Section section, Array data) {
      this.varNameFullEsc = varName;
      this.section = section;
      this.data = data;
    }
  }

  /**
   * Read the result of a data request. Only one variable at a time.
   * @param is read from input stream
   * @param ncfile need the metadata from here to interpret structure data
   * @return DataResult
   * @throws IOException on read error
   */
  public DataResult readData(InputStream is, NetcdfFile ncfile) throws IOException {
    if (!readAndTest(is, NcStream.MAGIC_DATA))
      throw new IOException("Data transfer corrupted on "+ncfile.getLocation());

    int psize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data message len= " + psize);
    byte[] dp = new byte[psize];
    NcStream.readFully(is, dp);
    NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
    // if (debug) System.out.println(" readData proto = " + dproto);

    DataType dataType = NcStream.decodeDataType(dproto.getDataType());
    Section section = (dataType == DataType.SEQUENCE) ? new Section() : NcStream.decodeSection(dproto.getSection());

    // special cases
    if (dataType == DataType.STRING) {
      Array data = Array.factory(dataType, section.getShape());
      IndexIterator ii = data.getIndexIterator();
      while(ii.hasNext()) {
        int slen = NcStream.readVInt(is);
        byte[] sb = new byte[slen];
        NcStream.readFully(is, sb);
        ii.setObjectNext( new String(sb, "UTF-8"));
      }
      return new DataResult(dproto.getVarName(), section, data);

    } else if (dataType == DataType.OPAQUE) {
      Array data = Array.factory(dataType, section.getShape());
      IndexIterator ii = data.getIndexIterator();
      while(ii.hasNext()) {
        int slen = NcStream.readVInt(is);
        byte[] sb = new byte[slen];
        NcStream.readFully(is, sb);
        ii.setObjectNext( ByteBuffer.wrap(sb));
      }
      return new DataResult(dproto.getVarName(), section, data);

    } 

    // otherwise read data message
    int dsize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data len= " + dsize);
    byte[] datab = new byte[dsize];
    NcStream.readFully(is, datab);

    if (dataType == DataType.STRUCTURE) {
      Structure s = (Structure) ncfile.findVariable(dproto.getVarName());
      StructureMembers members = s.makeStructureMembers();

      if (dproto.getVersion() == 0) {
        ArrayStructureBB.setOffsets(members); // not setting heap objects for version 0
        ArrayStructureBB data = new ArrayStructureBB(members, section.getShape(), ByteBuffer.wrap(datab), 0);
        return new DataResult(dproto.getVarName(), section, data);

      } else { // version > 0 uses a NcStreamProto.StructureData message
        ArrayStructureBB data = NcStream.decodeArrayStructure(members, section.getShape(), datab);
        return new DataResult(dproto.getVarName(), section, data);
      }

    } else {
      Array data = Array.factory(dataType, section.getShape(), ByteBuffer.wrap(datab));
      return new DataResult(dproto.getVarName(), section, data);
    }
  }

  public StructureDataIterator getStructureIterator(InputStream is, NetcdfFile ncfile) throws IOException {
    if (!readAndTest(is, NcStream.MAGIC_DATA))
      throw new IOException("Data transfer corrupted on "+ncfile.getLocation());

    int psize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data message len= " + psize);
    byte[] dp = new byte[psize];
    NcStream.readFully(is, dp);
    NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
    // if (debug) System.out.println(" readData proto = " + dproto);

    Structure s = (Structure) ncfile.findVariable(dproto.getVarName());
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureBB.setOffsets(members);

    return new StreamDataIterator(is, members);
  }

  private class StreamDataIterator implements StructureDataIterator {
    private InputStream is;
    private StructureMembers members;
    private StructureData curr = null;
    private int count = 0;

    StreamDataIterator(InputStream is, StructureMembers members) {
      this.is = is;
      this.members = members;
    }

    @Override
    public boolean hasNext() throws IOException {
      readNext();
      return (curr != null);
    }

    @Override
    public StructureData next() throws IOException {
      count++;
      return curr;
    }

    private void readNext() throws IOException {
      byte[] b = new byte[4];
      NcStream.readFully(is, b);

     // starts with MAGIC_START, MAGIC_HEADER or just MAGIC_HEADER
     if (test(b, NcStream.MAGIC_VDATA)) {
       int dsize = NcStream.readVInt(is);
       byte[] datab = new byte[dsize];
       NcStream.readFully(is, datab);
       curr = NcStream.decodeStructureData(members, datab);
       // System.out.printf("StreamDataIterator read sdata size= %d%n", dsize);

     } else if (test(b, NcStream.MAGIC_VEND)) {
        curr = null;

     } else {
       throw new IllegalStateException("bad stream");
     }
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public StructureDataIterator reset() {
      return (count == 0) ? this : null;
    }

    @Override
    public int getCurrentRecno() {
      return count;
    }

    // LOOK !!!
    public void finish() {
      if (is != null) {
          try { is.close(); }catch (IOException ioe) {};
      }
    }
  }


  private boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    NcStream.readFully(is, b);

    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  private boolean test(byte[] b, byte[] test) throws IOException {
    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  public NetcdfFile proto2nc(NcStreamProto.Header proto, NetcdfFile ncfile) throws InvalidProtocolBufferException {
    if (ncfile == null)
      ncfile = new NetcdfFileStream(); // not used i think
    ncfile.setLocation(proto.getLocation());
    if (proto.hasId()) ncfile.setId(proto.getId());
    if (proto.hasTitle()) ncfile.setTitle(proto.getTitle());

    NcStreamProto.Group root = proto.getRoot();
    NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();
    return ncfile;
  }

  private class NetcdfFileStream extends NetcdfFile {

  }
}
