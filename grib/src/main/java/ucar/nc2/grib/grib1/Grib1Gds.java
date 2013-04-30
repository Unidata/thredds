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

import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.QuasiRegular;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Stereographic;

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
      default:
        throw new UnsupportedOperationException("Unsupported GDS type = " + template);
    }
  }

  ///////////////////////////////////////////////////
  private static final float scale3 = (float) 1.0e-3;
  private static final float scale6 = (float) 1.0e-6;

  protected final byte[] data;
  protected int[] nptsInLine; // thin grids, else null

  public int template;
  public float earthRadius, majorAxis, minorAxis;
  public int earthShape;
  protected int nx, ny;
  public int scanMode, resolution;
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

  public int[] getNptsInLine() {
    return nptsInLine;
  }

  public void setNptsInLine(int[] nptsInLine) {
    this.nptsInLine = nptsInLine;
  }

  protected int getOctet(int index) {
    if (index > data.length) return GribNumbers.UNDEFINED;
    return data[index - 1] & 0xff;
  }

  protected int getOctet2(int start) {
    return GribNumbers.int2(getOctet(start), getOctet(start + 1));
  }

  protected int getOctet3(int start) {
    return GribNumbers.int3(getOctet(start), getOctet(start + 1), getOctet(start + 2));
  }

  protected int getOctet4(int start) {
    return GribNumbers.int4(getOctet(start), getOctet(start + 1), getOctet(start + 2), getOctet(start + 3));
  }

  /* protected float getScaledValue(int start) {
    int scaleFactor = getOctet(start);
    int scaleValue = getOctet4(start + 1);
    if (scaleFactor != 0)
      return (float) (scaleValue / Math.pow(10, scaleFactor));
    else
      return (float) scaleValue;
  } */

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

  public int getResolution() {
    return resolution;
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
        return new Earth(earthRadius);
      case 2:
        return EarthEllipsoid.IAU;
      case 3:
        return new EarthEllipsoid("Grib2 Type 3", -1, majorAxis, minorAxis, 0);
      case 4:
        return EarthEllipsoid.IAG_GRS80;
      case 5:
        return EarthEllipsoid.WGS84;
      case 6:
        return new Earth(6371229.0);
      case 7:
        return new EarthEllipsoid("Grib2 Type 37", -1, majorAxis * 1000, minorAxis * 1000, 0);
      case 8:
        return new Earth(6371200.0);
      default:
        return new Earth();
    }
  }

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

    public LatLon(int template) {
      super(template);
    }

    LatLon(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution = getOctet(17);
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
      return (float) (lo2 - lo1) / (getNx() - 1);
  }

    @Override
    public float getDy() {
      if (nptsInLine == null || deltaLat != GribNumbers.UNDEFINED) return deltaLat;
      return (float) (la2 - la1) / (getNy() - 1);
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
      if (!Misc.closeEnough(la1, other.la1)) return false;
      if (!Misc.closeEnough(lo1, other.lo1)) return false;
      if (!Misc.closeEnough(deltaLat, other.deltaLat)) return false;
      if (!Misc.closeEnough(deltaLon, other.deltaLon)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (la1 != +0.0f ? Float.floatToIntBits(la1) : 0); // LOOK this is an exact comparision
        result = 31 * result + (lo1 != +0.0f ? Float.floatToIntBits(lo1) : 0);
        result = 31 * result + (deltaLon != +0.0f ? Float.floatToIntBits(deltaLon) : 0);
        result = 31 * result + (deltaLat != +0.0f ? Float.floatToIntBits(deltaLat) : 0);
        hashCode = result;
      }
      return hashCode;
    }

    @Override
    public String toString() {
      return "LatLon{" +
              "la1=" + la1 +
              ", lo1=" + lo1 +
              ", la2=" + la2 +
              ", lo2=" + lo2 +
              ", deltaLon=" + deltaLon +
              ", deltaLat=" + deltaLat +
              '}';
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      LatLonProjection proj = new LatLonProjection();
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
      LatLonPointImpl endLL = new LatLonPointImpl(la2, lo2);

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
  Grid definition –   Gaussian latitude/longitude grid (including rotated, stretched or stretched and rotated)
 Octet    Contents
    7–8     Ni – number of points along a parallel
    9–10    Nj – number of points along a meridian
    11–13   La1 – latitude of first grid point
    14–16   Lo1 – longitude of first grid point
    17      Resolution and component flags (see Code table 7)
    18–20   La2 – latitude of last grid point
    21–23   Lo2 – longitude of last grid point
    24–25   Di – i direction increment
    26–27   N – number of parallels between a pole and the equator
    28      Scanning mode (flags – see Flag/Code table 8)
    29–32   Set to zero (reserved)
    33–35   Latitude of the southern pole in millidegrees (integer)
            Latitude of pole of stretching in millidegrees (integer)
    36–38   Longitude of the southern pole in millidegrees (integer)
            Longitude of pole of stretching in millidegrees (integer)
    39–42   Angle of rotation (represented in the same way as the reference value)
            Stretching factor (representation as for the reference value)
    43–45   Latitude of pole of stretching in millidegrees (integer)
    46–48   Longitude of pole of stretching in millidegrees (integer)
    49–52   Stretching factor (representation as for the reference value)
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
      resolution =  getOctet(17);
      lov = getOctet3(18) * scale3;

      dX = getOctet3(21) * scale3;
      dY = getOctet3(24) * scale3;

      projCenterFlag =  getOctet(27);
      scanMode =  getOctet(28);

      lastOctet = 28;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      PolarStereographic that = (PolarStereographic) o;

      if (Float.compare(that.dX, dX) != 0) return false;
      if (Float.compare(that.dY, dY) != 0) return false;
      if (Float.compare(that.la1, la1) != 0) return false;
      if (Float.compare(that.lo1, lo1) != 0) return false;
      if (Float.compare(that.lov, lov) != 0) return false;
      if (projCenterFlag != that.projCenterFlag) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (la1 != +0.0f ? Float.floatToIntBits(la1) : 0);
        result = 31 * result + (lo1 != +0.0f ? Float.floatToIntBits(lo1) : 0);
        result = 31 * result + (lov != +0.0f ? Float.floatToIntBits(lov) : 0);
        result = 31 * result + (dX != +0.0f ? Float.floatToIntBits(dX) : 0);
        result = 31 * result + (dY != +0.0f ? Float.floatToIntBits(dY) : 0);
        result = 31 * result + (int) projCenterFlag;
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

      ProjectionImpl proj = null;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new Stereographic(latOrigin, lov, scale);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection(
                latOrigin, lov, scale, lad, 0.0, 0.0, earth);
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
      resolution =  getOctet(17);
      lov = getOctet3(18) * scale3;

      dX = getOctet3(21) * scale3;
      dY = getOctet3(24) * scale3;

      projCenterFlag =  getOctet(27);
      scanMode = getOctet(28);

      latin1 = getOctet3(29) * scale3;
      latin2 = getOctet3(32) * scale3;
      latSouthPole = getOctet3(35) * scale3;
      lonSouthPole = getOctet3(38) * scale3;
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      LambertConformal that = (LambertConformal) o;

      if (Float.compare(that.dX, dX) != 0) return false;
      if (Float.compare(that.dY, dY) != 0) return false;
      if (Float.compare(that.la1, la1) != 0) return false;
      if (Float.compare(that.lad, lad) != 0) return false;
      if (Float.compare(that.latSouthPole, latSouthPole) != 0) return false;
      if (Float.compare(that.latin1, latin1) != 0) return false;
      if (Float.compare(that.latin2, latin2) != 0) return false;
      if (Float.compare(that.lo1, lo1) != 0) return false;
      if (Float.compare(that.lonSouthPole, lonSouthPole) != 0) return false;
      if (Float.compare(that.lov, lov) != 0) return false;
      if (projCenterFlag != that.projCenterFlag) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (la1 != +0.0f ? Float.floatToIntBits(la1) : 0);
      result = 31 * result + (lo1 != +0.0f ? Float.floatToIntBits(lo1) : 0);
      result = 31 * result + (lov != +0.0f ? Float.floatToIntBits(lov) : 0);
      result = 31 * result + (lad != +0.0f ? Float.floatToIntBits(lad) : 0);
      result = 31 * result + (dX != +0.0f ? Float.floatToIntBits(dX) : 0);
      result = 31 * result + (dY != +0.0f ? Float.floatToIntBits(dY) : 0);
      result = 31 * result + (latin1 != +0.0f ? Float.floatToIntBits(latin1) : 0);
      result = 31 * result + (latin2 != +0.0f ? Float.floatToIntBits(latin2) : 0);
      result = 31 * result + (latSouthPole != +0.0f ? Float.floatToIntBits(latSouthPole) : 0);
      result = 31 * result + (lonSouthPole != +0.0f ? Float.floatToIntBits(lonSouthPole) : 0);
      result = 31 * result + projCenterFlag;
      return result;
    }

    /*
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    LambertConformal that = (LambertConformal) o;

    if (hdX != that.hdX) return false;
    if (hdY != that.hdY) return false;
    if (hla1 != that.hla1) return false;
    if (hlad != that.hlad) return false;
    if (hlatin1 != that.hlatin1) return false;
    if (hlatin2 != that.hlatin2) return false;
    if (hlo1 != that.hlo1) return false;
    if (hlov != that.hlov) return false;

    return true;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = super.hashCode();
      result = 31 * result + hla1;
      result = 31 * result + hlo1;
      result = 31 * result + hlov;
      result = 31 * result + hlad;
      result = 31 * result + hdX;
      result = 31 * result + hdY;
      result = 31 * result + hlatin1;
      result = 31 * result + hlatin2;
      hashCode = result;
    }
    return hashCode;
  }

  private static int round(int a) { // NCEP rounding (!)
    return (a + 5) / 10;
  }  */

    public GdsHorizCoordSys makeHorizCoordSys() {
      ProjectionImpl proj = null;

      Earth earth = getEarth();
      if (earth.isSpherical()) {
        proj = new ucar.unidata.geoloc.projection.LambertConformal(latin1, lov, latin1, latin2, 0.0, 0.0, earth.getEquatorRadius() * .001);
      } else {
        proj = new ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse(
                latin1, lov, latin1, latin2, 0.0, 0.0, earth);
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
    protected int lastOctet;

    Mercator(byte[] data, int template) {
      super(data, template);

      la1 = getOctet3(11) * scale3;
      lo1 = getOctet3(14) * scale3;
      resolution = getOctet(47);
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Mercator mercator = (Mercator) o;

      if (Float.compare(mercator.dX, dX) != 0) return false;
      if (Float.compare(mercator.dY, dY) != 0) return false;
      if (Float.compare(mercator.la1, la1) != 0) return false;
      if (Float.compare(mercator.latin, latin) != 0) return false;
      if (Float.compare(mercator.lo1, lo1) != 0) return false;

      return true;
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = super.hashCode();
        result = 31 * result + (la1 != +0.0f ? Float.floatToIntBits(la1) : 0);
        result = 31 * result + (lo1 != +0.0f ? Float.floatToIntBits(lo1) : 0);
        result = 31 * result + (latin != +0.0f ? Float.floatToIntBits(latin) : 0);
        result = 31 * result + (dX != +0.0f ? Float.floatToIntBits(dX) : 0);
        result = 31 * result + (dY != +0.0f ? Float.floatToIntBits(dY) : 0);
        hashCode = result;
      }
      return hashCode;
    }

    public GdsHorizCoordSys makeHorizCoordSys() {
      // put longitude origin at first point - doesnt actually matter
      // param par standard parallel (degrees). cylinder cuts earth at this latitude.
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
      LatLonPointImpl endLL = new LatLonPointImpl(la2, lo2);

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
    protected int lastOctet;

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
      sb.append(", lastOctet=").append(lastOctet);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      RotatedLatLon other = (RotatedLatLon) o;
      if (!Misc.closeEnough(angleRotation, other.angleRotation)) return false;
      return true;
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

}
