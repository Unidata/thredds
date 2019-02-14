/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * The Local Use section 2 for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionLocalUse {

  private final byte[] rawData;

  /**
   * Read Grib2SectionLocalUse from raf.
   *
   * @param raf RandomAccessFile, with pointer at start od section
   */
  public Grib2SectionLocalUse(RandomAccessFile raf) throws IOException {

    // octets 1-4 (Length of GDS)
    int length = GribNumbers.int4(raf);
    int section = raf.read();  // This is section 2

    if (section != 2) {  // no local use section
      raf.skipBytes(-5);
      rawData = null;
    } else {
      rawData = new byte[length-5];
      raf.readFully(rawData);
    }
  }

  Grib2SectionLocalUse(byte[] rawData) {
    this.rawData = rawData;
  }

  public byte[] getRawBytes() {
    return rawData;
  }
}
