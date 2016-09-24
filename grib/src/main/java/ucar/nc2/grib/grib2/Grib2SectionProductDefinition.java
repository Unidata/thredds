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

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.zip.CRC32;

/**
 * The Product Definition section 4 for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionProductDefinition {
  private final byte[] rawData;
  private final int templateNumber;

  /**
   * Read Product Definition section from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of section
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
   public Grib2SectionProductDefinition( RandomAccessFile raf) throws IOException {

     long startingPosition = raf.getFilePointer();

     // octets 1-4 (Length of GDS)
     int length = GribNumbers.int4(raf);

    // octet 5
     int section = raf.read();
     if (section != 4)
       throw new IllegalArgumentException("Not a GRIB-2 PDS section");

     // octets 8-9
     raf.skipBytes(2);
     templateNumber = GribNumbers.int2(raf);

     // read in whole GDS as byte[]
     rawData = new byte[length];
     raf.seek(startingPosition);
     raf.readFully(rawData);
   }

  /**
   * Set PDS section from byte array.
   *
   * @param rawData the byte array
   */
  public Grib2SectionProductDefinition( byte[] rawData) {
    this.rawData = rawData;
    this.templateNumber = GribNumbers.int2( getInt(8), getInt(9) );
  }

  /**
   * get the raw bytes of the PDS
   *
   * @return PDS as byte[]
   */
  public byte[] getRawBytes() {
    return rawData;
  }

  /**
   * Calculate the CRC of the entire byte array
   * @return CRC
   */
  public long calcCRC() {
    CRC32 crc32 = new CRC32();
    crc32.update(rawData);
    return crc32.getValue();
  }

  public int getLength() {
    return rawData.length;
  }

  /**
   * Get PDS Template Number (code table 4.0)
   *
   * @return PDS Template Number
   */
  public int getPDSTemplateNumber() {
    return templateNumber;
  }

  private int getInt(int index) {
    return rawData[index-1] & 0xff;
  }

  /**
   * Parse the raw bytes into a Grib2Pds
   * @return Grib2Pds
   */
  public Grib2Pds getPDS() {
    return Grib2Pds.factory(templateNumber, rawData);
  }
}
