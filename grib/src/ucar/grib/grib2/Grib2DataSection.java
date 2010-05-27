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

// $Id: Grib2DataSection.java,v 1.29 2006/03/10 17:34:31 rkambic Exp $


package ucar.grib.grib2;


import ucar.jpeg.jj2000.j2k.decoder.Grib2JpegDecoder;

import ucar.grib.GribNumbers;

import ucar.unidata.io.RandomAccessFile;

/*
 * Grib2DataSection.java  1.0  08/02/2003
 * @author Robb Kambic
 *
 */

import java.io.IOException;


/**
 * A class that represents the DataSection of a GRIB product.
 */

public final class Grib2DataSection {


  private static final boolean debug = false;

  private static int bitsmv1[] = new int[31];
  static {
    for (int i = 0; i < 31; i++) {
      bitsmv1[i] = (int) java.lang.Math.pow((double) 2, (double) i) - 1;
      //System.out.println( "DS bitsmv1[ "+ i +" ] =" + bitsmv1[ i ] );
    }
  }  
  /**
   * logger
   */
  static private final org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(Grib2DataSection.class);

  /**
   * flag to signifly if a static Missing Value is used. Since it's possible to have different missing values
   * in a Grib file, the first record's missing value might not be the correct missing value for the current
   * record. If a static missing value is used (float.NaN) then there will be no conflict of missing value
   * processing.
   */
  static private boolean staticMissingValueInUse = true;

  /**
   * Length in bytes of DataSection section.
   */
  private int length;

  /**
   * Number of this section, should be 7.
   */
  private final int section;

  /**
   * Data Array used to return unpacked values.
   */
  private float[] data;

  /**
   * Buffer for one byte which will be processed bit by bit.
   */
  private int bitBuf = 0;

  /**
   * Current bit position in <tt>bitBuf</tt>.
   */
  private int bitPos = 0;

  /**
   * _more_
   */
  private int scanMode;

  /**
   * _more_
   */
  private int count;  // raw data count

  /**
   * _more_
   */
  private int Xlength;  // length of the x axis

  // *** constructors *******************************************************

  /**
   * Constructor for a Grib2 Data Section.
   *
   * @param getData  boolean whether to read data or skip read
   * @param raf RandomAccessFile of Grib file
   * @param gds Grib2GridDefinitionSection
   * @param drs Grib2DataRepresentationSection
   * @param bms Grib2BitMapSection
   * @throws IOException on data reads
   */
  public Grib2DataSection(boolean getData, RandomAccessFile raf,
                          Grib2GridDefinitionSection gds,
                          Grib2DataRepresentationSection drs,
                          Grib2BitMapSection bms)
      throws IOException {
    //System.out.println( "raf.FilePointer=" + raf.FilePointer() );
    // octets 1-4 (Length of DS)
    length = GribNumbers.int4(raf);
    //System.out.println( "DS length=" + length );
    //System.out.println( "DS calculated end=" + ( raf.getFilePointer() + length -4 ));
    // octet 5  section 7
    section = raf.read();
    //System.out.println( "DS is 7, section=" + section );
    if (!getData) {  // skip data read
      //System.out.println( "raf.position before reposition="+raf.getFilePointer());
      //System.out.println( "raf.length=" + raf.length() );
      // sanity check for erronous ds length
      if ((length > 0) && (length < raf.length())) {
        raf.skipBytes(length - 5);
        //System.out.println( "raf.skipBytes = " + (length -5) );
      } else {
        length = 5;  // only read length and section
      }
      //System.out.println( "raf.position after skip=" + raf.getFilePointer() );
      return;
    }
    int dtn = drs.getDataTemplateNumber();
    //System.out.println( "DS dtn=" + dtn );
    if (dtn == 0 ) {  // 0: Grid point data - simple packing
      simpleUnpacking(raf, gds, drs, bms);
    } else if (dtn == 1) {
      // 1: Matrix values - simple packing
      data = null;
      logger.error( "Matrix values - simple packing not implemented");
    } else if (dtn == 2) {                       // 2:Grid point data - complex packing
      complexUnpacking(raf, gds, drs, bms);
    } else if (dtn == 3) {    // 3: complex packing with spatial differencing
      //complexUnpackingWithSpatial(raf, gds, drs);
      complexUnpackingWithSpatial(raf, gds, drs, bms);
    } else if ((dtn == 40) || (dtn == 40000)) {  // JPEG 2000 Stream Format
      jpeg2000Unpacking(raf, gds, drs, bms);
    }
  }                                                // end Grib2DataSection

  /**
   * simple Unpacking method for Grib2 data.
   *
   * @param raf RandomAccessFile of Grib file
   * @param gds Grib2GridDefinitionSection
   * @param drs Grib2DataRepresentationSection
   * @param bms Grib2BitMapSection
   * @throws IOException on data reads
   */
  private void simpleUnpacking(RandomAccessFile raf,
                               Grib2GridDefinitionSection gds,
                               Grib2DataRepresentationSection drs,
                               Grib2BitMapSection bms)
      throws IOException {
    int mvm = drs.getMissingValueManagement();
    //System.out.println( "DS mvm=" + mvm );

    float mv = Float.NaN;
    if (staticMissingValueInUse ) {
      mv = Float.NaN;
    } else if ( mvm == 0 ) {
      mv = Float.NaN;
    } else if ( mvm == 1 ) {
       mv = drs.getPrimaryMissingValue();
    } else if ( mvm == 2 ) {
       mv = drs.getSecondaryMissingValue() ;
    }

    // dataPoints are number of points encoded, it could be less than the
    // numberPoints in the grid record if bitMap is used, otherwise equal
    //int dataPoints = drs.getDataPoints();
    //System.out.println( "DS DRS dataPoints=" + drs.getDataPoints() );
     
    int nb = drs.getNumberOfBits();
    //System.out.println( "DS nb=" + nb );
    int D = drs.getDecimalScaleFactor();
    //System.out.println( "DS D=" + D );
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    //System.out.println( "DS DD=" + DD );
    float R = drs.getReferenceValue();
    //System.out.println( "DS R=" + R );
    int E = drs.getBinaryScaleFactor();
    //System.out.println( "DS E=" + E );
    float EE = (float) java.lang.Math.pow((double) 2.0, (double) E);
    //System.out.println( "DS EE=" + EE );

    int numberPoints = gds.getGdsVars().getNumberPoints();
    //System.out.println( "DS GDS NumberPoints=" +  gds.getNumberPoints() );
    data = new float[numberPoints];

    boolean[] bitmap = bms.getBitmap();

    //  Y * 10**D = R + (X1 + X2) * 2**E
    //   E = binary scale factor
    //   D = decimal scale factor
    //   R = reference value
    //   X1 = 0
    //   X2 = scaled encoded value
    //   data[ i ] = (R + ( X1 + X2) * EE)/DD ;

    if (bitmap == null) {
      for (int i = 0; i < numberPoints; i++) {
        //data[ i ] = (R + ( X1 + X2) * EE)/DD ;
        data[i] = (R + bits2UInt(nb, raf) * EE) / DD;
      }
    } else {
      bitPos = 0;
      bitBuf = 0;
      for (int i = 0; i < bitmap.length; i++) {
        if (bitmap[i]) {
          data[i] = (R + bits2UInt(nb, raf) * EE) / DD;
        } else {
          data[i] = mv;   
          //data[i] = R / DD;
        }
      }
    }
    scanMode = gds.getGdsVars().getScanMode();
    Xlength = gds.getGdsVars().getNx();  // needs some smarts for different type Grids
    scanningModeCheck();
  }  // end simpleUnpacking

  /**
   * complex unpacking of Grib2 data.
   *
   * @param raf RandomAccessFile of Grib file
   * @param gds Grib2GridDefinitionSection
   * @param drs Grib2DataRepresentationSection
   * @param bms Grib2BitMapSection
   * @throws IOException on data reads
   */
  private void complexUnpacking(RandomAccessFile raf,
                                Grib2GridDefinitionSection gds,
                                Grib2DataRepresentationSection drs,
                                Grib2BitMapSection bms)
      throws IOException {

    int mvm = drs.getMissingValueManagement();
    //System.out.println( "DS mvm=" + mvm );

    float mv = Float.NaN;
    if (staticMissingValueInUse ) {
      mv = Float.NaN;
    } else if ( mvm == 0 ) {
      mv = Float.NaN;
    } else if ( mvm == 1 ) {
       mv = drs.getPrimaryMissingValue();
    } else if ( mvm == 2 ) {
       mv = drs.getSecondaryMissingValue() ;
    }

    int numberPoints = gds.getGdsVars().getNumberPoints();
    int NG = drs.getNumberOfGroups();
    //System.out.println( "DS NG=" + NG );
    if ( NG == 0 ) {
      logger.debug("Grib2DataSection.complexUnpacking : NG = 0 for file"+
      raf.getLocation());
      if( debug )
        System.out.println("Grib2DataSection.complexUnpacking : NG = 0 for file "+
          raf.getLocation());
      data = new float[numberPoints];
      if (mvm == 0) {
          for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
            //data[i] = X1[ i ];   should be equal to X1 but there's no X1 data
      } else { //if (mvm == 1) || (mvm == 2 )
          for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
      }
      return;
    }
    // 6-xx  Get reference values for groups (X1's)
    int[] X1 = new int[NG];
    int nb = drs.getNumberOfBits();
    //System.out.println( "DS nb=" + nb );
    if( nb != 0 ) {
      bitPos = 0;
      bitBuf = 0;
      for (int i = 0; i < NG; i++) {
        X1[i] = bits2UInt(nb, raf);
        //System.out.println( "DS X1[ i ]=" + X1[ i ] );
      }
    } else {
      for (int i = 0; i < NG; i++)
        X1[i] = 0;
    }

    // [xx +1 ]-yy Get number of bits used to encode each group
    int[] NB = new int[NG];
    nb = drs.getBitsGroupWidths();
    //System.out.println( "DS nb=" + nb );
    if( nb != 0 ) {
      bitPos = 0;
      bitBuf = 0;
      for (int i = 0; i < NG; i++) {
        NB[i] = bits2UInt(nb, raf);
        //System.out.println( "DS NB[ i ]=" + NB[ i ] );
      }
    } else {
      for (int i = 0; i < NG; i++)
        NB[i] = 0;
    }

    // [yy +1 ]-zz Get the scaled group lengths using formula
    //     Ln = ref + Kn * len_inc, where n = 1-NG,
    //          ref = referenceGroupLength, and  len_inc = lengthIncrement

    int[] L = new int[NG];
    int ref = drs.getReferenceGroupLength();
    //System.out.println( "DS ref=" + ref );

    int len_inc = drs.getLengthIncrement();
    //System.out.println( "DS len_inc=" + len_inc );

    nb = drs.getBitsScaledGroupLength();
    //System.out.println( "DS nb=" + nb );

    bitPos = 0;
    bitBuf = 0;
    for (int i = 0; i < NG; i++) {  // NG
      L[i] = ref + (bits2UInt(nb, raf) * len_inc);
      //System.out.println( "DS L[ i ]=" + L[ i ] );
    }
    //enter Length of Last Group
    L[NG - 1] = drs.getLengthLastGroup();

    int D = drs.getDecimalScaleFactor();
    //System.out.println( "DS D=" + D );
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    //System.out.println( "DS DD=" + DD );

    float R = drs.getReferenceValue();
    //System.out.println( "DS R=" + R );

    int E = drs.getBinaryScaleFactor();
    //System.out.println( "DS E=" + E );
    float EE = (float) java.lang.Math.pow((double) 2.0, (double) E);
    //System.out.println( "DS EE=" + EE );

    data = new float[numberPoints];
    //System.out.println( "DS dataPoints="+ gds.getNumberPoints() );
    Xlength = gds.getGdsVars().getNx();  // needs some smarts for different type Grids

    // [zz +1 ]-nn get X2 values and calculate the results Y using formula

//              Y = R + [(X1 + X2) * (2 ** E) * (10 ** D)]
//               WHERE:
//                     Y = THE VALUE WE ARE UNPACKING
//                     R = THE REFERENCE VALUE (FIRST ORDER MINIMA)
//                    X1 = THE PACKED VALUE
//                    X2 = THE SECOND ORDER MINIMA
//                     E = THE BINARY SCALE FACTOR
//                     D = THE DECIMAL SCALE FACTOR
    count = 0;
    int X2;
    bitPos = 0;
    bitBuf = 0;
    for (int i = 0; i < NG; i++) {
      //System.out.println( "DS NB[ i ]=" + NB[ i ] );
      //System.out.println( "DS L[ i ]=" + L[ i ] );
      //System.out.println( "DS X1[ i ]=" + X1[ i ] );
      for (int j = 0; j < L[i]; j++) {
        if (NB[i] == 0) {
          if (mvm == 0) {  // X2 = 0
            data[count++] = (R + X1[i] * EE) / DD;
          } else { //if (mvm == 1) || (mvm == 2 )
            data[count++] = mv;
          }
        } else {
          X2 = bits2UInt(NB[i], raf);
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
          //System.out.println( "DS count=" + count );
          //System.out.println( "DS NB[ "+ i +" ]=" + NB[ i ] );
          //System.out.println( "DS X1[ "+ i +" ]=" + X1[ i ] );
          //System.out.println( "DS X2 =" +X2 );
          //System.out.println( "DS X1[ i ] + X2 ="+(X1[ i ]+X2) );
        }
      }  // end for j
    }      // end for i

    boolean[] bitmap = bms.getBitmap();
    // bit map is used
    if (bitmap != null) {
      int idx = 0;
      float[] tmp = new float[numberPoints];
      for (int i = 0; i < numberPoints; i++) {
        if (bitmap[i]) {
          tmp[i] = data[idx++];
        } else {
          tmp[i] = mv;
        }
        //System.out.println( "tmp[ "+ i +"] ="+ data[ i]);
      }
      data = tmp;
    } //end bitmap

    scanMode = gds.getGdsVars().getScanMode();
    scanningModeCheck();
    //System.out.println( "DS true end =" + raf.position() );

  }  // end complexUnpacking

  /**
   * complex Unpacking With Spatial Differencing of Grib2 data.
   * The following code was extracted from the wgrib2 program from the NWS. The
   * c file commupack.c was used as the source. The variable names were changed to
   * make the understanding of the code clearer.
   *
   * @param raf RandomAccessFile of Grib file
   * @param gds Grib2GridDefinitionSection
   * @param drs Grib2DataRepresentationSection
   * @param bms Grib2BitMapSection
   * @throws IOException on data reads
   */
  private void complexUnpackingWithSpatial(RandomAccessFile raf,
                                           Grib2GridDefinitionSection gds,
                                           Grib2DataRepresentationSection drs,
                                           Grib2BitMapSection bms)
      throws IOException {



    // check first if missing values
    int mvm = drs.getMissingValueManagement();
    //System.out.println( "DS mvm=" + mvm );

    float mv = Float.NaN;
    if (staticMissingValueInUse ) {
      mv = Float.NaN;
    } else if ( mvm == 0 ) {
      mv = Float.NaN;
    } else if ( mvm == 1 ) {
       mv = drs.getPrimaryMissingValue();
    } else if ( mvm == 2 ) {
       mv = drs.getSecondaryMissingValue() ;
    }

    int  ival1 = 0,
    ival2 = 0,
    minsd = 0;

    // [6-ww]   1st values of undifferenced scaled values and minimums
    int os = drs.getOrderSpatial();
    int nbitsd = drs.getDescriptorSpatial();
    //System.out.println( "DS os=" + os +" ds =" + ds );
    bitPos = 0;
    bitBuf = 0;
    int sign;
    // ds is number of bytes, convert to bits -1 for sign bit
    nbitsd = nbitsd * 8;
    if (nbitsd != 0 ) {         // first order spatial differencing g1 and gMin
      sign = bits2UInt(1, raf);
      ival1 = bits2UInt(nbitsd -1, raf);
      if (sign == 1) {
        ival1 = -ival1;
      }
      if (os == 2) {  //second order spatial differencing h1, h2, hMin
        sign = bits2UInt(1, raf);
        ival2 = bits2UInt(nbitsd -1, raf);
        if (sign == 1) {
          ival2 = -ival2;
        }
      }
      sign = bits2UInt(1, raf);
      minsd = bits2UInt(nbitsd -1, raf);
      if (sign == 1) {
        minsd = -minsd;
      }
      //System.out.println( "DS nbitsd ="+ nbitsd +" ival1=" + ival1 +" ival2 =" + ival2 + " minsd=" + minsd );
    } else {
      System.out.println("DS error os=" + os + " nbitsd -1 =" + (nbitsd -1));
      return;
    }

    int numberPoints = gds.getGdsVars().getNumberPoints();
    int NG = drs.getNumberOfGroups();
    //System.out.println( "DS NG=" + NG );
    if ( NG == 0 ) {
      //logger.error("Grib2DataSection.complexUnpackingWithSpatial: NG = 0 for file"+ raf.getLocation());
      if( debug )
        System.out.println("Grib2DataSection.complexUnpackingWithSpatial: NG = 0 for file "+
          raf.getLocation());
      data = new float[numberPoints];
      if (mvm == 0) {
          for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
      } else { //if (mvm == 1) || (mvm == 2 )
          for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
      }
      return;
    }

    // [ww +1]-xx  Get reference values for groups (X1's)
    // X1 == gref
    int[] X1 = new int[NG];
    int nb = drs.getNumberOfBits();
    //System.out.println( "DS nb=" + nb );
    if ( nb != 0 ) {
      bitPos = 0;
      bitBuf = 0;
      for (int i = 0; i < NG; i++) {
        X1[i] = bits2UInt(nb, raf);
        //System.out.println( "DS X1[ i ]=" + X1[ i ] );
      }
    } else {
      for (int i = 0; i < NG; i++) {
        X1[i] = 0;
      }
    }

    // [xx +1 ]-yy Get number of bits used to encode each group
    // NB == gwidth
    int[] NB = new int[NG];
    nb = drs.getBitsGroupWidths();
    //System.out.println( "DS nb=" + nb );
    if ( nb != 0 ) {
      bitPos = 0;
      bitBuf = 0;
      for (int i = 0; i < NG; i++) {
        NB[i] = bits2UInt(nb, raf);
        //System.out.println( "DS X1[ i ]=" + X1[ i ] );
      }
    } else {
      for (int i = 0; i < NG; i++) {
        NB[i] = 0;
      }
    }

    int referenceGroupWidths = drs.getReferenceGroupWidths();
    //System.out.println( "DS len_inc=" + len_inc );
    for (int i = 0; i < NG; i++) {
        NB[i] += referenceGroupWidths;
    }

    // [yy +1 ]-zz Get the scaled group lengths using formula
    //     Ln = ref + Kn * len_inc, where n = 1-NG,
    //          ref = referenceGroupLength, and  len_inc = lengthIncrement

    int[] L = new int[NG];
    // L == glen
    int referenceGroupLength = drs.getReferenceGroupLength();
    //System.out.println( "DS ref=" + ref );

    nb = drs.getBitsScaledGroupLength();              
    //System.out.println( "DS nb=" + nb );
    int len_inc = drs.getLengthIncrement();

    bitPos = 0;
    bitBuf = 0;

    if (nb != 0) {
      for (int i = 0; i < NG; i++) {   
        L[i] = bits2UInt(nb, raf);

      }
    } else {
      for (int i = 0; i < NG; i++)
        L[i] = 0;
    }
    int totalL = 0;
    //System.out.println( "DS NG=" + NG );
    for (int i = 0; i < NG; i++) {
        L[i] = L[i] * len_inc  +referenceGroupLength;
        //System.out.print( "DS L[ i ]=" + L[ i ] );
        totalL += L[ i ];
        //System.out.println( " totalL=" + totalL +" "+ i);
    }
    totalL -= L[NG - 1];
    totalL += drs.getLengthLastGroup();

    //enter Length of Last Group
    L[NG - 1] = drs.getLengthLastGroup();

    // test
    if( mvm != 0 ) {
      if ( totalL != numberPoints ) {
        for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
        return;
      }
    } else {
      if ( totalL != drs.getDataPoints() ){
        for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
        return;
      }
    }
    


    int D = drs.getDecimalScaleFactor();
    //System.out.println( "DS D=" + D );
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    //System.out.println( "DS DD=" + DD );

    float R = drs.getReferenceValue();
    //System.out.println( "DS R=" + R );

    int E = drs.getBinaryScaleFactor();
    //System.out.println( "DS E=" + E );
    float EE = (float) java.lang.Math.pow((double) 2.0, (double) E);
    //System.out.println( "DS EE=" + EE );

    data = new float[numberPoints];
    //System.out.println( "DS dataPoints="+ gds.getNumberPoints() );
    Xlength = gds.getGdsVars().getNx();  // needs some smarts for different type Grids

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
    count = 0;
    bitPos = 0;
    bitBuf = 0;
    int dataSize = 0;
    boolean[] dataBitMap = null;
    if (mvm == 0) {
      for (int i = 0; i < NG; i++) {
      //System.out.println( "DS NB[ i ]=" + NB[ i ] );
      //System.out.println( "DS L[ i ]=" + L[ i ] );
      //System.out.println( "DS X1[ i ]=" + X1[ i ] );
        if (NB[i] != 0) {
            for (int j = 0; j < L[i]; j++)
              data[count++] = bits2UInt(NB[i], raf) +X1[i];
        } else {
            for (int j = 0; j < L[i]; j++)
              data[count++] = X1[i];
          //System.out.println( "DS count=" + count );
          //System.out.println( "DS NB[ "+ i +" ]=" + NB[ i ] );
          //System.out.println( "DS X1[ "+ i +" ]=" + X1[ i ] );
          //System.out.println( "data[ "+ (count -1) +"] ="+ data[ count -1 ]);
        }
      }  // end for i
    } else if (mvm == 1 || mvm == 2 )  {
      // don't add missing values into data but keep track of them in dataBitMap
      dataBitMap = new boolean[ numberPoints ];
      dataSize = 0;
      for (int i = 0; i < NG; i++) {
      //System.out.println( "DS NB[ i ]=" + NB[ i ] );
      //System.out.println( "DS L[ i ]=" + L[ i ] );
      //System.out.println( "DS X1[ i ]=" + X1[ i ] );
        if (NB[i] != 0) {
            int msng1 = bitsmv1[ NB[i] ];
            int msng2 = msng1 -1;
            for (int j = 0; j < L[i]; j++) {
              data[count] = bits2UInt(NB[i], raf);
              if ( data[count] == msng1 ) {
                dataBitMap[ count ] = false;
              } else if ( mvm == 2 && data[count] == msng2 ) {
                dataBitMap[ count ] = false;
              } else {
                dataBitMap[ count ] = true;
                data[ dataSize++ ] = data[count] + X1[ i ];
              }
              count++;
            }
        } else {
            int msng1 = bitsmv1[ drs.getNumberOfBits() ];
            int msng2 = msng1 -1;
            if ( X1[ i ] == msng1 ) {
               for (int j = 0; j < L[i]; j++)
                 dataBitMap[count++] = false;
                 //data[count++] = X1[i];
            } else if ( mvm == 2 &&  X1[ i ] == msng2 ) {
               for (int j = 0; j < L[i]; j++)
                 dataBitMap[count++] = false;
            } else {
              for (int j = 0; j < L[i]; j++)  {
                 dataBitMap[ count ] = true;
                 data[ dataSize++ ] = X1[ i ];
                 count++;
              }
            }
          //System.out.println( "DS count=" + count );
          //System.out.println( "DS NB[ "+ i +" ]=" + NB[ i ] );
          //System.out.println( "DS X1[ "+ i +" ]=" + X1[ i ] );
          //System.out.println( "DS X2 =" +X2 );
          //System.out.println( "DS X1[ i ] + X2 ="+(X1[ i ]+X2) );
          //System.out.println( "data[ "+ (count -1) +"] ="+ data[ count -1 ]);
        }
      }  // end for i
    }

    // first order spatial differencing
    if (os == 1) {   // g1 and gMin
      // encoded by G(n) = F(n) - F(n -1 )
      // decoded by F(n) = G(n) + F(n -1 )
      // data[] at this point contains G0, G1, G2, ....
      data[ 0 ] = ival1;
      int itemp;
      if (mvm == 0) {           // no missing values
        itemp = numberPoints;
      } else {
        itemp = dataSize;
      }
      for (int i = 1; i < itemp; i++) {
          data[i] += minsd;
          data[i] = data[i] + data[i - 1];
          //System.out.println( "data[ "+ i +"] ="+ data[ i ]);
       }
    } else if (os == 2) { // 2nd order
       data[ 0 ] = ival1;
       data[ 1 ] = ival2;
       int itemp;
       if (mvm == 0) {           // no missing values
         itemp = numberPoints;
       } else {
         itemp = dataSize;
       }
       for (int i = 2; i < itemp; i++) {
          data[i] += minsd;
          data[i] = data[i] + ( 2 * data[i - 1]) - data[ i -2 ];
          //System.out.println( "data[ "+ i +"] ="+ data[ i ]);
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
        //System.out.println( "data[ "+ i +"] ="+ data[ i ]);
      }
    } else {         // missing value == 1  || missing value == 2
      dataSize = 0;
      float[] tmp = new float[numberPoints];
      for (int i = 0; i < data.length; i++) {
        if ( dataBitMap[ i ] ) {
           tmp[i] = (R + (data[ dataSize++ ] * EE)) / DD;
        } else { // mvm = 1 or 2
          tmp[i] = mv;
        }
      }
      data = tmp;
    }

    boolean[] bitmap = bms.getBitmap();
    // bit map is used
    if (bitmap != null) {
      int idx = 0;
      float[] tmp = new float[numberPoints];
      for (int i = 0; i < numberPoints; i++) {
        if ( bitmap[i] ) {
          tmp[ i ] = data[ idx++] ;
        } else {
          tmp[i] = mv;
        }
        //System.out.println( "tmp[ "+ i +"] ="+ data[ i]);
      }
      data = tmp;
    }

    scanMode = gds.getGdsVars().getScanMode();
    scanningModeCheck();
    //System.out.println( "DS true end =" + raf.position() );

  }  // end complexUnpackingWithSpatial

  /**
   * Jpeg2000 unpacking method for Grib2 data.
   * @param raf RandomAccessFile of Grib file
   * @param gds Grib2GridDefinitionSection
   * @param drs Grib2DataRepresentationSection
   * @param bms Grib2BitMapSection
   * @throws IOException on data read
   *
   */
  private void jpeg2000Unpacking(RandomAccessFile raf,
                                 Grib2GridDefinitionSection gds,
                                 Grib2DataRepresentationSection drs,
                                 Grib2BitMapSection bms) throws IOException {
    // 6-xx  jpeg2000 data block to decode

    // dataPoints are number of points encoded, it could be less than the
    // numberPoints in the grid record if bitMap is used, otherwise equal
    //int dataPoints = drs.getDataPoints();
    //System.out.println( "DS DRS dataPoints=" + drs.getDataPoints() );
    //System.out.println( "DS length=" + length );

    int mvm = drs.getMissingValueManagement();
    //System.out.println( "DS mvm=" + mvm );

    float mv = Float.NaN;
    if (staticMissingValueInUse ) {
      mv = Float.NaN;
    } else if ( mvm == 0 ) {
      mv = Float.NaN;
    } else if ( mvm == 1 ) {
       mv = drs.getPrimaryMissingValue();
    } else if ( mvm == 2 ) {
       mv = drs.getSecondaryMissingValue() ;
    }

    int nb = drs.getNumberOfBits();
    //System.out.println( "DS nb = " + nb );

    int D = drs.getDecimalScaleFactor();
    //System.out.println( "DS D=" + D );
    float DD = (float) java.lang.Math.pow((double) 10, (double) D);
    //System.out.println( "DS DD=" + DD );

    float R = drs.getReferenceValue();
    //System.out.println( "DS R=" + R );

    int E = drs.getBinaryScaleFactor();
    //System.out.println( "DS E=" + E );
    float EE = (float) java.lang.Math.pow((double) 2.0, (double) E);
    //System.out.println( "DS EE=" + EE );

    Grib2JpegDecoder g2j = null;
    int numberPoints = 0;
    try {
      if (nb != 0) {  // there's data to decode
        String[] argv = new String[4];
        argv[0] = "-rate";
        argv[1] = Integer.toString(nb);
        argv[2] = "-verbose";
        argv[3] = "off";
        //argv[ 4 ] = "-debug" ;
        //argv[ 2 ] = "-nocolorspace" ;
        //argv[ 3 ] = "-Rno_roi" ;
        //argv[ 4 ] = "-cdstr_info" ;
        //argv[ 5 ] = "-verbose" ;
        g2j = new Grib2JpegDecoder(argv);
        // how jpeg2000.jar use to decode, used raf
        //g2j.decode(raf, length - 5);
        // jpeg-1.0.jar added method to have the data read first
        byte[] buf = new byte[ length - 5 ];
        raf.read( buf );
        g2j.decode( buf );
      }
      numberPoints = gds.getGdsVars().getNumberPoints();
      //System.out.println( "DS GDS NumberPoints=" +  gds.getNumberPoints() );
      data = new float[numberPoints];
      boolean[] bitmap = bms.getBitmap();

      if (nb == 0) {  // no data to decoded, set to reference or  MissingValue
        if (mvm == 0) {
          for (int i = 0; i < numberPoints; i++)
            data[i] = R;
        } else { //if (mvm == 1) || (mvm == 2 )
          for (int i = 0; i < numberPoints; i++)
            data[i] = mv;
        }
      } else if (bitmap == null) {
        //System.out.println( "DS jpeg data length ="+ g2j.data.length );
        if (g2j.data.length != numberPoints) {
          data = null;
          return;
        }
        for (int i = 0; i < numberPoints; i++) {
          //Y = (R + ( 0 + X2) * EE)/DD ;
          data[i] = (R + g2j.data[i] * EE) / DD;
          //System.out.println( "DS data[ " + i +"  ]=" + data[ i ] );
        }
      } else {  // use bitmap
        for (int i = 0, j = 0; i < bitmap.length; i++) {
          if (bitmap[i]) {
            data[i] = (R + g2j.data[j++] * EE) / DD;
          } else {
            data[i] = mv;
          }
        }
      }
    } catch (NullPointerException npe) {
      logger.error("Grib2DataSection.jpeg2000Unpacking: bit rate too small nb ="+
        nb +" for file"+ raf.getLocation());
      if ( debug ) {
        System.out.println("Grib2DataSection.jpeg2000Unpacking: bit rate too small nb ="+
        nb +" for file"+ raf.getLocation());
        long starting = raf.getFilePointer() - (long) length;
        System.out.println("location =" + starting);
      }  
      for (int i = 0; i < numberPoints; i++) {
        data[i] = mv;
      }
      return;
    }
    scanMode = gds.getGdsVars().getScanMode();
    scanningModeCheck();
  }  // end jpeg2000Unpacking


  /**
   * Convert bits (nb) to Unsigned Int .
   *
   * @param nb  the number of bits to convert to int
   * @param raf file handle
   * @return int of DataSections section
   * @throws IOException   exception
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
  }                                            // end bits2UInt

  /**
   * Rearrange the data array using the scanning mode.
   */
  private void scanningModeCheck() {
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
      for (int index = 0; index < data.length; index += Xlength) {
        for (int idx = 0; idx < mid; idx++) {
          tmp = data[index + idx];
          data[index + idx] = data[index + Xlength - idx - 1];
          data[index + Xlength - idx - 1] = tmp;
          //System.out.println( "switch " + (index + idx) + " " +
          //(index + Xlength -idx -1) );
        }
      }
      return;
    }
    // else
    // scanMode == 16, 80, 144, 208 adjacent rows scan opposite dir
    float tmp;
    int mid = (int) Xlength / 2;
    //System.out.println( "Xlength =" +Xlength +" mid ="+ mid );
    for (int index = 0; index < data.length; index += Xlength) {
      int row = (int) index / Xlength;
      if (row % 2 == 1) {  // odd numbered row, calculate reverse index
        for (int idx = 0; idx < mid; idx++) {
          tmp = data[index + idx];
          data[index + idx] = data[index + Xlength - idx - 1];
          data[index + Xlength - idx - 1] = tmp;
          //System.out.println( "switch " + (index + idx) + " " +
          //(index + Xlength -idx -1) );
        }
      }
    }
  }                            // end of scanningModeCheck

  /**
   * Grib2 data unpacked as floats.
   *
   * @return data
   */
  public final float[] getData() {
    return data;
  }

  /**
   * flag to signifly if a static Missing Value is used. Since it's possible to have different missing values
   * in a Grib file, the first record's missing value might not be the correct missing value for the current
   * record. If a static missing value is used (float.NaN) then there will be no conflict of missing value
   * processing.
   * @return staticMissingValueInUse boolean
   */
  public static boolean isStaticMissingValueInUse() {
    return staticMissingValueInUse;
  }

  /**
   * flag to signifly if a static Missing Value is used. Since it's possible to have different missing values
   * in a Grib file, the first record's missing value might not be the correct missing value for the current
   * record. If a static missing value is used (float.NaN) then there will be no conflict of missing value
   * processing.
   * @param staticMissingValueInUse boolean
   */
  public static void setStaticMissingValueInUse(boolean staticMissingValueInUse) {
    Grib2DataSection.staticMissingValueInUse = staticMissingValueInUse;
  }
}

