/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.GribData;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * The Binary Data Section for GRIB-1 files
 *
 * @author caron
 */
@Immutable
public class Grib1SectionBinaryData {
  private final int length;
  private final long startingPosition;

  /*   FM 92-XI EXT. GRIB EDITION 1
  Section 4 – Binary data section
  Octet   Contents
  1–3     Length of section
  4       Flag (see Code table 11) (first 4 bits). Number of unused bits at end of Section 4 (last 4 bits)
  5–6     Scale factor (E)
  7–10    Reference value (minimum of packed values)
  11      Number of bits containing each packed value
  12–     depending on the flag value in octet 4
   */

  Grib1SectionBinaryData(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-3 (Length of section)
    length = GribNumbers.uint3(raf);
    //if (length < 0)
    //  throw new IllegalStateException("GRIB record has bad length, pos = " + startingPosition);
    raf.seek(startingPosition + length);
  }

  Grib1SectionBinaryData(long startingPosition, int length) {
    this.startingPosition = startingPosition;
    this.length = length;
  }

  public long getStartingPosition() {
    return startingPosition;
  }
  public int getLength() {
    return length;
  }

  /////////////////////

  public byte[] getBytes(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition); // go to the data section
    byte[] data = new byte[length];
    raf.readFully(data);
    return data;
  }

    // for debugging
    GribData.Info getBinaryDataInfo(RandomAccessFile raf) throws IOException {
      return getBinaryDataInfo(raf, startingPosition);
    }

  public static GribData.Info getBinaryDataInfo(RandomAccessFile raf, long start) throws IOException {
    raf.seek(start); // go to the data section

    GribData.Info info = new GribData.Info();

    info.dataLength = GribNumbers.uint3(raf);    // // octets 1-3 (section length)

    // octet 4, 1st half (packing flag)
    info.flag = raf.read();

    // Y × 10^D = R + X × 2^E
    // octets 5-6 (E = binary scale factor)
    info.binaryScaleFactor = GribNumbers.int2(raf);

    // octets 7-10 (R = reference point = minimum value)
    info.referenceValue = GribNumbers.float4(raf);

    // octet 11 (number of bits per value)
    info.numberOfBits = raf.read();

    return info;
  }




}
