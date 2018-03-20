/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.stream;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * Write a NetcdfFile to a WritableByteChannel using ncstream protocol.
 * Experimental, checking performance vs OutputStream
 *
 * @author caron
 * @since 7/10/12
 */
public class NcStreamWriterChannel {
  static private final long maxChunk = 1000 * 1000; // 1 MByte
  static private final int sizeToCache = 100; // when to store a variable's data in the header, ie "immediate" mode

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

  public long sendData(Variable v, Section section, WritableByteChannel wbc, NcStreamCompression compress) throws IOException, InvalidRangeException {
    if (show) System.out.printf(" %s section=%s%n", v.getFullName(), section);

    ByteOrder bo = ByteOrder.nativeOrder(); // reader makes right
    long size = 0;
    size += writeBytes(wbc, NcStream.MAGIC_DATA); // magic
    NcStreamProto.Data dataProto = NcStream.encodeDataProto(v, section, compress.type, bo, 0);
    byte[] datab = dataProto.toByteArray();
    size += NcStream.writeVInt(wbc, datab.length); // dataProto len
    size += writeBytes(wbc, datab); // dataProto

    // version < 3
    if (v.getDataType() == DataType.SEQUENCE) {
      assert (v instanceof Structure);
      int count = 0;
      DataOutputStream os = new DataOutputStream(Channels.newOutputStream(wbc));
      Structure seq = (Structure) v; // superclass for Sequence, SequenceDS
      //coverity[FB.BC_UNCONFIRMED_CAST]
      try (StructureDataIterator iter = seq.getStructureIterator(-1)) {
        while (iter.hasNext()) {
          size += writeBytes(wbc, NcStream.MAGIC_VDATA); // magic
          ArrayStructureBB abb = StructureDataDeep.copyToArrayBB(iter.next());
          size += NcStream.encodeArrayStructure(abb, bo, os);
          count++;
        }
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
        size += copyChunks(wbc, v, maxChunk, compress);
      } else {
        size += sendData(v, v.getShapeAsSection(), wbc, compress);
      }
    }

    size += writeBytes(wbc, NcStream.MAGIC_END);
    if (show) System.out.printf("total size= %d%n", size);
    return size;
  }

  private long copyChunks(WritableByteChannel wbc, Variable oldVar, long maxChunkSize, NcStreamCompression compress) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    FileWriter2.ChunkingIndex index = new FileWriter2.ChunkingIndex(oldVar.getShape());
    long size = 0;
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        size += sendData(oldVar, new Section(chunkOrigin, chunkShape), wbc, compress);
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
