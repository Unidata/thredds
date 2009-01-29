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
   * deallocated memory of sweep
   */
  public void clearSweepMemory();

}
