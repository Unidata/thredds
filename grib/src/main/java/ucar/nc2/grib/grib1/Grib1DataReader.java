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

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.GribData;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.iosp.BitReader;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.EOFException;
import java.io.IOException;
import java.util.Formatter;

/**
 * Decodes the GRIB1 binary data record
 *
 * @author John
 * @since 9/8/11
 */
public class Grib1DataReader {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1DataReader.class);
  static private final float staticMissingValue = Float.NaN;

  ///////////////////////////////// Grib1Data

    /*   FM 92-XI EXT. GRIB EDITION 1
  Section 4 – Binary data section
  Octet   Contents
  1–3     Length of section
  4       Flag (see Code table 11) (first 4 bits). Number of unused bits at end of Section 4 (last 4 bits)
  5–6     Scale factor (E)
  7–10    Reference value (minimum of packed values)
  11      Number of bits containing each packed value
  12–     depending on the flag value in octet 4

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

  ///////////////////////////////////// Grib1BinaryDataSection

  private final int decimalScale;
  private final int scanMode;
  private final int nxRaw, nyRaw;
  private final int nPts;           // number of points from GDS
  private final long startPos;

  /**
   * @param startPos starting offset of the binary data section
   */
  public Grib1DataReader(int decimalScale, int scanMode, int nxRaw, int nyRaw, int nPts, long startPos) {
    this.decimalScale = decimalScale;
    this.scanMode = scanMode;
    this.nxRaw = nxRaw;
    this.nyRaw = nyRaw;
    this.nPts = nPts;
    this.startPos = startPos;
  }

  public float[] getData(RandomAccessFile raf, byte[] bitmap) throws IOException {
    GribData.Info info = Grib1SectionBinaryData.getBinaryDataInfo(raf, startPos);

    boolean isGridPointData = !GribNumbers.testBitIsSet(info.flag, 1);
    boolean isSimplePacking = !GribNumbers.testBitIsSet(info.flag, 2);

    if (isGridPointData && isSimplePacking) {
      return readSimplePacking(raf, bitmap, info);
    }

    if (isGridPointData && !isSimplePacking) {
      return readExtendedComplexPacking(raf, bitmap, info);
    }

    logger.warn("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing for {}", raf.getLocation());
    throw new IllegalStateException("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing ");
  }

    /*  From WMO Manual on Codes  I-2 bi - 5

  Data shall be coded in the form of non-negative scaled differences from a reference value.
  Notes:
  (1) The reference value is normally the minimum value of the data set which is represented.
  (2) The actual value Y (in the units of Code table 2) is linked to the coded value X, the reference
  value R, the binary scale factor E and the decimal scale factor D by means of the following formula:
    Y * 10 ^ D = R + X * 2 ^ E
*/

  // raf will be at byte 12
  private float[] readSimplePacking(RandomAccessFile raf, byte[] bitmap, GribData.Info info) throws IOException {

    boolean isConstant =  (info.numberOfBits == 0);
    int unusedbits = info.flag & 15;

    // *** read values *******************************************************

    double pow10 =  Math.pow(10.0, -decimalScale);
    float ref = (float) (pow10 * info.referenceValue);
    float scale = (float) (pow10 * Math.pow(2.0, info.binaryScaleFactor));
    float[] values;

    if (bitmap != null) {
      if (8 * bitmap.length < nPts) {
        logger.error("Bitmap section length = {} != grid length {} ({},{}) for {}", bitmap.length, nPts, nxRaw, nyRaw, raf.getLocation());
        throw new IllegalStateException("Bitmap section length!= grid length");
      }
      BitReader reader = new BitReader(raf, startPos+11);
      values = new float[nPts];
      for (int i = 0; i < nPts; i++) {
        if ((bitmap[i / 8] & GribNumbers.bitmask[i % 8]) != 0) {
          if (!isConstant) {
            values[i] = ref + scale * reader.bits2UInt(info.numberOfBits);
          } else {  // rdg - added this to handle a constant valued parameter
            values[i] = ref;
          }
        } else {
          values[i] = staticMissingValue;
        }
      }
      scanningModeCheck(values, scanMode, nxRaw);

    } else {  // bitmap is null
      if (!isConstant) {
        if (nxRaw > 0 && nyRaw > 0) {
          values = new float[nxRaw * nyRaw];
        } else {
          int nptsExpected = (int) ((info.dataLength - 11) * 8 - unusedbits) / info.numberOfBits;  // count bits
          if (!Grib1RecordScanner.allowBadDsLength && nptsExpected != nPts) logger.warn("nptsExpected {} != npts {}", nptsExpected, nPts);
          values = new float[nPts];
        }
        BitReader reader = new BitReader(raf, startPos+11);
        for (int i = 0; i < values.length; i++) {
          values[i] = ref + scale * reader.bits2UInt(info.numberOfBits);
        }
        scanningModeCheck(values, scanMode, nxRaw);

      } else {                     // constant valued - same min and max
        values = new float[nxRaw * nyRaw];
        for (int i = 0; i < values.length; i++) {
          values[i] = ref;
        }
      }
    }

    return values;
  }

  /*  From WMO Manual on Codes  I-2 bi - 5
  (3) When second-order grid-point packing is indicated, the actual value Y (in the units of Code table 2)
      is linked to the coded values Xi and Xj, the reference value R, the binary scale factor E and the
      decimal scale factor D by means of the following formula:
        Y * 10 ^ D = R + (Xi + Xj) * 2 ^ E

  Grid-point data – second-order packing
  Octet     Contents
  12–13     N1 – octet number at which first-order packed data begin
  14        Extended flags (see Code table 11)
  15–16     N2 – octet number at which second-order packed data begin
  17–18     P1 – number of first-order packed values
  19–20     P2 – number of second-order packed values
  21        Reserved
  22–(xx–1) Width(s) in bits of second-order packed values; each width is contained in 1 octet
  xx–(N1–1) Secondary bit-map, at least P2 bits long, padded to a whole number of octets with binary 0
  N1–(N2–1) P1 first-order packed values, padded to a whole number of octets with binary 0
  N2–. . .  P2 second-order packed values

  Notes:
  (1) The binary data shall consist of P1 first-order packed values, of width given by the contents of octet 11, followed by
  P2 second-order packed values; there shall be one second-order packed value for each point of the defined grid,
  as modified by application of the bit-map in Section 3 – Bit-map section, if present.

  (2) The width of the second-order packed values shall be indicated by the values of W2j:
    (a) If bit 8 of the extended flags (Code table 11) is 0, all second-order packed values will have the same width,
  indicated by a single value W2(1);
    (b) If bit 8 of the extended flags (Code table 11) is 1, P1 values of the widths of second-order packed values
  (W2(j), j = 1..P1) will be given.

  (3) The secondary bit-map, starting at octet xx, shall define with corresponding 1 bits the location where the use of the
  first-order packed values begins with reference to the defined grid (as modified by the bit-map, Section 3, if present);
  the first point of the grid, as modified by the bit-map in Section 3 if present, will always be present, and a
  corresponding 1 shall be set in the first bit of the secondary bit-map.

    (3) The secondary bit-map, starting at octet xx, shall define with corresponding 1 bits the location where the use of the
  first-order packed values begins with reference to the defined grid;81433 the first point of the grid, will always be present, and a
  corresponding 1 shall be set in the first bit of the secondary bit-map.

  (4) Where bit 7 of the extended flags (Code table 11) is 0, the secondary bit-map shall be omitted; and implied
  secondary bit-map shall be inferred such that a 1 bit is set for the first point of each row (or column) of the defined
  grid (row by row packing).

  (5) The original represented data at any point shall be obtained by scanning the points in the order defined by the grid
  description, as modified by the (optional) bit-map section; each first-order packed value shall remain defined until
  the point at which the use of a subsequent first-order packed value begins, as defined by the secondary bit-map;
  the unpacked value shall be obtained by applying the reference value, the binary and the decimal scales to the sum
  of the first- and second-order values for each point, by the following formula:
      Y × 10^D = R + (Xi + Xj) × 2^E
  where Xi is the appropriate first-order packed value;
        Xj is the appropriate second-order packed value.

  (6) If the number of bits W2j, for the appropriate subset, is zero, no values for that subset are represented; i.e. the actual
  value for that subset is a constant given by R + (Xi × 2E). This is a form of run-length encoding in which a string of
  identical values is represented by one value; the replication count for that value is, implicitly, in the secondary bit-map.*/

  // raf will be at byte 12
  private float[] readComplexPacking(RandomAccessFile raf, byte[] bitmap, GribData.Info info) throws IOException {

    // First-order descriptors width stored at the equivalent place of bit number for ordinary packing
    int foWidth = info.numberOfBits;

    boolean isConstant =  (info.numberOfBits == 0);
    int unusedbits = info.flag & 15;

   /* Octet     Contents
      12–13     N1 – octet number at which first-order packed data begin
      14        Extended flags (see Code table 11)
      15–16     N2 – octet number at which second-order packed data begin
      17–18     P1 – number of first-order packed values
      19–20     P2 – number of second-order packed values
      21        Reserved    */
    int N1 = GribNumbers.uint2(raf);
    int flagExt = raf.read();
    int N2 = GribNumbers.uint2(raf);
    int P1 = GribNumbers.uint2(raf);
    int P2 = GribNumbers.uint2(raf);
    raf.read(); // skip

      /*
From http://cost733.geo.uni-augsburg.de/cost733class-1.2/browser/grib_api-1.9.18/definitions/grib1/11-2.table?rev=4

# CODE TABLE 11-2, Flag
#  Undocumented use of octet 14 extededFlags
#  Taken from d2ordr.F
#         R------- only bit 1 is reserved.
#         -0------ single datum at each grid point.
#         -1------ matrix of values at each grid point.
#         --0----- no secondary bit map.
#         --1----- secondary bit map present.
#         ---0---- second order values have constant width.
#         ---1---- second order values have different widths.
#         ----0--- no general extended second order packing.
#         ----1--- general extended second order packing used.
#         -----0-- standard field ordering in section 4.
#         -----1-- boustrophedonic ordering in section 4.
1 0 Reserved
1 1 Reserved
2 0 Single datum at each grid point
2 1 Matrix of values at each grid point
3 0 No secondary bitmap Present
3 1 Secondary bitmap Present
4 0 Second-order values constant width
4 1 Second-order values different widths
5 0 no general extended second order packing
5 1 general extended second order packing used
6 0 standard field ordering in section 4
6 1 boustrophedonic ordering in section 4
#         ------00 no spatial differencing used.
#         ------01 1st-order spatial differencing used.
#         ------10 2nd-order    "         "         " .
#         ------11 3rd-order    "         "         " .

   */
    System.out.printf("flagExt=%s%n", Long.toBinaryString(flagExt));
    boolean hasBitmap2 = GribNumbers.testBitIsSet(flagExt,3);
    boolean hasDifferentWidths = GribNumbers.testBitIsSet(flagExt,4);
    boolean useGeneralExtended = GribNumbers.testBitIsSet(flagExt,5);
    boolean useBoustOrdering = GribNumbers.testBitIsSet(flagExt,6);


    // 22–(xx–1) Width(s) in bits of second-order packed values; each width is contained in 1 octet
   /*   (2) The width of the second-order packed values shall be indicated by the values of W2(j):
      (a) If bit 8 of the extended flags (Code table 11) is 0, all second-order packed values will have the same width,
              indicated by a single value W2(1);
      (b) If bit 8 of the extended flags (Code table 11) is 1, P1 values of the widths of second-order packed values
              (W2(j), j = 1..P1) will be given.
    */
    int constantWidth = -1;
    int[] widths;
    int bitmapStart = 21;
    if (!hasDifferentWidths) {
      constantWidth = info.numberOfBits; // LOOK not documented ??
    } else {
      widths = new int[P1];
      for (int i=0; i< P1; i++){
        widths[i] = raf.read();
      }
      bitmapStart = 21 + P1;
      System.out.printf("%s%n", Misc.showInts(widths));
    }

    /* (4) Where bit 7 of the extended flags (Code table 11) is 0, the secondary bit-map shall be omitted; and implied
    secondary bit-map shall be inferred such that a 1 bit is set for the first point of each row (or column) of the defined
    grid (row by row packing). */

    // xx–(N1–1) Secondary bit-map, at least P2 bits long, padded to a whole number of octets with binary 0
  /*   (3) The secondary bit-map, starting at octet xx, shall define with corresponding 1 bits the location where the use of the
    first-order packed values begins with reference to the defined grid (as modified by the bit-map, Section 3, if present);
    the first point of the grid, as modified by the bit-map in Section 3 if present, will always be present, and a
    corresponding 1 shall be set in the first bit of the secondary bit-map.
  */
    byte[] bitmap2;
    if (hasBitmap2) {
      int bitmapSize = N1 - bitmapStart - 1;
      System.out.printf("bitmapSize=%d%n", bitmapSize);
      bitmap2 = new byte[bitmapSize];
      raf.read(bitmap2);
      int bitson = GribNumbers.countBits(bitmap2);
      System.out.printf("bitson=%d%n", bitson);
    }
    long filePos = raf.getFilePointer();
    int offset = (int) (filePos - this.startPos);
    System.out.printf("offset=%d%n", offset);


    //   N1–(N2–1) P1 first-order packed values, padded to a whole number of octets with binary 0
    int nfo = N2-N1;  // docs say N1–(N2–1)
    System.out.printf("nfo=%d%n", nfo);

    //   N2–. . .  P2 second-order packed values
    int np = this.nPts;
    System.out.printf("need bitmap bytes=%d for npts=%d%n", np/8, np);

    float[] data = new float[1];

    return data;
  }

  // raf will be at byte 12
  private float[] readExtendedComplexPacking(RandomAccessFile raf, byte[] bitmap, GribData.Info info) throws IOException {

    int N1 = GribNumbers.uint2(raf);
    int flagExt = raf.read();

          /*
From http://cost733.geo.uni-augsburg.de/cost733class-1.2/browser/grib_api-1.9.18/definitions/grib1/11-2.table?rev=4

# CODE TABLE 11-2, Flag
#  Undocumented use of octet 14 extededFlags
#  Taken from d2ordr.F
#         R------- only bit 1 is reserved.
#         -0------ single datum at each grid point.
#         -1------ matrix of values at each grid point.
#         --0----- no secondary bit map.
#         --1----- secondary bit map present.
#         ---0---- second order values have constant width.
#         ---1---- second order values have different widths.
#         ----0--- no general extended second order packing.
#         ----1--- general extended second order packing used.
#         -----0-- standard field ordering in section 4.
#         -----1-- boustrophedonic ordering in section 4.
1 0 Reserved
1 1 Reserved
2 0 Single datum at each grid point
2 1 Matrix of values at each grid point
3 0 No secondary bitmap Present
3 1 Secondary bitmap Present
4 0 Second-order values constant width
4 1 Second-order values different widths
5 0 no general extended second order packing
5 1 general extended second order packing used
6 0 standard field ordering in section 4
6 1 boustrophedonic ordering in section 4
#         ------00 no spatial differencing used.
#         ------01 1st-order spatial differencing used.
#         ------10 2nd-order    "         "         " .
#         ------11 3rd-order    "         "         " .

   */
    System.out.printf("%n=====================%nflagExt=%s%n", Long.toBinaryString(flagExt));
    boolean hasBitmap2 = GribNumbers.testBitIsSet(flagExt, 3);
    boolean hasDifferentWidths = GribNumbers.testBitIsSet(flagExt, 4);
    boolean useGeneralExtended = GribNumbers.testBitIsSet(flagExt, 5);
    boolean useBoustOrdering = GribNumbers.testBitIsSet(flagExt, 6);
    System.out.printf(" hasBitmap2=%s, hasDifferentWidths=%s, useGeneralExtended=%s, useBoustOrdering=%s%n%n", hasBitmap2, hasDifferentWidths, useGeneralExtended, useBoustOrdering);

         /* Octet     Contents
      12–13     N1 – octet number at which first-order packed data begin
      14        Extended flags (see Code table 11)
      15–16     N2 – octet number at which second-order packed data begin

      try replacing:
      17–18     P1 – number of first-order packed values
      19–20     P2 – number of second-order packed values
      21        Reserved

      with:
    data.grid_second_order.def

    unsigned [2] N2 : dump;
    unsigned [2] codedNumberOfGroups : no_copy ;
    unsigned [2] numberOfSecondOrderPackedValues : dump;

    # used to extend
    unsigned [1] extraValues=0 : hidden, edition_specific;

    meta numberOfGroups evaluate(codedNumberOfGroups + 65536 * extraValues);

    unsigned [1] widthOfWidths : dump;
    unsigned [1] widthOfLengths : dump;
    unsigned [2] NL : dump;

    if (orderOfSPD) {
     unsigned[1] widthOfSPD ;
     meta SPD spd(widthOfSPD,orderOfSPD) : read_only;
    }
     */

    showOffset("N2", raf, 14, 672);
    int N2 = GribNumbers.uint2(raf);
    int codedNumberOfGroups = GribNumbers.uint2(raf);
    int numberOfSecondOrderPackedValues = GribNumbers.uint2(raf);
    int extraValues = raf.read();
    int NG = codedNumberOfGroups + 65536 * extraValues;
    int widthOfWidths = raf.read();
    int widthOfLengths = raf.read();
    int NL = GribNumbers.uint2(raf);
    System.out.printf("NG=%d NL=%d%n", NG, NL);

    // heres how many bits groupWidths should take
    int groupWidthsSizeBits = widthOfWidths * NG;
    int groupWidthsSizeBytes = (groupWidthsSizeBits+7)/8;
    int skipBytes = NL-groupWidthsSizeBytes-26;
    System.out.printf("groupWidthsSizeBytes=%d, skipBytes=%d%n", groupWidthsSizeBytes, skipBytes);

    raf.skipBytes(skipBytes);



    // from grib_dimp output
    // raf.skipBytes(6);
    //int widthOfSPD = raf.read();
    //int SPD = GribNumbers.int4(raf);
    //raf.read();
    //raf.read();
    showOffset("GroupWidth", raf, 31, 689);

    BitReader reader = new BitReader(raf, raf.getFilePointer());

    // meta groupWidths unsigned_bits(widthOfWidths,numberOfGroups) : read_only;
    int[] groupWidth = new int[NG];
    for (int group=0; group<NG; group++) {
      groupWidth[group] = (int) reader.bits2UInt(widthOfWidths);
    }

    reader.incrByte(); // assume on byte boundary
    showOffset("GroupLength", raf, NL-1, 2723);

    // try forcing to NL
    // reader = new BitReader(raf, this.startPos + NL - 1);

    // meta groupLengths unsigned_bits(widthOfLengths,numberOfGroups) : read_only;
    int[] groupLength = new int[NG];
    for (int group=0; group<NG; group++)
      groupLength[group] = (int) reader.bits2UInt(widthOfLengths);
    showOffset("FirstOrderValues", raf, N1-1, 5774);

    // meta countOfGroupLengths sum(groupLengths);
    int countOfGroupLengths = 0;
    for (int group=0; group<NG; group++)
      countOfGroupLengths += groupLength[group];
    System.out.printf("countOfGroupLengths = %d%n", countOfGroupLengths);
    System.out.printf("nPts = %d%n%n", nPts);

    // try forcing to N1
    // reader = new BitReader(raf, this.startPos + N1 - 1);

    // First-order descriptors width stored at the equivalent place of bit number for ordinary packing
    int foWidth = info.numberOfBits;

    // meta firstOrderValues unsigned_bits(widthOfFirstOrderValues,numberOfGroups) : read_only;
    reader.incrByte(); // assume on byte boundary
    int[] firstOrderValues = new int[NG];
    for (int group=0; group<NG; group++)
      firstOrderValues[group] = (int) reader.bits2UInt(foWidth);
    int offset3 = (int) (raf.getFilePointer() - this.startPos);
    System.out.printf("nbytes=%d%n", (foWidth * NG + 7) / 8);
    showOffset("SecondOrderValues", raf, N2-1, 11367);


    int total_nbits = 0;
    for (int group = 0; group < NG; group++) {
      int nbits = groupLength[group] * groupWidth[group];
      total_nbits += nbits;
    }
    int data_bytes = (total_nbits +7) / 8;
    System.out.printf(" total_nbits=%d, nbytes=%d%n", total_nbits, data_bytes);
    System.out.printf(" expect msgLen=%d, actual=%d%n", N2-1+data_bytes, info.dataLength);
    int simplepackSizeInBits = nPts * info.numberOfBits;
    int simplepackSizeInBytes = (simplepackSizeInBits +7) / 8;
    System.out.printf(" simplepackSizeInBits=%d, simplepackSizeInBytes=%d%n", simplepackSizeInBits, simplepackSizeInBytes);

        // meta bitsPerValue second_order_bits_per_value(codedValues,binaryScaleFactor,decimalScaleFactor);
    reader.incrByte(); // assume on byte boundary
    int[] secondOrderValues = new int[countOfGroupLengths];
    int countGroups = 0;
    try {
      int val = 0;
      double log2 = Math.log(2);
      for (int group = 0; group < NG; group++) {
        //System.out.printf("%3d: %3d %3d %d: ", group, groupLength[group], groupWidth[group], firstOrderValues[group]);
        for (int i = 0; i < groupLength[group]; i++) {
          int secVal = (int) reader.bits2UInt(groupWidth[group]);
          secondOrderValues[val++] = secVal;
          //int nbits = secVal == 0 ? 0 : (int) Math.ceil(Math.log(secVal) / log2);
          //System.out.printf("%d,", nbits);
        }
        //System.out.printf("%n");
        countGroups++;
      }
    } catch (EOFException ioe) {
      System.out.printf("Only did %d groups out of %d%n", countGroups, NG);
    }

    int offset4 = (int) (raf.getFilePointer() - this.startPos);
    showOffset("MessageEnd", raf, (int) info.dataLength, 82091);

    System.out.printf("nbytes= %d%n", (total_nbits + 7) / 8);
    System.out.printf("actual= %d%n", offset4-offset3);

    double pow10 =  Math.pow(10.0, -decimalScale);
    float ref = (float) (pow10 * info.referenceValue);
    float scale = (float) (pow10 * Math.pow(2.0, info.binaryScaleFactor));
    float[] values = new float[nPts];
    int val = 0;
    int n = Math.min(countOfGroupLengths, nPts);
    for (int group = 0; group < NG; group++) {
      for (int i = 0; i < groupLength[group]; i++) {
        values[val] = ref + scale * (firstOrderValues[group] + secondOrderValues[val]);
        val++;
      }
    }
    // use last average for shortfall
    for (int i = countOfGroupLengths; i<nPts; i++) {
      values[val] = ref + scale * firstOrderValues[NG-1];
      val++;
    }

    scanningModeCheck(values, scanMode, nxRaw);

    return values;
  }

  private void showOffset(String what, RandomAccessFile raf, int expectOffset, int expectDump) throws IOException {
    int offset = (int) (raf.getFilePointer() - this.startPos);
    System.out.printf("%s: filePos=%d, expectDump=%d, offset=%d expect=%d%n", what, raf.getFilePointer(), expectDump, offset, expectOffset);
  }

  private static void showOffset(Formatter f, String what, RandomAccessFile raf, long startPos, int expectOffset) throws IOException {
    int offset = (int) (raf.getFilePointer() - startPos);
    f.format("%s: filePos=%d, offset=%d expect=%d%n", what, raf.getFilePointer(), offset, expectOffset);
  }

  public static void showInfo(Formatter f, RandomAccessFile raf, long startPos) throws IOException {
      GribData.Info info = Grib1SectionBinaryData.getBinaryDataInfo(raf, startPos);

      boolean isGridPointData = !GribNumbers.testBitIsSet(info.flag, 1);
      boolean isSimplePacking = !GribNumbers.testBitIsSet(info.flag, 2);
      if (!isGridPointData ||isSimplePacking) return;

    int N1 = GribNumbers.uint2(raf);
    int flagExt = raf.read();
    boolean hasBitmap2 = GribNumbers.testBitIsSet(flagExt, 3);
    boolean hasDifferentWidths = GribNumbers.testBitIsSet(flagExt, 4);
    boolean useGeneralExtended = GribNumbers.testBitIsSet(flagExt, 5);
    boolean useBoustOrdering = GribNumbers.testBitIsSet(flagExt, 6);

    int N2 = GribNumbers.uint2(raf);
    int codedNumberOfGroups = GribNumbers.uint2(raf);
    int numberOfSecondOrderPackedValues = GribNumbers.uint2(raf);
    int extraValues = raf.read();
    int NG = codedNumberOfGroups + 65536 * extraValues;
    int widthOfWidths = raf.read();
    int widthOfLengths = raf.read();
    int NL = GribNumbers.uint2(raf);

    f.format("%n");
    f.format("       ----flagExt = %s%n", Long.toBinaryString(flagExt));
    f.format("        hasBitmap2 = %s%n", hasBitmap2);
    f.format("hasDifferentWidths = %s%n", hasDifferentWidths);
    f.format("useGeneralExtended = %s%n", useGeneralExtended);
    f.format("  useBoustOrdering = %s%n", useBoustOrdering);
    f.format("                NL = %d%n", NL);
    f.format("                N1 = %d%n", N1);
    f.format("                N2 = %d%n", N2);
    f.format("    numberOfGroups = %d%n", NG);
    f.format("     widthOfWidths = %d%n", widthOfWidths);
    f.format("    widthOfLengths = %d%n", widthOfLengths);
    f.format("%n");

    int groupWidthsSizeBits = widthOfWidths * NG;
    int groupWidthsSizeBytes = (groupWidthsSizeBits+7)/8;
    int skipBytes = NL-groupWidthsSizeBytes-26;
    System.out.printf("groupWidthsSizeBytes=%d, skipBytes=%d%n", groupWidthsSizeBytes, skipBytes);
    raf.skipBytes(skipBytes);

    BitReader reader = new BitReader(raf, raf.getFilePointer());
    int[] groupWidth = new int[NG];
    for (int group=0; group<NG; group++) {
      groupWidth[group] = (int) reader.bits2UInt(widthOfWidths);
    }
    reader.incrByte(); // assume on byte boundary
    showOffset(f, "GroupLength", raf, startPos, NL-1);

    // try forcing to NL
    // reader = new BitReader(raf, this.startPos + NL - 1);

    // meta groupLengths unsigned_bits(widthOfLengths,numberOfGroups) : read_only;
    int[] groupLength = new int[NG];
    for (int group=0; group<NG; group++)
      groupLength[group] = (int) reader.bits2UInt(widthOfLengths);
    showOffset(f, "FirstOrderValues", raf, startPos, N1-1);

    // meta countOfGroupLengths sum(groupLengths);
    int countOfGroupLengths = 0;
    for (int group=0; group<NG; group++)
      countOfGroupLengths += groupLength[group];
    f.format("countOfGroupLengths = %d%n", countOfGroupLengths);

    // try forcing to N1
    // reader = new BitReader(raf, this.startPos + N1 - 1);

    // First-order descriptors width stored at the equivalent place of bit number for ordinary packing
    int foWidth = info.numberOfBits;

    // meta firstOrderValues unsigned_bits(widthOfFirstOrderValues,numberOfGroups) : read_only;
    reader.incrByte(); // assume on byte boundary
    int[] firstOrderValues = new int[NG];
    for (int group=0; group<NG; group++)
      firstOrderValues[group] = (int) reader.bits2UInt(foWidth);

    showOffset(f, "SecondOrderValues", raf, startPos, N2-1);

    int total_nbits = 0;
    for (int group = 0; group < NG; group++) {
      int nbits = groupLength[group] * groupWidth[group];
      total_nbits += nbits;
    }
    int data_bytes = (total_nbits +7) / 8;
    f.format(" total_nbits=%d, nbytes=%d%n", total_nbits, data_bytes);
    f.format(" expect msgLen=%d, actual=%d%n", N2 - 1 + data_bytes, info.dataLength);
    //int simplepackSizeInBits = nPts * info.numberOfBits;
    //int simplepackSizeInBytes = (simplepackSizeInBits +7) / 8;
    //f.format(" simplepackSizeInBits=%d, simplepackSizeInBytes=%d%n", simplepackSizeInBits, simplepackSizeInBytes);
  }

  /*




    // 22–(xx–1) Width(s) in bits of second-order packed values; each width is contained in 1 octet
   /*   (2) The width of the second-order packed values shall be indicated by the values of W2(j):
      (a) If bit 8 of the extended flags (Code table 11) is 0, all second-order packed values will have the same width,
              indicated by a single value W2(1);
      (b) If bit 8 of the extended flags (Code table 11) is 1, P1 values of the widths of second-order packed values
              (W2(j), j = 1..P1) will be given.

    int constantWidth = -1;
    int[] widths;
    int bitmapStart = 21;
    if (!hasDifferentWidths) {
      constantWidth = info.numberOfBits; // LOOK not documented ??
    } else {
      widths = new int[P1];
      for (int i=0; i< P1; i++){
        widths[i] = raf.read();
      }
      bitmapStart = 21 + P1;
      System.out.printf("%s%n", Misc.showInts(widths));
    }

    /* (4) Where bit 7 of the extended flags (Code table 11) is 0, the secondary bit-map shall be omitted; and implied
    secondary bit-map shall be inferred such that a 1 bit is set for the first point of each row (or column) of the defined
    grid (row by row packing). */

    // xx–(N1–1) Secondary bit-map, at least P2 bits long, padded to a whole number of octets with binary 0
  /*   (3) The secondary bit-map, starting at octet xx, shall define with corresponding 1 bits the location where the use of the
    first-order packed values begins with reference to the defined grid (as modified by the bit-map, Section 3, if present);
    the first point of the grid, as modified by the bit-map in Section 3 if present, will always be present, and a
    corresponding 1 shall be set in the first bit of the secondary bit-map.

    byte[] bitmap2;
    if (hasBitmap2) {
      int bitmapSize = N1 - bitmapStart - 1;
      System.out.printf("bitmapSize=%d%n", bitmapSize);
      bitmap2 = new byte[bitmapSize];
      raf.read(bitmap2);
      int bitson = GribNumbers.countBits(bitmap2);
      System.out.printf("bitson=%d%n", bitson);
    }
    long filePos = raf.getFilePointer();
    int offset = (int) (filePos - this.startPos);
    System.out.printf("offset=%d%n", offset);


    //   N1–(N2–1) P1 first-order packed values, padded to a whole number of octets with binary 0
    int nfo = N2-N1;  // docs say N1–(N2–1)
    System.out.printf("nfo=%d%n", nfo);

    //   N2–. . .  P2 second-order packed values
    int np = this.nPts;
    System.out.printf("need bitmap bytes=%d for npts=%d%n", np/8, np);

    float[] data = new float[1];

    return data;
  } */

  /**
   * Rearrange the data array using the scanning mode.
   * LOOK: not handling scanMode generally
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

    if (Xlength <= 0) // old code
      return;

    // Mode  0 +x, -y, adjacent x, adjacent rows same dir
    // Mode  64 +x, +y, adjacent x, adjacent rows same dir
    //if ((scanMode == 0) || (scanMode == 64)) {
    // NOOP
    //} else
    if ((scanMode == 128) || (scanMode == 192)) {
      // Mode  128 -x, -y, adjacent x, adjacent rows same dir
      // Mode  192 -x, +y, adjacent x, adjacent rows same dir
      // change -x to +x ie east to west -> west to east
      int mid = Xlength / 2;
      for (int index = 0; index < data.length; index += Xlength) {
        for (int idx = 0; idx < mid; idx++) {
          float tmp = data[index + idx];
          data[index + idx] = data[index + Xlength - idx - 1];
          data[index + Xlength - idx - 1] = tmp;
        }
      }
    }
  }

  // debugging
  int[] getDataRaw(RandomAccessFile raf, byte[] bitmap) throws IOException {
    raf.seek(startPos); // go to the data section
    int msgLength = GribNumbers.uint3(raf);

    // octet 4, 1st half (packing flag)
    int unusedbits = raf.read();
    if ((unusedbits & 192) != 0) {
      logger.error("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing for {} len={}", raf.getLocation(), msgLength);
      throw new IllegalStateException("Grib1BinaryDataSection: (octet 4, 1st half) not grid point data and simple packing ");
    }

    GribNumbers.int2(raf); // octets 5-6 (binary scale factor)
    GribNumbers.float4(raf); // octets 7-10 (reference point = minimum value)

    // octet 11 (number of bits per value)
    int numbits = raf.read();
    // boolean isConstant =  (numbits == 0);

    // *** read int values *******************************************************
    BitReader reader = new BitReader(raf, startPos+11);
    int[] ivals = new int[nPts];
    for (int i = 0; i < nPts; i++) {
      ivals[i] = (int) reader.bits2UInt(numbits);
    }

    return ivals;
  }

}

 /* Grid-point data – second-order packing
  Octet     Contents
  12–13     N1 – octet number at which first-order packed data begin
  14        Extended flags (see Code table 11)
  15–16     N2 – octet number at which second-order packed data begin
  17–18     P1 – number of first-order packed values
  19–20     P2 – number of second-order packed values
  21        Reserved
  22–(xx–1) Width(s) in bits of second-order packed values; each width is contained in 1 octet
  xx–(N1–1) Secondary bit-map, at least P2 bits long, padded to a whole number of octets with binary 0
  N1–(N2–1) P1 first-order packed values, padded to a whole number of octets with binary 0
  N2–. . .  P2 second-order packed values
  Notes:
  (1) The binary data shall consist of P1 first-order packed values, of width given by the contents of octet 11, followed by
  P2 second-order packed values; there shall be one second-order packed value for each point of the defined grid,
  as modified by application of the bit-map in Section 3 – Bit-map section, if present.

  (2) The width of the second-order packed values shall be indicated by the values of W2j:
    (a) If bit 8 of the extended flags (Code table 11) is 0, all second-order packed values will have the same width,
  indicated by a single value W21;
    (b) If bit 8 of the extended flags (Code table 11) is 1, P1 values of the widths of second-order packed values
  (W2j, j = 1..P1) will be given.

  (3) The secondary bit-map, starting at octet xx, shall define with corresponding 1 bits the location where the use of the
  first-order packed values begins with reference to the defined grid (as modified by the bit-map, Section 3, if present);
  the first point of the grid, as modified by the bit-map in Section 3 if present, will always be present, and a
  corresponding 1 shall be set in the first bit of the secondary bit-map.

  (4) Where bit 7 of the extended flags (Code table 11) is 0, the secondary bit-map shall be omitted; and implied
  secondary bit-map shall be inferred such that a 1 bit is set for the first point of each row (or column) of the defined
  grid (row by row packing).

  (5) The original represented data at any point shall be obtained by scanning the points in the order defined by the grid
  description, as modified by the (optional) bit-map section; each first-order packed value shall remain defined until
  the point at which the use of a subsequent first-order packed value begins, as defined by the secondary bit-map;
  the unpacked value shall be obtained by applying the reference value, the binary and the decimal scales to the sum
  of the first- and second-order values for each point, by the following formula:
      Y × 10D = R + (Xi + Xj) × 2E
  where Xi is the appropriate first-order packed value;
        Xj is the appropriate second-order packed value.

  (6) If the number of bits W2j, for the appropriate subset, is zero, no values for that subset are represented; i.e. the actual
  value for that subset is a constant given by R + (Xi × 2E). This is a form of run-length encoding in which a string of
  identical values is represented by one value; the replication count for that value is, implicitly, in the secondary bit-map.
*/


/*
Spherical harmonics – complex packing
Octet No. Contents
12–13 N
14–15 IP (where IP = int (1000 x P))
16 J1
17 K1
18 M1
19 Binary data
.
. Unpacked binary data represented in 004 octets in the same way as the reference
. value (pairs of coefficients)
N Packed binary data
Notes:
(1) Removal of the real (0.0) coefficient considerably reduces the variability of the coefficients and results in better packing.
(2) For some spherical harmonic representations, the (0.0) coefficient represents the mean value of the parameter
represented.
(3) For spherical harmonics – complex packing, J1, K1, M1 are the pentagonal resolution parameters specifying the
truncation of a subset of the data, which shall be represented unpacked (as is the reference value) and shall precede
the packed data.
P defines a scaling factor by which is packed not the field itself, but the modulus of ∇2P of the field, where ∇2 is the
Laplacian operator. Thus the coefficients φmn
will be multiplied by (n(n+1))P before packing, and divided by this factor
after unpacking.
N is a pointer to the start of the packed data (i.e. gives octet number)
(J1, K1, M1 > 0 and P 0, + or –)
The representation mode (Code figure = 2 in Code table 10) in Section 2 shall indicate this type of packing, but as
Section 2 is optional, the flag field in Section 4 may also be used to indicate the more complex method

 */
