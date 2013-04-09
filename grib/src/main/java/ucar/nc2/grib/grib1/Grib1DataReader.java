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

import ucar.nc2.grib.GribNumbers;
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Decodes the GRIB1 binary data record
 *
 * @author John
 * @since 9/8/11
 */
public class Grib1DataReader {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1DataReader.class);

  ///////////////////////////////// Grib1Data

  /*  From WMO Manual on Codes  I-2 bi - 5

  Data shall be coded in the form of non-negative scaled differences from a reference value.
Notes:
(1) The reference value is normally the minimum value of the data set which is represented.
(2) The actual value Y (in the units of Code table 2) is linked to the coded value X, the reference
value R, the binary scale factor E and the decimal scale factor D by means of the following formula:

  Y * 10 ^ D = R + X * 2 ^ E

(3) When second-order grid-point packing is indicated, the actual value Y (in the units of Code table 2)
    is linked to the coded values Xi and Xj, the reference value R, the binary scale factor E and the
    decimal scale factor D by means of the following formula:

  Y * 10 ^ D = R + (Xi + Xj) * 2 ^ E

*/



  static private final boolean staticMissingValueInUse = true;
  static private final float staticMissingValue = Float.NaN;

  ///////////////////////////////////// Grib1BinaryDataSection

  private final int decimalScale;
  private final int scanMode;
  private final int nx, ny;
  private final long startPos;

  /**
   *
   * @param decimalScale
   * @param scanMode
   * @param nx
   * @param ny
   * @param startPos starting offset of the binary data section
   */
  public Grib1DataReader(int decimalScale, int scanMode, int nx, int ny, long startPos) {
    this.decimalScale = decimalScale;
    this.scanMode = scanMode;
    this.nx = nx;
    this.ny = ny;
    this.startPos = startPos;
  }

  public float[] getData(RandomAccessFile raf, byte[] bitmap) throws IOException {
    raf.seek(startPos); // go to the data section

    // octets 1-3 (section length)
    int msgLength = GribNumbers.uint3(raf);

    // octet 4, 1st half (packing flag)
    int unusedbits = raf.read();
    if ((unusedbits & 192) != 0) {
      logger.error("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing for {}", raf.getLocation());
      throw new IllegalStateException("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing ");
    }
    unusedbits = unusedbits & 15;

    // octets 5-6 (binary scale factor)
    int binscale = GribNumbers.int2(raf);

    // octets 7-10 (reference point = minimum value)
    float refvalue = GribNumbers.float4(raf);

    // octet 11 (number of bits per value)
    int numbits = raf.read();
    boolean isConstant =  (numbits == 0);

    // *** read values *******************************************************

    double pow10 =  Math.pow(10.0, -decimalScale);
    float ref = (float) (pow10 * refvalue);
    float scale = (float) (pow10 * Math.pow(2.0, binscale));
    float[] values;

    if (bitmap != null) {
      if (8 * bitmap.length < nx * ny) {
        logger.error("Bitmap section length = {} != grid length {} ({},{}) for {}", bitmap.length, nx * ny, nx, ny, raf.getLocation());
        throw new IllegalStateException("Bitmap section length!= grid length");
      }
      BitReader reader = new BitReader(raf, startPos+11);
      values = new float[nx * ny];
      for (int i = 0; i <nx * ny; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          if (!isConstant) {
            values[i] = ref + scale * reader.bits2UInt(numbits);
          } else {  // rdg - added this to handle a constant valued parameter
            values[i] = ref;
          }
        } else {
          values[i] = staticMissingValue;
        }
      }
      scanningModeCheck(values, scanMode, nx);

    } else {  // bitmap is null
      if (!isConstant) {
        if (nx != -1 && ny != -1) {
          values = new float[nx * ny];
        } else {
          values = new float[((msgLength - 11) * 8 - unusedbits) / numbits];
        }
        BitReader reader = new BitReader(raf, startPos+11);
        for (int i = 0; i < values.length; i++) {
          values[i] = ref + scale * reader.bits2UInt(numbits);
        }
        scanningModeCheck(values, scanMode, nx);

      } else {                     // constant valued - same min and max
        values = new float[nx * ny];
        for (int i = 0; i < values.length; i++) {
          values[i] = ref;
        }
      }
    }

    return values;
  }

  /**
   * Rearrange the data array using the scanning mode.
   *
   * Flag/Code table 8 – Scanning mode
    Bit   No. Value Meaning
      1   0   Points scan in +i direction
          1   Points scan in –i direction
      2   0   Points scan in –j direction
          1   Points scan in +j direction
      3   0   Adjacent points in i direction are consecutive
          1   Adjacent points in j direction are consecutive
   */
  private void scanningModeCheck(float[] data, int scanMode, int Xlength) {

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
      int mid = Xlength / 2;
      for (int index = 0; index < data.length; index += Xlength) {
        for (int idx = 0; idx < mid; idx++) {
          tmp = data[index + idx];
          data[index + idx] = data[index + Xlength - idx - 1];
          data[index + Xlength - idx - 1] = tmp;
        }
      }
      return;
    }
  }

}


////////////////////
// old stuff

    /*
   * Reads the Grib data
   *
   * @param gdsOffset PDS offset into file.
   * @param dataOffset GDS offset into file.
   * @return float[]
   * @throws java.io.IOException
   *
  public final float[] getData(long gdsOffset, long dataOffset, int decimalScale, boolean bmsExists) throws IOException {

    //long start = System.currentTimeMillis();
    Grib1GridDefinitionSection gds = null;
    Grib1GDSVariables gdsv = null;
    boolean isThin = false;
    try {
      // check for thin grids
      if ( gdsOffset != -1 ) {
        raf.seek(gdsOffset);
        gds = new Grib1GridDefinitionSection(raf);
        gdsv = gds.getGdsVars();
        int PVorPL = gdsv.getPVorPL();
        int NV = gdsv.getNV();
        isThin = false;
        if (PVorPL != 255 && (NV == 0 || NV == 255) ) {
          isThin = true;
        }
      }
    } catch (NoValidGribException nvge) {
      log.debug("gds exception was caught");
    }
    // seek data start
    raf.seek(dataOffset);

    // Need section 3 and 4 to read/interpet the data
    Grib1BitMapSection bms = null;
    if (bmsExists) {
      // read Bit Mapped Section 3
      bms = new Grib1BitMapSection(raf);
    }

      // read Binary Data Section 4

//      Grib1BinaryDataSection bds =
//          new Grib1BinaryDataSection(raf, decimalScale, bms, gdsv.getScanMode(), gdsv.getNx(), gdsv.getNy() );
//      if (isThin && expandGrib1ThinGrids) {
//        QuasiRegular qr = new QuasiRegular(bds.getValues(), gdsv.getParallels(), gdsv.getNx(), gdsv.getNy() );
//        return qr.getData();
//      } else {
//        return bds.getValues();
//      }

      if ( !isThin ) {  // 99% path
        Grib1SectionBinaryData bds = new Grib1SectionBinaryData(raf, decimalScale, bms, gdsv.getScanMode(), gdsv.getNx(), gdsv.getNy() );
        return bds.getValues();
      }
      // Process thin grids
      Grib1SectionBinaryData bds =
          new Grib1SectionBinaryData(raf, decimalScale, bms, gdsv.getScanMode(), -1, gdsv.getNy() );
      if (expandGrib1ThinGrids) {
        QuasiRegular qr = new QuasiRegular(bds.getValues(), gdsv.getParallels(), gdsv.getNx(), gdsv.getNy() );
        return qr.getData();
      } else { // return unexpanded values, does not work in CDM stack code
        return bds.getValues();
      }

  }  */