/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.bufr;

import ucar.bufr.*;
import ucar.bufr.Index;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;

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

  static private boolean forceNewIndex = false; // force that a new index file is written
  static private boolean extendIndex = false; // check if index needs to be extended

  static public void setExtendIndex(boolean b) {
    extendIndex = b;
  }

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Bufr/open");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private NetcdfFile ncfile;
  private RandomAccessFile raf;
  private Formatter parseInfo;
  //private ConstructNC delegate;
  private BufrMessage protoMessage;
  private DateFormatter dateFormatter = new DateFormatter();

  private List<BufrMessage> msgs = new ArrayList<BufrMessage>();
  private int[] obsStart; // for each message, the starting observation index

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return BufrScanner.isValidFile(raf);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    long start = System.nanoTime();
    if (debugOpen) {
      parseInfo = new Formatter();
      parseInfo.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);
    }

    // assume theres no index for now
    BufrScanner scan = new BufrScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      BufrMessage m = scan.next();
      if (m == null) continue;

      if (protoMessage == null) {
        protoMessage = m;
        protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables        
        if (!protoMessage.hasTablesComplete())
          throw new IllegalStateException("BUFR file has incomplete tables");
      } else {
        if (!protoMessage.equals(m)) {
          System.out.println("File has different BUFR message types msg=" + count);
          continue; // skip
          //throw new IllegalStateException("File has different BUFR message types msg=" + count);
        }
      }

      msgs.add(m);
      count++;
    }

    // count where the obs start in the messages
    obsStart = new int[msgs.size()];
    int mi = 0;
    int countObs = 0;
    for (BufrMessage m : msgs) {
      obsStart[mi++] = countObs;
      countObs += m.getNumberDatasets();
    }

    if (debugOpen) {
      long took = (System.nanoTime() - start) / (1000 * 1000);
      double rate = (took > 0) ? ((double) count / took) : 0.0;
      parseInfo.format("nmsgs= %d nobs = %d took %d msecs rate = %f msgs/msec\n", count, scan.getTotalObs(), took, rate);
    }

    // this fills the netcdf object
    new ConstructNC(protoMessage, countObs, ncfile);

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
    Range range = section.getRange(0);
    for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
      BufrMessage msg = msgf.find(obsIndex);
      if (msg == null) {
        log.error("MsgFinder failed on index " + obsIndex);
        throw new IllegalStateException("MsgFinder failed on index " + obsIndex);
      }

      readOneObs(msg, msgf.msgOffset(obsIndex), abb, bb);
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

    BufrMessage find(int index) {
      while (msgIndex < msgs.size()) {
        BufrMessage m = msgs.get(msgIndex);
        if ((obsStart[msgIndex] <= index) && (index < obsStart[msgIndex] + m.getNumberDatasets()))
          return m;
        msgIndex++;
      }
      return null;
    }

    int msgOffset(int index) {
      return index - obsStart[msgIndex];
    }
  }

  // want the data from the msgOffset-th data subset in the message.
  // this is the non-compressed case
  private void readOneObs(BufrMessage m, int msgOffset, ArrayStructureBB abb, ByteBuffer bb) throws IOException {
    BitReader reader = new BitReader(raf, m.dataSection.dataPos + 4);

    if (m.dds.isCompressed()) {
      m.calcTotalBits(); // make sure things have been counted.
      readDataCompressed(reader, m.getRootDataDescriptor(), msgOffset, m.getCounterFlds(), bb);

    } else {
      reader.setBitOffset(m.getBitOffset(msgOffset)); // bit offset from start of data section
      readData(reader, m.getRootDataDescriptor().getSubKeys(), abb, bb);
    }
  }

  private void readData(BitReader reader, List<DataDescriptor> dkeys, ArrayStructureBB abb, ByteBuffer bb) throws IOException {

    // LOOK : assumes we want all the members of the structure
    // transfer the bits to the ByteBuffer, aligning on byte boundaries
    for (DataDescriptor dkey : dkeys) {
      // misc skip
      if (!dkey.isOkForVariable())
        continue;

      // sequence
      if (dkey.replication == 0) {
        int count = reader.bits2UInt(dkey.replicationCountSize);
        ArraySequence seq = makeArraySequence(reader, count, dkey);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      if (dkey.type == 3) {
        for (int i = 0; i < dkey.replication; i++)
          readData(reader, dkey.subKeys, abb, bb);
        continue;
      }

      //System.out.println(dkey.name+" bbpos="+bb.position());

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
  private void readDataCompressed(BitReader reader, DataDescriptor parent, int msgOffset, CounterFld[] counters, ByteBuffer bb) throws IOException {

    for (int fldidx=0; fldidx < parent.getSubKeys().size(); fldidx++) {
      DataDescriptor dkey = parent.getSubKeys().get(fldidx);
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      CounterFld counter = counters[fldidx];

      // sequence : probably works the same way as structure
      if (dkey.replication == 0) {
        continue; // skip for the moment
      }

      // structure
      if (dkey.type == 3) {
        for (int i = 0; i < dkey.replication; i++) {
          CounterFld[] nested = counter.getNestedCounters(i);
          readDataCompressed(reader, dkey, msgOffset, nested, bb);
        }
        continue;
      }

      // char data - need to find some to test with
      if (dkey.type == 1) {
        continue; // skip for the moment
      }

      // skip to where this variable starts
      reader.setBitOffset( counter.getStartingBitPos());

      int value = reader.bits2UInt(dkey.bitWidth);
      int dataWidth = reader.bits2UInt(6);

      // if dataWidth == 0, just use min value
      if (dataWidth > 0) {
        // skip to where this observation starts in the variable data, and read the incremental value
        reader.setBitOffset( counter.getBitPos(msgOffset));
        value += reader.bits2UInt(dataWidth);
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

  private long getTime(Index.BufrObs obs) {
    if (obs.time == 0)
      try {
        Date date = dateFormatter.isoDateTimeFormat(obs.getIsoDate());
        obs.time = date.getTime();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    return obs.time;
  }

  // read in the data into an ArrayStructureBB
  private ArraySequence makeArraySequence(BitReader reader, int count, DataDescriptor seqdd) throws IOException {
    // kludge
    Sequence s = (Sequence) seqdd.refersTo;
    assert s != null;

    // for the obs structure
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
      readData(reader, seqdd.getSubKeys(), abb, bb);

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

  public void close() throws IOException {
    raf.close();
  }

  public String getDetailInfo() {
    Formatter ff = new Formatter();
    try {
      new BufrDump2().dump(ff, protoMessage);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (parseInfo != null)
      ff.format("%s", parseInfo.toString());
    return ff.toString();
  }

  /**
   * main.
   */
  public static void main(String args[]) throws Exception, IOException,
      InstantiationException, IllegalAccessException {

    //String fileIn = "C:/data/dt2/point/bufr/IUA_CWAO_20060202_12.bufr";
    //String fileIn = "C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr";
    //String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    //String fileIn = "R:/testdata/bufr/edition3/idd/profiler/PROFILER_1.bufr";
    String fileIn = "D:/motherlode/bufr/cat.out";
    NetcdfDataset ncf = NetcdfDataset.openDataset(fileIn);
    System.out.println(ncf.toString());

    /* Structure s = (Structure) ncf.findVariable(obsRecord);
    StructureData sdata = s.readStructure(2);
    PrintWriter pw = new PrintWriter(System.out);
    NCdumpW.printStructureData(pw, sdata);  */
    new WriteT41_ncFlat(ncf, "D:/motherlode/bufr/cat2.nc", true);

    //Variable v = ncf.findVariable("recordIndex");
    //NCdumpW.printArray(v.read(), "recordIndex", pw, null);

    /* ucar.nc2.Variable v;

    v = ncf.findVariable("trajectory_id");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("station_id");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("firstChild");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("numChildren");
    if (v != null) {
      Array data = v.read();
      NCdump.printArray(data, v.getName(), System.out, null);
    }
    System.out.println();

    v = ncf.findVariable("record");
    //ucar.nc2.Variable v = ncf.findVariable("Latitude");
    //ucar.nc2.Variable v = ncf.findVariable("time");
    //System.out.println();
    //System.out.println( v.toString());

    if (v instanceof Structure) {
      Structure s = (Structure) v;
      StructureDataIterator iter = s.getStructureIterator();
      int count = 0;
      PrintWriter pw = new PrintWriter( System.out);
      while (iter.hasNext()) {
        System.out.println("record "+count);
        NCdumpW.printStructureData(pw, iter.next());
        count++;
      }
      Array data = v.read();
      NCdump.printArray(data, "record", System.out, null);
    } else {
      Array data = v.read();
      int[] length = data.getShape();
      System.out.println();
      System.out.println("v2 length =" + length[0]);

      IndexIterator ii = data.getIndexIterator();
      for (; ii.hasNext();) {
        System.out.println(ii.getFloatNext());
      }
    }
    ncf.close();  */
  }
} // end BufrIosp
