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
//import ucar.bufr.BufrIndexExtender;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.net.URL;
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

  // debugging
  static boolean debugOpen = false, debugMissing = false, debugMissingDetails = false, debugProj = false, debugTiming = false, debugVert = false;

  static public boolean forceNewIndex = false; // force that a new index file is written
  static public boolean useMaximalCoordSys = false;
  static public boolean extendIndex = false; // check if index needs to be extended

  static public void useMaximalCoordSys(boolean b) {
    useMaximalCoordSys = b;
  }

  static public void setExtendIndex(boolean b) {
    extendIndex = b;
  }

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Bufr/open");
    debugMissing = debugFlag.isSet("Bufr/missing");
    debugMissingDetails = debugFlag.isSet("Bufr/missingDetails");
    debugProj = debugFlag.isSet("Bufr/projection");
    debugVert = debugFlag.isSet("Bufr/vertical");
    debugTiming = debugFlag.isSet("Bufr/timing");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfFile ncfile;
  protected RandomAccessFile raf;
  protected StringBuffer parseInfo = new StringBuffer();

  private Index2NC delegate;
  private Index index;
  private DateFormatter dateFormatter = new DateFormatter();

  // keep this info to reopen index when extending or syncing
  private Index saveIndex = null;    // the Bufr record index
  private File saveIndexFile = null; // the index file
  private String saveLocation;
  private BufrDataExtractor dataReader;


  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    try {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);
      // Create BufrInput instance
      ucar.bufr.BufrInput bi = new ucar.bufr.BufrInput(raf);
      return bi.isValidFile();
    } catch (IOException ex) {
      ex.printStackTrace();
      return false;
    }
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    index = getIndex(raf.getLocation(), cancelTask);
    construct(index);
  }

  private void construct(Index index) throws IOException {
    long startTime = System.currentTimeMillis();

    // read the first record
    raf.order(RandomAccessFile.BIG_ENDIAN);
    BufrInput bi = new BufrInput(raf);
    bi.scan(true, false);
    BufrRecord record = bi.getRecords().get(0);

    // populate the netcdf objects
    delegate = new Index2NC(record, index, ncfile);

    ncfile.finish();

    Map<String, String> atts = index.getGlobalAttributes();
    //System.out.println( "table ="+ (String)atts.get( "table" ) );
    dataReader = new BufrDataExtractor(raf, atts.get("table"));

    if (debugTiming) {
      long took = System.currentTimeMillis() - startTime;
      System.out.println(" open " + ncfile.getLocation() + " took=" + took + " msec ");
    }
  }

  /**
   * Open the index file. If not exists, create it.
   * When writing use DiskCache, to make sure location is writeable.
   *
   * @param location   location of the file. The index file has ".bfx" appended.
   * @param cancelTask user may cancel
   * @return ucar.bufr.Index
   * @throws IOException on read error
   */
  protected Index getIndex(String location, CancelTask cancelTask) throws IOException {
    // get an Index
    saveLocation = location;
    String indexLocation = location + ".bfx";

    if (indexLocation.startsWith("http:")) { // LOOK direct access through http
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        saveIndex = new Index();
        saveIndex.open(indexLocation, ios);
        if (debugOpen) System.out.println("  opened HTTP index = " + indexLocation);
        return saveIndex;

      } else { // otherwise write it to / get it from the cache
        saveIndexFile = DiskCache.getCacheFile(indexLocation);
        if (debugOpen) System.out.println("  HTTP index = " + saveIndexFile.getPath());
      }

    } else {
      // get the index file, look in cache if need be
      saveIndexFile = DiskCache.getFileStandardPolicy(indexLocation);
    }

    // if exist already, read it
    if (!forceNewIndex && saveIndexFile.exists()) {
      saveIndex = new Index();
      boolean ok = saveIndex.open(saveIndexFile.getPath());

      if (ok) {
        if (debugOpen) System.out.println("  opened index = " + saveIndexFile.getPath());

        // deal with possiblity that the bufr file has grown, and the index should be extended.
        // only do this if index extension is allowed.
        if (extendIndex) {
          Map<String, String> attrHash = saveIndex.getGlobalAttributes();
          String lengthS = (String) attrHash.get("length");
          long length = (lengthS == null) ? 0 : Long.parseLong(lengthS);
          if (length < raf.length()) {
            if (debugOpen) System.out.println("  calling extendIndex");
            saveIndex = extendIndex(raf, saveIndexFile, saveIndex);
          }
        }

      } else {  // rewrite if fail to open
        saveIndex = writeIndex(saveIndexFile, raf);
        if (debugOpen) System.out.println("  rewrite index = " + saveIndexFile.getPath());
      }

    } else {
      // doesnt exist (or is being forced), create it and write it
      saveIndex = writeIndex(saveIndexFile, raf);
      if (debugOpen) System.out.println("  write index = " + saveIndexFile.getPath());
    }
    return saveIndex;
  }

  private Index writeIndex(File indexFile, RandomAccessFile raf) throws IOException {
    Index index = null;
    raf.seek(0);

    PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
    ucar.bufr.BufrIndexer indexer = new ucar.bufr.BufrIndexer();
    index = indexer.writeFileIndex(raf, ps, true);

    return index;
  }

  public boolean sync() throws IOException {
    Map<String, String> attrHash = saveIndex.getGlobalAttributes();
    String lengthS = attrHash.get("length");
    long length = (lengthS == null) ? 0 : Long.parseLong(lengthS);

    if (length < raf.length()) {

      // case 1 is where we look to see if the index has grown. This can be turned off if needed to deal with multithreading
      // conflicts (eg TDS). We should get File locking working to deal with this instead.
      if (extendIndex && (saveIndexFile != null)) {
        if (debugOpen) System.out.println("calling IndexExtender");
        saveIndex = extendIndex(raf, saveIndexFile, saveIndex);

        // case 2 just reopen the index again
      } else {
        if (debugOpen) System.out.println("sync reopen Index");
        saveIndex = getIndex(saveLocation, null);
      }

      // reconstruct the ncfile objects
      ncfile.empty();
      construct(saveIndex);
      return true;
    }

    return false;
  }

  /*
   * takes a bufr data file, a .bfx index file and a index, reads the current
   * .bfx index, reads the data file starting at old eof for new
   * data, updates the *.bfx file and the index
   *
   */
  private Index extendIndex(RandomAccessFile raf, File indexFile, Index index) throws IOException {

    /*
     BufrIndexExtender indexExt = new BufrIndexExtender();
     index = indexExt.extendIndex(raf, indexFile, index);

     return index;
    */
    return null;
  }

  // if exists, return input stream, otherwise null
  private InputStream indexExistsAsURL(String indexLocation) {
    try {
      URL url = new URL(indexLocation);
      return url.openStream();
    } catch (IOException e) {
      return null;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {

    // LOOK
    if (!(v2 instanceof Structure)) {
      return readDataVariable(v2.getName(), v2.getDataType(), section.getRanges());
    }
    Structure s = (Structure) v2;

    if (v2.getName().equals("recordIndex")) {
      return readReportIndex(s, section);
    }

    // for the obs structure
    int[] shape = section.getShape();

    // allocate ArrayStructureBB for outer structure
    StructureMembers members = s.makeStructureMembers();
    int offset = setOffsets( members);

    /* for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      // System.out.println(m.getName()+" offset="+offset);

      // set the inner offsets
      if (m.getDataType() == DataType.STRUCTURE) {
        int innerOffset = 0;
        StructureMembers innerMembers = m.getStructureMembers();
        for (StructureMembers.Member mm : innerMembers.getMembers()) {
          mm.setDataParam(innerOffset);
          innerOffset += mm.getSize();
        }
      }

      Variable mv = s.findVariable(m.getName());
      DataDescriptor dk = (DataDescriptor) mv.getSPobject();
      if (dk.replication == 0)
        offset += 4;
      else
        offset += dk.getByteWidth();
    } */

    ArrayStructureBB abb = new ArrayStructureBB(members, shape);
    ByteBuffer bb = abb.getByteBuffer();
    bb.order(ByteOrder.BIG_ENDIAN);
    long total_offset = offset * v2.getSize();
    System.out.println("offset="+offset+ " var_size= "+v2.getSize());
    System.out.println("total offset="+total_offset+ " bb_size= "+bb.capacity());
    // assert offset == bb.capacity() : "total offset="+offset+ " bb_size= "+bb.capacity();

    // loop through desired obs
    List<Index.BufrObs> obsList = index.getObservations();
    Range range = section.getRange(0);
    for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
      Index.BufrObs obs = obsList.get(obsIndex);
      raf.seek(obs.getOffset());
      bitPos = obs.getBitPos();
      bitBuf = obs.getStartingByte();
      readData(delegate.dkeys, abb, bb);
    }

    return abb;
  }

  private int setOffsets(StructureMembers members) {
    int offset = 0;
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      offset += m.getTotalSize();

      // set inner offsets
      if (m.getDataType() == DataType.STRUCTURE) {
        setOffsets(m.getStructureMembers());
      }

    }
    return offset;
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
        ArraySequence2 seq = makeArraySequence2(count, dkey);
        int index = abb.addObjectToHeap(seq);
        bb.putInt(index); // an index into the Heap
        continue;
      }

      if (dkey.type == 3) {
        for (int i=0; i<dkey.replication; i++)
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

  private Array readReportIndex(Structure s, Section section) {

    int[] shape = section.getShape();
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureMA ama = new ArrayStructureMA(members, shape);
    ArrayInt.D1 timeArray = new ArrayInt.D1(shape[0]);
    ArrayObject.D1 nameArray = new ArrayObject.D1(String.class, shape[0]);

    for (StructureMembers.Member m : members.getMembers()) {
      if (m.getName().equals("time"))
        m.setDataArray(timeArray);
      else
        m.setDataArray(nameArray);
    }

    // loop through desired obs
    int count = 0;
    List<Index.BufrObs> obsList = index.getObservations();
    Range range = section.getRange(0);
    for (int obsIndex = range.first(); obsIndex <= range.last(); obsIndex += range.stride()) {
      Index.BufrObs obs = obsList.get(obsIndex);
      try {
        Date date = dateFormatter.isoDateTimeFormat(obs.getIsoDate());
        timeArray.set(count, (int) date.getTime() / 1000);
        nameArray.set(count, obs.getName());
      } catch (ParseException e) {
        e.printStackTrace();
      }
      count++;
    }

    return ama;
  }

  // read in the data into an ArrayStructureBB
  private ArraySequence2 makeArraySequence2(int count, DataDescriptor seqdd) throws IOException {
    // kludge
    Structure s = find(ncfile.getRootGroup().getVariables());
    if (s == null) {
      log.error("Cant find sequence " + seqdd);
      throw new IllegalStateException("Cant find sequence " + seqdd);
    }

    // for the obs structure
    int[] shape = new int[]{count};

    // allocate ArrayStructureBB for outer structure
    int offset = 0;
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      //System.out.println(m.getName()+" offset="+offset);

      Variable mv = s.findVariable(m.getName());
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

    return new ArraySequence2(members, new SequenceIterator(count, abb));
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

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /* Map<String,Array> dataHash = new HashMap<String,Array>();
   Map<String,IndexIterator> iiHash = new HashMap<String,IndexIterator>();
   ArraySequence ias = null;
   for (Variable v : s.getVariables()) {
     Array data;
     //Array data = Array.factory(v.getDataType().getPrimitiveClassType(), Range.getShape( section ));
     // there should be a better way of allocating storage
     int[] vshape = v.getShape();
     if (vshape.length != 0 && vshape[0] != -1) {
       vshape[0] *= shape[0];
       data = Array.factory(v.getDataType().getPrimitiveClassType(), vshape);
     } else {
       data = Array.factory(v.getDataType().getPrimitiveClassType(), shape);
     }

     //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );
     IndexIterator ii = data.getIndexIterator();
     dataHash.put(v.getShortName(), data);
     iiHash.put(v.getShortName(), ii);
   }

   // compressed data is organized by field, not sequential obs
   if ((atts.get("compressdata")).equals("true")) {
     System.out.println("compressed data");
     readDataCompressed(atts.get("table"), locations, range,
         observations, s.getVariables(), iiHash, dataHash, members);
     return ama;
   }

   // start of data reads for sequential obs
   BufrDataExtractor bde = new BufrDataExtractor(raf, atts.get("table"));
   int currOb = -1; // current ob being processed

   //Variable v = null;
   for (String loc : locations) {
     if (currOb > range.last()) { // done
       break;
     }

     List<Index.BufrObs> obs = observations.get(loc);
     ucar.bufr.Index.BufrObs bo;
     for (int j = 0; j < obs.size(); j++) {
       currOb++;
       if (currOb < range.first() || currOb > range.last()) {
         continue;
       }
       bo = obs.get(j);

       //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
       if (bde.getData(bo.ddsOffset, bo.obsOffset, bo.bitPos, bo.bitBuf)) {
         HashMap bufrdatas = (HashMap) bde.getBufrDatas();
         String bKey = null;
         BufrData bd;
         IndexIterator ii;
         for (Variable v : s.getVariables()) {
           Attribute a = v.findAttribute("Bufr_key");
           if (a == null)
             continue;
           bKey = a.getStringValue();

           bd = (BufrData) bufrdatas.get(bKey);
           if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
             ii = (IndexIterator) iiHash.get(v.getShortName());
             ii.setLongNext((bd.getLongData())[0]);
           } else if (bd.isNumeric()) {
             ii = (IndexIterator) iiHash.get(v.getShortName());
             float[] bufrdata = bd.getFloatData();
             for (int n = 0; n < bufrdata.length; n++) {
               ii.setFloatNext(bufrdata[n]);
             }

           } else { // String data
             ii = (IndexIterator) iiHash.get(v.getShortName());
             ii.setObjectNext((bd.getStringData())[0]);
           }
         }
       } else {
         System.out.println("getData failed for loc =" + loc);
       }
     }
   } // end looping over locations

   // enter data into members
   for (Variable v : s.getVariables()) {
     Array data = (Array) dataHash.get(v.getShortName());
     StructureMembers.Member m = members.findMember(v.getShortName());
     if (v instanceof Structure) {
       m.setDataArray(ias);
       // dump ias as a check
       //m = (StructureMembers.Member) imembers.findMember( "Hgt_above_station" );
       // data = (Array) m.getDataObject();
       //IndexIterator ii = data.getIndexIterator();
       //System.out.println( "Hgt_above_station" );
       //for( ; ii.hasNext(); ) {
       //   System.out.print( ii.getFloatNext()  +", " );
       //}
     } else {
       m.setDataArray(data);
     }
   }
   return ama;

 } */


  public Array readData2(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    // check if v2 is a Structure
    if (v2 instanceof Structure) {
      // non-structure variable read
      //System.out.println( "non-structure variable read name ="+ v2.getName() );
      return readDataVariable(v2.getName(), v2.getDataType(), section.getRanges());
    }

    Map<String, String> atts = saveIndex.getGlobalAttributes();
    List<String> locations = saveIndex.getLocations();
    Map<String, List<Index.BufrObs>> observations = saveIndex.getObservationsMap();

    int[] shape = section.getShape();
    Range range = section.getRange(0);

    // allocate ArrayStructureMA for outer structure
    Structure s = (Structure) v2;
    StructureMembers members = s.makeStructureMembers();
    ArrayStructureMA ama = new ArrayStructureMA(members, shape);
    List<Variable> vars = s.getVariables();

    HashMap dataHash = new HashMap();
    HashMap iiHash = new HashMap();

    // inner structure variables
    ArraySequence ias = null;
    StructureMembers imembers;
    ArrayList ivars = null;
    HashMap sdataHash;
    HashMap siiHash = null;
    for (Variable v : s.getVariables()) {
      // inner structure
      if (v instanceof Structure) {
        imembers = ((Structure) v).makeStructureMembers();
        ias = new ArraySequence(imembers, shape[0]);
        ivars = (ArrayList) ((Structure) v).getVariables();
        sdataHash = new HashMap();
        siiHash = new HashMap();
        int currOb = -1;
        int idx = 0;
        for (int i = 0; i < locations.size(); i++) {
          if (currOb > range.last()) { // done
            break;
          }
          String loc = (String) locations.get(i);
          //System.out.println( "loc =" + loc );
          ArrayList obs = (ArrayList) observations.get(loc);
          ucar.bufr.Index.BufrObs bo;
          for (int j = 0; j < obs.size(); j++) {
            currOb++;
            if (currOb < range.first() || currOb > range.last()) {
              continue;
            }
            bo = (ucar.bufr.Index.BufrObs) obs.get(j);
            //System.out.println( currOb +" "+ bo.name +" "+ bo.dim );
            //for (int seq=0; seq < outerLength; seq++)
            //   aseq.setSequenceLength(seq, seqLength);
            ias.setSequenceLength(idx++, bo.dim);
          }
        }
        ias.finish();

/*           sudo code
             for (int j=0; j<members.size(); j++) {
                Member m = members.get(j);
                Array data = (Array) m.getDataObject();
                Iterator iter = data.getIterator();
                Object data = extract(m); // really a float[], long[], String[]
                aseq.setDataArray(seq, m, data);
             }
*/
        ArrayList im = (ArrayList) imembers.getMembers();
        for (int j = 0; j < im.size(); j++) {
          StructureMembers.Member m = (StructureMembers.Member) im.get(j);
          Array data = (Array) m.getDataArray();
          IndexIterator ii = data.getIndexIterator();
          //System.out.println( "IndexIterator ii ="+ ii );
          sdataHash.put(m.getName(), data);
          siiHash.put(m.getName(), ii);
          //System.out.println( "m.getName() ="+ m.getName() );
          //ii = (IndexIterator) siiHash.get( m.getName() );
          //System.out.println( "IndexIterator ii ="+ ii );
        }
      } // end inner Structure

      // outer structure variables
      Array data = null;
      //Array data = Array.factory(v.getDataType().getPrimitiveClassType(), Range.getShape( section ));
      // there should be a better way of allocating storage
      int[] vshape = v.getShape();
      if (vshape.length != 0 && vshape[0] != -1) {
        //System.out.println( "v.sshape ="+ vshape[0] +" rank ="+ vshape.length );
        vshape[0] *= shape[0];
        data = Array.factory(v.getDataType().getPrimitiveClassType(), vshape);
      } else {
        data = Array.factory(v.getDataType().getPrimitiveClassType(), shape);
      }
      //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );
      IndexIterator ii = data.getIndexIterator();
      dataHash.put(v.getShortName(), data);
      iiHash.put(v.getShortName(), ii);
    }
    // compressed data is organized by field, not sequential obs
    if (((String) atts.get("compressdata")).equals("true")) {
      System.out.println("compressed data");
      readDataCompressed((String) atts.get("table"), locations, range,
          observations, vars, iiHash, dataHash, members);
      return ama;
    }

    // start of data reads for sequential obs
    BufrDataExtractor bde =
        new BufrDataExtractor(raf, (String) atts.get("table"));
    int currOb = -1; // current ob being processed
    Variable v = null;
    for (int i = 0; i < locations.size(); i++) {
      if (currOb > range.last()) { // done
        break;
      }
      String loc = (String) locations.get(i);
      //System.out.println( "loc =" + loc );
      ArrayList obs = (ArrayList) observations.get(loc);
      ucar.bufr.Index.BufrObs bo;
      for (int j = 0; j < obs.size(); j++) {
        currOb++;
        if (currOb < range.first() || currOb > range.last()) {
          continue;
        }
        bo = (ucar.bufr.Index.BufrObs) obs.get(j);
        //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
        if (bde.getData(bo.ddsOffset, bo.obsOffset, bo.bitPos, bo.bitBuf)) {
          HashMap bufrdatas = (HashMap) bde.getBufrDatas();
          String bKey = null;
          BufrData bd;
          IndexIterator ii;
          for (int k = 0; k < vars.size(); k++) {
            v = (Variable) vars.get(k);
            // inner structure
            if (v instanceof Structure) {
              //System.out.println( "v is a Structure name ="+ v.getShortName() );
              for (int m = 0; m < ivars.size(); m++) {
                v = (Variable) ivars.get(m);
                Attribute a = v.findAttribute("Bufr_key");
                if (a == null)
                  continue;
                bKey = a.getStringValue();
                bd = (BufrData) bufrdatas.get(bKey);
                if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
                  ii = (IndexIterator) siiHash.get(v.getShortName());
                  ii.setLongNext((bd.getLongData())[0]);
                } else if (bd.isNumeric()) {
                  //System.out.println( "v.getShortName() ="+ v.getShortName() );
                  ii = (IndexIterator) siiHash.get(v.getShortName());
                  //System.out.println( "IndexIterator ii ="+ ii );
                  float[] bufrdata = bd.getFloatData();
                  for (int n = 0; n < bufrdata.length; n++) {
                    ii.setFloatNext(bufrdata[n]);
                    //System.out.print( bufrdata[n] + ", " );
                  }
                  //System.out.println();

                } else { // String data
                  ii = (IndexIterator) siiHash.get(v.getShortName());
                  ii.setObjectNext((bd.getStringData())[0]);
                }
              }
              continue;
///////////////////// end inner structure

            } else if (v.getShortName().equals("parent_index")) {
              ii = (IndexIterator) iiHash.get(v.getShortName());
              ii.setIntNext(i);
              continue;
            } else {
              Attribute a = v.findAttribute("Bufr_key");
              if (a == null)
                continue;
              bKey = a.getStringValue();
            }
            bd = (BufrData) bufrdatas.get(bKey);
            if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
              ii = (IndexIterator) iiHash.get(v.getShortName());
              ii.setLongNext((bd.getLongData())[0]);
            } else if (bd.isNumeric()) {
              ii = (IndexIterator) iiHash.get(v.getShortName());
              float[] bufrdata = bd.getFloatData();
              for (int n = 0; n < bufrdata.length; n++) {
                ii.setFloatNext(bufrdata[n]);
              }

            } else { // String data
              ii = (IndexIterator) iiHash.get(v.getShortName());
              ii.setObjectNext((bd.getStringData())[0]);
            }
          }
        } else {
          System.out.println("getData failed for loc =" + loc);
        }
      }
    } // end looping over locations

    // enter data into members
    for (int k = 0; k < vars.size(); k++) {
      v = (Variable) vars.get(k);
      Array data = (Array) dataHash.get(v.getShortName());
      StructureMembers.Member m = members.findMember(v.getShortName());
      if (v instanceof Structure) {
        m.setDataArray(ias);
        // dump ias as a check
        //m = (StructureMembers.Member) imembers.findMember( "Hgt_above_station" );
        // data = (Array) m.getDataObject();
        //IndexIterator ii = data.getIndexIterator();
        //System.out.println( "Hgt_above_station" );
        //for( ; ii.hasNext(); ) {
        //   System.out.print( ii.getFloatNext()  +", " );
        //}
      } else {
        m.setDataArray(data);
      }
    }
    return ama;

  }

  private void readDataCompressed(String table, List<String> locations,
                                  Range range, Map<String, List<Index.BufrObs>> obsMap, List<Variable> vars, Map<String, IndexIterator> iiMap,
                                  Map<String, Array> dataHash, StructureMembers members)
      throws IOException, InvalidRangeException {

    // start of data reads
    BufrDataExtractor bde = new BufrDataExtractor(raf, table);
    int currOb = -1; // current ob being processed

    for (int i = 0; i < locations.size(); i++) {
      if (currOb > range.last())  // done
        break;

      String loc = locations.get(i);
      //System.out.println( "loc =" + loc );
      List<Index.BufrObs> obs = obsMap.get(loc);
      for (Index.BufrObs bo : obs) {
        if (currOb > range.last()) break; // done
        int maxCopy = range.last() - currOb;
        int numObs = bo.bitBuf; // number of obs in this data block

        //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
        // for compressed data bo.bitBuf contains number of obs in data
        //if ((currOb + numObs) > range.last()) {
        //  maxCopy = range.last() - currOb;
        //  //System.out.println( "end ="+ end );
        //} else {
        //  maxCopy = -1;
        //}
        currOb += numObs; // bo.bitBuf contains number of obs in data
        if (currOb < range.first()) continue; // skip until we get to first one

        // compressed data packs a number of Obs into one data block
        // bo.bitBuf is always 0 for compressed data
        if (bde.getData(bo.ddsOffset, bo.obsOffset, bo.bitPos, 0)) {
          Map<String, BufrData> bufrdatas = bde.getBufrDatas();
          for (Variable v : vars) {
            // handled specially
            if (v.getShortName().equals("parent_index")) {
              IndexIterator ii = iiMap.get(v.getShortName());
              ii.setIntNext(i);
              continue;
            }

            Attribute a = v.findAttribute("Bufr_key");
            if (a == null)
              continue;
            String bKey = a.getStringValue();

            BufrData bd = bufrdatas.get(bKey);
            if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
              IndexIterator ii = iiMap.get(v.getShortName());
              long[] bufrdata = bd.getLongData();
              for (int n = 0; n < bufrdata.length && n < maxCopy; n++)
                ii.setLongNext(bufrdata[n]);
              //System.out.println( "time_observation ="+ bufrdata[n] );

            } else if (bd.isNumeric()) {
              IndexIterator ii = iiMap.get(v.getShortName());
              float[] bufrdata = bd.getFloatData();
              if (v.getShortName().equals("Pressure")) {
                System.out.println("Var " + v.getNameAndDimensions());
                System.out.println("  bufrdata.length =" + bufrdata.length + " numObs= " + numObs + " end=" + maxCopy + " ii=" + ii);
              }
              int count = 0;
              for (int m = 0; m < numObs; m++) {
                if (count >= maxCopy) break;
                for (int n = m; n < bufrdata.length; n += numObs) {
                  if (count >= maxCopy) break;
                  ii.setFloatNext(bufrdata[n]);
                  count++;
                }
              }

            } else { // String data
              IndexIterator ii = iiMap.get(v.getShortName());
              String[] bufrdata = bd.getStringData();
              for (int n = 0; n < bufrdata.length && n < maxCopy; n++) {
                ii.setObjectNext(bufrdata[n]);
                //System.out.println( "String ="+ bufrdata[n] );
              }
            }
          }
        } else {
          System.out.println("getData failed for loc =" + loc);
        }
      }
    }

    // enter data in members
    for (Variable v : vars) {
      Array data = dataHash.get(v.getShortName());
      StructureMembers.Member m = members.findMember(v.getShortName());
      m.setDataArray(data);
    }
  } // end readDataCompressed

  private Array readDataVariable(String name, DataType datatype,
                                 java.util.List section) throws IOException, InvalidRangeException {

    //System.out.println( "readDataVariable name ="+ name );
    Index.Parameter p = null;
    int[] shape = Range.getShape(section);
    List<String> locations = saveIndex.getLocations();
    Map<String, List<Index.BufrObs>> observations = saveIndex.getObservationsMap();
    List<String> times = saveIndex.getObsTimes();
    //HashMap timeLocations = saveIndex.getObsLocations();
    //ArrayList parameters = saveIndex.getParameters();

    if (name.equals("number_stations") || name.equals("number_trajectories")) {
      int[] number = new int[1];
      number[0] = locations.size();
      return Array.factory(datatype.getPrimitiveClassType(), shape, number);
    }

    if (name.equals("station_id") || name.equals("trajectory_id")) {
      String[] ids = new String[locations.size()];
      for (int i = 0; i < locations.size(); i++) {
        ids[i] = (String) locations.get(i);
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, ids);
    }

    if (name.equals("firstChild")) {
      int pos = 0;
      int[] fc = new int[locations.size()];
      fc[0] = 0;
      for (int i = 0; i < locations.size(); i++) {
        String loc = (String) locations.get(i);
        if (i == 0) {
          fc[i] = 0;
          pos = ((ArrayList) observations.get(loc)).size();
        } else {
          fc[i] = pos;
          pos += ((ArrayList) observations.get(loc)).size();
        }
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, fc);
    }

    if (name.equals("numChildren")) {
      int[] nc = new int[locations.size()];
      for (int i = 0; i < locations.size(); i++) {
        String loc = (String) locations.get(i);
        nc[i] = ((ArrayList) observations.get(loc)).size();
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, nc);
    }

    if (name.equals("latitude")) {
      Map<String, Index.Coordinate> coordinates = saveIndex.getCoordinates();
      Index.Coordinate coord;
      String stn;
      float[] lat = new float[locations.size()];
      for (int i = 0; i < locations.size(); i++) {
        stn = (String) locations.get(i);
        coord = (Index.Coordinate) coordinates.get(stn);
        lat[i] = coord.latitude;
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, lat);
    }

    if (name.equals("longitude")) {
      Map<String, Index.Coordinate> coordinates = saveIndex.getCoordinates();
      Index.Coordinate coord;
      String stn;
      float[] lon = new float[locations.size()];
      for (int i = 0; i < locations.size(); i++) {
        stn = (String) locations.get(i);
        coord = (Index.Coordinate) coordinates.get(stn);
        lon[i] = coord.longitude;
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, lon);
    }

    if (name.equals("altitude")) {
      Map<String, Index.Coordinate> coordinates = saveIndex.getCoordinates();
      Index.Coordinate coord;
      String stn;
      int[] alt = new int[locations.size()];
      for (int i = 0; i < locations.size(); i++) {
        stn = (String) locations.get(i);
        coord = (Index.Coordinate) coordinates.get(stn);
        alt[i] = coord.altitude;
      }
      return Array.factory(datatype.getPrimitiveClassType(), shape, alt);
    }
    System.out.println("Don't know anything about " + name);
    return null; // should never get here
  }

  private Array readDataVariableRecord(Variable v2, List section)
      throws IOException, InvalidRangeException {

    //System.out.println( "v2 is a variable in a Structure" );
    // get the index data
    Map<String, String> atts = saveIndex.getGlobalAttributes();
    List<String> locations = saveIndex.getLocations();
    //System.out.println( "locations ="+ locations );
    Map<String, List<Index.BufrObs>> observations = saveIndex.getObservationsMap();
    //ArrayList times = saveIndex.getObsTimes();
    //HashMap timeLocations = saveIndex.getObsLocations();
    //ArrayList parameters = saveIndex.getParameters();

    int[] shape = Range.getShape(section);
    Range range = (Range) section.get(0);
    //System.out.println( "range.first ="+ range.first() +" range.last ="+ range.last() );
    //System.out.println( "shape[0]  ="+ shape[0] );

    Array data = null;
    IndexIterator ii = null;

    // inner structure variables if needed
    ArraySequence ias = null;
    StructureMembers imembers = null;
    StructureMembers.Member member = null;

    // inner structure
    if (v2.getName().startsWith("record.level")) {
      member = imembers.addMember(v2.getShortName(), v2.getDescription(),
          v2.getUnitsString(), v2.getDataType(), v2.getShape());
      imembers = new StructureMembers("level");
      imembers.addMember(member);
      ias = new ArraySequence(imembers, shape[0]);
      int currOb = -1;
      int idx = 0;
      for (int i = 0; i < locations.size(); i++) {
        if (currOb > range.last()) { // done
          break;
        }
        String loc = (String) locations.get(i);
        //System.out.println( "loc =" + loc );
        List<Index.BufrObs> obs = observations.get(loc);
        ucar.bufr.Index.BufrObs bo;
        for (int j = 0; j < obs.size(); j++) {
          currOb++;
          if (currOb < range.first() || currOb > range.last()) {
            continue;
          }
          bo = (ucar.bufr.Index.BufrObs) obs.get(j);
          //System.out.println( currOb +" "+ bo.name +" "+ bo.dim );
          //for (int seq=0; seq < outerLength; seq++)
          //   aseq.setSequenceLength(seq, seqLength);
          ias.setSequenceLength(idx++, bo.dim);
        }
      }
      ias.finish();
      data = member.getDataArray();
      //ii = data.getIndexIterator();
      //System.out.println( "IndexIterator ii ="+ ii );
    } else { // end inner Structure

      // outer structure variables
      //data = Array.factory(v.getDataType().getPrimitiveClassType(), Range.getShape( section ));
      // there should be a better way of allocating storage
      int[] vshape = v2.getShape();
      if (vshape.length != 0 && vshape[0] != -1) {
        //System.out.println( "v.sshape ="+ vshape[0] +" rank ="+ vshape.length );
        vshape[0] *= shape[0];
        data = Array.factory(v2.getDataType().getPrimitiveClassType(), vshape);
      } else {
        data = Array.factory(v2.getDataType().getPrimitiveClassType(), shape);
        //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );
      }
    }
    //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );

    ii = data.getIndexIterator();

    // compressed data is organized by field, not sequential obs
    if ((atts.get("compressdata")).equals("true")) {
      //System.out.println( "compressed data");
      readDataVariableRecordCompressed(atts.get("table"), locations, range,
          observations, v2, ii, data);
      return data;
    }

    // start of data reads for sequential obs
    BufrDataExtractor bde =
        new BufrDataExtractor(raf, atts.get("table"));
    int currOb = -1; // current ob being processed
    for (int i = 0; i < locations.size(); i++) {
      if (currOb > range.last()) { // done
        break;
      }
      String loc = (String) locations.get(i);
      //System.out.println( "loc =" + loc );
      List<Index.BufrObs> obs = observations.get(loc);
      ucar.bufr.Index.BufrObs bo;
      for (int j = 0; j < obs.size(); j++) {
        currOb++;
        if (currOb < range.first() || currOb > range.last()) {
          continue;
        }
        bo = (ucar.bufr.Index.BufrObs) obs.get(j);
        //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
        if (bde.getData(bo.ddsOffset, bo.obsOffset, bo.bitPos, bo.bitBuf)) {
          HashMap bufrdatas = (HashMap) bde.getBufrDatas();
          String bKey = null;
          BufrData bd;
          // inner structure
          if (v2.getName().startsWith("record.level")) {
            //System.out.println( "v2 is a Structure name ="+ v2.getShortName() );
            Attribute a = v2.findAttribute("Bufr_key");
            if (a == null)
              continue;
            bKey = a.getStringValue();
            bd = (BufrData) bufrdatas.get(bKey);
            if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
              ii.setLongNext((bd.getLongData())[0]);
            } else if (bd.isNumeric()) {
              //System.out.println( "v2.getShortName() ="+ v2.getShortName() );
              //System.out.println( "IndexIterator ii ="+ ii );
              float[] bufrdata = bd.getFloatData();
              for (int n = 0; n < bufrdata.length; n++) {
                ii.setFloatNext(bufrdata[n]);
                //System.out.print( bufrdata[n] + ", " );
              }
              //System.out.println();

            } else { // String data
              ii.setObjectNext((bd.getStringData())[0]);
            }
            continue;
///////////////////// end inner structure

          } else if (v2.getShortName().equals("parent_index")) {
            ii.setIntNext(i);
            continue;
          } else {
            Attribute a = v2.findAttribute("Bufr_key");
            if (a == null)
              continue;
            bKey = a.getStringValue();
          }
          //System.out.println( "bKey ="+ bKey );
          bd = (BufrData) bufrdatas.get(bKey);
          //System.out.println( "bd.getName() ="+ bd.getName() );
          if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
            ii.setLongNext((bd.getLongData())[0]);
          } else if (bd.isNumeric()) {
            float[] bufrdata = bd.getFloatData();
            for (int n = 0; n < bufrdata.length; n++) {
              ii.setFloatNext(bufrdata[n]);
            }
          } else { // String data
            ii.setObjectNext((bd.getStringData())[0]);
          }
        } else {
          System.out.println("getData failed for loc =" + loc);
        }
      }
    } // end looping over locations

    /*
    System.out.println( "data is returned" );
    ii = data.getIndexIterator();
    System.out.println( v2.getName() );
    for( ; ii.hasNext(); ) {
       System.out.print( ii.getLongNext()  +", " );
    }
    System.out.println();
    */
    return data;
  } // end readDataVariableRecord

  public void readDataVariableRecordCompressed(String table, List<String> locations,
                                               Range range, Map<String, List<Index.BufrObs>> observations, Variable v2,
                                               IndexIterator ii,
                                               Array data)
      throws IOException, InvalidRangeException {
    // start of data reads
    BufrDataExtractor bde = new BufrDataExtractor(raf, table);
    int currOb = -1; // current ob being processed
    int numObs = -1; // number of obs in this data block
    for (int i = 0; i < locations.size(); i++) {
      if (currOb > range.last()) { // done
        break;
      }
      String loc = (String) locations.get(i);
      //System.out.println( "loc =" + loc );
      List<Index.BufrObs> obs = observations.get(loc);
      ucar.bufr.Index.BufrObs bo;
      int end = -1;
      for (int j = 0; j < obs.size(); j++) {
        if (currOb > range.last()) { // done
          break;
        }
        bo = (ucar.bufr.Index.BufrObs) obs.get(j);
        //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
        // for compressed data bo.bitBuf contains number of obs in data
        if ((currOb + bo.bitBuf) > range.last()) {
          end = range.last() - currOb;
          //System.out.println( "end ="+ end );
        } else {
          end = -1;
        }
        currOb += bo.bitBuf; // bo.bitBuf contains number of obs in data
        //System.out.println( "currOb ="+ currOb );
        if (currOb < range.first()) {
          continue;
        }
        numObs = bo.bitBuf;
        // compressed data packs a number of Obs into one data block
        // bo.bitBuf is always 0 for compressed data
        if (bde.getData(bo.ddsOffset, bo.obsOffset, bo.bitPos, 0)) {
          HashMap bufrdatas = (HashMap) bde.getBufrDatas();
          String bKey = null;
          BufrData bd;
          if (v2.getShortName().equals("parent_index")) {
            ii.setIntNext(i);
            continue;
          } else {
            Attribute a = v2.findAttribute("Bufr_key");
            if (a == null)
              continue;
            bKey = a.getStringValue();
          }
          bd = (BufrData) bufrdatas.get(bKey);
          if (bd.getName().equals("time_nominal") || bd.getName().equals("time_observation")) {
            long[] bufrdata = bd.getLongData();
            for (int n = 0; n < bufrdata.length && n != end; n++) {
              ii.setLongNext(bufrdata[n]);
              //System.out.println( "time_observation ="+ bufrdata[n] );
            }
          } else if (bd.isNumeric()) {
            float[] bufrdata = bd.getFloatData();
            //System.out.println( "bufrdata.length ="+ bufrdata.length
            //+" = "+ numObs );
            for (int m = 0; m < numObs && m != end; m++) {
              for (int n = m; n < bufrdata.length; n += numObs) {
                ii.setFloatNext(bufrdata[n]);
              }
            }
          } else { // String data
            String[] bufrdata = bd.getStringData();
            for (int n = 0; n < bufrdata.length && n != end; n++) {
              ii.setObjectNext(bufrdata[n]);
              //System.out.println( "String ="+ bufrdata[n] );
            }
          }
        } else {
          System.out.println("getData failed for loc =" + loc);
        }
      }
    }
  } // end readDataVariableRecordCompressed

  public Array readNestedData(Variable v2, Section section)
      throws IOException, InvalidRangeException {
    //System.out.println( "readNestedData variable ="+ v2.getName() );
    //throw new UnsupportedOperationException("Bufr IOSP does not support nested variables");
    if (v2.getName().startsWith("record.")) {
      return readDataVariableRecord(v2, section.getRanges());
    } else {
      return readDataVariable(v2.getName(), v2.getDataType(), section.getRanges());
    }
  }

  protected void _readData(long DDSoffset, long obsOffset, int bitPos, int bitBuf) throws IOException {
    dataReader.getData(DDSoffset, obsOffset, bitPos, bitBuf);
  }

  public void close() throws IOException {
    raf.close();
  }

  public String getDetailInfo() {
    return parseInfo.toString();
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
    System.out.println(ncf.toString());

    Structure s = (Structure) ncf.findVariable("obsRecord");
    StructureData sdata = s.readStructure(0);
    PrintWriter pw = new PrintWriter(System.out);
    NCdumpW.printStructureData(pw, sdata);

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
