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
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.DataType;

import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Write a NetcdfFile to a WritableByteChannel using ncstream protocol
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamWriter {
  static private int sizeToCache = 100; // when to store a variable's data in the header, ie "immediate" mode

  private NetcdfFile ncfile;
  private NcStreamProto.Header header;
  private boolean show = false;

  public NcStreamWriter(NetcdfFile ncfile, String location) throws IOException {
    this.ncfile = ncfile;
    NcStreamProto.Group.Builder rootBuilder = NcStream.encodeGroup(ncfile.getRootGroup(), sizeToCache);

    NcStreamProto.Header.Builder headerBuilder = NcStreamProto.Header.newBuilder();
    headerBuilder.setLocation(location == null ? ncfile.getLocation() : location);
    if (ncfile.getTitle() != null) headerBuilder.setTitle(ncfile.getTitle());
    if (ncfile.getId() != null) headerBuilder.setId(ncfile.getId());
    headerBuilder.setRoot(rootBuilder);

    header = headerBuilder.build();
  }

  public long sendHeader(WritableByteChannel wbc) throws IOException {
    long size = 0;

    //// header message
    size += writeBytes(wbc, NcStream.MAGIC_START); // magic
    size += writeBytes(wbc, NcStream.MAGIC_HEADER); // magic
    byte[] b = header.toByteArray();
    size += NcStream.writeVInt(wbc, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);

    // payload
    size += writeBytes(wbc, b);
    if (show) System.out.println(" header size=" + size);

    return size;
  }

  public long sendData(Variable v, Section section, WritableByteChannel wbc) throws IOException, InvalidRangeException {
    long size = 0;
    size += writeBytes(wbc, NcStream.MAGIC_DATA); // magic
    NcStreamProto.Data dataProto = NcStream.encodeDataProto(v, section);
    byte[] datab = dataProto.toByteArray();
    size += NcStream.writeVInt(wbc, datab.length); // proto len
    size += writeBytes(wbc, datab); //proto
    if (show) System.out.println(v.getName() + " proto len=" + datab.length);

    long len = section.computeSize();
    if ((v.getDataType() != DataType.STRING) && (v.getDataType() != DataType.OPAQUE)) // vdataMessage
      len *= v.getElementSize();
    size += NcStream.writeVInt(wbc, (int) len); // data len or number of objects
    if (show) System.out.println(v.getName() + " vlen=" + len);

    long readCount = v.readToByteChannel(section, wbc);

    return readCount;
  }

  private int writeBytes(WritableByteChannel wbc, byte[] b) throws IOException {
    return wbc.write(ByteBuffer.wrap(b));
  }

  public void streamAll(WritableByteChannel wbc) throws IOException, InvalidRangeException {
    sendHeader(wbc);

    long size = 0;
    for (Variable v : ncfile.getVariables())
      size += sendData(v, v.getShapeAsSection(), wbc);

    if (show) System.out.println(" total size=" + size);
  }


}

