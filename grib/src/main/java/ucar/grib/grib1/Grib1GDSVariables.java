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
 * Date: Jul 16, 2009
 * Time: 3:59:47 PM
 */

package ucar.grib.grib1;

import ucar.grib.GribNumbers;
import ucar.grib.GribGDSVariablesIF;

import java.io.IOException;

public class Grib1GDSVariables implements GribGDSVariablesIF {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1GDSVariables.class);

  /**
   * GDS as byte array.
   */
  private final byte[] input;

  /**
   * Length in bytes of this GDS.
   */
  private final int length;

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
  public Grib1GDSVariables(byte[] input) throws IOException {

    this.input = input;

    this.length = GribNumbers.int3(getInt(0), getInt(1), getInt(2));

    // octet 6
    gdtn = getInt(5);

    double checkSum = gdtn;
    // TODO: create an unscaled getLa1() and getLo1 and use it to create gdsKey
    //checkSum = 7 * checkSum + getLa1();
    checkSum = 7 * checkSum + get80La1();
    //checkSum = 7 * checkSum + getLo1();
    checkSum = 7 * checkSum + get80Lo1();
    gdsKey = Double.toString(checkSum).hashCode();
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

  // octets 1-3 (Length of GDS)

  public final int getLength() {
    return length;
  }

  /**
   * Number of this section, should be 2.
   *
   * @return section as int
   */
  public final int getSection() {
    return 2;
  }

  /**
   * octet 4
   * NV.
   *
   * @return NV as int
   */
  public final int getNV() {
    return getInt(3);
  }

  /**
   * octet 5
   * NV.
   *
   * @return PVorPL as int
   */
  public final int getPVorPL() {
    return getInt(4);
  }

  /**
   * isThin.
   *
   * @return isThin as boolean
   */
  public final boolean isThin() {
    return ( getPVorPL() != 255 && ( getNV() == 0 || getNV() == 255 ));
  }

  /**
   * hasVerticalLevels.
   *
   * @return hasVerticalLevels as boolean
   */
  public final boolean hasVerticalPressureLevels() {
    return ( getPVorPL() != 255 && ! ( getNV() == 0 || getNV() == 255 ));
  }

  // octet 6
  /**
   * Get type of grid.
   *
   * @return type of grid
   */
  public final int getGdtn() {
    return gdtn;
  }

  /**
   * octet 7-8.
   *
   * @return Number of points on x-axis or parallel as a int
   */
  public final int getNx() {
    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 90:
      case 201:
      case 202:
      case 203:
      case 204:
      case 205: {
        int nx = GribNumbers.int2(getInt(6), getInt(7));
        if (nx == -1 || nx == GribNumbers.UNDEFINED) {
          if (getPVorPL() != 255 && ( (getNV() == 0 || getNV() == 255) )) {
            return calculateNx();
          } else {
            return 1;
          }
        } else {
          return nx;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 9-10.
   *
   * @return Number of points on y-axis or meridian as a int
   */
  public final int getNy() {
    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 90:
      case 202:
      case 204:
      case 205: {
        int ny = GribNumbers.int2(getInt(8), getInt(9));
        if (ny == -1 || ny == GribNumbers.UNDEFINED) {
          return 1; // TODO: check usage no use case available
        } else {
          return ny;
        }
      }
      case 201:
      case 203: {
        return 1; // TODO: check usage no use case available
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 11-13.
   *
   * @return Lap as float
   */
  public final float getLap() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int3(getInt(10), getInt(11), getInt(12));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 14-16.
   *
   * @return Lop as float
   */
  public final float getLop() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int3(getInt(13), getInt(14), getInt(15));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octets 11-13
   * GridCenter.
   *
   * @return GridCenter
   */
  public final int getGridCenter() {

    switch (gdtn) {
      case 204:
        return GribNumbers.int3(getInt(10), getInt(11), getInt(12));
      default:
        return GribNumbers.UNDEFINED;
    }
  }


  /**
   * octet 11-13.
   *
   * @return La1 as float
   */
  public final float getLa1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(10), getInt(11), getInt(12)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 11-13.
   * used in creating a gdsKey from ints
   * @return La1 as int
   */
  public final int getUnscaledLa1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(10), getInt(11), getInt(12));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 11-13.
   *
   * @return La1 as float
   */
  public final float get80La1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(10), getInt(11), getInt(12)) * GribGDSVariablesIF.tenToNegThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 14-16.
   *
   * @return Lo1 as float
   */
  public final float getLo1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(13), getInt(14), getInt(15)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 14-16.
   * used in creating gdsKey from ints
   * @return Lo1 as int
   */
  public final int getUnscaledLo1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(13), getInt(14), getInt(15));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 14-16.
   *
   * @return Lo1 as float
   */
  public final float get80Lo1() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(13), getInt(14), getInt(15)) * GribGDSVariablesIF.tenToNegThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 17.
   *
   * @return Resolution as int
   */
  public final int getResolution() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 90:
      case 201:
      case 202:
      case 203:
      case 204:
      case 205: {
        return getInt(16);
      }
      default:
        return GribNumbers.UNDEFINED;
    }

  }

  /**
   * octet 18-20.
   *
   * @return LoV as float
   */
  public final float getLoV() {

    switch (gdtn) {
      case 3:
      case 5:
      case 13: {
        return GribNumbers.int3(getInt(17), getInt(18), getInt(19)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 18-20.
   *
   * @return La2 as float
   */
  public final float getLa2() {

    switch (gdtn) {
      case 0:
      case 1:
      case 4:
      case 6:
      case 10:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(17), getInt(18), getInt(19)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 21-23.
   *
   * @return Lo2 as float
   */
  public final float getLo2() {

    switch (gdtn) {
      case 0:
      case 1:
      case 4:
      case 6:
      case 10:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        return GribNumbers.int3(getInt(20), getInt(21), getInt(22)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 24-25.
   *
   * @return Dx as float
   */
  public final float getDx() {
    switch (gdtn) {
      case 0:
      case 4:
      case 10:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        int dx = GribNumbers.int2(getInt(23), getInt(24));
        if (dx == -1 || dx == GribNumbers.UNDEFINED) {
          return calculateDx();
        } else {
          return dx / GribGDSVariablesIF.tenToThree;
        }
      }
      // 18-20
      case 90: {
        int dx = GribNumbers.int3(getInt(17), getInt(18), getInt(19));
        // all 1's == -8388607
        if (dx == -8388607 || dx == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dx;
        }
      }
      // 21-23
      case 3:
      case 5:
      case 13: {
        int dx = GribNumbers.int3(getInt(20), getInt(21), getInt(22));
        if (dx == -8388607 || dx == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dx;
        }
      }
      // 29-31
      case 1:
      case 6: {
        int dx = GribNumbers.int3(getInt(28), getInt(29), getInt(30));
        if (dx == -8388607 || dx == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dx;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 26-27.
   *
   * @return Dy as float
   */
  public final float getDy() {

    switch (gdtn) {
      case 0:
      case 10:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205: {
        int dy = GribNumbers.int2(getInt(25), getInt(26));
        if (dy == -1 || dy == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dy / GribGDSVariablesIF.tenToThree;
        }
      }
      case 4:
      case 14: {
        int dy = GribNumbers.int2(getInt(25), getInt(26));
        if (dy == -1 || dy == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dy;
        }
      }
      // 21-23
      case 90: {
        int dy = GribNumbers.int3(getInt(20), getInt(21), getInt(22));
        if (dy == -8388607 || dy == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dy;
        }
      }
      // 24-26
      case 3:
      case 5:
      case 13: {
        int dy = GribNumbers.int3(getInt(23), getInt(24), getInt(25));
        if (dy == -8388607 || dy == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dy;
        }
      }
      // 32-34
      case 1:
      case 6: {
        int dy = GribNumbers.int3(getInt(31), getInt(32), getInt(33));
        if (dy == -8388607 || dy == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return dy;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 24-25.
   *
   * @return Xp as float
   */
  public final float getXp() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int2(getInt(23), getInt(24));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 26-27.
   *
   * @return Yp as float
   */
  public final float getYp() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int2(getInt(25), getInt(26));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 27.
   *
   * @return ProjectionFlag as int
   */
  public final int getProjectionFlag() {

    switch (gdtn) {
      case 3:
      case 5:  
      case 13: {
        return getInt(26);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 28.
   *
   * @return ScanMode as int
   */
  public final int getScanMode() {

    switch (gdtn) {
      case 0:
      case 1:
      case 3:
      case 4:
      case 5:
      case 6:
      case 10:
      case 13:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 90:
      case 201:
      case 202:
      case 203:
      case 204:
      case 205: {
        return getInt(27) & 224; // mask off bits 4-8
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 29-31.
   *
   * @return Angle as float
   */
  public final int getAngle() {

    switch (gdtn) {
      case 90: {
        return GribNumbers.int3(getInt(28), getInt(29), getInt(30));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 32-34
   *
   * @return Nr as float
   */
  public final float getNr() {
    switch (gdtn) {
      case 90: {
        return GribNumbers.int3(getInt(31), getInt(32), getInt(33));
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 24-26.
   *
   * @return Latin1 as float
   */
  public final float getLatin1() {

    switch (gdtn) {
      case 1:
      case 6: {
        return GribNumbers.int3(getInt(23), getInt(24), getInt(25)) / GribGDSVariablesIF.tenToThree;
      }
      // octet 29-31
      case 3:
      case 13: {
        return GribNumbers.int3(getInt(28), getInt(29), getInt(30)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 32-34.
   *
   * @return Latin2 as float
   */
  public final float getLatin2() {

    switch (gdtn) {
      case 3:
      case 13: {
        return GribNumbers.int3(getInt(31), getInt(32), getInt(33)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 35-37.
   *
   * @return SpLat as float
   */
  public final float getSpLat() {

    switch (gdtn) {
      case 3:
      case 13: {
        return GribNumbers.int3(getInt(34), getInt(35), getInt(36)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 33-35 (lat of southern pole)
      case 10: {
        return GribNumbers.int3(getInt(32), getInt(33), getInt(34)) / GribGDSVariablesIF.tenToThree;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * octet 38-40.
   *
   * @return SpLon as float
   */
  public final float getSpLon() {

    switch (gdtn) {
      case 3:
      case 13: {
        return GribNumbers.int3(getInt(37), getInt(38), getInt(39)) / GribGDSVariablesIF.tenToThree;
      }
      // octets 36-38 (lon of southern pole)
      case 10: {
        return GribNumbers.int3(getInt(35), getInt(36), getInt(37)) / GribGDSVariablesIF.tenToThree;
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
      case 4:
      case 10:
      case 14:
      case 20:
      case 24:
      case 30:
      case 34:
      case 201:
      case 202:
      case 203:
      case 205:
        return "degrees";

      case 1:
      case 3:
      case 5:
      case 6:
      case 13:
        return "m";

      default:
        return "";
    }
  }

  /**
   *
   * @return gdsKey as int
   */
  public final int getGdsKey() {
    return gdsKey;
  }

  /**
   *
   * @return gdsKey as int
   */
  public final int get80TypeGdsKey() {
    double checkSum = gdtn;
    checkSum = 7 * checkSum + get80La1();
    checkSum = 7 * checkSum + get80Lo1();
    return Double.toString(checkSum).hashCode();

  }

  /**
   * MajorAxis static for Grib1
   *
   * @return MajorAxis as float
   */
  public final float getMajorAxis() {

    return (float) 6378.160;
  }

  /**
   * MinorAxis static for Grib1
   *
   * @return MajorAxis as float
   */
  public final float getMinorAxis() {

    return (float) 6356.775;
  }

  /**
   * EarthRadius static for Grib1
   *
   * @return EarthRadius as float
   */
  public final float getEarthRadius() {

     return (float) 6367.47;
  }

  // following are stubs
  /**
   * olon > 0 is a quasi regular grid.
   *
   * @return olon
   */
  public final int getOlon() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * are extreme points in the quasi regular grid.
   *
   * @return iolon
   */
  public final int getIolon() {
    return GribNumbers.UNDEFINED;
  }


  /**
   * @return shape as a int
   */
  public final int getShape() {

    int res = getResolution() >> 6;
    if ((res == 1) || (res == 3)) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * source of grid definition.
   *
   * @return source
   */
  public final int getSource() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * number of data points .
   *
   * @return numberPoints
   */
  public final int getNumberPoints() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * @return BasicAngle as a int
   */
  public final int getBasicAngle() {

    return GribNumbers.UNDEFINED;
  }

  /**
   * @return SubDivisions as a int
   */
  public final int getSubDivisions() {

    return GribNumbers.UNDEFINED;
  }


  /**
   * @return LaD as float
   */
  public final float getLaD() {


    return GribNumbers.UNDEFINED;
  }

  /**
   * octets 26-27
   * @return Np as a int
   */
  public final int getNp() {
    switch (gdtn) {
      case 4:
      case 14:
      case 24:
      case 34: {
        int np = GribNumbers.int2(getInt(25), getInt(26));
        if (np == -1 || np == GribNumbers.UNDEFINED) {
          return GribNumbers.UNDEFINED;
        } else {
          return np;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * @return Xo as float
   */
  public final float getXo() {

    return GribNumbers.UNDEFINED;
  }

  /**
   * @return Yo as float
   */
  public final float getYo() {

    return GribNumbers.UNDEFINED;
  }


  /**
   * @return RotationAngle as float
   */
  public final float getRotationAngle() {
    switch (gdtn) {

      // octets 39-42
      case 10: {
        return GribNumbers.float4(getInt(38), getInt(39), getInt(40), getInt(41)) ;
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * @return PoleLat as float
   */
  public final float getPoleLat() {

    return GribNumbers.UNDEFINED;
  }

  /**
   * @return PoleLon as float
   */
  public final float getPoleLon() {


    return GribNumbers.UNDEFINED;
  }

  /**
   * @return StretchingFactor as float
   */
  public final float getStretchingFactor() {

    return GribNumbers.UNDEFINED;
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

    int[] parallels = new int[numPts];
    int offset = getPVorPL() -1; // adjust from octet offset to input[] offset
    for (int i = 0; i < numPts; i++) {
      parallels[i] = GribNumbers.int2(getInt(offset++), getInt(offset++));
      log.debug( "parallel =" + i +" number pts ="+ parallels[i] );
    }
    return parallels;
  }

  /**
   * Gets the maximum number of points in the parallels for Quasi/Thin grids
   * @return Nx as int
   */
  private int calculateNx() {

    int numPts;

    if ((getScanMode() & 32) == 0) {
      numPts = getNy();
    } else {
      numPts = getNx();
    }
    //System.out.println( "GDS  numPts = " + numPts);
    int maxPts = 0;

    int[] numPV = new int[numPts];
    int offset = getPVorPL() -1; // adjust from octet offset to input[] offset
    for (int i = 0; i < numPts; i++) {
      numPV[i] = GribNumbers.int2(getInt(offset++), getInt(offset++));
      if (maxPts < numPV[i]) {
        maxPts = numPV[i];
      }
      //System.out.println( "parallel =" + i +" number pts ="+ numPV[i] );
    }
    return maxPts;
  }

  /**
   * Gets the number of points in each parallel for Quasi/Thin grids
   * @return Nx as int
   */
  private float calculateDx() {
  float lon2 = getLo2(), lon1 = getLo1();
  if (lon2 < lon1)
      lon2 += 360;
    return (float) (lon2 - lon1) / (getNx() - 1);
  }

  // Original code from GDS1
  /**
   * Gets the number of points in each parallel for Quasi/Thin grids
   *
   * @param raf RandomAccessFile
   * @throws IOException _more_
   */
  /*
  private void getPL(RandomAccessFile raf) throws IOException {

    isThin = true;
    int numPts;

    if ((scan & 32) == 0) {
      numPts = ny;
    } else {
      numPts = nx;
    }
    //System.out.println( "GDS length ="+ length );
    //System.out.println( "GDS  numPts = " + numPts);
    //int count = 0;
    int maxPts = 0;
    numPV = new int[numPts];
    for (int i = 0; i < numPts; i++) {
      numPV[i] = raf.readUnsignedShort();
      //numPV[i] = raf.read();
      if (maxPts < numPV[i]) {
        maxPts = numPV[i];
      }
      //count += numPV[ i ];
      //System.out.println( "parallel =" + i +" number pts ="+ numPV[i] );
    }
    if ((scan & 32) == 0) {
      nx = maxPts;
    } else {
      ny = maxPts;
    }
    //double lodiff = gds.getLo2() - gds.getLo1();
    if (lon2 < lon1)
      lon2 += 360;
    //dx = (float) (lon2 - lon1) / (nx - 0);
    dx = (float) (lon2 - lon1) / (nx - 1);
    //System.out.println( "maxPts ="+ maxPts );
    //System.out.println( "total number pts ="+ count );
  }
  */

  /**
   * get Vertical pressure levels
   */
  public final double[] getVerticalPressureLevels( double levelValue ) {
    // Documentation for the conversion process is at:
    // http://www.ecmwf.int/research/ifsdocs/DYNAMICS/Chap2_Discretization4.html
    // read data values a and b, all a's are read first, then b's
    // http://www.ecmwf.int/products/data/technical/model_levels/model_def_91.html
    // testdata   ECMWF.2008.09.07.T00Z.uad_HGrbF00.A08334081908 has 91 levels
    //
    int offset = getPVorPL() -1; // adjust from octet offset to input[] offset
    int NV = getNV();
    float[] numPV = new float[NV];
    for (int i = 0; i < NV; i++) {
      numPV[i] = GribNumbers.float4(getInt(offset++), getInt(offset++), getInt(offset++), getInt(offset++)) ;
      //System.out.println( "a and b values [ " + i +" ] ="+ numPV[i] );
    }
    // calculate half layers
    //  Pkp5 = akp5 + bkp5 * Psurf
    int nlevels = NV/2;
    double[] pressurekp5 = new double[ nlevels ];
    for (int i = 0; i < nlevels; i++) {
      pressurekp5[ i ] = numPV[ i ] + numPV[ i +nlevels ] * levelValue;
      //System.out.println( "Pressure kp5 [ "+ i +" ] ="+ pressurekp5[ i ] );
    }
    // average adjacent half layers
    // Pk = 1/2( Pkp-5 + Pkp5 )
    double[] pressureLevel = new double[ nlevels -1 ];
    for (int i = 0; i < nlevels -1; i++) {
      pressureLevel[ i ] =  (pressurekp5[ i ] + pressurekp5[ i +1 ] ) * 0.5 ;
      //System.out.println( "Pressure [ "+ i +" ] ="+ pressureLevel[ i ] );
    }

    return pressureLevel;
  }

  public final double[] getVerticalPressureLevels() {
    // Documentation for the conversion process is at:
    // http://www.ecmwf.int/research/ifsdocs/DYNAMICS/Chap2_Discretization4.html
    // read data values a and b, all a's are read first, then b's
    // http://www.ecmwf.int/products/data/technical/model_levels/model_def_91.html
    // testdata   ECMWF.2008.09.07.T00Z.uad_HGrbF00.A08334081908 has 91 levels
    //
    int offset = getPVorPL() -1; // adjust from octet offset to input[] offset
    int NV = getNV();
    double[] numPV = new double[ NV ];
    for (int i = 0; i < NV; i++) {
      numPV[i] = GribNumbers.float4(getInt(offset++), getInt(offset++), getInt(offset++), getInt(offset++)) ;
      //System.out.println( "a and b values [ " + i +" ] ="+ numPV[i] );
    }
    return numPV;
  }

  // Original code from GDS1
  /**
   * Gets the number of vertical coordinate for this parameter and converts
   * them to pressure coordinates.
   *
   * @param NV  number of vertical coordinates
   * @param raf RandomAccessFile
   * @throws IOException _more_
   */
  /*
  private void getPV(int NV, RandomAccessFile raf) throws IOException {

    // Documentation for the conversion process is at:
    // http://www.ecmwf.int/research/ifsdocs/DYNAMICS/Chap2_Discretization4.html
    // read data
    float[] numPV = new float[NV];
    for (int i = 0; i < NV; i++) {
      numPV[i] = GribNumbers.float4(raf);
      //System.out.println( "a and b values [ " + i +" ] ="+ numPV[i] );
    }
    // calculate half layers
    int nlevels = NV/2;
    float[] pressure0p5 = new float[ nlevels ];
    for (int i = 0; i < nlevels; i++) {
      //pressure0p5[ i ] = numPV[ i ] + numPV[ i +nlevels ] * pds.getPdsVars().getValueFirstFixedSurface();
      //System.out.println( "Pressure 0p5 [ "+ i +" ] ="+ pressure0p5[ i ] );
    }
    // average adjacent half layers
    float[] pressureLevel = new float[ nlevels -1 ];
    for (int i = 0; i < nlevels -1; i++) {
      pressureLevel[ i ] =  (pressure0p5[ i ] + pressure0p5[ i +1 ] ) * 1/2 ;
      //System.out.println( "Pressure [ "+ i +" ] ="+ pressureLevel[ i ] );
    }

  }
  */
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
