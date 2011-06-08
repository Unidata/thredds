package thredds.server.dataset;

import thredds.server.dataset.DatasetException;
import thredds.server.wms.ThreddsWmsController;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Extracts the dataset ID from the HttpServletRequest and determines if it is
 * a local dataset path or a remote dataset URL.
 *
 * <p>The requested dataset can be opened by using the
 * {@link #openAsGridDataset(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method.
 */
public class TdsRequestedDataset
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private boolean isRemote = false;
  private String path;

  public TdsRequestedDataset( HttpServletRequest request )
          throws DatasetException
  {
    path = TdsPathUtils.extractPath( request );
    if ( path == null )
    {
      path = ServletUtil.getParameterIgnoreCase( request, "dataset" );
      isRemote = ( path != null );
    }
    if ( path == null )
    {
      log.debug( "Request does not specify a dataset." );
      throw new DatasetException( "Request does not specify a dataset." );
    }
  }

  /**
   * Open the requested dataset as a GridDataset. If the request requires an
   * authentication challenge, a challenge will be sent back to the client using
   * the response object, and this method will return null.  (This is the only
   * circumstance in which this method will return null.)
   *
   * @param request the HttpServletRequest
   * @param response the HttpServletResponse
   * @return the requested dataset as a GridDataset
   * @throws java.io.IOException if have trouble opening the dataset
   */
  public GridDataset openAsGridDataset( HttpServletRequest request,
                                        HttpServletResponse response )
          throws IOException
  {
    return isRemote ? ucar.nc2.dt.grid.GridDataset.open( path )
                    : DatasetHandler.openGridDataset( request, response, path );
  }

  /**
   * Open the requested dataset as a NetcdfFile. If the request requires an
   * authentication challenge, a challenge will be sent back to the client using
   * the response object, and this method will return null.  (This is the only
   * circumstance in which this method will return null.)
   *
   * @param request the HttpServletRequest
   * @param response the HttpServletResponse
   * @return the requested dataset as a NetcdfFile
   * @throws java.io.IOException if have trouble opening the dataset
   */
  public NetcdfFile openAsNetcdfFile( HttpServletRequest request,
                                      HttpServletResponse response )
          throws IOException
  {
    return isRemote ? NetcdfDataset.openDataset( path )
                    : DatasetHandler.getNetcdfFile( request, response, path );
  }

  public boolean isRemote() {
    return isRemote;
  }

  public String getPath() {
    return path;
  }
}
