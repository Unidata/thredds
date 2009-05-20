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
package ucar.nc2.iosp.gini;


import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.util.Parameter;
import ucar.ma2.Array;
import ucar.ma2.DataType;
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
    static final byte[] MAGIC = new byte[] {0x43, 0x44, 0x46, 0x01 };
    static final int MAGIC_DIM = 10;
    static final int MAGIC_VAR = 11;
    static final int MAGIC_ATT = 12;
    private boolean debug = false, debugPos = false, debugString = false, debugHeaderSize = false;
    private ucar.unidata.io.RandomAccessFile raf;
    private ucar.nc2.NetcdfFile ncfile;
   // private PrintStream out = System.out;
    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Giniheader.class);
    int numrecs = 0; // number of records written
    int recsize = 0; // size of each record (padded)
    int dataStart = 0; // where the data starts
    int recStart = 0; // where the record data starts
    int GINI_PIB_LEN = 21;   // gini product indentification block
    int GINI_PDB_LEN = 512;  // gini product description block
    int GINI_HED_LEN = 533;  // gini product header
    double DEG_TO_RAD = 0.017453292;
    double EARTH_RAD_KMETERS = 6371.200;
    byte Z_DEFLATED = 8;
    byte DEF_WBITS = 15;
    private long actualSize, calcSize;
    protected int Z_type = 0;



    public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf)
    {
        try{
             return validatePIB( raf );
        }
        catch ( IOException e )
        {
            return false;

        }
    }

  /**
   * Read the header and populate the ncfile
   * @param raf
   * @throws IOException
   */
    boolean validatePIB(ucar.unidata.io.RandomAccessFile raf ) throws IOException {
        this.raf = raf;
        this.actualSize = raf.length();
        int pos = 0;
        raf.seek(pos);

        // gini header process
        byte[] b = new byte[GINI_PIB_LEN + GINI_HED_LEN];

        raf.read(b);
        String pib = new String(b);

        pos = pib.indexOf( "KNES" );
        if ( pos == -1 ) pos = pib.indexOf ( "CHIZ" );

        if ( pos != -1 ) {                    /* 'KNES' or 'CHIZ' found         */
            pos = pib.indexOf ( "\r\r\n" );    /* <<<<< UPC mod 20030710 >>>>>   */
            if ( pos != -1 ) {                 /* CR CR NL found             */
              pos  = pos + 3;
            }
        } else {
            pos = 0;
            return false;
        }
            return true;
    }


  /**
   * Read the header and populate the ncfile
   * @param raf
   * @throws IOException
   */
   byte[] readPIB(ucar.unidata.io.RandomAccessFile raf ) throws IOException {
    this.raf = raf;
    this.actualSize = raf.length();
    int doff = 0;
    int pos = 0;
    raf.seek(pos);

    // gini header process
    byte[] b = new byte[GINI_PIB_LEN + GINI_HED_LEN];
    byte[] buf = new byte[GINI_HED_LEN ];
    byte[] head = new byte[GINI_PDB_LEN ];

    raf.read(b);
    String pib = new String(b);

    //if( !pib.startsWith("TICZ")) return (int)pos; // gini header start with TICZ 99....

    pos = pib.indexOf( "KNES" );
    if ( pos == -1 ) pos = pib.indexOf ( "CHIZ" );

    if ( pos != -1 ) {                    /* 'KNES' or 'CHIZ' found         */
       pos = pib.indexOf ( "\r\r\n" );    /* <<<<< UPC mod 20030710 >>>>>   */
       if ( pos != -1 ) {                 /* CR CR NL found             */
          pos  = pos + 3;
       }
    } else {
      pos = 0;
    }

    dataStart = pos + GINI_PDB_LEN;

    // Test the next two bytes to see if the image portion looks like
    // it is zlib-compressed
    byte[] b2 = new byte[2];
    b2[0] = b[pos];
    b2[1] = b[pos+1];
    Inflater inflater = new Inflater( false);
    int resultLength = 0;
    int inflatedLen = 0;
    int pos1 = 0;

    if( isZlibHed( b2 ) == 1) {
       Z_type = 1;
       inflater.setInput(b, pos, GINI_HED_LEN );
       try {
            resultLength = inflater.inflate(buf, 0, GINI_HED_LEN);
       }
       catch (DataFormatException ex) {
          log.warn("ERROR on inflation "+ex.getMessage());
          ex.printStackTrace();
          throw new IOException( ex.getMessage());
       }

       if(resultLength != GINI_HED_LEN )System.out.println("Zlib inflated image header size error");
       inflatedLen = GINI_HED_LEN - inflater.getRemaining();

       String inf = new String(buf);
       pos1 = inf.indexOf( "KNES" );
       if ( pos1 == -1 ) pos1 = inf.indexOf ( "CHIZ" );

       if ( pos1 != -1 ) {                    /* 'KNES' or 'CHIZ' found         */
          pos1 = inf.indexOf ( "\r\r\n" );    /* <<<<< UPC mod 20030710 >>>>>   */
          if ( pos1 != -1 ) {                 /* CR CR NL found             */
            pos1  = pos1 + 3;
          }
       } else {
          pos1 = 0;
       }

       System.arraycopy(buf, pos1, head, 0, GINI_PDB_LEN );
       dataStart = pos + inflatedLen;

    } else {
       System.arraycopy(b, pos, head, 0, GINI_PDB_LEN );
    }

    if( pos == 0 && pos1 == 0 ) return null;

    return head;

  }

  void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, PrintStream out) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    int        proj;                        /* projection type indicator     */
                                            /* 1 - Mercator                  */
                                            /* 3 - Lambert Conf./Tangent Cone*/
                                            /* 5 - Polar Stereographic       */

    int        ent_id;                      /* GINI creation entity          */
    int        sec_id;                      /* GINI sector ID                */
    int        phys_elem;                   /* 1 - Visible, 2- 3.9IR, 3 - 6.7IR ..*/
    int        nx;
    int        ny;
    int        pole;
    int        gyear;
    int        gmonth;
    int        gday;
    int        ghour;
    int        gminute;
    int        gsecond;
    double     lonv;                        /* meridian parallel to y-axis */
    double     diff_lon;
    double     lon1 = 0.0, lon2 = 0.0;
    double     lat1 = 0.0, lat2 = 0.0;
    double     latt = 0.0, lont = 0.0;
    double     imageScale = 0.0;
    //long       hoff = 0;


    // long pos = GINI_PIB_LEN;
    byte [] head = readPIB(raf );
    ByteBuffer bos = ByteBuffer.wrap(head);
    //if (out != null) this.out = out;
    actualSize = raf.length();

    Attribute att = new Attribute( "Conventions", "GRIB");
    this.ncfile.addAttribute(null, att);

    bos.position(0);
    byte[] b2 = new byte[2];

    //sat_id = (int )( raf.readByte());
    Byte nv = new Byte(bos.get());
    att = new Attribute( "source_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = new Byte( bos.get());
    ent_id = nv.intValue();
    att = new Attribute( "entity_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = new Byte( bos.get());
    sec_id = nv.intValue();
    att = new Attribute( "sector_id", nv);
    this.ncfile.addAttribute(null, att);

    nv = new Byte ( bos.get());
    phys_elem = nv.intValue();
    att = new Attribute( "phys_elem", nv);
    this.ncfile.addAttribute(null, att);

    bos.position(bos.position() + 4);

    gyear = (int) ( bos.get());
      gyear += ( gyear < 50 ) ? 2000 : 1900;
    gmonth = (int) ( bos.get());
    gday = (int) ( bos.get());
    ghour = (int) ( bos.get());
    gminute = (int) ( bos.get());
    gsecond = (int) ( bos.get());

    DateFormat dformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dformat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    Calendar cal = Calendar.getInstance();
    cal.set(gyear, gmonth-1, gday, ghour, gminute, gsecond);
    cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    String dstring = dformat.format(cal.getTime());

    Dimension dimT  = new Dimension( "time", 1, true, false, false);
    ncfile.addDimension( null, dimT);

    String timeCoordName = "time";
    Variable taxis = new Variable(ncfile, null, null, timeCoordName);
    taxis.setDataType(DataType.DOUBLE);
    taxis.setDimensions("time");
    taxis.addAttribute( new Attribute("long_name", "time since base date"));
    taxis.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    double [] tdata = new double[1];
    tdata[0] = cal.getTimeInMillis();
    Array dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {1}, tdata);
    taxis.setCachedData( dataA, false);
    DateFormatter formatter = new DateFormatter();
    taxis.addAttribute( new Attribute("units", "msecs since "+formatter.toDateTimeStringISO(new Date(0))));
    ncfile.addVariable(null, taxis);

    //att = new Attribute( "Time", dstring);
    //this.ncfile.addAttribute(null, att);
    this.ncfile.addAttribute(null, new Attribute("time_coverage_start", dstring));
    this.ncfile.addAttribute(null, new Attribute("time_coverage_end", dstring));
    bos.get();   /* skip a byte for hundreds of seconds */

    nv   = new Byte ( bos.get());
    att = new Attribute( "ProjIndex", nv);
    this.ncfile.addAttribute(null, att);
    proj = nv.intValue();
    if( proj == 1) {
     att = new Attribute( "ProjName", "MERCATOR");
    } else if (proj == 3) {
     att = new Attribute( "ProjName", "LAMBERT_CONFORNAL");
    } else if (proj == 5) {
     att = new Attribute( "ProjName", "POLARSTEREOGRAPHIC");
    }

    this.ncfile.addAttribute(null, att);

    /*
    ** Get grid dimensions
    */

    byte[] b3 = new byte[3];
    bos.get(b2, 0, 2);
    nx = getInt( b2, 2);
    Integer ni = new Integer(nx);
    att = new Attribute( "NX", ni);
    this.ncfile.addAttribute(null, att);


    bos.get(b2, 0, 2);
    ny = getInt( b2, 2);
    ni = new Integer(ny);
    att = new Attribute( "NY", ni);
    this.ncfile.addAttribute(null, att);

    ProjectionImpl projection = null;
    double dxKm = 0.0, dyKm = 0.0, latin, lonProjectionOrigin ;

    switch( proj ) {

      case 1:                                                    /* Mercator */
        /*
        ** Get the latitude and longitude of first and last "grid" points
        */

        /* Latitude of first grid point */
        bos.get(b3, 0, 3);
        int nn = getInt( b3, 3);
        lat1 = (double) nn / 10000.0;
        Double nd = new Double(lat1);
        att = new Attribute( "Latitude0", nd);
        this.ncfile.addAttribute(null, att);

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        int b33 = (int)(b3[0] & (1<< 7));
      //  if( b33 == 128)      //west longitude
      //      nd = new Double(((double) nn) / 10000.0 * (-1));
      //  else
            nd = new Double(((double) nn) / 10000.0 );
        lon1 = nd.doubleValue();
        att = new Attribute( "Longitude0", nd);
        this.ncfile.addAttribute(null, att);

        /* Longitude of last grid point */
        bos.get(); /* skip one byte */

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        nd = new Double( ((double) nn) / 10000.0);
        lat2 = nd.doubleValue();
        att = new Attribute( "LatitudeN", nd);
        this.ncfile.addAttribute(null, att);

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        b33 = (int)(b3[0] & (1<< 7));
       // if( b33 == 128)      //west longitude
      //      nd = new Double(((double) nn) / 10000.0 * (-1));
      //  else
            nd = new Double(((double) nn) / 10000.0 );
        lon2 = nd.doubleValue();
        att = new Attribute( "LongitudeN", nd);
        this.ncfile.addAttribute(null, att);

        /*
        ** Hack to catch incorrect sign of lon2 in header.
        */

    //    if ( lon1 > 0.0 && lon2 < 0.0 ) lon2 *= -1;
        double lon_1 = lon1;
        double lon_2 = lon2;
        if ( lon1 < 0 ) lon_1 += 360.0;
        if ( lon2 < 0 ) lon_2 += 360.0;

        lonv = lon_1 - (lon_1 - lon_2) / 2.0;

        if ( lonv >  180.0 ) lonv -= 360.0;
        if ( lonv < -180.0 ) lonv += 360.0;
        /*
        ** Get the "Latin" parameter.  The ICD describes this value as:
        ** "Latin - The latitude(s) at which the Mercator projection cylinder
        ** intersects the earth."  It should read that this is the latitude
        ** at which the image resolution is that defined by octet 41.
        */
        bos.getInt(); /* skip 4 bytes */
        bos.get();    /* skip 1 byte */

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        nd = new Double(((double) nn) / 10000.0);

        /* Latitude of proj cylinder intersects */
        att = new Attribute( "LatitudeX", nd);
        latin = nd.doubleValue();
        this.ncfile.addAttribute(null, att);

        latt = 0.0; // this is not corrected  // jc 8/7/08 not used

       // dyKm =  Math.cos( DEG_TO_RAD*latt);
       // dxKm = DEG_TO_RAD * EARTH_RAD_KMETERS * Math.abs((lon_1-lon_2) / (nx-1));
      //  double dy  =  EARTH_RAD_KMETERS * Math.cos(DEG_TO_RAD*latt) / (ny - 1);
      //  dyKm = dy *( Math.log( Math.tan(DEG_TO_RAD*( (lat2-latt)/2.0 + 45.0 ) ) )
      //                  -Math.log( Math.tan(DEG_TO_RAD*( (lat1-latt)/2.0 + 45.0 ) ) ) );
      //  dxKm = DEG_TO_RAD * EARTH_RAD_KMETERS * Math.abs(lon1-lon2) / (ny-1);
        projection = new Mercator( lonv, latin) ;
        break;

      case 3:                               /* Lambert Conformal             */
      case 5:                               /* Polar Stereographic           */
        /*
        ** Get lat/lon of first grid point
        */
        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        nd = new Double(((double) nn) / 10000.0);
        lat1 = nd.doubleValue();
        //att = new Attribute( "Lat1", nd);
        //this.ncfile.addAttribute(null, att);
        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        nd = new Double(((double) nn) / 10000.0);
        lon1 = nd.doubleValue();
        /*
        ** Get Lov - the orientation of the grid; i.e. the east longitude of
        ** the meridian which is parallel to the y-aixs
        */
        bos.get(); /* skip one byte */

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        nd = new Double(((double) nn) / 10000.0);
        lonv = nd.doubleValue();
        lonProjectionOrigin = lonv;
        att = new Attribute( "Lov", nd);
        this.ncfile.addAttribute(null, att);

        /*
        ** Get distance increment of grid
        */
        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        dxKm = ((double) nn) / 10000.0;

        nd = new Double(((double) nn) / 10000.);
        att = new Attribute( "DxKm", nd);
        this.ncfile.addAttribute(null, att);

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        dyKm = ((double) nn) / 10000.0;

        nd = new Double( ((double) nn) / 10000.);
        att = new Attribute( "DyKm", nd);
        this.ncfile.addAttribute(null, att);
        /* calculate the lat2 and lon2 */

        if ( proj == 5 ) {
            latt = 60.0;            /* Fixed for polar stereographic */
            imageScale = (1. + Math.sin(DEG_TO_RAD*latt))/2.;
        }

        lat2 = lat1 + dyKm*(ny-1) / 111.26;


        /* Convert to east longitude */
        if ( lonv < 0. ) lonv += 360.;
        if ( lon1 < 0. ) lon1 += 360.;


        lon2 = lon1 + dxKm*(nx-1) / 111.26 * Math.cos(DEG_TO_RAD*lat1);

        diff_lon = lonv - lon1;

        if ( diff_lon >  180. )  diff_lon -= 360.;
        if ( diff_lon < -180. )  diff_lon += 360.;
        /*
        ** Convert to normal longitude to McIDAS convention
        */
        lonv = (lonv > 180.) ? -(360.-lonv) : lonv;
        lon1 = (lon1 > 180.) ? -(360.-lon1) : lon1;
        lon2 = (lon2 > 180.) ? -(360.-lon2) : lon2;
        /*
        ** Check high bit of octet for North or South projection center
        */
        nv= new Byte(bos.get());
        pole = nv.intValue();
        pole = ( pole > 127 ) ? -1 : 1;
        ni = new Integer(pole);
        att = new Attribute( "ProjCenter", ni);
        this.ncfile.addAttribute(null, att);

        bos.get(); /* skip one byte for Scanning mode */

        bos.get(b3, 0, 3);
        nn = getInt( b3, 3);
        latin = (((double) nn) / 10000.);

        nd = new Double(((double) nn) / 10000.);
        att = new Attribute( "Latin", nd);
        this.ncfile.addAttribute(null, att);

        if (proj == 3 )
          projection = new LambertConformal(latin, lonProjectionOrigin, latin, latin);
        else // (proj == 5)
          projection = new Stereographic(90.0, lonv, imageScale);

        break;

      default:
        System.out.println("unimplemented projection");


    }


    this.ncfile.addAttribute(null, new Attribute("title", gini_GetEntityID( ent_id )));
    this.ncfile.addAttribute(null, new Attribute("summary", getPhysElemSummary(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("id", gini_GetSectorID(sec_id)));
    this.ncfile.addAttribute(null, new Attribute("keywords_vocabulary", gini_GetPhysElemID(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.GRID.toString()));
    this.ncfile.addAttribute(null, new Attribute("standard_name_vocabulary", getPhysElemLongName(phys_elem, ent_id)));
    this.ncfile.addAttribute(null, new Attribute("creator_name", "UNIDATA"));
    this.ncfile.addAttribute(null, new Attribute("creator_url", "http://www.unidata.ucar.edu/"));
    this.ncfile.addAttribute(null, new Attribute("naming_authority", "UCAR/UOP"));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Float(lat1)));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Float(lat2)));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Float(lon1)));
    this.ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Float(lon2)));
    //this.ncfile.addAttribute(null, new Attribute("geospatial_vertical_min", new Float(0.0)));
    //this.ncfile.addAttribute(null, new Attribute("geospatial_vertical_max", new Float(0.0)));

    /** Get the image resolution.
    */

    bos.position(41);  /* jump to 42 bytes of PDB */
    nv = new Byte( ( bos.get() ) );      /* Res [km] */
    att = new Attribute( "imageResolution", nv);
    this.ncfile.addAttribute(null, att);
   // if(proj == 1)
   //     dyKm = nv.doubleValue()/dyKm;
    /* compression flag 43 byte */

    nv = new Byte( ( bos.get() ));      /* Res [km] */
    att = new Attribute( "compressionFlag", nv );
    this.ncfile.addAttribute(null, att);

    if ( convertunsignedByte2Short( nv.byteValue() ) == 128 )
    {
       Z_type = 2;
       //out.println( "ReadNexrInfo:: This is a Z file ");
    }

   /* new 47 - 60 */
    bos.position(46);
    nv = new Byte( ( bos.get() ) );      /* Cal indicator */
    int navcal = convertunsignedByte2Short(nv);
    int [] calcods = null;
    if(navcal == 128)
        calcods = getCalibrationInfo(bos, phys_elem, ent_id);

    // only one data variable per gini file

    String vname= gini_GetPhysElemID(phys_elem, ent_id);
    Variable var = new Variable( ncfile, ncfile.getRootGroup(), null, vname);
    var.addAttribute( new Attribute("long_name", getPhysElemLongName(phys_elem, ent_id)));
    var.addAttribute( new Attribute("units", getPhysElemUnits(phys_elem, ent_id)));
    // var.addAttribute( new Attribute("missing_value", new Byte((byte) 0))); // ??

      // get dimensions
    int velems;
    boolean isRecord = false;
    ArrayList dims = new ArrayList();

    Dimension dimX  = new Dimension( "x", nx, true, false, false);
    Dimension dimY  = new Dimension( "y", ny, true, false, false);

    ncfile.addDimension( null, dimY);
    ncfile.addDimension( null, dimX);

    velems = dimX.getLength() * dimY.getLength();
    dims.add( dimT);
    dims.add( dimY);
    dims.add( dimX);

    var.setDimensions( dims);

    // size and beginning data position in file
    int vsize = velems;
    long begin = dataStart ;
    if (debug) log.warn(" name= "+vname+" vsize="+vsize+" velems="+velems+" begin= "+begin+" isRecord="+isRecord+"\n");
    if( navcal == 128) {
        var.setDataType( DataType.FLOAT);
        var.setSPobject( new Vinfo (vsize, begin, isRecord, nx, ny, calcods));
     /*   var.addAttribute(new Attribute("_Unsigned", "true"));
        int numer = calcods[0] - calcods[1];
        int denom = calcods[2] - calcods[3];
        float a  = (numer*1.f) / (1.f*denom);
        float b  = calcods[0] - a * calcods[2];
        var.addAttribute( new Attribute("scale_factor", new Float(a)));
        var.addAttribute( new Attribute("add_offset", new Float(b)));
      */
    }
    else  {
        var.setDataType( DataType.BYTE);
        var.addAttribute(new Attribute("_Unsigned", "true"));
        var.addAttribute(new Attribute("_missing_value", new Short((short)255)));
        var.addAttribute( new Attribute("scale_factor", new Short((short)(1))));
        var.addAttribute( new Attribute("add_offset", new Short((short)(0))));
        var.setSPobject( new Vinfo (vsize, begin, isRecord, nx, ny));
    }
    String coordinates = "x y time";
    var.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
    ncfile.addVariable(null, var);

    // add coordinate information. we need:
    // nx, ny, dx, dy,
    // latin, lov, la1, lo1

    // we have to project in order to find the origin
    ProjectionPointImpl start = (ProjectionPointImpl) projection.latLonToProj( new LatLonPointImpl( lat1, lon1));
    if (debug) log.warn("start at proj coord "+start);

    double startx = start.getX();
    double starty = start.getY();

    // create coordinate variables
    Variable xaxis = new Variable( ncfile, null, null, "x");
    xaxis.setDataType( DataType.DOUBLE);
    xaxis.setDimensions( "x");
    xaxis.addAttribute( new Attribute("long_name", "projection x coordinate"));
    xaxis.addAttribute( new Attribute("units", "km"));
    xaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoX"));
    double[] data = new double[nx];
    if( proj == 1 ) {
        double lon_1 = lon1;
        double lon_2 = lon2;
        if ( lon1 < 0 ) lon_1 += 360.0;
        if ( lon2 < 0 ) lon_2 += 360.0;
        double dx = (lon_2 - lon_1) /(nx-1);

        for (int i = 0; i < data.length; i++) {
          double ln = lon1 + i * dx;
          ProjectionPointImpl pt = (ProjectionPointImpl) projection.latLonToProj( new LatLonPointImpl( lat1, ln));
          data[i] = pt.getX();  // startx + i*dx;
        }
    } else {
        for (int i = 0; i < data.length; i++)
          data[i] = startx + i*dxKm;
    }

    dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {nx}, data);
    xaxis.setCachedData( dataA, false);
    ncfile.addVariable(null, xaxis);

    Variable yaxis = new Variable( ncfile, null, null, "y");
    yaxis.setDataType( DataType.DOUBLE);
    yaxis.setDimensions( "y");
    yaxis.addAttribute( new Attribute("long_name", "projection y coordinate"));
    yaxis.addAttribute( new Attribute("units", "km"));
    yaxis.addAttribute( new Attribute(_Coordinate.AxisType, "GeoY"));
    data = new double[ny];
    double endy = starty + dyKm * (data.length - 1); // apparently lat1,lon1 is always the lower ledt, but data is upper left
    if(proj == 1) {
        double dy = (lat2 - lat1 ) / (ny-1);
        for (int i = 0; i < data.length; i++) {
          double la = lat2 - i*dy;
          ProjectionPointImpl pt = (ProjectionPointImpl) projection.latLonToProj( new LatLonPointImpl( la, lon1));
          data[i] = pt.getY();  //endyy - i*dy;
        }
    }
    else {
        for (int i = 0; i < data.length; i++)
          data[i] = endy - i*dyKm;
    }
    dataA = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[] {ny}, data);
    yaxis.setCachedData( dataA, false);
    ncfile.addVariable(null, yaxis);

    // coordinate transform variable
    Variable ct = new Variable( ncfile, null, null, projection.getClassName());
    ct.setDataType( DataType.CHAR);
    ct.setDimensions( "");
    List params = projection.getProjectionParameters();
    for (int i = 0; i < params.size(); i++) {
      Parameter p = (Parameter) params.get(i);
      ct.addAttribute( new Attribute(p));
    }
    ct.addAttribute( new Attribute(_Coordinate.TransformType, "Projection"));
    ct.addAttribute( new Attribute(_Coordinate.Axes, "x y "));
    // fake data
    dataA = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {});
    dataA.setChar(dataA.getIndex(), ' ');
    ct.setCachedData(dataA, false);

    ncfile.addVariable(null, ct);
    ncfile.addAttribute( null, new Attribute("Conventions", _Coordinate.Convention));

    // finish
    ncfile.finish();
  }

  int [] getCalibrationInfo(ByteBuffer bos, int phys_elem, int ent_id){

    bos.position(46);
    byte nv = new Byte( ( bos.get() ) );      /* Cal indicator */
    int navcal = convertunsignedByte2Short(nv);
    int [] calcods = null;
    if( navcal == 128 ) {    /* Unidata Cal block found; unpack values */
        int      scale=10000;
        int      jscale=100000000;
        byte  [] unsb = new byte[8];
        byte[] b4 = new byte[4];
        bos.get(unsb);
        String unitStr = new String(unsb).toUpperCase();
        String iname;
        String iunit;
        bos.position(55);
        nv = new Byte( ( bos.get() ) );
        int calcod = convertunsignedByte2Short(nv);

        if ( unitStr.contains("INCH") ) {
            iname = new String( "RAIN" );
            iunit = new String( "IN  ");

        } else if ( unitStr.contains("dBz" ) ) {
            iname = new String( "ECHO" );
            iunit = new String( "dBz " );

        } else if (unitStr.contains("KFT" ) ) {

            iname = new String( "TOPS");
            iunit = new String( "KFT ");

        } else if ( unitStr.contains("KG/M" ) ) {

            iname = new String( "VIL " );
            iunit = new String( "mm  " );

        } else {

            iname = new String( "    " );
            iunit = new String( "    " );
        }

        if ( calcod > 0 ) {
            calcods = new int[5*calcod + 1];
            calcods[0] = calcod;
            for (int i = 0; i < calcod; i++ ) {

                bos.position(56+i*16);
                bos.get(b4);
                int minb = getInt(b4, 4) / 10000;        /* min brightness values         */
                bos.get(b4);
                int maxb = getInt(b4, 4 ) / 10000;       /* max brightness values         */
                bos.get(b4);
                int mind = getInt(b4, 4 );               /* min data values               */
                bos.get(b4);
                int maxd = getInt(b4, 4 );               /* max data values               */

                int idscal = 1;
                while ( !(mind % idscal != 0) && !(maxd % idscal != 0) ) {
                    idscal *= 10;
                }
                idscal /= 10;

                if ( idscal < jscale ) jscale = idscal;

                calcods[1+i*5] = mind;
                calcods[2+i*5] = maxd;
                calcods[3+i*5] = minb;
                calcods[4+i*5] = maxb;
                calcods[5+i*5] = 0;

            }

           if ( jscale > scale ) jscale = scale;
                scale /= jscale;

            if ( gini_GetPhysElemID(phys_elem, ent_id).contains("Precipitation") ) {
                if ( scale < 100 ) {
                    jscale /= (100/scale);
                    scale   = 100;
                }
            }

            for (int i = 0; i < calcod; i++ ) {
                calcods[1+i*5] /= jscale;
                calcods[2+i*5] /= jscale;
                calcods[5+i*5]  = scale;
            }

      }

    }

    return calcods;
      
  }


  int gini_GetCompressType( )
  {
       return Z_type;
  }

  // Return the string of entity ID for the GINI image file

  String gini_GetSectorID(int ent_id )
  {
    String name;
     /* GINI channel ID          */

    switch( ent_id ) {
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
        name = "Alaska Regional" ;
        break;
      case 4:
        name = "Alaska National" ;
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
      default:
        name = "Unknown-ID";
	}

   return name;


  }

  // Return the channel ID for the GINI image file
  String gini_GetEntityID(int ent_id )
  {
    String name;
      switch ( ent_id ) {
           case 99:
             name = "RADAR-MOSIAC Composite Image";
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
           case 9:                                    /* METEOSAT (using 6)       */
             name = "METEOSAT satellite Image" ;
             break;
           case 10:                                   /* GOES-7                   */
             name = "GOES-7 satellite Image";
             break;
           case 11:                                   /* GOES-8                   */
             name = "GOES-8 satellite Image";
             break;
           case 12:                                   /* GOES-9                   */
             name = "GOES-9 satellite Image";
             break;
           case 13:                                   /* GOES-10                  */
             name = "GOES-10 satellite Image" ;
             break;
           case 14:                                   /* GOES-11                  */
             name = "GOES-11 satellite Image" ;
             break;
           case 15:                                   /* GOES-12                  */
             name = "GOES-12 satellite Image";
             break;
           case 16:                                   /* GOES-13                  */
             name = "GOES-13 satellite Image";
             break;
           default:
             name = "Unknown";
         }

   return name;


  }
  // Return the channel ID for the GINI image file
  String gini_GetPhysElemID(int phys_elem, int ent_id )
  {
    String name;

    switch( phys_elem ) {
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
        name = "IR" ;
        break;
      case 13:
        name = "LI" ;
        break;
      case 14:
        name = "PW" ;
        break;
      case 15:
        name = "SFC_T" ;
        break;
      case 16:
        name = "LI" ;
        break;
      case 17:
        name = "PW" ;
        break;
      case 18:
        name = "SFC_T" ;
        break;
      case 19:
        name = "CAPE" ;
        break;
      case 20:
        name = "T" ;
        break;
      case 21:
        name = "WINDEX" ;
        break;
      case 22:
        name = "DMPI" ;
        break;
      case 23:
        name = "MDPI" ;
        break;
      case 25:
        if( ent_id == 99)
            name = "Reflectivity";
        else
            name = "Volcano_imagery";
        break;
      case 27:
        if(ent_id == 99 )
            name = "Reflectivity";
        else
            name = "CTP";
        break;
      case 28:
        if(ent_id == 99 )
            name = "Reflectivity";
        else
            name = "Cloud_Amount";
        break;
      case 26:
        name = "EchoTops";
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
  String getPhysElemUnits(int phys_elem, int ent_id ) {
    switch( phys_elem ) {
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
      case 29:
      case 43:
      case 48:
      case 50:
      case 51:
      case 52:
      case 55:
      case 57:
      case 59: return  "N/A";
      case 26: return  "K FT";
      case 25:
        if( ent_id == 99)
            return "dBz";
        else
            return  "N/A";
      case 27:
        if( ent_id == 99)
            return "dBz";
        else
          return  "N/A";
      case 28:
        if( ent_id == 99)
            return "dBz";
        else
          return  "N/A";
      case 30: return "IN";
      case 31: return "IN";
      default: return "Unknown";
    }
  }

  // Return the channel ID for the GINI image file

  String getPhysElemLongName(int phys_elem, int ent_id ) {
     switch( phys_elem ) {
      case 1:
        return "Imager Visible";
      case 2:
        return "Imager 3.9 micron IR";
      case 3:
        return "Imager 6.7/6.5 micron IR (WV)";
      case 4:
        return "Imager 11 micron IR" ;
      case 5:
        return "Imager 12 micron IR" ;
      case 6:
        return "Imager 13 micron IR";
      case 7:
        return "Imager 1.3 micron IR";
      case 13:
        return  "Lifted Index LI" ;
      case 14:
        return  "Precipitable Water PW" ;
      case 15:
        return  "Surface Skin Temperature";
      case 16:
        return  "Lifted Index LI" ;
      case 17:
        return  "Precipitable Water PW" ;
      case 18:
        return  "Surface Skin Temperature" ;
      case 19:
        return  "Convective Available Potential Energy" ;
      case 20:
        return  "land-sea Temperature" ;
      case 21:
        return  "Wind Index" ;
      case 22:
        return  "Dry Microburst Potential Index" ;
      case 23:
        return  "Microburst Potential Index" ;
      case 25:
        if( ent_id == 99)
            return "2 km National 248 nm Base Composite Reflectivity";
        else
            return "Volcano_imagery";
      case 26:
        return "4 km National Echo Tops";
      case 27:
        if( ent_id == 99)
            return "1 km National Base Reflectivity Composite (Unidata)";
        else
            return "Cloud Top Pressure or Height";
      case 28:
        if( ent_id == 99)
            return "1 km National Composite Reflectivity (Unidata)";
        else
            return "Cloud Amount";

      case 29:
        return "4 km National Vertically Integrated Liquid Water";
      case 30:
        return "2 km National 1-hour Precipitation (Unidata)";
      case 31:
        return "4 km National Storm Total Precipitation (Unidata)";
      case 43:
        return "14.06 micron sounder image";
      case 48:
        return "11.03 micron sounder image";
      case 50:
        return "7.43 micron sounder image";
      case 51:
        return "7.02 micron sounder image" ;
      case 52:
        return "6.51 micron sounder image" ;
      case 55:
        return "4.45 micron sounder image";
      case 57:
        return "3.98 micron sounder image";
      case 59:
        return  "VIS sounder image ";
      default:
       return "unknown physical element "+ phys_elem;
    }
  }

  String getPhysElemSummary(int phys_elem, int ent_id ) {
     switch( phys_elem ) {
      case 1:
        return "Satellite Product Imager Visible";
      case 2:
        return "Satellite Product Imager 3.9 micron IR";
      case 3:
        return "Satellite Product Imager 6.7/6.5 micron IR (WV)";
      case 4:
        return "Satellite Product Imager 11 micron IR" ;
      case 5:
        return "Satellite Product Imager 12 micron IR" ;
      case 6:
        return "Satellite Product Imager 13 micron IR";
      case 7:
        return "Satellite Product Imager 1.3 micron IR";
      case 13:
        return  "Imager Based Derived Lifted Index LI" ;
      case 14:
        return  "Imager Based Derived Precipitable Water PW" ;
      case 15:
        return  "Imager Based Derived Surface Skin Temperature" ;
      case 16:
        return  "Sounder Based Derived Lifted Index LI" ;
      case 17:
        return  "Sounder Based Derived Precipitable Water PW" ;
      case 18:
        return  "Sounder Based Derived Surface Skin Temperature" ;
      case 19:
        return  "Derived Convective Available Potential Energy CAPE" ;
      case 20:
        return  "Derived land-sea Temperature" ;
      case 21:
        return  "Derived Wind Index WINDEX" ;
      case 22:
        return  "Derived Dry Microburst Potential Index DMPI" ;
      case 23:
        return  "Derived Microburst Day Potential Index MDPI" ;
      case 43:
        return "Satellite Product 14.06 micron sounder image";
      case 48:
        return "Satellite Product 11.03 micron sounder image";
      case 50:
        return "Satellite Product 7.43 micron sounder image";
      case 51:
        return "Satellite Product 7.02 micron sounder image" ;
      case 52:
        return "Satellite Product 6.51 micron sounder image" ;
      case 55:
        return "Satellite Product 4.45 micron sounder image";
      case 57:
        return "Satellite Product 3.98 micron sounder image";
      case 59:
        return "Satellite Product VIS sounder visible image ";
      case 25:
        if( ent_id == 99)
            return "Nexrad Level 3 National 248 nm Base Composite Reflectivity at Resolution 2 km";
        else
            return "Satellite Derived Volcano_imagery";

      case 26:
          return "Nexrad Level 3 National Echo Tops at Resolution 4 km";
      case 29: return "Nexrad Level 3 National Vertically Integrated Liquid Water at Resolution 4 km";
      case 28:
         if( ent_id == 99)
            return "Nexrad Level 3 National 248 nm Base Composite Reflectivity at Resolution 2 km";
         else
            return "Gridded Cloud Amount";
      case 27:
          if( ent_id == 99)
            return "Nexrad Level 3 Base Reflectivity National Composition at Resolution 1 km";
        else
            return "Gridded Cloud Top Pressure or Height";

      case 30: return "Nexrad Level 3 1 hour precipitation National Composition at Resolution 2 km";
      case 31: return "Nexrad Level 3 total precipitation National Composition at Resolution 4 km";
      default: return "unknown";
    }

  }


/*
** Name:       GetInt
**
** Purpose:    Convert GINI 2 or 3-byte quantities to int
**
*/
  int getInt( byte[] b, int num )
  {
      int            base=1;
      int            i;
      int            word=0;

      int bv[] = new int[num];

      for (i = 0; i<num; i++ )
      {
        bv[i] = convertunsignedByte2Short(b[i]);
      }

      if( bv[0] > 127 )
      {
         bv[0] -= 128;
         base = -1;
      }
      /*
      ** Calculate the integer value of the byte sequence
      */

      for ( i = num-1; i >= 0; i-- ) {
        word += base * bv[i];
        base *= 256;
      }

      return word;

  }

  public short convertunsignedByte2Short(byte b)
  {
     return (short)((b<0)? (short)b + 256 : (short)b);
  }


// this converts a byte array to a wrapped primitive (Byte, Short, Integer, Double, Float, Long)
  protected Object convert( byte[] barray, DataType dataType, int byteOrder) {

    if (dataType == DataType.BYTE) {
      return new Byte( barray[0]);
    }

    if (dataType == DataType.CHAR) {
      return new Character((char) barray[0]);
    }

    ByteBuffer bbuff = ByteBuffer.wrap( barray);
    if (byteOrder >= 0)
      bbuff.order( byteOrder == ucar.unidata.io.RandomAccessFile.LITTLE_ENDIAN? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

    if (dataType == DataType.SHORT) {
      ShortBuffer tbuff = bbuff.asShortBuffer();
      return new Short(tbuff.get());

    } else if (dataType == DataType.INT) {
      IntBuffer tbuff = bbuff.asIntBuffer();
      return new Integer(tbuff.get());

    } else if (dataType == DataType.LONG) {
      LongBuffer tbuff = bbuff.asLongBuffer();
      return new Long(tbuff.get());

    } else if (dataType == DataType.FLOAT) {
      FloatBuffer tbuff = bbuff.asFloatBuffer();
      return new Float(tbuff.get());

    } else if (dataType == DataType.DOUBLE) {
      DoubleBuffer tbuff = bbuff.asDoubleBuffer();
      return new Double(tbuff.get());
    }

    throw new IllegalStateException();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  // variable info for reading/writing
  class Vinfo {
    int vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    int nx;
    int ny;
    int [] levels;

    Vinfo( int vsize, long begin, boolean isRecord, int x, int y) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.nx = x;
      this.ny = y;
    }

    Vinfo( int vsize, long begin, boolean isRecord, int x, int y, int[] levels) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.nx = x;
      this.ny = y;
      this.levels = levels;
    }
     
  }


  int isZlibHed( byte[] buf ){
    short b0 = convertunsignedByte2Short(buf[0]);
    short b1 = convertunsignedByte2Short(buf[1]);

    if ( (b0 & 0xf) == Z_DEFLATED ) {
      if ( (b0 >> 4) + 8 <= DEF_WBITS ) {
        if ( (((b0 << 8) + b1) % 31)==0 ) {
          return 1;
        }
      }
    }

    return 0;

  }

}
/* Change History:
   $Log: Giniheader.java,v $
   Revision 1.15  2005/10/11 18:44:11  yuanho
   change missing value attribute to variable

   Revision 1.14  2005/10/11 18:09:06  yuanho
   adding missing value attribute

   Revision 1.13  2005/10/11 15:25:05  yuanho
   variable naming changes for satellite data

   Revision 1.12  2005/10/10 21:02:56  yuanho
   adding changes for satellite data

   Revision 1.11  2005/07/28 21:46:36  yuanho
   remove the static from Vinfo class

   Revision 1.10  2005/07/12 23:00:58  yuanho
   remove static, add global attr

   Revision 1.9  2005/05/11 00:10:03  caron
   refactor StuctureData, dt.point

   Revision 1.8  2004/12/15 22:35:25  caron
   add _unsigned

   Revision 1.7  2004/12/07 22:13:28  yuanho
   add phyElem for 1hour and total precipitation

   Revision 1.6  2004/12/07 22:13:15  yuanho
   add phyElem for 1hour and total precipitation

   Revision 1.5  2004/12/07 01:29:31  caron
   redo convention parsing, use _Coordinate encoding.

   Revision 1.4  2004/10/29 00:14:11  caron
   no message

   Revision 1.3  2004/10/19 15:17:22  yuanho
   gini header DxKm update

   Revision 1.2  2004/10/15 23:18:34  yuanho
   gini projection update

   Revision 1.1  2004/10/13 22:57:57  yuanho
   no message

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */