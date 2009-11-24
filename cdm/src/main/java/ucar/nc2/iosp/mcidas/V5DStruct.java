//
// V5DStruct.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2009 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

//package visad.data.vis5d;
package ucar.nc2.iosp.mcidas;


import visad.data.BadFormException;
// original V5DStruct uses ucar.unidata.netcdf.RandomAccessFile
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.net.URL;

/** An object representing the structure of a .v5d file.<P> */
public class V5DStruct {

  /** Amount of physical RAM in megabytes.
      Vis5D normally uses a bounded amount of memory to avoid swapping.
      When the limit is reached, the least-recently-viewed graphics will
      be deallocated.  If MBS is set to 0, however, vis5d will use ordinary
      malloc/free and not deallocate graphics (ok for systems with a lot
      of memory (&gt;=128MB)).
  */
  private static final int MBS = 32;

  /** Default topography file */
  private static final String TOPOFILE = "EARTH.TOPO";

  /** Default world map lines file */
  private static final String WORLDFILE = "OUTLSUPW";

  /** Default USA map lines file */
  private static final String USAFILE = "OUTLUSAM";

  /** Default filename of Tcl startup commands */
  private static final String TCL_STARTUP_FILE = "vis5d.tcl";

  /** Default directory to search for user functions */
  private static final String FUNCTION_PATH = "userfuncs";

  /** Default animation rate in milliseconds */
  private static final int ANIMRATE = 100;

  /** Default scale value for logarithmic vertical coordinate system */
  private static final double DEFAULT_LOG_SCALE = 1012.5;

  /** Default exponent value for logarithmic vertical coordinate system */
  private static final double DEFAULT_LOG_EXP = -7.2;


  // ******************************************************************** //
  // ******************************************************************** //
  // **          USERS:  DON'T CHANGE THE FOLLOWING CONSTANTS          ** //
  // ******************************************************************** //
  // ******************************************************************** //

  /** Define BIG_GFX to allow larger isosurfaces, contour slices, etc., if
      there's enough memory.
  */
  private static final boolean BIG_GFX = true;

  /** Shared by code above and below API */
  private static final int MAX_LABEL = 1000;

  /** Shared by code above and below API */
  private static final int MAX_FUNCS = 100;


  /** A numeric version number which we can test for in utility programs which
      use the v5d functions. If V5D_VERSION is not defined, then its value
      is considered to be zero.
  */
  private static final int V5D_VERSION = 42;

  /** Represents a missing value */
  private static final float MISSING = Float.NaN;

  /** Tests whether a given value is missing */
  private static boolean IS_MISSING(float x) {
    return Float.isNaN(x) || x >= 1.0e30;
  }

  /** Limit on 5-D grid variables */
  private static final int MAXVARS = 200;

  /** Limit on 5-D grid times */
  private static final int MAXTIMES = 400;

  /** Limit on 5-D grid rows */
  private static final int MAXROWS = 400;

  /** Limit on 5-D grid columns */
  private static final int MAXCOLUMNS = 400;

  /** Limit on 5-D grid levels */
  private static final int MAXLEVELS = 400;


  // ********************************************************************** //
  // **                                                                  ** //
  // ** Definition of v5d struct and related information.                ** //
  // **                                                                  ** //
  // ********************************************************************** //

  private static final int MAXPROJARGS = (MAXROWS+MAXCOLUMNS+1);
  private static final int MAXVERTARGS = (MAXLEVELS+1);

  /** File version. This should be updated when the file version changes. */
  private static final String FILE_VERSION = "4.3";

  // TODO: find optimal value of default buffer size
  private static final int DEFAULT_FILE_BUFFER = 204800;
  private static final int DEFAULT_HTTP_BUFFER = 204800;

  /*
   * New grid file format for VIS-5D:
   *
   * The header is a list of tagged items.  Each item has 3 parts:
   *    1. A tag which is a 4-byte integer identifying the type of item.
   *    2. A 4-byte integer indicating how many bytes of data follow.
   *    3. The binary data.
   *
   * If we need to add new information to a file header we just create a
   * new tag and add the code to read/write the information.
   *
   * If we're reading a header and find an unknown tag, we can use the
   * length field to skip ahead to the next tag.  Therefore, the file
   * format is forward (and backward) compatible.
   *
   * Grid data is stored as either:
   *     1-byte unsigned integers  (255=missing)
   *     2-byte unsigned integers  (65535=missing)
   *     4-byte IEEE floats        (>1.0e30 = missing)
   *
   * All numeric values are stored in big endian order.  All floating point
   * values are in IEEE format.
   */

  /*
   * Currently defined tags:
   * Note:  the notation a[i] doesn't mean a is an array of i elements,
   * rather it just refers to the ith element of a[].
   *
   * Tags marked as PHASED OUT should be readable but are no longer written.
   * Old tag numbers can't be reused!
   *
   */

  /** hex encoding of &quot;V5D\n&quot; */
  public static final int TAG_ID              = 0x5635440a;

  // general stuff 1000+

  /** char*10 FileVersion */
  public static final int TAG_VERSION         = 1000;

  /** int*4 NumTimes */
  public static final int TAG_NUMTIMES        = 1001;

  /** int*4 NumVars */
  public static final int TAG_NUMVARS         = 1002;

  /** int*4 var; char*10 VarName[var] */
  public static final int TAG_VARNAME         = 1003;

  /** int*4 Nr */
  public static final int TAG_NR              = 1004;

  /** int*4 Nc */
  public static final int TAG_NC              = 1005;

  /** int*4 Nl  (Nl for all vars) */
  public static final int TAG_NL              = 1006;

  /** int*4 var; int*4 Nl[var] */
  public static final int TAG_NL_VAR          = 1007;

  /** int*4 var; int*4 LowLev[var] */
  public static final int TAG_LOWLEV_VAR      = 1008;

  /** int*4 t; int*4 TimeStamp[t] */
  public static final int TAG_TIME            = 1010;

  /** int*4 t; int*4 DateStamp[t] */
  public static final int TAG_DATE            = 1011;

  /** int*4 var; real*4 MinVal[var] */
  public static final int TAG_MINVAL          = 1012;

  /** int*4 var; real*4 MaxVal[var] */
  public static final int TAG_MAXVAL          = 1013;

  /** int*4 CompressMode; (#bytes/grid) */
  public static final int TAG_COMPRESS        = 1014;

  /** int *4 var; char*20 Units[var] */
  public static final int TAG_UNITS           = 1015;

  // vertical coordinate system 2000+

  /** int*4 VerticalSystem */
  public static final int TAG_VERTICAL_SYSTEM = 2000;
  /** int*4 n;  real*4 VertArgs[0..n-1] */
  public static final int TAG_VERT_ARGS       = 2100;

  /** real*4 BottomBound (PHASED OUT) */
  public static final int TAG_BOTTOMBOUND     = 2001;
  /** real*4 LevInc (PHASED OUT) */
  public static final int TAG_LEVINC          = 2002;
  /** int*4 l;  real*4 Height[l] (PHASED OUT) */
  public static final int TAG_HEIGHT          = 2003;

  // projection 3000+

  /** int*4 projection.
      <li> 0 = generic linear
      <li> 1 = cylindrical equidistant
      <li> 2 = Lambert conformal/Polar Stereo
      <li> 3 = rotated equidistant
  */
  public static final int TAG_PROJECTION      = 3000;

  /** int*4 n; real*4 ProjArgs[0..n-1] */
  public static final int TAG_PROJ_ARGS       = 3100;

  /** real*4 NorthBound (PHASED OUT) */
  public static final int TAG_NORTHBOUND      = 3001;

  /** real*4 WestBound (PHASED OUT) */
  public static final int TAG_WESTBOUND       = 3002;

  /** real*4 RowInc (PHASED OUT) */
  public static final int TAG_ROWINC          = 3003;

  /** real*4 ColInc (PHASED OUT) */
  public static final int TAG_COLINC          = 3004;

  /** real*4 Lat1 (PHASED OUT) */
  public static final int TAG_LAT1            = 3005;

  /** real*4 Lat2 (PHASED OUT) */
  public static final int TAG_LAT2            = 3006;

  /** real*4 PoleRow (PHASED OUT) */
  public static final int TAG_POLE_ROW        = 3007;

  /** real*4 PoleCol (PHASED OUT) */
  public static final int TAG_POLE_COL        = 3008;

  /** real*4 CentralLon (PHASED OUT) */
  public static final int TAG_CENTLON         = 3009;

  /** real*4 CentralLat (PHASED OUT) */
  public static final int TAG_CENTLAT         = 3010;

  /** real*4 CentralRow (PHASED OUT) */
  public static final int TAG_CENTROW         = 3011;

  /** real*4 CentralCol (PHASED OUT) */
  public static final int TAG_CENTCOL         = 3012;

  /** real*4 Rotation (PHASED OUT) */
  public static final int TAG_ROTATION        = 3013;

  public static final int TAG_END             = 9999;


  // ******************************* //
  // *** Start of main v5dstruct *** //
  // ******************************* //

  // PUBLIC (user can freely read, sometimes write, these fields)

  /** Number of time steps */
  public int NumTimes;

  /** Number of variables */
  public int NumVars;

  /** Number of rows */
  public int Nr;

  /** Number of columns */
  public int Nc;

  /** Number of levels per variable */
  public int[] Nl = new int[MAXVARS];

  /** Lowest level per variable */
  public int[] LowLev = new int[MAXVARS];

  /** 9-character variable names */
  public char[][] VarName = new char[MAXVARS][10];

  /** 19-character units for variables */
  public char[][] Units = new char[MAXVARS][20];

  /** Time in HHMMSS format */
  public int[] TimeStamp = new int[MAXTIMES];

  /** Date in YYDDD format */
  public int[] DateStamp = new int[MAXTIMES];

  /** Minimum variable data values */
  public float[] MinVal = new float[MAXVARS];

  /** Maximum variable data values */
  public float[] MaxVal = new float[MAXVARS];

  // This info is used for external function computation

  /** McIDAS file number in 1..9999 */
  public short[][] McFile = new short[MAXTIMES][MAXVARS];

  /** McIDAS grid number in 1..? */
  public short[][] McGrid = new short[MAXTIMES][MAXVARS];

  /** Which vertical coordinate system */
  public int VerticalSystem;

  /** Vertical Coordinate System arguments. <br><br>
      <pre>
      IF VerticalSystem==0 THEN
          -- Linear scale, equally-spaced levels in generic units
          VertArgs[0] = Height of bottom-most grid level in generic units
          VertArgs[1] = Increment between levels in generic units
      ELSE IF VerticalSystem==1 THEN
          -- Linear scale, equally-spaced levels in km
          VertArgs[0] = Height of bottom grid level in km
          VertArgs[1] = Increment between levels in km
      ELSE IF VerticalSystem==2 THEN
          -- Linear scale, Unequally spaced levels in km
          VertArgs[0] = Height of grid level 0 (bottom) in km
          ...                ...
          VertArgs[n] = Height of grid level n in km
      ELSE IF VerticalSystem==3 THEN
          -- Linear scale, Unequally spaced levels in mb
          VertArgs[0] = Pressure of grid level 0 (bottom) in mb
          ...             ...
          VertArgs[n] = Pressure of grid level n in mb
      ENDIF
      </pre>
  */
  public float[] VertArgs = new float[MAXVERTARGS];


  /** Which map projection */
  public int Projection;

  /** Map projection arguments. <br><br>
      <pre>
      IF Projection==0 THEN
          -- Rectilinear grid, generic units
          ProjArgs[0] = North bound, Y coordinate of grid row 0
          ProjArgs[1] = West bound, X coordiante of grid column 0
          ProjArgs[2] = Increment between rows
          ProjArgs[3] = Increment between colums
          NOTES: X coordinates increase to the right, Y increase upward.
          NOTES: Coordinate system is right-handed.
     ELSE IF Projection==1 THEN
          -- Cylindrical equidistant (Old VIS-5D)
          -- Rectilinear grid in lat/lon
          ProjArgs[0] = Latitude of grid row 0, north bound, in degrees
          ProjArgs[1] = Longitude of grid column 0, west bound, in deg.
          ProjArgs[2] = Increment between rows in degrees
          ProjArgs[3] = Increment between rows in degrees
          NOTES: Coordinates (degrees) increase to the left and upward.
     ELSE IF Projection==2 THEN
          -- Lambert conformal
          ProjArgs[0] = Standared Latitude 1 of conic projection
          ProjArgs[1] = Standared Latitude 2 of conic projection
          ProjArgs[2] = Row of North/South pole
          ProjArgs[3] = Column of North/South pole
          ProjArgs[4] = Longitude which is parallel to columns
          ProjArgs[5] = Increment between grid columns in km
      ELSE IF Projection==3 THEN
          -- Polar Stereographic
          ProjArgs[0] = Latitude of center of projection
          ProjArgs[1] = Longitude of center of projection
          ProjArgs[2] = Grid row of center of projection
          ProjArgs[3] = Grid column of center of projection
          ProjArgs[4] = Increment between grid columns at center in km
      ELSE IF Projection==4 THEN
          -- Rotated
          ProjArgs[0] = Latitude on rotated globe of grid row 0
          ProjArgs[1] = Longitude on rotated globe of grid column 0
          ProjArgs[2] = Degrees of latitude on rotated globe between
                        grid rows
          ProjArgs[3] = Degrees of longitude on rotated globe between
                        grid columns
          ProjArgs[4] = Earth latitude of (0, 0) on rotated globe
          ProjArgs[5] = Earth longitude of (0, 0) on rotated globe
          ProjArgs[6] = Clockwise rotation of rotated globe in degrees
      ENDIF
      </pre>
  */
  public float[] ProjArgs = new float[MAXPROJARGS];

  /** 1, 2 or 4 = # bytes per grid point */
  public int CompressMode;

  /** 9-character version number */
  public String FileVersion;

  // PRIVATE (not to be touched by user code)

  /** COMP5D file version or 0 if .v5d */
  private int FileFormat;

  /** Java file descriptor */
  private RandomAccessFile FileDesc;

  /** 'r' = read, 'w' = write */
  char Mode;

  /** current position of file pointer */
  int CurPos;

  /** position of first grid in file */
  int FirstGridPos;

  /** size of each grid */
  int[] GridSize = new int[MAXVARS];

  /** sum of GridSize[0..NumVars-1] */
  int SumGridSizes;


  // ************************* //
  // *** GATEWAY FUNCTIONS *** //
  // ************************* //

  /** Open a Vis5D file */
  public 
    static 
      //V5DStruct v5d_open( byte[] name, int name_length, int[] sizes,
      V5DStruct v5d_open( RandomAccessFile raf, int[] sizes,
                          int[] n_levels,
                          String[] var_names, String[] var_units,
                          int[] map_proj, float[] projargs,
                          int[] vert_sys, float[] vert_args, double[] times)
    throws IOException, BadFormException
  {
    int i, j, k, k2, m, m2;
    int day, time, first_day, first_time;
    // char[] filename = new char[200];
    byte[] varnames = new byte[10*MAXVARS];
    byte[] varunits = new byte[20*MAXVARS];

    // open file
    // Modified 01-Feb-2005 DRM:  caused problems on Mac-OSX
    //for (i=0; i<name_length; i++) filename[i] = (char) name[i];
    //filename[name_length] = 0;
    //V5DStruct v = v5dOpenFile(new String(filename));
    //V5DStruct v = v5dOpenFile(new String( name, 0, name_length)); 
    V5DStruct v = v5dOpenFile(raf); 

    if (v != null) {
      // get basic sizes
      sizes[0] = v.Nr;
      sizes[1] = v.Nc;
      sizes[3] = v.NumTimes;
      sizes[4] = v.NumVars;

      // compute varnames
      for (j=0; j<v.NumVars; j++) {
        k = 10 * j;
        for (i=0; i<10; i++) {
          if (v.VarName[j][i] != 0 && i < 9) {
            varnames[k + i] = (byte) v.VarName[j][i];
          }
          else {
            varnames[k + i] = 0;
            break;
          }
        }
      }

      // compute varunits
      for (j=0; j<v.NumVars; j++) {
        k = 20 * j;
        for (i=0; i<20; i++) {
          if (v.Units[j][i] != 0 && i < 19) {
            varunits[k + i] = (byte) v.Units[j][i];
          }
          else {
            varunits[k + i] = 0;
            break;
          }
        }
      }

    //- make var/unit Strings:

      for (i=0; i<v.NumVars; i++) {
        k = 10 * i;
        k2 = 20 * i;
        m = k;
        m2 = k2;
        while (varnames[m] != 0) {m++;}
        while (varunits[m2] != 0) {m2++;}
        var_names[i] = new String(varnames, k, m - k);
        var_units[i] = new String(varunits, k2, m2 - k2);
      }


      // compute maximum level, and make sure all levels are equal
      int maxNl = v.Nl[0];
      for (i=0; i<v.NumVars; i++) {
        if (v.Nl[i] > maxNl) maxNl = v.Nl[i];
  /*----if (v.Nl[i] != maxNl) sizes[0] = -1; */
      }
 
      sizes[2] = maxNl;
      for (i = 0; i < v.NumVars; i++) {
        n_levels[i] = v.Nl[i];
      }

      vert_sys[0] = v.VerticalSystem;

      for ( int kk = 0; kk < maxNl; kk++) {
        vert_args[kk] = v.VertArgs[kk];
      }

      // compute times
      first_day = v5dYYDDDtoDays(v.DateStamp[0]);
      first_time = v5dHHMMSStoSeconds(v.TimeStamp[0]);
      for (i=0; i<v.NumTimes; i++) {
        day = v5dYYDDDtoDays(v.DateStamp[i]);
        time = v5dHHMMSStoSeconds(v.TimeStamp[i]);
        /*-TDR
        times[i] = (day - first_day) * 24 * 60 * 60 + (time - first_time);
         */
     //-float ff = (((float)day)*24f*60f*60f + (float)time);
        double ff = (((double)day)*24.0*60.0*60.0 + (double)time);
        times[i] = ff;
      }

      map_proj[0] = v.Projection;

      for (int kk = 0; kk < MAXPROJARGS; kk++) {
        projargs[kk] = v.ProjArgs[kk];
      }
    }
    else {  //- trouble with file
      // v == null
      sizes[0] = -1;
    }

    return v;
  }

  /** Read from a Vis5D file */
  public void v5d_read(int time, int vr, float[] ranges, float[] data)
    throws IOException, BadFormException
  {

    boolean status;

    ranges[0] = MinVal[vr];
    ranges[1] = MaxVal[vr];
    status = v5dReadGrid(time, vr, data);
    if (!status) {
      ranges[0] = 1.0f;
      ranges[1] = -1.0f;
    }
  }


  // ******************************************************************** //
  // ****                  Miscellaneous Functions                   **** //
  // ******************************************************************** //

  private static boolean SIMPLE_COMPRESSION = false;
  private static boolean KLUDGE = false;
  private static boolean ORIGINAL = false;

  /** Convert a signed byte to an unsigned one, and return it in an int */
  public static int getUnsignedByte(byte b) {
    int i = (b >= 0 ? (int) b : (int) b + 256);
    return i;
  }

  /** Convert two signed bytes to an unsigned short, and return it in an int */
  public static int getUnsignedShort(byte b1, byte b2) {
    int i1 = getUnsignedByte(b1);
    int i2 = getUnsignedByte(b2);
    return 256 * i1 + i2;
  }
  /** Convert four signed bytes to an unsigned short, and return it in an int */
  public static int getUnsignedInt(byte b1, byte b2, byte b3, byte b4) {
    int i1 = getUnsignedByte(b1);
    int i2 = getUnsignedByte(b2);
    int i3 = getUnsignedByte(b3);
    int i4 = getUnsignedByte(b4);
    return 16777216*i1 + 65536*i2 + 256*i3 + i4;
  }

  private static float pressure_to_height(float pressure) {
    return (float) (DEFAULT_LOG_EXP *
      Math.log((double) pressure / DEFAULT_LOG_SCALE));
  }

  private static float height_to_pressure(float height) {
    return (float) (DEFAULT_LOG_SCALE *
      Math.exp((double) height / DEFAULT_LOG_EXP));
  }

  /** Copy up to maxlen characters from src to dst stopping upon whitespace
      in src. Terminate dst with null character.
      @return length of dst
  */
  private static int copy_string2(char[] dst, char[] src, int maxlen) {
    int i;

    for (i=0;i<maxlen;i++) dst[i] = src[i];
    for (i=maxlen-1; i>=0; i--) {
      if (dst[i] == ' ' || i == maxlen - 1) dst[i] = 0;
      else break;
    }
    return new String(dst).length();
  }

  /** Copy up to maxlen characters from src to dst stopping upon whitespace
      in src. Terminate dst with null character.
      @return length of dst
  */
  private static int copy_string(char[] dst, char[] src, int maxlen) {
    int i;

    for (i=0; i<maxlen; i++) {
      if (src[i] == ' ' || i == maxlen - 1) {
        dst[i] = 0;
        break;
      }
      else dst[i] = src[i];
    }
    return i;
  }

  /** Convert a date from YYDDD format to days since Jan 1, 1900. */
  private static int v5dYYDDDtoDays(int yyddd) {
    int iy, id, idays;

    iy = yyddd / 1000;
    id = yyddd - 1000 * iy;
    // DRM - 17-Nov-2001 handle YYYYDDD form dates 
    if (iy >= 1900)
      iy -= 1900;
    else if (iy < 50)
      // WLH 31 July 96 << 31 Dec 99
      iy += 100; 
    // Updated from Vis5D+ (sub 1 since they do days from 12/31/1899)
    //idays = 365 * iy + (iy - 1) / 4 + id;
    idays = (365*iy + (iy-1)/4 - (iy-1)/100 + (iy+299)/400 + id) - 1;

    return idays;
  }

  /** Convert a time from HHMMSS format to seconds since midnight. */
  private static int v5dHHMMSStoSeconds(int hhmmss) {
    int h, m, s;

    h = hhmmss / 10000;
    m = (hhmmss / 100) % 100;
    s = hhmmss % 100;

    return s + m * 60 + h * 60 * 60;
  }

  /** Convert a day since Jan 1, 1900 to YYDDD format. */
  private static int v5dDaysToYYDDD(int days) {
    int iy, id, iyyddd;

    iy = (4 * days) / 1461;
    id = days - (365 * iy + (iy - 1) / 4);
    // WLH 31 July 96 << 31 Dec 99
    // iy = iy + 1900; is the right way to fix this, but requires
    // changing all places where dates are printed - procrastinate
    if (iy > 99) iy = iy - 100;
    iyyddd = iy * 1000 + id;

    return iyyddd;
  }

  /** Convert a time in seconds since midnight to HHMMSS format. */
  private static int v5dSecondsToHHMMSS(int seconds) {
    int hh, mm, ss;

    hh = seconds / (60 * 60);
    mm = (seconds / 60) % 60;
    ss = seconds % 60;
    return hh * 10000 + mm * 100 + ss;
  }

  /** Open a v5d file for reading.
      @return null if error, else a pointer to a new V5DStruct
  private static V5DStruct v5dOpenFile(String filename)
          throws IOException, BadFormException {
    RandomAccessFile fd = 
      (filename.toLowerCase().startsWith("http"))
          ? new HTTPRandomAccessFile(new URL(filename), DEFAULT_HTTP_BUFFER )
          : new RandomAccessFile(filename, "r", DEFAULT_FILE_BUFFER);

    if (fd == null) {
      // error
      return null;
    }

    V5DStruct v = new V5DStruct();

    v.FileDesc = fd;
    v.Mode = 'r';
    return (v.read_v5d_header() ? v : null);
  }
  */

  /** Open a v5d file for reading.
      @return null if error, else a pointer to a new V5DStruct
  */
  public static V5DStruct v5dOpenFile(RandomAccessFile fd)
          throws IOException, BadFormException {
    if (fd == null) {
      // error
      System.out.println("null file");
      return null;
    }

    V5DStruct v = new V5DStruct();

    v.FileDesc = fd;
    v.Mode = 'r';
    return (v.read_v5d_header() ? v : null);
  }

  /** Compute the ga and gb (de)compression values for a grid.
      @param nr            number of rows of grid
      @param nc            number of columns of grid
      @param nl            number of levels of grid
      @param data          the grid data
      @param ga            array to store results
      @param gb            array to store results
      @param minval        one-element float array for storing min value
      @param maxval        one-element float array for storing max value
      @param compressmode  1, 2 or 4 bytes per grid point
  */
  private static void compute_ga_gb(int nr, int nc, int nl, float[] data,
    int compressmode, float[] ga, float[] gb, float[] minval, float[] maxval)
          throws BadFormException{
    if (SIMPLE_COMPRESSION) {
      // compute ga, gb values for whole grid
      int i, lev, num;
      boolean allmissing;
      float min, max, a, b;

      min = 1.0e30f;
      max = -1.0e30f;
      num = nr * nc * nl;
      allmissing = true;
      for (i=0; i<num; i++) {
        if (!IS_MISSING(data[i])) {
          if (data[i] < min)  min = data[i];
          if (data[i] > max)  max = data[i];
          allmissing = false;
        }
      }
      if (allmissing) {
        a = 1.0f;
        b = 0.0f;
      }
      else {
        a = (float) ((max - min) / 254.0);
        b = min;
      }

      // return results
      for (i=0; i<nl; i++) {
        ga[i] = a;
        gb[i] = b;
      }

      minval[0] = min;
      maxval[0] = max;
    }
    else {
      // compress grid on level-by-level basis
      final float SMALLVALUE = -1.0e30f;
      final float BIGVALUE = 1.0e30f;
      float gridmin, gridmax;
      float[] levmin = new float[MAXLEVELS];
      float[] levmax = new float[MAXLEVELS];
      float[] d = new float[MAXLEVELS];
      float dmax;
      float ival, mval;
      int j, k, lev, nrnc;

      nrnc = nr * nc;

      // find min and max for each layer and the whole grid
      gridmin = BIGVALUE;
      gridmax = SMALLVALUE;
      j = 0;
      for (lev=0; lev<nl; lev++) {
        float min, max;
        min = BIGVALUE;
        max = SMALLVALUE;
        for (k=0; k<nrnc; k++) {
          if (!IS_MISSING(data[j]) && data[j] < min) min = data[j];
          if (!IS_MISSING(data[j]) && data[j] > max) max = data[j];
          j++;
        }
        if (min < gridmin) gridmin = min;
        if (max > gridmax) gridmax = max;
        levmin[lev] = min;
        levmax[lev] = max;
      }

      // WLH 2-2-95
      if (KLUDGE) {
        // if the grid minimum is within delt of 0.0, fudge all values
        // within delt of 0.0 to delt, and recalculate mins and maxes
        float delt;
        int nrncnl = nrnc * nl;

        delt = (float) ((gridmax - gridmin) / 100000.0);
        if (Math.abs(gridmin) < delt && gridmin != 0.0 && compressmode != 4) {
          float min, max;
          for (j=0; j<nrncnl; j++) {
            if (!IS_MISSING(data[j]) && data[j] < delt) data[j] = delt;
          }
          // re-calculate min and max for each layer and the whole grid
          gridmin = delt;
          for (lev=0; lev<nl; lev++) {
            if (Math.abs(levmin[lev]) < delt) levmin[lev] = delt;
            if (Math.abs(levmax[lev]) < delt) levmax[lev] = delt;
          }
        }
      }

      // find d[lev] and dmax = MAX(d[0], d[1], ... d[nl-1])
      dmax = 0.0f;
      for (lev=0; lev<nl; lev++) {
        if (levmin[lev] >= BIGVALUE && levmax[lev] <= SMALLVALUE) {
          // all values in the layer are MISSING
          d[lev] = 0.0f;
        }
        else {
          d[lev] = levmax[lev] - levmin[lev];
        }
        if (d[lev] > dmax) dmax = d[lev];
      }

      // compute ga (scale) and gb (bias) for each grid level
      if (dmax == 0.0) {
        // special cases
        if (gridmin == gridmax) {
          // whole grid is of same value
          for (lev=0; lev<nl; lev++) {
            ga[lev] = gridmin;
            gb[lev] = 0.0f;
          }
        }
        else {
          // every layer is of a single value
          for (lev=0; lev<nl; lev++) {
            ga[lev] = levmin[lev];
            gb[lev] = 0.0f;
          }
        }
      }
      else {
        // normal cases
        if (compressmode == 1) {
          ORIGINAL = true;
          if (ORIGINAL) {
            ival = dmax / 254.0f;
            mval = gridmin;
            for (lev=0; lev<nl; lev++) {
              ga[lev] = ival;
              gb[lev] = mval + ival * (int) ((levmin[lev] - mval) / ival);
            }
          }
          else {
            for (lev=0; lev<nl; lev++) {
              if (d[lev] == 0.0) ival = 1.0f;
              else ival = d[lev] / 254.0f;
              ga[lev] = ival;
              gb[lev] = levmin[lev];
            }
          }
        }
        else if (compressmode == 2) {
          ival = dmax / 65534.0f;
          mval = gridmin;
          for (lev=0; lev<nl; lev++) {
            ga[lev] = ival;
            gb[lev] = mval + ival * (int) ((levmin[lev] - mval) / ival);
          }
        }
        else {
          V5Dassert(compressmode == 4);
          for (lev=0; lev<nl; lev++) {
            ga[lev] = 1.0f;
            gb[lev] = 0.0f;
          }
        }
      }

      // update min, max values
      minval[0] = gridmin;
      maxval[0] = gridmax;
    }
  }

  /** Decompress a 3-D grid from 1-byte integers to 4-byte floats.
      @param nr            number of rows of grid
      @param nc            number of columns of grid
      @param nl            number of levels of grid
      @param compdata1     array of [nr*nc*nl*compressmode] bytes
      @param ga            array of decompression factors
      @param gb            array of decompression factors
      @param data          array to put decompressed values
      @param compressmode  1, 2 or 4 bytes per grid point
  */
  private void v5dDecompressGrid(int nr, int nc, int nl, int compressmode,
    byte[] compdata1, float[] ga, float[] gb, float[] data)
  {
    int nrnc = nr * nc;
    int nrncnl = nr * nc * nl;

    if (compressmode == 1) {
      int p, i, lev;
      p = 0;
      for (lev=0; lev<nl; lev++) {
        float a = ga[lev];
        float b = gb[lev];

        // WLH 2-2-95
        float d = 0f;
        float aa = 0f;
        int id;
        if (a > 0.0000000001) {
          d = b / a;
          id = (int) Math.floor(d);
          d = d - id;
          aa = (float) (a * 0.000001);
        }
        else id = 1;
        if (-254 <= id && id <= 0 && d < aa) {
          for (i=0; i<nrnc; i++, p++) {
            int cd1p = getUnsignedByte(compdata1[p]);
            if (cd1p == 255) data[p] = MISSING;
            else {
              data[p] = (float) cd1p * a + b;
              if (Math.abs(data[p]) < aa) data[p] = aa;
            }
          }
        }
        else {
          for (i=0; i<nrnc; i++, p++) {
            int cd1p = getUnsignedByte(compdata1[p]);
            if (cd1p == 255) data[p] = MISSING;
            else data[p] = (float) cd1p * a + b;
          }
        }
        // end of WLH 2-2-95
      }
    }
    else if (compressmode == 2) {
      int p, i, lev;
      p = 0;
      for (lev=0; lev<nl; lev++) {
        float a = ga[lev];
        float b = gb[lev];
        // sizeof(short)==2!
        for (i=0; i<nrnc; i++, p++) {
          int cd1p = getUnsignedShort(compdata1[2 * p], compdata1[2 * p + 1]);
          if (cd1p == 65535) data[p] = MISSING;
          else data[p] = (float) cd1p * a + b;
        }
      }
    }
    else {
      // compressmode==4
      // other machines: just copy 4-byte IEEE floats
      // CTR: I have no idea if this works properly in Java...
      /*-TDR: Nope this don't work, throws ArrayStoreException
      System.arraycopy(data, 0, compdata1, 0, nrncnl*4);
       */
      for (int i=0; i<nrncnl; i++) {
        int a = getUnsignedInt(compdata1[i*4], compdata1[i*4 + 1],
                               compdata1[i*4 + 2], compdata1[i*4 + 3]);
        data[i] = Float.intBitsToFloat(a);
      }
    }
  }

  /** Verifies that a certain condition holds */
  private static final void V5Dassert(boolean b)
          throws BadFormException {
    if (!b) {
      throw new BadFormException("Warning: assert failed");
    }
  }

  /** Read a block of memory.
      @param f         file descriptor
      @param data      address of first byte
      @param elements  number of elements to read
      @param elsize    size of each element to read (1, 2 or 4)
      @return number of elements written
  */
  private static int read_block(RandomAccessFile f, byte[] data,
    int elements, int elsize) throws IOException
  {
    int n;
    if (elsize == 1) {
      n = f.read(data, 0, elements);
    }
    else if (elsize == 2) {
      n = f.read(data, 0, elements*2) / 2;
    }
    else if (elsize == 4) {
      n = f.read(data, 0, elements*4) / 4;
    }
    else {
      throw new IOException("Fatal error in read_block(): " +
        "bad elsize (" + elsize + ")");
    }
    return n;
  }

  /** Read an array of 4-byte IEEE floats.
      @param f  file descriptor
      @param x  address to put floats
      @param n  number of floats to read
      @return number of floats read
  */
  private static int read_float4_array(RandomAccessFile f, float[] x, int n)
    throws IOException
  {
     for (int i=0; i<n; i++) x[i] = f.readFloat();
     return n;
  }

  /** Compress a 3-D grid from floats to 1-byte unsigned integers.
      @param nr            number of rows of grid
      @param nc            number of columns of grid
      @param nl            number of levels of grid
      @param compressmode  1, 2 or 4 bytes per grid point
      @param data          array of [nr*nc*nl] floats
      @param compdata      array of [nr*nc*nl*compressmode] bytes for results
      @param ga            array to put ga decompression values
      @param gb            array to put gb decompression values
      @param minval        one-element float array to put min value
      @param maxval        one-element float array to put max value
  */
  private static void v5dCompressGrid(int nr, int nc, int nl,
    int compressmode, float[] data, byte[] compdata1, float ga[], float gb[],
    float[] minval, float[] maxval)
          throws BadFormException {
    int nrnc = nr * nc;
    int nrncnl = nr * nc * nl;

    // compute ga, gb values
    compute_ga_gb(nr, nc, nl, data, compressmode, ga, gb, minval, maxval);

    // compress the data
    if (compressmode == 1) {
      int i, lev, p;
      p = 0;
      for (lev=0; lev<nl; lev++) {
        float one_over_a, b;
        // subtract an epsilon so the expr below doesn't get mis-truncated
        b = gb[lev] - 0.0001f;
        if (ga[lev] == 0.0f) {
          one_over_a = 1.0f;
        }
        else {
          one_over_a = 1.0f / ga[lev];
        }
        for (i=0; i<nrnc; i++, p++) {
          // CTR: this section is messy and untested
          if (IS_MISSING(data[p])) compdata1[p] = -1;
          else compdata1[p] = (byte) (int) ((data[p] - b) * one_over_a);
        }
      }
    }

    else if (compressmode == 2) {
      int i, lev, p;
      p = 0;
      for (lev=0; lev<nl; lev++) {
        float one_over_a, b;
        b = gb[lev] - 0.0001f;
        if (ga[lev] == 0.0f) {
          one_over_a = 1.0f;
        }
        else {
          one_over_a = 1.0f / ga[lev];
        }
        for (i=0; i<nrnc; i++, p++) {
          // CTR: this section is messy and untested
          if (IS_MISSING(data[p])) {
            compdata1[2 * p] = -1;
            compdata1[2 * p + 1] = -1;
            // compdata2[p] = 65535;
          }
          else {
            short s = (short) (int) ((data[p] - b) * one_over_a);
            compdata1[2 * p] = (byte) (s / 256);
            compdata1[2 * p + 1] = (byte) (s % 256);
            // compdata2[p] = (short) (int) ((data[p] - b) * one_over_a);
          }
        }
      }
    }

    else {
      // compressmode==4
      // other machines: just copy 4-byte IEEE floats
      // CTR: I have no idea if this works properly in Java...
      System.arraycopy(compdata1, 0, data, 0, nrncnl*4);
    }
  }

  /** Write a block of memory.
      @param f         file descriptor
      @param data      address of first byte
      @param elements  number of elements to write
      @param elsize    size of each element to write (1, 2 or 4)
      @return number of elements written
  */
  private static int write_block(RandomAccessFile f, byte[] data,
    int elements, int elsize) throws IOException
  {
    if (elsize == 1) f.write(data, 0, elements);
    else if (elsize == 2) f.write(data, 0, elements*2);
    else if (elsize == 4) f.write(data, 0, elements*4);
    else {
      throw new IOException("Fatal error in write_block(): " +
        "bad elsize (" + elsize +")");
    }
    return elements;
  }


  // ************************ //
  // ****   CONSTRUCTOR  **** //
  // ************************ //

  /** Construct and initialize a V5DStruct to reasonable initial values. */
  V5DStruct() {
    // special cases
    Projection = -1;
    VerticalSystem = -1;

    for (int i=0;i<MAXVARS;i++) {
      MinVal[i] = MISSING;
      MaxVal[i] = -MISSING;
    }

    // set file version
    FileVersion = FILE_VERSION;

    CompressMode = 1;
    FileDesc = null;
  }

  /** Return the size (in bytes) of the 3-D grid specified by time and var.
      @param time  timestep
      @param vr    variable
      @return number of data points
  */
  int v5dSizeofGrid(int time, int vr) {
    return Nr * Nc * Nl[vr] * CompressMode;
  }

  /** Compute the location of a compressed grid within a file.
      @param time  timestep
             vr    variable
      @return file offset in bytes
  */
  int grid_position(int time, int vr)
      throws BadFormException {
    int pos, i;

    V5Dassert(time >= 0);
    V5Dassert(vr >= 0);
    V5Dassert(time < NumTimes);
    V5Dassert(vr < NumVars);

    pos = FirstGridPos + time * SumGridSizes;
    for (i=0; i<vr; i++) pos += GridSize[i];

    return pos;
  }

  /** Do some checking that the information in a V5DStruct is valid.
      @return true if V5DStruct is ok, false if V5DStruct is invalid
  */
  boolean v5dVerifyStruct() {
    int vr, i, maxnl;

    boolean valid = true;

    // Number of variables
    if (NumVars < 0) {
      System.err.println("Invalid number of variables: " + NumVars);
      valid = false;
    }
    else if (NumVars > MAXVARS) {
      System.err.println("Too many variables: " + NumVars +
        "  (Maximum is " + MAXVARS + ")");
      valid = false;
    }

    // Variable Names
    for (i=0; i<NumVars; i++) {
      if (VarName[i][0] == 0) {
        System.err.println("Missing variable name: VarName[" + i + "]=\"\"");
        valid = false;
      }
    }

    // Number of timesteps
    if (NumTimes < 0) {
      System.err.println("Invalid number of timesteps: " + NumTimes);
      valid = false;
    }
    else if (NumTimes>MAXTIMES) {
      System.err.println("Too many timesteps: " + NumTimes +
        "  (Maximum is " + MAXTIMES + ")");
      valid = false;
    }

    // Make sure timestamps are increasing
    for (i=1; i<NumTimes; i++) {
      int date0 = v5dYYDDDtoDays(DateStamp[i - 1]);
      int date1 = v5dYYDDDtoDays(DateStamp[i]);
      int time0 = v5dHHMMSStoSeconds(TimeStamp[i - 1]);
      int time1 = v5dHHMMSStoSeconds(TimeStamp[i]);
      // WLH 19 Sept 2001
      // hack dates and times if out of order, in order to accept
      // 'invalid' files that Vis5D will accept
      if (date1 < date0 || (date1 == date0 && time1 <= time0)) {
        int inc = 1;
        if (i > 1) {
          int j = (v5dHHMMSStoSeconds(TimeStamp[i - 1]) -
                   v5dHHMMSStoSeconds(TimeStamp[i - 2])) +
                  86400 * (v5dYYDDDtoDays(DateStamp[i - 1]) -
                           v5dYYDDDtoDays(DateStamp[i - 2]));
          if (j > 0) inc = j;
        }
        time1 = time0 + inc;
        date1 = date0;
        if (time1 >= 86400) {
          time1 = 0;
          date1++;
        }
        DateStamp[i] = v5dDaysToYYDDD(date1);
        TimeStamp[i] = v5dSecondsToHHMMSS(time1);
      }
    }

    // Rows
    if (Nr < 2) {
      System.err.println("Too few rows: " + Nr + " (2 is minimum)");
      valid = false;
    }
    /* Don't check on max rows in case user overrode defaults
    else if (Nr > MAXROWS) {
      System.err.println("Too many rows: " + Nr +
        " (" + MAXROWS + " is maximum)");
      valid = false;
    }
    */

    // Columns
    if (Nc < 2) {
      System.err.println("Too few columns: " + Nc + " (2 is minimum)");
      valid = false;
    }
    /* Don't check on max columns in case user overrode defaults
    else if (Nc > MAXCOLUMNS) {
      System.err.println("Too many columns: " + Nc +
        " (" + MAXCOLUMNS + " is maximum)");
      valid = false;
    }
    */

    // Levels
    maxnl = 0;
    for (vr=0; vr<NumVars; vr++) {
      if (LowLev[vr] < 0) {
        System.err.println("Low level cannot be negative for var " +
          VarName[vr] + ": " + LowLev[vr]);
        valid = false;
      }
      if (Nl[vr] < 1) {
        System.err.println("Too few levels for var " + VarName[vr] + ": " +
          Nl[vr] + " (1 is minimum)");
        valid = false;
      }
      if (Nl[vr] + LowLev[vr] > MAXLEVELS) {
        System.err.println("Too many levels for var " + VarName[vr] + ": " +
          (Nl[vr] + LowLev[vr]) + " (" + MAXLEVELS + " is maximum)");
        valid = false;
      }
      if (Nl[vr] + LowLev[vr] > maxnl) {
        maxnl = Nl[vr] + LowLev[vr];
      }
    }

    if (CompressMode != 1 && CompressMode != 2 && CompressMode != 4) {
      System.err.println("Bad CompressMode: " + CompressMode +
        " (must be 1, 2 or 4)");
      valid = false;
    }

    switch (VerticalSystem) {
      case 0:
      case 1:
        if (VertArgs[1] == 0.0) {
          System.err.println("Vertical level increment is zero, " +
            "must be non-zero");
          valid = false;
        }
        break;
      case 2:
        // Check that Height values increase upward
        for (i=1; i<maxnl; i++) {
          if (VertArgs[i] <= VertArgs[i - 1]) {
            System.err.println("Height[" + i + "]=" + VertArgs[i] +
              " <= Height[" + (i - 1) + "]=" + VertArgs[i-1] +
              ", level heights must increase");
            valid = false;
            break;
          }
        }
        break;
      case 3:
        // Check that Pressure values decrease upward
        for (i=1; i<maxnl; i++) {
          if (VertArgs[i] <= VertArgs[i - 1]) {
            System.err.println("Pressure[" + i + "]=" +
              height_to_pressure(VertArgs[i]) + " >= Pressure[" + (i-1) +
              "]=" + height_to_pressure(VertArgs[i-1]) +
              ", level pressures must decrease");
            valid = false;
            break;
          }
        }
        break;
      default:
        System.err.println("VerticalSystem = " + VerticalSystem +
          ", must be in 0..3");
        valid = false;
    }

    switch (Projection) {
      case 0:  // Generic
        if (ProjArgs[2] == 0.0) {
          System.err.println("Row Increment (ProjArgs[2]) can't be zero");
          valid = false;
        }
        if (ProjArgs[3] == 0.0) {
          System.err.println("Column increment (ProjArgs[3]) can't be zero");
          valid = false;
        }
        break;
      case 1:  // Cylindrical equidistant
        if (ProjArgs[2] < 0.0) {
          System.err.println("Row Increment (ProjArgs[2]) = " +
            ProjArgs[2] + "  (must be >=0.0)");
          valid = false;
        }
        if (ProjArgs[3] <= 0.0) {
          System.err.println("Column Increment (ProjArgs[3]) = " +
            ProjArgs[3] + "  (must be >=0.0)");
          valid = false;
        }
        break;
      case 2:  // Lambert Conformal
        if (ProjArgs[0] < -90.0 || ProjArgs[0] > 90.0) {
          System.err.println("Lat1 (ProjArgs[0]) out of range: " + ProjArgs[0]);
          valid = false;
        }
        if (ProjArgs[1] < -90.0 || ProjArgs[1] > 90.0) {
          System.err.println("Lat2 (ProjArgs[1] out of range: " + ProjArgs[1]);
          valid = false;
        }
        if (ProjArgs[5] <= 0.0) {
          System.err.println("ColInc (ProjArgs[5]) = " +
            ProjArgs[5] + "  (must be >=0.0)");
          valid = false;
        }
        break;
      case 3:  // Stereographic
        if (ProjArgs[0] < -90.0 || ProjArgs[0] > 90.0) {
          System.err.println("Central Latitude (ProjArgs[0]) out of range: " +
            ProjArgs[0] + "  (must be in +/-90)");
          valid = false;
        }
        if (ProjArgs[1] < -180.0 || ProjArgs[1] > 180.0) {
          System.err.println("Central Longitude (ProjArgs[1]) out of range: " +
            ProjArgs[1] + "  (must be in +/-180)");
          valid = false;
        }
        if (ProjArgs[4] < 0) {
          System.err.println("Column spacing (ProjArgs[4]) = " +
            ProjArgs[4] + "  (must be positive)");
          valid = false;
        }
        break;
      case 4:  // Rotated
        // WLH 4-21-95
        if (ProjArgs[2] <= 0.0) {
          System.err.println("Row Increment (ProjArgs[2]) = " +
            ProjArgs[2] + "  (must be >=0.0)");
          valid = false;
        }
        if (ProjArgs[3] <= 0.0) {
          System.err.println("Column Increment = (ProjArgs[3]) " +
            ProjArgs[3] + "  (must be >=0.0)");
          valid = false;
        }
        if (ProjArgs[4] < -90.0 || ProjArgs[4] > 90.0) {
          System.err.println("Central Latitude (ProjArgs[4]) out of range: " +
            ProjArgs[4] + "  (must be in +/-90)");
          valid = false;
        }
        if (ProjArgs[5] < -180.0 || ProjArgs[5] > 180.0) {
          System.err.println("Central Longitude (ProjArgs[5]) out of range: " +
            ProjArgs[5] + "  (must be in +/-180)");
          valid = false;
        }
        if (ProjArgs[6] < -180.0 || ProjArgs[6] > 180.0) {
          System.err.println("Central Longitude (ProjArgs[6]) out of range: " +
            ProjArgs[6] + "  (must be in +/-180)");
          valid = false;
        }
        break;
      default:
        System.err.println("Projection = " + Projection + ", must be in 0..4");
        valid = false;
    }

    return valid;
  }

  /** Get the McIDAS file number and grid number associated with the grid
      identified by time and var.
      @param time    timestep
      @param vr      variable
      @param mcfile  one-element int array for storing McIDAS grid file number
      @param mcgrid  one-element int array for storing McIDAS grid number
  */
  boolean v5dGetMcIDASgrid(int time, int vr, int[] mcfile, int[] mcgrid) {
    if (time < 0 || time >= NumTimes) {
      System.err.println("Bad time argument to v5dGetMcIDASgrid: " + time);
      return false;
    }
    if (vr < 0 || vr >= NumVars) {
      System.err.println("Bad var argument to v5dGetMcIDASgrid: " + vr);
      return false;
    }

    mcfile[0] = (int) McFile[time][vr];
    mcgrid[0] = (int) McGrid[time][vr];
    return true;
  }

  /** Set the McIDAS file number and grid number associated with the grid
      identified by time and var.
      @param time    timestep
      @param vr      variable
      @param mcfile  McIDAS grid file number
      @param mcgrid  McIDAS grid number
      @return true = ok, false = error (bad time or var)
  */
  boolean v5dSetMcIDASgrid(int time, int vr, int mcfile, int mcgrid) {
     if (time < 0 || time >= NumTimes) {
        System.err.println("Bad time argument to v5dSetMcIDASgrid: " + time);
        return false;
     }
     if (vr < 0 || vr >= NumVars) {
        System.err.println("Bad var argument to v5dSetMcIDASgrid: " + vr);
        return false;
     }

     McFile[time][vr] = (short) mcfile;
     McGrid[time][vr] = (short) mcgrid;
     return true;
  }


  // ******************************************************************** //
  // ****                    Input Functions                         **** //
  // ******************************************************************** //

  /** Read the header from a COMP* file and return results in the V5DStruct.
      @return true = ok, false = error.
  */
  boolean read_comp_header() throws IOException {
    int id;
    RandomAccessFile f = FileDesc;

    // reset file position to start of file
    f.seek(0);

    // read file ID
    id = f.readInt();

    if (id == 0x80808080 || id == 0x80808081) {
      // Older COMP5D format
      int gridtimes, gridparms;
      int i, j, it, iv, nl;
      int gridsize;
      float hgttop, hgtinc;

      if (id == 0x80808080) {
        // 20 vars, 300 times
        gridtimes = 300;
        gridparms = 20;
      }
      else {
        // 30 vars, 400 times
        gridtimes = 400;
        gridparms = 30;
      }

      FirstGridPos = 12 * 4 + 8 * gridtimes + 4 * gridparms;

      NumTimes = f.readInt();
      NumVars = f.readInt();
      Nr = f.readInt();
      Nc = f.readInt();
      nl = f.readInt();
      for (i=0; i<NumVars; i++) {
        Nl[i] = nl;
        LowLev[i] = 0;
      }
      ProjArgs[0] = f.readFloat();
      ProjArgs[1] = f.readFloat();
      hgttop = f.readFloat();
      ProjArgs[2] = f.readFloat();
      ProjArgs[3] = f.readFloat();
      hgtinc = f.readFloat();
      VerticalSystem = 1;
      VertArgs[0] = hgttop - hgtinc * (nl - 1);
      VertArgs[1] = hgtinc;

      // read dates and times
      for (i=0; i<gridtimes; i++) {
        j = f.readInt();
        DateStamp[i] = v5dDaysToYYDDD(j);
      }
      for (i=0; i<gridtimes; i++) {
        j = f.readInt();
        TimeStamp[i] = v5dSecondsToHHMMSS(j);
      }

      // read variable names
      for (i=0; i<gridparms; i++) {
        char[] name = new char[4];
        for (int q=0; q<4; q++) name[q] = (char) f.readByte();

        // remove trailing spaces, if any
        for (j=3; j>0; j--) {
          if (name[j] == ' ' || name[j] == 0) name[j] = 0;
          else break;
        }
        System.arraycopy(name, 0, VarName[i], 0, 4);
        VarName[i][4] = 0;
      }

      gridsize = ((Nr * Nc * nl + 3) / 4) * 4;
      for (i=0; i<NumVars; i++) {
        GridSize[i] = 8 + gridsize;
      }
      SumGridSizes = (8 + gridsize) * NumVars;

      // read the grids and their ga,gb values to find min and max values

      for (i=0; i<NumVars; i++) {
        MinVal[i] = 999999.9f;
        MaxVal[i] = -999999.9f;
      }

      for (it=0; it<NumTimes; it++) {
        for (iv=0; iv<NumVars; iv++) {
          float ga, gb;
          float min, max;

          ga = f.readFloat();
          gb = f.readFloat();

          // skip ahead by gridsize bytes
          f.skipBytes(gridsize);
          min = -(125.0f + gb) / ga;
          max = (125.0f-gb) / ga;
          if (min < MinVal[iv])  MinVal[iv] = min;
          if (max > MaxVal[iv])  MaxVal[iv] = max;
        }
      }

      // done
    }
    else if (id == 0x80808082 || id == 0x80808083) {
      // Newer COMP5D format
      int gridtimes, gridsize;
      int it, iv, nl, i, j;
      float delta = 0f;

      gridtimes = f.readInt();
      NumVars = f.readInt();
      NumTimes = f.readInt();
      Nr = f.readInt();
      Nc = f.readInt();
      nl = f.readInt();
      for (i=0; i<NumVars; i++) {
        Nl[i] = nl;
      }

      ProjArgs[2] = f.readFloat();
      ProjArgs[3] = f.readFloat();

      // Read height and determine if equal spacing
      VerticalSystem = 1;
      for (i=0; i<nl; i++) {
        VertArgs[i] = f.readFloat();
        if (i == 1) {
          delta = VertArgs[1] - VertArgs[0];
        }
        else if (i > 1) {
          if (delta != (VertArgs[i] - VertArgs[i - 1])) {
            VerticalSystem = 2;
          }
        }
      }
      if (VerticalSystem == 1) VertArgs[1] = delta;

      // read variable names
      for (iv=0; iv<NumVars; iv++) {
        char[] name = new char[8];

        for (int q=0; q<8; q++) name[q] = (char) f.readByte();

        // remove trailing spaces, if any
        for (j=7; j>0; j--) {
          if (name[j] == ' ' || name[j] == 0) name[j] = 0;
          else break;
        }
        System.arraycopy(name, 0, VarName[iv], 0, 8);
        VarName[iv][8] = 0;
      }

      for (iv=0; iv<NumVars; iv++) {
        MinVal[iv] = f.readFloat();
      }
      for (iv=0; iv<NumVars; iv++) {
        MaxVal[iv] = f.readFloat();
      }
      for (it=0; it<gridtimes; it++) {
        j = f.readInt();
        TimeStamp[it] = v5dSecondsToHHMMSS(j);
      }
      for (it=0; it<gridtimes; it++) {
        j = f.readInt();
        DateStamp[it] = v5dDaysToYYDDD(j);
      }
      for (it=0; it<gridtimes; it++) {
        float nlat;
        nlat = f.readFloat();
        if (it == 0) ProjArgs[0] = nlat;
      }
      for (it=0; it<gridtimes; it++) {
        float wlon;
        wlon = f.readFloat();
        if (it == 0) ProjArgs[1] = wlon;
      }

      // calculate grid storage sizes
      if (id == 0x80808082) {
        gridsize = nl * 2 * 4 + ((Nr * Nc * nl + 3) / 4) * 4;
      }
      else {
        // McIDAS grid and file numbers present
        gridsize = 8 + nl * 2 * 4 + ((Nr * Nc * nl + 3) / 4) * 4;
      }
      for (i=0; i<NumVars; i++) {
        GridSize[i] = gridsize;
      }
      SumGridSizes = gridsize * NumVars;

      // read McIDAS numbers???

      // size (in bytes) of all header info
      FirstGridPos = 9 * 4 + Nl[0] * 4 + NumVars * 16 + gridtimes * 16;
    }

    // one byte per grid point
    CompressMode = 1;

    // Cylindrical equidistant
    Projection = 1;

    FileVersion = "";

    return true;
  }

  /** Read a compressed grid from a COMP* file.
      @return true = ok, false = error.
  */
  boolean read_comp_grid(int time, int vr, float[] ga, float[] gb,
    byte[] compdata1) throws IOException, BadFormException
  {
    long pos;
    short bias;
    int i, n, nl;
    RandomAccessFile f = FileDesc;

    // move to position in file
    pos = grid_position(time, vr);
    f.seek(pos);

    if (FileFormat == 0x80808083) {
      // read McIDAS grid and file numbers
      int mcfile, mcgrid;
      mcfile = f.readInt();
      mcgrid = f.readInt();
      McFile[time][vr] = (short) mcfile;
      McGrid[time][vr] = (short) mcgrid;
    }

    nl = Nl[vr];

    if (FileFormat == 0x80808080 || FileFormat == 0x80808081) {
      // single ga, gb pair for whole grid
      float a, b;
      a = f.readFloat();
      b = f.readFloat();
      // convert a, b to new v5d ga, gb values
      for (i=0; i<nl; i++) {
        if (a == 0.0) {
          ga[i] = gb[i] = 0.0f;
        }
        else {
          gb[i] = (b + 128.0f) / -a;
          ga[i] = 1.0f / a;
        }
      }
      bias = 128;
    }
    else {
      // read ga, gb arrays
      read_float4_array(f, ga, Nl[vr]);
      read_float4_array(f, gb, Nl[vr]);

      // convert ga, gb values to v5d system
      for (i=0; i<nl; i++) {
        if (ga[i] == 0.0) {
          ga[i] = gb[i] = 0.0f;
        }
        else {
          // gb[i] = (gb[i]+125.0) / -ga[i];
          gb[i] = (gb[i] + 128.0f) / -ga[i];
          ga[i] = 1.0f / ga[i];
        }
      }
      bias = 128;  // 125 ???
    }

    // read compressed grid data
    n = Nr * Nc * Nl[vr];
    if (f.read(compdata1, 0, n) != n) return false;

    // convert data values to v5d system
    n = Nr * Nc * Nl[vr];
    for (i=0; i<n; i++) compdata1[i] += bias;

    return true;
  }

  /** Read a v5d file header.
      @return true = ok, false = error.
  */
  boolean read_v5d_header() throws IOException, BadFormException {
    boolean end_of_header = false;
    int id;
    int idlen, vr, numargs;
    RandomAccessFile f;

    f = FileDesc;
    int order = f.BIG_ENDIAN;

    // first try to read the header id, check the endianness
    while (true) {
      f.seek(0);
      f.order(order);
      id = f.readInt();
      idlen = f.readInt();
      if (id == TAG_ID && idlen == 0) {
        // this is a v5d file
        FileFormat = 0;
        break;
      }
      else if (id >= 0x80808080 && id <= 0x80808083) {
        // this is an old COMP* file
        FileFormat = id;
        return read_comp_header();
      }
      else if (order == f.BIG_ENDIAN) {
        order = f.LITTLE_ENDIAN;
        continue;  // try again
      } else {
        // unknown file type
        System.out.println("unknown file type");
        return false;
      }
    }

    // default
    CompressMode = 1;

    while (!end_of_header) {
      int tag, length;
      int i, time, nl, lev;

      tag = f.readInt();
      length = f.readInt();

      switch (tag) {
        case TAG_VERSION:
          V5Dassert(length == 10);
          byte[] b = new byte[10];
          f.read(b, 0, 10);
          int index = 10;
          for (int q=0; q<10; q++) {
            if (b[q] == 0) {
              index = q;
              break;
            }
          }
          FileVersion = new String(b, 0, index);
          // Check if reading a file made by a future version of Vis5D
          if (FileVersion.compareTo(FILE_VERSION) > 0) {
            System.err.println("Warning: Trying to read a version " +
              FileVersion + " file, you should upgrade Vis5D.");
          }
          break;
        case TAG_NUMTIMES:
          V5Dassert(length == 4);
          NumTimes = f.readInt();
          break;
        case TAG_NUMVARS:
          V5Dassert(length == 4);
          NumVars = f.readInt();
          break;
        case TAG_VARNAME:
          // 1 int + 10 char
          V5Dassert(length == 14);
          vr = f.readInt();
          for (int q=0; q<10; q++) VarName[vr][q] = (char) f.readByte();
          break;
        case TAG_NR:
          // Number of rows for all variables
          V5Dassert(length == 4);
          Nr = f.readInt();
          break;
        case TAG_NC:
          // Number of columns for all variables
          V5Dassert(length == 4);
          Nc = f.readInt();
          break;
        case TAG_NL:
          // Number of levels for all variables
          V5Dassert(length == 4);
          nl = f.readInt();
          for (i=0; i<NumVars; i++) {
            Nl[i] = nl;
          }
          break;
        case TAG_NL_VAR:
          // Number of levels for one variable
          V5Dassert(length == 8);
          vr = f.readInt();
          Nl[vr] = f.readInt();
          break;
        case TAG_LOWLEV_VAR:
          // Lowest level for one variable
          V5Dassert(length == 8);
          vr = f.readInt();
          LowLev[vr] = f.readInt();
          break;

        case TAG_TIME:
          // Time stamp for 1 timestep
          V5Dassert(length == 8);
          time = f.readInt();
          TimeStamp[time] = f.readInt();
          break;
        case TAG_DATE:
          // Date stamp for 1 timestep
          V5Dassert(length == 8);
          time = f.readInt();
          DateStamp[time] = f.readInt();
          break;

        case TAG_MINVAL:
          // Minimum value for a variable
          V5Dassert(length == 8);
          vr = f.readInt();
          MinVal[vr] = f.readFloat();
          break;
        case TAG_MAXVAL:
          // Maximum value for a variable
          V5Dassert(length == 8);
          vr = f.readInt();
          MaxVal[vr] = f.readFloat();
          break;
        case TAG_COMPRESS:
          // Compress mode
          V5Dassert(length == 4);
          CompressMode = f.readInt();
          break;
        case TAG_UNITS:
          // physical units
          V5Dassert(length == 24);
          vr = f.readInt();
          for (int q=0; q<20; q++) Units[vr][q] = (char) f.readByte();
          break;

        // Vertical coordinate system
        case TAG_VERTICAL_SYSTEM:
          V5Dassert(length == 4);
          VerticalSystem = f.readInt();
          if (VerticalSystem < 0 || VerticalSystem > 3) {
            System.err.println("Error: bad vertical coordinate system: " +
              VerticalSystem);
          }
          break;
        case TAG_VERT_ARGS:
          numargs = f.readInt();
          V5Dassert(numargs <= MAXVERTARGS);
          for (int q=0; q<numargs; q++) VertArgs[q] = f.readFloat();
          V5Dassert(length == numargs * 4 + 4);
          break;
        case TAG_HEIGHT:
          // height of a grid level
          V5Dassert(length == 8);
          lev = f.readInt();
          VertArgs[lev] = f.readFloat();
          break;
        case TAG_BOTTOMBOUND:
          V5Dassert(length == 4);
          VertArgs[0] = f.readFloat();
          break;
        case TAG_LEVINC:
          V5Dassert(length == 4);
          VertArgs[1] = f.readFloat();
          break;

        // Map projection information
        case TAG_PROJECTION:
          V5Dassert(length == 4);
          Projection = f.readInt();
          // WLH 4-21-95
          if (Projection < 0 || Projection > 4) {
            System.err.println("Error while reading header, bad projection (" +
              Projection + ")");
            return false;
          }
          break;
        case TAG_PROJ_ARGS:
          numargs = f.readInt();
          V5Dassert(numargs <= MAXPROJARGS);
          for (int q=0; q<numargs; q++) ProjArgs[q] = f.readFloat();
          V5Dassert(length == 4 * numargs + 4);
          break;
        case TAG_NORTHBOUND:
          V5Dassert(length == 4);
          if (Projection == 0 || Projection == 1 || Projection == 4) {
            ProjArgs[0] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_WESTBOUND:
          V5Dassert(length == 4);
          if (Projection == 0 || Projection == 1 || Projection == 4) {
            ProjArgs[1] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_ROWINC:
          V5Dassert(length == 4);
          if (Projection == 0 || Projection == 1 || Projection == 4) {
            ProjArgs[2] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_COLINC:
          V5Dassert(length == 4);
          if (Projection == 0 || Projection == 1 || Projection == 4) {
            ProjArgs[3] = f.readFloat();
          }
          else if (Projection == 2) {
            ProjArgs[5] = f.readFloat();
          }
          else if (Projection == 3) {
            ProjArgs[4] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_LAT1:
          V5Dassert(length == 4);
          if (Projection == 2) {
            ProjArgs[0] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_LAT2:
          V5Dassert(length == 4);
          if (Projection == 2) {
            ProjArgs[1] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_POLE_ROW:
          V5Dassert(length == 4);
          if (Projection == 2) {
            ProjArgs[2] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_POLE_COL:
          V5Dassert(length == 4);
          if (Projection == 2) {
            ProjArgs[3] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_CENTLON:
          V5Dassert(length == 4);
          if (Projection == 2) {
            ProjArgs[4] = f.readFloat();
          }
          else if (Projection == 3) {
            ProjArgs[1] = f.readFloat();
          }
          // WLH 4-21-95
          else if (Projection == 4) {
            ProjArgs[5] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_CENTLAT:
          V5Dassert(length == 4);
          if (Projection == 3) {
            ProjArgs[0] = f.readFloat();
          }
          // WLH 4-21-95
          else if (Projection == 4) {
            ProjArgs[4] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_CENTROW:
          V5Dassert(length == 4);
          if (Projection == 3) {
            ProjArgs[2] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_CENTCOL:
          V5Dassert(length == 4);
          if (Projection == 3) {
            ProjArgs[3] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;
        case TAG_ROTATION:
          V5Dassert(length == 4);
          // WLH 4-21-95
          if (Projection == 4) {
            ProjArgs[6] = f.readFloat();
          }
          else {
            f.skipBytes(4);
          }
          break;

        case TAG_END:
          // end of header
          end_of_header = true;
          f.skipBytes(length);
          break;

        default:
          // unknown tag, skip to next tag
          System.err.println("Unknown tag: " + tag + "  length=" + length);
          f.skipBytes(length);
          break;
      }

    }

    v5dVerifyStruct();

    // Now we're ready to read the grid data

    // Save current file pointer
    FirstGridPos = (int) f.getFilePointer();

    // compute grid sizes
    SumGridSizes = 0;
    for (vr=0;vr<NumVars;vr++) {
      GridSize[vr] = 8 * Nl[vr] + v5dSizeofGrid(0, vr);
      SumGridSizes += GridSize[vr];
    }

    return true;
  }

  /** Read a compressed grid from a v5d file.
      @param time      timestep
      @param vr        variable
      @param ga        array to store grid (de)compression values
      @param gb        array to store grid (de)compression values
      @param compdata  address of where to store compressed grid data
      @return true = ok, false = error
  */
  boolean v5dReadCompressedGrid(int time, int vr, float[] ga, float[] gb,
    byte[] compdata) throws IOException, BadFormException
  {
    int pos, n;
    boolean k = false;

    if (time < 0 || time >= NumTimes) {
      throw new IOException("Error in v5dReadCompressedGrid: " +
        "bad timestep argument (" + time + ")");
    }
    if (vr < 0 || vr >= NumVars) {
      throw new IOException("Error in v5dReadCompressedGrid: " +
        "bad var argument (" + vr + ")");
    }

    if (FileFormat != 0) {
      // old COMP* file
      return read_comp_grid(time, vr, ga, gb, compdata);
    }

    // move to position in file
    pos = grid_position(time, vr);
    FileDesc.seek(pos);

    // read ga, gb arrays
    read_float4_array(FileDesc, ga, Nl[vr]);
    read_float4_array(FileDesc, gb, Nl[vr]);

    // read compressed grid data
    n = Nr * Nc * Nl[vr];
    if (CompressMode == 1) {
      k = read_block(FileDesc, compdata, n, 1) == n;
    }
    else if (CompressMode == 2) {
      k = read_block(FileDesc, compdata, n, 2) == n;
    }
    else if (CompressMode == 4) {
      k = read_block(FileDesc, compdata, n, 4) == n;
    }
    if (!k) {
      // error
      System.err.println("Error in v5dReadCompressedGrid: " +
        "read failed, bad file?");
    }

    // n = Nr * Nc * Nl[vr] * CompressMode;
    // if (FileDesc.read(compdata, 0, n) != n)
    //   throw new IOException("Error in v5dReadCompressedGrid: read failed");

    return k;
  }


  /** Read a grid from a v5d file, decompress it and return it.
      @param time  timestep
      @param vr    variable
      @param data  array to put grid data
      @return true = ok, false = error.
  */
  boolean v5dReadGrid(int time, int vr, float[] data)
          throws IOException, BadFormException {
    float[] ga = new float[MAXLEVELS];
    float[] gb = new float[MAXLEVELS];
    byte[] compdata;
    int bytes;

    if (time < 0 || time >= NumTimes) {
      System.err.println("Error in v5dReadGrid: " +
        "bad timestep argument (" + time + ")");
      return false;
    }
    if (vr < 0 || vr >= NumVars) {
      System.err.println("Error in v5dReadGrid: " +
        "bad variable argument (" + vr + ")");
      return false;
    }

    // allocate compdata buffer
    if (CompressMode == 1) {
      /*-TDR, bug? factor should be 1 
      bytes = Nr * Nc * Nl[vr] * 2; // sizeof(unsigned char);
       */
      bytes = Nr * Nc * Nl[vr] * 1; // sizeof(unsigned char);
    }
    else if (CompressMode == 2) {
      bytes = Nr * Nc * Nl[vr] * 2; // sizeof(unsigned short);
    }
    else if (CompressMode == 4) {
      bytes = Nr * Nc * Nl[vr] * 4; // sizeof(float);
    }
    else {
      System.err.println("Error in v5dReadGrid: " +
        "bad compression mode (" + CompressMode + ")");
      return false;
    }
    compdata = new byte[bytes];

    // read the compressed data
    if (!v5dReadCompressedGrid(time, vr, ga, gb, compdata)) return false;

    // decompress the data
    v5dDecompressGrid(Nr, Nc, Nl[vr], CompressMode, compdata, ga, gb, data);

    return true;
  }


  // ******************************************************************** //
  // ****                   Output Functions                         **** //
  // ******************************************************************** //

  boolean write_tag(int tag, int length, boolean newfile) throws IOException {
    if (!newfile) {
      // have to check that there's room in header to write this tagged item
      if (CurPos+8+length > FirstGridPos) {
        System.err.println("Error: out of header space!");
        // Out of header space!
        return false;
      }
    }

    FileDesc.writeInt(tag);
    FileDesc.writeInt(length);
    CurPos += 8 + length;
    return true;
  }

  /** Write the information in the given V5DStruct as a v5d file header.
      Note that the current file position is restored when this function
      returns normally.
      @return true = ok, false = error.
  */
  boolean write_v5d_header() throws IOException {
    int vr, time, filler, maxnl;
    RandomAccessFile f;
    boolean newfile;

    if (FileFormat != 0) {
      System.err.println("Error: " +
        "v5d library can't write comp5d format files.");
      return false;
    }

    f = FileDesc;

    if (!v5dVerifyStruct()) return false;

    // Determine if we're writing to a new file
    newfile = (FirstGridPos == 0);

    // compute grid sizes
    SumGridSizes = 0;
    for (vr=0; vr<NumVars; vr++) {
      GridSize[vr] = 8 * Nl[vr] + v5dSizeofGrid(0, vr);
      SumGridSizes += GridSize[vr];
    }

    // set file pointer to start of file
    f.seek(0);
    CurPos = 0;

    // Write the tagged header info

    // ID
    if (!write_tag(TAG_ID, 0, newfile)) return false;

    // File Version
    if (!write_tag(TAG_VERSION, 10, newfile)) return false;
    f.write(FILE_VERSION.getBytes(), 0, 10);

    // Number of timesteps
    if (!write_tag(TAG_NUMTIMES, 4, newfile)) return false;
    f.writeInt(NumTimes);

    // Number of variables
    if (!write_tag(TAG_NUMVARS, 4, newfile)) return false;
    f.writeInt(NumVars);

    // Names of variables
    for (vr=0; vr<NumVars; vr++) {
      if (!write_tag(TAG_VARNAME, 14, newfile)) return false;
      f.writeInt(vr);
      for (int q=0; q<10; q++) f.writeByte((byte) VarName[vr][q]);
    }

    // Physical Units
    for (vr=0; vr<NumVars; vr++) {
      if (!write_tag(TAG_UNITS, 24, newfile)) return false;
      f.writeInt(vr);
      for (int q=0; q<20; q++) f.writeByte((byte) Units[vr][q]);
    }

    // Date and time of each timestep
    for (time=0; time<NumTimes; time++) {
      if (!write_tag(TAG_TIME, 8, newfile)) return false;
      f.writeInt(time);
      f.writeInt(TimeStamp[time]);
      if (!write_tag(TAG_DATE, 8, newfile)) return false;
      f.writeInt(time);
      f.writeInt(DateStamp[time]);
    }

    // Number of rows
    if (!write_tag(TAG_NR, 4, newfile)) return false;
    f.writeInt(Nr);

    // Number of columns
    if (!write_tag(TAG_NC, 4, newfile)) return false;
    f.writeInt(Nc);

    // Number of levels, compute maxnl
    maxnl = 0;
    for (vr=0; vr<NumVars; vr++) {
      if (!write_tag(TAG_NL_VAR, 8, newfile)) return false;
      f.writeInt(vr);
      f.writeInt(Nl[vr]);
      if (!write_tag(TAG_LOWLEV_VAR, 8, newfile)) return false;
      f.writeInt(vr);
      f.writeInt(LowLev[vr]);
      if (Nl[vr] + LowLev[vr] > maxnl) maxnl = Nl[vr]+LowLev[vr];
    }

    // Min/Max values
    for (vr=0; vr<NumVars; vr++) {
      if (!write_tag(TAG_MINVAL, 8, newfile)) return false;
      f.writeInt(vr);
      f.writeFloat(MinVal[vr]);
      if (!write_tag(TAG_MAXVAL, 8, newfile)) return false;
      f.writeInt(vr);
      f.writeFloat(MaxVal[vr]);
    }

    // Compress mode
    if (!write_tag(TAG_COMPRESS, 4, newfile)) return false;
    f.writeInt(CompressMode);

    // Vertical Coordinate System
    if (!write_tag(TAG_VERTICAL_SYSTEM, 4, newfile)) return false;
    f.writeInt(VerticalSystem);
    if (!write_tag(TAG_VERT_ARGS, 4+4*MAXVERTARGS, newfile)) return false;
    f.writeInt(MAXVERTARGS);
    for (int q=0; q<MAXVERTARGS; q++) f.writeFloat(VertArgs[q]);

    // Map Projection
    if (!write_tag(TAG_PROJECTION, 4, newfile)) return false;
    f.writeInt(Projection);
    if (!write_tag(TAG_PROJ_ARGS, 4+4*MAXPROJARGS, newfile)) return false;
    f.writeInt(MAXPROJARGS);
    for (int q=0; q<MAXPROJARGS; q++) f.writeFloat(ProjArgs[q]);

    // write END tag
    if (newfile) {
      // We're writing to a brand new file.
      // Reserve 10000 bytes for future header growth.
      if (!write_tag(TAG_END, 10000, newfile)) return false;
      f.skipBytes(10000);

      // Let file pointer indicate where first grid is stored
      FirstGridPos = (int) f.getFilePointer();
    }
    else {
      // we're rewriting a header
      filler = FirstGridPos - (int) f.getFilePointer();
      if (!write_tag(TAG_END, filler - 8, newfile)) return false;
    }

    return true;
  }

  /** Open a v5d file for writing.  If the named file already exists,
      it will be deleted.
      @param filename  name of v5d file to create
      @return true = ok, false = error.
  */
  boolean v5dCreateFile(String filename) throws IOException {
    RandomAccessFile fd = new RandomAccessFile(filename, "rw");

    if (fd == null) {
      System.err.println("Error in v5dCreateFile: open failed");
      FileDesc = null;
      Mode = 0;
      return false;
    }
    else {
      // ok
      FileDesc = fd;
      Mode = 'w';
      // write header and return status
      return write_v5d_header();
    }
  }

  /** Write a compressed grid to a v5d file.
      @param time      timestep
      @param vr        variable
      @param ga        the GA (de)compression value array
      @param gb        the GB (de)compression value array
      @param compdata  array of compressed data values
      @return true = ok, false = error
  */
  boolean v5dWriteCompressedGrid(int time, int vr, float[] ga, float[] gb,
    byte[] compdata) throws IOException, BadFormException
  {
    int pos, n;
    boolean k;

    // simple error checks
    if (Mode != 'w') {
      System.err.println("Error in v5dWriteCompressedGrid: " +
        "file opened for reading, not writing.");
      return false;
    }
    if (time < 0 || time >= NumTimes) {
      System.err.println("Error in v5dWriteCompressedGrid: " +
        "bad timestep argument (" + time + ")");
      return false;
    }
    if (vr < 0 || vr >= NumVars) {
      System.err.println("Error in v5dWriteCompressedGrid: " +
        "bad variable argument (" + vr + ")");
      return false;
    }

    // move to position in file
    pos = grid_position(time, vr);
    FileDesc.seek(pos);

    // write ga, gb arrays
    k = false;
    for (int q=0; q<Nl[vr]; q++) FileDesc.writeFloat(ga[q]);
    for (int q=0; q<Nl[vr]; q++) FileDesc.writeFloat(gb[q]);

    // write compressed grid data (k=true=OK, k=false=Error)
    n = Nr * Nc * Nl[vr];
    if (CompressMode == 1) {
      k = write_block(FileDesc, compdata, n, 1) == n;
    }
    else if (CompressMode == 2) {
      k = write_block(FileDesc, compdata, n, 2) == n;
    }
    else if (CompressMode == 4) {
      k = write_block(FileDesc, compdata, n, 4) == n;
    }

    if (!k) {
      // Error while writing
      System.err.println("Error in v5dWrite[Compressed]Grid: " +
        "write failed, disk full?");
    }
    return k;

    // n = Nr * Nc * Nl[vr] * CompressMode;
    // if (write_bytes(FileDesc, compdata, n) != n) {
    //   System.err.println("Error in v5dWrite[Compressed]Grid: " +
    //     "write failed, disk full?");
    //   return false;
    // }
    // else return true;
  }

  /** Compress a grid and write it to a v5d file.
      @param time  timestep
      @param vr    variable
      @param data  array of uncompressed grid data
      @return true = ok, false = error
  */
  boolean v5dWriteGrid(int time, int vr, float[] data)
          throws IOException, BadFormException {
    float[] ga = new float[MAXLEVELS];
    float[] gb = new float[MAXLEVELS];
    byte[] compdata;
    int n, bytes;
    float min, max;

    if (Mode != 'w') {
      System.err.println("Error in v5dWriteGrid: " +
       "file opened for reading, not writing.");
      return false;
    }
    if (time < 0 || time >= NumTimes) {
      System.err.println("Error in v5dWriteGrid: " +
       "bad timestep argument (" + time + ")");
      return false;
    }
    if (vr < 0 || vr >= NumVars) {
      System.err.println("Error in v5dWriteGrid: " +
       "bad variable argument (" + vr + ")");
      return false;
    }

    // allocate compdata buffer
    if (CompressMode == 1) {
      bytes = Nr * Nc * Nl[vr] * 2; // sizeof(unsigned char);
    }
    else if (CompressMode == 2) {
      bytes = Nr * Nc * Nl[vr] * 2; // sizeof(unsigned short);
    }
    else if (CompressMode == 4) {
      bytes = Nr * Nc * Nl[vr] * 4; // sizeof(float);
    }
    else {
      System.err.println("Error in v5dWriteGrid: " +
       "bad compression mode (" + CompressMode + ")");
      return false;
    }
    compdata = new byte[bytes];

    // compress the grid data
    float[] min1 = new float[1];
    float[] max1 = new float[1];
    v5dCompressGrid(Nr, Nc, Nl[vr], CompressMode, data, compdata,
     ga, gb, min1, max1);
    min = min1[0];
    max = max1[0];

    // update min and max value
    if (min < MinVal[vr]) MinVal[vr] = min;
    if (max > MaxVal[vr]) MaxVal[vr] = max;

    // write the compressed grid
    return v5dWriteCompressedGrid(time, vr, ga, gb, compdata);
  }

  /** Close a v5d file which was opened with open_v5d_file() or
      create_v5d_file().
      @return true = ok, false = error
  */
  boolean v5dCloseFile() throws IOException {
    boolean status = true;

    if (Mode == 'w') {
      // rewrite header because writing grids updates minval and maxval fields
      FileDesc.seek(0);
      status = write_v5d_header();
      // CTR: is this seek necessary?
      FileDesc.seek(FileDesc.length());
      FileDesc.close();
    }
    else if (Mode == 'r') {
      // just close the file
      FileDesc.close();
    }
    else {
      System.err.println("Error in v5dCloseFile: bad V5DStruct argument");
      return false;
    }
    FileDesc = null;
    Mode = 0;
    return status;
  }

}

