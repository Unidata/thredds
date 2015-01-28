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

package ucar.nc2.grib.grib1;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;

/**
 * Level information contained in a particular PDS.
 * WMO Table 3
 *
 * @author caron
 * @since 1/13/12
 */

@Immutable
public class Grib1ParamLevel {
  private final Grib1Customizer cust;
  private final int levelType; // code Table 3 (octet 10)
  private final float value1;
  private final float value2;

  /**
   * Allows center specific parsing
   *
   * @param cust customized for this center/subcenter
   * @param levelType the level type
   * @param value1 first level value
   * @param value2 second level value
   */
  public Grib1ParamLevel(Grib1Customizer cust, int levelType, float value1, float value2) {
    this.cust = cust;
    this.levelType = levelType;
    this.value1 = value1;
    this.value2 = value2;
  }

  /**
   * Implements tables 3 and 3a.
   *
   * @param cust customized for this center/subcenter
   * @param pds the Grib1SectionProductDefinition
   */
  public Grib1ParamLevel(Grib1Customizer cust, Grib1SectionProductDefinition pds) {
    this.cust = cust;

    // default surface values
    levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    switch (levelType) {
      default:
        value1 = pds11;
        value2 = pds12;
        break;

      case 20:
        value1 = (float) (pds1112 * 0.01);
        value2 = GribNumbers.MISSING;
        break;

      case 100:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 101:
        value1 = pds11 * 10;  // convert from kPa to hPa - who uses kPa???
        value2 = pds12 * 10;
        break;

      case 103:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 104:
        value1 = (pds11 * 100);  // convert hm to m
        value2 = (pds12 * 100);
        break;

      case 105:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 106:
        value1 = (pds11 * 100);  // convert hm to m
        value2 = (pds12 * 100);
        break;

      case 107:
        value1 = (float) (pds1112 * 0.0001);
        value2 = GribNumbers.MISSING;
        break;

      case 108:
        value1 = (float) (pds11 * 0.01);
        value2 = (float) (pds12 * 0.01);
        break;

      case 109:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 110:
        value1 = pds11;
        value2 = pds12;
        break;

      case 111:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 112:
        value1 = pds11;
        value2 = pds12;
        break;

      case 113:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 114:
        value1 = 475 - pds11;
        value2 = 475 - pds12;
        break;

      case 115:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 116:
        value1 = pds11;
        value2 = pds12;
        break;

      case 117:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 119:
        value1 = (float) (pds1112 * 0.0001);
        value2 = GribNumbers.MISSING;
        break;

      case 120:
        value1 = (float) (pds11 * 0.01);
        value2 = (float) (pds12 * 0.01);
        break;

      case 121:
        value1 = 1100 - pds11;
        value2 = 1100 - pds12;
        break;

      case 125:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
       break;

      case 126:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;

      case 128:
        value1 = (float) (1.1 - (pds11 * 0.001));
        value2 = (float) (1.1 - (pds12 * 0.001));
        break;

      case 141:
        //value1 = pds11*10; // convert from kPa to hPa - who uses kPa???
        value1 = pds11;  // 388 nows says it is hPA
        value2 = 1100 - pds12;
        break;

      case 160:
        value1 = pds1112;
        value2 = GribNumbers.MISSING;
        break;
    }
  }

  /**
   * Index number from table 3 (pds octet 10)
   *
   * @return index
   */
  public int getLevelType() {
    return levelType;
  }

  /**
   * gets the 1st value for the level.
   *
   * @return level value 1
   */
  public float getValue1() {
    return value1;
  }

  /**
   * gets the 2nd value for the level.
   *
   * @return level value 2
   */
  public float getValue2() {
    return value2;
  }

  public String getNameShort() {
    return cust.getLevelNameShort(levelType);
  }

  public String getDescription() {
    return cust.getLevelDescription(levelType);
  }
}

