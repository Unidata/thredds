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

import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Template-specific fields for Grib2SectionDataRepresentation
 * @author caron
 * @since 4/1/11
 */
public class Grib2Drs {

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

      default:
        throw new UnsupportedOperationException("Unsupported DRS type = " + template);
    }
  }

  public int getNBits() {
    return -1;
  }

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

}
