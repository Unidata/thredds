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
package ucar.nc2.iosp.bufr;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CompareNetcdf;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * IOSP for BUFR data
 *
 * @author caron
 */
public class BufrIosp extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp.class);
  static public final String obsRecord = "obs";
  static final String obsIndex = "obsRecordIndex";

  // debugging
  static private boolean debugCompress = false;
  static private boolean debugOpen = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Bufr/open");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Formatter parseInfo;
  private ConstructNC construct;
  private Message protoMessage;
  // private DateFormatter dateFormatter = new DateFormatter();

  private List<Message> msgs = new ArrayList<Message>();
  private int[] obsStart; // for each message, the starting observation index

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    long start = System.nanoTime();
    if (debugOpen) {
      parseInfo = new Formatter();
      parseInfo.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);
    }

    this.raf = raf;

    // assume theres no index for now
    MessageScanner scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      if (protoMessage == null) {
        protoMessage = m;
        protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables        
        if (!protoMessage.isTablesComplete())
          throw new IllegalStateException("BUFR file has incomplete tables");
      } else {
        if (!protoMessage.equals(m)) {
          log.warn("File " + ncfile.getLocation() + " has different BUFR message types msgno=" + count + "; skipping");
          continue; // skip
        }
      }

      msgs.add(m);
      count++;
    }

    // count where the obs start in the messages
    obsStart = new int[msgs.size()];
    int mi = 0;
    int countObs = 0;
    for (Message m : msgs) {
      obsStart[mi++] = countObs;
      countObs += m.getNumberDatasets();
    }

    if (debugOpen) {
      long took = (System.nanoTime() - start) / (1000 * 1000);
      double rate = (took > 0) ? ((double) count / took) : 0.0;
      parseInfo.format("nmsgs= %d nobs = %d took %d msecs rate = %f msgs/msec\n", count, scan.getTotalObs(), took, rate);
    }

    // this fills the netcdf object
    construct = new ConstructNC(protoMessage, countObs, ncfile);

    ncfile.finish();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  private class MsgFinder {
    int msgIndex = 0;

    Message find(int index) {
      while (msgIndex < msgs.size()) {  // LOOK use binary search
        Message m = msgs.get(msgIndex);
        if ((obsStart[msgIndex] <= index) && (index < obsStart[msgIndex] + m.getNumberDatasets()))
          return m;
        msgIndex++;
      }
      return null;
    }

    // the offset in the message for this observation
    int obsOffsetInMessage(int index) {
      return index - obsStart[msgIndex];
    }
  }


  /* public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Structure s = construct.recordStructure; // LOOK always read all members (!)
    Range want = section.getRange(0);
    int n = want.length();
    boolean addTime = (s.findVariable(ConstructNC.TIME_NAME) != null);

    ArrayStructure result;
    ArrayStructureBB abb = null;
    ArrayStructureMA ama = null;
    if (protoMessage.dds.isCompressed()) {
      ama = ArrayStructureMA.factoryMA(s, new int[]{n});
      MessageCompressedDataReader.setIterators(ama);
      result = ama;
    } else {
      StructureMembers members = s.makeStructureMembers();
      ArrayStructureBB.setOffsets(members);
      abb = new ArrayStructureBB(members, new int[]{n});
      ByteBuffer bb = abb.getByteBuffer();
      bb.order(ByteOrder.BIG_ENDIAN);
      result = abb;
    }

    int count = 0;
    int begin = 0;
    for (Message m : msgs) {
      Range have = new Range(begin, begin + m.getNumberDatasets() - 1);
      int start = begin;
      begin += m.getNumberDatasets();
      if (have.past(want)) break;
      if (!want.intersects(have)) continue;

      // need some of this one
      Range use = want.intersect(have);
      use = use.shiftOrigin(start);

      DataDescriptor.transferInfo(protoMessage.getRootDataDescriptor().getSubKeys(), m.getRootDataDescriptor().getSubKeys());
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        reader.readData(ama, m, raf, use, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        count += reader.readData(abb, m, raf, use, addTime, null);
      }
    }

    if (addTime) addTime(result);

    return result;
  }  */

  private int nelems = -1;
   public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
      Structure s = construct.recordStructure;
      return new ArraySequence(s.makeStructureMembers(), new SeqIter(), nelems);
    }

  private void addTime(ArrayStructure as) throws IOException {
    int n = (int) as.getSize();
    Array timeData = Array.factory(double.class, new int[]{n});
    IndexIterator ii = timeData.getIndexIterator();
    StructureDataIterator iter = as.getStructureDataIterator();
    while (iter.hasNext())
      ii.setDoubleNext(construct.makeObsTimeValue(iter.next()));
    StructureMembers.Member m = as.findMember(ConstructNC.TIME_NAME);
    m.setDataArray(timeData);
  }

  /**
   * Get the structure iterator
   *
   * @param s          the Structure
   * @param bufferSize the buffersize
   * @return the data iterator
   * @throws java.io.IOException if problem reading data
   */
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return new SeqIter();
  }

  private class SeqIter implements StructureDataIterator {
    StructureDataIterator currIter;
    Iterator<Message> messIter;
    int recnum = 0;
    int bufferSize = -1;
    boolean addTime;

    SeqIter() {
      addTime = (construct.recordStructure.findVariable(ConstructNC.TIME_NAME) != null);
      reset();
    }

    @Override
    public StructureDataIterator reset() {
      recnum = 0;
      messIter = msgs.iterator();
      currIter = null;
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (currIter == null) {
        currIter = readNextMessage();
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      if (!currIter.hasNext()) {
        currIter = readNextMessage();
        return hasNext();
      }

      return true;
    }

    @Override
    public StructureData next() throws IOException {
      recnum++;
      return currIter.next();
    }

    private StructureDataIterator readNextMessage() throws IOException {
      if (!messIter.hasNext()) return null;
      Message m = messIter.next();
      ArrayStructure as;
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        as = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      }

      if (addTime) addTime(as);
      return as.getStructureDataIterator();
    }

    @Override
    public void setBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
    }

    @Override
    public int getCurrentRecno() {
      return recnum - 1;
    }
  }


  /* public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {

Structure s = (Structure) v2;
if (v2.getName().equals(obsIndex)) {  // LOOK
  return readIndex(s, section);
}

// allocate ArrayStructureBB for outer structure
StructureMembers members = s.makeStructureMembers();
ArrayStructureBB.setOffsets(members);

ArrayStructureBB abb = new ArrayStructureBB(members, section.getShape());
ByteBuffer bb = abb.getByteBuffer();
bb.order(ByteOrder.BIG_ENDIAN);
if (debugCompress) System.out.printf("bb.capacity= %d %n", bb.capacity());

// long total_offset = offset * section.computeSize();
//System.out.println("offset=" + offset + " nelems= " + section.computeSize());
//System.out.println("total offset=" + total_offset + " bb_size= " + bb.capacity());
// assert offset == bb.capacity() : "total offset="+offset+ " bb_size= "+bb.capacity();

MsgFinder msgf = new MsgFinder();
Message current = null;
List<MemberDD> m2dd = null;

// loop through the obs dimension index
Range range = section.getRange(0);
for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
  Message msg = msgf.find(obsIndex);
  if (msg == null) {
    log.error("MsgFinder failed on index " + obsIndex);
    throw new IllegalStateException("MsgFinder failed on index " + obsIndex);

  } else if (msg != current) {
    // transfer info from proto message
    DataDescriptor.transferInfo(protoMessage.getRootDataDescriptor().getSubKeys(), msg.getRootDataDescriptor().getSubKeys());
    // create message to member mapping
    m2dd = associateMessage2Members( msg.getRootDataDescriptor().getSubKeys(), members); // connect this message to the desired StructureMembers
    current = msg;
  }

  //System.out.println("read obs"+obsIndex+" in msg "+msgf.msgIndex);
  boolean addTime = construct.hasTime && (s.findVariable(ConstructNC.TIME_NAME) != null);
  readOneObs(msg, msgf.obsOffsetInMessage(obsIndex), abb, m2dd, addTime);
}

return abb;
}   */

  /* sequentially scan through messages to find the correct observation


  List<MemberDD> associateMessage2Members(List<DataDescriptor> dkeys, StructureMembers members) throws IOException {
    List<MemberDD> result = new ArrayList<MemberDD>(members.getMembers().size());

    int bitOffset = 0;
    for (DataDescriptor dkey : dkeys) {
      StructureMembers.Member m = members.findMember(dkey.name);
      if (m != null) {
        MemberDD mdd = new MemberDD(m, dkey, bitOffset);
        result.add(mdd);

        if (dkey.getSubKeys() != null) {
          mdd.nested = associateMessage2Members(dkey.getSubKeys(), m.getStructureMembers());
        }
      }

      bitOffset += dkey.getBitWidth();
    }


    return result;
  }

  // associate a structure member to the corresponding DataDescriptor in a particular message
  // cannot be precalculated with proto, since a particular message can vary compress/not and bitWidth
  private class MemberDD {
    StructureMembers.Member m;
    DataDescriptor dd;
    int bitOffset; // from start of structure
    List<MemberDD> nested;

    MemberDD(StructureMembers.Member m, DataDescriptor dd, int bitOffset) {
      this.m = m;
      this.dd = dd;
      this.bitOffset = bitOffset;
    }
  }

  // want the data from the msgOffset-th data subset in the message.
  private void readOneObs(Message m, int obsOffsetInMessage, ArrayStructureBB abb, List<MemberDD> m2dd, boolean addTime) throws IOException {
    if (addTime) {
      abb.getByteBuffer().putInt(0); // placeholder
    }

    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    if (m.dds.isCompressed()) {
      BitCounterCompressed[] bitCounter = m.getBitCounterCompressed();
      readDataCompressed(reader, bitCounter, obsOffsetInMessage, m.getRootDataDescriptor(), abb);
    } else {
      BitCounterUncompressed bitCounter = m.getBitCounterUncompressed(obsOffsetInMessage);
      readDataUncompressed(reader, bitCounter, 0, m2dd, abb);
    }

    if (addTime) {
      double val = construct.makeObsTimeValue(abb);
      abb.getByteBuffer().putInt(0, (int) val); // first field in the bb // LOOK maybe double ??
    }
  } */

  /*
   * Read uncompressed data for one observation
   *
   * @param reader     reads bits from teh raf
   * @param bitCounter tracks where observations live inside the message for each row
   * @param row        whicg row is this
   * @param m2dd       maps Member to DD
   * @param abb        the ArrayStructureBB
   * @throws IOException on read error
   *
  private void readDataUncompressed(BitReader reader, BitCounterUncompressed bitCounter, int row, List<MemberDD> m2dd, ArrayStructureBB abb) throws IOException {
    ByteBuffer bb = abb.getByteBuffer();

    // transfer the bits to the ByteBuffer, aligning on byte boundaries
    for (MemberDD mdd : m2dd) {
      DataDescriptor dkey = mdd.dd;

      // position raf to this field
      reader.setBitOffset(bitCounter.getStartBit(row) + mdd.bitOffset);

      // sequence
      if (dkey.replication == 0) {
        BitCounterUncompressed[] bitCounterNestedArray = bitCounter.getNested(dkey);
        if (bitCounterNestedArray == null)
          throw new IllegalStateException("No nested BitCounterUncompressed for " + dkey.name);
        if (row >= bitCounterNestedArray.length)
          throw new IllegalStateException("No BitCounterUncompressed for " + dkey.name + " row= " + row);
        BitCounterUncompressed bcn = bitCounterNestedArray[row];

        // make an ArraySequence for this observation
        ArraySequence seq = makeArraySequenceUncompressed(reader, bcn, mdd);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      // structure
      if (dkey.type == 3) {
        BitCounterUncompressed[] bitCounterNested = bitCounter.getNested(dkey);
        if (bitCounterNested == null)
          throw new IllegalStateException("No nested BitCounterUncompressed for " + dkey.name);
        for (int i = 0; i < dkey.replication; i++)
          readDataUncompressed(reader, bitCounterNested[row], i, mdd.nested, abb);
        continue;
      }

      // regular fields

      // char data
      if (dkey.type == 1) {
        for (int i = 0; i < dkey.getByteWidthCDM(); i++) {
          bb.put((byte) reader.bits2UInt(8));
        }
        continue;
      }

      // extract from BUFR file
      int result = reader.bits2UInt(dkey.bitWidth);
      //System.out.println(dkey.name+" result = " + result );

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

    // int used = (int) (raf.getFilePointer() - obs.getOffset());
    //System.out.println("bb pos="+bb.position()+" fileBytesUsed = "+used);
  }

  // read in the data into an ArrayStructureBB, wrapped by an ArraySequence
  private ArraySequence makeArraySequenceUncompressed(BitReader reader, BitCounterUncompressed bitCounterNested, MemberDD mdd) throws IOException {
    DataDescriptor seqdd = mdd.dd;
    Sequence seq = (Sequence) seqdd.refersTo;
    assert seq != null;

    // for the obs structure
    int count = bitCounterNested.getNumberRows();
    int[] shape = new int[]{count};

    // allocate ArrayStructureBB for outer structure
    int offset = 0;
    StructureMembers members = seq.makeStructureMembers();
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

    ArrayStructureBB abb = new ArrayStructureBB(members, shape);
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    // loop through desired obs
    for (int i = 0; i < count; i++)
      readDataUncompressed(reader, bitCounterNested, i, mdd.nested, abb);

    return new ArraySequence(members, abb.getStructureDataIterator(), count);
  }

  //////////////////////////////////////////////////////////////
  // compressed data

  /* Example msg has 60 obs in it. each obs has struct(18):
    struct {
      i1;
      i2;
      ...
      struct {
        f1;
        f2;
      } inner(18);
      ...
    } outer(60);

    layout is comp(i1,60), comp(i2,60), ... comp(inner,60), ...
    where comp(inner,60) = [comp(f1,60), comp(f2(60)] * 18
    where comp = compressed structure = (minValue, dataWidth, n * dataWidth)

    using BB layout is =
      [i1, i2, ..., [f1, f2] * 18, ...] * 60

   */

  /*
  sequences (delayed replication)
  From FM94REG-11-2007.pdf:

  "The binary data in compressed form may be described as follows:
     Ro1, NBINC1, I11, I12, . . . I1n
     Ro2, NBINC2, I21, I22, . . . I2n
     ...
     Ros, NBINCs, Is1, Is2, . . . Isn

  where Ro1, Ro2, . . . Ros are local reference values for the set of values for each data element (number of bits as Table B).
  NBINC1 . . . NBINCs contain, as 6-bit quantities, the number of bits occupied by the increments (I11 . . . I1n) . . . (Is1 . . . Isn).
  s is the number of data elements per data subset and n is the number of data subsets per BUFR message."

  What happens if we combine compression with replication?

  Let C be the entire compressed block for one dataset, as above. Then regular replication just repeats C for each dataset in the replication:

    C1, C2,... Cm

  One might guess that delayed replication would look just like regular replication preceded by the replication count, that is:

    m, C1, C2,... Cm

  where m is the replication count using the number of bits specified in the replication descriptor element.
  However, for some BUFR records, I am seeing an extra 6 bits, that is:

    m, x, C1, C2,... Cm

  Where x appears to be 6 zero bits.


  // read data for a particular obs

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
        // System.out.printf("compressed replication count = %d %n", count);
        int extra = reader.bits2UInt(6);
        // System.out.printf("EXTRA bits %d at %d %n", extra, bitOffset);
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

    return new ArraySequence(members, abb.getStructureDataIterator(), count);
  }

  ////////////////////////////////////////////////////////////////////

  private byte[] readDpi(Message m, int obsOffsetInMessage) throws IOException {
    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    if (m.dds.isCompressed()) {
      BitCounterCompressed[] bitCounter = m.getBitCounterCompressed();
      return readDpi(reader, bitCounter[1], obsOffsetInMessage, null);
    }
    return null;
  }

  private byte[] readDpi(BitReader reader, BitCounterCompressed counter, int msgOffset, DataDescriptor dkey) throws IOException {
    byte[] result = new byte[dkey.replication];

    // skip to where this variable starts
    reader.setBitOffset(counter.getStartingBitPos());
    // skip to where this observation starts in the variable data
    reader.setBitOffset(counter.getBitPos(msgOffset));
    // read the bits
    for (int i = 0; i < dkey.replication; i++) {
      int cv = reader.bits2UInt(1);
      result[i] = (byte) cv;
    }
    return result;
  }


  //////////////////////////

  private Array readIndex(Structure s, Section section) {
    int[] shape = section.getShape();
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureMA ama = new ArrayStructureMA(members, shape);
    ArrayLong.D1 timeArray = new ArrayLong.D1(shape[0]);
    ArrayObject.D1 nameArray = new ArrayObject.D1(String.class, shape[0]);

    for (StructureMembers.Member m : members.getMembers()) {
      if (m.getName().equals("time"))
        m.setDataArray(timeArray);
      else
        m.setDataArray(nameArray);
    }

    /* loop through desired obs
    int count = 0;
    List<Index.BufrObs> obsList = index.getObservations();
    Range range = section.getRange(0);
    for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
      Index.BufrObs obs = obsList.get(obsIndex);
      timeArray.set(count, getTime(obs));
      nameArray.set(count, obs.getName());
      count++;
    }

    return ama;
  }

  /* private long getTime(Index.BufrObs obs) {
    if (obs.time == 0)
      try {
        Date date = dateFormatter.isoDateTimeFormat(obs.getIsoDate());
        obs.time = date.getTime();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    return obs.time;
  }

  private Structure find(List<Variable> vars) {
    Structure s;
    for (Variable v : vars) {
      if (v instanceof Sequence)
        return (Structure) v;
      if (v instanceof Structure) {
        s = find(((Structure) v).getVariables());
        if (s != null) return s;
      }
    }
    return null;
  }  */


  public String getDetailInfo() {
    Formatter ff = new Formatter();
    try {
      protoMessage.dump(ff);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (parseInfo != null)
      ff.format("%s", parseInfo.toString());
    return ff.toString();
  }

  public String getFileTypeId() {
    return "BUFR";
  }

  public String getFileTypeDescription() {
    return "WMO Binary Universal Form";
  }

  public void readAll(boolean dump) throws IOException, InvalidRangeException {
    Formatter f = new Formatter(System.out);
    for (Message m : msgs) {
      Array data;
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        data = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        data = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      }
      if (dump) NCdumpW.printArray(data, "test", new PrintWriter(System.out), null);
    }
  }

  public void compare(Structure obs) throws IOException, InvalidRangeException {
    int start = 0;
    for (Message m : msgs) {
      Array data1;
      if (!m.isTablesComplete()) continue;
      if (!m.dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        data1 = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        data1 = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      }

      int n = m.getNumberDatasets();
      m.calcTotalBits(null);
      Array data2 = obs.read(new Section().appendRange(start, start + n - 1));
      CompareNetcdf.compareData(data1, data2);

      start += n;
    }
  }

  public static void doon(String filename) throws IOException, InvalidRangeException {
    System.out.printf("BufrIosp compare = %s%n", filename);
    NetcdfFile ncfile = NetcdfFile.open(filename);
    BufrIosp iosp = (BufrIosp) ncfile.getIosp();
    iosp.readAll(false);
    // iosp.compare(iosp.construct.recordStructure);
  }

  public static void main(String arg[]) throws IOException, InvalidRangeException {
    doon("D:/formats/bufr/tmp/IUST56.bufr");
    doon("D:/formats/bufr/tmp/dispatch/KWBC-IUST56.bufr");
  }

} // end BufrIosp
