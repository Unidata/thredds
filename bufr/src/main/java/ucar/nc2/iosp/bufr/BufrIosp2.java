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
import ucar.nc2.iosp.bufr.writer.Bufr2nc;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * IOSP for BUFR data - version 2, use the preprocssor
 *
 * @author caron
 */
public class BufrIosp2 extends AbstractIOServiceProvider {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp2.class);

  static final String TIME_NAME = "time";
  static public final String obsRecord = "obs";

  // debugging
  static private boolean debugCompress = false;
  static private boolean debugOpen = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Bufr/open");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Construct2 construct;
  private Message protoMessage;
  private MessageScanner scanner;
  private HashSet<Integer> messHash = null;

  //private final List<Message> msgs = new ArrayList<Message>();
  //private int[] obsStart; // for each message, the starting observation index
  private boolean wantTime;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    scanner = new MessageScanner(raf);
    int count = 0;
    protoMessage = scanner.getFirstDataMessage();
    if (protoMessage == null)
      throw new IOException("No data messages in the file= "+ncfile.getLocation());
    DataDescriptor dds = protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    BufrConfig convert = BufrConfig.openFromBufrFile(raf);

    // this fills the netcdf object
    construct = new Construct2(protoMessage, convert, ncfile);
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

  /* / for BufrMessageViewer
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
    construct = new Construct2(protoMessage, countObs, ncfile);

    ncfile.finish();
  }  */

  private int nelems = -1;

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Structure s = construct.recordStructure;
    return new ArraySequence(s.makeStructureMembers(), new SeqIter(), nelems);
  }

  private void addTime(ArrayStructure as) throws IOException {
    int n = (int) as.getSize();
    Array timeData = Array.factory(String.class, new int[]{n});
    IndexIterator ii = timeData.getIndexIterator();

    if (as instanceof ArrayStructureBB) {
      ArrayStructureBB asbb = (ArrayStructureBB) as;
      StructureMembers.Member m = asbb.findMember( TIME_NAME);
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
      StructureMembers.Member m = as.findMember(TIME_NAME);
      m.setDataArray(timeData);
    }
  }


  // LOOK not threadsafe - alterntive is to open raf for each iterator
  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return new SeqIter();
  }

  private class SeqIter implements StructureDataIterator {
    StructureDataIterator currIter;
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
      currIter = null;
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (currIter == null) {
        scanner.reset();

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
      if (!scanner.hasNext()) return null;
      Message m = scanner.next();
      if (m.containsBufrTable()) // data messages only
        return readNextMessage();

      // mixed messages
      if (!protoMessage.equals(m)) {
        if (messHash == null) messHash = new HashSet<Integer>(20);
        if (!messHash.contains(m.hashCode()))  {
          log.warn("File " + raf.getLocation() + " has different BUFR message types hash=" + protoMessage.hashCode() + "; skipping");
          messHash.add(m.hashCode());
        }
        return readNextMessage();
      }

      ArrayStructure as = readMessage(m);
      return as.getStructureDataIterator();
    }

    private ArrayStructure readMessage(Message m) throws IOException {
      ArrayStructure as;
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        as = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(construct.recordStructure, protoMessage, m, raf, null);
      }
      return as;
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

  @Override
  public String getDetailInfo() {
    Formatter ff = new Formatter();
    ff.format("%s",super.getDetailInfo());
    try {
      protoMessage.dump(ff);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ff.toString();
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.BUFR.toString();
  }

  @Override
  public String getFileTypeDescription() {
    return "WMO Binary Universal Form";
  }

} // end BufrIosp2
