/*
 * Copyright 2009-2012 University Corporation for Atmospheric Research/Unidata
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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * Not used
 *
 * @author caron
 * @since 7/10/12
 */
public class NcStreamWriterChannel {
  static private long maxChunk = 1 * 1000 * 1000; // 1 MByte
  static private int sizeToCache = 100; // when to store a variable's data in the header, ie "immediate" mode

  private NetcdfFile ncfile;
  private NcStreamProto.Header header;
  private boolean show = false;

  public NcStreamWriterChannel(NetcdfFile ncfile, String location) throws IOException {
    this.ncfile = ncfile;
    NcStreamProto.Group.Builder rootBuilder = NcStream.encodeGroup(ncfile.getRootGroup(), sizeToCache);

    NcStreamProto.Header.Builder headerBuilder = NcStreamProto.Header.newBuilder();
    headerBuilder.setLocation(location == null ? ncfile.getLocation() : location);
    if (ncfile.getTitle() != null) headerBuilder.setTitle(ncfile.getTitle());
    if (ncfile.getId() != null) headerBuilder.setId(ncfile.getId());
    headerBuilder.setRoot(rootBuilder);

    header = headerBuilder.build();
  }

  public long sendStart(WritableByteChannel wbc) throws IOException {
    return writeBytes(wbc, NcStream.MAGIC_START);
  }

  public long sendEnd(WritableByteChannel wbc) throws IOException {
    return writeBytes(wbc, NcStream.MAGIC_END);
  }

  public long sendHeader(WritableByteChannel wbc) throws IOException {
    long size = 0;

    //// header message
    size += writeBytes(wbc, NcStream.MAGIC_HEADER);
    byte[] b = header.toByteArray();
    size += NcStream.writeVInt(wbc, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);

    // payload
    size += writeBytes(wbc, b);
    if (show) System.out.println(" header size=" + size);

    return size;
  }

  public long sendData(Variable v, Section section, WritableByteChannel wbc, boolean deflate) throws IOException, InvalidRangeException {
    if (show) System.out.printf(" %s section=%s%n", v.getFullName(), section);

    long size = 0;
    size += writeBytes(wbc, NcStream.MAGIC_DATA); // magic
    NcStreamProto.Data dataProto = NcStream.encodeDataProto(v, section, deflate, 0);
    byte[] datab = dataProto.toByteArray();
    size += NcStream.writeVInt(wbc, datab.length); // dataProto len
    size += writeBytes(wbc, datab); // dataProto

    if (v.getDataType() == DataType.SEQUENCE) {
      int count = 0;
      DataOutputStream os = new DataOutputStream(Channels.newOutputStream(wbc));
      Structure seq = (Structure) v; // superclass for Sequence, SequenceDS
      //coverity[FB.BC_UNCONFIRMED_CAST]
      StructureDataIterator iter = seq.getStructureIterator(-1);
      try {
        while (iter.hasNext()) {
          size += writeBytes(wbc, NcStream.MAGIC_VDATA); // magic
          ArrayStructureBB abb = StructureDataDeep.copyToArrayBB(iter.next());
          size += NcStream.encodeArrayStructure(abb, os);
          count++;
        }
      } finally {
        iter.finish();
      }
      size += writeBytes(wbc, NcStream.MAGIC_VEND);
      if (show) System.out.printf(" NcStreamWriter sent %d sdata bytes = %d%n", count, size);
      return size;
    }

    // regular arrays
    long len = section.computeSize();
    if ((v.getDataType() != DataType.STRING) && (v.getDataType() != DataType.OPAQUE) && !v.isVariableLength())
      len *= v.getElementSize(); // nelems for vdata, else nbytes

    size += NcStream.writeVInt(wbc, (int) len); // data len or number of objects
    if (show) System.out.printf("  %s proto=%d data=%d%n", v.getFullName(), datab.length, len);

    size += v.readToByteChannel(section, wbc); // try to do a direct transfer

    return size;
  }

  /* public long sendData(WritableByteChannel wbc, StructureData sdata) throws IOException {
   long size = 0;
   ByteBuffer bb = IospHelper.copyToByteBuffer(sdata);
   byte[] datab = bb.array();
   size += writeBytes(wbc, NcStream.MAGIC_DATA); // magic
   size += NcStream.writeVInt(wbc, datab.length); // data len
   size += writeBytes(wbc, datab); // data
   return size;
 } */

  private int writeBytes(WritableByteChannel wbc, byte[] b) throws IOException {
    return wbc.write(ByteBuffer.wrap(b));
  }

  public long streamAll(WritableByteChannel wbc) throws IOException, InvalidRangeException {
    long size = writeBytes(wbc, NcStream.MAGIC_START);
    size += sendHeader(wbc);
    if (show) System.out.printf(" data starts at= %d%n", size);

    for (Variable v : ncfile.getVariables()) {
      Attribute compressAtt = v.findAttribute(CDM.COMPRESS);
      boolean deflate = (compressAtt != null) && compressAtt.isString() && compressAtt.getStringValue().equalsIgnoreCase(CDM.COMPRESS_DEFLATE);

      long vsize = v.getSize() * v.getElementSize();
      //if (vsize < sizeToCache) continue; // in the header;
      if (show) System.out.printf(" var %s len=%d starts at= %d%n", v.getFullName(), vsize, size);

      if (vsize > maxChunk) {
        size += copyChunks(wbc, v, maxChunk, deflate);
      } else {
        size += sendData(v, v.getShapeAsSection(), wbc, deflate);
      }
    }

    size += writeBytes(wbc, NcStream.MAGIC_END);
    if (show) System.out.printf("total size= %d%n", size);
    return size;
  }

  private long copyChunks(WritableByteChannel wbc, Variable oldVar, long maxChunkSize, boolean deflate) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(oldVar.getShape());
    long size = 0;
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        size += sendData(oldVar, new Section(chunkOrigin, chunkShape), wbc, deflate);
        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
    return size;
  }

  static public void main2(String args[]) throws InvalidRangeException {
    int[] totalShape = new int[]{1, 40, 530, 240};
    int[] chunkShape = new int[]{1, 1, 530, 240};
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(totalShape);
    while (index.currentElement() < index.getSize()) {
      int[] chunkOrigin = index.getCurrentCounter();
      System.out.printf(" %s%n", new Section(chunkOrigin, chunkShape));
      index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
    }

  }

  static public void main(String args[]) throws InvalidRangeException {
    long maxChunkElems = maxChunk / 4;
    int[] totalShape = new int[]{1, 40, 530, 240};
    //int[] chunkShape = new int[] {1, 1, 530, 240};
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(totalShape);
    while (index.currentElement() < index.getSize()) {
      int[] chunkOrigin = index.getCurrentCounter();
      int[] chunkShape = index.computeChunkShape(maxChunkElems);
      System.out.printf(" %s%n", new Section(chunkOrigin, chunkShape));
      index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
    }

  }

}
