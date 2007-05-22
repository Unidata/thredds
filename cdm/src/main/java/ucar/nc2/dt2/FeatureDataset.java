package ucar.nc2.dt2;

import ucar.nc2.VariableSimpleIF;

import java.util.*;

/**
 * Superclass for "scientific type" datasets.
 *
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public interface FeatureDataset {

  /** Title of the dataset. */
  public String getTitle();

  /** Text information about this dataset. */
  public String getDescription();

  /** The URI location of the dataset */
  public String getLocationURI();

  /** Start date for the entire dataset. */
  public Date getStartDate();

  /** End date for the entire dataset. */
  public Date getEndDate();

  /** The boundingBox for the entire dataset. */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /** List of global attributes.
   * @return List of type ucar.nc2.Attribute */
  public List getGlobalAttributes();

  /** Return the global attribute with the given name, ingnoring case. */
  public ucar.nc2.Attribute findGlobalAttributeIgnoreCase( String name );

  /** The data Variables available in this dataset.
   * Should just be data variable others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shape of this VariableSimpleIF does not necessarily match the 
   * @return List of type VariableSimpleIF */
  public List<VariableSimpleIF> getDataVariables();

  /** Get the named data Variable.
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null. */
  public VariableSimpleIF getDataVariable( String shortName);

  /** Return underlying NetcdfFile, or null if none. */
  public ucar.nc2.NetcdfFile getNetcdfFile();

  /** Close all resources associated with this dataset. */
  public void close() throws java.io.IOException;

  /** Show debug / underlying implementation details */
  public String getDetailInfo();
}