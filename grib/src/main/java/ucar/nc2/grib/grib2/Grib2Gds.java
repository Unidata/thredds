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

import java.util.Arrays;
import java.util.Formatter;

import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.EarthEllipsoid;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;

/**
 * Template-specific fields for Grib2SectionGridDefinition
 *
 * @author caron
 * @since 4/2/11
 */
public abstract class Grib2Gds {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2Gds.class);
  public static final double maxReletiveErrorPos = .01; // reletive error in position - GRIB numbers sometime miscoded

  public static Grib2Gds factory(int template, byte[] data) {
    Grib2Gds result;
    switch (template) {
      case 0:
        result = new LatLon(data);
        break;
      case 1:
        result = new RotatedLatLon(data);
        break;
      case 10:
        result = new Mercator(data);
        break;
      case 20:
        result = new PolarStereographic(data);
        break;
      case 30:
        result = new LambertConformal(data, 30);
        break;
      case 31:
        result = new AlbersEqualArea(data);
        break;
      case 40:
        result = new GaussLatLon(data);
        break;
      case 50:  // Spherical Harmonic Coefficients BOGUS
        result = new GdsSpherical(data, template);
        break;
      case 90:
        result = new SpaceViewPerspective(data);
        break;

      // LOOK NCEP specific
      case 204:
        result = new CurvilinearOrthogonal(data);
        break;
      case 32769:
        result = new RotatedLatLon32769(data);
        break;

      default:
        throw new UnsupportedOperationException("Unsupported GDS type = " + template);
    }

    result.finish(); // stuff that cant be done in the constructor
    return result;
  }

  ///////////////////////////////////////////////////
  private static final float scale3 = (float) 1.0e-3;
  private static final float scale6 = (float) 1.0e-6;

  protected final byte[] data;

  public int template;
  public int center;
  public float earthRadius, majorAxis, minorAxis; // in meters
  protected int scanMode;
  public int earthShape;

  private int nx, ny;         // raw
  protected int[] nptsInLine; // thin grids, else null
  protected int lastOctet;

  protected Grib2Gds(byte[] data) {
    this.data = data;
  }

  protected Grib2Gds(byte[] data, int template) {
    this.data = data;
    this.template = template;

    earthShape = getOctet(15);
    earthRadius = getScaledValue(16);
    majorAxis = getScaledValue(21);
    minorAxis = getScaledValue(26);

    nx = getOctet4(31);
    ny = getOctet4(35);
  }

  protected void finish() {
    if (isThin()) readNptsInLine();
  }

  public abstract GdsHorizCoordSys makeHorizCoordSys();

  public abstract void testHorizCoordSys(Formatter f);

  public void testScanMode(Formatter f) {
  }

  // number of points along nx, adjusted for thin grid
  public int getNx() {
    if (nptsInLine == null || nx > 0) return nx;
    return QuasiRegular.getMax(nptsInLine);
  }

  // number of points along ny, adjusted for thin grid
  public int getNy() {
    if (nptsInLine == null || ny > 0) return ny;
    return QuasiRegular.getMax(nptsInLine);
  }

  public int getNxRaw() {
    return nx;
  }

  public int getNyRaw() {
    return ny;
  }

  public int[] getNptsInLine() {
    return nptsInLine;
  }

  public byte[] getRawBytes() {
    return data;
  }

  public int getScanMode() {
    return scanMode;
  }

  // hack to fix eumetsat GDS
  public void setCenter(int center) {
    this.center = center;
  }

  public boolean isLatLon() {
    return false;
  }

  public String getNameShort() {
    String className = getClass().getName();
    int pos = className.lastIndexOf("$");
    return className.substring(pos + 1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib2Gds grib2Gds = (Grib2Gds) o;

    if (nx != grib2Gds.nx) return false;
    if (ny != grib2Gds.ny) return false;
    if (template != grib2Gds.template) return false;
    return Arrays.equals(nptsInLine, grib2Gds.nptsInLine);
  }

  @Override
  public int hashCode() {
    int result = template;
    result = 31 * result + nx;
    result = 31 * result + ny;
    if (nptsInLine != null)
      result = 31 * result + Arrays.hashCode(nptsInLine);
    return result;
  }

  protected int hashCode = 0;

  //////////////// thin grids
  /*

   11 Number of octets for optional list of numbers (see Note 2)
   12 Interpretation of list of numbers (see Code table 3.11)

   (Note 2) An optional list of numbers may be used to document a quasi-regular grid. In such a case, octet 11 is non zero and gives
   the number of octets used per item on the list. For all other cases, such as regular grids, octets 11 and 12 are zero and
   no list is appended to the grid definition template.

   (2) For data on a quasi-regular grid, where all the rows or columns do not necessarily have the same number of grid points,
   either Ni (octets 31–34) or Nj (octets 35–38) and the corresponding Di (octets 64–67) or
   Dj (octets 68–71) shall be coded with all bits set to 1 (missing). The actual number of points along each parallel or
   meridian shall be coded in the octets immediately following the grid definition template (octets [xx+1]–nn), as
   described in the description of the grid definition section.

   (3) A quasi-regular grid is only defined for appropriate grid scanning modes. Either rows or columns, but not both
   simultaneously, may have variable numbers of points or variable spacing. The first point in each row (column) shall be
   positioned at the meridian (parallel) indicated by octets 47–54. The grid points shall be evenly spaced in latitude
   (longitude).
   */

  public boolean isThin() {
    boolean isThin = (getOctet(11) != 0);
    assert !isThin || (nx < 0 || ny < 0);
    return isThin;
  }

  protected void readNptsInLine() {
    int numOctetsPerNumber = getOctet(11);
    int octet12 = getOctet(12);
    if (octet12 != 1)
      throw new IllegalArgumentException("Thin grid octet 12 =" + octet12);

    int numPts = (nx > 0) ? nx : ny;
    int[] parallels = new int[numPts];
    int offset = lastOctet;
    for (int i = 0; i < numPts; i++) {
      switch (numOctetsPerNumber) {
        case 1:
          parallels[i] = getOctet(offset++);
          break;
        case 2:
          parallels[i] = GribNumbers.int2(getOctet(offset++), getOctet(offset++));
          break;
        case 4:
          parallels[i] = getOctet4(offset);
          offset += 4;
        default:
          throw new IllegalArgumentException("Illegal numOctetsPerNumber in thin grid =" + numOctetsPerNumber);
      }
    }
    nptsInLine = parallels;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  protected int getOctet(int index) {
    return data[index - 1] & 0xff;
  }

  protected int getOctetSigned(int index) {
    return GribNumbers.convertSignedByte(data[index - 1]);
  }

  protected int getOctet4(int start) {
    return GribNumbers.int4(getOctet(start), getOctet(start + 1), getOctet(start + 2), getOctet(start + 3));
  }

  protected float getScaledValue(int start) {
    int scaleFactor = getOctetSigned(start);
    int scaleValue = getOctet4(start + 1);
    if (scaleFactor != 0)
      return (float) (scaleValue / Math.pow(10, scaleFactor));
    else
      return (float) scaleValue;
  }

  /*
  Code Table Code table 3.2 - Shape of the Earth (3.2)
      0: Earth assumed spherical with radius = 6 367 470.0 m
      1: Earth assumed spherical with radius specified (in m) by data producer
      2: Earth assumed oblate spheroid with size as determined by IAU in 1965 (major axis = 6 378 160.0 m, minor axis = 6 356 775.0 m, f = 1/297.0)
      3: Earth assumed oblate spheroid with major and minor axes specified (in km) by data producer
      4: Earth assumed oblate spheroid as defined in IAG-GRS80 model (major axis = 6 378 137.0 m, minor axis = 6 356 752.314 m, f = 1/298.257 222 101)
      5: Earth assumed represented by WGS84 (as used by ICAO since 1998)
      6: Earth assumed spherical with radius of 6 371 229.0 m
      7: Earth assumed oblate spheroid with major or minor axes specified (in m) by data producer
      8: Earth model assumed spherical with radius of 6 371 200 m, but the horizontal datum of the resulting
         latitude/longitude field is the WGS84 reference frame
  */
  protected Earth getEarth() {
    switch (earthShape) {
      case 0:
        return new Earth(6367470.0);
      case 1:
        if (earthRadius < 6000000) earthRadius *= 1000.0; // bad units
        return new Earth(earthRadius);
      case 2:
        return EarthEllipsoid.IAU;
      case 3:
        // oblate in km, so bad values will be large and not scaled
        if (majorAxis < 6000000) majorAxis *= 1000.0;
        if (minorAxis < 6000000) minorAxis *= 1000.0;
        return new EarthEllipsoid("Grib2 Type 3", -1, majorAxis, minorAxis, 0);
      case 4:
        return EarthEllipsoid.IAG_GRS80;
      case 5:
        return EarthEllipsoid.WGS84;
      case 6:
        return new Earth(6371229.0);
      case 7: // Oblate in meters
        if (majorAxis < 6000000) majorAxis *= 1000.0; // bad units
        if (minorAxis < 6000000) minorAxis *= 1000.0; // bad units
        return new EarthEllipsoid("Grib2 Type 7", -1, majorAxis, minorAxis, 0);
      case 8:
        return new Earth(6371200.0);
      case 9:
        return EarthEllipsoid.Airy1830;
      default:
        return new Earth();
    }
  }

  /*
  Template 3.0 (Grid definition template 3.0 - latitude/longitude (or equidistant cylindrical, or Plate Carre))
       1-4 (4): GDS length
       5-5 (1): Section
       6-6 (1): Source of Grid Definition (see code table 3.0)
      7-10 (4): Number of data points
     11-11 (1): Number of octects for optional list of numbers
     12-12 (1): Interpretation of list of numbers
     13-14 (2): Grid Definition Template Number
        15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
        16 (1): Scale factor of radius of spherical Earth
     17-20 (4): Scaled value of radius of spherical Earth
        21 (1): Scale factor of major axis of oblate spheroid Earth
     22-25 (4): Scaled value of major axis of oblate spheroid Earth
        26 (1): Scale factor of minor axis of oblate spheroid Earth
     27-30 (4): Scaled value of minor axis of oblate spheroid Earth
     31-34 (4): Ni - number of points along a parallel
     35-38 (4): Nj - number of points along a meridian
     39-42 (4): Basic angle of the initial production domain - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     43-46 (4): Subdivisions of basic angle used to define extreme longitudes and latitudes, and direction increments - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     47-50 (4): La1 - latitude of first grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     51-54 (4): Lo1 - longitude of first grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
        55 (1): Resolution and component flags - (see Flag table 3.3)#GRIB2_6_0_1_codeflag.doc#G2_CF33
     56-59 (4): La2 - latitude of last grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     60-63 (4): Lo2 - longitude of last grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     64-67 (4): Di - i direction increment - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
     68-71 (4): Dj - j direction increment - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
        72 (1): Scanning mode - (flags - see Flag table 3.4)#GRIB2_6_0_1_codeflag.doc#G2_CF34
     73-nn (0): List of number of points along each meridian or parallel. - (These octets are only present for quasi-regular grids as described in Notes 2 and 3)#GRIB2_6_0_1_temp.doc#G2_Gdt30n
  */
  public static class LatLon extends Grib2Gds {
    public float la1, lo1, la2, lo2, deltaLon, deltaLat;
    public int basicAngle, basicAngleSubdivisions;
    public int flags;

    LatLon(byte[] data) {
      super(data, 0);

      basicAngle = getOctet4(39);
      basicAngleSubdivisions = getOctet4(43);

      float scale = getScale();
      la1 = getOctet4(47) * scale;
      lo1 = getOctet4(51) * scale;
      flags = getOctet(55);
      la2 = getOctet4(56) * scale;
      lo2 = getOctet4(60) * scale;

      scanMode = getOctet(72);
      lastOctet = 73;
    }

    public void testScanMode(Formatter f) {
      if (GribUtils.scanModeYisPositive(scanMode)) {
        if (la1 > la2)
          f.format("  **latlon scan mode=%d dLat=%f lat=(%f,%f)%n", scanMode, deltaLat, la1, la2);
      } else {
        if (la1 < la2)
          f.format("  **latlon scan mode=%d dLat=%f lat=(%f,%f)%n", scanMode, deltaLat, la1, la2);
      }
    }

    protected void finish() {
      super.finish();

      if (lo2 < lo1) lo2 += 360.0F;
      if (Misc.closeEnough(lo1, lo2)) { // canadian met has global with lo1 = lo2 = 180
        lo1 -= 360.0F;
      }

      // GFS_Puerto_Rico_0p5deg seems to have deltaLat, deltaLon incorrectly encoded
      float scale = getScale();
      deltaLon = getOctet4(64) * scale;
      float calcDelta = (lo2 - lo1) / (getNx() - 1); // more accurate - deltaLon may have roundoff
      if (!Misc.closeEnough(deltaLon, calcDelta)) {
        log.debug("deltaLon {} != calcDeltaLon {}", deltaLon, calcDelta);
        deltaLon = calcDelta;
      }

      deltaLat = getOctet4(68) * scale;
      if (la2 < la1) deltaLat = -deltaLat;
      calcDelta = (la2 - la1) / (getNy() - 1); // more accurate - deltaLat may have roundoff
      if (!Misc.closeEnough(deltaLat, calcDelta)) {
        log.debug("deltaLat {} != calcDeltaLat {}", deltaLat, calcDelta);
        deltaLat = calcDelta;
      }
    }

    @Override
    public boolean isLatLon() {
      return true;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      LatLon other = (LatLon) o;
      if (!Misc.closeEnoughAbs(la1, other.la1, maxReletiveErrorPos * deltaLat))
        return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, other.lo1, maxReletiveErrorPos * deltaLon)) return false;
      if (!Misc.closeEnoughAbs(la2, other.la2, maxReletiveErrorPos * deltaLat)) return false;
      if (!Misc.closeEnoughAbs(lo2, other.lo2, maxReletiveErrorPos * deltaLon)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat1 = (int) Math.round(la1 / (maxReletiveErrorPos * deltaLat));  //  Two equal objects must have the same hashCode() value
        int useLon1 = (int) Math.round(lo1 / (maxReletiveErrorPos * deltaLon));
        int useLat2 = (int) Math.round(la2 / (maxReletiveErrorPos * deltaLat));
        int useLon2 = (int) Math.round(lo2 / (maxReletiveErrorPos * deltaLon));

        int result = super.hashCode();
        result = 31 * result + useLat1;
        result = 31 * result + useLon1;
        result = 31 * result + useLat2;
        result = 31 * result + useLon2;
        hashCode = result;
      }
      return hashCode;
    }

    protected float getScale() {
      if (basicAngle == 0 || basicAngle == GribNumbers.UNDEFINED || basicAngleSubdivisions == GribNumbers.UNDEFINED)
        return scale6;
      return ((float) basicAngle) / basicAngleSubdivisions;
    }

    public int[] getOptionalPoints() {
      int[] optionalPoints = null;
      int n = getOctet(11); // Number of octets for optional list of numbers
      if (n > 0) {
        optionalPoints = new int[n / 4];
        for (int i = 0; i < optionalPoints.length; i++)
          optionalPoints[i] = getOctet4(lastOctet + 4 * n);
      }
      return optionalPoints;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      LatLonProjection proj = new LatLonProjection(getEarth());
      //ProjectionPoint startP = proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      double startx = lo1; // startP.getX();
      double starty = la1; // startP.getY();
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, startx, deltaLon, starty, deltaLat,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      double Lo2 = lo2;
      if (Lo2 < lo1) Lo2 += 360;
      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      LatLonPointImpl endLL = new LatLonPointImpl(la2, Lo2);

      f.format("%s testProjection%n", getClass().getName());
      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) cs.proj.latLonToProj(endLL, new ProjectionPointImpl());
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      f.format("   should end at x= (%f,%f)%n", endx, endy);
    }

  }

  /*
Template 3.1 (Grid definition template 3.1 - rotated latitude/longitude (or equidistant cylindrical, or Plate Carre))
     1-4 (4): GDS length
     5-5 (1): Section
     6-6 (1): Source of Grid Definition (see code table 3.0)
    7-10 (4): Number of data points
   11-11 (1): Number of octects for optional list of numbers
   12-12 (1): Interpretation of list of numbers
   13-14 (2): Grid Definition Template Number
   15-72 (58): Same as grid definition template 3.0 - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt31n
   73-76 (4): Latitude of the southern pole of projection
   77-80 (4): Longitude of the southern pole of projection
   81-84 (4): Angle of rotation of projection
   85-nn (0): List of number of points along each meridian or parallel. - (These octets are only present for quasi-regular grids as described in Note 3)#GRIB2_6_0_1_temp.doc#G2_Gdt31n
   */
  public static class RotatedLatLon extends AbstractRotatedLatLon {

    RotatedLatLon(byte[] data) {
      super(data);
      template = 1;
      lastOctet = 85;
      float scale = getScale();
      float latSouthPole = getOctet4(73) * scale;
      float lonSouthPole = getOctet4(77) * scale;
      float angleRotation = getOctet4(81) * scale;
      if (angleRotation != 0) {
        throw new RuntimeException("Unsupported nonzero GRIB2 GDS template 1 angle of rotation: " + angleRotation);
      }
      latNorthPole = -LatLonPointImpl.latNormal(latSouthPole);
      lonNorthPole = LatLonPointImpl.lonNormal(lonSouthPole + 180);
      // la1/lo1/la2/lo2 are the grid corners in rotated coordinates,
      // as in COSMO test data; normalise to improve interoperability
      la1 = (float) LatLonPointImpl.latNormal(la1);
      lo1 = (float) LatLonPointImpl.lonNormal(lo1);
      la2 = (float) LatLonPointImpl.latNormal(la2);
      lo2 = (float) LatLonPointImpl.lonNormal(lo2);
      // if the corners wrap the rotated antimeridian or the domain does not
      // contain the origin, then something is very wrong: the reason rotated
      // latitude/longitude is used is to place the region of interest near
      // the origin
      if (la1 >= 0) {
        throw new RuntimeException("Unexpected nonnegative lower left rotated latitude: " + la1);
      }
      if (lo1 >= 0) {
        throw new RuntimeException("Unexpected nonnegative lower left rotated longitude: " + lo1);
      }
      if (la2 <= 0) {
        throw new RuntimeException("Unexpected nonpositive upper right rotated latitude: " + la2);
      }
      if (lo2 <= 0) {
        throw new RuntimeException("Unexpected nonpositive upper right rotated longitude: " + lo2);
      }
    }
  }

  public abstract static class AbstractRotatedLatLon extends LatLon {

    public double latNorthPole;
    public double lonNorthPole;

    AbstractRotatedLatLon(byte[] data) {
      super(data);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      AbstractRotatedLatLon other = (AbstractRotatedLatLon) o;
      return this.latNorthPole == other.latNorthPole && this.lonNorthPole == other.lonNorthPole;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (new Double(latNorthPole)).hashCode();
        result = 31 * result + (new Double(lonNorthPole)).hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      RotatedPole proj = new RotatedPole(latNorthPole, lonNorthPole);
      // LOOK dont transform - works for grib1 Q:/cdmUnitTest/transforms/HIRLAMhybrid.grib
      // LatLonPoint startLL = proj.projToLatLon(new ProjectionPointImpl(lo1, la1));
      //double startx = startLL.getLongitude();
      //double starty = startLL.getLatitude();
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, lo1, deltaLon, la1, deltaLat,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      LatLonPoint startLL = cs.proj.projToLatLon(new ProjectionPointImpl(lo1, la1));
      LatLonPoint endLL = cs.proj.projToLatLon(new ProjectionPointImpl(lo2, la2));

      f.format("%s testProjection%n", getClass().getName());
      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) cs.proj.latLonToProj(endLL, new ProjectionPointImpl());
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      f.format("   should end at x= (%f,%f)%n", endx, endy);
    }

  }

  /*
   * GRIB2 - GRID DEFINITION TEMPLATE 3.32769
   * Rotate Latitude/Longitude (Arakawa Non-E Staggered grid)
   * http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_temp3-32769.shtml
   * 
   * This grid is like template 1 (rotated lat/lon) except it has only 80 octets,
   * changed definition of La2 and Lo2, and is missing the projection south pole
   * and angle of rotation fields:
   * [...]
   * 56-59 La2—center latitude of grid point (see Note1)
   * 60-63 Lo2—Center longitude of grid point (see Note 1)
   * [...]
   * Notes: 4. The rotation of the Latitude/Longitude grid is such that
   * the intersection of the "prime meridian" and the "equator" has been
   * located at the central Latitude and Longitude at the area represented
   */
  public static class RotatedLatLon32769 extends AbstractRotatedLatLon {

    RotatedLatLon32769(byte[] data) {
      super(data);
      template = 32769;
      lastOctet = 81;
      // at this point the LatLon constructor has extracted la1/lo1/la2/lo2;
      // la1/lo1 is the lower left corner and la2/lo2 is the grid centre, both
      // in unrotated coordinates, so must use them to recalculate
      // la1/lo1/la2/lo2 as corners in rotated coordinates, as expected by
      // LatLon methods
      float latCentre = la2;
      float lonCentre = lo2;
      // position of north pole of rotated grid
      if (latCentre > 0) {
        latNorthPole = 90 - LatLonPointImpl.latNormal(latCentre);
        lonNorthPole = LatLonPointImpl.lonNormal(lonCentre + 180);
      } else {
        latNorthPole = 90 + LatLonPointImpl.latNormal(latCentre);
        lonNorthPole = LatLonPointImpl.lonNormal(lonCentre);
      }
      RotatedPole proj = new RotatedPole(latNorthPole, lonNorthPole);
      // recalculate la1/lo1/la2/lo2 in rotated coordinates
      LatLonPointImpl unrotated = new LatLonPointImpl(la1, lo1);
      ProjectionPointImpl rotated = new ProjectionPointImpl();
      proj.latLonToProj(unrotated, rotated);
      // expect grid centred on origin in rotated coordinates
      if (rotated.getX() >= 0) {
        throw new RuntimeException("Unexpected nonnegative lower left rotated longitude: " + rotated.getX());
      }
      if (rotated.getY() >= 0) {
        throw new RuntimeException("Unexpected nonnegative lower left rotated latitude: " + rotated.getY());
      }
      la1 = (float) rotated.getY();
      lo1 = (float) rotated.getX();
      // by symmetry about the centre
      la2 = -la1;
      lo2 = -lo1;
    }

  }

  /*
Template 3.10 (Grid definition template 3.10 - Mercator)
     1-4 (4): GDS length
     5-5 (1): Section
     6-6 (1): Source of Grid Definition (see code table 3.0)
    7-10 (4): Number of data points
   11-11 (1): Number of octects for optional list of numbers
   12-12 (1): Interpretation of list of numbers
   13-14 (2): Grid Definition Template Number
      15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
      16 (1): Scale factor of radius of spherical Earth
   17-20 (4): Scaled value of radius of spherical Earth
      21 (1): Scale factor of major axis of oblate spheroid Earth
   22-25 (4): Scaled value of major axis of oblate spheroid Earth
      26 (1): Scale factor of minor axis of oblate spheroid Earth
   27-30 (4): Scaled value of minor axis of oblate spheroid Earth
   31-34 (4): Ni - number of points along a parallel
   35-38 (4): Nj - number of points along a meridian
   39-42 (4): La1 - latitude of first grid point
   43-46 (4): Lo1 - longitude of first grid point
      47 (1): Resolution and component flags - (see Flag table 3.3)#GRIB2_6_0_1_codeflag.doc#G2_CF33
   48-51 (4): LaD - latitude(s) at which the Mercator projection intersects the Earth (Latitude(s) where Di and Dj are specified)
   52-55 (4): La2 - latitude of last grid point
   56-59 (4): Lo2 - longitude of last grid point
      60 (1): Scanning mode - (flags - see Flag table 3.4)#GRIB2_6_0_1_codeflag.doc#G2_CF34
   61-64 (4): Orientation of the grid, angle between i direction on the map and the Equator - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt310n
   65-68 (4): Di - longitudinal direction grid length - (see Note 2)#GRIB2_6_0_1_temp.doc#G2_Gdt310n
   69-72 (4): Dj - latitudinal direction grid length - (see Note 2)#GRIB2_6_0_1_temp.doc#G2_Gdt310n
   73-nn (0): List of number of points along each meridian or parallel. - (These octets are only present for quasi-regular grids as described in Notes 2 and 3 of GDT 3.1)#GRIB2_6_0_1_temp.doc#G2_Gdt310n
   */
  public static class Mercator extends Grib2Gds {
    public float la1, lo1, la2, lo2, lad, dX, dY;
    public int flags;

    Mercator(byte[] data) {
      super(data, 10);

      la1 = getOctet4(39) * scale6;
      lo1 = getOctet4(43) * scale6;
      flags = getOctet(47);
      lad = getOctet4(48) * scale6;
      la2 = getOctet4(52) * scale6;
      lo2 = getOctet4(56) * scale6;

      scanMode = getOctet(60);

      // float orient = getOctet4(61) * scale6; // LOOK not sure if should be scaled
      dX = getOctet4(65) * scale6;  // km
      dY = getOctet4(69) * scale6;  // km

      lastOctet = 73;
    }

    public void testScanMode(Formatter f) {
      float scale = scale6;
      float firstLat = getOctet4(39) * scale;
      float lastLat = getOctet4(52) * scale;
      float dY = getOctet4(69) * scale;       // may be pos or neg
      if (GribUtils.scanModeYisPositive(scanMode)) {
        if (firstLat > lastLat)
          f.format("  **Mercator scan mode=%d dY=%f lat=(%f,%f)%n", scanMode, dY, firstLat, lastLat);
      } else {
        if (firstLat < lastLat)
          f.format("  **Mercator scan mode=%d dY=%f lat=(%f,%f)%n", scanMode, dY, firstLat, lastLat);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Mercator that = (Mercator) o;

      if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY))
        return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, that.lo1, maxReletiveErrorPos * dX)) return false;
      if (!Misc.closeEnoughAbs(lad, that.lad, maxReletiveErrorPos * dY)) return false;
      if (!Misc.closeEnough(dY, that.dY)) return false;
      if (!Misc.closeEnough(dX, that.dX)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat = (int) Math.round(la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) Math.round(lo1 / (maxReletiveErrorPos * dX));
        int useLad = (int) Math.round(lad / (maxReletiveErrorPos * dY));
        int useDeltaLon = (int) Math.round(dX / Misc.maxReletiveError);
        int useDeltaLat = (int) Math.round(dY / Misc.maxReletiveError);

        int result = super.hashCode();
        result = 31 * result + useLat;
        result = 31 * result + useLon;
        result = 31 * result + useLad;
        result = 31 * result + useDeltaLon;
        result = 31 * result + useDeltaLat;
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      // put longitude origin at first point - doesnt actually matter
      // param par standard parallel (degrees). cylinder cuts earth at this latitude.
      Earth earth = getEarth();
      ucar.unidata.geoloc.projection.Mercator proj = new ucar.unidata.geoloc.projection.Mercator(lo1, lad, 0, 0, earth.getEquatorRadius() * .001);

      // find out where things start
      ProjectionPoint startP = proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      double startx = startP.getX();
      double starty = startP.getY();

      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, startx, dX, starty, dY,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }


    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      double Lo2 = lo2;
      if (Lo2 < lo1) Lo2 += 360;
      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      LatLonPointImpl endLL = new LatLonPointImpl(la2, Lo2);

      f.format("%s testProjection%n", getClass().getName());
      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) cs.proj.latLonToProj(endLL, new ProjectionPointImpl());
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      f.format("   should end at x= (%f,%f)%n", endx, endy);
    }

  }

  /*
Template 3.20 (Grid definition template 3.20 - polar stereographic projection)
     1-4 (4): GDS length
     5-5 (1): Section
     6-6 (1): Source of Grid Definition (see code table 3.0)
    7-10 (4): Number of data points
   11-11 (1): Number of octects for optional list of numbers
   12-12 (1): Interpretation of list of numbers
   13-14 (2): Grid Definition Template Number
      15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
      16 (1): Scale factor of radius of spherical Earth
   17-20 (4): Scaled value of radius of spherical Earth
      21 (1): Scale factor of major axis of oblate spheroid Earth
   22-25 (4): Scaled value of major axis of oblate spheroid Earth
      26 (1): Scale factor of minor axis of oblate spheroid Earth
   27-30 (4): Scaled value of minor axis of oblate spheroid Earth
   31-34 (4): Nx - number of points along the x-axis
   35-38 (4): Ny - number of points along the y-axis
   39-42 (4): La1 - latitude of first grid point
   43-46 (4): Lo1 - longitude of first grid point
      47 (1): Resolution and component flags - (see Flag table 3.3 and Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt320n
   48-51 (4): LaD - latitude where Dx and Dy are specified
   52-55 (4): LoV - orientation of the grid - (see Note 2)#GRIB2_6_0_1_temp.doc#G2_Gdt320n
   56-59 (4): Dx - x-direction grid length - (see Note 3)#GRIB2_6_0_1_temp.doc#G2_Gdt320n
   60-63 (4): Dy - y-direction grid length - (see Note 3)#GRIB2_6_0_1_temp.doc#G2_Gdt320n
      64 (1): Projection centre flag - (see Flag table 3.5)#GRIB2_6_0_1_codeflag.doc#G2_CF35
      65 (1): Scanning mode - (see Flag table 3.4)#GRIB2_6_0_1_codeflag.doc#G2_CF34
   */
  public static class PolarStereographic extends Grib2Gds {
    public float la1, lo1, lov, lad, dX, dY;
    public int flags, projCenterFlag;

    PolarStereographic(byte[] data) {
      super(data, 20);

      la1 = getOctet4(39) * scale6;
      lo1 = getOctet4(43) * scale6;
      flags = getOctet(47);
      lad = getOctet4(48) * scale6;
      lov = getOctet4(52) * scale6;

      dX = getOctet4(56) * scale6;  //  km
      dY = getOctet4(60) * scale6;  //  km

      projCenterFlag = getOctet(64);
      scanMode = getOctet(65);
    }

    public void testScanMode(Formatter f) {
      float scale = scale6;
      float dY = getOctet4(60) * scale;       // may be pos or neg
      if (GribUtils.scanModeYisPositive(scanMode)) {
        if (dY < 0) f.format("  **PS scan mode=%d dY=%f%n", scanMode, dY);
      } else {
        if (dY > 0) f.format("  **PS scan mode=%d dY=%f%n", scanMode, dY);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      PolarStereographic that = (PolarStereographic) o;

      if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY))
        return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, that.lo1, maxReletiveErrorPos * dX)) return false;
      if (!Misc.closeEnough(lad, that.lad)) return false;
      if (!Misc.closeEnough(lov, that.lov)) return false;
      if (!Misc.closeEnough(dY, that.dY)) return false;
      if (!Misc.closeEnough(dX, that.dX)) return false;

      if (projCenterFlag != that.projCenterFlag) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat = (int) Math.round(la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) Math.round(lo1 / (maxReletiveErrorPos * dX));
        int useLad = (int) Math.round(lad / Misc.maxReletiveError);
        int useLov = (int) Math.round(lov / Misc.maxReletiveError);
        int useDeltaLon = (int) Math.round(dX / Misc.maxReletiveError);
        int useDeltaLat = (int) Math.round(dY / Misc.maxReletiveError);

        int result = super.hashCode();
        result = 31 * result + useLat;
        result = 31 * result + useLon;
        result = 31 * result + useLad;
        result = 31 * result + useLov;
        result = 31 * result + useDeltaLon;
        result = 31 * result + useDeltaLat;
        result = 31 * result + projCenterFlag;
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      boolean northPole = (projCenterFlag & 128) == 0;
      double latOrigin = northPole ? 90.0 : -90.0;

      // Why the scale factor?. according to GRIB docs:
      // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
      // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
      // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
      double scale;
      if (GribNumbers.isUndefined(lad)) { // LOOK
        scale = 0.9330127018922193;
      } else {
        scale = (1.0 + Math.sin(Math.toRadians(Math.abs(Math.abs(lad))))) / 2;
      }

      ProjectionImpl proj;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new Stereographic(latOrigin, lov, scale);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection(
                latOrigin, lov, scale, lad, 0.0, 0.0, earth);
      }

      ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, start.getX(), dX, start.getY(), dY,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      f.format("%s testProjection %s%n", getClass().getName(), cs.proj.getClass().getName());

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      ProjectionPointImpl endPP = new ProjectionPointImpl(endx, endy);
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      LatLonPoint endLL = cs.proj.projToLatLon(endPP, new LatLonPointImpl());

      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);
    }

  }

  /*
Template 3.30 (Grid definition template 3.30 - Lambert conformal)
     1-4 (4): GDS length
     5-5 (1): Section
     6-6 (1): Source of Grid Definition (see code table 3.0)
    7-10 (4): Number of data points
   11-11 (1): Number of octects for optional list of numbers
   12-12 (1): Interpretation of list of numbers
   13-14 (2): Grid Definition Template Number
      15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
      16 (1): Scale factor of radius of spherical Earth
   17-20 (4): Scaled value of radius of spherical Earth
      21 (1): Scale factor of major axis of oblate spheroid Earth
   22-25 (4): Scaled value of major axis of oblate spheroid Earth
      26 (1): Scale factor of minor axis of oblate spheroid Earth
   27-30 (4): Scaled value of minor axis of oblate spheroid Earth
   31-34 (4): Nx - number of points along the x-axis
   35-38 (4): Ny - number of points along the y-axis
   39-42 (4): La1 - latitude of first grid point
   43-46 (4): Lo1 - longitude of first grid point
      47 (1): Resolution and component flags - (see Flag table 3.3)#GRIB2_6_0_1_codeflag.doc#G2_CF33
   48-51 (4): LaD - latitude where Dx and Dy are specified
   52-55 (4): LoV - longitude of meridian parallel to y-axis along which latitude increases as the y-coordinate increases
   56-59 (4): Dx - x-direction grid length - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt330n
   60-63 (4): Dy - y-direction grid length - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt330n
      64 (1): Projection centre flag - (see Flag table 3.5)#GRIB2_6_0_1_codeflag.doc#G2_CF35
      65 (1): Scanning mode - (see Flag table 3.4)#GRIB2_6_0_1_codeflag.doc#G2_CF34
   66-69 (4): Latin 1 - first latitude from the pole at which the secant cone cuts the sphere
   70-73 (4): Latin 2 - second latitude from the pole at which the secant cone cuts the sphere
   74-77 (4): Latitude of the southern pole of projection
   78-81 (4): Longitude of the southern pole of projection
   */
  public static class LambertConformal extends Grib2Gds {
    public float la1, lo1, lov, lad, dX, dY, latin1, latin2, latSouthPole, lonSouthPole;
    public int flags, projCenterFlag;

    LambertConformal(byte[] data, int template) {
      super(data, template);

      // floating point values
      la1 = getOctet4(39) * scale6;
      lo1 = getOctet4(43) * scale6;
      flags = getOctet(47);
      lad = getOctet4(48) * scale6;
      lov = getOctet4(52) * scale6;

      dX = getOctet4(56) * scale6; // km
      dY = getOctet4(60) * scale6; // km

      projCenterFlag = getOctet(64);
      scanMode = getOctet(65);

      latin1 = getOctet4(66) * scale6;
      latin2 = getOctet4(70) * scale6;
      latSouthPole = getOctet4(74) * scale6;
      lonSouthPole = getOctet4(78) * scale6;
    }

    public void testScanMode(Formatter f) {
      float scale = scale6;
      float dY = getOctet4(60) * scale;       // may be pos or neg
      if (GribUtils.scanModeYisPositive(scanMode)) {
        if (dY < 0) f.format("  **LC scan mode=%d dY=%f%n", scanMode, dY);
      } else {
        if (dY > 0) f.format("  **LC scan mode=%d dY=%f%n", scanMode, dY);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      LambertConformal that = (LambertConformal) o;

      if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY))
        return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, that.lo1, maxReletiveErrorPos * dX)) return false;
      if (!Misc.closeEnough(lad, that.lad)) return false;
      if (!Misc.closeEnough(lov, that.lov)) return false;
      if (!Misc.closeEnough(dY, that.dY)) return false;
      if (!Misc.closeEnough(dX, that.dX)) return false;
      if (!Misc.closeEnough(latin1, that.latin1)) return false;
      if (!Misc.closeEnough(latin2, that.latin2)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat = (int) Math.round(la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) Math.round(lo1 / (maxReletiveErrorPos * dX));
        int useLad = (int) Math.round(lad / Misc.maxReletiveError);
        int useLov = (int) Math.round(lov / Misc.maxReletiveError);
        int useDeltaLon = (int) Math.round(dX / Misc.maxReletiveError);
        int useDeltaLat = (int) Math.round(dY / Misc.maxReletiveError);
        int useLatin1 = (int) Math.round(latin1 / Misc.maxReletiveError);
        int useLatin2 = (int) Math.round(latin2 / Misc.maxReletiveError);

        int result = super.hashCode();
        result = 31 * result + useLat;
        result = 31 * result + useLon;
        result = 31 * result + useLad;
        result = 31 * result + useLov;
        result = 31 * result + useDeltaLon;
        result = 31 * result + useDeltaLat;
        result = 31 * result + useLatin1;
        result = 31 * result + useLatin2;
        result = 31 * result + projCenterFlag;

        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      ProjectionImpl proj;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new ucar.unidata.geoloc.projection.LambertConformal(latin1, lov, latin1, latin2, 0.0, 0.0, earth.getEquatorRadius() * .001);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse(
                latin1, lov, latin1, latin2, 0.0, 0.0, earth);
      }

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(startLL);
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, start.getX(), dX, start.getY(), dY,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      f.format("%s testProjection %s%n", getClass().getName(), cs.proj.getClass().getName());

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      ProjectionPointImpl endPP = new ProjectionPointImpl(endx, endy);
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      LatLonPoint endLL = cs.proj.projToLatLon(endPP, new LatLonPointImpl());

      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);
    }

  }

  /*
    Template 3.31 (Grid definition template 3.31 - Albers equal area)
         1-4 (4): GDS length
         5-5 (1): Section
         6-6 (1): Source of Grid Definition (see code table 3.0)
        7-10 (4): Number of data points
       11-11 (1): Number of octects for optional list of numbers
       12-12 (1): Interpretation of list of numbers
       13-14 (2): Grid Definition Template Number
          15 (1): Shape of the Earth - (see Code table 3.2)
          16 (1): Scale factor of radius of spherical Earth
       17-20 (4): Scaled value of radius of spherical Earth
          21 (1): Scale factor of major axis of oblate spheroid Earth
       22-25 (4): Scaled value of major axis of oblate spheroid Earth
          26 (1): Scale factor of minor axis of oblate spheroid Earth
       27-30 (4): Scaled value of minor axis of oblate spheroid Earth
       31-34 (4): Nx - number of points along the x-axis
       35-38 (4): Ny - number of points along the y-axis
       39-42 (4): La1 - latitude of first grid point
       43-46 (4): Lo1 - longitude of first grid point
          47 (1): Resolution and component flags - (see Flag table 3.3)
       48-51 (4): LaD - latitude where Dx and Dy are specified
       52-55 (4): LoV - longitude of meridian parallel to y-axis along which latitude increases as the y-coordinate increases
       56-59 (4): Dx - x-direction grid length - (see Note 1)
       60-63 (4): Dy - y-direction grid length - (see Note 1)
          64 (1): Projection centre flag - (see Flag table 3.5)
          65 (1): Scanning mode - (see Flag table 3.4)
       66-69 (4): Latin 1 - first latitude from the pole at which the secant cone cuts the sphere
       70-73 (4): Latin 2 - second latitude from the pole at which the secant cone cuts the sphere
       74-77 (4): Latitude of the southern pole of projection
       78-81 (4): Longitude of the southern pole of projection

      Notes:
      (1) Grid lengths are in units of 10–3 m, at the latitude specified by LaD.
      (2) If Latin 1 = Latin 2, then the projection is on a tangent cone.
      (3) The resolution flags (bits 3–4 of Flag table 3.3) are not applicable.
      (4) LoV is the longitude value of the meridian which is parallel to the y-axis (or columns of the grid) along which latitude
      increases as the y-coordinate increases (the orientation longitude may or may not appear on a particular grid).
      (5) A scaled value of radius of spherical Earth, or major or minor axis of oblate spheroid Earth, is derived by applying the
      appropriate scale factor to the value expressed in metres.
   */
  public static class AlbersEqualArea extends LambertConformal {

    AlbersEqualArea(byte[] data) {
      super(data, 31);
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      ProjectionImpl proj;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new ucar.unidata.geoloc.projection.AlbersEqualArea(latin1, lov, latin1, latin2, 0.0, 0.0, earth.getEquatorRadius() * .001);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse(
                latin1, lov, latin1, latin2, 0.0, 0.0, earth);
      }

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(startLL);
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, start.getX(), dX, start.getY(), dY,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      f.format("%s testProjection %s%n", getClass().getName(), cs.proj.getClass().getName());

      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      ProjectionPointImpl endPP = new ProjectionPointImpl(endx, endy);
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      LatLonPoint endLL = cs.proj.projToLatLon(endPP, new LatLonPointImpl());

      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);
    }

  }

  /*
(3.40) Grid definition template 3.40 - Gaussian latitude/longitude
Template 3.40 (Grid definition template 3.40 - Gaussian latitude/longitude)
     1-4 (4): GDS length
     5-5 (1): Section
     6-6 (1): Source of Grid Definition (see code table 3.0)
    7-10 (4): Number of data points
   11-11 (1): Number of octects for optional list of numbers
   12-12 (1): Interpretation of list of numbers
   13-14 (2): Grid Definition Template Number
      15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
      16 (1): Scale factor of radius of spherical Earth
   17-20 (4): Scaled value of radius of spherical Earth
      21 (1): Scale factor of major axis of oblate spheroid Earth
   22-25 (4): Scaled value of major axis of oblate spheroid Earth
      26 (1): Scale factor of minor axis of oblate spheroid Earth
   27-30 (4): Scaled value of minor axis of oblate spheroid Earth
   31-34 (4): Ni - number of points along a parallel
   35-38 (4): Nj - number of points along a meridian
   39-42 (4): Basic angle of the initial production domain - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   43-46 (4): Subdivisions of basic angle used to define extreme longitudes and latitudes, and direction increments - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   47-50 (4): La1 - latitude of first grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   51-54 (4): Lo1 - longitude of first grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
      55 (1): Resolution and component flags - (see Flag table 3.3)#GRIB2_6_0_1_codeflag.doc#G2_CF33
   56-59 (4): La2 - latitude of last grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   60-63 (4): Lo2 - longitude of last grid point - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   64-67 (4): Di - i direction increment - (see Note 1)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   68-71 (4): N - number of parallels between a pole and the Equator - (see Note 2)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
      72 (1): Scanning mode - (flags - see Flag table 3.4)#GRIB2_6_0_1_codeflag.doc#G2_CF34
   73-nn (0): List of number of points along each meridian or parallel. - (These octets are only present for quasi-regular grids as described in Note 4)#GRIB2_6_0_1_temp.doc#G2_Gdt340n
   */
  public static class GaussLatLon extends LatLon {
    public int Nparellels;

    GaussLatLon(byte[] data) {
      super(data);
      this.template = 40;
      Nparellels = getOctet4(68);
    }

    /*
case cfsr:
 31:                                                     Ni - number of points along a parallel == 1152
 35:                                                     Nj - number of points along a meridian == 576
 39:                                               Basic angle of the initial production domain == 0
 43: Subdivisions of basic angle used to define extreme longitudes and latitudes, and direction increments == 0
 47:                                                         La1 - latitude of first grid point == 89761000
 51:                                                        Lo1 - longitude of first grid point == 0
 55:                                                             Resolution and component flags == 48
 56:                                                          La2 - latitude of last grid point == -89761000
 60:                                                         Lo2 - longitude of last grid point == 359688000
 64:                                                                 Di - i direction increment == 313000
 68:                                     N - number of parallels between a pole and the Equator == 288

some records differ only by:
 60:                                                         Lo2 - longitude of last grid point == 359687000

 note that  1152 * .313 = 360.576
            359.688000 / (1152-1) = 0.31250043440486533449174630755864
            359.687000 / (1152-1) = 0.31249956559513466550825369244136

 so we need to tolerate .001 < toler * .3125, toler > 1/312, so set to 1/100
     */

    protected void finish() {
      super.finish();
      deltaLon = (lo2 - lo1) / (getNx() - 1); // more accurate - deltaLon may have roundoff
      deltaLat = 0.1f; // meaningless for gaussian
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      GaussLatLon that = (GaussLatLon) o;

      if (Nparellels != that.Nparellels) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + Nparellels;
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {

      /* int nlats = 2 * Nparellels;
      GaussianLatitudes gaussLats = new GaussianLatitudes(nlats);

      int bestStartIndex = 0, bestEndIndex = 0;
      double bestStartDiff = Double.MAX_VALUE;
      double bestEndDiff = Double.MAX_VALUE;
      for (int i = 0; i < nlats; i++) {
        double diff = Math.abs(gaussLats.latd[i] - la1);
        if (diff < bestStartDiff) {
          bestStartDiff = diff;
          bestStartIndex = i;
        }
        diff = Math.abs(gaussLats.latd[i] - la2);
        if (diff < bestEndDiff) {
          bestEndDiff = diff;
          bestEndIndex = i;
        }
      }
      int useNy = getNy();
      if (Math.abs(bestEndIndex - bestStartIndex + 1) != useNy) {
        log.warn("GRIB gaussian lats: NP != NY, use NY");  // see email from Toussaint@dkrz.de datafil:
        nlats = useNy;
        gaussLats = new GaussianLatitudes(nlats);
        bestStartIndex = 0;
        bestEndIndex = useNy - 1;
      }
      boolean goesUp = bestEndIndex > bestStartIndex;

      // create the data
      int useIndex = bestStartIndex;
      float[] data = new float[useNy];
      float[] gaussw = new float[useNy];
      for (int i = 0; i < useNy; i++) {
        data[i] = (float) gaussLats.latd[useIndex];
        gaussw[i] = (float) gaussLats.gaussw[useIndex];
        if (goesUp) {
          useIndex++;
        } else {
          useIndex--;
        }
      }  */

      GdsHorizCoordSys coordSys = new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, new LatLonProjection(), lo1, deltaLon, 0, 0,
              getNxRaw(), getNyRaw(), getNptsInLine());
      coordSys.setGaussianLats(Nparellels, la1, la2);

      return coordSys;
    }

    public void testHorizCoordSys(Formatter f) {
      GdsHorizCoordSys cs = makeHorizCoordSys();
      f.format("%s testProjection %s%n", getClass().getName(), cs.proj.getClass().getName());
    }

  }

  /*
Template 3.90 (Grid definition template 3.90 - space view perspective or orthographic)
    1-4 (4): GDS length
    5-5 (1): Section
    6-6 (1): Source of Grid Definition (see code table 3.0)
   7-10 (4): Number of data points
  11-11 (1): Number of octects for optional list of numbers
  12-12 (1): Interpretation of list of numbers
  13-14 (2): Grid Definition Template Number
     15 (1): Shape of the Earth - (see Code table 3.2)#GRIB2_6_0_1_codeflag.doc#G2_CF32
     16 (1): Scale factor of radius of spherical Earth
  17-20 (4): Scaled value of radius of spherical Earth
     21 (1): Scale factor of major axis of oblate spheroid Earth
  22-25 (4): Scaled value of major axis of oblate spheroid Earth
     26 (1): Scale factor of minor axis of oblate spheroid Earth
  27-30 (4): Scaled value of minor axis of oblate spheroid Earth
  31-34 (4): Nx - number of points along x-axis (columns)
  35-38 (4): Ny - number of points along y-axis (rows or lines)
  39-42 (4): Lap - latitude of sub-satellite point
  43-46 (4): Lop - longitude of sub-satellite point
     47 (1): Resolution and component flags - (see Flag table 3.3)#GRIB2_6_0_1_codeflag.doc#G2_CF33
  48-51 (4): dx - apparent diameter of Earth in grid lengths, in x-direction
  52-55 (4): dy - apparent diameter of Earth in grid lengths, in y-direction
  56-59 (4): Xp - x-coordinate of sub-satellite point (in units of 10-3 grid length expressed as an integer)
  60-63 (4): Yp - y-coordinate of sub-satellite point (in units of 10-3 grid length expressed as an integer)
     64 (1): Scanning mode (flags - see Flag table 3.4)
  65-68 (4): Orientation of the grid; i.e. the angle between the increasing y-axis and the meridian of the sub-satellite point in the direction of increasing latitude - (see Note 3)#GRIB2_6_0_1_temp.doc#G2_Gdt390n
  69-72 (4): Nr - altitude of the camera from the Earths centre, measured in units of the Earths (equatorial) radius multiplied by a scale factor of 106 - (see Notes 4 and 5)#GRIB2_6_0_1_temp.doc#G2_Gdt390n
  73-76 (4): Xo - x-coordinate of origin of sector image
  77-80 (4): Yo - y-coordinate of origin of sector image

      Notes:
   (1) It is assumed that the satellite is at its nominal position, i.e., it is looking directly at its sub-satellite point.
   (2) Octets 69-72 shall be set to all ones (missing) to indicate the orthographic view (from infinite distance)
   (3) It is the angle between the increasing Y-axis and the meridian 180E if the sub-satellite point is the North Pole; or the meridian 0 if the sub-satellite point is the South Pole.
   (4) The apparent angular size of the Earth will be given by 2 * Arcsin (10^6 )/Nr).
   (5) For orthographic view from infinite distance, the value of Nr should be encoded as missing (all bits set to 1).
   (6) The horizontal and vertical angular resolutions of the sensor (Rx and Ry), needed for navigation equation, can be calculated from the following:
        Rx = 2 * Arcsin (106 )/Nr)/ dx
        Ry = 2 * Arcsin (106 )/Nr)/ dy
  */
  public static class SpaceViewPerspective extends Grib2Gds {
    public float LaP, LoP, dX, dY, Xp, Yp, Nr, Xo, Yo;
    public int flags;

    SpaceViewPerspective(byte[] data) {
      super(data, 90);

      LaP = getOctet4(39) * scale6;
      LoP = getOctet4(43) * scale6;
      flags = getOctet(47);

      dX = getOctet4(48);
      dY = getOctet4(52);

      Xp = getOctet4(56) * scale3;
      Yp = getOctet4(60) * scale3;

      scanMode = getOctet(64);

      // float orient = getOctet4(65) * scale6;  // LOOK dunno about scale
      Nr = getOctet4(69) * scale6;
      Xo = getOctet4(73) * scale6;
      Yo = getOctet4(77) * scale6;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      SpaceViewPerspective that = (SpaceViewPerspective) o;

      if (Float.compare(that.LaP, LaP) != 0) return false;
      if (Float.compare(that.LoP, LoP) != 0) return false;
      if (Float.compare(that.Nr, Nr) != 0) return false;
      if (Float.compare(that.Xo, Xo) != 0) return false;
      if (Float.compare(that.Xp, Xp) != 0) return false;
      if (Float.compare(that.Yo, Yo) != 0) return false;
      if (Float.compare(that.Yp, Yp) != 0) return false;
      if (Float.compare(that.dX, dX) != 0) return false;
      if (Float.compare(that.dY, dY) != 0) return false;
      if (flags != that.flags) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (LaP != +0.0f ? Float.floatToIntBits(LaP) : 0);
        result = 31 * result + (LoP != +0.0f ? Float.floatToIntBits(LoP) : 0);
        result = 31 * result + (dX != +0.0f ? Float.floatToIntBits(dX) : 0);
        result = 31 * result + (dY != +0.0f ? Float.floatToIntBits(dY) : 0);
        result = 31 * result + (Xp != +0.0f ? Float.floatToIntBits(Xp) : 0);
        result = 31 * result + (Yp != +0.0f ? Float.floatToIntBits(Yp) : 0);
        result = 31 * result + (Nr != +0.0f ? Float.floatToIntBits(Nr) : 0);
        result = 31 * result + (Xo != +0.0f ? Float.floatToIntBits(Xo) : 0);
        result = 31 * result + (Yo != +0.0f ? Float.floatToIntBits(Yo) : 0);
        result = 31 * result + flags;
        hashCode = result;
      }
      return hashCode;
    }

    /**
     * Make a Eumetsat MSG "Normalized Geostationary Projection" projection.
     * Fake coordinates for now, then see if this can be generalized.
     * <p>
     * =======
     * <p>
     * from  simon.elliott@eumetsat.int
     * <p>
     * For products on a single pixel resolution grid, the scan angle is 83.84333 E-6 rad.
     * So dx = 2 * arcsin(10e6/Nr) / 83.84333 E-6 = 3622.30, which encoded to the nearest integer is 3622.
     * This is correctly encoded in our products.
     * <p>
     * For products on a 3x3 pixel resolution grid, the scan angle is 3 * 83.84333 E-6 rad = 251.52999 E-6 rad.
     * So dx = 2 * arcsin(10e6/Nr) / 251.52999 E-6 = 1207.43, which encoded to the nearest integer is 1207.
     * This is correctly encoded in our products.
     * <p>
     * Due to the elliptical shape of the earth, the calculation is a bit different in the y direction (Nr is in multiples of
     * the equatorial radius, but the tangent point is much closer to the polar radius from the earth's centre.
     * Approximating that the tangent point is actually at the polar radius from the earth's centre:
     * The sine of the angle subtended by the Earths centre and the tangent point on the equator as seen from the spacecraft
     * = Rp / (( Nr * Re) / 10^6) = (Rp * 10^6) / (Re * Nr)
     * <p>
     * The angle subtended by the Earth equator as seen by the spacecraft is, by symmetry twice the inverse sine above,
     * = 2 * arcsine ((Rp * 10^6) / (Re * Nr))
     * <p>
     * For products on a single pixel resolution grid, the scan angle is 83.84333 E-6 rad.
     * So dy = 2 * arcsine ((Rp * 10^6) / (Re * Nr)) / 83.84333 E-6 = 3610.06, which encoded to the nearest integer is 3610.
     * This is currently encoded in our products as 3568.
     * <p>
     * For products on a 3x3 pixel resolution grid, the scan angle is 3 * 83.84333 E-6 rad = 251.52999 E-6 rad.
     * So dy = 2 * arcsine ((Rp * 10^6) / (Re * Nr)) / 251.52999 E-6 = 1203.35, which encoded to the nearest integer is 1203.
     * This is currently encoded in our products as 1189.
     * <p>
     * As you can see the dx and dy values we are using will lead to an error of around 1% in the y direction.
     * I will ensure that the values are corrected to those explained here (3610 and 1203) as soon as possible.
     */
    public GdsHorizCoordSys makeHorizCoordSys() {

      // per Simon Eliot 1/18/2010, there is a bug in Eumetsat grib files,
      // we need to "correct for ellipsoidal earth"
      // "Originating_center" = 254 "EUMETSAT Operation Centre" in the GRIB id (section 1))
      if (center == 254) {
        if (dY < 2100) {
          dX = 1207;
          dY = 1203;
        } else {
          dX = 3622;
          dY = 3610;
        }
      }

      // CFAC = 2^16 / {[2 * arcsine (10^6 / Nr)] / dx }
      double as = 2 * Math.asin(1.0 / Nr);
      double cfac = dX / as;
      double lfac = dY / as;

      getEarth(); // fix units if needed

      // use km, so scale by the earth radius
      double scale_factor = (Nr - 1) * majorAxis / 1000; // this sets the units of the projection x,y coords in km
      double scale_x = scale_factor; // LOOK fake neg need scan value
      double scale_y = -scale_factor; // LOOK fake neg need scan value

      double startx = scale_factor * (1 - Xp) / cfac;
      double incrx = scale_factor / cfac;


      /*
      // scanMode
      boolean xscanPositive = (scanMode & GribNumbers.bitmask[0]) == 0;  // 0 Points of first row or column scan in the +i (+x) direction

      double startx, incrx;
      if (xscanPositive) {
        incrx =  -scale_factor / cfac;
        startx = (scale_factor * (1 - Xp) / cfac) - incrx * (getNx()-1);
      } else {
        startx = scale_factor * (1 - Xp) / cfac;
        incrx = scale_factor / cfac;
      }  */

      boolean yscanPositive = GribUtils.scanModeYisPositive(scanMode);
      double starty, incry;
      if (yscanPositive) {
        starty = scale_factor * (Yp - getNy()) / lfac;
        incry = (scale_factor / lfac);
      } else {
        incry = -(scale_factor / lfac);
        starty = scale_factor * (Yp - getNy()) / lfac - incry * (getNy() - 1);
      }

      MSGnavigation proj = new MSGnavigation(LaP, LoP, majorAxis, minorAxis, Nr * majorAxis, scale_x, scale_y);
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, startx, incrx, starty, incry,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
      f.format("%s testProjection%n", getClass().getName());

      GdsHorizCoordSys cs = makeHorizCoordSys();
      double endx = cs.startx + (getNx() - 1) * cs.dx;
      double endy = cs.starty + (getNy() - 1) * cs.dy;
      ProjectionPointImpl endPP = new ProjectionPointImpl(endx, endy);
      f.format("   start at proj coord= %s%n", new ProjectionPointImpl(cs.startx, cs.starty));
      f.format("     end at proj coord= %s%n", endPP);

      LatLonPointImpl startLL = new LatLonPointImpl(LaP, LoP);
      LatLonPoint endLL = cs.proj.projToLatLon(endPP, new LatLonPointImpl());

      f.format("  start at latlon= %s%n", startLL);
      f.format("    end at latlon= %s%n", endLL);
    }

  }

  /*
  Curvilinear Orthogonal Grids (NCEP grid 204)
  see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table3-1.shtml

  Octet	Contents
    15 Shape of the Earth (See Table 3.2)
    16 Scale Factor of radius of spherical Earth
    17-20 Scale value of radius of spherical Earth
    21 Scale factor of major axis of oblate spheroid Earth
    22-25 Scaled value of major axis of oblate spheroid Earth
    26 Scale factor of minor axis of oblate spheroid Earth
    27-30 Scaled value of minor axis of oblate spheroid Earth
    31-34 Ni number of points along a parallel
    35-38 Nj number of points along a meridian
    39-54 Reserved (set to zero)
    55 Resolution and component flags (see Table 3.3)
    56-71 Reserved (set to zero)
    72 Scanning mode (flags  see Table 3.4)
   */

  public static class CurvilinearOrthogonal extends Grib2Gds {
    public int flags;

    CurvilinearOrthogonal(byte[] data) {
      super(data, 204);

      flags = getOctet(55);
      scanMode = getOctet(72);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      CurvilinearOrthogonal that = (CurvilinearOrthogonal) o;

      if (flags != that.flags) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + flags;
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      LatLonProjection proj = new LatLonProjection();
      return new GdsHorizCoordSys(getNameShort(), template, getOctet4(7), scanMode, proj, 0, 1, 0, 1,
              getNxRaw(), getNyRaw(), getNptsInLine());
    }

    public void testHorizCoordSys(Formatter f) {
    }
  }

}
