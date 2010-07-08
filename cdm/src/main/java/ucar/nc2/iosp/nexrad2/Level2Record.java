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


package ucar.nc2.iosp.nexrad2;

import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;


/**
 * This class reads one record (radial) in an NEXRAD level II file.
 * File must be uncompressed.
 * Not handling messages yet, only data.
 * <p>
 * 10/16/05: Now returns data as a byte, so use scale and offset.
 *
 * Adapted with permission from the Java Iras software developed by David Priegnitz at NSSL.
 *
 * @author caron
 * @author David Priegnitz
 */
public class Level2Record {

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

 /* added for high resolution message type 31 */

  public static final int REFLECTIVITY_HIGH = 5;

  /** High Resolution Radial Velocity moment identifier */
  public static final int VELOCITY_HIGH = 6;

  /** High Resolution Sprectrum Width moment identifier */
  public static final int SPECTRUM_WIDTH_HIGH = 7;

  /** High Resolution Radial Velocity moment identifier */
  public static final int DIFF_REFLECTIVITY_HIGH = 8;

  /** High Resolution Radial Velocity moment identifier */
  public static final int DIFF_PHASE = 9;

  /** High Resolution Sprectrum Width moment identifier */
  public static final int CORRELATION_COEFFICIENT = 10;

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

  public static final byte MISSING_DATA = (byte) 1;
  public static final byte BELOW_THRESHOLD = (byte) 0;

  /** Size of the file header, aka title */
  static final int FILE_HEADER_SIZE = 24;

  /** Size of the CTM record header */
  private static final int CTM_HEADER_SIZE = 12;

  /** Size of the the message header, to start of the data message */
  private static final int MESSAGE_HEADER_SIZE = 28;

  /** Size of the entire message, if its a radar data message */
  private static final int RADAR_DATA_SIZE = 2432;

  static public String getDatatypeName( int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return "Reflectivity";
      case VELOCITY_HI :
      case VELOCITY_LOW : return "RadialVelocity";
      case SPECTRUM_WIDTH : return "SpectrumWidth";
      case REFLECTIVITY_HIGH : return "Reflectivity_HI";
      case VELOCITY_HIGH : return "RadialVelocity_HI";
      case SPECTRUM_WIDTH_HIGH : return "SpectrumWidth_HI";
      case DIFF_REFLECTIVITY_HIGH : return "Reflectivity_DIFF";
      case DIFF_PHASE : return "Phase";
      case CORRELATION_COEFFICIENT : return "RHO";


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

       case REFLECTIVITY_HIGH :
           return "dBz";
       case DIFF_REFLECTIVITY_HIGH :
           return "dBz";

       case VELOCITY_HIGH : 
       case SPECTRUM_WIDTH_HIGH :
           return "m/s";

       case DIFF_PHASE :
           return "deg";

       case CORRELATION_COEFFICIENT :
           return "N/A";
     }
     throw new IllegalArgumentException();
   }
    
   public short getDatatypeSNRThreshhold(int datatype) {
    switch (datatype) {
        case REFLECTIVITY_HIGH : return ref_snr_threshold;
        case VELOCITY_HIGH : return vel_snr_threshold;
        case SPECTRUM_WIDTH_HIGH : return sw_snr_threshold;
        case DIFF_REFLECTIVITY_HIGH : return zdrHR_snr_threshold;
        case DIFF_PHASE : return phiHR_snr_threshold;
        case CORRELATION_COEFFICIENT : return rhoHR_snr_threshold;
      default : throw new IllegalArgumentException();
    }
   }    

   public short getDatatypeRangeFoldingThreshhold(int datatype) {
    switch (datatype) {
        case REFLECTIVITY_HIGH : return ref_rf_threshold;
        case VELOCITY_HIGH : return vel_rf_threshold;
        case SPECTRUM_WIDTH_HIGH : return sw_rf_threshold;
        case REFLECTIVITY :
        case VELOCITY_LOW :
        case VELOCITY_HI :
        case SPECTRUM_WIDTH :  return threshhold;
        case DIFF_REFLECTIVITY_HIGH : return zdrHR_rf_threshold;
        case DIFF_PHASE : return phiHR_rf_threshold;
        case CORRELATION_COEFFICIENT : return rhoHR_rf_threshold;

      default : throw new IllegalArgumentException();
    }
   }

   public float getDatatypeScaleFactor(int datatype) {
    switch (datatype) {
      case REFLECTIVITY: return 0.5f;
      case VELOCITY_LOW : return 1.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH : return 0.5f;
        case REFLECTIVITY_HIGH : return 1/reflectHR_scale;
        case VELOCITY_HIGH : return 1/velocityHR_scale;
        case SPECTRUM_WIDTH_HIGH : return 1/spectrumHR_scale;
        case DIFF_REFLECTIVITY_HIGH : return 1.0f/zdrHR_scale;
        case DIFF_PHASE :  return 1.0f/phiHR_scale;
        case CORRELATION_COEFFICIENT : return 1.0f/rhoHR_scale;

      default : throw new IllegalArgumentException();
    }
  }

  public float getDatatypeAddOffset(int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return -33.0f;
      case VELOCITY_LOW : return -129.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH : return -64.5f;
      case REFLECTIVITY_HIGH : return reflectHR_addoffset*(-1)/reflectHR_scale;
      case VELOCITY_HIGH : return velocityHR_addoffset*(-1)/velocityHR_scale;
      case SPECTRUM_WIDTH_HIGH : return spectrumHR_addoffset*(-1)/spectrumHR_scale;
      case DIFF_REFLECTIVITY_HIGH : return zdrHR_addoffset*(-1)/zdrHR_scale;
      case DIFF_PHASE : return phiHR_addoffset*(-1)/phiHR_scale;
      case CORRELATION_COEFFICIENT : return rhoHR_addoffset*(-1)/rhoHR_scale;

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
      case 31: return "Digitail Radar Data Generic Format";
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
      case 211: return "14 elevations, 16 scans every 5 mins";
      case 212: return "14 elevations, 17 scans every 4 mins";
      case 221: return "9 elevations, 11 scans every 5 minutes";
      default : return "unknown "+code;
    }
  }

  static public java.util.Date getDate(int julianDays, int msecs) {
    long total = ((long) (julianDays - 1)) * 24 * 3600 * 1000 + msecs;
    return new Date( total);
  }

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Level2Record.class);

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  int recno;        //  record number within the file
  long message_offset; // offset of start of message

  boolean hasReflectData, hasDopplerData;
  boolean hasHighResREFData;
  boolean hasHighResVELData;
  boolean hasHighResSWData  ;
  boolean hasHighResZDRData  ;
  boolean hasHighResPHIData  ;
  boolean hasHighResRHOData ;
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
  short radial_num = 0; // radial number within the elevation : starts with one
  short radial_status = 0;
  short elevation_ang = 0;
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
  short ref_snr_threshold; // reflectivity signal to noise threshhold
  short vel_snr_threshold;
  short sw_snr_threshold;
  short zdrHR_snr_threshold;
  short phiHR_snr_threshold;
  short rhoHR_snr_threshold;
  short ref_rf_threshold; // reflectivity range folding threshhold
  short vel_rf_threshold;
  short sw_rf_threshold;
  short zdrHR_rf_threshold;
  short phiHR_rf_threshold;
  short rhoHR_rf_threshold;

  private short reflect_offset; // reflectivity data pointer (byte number from start of message)
  private short velocity_offset; // velocity data pointer (byte number from start of message)
  private short spectWidth_offset; // spectrum-width data pointer (byte number from start of message)
  // new addition for message type 31
  short rlength = 0;
  String id;
  float azimuth;
  byte compressIdx;
  byte sp;
  byte ars;
  byte rs;
  float elevation;
  byte rsbs;
  byte aim;
  short dcount;

  int dbp1;
  int dbp2;
  int dbp3;
  int dbp4;
  int dbp5;
  int dbp6;
  int dbp7;
  int dbp8;
  int dbp9;
  short reflectHR_gate_count = 0;
  short velocityHR_gate_count = 0;
  short spectrumHR_gate_count = 0;
  float reflectHR_scale = 0;
  float velocityHR_scale = 0;
  float spectrumHR_scale = 0;
  float zdrHR_scale = 0;
  float phiHR_scale = 0;
  float rhoHR_scale = 0;
  float reflectHR_addoffset = 0;
  float velocityHR_addoffset = 0;
  float spectrumHR_addoffset = 0;
  float zdrHR_addoffset = 0;
  float phiHR_addoffset = 0;
  float rhoHR_addoffset = 0;
  short reflectHR_offset = 0;
  short velocityHR_offset = 0;
  short spectrumHR_offset = 0;
  short zdrHR_offset = 0;
  short phiHR_offset = 0;
  short rhoHR_offset = 0;
  short zdrHR_gate_count = 0;
  short phiHR_gate_count = 0;
  short rhoHR_gate_count = 0;
  short reflectHR_gate_size = 0;
  short velocityHR_gate_size = 0;
  short spectrumHR_gate_size = 0;
  short zdrHR_gate_size = 0;
  short phiHR_gate_size = 0;
  short rhoHR_gate_size = 0;
  short reflectHR_first_gate = 0;
  short velocityHR_first_gate = 0;
  short spectrumHR_first_gate = 0;
  short zdrHR_first_gate = 0;
  short phiHR_first_gate = 0;
  short rhoHR_first_gate = 0;




  public static Level2Record factory(RandomAccessFile din, int record, long message_offset31) throws IOException {
    long offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE +  message_offset31;
    if (offset >= din.length())
      return null;
    else
      return new Level2Record(din, record, message_offset31);
  }

  public Level2Record(RandomAccessFile din, int record, long message_offset31) throws IOException {

    this.recno = record;
    message_offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE + message_offset31;
    din.seek(message_offset);
    din.skipBytes(CTM_HEADER_SIZE);

    // Message Header
     // int size = din.readInt();
    message_size  = din.readShort(); // size in "halfwords" = 2 bytes
    id_channel    = din.readByte(); // channel id
    message_type  = din.readByte();
    id_sequence   = din.readShort();
    mess_julian_date   = din.readShort(); // from 1/1/70; prob "message generation time"
    mess_msecs    = din.readInt();   // message generation time
    seg_count     = din.readShort(); // number of message segments
    seg_number    = din.readShort(); // this segment

   // if (message_type != 1 ) return;
    if(message_type == 1) {
    // data header
        data_msecs    = din.readInt();   // collection time for this radial, msecs since midnight
        data_julian_date  = din.readShort(); // prob "collection time"
        unamb_range   = din.readShort(); // unambiguous range
        azimuth_ang   = din.readUnsignedShort(); // LOOK why unsigned ??
        radial_num   = din.readShort(); // radial number within the elevation
        radial_status = din.readShort();
        elevation_ang = din.readShort();
        elevation_num = din.readShort(); // RDA elevation number
        reflect_first_gate = din.readShort(); // range to first gate of reflectivity (m) may be negetive
        doppler_first_gate = din.readShort(); // range to first gate of dopplar (m) may be negetive
        reflect_gate_size = din.readShort(); // reflectivity data gate size (m)
        doppler_gate_size = din.readShort(); // dopplar data gate size (m)
        reflect_gate_count = din.readShort(); // number of reflectivity gates
        doppler_gate_count = din.readShort(); // number of velocity or spectrum width gates
        cut           = din.readShort(); // sector number within cut
        calibration   = din.readFloat(); // system gain calibration constant (db biased)
        reflect_offset  = din.readShort(); // reflectivity data pointer (byte number from start of message)
        velocity_offset   = din.readShort(); // velocity data pointer (byte number from start of message)
        spectWidth_offset = din.readShort(); // spectrum-width data pointer (byte number from start of message)
        resolution    = din.readShort(); // dopplar velocity resolution
        vcp           = din.readShort(); // volume coverage pattern

        din.skipBytes(14);

        nyquist_vel   = din.readShort(); // nyquist velocity
        attenuation   = din.readShort(); // atmospheric attenuation factor
        threshhold    = din.readShort(); // threshhold paramter for minimum difference

        hasReflectData = (reflect_gate_count > 0);
        hasDopplerData = (doppler_gate_count > 0);
        return;
    }

    else if(message_type == 31) {
    // data header
        id = din.readString(4);
        data_msecs    = din.readInt();   // collection time for this radial, msecs since midnight
        data_julian_date  = din.readShort(); // prob "collection time"
        radial_num   = din.readShort(); // radial number within the elevation
        azimuth   = din.readFloat(); // LOOK why unsigned ??
        compressIdx = din.readByte();
        sp = din.readByte();
        rlength = din.readShort();
        ars = din.readByte();
        rs = din.readByte();
        elevation_num = din.readByte(); // RDA elevation number
        cut           = din.readByte(); // sector number within cut
        elevation =  din.readFloat();
        rsbs = din.readByte();
        aim = din.readByte();
        dcount = din.readShort();

        dbp1 = din.readInt();
        dbp2 = din.readInt();
        dbp3 = din.readInt();
        dbp4 = din.readInt();
        dbp5 = din.readInt();
        dbp6 = din.readInt();
        dbp7 = din.readInt();
        dbp8 = din.readInt();
        dbp9 = din.readInt();

        vcp = getDataBlockValue(din, (short) dbp1, 40);
        int dbpp4 = 0;
        int dbpp5 = 0;
        int dbpp6 = 0;
        if(dbp4 > 0) {
            String tname =    getDataBlockStringValue(din, (short) dbp4, 1,3);
            if(tname.startsWith("REF")) {
                hasHighResREFData = true;
                dbpp4 = dbp4;
            } else if (tname.startsWith("VEL")) {
                hasHighResVELData = true;
                dbpp5 = dbp4;
            } else if (tname.startsWith("SW")) {
                hasHighResSWData = true;
                dbpp6 = dbp4;
            }

        }
        if(dbp5 > 0) {

            String tname =    getDataBlockStringValue(din, (short) dbp5, 1,3);
            if(tname.startsWith("REF")) {
                hasHighResREFData = true;
                dbpp4 = dbp5;
            } else if (tname.startsWith("VEL")) {
                hasHighResVELData = true;
                dbpp5 = dbp5;
            } else if (tname.startsWith("SW")) {
                hasHighResSWData = true;
                dbpp6 = dbp5;
            }
        }
        if(dbp6 > 0) {

            String tname =    getDataBlockStringValue(din, (short) dbp6, 1,3);
            if(tname.startsWith("REF")) {
                hasHighResREFData = true;
                dbpp4 = dbp6;
            } else if (tname.startsWith("VEL")) {
                hasHighResVELData = true;
                dbpp5 = dbp6;
            } else if (tname.startsWith("SW")) {
                hasHighResSWData = true;
                dbpp6 = dbp6;
            }
        }

        //hasHighResREFData = (dbp4 > 0);

        if(hasHighResREFData ) {
            reflectHR_gate_count = getDataBlockValue(din, (short) dbpp4, 8);
            reflectHR_first_gate = getDataBlockValue(din, (short) dbpp4, 10);
            reflectHR_gate_size = getDataBlockValue(din, (short) dbpp4, 12);
            ref_rf_threshold = getDataBlockValue(din, (short) dbpp4, 14);
            ref_snr_threshold = getDataBlockValue(din, (short) dbpp4, 16);
            reflectHR_scale = getDataBlockValue1(din, (short) dbpp4, 20);
            reflectHR_addoffset = getDataBlockValue1(din, (short) dbpp4, 24);
            reflectHR_offset =   (short)( dbpp4+ 28);

        }
        //hasHighResVELData = (dbp5 > 0);
        if(hasHighResVELData) {

            velocityHR_gate_count = getDataBlockValue(din, (short) dbpp5, 8);
            velocityHR_first_gate = getDataBlockValue(din, (short) dbpp5, 10);
            velocityHR_gate_size = getDataBlockValue(din, (short) dbpp5, 12);
            vel_rf_threshold = getDataBlockValue(din, (short) dbpp5, 14);
            vel_snr_threshold = getDataBlockValue(din, (short) dbpp5, 16);
            velocityHR_scale = getDataBlockValue1(din, (short) dbpp5, 20);
            velocityHR_addoffset = getDataBlockValue1(din, (short) dbpp5, 24);
            velocityHR_offset = (short)( dbpp5+ 28);

        }
       // hasHighResSWData  = (dbp6 > 0);
        if(hasHighResSWData) {

            spectrumHR_gate_count = getDataBlockValue(din, (short) dbpp6, 8);
            spectrumHR_first_gate = getDataBlockValue(din, (short) dbpp6, 10);
            spectrumHR_gate_size = getDataBlockValue(din, (short) dbpp6, 12);
            sw_rf_threshold = getDataBlockValue(din, (short) dbpp6, 14);
            sw_snr_threshold = getDataBlockValue(din, (short) dbpp6, 16);
            spectrumHR_scale = getDataBlockValue1(din, (short) dbpp6, 20);
            spectrumHR_addoffset = getDataBlockValue1(din, (short) dbpp6, 24);
            spectrumHR_offset = (short) (dbpp6 + 28);

        }
        hasHighResZDRData = (dbp7 > 0);
        if(hasHighResZDRData) {
            zdrHR_gate_count = getDataBlockValue(din, (short) dbp7, 8);
            zdrHR_first_gate = getDataBlockValue(din, (short) dbp7, 10);
            zdrHR_gate_size = getDataBlockValue(din, (short) dbp7, 12);
            zdrHR_rf_threshold = getDataBlockValue(din, (short) dbpp6, 14);
            zdrHR_snr_threshold = getDataBlockValue(din, (short) dbpp6, 16);
            zdrHR_scale = getDataBlockValue1(din, (short) dbpp6, 20);
            zdrHR_addoffset = getDataBlockValue1(din, (short) dbpp6, 24);
            zdrHR_offset = (short) (dbpp6 + 28);
        }
        hasHighResPHIData = (dbp8 > 0);
        if(hasHighResPHIData) {
            phiHR_gate_count = getDataBlockValue(din, (short) dbp8, 8);
            phiHR_first_gate = getDataBlockValue(din, (short) dbp8, 10);
            phiHR_gate_size = getDataBlockValue(din, (short) dbp8, 12);
            phiHR_rf_threshold = getDataBlockValue(din, (short) dbpp6, 14);
            phiHR_snr_threshold = getDataBlockValue(din, (short) dbpp6, 16);
            phiHR_scale = getDataBlockValue1(din, (short) dbpp6, 20);
            phiHR_addoffset = getDataBlockValue1(din, (short) dbpp6, 24);
            phiHR_offset = (short) (dbpp6 + 28);
        }
        hasHighResRHOData = (dbp9 > 0);
        if(hasHighResRHOData)  {
            rhoHR_gate_count = getDataBlockValue(din, (short) dbp9, 8);
            rhoHR_first_gate = getDataBlockValue(din, (short) dbp9, 10);
            rhoHR_gate_size = getDataBlockValue(din, (short) dbp9, 12);
            rhoHR_rf_threshold = getDataBlockValue(din, (short) dbpp6, 14);
            rhoHR_snr_threshold = getDataBlockValue(din, (short) dbpp6, 16);
            rhoHR_scale = getDataBlockValue1(din, (short) dbpp6, 20);
            rhoHR_addoffset = getDataBlockValue1(din, (short) dbpp6, 24);
            rhoHR_offset = (short) (dbpp6 + 28);
        }

        return;
    }
    else
        return;

  }

  public void dumpMessage(PrintStream out) {
    out.println(recno+" ---------------------");
    out.println(" message type = "+getMessageTypeName(message_type)+" ("+message_type+")");
    out.println(" message size = "+message_size+" segment="+seg_number+"/"+seg_count);
  }

  public void dump(PrintStream out) {
    out.println(recno+" ------------------------------------------"+message_offset);
    out.println(" message type = "+getMessageTypeName(message_type));
    out.println(" data date = "+data_julian_date+" : "+data_msecs);
    out.println(" elevation = "+getElevation()+" ("+elevation_num+")");
    out.println(" azimuth = "+getAzimuth());
    out.println(" radial = "+radial_num+" status= "+getRadialStatusName( radial_status)+
        " ratio = "+getAzimuth()/radial_num);
    out.println(" reflectivity first= "+reflect_first_gate+" size= "+reflect_gate_size+" count= "+reflect_gate_count);
    out.println(" doppler first= "+doppler_first_gate+" size= "+doppler_gate_size+" count= "+doppler_gate_count);
    out.println(" offset: reflect= "+reflect_offset+" velocity= "+velocity_offset+" spWidth= "+spectWidth_offset);
    out.println(" pattern = "+vcp+" cut= "+cut);
  }

  public void dump2(PrintStream out) {
    out.println("recno= " + recno+ " massType= "+message_type+" massSize = "+message_size);
  }

  public boolean checkOk() {
    boolean ok = true;

      if ( Float.isNaN(getAzimuth()) ) {
        logger.warn("****"+recno+ " HAS bad azimuth value = "+ azimuth_ang);
        ok = false;
      }

    if (message_type != 1) return ok;

    if ((seg_count != 1) || (seg_number != 1)) {
      logger.warn("*** segment = "+seg_number+"/"+seg_count+who());
    }

    if ((reflect_offset < 0) || (reflect_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad reflect offset= "+reflect_offset+who());
      ok = false;
    }

    if ((velocity_offset < 0) || (velocity_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad velocity offset= "+velocity_offset+who());
      ok = false;
    }

    if ((spectWidth_offset < 0) || (spectWidth_offset > RADAR_DATA_SIZE)) {
      logger.warn("****"+recno+ " HAS bad spwidth offset= "+reflect_offset+who());
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

    if (mess_julian_date != data_julian_date) {
      logger.warn("*** message date = "+mess_julian_date+" : "+mess_msecs+who()+"\n"+
         " data date = "+data_julian_date+" : "+data_msecs);
      ok = false;
    }

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
      if( message_type == 31)
        return azimuth;
      else if (message_type == 1)
        return 180.0f * azimuth_ang / 32768.0f;
      else
        return -1.0f;
  }

    /**
     * Get the elevation angle in degrees
     *
     * @return  elevation angle in degrees 0 = parellel to pedestal base, 90 = perpendicular
     */
    public float getElevation() {
      if( message_type == 31)
        return elevation;
      else if (message_type == 1)
        return 180.0f * elevation_ang / 32768.0f;
      else
        return -1.0f;
    }


  /**
   * This method returns the gate size in meters
   * @param datatype which type of data : REFLECTIVITY, VELOCITY_HI, VELOCITY_LO, SPECTRUM_WIDTH
   * @return the gate size in meters
   */
  public int getGateSize(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
          return ((int) reflect_gate_size);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_gate_size);
      //high resolution
      case  REFLECTIVITY_HIGH :
          return ((int) reflectHR_gate_size);
      case   VELOCITY_HIGH :
          return ((int) velocityHR_gate_size);
      case   SPECTRUM_WIDTH_HIGH :
          return ((int) spectrumHR_gate_size);
      case   DIFF_REFLECTIVITY_HIGH :
          return ((int) zdrHR_gate_size);
      case   DIFF_PHASE :
          return ((int) phiHR_gate_size);
      case   CORRELATION_COEFFICIENT :
          return ((int) rhoHR_gate_size);

    }
    return -1;
  }

  /**
   * This method returns the starting gate in meters
   * @param datatype which type of data : REFLECTIVITY, VELOCITY_HI, VELOCITY_LO, SPECTRUM_WIDTH
   * @return the starting gate in meters
   */
  public int getGateStart(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
        return ((int) reflect_first_gate);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_first_gate);
      //high resolution
      case  REFLECTIVITY_HIGH :
          return ((int) reflectHR_first_gate);
      case   VELOCITY_HIGH :
          return ((int) velocityHR_first_gate);
      case   SPECTRUM_WIDTH_HIGH :
          return ((int) spectrumHR_first_gate);
      case   DIFF_REFLECTIVITY_HIGH :
          return ((int) zdrHR_first_gate);
      case   DIFF_PHASE :
          return ((int) phiHR_first_gate);
      case   CORRELATION_COEFFICIENT :
          return ((int) rhoHR_first_gate);

    }
    return -1;
  }

  /**
   * This method returns the number of gates
   * @param datatype which type of data : REFLECTIVITY, VELOCITY_HI, VELOCITY_LO, SPECTRUM_WIDTH
   * @return the number of gates
   */
  public int getGateCount(int datatype) {
    switch (datatype) {
      case REFLECTIVITY :
          return ((int) reflect_gate_count);

      case VELOCITY_HI :
      case VELOCITY_LOW :
      case SPECTRUM_WIDTH :
          return ((int) doppler_gate_count);
      // hight resolution
      case  REFLECTIVITY_HIGH :
          return ((int) reflectHR_gate_count);
      case   VELOCITY_HIGH :
          return ((int) velocityHR_gate_count);
      case   SPECTRUM_WIDTH_HIGH :
          return ((int) spectrumHR_gate_count);
      case   DIFF_REFLECTIVITY_HIGH :
          return ((int) zdrHR_gate_count);
      case   DIFF_PHASE :
          return ((int) phiHR_gate_count);
      case   CORRELATION_COEFFICIENT :
          return ((int) rhoHR_gate_count);
    }
    return 0;
  }

  private short getDataOffset(int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return reflect_offset;
      case VELOCITY_HI :
      case VELOCITY_LOW : return velocity_offset;
      case SPECTRUM_WIDTH : return spectWidth_offset;
      case REFLECTIVITY_HIGH : return reflectHR_offset;
      case VELOCITY_HIGH : return velocityHR_offset;
      case SPECTRUM_WIDTH_HIGH : return spectrumHR_offset;
      case DIFF_REFLECTIVITY_HIGH : return (short)dbp7;
      case DIFF_PHASE : return (short)dbp8;
      case CORRELATION_COEFFICIENT : return (short)dbp9;
    }
    return Short.MIN_VALUE;
  }

  private short getDataBlockValue(RandomAccessFile raf, short offset, int skip) throws IOException {
      long off = offset + message_offset + MESSAGE_HEADER_SIZE;
      raf.seek(off);
      raf.skipBytes(skip);
      return  raf.readShort();
  }
  private String getDataBlockStringValue(RandomAccessFile raf, short offset, int skip, int size) throws IOException {
      long off = offset + message_offset + MESSAGE_HEADER_SIZE;
      raf.seek(off);
      raf.skipBytes(skip);
      return  raf.readString(size);
  }
  private float getDataBlockValue1(RandomAccessFile raf, short offset, int skip) throws IOException {
      long off = offset + message_offset + MESSAGE_HEADER_SIZE;
      raf.seek(off);
      raf.skipBytes(skip);
      return  raf.readFloat();
  }
  public java.util.Date getDate() {
    return getDate( data_julian_date, data_msecs);
  }

  /**
   * Read data from this record.
   * @param raf read from this file
   * @param datatype which type of data : REFLECTIVITY, VELOCITY_HI, VELOCITY_LO, SPECTRUM_WIDTH
   * @param gateRange handles the possible subset of data to return
   * @param ii put the data here
   * @throws IOException on read error
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
    short [] ds = convertunsignedByte2Short(data);
    for (int i = gateRange.first(); i <= gateRange.last(); i += gateRange.stride()) {
      if (i >= dataCount)
        ii.setByteNext(MISSING_DATA);
      else
        ii.setByteNext(data[i]);
    }

  }

 /**
   * Instances which have same content are equal.
   *
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if ( !(oo instanceof Level2Record)) return false;
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
  public short[] convertunsignedByte2Short(byte[] inb) {
     int len = inb.length;
     short [] outs = new short[len];
     int i = 0;
     for(byte b: inb){
         outs[i++] = convertunsignedByte2Short(b);
     }
    return outs;
  }
  public short convertunsignedByte2Short(byte b) {
    return (short) ((b < 0) ? (short) b + 256 : (short) b);
  }

  public String toString() {
    return "elev= "+elevation_num+" radial_num = "+radial_num;
  }

}
