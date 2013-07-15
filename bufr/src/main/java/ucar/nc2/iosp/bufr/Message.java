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

import ucar.nc2.iosp.bufr.tables.TableA;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.bufr.tables.TableB;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
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

  private String header; // wmo header
  private long startPos; // starting pos in raf
  private byte[] raw; // raw bytes

  // bit counting
  BitCounterUncompressed[] counterDatasets; // uncompressed: one for each dataset
  BitCounterCompressed[] counterFlds; // compressed: one for each field
  int msg_nbits;

  public Message(RandomAccessFile raf, BufrIndicatorSection is, BufrIdentificationSection ids, BufrDataDescriptionSection dds,
                 BufrDataSection dataSection) throws IOException {
    this.raf = raf;
    this.is = is;
    this.ids = ids;
    this.dds = dds;
    this.dataSection = dataSection;
    lookup = new TableLookup(ids);
  }

  public void setTableLookup(TableLookup lookup) {
    this.lookup = lookup;
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
    return TableA.getDataCategory(ids.getCategory());
  }

  public String getCategoryNo() {
    String result = ids.getCategory() + "." + ids.getSubCategory();
    if (ids.getLocalSubCategory() >= 0) result += "." + ids.getLocalSubCategory();
    return result;
  }

  public String getCenterName() {
    String name = CommonCodeTable.getCenterNameBufr(ids.getCenterId(), is.getBufrEdition());
    String subname = CommonCodeTable.getSubCenterName(ids.getCenterId(), ids.getSubCenterId());
    if (subname != null) name = name +" / " + subname;
    return ids.getCenterId() + "." + ids.getSubCenterId() + " (" + name + ")";
  }

  public String getCenterNo() {
    return ids.getCenterId() + "." + ids.getSubCenterId();
  }

  public String getTableName() {
    return ids.getMasterTableId() + "." + ids.getMasterTableVersion() + "." + ids.getLocalTableVersion();
  }

  public CalendarDate getReferenceTime() {
    return ids.getReferenceTime();
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
   * Check if this message contains a BUFR table
   *
   * @return true if message contains a BUFR table
   * @throws IOException on read error
   */
  public boolean containsBufrTable() throws IOException {
    for (Short key : dds.getDataDescriptors()) {
      if (Descriptor.isBufrTable(key))
        return true;
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
    getTotalBits(); // make sure bits are counted
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
  }

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
    String subcatName = CommonCodeTable.getDataSubcategoy(ids.getCategory(), ids.getSubCategory());

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
