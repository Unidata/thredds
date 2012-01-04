package ucar.nc2.iosp.uamiv;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.io.File;

/**
 * Class for reading CAMx flavored uamiv files.
 * CAMx UAM-IV formatted files.
 * uses "IOAP Conventions", handled by M3IO CoordSysBuilder
 *
 * @author Barron Henderson barronh@gmail.com
 * @see "http://www.camx.com/"
 */
public class UAMIVServiceProvider extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UAMIVServiceProvider.class);

  static private final String AVERAGE = "A   V   E   R   A   G   E               ";
  static private final String EMISSIONS = "E   M   I   S   S   I   O   N   S       ";
  static private final String AIRQUALITY = "A   I   R   Q   U   A   L   I   T   Y   ";
  static private final String INSTANT = "I   N   S   T   A   N   T               ";

  static private final String HEIGHT = "HEIGHT";
  static private final String PBL = "PBL";

  static private final String TEMP = "TEMP";

  static private final String PRESS = "PRESS";

  static private final String WINDX = "WINDX";
  static private final String WINDY = "WINDY";
  static private final String VERTDIFF = "Kv";
  static private final String SPEED = "SPEED";

  static private final String CLDOD = "CLD OPDEP";

  static private final String CLDWATER = "CLD WATER";
  static private final String PRECIP = "PCP WATER";
  static private final String RAIN = "RAIN";

  private RandomAccessFile raf;
  private NetcdfFile ncfile;
  private String[] species_names;
  private long data_start;
  private int n2dvals;
  private int n3dvals;
  private int spc_2D_block;
  private int spc_3D_block;
  private int data_block;
  private int date_block;
  private int nspec;

  /**
   * Check if this is a valid file for this IOServiceProvider.
   *
   * @param raf RandomAccessFile
   * @return true if valid.
   */
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    try {

      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);
      raf.skipBytes(4);
      byte[] b = new byte[40];
      raf.read(b);
      String test = new String(b);
      // System.out.println("UAMIV");
      return test.equals(EMISSIONS) || test.equals(AVERAGE) ||
              test.equals(AIRQUALITY) || test.equals(INSTANT);
    } catch (IOException ioe) {
      return false;
    }
  }

  public String getFileTypeId() {
    return "UAMIV";
  }

  public String getFileTypeDescription() {
    return "CAMx UAM-IV formatted files";
  }

  /**
   * Open existing file, and populate ncfile with it.
   *
   * @param raf        the file to work on, it has already passed the isValidFile() test.
   * @param ncfile     add objects to this NetcdfFile
   * @param cancelTask used to monito user cancellation; may be null.
   * @throws IOException
   */
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    /*
     * <b>open</b> initializes the file meta data and creates all variables.
     * The meta-data and variable information is gathered from the UAM-IV
     * header.  The header format is detailed in the CAMx User's 
     * guide and copied here.
     * 
     * Header:
     * name,note,ione,nspec,ibdate,btime,iedate,etime
     * rdum,rdum,iutm,xorg,yorg,delx,dely,nx,ny,nz,idum,idum,rdum,rdum,rdum
     * ione,ione,nx,ny
     * (mspec(l),l=1,nspec)
     *
     * name - Text string (character*4(10) array)
     * note - Text string containing file description (character*4(60) array)
     * ione - Dummy variable = 1
     * nspec - Number of species on file
     * ibdate - Beginning date (YYJJJ)
     * btime - Beginning hour (HHMM)
     * iedate - Ending date (YYJJJ)
     * etime - Ending hour (HHMM)
     * rdum - Dummy real variable
     * iutm - UTM zone (ignored for other projections)
     * xorg - Grid x-origin at southwest corner of domain (m or degrees longitude)
     * yorg - Grid y-origin at southwest corner of domain (m or degrees latitude)
     * delx - Cell size in x-direction (m or degrees longitude)
     * dely - Cell size in y-direction (m or degrees longitude)
     * nx - Number of grid columns
     * ny - Number of grid rows
     * nz - Number of layers
     * idum - Dummy integer variable
     * mspec - Species names for nspec species (character*4(10,nspec) array)
     *
     *
     *   time step is HHMMSS
     *
     *  the projection is:
     *   LCC // >  :GDTYP = 2; // int
     *   First True Latitude (Alpha):  	30N // >  :P_ALP = 30.0; // double
     *   Second True Latitude (Beta): 	60N // >  :P_BET = 60.0; // double
     *   Central Longitude (Gamma): 	100W //>  :XCENT = -100.0; // double
     *   Projection Origin: 	(100W, 40N) //>  :YCENT = 40.0; // double
     *
     */
    // Internalize raf and ncfile
    this.raf = raf;
    this.ncfile = ncfile;

    // set raf to big endian and start at the beginning
    raf.order(RandomAccessFile.BIG_ENDIAN);
    raf.seek(0);

    // Read first line of UAM-IV header
    raf.skipBytes(4); // Skip record pad
    String name = raf.readString(40); // read 40 name
    String note = raf.readString(240);
    raf.skipBytes(4); // Skip dummy
    int nspec = raf.readInt(); // Read number of species
    this.nspec = nspec; // internalize nspec
    int bdate = raf.readInt(); // get file start date
    float btime = raf.readFloat(); // get file start time
    int edate = raf.readInt(); // get file end date
    float etime = raf.readFloat(); // get file end time
    int btimei = (int) btime; // convert btime to an integer

    // CAMx times are sometimes provided as HH or HHMM.
    // IOAPI times are always provided as HHMMSS.
    // CAMx times less than 100 are HH and should be
    // multipled by 100 to get HHMM.  CAMx times less
    // 10000 are HHMM and should be multipled by 100
    // to get HHMMSS.
    if (btimei < 100) btimei = btimei * 100;
    if (btimei < 10000) btimei = btimei * 100;

    /*
    * Dates are YYJJJ and are heuristically converted
    * to YYYYJJJ based on the following assumption:
    * YY < 70 are 2000
    * YY >= 70 are 1900
    *
    */
    if (bdate < 70000) {
      edate = edate + 2000000;
      bdate = bdate + 2000000;
    } else {
      edate = edate + 1900000;
      bdate = bdate + 1900000;
    }

    raf.skipBytes(4); //Skip record pad

    // Read second line of UAM-IV header
    raf.skipBytes(4); //Skip record pad
    raf.skipBytes(8); //Skip 2 dummies
    int iutm = raf.readInt(); // get utm
    float xorg = raf.readFloat(); // get x origin in meters
    float yorg = raf.readFloat(); // get y origin in meters
    float delx = raf.readFloat(); // get x cell size in meters
    float dely = raf.readFloat(); // get y cell size in meters
    int nx = raf.readInt(); // get number of columns
    int ny = raf.readInt(); // get number of rows
    int nz = raf.readInt(); // get number of layers
    raf.skipBytes(20); //Skip 5 dummies
    raf.skipBytes(4); //Skip record pad

    // Read third line of UAM-IV header
    raf.skipBytes(4); //Skip record pad
    raf.skipBytes(8); //Skip 2 dummies
    int nx2 = raf.readInt(); // duplicate number of columns
    int ny2 = raf.readInt(); // duplicate number of rows
    raf.skipBytes(8); //Skip 2 dummies    
    nz = Math.max(nz, 1); // number of layers; Emissions files occasionally report 0 layers
    /*
     * 1) Read each species name
     * 2) remove white space from the name
     * 3) store the names
     * 4) internalize them
     */
    int count = 0;
    String[] spc_names = new String[nspec];
    while (count < nspec) {
      String spc = raf.readString(40); // 1) read species name
      spc_names[count++] = spc.replace(" ", ""); // 2&3) store name without whitespace
    }
    this.species_names = spc_names; // 4) internalize names
    raf.skipBytes(4); // Skip record pad

    // Note this position; it is the start of the data block
    this.data_start = raf.getFilePointer();

    // Note the number of float equivalents (4 byte chunks) in data block
    int data_length_float_equivalents = ((int) raf.length() - (int) data_start) / 4;

    // Store 2D value size
    this.n2dvals = nx * ny;

    // Store 3D value size
    this.n3dvals = nx * ny * nz;

    // Store 2D binary data block size: include values (nx*ny), 
    // species name (10), a dummy (1) and 2 record pads
    this.spc_2D_block = nx * ny + 10 + 2 + 1;

    // Store 3D binary data block size
    this.spc_3D_block = this.spc_2D_block * nz;

    // Store whole data block size; includes date (6)
    this.data_block = this.spc_3D_block * nspec + 6;

    // Store the number of times
    int ntimes = data_length_float_equivalents / this.data_block;


    // Add dimensions based on header values
    ncfile.addDimension(null, new Dimension("TSTEP", ntimes, true));
    ncfile.addDimension(null, new Dimension("LAY", nz, true));
    ncfile.addDimension(null, new Dimension("ROW", ny, true));
    ncfile.addDimension(null, new Dimension("COL", nx, true));

    // Force sync of dimensions
    ncfile.finish();
    count = 0;

    /*
    * For each species, create a variable with long_name,
    * and var_desc, and units.  long_name and var_desc are
    * simply the species name.  units is heuristically
    * determined from the name
    */
    while (count < nspec) {
      String spc = spc_names[count++];
      Variable temp = ncfile.addVariable(null, spc, DataType.FLOAT, "TSTEP LAY ROW COL");
      if (spc.equals(WINDX) || spc.equals(WINDY) ||
              spc.equals(SPEED)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "m/s"));
      } else if (spc.equals(VERTDIFF)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "m**2/s"));
      } else if (spc.equals(TEMP)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "K"));
      } else if (spc.equals(PRESS)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "hPa"));
      } else if (spc.equals(HEIGHT) || spc.equals(PBL)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "m"));
      } else if (spc.equals(CLDWATER) || spc.equals(PRECIP) || spc.equals(RAIN)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "g/m**3"));
      } else if (spc.equals(CLDOD)) {
        temp.addAttribute(new Attribute(CDM.UNITS, "none"));
      } else if (spc.startsWith("SOA") ||
              spc.equals("PSO4") ||
              spc.equals("PNO3") ||
              spc.equals("PNH4") ||
              spc.equals("PH2O") ||
              spc.equals("SOPA") ||
              spc.equals("SOPB") ||
              spc.equals("NA") ||
              spc.equals("PCL") ||
              spc.equals("POA") ||
              spc.equals("PEC") ||
              spc.equals("FPRM") ||
              spc.equals("FCRS") ||
              spc.equals("CPRM") ||
              spc.equals("CCRS")) {
        temp.addAttribute(new Attribute(CDM.UNITS, "ug/m**3"));
      } else {
        temp.addAttribute(new Attribute(CDM.UNITS, "ppm"));
      }
      ;
      temp.addAttribute(new Attribute(CDM.LONG_NAME, spc));
      temp.addAttribute(new Attribute("var_desc", spc));
    }

    /*
    * Create 1...n array of "sigma" values
    */
    double[] sigma = new double[nz + 1];
    count = 0;
    while (count < nz + 1) {
      sigma[count++] = count;
    }
    int[] size = new int[1];
    size[0] = nz + 1;
    Array sigma_arr = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), size, sigma);

    /*
    * Add meta-data according to the IOAPI conventions
    * http://www.baronams.com/products/ioapi
    */
    ncfile.addAttribute(null, new Attribute("VGLVLS", sigma_arr));
    ncfile.addAttribute(null, new Attribute("SDATE", new Integer(bdate)));
    ncfile.addAttribute(null, new Attribute("STIME", new Integer(btimei)));
    ncfile.addAttribute(null, new Attribute("TSTEP", new Integer(10000)));
    ncfile.addAttribute(null, new Attribute("NSTEPS", new Integer(ntimes)));
    ncfile.addAttribute(null, new Attribute("NLAYS", new Integer(nz)));
    ncfile.addAttribute(null, new Attribute("NROWS", new Integer(ny)));
    ncfile.addAttribute(null, new Attribute("NCOLS", new Integer(nx)));
    ncfile.addAttribute(null, new Attribute("XORIG", new Double(xorg)));
    ncfile.addAttribute(null, new Attribute("YORIG", new Double(yorg)));
    ncfile.addAttribute(null, new Attribute("XCELL", new Double(delx)));
    ncfile.addAttribute(null, new Attribute("YCELL", new Double(dely)));

    /*
     * IOAPI Projection parameters are provided by a colocated camxproj.txt file;
     *
     * to do:
     * 1) needs earth radius
     * 2) needs better error checking
    */

    /* Defaults are based on Continental US */
    Integer gdtyp = 2;
    Double p_alp = 20.;
    Double p_bet = 60.;
    Double p_gam = 0.;
    Double xcent = -95.;
    Double ycent = 25.;

    String[] key_value = null;
    String thisLine;
    String projpath = raf.getLocation();
    Boolean lgdtyp = false;
    Boolean lp_alp = false;
    Boolean lp_bet = false;
    Boolean lp_gam = false;
    Boolean lxcent = false;
    Boolean lycent = false;
    int lastIndex = projpath.lastIndexOf(File.separator);
    if (lastIndex <= 0)
      lastIndex = projpath.lastIndexOf('/');
    if (lastIndex > 0)
      projpath = projpath.substring(0, lastIndex);
    projpath = projpath + File.separator + "camxproj.txt";
    File paramFile = new File(projpath);
    if (paramFile.exists()) {
      BufferedReader br = new BufferedReader(new FileReader( paramFile));
      while ((thisLine = br.readLine()) != null) {
        if (thisLine.substring(0, 1) != "#") {
          key_value = thisLine.split("=");
          if (key_value[0].equals("GDTYP")) {
            gdtyp = Integer.parseInt(key_value[1]);
            lgdtyp = true;
          } else if (key_value[0].equals("P_ALP")) {
            p_alp = Double.parseDouble(key_value[1]);
            lp_alp = true;
          } else if (key_value[0].equals("P_BET")) {
            p_bet = Double.parseDouble(key_value[1]);
            lp_bet = true;
          } else if (key_value[0].equals("P_GAM")) {
            p_gam = Double.parseDouble(key_value[1]);
            lp_gam = true;
          } else if (key_value[0].equals("YCENT")) {
            ycent = Double.parseDouble(key_value[1]);
            lycent = true;
          } else if (key_value[0].equals("XCENT")) {
            xcent = Double.parseDouble(key_value[1]);
            lxcent = true;
          }
        }
      }
      if (!lgdtyp) log.warn("GDTYP not found; using " + gdtyp.toString());
      if (!lp_alp) log.warn("P_ALP not found; using " + p_alp.toString());
      if (!lp_bet) log.warn("P_BET not found; using " + p_bet.toString());
      if (!lp_gam) log.warn("P_GAM not found; using " + p_gam.toString());
      if (!lxcent) log.warn("XCENT not found; using " + xcent.toString());
      if (!lycent) log.warn("YCENT not found; using " + ycent.toString());

    } else {
      if (log.isDebugEnabled()) log.debug("UAMIVServiceProvider: adding projection file");
      BufferedWriter bw = new BufferedWriter(new java.io.FileWriter( paramFile));
      bw.write("# Projection parameters are based on IOAPI.  For details, see www.baronsams.com/products/ioapi");
      bw.newLine();
      bw.write("GDTYP=");
      bw.write(gdtyp.toString());
      bw.newLine();
      bw.write("P_ALP=");
      bw.write(p_alp.toString());
      bw.newLine();
      bw.write("P_BET=");
      bw.write(p_bet.toString());
      bw.newLine();
      bw.write("P_GAM=");
      bw.write(p_gam.toString());
      bw.newLine();
      bw.write("XCENT=");
      bw.write(xcent.toString());
      bw.newLine();
      bw.write("YCENT=");
      bw.write(ycent.toString());
      bw.newLine();
      bw.flush();
      bw.close();
    }

    ncfile.addAttribute(null, new Attribute("GDTYP", gdtyp));
    ncfile.addAttribute(null, new Attribute("P_ALP", p_alp));
    ncfile.addAttribute(null, new Attribute("P_BET", p_bet));
    ncfile.addAttribute(null, new Attribute("P_GAM", p_gam));
    ncfile.addAttribute(null, new Attribute("XCENT", xcent));
    ncfile.addAttribute(null, new Attribute("YCENT", ycent));
  }

  /**
   * Read data from a top level Variable and return a memory resident Array.
   * This Array has the same element type as the Variable, and the requested shape.
   *
   * @param v2          a top-level Variable
   * @param wantSection List of type Range specifying the section of data to read.
   *                    There must be a Range for each Dimension in the variable, in order.
   *                    Note: no nulls.
   * @return the requested data in a memory-resident Array
   * @throws java.io.IOException
   * @throws ucar.ma2.InvalidRangeException
   * @see ucar.ma2.Range
   */
  public ucar.ma2.Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
    /*
     * <b>readData</b> seeks and reads the data for each variable.  The variable
     * data format is detailed in the CAMx User's guide and summarized here.
     * 
     * For each time:
     *   ibdate,btime,iedate,etime
     *   Loop from 1 to nspec species:
     *     ione,mspec(l),((val(i,j,l),i=1,nx),j=1,ny)
     *
     *
     * ione - Dummy variable = 1
     * nspec - Number of species on file
     * ibdate - Beginning date (YYJJJ)
     * btime - Beginning hour (HHMM)
     * iedate - Ending date (YYJJJ)
     * etime - Ending hour (HHMM)
     * mspec - Species names for nspec species (character*4(10,nspec) array)
     * val - Species l, layer k initial concentrations (ppm for gases, ug/m3 for aerosols)
     *       for nx grid columns and ny grid rows
     *
     */
    // CAMx UAM-IV Files are all big endian
    raf.order(RandomAccessFile.BIG_ENDIAN);

    // Prepare an array for binary data
    int size = (int) v2.getSize();
    float[] arr = new float[size];

    // Move to data block of file
    raf.seek(this.data_start);

    /*
     * First record is stime,sdate,etime,edate
     * We are skipping the data, but checking
     * the consistency of the Fortran "unformatted"
     * data record
    */
    int pad1 = raf.readInt();
    raf.skipBytes(16);
    int pad2 = raf.readInt();
    if (pad1 != pad2) {
      throw new IOException("Asymmetric fortran buffer values: 1");
    }

    // Find species name/id associated with this variable
    int spcid = -1;
    String spc = "";
    while (spc != v2.getShortName()) {
      spc = this.species_names[++spcid];
    }

    /*
    * Skip data associated with species that are prior
    * in the data block
    */
    raf.skipBytes(this.spc_3D_block * spcid * 4);

    // Initialize count for indexing arr
    int count = 0;


    while (count < size) {

      /*
      * Read species name and store the initial record pad.
      * Note: it might be good to compare
      *       spc string to variable.getShortName
      */
      if (count == 0) {
        pad1 = raf.readInt();
        int ione = raf.readInt();
        spc = raf.readString(40);
      }

      /*
      * If we have read a 2D slice, read the final record pad
      * and compare to initial record pad.  If everything is okay, proceed.
      * (1) skip to next 2D slice
      * (2) store initial pad
      * (3) read spc name
      * Note: it might be good to compare
      *       spc string to variable.getShortName
      */
      if ((count != 0) && ((count % this.n2dvals) == 0)) {
        pad2 = raf.readInt();
        if (pad1 != pad2) {
          //System.out.println(pad1);
          //System.out.println(pad2);
          throw new IOException("Asymmetric fortran buffer values: 2");
        }
        if ((count % this.n3dvals) == 0) {
          raf.skipBytes((this.data_block - this.spc_3D_block) * 4);
        }
        pad1 = raf.readInt();
        int ione = raf.readInt();
        spc = raf.readString(40);
      }

      /*
      * Attempt to read a Float from the file
      */
      try {
        arr[count++] = raf.readFloat();
      } catch (java.lang.ArrayIndexOutOfBoundsException io) {
        throw new IOException(io.getMessage());
      }
    }

    // Convert java float[] to ma2.Array
    Array data = Array.factory(DataType.FLOAT.getPrimitiveClassType(), v2.getShape(), arr);

    // Subset the data based on the wantSection and return a 4D variable
    return data.sectionNoReduce(wantSection.getRanges());
  }

  ;

  /**
   * Close the file.
   * It is the IOServiceProvider's job to close the file (even though it didnt open it),
   * and to free any other resources it has used.
   *
   * @throws IOException
   */
  public void close() throws IOException {
    raf.close();
  }
}