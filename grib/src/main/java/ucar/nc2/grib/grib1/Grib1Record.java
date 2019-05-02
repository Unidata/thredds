/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.GribData;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Formatter;

/**
 * A Grib1 message.
 *
 * @author John
 * @since 9/3/11
 */
public class Grib1Record {

  private final Grib1SectionIndicator is;
  private final Grib1SectionGridDefinition gdss;
  private final Grib1SectionProductDefinition pdss;
  private final Grib1SectionBitMap bitmap;
  private final Grib1SectionBinaryData dataSection;

  private final byte[] header; // anything in between the records - eg idd header
  private int file; // for multiple files in same dataset

  /**
   * Construction for Grib1Record.
   *
   * @param header      Grib header
   * @param is          Grib1IndicatorSection
   * @param gdss        Grib1GridDefinitionSection
   * @param pdss        Grib1ProductDefinitionSection
   * @param bitmap      Grib1SectionBitMap
   * @param dataSection Grib1SectionData
   */
  public Grib1Record(byte[] header, Grib1SectionIndicator is,
                     Grib1SectionGridDefinition gdss,
                     Grib1SectionProductDefinition pdss,
                     Grib1SectionBitMap bitmap,
                     Grib1SectionBinaryData dataSection) {

    this.header = header;
    this.is = is;
    this.gdss = gdss;
    this.pdss = pdss;
    this.bitmap = bitmap;
    this.dataSection = dataSection;
  }

  /**
   * Construct record by reading raf, no checking
   *
   * @param raf positioned at start of message: the 'G' in "GRIB"
   * @throws IOException on read error
   */
  public Grib1Record(RandomAccessFile raf) throws IOException {
    this.header = null;
    is = new Grib1SectionIndicator(raf);
    pdss = new Grib1SectionProductDefinition(raf);
    gdss = pdss.gdsExists() ? new Grib1SectionGridDefinition(raf) : new Grib1SectionGridDefinition(pdss);
    bitmap = pdss.bmsExists() ? new Grib1SectionBitMap(raf) : null;
    dataSection = new Grib1SectionBinaryData(raf);
  }

  // copy constructor
  Grib1Record(Grib1Record from) {
    this.header = from.header;
    this.is = from.is;
    this.gdss = from.gdss;
    this.pdss = from.pdss;
    this.bitmap = from.bitmap;
    this.dataSection = from.dataSection;
  }

  public byte[] getHeader() {
    return header;
  }

  public Grib1SectionIndicator getIs() {
    return is;
  }

  public Grib1SectionGridDefinition getGDSsection() {
    return gdss;
  }

  public Grib1Gds getGDS() {
    return gdss.getGDS();
  }

  public Grib1SectionProductDefinition getPDSsection() {
    return pdss;
  }

  public Grib1SectionBitMap getBitMapSection() {
    return bitmap;
  }

  public Grib1SectionBinaryData getDataSection() {
    return dataSection;
  }

  public CalendarDate getReferenceDate() {
    return pdss.getReferenceDate();
  }

  public int getFile() {
    return file;
  }

  public void setFile(int file) {
    this.file = file;
  }

  private Grib1ParamTime ptime;

  public Grib1ParamTime getParamTime(Grib1Customizer cust) {
    if (ptime == null) ptime = cust.getParamTime(pdss);
    return ptime;
  }

  /////////////// reading data

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf) throws IOException {
    return readData(raf, GribData.getInterpolationMethod());
  }

  // dont convertQuasiGrid
  public float[] readDataRaw(RandomAccessFile raf) throws IOException {
    return readData(raf, GribData.InterpolationMethod.none);
  }

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf, GribData.InterpolationMethod method) throws IOException {
    Grib1Gds gds = getGDS();
    Grib1DataReader reader = new Grib1DataReader(pdss.getDecimalScale(), gds.getScanMode(), gds.getNxRaw(), gds.getNyRaw(), gds.getNpts(), dataSection.getStartingPosition());
    byte[] bm = (bitmap == null) ? null : bitmap.getBitmap(raf);
    float[] data = reader.getData(raf, bm);

    if (gdss.isThin()) {
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw(), method);
    }

    lastRecordRead = this;
    return data;
  }

  public void showDataInfo(RandomAccessFile raf, Formatter f) throws IOException {
    Grib1Gds gds = getGDS();
    f.format(" decimal scale = %d%n", pdss.getDecimalScale());
    f.format("     scan mode = %d%n", gds.getScanMode());
    f.format("            nx = %d%n", gds.getNx());
    f.format("            ny = %d%n", gds.getNy());
    f.format("         nxRaw = %d%n", gds.getNxRaw());
    f.format("         nyRaw = %d%n", gds.getNyRaw());
    f.format("          Npts = %d%n", gds.getNpts());
    f.format("        isThin = %s%n", gdss.isThin());

    GribData.Info info = getBinaryDataInfo(raf);
    f.format("   dataLength = %d%n", info.dataLength);

    // octet 4, 1st half (packing flag)
    f.format("    ----flag = %s%n", Long.toBinaryString(info.flag));
    f.format("        data = %s%n", info.getGridPointS());
    f.format("     packing = %s%n", info.getPackingS());
    f.format("        type = %s%n", info.getDataTypeS());
    f.format(" moreOctet14 = %s%n", info.hasOctet14());
    f.format("    binscale = %d%n", info.binaryScaleFactor);
    f.format("    decscale = %d%n", info.decimalScaleFactor);
    f.format("reference value = %f%n", info.referenceValue);
    f.format("      nbits = %d%n", info.numberOfBits);

    Grib1DataReader.showComplexPackingInfo(f, raf, dataSection.getStartingPosition());
  }

  /**
   * Read data array by first reading in GribRecord.
   * All sections are read in, so scanMode is from the datafile, not the index.
   *
   * @param raf      from this RandomAccessFile
   * @param startPos message starts here
   * @return data as float[] array
   * @throws IOException on read error
   */
  public static float[] readData(RandomAccessFile raf, long startPos) throws IOException {
    raf.seek(startPos);
    Grib1Record gr = new Grib1Record(raf);
    return gr.readData(raf);
  }

  // debugging, do not use
  public static Grib1Record lastRecordRead;

  /*
   * Read data array: use when you want to be independent of the GribRecord
   *
   * @param raf          from this RandomAccessFile
   * @param bmPos        bitmap.start
   * @param decimalScale pds.decimalScale
   * @param scanMode     gds.scanMode
   * @param nx           gds.nx
   * @param ny           gds.ny
   * @return data as float[] array
   * @throws IOException on read error
   *
  public static float[] readData(RandomAccessFile raf, long bmPos, int decimalScale, int scanMode, int nx, int ny) throws IOException {
    raf.seek(bmPos);
    Grib1SectionBitMap bms = new Grib1SectionBitMap(raf);
    Grib1DataReader reader = new Grib1DataReader(decimalScale, scanMode, nx, ny, raf.getFilePointer());

    boolean[] bitmap = bms.getBitmap(raf);

    return reader.getData(raf, bitmap);
  } */

  public GribData.Info getBinaryDataInfo(RandomAccessFile raf) throws IOException {
    GribData.Info info = dataSection.getBinaryDataInfo(raf);
    info.decimalScaleFactor = pdss.getDecimalScale();
    info.bitmapLength = (bitmap == null) ? 0 : bitmap.getLength(raf);
    info.nPoints = getGDS().getNpts();
    info.msgLength = is.getMessageLength();

    if (bitmap == null) {
      info.ndataPoints = info.nPoints;
    } else {
      byte[] bm = bitmap.getBitmap(raf);
      if (bm == null) {
        info.ndataPoints = info.nPoints;
      } else { // have to count the bits to see how many data values are stored
        info.ndataPoints = GribNumbers.countBits(bm);
      }
    }

    return info;
  }

  // debugging - do not use
  public int[] readRawData(RandomAccessFile raf) throws IOException {
    Grib1Gds gds = getGDS();
    Grib1DataReader reader = new Grib1DataReader(pdss.getDecimalScale(), gds.getScanMode(), gds.getNxRaw(), gds.getNyRaw(), gds.getNpts(), dataSection.getStartingPosition());
    byte[] bm = (bitmap == null) ? null : bitmap.getBitmap(raf);
    return reader.getDataRaw(raf, bm);
  }
}
