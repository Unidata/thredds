/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

// $Id: Grib1BinaryDataSection.java,v 1.15 2005/12/12 18:22:32 rkambic Exp $

/*
 * Grib1BinaryDataSection.java
 * @author Robb Kambic
 */

package ucar.grib.grib1;


import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * A class representing the binary data section (BDS) of a GRIB record.
 *
 * @version 1.0
 */

public final class Grib1BinaryDataSection {

  /**
   * Constant value for an undefined grid value.
   */
  private static final float UNDEFINED = -9999f;

  /**
   * Length in bytes of this BDS.
   */
  private int length;

  /**
   * Buffer for one byte which will be processed bit by bit.
   */
  private int bitBuf = 0;

  /**
   * Current bit position in <tt>bitBuf</tt>.
   */
  private int bitPos = 0;

  /**
   * Array of grid values.
   */
  private float[] values;

  /**
   * Indicates whether the BMS is represented by a single value
   * Octet 12 is empty, and the data is represented by the reference value.
   */
  private boolean isConstant = false;

  // *** constructors *******************************************************

  /**
   * Constructs a Grib1BinaryDataSection object from a raf.
   * A bit map is not available.
   *
   * @param raf          RandomAccessFile stream with BDS content
   * @param decimalscale the exponent of the decimal scale
   * @throws IOException           if stream can not be opened etc.
   * @throws NotSupportedException if stream contains no valid GRIB file
   */
  public Grib1BinaryDataSection(RandomAccessFile raf, int decimalscale, Grib1BitMapSection bms)
      throws IOException, NotSupportedException {
    this(raf, decimalscale, bms, 64, -1);
    //this(raf, decimalscale, null, 64, -1);
  }

  /**
   * Constructs a Grib1BinaryDataSection object from a raf.
   * A bit map is defined.
   *
   * @param raf          raf with BDS content
   * @param decimalscale the exponent of the decimal scale
   * @param bms          bit map section of GRIB record
   * @throws IOException           if raf can not be opened etc.
   * @throws NotSupportedException if stream contains no valid GRIB file
   */
  public Grib1BinaryDataSection
      (RandomAccessFile raf, int decimalscale, Grib1BitMapSection bms, int scanMode, int Xlength)
      throws IOException, NotSupportedException {
    this(raf, decimalscale, bms, scanMode, Xlength, -1);
  }

  public Grib1BinaryDataSection
      (RandomAccessFile raf, int decimalscale, Grib1BitMapSection bms, int scanMode, int Xlength, int Ylength)
      throws IOException, NotSupportedException {
    // octets 1-3 (section length)
    length = GribNumbers.uint3(raf);
    //System.out.println( "BDS length = " + length );

    // octet 4, 1st half (packing flag)
    int unusedbits = raf.read();
    if ((unusedbits & 192) != 0) {
      throw new NotSupportedException(
          "Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing ");
    }

    // octet 4, 2nd half (number of unused bits at end of this section)
    unusedbits = unusedbits & 15;
    //System.out.println( "BDS unusedbits = " + unusedbits );

    // octets 5-6 (binary scale factor)
    int binscale = GribNumbers.int2(raf);

    // octets 7-10 (reference point = minimum value)
    float refvalue = GribNumbers.float4(raf);

    // octet 11 (number of bits per value)
    int numbits = raf.read();
    //System.out.println( "BDS numbits = " + numbits );
    if (numbits == 0) {
      isConstant = true;
    }
    //System.out.println( "BDS isConstant = " + isConstant );

    // *** read values *******************************************************

    float ref = (float) (Math.pow(10.0, -decimalscale) * refvalue);
    float scale = (float) (Math.pow(10.0, -decimalscale)
        * Math.pow(2.0, binscale));

    if (bms != null) {
      boolean[] bitmap = bms.getBitmap();

      values = new float[bitmap.length];
      for (int i = 0; i < bitmap.length; i++) {
        if (bitmap[i]) {
          if (!isConstant) {
            values[i] = ref + scale * bits2UInt(numbits, raf);
          } else {  // rdg - added this to handle a constant valued parameter
            values[i] = ref;
          }
        } else {
          values[i] = Grib1BinaryDataSection.UNDEFINED;
        }
      }
      scanningModeCheck(scanMode, Xlength);
    } else {  // bms is null
      if (!isConstant) {
        if (Xlength != -1 && Ylength != -1) {
          values = new float[Xlength * Ylength];
        } else {
          values = new float[((length - 11) * 8 - unusedbits) / numbits];
        }
        for (int i = 0; i < values.length; i++) {
          values[i] = ref + scale * bits2UInt(numbits, raf);
        }
        scanningModeCheck(scanMode, Xlength);
      } else {                     // constant valued - same min and max
        //System.out.println( "BDS constant valued - same min and max" );
        if (Xlength != -1 && Ylength != -1) {
          values = new float[Xlength * Ylength];
        } else { // sledge hammer approach
          int x = 0,
              y = 0;
          raf.seek(raf.getFilePointer() - 53);  // return to start of GDS
          length = GribNumbers.uint3(raf);
          if (length == 42) {      // Lambert/Mercator offset
            raf.skipBytes(3);
            x = raf.readShort();
            y = raf.readShort();
          } else {
            raf.skipBytes(7);
            length = GribNumbers.uint3(raf);
            if (length == 32) {  // Polar sterographic
              raf.skipBytes(3);
              x = raf.readShort();
              y = raf.readShort();
            } else {
              x = y = 1;
              System.out.println("Grib1BinaryDataSection constant value, can't determine array size");
            }
          }
          values = new float[x * y];
        }
        //System.out.println( "BDS  constant values x ="+ x +" y ="+ y );
        for (int i = 0; i < values.length; i++) {
          values[i] = ref;
        }
      }
    }
  }  // end Grib1BinaryDataSection

  /**
   * Rearrange the data array using the scanning mode.
   */
  private void scanningModeCheck(int scanMode, int Xlength) {

    if (Xlength == -1) // old code
      return;

    // Mode  0 +x, -y, adjacent x, adjacent rows same dir
    // Mode  64 +x, +y, adjacent x, adjacent rows same dir
    if ((scanMode == 0) || (scanMode == 64)) {
      return;
      // Mode  128 -x, -y, adjacent x, adjacent rows same dir
      // Mode  192 -x, +y, adjacent x, adjacent rows same dir
      // change -x to +x ie east to west -> west to east
    } else if ((scanMode == 128) || (scanMode == 192)) {
      float tmp;
      int mid = (int) Xlength / 2;
      //System.out.println( "Xlength =" +Xlength +" mid ="+ mid );
      for (int index = 0; index < values.length; index += Xlength) {
        for (int idx = 0; idx < mid; idx++) {
          tmp = values[index + idx];
          values[index + idx] = values[index + Xlength - idx - 1];
          values[index + Xlength - idx - 1] = tmp;
          //System.out.println( "switch " + (index + idx) + " " +
          //(index + Xlength -idx -1) );
        }
      }
      return;
    }
  }                            // end of scanningModeCheck

  /**
   * Convert bits (nb) to Unsigned Int .
   *
   * @param nb
   * @param raf
   * @return int of BinaryDataSection section
   * @throws IOException
   */
  private int bits2UInt(int nb, RandomAccessFile raf) throws IOException {
    int bitsLeft = nb;
    int result = 0;

    if (bitPos == 0) {
      bitBuf = raf.read();
      bitPos = 8;
    }

    while (true) {
      int shift = bitsLeft - bitPos;
      if (shift > 0) {
        // Consume the entire buffer
        result |= bitBuf << shift;
        bitsLeft -= bitPos;

        // Get the next byte from the RandomAccessFile
        bitBuf = raf.read();
        bitPos = 8;
      } else {
        // Consume a portion of the buffer
        result |= bitBuf >> -shift;
        bitPos -= bitsLeft;
        bitBuf &= 0xff >> (8 - bitPos);  // mask off consumed bits

        return result;
      }
    }                                        // end while
  }                                            // end bits2Int

  /**
   * Grid values as an array of float.
   *
   * @return array of grid values
   */
  public final float[] getValues() {
    return values;
  }

}  // end class Grib1BinaryDataSection


