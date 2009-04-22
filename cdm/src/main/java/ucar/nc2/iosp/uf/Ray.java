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
package ucar.nc2.iosp.uf;

import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Range;
import ucar.ma2.IndexIterator;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 3, 2008
 * Time: 1:30:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class Ray {

    int raySize;
    long rayOffset;
    static final int UF_MANDATORY_HEADER2_LEN = 90;
    static final int UF_FIELD_HEADER2_LEN = 50;
    static final boolean littleEndianData = true;
    boolean debug = false;

     /**   moment identifier */
    long data_msecs = 0;

    UF_mandatory_header2 uf_header2;
    UF_optional_header uf_opt_header;
    short      numberOfFields;   // in this ray
    short      numberOfRecords;  // in this ray
    short      numberOfFieldsInRecord;   // in this record

    HashMap<String, UF_field_header2>  field_header_map;


    public Ray( ByteBuffer bos, int raySize, long rayOffset) {
        this.raySize = raySize;
        this.rayOffset = rayOffset;
        field_header_map = new HashMap();
        bos.position(0);

        byte[] data = new byte[UF_MANDATORY_HEADER2_LEN];
        bos.get(data);

        uf_header2 = new  UF_mandatory_header2(data);

        if(uf_header2.offset2StartOfOptionalHeader > 0 &&
                (uf_header2.dataHeaderPosition != uf_header2.offset2StartOfOptionalHeader)){
            data = new byte[28];
            bos.get(data);
            uf_opt_header = new UF_optional_header(data);
        }
        int position = uf_header2.dataHeaderPosition*2 -2;
        bos.position(position);
        data_msecs = setDateMesc();
        byte [] b2 = new byte[2];
        bos.get(b2);
        numberOfFields = getShort(b2, 0);
        bos.get(b2);
        numberOfRecords = getShort(b2, 0);
        bos.get(b2);
        numberOfFieldsInRecord = getShort(b2, 0);
        data = new byte[UF_FIELD_HEADER2_LEN];
        for(int i = 0; i < numberOfFields; i++){
            bos.get(b2);
            //int type = getShort(b2, 0);
            String type = new String(b2);
            bos.get(b2);
            int offs = getShort(b2, 0);
            int position0 = bos.position();
            bos.position(offs*2 - 2);
            bos.get(data);
            UF_field_header2 field_header = new UF_field_header2(data);
            bos.position(position0);
            field_header_map.put(type, field_header);

        }
    }

    public int getRaySize(){
        return raySize;
    }

    public int getGateCount(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.binCount;
    }

    public String getDatatypeName(String abbrev) {
        if( abbrev.equals("ZN") || abbrev.equals("ZS"))
          return "Reflectivity";
        else if (abbrev.equals("ZF") || abbrev.equals("ZX") || abbrev.equals("DR"))
          return "Reflectivity";
        else if(abbrev.equals("VR") || abbrev.equals("DN") || abbrev.equals("DS") || abbrev.equals("DF") ||abbrev.equals("DX") )
          return "RadialVelocity";
        else if(abbrev.equals("VN") || abbrev.equals("VF") )
          return "CorrectedRadialVelocity";
        else if(abbrev.equals("SW") || abbrev.equals("WS") || abbrev.equals("WF") || abbrev.equals("WX") || abbrev.equals("WN"))
          return "SpectrumWidth";
        else if(abbrev.equals("PN") || abbrev.equals("PS") || abbrev.equals("PF") || abbrev.equals("PX"))
          return "Power";
        else if(abbrev.equals("MN") || abbrev.equals("MS") || abbrev.equals("MF") || abbrev.equals("MX"))
          return "Power";
        else if(abbrev.equals("PH"))
            return "PhiDP";
        else if(abbrev.equals("RH"))
            return "RhoHV";
        else if(abbrev.equals("LH"))
            return "LdrH";
        else if(abbrev.equals("KD"))
            return "KDP";
        else if(abbrev.equals("LV"))
            return "LdrV" ;
        else if(abbrev.equals("DR"))
            return "ZDR";
        else if(abbrev.equals("CZ"))
            return "CorrecteddBZ";
        else if(abbrev.equals("DZ"))
            return "TotalReflectivity";
        else if(abbrev.equals("DR"))
            return "ZDR";
        else
          return abbrev;

    }

   public String getDatatypeUnits(String abbrev) {
       if(abbrev.equals("CZ") || abbrev.equals("DZ") || abbrev.equals("ZN") || abbrev.equals("ZS"))
          return "dBz";
        else if (abbrev.equals("ZF") || abbrev.equals("ZX"))
          return "dBz";
        else if(abbrev.equals("VR") || abbrev.equals("DN") || abbrev.equals("DS") || abbrev.equals("DF") ||abbrev.equals("DX") )
          return "m/s";
        else if(abbrev.equals("VN") || abbrev.equals("VF") )
          return "m/s";
        else if(abbrev.equals("SW") || abbrev.equals("WS") || abbrev.equals("WF") || abbrev.equals("WX") || abbrev.equals("WN"))
          return "m/s";
        else if(abbrev.equals("PN") || abbrev.equals("PS") || abbrev.equals("PF") || abbrev.equals("PX"))
          return "dBM";
        else if(abbrev.equals("MN") || abbrev.equals("MS") || abbrev.equals("MF") || abbrev.equals("MX"))
          return "dBM";

        else
          return abbrev;

   }

    public short getDatatypeRangeFoldingThreshhold(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.thresholdValue;

    }

    public float getDatatypeScaleFactor(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return 1.0f/header.scaleFactor;

    }

    public float getDatatypeAddOffset(String abbrev) {
        return 0.0f;


    }


    public int getGateStart(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.startRange;

    }

    public int getDataOffset(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.dataOffset;

    }
    public int getGateSize(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.binSpacing;

    }

    public float getElevation(){
        return uf_header2.elevation/64.f;
    }

    public float getAzimuth(){
        return uf_header2.azimuth/64.f;
    }

    public short getMissingData() {
        return uf_header2.missing;
    }

    public int getYear() {
        return getYear(uf_header2.year) ;
    }

    public float getLatitude(){
        return uf_header2.latitudeD + (uf_header2.latitudeM + uf_header2.latitudeS/(64*60.f))/60.f;
    }

    public float getLongtitude(){
        return uf_header2.longitudeD + (uf_header2.longitudeM + uf_header2.longitudeS/(64*60.f))/60.f;
    }

    public float getHorizontalBeamWidth(String abbrev) {
        UF_field_header2 header = field_header_map.get(abbrev);
        return header.HorizontalBeamWidth/64.f;

    }

    public int getYear(int year) {
        if(year > 1970)
            return year;
        if( year > 70 && year < 100)
            return 1900+ year;
        if( year < 60)
            return 2000 + year;

        return 0;
    }

    public long getTitleMsecs() {
        return data_msecs;

    }

    public long setDateMesc() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, uf_header2.year);
        cal.set(Calendar.MONTH,uf_header2.month );
        cal.set(Calendar.DAY_OF_MONTH, uf_header2.day );
        cal.set(Calendar.HOUR, uf_header2.hour);
        cal.set(Calendar.MINUTE, uf_header2.minute);
        cal.set(Calendar.SECOND, uf_header2.second);

        return cal.getTimeInMillis();
    }

    public Date getDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(data_msecs);
        return cal.getTime();
    }

    class UF_mandatory_header2 {
        String  textUF;
        short   recordSize;   // in 16-bit words
        short   offset2StartOfOptionalHeader; //, origin 1
        short   localUseHeaderPosition;
        short   dataHeaderPosition;
        short   recordNumber;
        short   volumeNumber;       // on tape, n/a for disk
        short   rayNumber;          // within the volume scan
        short   recordNumber1;      // within ray (origin 1)
        short   sweepNumber;        // within the volume scan
        String  radarName;          // char[8]
        String  siteName;           // char[8]
        short   latitudeD;          // degrees (North positive, South negative)
        short   latitudeM;          // minutes
        short   latitudeS;          // seconds*64
        short   longitudeD;         // degrees (East positive, West negative)
        short   longitudeM;         // Minutes
        short   longitudeS;         // Seconds
        short   height;             // of antenna above sea level in meters
        short   year;               // (time of data acquisition)
        short   month ;
        short   day;
        short   hour;
        short   minute;
        short   second;
        String timeZone;            // UT for universal  char[2]
        short   azimuth;            // (degrees*64) of midpoint of sample
        short   elevation;          // (degrees*64)
        short   sweepMode;
            //   0:Cal       1:PPI       2:Coplane 3:RHI
            //   4:Vertical 5:Target 6:Manual 7:Idle
        short   fixedAngle;         // (degrees*64)
        short   sweepRate;          // ((degrees/second)*64)
        short   year1;              // (generation data of UF format)
        short   month1;

        short   day1;
        String  nameOfUFGeneratorProgram; //  char[8]
        short   missing;             // Value stored for deleted or missing data (0x8000)



        public UF_mandatory_header2(byte[] data){
            // data is of length 90 bytes
            textUF = new String(data, 0, 2);
            if (debug){
                System.out.println(textUF);
            }
            recordSize = getShort(data, 2);
            offset2StartOfOptionalHeader = getShort(data, 4);
            localUseHeaderPosition= getShort(data, 6);
            dataHeaderPosition= getShort(data, 8);
            recordNumber= getShort(data, 10);
            volumeNumber= getShort(data, 12);
            rayNumber= getShort(data, 14);
            recordNumber1= getShort(data, 16);
            sweepNumber= getShort(data, 18);
            radarName= new String(data, 20, 8) ;
            siteName= new String(data, 28, 8);
            latitudeD= getShort(data, 36);
            latitudeM= getShort(data, 38);
            latitudeS= getShort(data, 40);
            longitudeD= getShort(data, 42);
            longitudeM= getShort(data, 44);
            longitudeS= getShort(data, 46);
            height= getShort(data, 48);
            int yearValue= getShort(data, 50);
            year = (short)getYear(yearValue);
            month = getShort(data, 52);
            day= getShort(data, 54);
            hour= getShort(data, 56);
            minute= getShort(data, 58);
            second= getShort(data, 60);
            timeZone = new String(data, 62, 2);
            azimuth= getShort(data, 64);
            elevation= getShort(data, 66);
            sweepMode= getShort(data, 68);
            fixedAngle= getShort(data, 70);         // (degrees*64)
            sweepRate = getShort(data, 72);          // ((degrees/second)*64)
            year1 = getShort(data, 74);              // (generation data of UF format)
            month1 = getShort(data, 76);
            day1 = getShort(data, 78);
            nameOfUFGeneratorProgram = new String(data, 80, 8); //  char[8]
            missing = getShort(data, 88);             // Value stored for deleted or missing data (0x8000)

        }



    }

    class UF_optional_header{

        String    sProjectName;   //  char[8]
        short     iBaselineAzimuth;
        short     iBaselineelevation;
        short     iVolumeScanHour;  /* Time of start of current volume scan */
        short     iVolumeScanMinute;
        short     iVolumeScanSecond;
        String    sFieldTapeName;  // char[8]
        short     iFlag;

        public UF_optional_header(byte[] data){
            sProjectName = new String(data, 0, 8);
            iBaselineAzimuth = getShort(data, 8);
            iBaselineelevation = getShort(data, 10);
            iVolumeScanHour = getShort(data, 12);
            iVolumeScanMinute = getShort(data, 14);
            iVolumeScanSecond = getShort(data, 16);
            sFieldTapeName = new String(data, 18, 8);
            iFlag = getShort(data, 26);
        }

    }


    class UF_field_header2 {

        short   dataOffset;   // from start of record, origin 1
        short   scaleFactor;  // met units = file value/scale
        short   startRange;   // km
        short   startRange1;   // meters
        short   binSpacing;  // in meters
        short   binCount;
        short   pulseWidth;  // in meters
        short   HorizontalBeamWidth; // in degrees*64
        short   verticalBeamWidth; // in degrees*64
        short   receiverBandwidth; // in Mhz*64 ?
        short   polarization;  //:          1:horz   2:vert
        short   waveLength;  // in cm*64
        short   sampleSize;
        String  typeOfData;     //used to threshold  //  char[2]
        short   thresholdValue;
        short   scale;
        String  editCode; //  char[2]
        short   prt;  // in microseconds
        short   bits; // per bin, must be 16
       //         38         12      <uf_fsi2>

        public UF_field_header2(byte[] data){
            dataOffset = getShort(data, 0);
            scaleFactor = getShort(data, 2);
            startRange = getShort(data, 4);
            startRange1 = getShort(data, 6);
            binSpacing = getShort(data, 8);
            binCount = getShort(data, 10);
            pulseWidth = getShort(data, 12);
            HorizontalBeamWidth = getShort(data, 14);
            verticalBeamWidth =getShort(data, 16);
            receiverBandwidth = getShort(data, 18);
            polarization = getShort(data, 20);
            waveLength = getShort(data, 22);
            sampleSize = getShort(data, 24);
            typeOfData = new String(data, 26, 2);
            thresholdValue = getShort(data, 28);
            scale = getShort(data, 30);
            editCode = new String(data, 32, 2);
            prt = getShort(data, 34);
            bits = getShort(data, 36);
        }
    }


    class UF_fsi2{

       //If velocity data:
        short nyquistVelocity;
       //2           SINT2 <spare>
       //If DM data:
        short radarConstant;
        short noisePower;
        short receiverGain;
        short peakPower;
        short antennaGain;
        short pulseDuration; // (microseconds*64)

        UF_fsi2(byte[] data){
            if(data.length > 4){
                radarConstant = getShort(data, 0);
                noisePower = getShort(data, 2);
                receiverGain = getShort(data, 24);
                peakPower = getShort(data, 6);
                antennaGain = getShort(data, 8);
                pulseDuration = getShort(data, 10);

            }
            else {
                nyquistVelocity = getShort(data, 0);
            }
        }


    }

    protected short getShort1(byte[] bytes, int offset) {
        int ndx0 = offset + (littleEndianData ? 1 : 0);
        int ndx1 = offset + (littleEndianData ? 0 : 1);
        // careful that we only allow sign extension on the highest order byte
        return (short)(bytes[ndx0] << 8 | (bytes[ndx1] & 0xff));
    }

    protected short getShort(byte[] bytes, int offset) {

        // careful that we only allow sign extension on the highest order byte
        return (short) bytesToShort(bytes[offset], bytes[offset+1], false);
    }

    public static int bytesToShort(byte a, byte b, boolean swapBytes) {
        // again, high order bit is expressed left into 32-bit form
        if (swapBytes) {
            return (a & 0xff) + ((int)b << 8);
        } else {
            return ((int)a << 8) + (b & 0xff);
        }
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

    public short[] byte2short(byte [] a, int length){
        int len = length/2;
        short [] b = new short[len];
        byte [] b2 = new byte[2];

        for(int i = 0; i < len; i++){
            b2[0] = a[2*i];
            b2[1] = a[2*i + 1];
            b[i] = getShort(b2, 0);
        }

        return b;
    }

  /**
   * Read data from this ray.
   * @param raf read from this file
   * @param abbrev which data type we want
   * @param gateRange handles the possible subset of data to return
   * @param ii put the data here
   * @throws java.io.IOException
       */
    public void readData(RandomAccessFile raf, String abbrev, Range gateRange, IndexIterator ii) throws IOException {
        long offset = rayOffset;
        offset += (getDataOffset( abbrev) * 2 - 2) ;
        raf.seek(offset);
        byte[] b2 = new byte[2];
        int dataCount = getGateCount( abbrev);
        byte[] data = new byte[dataCount*2];
        raf.readFully(data);
        short[] tmp = byte2short(data, 2*dataCount);

        for (int i = gateRange.first(); i <= gateRange.last(); i += gateRange.stride()) {
            if (i >= dataCount)
                ii.setShortNext(uf_header2.missing);
            else {
                b2[0] = data[i*2];
                b2[1] = data[i*2+1];
                short value = getShort(b2, 0);

                ii.setShortNext(value);
            }
        }

    }

}