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

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * IOSP for BUFR data
 *
 * @author rkambic
 * @author caron
 */
public class BufrIosp extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp.class);
  static final String obsRecord = "obsRecord";
  static final String obsIndex = "obsRecordIndex";

  // debugging
  static private boolean debugSize = false;
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
          log.warn("File "+ncfile.getLocation()+" has different BUFR message types msgno=" + count+"; skipping");
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
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {

    Structure s = (Structure) v2;
    if (v2.getName().equals(obsIndex)) {  // LOOK
      return readIndex(s, section);
    }

    // allocate ArrayStructureBB for outer structure
    StructureMembers members = s.makeStructureMembers();
    setOffsets(members);

    ArrayStructureBB abb = new ArrayStructureBB(members, section.getShape());
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    // long total_offset = offset * section.computeSize();
    //System.out.println("offset=" + offset + " nelems= " + section.computeSize());
    //System.out.println("total offset=" + total_offset + " bb_size= " + bb.capacity());
    // assert offset == bb.capacity() : "total offset="+offset+ " bb_size= "+bb.capacity();

    // loop through the obs dimension index
    MsgFinder msgf = new MsgFinder();
    Message current = null;
    List<MemberDD> m2dd = null;

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
      readOneObs(msg, msgf.obsOffsetInMessage(obsIndex), abb, bb, m2dd);
    }

    return abb;
  }

  private int setOffsets(StructureMembers members) {
    int offset = 0;
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      offset += m.getSizeBytes();

      // set inner offsets
      if (m.getDataType() == DataType.STRUCTURE) {
        setOffsets(m.getStructureMembers());
      }
    }
    return offset;
  }

  // sequentially scan through messages to find the correct observation
  private class MsgFinder {
    int msgIndex = 0;

    Message find(int index) {
      while (msgIndex < msgs.size()) {
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

  List<MemberDD> associateMessage2Members(List<DataDescriptor> dkeys, StructureMembers members) throws IOException {
    List<MemberDD> result = new ArrayList<MemberDD>( members.getMembers().size());

    int bitOffset = 0;
    for (DataDescriptor dkey : dkeys) {
      StructureMembers.Member m = members.findMember(dkey.name);
      if (m != null) {
        MemberDD mdd = new MemberDD(m, dkey, bitOffset);
        result.add(mdd);

        if (dkey.getSubKeys() != null) 
          mdd.nested = associateMessage2Members(dkey.getSubKeys(), m.getStructureMembers());
      }

      bitOffset += dkey.getBitWidth();
    }

    /* for (StructureMembers.Member m : members.getMembers()) {
      if () {
        log.error("associateMessage2Members cant find "+m.getName());
      }
    } */

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
  private void readOneObs(Message m, int obsOffsetInMessage, ArrayStructureBB abb, ByteBuffer bb, List<MemberDD> m2dd) throws IOException {
    if (construct.hasTime) {
      bb.putInt(0); // placeholder
    }

    BitReader reader = new BitReader(raf, m.dataSection.getDataPos() + 4);
    if (m.dds.isCompressed()) {
      BitCounterCompressed[] bitCounter = m.getCounterFlds();      
      readDataCompressed(reader, m.getRootDataDescriptor(), obsOffsetInMessage, bitCounter, bb);
    } else {
      BitCounterUncompressed bitCounter = m.getBitCounterUncompressed(obsOffsetInMessage);
      readDataUncompressed(reader, bitCounter, 0, m2dd, abb, bb);
    }

    if (construct.hasTime) {
      double val = construct.makeObsTimeValue(abb); 
      bb.putInt(0, (int) val); // first field in the bb
    }
  }

  private void readDataUncompressed(BitReader reader, BitCounterUncompressed bitCounter, int row, List<MemberDD> m2dd, ArrayStructureBB abb, ByteBuffer bb) throws IOException {

    // transfer the bits to the ByteBuffer, aligning on byte boundaries
    for (MemberDD mdd : m2dd) {
      DataDescriptor dkey = mdd.dd;

      // position raf to this field
      reader.setBitOffset( bitCounter.getStartBit(row) + mdd.bitOffset);

      // sequence
      if (dkey.replication == 0) {
        BitCounterUncompressed[] bitCounterNestedArray = bitCounter.getNested(dkey);
        if (bitCounterNestedArray == null)
          throw new IllegalStateException("No nested BitCounterUncompressed for "+dkey.name);
        if (row >= bitCounterNestedArray.length)
          throw new IllegalStateException("No BitCounterUncompressed for "+dkey.name+" row= "+row);
        BitCounterUncompressed bcn = bitCounterNestedArray[row];

        // make an ArraySequence for this observation
        ArraySequence seq = makeArraySequence(reader, bcn, mdd);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      // structure
      if (dkey.type == 3) {
        BitCounterUncompressed[] bitCounterNested = bitCounter.getNested(dkey);
        if (bitCounterNested == null)
          throw new IllegalStateException("No nested BitCounterUncompressed for "+dkey.name);
        for (int i = 0; i < dkey.replication; i++)
          readDataUncompressed(reader, bitCounterNested[row], i, mdd.nested, abb, bb);
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
  // read data for a particular obs
  private void readDataCompressed(BitReader reader, DataDescriptor parent, int msgOffset, BitCounterCompressed[] counters, ByteBuffer bb) throws IOException {
    //Formatter out = new Formatter(System.out);

    for (int fldidx=0; fldidx < parent.getSubKeys().size(); fldidx++) {
      DataDescriptor dkey = parent.getSubKeys().get(fldidx);
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      BitCounterCompressed counter = counters[fldidx];

      // sequence : probably works the same way as structure
      if (dkey.replication == 0) {
        continue; // skip for the moment
      }

      // structure
      if (dkey.type == 3) {
        for (int i = 0; i < dkey.replication; i++) {
          //out.format("%nRead parent=%s obe=%d level=%d%n",dkey.name, msgOffset, i);
          BitCounterCompressed[] nested = counter.getNestedCounters(i);
          readDataCompressed(reader, dkey, msgOffset, nested, bb);
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
      int dataWidth = reader.bits2UInt(6); // incremental data woidth

      // if dataWidth == 0, just use min value
      if (dataWidth > 0) {
        // skip to where this observation starts in the variable data, and read the incremental value
        reader.setBitOffset( counter.getBitPos(msgOffset));
        value += reader.bits2UInt(dataWidth);
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



  ////////////////////////////////////////////////////////////////////

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
    }  */

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
  } */

  // read in the data into an ArrayStructureBB
  private ArraySequence makeArraySequence(BitReader reader, BitCounterUncompressed bitCounterNested, MemberDD mdd) throws IOException {
    DataDescriptor seqdd = mdd.dd;
    Sequence s = (Sequence) seqdd.refersTo;
    assert s != null;

    // for the obs structure
    int count = bitCounterNested.getNumberRows();
    int[] shape = new int[]{count};

    // allocate ArrayStructureBB for outer structure
    int offset = 0;
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      //System.out.println(m.getName()+" offset="+offset);

      Variable mv = s.findVariable(m.getName());
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
      readDataUncompressed(reader, bitCounterNested, i, mdd.nested, abb, bb);

    return new ArraySequence(members, new SequenceIterator(count, abb), count);
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
  }

  private class SequenceIterator implements StructureDataIterator {
    int count;
    ArrayStructure abb;
    StructureDataIterator siter;

    SequenceIterator(int count, ArrayStructure abb) {
      this.count = count;
      this.abb = abb;
    }

    public boolean hasNext() throws IOException {
      if (siter == null)
        siter = abb.getStructureDataIterator();
      return siter.hasNext();
    }

    public StructureData next() throws IOException {
      return siter.next();
    }

    public void setBufferSize(int bytes) {
      siter.setBufferSize(bytes);
    }

    public StructureDataIterator reset() {
      siter = null;
      return this;
    }
  }

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


} // end BufrIosp
