/*
 * Author: Sami Salkosuo, sami.salkosuo@fi.ibm.com
 *
 * (c) Copyright IBM Corp. 2007
 *
 * Downloader Agreement

The following are terms of a legal downloader agreement (the "Agreement") regarding Your download of Content (as defined below) from this Website.
IBM may change these terms of use and other requirements and guidelines for use of this Website at its sole discretion.
This Website may contain other proprietary notices and copyright information the terms of which must be observed and followed.
Any use of the Content in violation of this Agreement is strictly prohibited.

"Content" includes, but is not limited to, software, text and/or speech files, code, associated materials, media and /or documentation that
You download from this Website. The Content may be provided by IBM or third-parties. IBM Content is owned by IBM and is copyrighted and licensed,
not sold. Third-party Content is owned by its respective owner and is licensed by the third party directly to You. IBM's decision to permit
posting of third-party Content does not constitute an express or implied license from IBM to You or a recommendation or endorsement by IBM
of any particular product, service, company or technology.

The party providing the Content (the "Provider") grants You a nonexclusive, worldwide, irrevocable, royalty-free, copyright license to edit,
copy, reproduce, publish, publicly display and/or perform, format, modify and/or make derivative works of, translate, re-arrange, and distribute
the Content or any portions thereof and to sublicense any or all such rights and to permit sublicensees to further sublicense such rights, for both
commercial and non-commercial use, provided You abide by the terms of this Agreement. You understand that no assurances are provided that the Content
does not infringe the intellectual property rights of any other entity. Neither IBM nor the provider of the Content grants a patent license of any kind,
whether expressed or implied or by estoppel. As a condition of exercising the rights and licenses granted under this Agreement,
You assume sole responsibility to obtain any other intellectual property rights needed.

The Provider of the Content is the party that submitted the Content for Posting and who represents and warrants that they own all of the Content,
(or have obtained all written releases, authorizations and licenses from any other owner(s) necessary to grant IBM and downloaders this license
with respect to portions of the Content not owned by the Provider). All information provided on or through this Website may be changed or updated
without notice. You understand that IBM has no obligation to check information and /or Content on the Website and that the information and/or
Content provided on this Web site may contain technical inaccuracies or typographical errors.

IBM may, in its sole discretion, discontinue the Website, any service provided on or through the Website, as well as limit or discontinue access
to any Content posted on the Website for any reason without notice. IBM may terminate this Agreement and Your rights to access, use and download
Content from the Website at any time, with or without cause, immediately and without notice.

ALL INFORMATION AND CONTENT IS PROVIDED ON AN "AS IS" BASIS. IBM MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, CONCERNING USE OF
THE WEBSITE, THE CONTENT, OR THE COMPLETENESS OR ACCURACY OF THE CONTENT OR INFORMATION OBTAINED FROM THE WEBSITE. IBM SPECIFICALLY DISCLAIMS
ALL WARRANTIES WITH REGARD TO THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. IBM DOES NOT
WARRANT UNINTERRUPTED OR ERROR-FREE OPERATION OF ANY CONTENT. IBM IS NOT RESPONSIBLE FOR THE RESULTS OBTAINED FROM THE USE OF THE CONTENT OR
INFORMATION OBTAINED FROM THE WEBSITE.

LIMITATION OF LIABILITY. IN NO EVENT WILL IBM BE LIABLE TO ANY PARTY FOR ANY DIRECT, INDIRECT, SPECIAL OR OTHER CONSEQUENTIAL DAMAGES FOR A
NY USE OF THIS WEBSITE, THE USE OF CONTENT FROM THIS WEBSITE, OR ON ANY OTHER HYPER LINKED WEB SITE, INCLUDING, WITHOUT LIMITATION, ANY LOST
PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA ON YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE, EVEN IF IBM IS EXPRESSLY
ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

The laws of the State of New York, USA govern this Agreement, without reference to conflict of law principles. The "United Nations Convention
on International Sale of Goods" does not apply. This Agreement may not be assigned by You. The parties agree to waive their right to a trial by jury.

This Agreement is the complete and exclusive agreement between the parties and supersedes all prior agreements, oral or written, and all
other communications relating to the subject matter hereof. For clarification, it is understood and You agree, that any additional
agreement or license terms that may accompany the Content is invalid, void, and non-enforceable to any downloader of this Content including IBM.

If any section of this Agreement is found by competent authority to be invalid, illegal or unenforceable in any respect for any reason,
the validity, legality and enforceability of any such section in every other respect and the remainder of this Agreement shall continue in effect.
 */
package ucar.unidata.geoloc.projection;

import java.util.Hashtable;
import java.util.Map;

// doesnt seem to be used
class UtmCoordinateConversion {

  public UtmCoordinateConversion() {

  }

  public double[] utm2LatLon(String UTM) {
    UTM2LatLon c = new UTM2LatLon();
    return c.convertUTMToLatLong(UTM);
  }

  public String latLon2UTM(double latitude, double longitude) {
    LatLon2UTM c = new LatLon2UTM();
    return c.convertLatLonToUTM(latitude, longitude);

  }

  private void validate(double latitude, double longitude) {
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0
            || longitude >= 180.0) {
      throw new IllegalArgumentException(
              "Legal ranges: latitude [-90,90], longitude [-180,180).");
    }

  }

  public String latLon2MGRUTM(double latitude, double longitude) {
    LatLon2MGRUTM c = new LatLon2MGRUTM();
    return c.convertLatLonToMGRUTM(latitude, longitude);

  }

  public double[] mgrutm2LatLon(String MGRUTM) {
    MGRUTM2LatLon c = new MGRUTM2LatLon();
    return c.convertMGRUTMToLatLong(MGRUTM);
  }

  public double degreeToRadian(double degree) {
    return degree * Math.PI / 180;
  }

  public double radianToDegree(double radian) {
    return radian * 180 / Math.PI;
  }

  private double POW(double a, double b) {
    return Math.pow(a, b);
  }

  private double SIN(double value) {
    return Math.sin(value);
  }

  private double COS(double value) {
    return Math.cos(value);
  }

  private double TAN(double value) {
    return Math.tan(value);
  }

  private class LatLon2UTM {
    public String convertLatLonToUTM(double latitude, double longitude) {
      validate(latitude, longitude);
      String UTM = "";

      setVariables(latitude, longitude);

      String longZone = getLongZone(longitude);
      LatZones latZones = new LatZones();
      String latZone = latZones.getLatZone(latitude);

      double _easting = getEasting();
      double _northing = getNorthing(latitude);

      UTM = longZone + " " + latZone + " " + ((int) _easting) + " "
              + ((int) _northing);
      // UTM = longZone + " " + latZone + " " + decimalFormat.format(_easting) +
      // " "+ decimalFormat.format(_northing);

      return UTM;

    }

    protected void setVariables(double latitude, double longitude) {
      latitude = degreeToRadian(latitude);
      rho = equatorialRadius * (1 - e * e)
              / POW(1 - POW(e * SIN(latitude), 2), 3 / 2.0);

      nu = equatorialRadius / POW(1 - POW(e * SIN(latitude), 2), (1 / 2.0));

      double var1;
      if (longitude < 0.0) {
        var1 = ((int) ((180 + longitude) / 6.0)) + 1;
      } else {
        var1 = ((int) (longitude / 6)) + 31;
      }
      double var2 = (6 * var1) - 183;
      double var3 = longitude - var2;
      p = var3 * 3600 / 10000;

      S = A0 * latitude - B0 * SIN(2 * latitude) + C0 * SIN(4 * latitude) - D0
              * SIN(6 * latitude) + E0 * SIN(8 * latitude);

      K1 = S * k0;
      K2 = nu * SIN(latitude) * COS(latitude) * POW(sin1, 2) * k0 * (100000000)
              / 2;
      K3 = ((POW(sin1, 4) * nu * SIN(latitude) * Math.pow(COS(latitude), 3)) / 24)
              * (5 - POW(TAN(latitude), 2) + 9 * e1sq * POW(COS(latitude), 2) + 4
              * POW(e1sq, 2) * POW(COS(latitude), 4))
              * k0
              * (10000000000000000L);

      K4 = nu * COS(latitude) * sin1 * k0 * 10000;

      K5 = POW(sin1 * COS(latitude), 3) * (nu / 6)
              * (1 - POW(TAN(latitude), 2) + e1sq * POW(COS(latitude), 2)) * k0
              * 1000000000000L;

      A6 = (POW(p * sin1, 6) * nu * SIN(latitude) * POW(COS(latitude), 5) / 720)
              * (61 - 58 * POW(TAN(latitude), 2) + POW(TAN(latitude), 4) + 270
              * e1sq * POW(COS(latitude), 2) - 330 * e1sq
              * POW(SIN(latitude), 2)) * k0 * (1E+24);

    }

    protected String getLongZone(double longitude) {
      double longZone = 0;
      if (longitude < 0.0) {
        longZone = ((180.0 + longitude) / 6) + 1;
      } else {
        longZone = (longitude / 6) + 31;
      }
      String val = String.valueOf((int) longZone);
      if (val.length() == 1) {
        val = "0" + val;
      }
      return val;
    }

    protected double getNorthing(double latitude) {
      double northing = K1 + K2 * p * p + K3 * POW(p, 4);
      if (latitude < 0.0) {
        northing = 10000000 + northing;
      }
      return northing;
    }

    protected double getEasting() {
      return 500000 + (K4 * p + K5 * POW(p, 3));
    }

    // Lat Lon to UTM variables

    // equatorial radius
    double equatorialRadius = 6378137;

    // polar radius
    double polarRadius = 6356752.314;

    // flattening
    double flattening = 0.00335281066474748;// (equatorialRadius-polarRadius)/equatorialRadius;

    // inverse flattening 1/flattening
    double inverseFlattening = 298.257223563;// 1/flattening;

    // Mean radius
    double rm = POW(equatorialRadius * polarRadius, 1 / 2.0);

    // scale factor
    double k0 = 0.9996;

    // eccentricity
    double e = Math.sqrt(1 - POW(polarRadius / equatorialRadius, 2));

    double e1sq = e * e / (1 - e * e);

    double n = (equatorialRadius - polarRadius)
            / (equatorialRadius + polarRadius);

    // r curv 1
    double rho = 6368573.744;

    // r curv 2
    double nu = 6389236.914;

    // Calculate Meridional Arc Length
    // Meridional Arc
    double S = 5103266.421;

    double A0 = 6367449.146;

    double B0 = 16038.42955;

    double C0 = 16.83261333;

    double D0 = 0.021984404;

    double E0 = 0.000312705;

    // Calculation Constants
    // Delta Long
    double p = -0.483084;

    double sin1 = 4.84814E-06;

    // Coefficients for UTM Coordinates
    double K1 = 5101225.115;

    double K2 = 3750.291596;

    double K3 = 1.397608151;

    double K4 = 214839.3105;

    double K5 = -2.995382942;

    double A6 = -1.00541E-07;

  }

  private class LatLon2MGRUTM extends LatLon2UTM {
    public String convertLatLonToMGRUTM(double latitude, double longitude) {
      validate(latitude, longitude);
      String mgrUTM = "";

      setVariables(latitude, longitude);

      String longZone = getLongZone(longitude);
      LatZones latZones = new LatZones();
      String latZone = latZones.getLatZone(latitude);

      double _easting = getEasting();
      double _northing = getNorthing(latitude);
      Digraphs digraphs = new Digraphs();
      String digraph1 = digraphs.getDigraph1(Integer.parseInt(longZone),
              _easting);
      String digraph2 = digraphs.getDigraph2(Integer.parseInt(longZone),
              _northing);

      String easting = String.valueOf((int) _easting);
      if (easting.length() < 5) {
        easting = "00000" + easting;
      }
      easting = easting.substring(easting.length() - 5);

      String northing;
      northing = String.valueOf((int) _northing);
      if (northing.length() < 5) {
        northing = "0000" + northing;
      }
      northing = northing.substring(northing.length() - 5);

      mgrUTM = longZone + latZone + digraph1 + digraph2 + easting + northing;
      return mgrUTM;
    }
  }

  private class MGRUTM2LatLon extends UTM2LatLon {
    public double[] convertMGRUTMToLatLong(String mgrutm) {
      double[] latlon = {0.0, 0.0};
      // 02CNR0634657742
      int zone = Integer.parseInt(mgrutm.substring(0, 2));
      String latZone = mgrutm.substring(2, 3);

      String digraph1 = mgrutm.substring(3, 4);
      String digraph2 = mgrutm.substring(4, 5);
      easting = Double.parseDouble(mgrutm.substring(5, 10));
      northing = Double.parseDouble(mgrutm.substring(10, 15));

      LatZones lz = new LatZones();
      double latZoneDegree = lz.getLatZoneDegree(latZone);

      double a1 = latZoneDegree * 40000000 / 360.0;
      double a2 = 2000000 * Math.floor(a1 / 2000000.0);

      Digraphs digraphs = new Digraphs();

      double digraph2Index = digraphs.getDigraph2Index(digraph2);

      double startindexEquator = 1;
      if ((1 + zone % 2) == 1) {
        startindexEquator = 6;
      }

      double a3 = a2 + (digraph2Index - startindexEquator) * 100000;
      if (a3 <= 0) {
        a3 = 10000000 + a3;
      }
      northing = a3 + northing;

      zoneCM = -183 + 6 * zone;
      double digraph1Index = digraphs.getDigraph1Index(digraph1);
      int a5 = 1 + zone % 3;
      double[] a6 = {16, 0, 8};
      double a7 = 100000 * (digraph1Index - a6[a5 - 1]);
      easting = easting + a7;

      setVariables();

      double latitude = 0;
      latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

      if (latZoneDegree < 0) {
        latitude = 90 - latitude;
      }

      double d = _a2 * 180 / Math.PI;
      double longitude = zoneCM - d;

      if (getHemisphere(latZone).equals("S")) {
        latitude = -latitude;
      }

      latlon[0] = latitude;
      latlon[1] = longitude;
      return latlon;
    }
  }

  private class UTM2LatLon {
    double easting;

    double northing;

    int zone;

    String southernHemisphere = "ACDEFGHJKLM";

    protected String getHemisphere(String latZone) {
      String hemisphere = "N";
      if (southernHemisphere.indexOf(latZone) > -1) {
        hemisphere = "S";
      }
      return hemisphere;
    }

    public double[] convertUTMToLatLong(String UTM) {
      double[] latlon = {0.0, 0.0};
      String[] utm = UTM.split(" ");
      zone = Integer.parseInt(utm[0]);
      String latZone = utm[1];
      easting = Double.parseDouble(utm[2]);
      northing = Double.parseDouble(utm[3]);
      String hemisphere = getHemisphere(latZone);
      double latitude = 0.0;
      double longitude = 0.0;

      if (hemisphere.equals("S")) {
        northing = 10000000 - northing;
      }
      setVariables();
      latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

      if (zone > 0) {
        zoneCM = 6 * zone - 183.0;
      } else {
        zoneCM = 3.0;

      }

      longitude = zoneCM - _a3;
      if (hemisphere.equals("S")) {
        latitude = -latitude;
      }

      latlon[0] = latitude;
      latlon[1] = longitude;
      return latlon;

    }

    protected void setVariables() {
      arc = northing / k0;
      mu = arc
              / (a * (1 - POW(e, 2) / 4.0 - 3 * POW(e, 4) / 64.0 - 5 * POW(e, 6) / 256.0));

      ei = (1 - POW((1 - e * e), (1 / 2.0)))
              / (1 + POW((1 - e * e), (1 / 2.0)));

      ca = 3 * ei / 2 - 27 * POW(ei, 3) / 32.0;

      cb = 21 * POW(ei, 2) / 16 - 55 * POW(ei, 4) / 32;
      cc = 151 * POW(ei, 3) / 96;
      cd = 1097 * POW(ei, 4) / 512;
      phi1 = mu + ca * SIN(2 * mu) + cb * SIN(4 * mu) + cc * SIN(6 * mu) + cd
              * SIN(8 * mu);

      n0 = a / POW((1 - POW((e * SIN(phi1)), 2)), (1 / 2.0));

      r0 = a * (1 - e * e) / POW((1 - POW((e * SIN(phi1)), 2)), (3 / 2.0));
      fact1 = n0 * TAN(phi1) / r0;

      _a1 = 500000 - easting;
      dd0 = _a1 / (n0 * k0);
      fact2 = dd0 * dd0 / 2;

      t0 = POW(TAN(phi1), 2);
      Q0 = e1sq * POW(COS(phi1), 2);
      fact3 = (5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * POW(dd0, 4)
              / 24;

      fact4 = (61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - 3 * Q0
              * Q0)
              * POW(dd0, 6) / 720;

      //
      lof1 = _a1 / (n0 * k0);
      lof2 = (1 + 2 * t0 + Q0) * POW(dd0, 3) / 6.0;
      lof3 = (5 - 2 * Q0 + 28 * t0 - 3 * POW(Q0, 2) + 8 * e1sq + 24 * POW(t0, 2))
              * POW(dd0, 5) / 120;
      _a2 = (lof1 - lof2 + lof3) / COS(phi1);
      _a3 = _a2 * 180 / Math.PI;

    }

    double arc;

    double mu;

    double ei;

    double ca;

    double cb;

    double cc;

    double cd;

    double n0;

    double r0;

    double _a1;

    double dd0;

    double t0;

    double Q0;

    double lof1;

    double lof2;

    double lof3;

    double _a2;

    double phi1;

    double fact1;

    double fact2;

    double fact3;

    double fact4;

    double zoneCM;

    double _a3;

    double b = 6356752.314;

    double a = 6378137;

    double e = 0.081819191;

    double e1sq = 0.006739497;

    double k0 = 0.9996;

  }

  private class Digraphs {
    private Map digraph1 = new Hashtable();

    private Map digraph2 = new Hashtable();

    private String[] digraph1Array = {"A", "B", "C", "D", "E", "F", "G", "H",
            "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
            "Y", "Z"};

    private String[] digraph2Array = {"V", "A", "B", "C", "D", "E", "F", "G",
            "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V"};

    public Digraphs() {
      digraph1.put(new Integer(1), "A");
      digraph1.put(new Integer(2), "B");
      digraph1.put(new Integer(3), "C");
      digraph1.put(new Integer(4), "D");
      digraph1.put(new Integer(5), "E");
      digraph1.put(new Integer(6), "F");
      digraph1.put(new Integer(7), "G");
      digraph1.put(new Integer(8), "H");
      digraph1.put(new Integer(9), "J");
      digraph1.put(new Integer(10), "K");
      digraph1.put(new Integer(11), "L");
      digraph1.put(new Integer(12), "M");
      digraph1.put(new Integer(13), "N");
      digraph1.put(new Integer(14), "P");
      digraph1.put(new Integer(15), "Q");
      digraph1.put(new Integer(16), "R");
      digraph1.put(new Integer(17), "S");
      digraph1.put(new Integer(18), "T");
      digraph1.put(new Integer(19), "U");
      digraph1.put(new Integer(20), "V");
      digraph1.put(new Integer(21), "W");
      digraph1.put(new Integer(22), "X");
      digraph1.put(new Integer(23), "Y");
      digraph1.put(new Integer(24), "Z");

      digraph2.put(new Integer(0), "V");
      digraph2.put(new Integer(1), "A");
      digraph2.put(new Integer(2), "B");
      digraph2.put(new Integer(3), "C");
      digraph2.put(new Integer(4), "D");
      digraph2.put(new Integer(5), "E");
      digraph2.put(new Integer(6), "F");
      digraph2.put(new Integer(7), "G");
      digraph2.put(new Integer(8), "H");
      digraph2.put(new Integer(9), "J");
      digraph2.put(new Integer(10), "K");
      digraph2.put(new Integer(11), "L");
      digraph2.put(new Integer(12), "M");
      digraph2.put(new Integer(13), "N");
      digraph2.put(new Integer(14), "P");
      digraph2.put(new Integer(15), "Q");
      digraph2.put(new Integer(16), "R");
      digraph2.put(new Integer(17), "S");
      digraph2.put(new Integer(18), "T");
      digraph2.put(new Integer(19), "U");
      digraph2.put(new Integer(20), "V");

    }

    public int getDigraph1Index(String letter) {
      for (int i = 0; i < digraph1Array.length; i++) {
        if (digraph1Array[i].equals(letter)) {
          return i + 1;
        }
      }

      return -1;
    }

    public int getDigraph2Index(String letter) {
      for (int i = 0; i < digraph2Array.length; i++) {
        if (digraph2Array[i].equals(letter)) {
          return i;
        }
      }

      return -1;
    }

    public String getDigraph1(int longZone, double easting) {
      int a1 = longZone;
      double a2 = 8 * ((a1 - 1) % 3) + 1;

      double a3 = easting;
      double a4 = a2 + ((int) (a3 / 100000)) - 1;
      return (String) digraph1.get(new Integer((int) Math.floor(a4)));
    }

    public String getDigraph2(int longZone, double northing) {
      int a1 = longZone;
      double a2 = 1 + 5 * ((a1 - 1) % 2);
      double a3 = northing;
      double a4 = (a2 + ((int) (a3 / 100000)));
      a4 = (a2 + ((int) (a3 / 100000.0))) % 20;
      a4 = Math.floor(a4);
      if (a4 < 0) {
        a4 = a4 + 19;
      }
      return (String) digraph2.get(new Integer((int) Math.floor(a4)));

    }

  }

  private class LatZones {
    private char[] letters = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Z'};

    private int[] degrees = {-90, -84, -72, -64, -56, -48, -40, -32, -24, -16,
            -8, 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84};

    private char[] negLetters = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'L', 'M'};

    private int[] negDegrees = {-90, -84, -72, -64, -56, -48, -40, -32, -24,
            -16, -8};

    private char[] posLetters = {'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Z'};

    private int[] posDegrees = {0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84};

    private int arrayLength = 22;

    public LatZones() {
    }

    public int getLatZoneDegree(String letter) {
      char ltr = letter.charAt(0);
      for (int i = 0; i < arrayLength; i++) {
        if (letters[i] == ltr) {
          return degrees[i];
        }
      }
      return -100;
    }

    public String getLatZone(double latitude) {
      int latIndex = -2;
      int lat = (int) latitude;

      if (lat >= 0) {
        int len = posLetters.length;
        for (int i = 0; i < len; i++) {
          if (lat == posDegrees[i]) {
            latIndex = i;
            break;
          }

          if (lat > posDegrees[i]) {
            continue;
          } else {
            latIndex = i - 1;
            break;
          }
        }
      } else {
        int len = negLetters.length;
        for (int i = 0; i < len; i++) {
          if (lat == negDegrees[i]) {
            latIndex = i;
            break;
          }

          if (lat < negDegrees[i]) {
            latIndex = i - 1;
            break;
          } else {
            continue;
          }

        }

      }

      if (latIndex == -1) {
        latIndex = 0;
      }
      if (lat >= 0) {
        if (latIndex == -2) {
          latIndex = posLetters.length - 1;
        }
        return String.valueOf(posLetters[latIndex]);
      } else {
        if (latIndex == -2) {
          latIndex = negLetters.length - 1;
        }
        return String.valueOf(negLetters[latIndex]);

      }
    }

  }

  static public void main(String[] args) {
    //debug = true;
    UtmCoordinateConversion utm = new UtmCoordinateConversion();
    // convert latitude longitude to UTM
    assert (utm.latLon2UTM(0.0000, 0.0000).equals("31 N 166021 0"));
    assert (utm.latLon2UTM(0.1300, -0.2324).equals("30 N 808084 14385"));
    assert (utm.latLon2UTM(-45.6456, 23.3545).equals("34 G 683473 4942631"));
    assert (utm.latLon2UTM(-12.7650, -33.8765).equals("25 L 404859 8588690"));
    assert (utm.latLon2UTM(-80.5434, -170.6540).equals("02 C 506346 1057742"));
    assert (utm.latLon2UTM(90.0000, 177.0000).equals("60 Z 500000 9997964"));
    assert (utm.latLon2UTM(-90.0000, -177.0000).equals("01 A 500000 2035"));
    assert (utm.latLon2UTM(90.0000, 3.0000).equals("31 Z 500000 9997964"));
    assert (utm.latLon2UTM(23.4578, -135.4545).equals("08 Q 453580 2594272"));
    assert (utm.latLon2UTM(77.3450, 156.9876).equals("57 X 450793 8586116"));
    assert (utm.latLon2UTM(-89.3454, -48.9306).equals("22 A 502639 75072"));


    System.out.println(utm.latLon2UTM(0.0000, 0.0000)); //.equals(     "31 N 166021 0" ));
    System.out.println(utm.latLon2UTM(0.1300, -0.2324)); //.equals(     "30 N 808084 14385"));
    System.out.println(utm.latLon2UTM(-45.6456, 23.3545)); //.equals(     "34 G 683473 4942631"));
    System.out.println(utm.latLon2UTM(-12.7650, -33.8765)); //.equals(     "25 L 404859 8588690"));
    System.out.println(utm.latLon2UTM(-80.5434, -170.6540)); //.equals(     "02 C 506346 1057742"));
    System.out.println(utm.latLon2UTM(90.0000, 177.0000)); //.equals(     "60 Z 500000 9997964"));
    System.out.println(utm.latLon2UTM(-90.0000, -177.0000)); //.equals(     "01 A 500000 2035"));
    System.out.println(utm.latLon2UTM(90.0000, 3.0000)); //.equals(     "31 Z 500000 9997964"));
    System.out.println(utm.latLon2UTM(23.4578, -135.4545)); //.equals(     "08 Q 453580 2594272"));
    System.out.println(utm.latLon2UTM(77.3450, 156.9876)); //.equals(     "57 X 450793 8586116"));
    System.out.println(utm.latLon2UTM(-89.3454, -48.9306)); //.equals(     "22 A 502639 75072"));

    // convert UTM to latitude longitude
    double[] ll;
    ll = utm.utm2LatLon("31 N 166021 0");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("30 N 808084 14385");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("34 G 683473 4942631");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("25 L 404859 8588690");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("02 C 506346 1057742");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("60 Z 500000 9997964");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("01 A 500000 2035");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("31 Z 500000 9997964");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("08 Q 453580 2594272");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("57 X 450793 8586116");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
    ll = utm.utm2LatLon("22 A 502639 75072");
    System.out.println("latitude =" + ll[0] + " longitude =" + ll[1]);
  }
}
