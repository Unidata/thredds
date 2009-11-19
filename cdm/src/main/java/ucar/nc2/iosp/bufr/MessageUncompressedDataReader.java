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
    ………………………
    ………………………
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
 */

public class MessageUncompressedDataReader {

  public Array readData(Structure s, Message proto, Message m, RandomAccessFile raf, Formatter f) throws IOException, InvalidRangeException {
    // transfer info from proto message
    DataDescriptor.transferInfo(proto.getRootDataDescriptor().getSubKeys(), m.getRootDataDescriptor().getSubKeys());

    // allocate ArrayStructureBB for outer structure
    // This assumes that all of the fields and all of the datasets are being read
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureBB.setOffsets(members);

    ArrayStructureBB abb = new ArrayStructureBB(members, new int[] {m.getNumberDatasets()} );
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    readDataUncompressed(m, raf, f, abb);

    return abb;
  }

  // read / count the bits in an uncompressed message
  // This assumes that all of the fields and all of the datasets are being read

  public int readDataUncompressed(Message m, RandomAccessFile raf, Formatter f, ArrayStructureBB abb) throws IOException {
    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    DataDescriptor root = m.getRootDataDescriptor();
    if (root.isBad) return 0;

    int n = m.getNumberDatasets();
    BitCounterUncompressed[] counterDatasets = new BitCounterUncompressed[n]; // one for each dataset
    m.msg_nbits = 0;

    // loop over the rows
    for (int i = 0; i < n; i++) {
      if (f != null) f.format("Count bits in observation %d\n", i);
      // the top table always has exactly one "row", since we are working with a single obs
      counterDatasets[i] = new BitCounterUncompressed(root, 1, 0);
      DebugOut out = (f == null) ? null : new DebugOut(f, "obs " + i, 0, 1);
      readDataUncompressed(out, reader, counterDatasets[i], root.subKeys, 0, abb);
      m.msg_nbits += counterDatasets[i].countBits(m.msg_nbits);
    }

    return m.msg_nbits;
  }

  /**
   * count the bits in one row of a "nested table", defined by List<DataDescriptor> dkeys.
   *
   * @param out    optional debug output, may be null
   * @param reader read data with this
   * @param dkeys  the fields of the table
   * @param table  put the results here
   * @param row    which row of the table
   * @param abb    read data into here, may be null
   * @throws IOException on read error
   *
   * @return the current fldno
   */
  private int readDataUncompressed(DebugOut out, BitReader reader, BitCounterUncompressed table, List<DataDescriptor> dkeys,
                                    int row, ArrayStructureBB abb) throws IOException {

    ByteBuffer bb = (abb == null) ? null : abb.getByteBuffer();

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) {// misc skip
        System.out.printf("HEY skipping %s %n", dkey);
        continue;
      }

      // sequence
      if (dkey.replication == 0) {

        int count = reader.bits2UInt(dkey.replicationCountSize);
        if (out != null) out.f.format("%4d delayed replication %d %n", out.fldno++, count);
        //if (count == 0) continue;

        if ((out != null) && (count > 0)) {
          out.f.format("%4d %s read sequence %s count= %d bitSize=%d start at=0x%x %n",
              out.fldno++, out.indent(), dkey.getFxyName(), count, dkey.replicationCountSize, reader.getPos());
        }

        BitCounterUncompressed bitCounterNested = table.makeNested(dkey, count, row, dkey.replicationCountSize);

        // make an ArraySequence for this observation
        ArraySequence seq = makeArraySequenceUncompressed(out, reader, bitCounterNested, dkey, (abb == null));

        if (abb != null) {
          int index = abb.addObjectToHeap(seq);
          bb.putInt(index); // an index into the Heap
        }
        continue;
      }

      // compound
      if (dkey.type == 3) {
        BitCounterUncompressed nested = table.makeNested(dkey, dkey.replication, row, 0);
        if (out != null)
          out.f.format("%4d %s read structure %s count= %d\n",
              out.fldno++, out.indent(), dkey.getFxyName(), dkey.replication);

        for (int i = 0; i < dkey.replication; i++) {
          if (out != null) {
            out.f.format("%s read row %d (struct %s) of %s %n", out.indent(), i, dkey.getFxyName(), out.where);
            String whereNested = (out == null) ? null : out.where + " row " + i + "( struct " + dkey.getFxyName() + ")";
            out.fldno = readDataUncompressed( out.nested(whereNested), reader, nested, dkey.subKeys, i, abb);
          } else {
            readDataUncompressed( null, reader, nested, dkey.subKeys, i, abb);
          }
        }
        continue;
      }

      // char data
      if (dkey.type == 1) {
        byte[] vals = readCharData(dkey, reader, bb);
        if (out != null) {
          String s = new String(vals, "UTF-8");
          out.f.format("%4d %s read char %s bitWidth=%d end at= 0x%x val=%s\n",
              out.fldno++, out.indent(), dkey.getFxyName(), dkey.bitWidth, reader.getPos(), s);
        }
        continue;
      }

      // otherwise read a number
      int val = readNumericData(dkey, reader, bb);
      if (out != null)
        out.f.format("%4d %s read %s bitWidth=%d end at= 0x%x raw=%d convert=%f\n",
            out.fldno++, out.indent(), dkey.getFxyName(), dkey.bitWidth, reader.getPos(), val, dkey.convert(val));
    }

    return (out == null) ? 0 : out.fldno;
  }

  private byte[] readCharData(DataDescriptor dkey, BitReader reader, ByteBuffer bb) throws IOException {
    int nchars = dkey.getByteWidthCDM();
    byte[] b = new byte[nchars];
    for (int i = 0; i < nchars; i++)
      b[i] = (byte) reader.bits2UInt(8);

    if (bb != null) {
      for (int i = 0; i < nchars; i++)
        bb.put(b[i]);
    }
    return b;
  }

  private int readNumericData(DataDescriptor dkey, BitReader reader, ByteBuffer bb) throws IOException {
      // numeric data
      int result = reader.bits2UInt(dkey.bitWidth);

    if (bb != null) {

      // place into byte buffer
      if (dkey.getByteWidthCDM() == 1) {
        bb.put((byte) result);
      } else if (dkey.getByteWidthCDM() == 2) {
        int b1 = result & 0xff;
        int b2 = (result & 0xff00) >> 8;
        bb.put((byte) b2);
        bb.put((byte) b1);
      } else {
        int b1 = result & 0xff;
        int b2 = (result & 0xff00) >> 8;
        int b3 = (result & 0xff0000) >> 16;
        int b4 = (result & 0xff000000) >> 24;
        bb.put((byte) b4);
        bb.put((byte) b3);
        bb.put((byte) b2);
        bb.put((byte) b1);
      }
    }

    return result;
  }

  // read in the data into an ArrayStructureBB, wrapped by an ArraySequence
  private ArraySequence makeArraySequenceUncompressed(DebugOut out, BitReader reader, BitCounterUncompressed bitCounterNested,
          DataDescriptor seqdd, boolean countOnly) throws IOException {

    int count = bitCounterNested.getNumberRows();
    ArrayStructureBB abb = null;
    StructureMembers members = null;

    if (!countOnly) {
      Sequence seq = (Sequence) seqdd.refersTo;
      assert seq != null;

      // for the obs structure
      int[] shape = new int[]{count};

      // allocate ArrayStructureBB for outer structure
      int offset = 0;
      members = seq.makeStructureMembers();
      for (StructureMembers.Member m : members.getMembers()) {
        m.setDataParam(offset);
        //System.out.println(m.getName()+" offset="+offset);

        Variable mv = seq.findVariable(m.getName());
        if (mv == null)
          System.out.println("HEY");

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

    // loop through nested obs
    for (int i = 0; i < count; i++) {
      if (out != null) {
        out.f.format("%s read row %d (seq %s) of %s %n", out.indent(), i, seqdd.getFxyName(), out.where);
        String whereNested = (out == null) ? null : out.where + " row " + i + "( seq " + seqdd.getFxyName() + ")";
        readDataUncompressed(out.nested(whereNested), reader, bitCounterNested, seqdd.getSubKeys(), i, abb);
        
      } else {
        readDataUncompressed( null, reader, bitCounterNested, seqdd.getSubKeys(), i, abb);
      }
    }

    return countOnly ? null : new ArraySequence(members, new SequenceIterator(count, abb), count);
  }
}
