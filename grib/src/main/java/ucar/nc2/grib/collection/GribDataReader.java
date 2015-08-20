/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.collection;


import net.jcip.annotations.Immutable;
import ucar.ma2.*;
import ucar.nc2.grib.*;

import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.*;

/**
 * Grib Data Reader.
 * Split from GribIosp, so can be used by GribCoverage.
 *
 * @author caron
 * @since 4/6/11
 */
@Immutable
public abstract class GribDataReader {

  public static GribDataReader factory(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
    if (gribCollection.isGrib1)
      return new Grib1DataReader(gribCollection, vindex);
    else
      return new Grib2DataReader(gribCollection, vindex);
  }

  protected abstract float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException;
  protected abstract void show(RandomAccessFile rafData, long pos) throws IOException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.VariableIndex vindex;
  private List<DataRecord> records = new ArrayList<>();

  protected GribDataReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
    this.gribCollection = gribCollection;
    this.vindex = vindex;
  }

  public Array readData(Section want, int[] saShape) throws IOException, InvalidRangeException {
    System.out.printf("%nGribDataReader.want=%n%s%n",want.show());
    if (vindex instanceof PartitionCollectionImmutable.VariableIndexPartitioned)
      return readDataFromPartition((PartitionCollectionImmutable.VariableIndexPartitioned) vindex, want, saShape);
    else
      return readDataFromCollection(vindex, want, saShape);
  }

  private Array readDataFromCollection(GribCollectionImmutable.VariableIndex vindex, Section want, int[] saShape) throws IOException, InvalidRangeException {
    // first time, read records and keep in memory
    vindex.readRecords();

    int sectionLen = want.getRank();
    Section sectionWanted = want.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(saShape);
    int[] indexWanted = new int[sectionLen - 2];                              // place to put the iterator result

    // collect all the records that need to be read
    int count = 0;
    while (iterWanted.hasNext()) {
      int sourceIndex = iterWanted.next(indexWanted);
      addRecord(sourceIndex, count++);
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(want);
    read(dataReceiver);
    return dataReceiver.getArray();
  }

  private Array readDataFromPartition(PartitionCollectionImmutable.VariableIndexPartitioned vindexP, Section section, int[] saShape) throws IOException, InvalidRangeException {

    int sectionLen = section.getRank();
    Section sectionWanted = section.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(saShape);  // iterator over wanted indices in vindexP
    int[] indexWanted = new int[sectionLen - 2];                              // place to put the iterator result
    int[] useIndex = indexWanted;

    // collect all the records that need to be read
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted array

      // for 1D TP, second index is implictly 0
      if (vindexP.getType() == GribCollectionImmutable.Type.TP) {
        int[] indexReallyWanted = new int[indexWanted.length+1];
        indexReallyWanted[0] = indexWanted[0];
        indexReallyWanted[1] = 0;
        System.arraycopy(indexWanted, 1, indexReallyWanted, 2, indexWanted.length-1);
        useIndex = indexReallyWanted;
      }

      PartitionCollectionImmutable.DataRecord record = vindexP.getDataRecord(useIndex);
      if (record == null) {
        if (GribIosp.debugRead) System.out.printf("readDataFromPartition missing data%n");
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      addRecordPartitioned(record);
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(section);
    readPartitioned(dataReceiver);

    return dataReceiver.getArray();
  }

  /**
   * Identify the record that you want to read
   * @param sourceIndex 1d index into the vindex.sparseArray
   * @param resultIndex 1d index into the result array - used by the dataReciever
   */
  private void addRecord(int sourceIndex, int resultIndex) {
    GribCollectionImmutable.Record record = vindex.getRecordAt(sourceIndex);
    if (GribIosp.debugRead) {
      System.out.printf("GribIosp debugRead sourceIndex=%d resultIndex=%d record is null=%s%n", sourceIndex, resultIndex, record == null);
    }
    if (record != null)
      records.add( new DataRecord(resultIndex, record.fileno, record.pos, record.bmsPos, record.scanMode, vindex.group.getGdsHorizCoordSys()));
  }

  /**
   * Read all of the data records that have been added.
   * The full (x,y) record is read, the reciever will subset the (x, y) as needed.
   * @param dataReceiver send data here.
   * @throws IOException
   */
  private void read(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    int currFile = -1;
    RandomAccessFile rafData = null;
    try {
      for (DataRecord dr : records) {
        if (GribIosp.debugIndexOnly || GribIosp.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          if (GribIosp.debugIndexOnlyShow) dr.show(gribCollection);
          dataReceiver.setDataToZero();
          continue;
        }

        if (dr.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = gribCollection.getDataRaf(dr.fileno);
          currFile = dr.fileno;
        }

        if (dr.dataPos == GribCollectionMutable.MISSING_RECORD) continue;

        if (GribIosp.debugRead && rafData != null) { // for validation
          show(rafData, dr.dataPos);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null) rafData.close();  // make sure its closed even on exception
    }
  }

  private void addRecordPartitioned(PartitionCollectionImmutable.DataRecord dr) {
    if (dr != null) records.add(dr);
  }

  private void readPartitioned(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    PartitionCollectionImmutable.DataRecord lastRecord = null;
    RandomAccessFile rafData = null;
    try {

      for (DataRecord dr : records) {
        PartitionCollectionImmutable.DataRecord drp = (PartitionCollectionImmutable.DataRecord) dr;
        if (GribIosp.debugIndexOnly || GribIosp.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          if (GribIosp.debugIndexOnlyShow) drp.show();
          dataReceiver.setDataToZero();
          continue;
        }

        if ((rafData == null) || !drp.usesSameFile(lastRecord)) {
          if (rafData != null) rafData.close();
          rafData = drp.usePartition.getRaf(drp.partno, dr.fileno);
        }
        lastRecord = drp;

        if (dr.dataPos == GribCollectionMutable.MISSING_RECORD) continue;

        if (GribIosp.debugRead) { // for validation
          show(rafData, dr.dataPos);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = dr.hcs;
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null) rafData.close();  // make sure its closed even on exception
    }
  }

  static public class DataRecord implements Comparable<DataRecord> {
    int resultIndex; // index into the result array
    int fileno;
    long dataPos;  // grib1 - start of GRIB record; grib2 - start of drs (data record)
    long bmsPos;  // if non zero, use alternate bms
    int scanMode;
    GdsHorizCoordSys hcs;

    DataRecord(int resultIndex, int fileno, long dataPos, long bmsPos, int scanMode, GdsHorizCoordSys hcs) {
      this.resultIndex = resultIndex;
      this.fileno = fileno;
      this.dataPos = dataPos; // (dataPos == 0) && !isGrib1 ? GribCollection.MISSING_RECORD : dataPos; // 0 also means missing in Grib2
      this.bmsPos = bmsPos;
      this.scanMode = scanMode;
      this.hcs = hcs;
    }

    @Override
    public int compareTo(DataRecord o) {
      int r = Misc.compare(fileno, o.fileno);
      if (r != 0) return r;
      return Misc.compare(dataPos, o.dataPos);
    }

    // debugging
    public void show(GribCollectionImmutable gribCollection) throws IOException {
      String dataFilename = gribCollection.getFilename(fileno);
      System.out.printf(" fileno=%d filename=%s datapos=%d%n", fileno, dataFilename, dataPos);
    }
  }

  public interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx) throws IOException;
    void setDataToZero(); // only used when debugging with gbx/ncx only, to fake the data
    Array getArray();
  }

  public static class DataReceiver implements DataReceiverIF {
    private Array dataArray;
    private Range yRange, xRange;
    private int horizSize;

    public DataReceiver(Section section) {
      int rank = section.getRank();
      this.yRange = section.getRange(rank - 2);  // last 2
      this.xRange = section.getRange(rank - 1);
      this.horizSize = yRange.length() * xRange.length();

      // prefill primitive array efficiently
      int len = (int) section.computeSize();
      float[] data = new float[len];
      Arrays.fill(data, Float.NaN);
      dataArray = Array.factory(DataType.FLOAT, section.getShape(), data);
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y : yRange) {
        for (int x : xRange) {
          int dataIdx = y * nx + x;
          dataArray.setFloat(start + count, data[dataIdx]);
          count++;
        }
      }
    }

    // optimization
    @Override
    public void setDataToZero() {
      float[] data = (float[]) dataArray.get1DJavaArray(dataArray.getElementType());
      Arrays.fill(data, 0.0f);
    }

    @Override
    public Array getArray() {
      return dataArray;
    }
  }

    /* public static class ChannelReceiver implements DataReceiverIF {
    private WritableByteChannel channel;
    private DataOutputStream outStream;
    private Range yRange, xRange;

    ChannelReceiver(WritableByteChannel channel, Range yRange, Range xRange) {
      this.channel = channel;
      this.outStream = new DataOutputStream(Channels.newOutputStream(channel));
      this.yRange = yRange;
      this.xRange = xRange;
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      // LOOK: write some ncstream header
      // outStream.write(header);

      // now write the data
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          outStream.writeFloat(data[dataIdx]);
        }
      }
    }

    // optimization
    @Override
    public void setDataToZero() {
    }

    @Override
    public Array getArray() {
      return null;
    }
  }    */

  /////////////////////////////////////////////////////////

  private static class Grib2DataReader extends GribDataReader {
    private Grib2Customizer cust;

    protected Grib2DataReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib2Customizer) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribDataReader.DataRecord dr) throws IOException {
      GdsHorizCoordSys hcs = dr.hcs;
      int scanMode = (dr.scanMode == Grib2Index.ScanModeMissing) ? hcs.scanMode : dr.scanMode;
      return Grib2Record.readData(rafData, dr.dataPos, dr.bmsPos, hcs.gdsNumberPoints, scanMode,
              hcs.nxRaw, hcs.nyRaw, hcs.nptsInLine);
    }

    @Override
    protected void show(RandomAccessFile rafData, long pos) throws IOException {
      Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, pos);
      if (gr != null) {
        Formatter f = new Formatter();
        f.format("File=%s%n", rafData.getLocation());
        f.format("  Parameter=%s%n", cust.getVariableName(gr));
        f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
        f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
        TimeCoord.TinvDate tinv = cust.getForecastTimeInterval(gr);
        if (tinv != null) f.format("  TimeInterval=%s%n", tinv);
        f.format("  ");
        gr.getPDS().show(f);
        System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f.toString());
      }
    }
  }

  private static class Grib1DataReader extends GribDataReader {
    private Grib1Customizer cust;

    protected Grib1DataReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib1Customizer) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException {
      return Grib1Record.readData(rafData, dr.dataPos);
    }

    @Override
    protected void show(RandomAccessFile rafData, long dataPos) throws IOException {
      rafData.seek(dataPos);
      Grib1Record gr = new Grib1Record(rafData);
      Formatter f = new Formatter();
      f.format("File=%s%n", rafData.getLocation());
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1Parameter param = cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
      f.format("  Parameter=%s%n", param);
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      Grib1ParamTime ptime = gr.getParamTime(cust);
      f.format("  ForecastTime=%d%n", ptime.getForecastTime());
      if (ptime.isInterval()) {
        int tinv[] = ptime.getInterval();
        f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
      }
      f.format("%n");
      gr.getPDSsection().showPds(cust, f);
      System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f.toString());
    }
  }

}
