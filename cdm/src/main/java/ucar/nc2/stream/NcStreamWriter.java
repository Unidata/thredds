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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;

import java.io.*;
import java.nio.ByteOrder;

/**
 * Write a NetcdfFile to an OutputStream using ncstream protocol
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamWriter {
  static private long maxChunk = 1000 * 1000; // 1 MByte
  static private final int sizeToCache = 100; // when to store a variable's data in the header, ie "immediate" mode
  static private final int currentVersion = 1;

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
    headerBuilder.setVersion(currentVersion);

    header = headerBuilder.build();
  }

  public long sendStart(OutputStream out) throws IOException {
    return writeBytes(out, NcStream.MAGIC_START);
  }

  public long sendEnd(OutputStream out) throws IOException {
    return writeBytes(out, NcStream.MAGIC_END);
  }

  public long sendHeader(OutputStream out) throws IOException {
    long size = 0;

    //// header message
    size += writeBytes(out, NcStream.MAGIC_HEADER);
    byte[] b = header.toByteArray();
    size += NcStream.writeVInt(out, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);

    // payload
    size += writeBytes(out, b);
    if (show) System.out.println(" header size=" + size);

    return size;
  }

  public long sendData(Variable v, Section section, OutputStream out, NcStreamCompression compress) throws IOException, InvalidRangeException {
    if (show) System.out.printf(" %s section=%s%n", v.getFullName(), section);

    // length of data uncompressed
    long uncompressedLength = section.computeSize();
    if ((v.getDataType() != DataType.STRING) && (v.getDataType() != DataType.OPAQUE) && !v.isVariableLength())
      uncompressedLength *= v.getElementSize(); // nelems for vdata, else nbytes

    ByteOrder bo = ByteOrder.nativeOrder(); // reader makes right
    long size = 0;
    size += writeBytes(out, NcStream.MAGIC_DATA); // magic
    NcStreamProto.Data dataProto = NcStream.encodeDataProto(v, section, compress.type, bo, (int) uncompressedLength);
    byte[] datab = dataProto.toByteArray();
    size += NcStream.writeVInt(out, datab.length); // dataProto len
    size += writeBytes(out, datab); // dataProto

    // version < 3
    if (v.getDataType() == DataType.SEQUENCE) {
      assert (v instanceof Structure);
      int count = 0;
      Structure seq = (Structure) v; // superclass for Sequence, SequenceDS
      //coverity[FB.BC_UNCONFIRMED_CAST]
      try (StructureDataIterator iter = seq.getStructureIterator(-1)) {
        while (iter.hasNext()) {
          size += writeBytes(out, NcStream.MAGIC_VDATA); // magic
          StructureData sdata = iter.next();
          ArrayStructure as = new ArrayStructureW(sdata);
          size += NcStream.encodeArrayStructure(as, bo, out);
          count++;
        }
      }
      size += writeBytes(out, NcStream.MAGIC_VEND);
      if (show) System.out.printf(" NcStreamWriter sent %d seqData bytes = %d%n", count, size);
      return size;
    }

    // version < 3
    if (v.getDataType() == DataType.STRUCTURE) {
      ArrayStructure abb = (ArrayStructure) v.read();   // read all - LOOK break this up into chunks if needed
      //coverity[FB.BC_UNCONFIRMED_CAST]
      size += NcStream.encodeArrayStructure(abb, bo, out);
      if (show) System.out.printf(" NcStreamWriter sent ArrayStructure bytes = %d%n", size);
      return size;
    }

    // Writing the size of the block is handled for us.
    out = compress.setupStream(out, (int)uncompressedLength);
    size += v.readToStream(section, out);
    out.flush();
    return size;
  }

  // LOOK compression not used
  public long sendData2(Variable v, Section section, OutputStream out, NcStreamCompression compress) throws IOException, InvalidRangeException {
    if (show) System.out.printf(" %s section=%s%n", v.getFullName(), section);

    boolean isVlen = v.isVariableLength(); //  && v.getRank() > 1;
    if (isVlen)
      v.read(section);
    NcStreamDataCol encoder = new NcStreamDataCol();
    NcStreamProto.DataCol dataProto = encoder.encodeData2(v.getFullName(), isVlen, section, v.read(section));

    // LOOK trap error, write error message ??

    // dataProto.writeDelimitedTo(out);
    long size = 0;
    size += writeBytes(out, NcStream.MAGIC_DATA2); // data version 3

    byte[] datab = dataProto.toByteArray();
    size += NcStream.writeVInt(out, datab.length); // dataProto len
    size += writeBytes(out, datab); // dataProto
    return size;
  }

  private int writeBytes(OutputStream out, byte[] b) throws IOException {
    out.write(b);
    return b.length;
  }

  public long streamAll(OutputStream out) throws IOException, InvalidRangeException {
    long size = writeBytes(out, NcStream.MAGIC_START);
    size += sendHeader(out);
    if (show) System.out.printf(" data starts at= %d%n", size);

    for (Variable v : ncfile.getVariables()) {
      NcStreamCompression compress;
      Attribute compressAtt = v.findAttribute(CDM.COMPRESS);
      if (compressAtt != null && compressAtt.isString()) {
        String compType = compressAtt.getStringValue();
        if (compType.equalsIgnoreCase(CDM.COMPRESS_DEFLATE)) {
          compress = NcStreamCompression.deflate();
        } else {
          if (show) System.out.printf(" Unknown compression type %s. Defaulting to none.%n", compType);
          compress = NcStreamCompression.none();
        }
      } else {
        compress = NcStreamCompression.none();
      }

      long vsize = v.getSize() * v.getElementSize();
      //if (vsize < sizeToCache) continue; // in the header;
      if (show) System.out.printf(" var %s len=%d starts at= %d%n", v.getFullName(), vsize, size);

      if (vsize > maxChunk) {
        size += copyChunks(out, v, maxChunk, compress);
      } else {
        size += sendData(v, v.getShapeAsSection(), out, compress);
      }
    }

    size += writeBytes(out, NcStream.MAGIC_END);
    if (show) System.out.printf("total size= %d%n", size);
    return size;
  }

  private long copyChunks(OutputStream out, Variable oldVar, long maxChunkSize, NcStreamCompression compress) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(oldVar.getShape());
    long size = 0;
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        size += sendData(oldVar, new Section(chunkOrigin, chunkShape), out, compress);
        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
    return size;
  }

}

