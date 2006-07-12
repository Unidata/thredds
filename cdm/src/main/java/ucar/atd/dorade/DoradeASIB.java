package ucar.atd.dorade;

import java.io.RandomAccessFile;

/**
 * <p>Title: DoradeASIB</p>
 * <p>Description: DORADE aircraft/ship information block</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision$ $Date$
 */
/* $Id$ */


class DoradeASIB extends DoradeDescriptor {

    private float longitude;
    private float latitude;
    private float altitudeMSL; // km
    private float altitudeAGL; // km
    private float groundSpeedEW; // m/s
    private float groundSpeedNS; // m/s
    private float verticalVelocity; // m/s
    private float antennaHeading; // deg clockwise from true north
    private float rollAngle;
    private float pitchAngle;
    private float yawAngle;
    private float antennaScanAngle;  // deg clockwise from up w.r.t. airframe
    private float antennaFixedAngle; // deg toward nose
    private float uWind; // m/s
    private float vWind; // m/s
    private float wWind; // m/s
    private float headingChangeRate; // deg/s
    private float pitchChangeRate; // deg/s


    public DoradeASIB(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "ASIB");

        //
        // unpack
        //

        longitude = grabFloat(data, 8);
        latitude = grabFloat(data, 12);
        altitudeMSL = grabFloat(data, 16);
        altitudeAGL = grabFloat(data, 20);
        groundSpeedEW = grabFloat(data, 24);
        groundSpeedNS = grabFloat(data, 28);
        verticalVelocity = grabFloat(data, 32);
        antennaHeading = grabFloat(data, 36);
        rollAngle = grabFloat(data, 40);
        pitchAngle = grabFloat(data, 44);
        yawAngle = grabFloat(data, 48);
        antennaScanAngle = grabFloat(data, 52);
        antennaFixedAngle = grabFloat(data, 56);
        uWind = grabFloat(data, 60);
        vWind = grabFloat(data, 64);
        wWind = grabFloat(data, 68);
        headingChangeRate = grabFloat(data, 72);
        pitchChangeRate = grabFloat(data, 76);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "ASIB\n";
        s += "  longitude: " +  longitude + "\n";
        s += "  latitude: " +  latitude + "\n";
        s += "  altitude (MSL): " +  altitudeMSL + "\n";
        s += "  altitude (AGL): " +  altitudeAGL + "\n";
        s += "  EW ground speed: " + groundSpeedEW + "\n";
        s += "  NS ground speed: " + groundSpeedNS + "\n";
        s += "  vertical velocity: " + verticalVelocity + "\n";
        s += "  antenna heading: " + antennaHeading + "\n";
        s += "  roll: " + rollAngle + "\n";
        s += "  pitch: " + pitchAngle + "\n";
        s += "  yaw: " + yawAngle + "\n";
        s += "  scan angle: " + antennaScanAngle + "\n";
        s += "  fixed angle: " + antennaFixedAngle + "\n";
        s += "  u wind: " + uWind + "\n";
        s += "  v wind: " + vWind + "\n";
        s += "  w wind: " + wWind + "\n";
        s += "  heading change rate: " + headingChangeRate + "\n";
        s += "  pitch change rate: " + pitchChangeRate;
        return s;
    }

    /**
     * Get the latitude
     * @return  the latitude
     */
    public float getLatitude() {
        return latitude;
    }

    /**
     * Get the longitude
     * @return  the longitude
     */
    public float getLongitude() {
        return longitude;
    }

    /**
     * Get the altitude (MSL)
     * @return  the altitude in km MSL
     */
    public float getAltitude() {
        return altitudeMSL;
    }
}