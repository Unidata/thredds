/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt;

import ucar.nc2.VariableSimpleIF;

import java.io.IOException;
import java.util.Date;

/**
 * A RadialDataset in which the radials can be grouped into sweeps.
 * A sweep has the same gate geometry for all radials in the sweep, and has a RadialDatasetSweep.Type.
 *
 * @author yuan
 */
public interface RadialDatasetSweep extends ucar.nc2.dt.TypedDataset {
  /* definition of data format for level2*/
  public static final String LevelII = "Level II";

  /**
   * @return radar id
   */
  public String getRadarID();

  /**
   * @return radar name
   */
  public String getRadarName();

  /**
   * @return data format
   */
  public String getDataFormat();

  /**
   * If all the sweeps are the same type, return it here, else NONE
   * @return type of sweep, or NONE
   */
  public RadialDatasetSweep.Type getCommonType();

  /**
   * If all sweeps have the same origin, return it here, else null
   * @return common origin, or null
   */
  public ucar.nc2.dt.EarthLocation getCommonOrigin();

  /**
   * Get the units of Calendar time.
   * To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   * To get units as a String, call DateUnit.getUnitsString().
   * @return units of Calendar time
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /**
   * Stationary versus moving radar.
   *
   * @return true if this is stationary.
   */
  public boolean isStationary();

  /**
   * Remove cached data, if any.
   */
  public void clearDatasetMemory();

  // public boolean isRadial();

  /**
   * Get the basic property of Radar,
   *
   * @return 0 if there is only one sweep
   */
  public boolean isVolume();

  /**
   * The radial data variables available in the dataset.
   *
   * @return List of type RadialDatasetSweep.RadialVariable
   */
  public java.util.List<VariableSimpleIF> getDataVariables();

  /**
   * A data Variable with radial geometry.
   */
  public interface RadialVariable extends ucar.nc2.VariableSimpleIF {
    /**
     * @return the number of sweeps for this Variable
     */
    public int getNumSweeps();

    /**
     * @param sweepNum which sweep
     * @return the sweepNum sweep
     */
    public Sweep getSweep(int sweepNum);

    /**
     * @return data, of length getNumSweep()  by getNumRadials() by getNumGates()
     * @throws java.io.IOException on io error
     */
    public float[] readAllData() throws java.io.IOException;

    public void clearVariableMemory();


  }

  /**
   * A sweep is 2D data using radial coordinate system (elevation, azimuth, radial distance)
   */
  public interface Sweep {
    /**
     * @return the type of the Sweep
     */
    public RadialDatasetSweep.Type getType();

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
    public ucar.nc2.dt.EarthLocation getOrigin(int radial);

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

  /**
   * A Type of RadialSweep.
   */
  static public final class Type {
    public final static Type NONE = new Type("");
    
    private static java.util.List<Type> members = new java.util.ArrayList<Type>(20);
    private String name;

    private Type(String s) {
      this.name = s;
      members.add(this);
    }

    public static java.util.Collection getAllTypes() {
      return members;
    }

    /**
     * Find the DataType that matches this name, ignore case.
     *
     * @param name : match this name
     * @return DataType or null if no match.
     */
    public static Type getType(String name) {
      if (name == null) return null;
      for (Type m : members) {
        if (m.name.equalsIgnoreCase(name))
          return m;
      }
      return null;
    }

    /**
     * @return the type name.
     */
    public String toString() {
      return name;
    }

  }

}

