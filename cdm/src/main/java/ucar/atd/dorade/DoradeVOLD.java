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
 * <p>Title: DoradeVOLD</p>
 * <p>Description: DORADE volume descriptor</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeVOLD.java 51 2006-07-12 17:13:13Z caron $ */

class DoradeVOLD extends DoradeDescriptor {

    private short formatRev;
    private short volNumber;
    private int maxRecLen;
    private String projectName;
    private Date dataTime;
    private String flightId;
    private String facilityName;
    private Date fileGenTime;
    private short nSensors;
    private DoradeRADD[] myRADDs;

    public DoradeVOLD(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "VOLD");

        //
        // unpack
        //
        formatRev = grabShort(data, 8);
        volNumber = grabShort(data, 10);
        maxRecLen = grabInt(data, 12);

        projectName = new String(data, 16, 20).trim();

        Calendar calendar = Calendar.getInstance(TZ_UTC);

        short year = grabShort(data, 36);
        short month = grabShort(data, 38);
        short day = grabShort(data, 40);
        short hour = grabShort(data, 42);
        short minute = grabShort(data, 44);
        short second = grabShort(data, 46);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        dataTime = calendar.getTime();

        flightId = new String(data, 48, 8).trim();
        facilityName = new String(data, 56, 8).trim();

        year = grabShort(data, 64);
        month = grabShort(data, 66);
        day = grabShort(data, 68);
        calendar.clear();
        calendar.set(year, month - 1, day);
        fileGenTime = calendar.getTime();

        nSensors = grabShort(data, 70);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);

        //
        // get our myRADDs
        //
        myRADDs = new DoradeRADD[nSensors];
        for (int i = 0; i < nSensors; i++)
            myRADDs[i] = new DoradeRADD(file, littleEndianData);

    }


    public String toString() {
        String s = "VOLD\n";
        s += "  format: " + formatRev + "\n";
        s += "  volume: " + volNumber + "\n";
        s += "  maxRecLen: " + maxRecLen + "\n";
        s += "  project: " + projectName + "\n";
        s += "  data time: " + formatDate(dataTime) + "\n";
        s += "  flight id: " + flightId + "\n";
        s += "  facility name: " + facilityName + "\n";
        s += "  file made: " + formatDate(fileGenTime) + "\n";
        s += "  num sensors: " + nSensors;
        return s;
    }

    /**
     * Get the number of sensors (radars) associated with this VOLD.
     * @return the number of sensors
     */
    public int getNSensors() {
        return nSensors;
    }

    /**
     * Get the name of the nth sensor (radar) associated with this VOLD.
     * @param n  the index of the desired sensor
     * @return the name of the nth sensor
     * @see #getNSensors()
     */
    public String getSensorName(int n) {
        return myRADDs[n].getRadarName();
    }

    /**
     * Get the <code>DoradeRADD</code> for the nth sensor (radar).
     * @param n  the index of the desired sensor
     * @return the <code>DoradeRADD</code> for the nth sensor
     * @see #getNSensors()
     */
    public DoradeRADD getRADD(int n) {
        return myRADDs[n];
    }

    /**
     * Get the time for this volume.  In practice, this means the volume start
     * time, but this is not stated as part of the definition of DORADE.
     * @return the time for this volume
     */
    public Date getVolumeTime() {
        return dataTime;
    }

    // unidata added
    public short getVolumeNumber() {
        return volNumber;
    }

    // unidata added
    public String getProjectName() {
        return projectName;
    }

    /**
     * Get the array of available parameter names for this volume.
     * @return an array of parameter names
     */
    public DoradePARM[] getParamList() {
        int paramCount = 0;
        for (int i = 0; i < nSensors; i++)
            paramCount += myRADDs[i].getNParams();

        DoradePARM[] list = new DoradePARM[paramCount];
        int next = 0;
        for (int i = 0; i < nSensors; i++) {
            int nParams = myRADDs[i].getNParams();
            System.arraycopy(myRADDs[i].getParamList(), 0, list, next, nParams);
            next += nParams;
        }
        return list;
    }

}