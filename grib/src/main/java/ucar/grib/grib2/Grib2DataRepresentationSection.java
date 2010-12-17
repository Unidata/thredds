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

// $Id: Grib2DataRepresentationSection.java,v 1.17 2005/12/08 20:59:54 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;

import ucar.unidata.io.RandomAccessFile;

/*
 * Grib2DataRepresentationSection.java  1.0  08/02/2003
 * @author Robb Kambic
 *
 */

import java.io.IOException;


/**
 * A class that represents the DataRepresentationSection of a GRIB product.
 * These are the variables needed by the Data Section class
 *
 */
public final class Grib2DataRepresentationSection {

    /**
     * Length in bytes of DataRepresentationSection section.
     */
    private final int length;

    /**
     * Number of this section, should be 5.
     */
    private final int section;

    /**
     * Number of Data points.
     */
    private final int dataPoints;

    /**
     * Data representation template number.
     */
    private final int dataTemplate;

    /**
     * Reference value (R) (IEEE 32-bit floating-point value).
     */
    private float referenceValue;

    /**
     * Binary scale factor (E).
     */
    private int binaryScaleFactor;

    /**
     * Decimal scale factor (D).
     */
    private int decimalScaleFactor;

    /**
     * Number of bits used for each packed value.
     */
    private int numberOfBits;

    /**
     * data type of original field values.
     */
    private int originalType;

    /**
     * Group splitting method used (see Code Table 5.4).
     */
    private int splittingMethod;

    /**
     * Type compression method used (see Code Table 5.40000).
     */
    private int compressionMethod;

    /**
     * Compression ratio used.
     */
    private int compressionRatio;

    /**
     * Missing value management used (see Code Table 5.5).
     */
    private int missingValueManagement;

    /**
     * Primary missing value substitute.
     */
    private float primaryMissingValue = GribNumbers.UNDEFINED;

    /**
     * Secondary missing value substitute.
     */
    private float secondaryMissingValue = GribNumbers.UNDEFINED;

    /**
     * NG - Number of groups of data values into which field is split.
     */
    private int numberOfGroups;

    /**
     * Reference for group widths (see Note 12).
     */
    private int referenceGroupWidths;

    /**
     * Number of bits used for the group widths (after the reference value.
     * in octet 36 has been removed)
     */
    private int bitsGroupWidths;

    /**
     * Reference for group lengths (see Note 13).
     */
    private int referenceGroupLength;

    /**
     * Length increment for the group lengths (see Note 14).
     */
    private int lengthIncrement;

    /**
     * Length increment for the group lengths (see Note 14).
     */
    private int lengthLastGroup;

    /**
     * Number of bits used for the scaled group lengths (after subtraction of
     * the reference value given in octets 38-41 and division by the length
     * increment given in octet 42).
     */
    private int bitsScaledGroupLength;

    /**
     * Order of spatial differencing (see Code Table 5.6).
     */
    private int orderSpatial;

    /**
     * Number of octets required in the Data Section to specify extra
     * descriptors needed for spatial differencing (octets 6-ww in Data
     * Template 7.3) .
     */
    private int descriptorSpatial;

    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib2DataRepresentationSection</tt> object from a raf.
     *
     * @param  raf RandomAccessFile with Section DRS content
     * @throws IOException  if stream contains no valid GRIB file
     */
    public Grib2DataRepresentationSection(RandomAccessFile raf)
            throws IOException {

        // octets 1-4 (Length of DRS)
        length = GribNumbers.int4(raf);
        //System.out.println( "DRS length=" + length );

        // octet 5
        section = raf.read();
        //System.out.println( "DRS is 5 section=" + section );

        // octets 6-9 number of datapoints
        dataPoints = GribNumbers.int4(raf);
        //System.out.println( "DRS dataPoints=" + dataPoints );

        // octet 10
        dataTemplate = GribNumbers.uint2(raf);
        //System.out.println( "DRS dataTemplate=" + dataTemplate );

        switch (dataTemplate) {  // Data Template Number

          case 0 :
          case 1 :               // 0 - Grid point data - simple packing 
              // 1 - Matrix values - simple packing
              //System.out.println( "DRS dataTemplate=" + dataTemplate );
              referenceValue     = raf.readFloat();
              binaryScaleFactor  = GribNumbers.int2(raf);
              decimalScaleFactor = GribNumbers.int2(raf);
              numberOfBits       = raf.read();
              //System.out.println( "DRS numberOfBits=" + numberOfBits );
              originalType = raf.read();
              //System.out.println( "DRS originalType=" + originalType );

              if (dataTemplate == 0) {
                  break;
              }
              // case 1 not implememted
              System.out.println("DRS dataTemplate=1 not implemented yet");
              break;

          case 2 :
          case 3 :  // Grid point data - complex packing
              //System.out.println( "DRS dataTemplate=" + dataTemplate );
              // octet 12 - 15
              referenceValue = raf.readFloat();
              // octet 16 - 17
              binaryScaleFactor = GribNumbers.int2(raf);
              // octet 18 - 19
              decimalScaleFactor = GribNumbers.int2(raf);
              // octet 20
              numberOfBits = raf.read();
              //System.out.println( "DRS numberOfBits=" + numberOfBits );
              // octet 21
              originalType = raf.read();
              //System.out.println( "DRS originalType=" + originalType );
              // octet 22
              splittingMethod = raf.read();
              //System.out.println( "DRS splittingMethod=" + 
              //     splittingMethod );
              // octet 23
              missingValueManagement = raf.read();
              //System.out.println( "DRS missingValueManagement=" + 
              //     missingValueManagement );
              // octet 24 - 27
              primaryMissingValue = raf.readFloat();
              // octet 28 - 31
              secondaryMissingValue = raf.readFloat();
              // octet 32 - 35
              numberOfGroups = GribNumbers.int4(raf);
              //System.out.println( "DRS numberOfGroups=" + 
              //     numberOfGroups );
              // octet 36
              referenceGroupWidths = raf.read();
              //System.out.println( "DRS referenceGroupWidths=" + 
              //     referenceGroupWidths );
              // octet 37
              bitsGroupWidths = raf.read();
              // according to documentation subtract referenceGroupWidths
              // TODO: check again and delete
              //bitsGroupWidths = bitsGroupWidths - referenceGroupWidths;
              //System.out.println( "DRS bitsGroupWidths=" + 
              //     bitsGroupWidths );
              // octet 38 - 41
              referenceGroupLength = GribNumbers.int4(raf);
              //System.out.println( "DRS referenceGroupLength=" + 
              //     referenceGroupLength );
              // octet 42
              lengthIncrement = raf.read();
              //System.out.println( "DRS lengthIncrement=" + 
              //     lengthIncrement );
              // octet 43 - 46
              lengthLastGroup = GribNumbers.int4(raf);
              //System.out.println( "DRS lengthLastGroup=" + 
              //     lengthLastGroup );
              // octet 47
              bitsScaledGroupLength = raf.read();
              //System.out.println( "DRS bitsScaledGroupLength=" + 
              //     bitsScaledGroupLength );
              if (dataTemplate == 2) {
                  break;
              }

              // case 3 // complex packing & spatial differencing
              // octet 48
              orderSpatial = raf.read();
              //System.out.println( "DRS orderSpatial=" + orderSpatial);
              // octet 49
              descriptorSpatial = raf.read();
              //System.out.println( "DRS descriptorSpatial=" + descriptorSpatial);
              break;

          case 40 :
          case 40000 :  // Grid point data - JPEG 2000 Code Stream Format
              //System.out.println( "DRS dataTemplate=" + dataTemplate );
              referenceValue     = raf.readFloat();
              binaryScaleFactor  = GribNumbers.int2(raf);
              decimalScaleFactor = GribNumbers.int2(raf);
              numberOfBits       = raf.read();
              //System.out.println( "DRS numberOfBits=" + numberOfBits );
              originalType = raf.read();
              //System.out.println( "DRS originalType=" + originalType );
              compressionMethod = raf.read();
              //System.out.println( "DRS compressionMethod=" + compressionMethod );
              compressionRatio = raf.read();
              //System.out.println( "DRS compressionRatio=" + compressionRatio );
              break;

          default :
        }


    }  // end of Grib2DataRepresentationSection

    /**
     * Get the byte length of the Section DRS section.
     *
     * @return length in bytes of Section DRS section
     */
    public final int getLength() {
        return length;
    }

    /**
     * Get the number of dataPoints in DS section.
     *
     * @return number of dataPoints in DS section
     */
    public final int getDataPoints() {
        return dataPoints;
    }

    /**
     * Get the Data Template Number for the GRID.
     *
     * @return Data Template Number
     */
    public final int getDataTemplateNumber() {
        return dataTemplate;
    }

    /**
     * Reference value (R) (IEEE 32-bit floating-point value).
     * @return ReferenceValue
     */
    public final float getReferenceValue() {
        return referenceValue;
    }

    /**
     * Binary scale factor (E).
     * @return BinaryScaleFactor
     */
    public final int getBinaryScaleFactor() {
        return binaryScaleFactor;
    }

    /**
     * Decimal scale factor (D).
     * @return DecimalScaleFactor
     */
    public final int getDecimalScaleFactor() {
        return decimalScaleFactor;
    }

    /**
     * Number of bits used for each packed value..
     * @return NumberOfBits NB
     */
    public final int getNumberOfBits() {
        return numberOfBits;
    }

    /**
     * Type of original field values.
     * @return OriginalType dataType
     */
    public final int getOriginalType() {
        return originalType;
    }

    /**
     * Group splitting method used (see Code Table 5.4).
     * @return SplittingMethod
     */
    public final int getSplittingMethod() {
        return splittingMethod;
    }

    /**
     * Type compression method used (see Code Table 5.40000).
     * @return CompressionMethod
     */
    public final int getCompressionMethod() {
        return compressionMethod;
    }

    /**
     * Compression ratio used .
     * @return CompressionRatio
     */
    public final int getCompressionRatio() {
        return compressionRatio;
    }

    /**
     * Missing value management used (see Code Table 5.5).
     * @return MissingValueManagement
     */
    public final int getMissingValueManagement() {
        return missingValueManagement;
    }

    /**
     * Primary missing value substitute.
     * @return PrimaryMissingValue
     */
    public final float getPrimaryMissingValue() {
        return primaryMissingValue;
    }

    /**
     * Secondary missing value substitute.
     * @return SecondaryMissingValue
     */
    public final float getSecondaryMissingValue() {
        return secondaryMissingValue;
    }

    /**
     * NG - Number of groups of data values into which field is split.
     * @return NumberOfGroups NG
     */
    public final int getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * Reference for group widths (see Note 12).
     * @return ReferenceGroupWidths
     */
    public final int getReferenceGroupWidths() {
        return referenceGroupWidths;
    }

    /**
     * Number of bits used for the group widths (after the reference value
     * in octet 36 has been removed).
     * @return BitsGroupWidths
     */
    public final int getBitsGroupWidths() {
        return bitsGroupWidths;
    }

    /**
     * Reference for group lengths (see Note 13).
     * @return ReferenceGroupLength
     */
    public final int getReferenceGroupLength() {
        return referenceGroupLength;
    }

    /**
     * Length increment for the group lengths (see Note 14).
     * @return LengthIncrement
     */
    public final int getLengthIncrement() {
        return lengthIncrement;
    }

    /**
     * Length increment for the group lengths (see Note 14).
     * @return LengthLastGroup
     */
    public final int getLengthLastGroup() {
        return lengthLastGroup;
    }

    /**
     * Number of bits used for the scaled group lengths (after subtraction of
     * the reference value given in octets 38-41 and division by the length
     * increment given in octet 42).
     * @return BitsScaledGroupLength
     */
    public final int getBitsScaledGroupLength() {
        return bitsScaledGroupLength;
    }

    /**
     * Order of spatial differencing (see Code Table 5.6).
     * @return OrderSpatial
     */
    public final int getOrderSpatial() {
        return orderSpatial;
    }

    /**
     * Number of octets required in the Data Section to specify extra
     * descriptors needed for spatial differencing (octets 6-ww in Data
     * Template 7.3).
     * @return DescriptorSpatial
     */
    public final int getDescriptorSpatial() {
        return descriptorSpatial;
    }
}

