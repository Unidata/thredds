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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeRYIB.java 51 2006-07-12 17:13:13Z caron $ */

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