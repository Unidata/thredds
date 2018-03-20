/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;

import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;

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

        projectName = new String(data, 16, 20, CDM.utf8Charset).trim();

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

        flightId = new String(data, 48, 8, CDM.utf8Charset).trim();
        facilityName = new String(data, 56, 8, CDM.utf8Charset).trim();

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