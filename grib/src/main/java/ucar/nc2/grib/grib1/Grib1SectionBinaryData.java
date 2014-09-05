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
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

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
  12–Variable, depending on the flag value in octet 4

  Note: A negative value of E shall be indicated by setting the high-order bit (bit 1) in the left-hand octet to 1 (on).

  Code table 11 – Flag
  Bit Value Meaning
  1   0     Grid-point data
      1     Spherical harmonic coefficients
  2   0     Simple packing
      1     Complex or second-order packing
  3   0     Floating point values (in the original data) are represented
      1     Integer values (in the original data) are represented
  4   0     No additional flags at octet 14
      1     Octet 14 contains additional flag bits

  The following gives the meaning of the bits in octet 14 ONLY if bit 4 is set to 1. Otherwise octet 14 contains
  regular binary data.

  Bit Value Meaning
  5         Reserved – set to zero
  6   0     Single datum at each grid point
      1     Matrix of values at each grid point
  7   0     No secondary bit-maps
      1     Secondary bit-maps present
  8   0     Second-order values constant width
      1     Second-order values different widths
  9–12 Reserved for future use

   */

  public Grib1SectionBinaryData(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-3 (Length of section)
    length = GribNumbers.uint3(raf);
    //if (length < 0)
    //  throw new IllegalStateException("GRIB record has bad length, pos = " + startingPosition);
    raf.seek(startingPosition + length);
  }

  public Grib1SectionBinaryData(long startingPosition, int length) {
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
    raf.seek(startingPosition); // go to the data section

    GribData.Info info = new GribData.Info();

    info.dataLength = GribNumbers.uint3(raf);    // // octets 1-3 (section length)

    /*
    Code table 11 – Flag
    Bit No. Value Meaning
     1 0 Grid-point data
       1 Spherical harmonic coefficients
     2 0 Simple packing
       1 Complex or second-order packing
     3 0 Floating point values (in the original data) are represented
       1 Integer values (in the original data) are represented
     4 0 No additional flags at octet 14
       1 Octet 14 contains additional flag bits
     */

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
