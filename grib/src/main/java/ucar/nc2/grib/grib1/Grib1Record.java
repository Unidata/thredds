/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

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

  /////////////// reading data

  // isolate dependencies here - in case we have a "minimal I/O" mode where not all fields are available
  public float[] readData(RandomAccessFile raf) throws IOException {
    Grib1Gds gds = gdss.getGDS();
    Grib1DataReader reader = new Grib1DataReader(pdss.getDecimalScale(), gds.getScanMode(), gds.getNx(), gds.getNy(), dataSection.getStartingPosition());
    byte[] bm = (bitmap == null) ? null : bitmap.getBitmap(raf);
    float[] data = reader.getData(raf, bm);

    if (gdss.isThin()) {
      data = QuasiRegular.convertQuasiGrid(data, gds.getNptsInLine(), gds.getNxRaw(), gds.getNyRaw() );
    }

    return data;
  }

  /**
   * Read data array by first reading in GribRecord
   *
   * @param raf  from this RandomAccessFile
   * @param startPos message starts here
   * @return data as float[] array
   * @throws IOException on read error
   */
  static public float[] readData(RandomAccessFile raf, long startPos) throws IOException {
    raf.seek(startPos);
    Grib1Record gr = new Grib1Record(raf);
    return gr.readData(raf);
  }

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
  static public float[] readData(RandomAccessFile raf, long bmPos, int decimalScale, int scanMode, int nx, int ny) throws IOException {
    raf.seek(bmPos);
    Grib1SectionBitMap bms = new Grib1SectionBitMap(raf);
    Grib1DataReader reader = new Grib1DataReader(decimalScale, scanMode, nx, ny, raf.getFilePointer());

    boolean[] bitmap = bms.getBitmap(raf);

    return reader.getData(raf, bitmap);
  } */
}
