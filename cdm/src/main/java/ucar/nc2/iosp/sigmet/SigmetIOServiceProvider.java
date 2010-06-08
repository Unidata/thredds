package ucar.nc2.iosp.sigmet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ucar.ma2.Range;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.Layout;
import ucar.nc2.iosp.LayoutRegular;
import ucar.unidata.io.RandomAccessFile;

/** Implementation of the ServerProvider pattern provides input/output
 * of the SIGMET-IRIS dataset. IOSP are managed by the NetcdfFile class.
 * When the SigmetDataset is requested by calling NetcdfFile.open(), the file
 * is opened as a ucar.unidata.io.RandomAccessFile.
 * The SIGMET-IRIS data format are described in "IRIS Programmer's Manual" ch.3
 * The SIGMET-IRIS file consists of records with fixed length=6144 bytes. Data is written in
 * little-endian format. The organization of raw product SIGMET-IRIS file is:
 *      Record #1   { <product_hdr> 0,0,0,...}
 *      Record #2   { <ingest_header> 0,0,0,... }
 *       ---if there are several sweeps (usually 24) and one type of data:
 *      Record #3   { <raw_prod_bhdr><ingest_data_header>...Data for sweep 1.. }
 *      Record #4   { <raw_prod_bhdr>...Data for sweep 1...  }
 *             .............................................
 *      Record #n   { <raw_prod_bhdr>...Data for sweep 1... 0.... }
 *      Record #n+1 { <raw_prod_bhdr><ingest_data_header>...Data for sweep 2.. }
 *      Record #n+2 { <raw_prod_bhdr>...Data for sweep 2...  }
 *             .............................................
 *      Record #m   { <raw_prod_bhdr>...Data for sweep 2... 0...  }
 *      Record #m+1 { <raw_prod_bhdr><ingest_data_header>...Data for sweep 3.. }
 *             .............................................
 *             Structure of "Data for sweep" is:  <ray_header><ray_data>...<ray_header><ray_data>...
 *             <ray_header> and <ray_data> are encoded with the compression algorithm
 *             ("IRIS Programmer's Manual" 3.5.4.1)
 *       ---if there are "n" types of data (usually 4 or 5) and one sweep:
 *      Record #3   { <raw_prod_bhdr><ingest_data_header(data_type_1)><ingest_data_header(data_type_2)>...
 *                    <ingest_data_header(data_type_n)>...Data...}
 *      Record #4   { <raw_prod_bhdr>...Data...  }
 *             .............................................
 *      Record #n   { <raw_prod_bhdr>...Data...  }
 *             Structure of "Data" is:  <ray_header(data_type_1)><ray_data(data_type_1)><ray_header(data_type_2)><ray_data(data_type_2)>...
 *             <ray_header(data_type_n)><ray_data(data_type_n)><ray_header(data_type_1)><ray_data(data_type_1)>
 *         <ray_header(data_type_2)><ray_data(data_type_2)>... <ray_header(data_type_n)><ray_data(data_type_n)>...
 *             <ray_header> and <ray_data> are encoded with the compression algorithm
 *             ("IRIS Programmer's Manual" 3.5.4.1)
 *
 */
public class SigmetIOServiceProvider  extends AbstractIOServiceProvider {
    private static final String def_datafile="SIGMET-IRIS";
    // org.slf4j.Logger logger=org.slf4j.LoggerFactory.getLogger(TestNetcdf.class);
    // ucar.unidata.io.RandomAccessFile raf=null;
    //  static String infile="C:\\netcdf\\data\\sig00.dat";
    // static String infile="testdata2\\200705162030~~CONVOL-URP-XNC-RADAR-IRIS--20070516203210";
    // static String infile="testdata2\\200705162030~~DOPVOL1_A-URP-XNC-RADAR-IRIS--20070516203241";
    private ucar.nc2.NetcdfFile ncfile = null;
    ArrayList<Variable> varList = null;
    RandomAccessFile myRaf = null;
   //Ray[][] ray=null;
    int[] tsu_sec=null;
    int[] sweep_bins=null;
    String date0;
   
    public static java.util.Map<String, Number> recHdr = new java.util.HashMap<String, Number> ();
    private SigmetVolumeScan volScan;
    public static void main(String[] args) {
        String infile=" ";
        if (args.length == 1) {
            infile=args[0];
        } else { System.out.println("Usage: java SigmetIOServiceProvider inputFile");
        System.exit(0);  }
        try {
            NetcdfFile.registerIOProvider(SigmetIOServiceProvider.class);
            NetcdfFile ncfile=NetcdfFile.open(infile);
            System.out.println("ncfile = \n"+ncfile);
        } catch (Exception e) { System.out.println("MAIN!!!   "+e.toString());
        e.printStackTrace(); }
    }
    public String getFileTypeDescription() {
        return "SIGMET-IRIS";
    }
    public String getFileTypeVersion() {
        return "SIGMET-IRIS";
    }
    public String getFileTypeId() {
        return "SIGMET";
    }

    /** Check if this is a valid SIGMET-IRIS file for this IOServiceProvider. */
    public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
        try {
            raf.order(RandomAccessFile.LITTLE_ENDIAN);
            raf.seek(24);
            return (raf.readShort() == (short)15);
        } catch (IOException ioe) {  System.out.println("In isValidFile(): "+ioe.toString());
        return false;
        }
    }

    /** Open existing file, and populate ncfile with it.  */
    public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
            ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException  {
        this.ncfile = ncfile;
        this.myRaf = raf;
        //java.util.Map<String, Number> recHdr=new java.util.HashMap<String, Number>();
        java.util.Map<String, String> hdrNames=new java.util.HashMap<String, String>();
        volScan = new SigmetVolumeScan(raf, ncfile, varList);
        this.varList=init(raf, ncfile, hdrNames);

       // doData(raf, ncfile, varList);
       // raf.close();
       // this.ncfile.close();
    }

    /** Read some global data from SIGMET file. The SIGMET file consists of records with
     *  fixed length=6144 bytes.
     */
   static  public java.util.Map<String, Number> readRecordsHdr(ucar.unidata.io.RandomAccessFile raf) {
        java.util.Map<String, Number> recHdr1 = new java.util.HashMap<String, Number> ();
        try { int nparams=0;
        //      -- Read from <product_end> of the 1st record -- 12+320+120
        //      -- Calculate Nyquist velocity --------------------
        raf.seek(452);  int prf=raf.readInt();
        raf.seek(480);  int wave=raf.readInt();
        float vNyq=calcNyquist(prf, wave);
        recHdr1.put("vNyq", new Float(vNyq));

        //      -- Read from the 2nd record----------- 6144+12(strucr_hdr)+168(from ingest_config)
        raf.seek(6324);  int radar_lat=raf.readInt();
        int radar_lon=raf.readInt();           //6328
        short ground_height= raf.readShort();  //6332
        short radar_height= raf.readShort();   //6334
        raf.skipBytes(4);
        short num_rays= raf.readShort();      // 6340
        raf.skipBytes(2);
        int radar_alt=raf.readInt();          //6344
        raf.seek(6648);
        int time_beg=raf.readInt();
        raf.seek(6652);
        int time_end=raf.readInt();
        raf.seek(6772);
        int data_mask=raf.readInt();
        for (int j=0; j<32; j++) {  nparams+=((data_mask >> j) & (0x1));  }
        raf.seek(6912);
        short multiprf=raf.readShort();
        raf.seek(7408);
        int range_first=raf.readInt();   //  cm    7408
        int range_last=raf.readInt();    //  cm  7412
        raf.skipBytes(2);
        short bins=raf.readShort();             //7418
        if (bins % 2 != 0) bins=(short)(bins+1);
        raf.skipBytes(4);
        int step=raf.readInt();          //  cm    7424
        raf.seek(7574);
        short number_sweeps= raf.readShort();    // 7574
        raf.seek(12312);
        int base_time=raf.readInt();        //<ingest_data_header> 3d rec
        raf.skipBytes(2);
        short year=raf.readShort();
        short month=raf.readShort();
        short day=raf.readShort();
        recHdr1.put("radar_lat", new Float(calcAngle(radar_lat)));
        recHdr1.put("radar_lon", new Float(calcAngle(radar_lon)));
        recHdr1.put("range_first", new Integer(range_first));
        recHdr1.put("range_last", new Integer(range_last));
        recHdr1.put("ground_height", new Short(ground_height));
        recHdr1.put("radar_height", new Short(radar_height));
        recHdr1.put("radar_alt", new Integer(radar_alt));
        recHdr1.put("step", new Integer(step));
        recHdr1.put("bins", new Short(bins));    //System.out.println("  bins="+bins);
        recHdr1.put("num_rays", new Short(num_rays)); //System.out.println("  rays="+num_rays);
        recHdr1.put("nparams", new Integer(nparams));//System.out.println("  nparams="+nparams);
        recHdr1.put("multiprf", new Short(multiprf));
        recHdr1.put("number_sweeps", new Short(number_sweeps));   //System.out.println("IN HDR:  number_sweeps="+number_sweeps);
        recHdr1.put("year", new Short(year));
        recHdr1.put("month", new Short(month));
        recHdr1.put("day", new Short(day));
        recHdr1.put("base_time", new Integer(base_time));
        } catch (Exception e) { System.out.println(e.toString());
        e.printStackTrace(); }
        return recHdr1;
    }

    /** Read StationName strings  */
    public java.util.Map<String, String> readStnNames(ucar.unidata.io.RandomAccessFile raf) {
        java.util.Map<String, String> hdrNames=new java.util.HashMap<String, String> ();
        try {
            raf.seek(6288);
            String stnName=raf.readString(16);  //System.out.println(" stnName="+stnName.trim());
            raf.seek(6306);
            String stnName_util=raf.readString(16);
            hdrNames.put("StationName", stnName.trim());
            hdrNames.put("StationName_SetupUtility", stnName_util.trim());
        } catch (Exception e) { System.out.println(e.toString());
        e.printStackTrace(); }
        return hdrNames;
    }

    /** Define Dimensions, Variables, Attributes in ncfile
     * @param raf      ucar.unidata.io.RandomAccessFile corresponds of SIGMET datafile.
     * @param ncfile   an empty NetcdfFile object which will be filled.
     * @param hdrNames java.util.Map with values for "StationName.." Attributes
     * @return ArrayList of Variables of ncfile
     */
    public ArrayList<Variable> init(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
            java.util.Map<String, String> hdrNames)   throws java.io.IOException  {
        // prepare attribute values
        String[] data_name={ " ", "TotalPower", "Reflectivity", "Velocity",
                "Width", "Differential_Reflectivity"};
        String[] unit={" ", "dbZ", "dbZ", "m/sec", "m/sec", "dB"};
        int[] type={1, 2, 3, 4, 5};
        String def_datafile="SIGMET-IRIS";
        String tim="";
        int ngates=0;

        recHdr=readRecordsHdr(raf);
        hdrNames=readStnNames(raf);

        String stnName=hdrNames.get("StationName");
        String stnName_util=hdrNames.get("StationName_SetupUtility");
        float radar_lat=((Float)recHdr.get("radar_lat")).floatValue(); //System.out.println("rad_lat="+radar_lat);
        float radar_lon=((Float)recHdr.get("radar_lon")).floatValue(); //System.out.println("rad_lon="+radar_lon);
        short ground_height=((Short)recHdr.get("ground_height")).shortValue(); //System.out.println("ground_H="+ground_height);
        short radar_height=((Short)recHdr.get("radar_height")).shortValue(); //System.out.println("radar_H="+radar_height);
        int radar_alt=(((Integer)recHdr.get("radar_alt")).intValue())/100; //System.out.println("rad_alt="+radar_alt);
        short num_rays=((Short)recHdr.get("num_rays")).shortValue(); //System.out.println("num_rays="+num_rays);
        short bins=((Short)recHdr.get("bins")).shortValue(); //System.out.println("bins="+bins);
        float range_first=(((Integer)recHdr.get("range_first")).intValue())*0.01f; //System.out.println("range_1st="+range_first);
        float range_last=(((Integer)recHdr.get("range_last")).intValue())*0.01f; //System.out.println("step="+step);
        short number_sweeps=((Short)recHdr.get("number_sweeps")).shortValue();
        //System.out.println("number_sweeps="+number_sweeps);
        int nparams=(((Integer)recHdr.get("nparams")).intValue()); //System.out.println("nparams="+nparams);
        short year=((Short)recHdr.get("year")).shortValue(); //System.out.println("year="+year);
        short month=((Short)recHdr.get("month")).shortValue();
        short day=((Short)recHdr.get("day")).shortValue();
        int base_time=(((Integer)recHdr.get("base_time")).intValue());

        // define number of gates for every sweep
        sweep_bins=new int[nparams*number_sweeps];
        if (number_sweeps > 1) { sweep_bins=volScan.getNumberGates();
        } else {
            for (int kk=0; kk < nparams; kk++) {  sweep_bins[kk]=bins;  }
        }

        // add Dimensions
        Dimension scanR=new Dimension("scanR", number_sweeps, true);
        Dimension radial=new Dimension("radial", num_rays, true);
        Dimension[] gateR=new Dimension[number_sweeps];
        String dim_name="gateR";
        for (int j=0; j<number_sweeps; j++) {
            if (number_sweeps > 1) { dim_name="gateR_sweep_"+(j+1); }
            gateR[j]=new Dimension(dim_name, sweep_bins[j], true);
        }
        ncfile.addDimension(null, scanR);
        ncfile.addDimension(null, radial);
        for (int j=0; j<number_sweeps; j++) {
            ncfile.addDimension(null, gateR[j]);
        }
        ArrayList<Dimension> dims0=new ArrayList<Dimension>();
        ArrayList<Dimension> dims1=new ArrayList<Dimension>();
        ArrayList<Dimension> dims2=new ArrayList<Dimension>();
        ArrayList<Dimension> dims3=new ArrayList<Dimension>();

        ArrayList<Variable> varList=new ArrayList<Variable>();

        Variable[][] v=new Variable[nparams][number_sweeps];
        String var_name="";
        for (int j=0; j<nparams; j++) {
            int tp=type[j];
            var_name=data_name[tp];
            for (int jj=0; jj<number_sweeps; jj++) {
                if (number_sweeps > 1) {  var_name=data_name[tp]+"_sweep_"+(jj+1); }
                v[j][jj]=new Variable(ncfile, null, null, var_name);
                v[j][jj].setDataType(DataType.FLOAT);
                dims2.add(radial);
                dims2.add(gateR[jj]);
                v[j][jj].setDimensions(dims2);
                v[j][jj].addAttribute(new Attribute( "long_name", var_name));
                v[j][jj].addAttribute(new Attribute( "units", unit[tp]));
                String coordinates="time elevationR azimuthR distanceR";
                v[j][jj].addAttribute(new Attribute(_Coordinate.Axes, coordinates));
                v[j][jj].addAttribute(new Attribute("missing_value", -999.99f));
                ncfile.addVariable(null, v[j][jj]);
                varList.add(v[j][jj]);
                dims2.clear();
            }
        }
        tsu_sec=new int[number_sweeps];
        String[] tsu=new String[number_sweeps];
        String[] time_units=new String[number_sweeps];
        tsu_sec=volScan.getStartSweep();
        for (int i=0; i< number_sweeps; i++) {
            String st1=Short.toString(month);
            if (st1.length() < 2) st1="0"+st1;
            String st2=Short.toString(day);
            if (st2.length() < 2) st2="0"+st2;
            date0=String.valueOf(year)+"-"+st1+"-"+st2;
            tsu[i]=date0+"T"+calcTime(tsu_sec[i],0)+"Z";
        }
        for (int j=0; j<number_sweeps; j++) { time_units[j]="secs since "+tsu[j]; }

        dims0.add(radial);
        // add "time" variable
        Variable[] time=new Variable[number_sweeps];
        String tm="time";   String tm_name="";
        for (int j=0; j<number_sweeps; j++) {  tm_name=tm;
        if (number_sweeps > 1) { tm_name=tm+"_sweep_"+(j+1);  }
        time[j]=new Variable(ncfile, null, null, tm_name);
        time[j].setDataType(DataType.INT);
        time[j].setDimensions(dims0);
        time[j].addAttribute(new Attribute("long_name", "time from start of sweep"));
        time[j].addAttribute(new Attribute("units", time_units[j]));
        time[j].addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
        time[j].addAttribute(new Attribute("missing_value", -99));
        ncfile.addVariable(null, time[j]);
        varList.add(time[j]);
        }

        // add "elevationR" variable
        Variable[] elevationR=new Variable[number_sweeps];
        String ele="elevationR";   String ele_name="";
        for (int j=0; j<number_sweeps; j++) {  ele_name=ele;
        if (number_sweeps > 1) { ele_name=ele+"_sweep_"+(j+1);  }
        elevationR[j]=new Variable(ncfile, null, null, ele_name);
        elevationR[j].setDataType(DataType.FLOAT);
        elevationR[j].setDimensions(dims0);
        elevationR[j].addAttribute(new Attribute("long_name", "elevation angle"));
        elevationR[j].addAttribute(new Attribute("units", "degrees"));
        elevationR[j].addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));
        elevationR[j].addAttribute(new Attribute("missing_value", -999.99f));
        ncfile.addVariable(null, elevationR[j]);
        varList.add(elevationR[j]);
        }

        // add "azimuthR" variable
        Variable[] azimuthR=new Variable[number_sweeps];
        String azim="azimuthR";   String azim_name="";
        for (int j=0; j<number_sweeps; j++) {  azim_name=azim;
        if (number_sweeps > 1) { azim_name=azim+"_sweep_"+(j+1);  }
        azimuthR[j]=new Variable(ncfile, null, null, azim_name);
        azimuthR[j].setDataType(DataType.FLOAT);
        azimuthR[j].setDimensions(dims0);
        azimuthR[j].addAttribute(new Attribute("long_name", "azimuth angle"));
        azimuthR[j].addAttribute(new Attribute("units", "degrees"));
        azimuthR[j].addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString()));
        azimuthR[j].addAttribute(new Attribute("missing_value", -999.99f));
        ncfile.addVariable(null, azimuthR[j]);
        varList.add(azimuthR[j]);
        }

        // add "distanceR" variable
        Variable[] distanceR=new Variable[number_sweeps];
        String dName="distanceR";
        String dist_name="";
        for (int j=0; j<number_sweeps; j++) {  dist_name=dName;
        if (number_sweeps > 1) { dist_name=dName+"_sweep_"+(j+1);  }
        distanceR[j]=new Variable(ncfile, null, null, dist_name);
        distanceR[j].setDataType(DataType.FLOAT);
        dims1.add(gateR[j]);
        distanceR[j].setDimensions(dims1);
        distanceR[j].addAttribute(new Attribute("long_name", "radial distance"));
        distanceR[j].addAttribute(new Attribute("units", "m"));
        distanceR[j].addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString()));
        ncfile.addVariable(null, distanceR[j]);
        varList.add(distanceR[j]);
        dims1.clear();
        }
        // add "numGates" variable
        dims3.add(scanR);
        Variable numGates=new Variable(ncfile, null, null, "numGates");
        numGates.setDataType(DataType.INT);
        numGates.setDimensions(dims3);
        numGates.addAttribute(new Attribute("long_name", "number of gates in the sweep"));
        ncfile.addVariable(null, numGates);
        varList.add(numGates);

        // add global attributes
        ncfile.addAttribute(null, new Attribute("definition", "SIGMET-IRIS RAW"));
        ncfile.addAttribute(null, new Attribute("description", "SIGMET-IRIS data are reading by Netcdf IOSP"));
        ncfile.addAttribute(null, new Attribute("StationName", stnName));
        ncfile.addAttribute(null, new Attribute("StationName_SetupUtility", stnName_util));
        ncfile.addAttribute(null, new Attribute("radar_lat", new Float(radar_lat)));
        ncfile.addAttribute(null, new Attribute("radar_lon", new Float(radar_lon)));
        ncfile.addAttribute(null, new Attribute("ground_height", new Short(ground_height)));
        ncfile.addAttribute(null, new Attribute("radar_height", new Short(radar_height)));
        ncfile.addAttribute(null, new Attribute("radar_alt", new Integer(radar_alt)));
        ncfile.addAttribute(null, new Attribute("num_data_types", new Integer(nparams)));
        ncfile.addAttribute(null, new Attribute("number_sweeps", new Short(number_sweeps)));
        String sn="start_sweep";  String snn="";
        for (int j=0; j<number_sweeps; j++) { snn=sn;
        if (number_sweeps > 1) { snn=sn+"_"+(j+1);  }
        ncfile.addAttribute(null, new Attribute(snn, tsu[j]));
        }
        ncfile.addAttribute(null, new Attribute("num_rays", new Short(num_rays)));
        ncfile.addAttribute(null, new Attribute("max_number_gates", new Short(bins)));
        ncfile.addAttribute(null, new Attribute("range_first", new Float(range_first)));
        ncfile.addAttribute(null, new Attribute("range_last", new Float(range_last)));
        ncfile.addAttribute(null, new Attribute("DataType", "Radial"));
        ncfile.addAttribute(null, new Attribute("Conventions", _Coordinate.Convention));

        // --------- fill all of values in the ncfile ------
        doNetcdfFileCoordinate(ncfile, volScan.base_time, volScan.year, volScan.month, volScan.day, varList, recHdr);

        ncfile.finish();

        return varList;
    }

     /** Fill all of the variables/attributes in the ncfile
     * @param ncfile   NetcdfFile object which will be filled.

     * @param bst      number of seconds since midnight for start of sweep
     * @param yr       year of start of each sweep
     * @param m        month of start of each sweep
     * @param dda      day of start of each sweep
     * @param varList  ArrayList of Variables of ncfile
     * @param recHdr   java.util.Map with values for Attributes
     */
    public void doNetcdfFileCoordinate(ucar.nc2.NetcdfFile ncfile,  int[] bst,
            short[] yr, short[] m,  short[] dda,
            ArrayList<Variable> varList, java.util.Map<String, Number> recHdr)  {
        // prepare attribute values

        String[] unit={" ", "dbZ", "dbZ", "m/sec", "m/sec", "dB"};
        String def_datafile="SIGMET-IRIS";
        Short header_length=80;
        Short ray_header_length=6;
        int ngates=0;

        float radar_lat=((Float)recHdr.get("radar_lat")).floatValue(); //System.out.println("rad_lat="+radar_lat);
        float radar_lon=((Float)recHdr.get("radar_lon")).floatValue(); //System.out.println("rad_lon="+radar_lon);
        short ground_height=((Short)recHdr.get("ground_height")).shortValue(); //System.out.println("ground_H="+ground_height);
        short radar_height=((Short)recHdr.get("radar_height")).shortValue(); //System.out.println("radar_H="+radar_height);
        int radar_alt=(((Integer)recHdr.get("radar_alt")).intValue())/100; //System.out.println("rad_alt="+radar_alt);
        short num_rays=((Short)recHdr.get("num_rays")).shortValue(); //System.out.println("HERE!! num_rays="+num_rays);
        float range_first=(((Integer)recHdr.get("range_first")).intValue())*0.01f; //System.out.println("range_1st="+range_first);
        float range_last=(((Integer)recHdr.get("range_last")).intValue())*0.01f; //System.out.println("step="+step);
        short number_sweeps=((Short)recHdr.get("number_sweeps")).shortValue();
        int nparams=(((Integer)recHdr.get("nparams")).intValue()); //System.out.println("nparams="+nparams);
        // define date/time
        //int last_t=(int)(ray[nparams*number_sweeps-1][num_rays-1].getTime());
        int last_t = volScan.lastRay.getTime();
        String sss1=Short.toString(m[0]);
        if (sss1.length() < 2) sss1="0"+sss1;
        String sss2=Short.toString(dda[0]);
        if (sss2.length() < 2) sss2="0"+sss2;
        String base_date0=String.valueOf(yr[0])+"-"+sss1+"-"+sss2;
        String sss11=Short.toString(m[number_sweeps-1]);
        if (sss11.length() < 2) sss11="0"+sss11;
        String sss22=Short.toString(dda[number_sweeps-1]);
        if (sss22.length() < 2) sss22="0"+sss22;
        String base_date1=String.valueOf(yr[number_sweeps-1])+"-"+sss11+"-"+sss22;
        String start_time=base_date0+"T"+calcTime(bst[0], 0)+"Z";
        String end_time=base_date1+"T"+calcTime(bst[number_sweeps-1], last_t)+"Z";
        ncfile.addAttribute(null, new Attribute("time_coverage_start", start_time));
        ncfile.addAttribute(null, new Attribute("time_coverage_end", end_time));

        // set all of Variables
        try {
            int sz=varList.size();

            ArrayFloat.D2[] dataArr=new ArrayFloat.D2[nparams*number_sweeps];
            Index[] dataIndex=new Index[nparams*number_sweeps];

           // Variable[][] vv=new Variable[nparams][number_sweeps];
            Variable var=null;
            String var_name="";

            Ray[] rtemp=new Ray[(int)num_rays];

            // NCdump.printArray(dataArr[0], "Total_Power", System.out, null);

            Variable[] distanceR=new Variable[number_sweeps];
            ArrayFloat.D1[] distArr=new ArrayFloat.D1[number_sweeps];
            Index[] distIndex=new Index[number_sweeps];
            String distName="distanceR";
            for (int i=0; i<number_sweeps; i++) {
                if (number_sweeps > 1) {  distName="distanceR_sweep_"+(i+1); }
                for  (int ix=0; ix<sz; ix++)  {
                    var=varList.get(ix);
                    if ((var.getName()).equals(distName.trim())) { distanceR[i]= var; break; }
                }
                distArr[i]=(ArrayFloat.D1) Array.factory(DataType.FLOAT, distanceR[i].getShape());
                distIndex[i]=distArr[i].getIndex();

               // for (int jj=0; jj<num_rays; jj++) { rtemp[jj]=ray[i][jj]; }
                ngates=sweep_bins[i];
                float stp=calcStep(range_first, range_last,(short)ngates);
                for (int ii=0; ii<ngates; ii++)  {
                    distArr[i].setFloat(distIndex[i].set(ii), (range_first+ii*stp));
                }
            }
            // NCdump.printArray(distArr[0], "distanceR", System.out, null);
            List rgp =  volScan.getTotalPowerGroups();
            if(rgp.size() == 0) rgp = volScan.getReflectivityGroups(); 
            List [] sgp = new ArrayList[number_sweeps];
            for (int i=0; i<number_sweeps; i++) {
                sgp[i] = (List)rgp.get((short)i);
            }


            Variable[] time=new Variable[number_sweeps];
            ArrayInt.D1[] timeArr=new ArrayInt.D1[number_sweeps];
            Index[] timeIndex=new Index[number_sweeps];
            String t_n="time";
            for (int i=0; i<number_sweeps; i++) {
                if (number_sweeps > 1) { t_n="time_sweep_"+(i+1); }
                for  (int ix=0; ix<sz; ix++)  {
                    var=varList.get(ix);
                    if ((var.getName()).equals(t_n.trim())) { time[i]=var;  break; }
                }

                //                if (time[i].getShape().length == 0) {
                //                    continue;
                //                }
                timeArr[i]=(ArrayInt.D1) Array.factory(DataType.INT, time[i].getShape());
                timeIndex[i]=timeArr[i].getIndex();
                List rlist = sgp[i];

                for (int jj=0; jj<num_rays; jj++) { rtemp[jj]=(Ray)rlist.get(jj); }    //ray[i][jj]; }
                for (int jj=0; jj<num_rays; jj++) {
                    timeArr[i].setInt(timeIndex[i].set(jj), rtemp[jj].getTime());
                }
            }

            // NCdump.printArray(timeArr[0], "time", System.out, null);

            Variable[] azimuthR=new Variable[number_sweeps];
            ArrayFloat.D1[] azimArr=new ArrayFloat.D1[number_sweeps];
            Index[] azimIndex=new Index[number_sweeps];
            String azimName="azimuthR";
            for (int i=0; i<number_sweeps; i++) {
                if (number_sweeps > 1) {  azimName="azimuthR_sweep_"+(i+1); }
                for  (int ix=0; ix<sz; ix++)  {
                    var=varList.get(ix);
                    if ((var.getName()).equals(azimName.trim())) { azimuthR[i]=var; break;  }
                }
                azimArr[i]=(ArrayFloat.D1) Array.factory(DataType.FLOAT, azimuthR[i].getShape());
                azimIndex[i]=azimArr[i].getIndex();
                List rlist = sgp[i];

                for (int jj=0; jj<num_rays; jj++) { rtemp[jj]=(Ray)rlist.get(jj); } //ray[i][jj]; }
                for (int jj=0; jj<num_rays; jj++) {
                    azimArr[i].setFloat(azimIndex[i].set(jj), rtemp[jj].getAz());
                }
            }
            //NCdump.printArray(azimArr[0], "azimuthR", System.out, null);

            Variable[] elevationR=new Variable[number_sweeps];
            ArrayFloat.D1[] elevArr=new ArrayFloat.D1[number_sweeps];
            Index[] elevIndex=new Index[number_sweeps];
            String elevName="elevationR";
            for (int i=0; i<number_sweeps; i++) {
                if (number_sweeps > 1) {  elevName="elevationR_sweep_"+(i+1); }
                for  (int ix=0; ix<sz; ix++)  {
                    var=varList.get(ix);
                    if ((var.getName()).equals(elevName.trim())) { elevationR[i]=var; break;  }
                }
                elevArr[i]=(ArrayFloat.D1) Array.factory(DataType.FLOAT, elevationR[i].getShape());
                elevIndex[i]=elevArr[i].getIndex();
                List rlist = sgp[i];

                for (int jj=0; jj<num_rays; jj++) { rtemp[jj]=(Ray)rlist.get(jj); } //ray[i][jj]; }
                for (int jj=0; jj<num_rays; jj++) {
                    elevArr[i].setFloat(elevIndex[i].set(jj), rtemp[jj].getElev());
                }
            }
            // NCdump.printArray(elevArr[0], "elevationR", System.out, null);

            Variable numGates=null;
            for (int i=0; i<number_sweeps; i++) {
                for  (int ix=0; ix<sz; ix++)  {
                    var=varList.get(ix);
                    if ((var.getName()).equals("numGates")) { numGates=var; break;  }
                }
            }
            ArrayInt.D1 gatesArr=(ArrayInt.D1) Array.factory(DataType.INT, numGates.getShape());
            Index gatesIndex=gatesArr.getIndex();
            
            for (int i=0; i<number_sweeps; i++) {
                List rlist = sgp[i];
                for (int jj=0; jj<num_rays; jj++) { rtemp[jj]=(Ray)rlist.get(jj); } //ray[i][jj]; }
                ngates=rtemp[0].getBins();
                gatesArr.setInt(gatesIndex.set(i), ngates);
            }

            for (int i=0; i<number_sweeps; i++) {
                distanceR[i].setCachedData(distArr[i], false);
                time[i].setCachedData(timeArr[i], false);
                azimuthR[i].setCachedData(azimArr[i], false);
                elevationR[i].setCachedData(elevArr[i], false);
            }
            numGates.setCachedData(gatesArr, false);
            // startSweep.setCachedData(sweepArr, false);

            //          -------------------------------------------------
           // int b=(int)ray[0][0].getBins();

            // -- Test of readData() and readToByteChannel() -----------------
            /*
  Range r1=new Range(356, 359);
  Range r2=new Range(0, 15);
  java.util.List arlist=new ArrayList();
  arlist.add(r1);
  arlist.add(r2);
       Array testArr=readData(v[0], new Section(arlist));
       NCdump.printArray(testArr, "Total_Power_sweep_1", System.out, null);
   WritableByteChannel channel=new FileOutputStream(new File("C:\\netcdf\\tt.dat")).getChannel();
   long ikk=readToByteChannel(v[0], new Section(arlist), channel);
   System.out.println("IKK="+ikk);
   channel.close();
             */
            //---------------------------------------------------

        } catch (Exception e) { System.out.println(e.toString()); e.printStackTrace(); }
    }   //----------- end of doNetcdf ----------------------------------

    /** Read data from a top level Variable and return a memory resident Array.
     * @param v2  Variable. It may have FLOAT/INTEGER data type.
     * @param section  wanted section of data of Variable. The section list is a list
     *                 of ucar.ma2.Range which define the requested data subset.
     * @return  Array of data which will be read from Variable through this call.
     */
    public Array readData1(ucar.nc2.Variable v2, Section section)
    throws IOException, InvalidRangeException {
        //doData(raf, ncfile, varList);
        int[] sh=section.getShape();
        Array temp=Array.factory(v2.getDataType(), sh);
        long pos0=0;
        // Suppose that the data has LayoutRegular
        LayoutRegular index=new LayoutRegular(pos0, v2.getElementSize(), v2.getShape(), section);
        if (v2.getName().startsWith("time") | v2.getName().startsWith("numGates")) {
            temp=readIntData(index, v2);
        } else {   temp=readFloatData(index, v2);    }
        return temp;
    }

    public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
     // Vgroup vgroup = (Vgroup) v2.getSPobject();
     // Range scanRange = section.getRange(0);
     // Range radialRange = section.getRange(1);
     // Range gateRange = section.getRange(2);

      Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), section.getShape());
      IndexIterator ii = data.getIndexIterator();

      List groups = null;
      String shortName = v2.getName();
      if( shortName.startsWith("Reflectivity"))
        groups = volScan.getReflectivityGroups();
      else if( shortName.startsWith("Velocity"))
        groups = volScan.getVelocityGroups();
      else if( shortName.startsWith("TotalPower"))
        groups = volScan.getTotalPowerGroups();
      else if( shortName.startsWith("Width"))
        groups = volScan.getWidthGroups();
      else if( shortName.startsWith("DiffReflectivity"))
        groups = volScan.getDifferentialReflectivityGroups();

      if(section.getRank() == 2) {
          Range radialRange = section.getRange(0);
          Range gateRange = section.getRange(1);
          List lli = (List)groups.get(0);
          readOneScan(lli, radialRange, gateRange, ii);
      } else {
          Range scanRange = section.getRange(0);
          Range radialRange = section.getRange(1);
          Range gateRange = section.getRange(2);
          for (int i=scanRange.first(); i<=scanRange.last(); i+= scanRange.stride()) {
            readOneScan((List)groups.get(i), radialRange, gateRange, ii);
          }
      }
      return data;
    }


  private void readOneScan(List mapScan, Range radialRange, Range gateRange, IndexIterator ii) throws IOException {
    int siz = mapScan.size();
    for (int i=radialRange.first(); i<=radialRange.last(); i+= radialRange.stride()) {
      if(i >= siz)
           readOneRadial(null, gateRange, ii);
      else {
          Ray r = (Ray)mapScan.get(i);
          readOneRadial(r, gateRange, ii);
      }
    }
  }

  private void readOneRadial(Ray r, Range gateRange, IndexIterator ii) throws IOException {
    if (r == null) {
      for (int i=gateRange.first(); i<=gateRange.last(); i+= gateRange.stride())
        ii.setFloatNext( Float.NaN);
      return;
    }
    r.readData(volScan.raf, gateRange, ii);
  }
    /** Read data from a top level Variable of INTEGER data type and return a memory resident Array.
     * @param index  LayoutRegular object
     * @param v2     Variable has INTEGER data type.
     * @return  Array of data which will be read from Variable through this call.
     */
    public Array readIntData(LayoutRegular index, Variable v2)
    throws IOException  {
        int[] var=(int[])(v2.read().get1DJavaArray(v2.getDataType().getPrimitiveClassType()));
        int[] data = new int[(int)index.getTotalNelems()];
        while (index.hasNext()) {
            Layout.Chunk chunk=index.next();
            System.arraycopy(var, (int)chunk.getSrcPos()/4, data,
                    (int)chunk.getDestElem(), chunk.getNelems());
        }
        return Array.factory(data);
    }
    /** Read data from a top level Variable of FLOAT data type and return a memory resident Array.
     * @param index  LayoutRegular object
     * @param v2     Variable has FLOAT data type.
     * @return  Array of data which will be read from Variable through this call.
     */
    public Array readFloatData(LayoutRegular index, Variable v2)
    throws IOException  {
        float[] var=(float[])(v2.read().get1DJavaArray(v2.getDataType().getPrimitiveClassType()));
        float[] data = new float[(int)index.getTotalNelems()];
        while (index.hasNext()) {
            Layout.Chunk chunk=index.next();
            System.arraycopy(var, (int)chunk.getSrcPos()/4, data,
                    (int)chunk.getDestElem(), chunk.getNelems());
        }
        return Array.factory(data);
    }
    //----------------------------------------------------------------------------------
    /** A way to communicate arbitrary information to an iosp. */
    public Object sendIospMessage(Object message) {
        return super.sendIospMessage(message);
    }

    /** Read data from a top level Variable and send data to a WritableByteChannel.
     * @param v2  Variable
     * @param section  wanted section of data of Variable. The section list is a list
     *                 of ucar.ma2.Range which define the requested data subset.
     * @param channel  WritableByteChannel object - channel that can write bytes.
     * @return the number of bytes written, possibly zero.
     */
    public long readToByteChannel11(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
    throws java.io.IOException, ucar.ma2.InvalidRangeException {
        Array data = readData(v2, section);
        float[] ftdata=new float[(int)data.getSize()];
        byte[] bytedata=new byte[(int)data.getSize()*4];
        IndexIterator iter=data.getIndexIterator();
        int i=0;
        ByteBuffer buffer=ByteBuffer.allocateDirect(bytedata.length);
        while (iter.hasNext()) { ftdata[i]=iter.getFloatNext();
        bytedata[i]=new Float(ftdata[i]).byteValue();
        buffer.put(bytedata[i]);
        i++;
        }
        buffer=ByteBuffer.wrap(bytedata);
        // write the bytes to the channel
        int count=channel.write(buffer);  System.out.println("COUNT="+count);
        // check if all bytes where written
        if (buffer.hasRemaining()) {
            // if not all bytes were written, move the unwritten bytes to the beginning and
            // set position just after the last unwritten byte
            buffer.compact();
        } else { buffer.clear();  }
        return (long)count;
    }


    //  -----------------------------------------------------------------------
    /** Convert 2 bytes binary angle to float
     * @param angle   two bytes binary angle
     * @return float value of angle in degrees with precision of two decimal
     */
    static float calcAngle(short angle) {
        final double maxval=65536.0;
        double ang=(double)angle;
        if (ang < 0.0) { ang=maxval+ang; }
        double temp=(ang/maxval)*360.0;
        BigDecimal bd=new BigDecimal(temp);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Convert 4 bytes binary angle to float
     * @param ang   four bytes binary angle
     * @return float value of angle with precision of two decimal in degrees
     */
    static float calcAngle(int ang) {
        final double maxval=4294967296.0;
        double temp=(ang/maxval)*360.0;
        BigDecimal bd=new BigDecimal(temp);
        BigDecimal result=bd.setScale(3, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Calculate radial elevation of each ray
     * @param angle  two bytes binary angle
     * @return float value of elevation in degrees with precision of two decimal
     */
    static float calcElev(short angle) {
        final double maxval=65536.0;
        double ang=(double)angle;
        if (angle < 0) ang=(~angle)+1;
        double temp=(ang/maxval)*360.0;
        BigDecimal bd=new BigDecimal(temp);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Calculate distance between sequential bins in a ray
     * @param range_first  range of first bin in centimeters
     * @param range_last   range of last bin in centimeters
     * @param num_bins     number of bins
     * @return float distance in centimeters with precision of two decimal
     */
    static float calcStep(float range_first, float range_last, short num_bins) {
        float step=(range_last-range_first)/(num_bins-1);
        BigDecimal bd=new BigDecimal(step);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Calculate azimuth of a ray
     * @param az0  azimuth at beginning of ray (binary angle)
     * @param az1  azimuth at end of ray (binary angle)
     * @return float azimuth in degrees with precision of two decimal
     */
    static float calcAz(short az0, short az1) {
        // output in deg
        float azim0=calcAngle(az0);
        float azim1=calcAngle(az1);
        float d=0.0f;
        d=Math.abs(azim0-azim1);
        if ((az0 < 0)&(az1 > 0)) { d=Math.abs(360.0f-azim0)+Math.abs(azim1);  }
        double temp=azim0+d*0.5;
        if (temp>360.0) { temp-=360.0; }
        BigDecimal bd=new BigDecimal(temp);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Calculate data values from raw ingest data
     * @param recHdr  java.util.Map object with values for calculation
     * @param dty     type of data ( "Total_Power", "Reflectivity", "Velocity",
     *                "Width", "Differential_Reflectivity")
     * @param data    1-byte input value
     * @return float value with precision of two decimal
     */
    static float calcData(Map<String, Number> recHdr, short dty, byte data) {
        short[] coef={1, 2, 3, 4}; // MultiPRF modes
        short multiprf=((Short)recHdr.get("multiprf")).shortValue();
        float vNyq=((Float)recHdr.get("vNyq")).floatValue();
        double temp=-999.99;
        switch (dty) {
        default:        // dty=1,2 -total_power, reflectivity (dBZ)
            if (data != 0) { temp=(((int)data & 0xFF)-64)*0.5; }
            break;
        case 3:        // dty=3 - mean velocity (m/sec)
            if (data != 0) {
                temp=((((int)data & 0xFF)-128)/127.0)*vNyq*coef[multiprf];  }
            break;
        case 4:        // dty=4 - spectrum width (m/sec)
            if (data != 0) {
                double v=((((int)data & 0xFF)-128)/127.0)*vNyq*coef[multiprf];
                temp=(((int)data & 0xFF)/256.0)*v;  }
            break;
        case 5:        // dty=5 - differential reflectivity (dB)
            if (data != 0) {
                temp=((((int)data & 0xFF)-128)/16.0); }
            break;
        }
        BigDecimal bd=new BigDecimal(temp);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }

    /** Calculate time as hh:mm:ss
     * @param t   number of seconds since midnight for start of sweep
     * @param t0  time in seconds from start of sweep
     * @return time as string "hh:mm:ss"
     */
    static String calcTime(int t, int t0) {
        StringBuffer tim=new StringBuffer();
        int[] tt=new int[3];
        int mmh=(t+t0)/60;
        tt[2]=(t+t0)%60;                  // Define SEC
        tt[0]=mmh/60;                     // Define HOUR
        tt[1]=mmh%60;                     // Define MIN
        for (int i=0; i<3; i++) {
            String s=Integer.toString(tt[i]);
            int len=s.length();
            if (len < 2) { s="0"+tt[i]; }
            if (i != 2) s+=":";
            tim.append(s);
        }
        return tim.toString();
    }
    /** Calculate of Nyquist velocity
     * @param prf   PRF in Hertz
     * @param wave  wavelength in 1/100 of centimeters
     * @return float value of Nyquist velocity in m/sec with precision of two decimal
     */
   static float calcNyquist(int prf, int wave) {
        double tmp=(prf*wave*0.01)*0.25;
        tmp=tmp*0.01;                    //Make it m/sec
        BigDecimal bd=new BigDecimal(tmp);
        BigDecimal result=bd.setScale(2, RoundingMode.HALF_DOWN);
        return result.floatValue();
    }


    // dummy implementations of other methods
    public Array readNestedData(Variable v2, List section) throws IOException, InvalidRangeException {
        return null;
    }
    public void close() throws java.io.IOException {
      myRaf.close();
    }

 //   public void close() throws IOException {  }
//    public boolean syncExtend() throws IOException {
//        return false;
//    }
//    public boolean sync() throws IOException {
//        return false;
//    }
    public void setSpecial(Object special) {  }
    public String toStringDebug(Object o) {
        return null;
    }
    public String getDetailInfo() {
        return null;
    }


}