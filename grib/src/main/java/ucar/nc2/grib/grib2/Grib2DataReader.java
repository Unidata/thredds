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

import ucar.jpeg.jj2000.j2k.decoder.Grib2JpegDecoder;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Reads the data from one grib record.
 *
 * @author caron
 * @since 4/2/11
 */
public class Grib2DataReader {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2DataReader.class);

  private static final int bitsmv1[] = new int[31];
  static {
    for (int i = 0; i < 31; i++) {
      bitsmv1[i] = (int) java.lang.Math.pow((double) 2, (double) i) - 1;
    }
  }

  ///////////////////////////////////////////////

  private final int dataTemplate;
  private final int totalNPoints; // gds: number of points
  private final int dataNPoints; // drs: number of data points where one or more values are specified in Section 7 when a bit map is presen
  private final int scanMode;
  private final int nx;
  private final long startPos;
  private final int dataLength;

  private byte[] bitmap;

  public Grib2DataReader(int dataTemplate, int totalNPoints, int dataNPoints, int scanMode, int nx, long startPos, int dataLength) {
    this.dataTemplate = dataTemplate;
    this.totalNPoints = totalNPoints;
    this.dataNPoints = dataNPoints;
    this.scanMode = scanMode;
    this.nx = nx;
    this.startPos = startPos;
    this.dataLength = dataLength;
  }

  public float[] getData(RandomAccessFile raf, byte[] bitmap, Grib2Drs gdrs) throws IOException {
    this.bitmap = bitmap;

    if (bitmap != null) { // is bitmap ok ?
      if (bitmap.length * 8 < totalNPoints) { // gdsNumberPoints == nx * ny ??
        System.out.printf("Bitmap section length = %d != grid length %d (%d,%d)", bitmap.length, totalNPoints, nx, totalNPoints/nx);
        throw new IllegalStateException("Bitmap section length!= grid length %");
      }
    }

    raf.seek(startPos+5); // skip past first 5 bytes in data section, now ready to read

    float[] data;
    switch (dataTemplate) {
      case 0:
        data = getData0(raf, (Grib2Drs.Type0) gdrs);
        break;
      case 2:
        data = getData2(raf, (Grib2Drs.Type2) gdrs);
        break;
      case 3:
        data = getData3(raf, (Grib2Drs.Type3) gdrs);
        break;
      case 40:
        data = getData40(raf, (Grib2Drs.Type40) gdrs);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported DRS type = " + dataTemplate);
    }

    //int scanMode = gds.getGds().getScanMode();
    //int nx = gds.getGds().getNx();  // needs some smarts for different type Grids
    scanningModeCheck(data, scanMode, nx);

    return data;
  }

  static private final boolean staticMissingValueInUse = true;
  static private final float staticMissingValue = Float.NaN;

  float getMissingValue(Grib2Drs.Type2 gdrs) {
    int mvm = gdrs.missingValueManagement;

    float mv = Float.NaN;
    if (staticMissingValueInUse || mvm == 0) {
      mv = Float.NaN;
    } else if (mvm == 1) {
      mv = gdrs.primaryMissingValue;
    } else if (mvm == 2) {
      mv = gdrs.secondaryMissingValue;
    }
    return mv;
  }


  // Grid point data - simple packing
  private float[] getData0(RandomAccessFile raf, Grib2Drs.Type0 gdrs) throws IOException {
    int nb = gdrs.numberOfBits;
    int D = gdrs.decimalScaleFactor;
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    float R = gdrs.referenceValue;
    int E = gdrs.binaryScaleFactor;
    float EE = (float) java.lang.Math.pow( 2.0, (double) E);

    // LOOK: can # datapoints differ from bitmap and data ?
    // dataPoints are number of points encoded, it could be less than the
    // totalNPoints in the grid record if bitMap is used, otherwise equal
    float[] data = new float[totalNPoints];

    //  Y * 10**D = R + (X1 + X2) * 2**E
    //   E = binary scale factor
    //   D = decimal scale factor
    //   R = reference value
    //   X1 = 0
    //   X2 = scaled encoded value
    //   data[ i ] = (R + ( X1 + X2) * EE)/DD ;

    BitReader reader = new BitReader(raf, startPos+5);
    if (bitmap == null) {
      for (int i = 0; i < totalNPoints; i++) {
        //data[ i ] = (R + ( X1 + X2) * EE)/DD ;
        data[i] = (R + reader.bits2UInt(nb) * EE) / DD;
      }
    } else {
      for (int i = 0; i < totalNPoints; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          data[i] = (R + reader.bits2UInt(nb) * EE) / DD;
        } else {
          data[i] = staticMissingValue;  // LOOK ??
          //data[i] = R / DD;
        }
      }
    }

    return data;
  }

  // Grid point data - complex packing
  private float[] getData2(RandomAccessFile raf, Grib2Drs.Type2 gdrs) throws IOException {
    int mvm = gdrs.missingValueManagement;
    float mv = getMissingValue(gdrs);

    float DD = (float) java.lang.Math.pow((double) 10, (double) gdrs.decimalScaleFactor);
    float R = gdrs.referenceValue;
    float EE = (float) java.lang.Math.pow( 2.0, (double) gdrs.binaryScaleFactor);
    float ref_val = R / DD;

    int NG = gdrs.numberOfGroups;
    if (NG == 0) {
      float[] data = new float[totalNPoints];
      for (int i = 0; i < totalNPoints; i++) data[i] = ref_val;
      return data;
    }

    BitReader reader = new BitReader(raf, startPos+5);

    // 6-xx  Get reference values for groups (X1's)
    int[] X1 = new int[NG];
    int nb = gdrs.numberOfBits;
    if (nb != 0) {
      for (int i = 0; i < NG; i++)
        X1[i] = (int) reader.bits2UInt(nb);
    }

    // [xx +1 ]-yy Get number of bits used to encode each group
    int[] NB = new int[NG];
    nb = gdrs.bitsGroupWidths;
    if (nb != 0) {
      reader.incrByte();
      for (int i = 0; i < NG; i++)
        NB[i] = (int) reader.bits2UInt(nb);
    }

    // [yy +1 ]-zz Get the scaled group lengths using formula
    //     Ln = ref + Kn * len_inc, where n = 1-NG,
    //          ref = referenceGroupLength, and  len_inc = lengthIncrement

    int[] L = new int[NG];
    int ref = gdrs.referenceGroupLength;
    int len_inc = gdrs.lengthIncrement;
    nb = gdrs.bitsScaledGroupLength;

    reader.incrByte();
    for (int i = 0; i < NG; i++)
      L[i] = ref + (int) reader.bits2UInt(nb) * len_inc;
    L[NG - 1] = gdrs.lengthLastGroup; // enter Length of Last Group


    float[] data = new float[totalNPoints];

    // [zz +1 ]-nn get X2 values and calculate the results Y using formula

//              Y = R + [(X1 + X2) * (2 ** E) * (10 ** D)]
//               WHERE:
//                     Y = THE VALUE WE ARE UNPACKING
//                     R = THE REFERENCE VALUE (FIRST ORDER MINIMA)
//                    X1 = THE PACKED VALUE
//                    X2 = THE SECOND ORDER MINIMA
//                     E = THE BINARY SCALE FACTOR
//                     D = THE DECIMAL SCALE FACTOR
    int count = 0;
    reader.incrByte();
    for (int i = 0; i < NG; i++) {
      for (int j = 0; j < L[i]; j++) {
        if (NB[i] == 0) {
          if (mvm == 0) {  // X2 = 0
            data[count++] = (R + X1[i] * EE) / DD;
          } else { //if (mvm == 1) || (mvm == 2 )
            data[count++] = mv;
          }
        } else {
          int X2 = (int) reader.bits2UInt(NB[i]);
          if (mvm == 0) {
            data[count++] = (R + (X1[i] + X2) * EE) / DD;
          } else { //if (mvm == 1) || (mvm == 2 )
            // X2 is also set to missing value if all bits set to 1's
            if (X2 == bitsmv1[NB[i]]) {
              data[count++] = mv;
            } else {
              data[count++] = (R + (X1[i] + X2) * EE) / DD;
            }
          }
        }
      }  // end for j
    }  // end for i

    if (bitmap != null) {
      int idx = 0;
      float[] tmp = new float[totalNPoints];
      for (int i = 0; i < totalNPoints; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          tmp[i] = data[idx++];
        } else {
          tmp[i] = mv;
        }
      }
      data = tmp;
    }

    return data;
  }

  // Grid point data - complex packing and spatial differencing
  private float[] getData3(RandomAccessFile raf, Grib2Drs.Type3 gdrs) throws IOException {
    int mvm = gdrs.missingValueManagement;
    float mv = getMissingValue(gdrs);

    float DD = (float) java.lang.Math.pow((double) 10, (double) gdrs.decimalScaleFactor);
    float R = gdrs.referenceValue;
    float EE = (float) java.lang.Math.pow( 2.0, (double) gdrs.binaryScaleFactor);
    float ref_val = R / DD;

    BitReader reader = new BitReader(raf, startPos+5);

    int ival1 = 0;
    int ival2 = 0;
    int minsd = 0;

    // [6-ww]   1st values of undifferenced scaled values and minimums
    int os = gdrs.orderSpatial;
    int nbitsd = gdrs.descriptorSpatial;
    int sign;
    // ds is number of bytes, convert to bits -1 for sign bit
    nbitsd = nbitsd * 8;
    if (nbitsd > 0) {         // first order spatial differencing g1 and gMin
      sign = (int) reader.bits2UInt(1);
      ival1 = (int) reader.bits2UInt(nbitsd - 1);
      if (sign == 1) {
        ival1 = -ival1;
      }
      if (os == 2) {  //second order spatial differencing h1, h2, hMin
        sign = (int) reader.bits2UInt(1);
        ival2 = (int) reader.bits2UInt(nbitsd - 1);
        if (sign == 1) {
          ival2 = -ival2;
        }
      }
      sign = (int) reader.bits2UInt(1);
      minsd = (int) reader.bits2UInt(nbitsd - 1);
      if (sign == 1) {
        minsd = -minsd;
      }

    } else {
      float[] data = new float[totalNPoints];
      for (int i = 0; i < totalNPoints; i++) data[i] = mv;
      return data;
    }

    int NG = gdrs.numberOfGroups;
    if (NG == 0) {
      float[] data = new float[totalNPoints];
      for (int i = 0; i < totalNPoints; i++)
        data[i] = ref_val;
      return data;
    }

    // [ww +1]-xx  Get reference values for groups (X1's)
    // X1 == gref
    int[] X1 = new int[NG]; // initialized to zero
    int nb = gdrs.numberOfBits;
    if (nb != 0) {
      reader.incrByte();
      for (int i = 0; i < NG; i++) {
        X1[i] = (int) reader.bits2UInt(nb);
      }
    }

    // [xx +1 ]-yy Get number of bits used to encode each group
    // NB == gwidth
    int[] NB = new int[NG]; // initialized to zero
    nb = gdrs.bitsGroupWidths;
    if (nb != 0) {
      reader.incrByte();
      for (int i = 0; i < NG; i++) {
        NB[i] = (int) reader.bits2UInt(nb);
      }
    }

    int referenceGroupWidths = gdrs.referenceGroupWidths;
    for (int i = 0; i < NG; i++) {
      NB[i] += referenceGroupWidths;
    }

    // [yy +1 ]-zz Get the scaled group lengths using formula
    //     Ln = ref + Kn * len_inc, where n = 1-NG,
    //          ref = referenceGroupLength, and  len_inc = lengthIncrement

    int[] L = new int[NG];   // initialized to zero
    int referenceGroupLength = gdrs.referenceGroupLength;
    nb = gdrs.bitsScaledGroupLength;
    int len_inc = gdrs.lengthIncrement;

    if (nb != 0) {
      reader.incrByte();
      for (int i = 0; i < NG; i++) {
        L[i] = (int) reader.bits2UInt(nb);
      }
    }

    int totalL = 0;
    for (int i = 0; i < NG; i++) {
      L[i] = L[i] * len_inc + referenceGroupLength;
      totalL += L[i];
    }
    totalL -= L[NG - 1];
    totalL += gdrs.lengthLastGroup;

    //enter Length of Last Group
    L[NG - 1] = gdrs.lengthLastGroup;

    // test
    if (mvm != 0) {
      if (totalL != totalNPoints) {
        log.warn("NPoints != gds.nPts: " + totalL +"!="+ totalNPoints);
        float[] data = new float[totalNPoints];
        for (int i = 0; i < totalNPoints; i++)
          data[i] = mv;
        return data;
      }
    } else {
      if (totalL != dataNPoints) {
        log.warn("NPoints != drs.nPts: " + totalL +"!="+ totalNPoints);
        float[] data = new float[totalNPoints];
        for (int i = 0; i < totalNPoints; i++)
          data[i] = mv;
        return data;
      }
    }

    float[] data = new float[totalNPoints];

    // [zz +1 ]-nn get X2 values and calculate the results Y using formula
//      formula used to create values,  Y * 10**D = R + (X1 + X2) * 2**E

//               Y = (R + (X1 + X2) * (2 ** E) ) / (10 ** D)]
//               WHERE:
//                     Y = THE VALUE WE ARE UNPACKING
//                     R = THE REFERENCE VALUE (FIRST ORDER MINIMA)
//                    X1 = THE PACKED VALUE
//                    X2 = THE SECOND ORDER MINIMA
//                     E = THE BINARY SCALE FACTOR
//                     D = THE DECIMAL SCALE FACTOR
    int count = 0;
    reader.incrByte();
    int dataSize = 0;
    boolean[] dataBitMap = null;
    if (mvm == 0) {
      for (int i = 0; i < NG; i++) {
        if (NB[i] != 0) {
          for (int j = 0; j < L[i]; j++)
            data[count++] = (int) reader.bits2UInt(NB[i]) + X1[i];
        } else {
          for (int j = 0; j < L[i]; j++)
            data[count++] = X1[i];
        }
      }  // end for i

    } else if (mvm == 1 || mvm == 2) {
      // don't add missing values into data but keep track of them in dataBitMap
      dataBitMap = new boolean[totalNPoints];
      dataSize = 0;
      for (int i = 0; i < NG; i++) {
        if (NB[i] != 0) {
          int msng1 = bitsmv1[NB[i]];
          int msng2 = msng1 - 1;
          for (int j = 0; j < L[i]; j++) {
            data[count] = (int) reader.bits2UInt(NB[i]);
            if (data[count] == msng1 || mvm == 2 && data[count] == msng2) {
              dataBitMap[count] = false;
            } else {
              dataBitMap[count] = true;
              data[dataSize++] = data[count] + X1[i];
            }
            count++;
          }
        } else {  // (NB[i] == 0
          int msng1 = bitsmv1[gdrs.numberOfBits];
          int msng2 = msng1 - 1;
          if (X1[i] == msng1) {
            for (int j = 0; j < L[i]; j++)
              dataBitMap[count++] = false;
            //data[count++] = X1[i];
          } else if (mvm == 2 && X1[i] == msng2) {
            for (int j = 0; j < L[i]; j++)
              dataBitMap[count++] = false;
          } else {
            for (int j = 0; j < L[i]; j++) {
              dataBitMap[count] = true;
              data[dataSize++] = X1[i];
              count++;
            }
          }
        }
      }  // end for i
    }

    // first order spatial differencing
    if (os == 1) {   // g1 and gMin
      // encoded by G(n) = F(n) - F(n -1 )
      // decoded by F(n) = G(n) + F(n -1 )
      // data[] at this point contains G0, G1, G2, ....
      data[0] = ival1;
      int itemp;
      if (mvm == 0) {           // no missing values
        itemp = totalNPoints;
      } else {
        itemp = dataSize;
      }
      for (int i = 1; i < itemp; i++) {
        data[i] += minsd;
        data[i] = data[i] + data[i - 1];
      }
    } else if (os == 2) { // 2nd order
      data[0] = ival1;
      data[1] = ival2;
      int itemp;
      if (mvm == 0) {           // no missing values
        itemp = totalNPoints;
      } else {
        itemp = dataSize;
      }
      for (int i = 2; i < itemp; i++) {
        data[i] += minsd;
        data[i] = data[i] + (2 * data[i - 1]) - data[i - 2];
      }
    }

    // formula used to create values,  Y * 10**D = R + (X1 + X2) * 2**E

    //               Y = (R + (X1 + X2) * (2 ** E) ) / (10 ** D)]
//               WHERE:
//                     Y = THE VALUE WE ARE UNPACKING
//                     R = THE REFERENCE VALUE (FIRST ORDER MINIMA)
//                    X1 = THE PACKED VALUE
//                    X2 = THE SECOND ORDER MINIMA
//                     E = THE BINARY SCALE FACTOR
//                     D = THE DECIMAL SCALE FACTOR

    if (mvm == 0) {  // no missing values
      for (int i = 0; i < data.length; i++) {
        data[i] = (R + (data[i] * EE)) / DD;
      }
    } else {         // missing value == 1  || missing value == 2
      int count2 = 0;
      float[] tmp = new float[totalNPoints];
      for (int i = 0; i < data.length; i++) {
        if (dataBitMap[i]) {
          tmp[i] = (R + (data[count2++] * EE)) / DD;
        } else { // mvm = 1 or 2
          tmp[i] = mv;
        }
      }
      data = tmp;
    }

    // bit map is used
    if (bitmap != null) {
      int idx = 0;
      float[] tmp = new float[totalNPoints];
      for (int i = 0; i < totalNPoints; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          tmp[i] = data[idx++];
        } else {
          tmp[i] = mv;
        }
      }
      data = tmp;
    }

    return data;
  }

  // Grid point data - JPEG 2000 code stream format
  public float[] getData40(RandomAccessFile raf, Grib2Drs.Type40 gdrs) throws IOException {
    // 6-xx  jpeg2000 data block to decode

    // dataPoints are number of points encoded, it could be less than the
    // totalNPoints in the grid record if bitMap is used, otherwise equal
    //int dataPoints = drs.getDataPoints();

    int nb = gdrs.numberOfBits;
    int D = gdrs.decimalScaleFactor;
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    float R = gdrs.referenceValue;
    int E = gdrs.binaryScaleFactor;
    float EE = (float) java.lang.Math.pow((double) 2.0, (double) E);

    Grib2JpegDecoder g2j = null;
    try {
      if (nb != 0) {  // there's data to decode
        String[] argv = new String[6];
        argv[0] = "-rate";
        argv[1] = Integer.toString(nb);
        argv[2] = "-verbose";
        argv[3] = "off";
        argv[4] = "-debug" ;
        argv[5] = "on" ;
        //argv[ 2 ] = "-nocolorspace" ;
        //argv[ 3 ] = "-Rno_roi" ;
        //argv[ 4 ] = "-cdstr_info" ;
        //argv[ 5 ] = "-verbose" ;
        g2j = new Grib2JpegDecoder(argv);
        // how jpeg2000.jar use to decode, used raf
        //g2j.decode(raf, length - 5);
        // jpeg-1.0.jar added method to have the data read first
        byte[] buf = new byte[dataLength - 5];
        raf.read(buf);
        g2j.decode(buf);
        gdrs.hasSignedProblem = g2j.hasSignedProblem();
      }

      float[] data = new float[totalNPoints];

      if (nb == 0) {  // no data to decoded, set to reference or  MissingValue
        for (int i = 0; i < dataNPoints; i++)
          data[i] = R;
      } else if (bitmap == null) {
        if (g2j.data.length != dataNPoints) {
          data = null;
          return data;
        }
        for (int i = 0; i < dataNPoints; i++) {
          // Y * 10^D = R + (X1 + X2) * 2^E ; // regulation 92.9.4
          //Y = (R + ( 0 + X2) * EE)/DD ;
          data[i] = (R + g2j.data[i] * EE) / DD;
        }
      } else {  // use bitmap
        for (int i = 0, j = 0; i < totalNPoints; i++) {
          if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
            data[i] = (R + g2j.data[j++] * EE) / DD;
          } else {
            data[i] = staticMissingValue;  // LOOK ??
          }
        }
      }
      return data;

    } catch (NullPointerException npe) {

      log.error("Grib2DataSection.jpeg2000Unpacking: bit rate too small nb =" + nb + " for file" + raf.getLocation());
      float[] data = new float[dataNPoints];
      for (int i = 0; i < dataNPoints; i++) {
        data[i] = staticMissingValue;  // LOOK ??
      }
      return data;
    }
  }

  // Rearrange the data array using the scanning mode.
  // LOOK ight be wrong when a quasi regular (thin) grid ??
  private void scanningModeCheck(float[] data, int scanMode, int Xlength) {
    // Mode  0 +x, -y, adjacent x, adjacent rows same dir
    // Mode  64 +x, +y, adjacent x, adjacent rows same dir
    if ((scanMode == 0) || (scanMode == 64))
      return;

      // Mode  128 -x, -y, adjacent x, adjacent rows same dir
      // Mode  192 -x, +y, adjacent x, adjacent rows same dir
      // change -x to +x ie east to west -> west to east
    if ((scanMode == 128) || (scanMode == 192)) {
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

    // else
    // scanMode == 16, 80, 144, 208 adjacent rows scan opposite dir
    float tmp;
    int mid = Xlength / 2;
    for (int index = 0; index < data.length; index += Xlength) {
      int row = index / Xlength;
      if (row % 2 == 1) {  // odd numbered row, calculate reverse index
        for (int idx = 0; idx < mid; idx++) {
          tmp = data[index + idx];
          data[index + idx] = data[index + Xlength - idx - 1];
          data[index + Xlength - idx - 1] = tmp;
        }
      }
    }
  }

}
