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

import ucar.ma2.ArrayStructureBB;
import ucar.ma2.ArraySequence;
import ucar.ma2.StructureMembers;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Formatter;
import java.util.List;

/**
 * Reads through the data of a message.
 * Can count bits / transfer all or some data to an Array.
 *
 * @author caron
 * @since Nov 15, 2009
 */

/*
    Within one message there are n obs (datasets) and s fields in each dataset.
    For compressed datasets, storage order is data(fld, obs) (obs varying fastest) :

      Ro1, NBINC1, I11, I12, . . . I1n
      Ro2, NBINC2, I21, I22, . . . I2n
      ……………………………...
      ……………………………...
      ……………………………...
      Ros, NBINCs, Is1, Is2, . . . Isn

    where Ro1, Ro2, . . . Ros are local reference values (number of bits as Table B) for field i.
    NBINC1 . . . NBINCs contain, as 6-bit quantities, the number of bits occupied by the increments that follow.
     If NBINC1 = 0, all values of element I are equal to Ro1; in such cases, the increments shall be omitted.
     For character data, NBINC shall contain the number of octets occupied by the character element.
     However, if the character data in all subsets are identical NBINC=0.
    Iij is the increment for the ith field and the jth obs.

   A replicated field (structure) takes a group of fields and replicates them.
   Let C be the entire compressed block for the ith field, as above.

        Ci =  Roi, NBINCi, Ii1, Ii2, . . . Iin

   data:
        
        C1, (C2, C3)*r, ... Cs

   where r is set in the data descriptor, and is the same for all datasets.

   A delayed replicated field (sequence) takes a group of fields and replicates them, and adds the number of replications
   in the data :

         C1, dr, 6bits, (C2, C3)*dr, ... Cs

   where the width (nbits) of dr is set in the data descriptor. This dr must be the same for each dataset in the message.
   For some reason there is an extra 6 bits after the dr. My guess its a programming mistake that is now needed.
   There is no description of this case in the spec or the guide.
   */

public class MessageCompressedDataReader {
  boolean showData = true;


  void read(Message m) {

  }

   // count the bits in a compressed message
  private int countBitsCompressed(Message m, BitReader reader, Formatter out) throws IOException {
    DataDescriptor root = m.getRootDataDescriptor();
    BitCounterCompressed[] counterFlds = new BitCounterCompressed[root.subKeys.size()]; // one for each field
    countBitsCompressed(out, reader, counterFlds, 0, m.getNumberDatasets(), root);

    int msg_nbits = 0;
    for (BitCounterCompressed counter : counterFlds)
      if (counter != null) msg_nbits += counter.getTotalBits();
    return msg_nbits;
  }

  private int countBitsCompressed(Formatter out, BitReader reader, BitCounterCompressed[] counters, int bitOffset, int n, DataDescriptor parent) throws IOException {

    List<DataDescriptor> flds = parent.getSubKeys();
    for (int fldidx = 0; fldidx < flds.size(); fldidx++) {
      DataDescriptor dkey = flds.get(fldidx);
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      BitCounterCompressed counter = new BitCounterCompressed(dkey, n, bitOffset);
      counters[fldidx] = counter;

      if (dkey.replication == 0) {
       reader.setBitOffset(bitOffset);
       int count = reader.bits2UInt(dkey.replicationCountSize);
       bitOffset += dkey.replicationCountSize;

       int extra = reader.bits2UInt(6);
       // System.out.printf("EXTRA bits %d at %d %n", extra, bitOffset);
       if (null != out)
          out.format("--sequence %s bitOffset=%d replication=%s %n", dkey.getFxyName(), bitOffset, count);
        bitOffset += 6; // LOOK seems to be an extra 6 bits.

        counter.addNestedCounters(count);
        for (int i = 0; i < count; i++) {
          if (null != out) out.format("%n");
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          bitOffset = countBitsCompressed(out, reader, nested, bitOffset, n, dkey); // count subfields
        }
        if (null != out) out.format("--back %s %d %n", dkey.getFxyName(), bitOffset);

        continue;
      }

      // structure
      if (dkey.type == 3) {
        if (null != out)
          out.format("--nested %s bitOffset=%d replication=%s %n", dkey.getFxyName(), bitOffset, dkey.replication);

        // p 11 of "standard", doesnt really describe the case of replication AND compression
        counter.addNestedCounters(dkey.replication);
        for (int i = 0; i < dkey.replication; i++) {
          if (null != out) out.format("%n");
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          bitOffset = countBitsCompressed(out, reader, nested, bitOffset, n, dkey); // count subfields
        }
        if (null != out) out.format("--back %s %d %n", dkey.getFxyName(), bitOffset);

        continue;
      }

      // all other fields

      reader.setBitOffset(bitOffset); // ??
      int dataMin = reader.bits2UInt(dkey.bitWidth);
      int dataWidth = reader.bits2UInt(6);  // increment data width - always in 6 bits, so max is 2^6 = 64
      if (dataWidth > dkey.bitWidth && (null != out))
        out.format(" BAD WIDTH ");
      if (dkey.type == 1) dataWidth *= 8; // char data count is in bytes
      counter.setDataWidth(dataWidth);

      int totalWidth = dkey.bitWidth + 6 + dataWidth * n; // total width in bits for this compressed set of values
      bitOffset += totalWidth; // bitOffset now points to the next field

      if (null != out)
        out.format("  read %s (%s) bitWidth=%d dataMin=%d dataWidth=%d n=%d bitOffset=%d %n",
                dkey.name, dkey.getFxyName(), dkey.bitWidth, dataMin, dataWidth, n, bitOffset);

      if (null != out) {
        if (dataWidth > 0) {

          if (dkey.type == 1) { // chars
            int nchars = dataWidth / 8;
            for (int i = 0; i < n; i++) {
              byte[] b = new byte[nchars];
              for (int j = 0; j < nchars; j++)
                b[j] = (byte) reader.bits2UInt(8);
              if (showData) out.format(" %s,", new String(b));
            }
            if (showData) out.format("%n");

          } else { // numeric

            // bpacked = (value * 10^scale - refVal)
            // value = (bpacked + refVal) / 10^scale
            double scale = Math.pow(10.0, -dkey.scale); // LOOK could precompute for efficiency
            for (int i = 0; i < n; i++) {
              int val = reader.bits2UInt(dataWidth);
              if (val == BufrNumbers.missing_value[dataWidth]) // is this a missing value ??
                if (showData) out.format(" %d (MISSING)", val);
              else {
                float fval = (dataMin + val + dkey.refVal);
                if (showData) out.format(" %d (%f)", val, scale * fval);
              }
            }

            if (showData) out.format("%n");
          }
        } else {
          // show the constant value
          double scale = Math.pow(10.0, -dkey.scale); // LOOK could precompute for efficiency
          float fval = (dataMin + dkey.refVal);
          if (showData) out.format(" all values= %d (%f) %n", dataMin, scale * fval);
        }
      }
    }

    return bitOffset;
  }


   // read data for a particular obs
  private void readDataCompressed(BitReader reader, BitCounterCompressed[] counters, int msgOffset, DataDescriptor parent, ArrayStructureBB abb) throws IOException {
    ByteBuffer bb = abb.getByteBuffer();

    for (int fldidx=0; fldidx < parent.getSubKeys().size(); fldidx++) {
      DataDescriptor dkey = parent.getSubKeys().get(fldidx);
      if (!dkey.isOkForVariable()) // misc skip
        continue;
      if ((dkey.f == 2) && (dkey.x == 24))
        System.out.printf("HEY%n");

      BitCounterCompressed counter = counters[fldidx];

      if (dkey.replication == 0) {
        // sequence : works the same way as structure
        // except that theres also the count stored first
        int bitOffset = counter.getStartingBitPos();
        reader.setBitOffset(bitOffset);
        int count = reader.bits2UInt(dkey.replicationCountSize);
        bitOffset += dkey.replicationCountSize;
        System.out.printf("compressed replication count = %d %n", count);
        int extra = reader.bits2UInt(6);
        System.out.printf("EXTRA bits %d at %d %n", extra, bitOffset);
        bitOffset += 6; // LOOK seems to be an extra 6 bits. not yet getting counted

        // make an ArraySequence for this observation
        ArraySequence seq = makeArraySequenceCompressed(reader, counter, msgOffset, dkey, count);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      // structure
      if (dkey.type == 3) {
        for (int i = 0; i < dkey.replication; i++) {
          //out.format("%nRead parent=%s obe=%d level=%d%n",dkey.name, msgOffset, i);
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          readDataCompressed(reader, nested, msgOffset, dkey, abb);
        }
        continue;
      }

      // skip to where this variable starts
      reader.setBitOffset( counter.getStartingBitPos());

      // char data special case
      if (dkey.type == 1) {
        int n = dkey.bitWidth/8;
        byte[] minValue = new byte[n];
        for (int i=0; i<n; i++)
          minValue[i] = (byte) reader.bits2UInt(8);
        int dataWidth = reader.bits2UInt(6); // incremental data width

        if (dataWidth == 0) { // use the min value
          for (int i = 0; i < n; i++)
            bb.put(minValue[i]);

        } else { // read the incremental value
          reader.setBitOffset( counter.getBitPos(msgOffset));
          int nt = Math.min(n, dataWidth);
          for (int i = 0; i < nt; i++) {
            int cval = reader.bits2UInt(8);
            if (cval < 32 || cval > 126) cval = 0; // printable ascii KLUDGE!
            bb.put((byte) cval);
          }
          for (int i = nt; i < n; i++) // can dataWidth < n ?
            bb.put((byte) 0);
        }
        continue;
      }

      // numeric fields

      int value = reader.bits2UInt(dkey.bitWidth); // read min value
      int dataWidth = reader.bits2UInt(6); // incremental data width

      // if dataWidth == 0, just use min value, otherwise read the compressed value here
      if (dataWidth > 0) {
        // skip to where this observation starts in the variable data, and read the incremental value
        reader.setBitOffset( counter.getBitPos(msgOffset));
        int cv = reader.bits2UInt(dataWidth);

        if (dataWidth > BufrNumbers.missing_value.length)
          System.out.printf("HEY%n");

        if (cv == BufrNumbers.missing_value[dataWidth]) // is this a missing value ??
          value = BufrNumbers.missing_value[dkey.bitWidth]; // set to missing value
        else // add to minimum
          value += cv;
      }

      // workaround for misformed messages
      if (dataWidth > dkey.bitWidth) {
        int missingVal = BufrNumbers.missing_value[dkey.bitWidth];
        if ((value & missingVal) != value) // overflow
          value = missingVal;     // replace with missing value
      }

      // place into byte buffer
      if (dkey.getByteWidthCDM() == 1) {
        bb.put((byte) value);
      } else if (dkey.getByteWidthCDM() == 2) {
        int b1 = value & 0xff;
        int b2 = (value & 0xff00) >> 8;
        bb.put((byte) b2);
        bb.put((byte) b1);
      } else {
        int b1 = value & 0xff;
        int b2 = (value & 0xff00) >> 8;
        int b3 = (value & 0xff0000) >> 16;
        int b4 = (value & 0xff000000) >> 24;
        bb.put((byte) b4);
        bb.put((byte) b3);
        bb.put((byte) b2);
        bb.put((byte) b1);
      }

    }
  }

    // read in the data into an ArrayStructureBB, wrapped by an ArraySequence
  private ArraySequence makeArraySequenceCompressed(BitReader reader, BitCounterCompressed bitCounterNested, int msgOffset, DataDescriptor seqdd, int count) throws IOException {
    Sequence seq = (Sequence) seqdd.refersTo;
    assert seq != null;

    // for the obs structure
    int[] shape = new int[]{count};

    // allocate ArrayStructureBB for outer structure
    int offset = 0;
    StructureMembers members = seq.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      Variable mv = seq.findVariable(m.getName());
      DataDescriptor dk = (DataDescriptor) mv.getSPobject();
      if (dk.replication == 0)
        offset += 4;
      else
        offset += dk.getByteWidthCDM();
    }

    ArrayStructureBB abb = new ArrayStructureBB(members, shape);
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    for (int i = 0; i < count; i++) {
      //out.format("%nRead parent=%s obe=%d level=%d%n",dkey.name, msgOffset, i);
      BitCounterCompressed[] nested = bitCounterNested.getNestedCounters(i);
      readDataCompressed(reader, nested, msgOffset, seqdd, abb);
    }

    return null; // new ArraySequence(members, new SequenceIterator(count, abb), count);
  }
}
