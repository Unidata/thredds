/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.common.base.Throwables;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.ma2.*;
import ucar.nc2.ft2.coverage.CoordsSet;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
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
  private static final Logger logger = LoggerFactory.getLogger(GribDataReader.class);


  public static GribDataReader factory(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
    if (gribCollection.isGrib1)
      return new Grib1DataReader(gribCollection, vindex);
    else
      return new Grib2DataReader(gribCollection, vindex);
  }

  protected abstract float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException;
  protected abstract void show(RandomAccessFile rafData, long dataPos) throws IOException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static GribCollectionImmutable.Record currentDataRecord;
  public static GribDataValidator validator;
  public static String currentDataRafFilename;
  static boolean show = false;   // debug

  protected final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.VariableIndex vindex;
  private final List<DataRecord> records = new ArrayList<>();

  protected GribDataReader(GribCollectionImmutable gribCollection, GribCollectionImmutable.VariableIndex vindex) {
    this.gribCollection = gribCollection;
    this.vindex = vindex;
  }

  /**
   * Read the section of data described by want
   * @param want which data do you want?
   * @return data as an Array
   */
  public Array readData(SectionIterable want) throws IOException, InvalidRangeException {
    if (vindex instanceof PartitionCollectionImmutable.VariableIndexPartitioned)
      return readDataFromPartition((PartitionCollectionImmutable.VariableIndexPartitioned) vindex, want);
    else
      return readDataFromCollection(vindex, want);
  }

  /*
   SectionIterable iterates over the source indexes, corresponding to vindex's SparseArray.
     IOSP: works because variable coordinate corresponds 1-1 to Grib Coordinate.
     GribCoverage: must translate coordinates to Grib Coordinate index.
   want.getShape() indicates the result Array shape.
   SectionIterable.next(int[] index) is not used here.
   */
  private Array readDataFromCollection(GribCollectionImmutable.VariableIndex vindex, SectionIterable want) throws IOException {
    // first time, read records and keep in memory
    vindex.readRecords();

    int rank = want.getRank();
    int sectionLen = rank - 2; // all but x, y
    SectionIterable sectionWanted = want.subSection(0, sectionLen);
    // assert sectionLen == vindex.getRank(); LOOK true or false ??

    // collect all the records that need to be read
    int resultIndex = 0;
    for  (int sourceIndex : sectionWanted) {
      // addRecord(sourceIndex, count++);
      GribCollectionImmutable.Record record = vindex.getRecordAt(sourceIndex);
      if (Grib.debugRead)
        logger.debug("GribIosp debugRead sourceIndex=%d resultIndex=%d record is null=%s%n", sourceIndex, resultIndex, record == null);
      if (record != null)
        records.add( new DataRecord(resultIndex, record, vindex.group.getGdsHorizCoordSys()));
      resultIndex++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(want.getShape(), want.getRange(rank - 2), want.getRange(rank-1) );
    read(dataReceiver);
    return dataReceiver.getArray();
  }

  /*
   Iterates using SectionIterable.next(int[] index).
   The work of translating that down into partition heirarchy and finally to a GC is all in VariableIndexPartitioned.getDataRecord(int[] index)
   want.getShape() indicates the result Array shape.
 */
  private Array readDataFromPartition(PartitionCollectionImmutable.VariableIndexPartitioned vindexP, SectionIterable section) throws IOException {

    int rank = section.getRank();
    SectionIterable sectionWanted = section.subSection(0, rank - 2); // all but x, y
    SectionIterable.SectionIterator iterWanted = sectionWanted.getIterator();  // iterator over wanted indices in vindexP
    int[] indexWanted = new int[rank - 2];                              // place to put the iterator result
    int[] useIndex = indexWanted;

    // collect all the records that need to be read
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted array

      // for MRUTP, must munge the index here (not in vindexP.getDataRecord, because its recursive
      if (vindexP.getType() == GribCollectionImmutable.Type.MRUTP) {
        // find the partition from getRuntimeIdxFromMrutpTimeIndex
        CoordinateTime2D time2D = (CoordinateTime2D) vindexP.getCoordinateTime();
        assert time2D != null;
        int[] timeIndices = time2D.getTimeIndicesFromMrutp(indexWanted[0]);

        int[] indexReallyWanted = new int[indexWanted.length+1];
        indexReallyWanted[0] = timeIndices[0];
        indexReallyWanted[1] = timeIndices[1];
        System.arraycopy(indexWanted, 1, indexReallyWanted, 2, indexWanted.length-1);
        useIndex = indexReallyWanted;
      }

      PartitionCollectionImmutable.DataRecord record = vindexP.getDataRecord(useIndex);
      if (record == null) {
        if (Grib.debugRead) logger.debug("readDataFromPartition missing data%n");
        resultPos++; // can just skip, since result is prefilled with NaNs
        continue;
      }
      record.resultIndex = resultPos;
      records.add(record);
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(section.getShape(), section.getRange(rank-2), section.getRange(rank-1) );
    readPartitioned(dataReceiver);

    return dataReceiver.getArray();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Coordinate based subsetting for Coverage

  public Array readData2(CoordsSet want, RangeIterator yRange, RangeIterator xRange) throws IOException {
    if (vindex instanceof PartitionCollectionImmutable.VariableIndexPartitioned)
      return readDataFromPartition2((PartitionCollectionImmutable.VariableIndexPartitioned) vindex, want, yRange, xRange);
    else
      return readDataFromCollection2(vindex, want, yRange, xRange);
  }

  private Array readDataFromCollection2(GribCollectionImmutable.VariableIndex vindex, CoordsSet want, RangeIterator yRange, RangeIterator xRange) throws IOException {
    // first time, read records and keep in memory
    vindex.readRecords();

    // collect all the records that need to be read
    int resultIndex = 0;
    for (SubsetParams coords : want) {
      GribCollectionImmutable.Record record = vindex.getRecordAt(coords);
      if (record != null) {
        DataRecord dr = new DataRecord(resultIndex, record, vindex.group.getGdsHorizCoordSys());
        if (GribDataReader.validator != null) dr.validation = coords;
        records.add(dr);
      }
      resultIndex++;
    }

    DataReceiverIF dataReceiver = new DataReceiver(want.getShape(yRange, xRange), yRange, xRange );
    read(dataReceiver);
    return dataReceiver.getArray();
  }

  private Array readDataFromPartition2(PartitionCollectionImmutable.VariableIndexPartitioned vindexP, CoordsSet want, RangeIterator yRange, RangeIterator xRange) throws IOException {

    // collect all the records that need to be read
    int resultPos = 0;
    for (SubsetParams coords : want) {
      PartitionCollectionImmutable.DataRecord record = vindexP.getDataRecord(coords);
      if (record != null) {
        record.resultIndex = resultPos;
        records.add(record);
      }
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(want.getShape(yRange, xRange), yRange, xRange );
    readPartitioned(dataReceiver);

    return dataReceiver.getArray();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read all of the data records that have been added.
   * The full (x,y) record is read, the reciever will subset the (x, y) as needed.
   * @param dataReceiver send data here.
   */
  private void read(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    int currFile = -1;
    RandomAccessFile rafData = null;
    try {
      for (DataRecord dr : records) {
        if (Grib.debugIndexOnly || Grib.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          currentDataRecord = dr.record;
          currentDataRafFilename = gribCollection.getDataRafFilename(dr.record.fileno);
          if (Grib.debugIndexOnlyShow) dr.show(gribCollection);
          dataReceiver.setDataToZero();
          continue;
        }

        if (dr.record.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = gribCollection.getDataRaf(dr.record.fileno);
          currFile = dr.record.fileno;
        }

        if (dr.record.pos == GribCollectionMutable.MISSING_RECORD) continue;

        if (GribDataReader.validator != null && dr.validation != null && rafData != null) {
          GribDataReader.validator.validate(gribCollection.cust, rafData, dr.record.pos + dr.record.drsOffset, dr.validation);

        } else if (show && rafData != null) { // for validation
          show( dr.validation);
          show(rafData, dr.record.pos + dr.record.drsOffset);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null) rafData.close();  // make sure its closed even on exception
    }
  }

  private void show(SubsetParams validation) {
    if (validation == null) return;
    System.out.printf("Coords wanted%n %s", validation);
  }

  private void readPartitioned(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    PartitionCollectionImmutable.DataRecord lastRecord = null;
    RandomAccessFile rafData = null;
    try {

      for (DataRecord dr : records) {
        PartitionCollectionImmutable.DataRecord drp = (PartitionCollectionImmutable.DataRecord) dr;
        if (Grib.debugIndexOnly || Grib.debugGbxIndexOnly) {
          GribIosp.debugIndexOnlyCount++;
          if (Grib.debugIndexOnlyShow) drp.show();
          dataReceiver.setDataToZero();
          continue;
        }

        if ((rafData == null) || !drp.usesSameFile(lastRecord)) {
          if (rafData != null) rafData.close();
          rafData = drp.usePartition.getRaf(drp.partno, dr.record.fileno);
        }
        lastRecord = drp;

        if (dr.record.pos == GribCollectionMutable.MISSING_RECORD) continue;

        if (GribDataReader.validator != null && dr.validation != null) {
          GribDataReader.validator.validate(gribCollection.cust, rafData, dr.record.pos + dr.record.drsOffset, dr.validation);
        } else if (show) { // for validation
          show( dr.validation);
          show(rafData, dr.record.pos + dr.record.drsOffset);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = dr.hcs;
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null) rafData.close();  // make sure its closed even on exception
    }
  }

  public static class DataRecord implements Comparable<DataRecord> {
    int resultIndex; // index into the result array
    final GribCollectionImmutable.Record record;
    final GdsHorizCoordSys hcs;
    SubsetParams validation;

    DataRecord(int resultIndex, GribCollectionImmutable.Record record, GdsHorizCoordSys hcs) {
      this.resultIndex = resultIndex;
      this.record = record;
      this.hcs = hcs;
    }

    @Override
    public int compareTo(@Nonnull DataRecord o) {
      int r = Misc.compare(record.fileno, o.record.fileno);
      if (r != 0) return r;
      return Misc.compare(record.pos, o.record.pos);
    }

    // debugging
    public void show(GribCollectionImmutable gribCollection) {
      String dataFilename = gribCollection.getFilename(record.fileno);
      System.out.printf(" fileno=%d filename=%s startPos=%d%n", record.fileno, dataFilename, record.pos);
    }
  }

  public interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx);
    void setDataToZero(); // only used when debugging with gbx/ncx only, to fake the data
    Array getArray();
  }

  public static class DataReceiver implements DataReceiverIF {
    private Array dataArray;
    private final RangeIterator yRange;
    private final RangeIterator xRange;
    private final int horizSize;

    DataReceiver(int[] shape, RangeIterator yRange, RangeIterator xRange) {
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      long len = Section.computeSize(shape);
      if (len > 100 * 1000 * 1000*4) { // LOOK make configurable
        logger.debug("Len greater that 100MB shape={}%n{}", Misc.showInts(shape),
                Throwables.getStackTraceAsString(new Throwable()));
        throw new IllegalArgumentException("RequestTooLarge: Len greater that 100M ");
      }
      float[] data = new float[ (int) len];
      Arrays.fill(data, Float.NaN); // prefill primitive array
      dataArray = Array.factory(DataType.FLOAT, shape, data);
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) {
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
      float[] data = (float[]) dataArray.get1DJavaArray(dataArray.getDataType());
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
    private final Grib2Tables cust;

    Grib2DataReader(GribCollectionImmutable gribCollection,
        GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib2Tables) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribDataReader.DataRecord dr) throws IOException {
      GdsHorizCoordSys hcs = dr.hcs;
      long dataPos = dr.record.pos + dr.record.drsOffset;
      long bmsPos = (dr.record.bmsOffset > 0) ? dr.record.pos + dr.record.bmsOffset : 0;
      return Grib2Record.readData(rafData, dataPos, bmsPos, hcs.gdsNumberPoints, hcs.getScanMode(),
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
        TimeCoordIntvDateValue tinv = cust.getForecastTimeInterval(gr);
        if (tinv != null) f.format("  TimeInterval=%s%n", tinv);
        f.format("  ");
        gr.getPDS().show(f);
        System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f.toString());
      }
    }
  }

  private static class Grib1DataReader extends GribDataReader {
    private final Grib1Customizer cust;

    Grib1DataReader(GribCollectionImmutable gribCollection,
        GribCollectionImmutable.VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib1Customizer) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException {
      return Grib1Record.readData(rafData, dr.record.pos);
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
        int[] tinv = ptime.getInterval();
        f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
      }
      f.format("%n");
      gr.getPDSsection().showPds(cust, f);
      System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f.toString());
    }
  }

}
