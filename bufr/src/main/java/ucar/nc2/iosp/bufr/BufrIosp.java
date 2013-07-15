/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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

import thredds.catalog.DataFormatType;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CompareNetcdf2;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * IOSP for BUFR data
 *
 * @author caron
 */
public class BufrIosp extends AbstractIOServiceProvider {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp.class);
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

  private final List<Message> msgs = new ArrayList<Message>();
  private int[] obsStart; // for each message, the starting observation index
  private boolean wantTime;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  @Override
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
      if (m.containsBufrTable()) continue; // not data

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
    if (protoMessage == null)
      throw new IOException("No data messages in the file= "+ncfile.getLocation());
    construct = new ConstructNC(protoMessage, countObs, ncfile);

    ncfile.finish();
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof String) {
      String mess = (String) message;
      if (mess.equals("AddTime")) {
        wantTime = true;
        return true;
      }
    }
    return super.sendIospMessage(message);
  }

  // for BufrMessageViewer
  public void open(RandomAccessFile raf, NetcdfFile ncfile, Message single) throws IOException {
    this.raf = raf;

    protoMessage = single;
    protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    msgs.add(single);

    // count where the obs start in the messages
    obsStart = new int[msgs.size()];
    int mi = 0;
    int countObs = 0;
    for (Message m : msgs) {
      obsStart[mi++] = countObs;
      countObs += m.getNumberDatasets();
    }

    // this fills the netcdf object
    construct = new ConstructNC(protoMessage, countObs, ncfile);

    ncfile.finish();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  /* private class MsgFinder {
    int msgIndex = 0;

    Message find(int index) {
      while (msgIndex < msgs.size()) {  // use binary search
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
    Structure s = construct.recordStructure; // always read all members (!)
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

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Structure s = construct.recordStructure;
    return new ArraySequence(s.makeStructureMembers(), new SeqIter(), nelems);
  }

  ArraySequence readAll() throws IOException {
    Structure s = construct.recordStructure;
    return new ArraySequence(s.makeStructureMembers(), new SeqIter(), nelems);
  }

  private void addTime(ArrayStructure as) throws IOException {
    int n = (int) as.getSize();
    Array timeData = Array.factory(String.class, new int[]{n});
    IndexIterator ii = timeData.getIndexIterator();

    if (as instanceof ArrayStructureBB) {
      ArrayStructureBB asbb = (ArrayStructureBB) as;
      StructureMembers.Member m = asbb.findMember( ConstructNC.TIME_NAME);
      StructureDataIterator iter = as.getStructureDataIterator();
      try {
        int recno = 0;
        while (iter.hasNext()) {
          CalendarDate cd = construct.makeObsTimeValue(iter.next());
          asbb.addObjectToHeap(recno, m, cd.toString()); // add object into the Heap
          recno++;
        }
      } finally {
        iter.finish();
      }

    } else {
      StructureDataIterator iter = as.getStructureDataIterator();
      try {
        while (iter.hasNext()) {
          CalendarDate cd = construct.makeObsTimeValue(iter.next());
          ii.setObjectNext( cd.toString());
        }
      } finally {
        iter.finish();
      }
      StructureMembers.Member m = as.findMember(ConstructNC.TIME_NAME);
      m.setDataArray(timeData);
    }
  }


  @Override
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
      addTime = false; // construct.recordStructure.findVariable(ConstructNC.TIME_NAME) != null;
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

      if (wantTime && construct.isTimeOk()) addTime(as);
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

    @Override
    public void finish() {
      if (currIter != null) currIter.finish();
      currIter = null;
    }
  }

  public String getDetailInfo() {
    Formatter ff = new Formatter();
    ff.format("%s",super.getDetailInfo());
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
    return DataFormatType.BUFR.toString();
  }

  public String getFileTypeDescription() {
    return "WMO Binary Universal Form";
  }

  public void readAll(boolean dump) throws IOException, InvalidRangeException {
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
      CompareNetcdf2 cn = new CompareNetcdf2(new Formatter(System.out), true, true, true);
      cn.compareData("all", data1, data2, true);

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
    doon("Q:/cdmUnitTest/formats/bufr/eumetsat/MSG2-SEVI-MSGCLDS-0101-0101-20080405114500.000000000Z-909326.bfr");
  }

} // end BufrIosp
