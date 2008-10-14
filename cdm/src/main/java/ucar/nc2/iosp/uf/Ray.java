package ucar.nc2.iosp.uf;

import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Range;
import ucar.ma2.IndexIterator;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Calendar;
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
    static final boolean littleEndianData = false;
    boolean debug = false;

     /**   moment identifier */
    public static final int VELOCITY = 1;
    public static final int SPECTRUM = 2;
    public static final int ZDR = 3;
    public static final int CORRECTEDDBZ = 4;
    public static final int TOTALDBZ = 5;
    public static final int RHOHV = 6;
    public static final int PHIDP = 7;
    public static final int KDP = 8;
    public static final int LDRH = 9;
    public static final int LDRV = 10;

    long data_msecs = 0;

    UF_mandatory_header2 uf_header2;
    UF_optional_header uf_opt_header;
    short      numberOfFields;   // in this ray
    short      numberOfRecords;  // in this ray
    short      numberOfFieldsInRecord;   // in this record
    boolean hasVR = false;
    boolean hasSW = false;
    boolean hasDR = false;
    boolean hasCZ = false;
    boolean hasDZ = false;
    boolean hasRH = false;
    boolean hasPH = false;
    boolean hasKD = false;
    boolean hasLH = false;
    boolean hasLV = false;
    UF_field_header2 vr_field_header;
    UF_field_header2 sw_field_header;
    UF_field_header2 dr_field_header;
    UF_field_header2 cz_field_header;
    UF_field_header2 dz_field_header;
    UF_field_header2 rh_field_header;
    UF_field_header2 ph_field_header;
    UF_field_header2 kd_field_header;
    UF_field_header2 lh_field_header;
    UF_field_header2 lv_field_header;

    public Ray( ByteBuffer bos, int raySize, long rayOffset) {
        this.raySize = raySize;
        this.rayOffset = rayOffset;
        
        bos.position(0);

        byte[] data = new byte[UF_MANDATORY_HEADER2_LEN];
        bos.get(data);

        uf_header2 = new  UF_mandatory_header2(data);

/*         if(uf_header2.offset2StartOfOptionalHeader > 0){
            data = new byte[28];
            bos.get(data);
            uf_opt_header = new UF_optional_header(data);
        }
*/      data_msecs = setDateMesc();
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
            if(type.equalsIgnoreCase("VR")){
                hasVR = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                vr_field_header = new UF_field_header2(data);

                 // test the data part
                int dataOffset = vr_field_header.dataOffset;
                byte [] testData = new byte[2*vr_field_header.binCount];
                bos.position(dataOffset*2 - 2);
                bos.get(testData);

                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("SW")){
                hasSW = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                sw_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("DR")){
                hasDR = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                dr_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("CZ")){
                hasCZ = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                cz_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("DZ")){
                hasDZ = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                dz_field_header = new UF_field_header2(data);


                 // test the data part
                int dataOffset = dz_field_header.dataOffset;
                byte [] testData = new byte[2*dz_field_header.binCount];
                bos.position(dataOffset*2 -2);
                bos.get(testData);
                short[] tmp = byte2short(testData, 2*dz_field_header.binCount);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("RH")){
                hasRH = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                rh_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("PH")){
                hasPH = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                ph_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("KD")){
                hasKD = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                kd_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("LH")){
                hasLH = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                lh_field_header = new UF_field_header2(data);
                bos.position(position0);
            }
            else if(type.equalsIgnoreCase("LV")){
                hasLV = true;
                bos.get(b2);
                int offs = getShort(b2, 0);
                int position0 = bos.position();
                bos.position(offs*2 - 2);
                bos.get(data);
                lv_field_header = new UF_field_header2(data);
                bos.position(position0);
            }


        }
    }

    public int getRaySize(){
        return raySize;
    }

    public int getGateCount(int dataType) {
        switch (dataType) {
            case VELOCITY : return vr_field_header.binCount;
            case SPECTRUM : return sw_field_header.binCount;
            case ZDR : return dr_field_header.binCount;
            case CORRECTEDDBZ : return cz_field_header.binCount;
            case TOTALDBZ : return dz_field_header.binCount;
            case RHOHV : return rh_field_header.binCount;
            case PHIDP : return ph_field_header.binCount;
            case KDP : return kd_field_header.binCount;
            case LDRH : return lh_field_header.binCount;
            case LDRV : return lv_field_header.binCount;
            default : throw new IllegalArgumentException();
        }
    }
    static public String getDatatypeName( int datatype) {
        switch (datatype) {
          case TOTALDBZ : return "Reflectivity";
          case VELOCITY : return "RadialVelocity";
          case SPECTRUM : return "SpectrumWidth";
          default : throw new IllegalArgumentException();
        }
    }

   static public String getDatatypeUnits(int datatype) {
     switch (datatype) {
       case TOTALDBZ :
           return "dBZ";

       case VELOCITY :

       case SPECTRUM :
           return "m/s";
     }
     throw new IllegalArgumentException();
   }

    public short getDatatypeRangeFoldingThreshhold(int datatype) {
        switch (datatype) {
            case VELOCITY : return vr_field_header.thresholdValue;
            case SPECTRUM : return sw_field_header.thresholdValue;
            case ZDR : return dr_field_header.thresholdValue;
            case CORRECTEDDBZ : return cz_field_header.thresholdValue;
            case TOTALDBZ : return dz_field_header.thresholdValue;
            case RHOHV : return rh_field_header.thresholdValue;
            case PHIDP : return ph_field_header.thresholdValue;
            case KDP : return kd_field_header.thresholdValue;
            case LDRH : return lh_field_header.thresholdValue;
            case LDRV : return lv_field_header.thresholdValue;

            default : throw new IllegalArgumentException();
        }
    }

    public float getDatatypeScaleFactor(int datatype) {
        switch (datatype) {
            case VELOCITY : return 1.f/vr_field_header.scaleFactor;
            case SPECTRUM : return 1.f/sw_field_header.scaleFactor;
            case ZDR : return 1.f/dr_field_header.scaleFactor;
            case CORRECTEDDBZ : return 1.f/cz_field_header.scaleFactor;
            case TOTALDBZ : return 1.f/dz_field_header.scaleFactor;
            case RHOHV : return 1.f/rh_field_header.scaleFactor;
            case PHIDP : return 1.f/ph_field_header.scaleFactor;
            case KDP : return 1.f/kd_field_header.scaleFactor;
            case LDRH : return 1.f/lh_field_header.scaleFactor;
            case LDRV : return 1.f/lv_field_header.scaleFactor;

            default : throw new IllegalArgumentException();
        }
    }

    public float getDatatypeAddOffset(int datatype) {
        return 0.0f;

   /*     switch (datatype) {
            case VELOCITY : return vr_field_header.scale;
            case SPECTRUM : return sw_field_header.scale;
            case ZDR : return dr_field_header.scale;
            case CORRECTEDDBZ : return cz_field_header.scale;
            case TOTALDBZ : return dz_field_header.scale;
            case RHOHV : return rh_field_header.scale;
            case PHIDP : return ph_field_header.scale;
            case KDP : return kd_field_header.scale;
            case LDRH : return lh_field_header.scale;
            case LDRV : return lv_field_header.scale;         

            default : throw new IllegalArgumentException();
        }   */
    }


    public int getGateStart(int datatype) {
        switch (datatype) {
            case VELOCITY : return vr_field_header.startRange;
            case SPECTRUM : return sw_field_header.startRange;
            case ZDR : return dr_field_header.startRange;
            case CORRECTEDDBZ : return cz_field_header.startRange;
            case TOTALDBZ : return dz_field_header.startRange;
            case RHOHV : return rh_field_header.startRange;
            case PHIDP : return ph_field_header.startRange;
            case KDP : return kd_field_header.startRange;
            case LDRH : return lh_field_header.startRange;
            case LDRV : return lv_field_header.startRange;

            default : throw new IllegalArgumentException();
        }

    }

    public int getDataOffset(int datatype) {
        switch (datatype) {
            case VELOCITY : return vr_field_header.dataOffset;
            case SPECTRUM : return sw_field_header.dataOffset;
            case ZDR : return dr_field_header.dataOffset;
            case CORRECTEDDBZ : return cz_field_header.dataOffset;
            case TOTALDBZ : return dz_field_header.dataOffset;
            case RHOHV : return rh_field_header.dataOffset;
            case PHIDP : return ph_field_header.dataOffset;
            case KDP : return kd_field_header.dataOffset;
            case LDRH : return lh_field_header.dataOffset;
            case LDRV : return lv_field_header.dataOffset;

            default : throw new IllegalArgumentException();
        }

    }
    public int getGateSize(int datatype) {
        switch (datatype) {
            case VELOCITY : return vr_field_header.binSpacing;
            case SPECTRUM : return sw_field_header.binSpacing;
            case ZDR : return dr_field_header.binSpacing;
            case CORRECTEDDBZ : return cz_field_header.binSpacing;
            case TOTALDBZ : return dz_field_header.binSpacing;
            case RHOHV : return rh_field_header.binSpacing;
            case PHIDP : return ph_field_header.binSpacing;
            case KDP : return kd_field_header.binSpacing;
            case LDRH : return lh_field_header.binSpacing;
            case LDRV : return lv_field_header.binSpacing;

            default : throw new IllegalArgumentException();
        }

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

    public float getHorizontalBeamWidth() {
        if(vr_field_header != null)
            return  vr_field_header.HorizontalBeamWidth/64.f;
        else if(sw_field_header != null)
            return  sw_field_header.HorizontalBeamWidth/64.f;
        else if(dr_field_header != null)
            return dr_field_header.HorizontalBeamWidth/64.f;
        else if(cz_field_header != null)
            return cz_field_header.HorizontalBeamWidth/64.f;
        else if(dz_field_header != null)
            return dz_field_header.HorizontalBeamWidth/64.f;
        else if(rh_field_header != null)
            return rh_field_header.HorizontalBeamWidth/64.f;
        else if(ph_field_header != null)
            return ph_field_header.HorizontalBeamWidth/64.f;
        else if(kd_field_header != null)
            return kd_field_header.HorizontalBeamWidth/64.f;
        else if(lh_field_header != null)
            return lh_field_header.HorizontalBeamWidth/64.f;
        else if(lv_field_header != null)
            return lv_field_header.HorizontalBeamWidth/64.f;
        else
            return 0.0f;
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

    protected short getShort(byte[] bytes, int offset) {
        int ndx0 = offset + (littleEndianData ? 1 : 0);
        int ndx1 = offset + (littleEndianData ? 0 : 1);
        // careful that we only allow sign extension on the highest order byte
        return (short)(bytes[ndx0] << 8 | (bytes[ndx1] & 0xff));
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
   * @param datatype which data type we want
   * @param gateRange handles the possible subset of data to return
   * @param ii put the data here
   * @throws java.io.IOException
       */
    public void readData(RandomAccessFile raf, int datatype, Range gateRange, IndexIterator ii) throws IOException {
        long offset = rayOffset;
        offset += (getDataOffset( datatype) * 2 - 2) ;
        raf.seek(offset);
        byte[] b2 = new byte[2];
        int dataCount = getGateCount( datatype);
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