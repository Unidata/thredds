/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.iosp.gini;

import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.unidata.util.Parameter;

import java.io.*;
import java.util.*;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import java.text.*;
import java.nio.*;


/**
 * Netcdf header reading and writing for version 3 file format.
 * This is used by Giniiosp.
 */

class Giniheader {
  static private final int GINI_PIB_LEN = 21;   // gini product identification block
  static private final int GINI_PDB_LEN = 512;  // gini product description block
  static private final int GINI_HED_LEN = GINI_PDB_LEN + GINI_PIB_LEN;  // gini product header
  static private final double DEG_TO_RAD = 0.017453292;
  private boolean debug = false;
  private ucar.nc2.NetcdfFile ncfile;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Giniheader.class);
  int dataStart = 0; // where the data starts
  protected int Z_type = 0;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    try {
      return validatePIB(raf);
    } catch (IOException e) {
      return false;

    }
  }

  static private int findWMOHeader(String pib) {
    int pos = pib.indexOf("KNES");
    if (pos == -1) pos = pib.indexOf("CHIZ");

    if (pos != -1) {                    /* 'KNES' or 'CHIZ' found         */
      pos = pib.indexOf("\r\r\n");    /* ----- UPC mod 20030710 -----   */
      if (pos != -1) {                 /* CR CR NL found             */
        pos = pos + 3;
      }
    } else {
      pos = 0;
    }
    return pos;
  }

  static boolean validatePIB(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    int pos = 0;
    raf.seek(pos);

    // gini header process
    String pib = raf.readString(GINI_PIB_LEN + GINI_HED_LEN);

    return findWMOHeader(pib) != 0;
  }

  byte[] readPIB(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    int pos = 0;
    raf.seek(pos);

    // gini header process
    byte[] b = new byte[GINI_PIB_LEN + GINI_HED_LEN];
    byte[] buf = new byte[GINI_HED_LEN];
    byte[] head = new byte[GINI_PDB_LEN];

    raf.readFully(b);
    String pib = new String(b, CDM.utf8Charset);

    pos = findWMOHeader(pib);
    dataStart = pos + GINI_PDB_LEN;

    // Test the next two bytes to see if the image portion looks like
    // it is zlib-compressed
    byte[] b2 = new byte[] {b[pos], b[pos + 1]};
    int pos1 = 0;

    if (Giniiosp.isZlibHed(b2)) {
      Z_type = 1;
      Inflater inflater = new Inflater(false);
      inflater.setInput(b, pos, GINI_HED_LEN);
      try {
        int resultLength = inflater.inflate(buf, 0, GINI_HED_LEN);
        if (resultLength != GINI_HED_LEN) log.warn("GINI: Zlib inflated image header size error");
      } catch (DataFormatException ex) {
        log.error("ERROR on inflation " + ex.getMessage());
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }

      int inflatedLen = GINI_HED_LEN - inflater.getRemaining();

      String inf = new String(buf, CDM.utf8Charset);
      pos1 = findWMOHeader(inf);

      System.arraycopy(buf, pos1, head, 0, GINI_PDB_LEN);
      dataStart = pos + inflatedLen;

    } else {
      System.arraycopy(b, pos, head, 0, GINI_PDB_LEN);
    }

    if (pos == 0 && pos1 == 0) {
      throw new IOException("Error on Gini File");
    }

    return head;
  }

  void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile) throws IOException {
    this.ncfile = ncfile;
    int proj;                        /* projection type indicator     */
                                            /* 1 - Mercator                  */
                                            /* 3 - Lambert Conf./Tangent Cone*/
                                            /* 5 - Polar Stereographic       */

    int ent_id;                      /* GINI creation entity          */
    int sec_id;                      /* GINI sector ID                */
    int phys_elem;                   /* 1 - Visible, 2- 3.9IR, 3 - 6.7IR ..*/
    int nx;
    int ny;
    int pole;
    int gyear;
    int gmonth;
    int gday;
    int ghour;
    int gminute;
    int gsecond;
    double lonv;                        /* meridian parallel to y-axis */
    double lon1 = 0.0, lon2 = 0.0;
    double lat1 = 0.0, lat2 = 0.0;
    double latt;
    double imageScale = 0.0;

    byte[] head = readPIB(raf);
    ByteBuffer bos = ByteBuffer.wrap(head);

    Attribute att = new Attribute(CDM.CONVENTIONS, "GRIB");
    this.ncfile.addAttribute(null, att);

    bos.position(0);

    //sat_id = (int )( raf.readByte());
    Byte nv = bos.get();
    att = new Attribute("source_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = bos.get();
    ent_id = nv.intValue();
    att = new Attribute("entity_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = bos.get();
    sec_id = nv.intValue();
    att = new Attribute("sector_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = bos.get();
    phys_elem = nv.intValue();
    att = new Attribute("phys_elem", nv);
    this.ncfile.addAttribute(null, att);

    bos.position(bos.position() + 4);

    gyear = (int) (bos.get());
    gyear += (gyear < 50) ? 2000 : 1900; //TODO: Find example where this hack is necessary
    gmonth = (int) (bos.get());
    gday = (int) (bos.get());
    ghour = (int) (bos.get());
    gminute = (int) (bos.get());
    gsecond = (int) (bos.get());

    DateFormat dformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dformat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    Calendar cal = Calendar.getInstance();

    cal.set(Calendar.MILLISECOND, 0);

    cal.set(gyear, gmonth - 1, gday, ghour, gminute, gsecond);
    cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    String dstring = dformat.format(cal.getTime());

    Dimension dimT = new Dimension("time", 1, true, false, false);
    ncfile.addDimension(null, dimT);

    String timeCoordName = "time";
    Variable taxis = new Variable(ncfile, null, null, timeCoordName);
    taxis.setDataType(DataType.DOUBLE);
    taxis.setDimensions("time");
    taxis.addAttribute(new Attribute(CDM.LONG_NAME, "time since base date"));
    taxis.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    double[] tdata = new double[1];
    tdata[0] = cal.getTimeInMillis();
    Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{1}, tdata);
    taxis.setCachedData(dataA, false);
    DateFormatter formatter = new DateFormatter();
    taxis.addAttribute(new Attribute(CDM.UNITS, "msecs since " + formatter.toDateTimeStringISO(new Date(0))));
    ncfile.addVariable(null, taxis);

    //att = new Attribute( "Time", dstring);
    //this.ncfile.addAttribute(null, att);
    this.ncfile.addAttribute(null, new Attribute("time_coverage_start", dstring));
    this.ncfile.addAttribute(null, new Attribute("time_coverage_end", dstring));
    bos.get();   /* skip a byte for hundreds of seconds */

    nv = bos.get();
    att = new Attribute("ProjIndex", nv);
    this.ncfile.addAttribute(null, att);
    proj = nv.intValue();
    if (proj == 1) {
      att = new Attribute("ProjName", "MERCATOR");
    } else if (proj == 3) {
      att = new Attribute("ProjName", "LAMBERT_CONFORNAL");
    } else if (proj == 5) {
      att = new Attribute("ProjName", "POLARSTEREOGRAPHIC");
    }

    this.ncfile.addAttribute(null, att);

    /*
    ** Get grid dimensions
    */

    nx = bos.getShort();
    att = new Attribute("NX", nx);
    this.ncfile.addAttribute(null, att);

    ny = bos.getShort();
    att = new Attribute("NY", ny);
    this.ncfile.addAttribute(null, att);

    ProjectionImpl projection = null;
    double dxKm = 0.0, dyKm = 0.0, latin, lonProjectionOrigin;

    switch (proj) {

      case 1:                                                    /* Mercator */
        /*
        ** Get the latitude and longitude of first and last "grid" points
        */

        /* Latitude of first grid point */
        lat1 = readScaledInt(bos);
        att = new Attribute("Latitude0", lat1);
        this.ncfile.addAttribute(null, att);

        lon1 = readScaledInt(bos);
        att = new Attribute("Longitude0", lon1);
        this.ncfile.addAttribute(null, att);

        /* Longitude of last grid point */
        bos.get(); /* skip one byte */

        lat2 = readScaledInt(bos);
        att = new Attribute("LatitudeN", lat2);
        this.ncfile.addAttribute(null, att);

        lon2 = readScaledInt(bos);
        att = new Attribute("LongitudeN", lon2);
        this.ncfile.addAttribute(null, att);

        /*
        ** Hack to catch incorrect sign of lon2 in header.
        */

        //    if ( lon1 > 0.0 && lon2 < 0.0 ) lon2 *= -1;
        double lon_1 = lon1;
        double lon_2 = lon2;
        if (lon1 < 0) lon_1 += 360.0;
        if (lon2 < 0) lon_2 += 360.0;

        lonv = (lon_1 + lon_2) / 2.0;

        if (lonv > 180.0) lonv -= 360.0;
        if (lonv < -180.0) lonv += 360.0;
        /*
        ** Get the "Latin" parameter.  The ICD describes this value as:
        ** "Latin - The latitude(s) at which the Mercator projection cylinder
        ** intersects the earth."  It should read that this is the latitude
        ** at which the image resolution is that defined by octet 41.
        */
        bos.getInt(); /* skip 4 bytes */
        bos.get();    /* skip 1 byte */

        /* Latitude of proj cylinder intersects */
        latin = readScaledInt(bos);
        att = new Attribute("LatitudeX", latin);
        this.ncfile.addAttribute(null, att);

        projection = new Mercator(lonv, latin);
        break;

      case 3:                               /* Lambert Conformal             */
      case 5:                               /* Polar Stereographic           */
        /*
        ** Get lat/lon of first grid point
        */
        lat1 = readScaledInt(bos);
        lon1 = readScaledInt(bos);
        /*
        ** Get Lov - the orientation of the grid; i.e. the east longitude of
        ** the meridian which is parallel to the y-aixs
        */
        bos.get(); /* skip one byte */

        lonv = readScaledInt(bos);
        lonProjectionOrigin = lonv;
        att = new Attribute("Lov", lonv);
        this.ncfile.addAttribute(null, att);

        /*
        ** Get distance increment of grid
        */
        dxKm = readScaledInt(bos);
        att = new Attribute("DxKm", dxKm);
        this.ncfile.addAttribute(null, att);

        dyKm = readScaledInt(bos);
        att = new Attribute("DyKm", dyKm);
        this.ncfile.addAttribute(null, att);

        /* calculate the lat2 and lon2 */
        if (proj == 5) {
          latt = 60.0;            /* Fixed for polar stereographic */
          imageScale = (1. + Math.sin(DEG_TO_RAD * latt)) / 2.;
        }

        lat2 = lat1 + dyKm * (ny - 1) / 111.26;

        /* Convert to east longitude */
        if (lonv < 0.) lonv += 360.;
        if (lon1 < 0.) lon1 += 360.;

        lon2 = lon1 + dxKm * (nx - 1) / 111.26 * Math.cos(DEG_TO_RAD * lat1);

        /*
        ** Convert to normal longitude to McIDAS convention
        */
        lonv = (lonv > 180.) ? -(360. - lonv) : lonv;
        lon1 = (lon1 > 180.) ? -(360. - lon1) : lon1;
        lon2 = (lon2 > 180.) ? -(360. - lon2) : lon2;
        /*
        ** Check high bit of octet for North or South projection center
        */
        nv = bos.get();
        pole = nv.intValue();
        pole = (pole > 127) ? -1 : 1;
        att = new Attribute("ProjCenter", pole);
        this.ncfile.addAttribute(null, att);

        bos.get(); /* skip one byte for Scanning mode */

        latin = readScaledInt(bos);
        att = new Attribute("Latin", latin);
        this.ncfile.addAttribute(null, att);

        if (proj == 3)
          projection = new LambertConformal(latin, lonProjectionOrigin, latin, latin);
        else // (proj == 5)
          projection = new Stereographic(90.0, lonv, imageScale);

        break;

      default:
        System.out.println("unimplemented projection");
    }

    this.ncfile.addAttribute(null, new Attribute("title", gini_GetEntityID(ent_id)));
    this.ncfile.addAttribute(null, new Attribute("summary", getPhysElemSummary(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("id", gini_GetSectorID(sec_id)));
    this.ncfile.addAttribute(null, new Attribute("keywords_vocabulary", gini_GetPhysElemID(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
    this.ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.toString()));
    this.ncfile.addAttribute(null, new Attribute("standard_name_vocabulary", getPhysElemLongName(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("creator_name", "UNIDATA"));
    this.ncfile.addAttribute(null, new Attribute("creator_url", "http://www.unidata.ucar.edu/"));
    this.ncfile.addAttribute(null, new Attribute("naming_authority", "UCAR/UCP"));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lat_min", lat1));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lat_max", lat2));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lon_min", lon1));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lon_max", lon2));
    //this.ncfile.addAttribute(null, new Attribute("geospatial_vertical_min", new Float(0.0)));
    //this.ncfile.addAttribute(null, new Attribute("geospatial_vertical_max", new Float(0.0)));

    /*
     * Get the image resolution.
     */
    bos.position(41);  /* jump to 42 bytes of PDB */
    nv = bos.get();      /* Res [km] */
    att = new Attribute("imageResolution", nv);
    this.ncfile.addAttribute(null, att);
    // if(proj == 1)
    //     dyKm = nv.doubleValue()/dyKm;
    /* compression flag 43 byte */

    nv = bos.get();      /* Res [km] */
    att = new Attribute("compressionFlag", nv);
    this.ncfile.addAttribute(null, att);

    if (DataType.unsignedByteToShort(nv) == 128) {
      Z_type = 2;
      //out.println( "ReadNexrInfo:: This is a Z file ");
    }

   /* new 47 - 60 */
    bos.position(46);
    nv = bos.get();      /* Cal indicator */
    int navcal = DataType.unsignedByteToShort(nv);
    int[] calcods = null;
    if (navcal == 128)
      calcods = getCalibrationInfo(bos, phys_elem, ent_id);

    // only one data variable per gini file

    String vname = gini_GetPhysElemID(phys_elem, ent_id);
    Variable var = new Variable(ncfile, ncfile.getRootGroup(), null, vname);
    var.addAttribute(new Attribute(CDM.LONG_NAME, getPhysElemLongName(phys_elem, ent_id)));
    var.addAttribute(new Attribute(CDM.UNITS, getPhysElemUnits(phys_elem, ent_id)));
    // var.addAttribute( new Attribute(CDM.MISSING_VALUE, new Byte((byte) 0))); // ??

    // get dimensions
    List<Dimension> dims = new ArrayList<>();

    Dimension dimX = new Dimension("x", nx, true, false, false);
    Dimension dimY = new Dimension("y", ny, true, false, false);

    ncfile.addDimension(null, dimY);
    ncfile.addDimension(null, dimX);

    dims.add(dimT);
    dims.add(dimY);
    dims.add(dimX);

    var.setDimensions(dims);

    // size and beginning data position in file
    long begin = dataStart;
    if (debug) log.warn(" name= " + vname + " velems=" + var.getSize() + " begin= " + begin + "\n");
    if (navcal == 128) {
      var.setDataType(DataType.FLOAT);
      var.setSPobject(new Vinfo(begin, nx, ny, calcods));
     /*   var.addAttribute(new Attribute("_Unsigned", "true"));
        int numer = calcods[0] - calcods[1];
        int denom = calcods[2] - calcods[3];
        float a  = (numer*1.f) / (1.f*denom);
        float b  = calcods[0] - a * calcods[2];
        var.addAttribute( new Attribute("scale_factor", new Float(a)));
        var.addAttribute( new Attribute("add_offset", new Float(b)));
      */
    } else {
      var.setDataType(DataType.BYTE);
      var.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
      // var.addAttribute(new Attribute("_missing_value", new Short((short)255)));
      var.addAttribute(new Attribute(CDM.SCALE_FACTOR, (short) (1)));
      var.addAttribute(new Attribute(CDM.ADD_OFFSET, (short) (0)));
      var.setSPobject(new Vinfo(begin, nx, ny));
    }
    String coordinates = "x y time";
    var.addAttribute(new Attribute(_Coordinate.Axes, coordinates));
    ncfile.addVariable(null, var);

    // add coordinate information. we need:
    // nx, ny, dx, dy,
    // latin, lov, la1, lo1

    // we have to project in order to find the origin
    ProjectionPoint start = projection.latLonToProj(new LatLonPointImpl(lat1, lon1));
    if (debug) log.warn("start at proj coord " + start);

    double startx = start.getX();
    double starty = start.getY();

    // create coordinate variables
    Variable xaxis = new Variable(ncfile, null, null, "x");
    xaxis.setDataType(DataType.DOUBLE);
    xaxis.setDimensions("x");
    xaxis.addAttribute(new Attribute(CDM.LONG_NAME, "projection x coordinate"));
    xaxis.addAttribute(new Attribute(CDM.UNITS, "km"));
    xaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoX"));
    double[] data = new double[nx];
    if (proj == 1) {
      double lon_1 = lon1;
      double lon_2 = lon2;
      if (lon1 < 0) lon_1 += 360.0;
      if (lon2 < 0) lon_2 += 360.0;
      double dx = (lon_2 - lon_1) / (nx - 1);

      for (int i = 0; i < data.length; i++) {
        double ln = lon1 + i * dx;
        ProjectionPoint pt = projection.latLonToProj(new LatLonPointImpl(lat1, ln));
        data[i] = pt.getX();  // startx + i*dx;
      }
    } else {
      for (int i = 0; i < data.length; i++)
        data[i] = startx + i * dxKm;
    }

    dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{nx}, data);
    xaxis.setCachedData(dataA, false);
    ncfile.addVariable(null, xaxis);

    Variable yaxis = new Variable(ncfile, null, null, "y");
    yaxis.setDataType(DataType.DOUBLE);
    yaxis.setDimensions("y");
    yaxis.addAttribute(new Attribute(CDM.LONG_NAME, "projection y coordinate"));
    yaxis.addAttribute(new Attribute(CDM.UNITS, "km"));
    yaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoY"));
    data = new double[ny];
    double endy = starty + dyKm * (data.length - 1); // apparently lat1,lon1 is always the lower ledt, but data is upper left
    if (proj == 1) {
      double dy = (lat2 - lat1) / (ny - 1);
      for (int i = 0; i < data.length; i++) {
        double la = lat2 - i * dy;
        ProjectionPoint pt = projection.latLonToProj(new LatLonPointImpl(la, lon1));
        data[i] = pt.getY();  //endyy - i*dy;
      }
    } else {
      for (int i = 0; i < data.length; i++)
        data[i] = endy - i * dyKm;
    }
    dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{ny}, data);
    yaxis.setCachedData(dataA, false);
    ncfile.addVariable(null, yaxis);

    // coordinate transform variable
    Variable ct = new Variable(ncfile, null, null, projection.getClassName());
    ct.setDataType(DataType.CHAR);
    ct.setDimensions("");
    for (Parameter p : projection.getProjectionParameters()) {
      ct.addAttribute(new Attribute(p));
    }
    ct.addAttribute(new Attribute(_Coordinate.TransformType, "Projection"));
    ct.addAttribute(new Attribute(_Coordinate.Axes, "x y "));
    // fake data
    dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[]{});
    dataA.setChar(dataA.getIndex(), ' ');
    ct.setCachedData(dataA, false);

    ncfile.addVariable(null, ct);
    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, _Coordinate.Convention));

    // finish
    ncfile.finish();
  }

  int[] getCalibrationInfo(ByteBuffer bos, int phys_elem, int ent_id) {

    bos.position(46);
    byte nv = bos.get();      /* Cal indicator */
    int navcal = DataType.unsignedByteToShort(nv);
    int[] calcods = null;
    if (navcal == 128) {    /* Unidata Cal block found; unpack values */
      int scale = 10000;
      int jscale = 100000000;
      byte[] unsb = new byte[8];
      bos.get(unsb);
      bos.position(55);
      nv = bos.get();
      int calcod = DataType.unsignedByteToShort(nv);

      if (calcod > 0) {
        calcods = new int[5 * calcod + 1];
        calcods[0] = calcod;
        for (int i = 0; i < calcod; i++) {

          bos.position(56 + i * 16);
          int minb = bos.getInt() / 10000;        /* min brightness values         */
          int maxb = bos.getInt() / 10000;       /* max brightness values         */
          int mind = bos.getInt();               /* min data values               */
          int maxd = bos.getInt();               /* max data values               */

          int idscal = 1;
          while (mind % idscal == 0 && maxd % idscal == 0) {
            idscal *= 10;
          }
          idscal /= 10;

          if (idscal < jscale) jscale = idscal;

          calcods[1 + i * 5] = mind;
          calcods[2 + i * 5] = maxd;
          calcods[3 + i * 5] = minb;
          calcods[4 + i * 5] = maxb;
          calcods[5 + i * 5] = 0;

        }

        if (jscale > scale) jscale = scale;
        scale /= jscale;

        if (gini_GetPhysElemID(phys_elem, ent_id).contains("Precipitation")) {
          if (scale < 100) {
            jscale /= (100 / scale);
            scale = 100;
          }
        }

        for (int i = 0; i < calcod; i++) {
          calcods[1 + i * 5] /= jscale;
          calcods[2 + i * 5] /= jscale;
          calcods[5 + i * 5] = scale;
        }

      }

    }

    return calcods;
  }


  int gini_GetCompressType() {
    return Z_type;
  }

  // Return the string of Sector for the GINI image file
  String gini_GetSectorID(int ent_id) {
    String name;
    switch (ent_id) {
      case 0:
        name = "Northern Hemisphere Composite";
        break;
      case 1:
        name = "East CONUS";
        break;
      case 2:
        name = "West CONUS";
        break;
      case 3:
        name = "Alaska Regional";
        break;
      case 4:
        name = "Alaska National";
        break;
      case 5:
        name = "Hawaii Regional";
        break;
      case 6:
        name = "Hawaii National";
        break;
      case 7:
        name = "Puerto Rico Regional";
        break;
      case 8:
        name = "Puerto Rico National";
        break;
      case 9:
        name = "Supernational";
        break;
      case 10:
        name = "NH Composite - Meteosat/GOES E/ GOES W/GMS";
        break;
      case 11:
        name = "Central CONUS";
        break;
      case 12:
        name = "East Floater";
        break;
      case 13:
        name = "West Floater";
        break;
      case 14:
        name = "Central Floater";
        break;
      case 15:
        name = "Polar Floater";
        break;
      default:
        name = "Unknown-ID";
    }

    return name;


  }

  // Return the channel ID for the GINI image file
  String gini_GetEntityID(int ent_id) {
    String name;
    switch (ent_id) {
      case 2:
        name = "Miscellaneous";
        break;
      case 3:
        name = "JERS";
        break;
      case 4:
        name ="ERS/QuikSCAT/Scatterometer";
        break;
      case 5:
        name = "POES/NPOESS";
        break;
      case 6:
        name = "Composite";
        break;
      case 7:
        name = "DMSP satellite Image";
        break;
      case 8:
        name = "GMS satellite Image";
        break;
      case 9:
        name = "METEOSAT satellite Image";
        break;
      case 10:
        name = "GOES-7 satellite Image";
        break;
      case 11:
        name = "GOES-8 satellite Image";
        break;
      case 12:
        name = "GOES-9 satellite Image";
        break;
      case 13:
        name = "GOES-10 satellite Image";
        break;
      case 14:
        name = "GOES-11 satellite Image";
        break;
      case 15:
        name = "GOES-12 satellite Image";
        break;
      case 16:
        name = "GOES-13 satellite Image";
        break;
      case 17:
        name = "GOES-14 satellite Image";
        break;
      case 18:
        name = "GOES-15 satellite Image";
        break;
      case 19: // GOES-R
        name = "GOES-16 satellite Image";
        break;
      case 99: // special snowflake GEMPAK Composite Images generated by Unidata
        name = "RADAR-MOSIAC Composite Image";
        break;
      default:
        name = "Unknown";
    }

    return name;


  }

  // Return the channel ID for the GINI image file
  String gini_GetPhysElemID(int phys_elem, int ent_id) {
    String name;
    switch (phys_elem) {
      case 1:
        name = "VIS";
        break;
      case 3:
        name = "IR_WV";
        break;
      case 2:
      case 4:
      case 5:
      case 6:
      case 7:
        name = "IR";
        break;
      case 13:
        name = "LI";
        break;
      case 14:
        name = "PW";
        break;
      case 15:
        name = "SFC_T";
        break;
      case 16:
        name = "LI";
        break;
      case 17:
        name = "PW";
        break;
      case 18:
        name = "SFC_T";
        break;
      case 19:
        name = "CAPE";
        break;
      case 20:
        name = "T";
        break;
      case 21:
        name = "WINDEX";
        break;
      case 22:
        name = "DMPI";
        break;
      case 23:
        name = "MDPI";
        break;
      case 25:
        if (ent_id == 99)
          name = "HHC";
        else
          name = "Volcano_imagery";
        break;
      case 26:
        name = "EchoTops";
        break;
      case 27:
        if (ent_id == 99)
          name = "Reflectivity";
        else
          name = "CTP";
        break;
      case 28:
        if (ent_id == 99)
          name = "Reflectivity";
        else
          name = "Cloud_Amount";
        break;
      case 29:
        name = "VIL";
        break;
      case 30:
      case 31:
        name = "Precipitation";
        break;
      case 40:
      case 41:
      case 42:
      case 43:
      case 44:
      case 45:
      case 46:
      case 47:
      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
        name = "sounder_imagery";
        break;
      case 59:
        name = "VIS_sounder";
        break;
      default:
        name = "Unknown";
    }

    return name;


  }


  // ??
  String getPhysElemUnits(int phys_elem, int ent_id) {
    switch (phys_elem) {
      case 1:
      case 3:
      case 2:
      case 4:
      case 5:
      case 6:
      case 7:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 25:
      case 43:
      case 48:
      case 50:
      case 51:
      case 52:
      case 55:
      case 57:
      case 59:
        return "N/A";
      case 26:
        return "kft";
      case 27:
        if (ent_id == 99)
          return "dBz";
        else
          return "N/A";
      case 28:
        if (ent_id == 99)
          return "dBz";
        else
          return "N/A";
      case 29:
        return "kg m-2";
      case 30:
        return "IN";
      case 31:
        return "IN";
      default:
        return "Unknown";
    }
  }

  // Return the channel ID for the GINI image file

  String getPhysElemLongName(int phys_elem, int ent_id) {
    switch (phys_elem) {
      case 1:
        return "Imager Visible";
      case 2:
        return "Imager 3.9 micron IR";
      case 3:
        return "Imager 6.7/6.5 micron IR (WV)";
      case 4:
        return "Imager 11 micron IR";
      case 5:
        return "Imager 12 micron IR";
      case 6:
        return "Imager 13 micron IR";
      case 7:
        return "Imager 1.3 micron IR";
      case 13:
        return "Lifted Index LI";
      case 14:
        return "Precipitable Water PW";
      case 15:
        return "Surface Skin Temperature";
      case 16:
        return "Lifted Index LI";
      case 17:
        return "Precipitable Water PW";
      case 18:
        return "Surface Skin Temperature";
      case 19:
        return "Convective Available Potential Energy";
      case 20:
        return "land-sea Temperature";
      case 21:
        return "Wind Index";
      case 22:
        return "Dry Microburst Potential Index";
      case 23:
        return "Microburst Potential Index";
      case 24:
        return "Derived Convective Inhibition";
      case 25:
        if (ent_id == 99)
          return "1km National Hybrid Hydrometeor Classification Composite (Unidata)";
        else
          return "Volcano_imagery";
      case 26:
        if (ent_id == 99)
          return "1 km National Echo Tops Composite (Unidata)";
        else
          return "4 km National Echo Tops";
      case 27:
        if (ent_id == 99)
          return "1 km National Base Reflectivity Composite (Unidata)";
        else
          return "Cloud Top Pressure or Height";
      case 28:
        if (ent_id == 99)
          return "1 km National Reflectivity Composite (Unidata)";
        else
          return "Cloud Amount";
      case 29:
        if (ent_id == 99)
          return "1 km National Vertically Integrated Liquid Water (Unidata)";
        else
          return "4 km National Vertically Integrated Liquid Water";
      case 30:
        if (ent_id == 99)
          return "1 km National 1-hour Precipitation (Unidata)";
        else
          return "Surface wind speeds over oceans and Great Lakes";
      case 31:
        if (ent_id == 99)
          return "4 km National Storm Total Precipitation (Unidata)";
        else
          return "Surface Wetness";
      case 32:
        return "Ice concentrations";
      case 33:
        return "Ice type";
      case 34:
        return "Ice edge";
      case 35:
        return "Cloud water content";
      case 36:
        return "Surface type";
      case 37:
        return "Snow indicator";
      case 38:
        return "Snow/water content";
      case 39:
        return "Derived volcano imagery";
      case 41:
        return "Sounder 14.71 micron imagery";
      case 42:
        return "Sounder 14.37 micron imagery";
      case 43:
        return "Sounder 14.06 micron imagery";
      case 44:
        return "Sounder 13.64 micron imagery";
      case 45:
        return "Sounder 13.37 micron imagery";
      case 46:
        return "Sounder 12.66 micron imagery";
      case 47:
        return "Sounder 12.02 micron imagery";
      case 48:
        return "11.03 micron sounder image";
      case 49:
        return "Sounder 11.03 micron imagery";
      case 50:
        return "7.43 micron sounder image";
      case 51:
        return "7.02 micron sounder image";
      case 52:
        return "6.51 micron sounder image";
      case 53:
        return "Sounder 4.57 micron imagery";
      case 54:
        return "Sounder 4.52 micron imagery";
      case 55:
        return "4.45 micron sounder image";
      case 56:
        return "Sounder 4.13 micron imagery";
      case 57:
        return "3.98 micron sounder image";
      case 58:
        return "Sounder 3.74 micron imagery";
      case 59:
        return "VIS sounder image ";
      default:
        return "unknown physical element " + phys_elem;
    }
  }

  String getPhysElemSummary(int phys_elem, int ent_id) {
    switch (phys_elem) {
      case 1:
        return "Satellite Product Imager Visible";
      case 2:
        return "Satellite Product Imager 3.9 micron IR";
      case 3:
        return "Satellite Product Imager 6.7/6.5 micron IR (WV)";
      case 4:
        return "Satellite Product Imager 11 micron IR";
      case 5:
        return "Satellite Product Imager 12 micron IR";
      case 6:
        return "Satellite Product Imager 13 micron IR";
      case 7:
        return "Satellite Product Imager 1.3 micron IR";
      case 13:
        return "Imager Based Derived Lifted Index LI";
      case 14:
        return "Imager Based Derived Precipitable Water PW";
      case 15:
        return "Imager Based Derived Surface Skin Temperature";
      case 16:
        return "Sounder Based Derived Lifted Index LI";
      case 17:
        return "Sounder Based Derived Precipitable Water PW";
      case 18:
        return "Sounder Based Derived Surface Skin Temperature";
      case 19:
        return "Derived Convective Available Potential Energy CAPE";
      case 20:
        return "Derived land-sea Temperature";
      case 21:
        return "Derived Wind Index WINDEX";
      case 22:
        return "Derived Dry Microburst Potential Index DMPI";
      case 23:
        return "Derived Microburst Day Potential Index MDPI";
      case 43:
        return "Satellite Product 14.06 micron sounder image";
      case 48:
        return "Satellite Product 11.03 micron sounder image";
      case 50:
        return "Satellite Product 7.43 micron sounder image";
      case 51:
        return "Satellite Product 7.02 micron sounder image";
      case 52:
        return "Satellite Product 6.51 micron sounder image";
      case 55:
        return "Satellite Product 4.45 micron sounder image";
      case 57:
        return "Satellite Product 3.98 micron sounder image";
      case 59:
        return "Satellite Product VIS sounder visible image ";
      case 25:
        if (ent_id == 99)
          return "National Hybrid Hydrometeor Classification Composite at Resolution 1 km";
        else
          return "Satellite Derived Volcano_imagery";
      case 26:
        if (ent_id == 99)
          return "Nexrad Level 3 National Echo Tops at Resolution 1 km";
        else
          return "Nexrad Level 3 National Echo Tops at Resolution 4 km";
      case 27:
        if (ent_id == 99)
          return "Nexrad Level 3 Base Reflectivity National Composition at Resolution 1 km";
        else
          return "Gridded Cloud Top Pressure or Height";
      case 28:
        if (ent_id == 99)
          return "Nexrad Level 3 National 248 nm Base Composite Reflectivity at Resolution 2 km";
        else
          return "Gridded Cloud Amount";
      case 29:
        if (ent_id == 99)
          return "Nexrad Level 3 National Vertically Integrated Liquid Water at Resolution 1 km";
        else
          return "Nexrad Level 3 National Vertically Integrated Liquid Water at Resolution 4 km";
      case 30:
        return "Nexrad Level 3 1 Hour Precipitation National Composition at Resolution 2 km";
      case 31:
        return "Nexrad Level 3 Storm Total Precipitation National Composition at Resolution 4 km";
      default:
        return "unknown";
    }

  }

  // Read a scaled, 3-byte integer from file and convert to double
  private double readScaledInt(ByteBuffer buf) {
    // Get the first two bytes
    short s1 = buf.getShort();

    // And the last one as unsigned
    short s2 = DataType.unsignedByteToShort(buf.get());

    // Get the sign bit, converting from 0 or 2 to +/- 1.
    int posneg = 1 - ((s1 & 0x8000) >> 14);

    // Combine the first two bytes (without sign bit) with the last byte.
    // Multiply by proper factor for +/-
    int nn = (((s1 & 0x7FFF) << 8) | s2) * posneg;
    return (double) nn / 10000.0;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  // variable info for reading/writing
  static class Vinfo {
    long begin; // offset of start of data from start of file
    int nx;
    int ny;
    int[] levels;

    Vinfo(long begin, int x, int y) {
      this.begin = begin;
      this.nx = x;
      this.ny = y;
    }

    Vinfo(long begin, int x, int y, int[] levels) {
      this.begin = begin;
      this.nx = x;
      this.ny = y;
      this.levels = levels;
    }

  }

}
