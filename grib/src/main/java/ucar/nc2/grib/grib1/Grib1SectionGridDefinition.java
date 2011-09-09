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

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.zip.CRC32;

/**
 * The Grid Definition Section for GRIB-1 files
 *
 * @author caron
 */

@Immutable
public class Grib1SectionGridDefinition {
  private final byte[] rawData;
  private final long startingPosition;
  private final int gridTemplate;

  /**
   * Read Grib Definition section from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of section
   * @throws java.io.IOException      on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib1SectionGridDefinition(RandomAccessFile raf) throws IOException {

    startingPosition = raf.getFilePointer();

    // octets 1-3 (Length of GDS)
    int length = GribNumbers.int3(raf);

    // octet 6
    raf.skipBytes(2);
    gridTemplate = raf.read();

    // read in whole GDS as byte[]
    rawData = new byte[length];
    raf.seek(startingPosition);
    raf.readFully(rawData);
  }

  /**
   * Set Grib Definition section from byte array.
   *
   * @param rawData the byte array
   */
  public Grib1SectionGridDefinition(byte[] rawData) {
    this.rawData = rawData;
    this.gridTemplate = getOctet(6);
    this.startingPosition = -1;
  }

  public Grib1SectionGridDefinition(Grib1SectionProductDefinition pds) throws IOException {
    startingPosition = -1;
    gridTemplate = 1000 * pds.getGridDefinition(); // LOOK
    rawData = null;
    gds = ucar.nc2.grib.grib1.Grib1GdsPredefined.factory(pds.getCenter(), pds.getGridDefinition());
  }


  /**
   * get the raw bytes of the GDS
   *
   * @return GDS as byte[]
   */
  public byte[] getRawBytes() {
    return rawData;
  }

  /**
   * Calculate the CRC of the entire byte array
   *
   * @return CRC  of the entire byte array
   */
  public long calcCRC() {
    if (crc == 0) {
      if (rawData == null)
        crc = hashCode();
      else {
        CRC32 crc32 = new CRC32();
        crc32.update(rawData);
        crc = crc32.getValue();
      }
    }
    return crc;
  }

  private long crc = 0;

  public int getLength() {
    return (rawData == null) ? 0 : rawData.length;
  }

  public long getOffset() {
    return startingPosition;
  }

  /**
   * Get Grid Template number (code table 6)
   *
   * @return Grod Template number.
   */
  public int getGridTemplate() {
    return gridTemplate;
  }

  private final int getOctet(int index) {
    if (index > rawData.length) return GribNumbers.UNDEFINED;
    return rawData[index - 1] & 0xff;
  }

  private Grib1Gds gds = null;

  public Grib1Gds getGDS() {
    if (gds == null)
      gds = Grib1Gds.factory(gridTemplate, rawData);
    return gds;
  }

}

