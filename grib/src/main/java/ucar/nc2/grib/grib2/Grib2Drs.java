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
import ucar.nc2.iosp.BitReader;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Template-specific fields for Grib2SectionDataRepresentation
 * @author caron
 * @since 4/1/11
 */
public abstract class Grib2Drs {

  public static Grib2Drs factory(int template, RandomAccessFile raf) throws IOException {
    switch (template) {
      case 0:
        return new Type0(raf);
      case 2:
        return new Type2(raf);
      case 3:
        return new Type3(raf);
      case 40:
        return new Type40(raf);
      case 50002: // ECMWF's second order packing
    	return new Type50002(raf);
      default:
        throw new UnsupportedOperationException("Unsupported DRS type = " + template);
    }
  }

  public abstract int getNBits();

  public int getNGroups() {
    return 1;
  }

  public static class Type0 extends Grib2Drs {
    public float referenceValue;
    public int binaryScaleFactor, decimalScaleFactor, numberOfBits, originalType;

    Type0(RandomAccessFile raf) throws IOException {
      this.referenceValue = raf.readFloat();
      this.binaryScaleFactor = GribNumbers.int2(raf);
      this.decimalScaleFactor = GribNumbers.int2(raf);
      this.numberOfBits = raf.read();
      this.originalType = raf.read();
    }

    @Override
    public int getNBits() {
      return numberOfBits;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Type0");
      sb.append("{referenceValue=").append(referenceValue);
      sb.append(", binaryScaleFactor=").append(binaryScaleFactor);
      sb.append(", decimalScaleFactor=").append(decimalScaleFactor);
      sb.append(", numberOfBits=").append(numberOfBits);
      sb.append(", originalType=").append(originalType);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Type0 type0 = (Type0) o;

      if (binaryScaleFactor != type0.binaryScaleFactor) return false;
      if (decimalScaleFactor != type0.decimalScaleFactor) return false;
      if (numberOfBits != type0.numberOfBits) return false;
      if (originalType != type0.originalType) return false;
      if (Float.compare(type0.referenceValue, referenceValue) != 0) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (referenceValue != +0.0f ? Float.floatToIntBits(referenceValue) : 0);
      result = 31 * result + binaryScaleFactor;
      result = 31 * result + decimalScaleFactor;
      result = 31 * result + numberOfBits;
      result = 31 * result + originalType;
      return result;
    }
  }

  /*
  Data representation template 5.2 – Grid point data – complex packing
  Note: For most templates, details of the packing process are described in Regulation

  22 Group splitting method used (see Code table 5.4)
  23 Missing value management used (see Code table 5.5)
  24–27 Primary missing value substitute
  28–31 Secondary missing value substitute
  32–35 NG – number of groups of data values into which field is split
  36 Reference for group widths (see Note 12)
  37 Number of bits used for the group widths (after the reference value in octet 36 has been removed)
  38–41 Reference for group lengths (see Note 13)
  42 Length increment for the group lengths (see Note 14)
  43–46 True length of last group
  47 Number of bits used for the scaled group lengths (after subtraction of the reference value
  given in octets 38–41 and division by the length increment given in octet 42)

  Code Table Code table 5.5 - Missing value management for complex packing (5.5)
      0: No explicit missing values included within data values
      1: Primary missing values included within data values
      2: Primary and secondary missing values included within data values
   */

  public static class Type2 extends Type0 {
    public float secondaryMissingValue, primaryMissingValue;
    public int missingValueManagement, splittingMethod, numberOfGroups, referenceGroupWidths, bitsGroupWidths;
    public int referenceGroupLength, lengthIncrement, lengthLastGroup, bitsScaledGroupLength;

    Type2(RandomAccessFile raf) throws IOException {
      super(raf);
      this.splittingMethod = raf.read();
      this.missingValueManagement = raf.read();
      this.primaryMissingValue = raf.readFloat();
      this.secondaryMissingValue = raf.readFloat();
      this.numberOfGroups = GribNumbers.int4(raf);
      this.referenceGroupWidths = raf.read();
      this.bitsGroupWidths = raf.read();
      this.referenceGroupLength = GribNumbers.int4(raf);
      this.lengthIncrement = raf.read();
      this.lengthLastGroup = GribNumbers.int4(raf);
      this.bitsScaledGroupLength = raf.read();
    }

    @Override
    public int getNGroups() {
      return numberOfGroups;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nType2");
      sb.append("{secondaryMissingValue=").append(secondaryMissingValue);
      sb.append(", primaryMissingValue=").append(primaryMissingValue);
      sb.append(", missingValueManagement=").append(missingValueManagement);
      sb.append(", splittingMethod=").append(splittingMethod);
      sb.append(", numberOfGroups=").append(numberOfGroups);
      sb.append(", referenceGroupWidths=").append(referenceGroupWidths);
      sb.append(", bitsGroupWidths=").append(bitsGroupWidths);
      sb.append(", referenceGroupLength=").append(referenceGroupLength);
      sb.append(", lengthIncrement=").append(lengthIncrement);
      sb.append(", lengthLastGroup=").append(lengthLastGroup);
      sb.append(", bitsScaledGroupLength=").append(bitsScaledGroupLength);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Type2 type2 = (Type2) o;

      if (bitsGroupWidths != type2.bitsGroupWidths) return false;
      if (bitsScaledGroupLength != type2.bitsScaledGroupLength) return false;
      if (lengthIncrement != type2.lengthIncrement) return false;
      if (lengthLastGroup != type2.lengthLastGroup) return false;
      if (missingValueManagement != type2.missingValueManagement) return false;
      if (numberOfGroups != type2.numberOfGroups) return false;
      if (Float.compare(type2.primaryMissingValue, primaryMissingValue) != 0) return false;
      if (referenceGroupLength != type2.referenceGroupLength) return false;
      if (referenceGroupWidths != type2.referenceGroupWidths) return false;
      if (Float.compare(type2.secondaryMissingValue, secondaryMissingValue) != 0) return false;
      if (splittingMethod != type2.splittingMethod) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (secondaryMissingValue != +0.0f ? Float.floatToIntBits(secondaryMissingValue) : 0);
      result = 31 * result + (primaryMissingValue != +0.0f ? Float.floatToIntBits(primaryMissingValue) : 0);
      result = 31 * result + missingValueManagement;
      result = 31 * result + splittingMethod;
      result = 31 * result + numberOfGroups;
      result = 31 * result + referenceGroupWidths;
      result = 31 * result + bitsGroupWidths;
      result = 31 * result + referenceGroupLength;
      result = 31 * result + lengthIncrement;
      result = 31 * result + lengthLastGroup;
      result = 31 * result + bitsScaledGroupLength;
      return result;
    }
  }

  public static class Type3 extends Type2 {
    public int orderSpatial, descriptorSpatial;

    Type3(RandomAccessFile raf) throws IOException {
      super(raf);
      this.orderSpatial = raf.read();
      this.descriptorSpatial = raf.read();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nType3");
      sb.append("{orderSpatial=").append(orderSpatial);
      sb.append(", descriptorSpatial=").append(descriptorSpatial);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Type3 type3 = (Type3) o;

      if (descriptorSpatial != type3.descriptorSpatial) return false;
      if (orderSpatial != type3.orderSpatial) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + orderSpatial;
      result = 31 * result + descriptorSpatial;
      return result;
    }
  }

  public static class Type40 extends Type0 {
    public int compressionMethod, compressionRatio;
    boolean hasSignedProblem = false;

    Type40(RandomAccessFile raf) throws IOException {
      super(raf);
      this.compressionMethod = raf.read();
      this.compressionRatio = raf.read();
    }

    public boolean hasSignedProblem() {
      return hasSignedProblem;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nType40");
      sb.append("{compressionMethod=").append(compressionMethod);
      sb.append(", compressionRatio=").append(compressionRatio);
      sb.append(", hasSignedProblem=").append(hasSignedProblem);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Type40 type40 = (Type40) o;

      if (compressionMethod != type40.compressionMethod) return false;
      if (compressionRatio != type40.compressionRatio) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + compressionMethod;
      result = 31 * result + compressionRatio;
      return result;
    }
  }

  // pull request #52 "lost-carrier" jkaehler@meteomatics.com
  public static class Type50002 extends Grib2Drs {
	    public float referenceValue;
	    public int binaryScaleFactor, decimalScaleFactor, numberOfBits;
	    public int p1, p2;
	    public int widthOfFirstOrderValues, widthOfWidth, widthOfLength;
	    public int boustrophonic, orderOfSPD, widthOfSPD;
	    public int[] spd;
	    public int lengthOfSection6, section6;
	    public int bitMapIndicator;
	    public int lengthOfSection7, section7;

	    Type50002(RandomAccessFile raf) throws IOException {
	      // according to https://github.com/erdc-cm/grib_api/blob/master/definitions/grib2/template.5.50002.def
	      this.referenceValue = raf.readFloat();
	      this.binaryScaleFactor = GribNumbers.int2(raf);
	      this.decimalScaleFactor = GribNumbers.int2(raf);
	      this.numberOfBits = raf.read();
	      this.widthOfFirstOrderValues = raf.read();
	      this.p1 = GribNumbers.int4(raf);
	      this.p2 = GribNumbers.int4(raf);
	      this.widthOfWidth = raf.read();
	      this.widthOfLength = raf.read();
	      this.boustrophonic = raf.read();
	      this.orderOfSPD = raf.read();
	      this.widthOfSPD = raf.read();
	      this.spd = new int[orderOfSPD+1];
	      BitReader bitReader = new BitReader(raf, raf.getFilePointer());
	      for (int i = 0; i < orderOfSPD; i++) {
/*
 	    	  int myInt = raf.read();
	    	  System.out.println("spd["+i+"]="+myInt+" 0x"+Integer.toHexString(myInt)+" 0b"+Integer.toBinaryString(myInt)+" widthOfSPD="+widthOfSPD);
	    	  myInt = raf.read();
	    	  System.out.println("spd["+i+"]="+myInt+" 0x"+Integer.toHexString(myInt)+" 0b"+Integer.toBinaryString(myInt)+" widthOfSPD="+widthOfSPD);
/**/
		     this.spd[i] = (int) bitReader.bits2UInt(widthOfSPD);
	      }
	      this.spd[orderOfSPD] = (int) bitReader.bits2SInt(widthOfSPD);
	      this.lengthOfSection6 = GribNumbers.int4(raf);
	      this.section6 = raf.read();
	      this.bitMapIndicator = raf.read();
	      this.lengthOfSection7 = GribNumbers.int4(raf);
	      this.section7 = raf.read();
	    }

	    @Override
	    public int getNBits() {
	      return numberOfBits;
	    }

	    @Override
	    public String toString() {
	      final StringBuilder sb = new StringBuilder();
	      sb.append("Type50002");
	      sb.append("{\n\treferenceValue=").append(referenceValue);
	      sb.append(",\n\tbinaryScaleFactor=").append(binaryScaleFactor);
	      sb.append(",\n\tdecimalScaleFactor=").append(decimalScaleFactor);
	      sb.append(",\n\tnumberOfBits=").append(numberOfBits);
	      sb.append(",\n\twidthOfFirstOrderValues=").append(widthOfFirstOrderValues);
	      sb.append(",\n\tp1=").append(p1);
	      sb.append(",\n\tp2=").append(p2);
	      sb.append(",\n\twidthOfWidth=").append(widthOfWidth);
	      sb.append(",\n\twidthOfLength=").append(widthOfLength);
	      sb.append(",\n\tboustrophonic=").append(boustrophonic);
	      sb.append(",\n\torderOfSPD=").append(orderOfSPD);
	      sb.append(",\n\twidthOfSPD=").append(widthOfSPD);
	      sb.append(",\n\tspd=");
	      for (float i : spd) {
	    	  sb.append(i).append(",");
	      }
	      sb.deleteCharAt(sb.length()-1);
	      sb.append(",\n\tlengthOfSection6=").append(lengthOfSection6);
	      sb.append(",\n\tsection6=").append(section6);
	      sb.append(",\n\tbitMapIndicator=").append(bitMapIndicator);
	      sb.append(",\n\tlengthOfSection7=").append(lengthOfSection7);
	      sb.append(",\n\tsection7=").append(section7);
	      sb.append("\n}");
	      return sb.toString();
	    }

	    @Override
	    public boolean equals(Object o) {
	      if (this == o) return true;
	      if (o == null || getClass() != o.getClass()) return false;

	      Type50002 type50002 = (Type50002) o;

	      if (Float.compare(type50002.referenceValue, referenceValue) != 0) return false;
	      if (binaryScaleFactor != type50002.binaryScaleFactor) return false;
	      if (decimalScaleFactor != type50002.decimalScaleFactor) return false;
	      if (numberOfBits != type50002.numberOfBits) return false;
	      if (widthOfFirstOrderValues != type50002.widthOfFirstOrderValues) return false;
	      if (p1 != type50002.p1) return false;
	      if (p2 != type50002.p2) return false;
	      if (widthOfWidth != type50002.widthOfWidth) return false;
	      if (widthOfLength != type50002.widthOfLength) return false;
	      if (boustrophonic != type50002.boustrophonic) return false;
	      if (orderOfSPD != type50002.orderOfSPD) return false;
	      if (widthOfSPD != type50002.widthOfSPD) return false;
	      for (int i = 0; i < spd.length; i++) {
	    	  if (spd[i] != type50002.spd[i]) return false;
	      }
	      if (lengthOfSection6 != type50002.lengthOfSection6) return false;
	      if (section6 != type50002.section6) return false;
	      if (bitMapIndicator != type50002.bitMapIndicator) return false;
	      if (lengthOfSection7 != type50002.lengthOfSection7) return false;
	      if (section7 != type50002.section7) return false;

	      return true;
	    }

	    @Override
	    public int hashCode() {
	      int result = (referenceValue != +0.0f ? Float.floatToIntBits(referenceValue) : 0);
	      result = 31 * result + binaryScaleFactor;
	      result = 31 * result + decimalScaleFactor;
	      result = 31 * result + numberOfBits;
	      result = 31 * result + widthOfFirstOrderValues;
	      result = 31 * result + p1;
	      result = 31 * result + p2;
	      result = 31 * result + widthOfWidth;
	      result = 31 * result + widthOfLength;
	      result = 31 * result + boustrophonic;
	      result = 31 * result + orderOfSPD;
	      result = 31 * result + widthOfSPD;
	      for (int i = 0; i < spd.length; i++) {
	    	  result = 31 * result + spd[i];
	      }
	      result = 31 * result + lengthOfSection6;
	      result = 31 * result + section6;
	      result = 31 * result + section6;
	      result = 31 * result + lengthOfSection7;
	      result = 31 * result + section7;
      return result;
    }
  }

}
