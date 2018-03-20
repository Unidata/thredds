/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import org.jdom2.Element;
import ucar.nc2.constants.DataFormatType;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * IOSP for BUFR data - version 2, use the preprocessor
 *
 * @author caron
 * @since 8/8/13
 */
public class BufrIosp2 extends AbstractIOServiceProvider {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrIosp2.class);

  static public final String obsRecord = "obs";
  static public final String fxyAttName = "BUFR:TableB_descriptor";
  static public final String centerId = "BUFR:centerId";

  // debugging
  static private boolean debugIter = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
//    debugOpen = debugFlag.isSet("Bufr/open");
    debugIter = debugFlag.isSet("Bufr/iter");
  }

  //static public final Set<NetcdfDataset.Enhance> enhance = Collections.unmodifiableSet(EnumSet.of(NetcdfDataset.Enhance.ScaleMissing));


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Structure obsStructure;
  private Message protoMessage;
  private MessageScanner scanner;
  private HashSet<Integer> messHash = null;
  private boolean isSingle;
  private BufrConfig config;
  private Element iospParam;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return MessageScanner.isValidFile(raf);
  }

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    scanner = new MessageScanner(raf);
    protoMessage = scanner.getFirstDataMessage();
    if (protoMessage == null)
      throw new IOException("No data messages in the file= "+ncfile.getLocation());
    // DataDescriptor dds = protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    // just get the fields
    config = BufrConfig.openFromMessage(raf, protoMessage, iospParam);

    // this fills the netcdf object
    Construct2 construct = new Construct2(protoMessage, config, ncfile);
    obsStructure = construct.getObsStructure();
    ncfile.finish();
    isSingle = false;
  }

    // for BufrMessageViewer
  public void open(RandomAccessFile raf, NetcdfFile ncfile, Message single) throws IOException {
    this.raf = raf;

    protoMessage = single;
    protoMessage.getRootDataDescriptor(); // construct the data descriptors, check for complete tables
    if (!protoMessage.isTablesComplete())
      throw new IllegalStateException("BUFR file has incomplete tables");

    BufrConfig config = BufrConfig.openFromMessage(raf, protoMessage, null);

    // this fills the netcdf object
    Construct2 construct = new Construct2(protoMessage, config, ncfile);
    obsStructure = construct.getObsStructure();
    isSingle = true;

    ncfile.finish();
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message instanceof Element) {
      iospParam = (Element) message;
      iospParam.detach();
      return true;
    }

    return super.sendIospMessage(message);
  }

  public BufrConfig getConfig() {
    return config;
  }

  public Element getElem() {
    return iospParam;
  }

  private int nelems = -1;

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    //return new ArraySequence(obsStructure.makeStructureMembers(), getStructureIterator(null, -1), nelems);
    return new ArraySequence(obsStructure.makeStructureMembers(), new SeqIter(), nelems);
  }

  @Override
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    return isSingle ? new SeqIterSingle() : new SeqIter();
  }

  private class SeqIter implements StructureDataIterator {
    StructureDataIterator currIter;
    int recnum = 0;

    SeqIter() {
      reset();
    }

    @Override
    public StructureDataIterator reset() {
      recnum = 0;
      currIter = null;
      scanner.reset();
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
      if (!scanner.hasNext()) return null;
      Message m = scanner.next();
      if (m == null) {
          log.warn("BUFR scanner hasNext() true but next() null!");
          return null;
      }
      if (m.containsBufrTable()) // data messages only
        return readNextMessage();

      // mixed messages
      if (!protoMessage.equals(m)) {
        if (messHash == null) messHash = new HashSet<>(20);
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
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      }
      return as;
    }

    @Override
    public int getCurrentRecno() {
      return recnum - 1;
    }

    @Override
    public void close() {
      if (currIter != null) currIter.close();
      currIter = null;
      if (debugIter) System.out.printf("BUFR read recnum %d%n", recnum);
    }
  }

  private class SeqIterSingle implements StructureDataIterator {
    StructureDataIterator currIter;
    int recnum = 0;

    SeqIterSingle() {
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
        currIter = readProtoMessage();
        if (currIter == null) {
          nelems = recnum;
          return false;
        }
      }

      return currIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      recnum++;
      return currIter.next();
    }

    private StructureDataIterator readProtoMessage() throws IOException {
      Message m = protoMessage;
      ArrayStructure as;
      if (m.dds.isCompressed()) {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      } else {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        as = reader.readEntireMessage(obsStructure, protoMessage, m, raf, null);
      }

      return as.getStructureDataIterator();
    }

    @Override
    public int getCurrentRecno() {
      return recnum - 1;
    }

    @Override
    public void close() {
      if (currIter != null) currIter.close();
      currIter = null;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String getDetailInfo() {
    Formatter ff = new Formatter();
    ff.format("%s", super.getDetailInfo());
    protoMessage.dump(ff);
    ff.format("%n");
    config.show(ff);
    return ff.toString();
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.BUFR.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "WMO Binary Universal Form";
  }

}
