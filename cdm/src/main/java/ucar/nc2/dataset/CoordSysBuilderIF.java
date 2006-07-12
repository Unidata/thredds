package ucar.nc2.dataset;

import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * Implement this interface to add Coordinate Systems to a NetcdfDataset.
 * @author john caron
 * @version $Revision$ $Date$
 */
public interface CoordSysBuilderIF {
  /**
   * This will be called first. 
   * @param convName the name of the Convention used to locate this CoordSysBuilderIF.
   */
  public void setConventionUsed( String convName);

  /**
   * Make changes to the dataset, like adding new variables, attribuites, etc.
   *
   * @param ncDataset modify this dataset
   * @param cancelTask give user a chance to bail out
   * @throws java.io.IOException
   */
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;

  /**
   * Create the coordinate system objects: coordinate axes, coordinate systems, coordinate transforms.
   * @param ncDataset add to this dataset
   */
  public void buildCoordinateSystems( NetcdfDataset ncDataset);

  /** Give advice for a user trying to figure out why things arent working */
  public void addUserAdvice( String advice);

}
