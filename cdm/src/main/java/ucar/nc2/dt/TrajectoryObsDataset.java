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
package ucar.nc2.dt;

import java.util.List;

/** A collection of TrajectoryObsDatatype.
 *
 * To open a data file as a TrajectoryObsDataset and access the trajectories it
 * contains use TrajectoryObsDatasetFactory.open() and TrajectoryObsDataset.getTrajectories(),
 * for example:
 *
 * <pre>
    TrajectoryObsDataset trajDs = TrajectoryObsDatasetFactory.open (uriString);
    for ( Iterator it = trajDs.getTrajectories().iterator(); it.hasNext(); )
    {
      TrajectoryObsDatatype traj = (TrajectoryObsDatatype) it.next();
    }
   </pre>
 *
 * @author caron
 */
public interface TrajectoryObsDataset extends ucar.nc2.dt.TypedDataset {

//  /** Get a description of the data variables in the StructureData.
//   * return List of type TypedDataVariable
//   */
//  public java.util.List getMemberVariables();
//
//  /**
//   * Get the named data variable.
//   * @param name valid member name from the StructureData.
//   * @return TypedDataVariable corresponding to a StructureData member name.
//   */
//  public ucar.nc2.dt.TypedDataVariable getMemberVariable( String name);

  /**
   * Get a list of String IDs for the available trajectories.
   * @return list of ids for this dataset
   */
  public List<String> getTrajectoryIds();

  /** Get trajectories contained in this dataset.
   *  @return List of type TrajectoryObsDatatype.
   */
  public List getTrajectories(); // throws IOException;

  /** Get the named trajectory
   * @param trajectoryId id of trajectory
   * @return the named trajectory
   */
  public TrajectoryObsDatatype getTrajectory( String trajectoryId); // throws IOException;

  /**
   * Get an efficient iterator over all the data in the TrajectoryObsDataset.
   *
   * This is the efficient way to get all the data, it can be 100 times faster than getData().
   * This will return an iterator over type getDataClass(), and the actual data has already been read
   * into memory, that is, dataType.getData() will not incur any I/O.
   * <p> This is accomplished by buffering bufferSize amount of data at once. You must fully process the
   * data, or copy it out of the StructureData, as you iterate over it, in order for the garbage collector
   * to work.
   * <p> We dont need a cancelTask, just stop the iteration if the user want to cancel.
   *
   * @param bufferSize if > 0, the internal buffer size, else use the default. Typically 100k - 1M for best results.
   * @return Iterator over type getDataClass(), no guaranteed order.
   * @throws java.io.IOException
   */
  //public java.util.Iterator getDataIterator( int bufferSize ) throws IOException;

  /**
   * Syncronize with the underlying dataset if it has been extended in a way
   * that is compatible with the existing structural metadata (for instance,
   * if the unlimited dimension has grown). Return true if syncronization was
   * needed and sucessful. Otherwise, return false.
   *
   * NOTE: For now, assuming growth of the unlimited dimension only allowed change.
   * To get range for new extent only, use getRange( oldNumPoints + 1, newNumPoints, 1)
   *
   * @return true if syncronization was needed and sucessful, otherwise false.
   */
  public boolean syncExtend();
}
