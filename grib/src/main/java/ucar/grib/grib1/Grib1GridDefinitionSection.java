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

// $Id: Grib1GridDefinitionSection.java,v 1.28 2005/12/08 21:00:05 rkambic Exp $

/*
 * Grib1GridDefinitionSection.java  1.0  10/01/04
 * @author Robb Kambic
 *
 */

package ucar.grib.grib1;


import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.zip.CRC32;


/**
 * A class that represents the grid definition section (GDS) of a GRIB record.
 */

public class Grib1GridDefinitionSection {

  /**
   * Length in bytes of this section.
   */
  private int length;

  /**
   * P(V|L).
   * PV - list of vertical coordinate parameters.
   * PL - list of numbers of points in each row.
   * or 255 missing.
   */
  private int P_VorL;

  /**
   * _more_
   */
  protected int[] numPV;

  /**
   * Is this a thin grid.
   */
  private boolean isThin = false;

  /**
   * Type of grid (See table 6)ie 1 == Mercator Projection Grid.
   */
  protected int type;

  /**
   * Grid name.
   */
  protected String name = "";

  /**
   * Number of grid columns. (Also Ni).
   */
  protected int nx;

  /**
   * Number of grid rows. (Also Nj).
   */
  protected int ny;

  /**
   * Latitude of grid start point.
   */
  protected double lat1;

  /**
   * Longitude of grid start point.
   */
  protected double lon1;

  /**
   * Latitude of grid last point.
   */
  protected double lat2;

  /**
   * Longitude of grid last point.
   */
  protected double lon2;

  /**
   * orientation of the grid.
   */
  protected double lov;

  /**
   * Resolution of grid (See table 7).
   */
  protected int resolution;

  /**
   * x-distance between two grid points
   * can be delta-Lon or delta x.
   */
  protected double dx;

  /**
   * y-distance of two grid points
   * can be delta-Lat or delta y.
   */
  protected double dy;

  /**
   * units of the dx and dy variables
   */
  protected String grid_units;

  /**
   * Number of parallels between a pole and the equator.
   */
  private int np;

  /**
   * Scanning mode (See table 8).
   */
  protected int scan;

  /**
   * Projection Center Flag.
   */
  private int proj_center;

  /**
   * Latin 1 - The first latitude from pole at which secant cone cuts the
   * sperical earth.  See Note 8 of ON388.
   */
  private double latin1;

  /**
   * Latin 2 - The second latitude from pole at which secant cone cuts the
   * sperical earth.  See Note 8 of ON388.
   */
  private double latin2;

  /**
   * latitude of south pole.
   */
  private double latsp;

  /**
   * longitude of south pole.
   */
  private double lonsp;

  /**
   * angle of rotation.
   */
  private double angle;

  /**
   * checksum value for this gds.
   */
  protected String checksum = "";
  protected int gdskey;

  /**
   * GDS as Variables from a byte[]
   */
  private final Grib1GDSVariables gdsVars;

  // *** constructors *******************************************************

  /**
   * constructor
   */
  public Grib1GridDefinitionSection() {
    gdsVars = null;
  }

  /**
   * Constructs a <tt>Grib1GridDefinitionSection</tt> object from a raf.
   *
   * @param raf RandomAccessFile with GDS content
   * @throws IOException          if RandomAccessFile has error.
   * @throws NoValidGribException if raf contains no valid GRIB info
   */
  public Grib1GridDefinitionSection(RandomAccessFile raf)
      throws IOException, NoValidGribException {

    double checkSum;
    int reserved;  // used to read empty space
    long sectionEnd = raf.getFilePointer();

    // octets 1-3 (Length of GDS)
    length = GribNumbers.uint3(raf);
    if (length == 0) {  // there's a extra byte between PDS and GDS
      raf.skipBytes(-2);
      length = GribNumbers.uint3(raf);
    }
    //System.out.println( "length ="+ length );

    // read in whole GDS as byte[]
    byte[] gdsData = new byte[length];
    // reset to beginning of section and read data
    raf.skipBytes(-3);
    raf.read(gdsData);
    gdsVars = new Grib1GDSVariables(gdsData);
    // reset for variable section read and set sectionEnd
    raf.seek(sectionEnd + 3);
    sectionEnd += length;

    //System.out.println( "GDS length = " + length );

    // octets 4 NV
    int NV = raf.read();
    //System.out.println( "GDS NV = " + NV );

    // octet 5 PL the location (octet number) of the list of numbers of points in each row
    P_VorL = raf.read();
    //System.out.println( "GDS PL = " + P_VorL );

    // octet 6 (grid type)
    type = raf.read();
    checkSum = type;
    //System.out.println( "GDS grid type = " + type );
    name = getName(type);

    if (type != 50) {  // values same up to resolution

      // octets 7-8 (Nx - number of points along x-axis)
      nx = raf.readShort();
      nx = (nx == -1)
          ? 1
          : nx;
      checkSum = 7 * checkSum + nx;

      // octets 9-10 (Ny - number of points along y-axis)
      ny = raf.readShort();
      ny = (ny == -1)
          ? 1
          : ny;
      checkSum = 7 * checkSum + ny;

      // octets 11-13 (La1 - latitude of first grid point)
      lat1 = GribNumbers.int3(raf) / 1000.0;
      checkSum = 7 * checkSum + lat1;

      // octets 14-16 (Lo1 - longitude of first grid point)
      lon1 = GribNumbers.int3(raf) / 1000.0;
      checkSum = 7 * checkSum + lon1;

      // octet 17 (resolution and component flags).  See Table 7
      resolution = raf.read();

    }

    switch (type) {

      //  Latitude/Longitude  grids ,  Arakawa semi-staggered e-grid rotated
      //  Arakawa filled e-grid rotated
      case 0:
      case 4:
      case 10:
      case 40:
      case 201:
      case 202:

        grid_units = "degrees";

        // octets 18-20 (La2 - latitude of last grid point)
        lat2 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lat2;

        // octets 21-23 (Lo2 - longitude of last grid point)
        lon2 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lon2;

        // octets 24-25 (Dx - Longitudinal Direction Increment )
        //dx = raf.readShort() / 1000.0;
        dx = smartRead( raf, 16, 1000.0 );
        // octets 26-27 (Dy - Latitudinal Direction Increment )
        //               Np - parallels between a pole and the equator
        if (type == 4) {
          np = raf.readShort();
        } else {
          //dy = raf.readShort() / 1000.0;  
          dy = smartRead( raf, 16, 1000.0 );
        }

        // octet 28 (Scanning mode)  See Table 8
        scan = raf.read();

        // octet 29-32 reserved
        reserved = raf.readInt();

        if (type == 10) {  //rotated
          // octets 33-35 (lat of southern pole)
          latsp = GribNumbers.int3(raf) / 1000.0;
          checkSum = 7 * checkSum + latsp;

          // octets 36-38 (lon of southern pole)
          lonsp = GribNumbers.int3(raf) / 1000.0;
          checkSum = 7 * checkSum + lonsp;

          // octet 39-42 rotationAngle
          //angle = raf.readFloat();
          angle = GribNumbers.float4(raf);

        }

        if (P_VorL != 255) {
          if (NV == 0 || NV == 255) {
            getPL(raf);
          } else {
            getPV(NV, raf);
          }
        }
        break;  // end Latitude/Longitude grids

      case 1:    //  Mercator grids

        grid_units = "m";
        
        // octets 18-20 (La2 - latitude of last grid point)
        lat2 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lat2;

        // octets 21-23 (Lo2 - longitude of last grid point)
        lon2 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lon2;

        // octets 24-26 (Latin - latitude where cylinder intersects the earth
        latin1 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + latin1;

        // octet 27 reserved
        reserved = raf.read();

        // octet 28 (Scanning mode)  See Table 8
        scan = raf.read();

        // octets 29-31 (Dx - Longitudinal Direction Increment )
        dx = GribNumbers.int3(raf);

        // octets 32-34 (Dx - Longitudinal Direction Increment )
        dy = GribNumbers.int3(raf);

        // octet 35-42 reserved
        reserved = raf.readInt();
        reserved = raf.readInt();
        if (P_VorL != 255) {
          if (NV == 0 || NV == 255) {
            getPL(raf);
          } else {
            getPV(NV, raf);
          }
        }

        break;  // end Mercator grids

      case 3:    // Lambert Conformal

        grid_units = "m";
        
        // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
        lov = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lov;

        // octets 21-23 (Dx - the X-direction grid length) See Note 2 of Table D
        dx = GribNumbers.int3(raf);

        // octets 24-26 (Dy - the Y-direction grid length) See Note 2 of Table D
        dy = GribNumbers.int3(raf);

        // octets 27 (Projection Center flag) See Note 5 of Table D
        proj_center = raf.read();

        // octet 28 (Scanning mode)  See Table 8
        scan = raf.read();

        // octets 29-31 (Latin1 - first lat where secant cone cuts spherical earth
        latin1 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + latin1;

        // octets 32-34 (Latin2 - second lat where secant cone cuts spherical earth)
        latin2 = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + latin2;

        // octets 35-37 (lat of southern pole)
        latsp = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + latsp;

        // octets 38-40 (lon of southern pole)
        lonsp = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lonsp;

        // octets 41-42
        reserved = raf.readShort();
        if (P_VorL != 255) {
          if (NV == 0 || NV == 255) {
            getPL(raf);
          } else {
            getPV(NV, raf);
          }
        }

        break;  // end Lambert Conformal

      case 5:    //  Polar Stereographic grids
      case 87:

        grid_units = "m";
        
        // octets 18-20 (Lov - Orientation of the grid - east lon parallel to y axis)
        lov = GribNumbers.int3(raf) / 1000.0;
        checkSum = 7 * checkSum + lov;

        if (type == 87) {
          lon2 = GribNumbers.int3(raf) / 1000.0;
          checkSum = 7 * checkSum + lon2;
        }

        // octets 21-23 (Dx - Longitudinal Direction Increment )
        dx = GribNumbers.int3(raf);

        // octets 24-26(Dy - Latitudinal Direction Increment )
        dy = GribNumbers.int3(raf);

        // octets 27 (Projection Center flag) See Note 5 of Table D
        proj_center = raf.read();

        // octet 28 (Scanning mode)  See Table 8
        scan = raf.read();

        // octet 29-32 reserved
        reserved = GribNumbers.int4(raf);
        if (P_VorL != 255) {
          if (NV == 0 || NV == 255) {
            getPL(raf);
          } else {
            getPV(NV, raf);
          }
        }

        break;  // end Polar Stereographic grids

      default:
        System.out.println("Unknown Grid Type : " + type);
        throw new NoValidGribException("GDS: Unknown Grid Type : " + type
            + ") is not supported.");

    }             // end switch grid_type
    gdskey = Double.toString(checkSum).hashCode();
    checksum = Integer.toString(gdskey);

    // seek to end of section no matter what
    raf.seek(sectionEnd);

  }  // end Grib1GridDefinitionSection( raf )

  /**
   * Check if read returns a missing value by all bits set to 1's
   *
   * @param raf RandomAccessFile
   * @param bits number of bits in read
   * @param divider
   * @throws IOException _more_
   */
  private double smartRead( RandomAccessFile raf, int bits, double divider ) throws IOException {

    if( bits == 16 ) {
      short s = raf.readShort();
      if( s == -1) {
        return GribNumbers.UNDEFINED;
      } else {
        return  (double) s / divider;
      }
    } else if( bits == 24 ) { //TODO: check before using
      int i = GribNumbers.int3(raf);
      if( i == -1) {
        return GribNumbers.UNDEFINED;
      } else {
        return  (double) i / divider;
      }
    }
    return GribNumbers.UNDEFINED;
  }

  /**
   * Gets the number of points in each parallel for Quasi/Thin grids
   *
   * @param raf RandomAccessFile
   * @throws IOException _more_
   */
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

  /**
   * Gets the number of vertical coordinate for this parameter and converts
   * them to pressure coordinates.
   *
   * @param NV  number of vertical coordinates
   * @param raf RandomAccessFile
   * @throws IOException _more_
   */
  private void getPV(int NV, RandomAccessFile raf) throws IOException {

    if( 0 < 1)
        return;

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

  // *** public methods ***************************************************

  /**
   * Get length in bytes of this section.
   * @deprecated
   * @return length in bytes of this section
   */
  public final int getLength() {
    return length;
  }

  /**
   * is this a thin grid.
   * @deprecated
   * @return isThin grid boolean
   */
  public final boolean getIsThin() {
    return isThin;
  }

  /**
   * @deprecated
   * @return  numPV int[]
   */
  public final int[] getNumPV() {
    return numPV;
  }

  /**
   * Get type of grid.
   * @deprecated
   * @return type of grid
   */
  public final int getGridType() {
    return type;
  }

  /**
   * Get type of grid.
   * @deprecated
   * @return type of grid
   */
  public final int getGdtn() {
    return type;
  }

  /**
   * Get Grid name.
   * @deprecated
   * @return name
   */
  public final String getName() {
    return name;
  }

  /**
   * Get Grid name.
   * @deprecated
   * @param type
   * @return name
   */
  static public String getName(int type) {
    switch (type) {

      case 0:
        return "Latitude/Longitude Grid";

      case 1:
        return "Mercator Projection Grid";

      case 2:
        return "Gnomonic Projection Grid";

      case 3:
        return "Lambert Conformal";

      case 4:
        return "Gaussian Latitude/Longitude";

      case 5:
      case 87:
        return "Polar Stereographic projection Grid";

      case 6:
        return "Universal Transverse Mercator";

      case 7:
        return "Simple polyconic projection";

      case 8:
        return "Albers equal-area, secant or tangent, conic or bi-polar, projection";

      case 9:
        return "Miller's cylindrical projection";

      case 10:
        return "Rotated latitude/longitude grid";

      case 13:
        return "Oblique Lambert conformal, secant or tangent, conical or bipolar, projection";

      case 14:
        return "Rotated Gaussian latitude/longitude grid";

      case 20:
        return "Stretched latitude/longitude grid";

      case 24:
        return "Stretched Gaussian latitude/longitude grid";

      case 30:
        return "Stretched and rotated latitude/longitude grids";

      case 34:
        return "Stretched and rotated Gaussian latitude/longitude grids";

      case 50:
        return "Spherical Harmonic Coefficients";

      case 60:
        return "Rotated spherical harmonic coefficients";

      case 70:
        return "Stretched spherical harmonics";

      case 80:
        return "Stretched and rotated spherical harmonic coefficients";

      case 90:
        return "Space view perspective or orthographic";

      case 201:
        return "Arakawa semi-staggered E-grid on rotated latitude/longitude grid-point array";

      case 202:
        return "Arakawa filled E-grid on rotated latitude/longitude grid-point array";
    }

    return "Unknown";

  }  // end getName

  /**
   * Get number of grid columns.
   * @deprecated
   * @return number of grid columns
   */
  public final int getNx() {
    return nx;
  }

  /**
   * Get number of grid rows.
   * @deprecated
   * @return number of grid rows.
   */
  public final int getNy() {
    return ny;
  }

  /**
   * Get y-coordinate/latitude of grid start point.
   * @deprecated
   * @return y-coordinate/latitude of grid start point
   */
  public final double getLa1() {
    return lat1;
  }

  /**
   * Get x-coordinate/longitude of grid start point.
   * @deprecated
   * @return x-coordinate/longitude of grid start point
   */
  public final double getLo1() {
    return lon1;
  }

  /**
   * Get grid resolution.
   * @deprecated
   * @return resolution
   */
  public final int getResolution() {
    return resolution;
  }

  /**
   * grid shape spherical or oblate.
   * @deprecated
   * @return int grid shape code 1 or 3
   */
  public final int getShape() {
    int res = resolution >> 6;
    if ((res == 1) || (res == 3)) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * shape of grid.
   * @deprecated
   * @return grid shape name
   */
  public final String getShapeName() {
    return getShapeName(getShape());
  }

  /**
   * shape of grid.
   * @deprecated
   * @param code grid shape code
   * @return String grid shape name
   */
  static public String getShapeName(int code) {
    if (code == 1) {
      return "oblate spheroid";
    } else {
      return "spherical";
    }
  }

  /**
   * Grib 1 has static radius.
   * @deprecated
   * @return ShapeRadius of 6367.47
   */
  public static final double getShapeRadius() {
    return 6367.47;
  }

  /**
   * Grib 1 has static MajorAxis.
   * @deprecated
   * @return ShapeMajorAxis of 6378.160
   */
  public static final double getShapeMajorAxis() {
    return 6378.160;
  }

  /**
   * Grib 1 has static MinorAxis.
   * @deprecated
   * @return ShapeMinorAxis of 6356.775
   */
  public static final double getShapeMinorAxis() {
    return 6356.775;
  }

  /**
   * Get y-coordinate/latitude of grid end point.
   * @deprecated
   * @return y-coordinate/latitude of grid end point
   */
  public final double getLa2() {
    return lat2;
  }

  /**
   * Get x-coordinate/longitude of grid end point.
   * @deprecated
   * @return x-coordinate/longitude of grid end point
   */
  public final double getLo2() {
    return lon2;
  }

  /**
   * orientation of the grid.
   * @deprecated
   * @return lov
   */
  public final double getLov() {
    return lov;
  }

  /**
   * not defined in Grib1.
   *  @deprecated
   * @return lad
   */
  public final double getLad() {
    return 0;
  }

  /**
   * Get x-increment/distance between two grid points.
   * @deprecated
   * @return x-increment
   */
  public final double getDx() {
    return dx;
  }

  /**
   * Get y-increment/distance between two grid points.
   * @deprecated
   * @return y-increment
   */
  public final double getDy() {
    return dy;
  }

  /**
   * grid units
   * @deprecated
   * @return   grid_units
   */
  public String getGrid_units() {
    return grid_units;
  }

  /**
   * Get parallels between a pole and the equator.
   * @deprecated
   * @return np
   */
  public final double getNp() {
    return np;
  }

  /**
   * Get scan mode.
   * @deprecated
   * @return scan mode
   */
  public final int getScanMode() {
    return scan;
  }

  /**
   * Get Projection Center flag - see note 5 of Table D.
   * @deprecated
   * @return Projection Center flag
   */
  public final int getProjectionCenter() {
    return proj_center;
  }

  /**
   * Get first latitude from the pole at which cylinder cuts spherical earth -
   * see note 8 of Table D.
   * @deprecated
   * @return latitude
   */
  public final double getLatin() {
    return latin1;
  }

  /**
   * Get first latitude from the pole at which cone cuts spherical earth -
   * see note 8 of Table D.
   * @deprecated
   * @return latitude of south pole
   */
  public final double getLatin1() {
    return latin1;
  }

  /**
   * Get second latitude from the pole at which cone cuts spherical earth -
   * see note 8 of Table D.
   * @deprecated
   * @return latitude of south pole
   */
  public final double getLatin2() {
    return latin2;
  }

  /**
   * Get latitude of south pole.
   * @deprecated
   * @return latitude
   */
  public final double getSpLat() {
    return latsp;
  }

  /**
   * Get longitude of south pole of a rotated latitude/longitude grid.
   * @deprecated
   * @return longitude
   */
  public final double getSpLon() {
    return lonsp;
  }

  /**
   * Get angle of rotation.
   * @deprecated
   * @return angle
   */
  public final double getAngle() {
    return angle;
  }

  /**
   * checksum of this gds, used for comparisons.
   * @deprecated
   * @return string representation of this GDS checksum
   */
  public final String getCheckSum() {
    return checksum;
  }

  /**
   * .
   *
   * @return gdskey as a int
   */
  public final int getGdsKey() {
    if ( gdsVars == null ) {  // Grib record has no GDS
      return gdskey;
    } else {
      return gdsVars.getGdsKey();
    }
  }

  /**
   * GDS as Grib2GDSVariables
   *
   * @return Grib2GDSVariables GDS vars
   */
  public Grib1GDSVariables getGdsVars() {
    return gdsVars;
  }

}  // end Grib1GridDefinitionSection


