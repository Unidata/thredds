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
package ucar.nc2.geotiff;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;

/**
 * Low level read/write geotiff files.
 *
 * @author John Caron
 * @author Yuan Ho
 * @see GeotiffWriter
 */
public class GeoTiff {
  private String filename;
  private RandomAccessFile file;
  private FileChannel channel;
  private List<IFDEntry> tags = new ArrayList<IFDEntry>();
  private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
  private boolean readonly;

  private boolean showBytes = false, debugRead = false, debugReadGeoKey = false;
  private boolean showHeaderBytes = false;

  /**
   * Constructor. Does not open or create the file.
   * @param filename pathname of file
   */
  public GeoTiff(String filename) {
    this.filename = filename;
  }

  /**
   * Close the Geotiff file.
   * @throws java.io.IOException on io error
   */
  public void close() throws IOException {
    if (channel != null) {
      if (!readonly) {
        channel.force(true);
        channel.truncate(nextOverflowData);
      }
      channel.close();
    }
    if (file != null)
      file.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // writing

  private int headerSize = 8;
  private int firstIFD = 0;
  private int lastIFD = 0;
  private int startOverflowData = 0;
  private int nextOverflowData = 0;

  void addTag(IFDEntry ifd) {
    tags.add(ifd);
  }

  void deleteTag(IFDEntry ifd) {
    tags.remove(ifd);
  }

  void setTransform(double xStart, double yStart, double xInc, double yInc) {
    // tie the raster 0, 0 to xStart, yStart
    addTag(new IFDEntry(Tag.ModelTiepointTag, FieldType.DOUBLE).setValue(
        new double[]{0.0, 0.0, 0.0, xStart, yStart, 0.0}));

    // define the "affine transformation" : requires grid to be regular (!)
    addTag(new IFDEntry(Tag.ModelPixelScaleTag, FieldType.DOUBLE).setValue(
        new double[]{xInc, yInc, 0.0}));
  }

  private List<GeoKey> geokeys = new ArrayList<GeoKey>();

  void addGeoKey(GeoKey geokey) {
    geokeys.add(geokey);
  }

  private void writeGeoKeys() {
    if (geokeys.size() == 0) return;

    // count extras
    int extra_chars = 0;
    int extra_ints = 0;
    int extra_doubles = 0;
    for (GeoKey geokey : geokeys) {
      if (geokey.isDouble)
        extra_doubles += geokey.count();
      else if (geokey.isString)
        extra_chars += geokey.valueString().length() + 1;
      else if (geokey.count() > 1)
        extra_ints += geokey.count();
    }

    int n = (geokeys.size() + 1) * 4;
    int[] values = new int[n + extra_ints];
    double[] dvalues = new double[extra_doubles];
    char[] cvalues = new char[extra_chars];
    int icounter = n;
    int dcounter = 0;
    int ccounter = 0;

    values[0] = 1;
    values[1] = 1;
    values[2] = 0;
    values[3] = geokeys.size();
    int count = 4;
    for (GeoKey geokey : geokeys) {
      values[count++] = geokey.tagCode();

      if (geokey.isDouble) {
        values[count++] = Tag.GeoDoubleParamsTag.getCode(); // extra double values here
        values[count++] = geokey.count();
        values[count++] = dcounter;
        for (int k = 0; k < geokey.count(); k++)
          dvalues[dcounter++] = geokey.valueD(k);

      } else if (geokey.isString) {
        String s = geokey.valueString();
        values[count++] = Tag.GeoAsciiParamsTag.getCode(); // extra double values here
        values[count++] = s.length(); // dont include trailing 0 in the count
        values[count++] = ccounter;
        for (int k = 0; k < s.length(); k++)
          cvalues[ccounter++] = s.charAt(k);
        cvalues[ccounter++] = (char) 0;

      } else if (geokey.count() > 1) { // more than one int value
        values[count++] = Tag.GeoKeyDirectoryTag.getCode(); // extra int values here
        values[count++] = geokey.count();
        values[count++] = icounter;
        for (int k = 0; k < geokey.count(); k++)
          values[icounter++] = geokey.value(k);

      } else { // normal case of one int value
        values[count++] = 0;
        values[count++] = 1;
        values[count++] = geokey.value();
      }
    } // loop over geokeys

    addTag(new IFDEntry(Tag.GeoKeyDirectoryTag, FieldType.SHORT).setValue(values));
    if (extra_doubles > 0)
      addTag(new IFDEntry(Tag.GeoDoubleParamsTag, FieldType.DOUBLE).setValue(dvalues));
    if (extra_chars > 0)
      addTag(new IFDEntry(Tag.GeoAsciiParamsTag, FieldType.ASCII).setValue(new String(cvalues)));
  }

  int writeData(byte[] data, int imageNumber) throws IOException {
    if (file == null)
      init();

    if (imageNumber == 1)
      channel.position(headerSize);
    else
      channel.position(nextOverflowData);

    ByteBuffer buffer = ByteBuffer.wrap(data);
    channel.write(buffer);

    if (imageNumber == 1)
      firstIFD = headerSize + data.length;
    else
      firstIFD = data.length + nextOverflowData;

    return nextOverflowData;
  }

  int writeData(float[] data, int imageNumber) throws IOException {
    if (file == null)
      init();

    if (imageNumber == 1)
      channel.position(headerSize);
    else
      channel.position(nextOverflowData);

    // no way around making a copy
    ByteBuffer direct = ByteBuffer.allocateDirect(4 * data.length);
    FloatBuffer buffer = direct.asFloatBuffer();
    buffer.put(data);
    //buffer.flip();
    channel.write(direct);

    if (imageNumber == 1)
      firstIFD = headerSize + 4 * data.length;
    else
      firstIFD = 4 * data.length + nextOverflowData;

    return nextOverflowData;
  }

  void writeMetadata(int imageNumber) throws IOException {
    if (file == null)
      init();

    // geokeys all get added at once
    writeGeoKeys();

    // tags gotta be in order
    Collections.sort(tags);
    int start = 0;
    if (imageNumber == 1)
      start = writeHeader(channel);
    else {
      //now this is not the first image we need to fill the Offset of nextIFD
      channel.position(lastIFD);
      ByteBuffer buffer = ByteBuffer.allocate(4);
      if (debugRead)
        System.out.println("position before writing nextIFD= " + channel.position() + " IFD is " + firstIFD);
      buffer.putInt(firstIFD);
      buffer.flip();
      channel.write(buffer);
    }
    writeIFD(channel, firstIFD);
  }

  private int writeHeader(FileChannel channel) throws IOException {
    channel.position(0);

    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.put((byte) 'M');
    buffer.put((byte) 'M');
    buffer.putShort((short) 42);
    buffer.putInt(firstIFD);

    buffer.flip();
    channel.write(buffer);

    return firstIFD;
  }

  public void initTags() throws IOException {
    tags = new ArrayList<IFDEntry>();
    geokeys = new ArrayList<GeoKey>();
  }

  private void init() throws IOException {
    file = new RandomAccessFile(filename, "rw");
    channel = file.getChannel();
    if (debugRead) System.out.println("Opened file to write: '" + filename + "', size=" + channel.size());
    readonly = false;
  }

  private void writeIFD(FileChannel channel, int start) throws IOException {
    channel.position(start);

    ByteBuffer buffer = ByteBuffer.allocate(2);
    int n = tags.size();
    buffer.putShort((short) n);
    buffer.flip();
    channel.write(buffer);

    start += 2;
    startOverflowData = start + 12 * tags.size() + 4;
    nextOverflowData = startOverflowData;

    for (IFDEntry elem : tags) {
      writeIFDEntry(channel, elem, start);
      start += 12;
    }
    // firstIFD = startOverflowData;
    // position to where the "next IFD" goes
    channel.position(startOverflowData - 4);
    lastIFD = startOverflowData - 4;
    if (debugRead) System.out.println("pos before writing nextIFD= " + channel.position());
    buffer = ByteBuffer.allocate(4);
    buffer.putInt(0);
    buffer.flip();
    channel.write(buffer);
  }

  private void writeIFDEntry(FileChannel channel, IFDEntry ifd, int start) throws IOException {
    channel.position(start);
    ByteBuffer buffer = ByteBuffer.allocate(12);

    buffer.putShort((short) ifd.tag.getCode());
    buffer.putShort((short) ifd.type.code);
    buffer.putInt(ifd.count);

    int size = ifd.count * ifd.type.size;
    if (size <= 4) {
      int done = writeValues(buffer, ifd);
      for (int k = 0; k < 4 - done; k++) // fill out to 4 bytes
        buffer.put((byte) 0);
      buffer.flip();
      channel.write(buffer);

    } else { // write offset
      buffer.putInt(nextOverflowData);
      buffer.flip();
      channel.write(buffer);
      // write data
      channel.position(nextOverflowData);
      //System.out.println(" write offset = "+ifd.tag.getName());
      ByteBuffer vbuffer = ByteBuffer.allocate(size);
      writeValues(vbuffer, ifd);
      vbuffer.flip();
      channel.write(vbuffer);
      nextOverflowData += size;
    }
  }

  private int writeValues(ByteBuffer buffer, IFDEntry ifd) {
    int done = 0;

    if (ifd.type == FieldType.ASCII) {
      return writeSValue(buffer, ifd);

    } else if (ifd.type == FieldType.RATIONAL) {
      for (int i = 0; i < ifd.count * 2; i++)
        done += writeIntValue(buffer, ifd, ifd.value[i]);

    } else if (ifd.type == FieldType.FLOAT) {
      for (int i = 0; i < ifd.count; i++)
        buffer.putFloat((float) ifd.valueD[i]);
      done += ifd.count * 4;

    } else if (ifd.type == FieldType.DOUBLE) {
      for (int i = 0; i < ifd.count; i++)
        buffer.putDouble(ifd.valueD[i]);
      done += ifd.count * 8;

    } else {
      for (int i = 0; i < ifd.count; i++)
        done += writeIntValue(buffer, ifd, ifd.value[i]);
    }

    return done;
  }

  private int writeIntValue(ByteBuffer buffer, IFDEntry ifd, int v) {
    switch (ifd.type.code) {
      case 1:
        buffer.put((byte) v);
        return 1;
      case 3:
        buffer.putShort((short) v);
        return 2;
      case 4:
        buffer.putInt(v);
        return 4;
      case 5:
        buffer.putInt(v);
        return 4;
    }
    return 0;
  }

  private int writeSValue(ByteBuffer buffer, IFDEntry ifd) {
    buffer.put(ifd.valueS.getBytes());
    int size = ifd.valueS.length();
    if ((size % 2) == 1) size++;
    return size;
  }

  /////////////////////////////////////////////////////////////////////////////
  // reading

  /**
   * Read the geotiff file, using the filename passed in the constructor.
   *
   * @throws IOException on read error
   */
  public void read() throws IOException {
    file = new RandomAccessFile(filename, "r");
    channel = file.getChannel();
    if (debugRead) System.out.println("Opened file to read:'" + filename + "', size=" + channel.size());
    readonly = true;

    int nextOffset = readHeader(channel);
    while (nextOffset > 0) {
      nextOffset = readIFD(channel, nextOffset);
      parseGeoInfo();
    }

    //parseGeoInfo();
  }

  IFDEntry findTag(Tag tag) {
    if (tag == null) return null;
    for (IFDEntry ifd : tags) {
      if (ifd.tag == tag)
        return ifd;
    }
    return null;
  }

  void testReadData() throws IOException {
    IFDEntry tileOffsetTag = findTag(Tag.TileOffsets);
    if (tileOffsetTag != null) {
      int tileOffset = tileOffsetTag.value[0];
      IFDEntry tileSizeTag = findTag(Tag.TileByteCounts);
      int tileSize = tileSizeTag.value[0];
      System.out.println("tileOffset =" + tileOffset + " tileSize=" + tileSize);
      testReadData(tileOffset, tileSize);
    } else {

      IFDEntry stripOffsetTag = findTag(Tag.StripOffsets);
      if (stripOffsetTag != null) {
        int stripOffset = stripOffsetTag.value[0];
        IFDEntry stripSizeTag = findTag(Tag.StripByteCounts);
        int stripSize = stripSizeTag.value[0];
        System.out.println("stripOffset =" + stripOffset + " stripSize=" + stripSize);
        testReadData(stripOffset, stripSize);
      }
    }
  }

  private void testReadData(int offset, int size) throws IOException {
    channel.position(offset);
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(byteOrder);

    channel.read(buffer);
    buffer.flip();

    //printBytes( System.out, "data", buffer, 32);
    //buffer.rewind();

    for (int i = 0; i < size / 4; i++) {
      System.out.println(i + ": " + buffer.getFloat());
    }
  }

  private int readHeader(FileChannel channel) throws IOException {
    channel.position(0);

    ByteBuffer buffer = ByteBuffer.allocate(8);
    channel.read(buffer);
    buffer.flip();
    if (showHeaderBytes) {
      printBytes(System.out, "header", buffer, 4);
      buffer.rewind();
    }

    byte b = buffer.get();
    if (b == 73)
      byteOrder = ByteOrder.LITTLE_ENDIAN;
    buffer.order(byteOrder);
    buffer.position(4);
    int firstIFD = buffer.getInt();
    if (debugRead) System.out.println(" firstIFD == " + firstIFD);

    return firstIFD;
  }

  private int readIFD(FileChannel channel, int start) throws IOException {
    channel.position(start);

    ByteBuffer buffer = ByteBuffer.allocate(2);
    buffer.order(byteOrder);

    int n = channel.read(buffer);
    buffer.flip();
    if (showBytes) {
      printBytes(System.out, "IFD", buffer, 2);
      buffer.rewind();
    }
    short nentries = buffer.getShort();
    if (debugRead) System.out.println(" nentries = " + nentries);

    start += 2;
    for (int i = 0; i < nentries; i++) {
      IFDEntry ifd = readIFDEntry(channel, start);
      if (debugRead) System.out.println(i + " == " + ifd);

      tags.add(ifd);
      start += 12;
    }

    if (debugRead) System.out.println(" looking for nextIFD at pos == " + channel.position() + " start = " + start);
    channel.position(start);
    buffer = ByteBuffer.allocate(4);
    buffer.order(byteOrder);
    n = channel.read(buffer);
    buffer.flip();
    int nextIFD = buffer.getInt();
    if (debugRead) System.out.println(" nextIFD == " + nextIFD);
    return nextIFD;
  }

  private IFDEntry readIFDEntry(FileChannel channel, int start) throws IOException {
    if (debugRead) System.out.println("readIFDEntry starting position to " + start);

    channel.position(start);
    ByteBuffer buffer = ByteBuffer.allocate(12);
    buffer.order(byteOrder);
    channel.read(buffer);
    buffer.flip();
    if (showBytes) printBytes(System.out, "IFDEntry bytes", buffer, 12);

    IFDEntry ifd;
    buffer.position(0);
    int code = readUShortValue(buffer);
    Tag tag = Tag.get(code);
    if (tag == null) tag = new Tag(code);
    FieldType type = FieldType.get(readUShortValue(buffer));
    int count = buffer.getInt();

    ifd = new IFDEntry(tag, type, count);

    if (ifd.count * ifd.type.size <= 4) {
      readValues(buffer, ifd);
    } else {
      int offset = buffer.getInt();
      if (debugRead) System.out.println("position to " + offset);
      channel.position(offset);
      ByteBuffer vbuffer = ByteBuffer.allocate(ifd.count * ifd.type.size);
      vbuffer.order(byteOrder);
      channel.read(vbuffer);
      vbuffer.flip();
      readValues(vbuffer, ifd);
    }

    return ifd;
  }

  /*
     * Construct a GeoKey from an IFDEntry.
     * @param id GeoKey.Tag number
     * @param v value
     *
    GeoKey(int id, IFDEntry data, int vcount, int offset) {
      this.id = id;
      this.geoTag = GeoKey.Tag.get(id);
      this.count = vcount;

      if (data.type == FieldType.SHORT) {

        if (vcount == 1)
          geoValue = TagValue.get(geoTag, offset);
        else {
          value = new int[vcount];
          for (int i=0; i<vcount; i++)
            value[i] = data.value[offset + i];
        }

      if (geoValue == null) {
        if (data.type == FieldType.ASCII)
          valueS = data.valueS.substring( offset, offset+vcount);
        else {
          value = new int[vcount];
          for (int i=0; i<vcount; i++)
            value[i] = data.value[offset + i];
        }
      }
    }

  */

  private void readValues(ByteBuffer buffer, IFDEntry ifd) {

    if (ifd.type == FieldType.ASCII) {
      ifd.valueS = readSValue(buffer, ifd);

    } else if (ifd.type == FieldType.RATIONAL) {
      ifd.value = new int[ifd.count * 2];
      for (int i = 0; i < ifd.count * 2; i++)
        ifd.value[i] = readIntValue(buffer, ifd);

    } else if (ifd.type == FieldType.FLOAT) {
      ifd.valueD = new double[ifd.count];
      for (int i = 0; i < ifd.count; i++)
        ifd.valueD[i] = (double) buffer.getFloat();

    } else if (ifd.type == FieldType.DOUBLE) {
      ifd.valueD = new double[ifd.count];
      for (int i = 0; i < ifd.count; i++)
        ifd.valueD[i] = buffer.getDouble();

    } else {
      ifd.value = new int[ifd.count];
      for (int i = 0; i < ifd.count; i++)
        ifd.value[i] = readIntValue(buffer, ifd);
    }

  }

  private int readIntValue(ByteBuffer buffer, IFDEntry ifd) {
    switch (ifd.type.code) {
      case 1:
        return (int) buffer.get();
      case 2:
        return (int) buffer.get();
      case 3:
        return readUShortValue(buffer);
      case 4:
        return buffer.getInt();
      case 5:
        return buffer.getInt();
    }
    return 0;
  }

  private int readUShortValue(ByteBuffer buffer) {
    return buffer.getShort() & 0xffff;
  }

  private String readSValue(ByteBuffer buffer, IFDEntry ifd) {
    byte[] dst = new byte[ifd.count];
    buffer.get(dst);
    return new String(dst);
  }

  private void printBytes(PrintStream ps, String head, ByteBuffer buffer, int n) {
    ps.print(head + " == ");
    for (int i = 0; i < n; i++) {
      byte b = buffer.get();
      int ub = (b < 0) ? b + 256 : b;
      ps.print(ub + "(");
      ps.write(b);
      ps.print(") ");
    }
    ps.println();
  }

  /////////////////////////////////////////////////////////////////////////////
  // geotiff stuff

  private void parseGeoInfo() {
    IFDEntry keyDir = findTag(Tag.GeoKeyDirectoryTag);
    IFDEntry dparms = findTag(Tag.GeoDoubleParamsTag);
    IFDEntry aparams = findTag(Tag.GeoAsciiParamsTag);

    if (null == keyDir) return;

    int nkeys = keyDir.value[3];
    if (debugReadGeoKey) System.out.println("parseGeoInfo nkeys = " + nkeys + " keyDir= " + keyDir);
    int pos = 4;

    for (int i = 0; i < nkeys; i++) {
      int id = keyDir.value[pos++];
      int location = keyDir.value[pos++];
      int vcount = keyDir.value[pos++];
      int offset = keyDir.value[pos++];

      GeoKey.Tag tag = GeoKey.Tag.getOrMake(id);

      GeoKey key = null;
      if (location == 0) { // simple case
        key = new GeoKey(id, offset);

      } else { // more than one, or non short value
        IFDEntry data = findTag(Tag.get(location));
        if (data == null) {
          System.out.println("********ERROR parseGeoInfo: cant find Tag code = " + location);
        } else if (data.tag == Tag.GeoDoubleParamsTag) { // double params
          double[] dvalue = new double[vcount];
          for (int k = 0; k < vcount; k++)
            dvalue[k] = data.valueD[offset + k];
          key = new GeoKey(tag, dvalue);

        } else if (data.tag == Tag.GeoKeyDirectoryTag) { // int params
          int[] value = new int[vcount];
          for (int k = 0; k < vcount; k++)
            value[k] = data.value[offset + k];
          key = new GeoKey(tag, value);

        } else if (data.tag == Tag.GeoAsciiParamsTag) { // ascii params
          String value = data.valueS.substring(offset, offset + vcount);
          key = new GeoKey(tag, value);
        }

      }

      if (key != null) {
        keyDir.addGeoKey(key);
        if (debugReadGeoKey) System.out.println(" yyy  add geokey=" + key);
      }
    }

  }

  /**
   * Write the geotiff Tag information to out.
   */
  public void showInfo(PrintStream out) {
    out.println("Geotiff file= " + filename);
    for (int i = 0; i < tags.size(); i++) {
      IFDEntry ifd = tags.get(i);
      out.println(i + " IFDEntry == " + ifd);
    }
  }

  /**
   * Write the geotiff Tag information to a String.
   */
  public String showInfo() {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
    showInfo(new PrintStream(bout));
    return bout.toString();
  }

  /**
   * test
   */
  public static void main(String[] argv) {
    try {
      GeoTiff geotiff = new GeoTiff("/home/yuanho/tmp/ilatlon_float.tif");
      //GeoTiff geotiff = new GeoTiff("/home/yuanho/tmp/maxtemp.tif");
      //GeoTiff geotiff = new GeoTiff("/home/yuanho/tmp/test.tif");
      //GeoTiff geotiff = new GeoTiff("C:/data/geotiff/c41078a1.tif");
      //GeoTiff geotiff = new GeoTiff("C:/data/geotiff/L7ETMbands147.tif");
      //GeoTiff geotiff = new GeoTiff("data/blueTest.tiff");

      /*GeoTiff geotiff = new GeoTiff("data/testWrite.tiff");
      geotiff.addTag( new IFDEntry(Tag.SampleFormat, FieldType.SHORT, 3));
      geotiff.write();
      geotiff.close();

      geotiff = new GeoTiff("data/testWrite.tiff"); */
      geotiff.read();
      geotiff.showInfo(System.out);
      geotiff.testReadData();
      geotiff.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}