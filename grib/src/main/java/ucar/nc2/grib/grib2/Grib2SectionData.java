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
 * The Data section 7 for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionData {

  private final long startingPosition;
  private final int msgLength;

  public Grib2SectionData(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of section)
    msgLength = GribNumbers.int4(raf);

    // octet 5
    int section = raf.read();
    if (section != 7)
      throw new IllegalStateException("Not a Grib2SectionData (section 7)");

    // skip to end of the data section
    raf.seek(startingPosition + msgLength);
  }

  Grib2SectionData(long startingPosition, int msgLength) {
    this.startingPosition = startingPosition;
    this.msgLength = msgLength;
  }

  public long getStartingPosition() {
    return startingPosition;
  }
  public long getEndingPosition() {
    return startingPosition+msgLength;
  }

  public int getMsgLength() {
    return msgLength;
  }

  public byte[] getBytes(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition); // go to the data section
    byte[] data = new byte[msgLength];
    raf.readFully(data);
    return data;
  }
}
