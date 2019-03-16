/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * Grib1 Section 3 (BitMap)
 *
 * @author John
 * @since 9/3/11
 */
@Immutable
public class Grib1SectionBitMap {
  private static final Logger logger = LoggerFactory.getLogger(Grib1SectionBitMap.class);

  private final long startingPosition;

  Grib1SectionBitMap(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-3 (Length of section)
    int length = GribNumbers.int3(raf);

    raf.seek(startingPosition + length);
  }

  Grib1SectionBitMap(long startingPosition) {
    this.startingPosition = startingPosition;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  /**
   * Read the bitmap array when needed, return null if none.
   */
  @Nullable
  public byte[] getBitmap(RandomAccessFile raf) throws IOException {
    if (startingPosition <= 0) {
      throw new IllegalStateException("Grib1 Bit map has bad starting position");
    }

    raf.seek(startingPosition);

    // octet 1-3 (length of section)
    int length = GribNumbers.uint3(raf);

    // octet 4 unused bits
    raf.read();   // unused

    // octets 5-6
    int bm = raf.readShort();
    if (bm != 0) {
      logger.warn("Grib1 Bit map section pre-defined (provided by center) bitmap number = {}", bm);
      return null;
    }

    // not sure if length is set correctly when pre-define bitmap is used, so  wait until that to test
    // seeing a -1, bail out
    if (length <= 6 || length > 10e6) {   // look max  ??
      return null;
    }

    // read the bits as integers
    int n = length - 6;
    byte[] data = new byte[n];
    raf.readFully(data);
    return data;
  }

  int getLength(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition);
    return GribNumbers.uint3(raf);
  }

}
