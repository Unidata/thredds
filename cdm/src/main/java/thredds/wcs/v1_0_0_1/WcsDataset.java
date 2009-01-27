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
package thredds.wcs.v1_0_0_1;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;

import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsDataset
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( WcsDataset.class );

  // Dataset specific
  private String datasetPath;
  private String datasetName;
  private GridDataset dataset;
  private HashMap<String, WcsCoverage> availableCoverages;

  public WcsDataset( GridDataset dataset, String datasetPath )
  {
    this.datasetPath = datasetPath;
    int pos = datasetPath.lastIndexOf( "/" );
    this.datasetName = ( pos > 0 ) ? datasetPath.substring( pos + 1 ) : datasetPath;
    this.dataset = dataset;

    this.availableCoverages = new HashMap<String, WcsCoverage>();

    // ToDo WCS 1.0PlusPlus - compartmentalize coverage to hide GridDatatype vs GridDataset.Gridset ???
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.0 coverage for each parameter
    for ( GridDatatype curGridDatatype : this.dataset.getGrids() )
    {
      GridCoordSystem gcs = curGridDatatype.getCoordinateSystem();
      if ( !gcs.isRegularSpatial() )
        continue;
      this.availableCoverages.put( curGridDatatype.getName(), new WcsCoverage( curGridDatatype, this) );
    }
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.1 style coverage for each coordinate system
    // for ( GridDataset.Gridset curGridSet : this.dataset.getGridsets())
    // {
    //   GridCoordSystem gcs = curGridSet.getGeoCoordSystem();
    //   if ( !gcs.isRegularSpatial() )
    //     continue;
    //   this.availableCoverages.put( gcs.getName(), curGridSet );
    // }
  }

  public String getDatasetPath() { return datasetPath; }
  public String getDatasetName() { return datasetName; }
  public GridDataset getDataset() { return dataset; }

  public void close() throws IOException
  {
    if ( this.dataset != null )
      this.dataset.close();
  }

  public boolean isAvailableCoverageName( String name )
  {
    return availableCoverages.containsKey( name );
  }

  public WcsCoverage getAvailableCoverage( String name )
  {
    return availableCoverages.get( name );
  }

  public Collection<WcsCoverage> getAvailableCoverageCollection()
  {
    return Collections.unmodifiableCollection( availableCoverages.values() );
  }
}
