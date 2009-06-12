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

package ucar.nc2.iosp;

import ucar.ma2.*;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Structure;
import ucar.nc2.stream.NcStream;

import java.io.IOException;
import java.io.DataOutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;

public abstract class AbstractIOServiceProvider implements IOServiceProvider {

  // a no-op but leave it in in case we change our minds
  static public String createValidNetcdfObjectName(String name) {
    return name;
  }

  protected ucar.unidata.io.RandomAccessFile raf;

  public void close() throws java.io.IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // default implementation, reads into an Array, then writes to WritableByteChannel
  // subclasses should override if possible
  // LOOK DataOutputStream uses big-endian
  public long readToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Array data = readData(v2, section);
    return copyToByteChannel(data,  channel);
  }

  public static long copyToByteChannel(Array data, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {

    // ArrayStructureBB can be optimised
    // LOOK not actually right until we define the on-the-wire protocol
    if (data instanceof ArrayStructureBB) {
      ArrayStructureBB dataBB = (ArrayStructureBB) data;
      ByteBuffer bb = dataBB.getByteBuffer();
      bb.rewind();
      channel.write(bb);
      return bb.limit();
    }

    DataOutputStream outStream = new DataOutputStream( Channels.newOutputStream( channel));

    IndexIterator iterA = data.getIndexIterator();
    Class classType = data.getElementType();

    if (classType == double.class) {
      while (iterA.hasNext())
        outStream.writeDouble(iterA.getDoubleNext());

    } else if (classType == float.class) {
      while (iterA.hasNext())
        outStream.writeFloat(iterA.getFloatNext());

    } else if (classType == long.class) {
      while (iterA.hasNext())
        outStream.writeLong(iterA.getLongNext());

    } else if (classType == int.class) {
      while (iterA.hasNext())
        outStream.writeInt(iterA.getIntNext());

    } else if (classType == short.class) {
      while (iterA.hasNext())
        outStream.writeShort(iterA.getShortNext());

    } else if (classType == char.class) {
      while (iterA.hasNext())
        outStream.writeChar(iterA.getCharNext());

    } else if (classType == byte.class) {
      while (iterA.hasNext())
        outStream.writeByte(iterA.getByteNext());

    } else if (classType == boolean.class) {
      while (iterA.hasNext())
        outStream.writeBoolean(iterA.getBooleanNext());

    } else if (classType == String.class) {
      long size = 0;
      while (iterA.hasNext()) {
        String s = (String) iterA.getObjectNext();
        size += NcStream.writeVInt( outStream, s.length());
        byte[] b = s.getBytes("UTF-8");
        outStream.write(b);
        size += b.length;
      }
      return size;

    } else if (classType == ByteBuffer.class) { // OPAQUE
      long size = 0;
      while (iterA.hasNext()) {
        ByteBuffer bb = (ByteBuffer) iterA.getObjectNext();
        size += NcStream.writeVInt( outStream, bb.limit());
        bb.rewind();
        channel.write(bb);
        size += bb.limit();
      }
      return size;

    } else
      throw new UnsupportedOperationException("Class type = " + classType.getName());

    return data.getSizeBytes();
  }


  public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {
    return IospHelper.readSection(cer);  //  IOSPs can optimize by overriding
  }

  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return null;
  }

  public Object sendIospMessage(Object message) {
    return null;
  }

  public boolean syncExtend() throws IOException {
    return false;
  }

  public boolean sync() throws IOException {
    return false;
  }

  public String toStringDebug(Object o) {
    return "";
  }

  public String getDetailInfo() {
    return "";
  }

  public String getFileTypeVersion() {
    return "N/A";
  }

}
