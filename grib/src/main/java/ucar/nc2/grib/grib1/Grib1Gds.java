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

import com.google.common.base.MoreObjects;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Stereographic;

import java.util.Arrays;
import java.util.Formatter;

/**
 * GRIB1 GDS - subclass for each type
 * <p/>
 * <ul>
 * <li>0 Latitude/longitude grid – equidistant cylindrical or Plate Carrée projection
 * <li>1 Mercator projection
 * <li>2 Gnomonic projection
 * <li>3 Lambert conformal, secant or tangent, conic or bi-polar, projection
 * <li>4 Gaussian latitude/longitude grid
 * <li>5 Polar stereographic projection
 * <li>6 Universal Transverse Mercator (UTM) projection
 * <li>7 Simple polyconic projection
 * <li>8 Albers equal-area, secant or tangent, conic or bi-polar, projection
 * <li>9 Miller’s cylindrical projection
 * <li>10 Rotated latitude/longitude grid
 * <li>13 Oblique Lambert conformal, secant or tangent, conic or bi-polar, projection
 * <li>14 Rotated Gaussian latitude/longitude grid
 * <li>20 Stretched latitude/longitude grid
 * <li>24 Stretched Gaussian latitude/longitude grid
 * <li>30 Stretched and rotated latitude/longitude grids
 * <li>34 Stretched and rotated Gaussian latitude/longitude grids
 * <li>50 Spherical harmonic coefficients
 * <li>60 Rotated spherical harmonic coefficients
 * <li>70 Stretched spherical harmonics
 * <li>80 Stretched and rotated spherical harmonic coefficients
 * <li>90 Space view, perspective or orthographic
 * </ul>
 *
 * @author John
 * @since 9/3/11
 */
public abstract class Grib1Gds {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1Gds.class);
  public static final double maxReletiveErrorPos = .01; // reletive error in position - GRIB numbers sometime miscoded

  /*
  Code table 6 – Data representation type
  Code figure Meaning
 *  0 Latitude/longitude grid – equidistant cylindrical or Plate Carrée projection
 *  1 Mercator projection
    2 Gnomonic projection
 *  3 Lambert conformal, secant or tangent, conic or bi-polar, projection
 *  4 Gaussian latitude/longitude grid
 *  5 Polar stereographic projection
    6 Universal Transverse Mercator (UTM) projection
    7 Simple polyconic projection
    8 Albers equal-area, secant or tangent, conic or bi-polar, projection
    9 Miller’s cylindrical projection
 *  10 Rotated latitude/longitude grid
    11–12 Reserved
    13 Oblique Lambert conformal, secant or tangent, conic or bi-polar, projection
    14 Rotated Gaussian latitude/longitude grid
    15–19 Reserved
    20 Stretched latitude/longitude grid
    21–23 Reserved
    24 Stretched Gaussian latitude/longitude grid
    25–29 Reserved
    30 Stretched and rotated latitude/longitude grids
    31–33 Reserved
    34 Stretched and rotated Gaussian latitude/longitude grids
    35–49 Reserved
 ^  50 Spherical harmonic coefficients
    51–59 Reserved
    60 Rotated spherical harmonic coefficients
    61–69 Reserved
    70 Stretched spherical harmonics
    71–79 Reserved
    80 Stretched and rotated spherical harmonic coefficients
    81–89 Reserved
    90 Space view, perspective or orthographic
    91–191 Reserved
    192–254 Reserved for local use
   */
  public static Grib1Gds factory(int template, byte[] data) {
    switch (template) {
      case 0:
        return new LatLon(data, 0);
      case 1:
        return new Mercator(data, 1);
      case 3:
        return new LambertConformal(data, 3);
      case 4:
        return new GaussianLatLon(data, 4);
      case 5:
        return new PolarStereographic(data, 5);
      case 10:
        return new RotatedLatLon(data, 10);
      case 50:
        return new SphericalHarmonicCoefficients(data, 50);
      default:
        throw new UnsupportedOperationException("Unsupported GDS type = " + template);
    }
  }

  ///////////////////////////////////////////////////
  private static final float scale3 = (float) 1.0e-3;
  // private static final float scale6 = (float) 1.0e-6;

  protected final byte[] data;
  protected int[] nptsInLine; // thin grids, else null

  public int template;
  protected int nx, ny;
  public int scanMode, resolution;    // LOOK problem is if these differ for records in the same collection, since we only store one Gds.
  protected int lastOctet;

  protected Grib1Gds(int template) {
    this.template = template;
    this.data = null;
  }

  public Grib1Gds(byte[] data, int template) {
    this.data = data;
    this.template = template;

    nx = getOctet2(7);
    ny = getOctet2(9);
  }

  public byte[] getRawBytes() {
    return data;
  }

  public int getNpts() {

	  if (nptsInLine != null) {
		  int npts = 0;
		  for (int pts : nptsInLine) {
			  npts += pts;
		  }
		  return npts;
	  } else {
		  return nx * ny;
	  }

  }

  public int[] getNptsInLine() {
    return nptsInLine;
  }

  void setNptsInLine(int[] nptsInLine) {
    this.nptsInLine = nptsInLine;
  }

  protected int getOctet(int index) {
    if (index > data.length) return GribNumbers.UNDEFINED;
    return data[index - 1] & 0xff;
  }

  // signed
  protected int getOctet2(int start) {
    return GribNumbers.int2(getOctet(start), getOctet(start + 1));
  }

  // signed
  protected int getOctet3(int start) {
    return GribNumbers.int3(getOctet(start), getOctet(start + 1), getOctet(start + 2));
  }

  // signed
  protected int getOctet4(int start) {
    return GribNumbers.int4(getOctet(start), getOctet(start + 1), getOctet(start + 2), getOctet(start + 3));
  }

  /*
  Code table 7 – Resolution and component flags
  Bit Value Meaning
    1 0     Direction increments not given
      1     Direction increments given
    2 0     Earth assumed spherical with radius 6367.47 km
      1     Earth assumed oblate spheroidal with size as determined by IAU in 1965 (6378.160 km, 6356.775 km, f = 1/297.0)
  3–4       Reserved
    5 0     Resolved u- and v-components of vector quantities relative to easterly and northerly directions
      1     Resolved u- and v-components of vector quantities relative to the defined grid in the direction of increasing x and y (or i and j) coordinates respectively
   6–8 0    Reserved – set to zero */

  static private boolean getDirectionIncrementsGiven(int resolution) {
    return ((resolution & GribNumbers.bitmask[0]) != 0);
  }
  static private boolean getEarthShapeIsSpherical(int resolution) {
    return ((resolution & GribNumbers.bitmask[1]) == 0);
  }
  static private boolean getUVisReletive(int resolution) {
    return ((resolution & GribNumbers.bitmask[1]) != 0);
  }

  protected Earth getEarth() {
    if (getEarthShapeIsSpherical(resolution))
        return new Earth(6367470.0);
    else
        return EarthEllipsoid.IAU;
  }

  public int getEarthShape() {
    return getEarthShapeIsSpherical(resolution) ? 0 : 1;
  }

  public boolean getUVisReletive() {
    return getUVisReletive(resolution);
  }

  public int getResolution() {
    return resolution;
  }

  public boolean isLatLon() {
    return false;
  }

  /*
  Flag/Code table 8 – Scanning mode
 Bit Value Meaning
  1   0   Points scan in +i direction
      1   Points scan in –i direction
  2   0   Points scan in –j direction
      1   Points scan in +j direction
  3   0   Adjacent points in i direction are consecutive
      1   Adjacent points in j direction are consecutive
   */
  public int getScanMode() {
    return scanMode;
  }

  public int getNxRaw() {
    return nx;
  }

  public int getNyRaw() {
    return ny;
  }

  ///////// thin grid crapola
  /*
    For data on a quasi-regular grid, in which all the rows or columns do not necessarily have the same number of grid
    points, either Ni (octets 7–8) or Nj (octets 9–10) and the corresponding Di (octets 24–25) or Dj (octets 26–27) shall be
    coded with all bits set to 1 (missing); the actual number of points along each parallel or meridian shall be coded.
  */

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

  // delta in x direction
  public float getDx() {
    return getDxRaw();
  }

  // delta in y direction
  public float getDy() {
    return getDyRaw();
  }

  public abstract float getDxRaw();

  public abstract float getDyRaw();

  public abstract GdsHorizCoordSys makeHorizCoordSys();

  public abstract void testHorizCoordSys(Formatter f);

  public String getNameShort() {
    String className = getClass().getName();
    int pos = className.lastIndexOf("$");
    return className.substring(pos + 1);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Grib1Gds{");
    sb.append(" template=").append(template);
    sb.append(", nx=").append(nx);
    sb.append(", ny=").append(ny);
    sb.append(", scanMode=").append(scanMode);
    sb.append(", resolution=").append(resolution);
    sb.append(", lastOctet=").append(lastOctet);
    if (nptsInLine == null) sb.append(", nptsInLine=null");
    else {
      sb.append(", nptsInLine (").append(nptsInLine.length);
      sb.append(")=").append(Arrays.toString(nptsInLine));
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Grib1Gds Grib1Gds = (Grib1Gds) o;
    if (nx != Grib1Gds.nx) return false;
    if (ny != Grib1Gds.ny) return false;
    if (template != Grib1Gds.template) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = template;
    result = 31 * result + nx;
    result = 31 * result + ny;
    return result;
  }

  protected int hashCode = 0;

  /*
    Grid definition –   latitude/longitude grid (or equidistant cylindrical, or Plate Carrée)
    Octet No. Contents
    7–8 Ni – number of points along a parallel
    9–10 Nj – number of points along a meridian
    11–13 La1 – latitude of first grid point
    14–16 Lo1 – longitude of first grid point
    17 Resolution and component flags (see Code table 7)
    18–20 La2 – latitude of last grid point
    21–23 Lo2 – longitude of last grid point
    24–25 Di – i direction increment
    26–27 Dj – j direction increment
    28 Scanning mode (flags – see Flag/Code table 8)
    29–32 Set to zero (reserved)
    33–35 Latitude of the southern pole in millidegrees (integer) / Latitude of pole of stretching in millidegrees (integer)
    36–38 Longitude of the southern pole in millidegrees (integer) / Longitude of pole of stretching in millidegrees (integer)
    39–42 Angle of rotation (represented in the same way as the reference value) / Stretching factor (representation as for the reference value)
    43–45 Latitude of pole of stretching in millidegrees (integer)
    46–48 Longitude of pole of stretching in millidegrees (integer)
    49–52 Stretching factor (representation as for the reference value)
*/
  public static class LatLon extends Grib1Gds {
    public float la1, lo1, la2, lo2, deltaLon, deltaLat;

    protected LatLon(int template) {
      super(template);
    }

    LatLon(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution = getOctet(17);    // Resolution and component flags (see Code table 7)
      la2 = getOctet3(18) * scale3;
      lo2 = getOctet3(21) * scale3;

      if (lo2 < lo1) lo2 += 360.0F;

      deltaLon = getOctet2(24);
      float calcDelta = (lo2 - lo1) / (nx-1); // more accurate - deltaLon may have roundoff
      if (deltaLon != GribNumbers.UNDEFINED) deltaLon *= scale3; // undefined for thin grids
      else deltaLon = calcDelta;
      if (!Misc.closeEnough(deltaLon, calcDelta)) {
        log.debug("deltaLon != calcDeltaLon");
        deltaLon = calcDelta;
      }

      deltaLat = getOctet2(26);
      calcDelta = (la2 - la1) / (ny-1); // more accurate - deltaLon may have roundoff
      if (deltaLat != GribNumbers.UNDEFINED) {
        deltaLat *= scale3; // undefined for thin grids
        if (la2 < la1) deltaLat *= -1.0;
      } else deltaLat = calcDelta;

      /*  thanks to johann.sorel@geomatys.com 11/1/2012. withdrawn for now 11/2/2012
      if (deltaLat != GribNumbers.UNDEFINED) {
        deltaLat *= scale3; // undefined for thin grids
        if (la2 < la1) {
          //flip declaration order
          float latemp = la1;
          la1 = la2;
          la2 = latemp;
          calcDelta *= -1.0;

          //we must also consider the cell corner, since we flipped the order
          //we should specify that the true value is at the BOTTOM-LEFT corner
          //but we can't show this information so we make a one cell displacement
          //to move the value on a TOP-LEFT corner.
          la1 -= calcDelta;
          la2 -= calcDelta;
        }
      } else {
        deltaLat = calcDelta;
      }  */

      if (!Misc.closeEnough(deltaLat, calcDelta)) {
        log.debug("deltaLat != calcDeltaLat");
        deltaLat = calcDelta;
      }

      scanMode = (byte) getOctet(28);

      lastOctet = 28;
    }

    @Override
    public void setNptsInLine(int[] nptsInLine) {
      super.setNptsInLine(nptsInLine);
      // now that we know this, we may have to recalc some stuff
      int n = QuasiRegular.getMax(nptsInLine);
      if (nx < 0) {
        deltaLon = (lo2 - lo1) / (n-1);
      } if (ny < 0) {
        deltaLat = (la2 - la1) / (n-1);
      }
    }


    @Override
    public boolean isLatLon() {
      return true;
    }

    @Override
    public float getDx() {
      if (nptsInLine == null || deltaLon != GribNumbers.UNDEFINED) return deltaLon;
      return (lo2 - lo1) / (getNx() - 1);
  }

    @Override
    public float getDy() {
      if (nptsInLine == null || deltaLat != GribNumbers.UNDEFINED) return deltaLat;
      return (la2 - la1) / (getNy() - 1);
  }

    @Override
    public float getDxRaw() {
       return deltaLon;
    }

    @Override
    public float getDyRaw() {
      return deltaLat;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      LatLon other = (LatLon) o;
      if (!Misc.closeEnoughAbs(la1, other.la1, maxReletiveErrorPos * deltaLat)) return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, other.lo1, maxReletiveErrorPos * deltaLon)) return false;
      if (!Misc.closeEnough(deltaLat, other.deltaLat)) return false;
      if (!Misc.closeEnough(deltaLon, other.deltaLon)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat = (int) (la1 / (maxReletiveErrorPos * deltaLat));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) (lo1 / (maxReletiveErrorPos * deltaLon));
        int useDeltaLon = (int) (deltaLon / Misc.maxReletiveError);
        int useDeltaLat = (int) (deltaLat / Misc.maxReletiveError);

        int result = super.hashCode();
        result = 31 * result + useLat;
        result = 31 * result + useLon;
        result = 31 * result + useDeltaLon;
        result = 31 * result + useDeltaLat;
        hashCode = result;
      }
      return hashCode;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nLatLon{ la1=").append(la1);
      sb.append(", lo1=").append(lo1);
      sb.append(", la2=").append(la2);
      sb.append(", lo2=").append(lo2);
      sb.append(", deltaLon=").append(deltaLon);
      sb.append(", deltaLat=").append(deltaLat);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public GdsHorizCoordSys makeHorizCoordSys() {
      LatLonProjection proj = new LatLonProjection(getEarth());
      //ProjectionPoint startP = proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      double startx = lo1; // startP.getX();
      double starty = la1; // startP.getY();
      return new GdsHorizCoordSys(getNameShort(), template, 0, scanMode, proj, startx, getDx(), starty, getDy(), getNx(), getNy(), null);
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

  /* Template 4
    Grid definition – Gaussian latitude/longitude grid (including rotated, stretched or stretched and rotated)

    Octet No. Contents
    7–8   	Ni – number of points along a parallel
    9–10 	Nj – number of points along a meridian
    11–13 	La1 – latitude of first grid point
    14–16 	Lo1 – longitude of first grid point
    17 	  Resolution and component flags (see Code table 7)
    18–20 	La2 – latitude of last grid point
    21–23 	Lo2 – longitude of last grid point
    24–25 	Di – i direction increment
    26–27 	N – number of parallels between a pole and the equator
    28 	  Scanning mode (flags – see Flag/Code table 8)
    29–32 	Set to zero (reserved)
    33–35 	Latitude of the southern pole in millidegrees (integer)
    	    Latitude of pole of stretching in millidegrees (integer)
    36–38 	Longitude of the southern pole in millidegrees (integer)
    	    Longitude of pole of stretching in millidegrees (integer)
    39–42 	Angle of rotation (represented in the same way as the reference value)
    	    Stretching factor (representation as for the reference value)
    43–45 	Latitude of pole of stretching in millidegrees (integer)
    46–48 	Longitude of pole of stretching in millidegrees (integer)
    49–52 	Stretching factor (representation as for the reference value)

    Notes:
    (1) Latitude, longitude and increments are in millidegrees.

    (2) Latitude values are limited to the range 0–90 000; bit 1 is set to 1 to indicate south latitude.

    (3) Longitude values are limited to the range 0–360 000; bit 1 is set to 1 to indicate west longitude.

    (4) The number of parallels between a pole and the equator is used to establish the variable (Gaussian) spacing of the
    parallels; this value must always be given.

    (5) The latitude and longitude of the last grid point and the first grid point should always be given for regular grids.

    (6) Where items are not given, the appropriate octet(s) should have all bits set to 1.

    (7) See Notes 6 to 11 under Grid definition – latitude/longitude grid (or equidistant cylindrical, or Plate Carrée) –
    page I.2 – Bi — 8.

    (6) Three parameters define a general latitude/longitude coordinate system, formed by a general rotation of the sphere. One
    choice for these parameters is:
      (a) The geographic latitude in degrees of the southern pole of the coordinate system, θp for example;
      (b) The geographic longitude in degrees of the southern pole of the coordinate system, λp for example;
      (c) The angle of rotation in degrees about the new polar axis (measured clockwise when looking from the southern to
          the northern pole) of the coordinate system, assuming the new axis to have been obtained by first rotating the
          sphere through λp degrees about the geographic polar axis, and then rotating through (90 + θp) degrees so that
          the southern pole moved along the (previously rotated) Greenwich meridian.
    (7) For rotated grids, the vertical coordinate parameters start at octet 43 instead of 33.
    (8) The stretching is defined by three parameters:
      (a) The latitude in degrees (measured in the model coordinate system) of the pole of stretching;
      (b) The longitude in degrees (measured in the model coordinate system) of the pole of stretching;
      (c) The stretching factor C.
      The stretching is defined by representing data uniformly in a coordinate system with longitude λ and latitude θ1, where:
        (1 – C2) + (1 + C2) sin θ
        θ1 = sin–1
        (1 + C2) + (1 – C2) sin θ
      and λ and θ are longitude and latitude in a coordinate system in which the pole of stretching is the northern pole. C = 1
      gives uniform resolution, while C > 1 gives enhanced resolution around the pole of stretching.
    (9) For stretched grids, the vertical coordinate parameters start at octet 43 instead of 33.
    (10) For stretched and rotated latitude/longitude grids, the vertical coordinate parameters start at octet 53.
    (11) The first and last grid points may not necessarily correspond to the first and last data points, respectively, if the bit-map
      section is used.

    (8) Quasi-regular Gaussian latitude/longitude grids are defined only for subsets of global grids containing full latitude rows (360°).

    (9) For data on a quasi-regular grid, in which all the rows do not necessarily have the same number of grid points, Ni
    (octets 7–8) and the corresponding Di (octets 24–25) shall be coded with all bits set to 1 (missing); the actual number
    of points along each parallel shall be coded.

    (10) A quasi-regular Gaussian latitude/longitude grid is only defined for the grid scanning mode with consecutive points on
    parallels (bit 3 set to zero in Code table 8). The first point in each row shall be positioned at the meridian indicated by
    octets 14–16 and the last shall be positioned at the meridian indicated by octets 21–23. The grid points along each
    parallel shall be evenly spaced in longitude.

    // see E:/ecmwf/ICMGGECE3_000000.grb  (1/14/2015)
   */
  public static class GaussianLatLon extends LatLon {
    int nparellels;
    public float latSouthPole, lonSouthPole, rotAngle, latPole, lonPole, stretchFactor;

    GaussianLatLon(byte[] data, int template) {
      super(data, template);
      nparellels = getOctet2(26);

      if (data.length > 32) {
        latSouthPole = getOctet3(33) * scale3;
        lonSouthPole = getOctet3(36) * scale3;
        rotAngle = getOctet4(39) * scale3;
        latPole = getOctet3(43) * scale3;
        lonPole = getOctet3(46) * scale3;
        stretchFactor = getOctet4(49) * scale3;
      }

      lastOctet = 52;
    }

    @Override
    public GdsHorizCoordSys makeHorizCoordSys() {
      GdsHorizCoordSys hc = super.makeHorizCoordSys();
      hc.setGaussianLats(nparellels, la1, la2);
      return hc;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      GaussianLatLon that = (GaussianLatLon) o;
      if (nparellels != that.nparellels) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + nparellels;
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nGaussianLatLon");
      sb.append("{nparellels=").append(nparellels);
      sb.append(", latSouthPole=").append(latSouthPole);
      sb.append(", lonSouthPole=").append(lonSouthPole);
      sb.append(", rotAngle=").append(rotAngle);
      sb.append(", latPole=").append(latPole);
      sb.append(", lonPole=").append(lonPole);
      sb.append(", stretchFactor=").append(stretchFactor);
      sb.append('}');
      return sb.toString();
    }
  }

  /*
Grid definition –   polar stereographic
 Octet No. Contents
 7–8    Nx – number of points along x-axis
 9–10   Ny – number of points along y-axis
 11–13  La1 – latitude of first grid point
 14–16  Lo1 – longitude of first grid point
 17     Resolution and component flags (see Code table 7)
 18–20  LoV – orientation of the grid; i.e. the longitude value of the meridian which is parallel to the y-axis (or columns
              of the grid) along which latitude increases as the Y-coordinate increases (the orientation longitude may or may not appear on a particular grid)
 21–23  Dx – X-direction grid length (see Note 2)
 24–26  Dy – Y-direction grid length (see Note 2)
 27     Projection centre flag (see Note 5)
 28     Scanning mode (flags – see Flag/Code table 8)
 29–32  Set to zero (reserved)
  */
  public static class PolarStereographic extends Grib1Gds {
    public float la1, lo1, lov, dX, dY;
    public int projCenterFlag;
    private float lad = (float) 60.0; // LOOK

    protected PolarStereographic(int template) {
      super(template);
    }

    PolarStereographic(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution =  getOctet(17); // Resolution and component flags (see Code table 7)
      lov = getOctet3(18) * scale3;

      dX = getOctet3(21) * scale3;
      dY = getOctet3(24) * scale3;

      projCenterFlag =  getOctet(27);
      scanMode =  getOctet(28);

      lastOctet = 28;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nPolarStereographic{");
      sb.append("la1=").append(la1);
      sb.append(", lo1=").append(lo1);
      sb.append(", lov=").append(lov);
      sb.append(", dX=").append(dX);
      sb.append(", dY=").append(dY);
      sb.append(", projCenterFlag=").append(projCenterFlag);
      sb.append(", lad=").append(lad);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      PolarStereographic that = (PolarStereographic) o;

      if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY)) return false;   // allow some slop, reletive to grid size
      if (!Misc.closeEnoughAbs(lo1, that.lo1, maxReletiveErrorPos * dX)) return false;
      if (!Misc.closeEnough(lov, that.lov)) return false;
      if (!Misc.closeEnough(dY, that.dY)) return false;
      if (!Misc.closeEnough(dX, that.dX)) return false;

      if (projCenterFlag != that.projCenterFlag) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int useLat = (int) (la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) (lo1 / (maxReletiveErrorPos * dX));
        int useLov = (int) (lov / Misc.maxReletiveError);
        int useDeltaLon = (int) (dX / Misc.maxReletiveError);
        int useDeltaLat = (int) (dY / Misc.maxReletiveError);

        int result = super.hashCode();
        result = 31 * result + useLat;
        result = 31 * result + useLon;
        result = 31 * result + useLov;
        result = 31 * result + useDeltaLon;
        result = 31 * result + useDeltaLat;
        result = 31 * result + projCenterFlag;
        hashCode = result;
      }
      return hashCode;
    }

    @Override
    public float getDxRaw() {
      return dX;
    }

    @Override
    public float getDyRaw() {
      return dY;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      boolean northPole = (projCenterFlag & 128) == 0;
      double latOrigin = northPole ? 90.0 : -90.0;

      // Why the scale factor?. according to GRIB docs:
      // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
      // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
      // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
      double scale;
      if (Double.isNaN(lad)) { // LOOK ??
        scale = 0.9330127018922193;
      } else {
        scale = (1.0 + Math.sin(Math.toRadians( Math.abs(lad)))) / 2;
      }

      ProjectionImpl proj;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new Stereographic(latOrigin, lov, scale);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection(latOrigin, lov, scale, lad, 0.0, 0.0, earth);
      }

      ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      return new GdsHorizCoordSys(getNameShort(), template, 0, scanMode, proj, start.getX(), getDx(), start.getY(), getDy(), getNx(), getNy(), null);
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
  Grid definition –   Lambert conformal, secant or tangent, conic or bi-polar (normal or oblique), or
  Albers  equal-area,  secant  or  tangent,  conic  or  bi-polar  (normal  or  oblique), projection
  Octet No. Contents
  7–8   Nx – number of points along x-axis
  9–10  Ny – number of points along y-axis
  11–13 La1 – latitude of first grid point
  14–16 Lo1– longitude of first grid point
  17    Resolution and component flags (see Code table 7)
  18–20 LoV –   orientation of the grid; i.e. the east longitude value of the meridian which is parallel to the y-axis
    (or columns of the grid) along which latitude increases as the y-coordinate increases (the orientation longitude may
    or may not appear on a particular grid)
  21–23 Dx – x-direction grid length (see Note 2)
  24–26 Dy – y-direction grid length (see Note 2)
  27    Projection centre flag (see Note 5)
  28    Scanning mode (flags – see Flag/Code table 8)
  29–31 Latin 1 – first latitude from the pole at which the secant cone cuts the sphere
  32–34 Latin 2 – second latitude from the pole at which the secant cone cuts the sphere
  35–37 Latitude of the southern pole in millidegrees (integer)
  38–40 Longitude of the southern pole in millidegrees (integer)
  41–42 Set to zero (reserved)
   */
  public static class LambertConformal extends Grib1Gds {
    public float la1, lo1, lov, lad, dX, dY, latin1, latin2, latSouthPole, lonSouthPole;
    public int projCenterFlag;

    // private int hla1, hlo1, hlov, hlad, hdX, hdY, hlatin1, hlatin2; // hasheesh

    LambertConformal(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution =  getOctet(17); // Resolution and component flags (see Code table 7)
      lov = getOctet3(18) * scale3;

      dX = getOctet3(21) * scale3;
      dY = getOctet3(24) * scale3;

      projCenterFlag =  getOctet(27);
      scanMode = getOctet(28);

      latin1 = getOctet3(29) * scale3;
      latin2 = getOctet3(32) * scale3;
      latSouthPole = getOctet3(35) * scale3;
      lonSouthPole = getOctet3(38) * scale3;

      lastOctet = 42;
    }

    @Override
    public float getDxRaw() {
      return dX;
    }

    @Override
    public float getDyRaw() {
      return dY;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nLambertConformal{");
      sb.append("la1=").append(la1);
      sb.append(", lo1=").append(lo1);
      sb.append(", lov=").append(lov);
      sb.append(", lad=").append(lad);
      sb.append(", dX=").append(dX);
      sb.append(", dY=").append(dY);
      sb.append(", latin1=").append(latin1);
      sb.append(", latin2=").append(latin2);
      sb.append(", latSouthPole=").append(latSouthPole);
      sb.append(", lonSouthPole=").append(lonSouthPole);
      sb.append(", projCenterFlag=").append(projCenterFlag);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      LambertConformal that = (LambertConformal) o;

      if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY)) return false;   // allow some slop, reletive to grid size
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
        int useLat = (int) (la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
        int useLon = (int) (lo1 / (maxReletiveErrorPos * dX));
        int useLad = (int) (lad / Misc.maxReletiveError);
        int useLov = (int) (lov / Misc.maxReletiveError);
        int useDeltaLon = (int) (dX / Misc.maxReletiveError);
        int useDeltaLat = (int) (dY / Misc.maxReletiveError);
        int useLatin1 = (int) (latin1 / Misc.maxReletiveError);
        int useLatin2 = (int) (latin2 / Misc.maxReletiveError);

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
      ProjectionImpl proj = null;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new ucar.unidata.geoloc.projection.LambertConformal(latin1, lov, latin1, latin2, 0.0, 0.0, earth.getEquatorRadius() * .001);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse(latin1, lov, latin1, latin2, 0.0, 0.0, earth);
      }

      LatLonPointImpl startLL = new LatLonPointImpl(la1, lo1);
      ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(startLL);
      return new GdsHorizCoordSys(getNameShort(), template, 0, scanMode, proj, start.getX(), getDx(), start.getY(), getDy(), getNx(), getNy(), null);
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
  Grid definition – Mercator
  Octet  Contents
    7–8   Ni – number of points along a parallel
    9–10  Nj – number of points along a meridian
    11–13 La1 – latitude of first grid point
    14–16 Lo1 – longitude of first grid point
    17    Resolution and component flags (see Code table 7)
    18–20 La2 – latitude of last grid point
    21–23 Lo2 – longitude of last grid point
    24–26 Latin – latitude(s) at which the Mercator projection cylinder intersects the Earth
    27    Set to zero (reserved)
    28    Scanning mode (flags – see Flag/Code table 8)
    29–31 Di – longitudinal direction grid length (see Note 2)
    32–34 Dj – latitudinal direction grid length (see Note 2)
    35–42 Set to zero (reserved)
   */

  public static class Mercator extends Grib1Gds {
    public float la1, lo1, la2, lo2, latin, dX, dY;

    Mercator(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution =  getOctet(17); // Resolution and component flags (see Code table 7)
      la2 = getOctet3(18) * scale3;
      lo2 = getOctet3(21) * scale3;
      latin = getOctet3(24) * scale3;

      scanMode = getOctet(28);

      dX = getOctet3(29) * scale3;  // km
      dY = getOctet3(32) * scale3;  // km

      lastOctet = 42;
    }

    @Override
    public float getDxRaw() {
      return dX;
    }

    @Override
    public float getDyRaw() {
      return dY;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nMercator{ ");
      sb.append("la1=").append(la1);
      sb.append(", lo1=").append(lo1);
      sb.append(", la2=").append(la2);
      sb.append(", lo2=").append(lo2);
      sb.append(", latin=").append(latin);
      sb.append(", dX=").append(dX);
      sb.append(", dY=").append(dY);
      sb.append('}');
      return sb.toString();
    }

    @Override
     public boolean equals(Object o) {
       if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;
       if (!super.equals(o)) return false;

       Mercator that = (Mercator) o;

       if (!Misc.closeEnoughAbs(la1, that.la1, maxReletiveErrorPos * dY)) return false;   // allow some slop, reletive to grid size
       if (!Misc.closeEnoughAbs(lo1, that.lo1, maxReletiveErrorPos * dX)) return false;
       if (!Misc.closeEnough(latin, that.latin)) return false;
       if (!Misc.closeEnough(dY, that.dY)) return false;
       if (!Misc.closeEnough(dX, that.dX)) return false;

       return true;
     }

     @Override
     public int hashCode() {
       if (hashCode == 0) {
         int useLat = (int) (la1 / (maxReletiveErrorPos * dY));  //  Two equal objects must have the same hashCode() value
         int useLon = (int) (lo1 / (maxReletiveErrorPos * dX));
         int useLad = (int) (latin / Misc.maxReletiveError);
         int useDeltaLon = (int) (dX / Misc.maxReletiveError);
         int useDeltaLat = (int) (dY / Misc.maxReletiveError);

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
      // LOOK dont have an elipsoidal Mercator projection
      Earth earth = getEarth();
      ucar.unidata.geoloc.projection.Mercator proj = new ucar.unidata.geoloc.projection.Mercator(lo1, latin, 0, 0, earth.getEquatorRadius() * .001);

      // find out where things start
      ProjectionPoint startP = proj.latLonToProj(new LatLonPointImpl(la1, lo1));
      double startx = startP.getX();
      double starty = startP.getY();

      return new GdsHorizCoordSys(getNameShort(), template, 0, scanMode, proj, startx, getDx(), starty, getDy(), getNx(), getNy(), null);
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

  public static class RotatedLatLon extends LatLon {
    public float angleRotation; // Angle of rotation (represented in the same way as the reference value)
    public float latSouthPole; // Latitude of pole of stretching in millidegrees (integer)
    public float lonSouthPole;  // Longitude of pole of stretching in millidegrees (integer)

    RotatedLatLon(byte[] data, int template) {
      super(data, template);

      latSouthPole = getOctet3(33) * scale3;
      lonSouthPole = getOctet3(36) * scale3;
      angleRotation = getOctet4(39) * scale3;

      lastOctet = 43;
    }

    public String getName() {
      return "Rotated latitude/longitude";
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append("\nRotLatLon");
      sb.append("{angleRotation=").append(angleRotation);
      sb.append(", latSouthPole=").append(latSouthPole);
      sb.append(", lonSouthPole=").append(lonSouthPole);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      RotatedLatLon other = (RotatedLatLon) o;
      return Misc.closeEnough(angleRotation, other.angleRotation);
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (angleRotation != +0.0f ? Float.floatToIntBits(angleRotation) : 0);
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      ucar.unidata.geoloc.projection.RotatedLatLon proj =
              new ucar.unidata.geoloc.projection.RotatedLatLon(latSouthPole, lonSouthPole, angleRotation);
      // LOOK dont transform - works for grib1 Q:/cdmUnitTest/transforms/HIRLAMhybrid.grib
      // LatLonPoint startLL = proj.projToLatLon(new ProjectionPointImpl(lo1, la1));
      //double startx = startLL.getLongitude();
      //double starty = startLL.getLatitude();
      return new GdsHorizCoordSys(getNameShort(), template, 0, scanMode, proj, lo1, deltaLon, la1, deltaLat, nx, ny, null);
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

      double endx = cs.startx + (nx - 1) * cs.dx;
      double endy = cs.starty + (ny - 1) * cs.dy;
      f.format("   should end at x= (%f,%f)%n", endx, endy);
    }

  }

  /*  LOOK not done yet
  Grid definition – spherical harmonic coefficients (including rotated, stretched or stretched and rotated)
  Octet No. Contents
  7–8   J – pentagonal resolution parameter
  9–10  K – pentagonal resolution parameter
  11–12 M – pentagonal resolution parameter
  13    Representation type (see Code table 9)
  14    Representation mode (see Code table 10)
  15–32 Set to zero (reserved)
  33–35 Latitude of the southern pole in millidegrees (integer)
        Latitude of pole of stretching in millidegrees (integer)
  36–38 Longitude of the southern pole in millidegrees (integer)
        Longitude of pole of stretching in millidegrees (integer)
  39–42 Angle of rotation (represented in the same way as the reference value)
        Stretching factor (representation as for the reference value)
  43–45 Latitude of pole of stretching in millidegrees (integer)
  46–48 Longitude of pole of stretching in millidegrees (integer)
  49–52 Stretching factor (representation as for the reference value
   */
  public static class SphericalHarmonicCoefficients extends Grib1Gds {
    int j,k,m,type,mode;
    SphericalHarmonicCoefficients(byte[] data, int template) {
      super(data, template);

      j = getOctet2(7);
      k = getOctet2(9);
      m = getOctet2(11);
      type = getOctet(13);  // code table 9
      mode = getOctet(14);  // code table 10
    }

    @Override
    public float getDxRaw() {
      return 0;
    }

    @Override
    public float getDyRaw() {
      return 0;
    }

    @Override
    public GdsHorizCoordSys makeHorizCoordSys() {
      return null;
    }

    @Override
    public void testHorizCoordSys(Formatter f) {
    }

    @Override
    public String toString() {

      return MoreObjects.toStringHelper(this)
              .add("j", j)
              .add("k", k)
              .add("m", m)
              .add("type", type)
              .add("mode", mode)
              .toString();
    }
  }

}
