/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

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
   *
   * @return the scan mode name
   */
  public String getName() {
    return modeName;
  }

  public String toString() {
    return modeName;
  }
}
