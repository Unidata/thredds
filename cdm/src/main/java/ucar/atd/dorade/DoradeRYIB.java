package ucar.atd.dorade;

import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>Title: DoradeRYIB</p>
 * <p>Description: DORADE ray information block</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision$ $Date$
 */
/* $Id$ */

class DoradeRYIB extends DoradeDescriptor {

    private int sweepNumber;
    private Date rayTime;
    private float azimuth;
    private float elevation;
    private float lastPeakPower; // kW
    private float scanRate; // deg/s
    private int rayStatus; // 0 normal, 1 transition, 2 bad
    private DoradeVOLD myVOLD;

    public DoradeRYIB(RandomAccessFile file, boolean littleEndianData,
                      DoradeVOLD vold)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "RYIB");
        myVOLD = vold;

        //
        // unpack
        //
        sweepNumber = grabInt(data, 8);
        int julianDay = grabInt(data, 12);
        int hour = grabShort(data, 16);
        int minute = grabShort(data, 18);
        int second = grabShort(data, 20);
        int milliSecond = grabShort(data, 22);
        azimuth = grabFloat(data, 24);
        elevation = grabFloat(data, 28);
        lastPeakPower = grabFloat(data, 32);
        scanRate = grabFloat(data, 36);
        rayStatus = grabInt(data, 40);

        //
        // Assemble the ray time, starting from the volume time, setting the
        // hh:mm:ss, and adjusting up by a day if we crossed a day bound
        // since the beginning of the volume.  Why didn't they just put
        // a complete time here?  Julian day without the year is useless!
        // GRRR...
        //
        Date volumeTime = myVOLD.getVolumeTime();
        Calendar volumeCalendar = Calendar.getInstance(TZ_UTC);
        volumeCalendar.setTime(volumeTime);

        Calendar rayTimeCalendar = (Calendar)volumeCalendar.clone();
        rayTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
        rayTimeCalendar.set(Calendar.MINUTE, minute);
        rayTimeCalendar.set(Calendar.SECOND, second);
        rayTimeCalendar.set(Calendar.MILLISECOND, milliSecond);

        if (rayTimeCalendar.before(volumeCalendar))
            rayTimeCalendar.add(Calendar.DAY_OF_MONTH, 1);

        rayTime = rayTimeCalendar.getTime();

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "RYIB\n";
        s += "  sweep number: " + sweepNumber + "\n";
        s += "  ray time: " + formatDate(rayTime) + "\n";
        s += "  azimuth: " + azimuth + "\n";
        s += "  elevation: " + elevation + "\n";
        s += "  last peak transmitted power: " + lastPeakPower + "\n";
        s += "  scan rate: " + scanRate + "\n";
        s += "  ray status: " + rayStatus;
        return s;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public float getElevation() {
        return elevation;
    }
    // unidata added
    public Date getRayTime() {
        return rayTime;
    }

}