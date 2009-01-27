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

/**
 * <p>Title: DoradeCFAC</p>
 * <p>Description: DORADE correction factor descriptor</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeCFAC.java 51 2006-07-12 17:13:13Z caron $ */

class DoradeCFAC extends DoradeDescriptor {

    private float azimCorrection; // deg
    private float elevCorrection; // deg
    private float rangeCorrection; // m
    private float longitudeCorrection; // deg
    private float latitudeCorrection; // deg
    private float presAltCorrection; // km
    private float radarAltCorrection; // km
    private float uSpeedCorrection; // m/s
    private float vSpeedCorrection; // m/s
    private float wSpeedCorrection; // m/s
    private float headingCorrection; // deg
    private float rollCorrection; // deg
    private float pitchCorrection; // deg
    private float driftCorrection; // deg
    private float rotationAngleCorrection; // deg
    private float tiltAngleCorrection; // deg

    public DoradeCFAC(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "CFAC");

        //
        // unpack
        //
        azimCorrection = grabFloat(data, 8);
        elevCorrection = grabFloat(data, 12);
        rangeCorrection = grabFloat(data, 16);
        longitudeCorrection = grabFloat(data, 20);
        latitudeCorrection = grabFloat(data, 24);
        presAltCorrection = grabFloat(data, 28);
        radarAltCorrection = grabFloat(data, 32);
        uSpeedCorrection = grabFloat(data, 36);
        vSpeedCorrection = grabFloat(data, 40);
        wSpeedCorrection = grabFloat(data, 44);
        headingCorrection = grabFloat(data, 48);
        rollCorrection = grabFloat(data, 52);
        pitchCorrection = grabFloat(data, 56);
        driftCorrection = grabFloat(data, 60);
        rotationAngleCorrection = grabFloat(data, 64);
        tiltAngleCorrection = grabFloat(data, 68);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "CFAC\n";
        s += "  azimuth correction: " + azimCorrection + "\n";
        s += "  elevation correction: " + elevCorrection + "\n";
        s += "  range correction: " + rangeCorrection + "\n";
        s += "  longitude correction: " + longitudeCorrection + "\n";
        s += "  latitude correction: " + latitudeCorrection + "\n";
        s += "  pressure altitude correction: " + presAltCorrection + "\n";
        s += "  radar altitude correction: " + radarAltCorrection + "\n";
        s += "  u speed correction: " + uSpeedCorrection + "\n";
        s += "  v speed correction: " + vSpeedCorrection + "\n";
        s += "  w speed correction: " + wSpeedCorrection + "\n";
        s += "  heading correction: " + headingCorrection + "\n";
        s += "  roll correction: " + rollCorrection + "\n";
        s += "  pitch correction: " + pitchCorrection + "\n";
        s += "  drift correction: " + driftCorrection + "\n";
        s += "  rotation angle correction: " + rotationAngleCorrection + "\n";
        s += "  tilt angle correction: " + tiltAngleCorrection;
        return s;
    }
}