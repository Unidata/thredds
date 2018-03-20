/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import java.io.RandomAccessFile;

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

  public DoradeCFAC(RandomAccessFile file, boolean littleEndianData) throws DescriptorException {
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