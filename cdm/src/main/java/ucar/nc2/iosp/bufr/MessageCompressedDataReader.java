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
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.Sequence;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.HashMap;

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

   A delayed replicated field (sequence) takes a group of fields and replicates them, with the number of replications
   in the data :

         C1, dr, 6bits, (C2, C3)*dr, ... Cs

   where the width (nbits) of dr is set in the data descriptor. This dr must be the same for each dataset in the message.
   For some reason there is an extra 6 bits after the dr. My guess its a programming mistake that is now needed.
   There is no description of this case in the spec or the guide.


   --------------------------

   We use an ArrayStructureMA to hold the data, and fill it sequentially as we scan the message.
   Each field is held in an Array stored in the member.getDataArray().
   An iterator is stored in member.getDataObject() which keeps track of where we are.
   For fixed length nested Structures, we need fld(dataset, inner) but we have fld(inner, dataset) se we transpose the dimensions
     before we set the iterator.
   For Sequences, inner.length is the same for all datasets in the message. However, it may vary across messages. However, we
     only iterate over the inner sequence, never across all messages. So the implementation can be specific to the meassage.

   */

public class MessageCompressedDataReader {
  boolean showData = true;

  public Array readData(Structure s, Message proto, Message m, RandomAccessFile raf, Formatter f) throws IOException, InvalidRangeException {
    // transfer info from proto message
    DataDescriptor.transferInfo(proto.getRootDataDescriptor().getSubKeys(), m.getRootDataDescriptor().getSubKeys());

    // allocate ArrayStructureBB for outer structure
    ArrayStructureMA ama = ArrayStructureMA.factoryMA(s, s.getShape());
    setIterators(ama);

    HashMap<DataDescriptor, StructureMembers.Member> map = new HashMap<DataDescriptor, StructureMembers.Member>(100);
    associateMessage2Members(ama.getStructureMembers(), m.getRootDataDescriptor(), map);

    readDataCompressed(m, raf, f, map);

    return ama;
  }

  private void setIterators(ArrayStructureMA ama) {
    StructureMembers sms = ama.getStructureMembers();
    for (StructureMembers.Member sm : sms.getMembers()) {
      //System.out.printf("doin %s%n", sm.getName());
      Array data = sm.getDataArray();
      if (data instanceof ArrayStructureMA) {
        setIterators( (ArrayStructureMA) data);

      } else  if (data instanceof ArraySequenceNested) {
          System.out.println("HEY");
        
      } else {
        int[] shape = data.getShape();
        if ((shape.length > 1) && (sm.getDataType() != DataType.CHAR)) {
          Array datap = data.transpose(0, 1);
          sm.setDataObject(datap.getIndexIterator());
        } else {
          sm.setDataObject(data.getIndexIterator());
        }
      }
    }
  }

  void associateMessage2Members(StructureMembers members, DataDescriptor parent, HashMap<DataDescriptor, StructureMembers.Member> map) throws IOException {
    for (DataDescriptor dkey : parent.getSubKeys()) {
      if (dkey.name == null) {
        //System.out.printf("ass skip %s%n", dkey);
        if (dkey.getSubKeys() != null)
          associateMessage2Members(members, dkey, map);        
        continue;
      }
      //System.out.printf("ass %s%n", dkey.name);
      StructureMembers.Member m = members.findMember(dkey.name);
      if (m != null) {
        map.put(dkey, m);

        if (m.getDataType() == DataType.STRUCTURE) {
          ArrayStructure nested = (ArrayStructure) m.getDataArray();
          associateMessage2Members(nested.getStructureMembers(), dkey, map);
        }

        else if (m.getDataType() == DataType.SEQUENCE) {
          ArraySequenceNested nested = (ArraySequenceNested) m.getDataArray();
          associateMessage2Members(nested.getStructureMembers(), dkey, map);
        }

      } else {
        System.out.printf("Cant find %s%n", dkey);
      }
    }
  }

  // read / count the bits in a compressed message
  public int readDataCompressed(Message m, RandomAccessFile raf, Formatter f,
                                HashMap<DataDescriptor, StructureMembers.Member> map) throws IOException {
    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    DataDescriptor root = m.getRootDataDescriptor();
    if (root.isBad) return 0;

    DebugOut out = (f == null) ? null : new DebugOut(f, "top ", 0, 1);
    BitCounterCompressed[] counterFlds = new BitCounterCompressed[root.subKeys.size()]; // one for each field
    readDataCompressed(out, reader, counterFlds, root, 0, m.getNumberDatasets(), map);

    m.msg_nbits = 0;
    for (BitCounterCompressed counter : counterFlds)
      if (counter != null) m.msg_nbits += counter.getTotalBits();
    return m.msg_nbits;
  }


  private int readDataCompressed(DebugOut out, BitReader reader, BitCounterCompressed[] counters, DataDescriptor parent, int bitOffset,
                                  int ndatasets, HashMap<DataDescriptor, StructureMembers.Member> map) throws IOException {

    List<DataDescriptor> flds = parent.getSubKeys();
    for (int fldidx = 0; fldidx < flds.size(); fldidx++) {
      DataDescriptor dkey = flds.get(fldidx);
      if (!dkey.isOkForVariable()) {// misc skip
        System.out.printf("HEY skipping %s %n", dkey);
        continue;
      }
      if ((dkey.f == 2) && (dkey.x == 24) && (dkey.y == 255))
         System.out.printf("HEY%n");

      BitCounterCompressed counter = new BitCounterCompressed(dkey, ndatasets, bitOffset);
      counters[fldidx] = counter;

      if (dkey.replication == 0) {
        reader.setBitOffset(bitOffset);
        int count = reader.bits2UInt(dkey.replicationCountSize);
        bitOffset += dkey.replicationCountSize;

        reader.bits2UInt(6);
        // System.out.printf("EXTRA bits %d at %d %n", extra, bitOffset);
        if (null != out)
          out.f.format("--sequence %s bitOffset=%d replication=%s %n", dkey.getFxyName(), bitOffset, count);
        bitOffset += 6; // LOOK seems to be an extra 6 bits.

        counter.addNestedCounters(count);

        // make an ArrayObject of ArraySequence, place it into the data array
        makeArraySequenceCompressed(out, reader, counter, 0 /*msgOffset*/, dkey, count, map);
        if (null != out) out.f.format("--back %s %d %n", dkey.getFxyName(), bitOffset);

        /* for (int i = 0; i < count; i++) {
          if (null != out) out.f.format("%n");
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          bitOffset = readDataCompressed(out, reader, nested, dkey, bitOffset, ndatasets, map); // count subfields
        }  */
        continue;
      }

      // structure
      if (dkey.type == 3) {
        if (null != out)
          out.f.format("--nested %s bitOffset=%d replication=%s %n", dkey.getFxyName(), bitOffset, dkey.replication);

        // p 11 of "standard", doesnt really describe the case of replication AND compression
        counter.addNestedCounters(dkey.replication);
        for (int i = 0; i < dkey.replication; i++) {
          if (null != out) out.f.format("%n");
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          bitOffset = readDataCompressed(out, reader, nested, dkey, bitOffset, ndatasets, map); // count subfields
        }
        if (null != out) out.f.format("--back %s %d %n", dkey.getFxyName(), bitOffset);

        continue;
      }

      // all other fields
      IndexIterator iter = null;
      if (map != null) {
        StructureMembers.Member m = map.get(dkey);
        if (m == null)
          System.out.printf("HEY %s%n", dkey);
        iter = (IndexIterator) m.getDataObject();
      }

      reader.setBitOffset(bitOffset);  // ?? needed ??

      // char data special case
      if (dkey.type == 1) {
        int nc = dkey.bitWidth / 8;
        byte[] minValue = new byte[nc];
        for (int i = 0; i < nc; i++)
          minValue[i] = (byte) reader.bits2UInt(8);
        int dataWidth = reader.bits2UInt(6); // incremental data width in bytes
        counter.setDataWidth(8*dataWidth);
        int totalWidth = dkey.bitWidth + 6 + 8*dataWidth * ndatasets; // total width in bits for this compressed set of values
        bitOffset += totalWidth; // bitOffset now points to the next field
 
        if (null != out)
          out.f.format("  read %s (%s) bitWidth=%d defValue=%s dataWidth=%d n=%d bitOffset=%d %n",
                  dkey.name, dkey.getFxyName(), dkey.bitWidth, new String(minValue, "UTF-8"), dataWidth, ndatasets, bitOffset);

        for (int dataset = 0; dataset < ndatasets; dataset++) {
          if (dataWidth == 0) { // use the min value
            if (iter != null)
              for (int i = 0; i < nc; i++)
                iter.setCharNext((char) minValue[i]); // ??

          } else { // read the incremental value
            int nt = Math.min(nc, dataWidth);
            byte[] incValue = new byte[nc];
            for (int i = 0; i < nt; i++)
              incValue[i] = (byte) reader.bits2UInt(8);
            for (int i = nt; i < nc; i++) // can dataWidth < n ?
              incValue[i] = 0;

            if (iter != null)
              for (int i = 0; i < nc; i++) {
                int cval = incValue[i];
                if (cval < 32 || cval > 126) cval = 0; // printable ascii KLUDGE!
                iter.setCharNext((char) cval); // ??
              }
            if (out != null) out.f.format(" %s,", new String(incValue, "UTF-8"));
          }
        }
        if (out != null) out.f.format("%n");
        continue;
      }

      // numeric fields

      int dataMin = reader.bits2UInt(dkey.bitWidth);
      int dataWidth = reader.bits2UInt(6);  // increment data width - always in 6 bits, so max is 2^6 = 64
      if (dataWidth > dkey.bitWidth && (null != out))
        out.f.format(" BAD WIDTH ");
      if (dkey.type == 1) dataWidth *= 8; // char data count is in bytes
      counter.setDataWidth(dataWidth);

      int totalWidth = dkey.bitWidth + 6 + dataWidth * ndatasets; // total width in bits for this compressed set of values
      bitOffset += totalWidth; // bitOffset now points to the next field

      if (null != out)
        out.f.format("  read %s (%s) bitWidth=%d dataMin=%d (%f) dataWidth=%d n=%d bitOffset=%d %n",
                dkey.name, dkey.getFxyName(), dkey.bitWidth, dataMin, dkey.convert(dataMin), dataWidth, ndatasets, bitOffset);

      // numeric fields

      // if dataWidth == 0, just use min value, otherwise read the compressed value here
      for (int i = 0; i < ndatasets; i++) {
        int value = dataMin;

        if (dataWidth > 0) {
          int cv = reader.bits2UInt(dataWidth);
          if (cv == BufrNumbers.missing_value[dataWidth]) // is this a missing value ??
            value = BufrNumbers.missing_value[dkey.bitWidth]; // set to missing value
          else // add to minimum
            value += cv;
        }

        // workaround for malformed messages
        if (dataWidth > dkey.bitWidth) {
          int missingVal = BufrNumbers.missing_value[dkey.bitWidth];
          if ((value & missingVal) != value) // overflow
            value = missingVal;     // replace with missing value
        }

        if (iter != null)
          iter.setIntNext(value);

        if ((out != null) && (dataWidth > 0)) out.f.format(" %d (%f)", value, dkey.convert(value));
      }
      if (out != null) out.f.format("%n");
    }

    return bitOffset;
  }

  // read in the data into an ArrayStructureBB, wrapped by an ArraySequence
  private void makeArraySequenceCompressed(DebugOut out, BitReader reader, BitCounterCompressed bitCounterNested, int index,
                         DataDescriptor seqdd, int count, HashMap<DataDescriptor, StructureMembers.Member> map) throws IOException {

    // all other fields
    ArrayObject arrObj = null;
    if (map != null) {
      StructureMembers.Member m = map.get(seqdd);
      if (m == null)
        System.out.printf("HEY %s%n", seqdd);
      arrObj = (ArrayObject) m.getDataArray();
    }


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

    // allocate ArrayStructureBB for outer structure
    ArrayStructureMA ama = ArrayStructureMA.factoryMA(seq, shape);
    setIterators(ama);

    for (int i = 0; i < count; i++) {
      //out.format("%nRead parent=%s obe=%d level=%d%n",dkey.name, msgOffset, i);
      BitCounterCompressed[] nested = bitCounterNested.getNestedCounters(i);
      // LOOK readDataCompressed(reader, nested, msgOffset, seqdd, ama);
    }

    ArraySequence arrSeq = new ArraySequence(members, new SequenceIterator(count, ama), count);
    arrObj.setObject(index, arrSeq);

  }
  /* read data for a particular obs
  private void readDataCompressed(BitReader reader, BitCounterCompressed[] counters, int msgOffset, DataDescriptor parent, ArrayStructureBB abb) throws IOException {
    ByteBuffer bb = abb.getByteBuffer();

    for (int fldidx = 0; fldidx < parent.getSubKeys().size(); fldidx++) {
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
      reader.setBitOffset(counter.getStartingBitPos());

      // char data special case
      if (dkey.type == 1) {
        int n = dkey.bitWidth / 8;
        byte[] minValue = new byte[n];
        for (int i = 0; i < n; i++)
          minValue[i] = (byte) reader.bits2UInt(8);
        int dataWidth = reader.bits2UInt(6); // incremental data width

        if (dataWidth == 0) { // use the min value
          for (int i = 0; i < n; i++)
            bb.put(minValue[i]);

        } else { // read the incremental value
          reader.setBitOffset(counter.getBitPos(msgOffset));
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
        reader.setBitOffset(counter.getBitPos(msgOffset));
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
  }  */


}
