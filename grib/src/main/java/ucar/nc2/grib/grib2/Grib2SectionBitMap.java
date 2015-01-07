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

package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * The BitMap section 6 for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionBitMap {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2SectionBitMap.class);

  private final long startingPosition;
  private final int bitMapIndicator;

  static public Grib2SectionBitMap factory(RandomAccessFile raf, long startingPos) throws IOException {
    raf.seek(startingPos);
    return new Grib2SectionBitMap(raf);
  }

  public Grib2SectionBitMap(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of section)
    int length = GribNumbers.int4(raf);

    // octet 5
    int section = raf.read();
    if (section != 6)
      throw new IllegalArgumentException("Not a GRIB-2 Bitmap section");

    // octet 6
    bitMapIndicator = raf.read();
    raf.seek(startingPosition + length);
  }

  public Grib2SectionBitMap(long startingPosition, int bitMapIndicator) {
    this.startingPosition = startingPosition;
    this.bitMapIndicator = bitMapIndicator;
  }

  int getLength(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition);
    return GribNumbers.int4(raf);
  }

  /*
  Code Table Code table 6.0 - Bit map indicator (6.0)
      0: A bit map applies to this product and is specified in this Section
     -1: A bit map predetermined by the originating/generating centre applies to this product and is not specified in this Section
    254: A bit map defined previously in the same "GRIB" message applies to this product
    255: A bit map does not apply to this product
   */
  public int getBitMapIndicator() {
    return bitMapIndicator;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  /**
   * Read the bit map array.
   *
   * @param raf read from here
   * @return bit map as array of byte values
   * @throws java.io.IOException on read error
   */
  public byte[] getBitmap(RandomAccessFile raf) throws IOException {
    // no bitMap
    if (bitMapIndicator == 255)
      return null;

    // LOOK: bitMapIndicator=254 == previously defined bitmap
    if (bitMapIndicator == 254)
      logger.debug("HEY bitMapIndicator=254 previously defined bitmap");

    if (bitMapIndicator != 0) {
      throw new UnsupportedOperationException("Grib2 Bit map section pre-defined (provided by center) = " + bitMapIndicator);
    }

    raf.seek(startingPosition);
    int length = GribNumbers.int4(raf);
    raf.skipBytes(2);

    byte[] data = new byte[length - 6];
    raf.readFully(data);

    return data;

    /* create new bit map when it is first asked for
    boolean[] bitmap = new boolean[numberOfPoints];
    int[] bitmask = {128, 64, 32, 16, 8, 4, 2, 1};
    for (int i = 0; i < bitmap.length; i++) {
      bitmap[i] = (data[i / 8] & bitmask[i % 8]) != 0;
    }
    return bitmap; */
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Grib2SectionBitMap");
    sb.append("{startingPosition=").append(startingPosition);
    sb.append(", bitMapIndicator=").append(bitMapIndicator);
    sb.append('}');
    return sb.toString();
  }
}
