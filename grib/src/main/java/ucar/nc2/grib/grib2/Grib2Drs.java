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
  }

  public static class Type3 extends Type2 {
    public int orderSpatial, descriptorSpatial;

    Type3(RandomAccessFile raf) throws IOException {
      super(raf);
      this.orderSpatial = raf.read();
      this.descriptorSpatial = raf.read();

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
  }

}
