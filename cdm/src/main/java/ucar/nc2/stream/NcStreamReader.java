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
import ucar.nc2.Group;

import java.io.InputStream;
import java.io.IOException;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Read an InputStream, make it into a NetcdfFile.
 * Probably doesnt actually do the data yet.
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamReader {

  private NcStreamProto.Stream proto;

  public NetcdfFile readStream(InputStream is, NetcdfFile ncfile) throws IOException {
    assert readAndTest(is, NcStream.MAGIC_HEADER);

    int msize = readVInt(is);
    System.out.println("READ header len= " + msize);

    byte[] m = new byte[msize];
    is.read(m);
    proto = NcStreamProto.Stream.parseFrom(m);
    ncfile = proto2nc(proto, ncfile);

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

  public NetcdfFile proto2nc(NcStreamProto.Stream proto, NetcdfFile ncfile) throws InvalidProtocolBufferException {
    if (ncfile == null)
      ncfile = new NetcdfFileStream();
    ncfile.setLocation(proto.getName());

    NcStreamProto.Group root = proto.getRoot();
    readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();
    return ncfile;
  }

  private void readGroup(NcStreamProto.Group proto, NetcdfFile ncfile, Group g) throws InvalidProtocolBufferException {

    for (NcStreamProto.Dimension dim : proto.getDimsList())
      g.addDimension(NcStream.makeDim(dim));

    for (NcStreamProto.Attribute att : proto.getAttsList())
      g.addAttribute(NcStream.makeAtt(att));

    for (NcStreamProto.Variable var : proto.getVarsList())
      g.addVariable(NcStream.makeVar(ncfile, g, null, var));

    for (NcStreamProto.Structure s : proto.getStructsList())
      g.addVariable(NcStream.makeStructure(ncfile, g, null, s));

    for (NcStreamProto.Group gp : proto.getGroupsList())
      readGroup(gp, ncfile, g);

  }

  private class NetcdfFileStream extends NetcdfFile {

  }
}
