package ucar.nc2.dt;

import ucar.nc2.VariableSimpleIF;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * A dataset containing Radial data (eg radar).
 * Radial data uses a radial coordinate system (azimuth, elevation, distance).
 *
 * @author
 * @version $Revision: 1.2 $
 */
public interface RadialDatasetAll extends ucar.nc2.dt.TypedDataset {

  /** Get the units of Calendar time.
   *  To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   *  To get units as a String, call DateUnit.makeStandardDateString( double value).
   */
  public ucar.nc2.units.DateUnit getTimeUnits();

  /** The radial data variables available in the dataset.
   * @return List of type RadialVariable */
  public java.util.List getDataVariables();

  /**
   * Get the named radial data variable.
   * @param name radial data variable namea.
   * @return RadialVariable or null if not found.
   */
  public VariableSimpleIF getDataVariable( String name);

  ///////////////////////////////////////////////////////////////////
  /** A data variable whose data is a connected list of Radials. */
  public interface RadialVariable extends ucar.nc2.VariableSimpleIF {

    public int getNumSweeps();
    public RadialDatasetAll.Sweep getSweep(int nsweep) throws java.io.IOException;
    public RadialDatasetAll.Radial getRadial(int nradial) throws java.io.IOException;
    public List getAllRadials() throws java.io.IOException;
  }

  ///////////////////////////////////////////////////////////////////
 public interface Sweep {
    /* Get the type of the Sweep */
    public RadialDatasetSweep.Type getType();

    /** Number of sweeps for this Variable */
    public int getNumRadials();

    /** Get the actual data, of length getNumGates() by getNumRadials() */
    public float[] readData() throws java.io.IOException;

    /** Get the actual data of the given radial, of length getNumGates() */
    public float[] readData(int radial) throws java.io.IOException;

    /** Get the average elevation of all the radials in the sweep, in degrees.
     * Only valid if getType() == TYPE_ */
    public float getMeanElevation();

    /** Get the average azimuth of all the radials in the sweep, in degrees.
     * Only valid if getType() == TYPE_ */
    public float getMeanAzimuth();

    /** If all radials have the same origin, return it here, else null */
    public ucar.nc2.dt.EarthLocation  getCommonOrigin();

    /** Get the starting time of the sweep, in units of getTimeUnits(). */
    public float getStartingTime();

    /** Get the ending time of the sweep, in units of getTimeUnits(). */
    public float getEndingTime();
  }


  /** A radial has an origin and data along a straight line */
  public interface Radial {
    /** Get number of gates in this radial */
    public int getNumGates();

    /** Get the actual dataa, of length getNumGates */
    public float[] getData() throws java.io.IOException;

    /** Get the beam width, in degrees */
    public float getBeamWidth();

    /** Get the Nyquist Frequency, whatever that is */
    public float getNyquistFrequency();

    /**
     * Get the radial distance from origin to the start of the first data gate.
     * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
     * @return distance to first gate in meters */
    public float getRangeToFirstGate();

    /**
     *  Get the radial length of each data gate.
     * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
     * @return gate size in meters */
     public float getGateSize();

    /**
     * Get the elevation of the radial, measured from a horizontal earth tangent plane, increasing towards zenith
     * @return elevation in degrees
     */
    public float getElevation();

    /**
     * Get the azimuth of the radial, measured from true North, increasing clockwise
     * @return azimuth in degrees
     */
    public float getAzimuth();

    /** Location of the origin of the radial. */
    public ucar.nc2.dt.EarthLocation getOrigin();

    /** Time the data was measured, in units of getTimeUnits(). */
    public double getTime();
  }

}
