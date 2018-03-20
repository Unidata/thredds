/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.radial;

import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;

/**
 * @author caron
 * @since Feb 18, 2008
 */
public interface RadialSweepFeature {

  public enum Type { NONE }

  /**
   * @return the type of the Sweep
   */
  public RadialSweepFeature.Type getType();


  public Variable getSweepVar();

  /**
   * @return the number of radials for this Sweep
   */
  public int getRadialNumber();

  /**
   * @return the number of gates for all radials
   */
  public int getGateNumber();

  /**
   * @return the beam width for all radials, in degrees
   */
  public float getBeamWidth();

  /**
   * @return the Nyquist Frequency for all radials
   */
  public float getNyquistFrequency();

  /**
   * Get the radial distance from origin to the start of the first data gate.
   * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
   *
   * @return distance to first gate in meters, for all radials
   */
  public float getRangeToFirstGate();

  /**
   * Get the radial length of each data gate.
   * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
   *
   * @return gate size in meters,  for all radials
   */
  public float getGateSize();

  /**
   * @return all the sweep data, of length getNumRadials() by getNumGates()
   * @throws java.io.IOException on io error
   */
  public float[] readData() throws java.io.IOException;

  /**
   * @param radial which radial, must in interval [0,getRadialNumber())
   * @return the actual data, of length getNumGates()
   * @throws java.io.IOException on io error
   */
  public float[] readData(int radial) throws java.io.IOException;

  /**
   * @param radial which radial, must in interval [0,getRadialNumber())
   * @return the elevation of the ith radial, in degrees
   * @throws java.io.IOException on io error
   */
  public float getElevation(int radial) throws java.io.IOException;

  /**
   * @return all elevation in the sweep
   * @throws java.io.IOException on io error
   */
  public float[] getElevation() throws java.io.IOException;

  /**
   * @return the average elevation of all the radials in the sweep, in degrees.
   *         Only valid if getType() == TYPE_
   */
  public float getMeanElevation();

  /**
   * @param radial which radial, must in interval [0,getRadialNumber())
   * @return the azimuth of the ith radial, in degrees
   * @throws java.io.IOException on io error
   */
  public float getAzimuth(int radial) throws java.io.IOException;

  /**
   * @return all azimuth in the sweep
   * @throws java.io.IOException on io error
   */
  public float[] getAzimuth() throws java.io.IOException;

  /**
   * @return the average azimuth of all the radials in the sweep, in degrees.
   *         Only valid if getType() == TYPE_
   */
  public float getMeanAzimuth();

  /**
   * @param radial which radial, must in interval [0,getRadialNumber())
   * @return the location of the origin of the ith radial.
   */
  public ucar.unidata.geoloc.EarthLocation getOrigin(int radial);

  /**
   * @param radial which radial, must in interval [0,getRadialNumber())
   * @return the time of the ith radial, in units of getTimeUnits().
   * @throws java.io.IOException on io error
   */
  public float getTime(int radial) throws IOException;

  /**
   * @return the starting time of the sweep, in units of getTimeUnits().
   */
  public Date getStartingTime();

  /**
   * @return the ending time of the sweep, in units of getTimeUnits().
   */
  public Date getEndingTime();

  /**
   * @return the index of sweep
   */
  public int getSweepIndex();

  /**
   * deallocate memory of sweep
   */
  public void clearSweepMemory();

}
