package ucar.atd.dorade;

import java.io.RandomAccessFile;
import java.util.Date;

/**
 * <p>Title: DoradeSWIB</p>
 * <p>Description: DORADE sweep information block</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision$ $Date$
 */
/* $Id$ */

class DoradeSWIB extends DoradeDescriptor {

    private String comment;
    private int sweepNumber;
    private int nRays;
    private float startAngle;
    private float endAngle;
    private float fixedAngle;
    private int filterFlag; // 0 = no filtering, 1 = filtered as described at
                            // the beginning of the file

    private long[] rayDataOffsets;
    private DoradeRYIB[] myRYIBs;
    private DoradeASIB[] myASIBs;
    private DoradeVOLD myVOLD;

    private float[] azimuths = null;
    private float[] elevations = null;

    public DoradeSWIB(RandomAccessFile file, boolean littleEndianData,
                      DoradeVOLD vold)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "SWIB");
        myVOLD = vold;

        //
        // unpack
        //
        comment = new String(data, 8, 8).trim();
        sweepNumber = grabInt(data, 16);
        nRays = grabInt(data, 20);
        startAngle = grabFloat(data, 24);
        endAngle = grabFloat(data, 28);
        fixedAngle = grabFloat(data, 32);
        filterFlag = grabInt(data, 36);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);

        //
        // Get the RYIBs and save offsets to the data for each ray
        //
        myRYIBs = new DoradeRYIB[nRays];
        myASIBs = new DoradeASIB[nRays];
        rayDataOffsets = new long[nRays];
        boolean haveASIBs = false;
        for (int i = 0; i < nRays; i++) {
            myRYIBs[i] = new DoradeRYIB(file, littleEndianData, myVOLD);
            try {
                rayDataOffsets[i] = file.getFilePointer();
            } catch (java.io.IOException ex) {
                throw new DescriptorException(ex);
            }
            //
            // Look for an ASIB if this is the first ray or if we (based on
            // the results from the first ray) are expecting an ASIB for every
            // ray.
            //
            if (i == 0 || haveASIBs) try {
                myASIBs[i] = new DoradeASIB(file, littleEndianData);
                haveASIBs = true;
            } catch (DescriptorException ex) {
                //
                // We failed to find an ASIB.  If this is the first ray, just
                // assume we have no ASIBs and move on.  Otherwise, we're
                // missing an expected ASIB, and that's bad...
                //
                if (i == 0) {
                    haveASIBs = false;
                    myASIBs = null;
                    //
                    // We're at EOF.  Move back to where we started.
                    //
                    try {
                        file.seek(rayDataOffsets[i]);
                    } catch (java.io.IOException ioex) {
                        throw new DescriptorException(ioex);
                    }
                } else {
                    throw new DescriptorException("not enough ASIBs");
                }
            }
        }
    }

    public String toString() {
        String s = "SWIB\n";
        s += "  sweep number: " + sweepNumber + "\n";
        s += "  number of rays: " + nRays + "\n";
        s += "  start angle: " + startAngle + "\n";
        s += "  end angle: " + endAngle + "\n";
        s += "  fixed angle: " + fixedAngle + "\n";
        s += "  filter flag: " + filterFlag;
        return s;
    }


    public float[] getRayData(DoradePARM parm, int ray)
            throws DescriptorException {
	return getRayData(parm, ray,null);
    }


    public float[] getRayData(DoradePARM parm, int ray, float[] workingArray)
            throws DescriptorException {
        try {
            //
            // position to the beginning of the ray
            //
            file.seek(rayDataOffsets[ray]);

            //
            // find the RDAT for the selected parameter
            //
            DoradeRDAT rdat = DoradeRDAT.getNextOf(parm, file,
                                                   littleEndianData);
            return parm.getParamValues(rdat,workingArray);

        } catch (java.io.IOException ex) {
            throw new DescriptorException(ex);
        }
    }

    public int getNRays() {
        return nRays;
    }

    //  unidata added
    public Date[] getTimes() {
        if (myRYIBs == null)
            return null;
        Date[] times = new Date[nRays];
        for (int i = 0; i < nRays; i++)
            times[i] = myRYIBs[i].getRayTime();

        return times;
    }

    // unidata added
    public Date getRayTime(int ray) {
        return myRYIBs[ray].getRayTime();
    }
    /**
     * Get the array of per-ray latitudes.  If we do not have per-ray position
     * information, null is returned.
     * @return an array of per-ray latitudes, or null if no per-ray position
     * information is available.
     */
    public float[] getLatitudes() {
        if (myASIBs == null)
            return null;
        float[] lats = new float[nRays];
        for (int i = 0; i < nRays; i++)
            lats[i] = myASIBs[i].getLatitude();
        return lats;
    }

    /**
     * Get the array of per-ray longitudes.  If we do not have per-ray position
     * information, null is returned.
     * @return an array of per-ray longitudes, or null if no per-ray position
     * information is available.
     */
    public float[] getLongitudes() {
        if (myASIBs == null)
            return null;
        float[] lons = new float[nRays];
        for (int i = 0; i < nRays; i++)
            lons[i] = myASIBs[i].getLongitude();
        return lons;
    }

    /**
     * Get the array of per-ray altitudes.  If we do not have per-ray position
     * information, null is returned.
     * @return an array of per-ray altitudes in km MSL, or null if no per-ray
     * position information is available.
     */
    public float[] getAltitudes() {
        if (myASIBs == null)
            return null;
        float[] alts = new float[nRays];
        for (int i = 0; i < nRays; i++)
            alts[i] = myASIBs[i].getAltitude();
        return alts;
    }

    /**
     * Get the array of azimuths for this sweep.
     * @return the array of azimuths for this sweep
     * @see #getNRays()
     */
    public float[] getAzimuths() {
        if (azimuths == null) {
            azimuths = new float[nRays];
            for (int r = 0; r < nRays; r++) {
                azimuths[r] = myRYIBs[r].getAzimuth();
            }
        }
        return azimuths;
    }

    /**
     * Get the array of elevations for this sweep.
     * @return the array of elevations for this sweep
     * @see #getNRays()
     */
    public float[] getElevations() {
        if (elevations == null) {
            elevations = new float[nRays];
            for (int r = 0; r < nRays; r++) {
                elevations[r] = myRYIBs[r].getElevation();
            }
        }
        return elevations;
    }

    /**
     * Get the fixed angle for this sweep.
     * @return the fixed angle for this sweep
     */
    public float getFixedAngle() {
        return fixedAngle;
    }

    // unidata added
    public int getSweepNumber() {
        return sweepNumber;
    }
}
