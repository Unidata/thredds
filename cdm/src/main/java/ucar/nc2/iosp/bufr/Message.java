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

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.bufr.tables.CommonCodeTables;
import ucar.nc2.iosp.bufr.tables.TableB;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Encapsolates a complete BUFR message.
 * A message has a DataDescriptor and one or more "datasets" aka "data subsets" aka "observations" aka "obs".
 *
 * @author caron
 * @since May 9, 2008
 */
public class Message {
  private static final Pattern wmoPattern = Pattern.compile(".*([IJ]..... ....) .*");

  public BufrIndicatorSection is;
  public BufrIdentificationSection ids;
  public BufrDataDescriptionSection dds;
  public BufrDataSection dataSection;

  private RandomAccessFile raf;
  private TableLookup lookup;
  private DataDescriptor root;
  private GregorianCalendar cal;

  private String header; // wmo header
  private long startPos; // starting pos in raf
  private byte[] raw; // raw bytes

  // bit counting
  BitCounterUncompressed[] counterDatasets; // uncompressed: one for each dataset
  BitCounterCompressed[] counterFlds; // compressed: one for each field
  int msg_nbits;

  public Message(RandomAccessFile raf, BufrIndicatorSection is, BufrIdentificationSection ids, BufrDataDescriptionSection dds,
                 BufrDataSection dataSection, GregorianCalendar cal) throws IOException {
    this.raf = raf;
    this.is = is;
    this.ids = ids;
    this.dds = dds;
    this.dataSection = dataSection;
    this.cal = cal;
    lookup = new TableLookup(ids);
  }

  public void close() throws IOException {
    if (raf != null) raf.close();
  }

  /**
   * Get number of datasets in this message.
   *
   * @return number of datasets in this message
   */
  public int getNumberDatasets() {
    return dds.getNumberDatasets();
  }

  public String getCategoryName() {
    return CommonCodeTables.getDataCategory( ids.getCategory());
  }

  public String getCategoryNo() {
    String result = ids.getCategory() + "." + ids.getSubCategory();
    if (ids.getLocalSubCategory() >= 0) result += "." + ids.getLocalSubCategory();
    return result;
  }

  public String getCenterName() {
    String name = CommonCodeTables.getCenterName(ids.getCenterId());
    String subname = CommonCodeTables.getSubCenterName(ids.getCenterId(), ids.getSubCenterId());
    if (subname != null) name = name +" / " + subname;
    return ids.getCenterId() + "." + ids.getSubCenterId() + " (" + name + ")";
  }

  public String getCenterNo() {
    return ids.getCenterId() + "." + ids.getSubCenterId();
  }

  public String getTableName() {
    return ids.getMasterTableId() + "." + ids.getMasterTableVersion() + "." + ids.getLocalTableVersion();
  }

  public final Date getReferenceTime() {
    return ids.getReferenceTime( cal);
  }

  ///////////////////////////////////////////////////////////////////////////

  // the WMO header is in here somewhere when the message comes over the IDD
  public void setHeader(String header) {
    this.header = header;
  }

  public String getHeader() {
    return header;
  }

  // where the message starts in the file
  public void setStartPos(long startPos) {
    this.startPos = startPos;
  }

  public long getStartPos() {
    return startPos;
  }

  public void setRawBytes(byte[] raw) {
    this.raw = raw;
  }

  public byte[] getRawBytes() {
    return raw;
  }

  public String extractWMO() {
    Matcher matcher = wmoPattern.matcher(header);
    if (!matcher.matches()) {
      return "";
    }
    return matcher.group(1);
  }

  /**
   * Get the byte length of the entire BUFR record.
   *
   * @return length in bytes of BUFR record
   */
  public long getMessageSize() {
    return is.getBufrLength();
  }

  /**
   * Get the root of the DataDescriptor tree.
   *
   * @return root DataDescriptor
   * @throws IOException on read error
   */
  public DataDescriptor getRootDataDescriptor() throws IOException {
    if (root == null)
      root = new DataDescriptorTreeConstructor().factory(lookup, dds);
    return root;
  }

  public boolean usesLocalTable() throws IOException {
    DataDescriptor root = getRootDataDescriptor();
    return usesLocalTable(root);
  }

  private boolean usesLocalTable(DataDescriptor dds) throws IOException {
    for (DataDescriptor key : dds.getSubKeys()) {
      if (key.isLocal()) return true;
      if ((key.getSubKeys() != null) && usesLocalTable(key)) return true;
    }
    return false;
  }

  /**
   * Check if all descriptors were found in the tables.
   *
   * @return true if all dds were found.
   * @throws IOException on read error
   */
  public boolean isTablesComplete() throws IOException {
    DataDescriptor root = getRootDataDescriptor();
    return !root.isBad;
  }

  public void showMissingFields(Formatter out) throws IOException {
    showMissingFields(dds.getDataDescriptors(), out);
  }

  private void showMissingFields(List<Short> ddsList, Formatter out) throws IOException {
    for (short fxy : ddsList) {
      int f = (fxy & 0xC000) >> 14;
      if (f == 3) {
        List<Short> sublist = lookup.getDescriptorsTableD(fxy);
        if (sublist == null) out.format("%s, ", Descriptor.makeString(fxy));
        else showMissingFields(sublist, out);

      } else if (f == 0) {  // skip the 2- operators for now
        TableB.Descriptor b = lookup.getDescriptorTableB(fxy);
        if (b == null) out.format("%s, ", Descriptor.makeString(fxy));
      }
    }
  }

  public TableLookup getTableLookup() {
    return lookup;
  }

  ////////////////////////////////////////////////////////////////////////
  // bit counting

  public boolean isBitCountOk() throws IOException {
    getRootDataDescriptor(); // make sure root is calculated
    getTotalBits(); // make sure buts are counted
    //int nbitsGiven = 8 * (dataSection.getDataLength() - 4);
    int nbytesCounted = getCountedDataBytes();
    int nbytesGiven = dataSection.getDataLength();
    return Math.abs(nbytesCounted - nbytesGiven) <= 1; // radiosondes dataLen not even number of bytes
  }

  public int getCountedDataBytes() {
    int msg_nbytes = msg_nbits / 8;
    if (msg_nbits % 8 != 0) msg_nbytes++;
    msg_nbytes += 4;
    if (msg_nbytes % 2 != 0) msg_nbytes++; // LOOK seems to be violated by some messages
    return msg_nbytes;
  }

  public int getCountedDataBits() {
    return msg_nbits;
  }


  /**
   * Get the offset of this obs from the start of the message data.
   * Use only for non compressed data
   *
   * @param obsOffsetInMessage index of obs in the message
   * @return offset in bits
   *         <p/>
   *         public int getBitOffset(int obsOffsetInMessage) {
   *         if (dds.isCompressed())
   *         throw new IllegalArgumentException("cant call BufrMessage.getBitOffset() on compressed message");
   *         <p/>
   *         if (!root.isVarLength)
   *         return root.total_nbits * obsOffsetInMessage;
   *         <p/>
   *         getTotalBits(); // make sure its been set
   *         return nestedTableCounter[obsOffsetInMessage].getStartBit();
   *         }
   */

  public BitCounterUncompressed getBitCounterUncompressed(int obsOffsetInMessage) {
    if (dds.isCompressed())
      throw new IllegalArgumentException("cant call BufrMessage.getBitOffset() on compressed message");

    calcTotalBits(null); // make sure its been set
    return counterDatasets[obsOffsetInMessage];
  }

  /**
   * This is the total number of bits taken by the data in the data section of the message.
   * This is the counted number.
   *
   * @return total number of bits
   */
  public int getTotalBits() {
    if (msg_nbits == 0) calcTotalBits(null);
    return msg_nbits;
  }

  // sets msg_nbits as side-effect
  public int calcTotalBits(Formatter out) {
    try {
      if (!dds.isCompressed()) {
        MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
        reader.readData(null, this, raf, null, false, out);
      } else {
        MessageCompressedDataReader reader = new MessageCompressedDataReader();
        reader.readData(null, this, raf, null, out);
      }
    } catch (IOException ioe) {
      return 0;
    }
    return msg_nbits;

    /* boolean compressed = dds.isCompressed();

   try {
     getRootDataDescriptor(); // make sure root has been done

     if (compressed)
       return countBitsCompressed(out);  // compressed
     else
       return countBitsUncompressed(out); // varLength

   } catch (IOException e) {
     e.printStackTrace();
     throw new RuntimeException(e.getMessage());
   } */
  }

  /* count the bits in an uncompressed message
  private int countBitsUncompressed(Formatter out) throws IOException {
    BitReader reader = new BitReader(raf, dataSection.getDataPos() + 4);

    int n = getNumberDatasets();
    counterDatasets = new BitCounterUncompressed[n]; // one for each dataset
    msg_nbits = 0;
    // count the bits in each obs
    for (int i = 0; i < n; i++) {
      if (out != null) out.format("Count bits in observation %d\n", i);
      // the top table always has exactly one "row", since we are working with a single obs
      counterDatasets[i] = new BitCounterUncompressed(root, 1, 0);
      String where = (out == null) ? null : "obs " + i;
      countBitsUncompressed(out, reader, root.subKeys, counterDatasets[i], 0, where, 0, 1);
      msg_nbits += counterDatasets[i].countBits(msg_nbits);
    }

    return msg_nbits;
  }

  /*
   * count the bits in one row of a "nested table", defined by List<DataDescriptor> dkeys.
   *
   * @param out    optional debug poutput, may be null
   * @param reader read data with this
   * @param dkeys  defines the "nested table row"
   * @param tc     put the results here
   * @param row    which row of the table
   * @param fldno track fldno to compare with EU output

   * @throws IOException on read error
   *
  private int countBitsUncompressed(Formatter out, BitReader reader, List<DataDescriptor> dkeys, BitCounterUncompressed tc,
          int row, String where, int indent, int fldno) throws IOException {

    for (DataDescriptor dkey : dkeys) {
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      // sequence
      if (dkey.replication == 0) {

        int count = (int) reader.bits2UInt(dkey.replicationCountSize);
        if (out != null) out.format("%4d delayed replication %d %n", fldno++, count);
        //if (count == 0) continue;

        if ((out != null) && (count > 0))  {
          out.format("%4d %s read sequence %s count= %d bitSize=%d start at=0x%x %n",
                  fldno++, blank(indent), dkey.getFxyName(), count, dkey.replicationCountSize, reader.getPos());
        }

        BitCounterUncompressed nested = tc.makeNested(dkey, count, row, dkey.replicationCountSize);
        for (int i = 0; i < count; i++) {
          String whereNested = null;
          if (out != null) { // bitCount output
            out.format("%s read row %d (seq %s) of %s %n", blank(indent), i, dkey.getFxyName(), where);
            whereNested = (out == null) ? null : where + " row " + i + "( seq " + dkey.getFxyName() + ")";
          }

          fldno = countBitsUncompressed(out, reader, dkey.subKeys, nested, i, whereNested, indent + 2, fldno);
        }

        continue;
      }

      // compound
      if (dkey.type == 3) {
        BitCounterUncompressed nested = tc.makeNested(dkey, dkey.replication, row, 0);
        if (out != null)
          out.format("%4d %s read structure %s count= %d\n",
                  fldno++, blank(indent), dkey.getFxyName(), dkey.replication);

        for (int i = 0; i < dkey.replication; i++) {
          if (out != null)
            out.format("%s read row %d (struct %s) of %s %n", blank(indent), i, dkey.getFxyName(), where);

          String whereNested = (out == null) ? null : where + " row " + i + "( struct " + dkey.getFxyName() + ")";
          fldno = countBitsUncompressed(out, reader, dkey.subKeys, nested, i, whereNested, indent + 2, fldno);
        }
        continue;
      }

      // char data
      if (dkey.type == 1) {
        String val = new String(readCharData(dkey, reader));
        if (out != null)
          out.format("%4d %s read char %s bitWidth=%d end at= 0x%x val=%s\n",
                  fldno++, blank(indent), dkey.getFxyName(), dkey.bitWidth, reader.getPos(), val);
        continue;
      }

      // otherwise read a number
      long val = reader.bits2UInt(dkey.bitWidth);
      if (out != null)
        out.format("%4d %s read %s bitWidth=%d end at= 0x%x raw=%d convert=%f\n",
                fldno++, blank(indent), dkey.getFxyName(), dkey.bitWidth, reader.getPos(), val, dkey.convert(val));
    }

    return fldno;
  }

  private String blanks = "                      ";

  private String blank(int indent) {
    return blanks.substring(0, indent + 1);
  }

  private byte[] readCharData(DataDescriptor dkey, BitReader reader) throws IOException {
    int nchars = dkey.getByteWidthCDM();
    byte[] b = new byte[nchars];
    for (int i = 0; i < nchars; i++)
      b[i] = (byte) reader.bits2UInt(8);
    return b;
  }

  /* private Number readNumericData(DataDescriptor dkey, BitReader reader) throws IOException {
    int val = reader.bits2UInt(dkey.bitWidth);
    if (dkey.scale == 0)
      return val + dkey.refVal; // remain an integer
    return convert(dkey, val);
  } */

  /* private float convert(DataDescriptor dkey, int raw) {
    // bpacked = (value * 10^scale - refVal)
    // value = (bpacked + refVal) / 10^scale
    float scale = (float) Math.pow(10.0, -dkey.scale);
    float fval = (raw + dkey.refVal);
    return scale * fval;
  }

  // count the bits in a compressed message
  private int countBitsCompressed(Formatter out) throws IOException {
    BitReader reader = new BitReader(raf, dataSection.getDataPos() + 4);
    counterFlds = new BitCounterCompressed[root.subKeys.size()];
    countBitsCompressed(out, reader, counterFlds, 0, getNumberDatasets(), root);

    msg_nbits = 0;
    for (BitCounterCompressed counter : counterFlds)
      if (counter != null) msg_nbits += counter.getTotalBits();
    return msg_nbits;
  }

  public void showCounters(Formatter out) throws IOException {
    out.format("   total   offset size name%n");    
    for (BitCounterCompressed counter : counterFlds)
      counter.show(out, 0);
  }


  public BitCounterCompressed[] getBitCounterCompressed() throws IOException {
    if (counterFlds == null)
      countBitsCompressed(null);
    return counterFlds;
  }

  /* Example msg has 60 obs in it. each obs has a struct(18):
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
    where comp(inner,60) = [comp(f1,60), comp(f2,60)] * 18
    where comp = compressed structure = (minValue, dataWidth, n * dataWidth)

    compSequence = [m, 6, comp(f1,60), comp(f2,60), ...] * m

    see p 11 of "definition" document
   */
  /*
   * Count bits in a compressed message
   * @param out optional debug output, may be null
   * @param reader read from here
   * @param counters one for each field
   * @param bitOffset starting offset
   * @param n number of datasets (obs)
   * @param parent the parent descriptor
   * @return the ending bitOffset
   * @throws IOException on I/O error
   *
  private int countBitsCompressed(Formatter out, BitReader reader, BitCounterCompressed[] counters, int bitOffset, int n, DataDescriptor parent) throws IOException {

    for (int fldidx = 0; fldidx < parent.getSubKeys().size(); fldidx++) {
      DataDescriptor dkey = parent.getSubKeys().get(fldidx);
      if (!dkey.isOkForVariable()) // misc skip
        continue;

      BitCounterCompressed counter = new BitCounterCompressed(dkey, n, bitOffset);
      counters[fldidx] = counter;

      // sequence : probably works the same way as structure
      if (dkey.replication == 0) {

        // except that theres the count stored first
       reader.setBitOffset(bitOffset);
       int count = (int) reader.bits2UInt(dkey.replicationCountSize);
       bitOffset += dkey.replicationCountSize;

       int extra = (int) reader.bits2UInt(6);
       // System.out.printf("EXTRA bits %d at %d %n", extra, bitOffset);
       if (null != out)
          out.format("--sequence %s bitOffset=%d replication=%s %n", dkey.getFxyName(), bitOffset, count);
        bitOffset += 6; // LOOK seems to be an extra 6 bits. not yet getting counted

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

      reader.setBitOffset(bitOffset);
      long dataMin = reader.bits2UInt(dkey.bitWidth);
      int dataWidth = (int) reader.bits2UInt(6);  // increment data width - always in 6 bits, so max is 2^6 = 64
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
              long val = reader.bits2UInt(dataWidth);
              if (val == BufrNumbers.missingValue(dataWidth)) {// is this a missing value ??
                if (showData) out.format(" %d (MISSING)", val);
              } else {
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

  boolean showData = true;  */

  ///////////////////////////////////////////////////////////////////

  /**
   * Get values of selected fields. Must be in the top row (not nested).
   * Used by BerkeleyDBIndexer.
   * 
   * @param indexFlds index of desired field in the dds
   * @return field values as Object[nobs][nfields]
   * @throws IOException on read error
   *
  public Object[][] readValues(List<Integer> indexFlds) throws IOException {
    getRootDataDescriptor();
    BitReader reader = new BitReader(raf, dataSection.getDataPos() + 4);

    int n = getNumberDatasets();
    Object[][] result = new Object[n][indexFlds.size()];

    if (dds.isCompressed()) {
      readDataCompressed(reader, indexFlds, result);
    } else {
      readDataUncompressed(reader, indexFlds, result);
    }

    return result;
  }

  // LOOK : cant use fxy to match, since may be multiple fields with same fxy
  // better to preprocess the dds and pass in the actual fldidx we want.
  private void readDataCompressed(BitReader reader, List<Integer> indexFlds, Object[][] result) throws IOException {
    int nobs = getNumberDatasets();
    BitCounterCompressed[] bitCounter = getBitCounterCompressed();
    int count = 0;

    for (int index : indexFlds) {
      if (index < 0) {
        count++;
        continue;
      }

      DataDescriptor dkey = root.getSubKeys().get(index);
      BitCounterCompressed counter = bitCounter[index];
      reader.setBitOffset(counter.getStartingBitPos());

      // char data special case
      if (dkey.type == 1) {
        int n = dkey.bitWidth / 8;
        byte[] defValue = new byte[n];
        for (int i = 0; i < n; i++)
          defValue[i] = (byte) reader.bits2UInt(8);

        int dataWidth = reader.bits2UInt(6); // incremental data width
        if (dataWidth == 0) { // use the def value
          for (int obs = 0; obs < nobs; obs++)
            result[obs][count] = defValue;

        } else { // read the incremental value
          int nt = Math.min(n, dataWidth);
          for (int obs = 0; obs < nobs; obs++) {
            byte[] bval = new byte[nt];
            for (int i = 0; i < nt; i++) {
              int cval = reader.bits2UInt(8);
              if (cval < 32 || cval > 126) continue; // skip non-printable ascii KLUDGE!
              bval[i] = (byte) cval;
            }
            result[obs][count] = bval;
          }
        }
        count++;
        continue;
      }

      // numeric fields
      int minValue = reader.bits2UInt(dkey.bitWidth); // read min value
      int dataWidth = reader.bits2UInt(6); // incremental data woidth

      for (int obs = 0; obs < nobs; obs++) {
        int value = (dataWidth > 0) ? minValue + reader.bits2UInt(dataWidth) : minValue; // if dataWidth == 0, just use min value
        if (dkey.scale == 0)
          result[obs][count] = value + dkey.refVal; // remain an integer
        else
          result[obs][count] = convert(dkey, value);
      } // loop over obs

      count++;
    } // loop over fields
  }

  private void readDataUncompressed(BitReader reader, List<Integer> indexFlds, Object[][] result) throws IOException {
    List<DataDescriptor> dkeyList = root.getSubKeys();

    // loop over each obs
    for (int obs = 0; obs < getNumberDatasets(); obs++) {
      BitCounterUncompressed bitCounter = getBitCounterUncompressed(obs);
      int bitOffset = 0;
      for (int index=0; index < dkeyList.size(); index++) {
        DataDescriptor dkey = dkeyList.get(index);
        int count = indexFlds.indexOf(index);
        if (count >= 0) {
          reader.setBitOffset(bitCounter.getStartBit(0) + bitOffset);
          if (dkey.type == 0) { // numeric
            result[obs][count] = readNumericData(dkey, reader);
          } else if (dkey.type == 1) { // char
            result[obs][count] = readCharData(dkey, reader);
          }
        } // wanted field

        bitOffset += dkey.getBitWidth();
      } // loop over dkeys
    } // loop over obs
  } */

  ///////////////////////////////////////////////////////////////////

  /**
   * Override hashcode to be consistent with equals.
   *
   * @return the hash code of dds.getDescriptors()
   */
  public int hashCode() {
    int result = 17;
    result += 37 * result + dds.getDataDescriptors().hashCode();
    result += 37 * result + ids.getCenterId();
    //result += 37 * result + ids.getSubCenter_id();
    result += 37 * result + ids.getCategory();
    result += 37 * result + ids.getSubCategory();
    return result;
  }

  /**
   * BufrMessage is equal if they have the same dds.
   *
   * @param obj other BufrMessage
   * @return true if equals
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof Message)) return false;
    Message o = (Message) obj;
    if (!dds.getDataDescriptors().equals(o.dds.getDataDescriptors())) return false;
    if (ids.getCenterId() != o.ids.getCenterId()) return false;
    //if (ids.getSubCenter_id() != o.ids.getSubCenter_id()) return false;
    if (ids.getCategory() != o.ids.getCategory()) return false;
    if (ids.getSubCategory() != o.ids.getSubCategory()) return false;
    return true;
  }

  ////////////////////////////////////////////////////////////////////
  // perhaps move this into a helper class - started from ucar.bufr.Dump

  public void dump(Formatter out) throws IOException {

    int listHash = dds.getDataDescriptors().hashCode();
    out.format(" BUFR edition %d time= %s wmoHeader=%s hash=[0x%x] listHash=[0x%x] (%d) %n",
            is.getBufrEdition(), getReferenceTime(), getHeader(), hashCode(), listHash, listHash);
    out.format("   Category= %s %n", getCategoryFullName());
    out.format("   Center= %s %n", getCenterName());
    out.format("   Table= %s %n", getTableName());
    out.format("    Table B= wmoTable= %s localTable= %s mode=%s%n", lookup.getWmoTableBName(), lookup.getLocalTableBName(), lookup.getMode());
    out.format("    Table D= wmoTable= %s localTable= %s%n", lookup.getWmoTableDName(), lookup.getLocalTableDName());

    out.format("  DDS nsubsets=%d type=0x%x isObs=%b isCompressed=%b\n", dds.getNumberDatasets(), dds.getDataType(),
            dds.isObserved(), dds.isCompressed());

    long startPos = is.getStartPos();
    long startData = dataSection.getDataPos();
    out.format("  startPos=%d len=%d endPos=%d dataStart=%d dataLen=%d dataEnd=%d %n",
            startPos, is.getBufrLength(), (startPos + is.getBufrLength()),
            startData, dataSection.getDataLength(), startData +dataSection.getDataLength());

    dumpDesc(out, dds.getDataDescriptors(), lookup, 4);

    out.format("%n  CDM Nested Table=\n");
    DataDescriptor root = new DataDescriptorTreeConstructor().factory(lookup, dds);
    dumpKeys(out, root, 4);

    /* int nbits = m.getTotalBits();
    int nbytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
    out.format("  totalBits = %d (%d bytes) outputBytes= %d isVarLen=%s isCompressed=%s\n\n",
            nbits, nbytes, root.getByteWidthCDM(), root.isVarLength(), m.dds.isCompressed()); */
  }

  private void dumpDesc(Formatter out, List<Short> desc, TableLookup table, int indent) {
    if (desc == null) return;

    for (Short fxy : desc) {
      for (int i = 0; i < indent; i++) out.format(" ");
      Descriptor.show(out, fxy, table);
      out.format("%n");
      int f = (fxy & 0xC000) >> 14;
      if (f == 3) {
        List<Short> sublist = table.getDescriptorsTableD(fxy);
        dumpDesc(out, sublist, table, indent + 2);
      }
    }
  }

  private void dumpKeys(Formatter out, DataDescriptor tree, int indent) {
    for (DataDescriptor key : tree.subKeys) {
      for (int i = 0; i < indent; i++) out.format(" ");
      out.format("%s\n", key);
      if (key.getSubKeys() != null)
        dumpKeys(out, key, indent + 2);
    }
  }

  public String getCategoryFullName() throws IOException {
    String catName = getCategoryName();
    String subcatName = CommonCodeTables.getDataSubcategoy(ids.getCategory(), ids.getSubCategory());

    if (subcatName != null)
      return getCategoryNo() + "="+ catName + " / " + subcatName;
    else
      return getCategoryNo() + "="+ catName;
  }

  public void dumpHeader(Formatter out) {

    out.format(" BUFR edition %d time= %s wmoHeader=%s %n", is.getBufrEdition(), getReferenceTime(), getHeader());
    out.format("   Category= %d %s %s %n", ids.getCategory(), getCategoryName(), getCategoryNo());
    out.format("   Center= %s %s %n", getCenterName(), getCenterNo());
    out.format("   Table= %d.%d local= %d wmoTables= %s,%s localTables= %s,%s %n",
            ids.getMasterTableId(), ids.getMasterTableVersion(), ids.getLocalTableVersion(),
            lookup.getWmoTableBName(),lookup.getWmoTableDName(),lookup.getLocalTableBName(),lookup.getLocalTableDName());

    out.format("  DDS nsubsets=%d type=0x%x isObs=%b isCompressed=%b\n", dds.getNumberDatasets(), dds.getDataType(),
            dds.isObserved(), dds.isCompressed());
  }

  public void dumpHeaderShort(Formatter out) {
    out.format(" %s, Cat= %s, Center= %s (%s), Table= %d.%d.%d %n", getHeader(),
            getCategoryName(), getCenterName(), getCenterNo(),
            ids.getMasterTableId(), ids.getMasterTableVersion(), ids.getLocalTableVersion());
  }

}
