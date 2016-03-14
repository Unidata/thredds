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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.Structure;
import ucar.ma2.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.InflaterInputStream;

import com.google.protobuf.InvalidProtocolBufferException;
import ucar.nc2.constants.CDM;

/**
 * Read an ncStream InputStream into a NetcdfFile.
 * Used by CdmRemote
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamReader {
  static private final Logger logger = LoggerFactory.getLogger(NcStreamReader.class);

  private static final boolean debug = false;
  private static final boolean showDeflate = false;

  private static double total_uncompressedSize = 0.0;
  private static double total_compressedSize = 0.0;

  static public double getCompression(boolean reset) {
    double result = total_uncompressedSize / total_compressedSize;
    if (reset) {
      total_compressedSize = 0;
      total_uncompressedSize = 0;
    }
    return result;
  }

  public NetcdfFile readStream(InputStream is, NetcdfFile ncfile) throws IOException {
    byte[] b = new byte[4];
    NcStream.readFully(is, b);

    // starts with MAGIC_START, MAGIC_HEADER or just MAGIC_HEADER
    if (NcStream.test(b, NcStream.MAGIC_START)) {
      if (!NcStream.readAndTest(is, NcStream.MAGIC_HEADER))
        throw new IOException("Data corrupted on " + ncfile.getLocation());

    } else {
      if (!NcStream.test(b, NcStream.MAGIC_HEADER))
        throw new IOException("Data corrupted on " + ncfile.getLocation());
    }

    // header
    int msize = NcStream.readVInt(is);
    byte[] m = new byte[msize];
    NcStream.readFully(is, m);

    if (debug) {
      System.out.println("READ header len= " + msize);
    }

    NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);
    ncfile = proto2nc(proto, ncfile);
    if (debug) System.out.printf("  proto= %s%n", proto);

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    /* why are we reading then ignoring the data?
    while (is.available() > 0) {
      readData(is, ncfile, ncfile.getLocation());
    } */

    return ncfile;
  }

  static public class DataResult {
    public String varNameFullEsc;
    public Array data;

    DataResult(String varName, Array data) {
      this.varNameFullEsc = varName;
      this.data = data;
    }
  }

  /**
   * Read the result of a data request. Only one variable at a time.
   *
   * @param is     read from input stream
   * @param ncfile need the metadata from here to interpret structure data
   * @return DataResult
   * @throws IOException on read error
   */
  public DataResult readData(InputStream is, NetcdfFile ncfile, String location) throws IOException {
    byte[] b = new byte[4];
    int bytesRead = NcStream.readFully(is, b);
    if (bytesRead < b.length)
      throw new EOFException(location);

    if (NcStream.test(b,NcStream.MAGIC_DATA)) return readData1(is, ncfile);
    if (NcStream.test(b,NcStream.MAGIC_DATA2)) return readData2(is);

    throw new IOException("Data transfer corrupted on " + location);
  }

  private DataResult readData1(InputStream is, NetcdfFile ncfile) throws IOException {
    int psize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data message len= " + psize);
    byte[] dp = new byte[psize];
    NcStream.readFully(is, dp);
    NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
    ByteOrder bo = NcStream.decodeDataByteOrder(dproto); // LOOK not using bo !!

    DataType dataType = NcStream.convertDataType(dproto.getDataType());
    Section section = (dataType == DataType.SEQUENCE) ? new Section() : NcStream.decodeSection(dproto.getSection());

    // special cases
    if (dataType == DataType.STRING) {
      int nobjs = NcStream.readVInt(is);
      Array data = Array.factory(dataType, section.getShape());
      assert nobjs == section.computeSize();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        int slen = NcStream.readVInt(is);
        byte[] sb = new byte[slen];
        NcStream.readFully(is, sb);
        ii.setObjectNext(new String(sb, CDM.utf8Charset));
      }
      return new DataResult(dproto.getVarName(), data);

    } else if (dataType == DataType.OPAQUE) {
      int nobjs = NcStream.readVInt(is);
      Array data = Array.factory(dataType, section.getShape());
      assert nobjs == section.computeSize();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext()) {
        int slen = NcStream.readVInt(is);
        byte[] sb = new byte[slen];
        NcStream.readFully(is, sb);
        ii.setObjectNext(ByteBuffer.wrap(sb));
      }
      return new DataResult(dproto.getVarName(), data);
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
        return new DataResult(dproto.getVarName(), data);

      } else { // version > 0 and < 3 uses a NcStreamProto.StructureData message
        ArrayStructureBB data = NcStream.decodeArrayStructure(members, section.getShape(), datab);
        return new DataResult(dproto.getVarName(), data);
      }
    }

    // otherwise its a multidim array

    // is it compressed ?
    Array data;
    NcStreamProto.Compress compress = dproto.getCompress();
    int uncompressedSize = dproto.getUncompressedSize();
    if (compress == NcStreamProto.Compress.DEFLATE) {
      ByteArrayInputStream bin = new ByteArrayInputStream(datab);
      InflaterInputStream in = new InflaterInputStream(bin);
      byte[] resultb = new byte[uncompressedSize];
      NcStream.readFully(in, resultb);

      data = Array.factory(dataType, section.getShape(), ByteBuffer.wrap(resultb)); // another copy, not sure can do anything
      if (showDeflate)
        System.out.printf("Deflate = %d / %d = %f %n", uncompressedSize, dsize, ((float) uncompressedSize) / dsize);
      total_uncompressedSize += uncompressedSize;
      total_compressedSize += dsize;

    } else {
      data = Array.factory(dataType, section.getShape(), ByteBuffer.wrap(datab));
    }

    return new DataResult(dproto.getVarName(), data);
  }

  private DataResult readData2(InputStream is) throws IOException {
    int psize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data message len= " + psize);
    byte[] dp = new byte[psize];
    NcStream.readFully(is, dp);

    NcStreamProto.DataCol dproto = NcStreamProto.DataCol.parseFrom(dp);
    // NcStreamProto.Data2 dproto = NcStreamProto.Data2.parseDelimitedFrom(is);

    NcStreamDataCol decoder = new NcStreamDataCol();
    Array data = decoder.decode(dproto, null);
    return new DataResult(dproto.getName(), data);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // LOOK

  public StructureDataIterator getStructureIterator(InputStream is, NetcdfFile ncfile) throws IOException {
    if (!NcStream.readAndTest(is, NcStream.MAGIC_DATA))
      throw new IOException("Data transfer corrupted on " + ncfile.getLocation());

    int psize = NcStream.readVInt(is);
    if (debug) System.out.println("  readData data message len= " + psize);
    byte[] dp = new byte[psize];
    NcStream.readFully(is, dp);
    NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
    // if (debug) System.out.println(" readData proto = " + dproto);

    Structure s = (Structure) ncfile.findVariable(dproto.getVarName());
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureBB.setOffsets(members);

    ByteOrder bo = NcStream.decodeDataByteOrder(dproto);
    return new StreamDataIterator(is, members, bo);
  }

  private static class StreamDataIterator implements StructureDataIterator {
    private InputStream is;
    private StructureMembers members;
    private StructureData curr = null;
    private ByteOrder bo;
    private int count = 0;
    private boolean done;

    StreamDataIterator(InputStream is, StructureMembers members, ByteOrder bo) {
      this.is = is;
      this.members = members;
      this.bo = bo;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (!done) readNext();
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
      if (NcStream.test(b, NcStream.MAGIC_VDATA)) {
        int dsize = NcStream.readVInt(is);
        byte[] datab = new byte[dsize];
        NcStream.readFully(is, datab);
        // curr = NcStream.decodeStructureData(members, bo, datab); LOOK
        // System.out.printf("StreamDataIterator read sdata size= %d%n", dsize);

      } else if (NcStream.test(b, NcStream.MAGIC_VEND)) {
        curr = null;
        close();

      } else {
        throw new IllegalStateException("bad stream");
      }
    }

    @Override
    public StructureDataIterator reset() {
      return (count == 0) && (is != null) ? this : null;
    }

    @Override
    public int getCurrentRecno() {
      return count;
    }

    @Override
    public void close() {
      done = true;
      if (is != null) {
        try {
          is.close();
          is = null;
        } catch (IOException ioe) {
          logger.error("NcStreamReader: Error closing input stream.");
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////

  private NetcdfFile proto2nc(NcStreamProto.Header proto, NetcdfFile ncfile) throws InvalidProtocolBufferException {
    if (ncfile == null)
      ncfile = new NetcdfFileSubclass(); // not used i think
    ncfile.setLocation(proto.getLocation());
    if (proto.getId().length() > 0) ncfile.setId(proto.getId());
    if (proto.getTitle().length() > 0) ncfile.setTitle(proto.getTitle());

    NcStreamProto.Group root = proto.getRoot();
    NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();
    return ncfile;
  }

}
