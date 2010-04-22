/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.wms;

import org.joda.time.DateTime;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.controller.AbstractServerConfig;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * {@link uk.ac.rdg.resc.ncwms.controller.ServerConfig} for a THREDDS Data Server.  This is injected by Spring
 * into the {@link uk.ac.rdg.resc.ncwms.controller.WmsController} to provide access to data and metadata.
 * @todo There is an inefficiency here: each call to {@link #getDatasetById(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * will generate a new {@link ThreddsDataset} object, which will contain
 * all the child layers.  This means that a lot of unnecessary objects will be
 * created when only a single layer is needed, e.g. for a GetMap operation.
 * An alternative approach would be to override {@link #getLayerByUniqueName(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * and only create the layer in question; but this is a bit complicated because
 * the requested layer might be a dynamically-created "virtual" layer (e.g.
 * a vector layer).
 * @author Jon
 */
public class ThreddsServerConfig extends AbstractServerConfig
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private DataRootHandler dataRootHandler;

  private ThreddsServerConfig() {}

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void init()
  {

  }
  
    /**
     * Returns false: THREDDS servers can't produce a capabilities document
     * containing all datasets.
     */
    @Override
    public boolean getAllowsGlobalCapabilities() {
        return false;
    }

    /**
     * Returns null: THREDDS servers can't produce a collection of all the
     * datasets they hold
     */
    @Override
    public Map<String, ? extends Dataset> getAllDatasets() {
        return null;
    }

    /**
     * Returns the current time.  THREDDS servers don't cache their metadata
     * so the datasets could change at any time.  This effectively means that
     * clients should not cache Capabilities documents from THREDDS servers for
     * any "significant" period of time, to prevent inconsistencies between
     * client and server.
     */
    @Override
    public DateTime getLastUpdateTime() {
        return new DateTime();
    }

  /**
   * {@inheritDoc}
   *
   * This implementation ignores the given datasetId and determines the desired
   * dataset from the request object.
   *
   * @param datasetId always ignored.
   * @param request the HttpServletRequest
   * @param response the HttpServletResponse
   * @return the Dataset or null if the requested dataset is not found.
   * @throws IOException if couldn't access the dataset.
   */
    @Override
    public Dataset getDatasetById( String datasetId, HttpServletRequest request,
                                   HttpServletResponse response )
          throws IOException, WmsException
    {
      String datasetPath = TdsPathUtils.extractPath( request );
      boolean isRemote = false;
      if ( datasetPath == null )
      {
        datasetPath = ServletUtil.getParameterIgnoreCase( request, "dataset" );
        isRemote = ( datasetPath != null );
      }
      if ( datasetPath == null )
      {
        log.debug( "Request does not specify a dataset." );
        throw new WmsException( "Request does not specify a dataset." );
      }

      GridDataset gridDataset = isRemote ? ucar.nc2.dt.grid.GridDataset.open( datasetPath )
                                         : DatasetHandler.openGridDataset( request, response, datasetPath );
      if ( gridDataset == null )
        return null;

      ThreddsDataset threddsDataset = new ThreddsDataset( datasetPath, gridDataset );
      if ( !isRemote )
      {
        // @ToDo Get title and such from InvDataset???

      }
      return threddsDataset;
    }

  @Override
  public Layer getLayerByUniqueName( String uniqueLayerName, HttpServletRequest request, HttpServletResponse response )
          throws WmsException, IOException
  {
    Dataset dataset = this.getDatasetById( null, request, response );
    return dataset.getLayerById( uniqueLayerName );
  }

  //// The methods below should be easily populated from existing THREDDS
    //// metadata or the OGCMeta.xml file

    @Override
    public String getTitle() {
//      this.tdsContext.getHtmlConfig();
//      this.folderIconUrl = ThreddsConfig.get( "htmlSetup.folderIconUrl", "" );

      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public int getMaxImageWidth() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public int getMaxImageHeight() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getAbstract() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Set<String> getKeywords() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getServiceProviderUrl() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getContactName() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getContactOrganization() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getContactTelephone() {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public String getContactEmail() {
        throw new UnsupportedOperationException("Implement me!");
    }

}