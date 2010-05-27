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
/**
 * User: rkambic
 * Date: Jul 13, 2009
 * Time: 12:54:02 PM
 */

package ucar.grib.grib2;

import ucar.grib.GribNumbers;
import ucar.grib.GribGDSVariablesIF;

import java.io.IOException;

/**
 * Representing the grid definition section (GDS) of a GRIB record as variables are
 * extracted from a byte[].
 * This is section 3 of a Grib record that contains information about the grid
 */
public class Grib2GDSVariables implements GribGDSVariablesIF {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2GDSVariables.class);

  /**
   * GDS as byte array.
   */
  private final byte[] input;

  /**
   * Grid Definition Template Number.
   */
  private final int gdtn;

  /**
   * GDS key for this .
   */
  private final int gdsKey;

  /**
   * Constructs a Grib2GDSVariables  object from a byte[].
   *
   * @param input PDS
   * @throws java.io.IOException if raf contains no valid GRIB file
   */
  public Grib2GDSVariables(byte[] input) throws IOException {

    this.input = input;

    // octet 13-14
    gdtn = GribNumbers.int2(getInt(12), getInt(13));
    
    double checkSum = gdtn;
    // TODO: use the unscaled getLa1() and getLo1 to create new type gdsKey
    //checkSum = 7 * checkSum + getLa1();
    checkSum = 7 * checkSum + get80La1();
    //checkSum = 7 * checkSum + getLo1();
    checkSum = 7 * checkSum + get80Lo1();
    gdsKey = Double.toString(checkSum).hashCode();

    //gdsKey = java.util.Arrays.hashCode( input );
  }

  /**
   * GDS as byte[]
   *
   * @return input as byte[]
   */
  public byte[] getGDSBytes() {
    return input;
  }

  // getters  Covers ProductDefinitions 0-14 first

  // octets 1-4 (Length of PDS)

  public final int getLength() {
    return GribNumbers.int4(getInt(0), getInt(1), getInt(2), getInt(3));
  }

  /**
   * octet 5
   * Number of this section, should be 3.
   *
   * @return section as int
   */
  public final int getSection() {
    return getInt(4);
  }

  /**
   * octet 6
   * source of grid definition.
   *
   * @return source
   */
  public final int getSource() {
    return getInt(5);
  }

  /**
   * octets 7-10
   * number of data points .
   *
   * @return numberPoints
   */
  public final int getNumberPoints() {
    return GribNumbers.int4(getInt(6), getInt(7), getInt(8), getInt(9));
  }

  /**
   * octet 11
   * olon > 0 is a quasi regular grid.
   *
   * @return olon
   */
  public final int getOlon() {
    return getInt(10);
  }

  /**
   * octet 12
   * are extreme points in the quasi regular grid.
   *
   * @return iolon
   */
  public final int getIolon() {
    return getInt(11);
  }

  // octets 13-14
  /**
   * Get type of grid.
   *
   * @return type of grid
   */
  public final int getGdtn() {
    //return GribNumbers.int2( getInt(12), getInt(13) );
    return gdtn;
  }

  /**
   * octet 15.
   *
   * @return shape as a int
   */
  public final int getShape() {
    switch (gdtn) {
      // octet 15
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return getInt(14);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 16.
   *
   * @return scaleFactorRadius as a int
   */
  public final int getScaleFactorRadius() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return getInt(15);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 17-20.
   *
   * @return scaleValueRadius as a int
   */
  public final int getScaleValueRadius() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return GribNumbers.int4(getInt(16), getInt(17), getInt(18), getInt(19));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 21.
   *
   * @return scaleFactorMajor as a int
   */
  public final int getScaleFactorMajor() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return getInt(20);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 22-25.
   *
   * @return scaleValueMajor as a int
   */
  public final int getScaleValueMajor() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return GribNumbers.int4(getInt(21), getInt(22), getInt(23), getInt(24));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 26.
   *
   * @return scaleFactorMinor as a int
   */
  public final int getScaleFactorMinor() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return getInt(25);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 27-30.
   *
   * @return scaleValueMinor as a int
   */
  public final int getScaleValueMinor() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        return GribNumbers.int4(getInt(26), getInt(27), getInt(28), getInt(29));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 31-34.
   *
   * @return Number of points on x-axis or parallel as a int
   */
  public final int getNx() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 32768: {
        int nx = GribNumbers.int4(getInt(30), getInt(31), getInt(32), getInt(33));
        if (nx == -1 || nx == GribNumbers.UNDEFINED) {
            return calculateNx();
        } else {
          return nx;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 35-38.
   *
   * @return Number of points on y-axis or meridian as a int
   */
  public final int getNy() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 32768: {
        return GribNumbers.int4(getInt(34), getInt(35), getInt(36), getInt(37));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 39-42.
   *
   * @return Lap as float
   */
  public final float getLap() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 43-46.
   *
   * @return Lop as float
   */
  public final float getLop() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 39-42.
   *
   * @return BasicAngle as a int
   */
  public final int getBasicAngle() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 204:
      case 32768: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41));
      }
      // octets 35-38
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(34), getInt(35), getInt(36), getInt(37));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 43-46.
   *
   * @return SubDivisions as a int
   */
  public final int getSubDivisions() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 204:
      case 32768: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45));
      }
      // octets 39-42
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 47-50.
   *
   * @return La1 as float
   */
  public final float getLa1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49)) / getRatio();
      }
      // octets 39-42
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 23-26
      case 120: {
        return GribNumbers.int4(getInt(22), getInt(23), getInt(24), getInt(25)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 43-46
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 47-50.
   * Used in creating GdsKey from ints
   * @return La1 as int
   */
  public final int getUnscaledLa1() {


    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49));
      }
      // octets 39-42
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41));
      }
      // octets 23-26
      case 120: {
        return GribNumbers.int4(getInt(22), getInt(23), getInt(24), getInt(25));
      }
      // octets 43-46
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 47-50.
   *
   * @return La1 as float
   */
  public final float get80La1() {

    float ratio = (getBasicAngle() == 0 || getBasicAngle() == GribNumbers.UNDEFINED) ?
        GribGDSVariablesIF.tenToNegSix : getBasicAngle() / getSubDivisions();

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49)) * ratio;
      }
      // octets 39-42
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(38), getInt(39), getInt(40), getInt(41)) * GribGDSVariablesIF.tenToNegSix;
      }
      // octets 23-26
      case 120: {
        return GribNumbers.int4(getInt(22), getInt(23), getInt(24), getInt(25)) * GribGDSVariablesIF.tenToNegSix;
      }
      // octets 43-46
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45)) * ratio;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 51-54.
   *
   * @return Lo1 as float
   */
  public final float getLo1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(50), getInt(51), getInt(52), getInt(53)) / getRatio();
      }
      // octets 43-46
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 27-30
      case 120: {
        return GribNumbers.int4(getInt(26), getInt(27), getInt(28), getInt(29)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 47-50
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 51-54.
   * used to create a gdsKey from ints
   * @return Lo1 int
   */
  public final int getUnscaledLo1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(50), getInt(51), getInt(52), getInt(53));
      }
      // octets 43-46
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45));
      }
      // octets 27-30
      case 120: {
        return GribNumbers.int4(getInt(26), getInt(27), getInt(28), getInt(29));
      }
      // octets 47-50
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }
  
  /**
   * octet 51-54.
   *
   * @return Lo1 as float
   */
  public final float get80Lo1() {

    float ratio = (getBasicAngle() == 0 || getBasicAngle() == GribNumbers.UNDEFINED) ?
        GribGDSVariablesIF.tenToNegSix : getBasicAngle() / getSubDivisions();

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(50), getInt(51), getInt(52), getInt(53)) * ratio;
      }
      // octets 43-46
      case 10:
      case 20:
      case 30:
      case 31:
      case 110: {
        return GribNumbers.int4(getInt(42), getInt(43), getInt(44), getInt(45)) * GribGDSVariablesIF.tenToNegSix;
      }
      // octets 27-30
      case 120: {
        return GribNumbers.int4(getInt(26), getInt(27), getInt(28), getInt(29)) * GribGDSVariablesIF.tenToNegSix;
      }
      // octets 47-50
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(46), getInt(47), getInt(48), getInt(49)) * ratio;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 55.
   *
   * @return Resolution as int
   */
  public final int getResolution() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 204:
      case 32768: {
        return getInt(54);
      }
      // octets 47
      case 10:
      case 20:
      case 30:
      case 31:
      case 90:
      case 110: {
        return getInt(46);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 48-51.
   *
   * @return LaD as float
   */
  public final float getLaD() {

    switch (gdtn) {
      case 10:
      case 20:
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(47), getInt(48), getInt(49), getInt(50)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 52-55.
   *
   * @return LoV as float
   */
  public final float getLoV() {

    switch (gdtn) {
      case 20:
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(51), getInt(52), getInt(53), getInt(54)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 56-59.
   *
   * @return La2 as float
   */
  public final float getLa2() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(55), getInt(56), getInt(57), getInt(58)) / getRatio();
      }
      // octets 52-55
      case 10: {
        return GribNumbers.int4(getInt(51), getInt(52), getInt(53), getInt(54)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 52-55
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(51), getInt(52), getInt(53), getInt(54)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 60-63.
   *
   * @return Lo2 as float
   */
  public final float getLo2() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(59), getInt(60), getInt(61), getInt(62)) / getRatio();
      }
      // octets 56-59
      case 10: {
        return GribNumbers.int4(getInt(55), getInt(56), getInt(57), getInt(58)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 56-59
      case 1000:
      case 1100: {
        return GribNumbers.int4(getInt(55), getInt(56), getInt(57), getInt(58)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 64-67.
   *
   * @return Dx as float
   */
  public final float getDx() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 32768: {
        return GribNumbers.int4(getInt(63), getInt(64), getInt(65), getInt(66)) / getRatio();
      }
      // octets 65-68
      case 10: {
        return GribNumbers.int4(getInt(64), getInt(65), getInt(66), getInt(67)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 56-59
      case 20:
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(55), getInt(56), getInt(57), getInt(58)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 48-51
      case 90: {
        return GribNumbers.int4(getInt(47), getInt(48), getInt(49), getInt(50));
      }
      // octets 48-51
      case 110: {
        return GribNumbers.int4(getInt(47), getInt(48), getInt(49), getInt(50)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 31-34
      case 120: {
        return GribNumbers.int4(getInt(47), getInt(48), getInt(49), getInt(50));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 68-71.
   *
   * @return Dy as float
   */
  public final float getDy() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 32768: {
        return GribNumbers.int4(getInt(67), getInt(68), getInt(69), getInt(70)) / getRatio();
      }
      // octets 69-72
      case 10: {
        return GribNumbers.int4(getInt(68), getInt(69), getInt(70), getInt(71)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 60-63
      case 20:
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(59), getInt(60), getInt(61), getInt(62)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 52-55
      case 90: {
        return GribNumbers.int4(getInt(51), getInt(52), getInt(53), getInt(54));
      }
      // octets 52-55
      case 110: {
        return GribNumbers.int4(getInt(51), getInt(52), getInt(53), getInt(54)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 56-59.
   *
   * @return Xp as float
   */
  public final float getXp() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(55), getInt(56), getInt(57), getInt(58)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 60-63.
   *
   * @return Yp as float
   */
  public final float getYp() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(59), getInt(60), getInt(61), getInt(62)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 68-71.
   *
   * @return Np as a int
   */
  public final int getNp() {
    switch (gdtn) {
      case 40:
      case 41:
      case 42:
      case 43: {
        return GribNumbers.int4(getInt(67), getInt(68), getInt(69), getInt(70));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 61-62.
   *
   * @return N as a int
   */
  public final int getN() {
    switch (gdtn) {
      // octets 61-62
      case 1000: {
        return GribNumbers.int2(getInt(60), getInt(61));
      }
      // octets 61-64
      case 1100: {
        return GribNumbers.int4(getInt(60), getInt(61), getInt(62), getInt(63));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 64.
   *
   * @return ProjectionFlag as int
   */
  public final int getProjectionFlag() {

    switch (gdtn) {
      case 20:
      case 30:
      case 31: {
        return getInt(63);
      }
      // octet 56
      case 110: {
        return getInt(55);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 72.
   *
   * @return ScanMode as int
   */
  public final int getScanMode() {

    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
      case 204:
      case 32768: {
        return getInt(71);
      }
      // octets 60
      case 10: {
        return getInt(59);
      }
      // octet 65
      case 20:
      case 30:
      case 31: {
        return getInt(64);
      }
      // octet 64
      case 90: {
        return getInt(63);
      }
      // octet 34
      case 100: {
        return getInt(33);
      }
      // octet 57
      case 110: {
        return getInt(56);
      }
      // octet 39
      case 120: {
        return getInt(38);
      }
      // octets 51
      case 1000:
      case 1100: {
        return getInt(50);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 66-69.
   *
   * @return Latin1 as float
   */
  public final float getLatin1() {

    switch (gdtn) {
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(65), getInt(66), getInt(67), getInt(68)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 70-73.
   *
   * @return Latin2 as float
   */
  public final float getLatin2() {

    switch (gdtn) {
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(69), getInt(70), getInt(71), getInt(72)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 61-64.
   *
   * @return Angle as float
   */
  public final int getAngle() {

    switch (gdtn) {
      case 10: {
        return GribNumbers.int4(getInt(60), getInt(61), getInt(62), getInt(63));
      }
      // octets 65-68
      case 90: {
        return GribNumbers.int4(getInt(64), getInt(65), getInt(66), getInt(67));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 69-72.  
   *
   * @return Nr as float
   */
  public final float getNr() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(68), getInt(69), getInt(70), getInt(71));
        //return GribNumbers.int4(getInt(68), getInt(69), getInt(70), getInt(71)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 73-76.
   *
   * @return Xo as float
   */
  public final float getXo() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(72), getInt(73), getInt(74), getInt(75));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 77-80.
   *
   * @return Yo as float
   */
  public final float getYo() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int4(getInt(76), getInt(77), getInt(78), getInt(79));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 73-76.
   *
   * @return SpLat as float
   */
  public final float getSpLat() {

    switch (gdtn) {
      case 1:
      case 2:
      case 3:
      case 41:
      case 43: {
        return GribNumbers.int4(getInt(72), getInt(73), getInt(74), getInt(75)) / getRatio();
      }
      // octets 74-77
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(73), getInt(74), getInt(75), getInt(76)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 77-80.
   *
   * @return SpLon as float
   */
  public final float getSpLon() {

    switch (gdtn) {
      case 1:
      case 2:
      case 3:
      case 41:
      case 43: {
        return GribNumbers.int4(getInt(76), getInt(77), getInt(78), getInt(79)) / getRatio();
      }
      // octets 78-81
      case 30:
      case 31: {
        return GribNumbers.int4(getInt(77), getInt(78), getInt(79), getInt(80)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 81-84.
   *
   * @return RotationAngle as float
   */
  public final float getRotationAngle() {

    switch (gdtn) {
      case 1:
      case 2:
      case 3:
      case 41:
      case 43: {
        return GribNumbers.float4(getInt(80), getInt(81), getInt(82), getInt(83));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 85-88.
   *
   * @return PoleLat as float
   */
  public final float getPoleLat() {

    switch (gdtn) {
      case 3:
      case 43: {
        return GribNumbers.int4(getInt(84), getInt(85), getInt(86), getInt(87)) / getRatio();
      }
      // octets 73-76
      case 42: {
        return GribNumbers.int4(getInt(72), getInt(73), getInt(74), getInt(75)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 89-92.
   *
   * @return PoleLon as float
   */
  public final float getPoleLon() {

    switch (gdtn) {
      case 3:
      case 43: {
        return GribNumbers.int4(getInt(88), getInt(89), getInt(90), getInt(91)) / getRatio();
      }
      // octets 77-80
      case 42: {
        return GribNumbers.int4(getInt(76), getInt(77), getInt(78), getInt(79)) / getRatio();
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 93-96.
   *
   * @return StretchingFactor as float
   */
  public final float getStretchingFactor() {

    switch (gdtn) {
      case 3:
      case 43: {
        return GribNumbers.int4(getInt(92), getInt(93), getInt(94), getInt(95)) / GribGDSVariablesIF.tenToSix;
      }
      // octets 81-84
      case 42: {
        return GribNumbers.int4(getInt(80), getInt(81), getInt(82), getInt(83)) / GribGDSVariablesIF.tenToSix;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }


  /**
   * MajorAxis
   *
   * @return MajorAxis as float
   */
  public final float getMajorAxis() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        if (getShape() == 2) {
          //majorAxis = (float) 6378160.0;
          return (float) 6378160.0;
        } else if (getShape() == 3) {
          //majorAxis = scaledvaluemajor;
          float majorAxis = getScaleValueMajor();
          majorAxis /= Math.pow(10, getScaleFactorMajor());
          return majorAxis * 1000;
        } else if (getShape() == 4) {
          //majorAxis = (float) 6378137.0;
          return (float) 6378137.0;
        } else if (getShape() == 5) {
          //majorAxis = (float) 6,378,137;
          return (float) 6378137.0;
        } else if (getShape() == 7) {
          //majorAxis = scaledvaluemajor in meters;
          float majorAxis = getScaleValueMajor();
          majorAxis /= Math.pow(10, getScaleFactorMajor());
          return majorAxis;
        } else {
          return GribNumbers.UNDEFINED;
        }
      }

      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * MinorAxis
   *
   * @return MajorAxis as float
   */
  public final float getMinorAxis() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        if (getShape() == 2) {
          //minorAxis = (float) 6356775.0;
          return (float) 6356775.0;
        } else if (getShape() == 3) {
          float minorAxis = getScaleValueMinor();
          minorAxis /= Math.pow(10, getScaleFactorMinor());
          return minorAxis * 1000;
        } else if (getShape() == 4) {
          //minorAxis = (float) 6356752.314;
          return (float) 6356752.314;
        } else if (getShape() == 5) {
          //minorAxis = (float) 6356752.314;
          return (float) 6356752.314;
        } else if (getShape() == 7) {
          float minorAxis = getScaleValueMinor();
          minorAxis /= Math.pow(10, getScaleFactorMinor());
          return minorAxis;
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * EarthRadius
   *
   * @return EarthRadius as float
   */
  public final float getEarthRadius() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 10:
      case 20:
      case 30:
      case 31:
      case 40:
      case 41:
      case 42:
      case 43:
      case 90:
      case 110:
      case 204:
      case 1000:
      case 1100:
      case 32768: {
        if (getShape() == 0) {
          //earthRadius = 6367470;
          return (float) 6367470;
        } else if (getShape() == 1) {
          float earthRadius = getScaleValueRadius();
          if (getScaleFactorRadius() != 0) {
            earthRadius /= Math.pow(10, getScaleFactorRadius());
          }
          return earthRadius;
        } else if (getShape() == 6) {
          //earthRadius = 6371229;
          return 6371229;
        } else if (getShape() == 8) {
          // earthRadius = 6,371,200
          return 6371200;
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * GridUnits
   *
   * @return GridUnits as String
   */
  public final String getGridUnits() {
    switch (gdtn) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 40:
      case 41:
      case 42:
      case 43:
        return "degrees";

      case 10:
      case 20:
      case 30:
      case 31:
        return "m";

      default:
        return "";
    }
  }

  /**
   * Gets the Maximum number of points in a parallel for Quasi/Thin grids
   * @return parallels as int[]
   */
  public final int calculateNx() {

    int numPts;
    if ((getScanMode() & 32) == 0) {
      numPts = getNy();
    } else {
      numPts = getNx();
    }
    log.debug( "GDS  numPts = " + numPts);

    int offset;
    switch (gdtn) {
      case 0:
      case 10:
      case 40:
        offset = 72;
        break;
      case 1:
      case 2:
      case 41:
      case 42:
        offset = 84;
        break;
      case 3:
      case 43:
        offset = 96;
        break;

      default:
        offset = -1;
    }

    int maxPts = 0;
    int[] parallels = new int[numPts];
    int olon = getOlon();
    if (olon == 1) {
      for (int i = 0; i < numPts; i++) {
        parallels[i] = getInt(offset++);
        if (maxPts < parallels[i])
            maxPts = parallels[i];
        log.debug( "parallel =" + i +" number pts ="+ parallels[i] );
      }
    } else if (olon == 2) {
      for (int i = 0; i < numPts; i++) {
        parallels[i] = GribNumbers.int2(getInt(offset++), getInt(offset++));
        if (maxPts < parallels[i])
            maxPts = parallels[i];
        log.debug( "parallel =" + i +" number pts ="+ parallels[i] );
      }
    }
    return maxPts;
  }

  /**
   * Gets the number of points in each parallel for Quasi/Thin grids
   * @return parallels as int[]
   */
  public final int[] getParallels() {

    int numPts;
    if ((getScanMode() & 32) == 0) {
      numPts = getNy();
    } else {
      numPts = getNx();
    }
    log.debug( "GDS  numPts = " + numPts);

    int offset;
    switch (gdtn) {
      case 0:
      case 10:
      case 40:
        offset = 72;
        break;
      case 1:
      case 2:
      case 41:
      case 42:
        offset = 84;
        break;
      case 3:
      case 43:
        offset = 96;
        break;

      default:
        offset = -1;
    }
    int[] parallels = new int[numPts];
    int olon = getOlon();
    if (olon == 1) {
      for (int i = 0; i < numPts; i++) {
        parallels[i] = getInt(offset++);
        log.debug( "parallel =" + i +" number pts ="+ parallels[i] );
      }
    } else if (olon == 2) {
      for (int i = 0; i < numPts; i++) {
        parallels[i] = GribNumbers.int2(getInt(offset++), getInt(offset++));
        log.debug( "parallel =" + i +" number pts ="+ parallels[i] );
      }
    }
    return parallels;
  }

  /**
   * Get the ratio   tenToSix or basic angle / sub divisions
   * returns ratio
   */
  private float getRatio() {
    //float ratio = (getBasicAngle() == 0 || getBasicAngle() == GribNumbers.UNDEFINED) ?
    //    GribGDSVariablesIF.tenToNegSix : getBasicAngle() / getSubDivisions();
    float ba = getBasicAngle();

    if( ba == 0 || ba == GribNumbers.UNDEFINED )
        return GribGDSVariablesIF.tenToSix;

    return  ba / getSubDivisions();
  }

  /**
   * @return gdsKey as int
   */
  public final int getGdsKey() {
    return gdsKey;
  }

  /**
   * gdsKey for version 8.0
   * @return gdsKey as int
   */
  public final int get80TypeGdsKey() {
    double checkSum = gdtn;
    checkSum = 7 * checkSum + get80La1();
    checkSum = 7 * checkSum + get80Lo1();
    return Double.toString(checkSum).hashCode();
  }

  /**
   * Converts byte to int.
   *
   * @param index in the byte[] to convert
   * @return int  byte as int
   */
  public final int getInt(int index) {
    return input[index] & 0xff;
  }
}
