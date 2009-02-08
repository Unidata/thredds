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

import ucar.nc2.*;
import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Write a NetcdfFile to a nc stream
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamWriter {
  private NetcdfFile ncfile;
  private NcStreamProto.Stream proto;
  private boolean show = false;

  public NcStreamWriter(NetcdfFile ncfile) throws InvalidProtocolBufferException {
    this.ncfile = ncfile;
    NcStreamProto.Group.Builder rootBuilder = NcStream.makeGroup( ncfile.getRootGroup());

    NcStreamProto.Stream.Builder streamBuilder = NcStreamProto.Stream.newBuilder();
    streamBuilder.setName(ncfile.getLocation());
    streamBuilder.setRoot(rootBuilder);
    streamBuilder.setIndexPos(0);

    proto = streamBuilder.build();
  }

  ////////////////////////////////////////////////////////////

  public void sendHeader(WritableByteChannel wbc) throws IOException {
    OutputStream out = Channels.newOutputStream(wbc);
    long size = 0;

    //// header message
    size += writeBytes(out, NcStream.MAGIC_HEADER); // magic
    byte[] b = proto.toByteArray();
    size += writeVInt(out, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);
    // payload
    size += writeBytes(out, b);
    System.out.println(" header size=" + size);
  }


  public void stream(WritableByteChannel wbc) throws IOException {
    OutputStream out = Channels.newOutputStream(wbc);
    long size = 0;

    //// header message
    size += writeBytes(out, NcStream.MAGIC_HEADER); // magic
    byte[] b = proto.toByteArray();
    size += writeVInt(out, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);
    // payload
    size += writeBytes(out, b);

    // data messages
    for (Variable v : ncfile.getVariables()) {
      // magic
      size += writeBytes(out, NcStream.MAGIC_DATA); // magic
      NcStreamProto.Data dataProto = NcStream.makeDataProto(v);
      byte[] datab = dataProto.toByteArray();
      size += writeVInt(out, datab.length); // proto len
      size += writeBytes(out, datab); //proto
      if (show) System.out.println(v.getName() + " proto len=" + datab.length);

      long len = v.getSize() * v.getElementSize();
      size += writeVInt(out, (int) len); // data len
      if (show) System.out.println(v.getName() + " data len=" + len);
      out.flush();

      long readCount = ncfile.readToByteChannel(v, wbc);
      assert readCount == len;
      size += len;
    }
    System.out.println(" total size=" + size);
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

  /* ///////////////////////////////////////////////////////////
  public NetcdfFile readStream(InputStream is) throws IOException {
    assert readAndTest(is, NcStream.MAGIC_HEADER);

    int msize = readVInt(is);
    System.out.println("READ header len= " + msize);

    byte[] m = new byte[msize];
    is.read(m);
    proto = NcStreamProto.Stream.parseFrom(m);
    ncfile = proto2nc(proto);

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    while (is.available() > 0) {
      assert readAndTest(is, NcStream.MAGIC_DATA);

      int psize = readVInt(is);
      System.out.println(" dproto len= " + psize);
      byte[] dp = new byte[psize];
      is.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      System.out.println(" dproto = " + dproto);

      int dsize = readVInt(is);
      System.out.println(" data len= " + dsize);
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
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }

  public NetcdfFile proto2nc(NcStreamProto.Stream proto) throws InvalidProtocolBufferException {
    NetcdfFile ncfile = new NetcdfFileStream();
    ncfile.setLocation(proto.getName());

    NcStreamProto.Group root = proto.getRoot();

    for (NcStreamProto.Dimension dim : root.getDimsList()) {
      ncfile.addDimension(null, NcStream.makeDim(dim));
    }

    for (NcStreamProto.Attribute att : root.getAttsList()) {
      ncfile.addAttribute(null, NcStream.makeAtt(att));
    }

    for (NcStreamProto.Variable var : root.getVarsList()) {
      ncfile.addVariable(null, NcStream.makeVar(ncfile, var));
    }

    return ncfile;
  }

  private class NetcdfFileStream extends NetcdfFile {

  }      */

}

