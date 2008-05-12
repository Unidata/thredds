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
  static private boolean debugSize = true;
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
  private Formatter parseInfo = new Formatter();
  private ConstructNC delegate;
  private BufrMessage protoMessage;
  private List<BufrMessage> msgs = new ArrayList<BufrMessage>();
  private List<BufrDataset> datasets = new ArrayList<BufrDataset>();
  private DateFormatter dateFormatter = new DateFormatter();

  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return BufrScanner.isValidFile(raf);
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    long start = System.nanoTime();
    if (debugOpen) parseInfo.format("\nOpen %s size = %d Kb \n", raf.getLocation(), raf.length() / 1000);

    BufrScanner scan = new BufrScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      BufrMessage m = scan.next();
      if (protoMessage == null) {
        protoMessage = m;
        m.getRoot();
      }
      readOneMessage(m, protoMessage.dds.getDescriptorRoot());
      msgs.add(m);
      count++;
    }

    if (debugOpen) {
      long took = (System.nanoTime() - start);
      double rate = (took > 0) ? ((double) (1000 * 1000) * count / took) : 0.0;
      parseInfo.format("nmsgs= %d nobs = %d took %d msecs rate = %f msgs/msec\n", count, scan.getTotalObs(), took / (1000 * 1000), rate);
    }

    delegate = new ConstructNC(protoMessage, scan.getTotalObs(), ncfile);

    if (debugSize) {
      Formatter out = new Formatter(System.out);
      int nbits = protoMessage.dds.getTotalBits();
      int ndatasets = protoMessage.dds.getNumberDataSets();
      int total_nbits = nbits * ndatasets;
      int total_nbytes = (total_nbits % 8 == 0) ? total_nbits / 8 : total_nbits / 8 + 1;
      out.format(" nbits= %d ndatasets= %s totalBits=%d totalBytes=%d dataLen=%d\n", nbits, ndatasets, total_nbits, total_nbytes, protoMessage.dataSection.dataLength);
      out.flush();
      assert (total_nbytes + 4) == protoMessage.dataSection.dataLength;
    }

    ncfile.finish();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {

    // LOOK
    //if (!(v2 instanceof Structure)) {
    //  return readDataVariable(v2.getName(), v2.getDataType(), section.getRanges());
    //}
    Structure s = (Structure) v2;

    if (v2.getName().equals(obsIndex)) {
      return readIndex(s, section);
    }

    // for the obs structure
    int[] shape = section.getShape();

    // allocate ArrayStructureBB for outer structure
    StructureMembers members = s.makeStructureMembers();
    int offset = setOffsets(members);

    ArrayStructureBB abb = new ArrayStructureBB(members, shape);
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);
    long total_offset = offset * section.computeSize();
    //System.out.println("offset=" + offset + " nelems= " + section.computeSize());
    //System.out.println("total offset=" + total_offset + " bb_size= " + bb.capacity());
    // assert offset == bb.capacity() : "total offset="+offset+ " bb_size= "+bb.capacity();

    /* loop through desired obs
    List<Index.BufrObs> obsList = index.getObservations();
    Range range = section.getRange(0);
    for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
      if (obsIndex >= obsList.size())
        System.out.println("HEY");
      Index.BufrObs obs = obsList.get(obsIndex);
      raf.seek(obs.getOffset());
      bitPos = obs.getBitPos();
      bitBuf = obs.getStartingByte();

      bb.putLong(getTime(obs));
      readData(delegate.dkeys, abb, bb);
    } */

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

  private void readOneMessage(BufrMessage m, DataDescriptor keyRoot) throws IOException {

      raf.seek(m.dataSection.dataPos + 4);
      int msg_nbits = 0;

      for (int i = 0; i<m.getNumberDatasets(); i++) {
        BufrDataset ds = new BufrDataset(m);
        ds.setFilePos( raf.getFilePointer());
        countData(keyRoot.subKeys, ds.getReplicationCounter(keyRoot), 0);

        int nbits1 = m.dds.getTotalBits();
        int nbits = ds.getBitCount();
        if (nbits1 != nbits) {
          Formatter out = new Formatter(System.out);
          new BufrDump2().dump(out, m);
          new BufrDump2().dump(out, protoMessage);
          out.flush();
          ds.getBitCount();  
        }
        msg_nbits += nbits;
        System.out.println("  dataset BitCount= "+nbits);
      }

      int msg_nbytes = msg_nbits / 8;
      if (msg_nbits % 8 != 0) msg_nbytes++;

      msg_nbytes += 4;
      if (msg_nbytes % 2 != 0) msg_nbytes++;
      if (msg_nbytes != m.dataSection.dataLength) System.out.print("***BAD ");
      System.out.println("  message BitCount= "+msg_nbits+" nbytes= "+msg_nbytes+" dataLength="+m.dataSection.dataLength);
  }

  private void countData(List<DataDescriptor> dkeys, ReplicationCounter rc, int index) throws IOException {

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      // sequence
      if (dkey.replication == 0) {
        int count = bits2UInt(dkey.replicationCountSize);
        ReplicationCounter nested = rc.makeNested(dkey, count, index);

        for (int i = 0; i < count; i++)
          countData(dkey.subKeys, nested, i);

        continue;
      }

      // compound
      if (dkey.type == 3) {
        ReplicationCounter nested = rc.makeNested(dkey, dkey.replication, index);
        for (int i = 0; i < dkey.replication; i++)
          countData(dkey.subKeys, nested, i);
        continue;
      }

      // char data
      if (dkey.type == 1) {
        for (int i = 0; i < dkey.getByteWidth(); i++)
          bits2UInt(8);
        continue;
      }

      // otherwise read a numberic
      bits2UInt(dkey.bitWidth);
    }
  }

  private void readData(List<DataDescriptor> dkeys, ArrayStructureBB abb, ByteBuffer bb) throws IOException {

    // LOOK : assumes we want all members
    // transfer the bits to the ByteBuffer, aligning on byte boundaries
    for (DataDescriptor dkey : dkeys) {
      // misc skip
      if (!dkey.isOkForVariable())
        continue;

      // sequence
      if (dkey.replication == 0) {
        int count = bits2UInt(dkey.replicationCountSize);
        ArraySequence seq = makeArraySequence(count, dkey);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      if (dkey.type == 3) {
        for (int i = 0; i < dkey.replication; i++)
          readData(dkey.subKeys, abb, bb);
        continue;
      }

      // System.out.println(dkey.name+" bbpos="+bb.position());

      // char data
      if (dkey.type == 1) {
        for (int i = 0; i < dkey.getByteWidth(); i++) {
          bb.put((byte) bits2UInt(8));
        }
        continue;
      }

      // extract from BUFR file
      int result = bits2UInt(dkey.bitWidth);
      //System.out.println(dkey.name+" result = " + result );

      // place into byte buffer
      if (dkey.getByteWidth() == 1) {
        bb.put((byte) result);
      } else if (dkey.getByteWidth() == 2) {
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

  private int bitBuf = 0;
  private int bitPos = 0;

  // read the next nb bits
  private int bits2UInt(int nb) throws IOException {

    int bitsLeft = nb;
    int result = 0;

    if (bitPos == 0) {
      bitBuf = raf.read();
      bitPos = 8;
    }

    while (true) {
      int shift = bitsLeft - bitPos;
      if (shift > 0) {
        // Consume the entire buffer
        result |= bitBuf << shift;
        bitsLeft -= bitPos;

        // Get the next byte from the RandomAccessFile
        bitBuf = raf.read();
        bitPos = 8;
      } else {
        // Consume a portion of the buffer
        result |= bitBuf >> -shift;
        bitPos -= bitsLeft;
        bitBuf &= 0xff >> (8 - bitPos);   // mask off consumed bits
        //System.out.println( "bitBuf = " + bitBuf );

        return result;
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
  private ArraySequence makeArraySequence(int count, DataDescriptor seqdd) throws IOException {
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
        offset += dk.getByteWidth();
    }

    ArrayStructureBB abb = new ArrayStructureBB(members, shape);
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);

    // loop through desired obs
    for (int i = 0; i < count; i++) {
      readData(seqdd.getSubKeys(), abb, bb);
    }

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
    String fileIn = "C:/data/bufr/edition3/ecmwf/synop.bufr";
    NetcdfDataset ncf = NetcdfDataset.openDataset(fileIn);
    /* System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable(obsRecord);
    StructureData sdata = s.readStructure(0);
    PrintWriter pw = new PrintWriter(System.out);
    NCdumpW.printStructureData(pw, sdata);   */

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
