/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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



package ucar.nc2.iosp.nowrad;

//~--- non-JDK imports --------------------------------------------------------

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 10, 2010
 * Time: 11:21:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class NOWRadheader {
    final static int                NEXET        = 2;        // Echo Tops Composite
    final static int                NEXLH        = 5;        // Layer Reflectivity - High
    final static int                NEXLL        = 3;        // Layer Reflectivity - Low
    final static int                NEXLM        = 4;        // Layer Reflectivity - Mid
    final static int                NEXVI        = 6;        // Vert. Integrated Liquid Water
    final static int                NOWRADHF     = 0;        // 2km Base Reflectivity
    final static int                USRADHF      = 1;        // 8km Base Reflectivity
    static public String            mons[]       = {
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };
    final private static boolean    useStationDB = false;    // use station db for loactions
    static private org.slf4j.Logger log          = org.slf4j.LoggerFactory.getLogger(NOWRadheader.class);
    DateFormatter                   formatter    = new DateFormatter();
    private boolean                 isR          = false;

    // private PrintStream out = System.out;
    // private Vinfo myInfo;
    private String              cmemo, ctilt, ctitle, cunit, cname;
    private ucar.nc2.NetcdfFile ncfile;

    // message header block
    // production dessciption block
    private boolean                          noHeader;
    private int                              numX;
    private int                              numY;
    private ucar.unidata.io.RandomAccessFile raf;

    /**
     * check if this file is a nids / tdwr file
     * @param raf    input file
     * @return  true  if valid
     */
    public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
        try {
            long t = raf.length();

            if (t == 0) {
                throw new IOException("zero length file ");
            }
        } catch (IOException e) {
            return (false);
        }

        try {
            int p = this.readTop(raf);

            if (p == 0) {
                return false;
            }
        } catch (IOException e) {
            return (false);
        }

        return true;
    }

    /**
     * read the header of input file and parsing the NOWRAD part
     * @param raf    input file
     * @return        1 if checking passing
     * @throws IOException
     */
    int readTop(ucar.unidata.io.RandomAccessFile raf) throws IOException {
        int pos = 0;

        // long     actualSize = 0;
        raf.seek(pos);

        int readLen = 35;
        int rc      = 0;

        // Read in the contents of the NEXRAD Level III product head
        byte[] b = new byte[readLen];

        rc = raf.read(b);

        if (rc != readLen) {
            return 0;
        }

        // check
        if ((convertunsignedByte2Short(b[0]) != 0x00) || (convertunsignedByte2Short(b[1]) != 0xF0)
                || (convertunsignedByte2Short(b[2]) != 0x09)) {
            return 0;
        }

        int    hsize = b[3];
        String pidd  = new String(b, 15, 5);

        if (pidd.contains("NOWRA") || pidd.contains("USRAD") || pidd.contains("NEX")) {
            return 1;
        } else {
            return 0;
        }
    }

    public byte[] getData(int offset) throws Exception {
        int    readLen = (int) raf.length();
        byte[] b       = new byte[readLen];
        int    pos     = 0;

        raf.seek(pos);

        int rc = raf.read(b);

        return b;
    }

    // ////////////////////////////////////////////////////////////////////////////////

    public void setProperty(String name, String value) {}

    /**
     * read and parse the header of the nids/tdwr file
     * @param raf       input file
     * @param ncfile    output file
     * @throws IOException
     */
    void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile) throws Exception {
        this.raf = raf;

        int rc;    /* function return status */
        int hoffset = 0;
        int readLen = 250;

        this.ncfile = ncfile;

        int pos = 0;

        raf.seek(pos);

        byte[] b = new byte[readLen];

        rc = raf.read(b);

        if (rc != readLen) {
            log.warn(" error reading nids product header " + raf.getLocation());
        }

        int    hsize   = b[3];
        String product = new String(b, 15, 8);
        int    imgres  = b[4 + hsize];

        // image lines
        String head = new String(b);
        byte[] bt   = new byte[] { (byte) 0xF0, (byte) 0x0A };
        String t0   = new String(bt);
        int    t1   = head.indexOf(t0);

        // if(convertunsignedByte2Short(b[6+hsize]) != 0xF0 ||
        // convertunsignedByte2Short(b[7+hsize]) != 0x0A )
        // return;
        String lstr = trim(new String(b, t1 + 2, 4));

        numY = Integer.parseInt(lstr);

        String estr = trim(new String(b, t1 + 6, 5));

        numX = Integer.parseInt(estr);
        bt   = new byte[] { (byte) 0xF0, (byte) 0x03 };
        t0   = new String(bt);
        t1   = head.indexOf(t0);

        // if((lstr.length()+estr.length() < 8))
        // hsize = hsize -2;

        // if(convertunsignedByte2Short(b[18+hsize]) != 0xF0 ||
        // convertunsignedByte2Short(b[19+hsize]) != 0x03 )
        // return;
        String ss  = new String(b, t1 + 2, 20);
        int    off = 0;

        if (product.contains("USRADHF")) {
            off = 3;
        }

        // Image time, HHMMSS.  The time will be in the form HH:MM, so look :
        String ts = new String(b, t1 + 22 + off, 2);
        int    hr = Integer.parseInt(ts);

        ts = new String(b, t1 + 25 + off, 2);

        int min = Integer.parseInt(ts);

        ts = new String(b, t1 + 28 + off, 2);

        int dd = Integer.parseInt(ts);

        ts = new String(b, t1 + 31 + off, 3);

        String mon   = ts;
        int    month = getMonth(mon);

        ts = new String(b, t1 + 35 + off, 2);

        int              year = Integer.parseInt(ts);
        SimpleDateFormat sdf  = new SimpleDateFormat();

        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd HH:mm");

        Date   date = sdf.parse(year + "/" + month + "/" + dd + " " + hr + ":" + min);
        String ot   = new String(b, t1 + 40, 45);

        bt = new byte[] { (byte) 0xF0, (byte) 0x0b };
        t0 = new String(bt);
        t1 = head.indexOf(t0);

        // if( convertunsignedByte2Short(b[101 + hsize]) != 0xF0 ||
        // convertunsignedByte2Short(b[102 + hsize]) != 0x0b )
        // return;
        ProjectionImpl projection = null;
        if (product.contains("NOWRAD")) {
            ot = new String(b, t1 + 2, 68);

            List<String> toks = StringUtil.split(ot, " ", true, true);
            String       pj   = toks.get(0);
            double       nav1 = Math.toDegrees(Double.parseDouble(toks.get(1)));    // lon
            double       nav2 = Math.toDegrees(Double.parseDouble(toks.get(2)));    // lat
            double       nav3 = Math.toDegrees(Double.parseDouble(toks.get(3)));
            double       nav4 = Math.toDegrees(Double.parseDouble(toks.get(4)));    // lat sp
            double       nav5 = Math.toDegrees(Double.parseDouble(toks.get(5)));    // lon sp

            // lower left and upper right corner
            float rlat1;
            float rlon1;
            float rlat2;
            float rlon2;

            rlat1   = (float) (nav2 - (numY - 1) * nav4);
            rlon1   = (float) (nav1 + nav3);
            rlat2   = (float) nav2;
            rlon2   = (float) (nav1 - nav3);
            hoffset = t1 + 71;    // 172 + hsize;

            // start of the image sequence
            if ((convertunsignedByte2Short(b[172 + hsize]) != 0xF0)
                    || (convertunsignedByte2Short(b[173 + hsize]) != 0x0c)) {
                return;
            }

            // hoffset = 174 + hsize;
            // Set product-dependent information
            setProductInfo(product, date);

            // data struct
            nowrad(hoffset, rlat1, rlon1, rlat2, rlon2, (float) nav4, (float) nav5, date);
        } else if (product.contains("USRADHF")) {
            ot = new String(b, t1 + 2, 107);

            List<String> toks = StringUtil.split(ot, " ", true, true);
            String       pj   = toks.get(0);
            double       nav1 = Math.toDegrees(Double.parseDouble(toks.get(1)));    // standard lat 1
            double       nav2 = Math.toDegrees(Double.parseDouble(toks.get(2)));    // standard lat 2
            double       nav3 = Math.toDegrees(Double.parseDouble(toks.get(3)));    // lat. center of proj
            double       nav4 = Math.toDegrees(Double.parseDouble(toks.get(4)));    // lon. center of proj
            double       nav5 = Math.toDegrees(Double.parseDouble(toks.get(5)));    // upper left lat
            double       nav6 = Math.toDegrees(Double.parseDouble(toks.get(6)));    // upper left lon
            double       nav7 = Math.toDegrees(Double.parseDouble(toks.get(7)));    // lat sp
            double       nav8 = Math.toDegrees(Double.parseDouble(toks.get(8)));    // lon sp

            // lower left and upper right corner
            // int offh = 39;
            hoffset = t1 + 110;    // 172 + hsize+ offh;

            // start of the image sequence
            if ((convertunsignedByte2Short(b[t1 + 110]) != 0xF0) || (convertunsignedByte2Short(b[t1 + 111]) != 0x0c)) {
                return;
            }

            // Set product-dependent information
            setProductInfo(product, date);

            // data struct
            nowradL(hoffset, (float) nav1, (float) nav2, (float) nav3, (float) nav4, (float) nav5, (float) nav6, date);
        }


        ncfile.finish();
    }

    String trim(String str) {
        int          len  = str.length();
        StringBuffer ostr = new StringBuffer();

        for (int i = 0; i < len; i++) {
            char sc = str.charAt(i);

            if (Character.isDigit(sc)) {
                ostr.append(sc);
            }
        }

        return ostr.toString();
    }

    int getMonth(String m) {
        int i = 0;

        while (i < 12) {
            if (m.equalsIgnoreCase(mons[i])) {
                return i + 1;
            } else {
                i++;
            }
        }

        return 0;
    }

    ProjectionImpl  nowradL(int hoff, float lat1, float lat2, float clat, float clon, float lat, float lon, Date dd) {
        ArrayList dims = new ArrayList();
        Dimension dimT = new Dimension("time", 1, true, false, false);

        ncfile.addDimension(null, dimT);

        String   timeCoordName = "time";
        Variable taxis         = new Variable(ncfile, null, null, timeCoordName);

        taxis.setDataType(DataType.DOUBLE);
        taxis.setDimensions("time");
        taxis.addAttribute(new Attribute("long_name", "time since base date"));
        taxis.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

        double[] tdata = new double[1];

        tdata[0] = dd.getTime();

        Array dataT = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { 1 }, tdata);

        taxis.setCachedData(dataT, false);

        DateFormatter formatter = new DateFormatter();

        taxis.addAttribute(new Attribute("units", "msecs since " + formatter.toDateTimeStringISO(new Date(0))));
        ncfile.addVariable(null, taxis);
        dims.add(dimT);

        Dimension jDim = new Dimension("y", numY, true, false, false);
        Dimension iDim = new Dimension("x", numX, true, false, false);

        dims.add(jDim);
        dims.add(iDim);
        ncfile.addDimension(null, iDim);
        ncfile.addDimension(null, jDim);
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));

        String   coordinates = "time y x";
        Variable v           = new Variable(ncfile, null, null, cname);

        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute(new Attribute("long_name", ctitle));
        v.addAttribute(new Attribute("units", cunit));
        v.setSPobject(new Vinfo(numX, numY, hoff, false));
        v.addAttribute(new Attribute(_Coordinate.Axes, coordinates));

        // create coordinate variables
        Variable xaxis = new Variable(ncfile, null, null, "x");

        xaxis.setDataType(DataType.DOUBLE);
        xaxis.setDimensions("x");
        xaxis.addAttribute(new Attribute("standard_name", "projection x coordinate"));
        xaxis.addAttribute(new Attribute("units", "km"));
        xaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoX"));

        double[]       data1      = new double[numX];
        ProjectionImpl projection = new LambertConformal(clat, clon, lat1, lat2);
        double ullat = 51.8294;
        double ullon = -135.8736;
        double lrlat = 17.2454;
        double lrlon = -70.1154;

        ProjectionPointImpl ptul = (ProjectionPointImpl) projection.latLonToProj(new LatLonPointImpl(ullat, ullon));
        ProjectionPointImpl ptlr = (ProjectionPointImpl) projection.latLonToProj(new LatLonPointImpl(lrlat, lrlon));
        ProjectionPointImpl ptc = (ProjectionPointImpl) projection.latLonToProj(new LatLonPointImpl(clat, clon));
        double startX = ptul.getX();
        double startY = ptlr.getY();
        double dx = (ptlr.getX() - ptul.getX())/(numX-1);
        for (int i = 0; i < numX; i++) {
            data1[i] = startX + i*dx;
        }

        Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { numX }, data1);

        xaxis.setCachedData(dataA, false);
        ncfile.addVariable(null, xaxis);

        Variable yaxis = new Variable(ncfile, null, null, "y");

        yaxis.setDataType(DataType.DOUBLE);
        yaxis.setDimensions("y");
        yaxis.addAttribute(new Attribute("standard_name", "projection y coordinate"));
        yaxis.addAttribute(new Attribute("units", "km"));
        yaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoY"));
        data1 = new double[numY];
        double dy = (ptul.getY() - ptlr.getY())/(numY-1);

        for (int i = 0; i < numY; i++) {
            data1[i] = startY + i*dy;
        }

        dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { numY }, data1);
        yaxis.setCachedData(dataA, false);
        ncfile.addVariable(null, yaxis);

        // projection
        // lower left and upper right corner lat/lons
        // modified cylind. equidistant or  CED with lat/lon ration != 1
        Variable ct = new Variable(ncfile, null, null, projection.getClassName());

        ct.setDataType(DataType.CHAR);
        ct.setDimensions("");

        List params = projection.getProjectionParameters();

        for (int i = 0; i < params.size(); i++) {
            Parameter p = (Parameter) params.get(i);

            ct.addAttribute(new Attribute(p));
        }

        ct.addAttribute(new Attribute(_Coordinate.TransformType, "Projection"));
        //ct.addAttribute(new Attribute(_Coordinate.Axes, "lat lon"));
        ct.addAttribute( new Attribute(_Coordinate.Axes, "x y "));
        // fake data
        dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {});
        dataA.setChar(dataA.getIndex(), ' ');
        ct.setCachedData(dataA, false);
        ncfile.addVariable(null, ct);

        return projection;
    }

    /**
     *  construct a raster dataset for NIDS raster products;
     *
     * @return  soff -- not used
     */
    ProjectionImpl  nowrad(int hoff, float rlat1, float rlon1, float rlat2, float rlon2, float dlat, float dlon, Date dd) {
        ArrayList dims = new ArrayList();
        Dimension dimT = new Dimension("time", 1, true, false, false);

        ncfile.addDimension(null, dimT);

        String   timeCoordName = "time";
        Variable taxis         = new Variable(ncfile, null, null, timeCoordName);

        taxis.setDataType(DataType.DOUBLE);
        taxis.setDimensions("time");
        taxis.addAttribute(new Attribute("long_name", "time since base date"));
        taxis.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

        double[] tdata = new double[1];

        tdata[0] = dd.getTime();

        Array dataT = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { 1 }, tdata);

        taxis.setCachedData(dataT, false);

        DateFormatter formatter = new DateFormatter();

        taxis.addAttribute(new Attribute("units", "msecs since " + formatter.toDateTimeStringISO(new Date(0))));
        ncfile.addVariable(null, taxis);
        dims.add(dimT);

        Dimension jDim = new Dimension("lat", numY, true, false, false);
        Dimension iDim = new Dimension("lon", numX, true, false, false);

        dims.add(jDim);
        dims.add(iDim);
        ncfile.addDimension(null, iDim);
        ncfile.addDimension(null, jDim);
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));

        String   coordinates = "time lat lon";
        Variable v           = new Variable(ncfile, null, null, cname);

        v.setDataType(DataType.BYTE);
        v.setDimensions(dims);
        ncfile.addVariable(null, v);
        v.addAttribute(new Attribute("long_name", ctitle));
        v.addAttribute(new Attribute("units", cunit));
        v.setSPobject(new Vinfo(numX, numY, hoff, false));
        v.addAttribute(new Attribute(_Coordinate.Axes, coordinates));

        // create coordinate variables
        Variable xaxis = new Variable(ncfile, null, null, "lon");
        xaxis.setDataType(DataType.DOUBLE);
        xaxis.setDimensions("lon");
        xaxis.addAttribute(new Attribute("long_name", "longitude"));
        xaxis.addAttribute(new Attribute("units", "degree"));
        xaxis.addAttribute(new Attribute(_Coordinate.AxisType, "Lon"));

        double[] data1 = new double[numX];

        for (int i = 0; i < numX; i++) {
            data1[i] = (double) (rlon1 + i * dlon);
        }

        Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { numX }, data1);

        xaxis.setCachedData(dataA, false);
        ncfile.addVariable(null, xaxis);

        Variable yaxis = new Variable(ncfile, null, null, "lat");
        yaxis.setDataType(DataType.DOUBLE);
        yaxis.setDimensions("lat");
        yaxis.addAttribute(new Attribute("long_name", "latitude"));
        yaxis.addAttribute(new Attribute("units", "degree"));
        yaxis.addAttribute(new Attribute(_Coordinate.AxisType, "Lat"));
        data1 = new double[numY];

        for (int i = 0; i < numY; i++) {
            data1[i] = rlat1 + i * dlat;
        }

        dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] { numY }, data1);
        yaxis.setCachedData(dataA, false);
        ncfile.addVariable(null, yaxis);

        // projection
        // lower left and upper right corner lat/lons
        // modified cylind. equidistant or  CED with lat/lon ration != 1
    /*    LatLonProjection llproj = new LatLonProjection("LatitudeLongitudeProjection",
                                  new ProjectionRect(rlat1, rlon1, rlat2, rlon2));
        Variable ct = new Variable(ncfile, null, null, llproj.getClassName());

        ct.setDataType(DataType.CHAR);
        ct.setDimensions("");

        List params = llproj.getProjectionParameters();

        for (int i = 0; i < params.size(); i++) {
            Parameter p = (Parameter) params.get(i);

            ct.addAttribute(new Attribute(p));
        }

        ct.addAttribute(new Attribute(_Coordinate.TransformType, "Projection"));
        ct.addAttribute(new Attribute(_Coordinate.Axes, "lat lon"));

        // fake data
        dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {});
        dataA.setChar(dataA.getIndex(), ' ');
        ct.setCachedData(dataA, false);
        ncfile.addVariable(null, ct);
      */
        return null;
    }



    /**
     *  parsing the product information into netcdf dataset
     */
    void setProductInfo(String prod, Date dd) {
        int    radial  = 0;
        String summary = null;

        if (prod.contains("NOWRADHF")) {
            radial  = 0;
            cmemo   = "NOWRAD  Base Reflectivity at Tilt 1";
            ctitle  = "BREF: Base Reflectivity [dBZ]";
            cunit   = "dBZ";
            cname   = "Reflectivity";
            summary = "NOWRAD Product";
        } else if (prod.contains("USRADHF")) {
            radial  = 0;
            cmemo   = "NOWRAD  Base Reflectivity at Tilt 1";
            ctitle  = "BREF: Base Reflectivity [dBZ]";
            cunit   = "dBZ";
            cname   = "Reflectivity";
            summary = "NOWRAD Product";
        } else if (prod.contains("NEXET")) {
            radial  = 0;
            cmemo   = "NOWRAD Echo Tops";
            ctitle  = "Echo Tops Composite";
            cunit   = "K FT";
            cname   = "EchoTopsComposite";
            summary = "NOWRAD Product";
        } else if (prod.contains("NEXLL")) {
            radial  = 0;
            cmemo   = "NOWRAD Layer Comp. Reflectivity - Low";
            ctitle  = "LayerReflectivityLow";
            cunit   = "dBZ";
            cname   = "Reflectivity";
            summary = "NOWRAD Product";
        } else if (prod.contains("NEXLM")) {
            radial  = 0;
            cmemo   = "NOWRAD Layer Comp. Reflectivity - Mid";
            ctitle  = "LayerReflectivityMid";
            cunit   = "dBZ";
            cname   = "Reflectivity";
            summary = "NOWRAD Product";
        } else if (prod.contains("NEXLH")) {
            radial  = 0;
            cmemo   = "NOWRAD Layer Comp. Reflectivity - High";
            ctitle  = "LayerReflectivityHigh";
            cunit   = "dBZ";
            cname   = "ReflectivityHigh";
            summary = "NOWRAD Product";
        } else if (prod.contains("NEXVI")) {
            radial  = 0;
            cmemo   = "NOWRAD ";
            ctitle  = "Vert. Integrated Liquid Water";
            cunit   = "Knots";
            cname   = "VILwater";
            summary = "NOWRAD ";
        } else {
            ctilt  = "error";
            ctitle = "error";
            cunit  = "error";
            cname  = "error";
        }

        /* add geo global att */
        ncfile.addAttribute(null, new Attribute("summary", "NOWRAD radar composite products." + summary));
        ncfile.addAttribute(null, new Attribute("title", "NOWRAD"));
        ncfile.addAttribute(null, new Attribute("keywords", "NOWRAD"));
        ncfile.addAttribute(null, new Attribute("creator_name", "NOAA/NWS"));
        ncfile.addAttribute(null, new Attribute("creator_url", "http://www.ncdc.noaa.gov/oa/radar/radarproducts.html"));
        ncfile.addAttribute(null, new Attribute("naming_authority", "NOAA/NCDC"));
        ncfile.addAttribute(null, new Attribute("base_date", formatter.toDateOnlyString(dd)));
        ncfile.addAttribute(null, new Attribute("conventions", _Coordinate.Convention));
        ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));

    }

    /* thredds global att */

    /**
     * convert two short into a integer
     * @param s1            short one
     * @param s2            short two
     * @param swapBytes      if swap bytes
     * @return
     */
    public static int shortsToInt(short s1, short s2, boolean swapBytes) {
        byte[] b = new byte[4];

        b[0] = (byte) (s1 >>> 8);
        b[1] = (byte) (s1 >>> 0);
        b[2] = (byte) (s2 >>> 8);
        b[3] = (byte) (s2 >>> 0);

        return bytesToInt(b, false);
    }

    /**
     * convert bytes into integer
     * @param bytes           bytes array
     * @param swapBytes       if need to swap
     * @return
     */
    public static int bytesToInt(byte[] bytes, boolean swapBytes) {
        byte a = bytes[0];
        byte b = bytes[1];
        byte c = bytes[2];
        byte d = bytes[3];

        if (swapBytes) {
            return ((a & 0xff)) + ((b & 0xff) << 8) + ((c & 0xff) << 16) + ((d & 0xff) << 24);
        } else {
            return ((a & 0xff) << 24) + ((b & 0xff) << 16) + ((c & 0xff) << 8) + ((d & 0xff));
        }
    }

    /**
     * get unsigned integer from byte array
     * @param b
     * @param num
     * @return
     */
    int getUInt(byte[] b, int num) {
        int base = 1;
        int i;
        int word = 0;
        int bv[] = new int[num];

        for (i = 0; i < num; i++) {
            bv[i] = convertunsignedByte2Short(b[i]);
        }

        /*
         * Calculate the integer value of the byte sequence
         */
        for (i = num - 1; i >= 0; i--) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    /**
     * get signed integer from bytes
     * @param b
     * @param num
     * @return
     */
    int getInt(byte[] b, int num) {
        int base = 1;
        int i;
        int word = 0;
        int bv[] = new int[num];

        for (i = 0; i < num; i++) {
            bv[i] = convertunsignedByte2Short(b[i]);
        }

        if (bv[0] > 127) {
            bv[0] -= 128;
            base  = -1;
        }

        /*
         * Calculate the integer value of the byte sequence
         */

        for (i = num - 1; i >= 0; i--) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    /**
     * Concatenate two bytes to a 32-bit int value.  <b>a</b> is the high order
     * byte in the resulting int representation, unless swapBytes is true, in
     * which <b>b</b> is the high order byte.
     * @param a high order byte
     * @param b low order byte
     * @param swapBytes byte order swap flag
     * @return 32-bit integer
     */
    public static int bytesToInt(byte a, byte b, boolean swapBytes) {

        // again, high order bit is expressed left into 32-bit form
        if (swapBytes) {
            return (a & 0xff) + ((int) b << 8);
        } else {
            return ((int) a << 8) + (b & 0xff);
        }
    }

    /**
     * convert unsigned byte to short
     * @param b
     * @return
     */
    public short convertunsignedByte2Short(byte b) {
        return (short) ((b < 0)
                        ? (short) b + 256
                        : (short) b);
    }

    /**
     *  convert short to unsigned integer
     * @param b
     * @return
     */
    public int convertShort2unsignedInt(short b) {
        return (b < 0)
               ? (-1) * b + 32768
               : b;
    }

    /**
     * get jave date
     * @param julianDays
     * @param msecs
     * @return
     */
    static public java.util.Date getDate(int julianDays, int msecs) {
        long total = ((long) (julianDays - 1)) * 24 * 3600 * 1000 + msecs;

        return new Date(total);
    }

    /**
     * Flush all data buffers to disk.
     * @throws IOException
     */
    public void flush() throws IOException {
        raf.flush();
    }

    /**
     *  Close the file.
     * @throws IOException
     */
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }

    // variable info for reading/writing
    class Vinfo {
        long    hoff;        // header offset
        boolean isRadial;    // is it a radial variable?
        int     xt;
        int     yt;

        Vinfo(int xt, int yt, long hoff, boolean isRadial) {
            this.xt       = xt;
            this.yt       = yt;
            this.hoff     = hoff;
            this.isRadial = isRadial;
        }
    }
}
