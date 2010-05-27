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

// $Id: Grib2GridDefinitionSection.java,v 1.27 2005/12/12 18:22:40 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;

/*
 * Grib2GridDefinitionSection.java  1.0  07/29/2003
 * @author Robb Kambic
 *
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * A class that represents the grid definition section (GDS) of a GRIB product.
 * This is section 3 of a Grib record that contains coordinate information.
 */

public final class Grib2GridDefinitionSection {

  /**
   * scale factor for Lat/Lon variables in degrees.
   */
  private static final float tenToNegSix = (float) (1 / 1000000.0);

  /**
   * scale factor for dx and dy variables plus others
   */
  private static final float tenToNegThree = (float) (1 / 1000.0);

  /**
   * Length in bytes of this section.
   */
  private final int length;

  /**
   * section number should be 3.
   */
  private final int section;

  /**
   * source of grid definition.
   */
  private final int source;

  /**
   * number of data points.
   */
  private final int numberPoints;

  /**
   * olon > 0 is a quasi regular grid.
   */
  private final int olon;

  /**
   * are extreme points in the quasi regular grid.
   */
  private final int iolon;

  /**
   * number of points in each parallel for quasi grids.
   */
  private int[] olonPts;

  /**
   * Max number of points in parallel for quasi grids.
   */
  private int maxPts;

  /**
   * Grid Definition Template Number.
   */
  private final int gdtn;

  /**
   * Grid name.
   */
  private final String name;

  /**
   * grid definitions from template 3.
   */
  private int shape;

  /**
   * earthRadius
   */
  private float earthRadius;

  /**
   * majorAxis
   */
  private float majorAxis;

  /**
   * minorAxis
   */
  private float minorAxis;

  /**
   * Number of grid columns. (Also Ni).
   */
  private int nx;

  /**
   * Number of grid rows. (Also Nj).
   */
  private int ny;

  /**
   * angle
   */
  private int angle;

  /**
   * subdivisionsangle
   */
  private int subdivisionsangle;

  /**
   * First latitude
   */
  private float la1;

  /**
   * First longitude
   */
  private float lo1;

  /**
   * resolution
   */
  private int resolution;

  /**
   * 2nd latitude
   */
  private float la2;

  /**
   * 2nd longitude
   */
  private float lo2;

  /**
   * lad
   */
  private float lad;

  /**
   * lov
   */
  private float lov;

  /**
   * x-distance between two grid points
   * can be delta-Lon or delta x.
   */
  private float dx;

  /**
   * y-distance of two grid points
   * can be delta-Lat or delta y.
   */
  private float dy;

  /**
   * units of the dx and dy variables
   */
  private String grid_units;

  /**
   * projectionCenter
   */
  private int projectionCenter;

  /**
   * scanMode
   */
  private int scanMode;

  /**
   * latin1
   */
  private float latin1;

  /**
   * latin2
   */
  private float latin2;

  /**
   * spLat
   */
  private float spLat;

  /**
   * spLon
   */
  private float spLon;

  /**
   * rotationangle
   */
  private float rotationangle;

  /**
   * poleLat
   */
  private float poleLat;

  /**
   * poleLon
   */
  private float poleLon;

  /**
   * lonofcenter
   */
  private int lonofcenter;

  /**
   * factor
   */
  private int factor;

  /**
   * n
   */
  private int n;

  /**
   * j
   */
  private float j;

  /**
   * k
   */
  private float k;

  /**
   * m
   */
  private float m;

  /**
   * method
   */
  private int method;

  /**
   * mode
   */
  private int mode;

  /**
   * xp
   */
  private float xp;

  /**
   * yp
   */
  private float yp;

  /**
   * lap
   */
  private int lap;

  /**
   * lop
   */
  private int lop;

  /**
   * xo
   */
  private int xo;

  /**
   * yo
   */
  private int yo;

  /**
   * altitude
   */
  private int altitude;

  /**
   * n2
   */
  private int n2;

  /**
   * n3
   */
  private int n3;

  /**
   * ni
   */
  private int ni;

  /**
   * nd
   */
  private int nd;

  /**
   * position
   */
  private int position;

  /**
   * order
   */
  private int order;

  /**
   * nb
   */
  private float nb;

  /**
   * nr
   */
  private float nr;

  /**
   * dstart
   */
  private float dstart;

  /**
   * checksum
   */
  private String checksum = "";

  /**
   * 4.0 indexes use this for the GDS key
   */
  private int gdskey;

  /**
   * GDS as Variables from a byte[]
   */
  private final Grib2GDSVariables gdsVars;
  // *** constructors *******************************************************

  /**
   * Constructs a <tt>Grib2GridDefinitionSection</tt> object from a raf.
   *
   * @param raf        RandomAccessFile
   * @param doCheckSum calculate the checksum
   * @throws IOException if raf contains no valid GRIB product
   */
  public Grib2GridDefinitionSection(
      RandomAccessFile raf,
      boolean doCheckSum)
      throws IOException {

    double checkSum;
    int scalefactorradius = 0;
    int scaledvalueradius = 0;
    int scalefactormajor = 0;
    int scaledvaluemajor = 0;
    int scalefactorminor = 0;
    int scaledvalueminor = 0;

    long sectionEnd = raf.getFilePointer();

    // octets 1-4 (Length of GDS)
    length = GribNumbers.int4(raf);
    //System.out.println( "GDS length=" + length );

    // read in whole GDS as byte[]
    byte[] gdsData = new byte[length];
    // reset to beginning of section and read data
    raf.skipBytes(-4);
    raf.read(gdsData);
    gdsVars = new Grib2GDSVariables(gdsData);
    // reset for variable section read and set sectionEnd
    raf.seek(sectionEnd + 4);
    sectionEnd += length;

    // octet 5
    section = raf.read();  // This is section 3
    //System.out.println( "GDS is 3, section=" + section );

    // octet 6
    source = raf.read();
    //System.out.println( "GDS source=" + source );

    // octets 7-10
    numberPoints = GribNumbers.int4(raf);
    //System.out.println( "GDS numberPoints=" + numberPoints );
    checkSum = numberPoints;

    // octet 11
    olon = raf.read();
    //System.out.println( "GDS olon=" + olon );

    // octet 12
    iolon = raf.read();
    //System.out.println( "GDS iolon=" + iolon );

    // octets 13-14
    gdtn = GribNumbers.int2(raf);
    //System.out.println( "GDS gdtn=" + gdtn );
    checkSum = 7 * checkSum + gdtn;

    name = getGridName(gdtn);

    float ratio;

    switch (gdtn) {  // Grid Definition Template Number

      case 0:
      case 1:
      case 2:
      case 3:       // Latitude/Longitude Grid
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        angle = GribNumbers.int4(raf);
        subdivisionsangle = GribNumbers.int4(raf);
        if (angle == 0) {
          ratio = tenToNegSix;
        } else {
          ratio = angle / subdivisionsangle;
        }
        //System.out.println( "ratio =" + ratio );
        la1 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + la1;
        lo1 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + lo1;
        resolution = raf.read();
        la2 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + la2;
        lo2 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + lo2;
        dx = (float) (GribNumbers.int4(raf) * ratio);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * ratio);

        grid_units = "degrees";

        //checkSum = 7 * checkSum + dy;
        scanMode = raf.read();

        //  1, 2, and 3 needs checked
        if (gdtn == 1) {         //Rotated Latitude/longitude
          spLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLat;
          spLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLon;
          rotationangle = raf.readFloat();

        } else if (gdtn == 2) {  //Stretched Latitude/longitude
          poleLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLat;
          poleLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLon;
          factor = GribNumbers.int4(raf);

        } else if (gdtn == 3) {  //Stretched and Rotated
          // Latitude/longitude
          spLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLat;
          spLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLon;
          rotationangle = raf.readFloat();
          poleLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLat;
          poleLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLon;
          factor = GribNumbers.int4(raf);
        }
        break;

      case 10:  // Mercator
        // la1, lo1, lad, la2, and lo2 need checked
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        la1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + la1;
        lo1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lo1;
        resolution = raf.read();
        lad = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lad;
        la2 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + la2;
        lo2 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lo2;
        scanMode = raf.read();
        angle = GribNumbers.int4(raf);
        dx = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dy;
        grid_units = "m";

        break;

      case 20:  // Polar stereographic projection
        // la1, lo1, lad, and lov need checked
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        la1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + la1;
        lo1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lo1;
        resolution = raf.read();
        lad = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lad;
        lov = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lov;
        dx = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * tenToNegThree);
        grid_units = "m";
        //checkSum = 7 * checkSum + dy;
        projectionCenter = raf.read();
        scanMode = raf.read();

        break;

      case 30:  // Lambert Conformal
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        la1 = (float) (GribNumbers.int4(raf) * tenToNegSix);
        checkSum = 7 * checkSum + la1;
        //System.out.println( "la1=" + la1 );
        lo1 = (float) (GribNumbers.int4(raf) * tenToNegSix);
        checkSum = 7 * checkSum + lo1;
        //System.out.println( "lo1=" + lo1);
        resolution = raf.read();
        lad = (float) (GribNumbers.int4(raf)
            * tenToNegSix);
        checkSum = 7 * checkSum + lad;
        lov = (float) (GribNumbers.int4(raf)
            * tenToNegSix);
        checkSum = 7 * checkSum + lov;
        dx = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dy;
        grid_units = "m";

        projectionCenter = raf.read();
        scanMode = raf.read();
        latin1 = (float) (GribNumbers.int4(raf)
            * tenToNegSix);
        checkSum = 7 * checkSum + latin1;
        latin2 = (float) (GribNumbers.int4(raf)
            * tenToNegSix);
        checkSum = 7 * checkSum + latin2;
        //System.out.println( "latin1=" + latin1);
        //System.out.println( "latin2=" + latin2);
        spLat = (float) (GribNumbers.int4(raf) * tenToNegSix);
        checkSum = 7 * checkSum + spLat;
        spLon = (float) (GribNumbers.int4(raf) * tenToNegSix);
        checkSum = 7 * checkSum + spLon;
        //System.out.println( "spLat=" + spLat);
        //System.out.println( "spLon=" + spLon);

        break;

      case 31:  // Albers Equal Area
        // la1, lo1, lad, and lov need checked
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        la1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + la1;
        //System.out.println( "la1=" + la1 );
        lo1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lo1;
        //System.out.println( "lo1=" + lo1);
        resolution = raf.read();
        lad = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lad;
        lov = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lov;
        dx = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dy;
        grid_units = "m";

        projectionCenter = raf.read();
        scanMode = raf.read();
        latin1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + latin1;
        latin2 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + latin2;
        //System.out.println( "latin1=" + latin1);
        //System.out.println( "latin2=" + latin2);
        spLat = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + spLat;
        spLon = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + spLon;
        //System.out.println( "spLat=" + spLat);
        //System.out.println( "spLon=" + spLon);

        break;

      case 40:
      case 41:
      case 42:
      case 43:  // Gaussian latitude/longitude
        // octet 15
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        // octet 16
        scalefactorradius = raf.read();
        // octets 17-20
        scaledvalueradius = GribNumbers.int4(raf);
        // octet 21
        scalefactormajor = raf.read();
        // octets 22-25
        scaledvaluemajor = GribNumbers.int4(raf);
        // octet 26
        scalefactorminor = raf.read();
        // octets 27-30
        scaledvalueminor = GribNumbers.int4(raf);
        // octets 31-34
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        // octets 35-38
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        // octets 39-42
        angle = GribNumbers.int4(raf);
        // octets 43-46
        subdivisionsangle = GribNumbers.int4(raf);
        if (angle == 0) {
          ratio = tenToNegSix;
        } else {
          ratio = angle / subdivisionsangle;
        }
        //System.out.println( "ratio =" + ratio );
        // octets 47-50
        la1 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + la1;
        // octets 51-54
        lo1 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + lo1;
        // octet 55
        resolution = raf.read();
        // octets 56-59
        la2 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + la2;
        // octets 60-63
        lo2 = (float) (GribNumbers.int4(raf) * ratio);
        checkSum = 7 * checkSum + lo2;
        // octets 64-67
        dx = (float) (GribNumbers.int4(raf) * ratio);
        //checkSum = 7 * checkSum + dx;
        grid_units = "degrees";

        // octet 68-71
        n = GribNumbers.int4(raf);
        // octet 72
        scanMode = raf.read();

        if (gdtn == 41) {  //Rotated Gaussian Latitude/longitude
          // octets 73-76
          spLat = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + spLat;
          // octets 77-80
          spLon = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + spLon;
          // octets 81-84
          rotationangle = raf.readFloat();

        } else if (gdtn == 42) {  //Stretched Gaussian
          // Latitude/longitude
          // octets 73-76
          poleLat = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + poleLat;
          // octets 77-80
          poleLon = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + poleLon;
          // octets 81-84
          factor = GribNumbers.int4(raf);

        } else if (gdtn == 43) {  //Stretched and Rotated Gaussian
          // Latitude/longitude
          // octets 73-76
          spLat = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + spLat;
          // octets 77-80
          spLon = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + spLon;
          // octets 81-84
          rotationangle = raf.readFloat();
          // octets 85-88
          poleLat = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + poleLat;
          // octets 89-92
          poleLon = GribNumbers.int4(raf) * ratio;
          checkSum = 7 * checkSum + poleLon;
          // octets 93-96
          factor = GribNumbers.int4(raf);
        }
        break;

      case 50:
      case 51:
      case 52:
      case 53:                     // Spherical harmonic coefficients

        j = raf.readFloat();
        k = raf.readFloat();
        m = raf.readFloat();
        method = raf.read();
        mode = raf.read();
        grid_units = "";
        if (gdtn == 51) {         //Rotated Spherical harmonic coefficients

          spLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLat;
          spLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLon;
          rotationangle = raf.readFloat();

        } else if (gdtn == 52) {  //Stretched Spherical
          // harmonic coefficients

          poleLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLat;
          poleLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLon;
          factor = GribNumbers.int4(raf);

        } else if (gdtn == 53) {  //Stretched and Rotated
          // Spherical harmonic coefficients

          spLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLat;
          spLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + spLon;
          rotationangle = raf.readFloat();
          poleLat = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLat;
          poleLon = GribNumbers.int4(raf) * tenToNegSix;
          checkSum = 7 * checkSum + poleLon;
          factor = GribNumbers.int4(raf);
        }
        break;

      case 90:  // Space view perspective or orthographic
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        lap = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + lap;
        lop = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + lop;
        resolution = raf.read();
        dx = GribNumbers.int4(raf);
        //checkSum = 7 * checkSum + dx;
        dy = GribNumbers.int4(raf);
        //checkSum = 7 * checkSum + dy;
        grid_units = "";
        xp = (float) (GribNumbers.int4(raf) * tenToNegThree);
        checkSum = 7 * checkSum + xp;
        yp = (float) (GribNumbers.int4(raf) * tenToNegThree);
        checkSum = 7 * checkSum + yp;
        scanMode = raf.read();
        angle = GribNumbers.int4(raf);
        //altitude = GribNumbers.int4( raf ) * 1000000;
        altitude = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + altitude;
        xo = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + xo;
        yo = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + yo;

        break;

      case 100:  // Triangular grid based on an icosahedron

        n2 = raf.read();
        checkSum = 7 * checkSum + n2;
        n3 = raf.read();
        checkSum = 7 * checkSum + n3;
        ni = GribNumbers.int2(raf);
        checkSum = 7 * checkSum + ni;
        nd = raf.read();
        checkSum = 7 * checkSum + nd;
        poleLat = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + poleLat;
        poleLon = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + poleLon;
        lonofcenter = GribNumbers.int4(raf);
        position = raf.read();
        order = raf.read();
        scanMode = raf.read();
        n = GribNumbers.int4(raf);
        grid_units = "";
        break;

      case 110:  // Equatorial azimuthal equidistant projection
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        la1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + la1;
        lo1 = GribNumbers.int4(raf) * tenToNegSix;
        checkSum = 7 * checkSum + lo1;
        resolution = raf.read();
        dx = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dx;
        dy = (float) (GribNumbers.int4(raf) * tenToNegThree);
        //checkSum = 7 * checkSum + dy;
        grid_units = "";
        projectionCenter = raf.read();
        scanMode = raf.read();

        break;

      case 120:  // Azimuth-range Projection
        nb = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + nb;
        nr = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + nr;
        la1 = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + la1;
        lo1 = GribNumbers.int4(raf);
        checkSum = 7 * checkSum + lo1;
        dx = GribNumbers.int4(raf);
        //checkSum = 7 * checkSum + dx;
        grid_units = "";
        dstart = raf.readFloat();
        scanMode = raf.read();
        for (int i = 0; i < nr; i++) {
          // get azi (33+4(Nr-1))-(34+4(Nr-1))
          // get adelta (35+4(Nr-1))-(36+4(Nr-1))
        }
        System.out.println("need code to get azi and adelta");

        break;

      case 204:  // Curvilinear orthographic
        shape = raf.read();
        //System.out.println( "shape=" + shape );
        scalefactorradius = raf.read();
        scaledvalueradius = GribNumbers.int4(raf);
        scalefactormajor = raf.read();
        scaledvaluemajor = GribNumbers.int4(raf);
        scalefactorminor = raf.read();
        scaledvalueminor = GribNumbers.int4(raf);
        nx = GribNumbers.int4(raf);
        //System.out.println( "nx=" + nx);
        ny = GribNumbers.int4(raf);
        //System.out.println( "ny=" + ny);
        // octets 39 - 54 not used, set to 0
        byte[] dst = new byte[16];
        raf.read(dst);
        resolution = raf.read();
        // octets 56 - 71 not used
        raf.read(dst);
        scanMode = raf.read();
        grid_units = "";
        break;
      default:
        System.out.println("Unknown Grid Type "
            + Integer.toString(gdtn));

    }  // end switch

    // calculate earth radius
    if (((gdtn < 50) || (gdtn > 53)) && (gdtn != 100) && (gdtn != 120)) {
      if (shape == 0) {
        earthRadius = 6367470;
      } else if (shape == 1) {
        earthRadius = scaledvalueradius;
        if (scalefactorradius != 0) {
          earthRadius /= Math.pow(10, scalefactorradius);
        }
      } else if (shape == 2) {
        majorAxis = (float) 6378160.0;
        minorAxis = (float) 6356775.0;
      } else if (shape == 3) {
        majorAxis = scaledvaluemajor;
        //System.out.println( "majorAxisScale =" + scalefactormajor );
        //System.out.println( "majorAxisiValue =" + scaledvaluemajor );
        majorAxis /= Math.pow(10, scalefactormajor);

        minorAxis = scaledvalueminor;
        //System.out.println( "minorAxisScale =" + scalefactorminor );
        //System.out.println( "minorAxisValue =" + scaledvalueminor );
        minorAxis /= Math.pow(10, scalefactorminor);
      } else if (shape == 4) {
        majorAxis = (float) 6378137.0;
        minorAxis = (float) 6356752.314;
      } else if (shape == 6) {
        earthRadius = 6371229;
      }
    }
    // This is a quasi-regular grid, save the number of pts in each parallel
    if (olon != 0) {
      //System.out.println( "olon ="+ olon +" iolon ="+ iolon );
      int numPts;
      if ((scanMode & 32) == 0) {
        numPts = ny;
      } else {
        numPts = nx;
      }
      olonPts = new int[numPts];
      //int count = 0;
      maxPts = 0;
      if (olon == 1) {
        for (int i = 0; i < numPts; i++) {
          olonPts[i] = raf.read();
          if (maxPts < olonPts[i]) {
            maxPts = olonPts[i];
          }
          //count += olonPts[ i ];
          //System.out.println( "parallel =" + i +" number pts ="+ latPts );
        }
      } else if (olon == 2) {
        for (int i = 0; i < numPts; i++) {
          olonPts[i] = raf.readUnsignedShort();
          if (maxPts < olonPts[i]) {
            maxPts = olonPts[i];
          }
          //count += olonPts[ i ];
          //System.out.println( "parallel =" + i +" number pts ="+ latPts );
        }
      }
      if ((scanMode & 32) == 0) {
        nx = maxPts;
      } else {
        ny = maxPts;
      }
      //double lodiff = gds.getLo2() - gds.getLo1();
      dx = (float) (lo2 - lo1) / (nx - 0);
      //System.out.println( "total number pts ="+ count );
    }

    gdskey = Double.toString(checkSum).hashCode();
    checksum = Integer.toString(gdskey);
  }  // end of Grib2GridDefinitionSection

  /**
   * .
   * @deprecated
   * @param gdtn Grid definition template number same as type of grid
   * @return GridName as a String
   */
  public static String getGridName(int gdtn) {
    switch (gdtn) {  // code table 3.2

      case 0:
        return "Latitude/Longitude";

      case 1:
        return "Rotated Latitude/Longitude";

      case 2:
        return "Stretched Latitude/Longitude";

      case 3:
        return "iStretched and Rotated Latitude/Longitude";

      case 10:
        return "Mercator";

      case 20:
        return "Polar stereographic";

      case 30:
        return "Lambert Conformal";

      case 31:
        return "Albers Equal Area";

      case 40:
        return "Gaussian latitude/longitude";

      case 41:
        return "Rotated Gaussian Latitude/longitude";

      case 42:
        return "Stretched Gaussian Latitude/longitude";

      case 43:
        return "Stretched and Rotated Gaussian Latitude/longitude";

      case 50:
        return "Spherical harmonic coefficients";

      case 51:
        return "Rotated Spherical harmonic coefficients";

      case 52:
        return "Stretched Spherical harmonic coefficients";

      case 53:
        return "Stretched and Rotated Spherical harmonic coefficients";

      case 90:
        return "Space View Perspective or Orthographic";

      case 100:
        return "Triangular Grid Based on an Icosahedron";

      case 110:
        return "Equatorial Azimuthal Equidistant";

      case 120:
        return "Azimuth-Range";

      case 204:
        return "Curvilinear Orthogonal Grid";

      default:
        return "Unknown projection" + gdtn;
    }
  }                    // end getGridName

  /**
   * source of grid definition.
   * @deprecated
   * @return source
   */
  public final int getSource() {
    return source;
  }

  /**
   * number of data points .
   * @deprecated
   * @return numberPoints
   */
  public final int getNumberPoints() {
    return numberPoints;
  }

  /**
   * olon > 0 is a quasi regular grid.
   * @deprecated
   * @return olon
   */
  public final int getOlon() {
    return olon;
  }

  /**
   * are extreme points in the quasi regular grid.
   * @deprecated
   * @return iolon
   */
  public final int getIolon() {
    return iolon;
  }

  /**
   * number of points in each parallel for quasi grids.
   * @deprecated
   * @return olonPts
   */
  public final int[] getOlonPoints() {
    return olonPts;
  }

  /**
   * Max number of points in parallel for quasi grids.
   *  @deprecated
   * @return maxPts
   */
  public final int getMaxPts() {
    return maxPts;
  }

  /**
   * Grid Definition Template Number .
   * @deprecated
   * @return gdtn
   */
  public final int getGdtn() {
    return gdtn;
  }

  /**
   * Grid name .
   * @deprecated
   * @return gridName
   */
  public final String getName() {
    return name;
  }

  /**
   * .
   * @deprecated
   * @return shape as a int
   */
  public final int getShape() {
    return shape;
  }

  /**
   * .
   * @deprecated
   * @return shapeName as a String
   */
  public final String getShapeName() {
    return getShapeName(shape);
  }

  /**
   * .
   * @deprecated
   * @param shape as an int
   * @return shapeName as a String
   */
  static public String getShapeName(int shape) {
    switch (shape) {  // code table 3.2

      case 0:
        return "Earth spherical with radius = 6367470 m";

      case 1:
        return "Earth spherical with radius specified by producer";

      case 2:
        return "Earth oblate spheroid with major axis = 6378160.0 m and minor axis = 6356775.0 m";

      case 3:
        return "Earth oblate spheroid with axes specified by producer";

      case 4:
        return "Earth oblate spheroid with major axis = 6378137.0 m and minor axis = 6356752.314 m";

      case 5:
        return "Earth represent by WGS84";

      case 6:
        return "Earth spherical with radius of 6371229.0 m";

      default:
        return "Unknown Earth Shape";
    }
  }

  /**
   * .
   * @deprecated
   * @return EarthRadius as a float
   */
  public final float getEarthRadius() {
    return earthRadius;
  }

  /**
   * .
   * @deprecated
   * @return MajorAxis as a float
   */
  public final float getMajorAxis() {
    return majorAxis;
  }

  /**
   * .
   * @deprecated
   * @return MinorAxis as a float
   */
  public final float getMinorAxis() {
    return minorAxis;
  }

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
   * .
   * @deprecated
   * @return Angle as a int
   */
  public final int getAngle() {
    return angle;
  }

  /**
   * .
   * @deprecated
   * @return Subdivisionsangle as a int
   */
  public final int getSubdivisionsangle() {
    return subdivisionsangle;
  }

  /**
   * .
   * @deprecated
   * @return La1 as a float
   */
  public final float getLa1() {
    return la1;
  }

  /**
   * .
   * @deprecated
   * @return Lo1 as a float
   */
  public final float getLo1() {
    return lo1;
  }

  /**
   * .
   *  @deprecated
   * @return Resolution as a int
   */
  public final int getResolution() {
    return resolution;
  }

  /**
   * .
   * @deprecated
   * @return La2 as a float
   */
  public final float getLa2() {
    return la2;
  }

  /**
   * .
   * @deprecated
   * @return Lo2 as a float
   */
  public final float getLo2() {
    return lo2;
  }

  /**
   * .
   * @deprecated
   * @return Lad as a float
   */
  public final float getLad() {
    return lad;
  }

  /**
   * .
   * @deprecated
   * @return Lov as a float
   */
  public final float getLov() {
    return lov;
  }

  /**
   * Get x-increment/distance between two grid points.
   * @deprecated
   * @return x-increment
   */
  public final float getDx() {
    return dx;
  }

  /**
   * Get y-increment/distance between two grid points.
   * @deprecated
   * @return y-increment
   */
  public final float getDy() {
    return dy;
  }

  /**
   * grid units
   * @deprecated
   * @return grid_units
   */
  public String getGrid_units() {
    return grid_units;
  }

  /**
   * .
   * @deprecated
   * @return ProjectionCenter as a int
   */
  public final int getProjectionCenter() {
    return projectionCenter;
  }

  /**
   * Get scan mode.
   * @deprecated
   * @return scan mode
   */
  public final int getScanMode() {
    return scanMode;
  }

  /**
   * .
   * @deprecated
   * @return Latin1 as a float
   */
  public final float getLatin1() {
    return latin1;
  }

  /**
   * .
   * @deprecated
   * @return Latin2 as a float
   */
  public final float getLatin2() {
    return latin2;
  }

  /**
   * .
   * @deprecated
   * @return SpLat as a float
   */
  public final float getSpLat() {
    return spLat;
  }

  /**
   * .
   * @deprecated
   * @return SpLon as a float
   */
  public final float getSpLon() {
    return spLon;
  }

  /**
   * .
   * @deprecated
   * @return Rotationangle as a float
   */
  public final float getRotationangle() {
    return rotationangle;
  }

  /**
   * .
   * @deprecated
   * @return PoleLat as a float
   */
  public final float getPoleLat() {
    return poleLat;
  }

  /**
   * .
   * @deprecated
   * @return PoleLon as a float
   */
  public final float getPoleLon() {
    return poleLon;
  }

  /**
   * .
   * @deprecated
   * @return Factor as a float
   */
  public final float getFactor() {
    return factor;
  }

  /**
   * .
   * @deprecated
   * @return N as a int
   */
  public final int getN() {
    return n;
  }

  /**
   * .
   * @deprecated
   * @return J as a float
   */
  public final float getJ() {
    return j;
  }

  /**
   * .
   * @deprecated
   * @return K as a float
   */
  public final float getK() {
    return k;
  }

  /**
   * .
   * @deprecated
   * @return M as a float
   */
  public final float getM() {
    return m;
  }

  /**
   * .
   * @deprecated
   * @return Method as a int
   */
  public final int getMethod() {
    return method;
  }

  /**
   * .
   * @deprecated
   * @return Mode as a int
   */
  public final int getMode() {
    return mode;
  }

  /**
   * .
   * @deprecated
   * @return Lap as a float
   */
  public final float getLap() {
    return lap;
  }

  /**
   * .
   * @deprecated
   * @return Lop as a float
   */
  public final float getLop() {
    return lop;
  }

  /**
   * .
   * @deprecated
   * @return Xp as a float
   */
  public final float getXp() {
    return xp;
  }

  /**
   * .
   * @deprecated
   * @return Yp as a float
   */
  public final float getYp() {
    return yp;
  }

  /**
   * .
   * @deprecated
   * @return Xo as a float
   */
  public final float getXo() {
    return xo;
  }

  /**
   * .
   * @deprecated
   * @return Yo as a float
   */
  public final float getYo() {
    return yo;
  }

  /**
   * .
   * @deprecated
   * @return Altitude as a float
   */
  public final float getAltitude() {
    return altitude;
  }

  /**
   * .
   * @deprecated
   * @return N2 as a int
   */
  public final int getN2() {
    return n2;
  }

  /**
   * .
   * @deprecated
   * @return N3 as a int
   */
  public final int getN3() {
    return n3;
  }

  /**
   * .
   * @deprecated
   * @return Ni as a int
   */
  public final int getNi() {
    return ni;
  }

  /**
   * .
   * @deprecated
   * @return Nd as a int
   */
  public final int getNd() {
    return nd;
  }

  /**
   * .
   * @deprecated
   * @return Position as a int
   */
  public final int getPosition() {
    return position;
  }

  /**
   * .
   * @deprecated
   * @return Order as a int
   */
  public final int getOrder() {
    return order;
  }

  /**
   * .
   * @deprecated
   * @return Nb as a float
   */
  public final float getNb() {
    return nb;
  }

  /**
   * .
   * @deprecated
   * @return Nr as a float
   */
  public final float getNr() {
    return nr;
  }

  /**
   * .
   * @deprecated
   * @return Dstart as a float
   */
  public final float getDstart() {
    return dstart;
  }

  /**
   * .
   * @deprecated
   * @return CheckSum as a String
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
    //return gdskey;
    return gdsVars.getGdsKey();
  }

  /**
   * GDS as Grib2GDSVariables
   *
   * @return Grib2GDSVariables GDS vars
   */
  public Grib2GDSVariables getGdsVars() {
    return gdsVars;
  }

  @Override
  public String toString() {
    return "Grib2GridDefinitionSection{" +
            "\n   length=" + length +
            "\n   section=" + section +
            "\n   source=" + source +
            "\n   numberPoints=" + numberPoints +
            "\n   olon=" + olon +
            "\n   iolon=" + iolon +
            "\n   olonPts=" + olonPts +
            "\n   maxPts=" + maxPts +
            "\n   gdtn=" + gdtn +
            "\n   name='" + name + '\'' +
            "\n   shape=" + shape +
            "\n   earthRadius=" + earthRadius +
            "\n   majorAxis=" + majorAxis +
            "\n   minorAxis=" + minorAxis +
            "\n   nx=" + nx +
            "\n   ny=" + ny +
            "\n   angle=" + angle +
            "\n   subdivisionsangle=" + subdivisionsangle +
            "\n   la1=" + la1 +
            "\n   lo1=" + lo1 +
            "\n   resolution=" + resolution +
            "\n   la2=" + la2 +
            "\n   lo2=" + lo2 +
            "\n   lad=" + lad +
            "\n   lov=" + lov +
            "\n   dx=" + dx +
            "\n   dy=" + dy +
            "\n   grid_units='" + grid_units + '\'' +
            "\n   projectionCenter=" + projectionCenter +
            "\n   scanMode=" + scanMode +
            "\n   latin1=" + latin1 +
            "\n   latin2=" + latin2 +
            "\n   spLat=" + spLat +
            "\n   spLon=" + spLon +
            "\n   rotationangle=" + rotationangle +
            "\n   poleLat=" + poleLat +
            "\n   poleLon=" + poleLon +
            "\n   lonofcenter=" + lonofcenter +
            "\n   factor=" + factor +
            "\n   n=" + n +
            "\n   j=" + j +
            "\n   k=" + k +
            "\n   m=" + m +
            "\n   method=" + method +
            "\n   mode=" + mode +
            "\n   xp=" + xp +
            "\n   yp=" + yp +
            "\n   lap=" + lap +
            "\n   lop=" + lop +
            "\n   xo=" + xo +
            "\n   yo=" + yo +
            "\n   altitude=" + altitude +
            "\n   n2=" + n2 +
            "\n   n3=" + n3 +
            "\n   ni=" + ni +
            "\n   nd=" + nd +
            "\n   position=" + position +
            "\n   order=" + order +
            "\n   nb=" + nb +
            "\n   nr=" + nr +
            "\n   dstart=" + dstart +
            "\n   checksum='" + checksum + '\'' +
            "\n   gdskey=" + gdskey +
            "\n   gdsVars=" + gdsVars +
            '}';
  }
}  // end Grib2GridDefinitionSection


