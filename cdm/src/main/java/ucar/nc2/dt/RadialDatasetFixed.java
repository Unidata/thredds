package ucar.nc2.dt;

import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.VariableSimpleIF;

import java.io.IOException;

/**
 * A dataset containing Radial data (eg radar) that is fixed at one location.
 * @author caron
 * @version $Revision: 1.5 $ $Date: 2005/05/23 20:18:36 $
 */
public interface RadialDatasetFixed extends RadialDataset {

  /** Location of the origin of the dataset*/
  public ucar.nc2.dt.EarthLocation getEarthLocation();

  /** The radial data variables available in the dataset.
   * @return List of type RadialVariableFixed */
  public java.util.List getDataVariables();

  /**
   * Get the named radial data variable.
   * @param name radial data variable namea.
   * @return RadialVariableFixed or null if not found.
   */
  public VariableSimpleIF getDataVariable( String name);

  /** A data variable that is a collection of Sweeps. */
  public interface RadialVariableFixed extends RadialDataset.RadialVariable {
    public int getNumSweeps();
    public Sweep getSweep(int sweepNum);
  }

  /** A sweep is 2D data using radial coordinate system (elevation, azimuth, radial distance) */
  public interface Sweep {
    public int getNumRadials();
    public int getNumGates();

    // a 2D array nradials * ngates
    public float[] readData() throws java.io.IOException;

    public float getElevation(int radial)  throws java.io.IOException;
    public float getMeanElevation();

    public float getAzimuth(int radial) throws java.io.IOException;
    public float getTime(int radial) throws IOException;

    // assume all this is constant for the sweep ??
    public float getBeamWidth(); // degrees
    public float getNyquistFrequency();
    public float getRangeToFirstGate(); // meters
    public float getGateSize(); // meters
  }

}
