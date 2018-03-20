/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * Implement this interface to add Coordinate Systems to a NetcdfDataset.
 * @author john caron
 */
public interface CoordSysBuilderIF {

  /**
   * Pass in the name of the Convention used to locate this CoordSysBuilderIF.
   * @param convName the name of the Convention
   */
  void setConventionUsed( String convName);

  /**
   * Get the name of the Convention. In the case where the Convention attribute is not set in the file,
   * this name cannot be used to identify the Convention. The isMine() method is called instead.
   * @return Convention name
   */
  String getConventionUsed();

  /**
   * Detailed information when the coordinate systems were parsed
   * @return String containing parsing info
   */
  String getParseInfo();

  /**
   * Specific advice to a user about problems with the coordinate information in the file.
   * @return String containing advice to a user about problems with the coordinate information in the file.
   */
  String getUserAdvice();

  /**
   * Make changes to the dataset that are needed before processing scale/offset in NetcdfDataset.
   *
   * @param ncDataset modify this dataset
   * @param cancelTask give user a chance to bail out
   * @throws java.io.IOException on error
   */
  void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;

  /**
   * Create the coordinate system objects: coordinate axes, coordinate systems, coordinate transforms.
   * @param ncDataset add to this dataset
   */
  void buildCoordinateSystems( NetcdfDataset ncDataset);

  /** Give advice for a user trying to figure out why things arent working
   * @param advice add this advice to the User Advice String
   */
  void addUserAdvice( String advice);
}