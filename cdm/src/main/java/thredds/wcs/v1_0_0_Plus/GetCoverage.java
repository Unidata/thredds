package thredds.wcs.v1_0_0_Plus;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.util.DiskCache2;
import ucar.ma2.InvalidRangeException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCoverage extends WcsRequest
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( GetCoverage.class );

  private String coverageId;

  public GetCoverage( Operation operation, String version, String datasetPath, GridDataset dataset,
                      String coverageId )
  {
    super( operation, version, datasetPath, dataset);
    this.coverageId = coverageId;
    if ( this.coverageId == null )
      throw new IllegalArgumentException( "Non-null coverage identifier required." );
  }

  //public NetcdfFile getCoverageData() {}

  static private DiskCache2 diskCache = null;

  static public void setDiskCache( DiskCache2 _diskCache )
  {
    diskCache = _diskCache;
  }

  static private DiskCache2 getDiskCache()
  {
    if ( diskCache == null )
    {
      log.error( "getDiskCache(): Disk cache has not been set." );
      throw new IllegalStateException( "Disk cache must be set before calling GetCoverage.getDiskCache()." );
      //diskCache = new DiskCache2( "/wcsCache/", true, -1, -1 );
    }
    return diskCache;
  }


  public File writeCoverageDataToFile()
          throws WcsException
  {
    File ncFile = getDiskCache().getCacheFile( this.getDatasetPath() + "-" + coverageId + ".nc" );

    NetcdfCFWriter writer = new NetcdfCFWriter();
    try
    {
      writer.makeFile( ncFile.getPath(), this.getDataset(),
                       Collections.singletonList( coverageId ), null, null,
                       //                     Collections.singletonList( req.getCoverage() ),
                       //                     req.getBoundingBox(), dateRange,
                       true, 1, 1, 1 );
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + coverageId + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.CoverageNotDefined, "", "Failed to subset coverage <" + coverageId + ">." );
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + coverageId + ">: " + e.getMessage() );
      throw new WcsException( WcsException.Code.UNKNOWN, "", "Problem creating coverage <" + coverageId + ">." );
    }
    return ncFile;

  }
}
