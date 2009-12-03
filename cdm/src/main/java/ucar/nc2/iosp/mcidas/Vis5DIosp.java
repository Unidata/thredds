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



package ucar.nc2.iosp.mcidas;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;

import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import visad.Set;
import visad.VisADException;

import visad.data.BadFormException;
import visad.data.vis5d.Vis5DCoordinateSystem;

import visad.data.vis5d.Vis5DVerticalSystem;

import java.io.IOException;

import java.util.Hashtable;


/**
 * Vis5D grid file reader.  Only support Vis5D grids, not the TOPO files.
 *
 * @author Unidata Development Team
 */
public class Vis5DIosp extends AbstractIOServiceProvider {

    /** Vis5D file reader */
    private V5DStruct v5dstruct;

    /** file header */
    private static final String V5D = "V5D";

    /** from vis5d-4.3/src/v5d.h */

    /** maximum number of varaiables */
    private final static int MAXVARS = 200;

    /** maximum number of times */
    private final static int MAXTIMES = 400;

    /** maximum number of rows */
    private final static int MAXROWS = 400;

    /** maximum number of columns */
    private final static int MAXCOLUMNS = 400;

    /** maximum number of levels */
    private final static int MAXLEVELS = 400;

    /** row variable name */
    private final static String ROW = "row";

    /** column variable name */
    private final static String COLUMN = "col";

    /** level variable name */
    private final static String LEVEL = "lev";

    /** time variable name */
    private final static String TIME = "time";

    /** latitude variable name */
    private final static String LAT = "lat";

    /** longitude variable name */
    private final static String LON = "lon";

    /** maximum number of projection arguments */
    private final int MAXPROJARGS = MAXROWS + MAXCOLUMNS + 1;

    /** maximum number of projection level arguments */
    private final int MAXVERTARGS = MAXLEVELS + 1;

    /** table of param name to units for well know params */
    private static Hashtable<String, String> unitTable = null;

    /** table of param name to var index */
    private static Hashtable<Variable, Integer> varTable;

    /** local copy of the raf */
    private RandomAccessFile raf;

    /** local copy of the ncfile */
    private NetcdfFile ncfile;

    /**
     * Is this a valid file?
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid McIDAS AREA file
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        // quick test
        raf.order(raf.BIG_ENDIAN);
        raf.seek(0);
        int    n = V5D.length();
        byte[] b = new byte[n];
        raf.read(b);
        String got = new String(b);
        if (got.equals(V5D)) {
            return true;
        } else {  // more rigorous test
            V5DStruct vv = null;
            try {
                vv = V5DStruct.v5dOpenFile(raf);
            } catch (BadFormException bfe) {
                vv = null;
            }
            return vv != null;
        }
    }

    /**
     * Get the file type id
     *
     * @return the file type id
     */
    public String getFileTypeId() {
        return "Vis5D";
    }

    /**
     * Get the file type description
     *
     * @return the file type description
     */
    public String getFileTypeDescription() {
        return "Vis5D grid file";
    }

    /**
     * Open the service provider for reading.
     * @param raf  file to read from
     * @param ncfile  netCDF file we are writing to (memory)
     * @param cancelTask  task for cancelling
     *
     * @throws IOException  problem reading file
     */
    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask)
            throws IOException {
        long start = System.currentTimeMillis();
        this.raf    = raf;
        this.ncfile = ncfile;
        if (unitTable == null) {
            initUnitTable();
        }
        if (v5dstruct == null) {
            makeFile(raf, ncfile, cancelTask);
        }
        long end = System.currentTimeMillis() - start;
    }

    /**
     * Make the netcdf file
     *
     * @param raf   the RandomAccessFile
     * @param ncfile  the netCDF file handle
     * @param cancelTask  the cancel task
     *
     * @throws IOException  problem reading the file
     */
    private void makeFile(RandomAccessFile raf, NetcdfFile ncfile,
                          CancelTask cancelTask)
            throws IOException {


        ncfile.empty();
        int[]     sizes    = new int[5];
        int[]     map_proj = new int[1];
        String[]  varnames = new String[MAXVARS];
        String[]  varunits = new String[MAXVARS];
        int[]     n_levels = new int[MAXVARS];
        int[]     vert_sys = new int[1];
        float[]   vertargs = new float[MAXVERTARGS];
        double[]  times    = new double[MAXTIMES];
        float[]   projargs = new float[MAXPROJARGS];

        V5DStruct vv       = null;
        try {
            v5dstruct = V5DStruct.v5d_open(raf, sizes, n_levels, varnames,
                                           varunits, map_proj, projargs,
                                           vert_sys, vertargs, times);
        } catch (BadFormException bfe) {
            throw new IOException("Vis5DIosp.makeFile: bad file "
                                  + bfe.getMessage());
        }

        if (sizes[0] < 1) {
            throw new IOException("Vis5DIosp.makeFile: bad file");
        }

        int nr     = sizes[0];
        int nc     = sizes[1];
        int nl     = sizes[2];
        int ntimes = sizes[3];
        int nvars  = sizes[4];
        // System.out.println("nr: "+nr);
        // System.out.println("nc: "+nc);
        // System.out.println("nl: "+nl);
        // System.out.println("ntimes: "+ntimes);
        // System.out.println("nvars: "+nvars);

        Dimension time = new Dimension(TIME, ntimes, true);
        Dimension row  = new Dimension(ROW, nr, true);
        Dimension col  = new Dimension(COLUMN, nc, true);
        ncfile.addDimension(null, time);
        ncfile.addDimension(null, row);
        ncfile.addDimension(null, col);

        // time
        Variable timeVar = new Variable(ncfile, null, null, TIME);
        timeVar.setDataType(DataType.DOUBLE);
        timeVar.setDimensions(TIME);
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1900-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", TIME));
        timeVar.addAttribute(new Attribute(_Coordinate.AxisType,
                                           AxisType.Time.toString()));
        Array varArray = new ArrayDouble.D1(ntimes);
        for (int i = 0; i < ntimes; i++) {
            ((ArrayDouble.D1) varArray).set(i, times[i]);
        }
        timeVar.setCachedData(varArray, false);
        ncfile.addVariable(null, timeVar);

        // rows and columns
        Variable rowVar = new Variable(ncfile, null, null, ROW);
        rowVar.setDataType(DataType.INT);
        rowVar.setDimensions(ROW);
        varArray = new ArrayInt.D1(nr);
        for (int i = 0; i < nr; i++) {
            ((ArrayInt.D1) varArray).set(i, i);
        }
        rowVar.setCachedData(varArray, false);
        ncfile.addVariable(null, rowVar);

        Variable colVar = new Variable(ncfile, null, null, COLUMN);
        colVar.setDataType(DataType.INT);
        colVar.setDimensions(COLUMN);
        varArray = new ArrayInt.D1(nc);
        for (int i = 0; i < nc; i++) {
            ((ArrayInt.D1) varArray).set(i, i);
        }
        colVar.setCachedData(varArray, false);
        ncfile.addVariable(null, colVar);

        // sanity check on levels
        Hashtable<Integer, Object> var_table = new Hashtable<Integer,
                                                   Object>();
        boolean have3D = false;
        for (int i = 0; i < nvars; i++) {
            int nlevs = n_levels[i];
            if ( !have3D && (nlevs > 1)) {
                have3D = true;
            }
            var_table.put(new Integer(nlevs), new Object());
        }
        int n_var_groups = var_table.size();
        if (n_var_groups > 2) {
            throw new IOException(
                "Vis5DIosp.makeFile: more than two variable groups by n_levels");
        } else if (n_var_groups == 0) {
            throw new IOException(
                "Vis5DIosp.makeFile: number of variable groups == 0");
        }
        Variable vert = null;
        if (have3D) {
            Dimension lev = new Dimension(LEVEL, nl, true);
            ncfile.addDimension(null, lev);
            vert = makeVerticalVariable(vert_sys[0], nl, vertargs);
            if (vert != null) {
                ncfile.addVariable(null, vert);
            }
        }
        varTable = new Hashtable<Variable, Integer>();
        String dim3D    = TIME + " " + LEVEL + " " + COLUMN + " " + ROW;
        String dim2D    = TIME + " " + COLUMN + " " + ROW;
        //String coords3D = TIME + " " + vert.getName() + " " + LAT + " " + LON;
        String coords3D = "unknown";
        if (vert != null) coords3D = TIME + " Height " + LAT + " " + LON;
        String coords2D = TIME + " " + LAT + " " + LON;

        for (int i = 0; i < nvars; i++) {
            Variable v = new Variable(ncfile, null, null, varnames[i]);
            if (n_levels[i] > 1) {
                v.setDimensions(dim3D);
                v.addAttribute(new Attribute("coordinates", coords3D));
            } else {
                v.setDimensions(dim2D);
                v.addAttribute(new Attribute("coordinates", coords2D));
            }
            v.setDataType(DataType.FLOAT);
            String units = varunits[i].trim();
            if (units.equals("")) {  // see if its in the unitTable
                String key = varnames[i].trim().toLowerCase();
                units = unitTable.get(key);
            }
            if (units != null) {
                v.addAttribute(new Attribute(CF.UNITS, units));
            }
            // TODO: do two vars with the same name have different values?
            // check agaist duplicat variable names
            if (varTable.get(v) == null) {
                varTable.put(v, new Integer(i));
                ncfile.addVariable(null, v);
            }
        }
        double[][] proj_args = Set.floatToDouble(new float[][] {
            projargs
        });
        addLatLonVariables(map_proj[0], proj_args[0], nr, nc);
        ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
        ncfile.finish();
    }

    /**
     * Read the data for the variable
     * @param v2  Variable to read
     * @param section   section infomation
     * @return Array of data
     *
     * @throws IOException problem reading from file
     * @throws InvalidRangeException  invalid Range
     */
    public Array readData(Variable v2, Section section)
            throws IOException, InvalidRangeException {
        long    startTime = System.currentTimeMillis();
        Integer varIdx    = varTable.get(v2);
        if (varIdx == null) {
            throw new IOException("unable to find variable index");
        }

        int     count = 0;
        int[]   shape = v2.getShape();
        boolean haveZ = shape.length == 4;
        int     nt    = shape[count++];
        int     nz    = haveZ
                        ? shape[count++]
                        : 1;
        int     ny    = shape[count++];
        int     nx    = shape[count];
        count = 0;

        Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
        Range         timeRange = section.getRange(count++);
        Range         zRange    = haveZ
                                  ? section.getRange(count++)
                                  : null;
        Range         yRange    = section.getRange(count++);
        Range         xRange    = section.getRange(count);
        int           grid_size = nx * ny * nz;

        IndexIterator ii        = dataArray.getIndexIterator();

        // loop over time
        for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last();
                timeIdx += timeRange.stride()) {

            float[] data   = new float[grid_size];
            float[] ranges = new float[2];
            try {
                v5dstruct.v5d_read(timeIdx, varIdx.intValue(), ranges, data);
            } catch (BadFormException bfe) {
                throw new IOException("Vis5DIosp.readData: "
                                      + bfe.getMessage());
            }

            if ((ranges[0] >= 0.99E30) && (ranges[1] <= -0.99E30)) {
                //range_sets[j] = new Linear1DSet(0.0, 1.0, 255);
            } else if (ranges[0] > ranges[1]) {
                throw new IOException("Vis5DIosp.readData: bad read "
                                      + v2.getName());
            }

            // invert the rows
            float[] tmp_data = new float[grid_size];

            if (zRange == null) {
                int cnt = 0;
                for (int mm = 0; mm < ny; mm++) {
                    int start = (mm + 1) * nx - 1;
                    for (int nn = 0; nn < nx; nn++) {
                        tmp_data[cnt++] = data[start--];
                    }
                }
            } else {
                int cnt = 0;
                for (int ll = 0; ll < nz; ll++) {
                    for (int mm = 0; mm < ny; mm++) {
                        int start = ((mm + 1) * nx - 1) + nx * ny * ll;
                        for (int nn = 0; nn < nx; nn++) {
                            tmp_data[cnt++] = data[start--];
                        }
                    }
                }
            }
            data = tmp_data;

            if (zRange != null) {
                for (int z = zRange.first(); z <= zRange.last();
                        z += zRange.stride()) {
                    for (int y = yRange.first(); y <= yRange.last();
                            y += yRange.stride()) {
                        for (int x = xRange.first(); x <= xRange.last();
                                x += xRange.stride()) {
                            int index = z * nx * ny + y * nx + x;
                            ii.setFloatNext(data[index]);
                        }
                    }
                }
            } else {
                for (int y = yRange.first(); y <= yRange.last();
                        y += yRange.stride()) {
                    for (int x = xRange.first(); x <= xRange.last();
                            x += xRange.stride()) {
                        int index = y * nx + x;
                        ii.setFloatNext(data[index]);
                    }
                }
            }
        }

        long end = System.currentTimeMillis() - startTime;
        return dataArray;
    }

    /**
     * Close this IOSP
     *
     * @throws IOException problem closing file
     */
    public void close() throws IOException {
        if (v5dstruct != null) {
            v5dstruct = null;
        }
    }

    /**
     * Test this.
     *
     * @param args [0] input file name [0] output file name
     *
     * @throws IOException  problem reading the file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider areaiosp = new Vis5DIosp();
        RandomAccessFile  rf       = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(areaiosp, rf, args[0], null);
        System.out.println(ncfile);
        if (args.length > 1) {
            ucar.nc2.FileWriter.writeToFile(ncfile, args[1]);
        }
    }

    /**
     *  TODO:  generalize this
     *  static class for testing
     */
    protected static class MakeNetcdfFile extends NetcdfFile {

        MakeNetcdfFile(IOServiceProvider spi, RandomAccessFile raf,
                       String location, CancelTask cancelTask)
                throws IOException {
            super(spi, raf, location, cancelTask);
        }
    }

    /**
     * Initialize the unit table.  This is used if there are no
     * units in the file.
     */
    private static void initUnitTable() {
        unitTable = new Hashtable<String, String>();
        // temperatures
        unitTable.put("t", "K");
        unitTable.put("td", "K");
        unitTable.put("thte", "K");
        // winds
        unitTable.put("u", "m/s");
        unitTable.put("v", "m/s");
        unitTable.put("w", "m/s");
        // pressure
        unitTable.put("p", "hPa");
        unitTable.put("mmsl", "hPa");
        // moisture
        unitTable.put("rh", "%");
        // misc
        unitTable.put("rhfz", "%");
        unitTable.put("zagl", "m");
    }

    /**
     * Create a vertical dimension variable based on the info.  Based on
     * visad.data.vis5d.Vis5DVerticalSystem.
     *
     * @param vert_sys  the vertical system id
     * @param n_levels  the number of levels
     * @param vert_args  the vertical system arguments
     *
     * @return  the vertical dimesion variable
     *
     * @throws IOException  problem reading the file or creating the data
     */
    private Variable makeVerticalVariable(int vert_sys, int n_levels,
                                          float[] vert_args)
            throws IOException {

        String        vert_unit = null;
        String        vert_type;
        ArrayFloat.D1 data     = new ArrayFloat.D1(n_levels);
        AxisType      axisType = null;

        switch (vert_sys) {

          case (0) :
              vert_unit = null;
              vert_type = "height";
              break;

          case (1) :
          case (2) :
              vert_unit = "km";
              vert_type = "altitude";
              axisType  = AxisType.Height;
              break;

          case (3) :
              vert_unit = "mbar";
              vert_type = "pressure";
              axisType  = AxisType.Pressure;
              break;

          default :
              throw new IOException("vert_sys unknown");
        }

        Variable vertVar = new Variable(ncfile, null, null, vert_type);
        vertVar.setDimensions(LEVEL);
        vertVar.setDataType(DataType.FLOAT);
        if (vert_unit != null) {
            vertVar.addAttribute(new Attribute(CF.UNITS, vert_unit));
        }
        if (axisType != null) {
            vertVar.addAttribute(new Attribute(_Coordinate.AxisType,
                    axisType.toString()));
        }

        switch (vert_sys) {

          case (0) :
          case (1) :
              for (int i = 0; i < n_levels; i++) {
                  data.set(i, vert_args[0] + vert_args[1] * i);
              }
              break;

          case (2) :  // Altitude in km - non-linear
              for (int i = 0; i < n_levels; i++) {
                  data.set(i, vert_args[i]);
              }
              break;

          case (3) :  // heights of pressure surfaces in km - non-linear
              try {
                  Vis5DVerticalSystem.Vis5DVerticalCoordinateSystem vert_cs =
                      new Vis5DVerticalSystem.Vis5DVerticalCoordinateSystem();
                  float[][] pressures = new float[1][n_levels];
                  System.arraycopy(vert_args, 0, pressures[0], 0, n_levels);
                  for (int i = 0; i < n_levels; i++) {
                      pressures[0][i] *= 1000;  // km->m
                  }
                  pressures = vert_cs.fromReference(pressures);  // convert to pressures
                  for (int i = 0; i < n_levels; i++) {
                      data.set(i, pressures[0][i]);
                  }
              } catch (VisADException ve) {
                  throw new IOException("unable to make vertical system");
              }
              break;

          default :
              throw new IOException("vert_sys unknown");
        }
        vertVar.setCachedData(data, false);
        return vertVar;
    }

    /**
     * Add lat/lon variables to the file
     *
     * @param map_proj  the map projection identifier
     * @param proj_args the projection args
     * @param nr  number of rows
     * @param nc  number of columns
     *
     * @throws IOException Problem making the projection
     */
    private void addLatLonVariables(int map_proj, double[] proj_args, int nr,
                                    int nc)
            throws IOException {
        //printProjArgs(map_proj, proj_args);

        Vis5DCoordinateSystem coord_sys;
        try {
            coord_sys = new Vis5DCoordinateSystem(map_proj, proj_args, nr,
                    nc);

            Variable lat = new Variable(ncfile, null, null, LAT);
            lat.setDimensions(COLUMN + " " + ROW);
            lat.setDataType(DataType.DOUBLE);
            lat.addAttribute(new Attribute("long_name", "latitude"));
            lat.addAttribute(new Attribute(CF.UNITS, "degrees_north"));
            lat.addAttribute(new Attribute(CF.STANDARD_NAME, "latitude"));
            lat.addAttribute(new Attribute(_Coordinate.AxisType,
                                           AxisType.Lat.toString()));
            ncfile.addVariable(null, lat);

            Variable lon = new Variable(ncfile, null, null, LON);
            lon.setDimensions(COLUMN + " " + ROW);
            lon.setDataType(DataType.DOUBLE);
            lon.addAttribute(new Attribute(CF.UNITS, "degrees_east"));
            lon.addAttribute(new Attribute("long_name", "longitude"));
            lon.addAttribute(new Attribute(CF.STANDARD_NAME, "longitude"));
            lon.addAttribute(new Attribute(_Coordinate.AxisType,
                                           AxisType.Lon.toString()));
            ncfile.addVariable(null, lon);

            int[]      shape    = new int[] { nc, nr };
            Array      latArray = Array.factory(DataType.DOUBLE, shape);
            Array      lonArray = Array.factory(DataType.DOUBLE, shape);
            double[][] rowcol   = new double[2][nr * nc];
            for (int x = 0; x < nc; x++) {
                for (int y = 0; y < nr; y++) {
                    int index = x * nr + y;
                    rowcol[0][index] = y;
                    rowcol[1][index] = x;
                }
            }
            double[][] latlon   = coord_sys.toReference(rowcol);
            Index      latIndex = latArray.getIndex();
            Index      lonIndex = lonArray.getIndex();
            /*
            for (int y = 0; y < nr; y++) {
                for (int x = 0; x < nc; x++) {
                    int index = y * nc + x;
            */
            for (int x = 0; x < nc; x++) {
                for (int y = 0; y < nr; y++) {
                    int index = x * nr + y;
                    /*
                    latArray.setDouble(latIndex.set(x, y), latlon[0][index]);
                    lonArray.setDouble(lonIndex.set(x, y), latlon[1][index]);
                    */
                    latArray.setDouble(index, latlon[0][index]);
                    lonArray.setDouble(index, latlon[1][index]);
                }
            }
            lat.setCachedData(latArray, false);
            lon.setCachedData(lonArray, false);
        } catch (VisADException ve) {
            throw new IOException("Vis5DIosp.addLatLon: " + ve.getMessage());
        }

    }

    /** _more_          */
    private static final int PROJ_GENERIC = 0;

    /** _more_          */
    private static final int PROJ_LINEAR = 1;

    /** _more_          */
    private static final int PROJ_CYLINDRICAL = 20;

    /** _more_          */
    private static final int PROJ_SPHERICAL = 21;

    /** _more_          */
    private static final int PROJ_LAMBERT = 2;

    /** _more_          */
    private static final int PROJ_STEREO = 3;

    /** _more_          */
    private static final int PROJ_ROTATED = 4;

    /**
     * _more_
     *
     * @param Projection _more_
     * @param projargs _more_
     */
    private void printProjArgs(int Projection, double[] projargs) {
        double NorthBound;
        double SouthBound;
        double WestBound;
        double EastBound;
        double RowInc;
        double ColInc;
        double Lat1;
        double Lat2;
        double PoleRow;
        double PoleCol;
        double CentralLat;
        double CentralLon;
        double CentralRow;
        double CentralCol;
        double Rotation;  /* radians */
        double Cone;
        double Hemisphere;
        double ConeFactor;
        double CosCentralLat;
        double SinCentralLat;
        double StereoScale;
        double InvScale;
        double CylinderScale;
        switch (Projection) {

          case PROJ_GENERIC :
          case PROJ_LINEAR :
          case PROJ_CYLINDRICAL :
          case PROJ_SPHERICAL :
              NorthBound = projargs[0];
              WestBound  = projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              System.out.println("Generic, Linear, Cylindrical, Spherical:");
              System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                                 + ", rowInc: " + RowInc + ", colInc: "
                                 + ColInc);
              break;

          case PROJ_ROTATED :
              NorthBound = projargs[0];
              WestBound  = projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              CentralLat = projargs[4];
              CentralLon = projargs[5];
              Rotation   = projargs[6];
              System.out.println("Rotated:");
              System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                                 + ", rowInc: " + RowInc + ", colInc: "
                                 + ColInc + ", clat: " + CentralLat
                                 + ", clon: " + CentralLon + ", rotation: "
                                 + Rotation);
              break;

          case PROJ_LAMBERT :
              Lat1       = projargs[0];
              Lat2       = projargs[1];
              PoleRow    = projargs[2];
              PoleCol    = projargs[3];
              CentralLon = projargs[4];
              ColInc     = projargs[5];
              System.out.println("Lambert: ");
              System.out.println("lat1: " + Lat1 + ", lat2: " + Lat2
                                 + ", poleRow: " + PoleRow + ", PoleCol: "
                                 + PoleCol + ", clon: " + CentralLon
                                 + ", colInc: " + ColInc);
              break;

          case PROJ_STEREO :
              CentralLat = projargs[0];
              CentralLon = projargs[1];
              CentralRow = projargs[2];
              CentralCol = projargs[3];
              ColInc     = projargs[4];
              System.out.println("Stereo: ");
              System.out.println("clat: " + CentralLat + ", clon: "
                                 + CentralLon + ", cRow: " + CentralRow
                                 + ", cCol: " + CentralCol + ", colInc: "
                                 + ColInc);
              break;

          default :
              System.out.println("Projection unknown");
        }
    }

}

