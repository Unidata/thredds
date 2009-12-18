/*
 * $Id:Cinrad2Record.java 63 2006-07-12 21:50:51Z edavis $
 *
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


package ucar.nc2.iosp.cinrad;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.units.DateUnit;
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

  public static final byte MISSING_DATA = (byte) 1;
  public static final byte BELOW_THRESHOLD = (byte) 0;

  /** Size of the file header, aka title */
  static final int FILE_HEADER_SIZE = 0;

  /** Size of the CTM record header */
  private static final int CTM_HEADER_SIZE = 14;

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
      case REFLECTIVITY: return 0.5f;
      case VELOCITY_LOW : return 1.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH : return 0.5f;
      default : throw new IllegalArgumentException();
    }
  }

  static public float getDatatypeAddOffset(int datatype) {
    switch (datatype) {
      case REFLECTIVITY : return -33.0f;
      case VELOCITY_LOW : return -129.0f;
      case VELOCITY_HI:
      case SPECTRUM_WIDTH : return -64.5f;
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

  private short reflect_offset; // reflectivity data pointer (byte number from start of message)
  private short velocity_offset; // velocity data pointer (byte number from start of message)
  private short spectWidth_offset; // spectrum-width data pointer (byte number from start of message)

  public static Cinrad2Record factory(RandomAccessFile din, int record) throws IOException {
    long offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
    if (offset >= din.length())
      return null;
    else
      return new Cinrad2Record(din, record);
  }
    int getUInt( byte[] b, int num )
  {
      int            base=1;
      int            i;
      int            word=0;

      int bv[] = new int[num];

      for (i = 0; i<num; i++ )
      {
        bv[i] = convertunsignedByte2Short(b[i]);
      }

      /*
      ** Calculate the integer value of the byte sequence
      */

      for ( i = num-1; i >= 0; i-- ) {
        word += base * b[i];
        base *= 256;
      }

      return word;
  }
    public short convertunsignedByte2Short(byte b)
     {
        return (short)((b<0)? (short)b + 256 : (short)b);
     }

  public Cinrad2Record(RandomAccessFile din, int record) throws IOException {

    this.recno = record;
    message_offset = record * RADAR_DATA_SIZE + FILE_HEADER_SIZE;
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
    message_type  = din.readByte();
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
    data_msecs    = bytesToInt(b4, true); //din.readInt();   // collection time for this radial, msecs since midnight
    byte[] b2 = din.readBytes(2);
    data_julian_date  = (short)bytesToShort(b2, true); //din.readShort(); // prob "collection time"
      java.util.Date dd =  getDate();
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
      din.skipBytes(6);
    //cut           = din.readShort(); // sector number within cut
    //calibration   = din.readFloat(); // system gain calibration constant (db biased)
    reflect_offset  = din.readShort(); // reflectivity data pointer (byte number from start of message)
    velocity_offset   = din.readShort(); // velocity data pointer (byte number from start of message)
    spectWidth_offset = din.readShort(); // spectrum-width data pointer (byte number from start of message)
    resolution    = din.readShort(); // dopplar velocity resolution
    vcp           = din.readShort(); // volume coverage pattern

    din.skipBytes(14);

    nyquist_vel   = din.readShort(); // nyquist velocity
     din.skipBytes(38);
    //attenuation   = din.readShort(); // atmospheric attenuation factor
    //threshhold    = din.readShort(); // threshhold paramter for minimum difference

    hasReflectData = (reflect_gate_count > 0);
    hasDopplerData = (doppler_gate_count > 0);
    //  dump(System.out);
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
    return 180.0f * azimuth_ang / 32768.0f;
  }

    /**
     * Get the elevation angle in degrees
     *
     * @return  elevation angle in degrees 0 = parellel to pedestal base, 90 = perpendicular
     */
    public float getElevation() {
      if (message_type != 1) return -1.0f;
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

}
