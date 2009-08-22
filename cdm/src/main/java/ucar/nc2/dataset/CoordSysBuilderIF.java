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
  public void setConventionUsed( String convName);

  /**
   * Get the name of the Convention. In the case where the Convention attribute is not set in the file,
   *  this name cannot be used to identify the Convention. The isMine() method is called instead.
   * @return Convention name
   */
  public String getConventionUsed();

  /**
   * Detailed information when the coordinate systems were parsed
   * @return String containing parsing info
   */
  public String getParseInfo();

  /**
   * Specific advice to a user about problems with the coordinate information in the file.
   * @return String containing advice to a user about problems with the coordinate information in the file.
   */
  public String getUserAdvice();

  /**
   * Make changes to the dataset that are needed before processing scale/offset in NetcdfDataset.
   *
   * @param ncDataset modify this dataset
   * @param cancelTask give user a chance to bail out
   * @throws java.io.IOException on error
   */
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;

  /**
   * Create the coordinate system objects: coordinate axes, coordinate systems, coordinate transforms.
   * @param ncDataset add to this dataset
   */
  public void buildCoordinateSystems( NetcdfDataset ncDataset);

  /** Give advice for a user trying to figure out why things arent working
   * @param advice add this advice to the User Advice String
   */
  public void addUserAdvice( String advice);
}