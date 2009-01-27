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

/**
 * <p>Title: ScanMode</p>
 * <p>Description: Simple enumerated class for radar scan modes.  Only
 * the static singleton instances provided should be used; no public
 * constructor is available.</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:ScanMode.java 51 2006-07-12 17:13:13Z caron $ */

public class ScanMode {
    /**
     * PPI (Plan Position Indicator): fixed elevation and varying azimuth
     */
    public static final ScanMode MODE_PPI = new ScanMode("PPI");
    /**
     * RHI (Range-Height Indicator): fixed azimuth and varying elevation
     */
    public static final ScanMode MODE_RHI = new ScanMode("RHI");
    /**
     * SUR (surveillance)
     * <p>360 degree PPI
     */
    public static final ScanMode MODE_SUR = new ScanMode("SUR");
    /**
     * coplane: the radar is scanned in a single plane, so both azimuth
     * and elevation can vary
     */
    public static final ScanMode MODE_coplane = new ScanMode("coplane");
    /**
     * calibration: a calibration is being performed
     */
    public static final ScanMode MODE_calibration = new ScanMode("calibration");
    /**
     * vertical: the antenna is pointing vertically
     */
    public static final ScanMode MODE_vertical = new ScanMode("vertical");
    /**
     * idle: antenna position and scanning are undefined
     */
    public static final ScanMode MODE_idle = new ScanMode("idle");
    /**
     * target: the antenna is pointed at a fixed location
     */
    public static final ScanMode MODE_target = new ScanMode("target");
    /**
     * manual: antenna position is being manually controlled
     */
    public static final ScanMode MODE_manual = new ScanMode("manual");
    /**
     * air: air (aircraft?) scanning
     */
    public static final ScanMode MODE_air = new ScanMode("air");
    /**
     * horizontal: not scanning, horizontally pointing
     */
    public static final ScanMode MODE_horizontal = new ScanMode("horizontal");


    private String modeName;
    private ScanMode(String modeName) {
        this.modeName = modeName;
    }

    /**
     * Get the name of this scan mode
     * @return the scan mode name
     */
    public String getName() {
        return modeName;
    }

    public String toString() {
        return modeName;
    }
}
