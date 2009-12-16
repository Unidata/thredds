/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.bufr;

import ucar.ma2.*;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.unidata.io.RandomAccessFile;

import java.util.Formatter;
import java.util.List;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Class Description
 *
 * @author caron
 * @since Nov 15, 2009
 */


/*
  Within one message there are n obs (datasets) and s fields in each dataset.
  For uncompressed datasets, storage order is data(obs, fld) (fld varying fastest) :

    R11, R12, R13, . . . R1s
    R21, R22, R23, . . . R2s
    ....
    Rn1, Rn2, Rn3, . . . Rns

   where Rij is the jth value of the ith data subset. 
   the datasets each occupy an identical number of bits, unless delayed replication is used,
   and are not necessarily aligned on octet boundaries.

   A replicated field (structure) takes a group of fields and replicates them:

     Ri1, (Ri2, Ri3)*r, . . . Ris

   where r is set in the data descriptor, and is the same for all datasets.

   A delayed replicated field (sequence) takes a group of fields and replicates them, and adds the number of replications
   in the data :

     Ri1, dri, (Ri2, Ri3)*dri, . . . Ris

   where the width (nbits) of dr is set in the data descriptor. This dr can be different for each dataset in the message.
   It can be 0. When it has a bit width of 1, it indicates an optional set of fields.

   --------------------------

   We use an ArrayStructureBB to hold the data, and fill it sequentially as we scan the message.
   Fixed length nested Structures are kept in the ArrayStructureBB.
   Variable length objects (Strings, Sequences) are added to the heap.
 */

public class MessageUncompressedDataReader {

  /**
   * Read all datasets from a single message
   * @param s outer variables
   * @param proto prototype message, has been processed
   * @param m   read this message
   * @param raf from this file
   * @param f  output bit count debugging info (may be null)
   * @return  ArraySTructure with all the data from the message in it.
   * @throws IOException on read error
   */
  public ArrayStructure readEntireMessage(Structure s, Message proto, Message m, RandomAccessFile raf, Formatter f) throws IOException {
    // transfer info from proto message
    DataDescriptor.transferInfo(proto.getRootDataDescriptor().getSubKeys(), m.getRootDataDescriptor().getSubKeys());

    // allocate ArrayStructureBB for outer structure
    // This assumes that all of the fields and all of the datasets are being read
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureBB.setOffsets(members);

    int n = m.getNumberDatasets();
    ArrayStructureBB abb = new ArrayStructureBB(members, new int[]{n});
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    boolean addTime = (s.findVariable(ConstructNC.TIME_NAME) != null);
    readData(abb, m, raf, null, addTime, f);

    return abb;
  }

  // read / count the bits in an uncompressed message
  // This assumes that all of the fields and all of the datasets are being read

  /**
   * Read some or all datasets from a single message
   *
   * @param abb place data into here in order (may be null)
   * @param m   read this message
   * @param raf from this file
   * @param r which datasets, reletive to this message. null == all.
   * @param addTime add the time coordinate
   * @param f  output bit count debugging info (may be null)
   * @return number of datasets read
   * @throws IOException on read error
   */
  public int readData(ArrayStructureBB abb, Message m, RandomAccessFile raf, Range r, boolean addTime, Formatter f) throws IOException {
    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    DataDescriptor root = m.getRootDataDescriptor();
    if (root.isBad) return 0;

    Request req = new Request(abb, r);

    int n = m.getNumberDatasets();
    m.counterDatasets = new BitCounterUncompressed[n]; // one for each dataset
    m.msg_nbits = 0;

    // loop over the rows
    int count = 0;
    for (int i = 0; i < n; i++) {
      if (f != null) f.format("Count bits in observation %d\n", i);
      // the top table always has exactly one "row", since we are working with a single obs
      m.counterDatasets[i] = new BitCounterUncompressed(root, 1, 0);
      DebugOut out = (f == null) ? null : new DebugOut(f);

      req.setRow(i);
      int timePos = 0;
      if (req.wantRow() && addTime) {
        timePos = req.bb.position();
        req.bb.putInt(0); // placeholder for time assumes an int
        count++;
      }

      readData(out, reader, m.counterDatasets[i], root.subKeys, 0, req);
      m.msg_nbits += m.counterDatasets[i].countBits(m.msg_nbits);
    }

    return count;
  }

  private class Request {
    ArrayStructureBB abb;
    ByteBuffer bb;
    Range r;
    int row;

    Request(ArrayStructureBB abb, Range r) {
      this.abb = abb;
      if (abb != null) bb = abb.getByteBuffer();
      this.r = r;
      this.row = 0;
    }

    Request setRow(int row) {
      this.row = row;
      return this;
    }

    boolean wantRow() {
      if (abb == null) return false;
      if (r == null) return true;
      return r.contains(row);
    }

  }

  /**
   * count/read the bits in one row of a "nested table", defined by List<DataDescriptor> dkeys.
   *
   * @param out    optional debug output, may be null
   * @param reader read data with this
   * @param dkeys  the fields of the table
   * @param table  put the results here
   * @param nestedRow    which row of the table
   * @param req    read data into here, may be null
   * @throws IOException on read error
   */
  private void readData(DebugOut out, BitReader reader, BitCounterUncompressed table, List<DataDescriptor> dkeys,
                                    int nestedRow, Request req) throws IOException {

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) {// misc skip
        if (out != null) out.f.format("%s %d %s (%s) %n", out.indent(), out.fldno++, dkey.name, dkey.getFxyName());
        continue;
      }

      // sequence
      if (dkey.replication == 0) {

        int count = (int) reader.bits2UInt(dkey.replicationCountSize);
        if (out != null) out.f.format("%4d delayed replication count=%d %n", out.fldno++, count);
        /* if (count < 0) {
          System.out.println("HEY");
          count = 0;
        } */

        if ((out != null) && (count > 0)) {
          out.f.format("%4d %s read sequence %s count= %d bitSize=%d start at=0x%x %n",
                  out.fldno, out.indent(), dkey.getFxyName(), count, dkey.replicationCountSize, reader.getPos());
        }

        BitCounterUncompressed bitCounterNested = table.makeNested(dkey, count, nestedRow, dkey.replicationCountSize);

        // make an ArraySequence for this observation
        ArraySequence seq = makeArraySequenceUncompressed(out, reader, bitCounterNested, dkey, req);

        if (req.wantRow()) {
          int index = req.abb.addObjectToHeap(seq);
          req.bb.putInt(index); // an index into the Heap
        }
        continue;
      }

      // compound
      if (dkey.type == 3) {
        BitCounterUncompressed nested = table.makeNested(dkey, dkey.replication, nestedRow, 0);
        if (out != null)
          out.f.format("%4d %s read structure %s count= %d\n",
                  out.fldno, out.indent(), dkey.getFxyName(), dkey.replication);

        for (int i = 0; i < dkey.replication; i++) {
          if (out != null) {
            out.f.format("%s read row %d (struct %s) %n", out.indent(), i, dkey.getFxyName());
            out.indent.incr();
            readData(out, reader, nested, dkey.subKeys, i, req);
            out.indent.decr();
          } else {
            readData(null, reader, nested, dkey.subKeys, i, req);
          }
        }
        continue;
      }

      // char data
      if (dkey.type == 1) {
        byte[] vals = readCharData(dkey, reader, req);
        if (out != null) {
          String s = new String(vals, "UTF-8");
          out.f.format("%4d %s read char %s (%s) width=%d end at= 0x%x val=<%s>\n",
                  out.fldno++, out.indent(), dkey.getFxyName(), dkey.getName(), dkey.bitWidth, reader.getPos(), s);
        }
        continue;
      }

      // otherwise read a number
      long val = readNumericData(dkey, reader, req);
      if (out != null)
        out.f.format("%4d %s read %s (%s %s) bitWidth=%d end at= 0x%x raw=%d convert=%f\n",
                out.fldno++, out.indent(), dkey.getFxyName(), dkey.getName(), dkey.getUnits(), dkey.bitWidth, reader.getPos(), val, dkey.convert(val));
    }

  }

  private byte[] readCharData(DataDescriptor dkey, BitReader reader, Request req) throws IOException {
    int nchars = dkey.getByteWidthCDM();
    byte[] b = new byte[nchars];
    for (int i = 0; i < nchars; i++)
      b[i] = (byte) reader.bits2UInt(8);

    if (req.wantRow()) {
      for (int i = 0; i < nchars; i++)
        req.bb.put(b[i]);
    }
    return b;
  }

  private long readNumericData(DataDescriptor dkey, BitReader reader, Request req) throws IOException {
    // numeric data
    long result = reader.bits2UInt(dkey.bitWidth);

    if (req.wantRow()) {

      // place into byte buffer
      if (dkey.getByteWidthCDM() == 1) {
        req.bb.put((byte) result);

      } else if (dkey.getByteWidthCDM() == 2) {
        byte b1 = (byte) (result & 0xff);
        byte b2 = (byte) ((result & 0xff00) >> 8);
        req.bb.put( b2);
        req.bb.put( b1);

      } else if (dkey.getByteWidthCDM() == 4) {
        byte b1 = (byte) (result & 0xff);
        byte b2 = (byte) ((result & 0xff00) >> 8);
        byte b3 = (byte) ((result & 0xff0000) >> 16);
        byte b4 = (byte) ((result & 0xff000000) >> 24);
        req.bb.put(b4);
        req.bb.put(b3);
        req.bb.put(b2);
        req.bb.put(b1);

      } else  {
        byte b1 = (byte)  (result & 0xff);
        byte b2 = (byte) ((result & 0xff00) >> 8);
        byte b3 = (byte) ((result & 0xff0000) >> 16);
        byte b4 = (byte) ((result & 0xff000000) >> 24);
        byte b5 = (byte) ((result & 0xff00000000L) >> 32);
        byte b6 = (byte) ((result & 0xff0000000000L) >> 40);
        byte b7 = (byte) ((result & 0xff000000000000L) >> 48);
        byte b8 = (byte) ((result & 0xff00000000000000L) >> 56);
        req.bb.put(b8);
        req.bb.put(b7);
        req.bb.put(b6);
        req.bb.put(b5);
        req.bb.put(b4);
        req.bb.put(b3);
        req.bb.put(b2);
        req.bb.put(b1);
      }
    }

    return result;
  }

  // read in the data into an ArrayStructureBB, wrapped by an ArraySequence
  private ArraySequence makeArraySequenceUncompressed(DebugOut out, BitReader reader, BitCounterUncompressed bitCounterNested,
                                                      DataDescriptor seqdd, Request req) throws IOException {

    int count = bitCounterNested.getNumberRows();
    ArrayStructureBB abb = null;
    StructureMembers members = null;

    if (req.wantRow()) {
      Sequence seq = (Sequence) seqdd.refersTo;
      assert seq != null;

      // for the obs structure
      int[] shape = new int[]{count};
      //if (count <= 0)
      //    System.out.println("HEY");

      // allocate ArrayStructureBB for outer structure
      int offset = 0;
      members = seq.makeStructureMembers();
      for (StructureMembers.Member m : members.getMembers()) {
        m.setDataParam(offset);
        //System.out.println(m.getName()+" offset="+offset);

        Variable mv = seq.findVariable(m.getName());
        DataDescriptor dk = (DataDescriptor) mv.getSPobject();
        if (dk.replication == 0)
          offset += 4;
        else
          offset += dk.getByteWidthCDM();
      }

      abb = new ArrayStructureBB(members, shape);
      ByteBuffer bb = abb.getByteBuffer();
      bb.order(ByteOrder.BIG_ENDIAN);
    }

    Request nreq = (req == null) ? null : new Request(abb, null);

    // loop through nested obs
    for (int i = 0; i < count; i++) {
      if (out != null) {
        out.f.format("%s read row %d (seq %s) %n", out.indent(), i, seqdd.getFxyName());
        out.indent.incr();
        readData(out, reader, bitCounterNested, seqdd.getSubKeys(), i, nreq);
        out.indent.decr();

      } else {
        readData(null, reader, bitCounterNested, seqdd.getSubKeys(), i, nreq);
      }
    }

    return req.wantRow() ? new ArraySequence(members, abb.getStructureDataIterator(), count) : null;
  }
}
