/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Reads the data from one grib2 record.
 * Original code almost for sure came from GEMPAK, but the lineage is unknown.
 * @see "https://raw.githubusercontent.com/Unidata/gempak/master/extlibs/NDFD/mdlg2dec/unpk_cmplx.f"
 * @see "http://slosh.nws.noaa.gov/svn/degrib/vendor/grib2_unpacker/current/unpksecdif.f"
 * @see "unpk_complex in wgrib2 code"
 * @author caron
 * @since 4/2/11
 */
public class Grib2DataReader2 {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2DataReader2.class);

  private static final int bitsmv1[] = new int[31];
  static {
    for (int i = 0; i < 31; i++) {
      bitsmv1[i] = (int) java.lang.Math.pow((double) 2, (double) i) - 1;
    }
  }

  ///////////////////////////////////////////////

  private final int dataTemplate;
  private final int totalNPoints; // gds: number of points
  private final int dataNPoints; // drs: number of data points where one or more values are specified in Section 7 when a bit map is present
  private final int scanMode;
  private final int nx;
  private final long startPos;
  private final int dataLength;

  private int bitmapIndicator;
  private byte[] bitmap;

  public Grib2DataReader2(int dataTemplate, int totalNPoints, int dataNPoints, int scanMode, int nx, long startPos, int dataLength) {
    this.dataTemplate = dataTemplate;
    this.totalNPoints = totalNPoints;
    this.dataNPoints = dataNPoints;
    this.scanMode = scanMode;
    this.nx = nx;
    this.startPos = startPos;
    this.dataLength = dataLength;
  }

  /*
  Code Table Code table 5.0 - Data representation template number (5.0)
    0: Grid point data - simple packing
    1: Matrix value at grid point - simple packing
    2: Grid point data - complex packing
    3: Grid point data - complex packing and spatial differencing
    4: Grid point data - IEEE floating point data
   40: Grid point data - JPEG 2000 code stream format
   41: Grid point data - Portable Network Graphics (PNG)
   50: Spectral data - simple packing
   51: Spherical harmonics data - complex packing
   61: Grid point data - simple packing with logarithm pre-processing
  200: Run length packing with level values
  65535: Missing
   */

  public float[] getData(RandomAccessFile raf, Grib2SectionBitMap bitmapSection, Grib2Drs gdrs) throws IOException {
    this.bitmap = bitmapSection.getBitmap(raf);
    this.bitmapIndicator = bitmapSection.getBitMapIndicator();

    if (bitmap != null) { // is bitmap ok ?
      if (bitmap.length * 8 < totalNPoints) { // gdsNumberPoints == nx * ny ??
        log.warn("Bitmap section length = {} != grid length {} ({},{})", bitmap.length, totalNPoints, nx, totalNPoints/nx);
        throw new IllegalStateException("Bitmap section length!= grid length");
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
      case 41:
        data = getData41(raf, (Grib2Drs.Type0) gdrs);
        break;
      case 50002:
        data = getData50002(raf, (Grib2Drs.Type50002) gdrs);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported DRS type = " + dataTemplate);
    }

    //int scanMode = gds.getGds().getScanMode();
    //int nx = gds.getGds().getNx();  // needs some smarts for different type Grids
    scanningModeCheck(data, scanMode, nx);

    return data;
  }

  public int[] getRawData(RandomAccessFile raf, Grib2SectionBitMap bitmapSection, Grib2Drs gdrs) throws IOException {
    this.bitmap = bitmapSection.getBitmap(raf);
    this.bitmapIndicator = bitmapSection.getBitMapIndicator();

    if (bitmap != null) { // is bitmap ok ?
      if (bitmap.length * 8 < totalNPoints) { // gdsNumberPoints == nx * ny ??
        log.warn("Bitmap section length = {} != grid length {} ({},{})", bitmap.length, totalNPoints, nx, totalNPoints/nx);
        throw new IllegalStateException("Bitmap section length!= grid length");
      }
    }

    raf.seek(startPos+5); // skip past first 5 bytes in data section, now ready to read

    if (dataTemplate != 40) return null;

    // LOOK jpeg2k only
    return getData40raw(raf, (Grib2Drs.Type40) gdrs);
  }

  static private final boolean staticMissingValueInUse = true;
  static private final float staticMissingValue = Float.NaN;

  float getMissingValue(Grib2Drs.Type2 gdrs) {
    int mvm = gdrs.missingValueManagement;

    float mv;
    if (staticMissingValueInUse || mvm == 0) {
      mv = Float.NaN;
    } else if (mvm == 1) {
      mv = gdrs.primaryMissingValue;
    } else if (mvm == 2) {
      mv = gdrs.secondaryMissingValue;
    }
    return mv;
  }

  /*
  92.9.4 Data shall be coded in the form of non-negative scaled differences from a reference value
  of the whole field plus, if applicable, a local reference value.
  Notes:
  (1) A reference value is normally the minimum value of the data set which is represented.

  (2) For grid-point values, complex packing features are intended to reduce the whole size of the GRIB message (data
  compression without loss of information with respect to simple packing). The basic concept is to reduce data size
  thanks to local redundancy. This is achieved just before packing, by splitting the whole set of scaled data values into
  groups, on which local references (such as local minima) are removed. It is done with some overhead, because extra
  descriptors are needed to manage the groups’ characteristics. An optional pre-processing of the scaled values (spatial
  differencing) may also be applied before splittig into groups, and combined methods, along with use of alternate row
  scanning mode, are very efficient on interpolated data.

  (3) For spectral data, complex packing is provided for better accuracy of packing. This is because many spectral coefficients
  have small values (regardless of the sign), especially for large wave numbers. The first principle is not to pack a subset of
  coefficients, associated with small wave numbers so that the amplitude of the packed coefficients is reduced. The second
  principle is to apply an operator to the remaining part of the spectrum: with appropriate tuning it leads to a more
  homogeneous set of values to pack.

  (4) The original data value Y (in the units of Code table 4.2, unless Notes in Code table 4.10 apply) can be recovered with the formula:
      Y × 10^D = R + (X1 + X2) × 2^E
    For simple packing and all spectral data
      E = Binary scale factor
      D = Decimal scale factor
      R = Reference value of the whole field
      X1 = 0
      X2 = Scaled (encoded) value.
    For complex grid-point packing schemes, E, D and R are as above, but
      X1 = Reference value (scaled integer) of the group the data value belongs to
      X2 = Scaled (encoded) value with the group reference value (X1) removed.
   */


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
          data[i] = staticMissingValue;
          //data[i] = R / DD;
        }
      }
    }

    return data;
  }

  /*
 Data template 7.2 – Grid point data – complex packing
 Note: For most templates, details of the packing process are described in Regulation 92.9.4.
 Octet No. Contents
    6–xx    NG group reference values (X1 in the decoding formula), each of which is encoded using
 the number of bits specified in octet 20 of data representation template 5.0. Bits set to zero
 shall be appended as necessary to ensure this sequence of numbers ends on an octet boundary
  [xx+1]–yy NG group widths, each of which is encoded using the number of bits specified in octet 37
 of data representation template 5.2. Bits set to zero shall be appended as necessary to
 ensure this sequence of numbers ends on an octet boundary
  [yy+1]–zz NG scaled group lengths, each of which is encoded using the number of bits specified in
 octet 47 of data representation template 5.2. Bits set to zero shall be appended as necessary
 to ensure this sequence of numbers ends on an octet boundary (see Note 14 of data
 representation template 5.2)
  [zz+1]–nn Packed values (X2 in the decoding formula), where each value is a deviation from its respective
 group reference value

 Notes:
 (1) Group descriptors mentioned above may not be physically present; if associated field width is 0.
 (2) Group lengths have no meaning for row by row packing; for consistency, associated field width should then be
 encoded as 0. So no specific test for row by row case is mandatory at decoding software level to handle
 encoding/decoding of group descriptors.
 (3) Scaled group lengths, if present, are encoded for each group. But the true last group length (unscaled) should be taken
 from data representation template.
 (4) For groups with a constant value, associated field width is 0, and no incremental data are physically present.
   */
  private float[] getData2(RandomAccessFile raf, Grib2Drs.Type2 gdrs) throws IOException {
    int mvm = gdrs.missingValueManagement;
    float mv = getMissingValue(gdrs);

    float DD = (float) java.lang.Math.pow((double) 10, (double) gdrs.decimalScaleFactor);
    float R = gdrs.referenceValue;
    float EE = (float) java.lang.Math.pow( 2.0, (double) gdrs.binaryScaleFactor);
    float ref_val = R / DD;

    int NG = gdrs.numberOfGroups;
    if (NG == 0) {
      return nGroups0(bitmapIndicator, ref_val, mv);
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


  /* from wgrib unpk_complex():

    p = sec[5];                               // drs
    ref_val = ieee2flt(p+11);                 // R
    factor_2 = Int_Power(2.0, int2(p+15));    // 2^E
    factor_10 = Int_Power(10.0, -int2(p+17)); // 10^-D
    ref_val *= factor_10;                     // R/10^D
    factor = factor_2 * factor_10;            // 2^E*10^-D
    nbits = p[19];                            // NB
    ngroups = uint4(p+31);                    // NG
    bitmap_flag = code_table_6_0(sec);        // bitmap->indicator
    ctable_5_6 = code_table_5_6(sec);

    bitmap_flag = code_table_6_0(sec); // == gr.getBitmapSection().getBitMapIndicator();
    if (ngroups == 0) {
	      if (bitmap_flag == 255) {
            for (i = 0; i < ndata; i++) data[i] = ref_val;
            return 0;
        }
        if (bitmap_flag == 0 || bitmap_flag == 254) {
            mask_pointer = sec[6] + 6;                              // probably bitmask is here ??
            mask = 0;
            for (i = 0; i < ndata; i++) {
                if ((i & 7) == 0) mask = *mask_pointer++;           // get next byte ??
                data[i] = (mask & 128) ?  ref_val : UNDEFINED;      // bit 7 set
                mask <<= 1;                                         // shift
            }
            return 0;
        }
        fatal_error("unknown bitmap", "");
    }
   */
  private float[] nGroups0(int bitmap_flag, float ref, float mv1) {
    float[] data = new float[totalNPoints];
    if (bitmap_flag == 255) {
      for (int i = 0; i < totalNPoints; i++) data[i] = ref;

    } else if (bitmap_flag == 0 || bitmap_flag == 254) {
      int mask = 0;
      int mask_pointer  = 0;
      for (int i = 0; i < totalNPoints; i++) {
        if ((i & 7) == 0) {
          mask = bitmap[mask_pointer];
          mask_pointer++;
        }
        data[i] = ((mask & 128) == 0) ?  ref : mv1;
        mask <<= 1;
      }

    } else {
      throw new IllegalArgumentException("unknown bitmap type ="+bitmap_flag);
    }
    return data;
  }

  /*
  Data template 7.3 – Grid point data – complex packing and spatial differencing
  Note: For most templates, details of the packing process are described in Regulation 92.9.4.
  Octet No. Contents
  6–ww      First value(s) of original (undifferenced) scaled data values, followed by the overall minimum
  of the differences. The number of values stored is 1 greater than the order of differentiation,
  and the field width is described at octet 49 of data representation template 5.3 (see Note 1)
  [ww+1]–xx NG group reference values (X1 in the decoding formula), each of which is encoded using the number
  of bits specified in octet 20 of data representation template 5.0. Bits set to zero shall be
  appended where necessary to ensure this sequence of numbers ends on an octet boundary
  [xx+1]–nn Same as for data representation template 7.2

  Notes:
  (1) Referring to the notation in Note 1 of data representation template 5.3, at order 1, the values stored in octets 6–ww are
      g1 and gmin. At order 2, the values stored are h1, h2, and hmin.
  (2) Extra descriptors related to spatial differencing are added before the splitting descriptors, to reflect the separation
      between the two approaches. It enables to share software parts between cases with and without spatial differencing.
  (3) The position of overall minimum after initial data values is a choice that enables less software management.
  (4) Overall minimum will be negative in most cases. First bit should indicate the sign: 0 if positive, 1 if negative.
   */
  private float[] getData3(RandomAccessFile raf, Grib2Drs.Type3 gdrs) throws IOException {
    int mvm = gdrs.missingValueManagement;
    float mv = getMissingValue(gdrs);

    float DD = (float) java.lang.Math.pow((double) 10, (double) gdrs.decimalScaleFactor);
    float R = gdrs.referenceValue;
    float EE = (float) java.lang.Math.pow( 2.0, (double) gdrs.binaryScaleFactor);
    float ref_val = R / DD;

    int NG = gdrs.numberOfGroups;
    if (NG == 0) {
      return nGroups0(bitmapIndicator, ref_val, mv);
    }

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
    } else if (mvm == 1 || mvm == 2) {         // missing value == 1  || missing value == 2
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
    float EE = (float) java.lang.Math.pow( 2.0, (double) E);
    float ref_val = R / DD;

    Grib2JpegDecoder g2j = null;
    // try {
      if (nb != 0) {  // there's data to decode
        g2j = new Grib2JpegDecoder(nb, false);
        byte[] buf = new byte[dataLength - 5];
        raf.readFully(buf);
        g2j.decode(buf);
        gdrs.hasSignedProblem = g2j.hasSignedProblem();
      }

      float[] result = new float[totalNPoints];

      // no data to decode, set to reference value
      if (nb == 0) {
        for (int i = 0; i < dataNPoints; i++)
          result[i] = ref_val;
        return result;
      }

      int[] idata = g2j.getGdata();
      if (bitmap == null) { // must be one decoded value in idata for every expected data point
        if (idata.length != dataNPoints) {
          log.debug("Number of points in the data record {} != {} expected from GDS", idata.length, dataNPoints);
          throw new IllegalStateException("Number of points in the data record {} != expected from GDS");
        }

        for (int i = 0; i < dataNPoints; i++) {
          // Y * 10^D = R + (X1 + X2) * 2^E ; // regulation 92.9.4
          // Y = (R + ( 0 + X2) * EE)/DD ;
          result[i] = (R + idata[i] * EE) / DD;
        }
        return result;

      } else {  // use bitmap to skip missing values
        for (int i = 0, j = 0; i < totalNPoints; i++) {
          if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
            if (j >= idata.length) {
              System.out.printf("HEY jj2000 data count %d < bitmask count %d, i=%d, totalNPoints=%d%n", idata.length, j, i, totalNPoints);
              break;
            }
            int indata = idata[j];
            result[i] = (R + indata * EE) / DD;
            j++;
          } else {
            result[i] = staticMissingValue;
          }
        }
      }
      return result;

    /* } catch (NullPointerException npe) {

      log.error("Grib2DataReader2.jpeg2000Unpacking: bit rate too small nb =" + nb + " for file" + raf.getLocation());
      float[] data = new float[dataNPoints];
      for (int i = 0; i < dataNPoints; i++) {
        data[i] = staticMissingValue;  // LOOK ??
      }
      return data;
    } */
  }

    // Grid point data - JPEG 2000 code stream format
  public int[] getData40raw(RandomAccessFile raf, Grib2Drs.Type40 gdrs) throws IOException {
    int nb = gdrs.numberOfBits;
    if (nb == 0) return null;
    int missing_value = (2 << nb - 1) - 1;       // all ones - reserved for missing value

    Grib2JpegDecoder g2j;
    g2j = new Grib2JpegDecoder(nb, false);
    byte[] buf = new byte[dataLength - 5];
    raf.readFully(buf);
    g2j.decode(buf);
    gdrs.hasSignedProblem = g2j.hasSignedProblem();

    int[] idata = g2j.getGdata();

    if (bitmap == null) { // must be one decoded value in idata for every expected data point
      if (idata.length != totalNPoints) {
        log.debug("Number of points in the data record {} != {} expected from GDS", idata.length, totalNPoints);
        return null;
      }
      return idata;

    } else {  // use bitmap to skip missing values
      int[] result = new int[totalNPoints];

      for (int i = 0, j = 0; i < totalNPoints; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          if (j >= idata.length) {
            System.out.printf("HEY jj2000 data count %d < bitmask count %d, i=%d, totalNPoints=%d%n", idata.length, j, i, totalNPoints);
            break;
          }
          result[i] = idata[j];
          j++;
        } else {
          result[i] = missing_value;
        }
      }
      return result;
    }

  }

  // Loosely based on code by earl.barker.ctr AT us.af.mil, but moved from
  // anceient version of Grib support.
  // Code taken from esupport ticket ZVT-415274
  public float[] getData41(RandomAccessFile raf, Grib2Drs.Type0 gdrs) throws IOException {
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

    // no data to decode, set to reference value
    if (nb == 0) {
      Arrays.fill(data, R);
      return data;
    }

    //  Y * 10**D = R + (X1 + X2) * 2**E
    //   E = binary scale factor
    //   D = decimal scale factor
    //   R = reference value
    //   X1 = 0
    //   X2 = scaled encoded value
    //   data[ i ] = (R + ( X1 + X2) * EE)/DD ;

    byte[] buf = new byte[dataLength - 5];
    raf.readFully(buf);
    InputStream in = new ByteArrayInputStream(buf);
    BufferedImage image = ImageIO.read(in);

    if (nb != image.getColorModel().getPixelSize())
      log.debug("PNG pixel size disagrees with grib number of bits: ",
              image.getColorModel().getPixelSize(), nb);

    DataBuffer db = image.getRaster().getDataBuffer();
    if (bitmap == null) {
      for (int i = 0; i < dataNPoints; i++) {
//        data[i] = (R + imageData[i] * EE) / DD;
        data[i] = (R + db.getElem(i) * EE) / DD;
      }
    } else {
      for (int bitPt = 0, dataPt = 0; bitPt < totalNPoints; bitPt++) {
        if ((bitmap[bitPt / 8] & GribNumbers.bitmask[bitPt % 8]) != 0) {
//          data[i] = (R + imageData[i] * EE) / DD;
          data[bitPt] = (R + db.getElem(dataPt++) * EE) / DD;
        } else {
          data[bitPt] = staticMissingValue;
        }
      }
    }

    return data;
  }

  // by jkaehler@meteomatics.com
  // ported from https://github.com/erdc-cm/grib_api/blob/master/src/grib_accessor_class_data_g1second_order_general_extended_packing.c
  public float[] getData50002(RandomAccessFile raf, Grib2Drs.Type50002 gdrs) throws IOException {

		BitReader reader;

		reader = new BitReader(raf, startPos+5);
		int[] groupWidth = new int[gdrs.p1];
		for (int i = 0; i < gdrs.p1; i++) {
			groupWidth[i] = (int) reader.bits2UInt(gdrs.widthOfWidth);
//			System.out.println("groupWidths["+i+"]="+groupWidth[i]);
		}

		reader = new BitReader(raf, raf.getFilePointer());
		int[] groupLength = new int[gdrs.p1];
		for (int i = 0; i < gdrs.p1; i++) {
			groupLength[i] = (int) reader.bits2UInt(gdrs.widthOfLength);
//			System.out.println("groupLengths["+i+"]="+groupLength[i]);
		}

		reader = new BitReader(raf, raf.getFilePointer());
		int[] firstOrderValues = new int[gdrs.p1];
		for (int i = 0; i < gdrs.p1; i++) {
			firstOrderValues[i] = (int) reader.bits2UInt(gdrs.widthOfFirstOrderValues);
//			System.out.println("firstOrderValues["+i+"]="+firstOrderValues[i]);
		}

//		System.out.println(gdrs);

		int bias = 0;
		if (gdrs.orderOfSPD > 0) {
			  bias=gdrs.spd[gdrs.orderOfSPD];
		}

		reader = new BitReader(raf, raf.getFilePointer());
		int cnt = gdrs.orderOfSPD;
		int[] data = new int[totalNPoints];
		for (int i=0; i < gdrs.p1; i++) {
			if (groupWidth[i] > 0) {

				for (int j=0; j < groupLength[i]; j++) {
					data[cnt]=(int) reader.bits2UInt(groupWidth[i]);
//					System.out.println("secondOrderValues["+cnt+"]="+data[cnt]);
					data[cnt]+=firstOrderValues[i];
					cnt++;
				}

			} else {

				for (int j=0; j < groupLength[i]; j++) {
					data[cnt]=firstOrderValues[i];
					cnt++;
				}

			}

		}

		for (int i=0; i < gdrs.orderOfSPD; i++) {
			data[i]=gdrs.spd[i];
		}

		int y, z, w;
		switch (gdrs.orderOfSPD) {
		case 1:
			y=data[0];
			for (int i = 1; i < totalNPoints; i++) {
				y+=data[i]+bias;
				data[i]=y;
			}

			break;
		case 2:
			y=data[1]-data[0];
			z=data[1];
			for (int i = 2; i < totalNPoints; i++) {
				y+=data[i]+bias;
				z+=y;
				data[i]=z;
//                System.out.println("i="+i+" X[i]="+data[i]+" y="+y+" z="+z+" bias="+bias);
			}

			break;
		case 3:
			y=data[2]-data[1];
			z=y-(data[1]-data[0]);
			w=data[2];
			for (int i = 3; i < totalNPoints; i++) {
				z+=data[i]+bias;
				y+=z;
				w+=y;
				data[i]=w;
			}

			break;
		}

		int D = gdrs.decimalScaleFactor;
		float DD = (float) java.lang.Math.pow((double) 10, (double) D);
		float R = gdrs.referenceValue;
		int E = gdrs.binaryScaleFactor;
		float EE = (float) java.lang.Math.pow( 2.0, (double) E);

//	    for (int i = 0; i < totalNPoints; i++) {
//	        System.out.println(i+"="+data[i]);
//	    }

		float[] ret = new float[totalNPoints];
		for (int i=0; i < totalNPoints; i++) {
			ret[i] = (((data[i]*EE)+R)*DD);
		}

		return ret;

  }

  /*

   Flag table 3.4 – Scanning mode
       Bit No. Value Meaning
 128    1 0 Points of first row or column scan in the +i (+x) direction
          1 Points of first row or column scan in the –i (–x) direction
 64     2 0 Points of first row or column scan in the –j (–y) direction



 32     3 0 Adjacent points in i (x) direction are consecutive
          1 Adjacent points in j (y) direction is consecutive
 16     4 0 All rows scan in the same direction
          1 Adjacent rows scans in the opposite direction
        5–8 Reserved
        Notes:
        (1) i direction: west to east along a parallel or left to right along an x-axis.
        (2) j direction: south to north along a meridian, or bottom to top along a y-axis.
        (3) If bit number 4 is set, the first row scan is as defined by previous flags.
   */

  /*
  Flag table 3.4 – Scanning mode
  Bit No. Value Meaning
  1 0 Points of first row or column scan in the +i (+x) direction
    1 Points of first row or column scan in the –i (–x) direction
  2 0 Points of first row or column scan in the –j (–y) direction
    1 Points of first row or column scan in the +j (+y) direction
  3 0 Adjacent points in i (x) direction are consecutive
    1 Adjacent points in j (y) direction is consecutive
  4 0 All rows scan in the same direction
    1 Adjacent rows scans in the opposite direction
  5–8 Reserved

  Code Table Flag table 3.4 - Scanning mode (3.4)
      1: Points of first row or column scan in the +i (+x) direction
      1: Points of first row or column scan in the -i (-x) direction
      2: Points of first row or column scan in the -j (-y) direction
      2: Points of first row or column scan in the +j (+y) direction
      3: Adjacent points in i (x) direction are consecutive
      3: Adjacent points in j (y) direction is consecutive
      4: All rows scan in the same direction
      4: Adjacent rows scans in the opposite direction
      5: Points within odd rows are not offset in i (x) direction
      5: Points within odd rows are offset by Di/2 in i (x) direction
      6: Points within even rows are not offset in i (x) direction
      6: Points within even rows are offset by Di/2 in i (x) direction
      7: Points are not offset in j (y) direction
      7: Points are offset by Dj/2 in j (y) direction
      8: Rows have Ni grid points and columns have Nj grid points
      8: Rows have Ni grid points if points are not offset in i direction Rows have Ni-1 grid points if points are offset by Di/2 in i direction
         Columns have Nj grid points if points are not offset in j direction Columns have Nj-1 grid points if points are offset by Dj/2 in j direction
   */

  // Rearrange the data array using the scanning mode.
  // LOOK: not handling scanMode generally
  // LOOK might be wrong for a quasi regular (thin) grid ??
  private void scanningModeCheck(float[] data, int scanMode, int Xlength) {
    // Mode  0  +x, -y, adjacent x, adjacent rows same dir
    // Mode  64 +x, +y, adjacent x, adjacent rows same dir
    if ((scanMode == 0) || (scanMode == 64))  // dont flip Y - handle it in the HorizCoordSys
      return;

    // change -x to +x ie east to west -> west to east
    if (!GribUtils.scanModeXisPositive(scanMode)) {
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

    if (!GribUtils.scanModeSameDirection(scanMode)) {
      float tmp;
      int mid = Xlength / 2;
      for (int index = 0; index < data.length; index += Xlength) {
        int row = index / Xlength;
        if (row % 2 != 0) {  // odd numbered row, calculate reverse index
          for (int idx = 0; idx < mid; idx++) {
            tmp = data[index + idx];
            data[index + idx] = data[index + Xlength - idx - 1];
            data[index + Xlength - idx - 1] = tmp;
          }
        }
      }
    }
  }

}

// what do you do with nbit=0 ??

/*
https://raw.githubusercontent.com/Unidata/gempak/master/extlibs/NDFD/mdlg2dec/unpkcmbm.f

IF (MAPBIT.EQ.0)THEN
C
C        A BIT MAP IS NOT PRESENT.
C
         DO K=1,LX
C
C              CHECK TO SEE IF THE NUMBER OF BITS
C              TO UNPACK FOR EACH VALUE IS 0.  IF IT
C              IS 0, THEN DETERMINE IF THE FIELD CAN
C              HAVE MISSING VALUES.  IF IT CAN,
C              THEN A VALUE OF ALL 1'S IN THE FIELD MEANS
C              A PRIMARY MISSING VALUE, AND A VALUE OF ALL
C              1'S MINUE 1 MEANS A SECONDARY MISSING VALUE.
C              OTHERWISE, ALL OF THE VALUES ARE VALID AND
C              THE SAME AS EACH OTHER.

            IF(LBIT(K).EQ.0)THEN
C
               IF(IMISSING.EQ.1)THEN
C
                  IF(JMIN(K).EQ.MAXGPREF)THEN
                     VALUE=XMISSP
                  ENDIF
C
               ELSEIF(IMISSING.EQ.2)THEN
C
                  IF(JMIN(K).EQ.MAXGPREF)THEN
                     VALUE=XMISSP
                  ELSEIF(JMIN(K).EQ.MAXGPREF-1)THEN
                     VALUE=XMISSS
                  ENDIF
C
               ELSE
                  VALUE=((JMIN(K)*SCAL2)+REF)/SCAL10
               ENDIF
C
               DO J=1,NOV(K)
                  A(M+J)=VALUE
               ENDDO

....

 ELSE
C
C           A BIT-MAP IS PRESENT.
C
         DO K=1,LX
C
            IF(LBIT(K).EQ.0)THEN
               NVALUE=0
C
C                 NO VALUES ARE PACKED BECAUSE ALL OF THE VALUES
C                 IN THIS GROUP ARE THE SAME.
               DO J=1,NOV(K)
C
                  DO WHILE((IBMP(M+J).EQ.0).AND.(.NOT.CLEAN))
                     A(M+J)=XMISSP
                     M=M+1
                  ENDDO
C
                  A(M+J)=(((FLOAT(JMIN(K)+NVALUE)*SCAL2)+REF)/SCAL10)
C
               ENDDO
C

 */

/*
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include "grb2.h"
#include "wgrib2.h"
#include "fnlist.h"

// 2009 public domain wesley ebisuzaki
//
// note: assumption that the grib file will use 25 bits or less for storing data
//       (limit of bitstream unpacking routines)
// note: assumption that all data can be stored as integers and have a value < INT_MAX

int unpk_complex(unsigned char **sec, float *data, unsigned int ndata) {

    unsigned int j, n;
    int i, k, nbits, ref_group_length;
    unsigned char *p, *d, *mask_pointer;
    double ref_val,factor_10, factor_2, factor;
    float missing1, missing2;
    int n_sub_missing;
    int pack, offset;
    unsigned clocation;
    unsigned int ngroups, ref_group_width, nbit_group_width, len_last, npnts;
    int nbits_group_len, group_length_factor;
    int *group_refs, *group_widths, *group_lengths, *group_location, *group_offset, *udata;
    unsigned int *group_clocation;

    int m1, m2, mask, last, penultimate;
    int extra_vals[2];
    int min_val;
    int ctable_5_4, ctable_5_6,  bitmap_flag, extra_octets;


    extra_vals[0] = extra_vals[1] = 0;
    pack = code_table_5_0(sec);
    if (pack != 2 && pack != 3) return 0;

    p = sec[5];
    ref_val = ieee2flt(p+11);
    factor_2 = Int_Power(2.0, int2(p+15));
    factor_10 = Int_Power(10.0, -int2(p+17));
    ref_val *= factor_10;
    factor = factor_2 * factor_10;
    nbits = p[19];
    ngroups = uint4(p+31);
    bitmap_flag = code_table_6_0(sec);
    ctable_5_6 = code_table_5_6(sec);

    if (pack == 3 && (ctable_5_6 != 1 && ctable_5_6 != 2))
	fatal_error_i("unsupported: code table 5.6=%d", ctable_5_6);

    extra_octets = (pack == 2) ? 0 : sec[5][48];

    if (ngroups == 0) {
	if (bitmap_flag == 255) {
            for (i = 0; i < ndata; i++) data[i] = ref_val;
            return 0;
        }
        if (bitmap_flag == 0 || bitmap_flag == 254) {
            mask_pointer = sec[6] + 6;
            mask = 0;
            for (i = 0; i < ndata; i++) {
                if ((i & 7) == 0) mask = *mask_pointer++;
                data[i] = (mask & 128) ?  ref_val : UNDEFINED;
                mask <<= 1;
            }
            return 0;
        }
        fatal_error("unknown bitmap", "");
    }

    ctable_5_4 = code_table_5_4(sec);
    ref_group_width = p[35];
    nbit_group_width = p[36];
    ref_group_length = uint4(p+37);
    group_length_factor = p[41];
    len_last = uint4(p+42);
    nbits_group_len = p[46];

    npnts =  GB2_Sec5_nval(sec); 	// number of defined points
    n_sub_missing = sub_missing_values(sec, &missing1, &missing2);

    // allocate group widths and group lengths
    group_refs = (int *) malloc(ngroups * sizeof (unsigned int));
    group_widths = (int *) malloc(ngroups * sizeof (unsigned int));
    group_lengths = (int *) malloc(ngroups * sizeof (unsigned int));
    group_location = (int *) malloc(ngroups * sizeof (unsigned int));
    group_clocation = (unsigned int *) malloc(ngroups * sizeof (unsigned int));
    group_offset = (int *) malloc(ngroups * sizeof (unsigned int));
    udata = (int *) malloc(npnts * sizeof (unsigned int));
    if (group_refs == NULL || group_widths == NULL || group_lengths ==
		NULL || udata == NULL) fatal_error("com unpack error","");

    // read any extra values
    d = sec[7]+5;
    min_val = 0;
    if (extra_octets) {
	extra_vals[0] = uint_n(d,extra_octets);
	d += extra_octets;
	if (ctable_5_6 == 2) {
	    extra_vals[1] = uint_n(d,extra_octets);
	    d += extra_octets;
	}
	min_val = int_n(d,extra_octets);
	d += extra_octets;
    }

    if (ctable_5_4 != 1) fatal_error_i("internal decode does not support code table 5.4=%d",
		ctable_5_4);

#pragma omp parallel
{
#pragma omp sections
    {


#pragma omp section
        {
           // read the group reference values
   	   rd_bitstream(d, 0, group_refs, nbits, ngroups);
	}


#pragma omp section
	{
	    int i;
	    // read the group widths

	    rd_bitstream(d+(nbits*ngroups+7)/8,0,group_widths,nbit_group_width,ngroups);
	    for (i = 0; i < ngroups; i++) group_widths[i] += ref_group_width;
	}


#pragma omp section
	{
	    int i;
	    // read the group lengths

	    if (ctable_5_4 == 1) {
		rd_bitstream(d+(nbits*ngroups+7)/8+(ngroups*nbit_group_width+7)/8,
		0,group_lengths, nbits_group_len, ngroups-1);

		for (i = 0; i < ngroups-1; i++) {
		    group_lengths[i] = group_lengths[i] * group_length_factor + ref_group_length;
		}
		group_lengths[ngroups-1] = len_last;
	    }
	}

    }


#pragma omp single
    {
        d += (nbits*ngroups + 7)/8 +
             (ngroups * nbit_group_width + 7) / 8 +
             (ngroups * nbits_group_len + 7) / 8;

	// do a check for number of grid points and size
	clocation = offset = n = j = 0;
    }

#pragma omp sections
    {


#pragma omp section
        {
	    int i;
            for (i = 0; i < ngroups; i++) {
	        group_location[i] = j;
	        j += group_lengths[i];
	        n += group_lengths[i]*group_widths[i];
            }
        }

#pragma omp section
	{
	    int i;
            for (i = 0; i < ngroups; i++) {
	        group_clocation[i] = clocation;
	        clocation = clocation + group_lengths[i]*(group_widths[i]/8) +
	              (group_lengths[i]/8)*(group_widths[i] % 8);
            }
        }

#pragma omp section
        {
	    int i;
            for (i = 0; i < ngroups; i++) {
	        group_offset[i] = offset;
	        offset += (group_lengths[i] % 8)*(group_widths[i] % 8);
	    }
        }
    }
}

    if (j != npnts) fatal_error_i("bad complex packing: n points %d",j);
    if (d + (n+7)/8 - sec[7] != GB2_Sec7_size(sec))
        fatal_error("complex unpacking size mismatch old test","");


    if (d + clocation + (offset + 7)/8 - sec[7] != GB2_Sec7_size(sec)) fatal_error("complex unpacking size mismatch","");

#pragma omp parallel for private(i) schedule(static)
    for (i = 0; i < ngroups; i++) {
	group_clocation[i] += (group_offset[i] / 8);
	group_offset[i] = (group_offset[i] % 8);

	rd_bitstream(d + group_clocation[i], group_offset[i], udata+group_location[i],
		group_widths[i], group_lengths[i]);
    }

    // handle substitute, missing values and reference value
    if (n_sub_missing == 0) {
#pragma omp parallel for private(i,k,j)
	for (i = 0; i < ngroups; i++) {
	    j = group_location[i];
	    for (k = 0; k < group_lengths[i]; k++) {
		udata[j++] += group_refs[i];
	    }
	}
    }
    else if (n_sub_missing == 1) {

#pragma omp parallel for private(i,m1,k,j)
	for (i = 0; i < ngroups; i++) {
	    j = group_location[i];
	    if (group_widths[i] == 0) {
	        m1 = (1 << nbits) - 1;
		if (m1 == group_refs[i]) {
		    for (k = 0; k < group_lengths[i]; k++) udata[j++] = INT_MAX;
		}
		else {
		    for (k = 0; k < group_lengths[i]; k++) udata[j++] += group_refs[i];
		}
	    }
	    else {
	        m1 = (1 << group_widths[i]) - 1;
	        for (k = 0; k < group_lengths[i]; k++) {
		    if (udata[j] == m1) udata[j] = INT_MAX;
		    else udata[j] += group_refs[i];
		    j++;
		}
	    }
	}
    }
    else if (n_sub_missing == 2) {
#pragma omp parallel for private(i,j,k,m1,m2)
	for (i = 0; i < ngroups; i++) {
	    j = group_location[i];
	    if (group_widths[i] == 0) {
	        m1 = (1 << nbits) - 1;
	        m2 = m1 - 1;
		if (m1 == group_refs[i] || m2 == group_refs[i]) {
		    for (k = 0; k < group_lengths[i]; k++) udata[j++] = INT_MAX;
		}
		else {
		    for (k = 0; k < group_lengths[i]; k++) udata[j++] += group_refs[i];
		}
	    }
	    else {
	        m1 = (1 << group_widths[i]) - 1;
	        m2 = m1 - 1;
	        for (k = 0; k < group_lengths[i]; k++) {
		    if (udata[j] == m1 || udata[j] == m2) udata[j] = INT_MAX;
		    else udata[j] += group_refs[i];
		    j++;
		}
	    }
	}
    }

    // post processing

	if (pack == 3) {
	    if (ctable_5_6 == 1) {
		last = extra_vals[0];
		i = 0;
		while (i < npnts) {
		    if (udata[i] == INT_MAX) i++;
		    else {
			udata[i++] = extra_vals[0];
			break;
		    }
		}
		while (i < npnts) {
		    if (udata[i] == INT_MAX) i++;
		    else {
			udata[i] += last + min_val;
			last = udata[i++];
		    }
		}
	    }
	    else if (ctable_5_6 == 2) {
		penultimate = extra_vals[0];
		last = extra_vals[1];

		i = 0;
		while (i < npnts) {
		    if (udata[i] == INT_MAX) i++;
		    else {
			udata[i++] = extra_vals[0];
			break;
		    }
		}
		while (i < npnts) {
		    if (udata[i] == INT_MAX) i++;
		    else {
			udata[i++] = extra_vals[1];
			break;
		    }
		}
	        for (; i < npnts; i++) {
		    if (udata[i] != INT_MAX) {
			udata[i] =  udata[i] + min_val + last + last - penultimate;
			penultimate = last;
			last = udata[i];
		    }
		}
	    }
	    else fatal_error_i("Unsupported: code table 5.6=%d", ctable_5_6);
	}

	// convert to float

	if (bitmap_flag == 255) {
#pragma omp parallel for schedule(static) private(i)
	    for (i = 0; i < (int) ndata; i++) {
		data[i] = (udata[i] == INT_MAX) ? UNDEFINED :
			ref_val + udata[i] * factor;
	    }
	}
        else if (bitmap_flag == 0 || bitmap_flag == 254) {
	    n = 0;
	    mask = 0;
            mask_pointer = sec[6] + 6;
            for (i = 0; i < ndata; i++) {
                if ((i & 7) == 0) mask = *mask_pointer++;
		if (mask & 128) {
		    if (udata[n] == INT_MAX) data[i] = UNDEFINED;
		    else data[i] = ref_val + udata[n] * factor;
		    n++;
		}
		else data[i] = UNDEFINED;
		mask <<= 1;
            }
        }
        else fatal_error_i("unknown bitmap: %d", bitmap_flag);

	free(group_refs);
	free(group_widths);
	free(group_lengths);
	free(group_location);
	free(group_clocation);
	free(group_offset);
	free(udata);

    return 0;
}

 */