/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.hdf4;

import ucar.nc2.iosp.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.PositioningDataInputStream;
import ucar.ma2.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.nio.ByteBuffer;

/**
 * @author caron
 * @since Dec 17, 2007
 */
public class H4iosp extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4iosp.class);
  static private boolean showLayoutTypes = false;

  private RandomAccessFile raf;
  private H4header header = new H4header();

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return H4header.isValidFile(raf);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    header.read(raf, ncfile);
    ncfile.finish();
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    if (v instanceof Structure)
      return readStructureData((Structure) v, section);

    H4header.Vinfo vinfo = (H4header.Vinfo) v.getSPobject();
    DataType dataType = v.getDataType();
    vinfo.setLayoutInfo();   // make sure needed info is present

    // make sure section is complete
    section = Section.fill(section, v.getShape());

    if (vinfo.hasNoData) {
      Object arr = (vinfo.fillValue == null) ? IospHelper.makePrimitiveArray((int) section.computeSize(), dataType) :
        IospHelper.makePrimitiveArray((int) section.computeSize(), dataType, vinfo.fillValue);
      if (dataType == DataType.CHAR)
        arr = IospHelper.convertByteToChar((byte[]) arr);
      return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), arr);
    }

    if (!vinfo.isCompressed) {
      if (!vinfo.isLinked && !vinfo.isChunked) {
        Layout layout = new LayoutRegular(vinfo.start, v.getElementSize(), v.getShape(), section);
        Object data = IospHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, -1);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

      } else if (vinfo.isLinked) {
        Layout layout = new LayoutSegmented(vinfo.segPos, vinfo.segSize, v.getElementSize(), v.getShape(), section);
        Object data = IospHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, -1);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

      } else if (vinfo.isChunked) {
        H4ChunkIterator chunkIterator = new H4ChunkIterator(vinfo);
        Layout layout = new LayoutTiled(chunkIterator, vinfo.chunkSize, v.getElementSize(), section);
        Object data = IospHelper.readDataFill(raf, layout, dataType, vinfo.fillValue, -1);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);
      }

    } else {
      if (!vinfo.isLinked && !vinfo.isChunked) {
        if (showLayoutTypes) System.out.println("***notLinked, compressed");
        Layout index = new LayoutRegular(0, v.getElementSize(), v.getShape(), section);
        InputStream is = getCompressedInputStream(vinfo);
        PositioningDataInputStream dataSource = new PositioningDataInputStream(is);
        Object data = IospHelper.readDataFill(dataSource, index, dataType, vinfo.fillValue);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

      } else if (vinfo.isLinked) {
        if (showLayoutTypes) System.out.println("***Linked, compressed");
        Layout index = new LayoutRegular(0, v.getElementSize(), v.getShape(), section);
        InputStream is = getLinkedCompressedInputStream(vinfo);
        PositioningDataInputStream dataSource = new PositioningDataInputStream(is);
        Object data = IospHelper.readDataFill(dataSource, index, dataType, vinfo.fillValue);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);

      } else if (vinfo.isChunked) {
        LayoutBBTiled.DataChunkIterator chunkIterator = new H4CompressedChunkIterator(vinfo);
        LayoutBB layout = new LayoutBBTiled(chunkIterator, vinfo.chunkSize, v.getElementSize(), section);
        Object data = IospHelper.readDataFill(layout, dataType, vinfo.fillValue);
        return Array.factory(dataType.getPrimitiveClassType(), section.getShape(), data);
      }
    }

    throw new IllegalStateException();
  }

  /**
   * Structures must be fixed sized
   *
   * @param s       the record structure
   * @param section the record range to read
   * @return an ArrayStructure, with all the data read in.
   * @throws IOException                    on error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */
  private ucar.ma2.ArrayStructure readStructureData(ucar.nc2.Structure s, Section section) throws java.io.IOException, InvalidRangeException {
    H4header.Vinfo vinfo = (H4header.Vinfo) s.getSPobject();
    vinfo.setLayoutInfo();   // make sure needed info is present
    int recsize = vinfo.elemSize;

    // create the ArrayStructure
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      H4header.Minfo minfo = (H4header.Minfo) v2.getSPobject();
      m.setDataParam((int) (minfo.offset));
    }
    members.setStructureSize(recsize);
    ArrayStructureBB structureArray = new ArrayStructureBB(members, section.getShape());  // LOOK subset

    // loop over records
    byte[] result = structureArray.getByteBuffer().array();

    /*if (vinfo.isChunked) {
      InputStream is = getChunkedInputStream(vinfo);
      PositioningDataInputStream dataSource = new PositioningDataInputStream(is);
      Layout layout = new LayoutRegular(vinfo.start, recsize, s.getShape(), section);
      IospHelper.readData(dataSource, layout, DataType.STRUCTURE, result);  */

    if (!vinfo.isLinked && !vinfo.isCompressed) {
      Layout layout = new LayoutRegular(vinfo.start, recsize, s.getShape(), section);
      IospHelper.readData(raf, layout, DataType.STRUCTURE, result, -1);

    } else if (vinfo.isLinked && !vinfo.isCompressed) {
      Layout layout = new LayoutSegmented(vinfo.segPos, vinfo.segSize, recsize, s.getShape(), section);
      IospHelper.readData(raf, layout, DataType.STRUCTURE, result, -1);

    } else if (!vinfo.isLinked && vinfo.isCompressed) {
      InputStream is = getCompressedInputStream(vinfo);
      PositioningDataInputStream dataSource = new PositioningDataInputStream(is);
      Layout layout = new LayoutRegular(0, recsize, s.getShape(), section);
      IospHelper.readData(dataSource, layout, DataType.STRUCTURE, result);

    } else if (vinfo.isLinked && vinfo.isCompressed) {
      InputStream is = getLinkedCompressedInputStream(vinfo);
      PositioningDataInputStream dataSource = new PositioningDataInputStream(is);
      Layout layout = new LayoutRegular(0, recsize, s.getShape(), section);
      IospHelper.readData(dataSource, layout, DataType.STRUCTURE, result);

    } else {
      throw new IllegalStateException();
    }

    return structureArray;
  }

  public void close() throws IOException {
    raf.close();
  }

  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      H4header.Vinfo vinfo = (H4header.Vinfo) v.getSPobject();
      return (vinfo != null) ? vinfo.toString() : "";
    }
    return null;
  }

  private InputStream getCompressedInputStream(H4header.Vinfo vinfo) throws IOException {
    // probably could construct an input stream from a channel from a raf
    // for now, just read it in.
    //System.out.println(" read "+ vinfo.length+" from "+ vinfo.start);
    byte[] buffer = new byte[vinfo.length];
    raf.seek(vinfo.start);
    raf.readFully(buffer);
    ByteArrayInputStream in = new ByteArrayInputStream(buffer);
    return new java.util.zip.InflaterInputStream(in);
  }

  private InputStream getLinkedCompressedInputStream(H4header.Vinfo vinfo) {
    return new java.util.zip.InflaterInputStream(new LinkedInputStream(vinfo));
  }

  private class LinkedInputStream extends InputStream {
    byte[] buffer;
    long pos = 0;

    int segno = -1;
    int segpos = 0;
    int segSize = 0;
    H4header.Vinfo vinfo;

    LinkedInputStream(H4header.Vinfo vinfo) {
      this.vinfo = vinfo;
    }

    private void readSegment() throws IOException {
      segno++;
      segSize = vinfo.segSize[segno];
      buffer = new byte[segSize];  // Look: could do this in buffer size 4096 to save memory
      raf.seek(vinfo.segPos[segno]);
      raf.readFully(buffer);
      segpos = 0;
      //System.out.println("read at "+vinfo.segPos[segno]+ " n= "+segSize);
    }

    /*public int read(byte b[]) throws IOException {
      //System.out.println("request "+b.length);
      return super.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
      //System.out.println("request "+len+" buffer size="+b.length+" offset="+off);
      return super.read(b, off, len);
    } */

    public int read() throws IOException {
      if (segpos == segSize)
        readSegment();
      int b = buffer[segpos] & 0xff;
      //System.out.println("  byte "+b+ " at= "+segpos);
      segpos++;
      return b;
    }
  }

  private InputStream getChunkedInputStream(H4header.Vinfo vinfo) {
    return new ChunkedInputStream(vinfo);
  }

  // look not implemented : skip data
  private class ChunkedInputStream extends InputStream {
    List<H4header.DataChunk> chunks;
    int chunkNo;

    byte[] buffer;
    int segPos, segSize;

    ChunkedInputStream(H4header.Vinfo vinfo) {
      this.chunks = vinfo.chunks;
      this.chunkNo = 0;
    }

    private void readChunk() throws IOException {
      H4header.DataChunk chunk = chunks.get(chunkNo);
      H4header.TagData chunkData = chunk.data;
      chunkNo++;

      if (chunkData.ext_type == TagEnum.SPECIAL_COMP) {
        // read compressed data in
        H4header.TagData cdata = chunkData.compress.getDataTag();
        byte[] cbuffer = new byte[cdata.length];
        raf.seek(cdata.offset);
        raf.readFully(cbuffer);
        // System.out.println("  read compress " + cdata.length + " at= " + cdata.offset);

        // uncompress it
        if (chunkData.compress.compress_type == TagEnum.COMP_CODE_DEFLATE) {
          InputStream in = new java.util.zip.InflaterInputStream(new ByteArrayInputStream(cbuffer));
          ByteArrayOutputStream out = new ByteArrayOutputStream(chunkData.compress.uncomp_length);
          IO.copy(in, out);
          buffer = out.toByteArray();
          //System.out.println("  uncompress "+buffer.length + ", wanted="+chunkData.compress.uncomp_length);
        } else {
          throw new IllegalStateException("unknown compression type =" + chunkData.compress.compress_type);
        }

      } else {
        buffer = new byte[chunkData.length];
        raf.seek(chunkData.offset);
        raf.readFully(buffer);
      }

      segPos = 0;
      segSize = buffer.length;
    }

    public int read() throws IOException {
      if (segPos == segSize)
        readChunk();
      int b = buffer[segPos] & 0xff;
      //System.out.println("  byte "+b+ " at= "+segPos);
      segPos++;
      return b;
    }

    /* public int read(byte b[]) throws IOException {
      System.out.println("request "+b.length);
      return super.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
      System.out.println("request "+len+" buffer size="+b.length+" offset="+off);
      return super.read(b, off, len);
    } // */
  }

  private class H4ChunkIterator implements LayoutTiled.DataChunkIterator {
    List<H4header.DataChunk> chunks;
    int chunkNo;

    byte[] buffer;
    int segPos, segSize;

    H4ChunkIterator(H4header.Vinfo vinfo) {
      this.chunks = vinfo.chunks;
      this.chunkNo = 0;
    }

    public boolean hasNext() {
      return chunkNo < chunks.size();
    }

    public LayoutTiled.DataChunk next() throws IOException {
      H4header.DataChunk chunk = chunks.get(chunkNo);
      H4header.TagData chunkData = chunk.data; 
      chunkNo++;

      return new LayoutTiled.DataChunk(chunk.origin, chunkData.offset);
    }
  }

  private class H4CompressedChunkIterator implements LayoutBBTiled.DataChunkIterator {
    List<H4header.DataChunk> chunks;
    int chunkNo;

    H4CompressedChunkIterator(H4header.Vinfo vinfo) {
      this.chunks = vinfo.chunks;
      this.chunkNo = 0;
    }

    public boolean hasNext() {
      return chunkNo < chunks.size();
    }

    public LayoutBBTiled.DataChunk next() throws IOException {
      H4header.DataChunk chunk = chunks.get(chunkNo);
      H4header.TagData chunkData = chunk.data;
      assert (chunkData.ext_type == TagEnum.SPECIAL_COMP);
      chunkNo++;

      return new DataChunk(chunk.origin, chunkData.compress);
    }
  }

  private class DataChunk implements LayoutBBTiled.DataChunk {
    private int[] offset; // offset index of this chunk, reletive to entire array
    private H4header.SpecialComp compress;
    private ByteBuffer bb;  // the data is placed into here

    public DataChunk(int[] offset, H4header.SpecialComp compress) {
      this.offset = offset;
      this.compress = compress;
    }

    public int[] getOffset() {
      return offset;
    }

    public ByteBuffer getByteBuffer() throws IOException {
      if (bb == null) {
        // read compressed data in
        H4header.TagData cdata = compress.getDataTag();
        byte[] cbuffer = new byte[cdata.length];
        raf.seek(cdata.offset);
        raf.readFully(cbuffer);
        //System.out.println("  read compress " + cdata.length + " at= " + cdata.offset);

        // uncompress it
        if (compress.compress_type == TagEnum.COMP_CODE_DEFLATE) {
          InputStream in = new java.util.zip.InflaterInputStream(new ByteArrayInputStream(cbuffer));
          ByteArrayOutputStream out = new ByteArrayOutputStream(compress.uncomp_length);
          IO.copy(in, out);
          byte[] buffer = out.toByteArray();
          bb = ByteBuffer.wrap(buffer);
          //System.out.println("  uncompress " + buffer.length + ", wanted=" + compress.uncomp_length);
        } else {
          throw new IllegalStateException("unknown compression type =" + compress.compress_type);
        }
      }
      
      return bb;
    }

  }


}
