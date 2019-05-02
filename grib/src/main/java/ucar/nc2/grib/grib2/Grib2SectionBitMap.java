/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * The BitMap section 6 for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionBitMap {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2SectionBitMap.class);

  private final long startingPosition;
  private final int bitMapIndicator;

  public static Grib2SectionBitMap factory(RandomAccessFile raf, long startingPos) throws IOException {
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

  Grib2SectionBitMap(long startingPosition, int bitMapIndicator) {
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
   * @return bit map as array of byte values, or null if none.
   * @throws java.io.IOException on read error
   */
  @Nullable
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
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("startingPosition", startingPosition)
        .add("bitMapIndicator", bitMapIndicator)
        .toString();
  }
}
