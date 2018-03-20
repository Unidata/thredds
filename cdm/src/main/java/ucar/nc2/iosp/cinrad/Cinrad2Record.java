/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.cinrad;

import org.joda.time.DateTime;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;


/**
 * This class reads one record (radial) in an CINRAD level II file.
 * File must be uncompressed.
 * Not handling messages yet, only data.
 * <p>
 * 10/16/05: Now returns data as a byte, so use scale and offset.
 *
 * Adapted with permission from the Java Iras software developed by David Priegnitz at NSSL.
 *
 * @author caron
 * @author David Priegnitz
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class Cinrad2Record {

  /** Reflectivity moment identifier */
  public static final int REFLECTIVITY = 1;

  /** Radial Velocity moment identifier */
  public static final int VELOCITY_HI = 2;

  /** Radial Velocity moment identifier */
  public static final int VELOCITY_LOW = 4;

  /** Sprectrum Width moment identifier */
  public static final int SPECTRUM_WIDTH = 3;

  /** Low doppler resolution code */
  public static final int DOPPLER_RESOLUTION_LOW_CODE = 4;

  /** High doppler resolution code */
  public static final int DOPPLER_RESOLUTION_HIGH_CODE = 2;

  /** Horizontal beam width */
  public static final float HORIZONTAL_BEAM_WIDTH = (float) 1.5;         // LOOK

  // Lookup Tables

  /** Initialization flag for lookup tables
  public static int data_lut_init_flag = 0;

  /** Reflectivity look up table
  public static float[] Reflectivity_LUT = new float[256];

  /** 1 km Velocity look up table
  public static float[] Velocity_1km_LUT = new float[256];

  /** 1/2 km Velocity look up table
  public static float[] Velocity_hkm_LUT = new float[256];

  static {
    Reflectivity_LUT[0] = 0.0f; // Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
    Reflectivity_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;
    Velocity_1km_LUT[0] = 0.0f; // Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
    Velocity_1km_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;
    Velocity_hkm_LUT[0] = 0.0f; // Float.NaN;  //(float) SIGNAL_BELOW_THRESHOLD;
    Velocity_hkm_LUT[1] = Float.NaN;  //(float) SIGNAL_OVERLAID;

    for (int i = 2; i < 256; i++) {
      Reflectivity_LUT[i] = (float) (i / 2.0 - 33.0);
      Velocity_1km_LUT[i] = (float) (i - 129.0);
      Velocity_hkm_LUT[i] = (float) (i / 2.0 - 64.5); // also spectrum width
    }
  } */

  public static byte MISSING_DATA = (byte) 1;
  public static final byte BELOW_THRESHOLD = (byte) 0;

  /** Size of the file header, aka title */
  static int FILE_HEADER_SIZE = 0;

  /** Size of the CTM record header */
  private static int CTM_HEADER_SIZE = 14;

  /** Size of the the message header, to start of the data message */
  private static final int MESSAGE_HEADER_SIZE = 28;

  /** Size of the entire message, if its a radar data message */
  private static int RADAR_DATA_SIZE = 2432;

  static public String getDatatypeName( int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return "Reflectivity";
      case VELOCITY_HI :
      case VELOCITY_LOW : return "RadialVelocity";
      case SPECTRUM_WIDTH : return "SpectrumWidth";
      default : throw new IllegalArgumentException();
    }
  }

   static public String getDatatypeUnits(int datatype) {
     switch (datatype) {
       case REFLECTIVITY :
           return "dBz";

       case VELOCITY_HI :
       case VELOCITY_LOW :
       case SPECTRUM_WIDTH :
           return "m/s";
     }
     throw new IllegalArgumentException();
   }

  static public float getDatatypeScaleFactor(int datatype) {
    switch (datatype) {
      case REFLECTIVITY:
        if(Cinrad2IOServiceProvider.isCC)
          return 0.1f;
        if(Cinrad2IOServiceProvider.isCC20)
          return 0.5f;
        else
          return 0.5f;
      case VELOCITY_LOW :
        if(Cinrad2IOServiceProvider.isSC)
          return 0.3673f;
        else if(Cinrad2IOServiceProvider.isCC)
          return 0.1f;
        else
          return 1.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH :
        if(Cinrad2IOServiceProvider.isSC)
          return 0.1822f;
        else if(Cinrad2IOServiceProvider.isCC)
          return 0.1f;
        else if(Cinrad2IOServiceProvider.isCC20)
          return 1.0f;
        else
          return 0.5f;
      default : throw new IllegalArgumentException();
    }
  }

  static public float getDatatypeAddOffset(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
        if(Cinrad2IOServiceProvider.isSC)
          return -32.0f;
        else if (Cinrad2IOServiceProvider.isCC)
          return 0.0f;
        else if (Cinrad2IOServiceProvider.isCC20)
          return -32.0f;
        else
          return -33.0f;
      case VELOCITY_LOW :
        if(Cinrad2IOServiceProvider.isSC)
          return 0.0f;
        else if(Cinrad2IOServiceProvider.isCC)
          return 0.0f;
        else if (Cinrad2IOServiceProvider.isCC20)
          return 0.0f;
        else
          return -129.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH :
          if(Cinrad2IOServiceProvider.isSC)
            return 0.0f;
          else if(Cinrad2IOServiceProvider.isCC)
            return 0.0f;
          else if (Cinrad2IOServiceProvider.isCC20)
            return 0.0f;
          else
            return -64.5f;
      default : throw new IllegalArgumentException();
    }
  }

  static public String getMessageTypeName( int code) {
    switch (code) {
      case 1 : return "digital radar data";
      case 2 : return "RDA status data";
      case 3 : return "performance/maintainence data";
      case 4 : return "console message - RDA to RPG";
      case 5 : return "maintainence log data";
      case 6 : return "RDA control ocmmands";
      case 7 : return "volume coverage pattern";
      case 8 : return "clutter censor zones";
      case 9 : return "request for data";
      case 10 : return "console message - RPG to RDA";
      case 11 : return "loop back test - RDA to RPG";
      case 12 : return "loop back test - RPG to RDA";
      case 13 : return "clutter filter bypass map - RDA to RPG";
      case 14 : return "edited clutter filter bypass map - RDA to RPG";
      case 15: return "Notchwidth Map";
      case 18: return "RDA Adaptation data";
      default : return "unknown "+code;
    }
  }

  static public String getRadialStatusName( int code) {
    switch (code) {
      case 0 : return "start of new elevation";
      case 1 : return "intermediate radial";
      case 2 : return "end of elevation";
      case 3 : return "begin volume scan";
      case 4 : return "end volume scan";
      default : return "unknown "+code;
    }
  }

  static public String getVolumeCoveragePatternName( int code) {
    switch (code) {
      case 11 : return "16 elevation scans every 5 mins";
      case 12 : return "14 elevation scan every 4.1 mins";
      case 21 : return "11 elevation scans every 6 mins";
      case 31 : return "8 elevation scans every 10 mins";
      case 32 : return "7 elevation scans every 10 mins";
      case 121: return "9 elevations, 20 scans every 5 minutes";
      default : return "unknown "+code;
    }
  }

  static public java.util.Date getDate(int julianDays, int msecs) {
    long total = ((long) (julianDays - 1)) * 24 * 3600 * 1000 + msecs;
    return new Date( total);
  }

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Cinrad2Record.class);

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  int recno;        //  record number within the file
  long message_offset; // offset of start of message
  boolean hasReflectData, hasDopplerData;

  // message header
  short message_size = 0;
  byte id_channel = 0;
  public byte message_type = 0;
  short id_sequence = 0;
  short mess_julian_date = 0;
  int mess_msecs = 0;
  short seg_count = 0;
  short seg_number = 0;

  // radar data header
  int data_msecs = 0;
  short data_julian_date = 0;
  short unamb_range = 0;
  int azimuth_ang = 0;
  int azimuth_ang_end = 0;
  short radial_num = 0; // radial number within the elevation : starts with one
  short radial_status = 0;
  short elevation_ang = 0;
  short elevation_ang_end = 0;
  short elevation_num = 0;

  short reflect_first_gate = 0; // distance to first reflectivity gate (m)
  short reflect_gate_size = 0; //  reflectivity gate size (m)
  short reflect_gate_count = 0; //  number of reflectivity gates

  short doppler_first_gate = 0; // distance to first reflectivity gate (m)
  short doppler_gate_size = 0; //  reflectivity gate size (m)
  short doppler_gate_count = 0; //  number of reflectivity gates

  short cut = 0;
  float calibration = 0; // system gain calibration constant (db biased)
  short resolution = 0; // dopplar velocity resolution
  short vcp = 0;        // volume coverage pattern

  short nyquist_vel; // nyquist velocity
  short attenuation; // atmospheric attenuation factor
  short threshhold; // threshhold paramter for minimum difference

  private short reflect_offset; // reflectivity data pointer (byte number from start of message)
  private short velocity_offset; // velocity data pointer (byte number from start of message)
  private short spectWidth_offset; // spectrum-width data pointer (byte number from start of message)

  public DateTime dateTime0;
  public DateTime dateTimeE;
  public SweepInfo [] sweepInfo;
  public static Cinrad2Record factory(RandomAccessFile din, int record) throws IOException {
    long offset = (long)record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
    if (offset >= din.length())
      return null;
    else
      return new Cinrad2Record(din, record);
  }


  public short convertunsignedByte2Short(byte b) {
      return (short)((b<0)? (short)b + 256 : (short)b);
   }

  public void readSCHeader(RandomAccessFile din) throws IOException {

    message_offset = 0;
    din.seek(message_offset);
// site info 170
    din.skipBytes(90);
    byte[] b10 = new byte[10];
    // Message Header
    din.read(b10);
    String stationNId = new String(b10);

    din.skipBytes(52);

    // latlon
    int lon = din.readInt();
    int lat = din.readInt();
    int hhh = din.readInt();

    din.skipBytes(6);
    //
    message_type = 1;

    //PerformanceInfo
    din.skipBytes(31);

    //ObservationInfo
    vcp = convertunsignedByte2Short(din.readByte());
    short syear = (short)din.readUnsignedShort();
    short smm = convertunsignedByte2Short(din.readByte());
    short sdd = convertunsignedByte2Short(din.readByte());
    short shh = convertunsignedByte2Short(din.readByte());
    short smi = convertunsignedByte2Short(din.readByte());
    short sss = convertunsignedByte2Short(din.readByte());

    dateTime0 = new DateTime(syear,smm,sdd,shh,smi,sss);

    din.skipBytes(8);
    long offset = din.getFilePointer();
    sweepInfo = new SweepInfo[30];
    for(int i = 0; i < 30; i++){
      sweepInfo[i] = new SweepInfo(din, (int)offset);
      offset = offset + 21;
    }

    din.skipBytes(6);

    syear = (short)din.readUnsignedShort();
    smm = convertunsignedByte2Short(din.readByte());
    sdd = convertunsignedByte2Short(din.readByte());
    shh = convertunsignedByte2Short(din.readByte());
    smi = convertunsignedByte2Short(din.readByte());
    sss = convertunsignedByte2Short(din.readByte());

    dateTimeE = new DateTime(syear,smm,sdd,shh,smi,sss);

  }

  int sweepN = 1;
  int echoType;
  int [] elev;
  int [] recordNum;
  byte cDataForm;
  public void readCCHeader(RandomAccessFile din) throws IOException {

    message_offset = 0;
    din.seek(message_offset);
// site info 170
    din.skipBytes(66);

    // Message Header
    String stationId = din.readString(40);
    String stationNbr = din.readString(10);

    din.skipBytes(20);

    String clon = din.readString(16);
    String clat = din.readString(16);
    // latlon
    int lon = (int)din.readInt();
    int lat = (int)din.readInt();
    int hhh = (int)din.readInt();

    din.skipBytes(4);

    //ObservationInfo

    short syear1 = convertunsignedByte2Short(din.readByte());
    short syear2 = convertunsignedByte2Short(din.readByte());
    short syear = (short)(syear1 * 100 + syear2);
    short smm = convertunsignedByte2Short(din.readByte());
    short sdd = convertunsignedByte2Short(din.readByte());
    short shh = convertunsignedByte2Short(din.readByte());
    short smi = convertunsignedByte2Short(din.readByte());
    short sss = convertunsignedByte2Short(din.readByte());

    dateTime0 = new DateTime(syear,smm,sdd,shh,smi,sss);

    din.skipBytes(1);

    syear1 = convertunsignedByte2Short(din.readByte());
    syear2 = convertunsignedByte2Short(din.readByte());
    syear = (short)(syear1 * 100 + syear2);
    smm = convertunsignedByte2Short(din.readByte());
    sdd = convertunsignedByte2Short(din.readByte());
    shh = convertunsignedByte2Short(din.readByte());
    smi = convertunsignedByte2Short(din.readByte());
    sss = convertunsignedByte2Short(din.readByte());

    dateTimeE = new DateTime(syear,smm,sdd,shh,smi,sss);
    short scanMode = convertunsignedByte2Short(din.readByte());
    if(scanMode == 10) {
      sweepN = 1;
    } else if(scanMode >= 100){
      sweepN = scanMode - 100;
    } else {
      throw new IOException("Error reading CINRAD CC data: Unsupported product: RHI/FFT");
    }

    elev = new int[sweepN];
    din.skipBytes(4);
    short sRHIA= (short)din.readUnsignedShort();

    din.skipBytes(4);

    echoType = din.readUnsignedShort();
    if(echoType != 0x408a) //only support vppi at this moment
      throw new IOException("Error reading CINRAD CC data: Unsupported level 2 data");

    int prodCode = din.readUnsignedShort();

    if(prodCode != 0x8003) //only support vppi at this moment
      throw new IOException("Error reading CINRAD CC data: Unsupported product: RHI/FFT");
    din.skipBytes(4);

    //remain2[660]
    for(int i = 0; i < sweepN; i++) {
      int maxV =  din.readUnsignedShort();
      int maxL =  din.readUnsignedShort();
      int binWidth =  din.readUnsignedShort();
      int binNum =   din.readUnsignedShort();
      int recordTotalNum = din.readUnsignedShort();
      din.skipBytes(8);
      elev[i] = din.readUnsignedShort();
      din.skipBytes(2);
      //System.out.println("bin num: " + binNum + " maxL " + maxL + " totalRNumber " + recordTotalNum);
    }
    //din.seek(1020);
   // int doffset = din.readInt();
    //System.out.println(" Offset: " + doffset);
  }

  public void readCC20Header(RandomAccessFile din) throws IOException {

    message_offset = 0;
    din.seek(message_offset);
// site info 170
    din.skipBytes(62);

    // Message Header
    String stationId = din.readString(40);
    String stationNbr = din.readString(10);

    din.skipBytes(20);

    String clon = din.readString(16);
    String clat = din.readString(16);
    // latlon
    int lon = (int)din.readInt();
    int lat = (int)din.readInt();
    int hhh = (int)din.readInt();

    din.skipBytes(40);

    //ObservationInfo
    short scanMode = convertunsignedByte2Short(din.readByte());
    if(scanMode == 10) {
      sweepN = 1;
    } else if(scanMode >= 100){
      sweepN = scanMode - 100;
    } else {
      throw new IOException("Error reading CINRAD CC data: Unsupported product: RHI/FFT");
    }

    short syear = (short)din.readUnsignedShort();
    short smm = convertunsignedByte2Short(din.readByte());
    short sdd = convertunsignedByte2Short(din.readByte());
    short shh = convertunsignedByte2Short(din.readByte());
    short smi = convertunsignedByte2Short(din.readByte());
    short sss = convertunsignedByte2Short(din.readByte());

    dateTime0 = new DateTime(syear,smm,sdd,shh,smi,sss);

    din.skipBytes(14); // 14+ (35 - 21)

    //remain2[660]
    elev = new int[sweepN];
    recordNum = new int[sweepN];
    for(int i = 0; i < sweepN; i++) {
      din.skipBytes(14);
      int zbinWidth =  din.readUnsignedShort();
      int vbinWidth =  din.readUnsignedShort();
      int sbinWidth =  din.readUnsignedShort();
      int zbinNum =   din.readUnsignedShort();
      int vbinNum =   din.readUnsignedShort();
      int sbinNum =   din.readUnsignedShort();
      recordNum[i] = din.readUnsignedShort();
      //if(i > 0)
      //  recordNum[i] = recordNum[i] + recordNum[i-1];
      elev[i] = din.readShort();
      cDataForm = din.readByte();
      if(cDataForm != 22 && cDataForm != 23 && cDataForm != 24)
        throw new IOException("Unsupported CC data format");
      int dataP = din.readInt();
      //din.skipBytes(2);
      //System.out.println("zbin num: " + zbinNum + " vbin num: " + vbinNum + " sbin num: " + sbinNum + " dataForm " + cDataForm);
    }

    for(int i = sweepN; i < 32; i++) {
      din.skipBytes(35);
    }

    din.skipBytes(6);

    syear = (short)din.readUnsignedShort();
    smm = convertunsignedByte2Short(din.readByte());
    sdd = convertunsignedByte2Short(din.readByte());
    shh = convertunsignedByte2Short(din.readByte());
    smi = convertunsignedByte2Short(din.readByte());
    sss = convertunsignedByte2Short(din.readByte());

    dateTimeE = new DateTime(syear,smm,sdd,shh,smi,sss);

  }


  public Cinrad2Record(RandomAccessFile din, int record) throws IOException {
    if(!Cinrad2IOServiceProvider.isSC && !Cinrad2IOServiceProvider.isCC
            && !Cinrad2IOServiceProvider.isCC20) {
      //CINRAD SA/SB
      this.recno = record;
      message_offset = (long) record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;

      din.seek(message_offset);

      din.skipBytes(CTM_HEADER_SIZE);
      //byte[] b2 = new byte[2];
      // Message Header
      //din.read(b2);
      //
      // b2[0] = din.readByte();
      //  b2[1] = din.readByte();
      //  message_size = (short)getUInt(b2, 2);
      //message_size  = (short)din.readShort(); // size in "halfwords" = 2 bytes

      //id_channel    = din.readByte(); // channel id
      message_type = din.readByte();
      //id_sequence   = din.readShort();
      //skip 2 byte
      din.skipBytes(13);
      //mess_msecs    = din.readInt();   // message generation time
      //mess_julian_date   = din.readShort(); // from 1/1/70; prob "message generation time"

      // seg_count     = din.readShort(); // number of message segments
      //seg_number    = din.readShort(); // this segment

      //dumpMessage(System.out, dd);
      if (message_type != 1) return;

      // data header
      byte[] b4 = din.readBytes(4);
      data_msecs = bytesToInt(b4, true); //din.readInt();   // collection time for this radial, msecs since midnight
      byte[] b2 = din.readBytes(2);
      data_julian_date = (short) bytesToShort(b2, true); //din.readShort(); // prob "collection time"
      unamb_range = din.readShort(); // unambiguous range
      azimuth_ang = din.readUnsignedShort(); // LOOK why unsigned ??
      radial_num = din.readShort(); // radial number within the elevation
      radial_status = din.readShort();
      elevation_ang = din.readShort();
      elevation_num = din.readShort(); // RDA elevation number
      reflect_first_gate = din.readShort(); // range to first gate of reflectivity (m) may be negetive
      doppler_first_gate = din.readShort(); // range to first gate of dopplar (m) may be negetive
      reflect_gate_size = din.readShort(); // reflectivity data gate size (m)
      doppler_gate_size = din.readShort(); // dopplar data gate size (m)
      reflect_gate_count = din.readShort(); // number of reflectivity gates
      doppler_gate_count = din.readShort(); // number of velocity or spectrum width gates
      if (record == 0) {
        if (reflect_gate_count == 1000 && doppler_gate_count == 1000)
          RADAR_DATA_SIZE = 3132;
        else if (reflect_gate_count == 800 || doppler_gate_count == 1600)
          RADAR_DATA_SIZE = 4132;
        else
          RADAR_DATA_SIZE = 2432;
      }
      din.skipBytes(6);
      //cut           = din.readShort(); // sector number within cut
      //calibration   = din.readFloat(); // system gain calibration constant (db biased)
      reflect_offset = din.readShort(); // reflectivity data pointer (byte number from start of message)
      velocity_offset = din.readShort(); // velocity data pointer (byte number from start of message)
      spectWidth_offset = din.readShort(); // spectrum-width data pointer (byte number from start of message)
      resolution = din.readShort(); // dopplar velocity resolution
      vcp = din.readShort(); // volume coverage pattern
      message_type = 1;
      din.skipBytes(14);

      nyquist_vel = din.readShort(); // nyquist velocity
      din.skipBytes(38);
      //attenuation   = din.readShort(); // atmospheric attenuation factor
      //threshhold    = din.readShort(); // threshhold paramter for minimum difference

      hasReflectData = (reflect_gate_count > 0);
      hasDopplerData = (doppler_gate_count > 0);
      //  dump(System.out);
    } else if(Cinrad2IOServiceProvider.isSC) {
      //CINRAD SC
      this.recno = record;
      // read header for every record
      readSCHeader(din);
      RADAR_DATA_SIZE = 4000;
        //FILE_HEADER_SIZE = 1024;

      message_offset = (long) record * RADAR_DATA_SIZE + 1024;
      if (message_offset >= din.length())
        return;
      din.seek(message_offset);
      //System.out.println("record = " + record);
      azimuth_ang = din.readUnsignedShort(); // LOOK why unsigned ??
      elevation_ang = (short)din.readUnsignedShort();
      azimuth_ang_end = din.readUnsignedShort(); // LOOK why unsigned ??
      elevation_ang_end = (short)din.readUnsignedShort();
      radial_num = (short)(record % 360 + 1); // radial number within the elevation

      elevation_num =(short)((record/360) + 1); // RDA elevation number
      reflect_first_gate = 300; // range to first gate of reflectivity (m) may be negetive
      doppler_first_gate = 300; // range to first gate of dopplar (m) may be negetive
      reflect_gate_size = 300; // reflectivity data gate size (m)
      doppler_gate_size = 300; // dopplar data gate size (m)
      reflect_gate_count = 998; // number of reflectivity gates
      doppler_gate_count = 998; // number of velocity or spectrum width gates
      //vcp = sweepInfo[0].svcp;
      //cut           = din.readShort(); // sector number within cut
      //calibration   = din.readFloat(); // system gain calibration constant (db biased)
      reflect_offset =   8; // reflectivity data pointer (byte number from start of message)
      velocity_offset =  8; // 9 velocity data pointer (byte number from start of message)
      spectWidth_offset = 8; // 11 spectrum-width data pointer (byte number from start of message)
      //resolution = din.readShort(); // dopplar velocity resolution

      hasReflectData = (reflect_gate_count > 0);
      hasDopplerData = (doppler_gate_count > 0);
    } else if(Cinrad2IOServiceProvider.isCC){
      //cinrad CC
      this.recno = record;
      // read header for every record
      //if(record == 0)
        readCCHeader(din);
      RADAR_DATA_SIZE = 3000;
      //FILE_HEADER_SIZE = 1021;
      message_type = 1;
      message_offset = (long) record * RADAR_DATA_SIZE + 1024;
      if (message_offset >= din.length())
        return;
      din.seek(message_offset);
      //System.out.println("record = " + record);
      radial_num = (short)(record % 512 + 1); // radial number within the elevation
      azimuth_ang = radial_num;

      elevation_num =(short)((record/512) + 1); // RDA elevation number
      //System.out.println("elevation num: " + elevation_num + " record " + record + " radial " + radial_num);
      if(elevation_num > sweepN)
        elevation_num = (short)sweepN;
      elevation_ang = (short)elev[elevation_num - 1];
      reflect_first_gate = 300; // range to first gate of reflectivity (m) may be negetive
      doppler_first_gate = 300; // range to first gate of dopplar (m) may be negetive
      reflect_gate_size = 150; // reflectivity data gate size (m)
      doppler_gate_size = 150; // dopplar data gate size (m)
      reflect_gate_count = 500; // number of reflectivity gates
      doppler_gate_count = 500; // number of velocity or spectrum width gates
      //vcp = sweepInfo[0].svcp;
      //cut           = din.readShort(); // sector number within cut
      //calibration   = din.readFloat(); // system gain calibration constant (db biased)
      reflect_offset =   0; // reflectivity data pointer (byte number from start of message)
      velocity_offset =  1000; // 9 velocity data pointer (byte number from start of message)
      spectWidth_offset = 2000; // 11 spectrum-width data pointer (byte number from start of message)
      //resolution = din.readShort(); // dopplar velocity resolution

      hasReflectData = (reflect_gate_count > 0);
      hasDopplerData = (doppler_gate_count > 0);
    } else if(Cinrad2IOServiceProvider.isCC20){
      //cinrad CC
      this.recno = record;
      // read header for every record
      // if(record == 0)
      readCC20Header(din);

      //if(record > recordNum[recordNum.length-1]-1)
      //  return;
      if(cDataForm == 24)
        RADAR_DATA_SIZE = 4011;
      else
        RADAR_DATA_SIZE = 3011;
      //FILE_HEADER_SIZE = 1021;
      message_type = 1;
      message_offset = (long) record * RADAR_DATA_SIZE + 2060;
      if (message_offset >= din.length())
        return;
      din.seek(message_offset);
      //System.out.println("record = " + record);
      elevation_ang =din.readShort(); // RDA elevation number
      azimuth_ang = din.readUnsignedShort();

      din.skipBytes(3);
      data_msecs = din.readInt();
     /* if (record < recordNum[0]) {
        radial_num = (short) (record +1);
        elevation_num = 0;
      } else {
        for (int i = 1; i < sweepN; i++) {
          if (record >= recordNum[i - 1] && record < recordNum[i]) {
            radial_num = (short) (record - recordNum[i - 1] + 1);
            elevation_num = (short) i;
            continue;
          }
        }
      }
      */
      //System.out.println(elevation_ang + " and " +azimuth_ang + " and " +record + " and " + data_msecs );
      reflect_first_gate = 0; // range to first gate of reflectivity (m) may be negetive
      doppler_first_gate = 0; // range to first gate of dopplar (m) may be negetive
      reflect_gate_size = 1500; // reflectivity data gate size (m)
      doppler_gate_size = 1500; // dopplar data gate size (m)
      reflect_gate_count = 1000; // number of reflectivity gates
      doppler_gate_count = 1000; // number of velocity or spectrum width gates
      //vcp = sweepInfo[0].svcp;
      //cut           = din.readShort(); // sector number within cut
      //calibration   = din.readFloat(); // system gain calibration constant (db biased)
      if(cDataForm == 24) {
        reflect_offset = 11; // reflectivity data pointer (byte number from start of message)
        velocity_offset = 2011; // 9 velocity data pointer (byte number from start of message)
        spectWidth_offset = 3011;
      } else {
        reflect_offset = 11; // reflectivity data pointer (byte number from start of message)
        velocity_offset = 1011; // 9 velocity data pointer (byte number from start of message)
        spectWidth_offset = 2011;
      }
      // 11 spectrum-width data pointer (byte number from start of message)
      //resolution = din.readShort(); // dopplar velocity resolution

      hasReflectData = (reflect_gate_count > 0);
      hasDopplerData = (doppler_gate_count > 0);
    }

  }

  public int findClosestIdx(int [] numbers, short myNumber){
    int distance = Math.abs(numbers[0] - myNumber);
    int idx = 0;
    for(int c = 1; c < numbers.length; c++){
      int cdistance = Math.abs(numbers[c] - myNumber);
      if(cdistance < distance){
        idx = c;
        distance = cdistance;
      }
    }
    return idx;
  }
    public static int bytesToInt(byte [] bytes, boolean swapBytes) {
       byte a = bytes[0];
       byte b = bytes[1];
       byte c = bytes[2];
       byte d = bytes[3];
       if (swapBytes) {
           return ((a & 0xff) ) +
               ((b & 0xff) << 8 ) +
               ((c & 0xff) << 16 ) +
               ((d & 0xff) << 24);
       } else {
           return ((a & 0xff) << 24 ) +
               ((b & 0xff) << 16 ) +
               ((c & 0xff) << 8 ) +
               ((d & 0xff) );
       }
    }

    public static int bytesToShort(byte [] bytes, boolean swapBytes) {
       byte a = bytes[0];
       byte b = bytes[1];

       if (swapBytes) {
           return ((a & 0xff) ) +
               ((b & 0xff) << 8 );

       } else {
           return ((a & 0xff) << 24 ) +
               ((b & 0xff) << 16 );

       }
    }

  public void dumpMessage(PrintStream out, java.util.Date d) {
    out.println(recno+" ---------------------");
    out.println(" message type = "+getMessageTypeName(message_type)+" ("+message_type+")");
    out.println(" message size = "+message_size+" segment="+seg_number+"/"+seg_count);
    out.println(" message date = "+ d.toString());
    out.println(" channel id = "+ id_channel );
  }

  public void dump(PrintStream out) {
    out.println(recno+" ------------------------------------------"+message_offset);
    out.println(" message type = "+getMessageTypeName(message_type));
    out.println(" data date = "+ getDate().toString());

    out.println(" elevation = "+getElevation()+" ("+elevation_num+")");
    out.println(" azimuth = "+getAzimuth());
    out.println(" radial = "+radial_num+" status= "+getRadialStatusName( radial_status)+
        " ratio = "+getAzimuth()/radial_num);
    out.println(" reflectivity first= "+reflect_first_gate+" size= "+reflect_gate_size+" count= "+reflect_gate_count);
    out.println(" doppler first= "+doppler_first_gate+" size= "+doppler_gate_size+" count= "+doppler_gate_count);
    out.println(" offset: reflect= "+reflect_offset+" velocity= "+velocity_offset+" spWidth= "+spectWidth_offset);
    out.println(" pattern = "+vcp );
  }

  public void dump2(PrintStream out) {
    out.println(recno+"= "+elevation_num+" size = "+message_size);
  }

  public boolean checkOk() {
    boolean ok = true;

    if (message_type != 1) return ok;

////    if ((seg_count != 1) || (seg_number != 1)) {
//      logger.warn("*** segment = "+seg_number+"/"+seg_count+who());
 //   }

    if ((reflect_offset < 0) || (reflect_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad reflect offset= "+reflect_offset+who());
      ok = false;
    }

    if ((velocity_offset < 0) || (velocity_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad velocity offset= "+velocity_offset+who());
      ok = false;
    }

    if ((spectWidth_offset < 0) || (spectWidth_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad spwidth offset= "+spectWidth_offset+who());
      ok = false;
    }

    if ((velocity_offset > 0) && (spectWidth_offset <= 0)) {
      logger.warn("****"+recno+ " HAS velocity NOT spectWidth!!"+who());
      ok = false;
    }

    if ((velocity_offset <= 0) && (spectWidth_offset > 0)) {
      logger.warn("****"+recno+ " HAS spectWidth AND NOT velocity!!"+who());
      ok = false;
    }

//    if (mess_julian_date != data_julian_date) {
//      logger.warn("*** message date = "+mess_julian_date+" : "+mess_msecs+who()+"\n"+
 //        " data date = "+data_julian_date+" : "+data_msecs);
//      ok = false;
//    }

    if (!hasReflectData && !hasDopplerData) {
      logger.info("*** no reflect or dopplar = "+who());
    }

    return ok;
  }

  private String who() { return " message("+recno +" "+ message_offset+")"; }


  /**
   * Get the azimuth in degrees
   *
   * @return   azimuth angle in degrees 0 = true north, 90 = east
   */
  public float getAzimuth() {
    if (message_type != 1) return -1.0f;

    if(Cinrad2IOServiceProvider.isSC)
      return 360.0f * azimuth_ang / 65536.0f;
    else if(Cinrad2IOServiceProvider.isCC)
      return 360.0f * azimuth_ang / 512.0f;
    else if(Cinrad2IOServiceProvider.isCC20)
      return  azimuth_ang * 0.01f;

    return 180.0f * azimuth_ang / 32768.0f;
  }

    /**
     * Get the elevation angle in degrees
     *
     * @return  elevation angle in degrees 0 = parellel to pedestal base, 90 = perpendicular
     */
    public float getElevation() {
      if (message_type != 1) return -1.0f;
      if(Cinrad2IOServiceProvider.isSC)
        return 120.0f * elevation_ang / 65536.0f;
      else if(Cinrad2IOServiceProvider.isCC)
        return elevation_ang * 0.01f;
      else if(Cinrad2IOServiceProvider.isCC20)
        return elevation_ang * 0.01f;

      return 180.0f * elevation_ang / 32768.0f;
    }


  /**
   * This method returns the gate size in meters
   */
  public int getGateSize(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
          return ((int) reflect_gate_size);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_gate_size);
    }
    return -1;
  }

  /**
   * This method returns the starting gate in meters
   */
  public int getGateStart(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
        return ((int) reflect_first_gate);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_first_gate);
    }
    return -1;
  }

  /**
   * This method returns the number of gates
   */
  public int getGateCount(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
          return ((int) reflect_gate_count);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_gate_count);
    }
    return 0;
  }

  private short getDataOffset(int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return reflect_offset;
      case VELOCITY_HI :
      case VELOCITY_LOW : return velocity_offset;
      case SPECTRUM_WIDTH : return spectWidth_offset;
    }
    return Short.MIN_VALUE;
  }

  public java.util.Date getDate() {
    if(Cinrad2IOServiceProvider.isSC || Cinrad2IOServiceProvider.isCC)
      return dateTime0.toDate();
    else if(Cinrad2IOServiceProvider.isCC20)
      return dateTime0.toDate();
    else
      return getDate( data_julian_date, data_msecs);
  }

  /**
   * Read data from this record.
   * @param raf read from this file
   * @param datatype which data type we want
   * @param gateRange handles the possible subset of data to return
   * @param ii put the data here
   * @throws IOException
   */
  public void readData(RandomAccessFile raf, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    long offset = message_offset;
    offset += MESSAGE_HEADER_SIZE; // offset is from "start of digital radar data message header"
    offset += getDataOffset( datatype);
    raf.seek(offset);
    if (logger.isDebugEnabled()) {
      logger.debug("  read recno "+recno+" at offset "+offset+" count= "+getGateCount(datatype));
      logger.debug("   offset: reflect= "+reflect_offset+" velocity= "+velocity_offset+" spWidth= "+spectWidth_offset);
    }

    int dataCount = getGateCount( datatype);
    byte[] data = new byte[dataCount];
    raf.readFully(data);

    for (int gateIdx : gateRange) {
      if (gateIdx >= dataCount)
        ii.setByteNext(MISSING_DATA);
      else
        ii.setByteNext(data[gateIdx]);
    }

  }


  public void readData0(RandomAccessFile raf, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    long offset = message_offset;
    offset += MESSAGE_HEADER_SIZE; // offset is from "start of digital radar data message header"
    offset += getDataOffset( datatype);
    raf.seek(offset);
    if (logger.isDebugEnabled()) {
      logger.debug("  read recno "+recno+" at offset "+offset+" count= "+getGateCount(datatype));
      logger.debug("   offset: reflect= "+reflect_offset+" velocity= "+velocity_offset+" spWidth= "+spectWidth_offset);
    }

    int dataCount = getGateCount( datatype);
    byte[] data = new byte[dataCount];
    byte[] b4 = new byte[4];
    int j = 0;
    if(datatype == REFLECTIVITY)
      j = 0;
    else if(datatype == VELOCITY_LOW)
      j = 1;
    else if(datatype == SPECTRUM_WIDTH)
      j = 3;

    //raf.readFully(data);

    for (int gateIdx : gateRange) {
      if (gateIdx >= dataCount)
        ii.setByteNext(MISSING_DATA);
      else {
        raf.read(b4);
        data[gateIdx] = b4[j];
        ii.setByteNext(data[gateIdx]);
      }
    }

  }

  public void readData1(RandomAccessFile raf, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    long offset = message_offset;
    offset += MESSAGE_HEADER_SIZE; // offset is from "start of digital radar data message header"
    offset += getDataOffset( datatype);
    raf.seek(offset);
    if (logger.isDebugEnabled()) {
      logger.debug("  read recno "+recno+" at offset "+offset+" count= "+getGateCount(datatype));
      logger.debug("   offset: reflect= "+reflect_offset+" velocity= "+velocity_offset+" spWidth= "+spectWidth_offset);
    }

    int dataCount = getGateCount( datatype );
    short[] data = new short[dataCount];
    raf.readShort(data, 0, dataCount);

    for (int idx : gateRange) {
      if (idx >= dataCount)
        ii.setShortNext((short)-32768);
      else
        ii.setShortNext(data[idx]);
    }

  }
 /**
   * Instances which have same content are equal.
   *
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Cinrad2Record)) return false;
    return hashCode() == oo.hashCode();
  }
  /** Override Object.hashCode() to implement equals. *
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + elevation_num;
      //result = 37*result + cut;
      //result = 37*result + datatype;
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;  */

  public String toString() {
    return "elev= "+elevation_num+" radial_num = "+radial_num;
  }

  static class SweepInfo {
    byte amb;
    short arotate;
    short pref1;
    short pref2;
    short spulseW;
    short maxV;
    short maxL;
    short binWidth;
    short binnumber;
    short recordnumber;
    float elevationAngle;

    SweepInfo(RandomAccessFile din, int hoff ) throws IOException {
      din.seek(hoff);
      amb = din.readByte();
      arotate = (short)din.readUnsignedShort();
      pref1 = (short)din.readUnsignedShort();
      pref2 = (short)din.readUnsignedShort();
      spulseW = (short)din.readUnsignedShort();
      maxV = (short)din.readUnsignedShort();
      maxL = (short)din.readUnsignedShort();
      binWidth = (short)din.readUnsignedShort();
      binnumber = (short)din.readUnsignedShort();
      recordnumber = (short)din.readUnsignedShort();
      elevationAngle  = (short)din.readUnsignedShort()/100.0f;
    }
  }
}