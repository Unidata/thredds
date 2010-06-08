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
// $Id: TrajectoryObsDatasetFactory.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TrajectoryObsDataset;

import java.io.IOException;

/**
 * A factory for TrajectoryObsDataset.
 *
 * @author edavis
 * @since Feb 9, 2005T12:08:44 PM
 * @deprecated use ucar.nc2.dt.TypedDatasetFactory
 */
public class TrajectoryObsDatasetFactory
{

  static public TrajectoryObsDataset open( String netcdfFileURI) throws IOException {
     return open( netcdfFileURI, null);
  }

  static public TrajectoryObsDataset open( String netcdfFileURI, ucar.nc2.util.CancelTask cancelTask)
          throws  IOException
  {
    NetcdfDataset ds = NetcdfDataset.acquireDataset( netcdfFileURI, cancelTask);
    if ( RafTrajectoryObsDataset.isValidFile( ds) )
      return new RafTrajectoryObsDataset( ds);
    else if ( SimpleTrajectoryObsDataset.isValidFile( ds))
      return new SimpleTrajectoryObsDataset( ds);
    else if ( Float10TrajectoryObsDataset.isValidFile( ds))
      return new Float10TrajectoryObsDataset( ds);
    else if ( ZebraClassTrajectoryObsDataset.isValidFile( ds ) )
      return new ZebraClassTrajectoryObsDataset( ds );
    else if ( ARMTrajectoryObsDataset.isValidFile( ds ) )
      return new ARMTrajectoryObsDataset( ds );
    else
      return null;

  }
}
