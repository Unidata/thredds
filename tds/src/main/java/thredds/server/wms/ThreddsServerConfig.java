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
import thredds.servlet.*;
import thredds.util.TdsPathUtils;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.controller.AbstractServerConfig;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
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
  private org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  private TdsContext tdsContext;
  private DataRootHandler dataRootHandler;

  private boolean allow;
  private boolean allowRemote;

  private String defaultPaletteLocation;
  private String paletteLocation;

  private ThreddsServerConfig() {}

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public String getDefaultPaletteLocation() {
    return this.defaultPaletteLocation;
  }

  public void setDefaultPaletteLocation( String defaultPaletteDirectory) {
    this.defaultPaletteLocation = defaultPaletteDirectory;
  }

  public String getPaletteLocation() {
    return this.paletteLocation;
  }

  public void setPaletteLocation( String paletteLocation ) {
    this.paletteLocation = paletteLocation;
  }


///*
//  public void init()
//  {
//    allow = ThreddsConfig.getBoolean( "WMS.allow", false );
//    allowRemote = ThreddsConfig.getBoolean( "WMS.allowRemote", false );
//
//    if ( allow )
//    {
//
//    }
//    // Configure color palette directory.
//    String wmsPaletteDirectory = ThreddsConfig.get( "WMS.paletteLocationDir", null );
//    if ( wmsPaletteDirectory != null )
//      this.setPaletteLocation( wmsPaletteDirectory );
//
//      String paletteLocation = ThreddsConfig.get( "WMS.paletteLocationDir",
//                                                this.getServletContext().getRealPath( "/WEB-INF/palettes" ) );
//
//    if ( paletteLocation == null )
//    {
//      // Default palette directory not found!!!
//      allow = allowRemote = false;
//      logServerStartup.error( "Palette location not configured and default location not found." +
//                              "\n**** Disabling WMS - check palette configuration: "
//                              + UsageLog.closingMessageNonRequestContext() );
//      return;
//    }
//    File paletteLocationDir = new File( paletteLocation );
//    if ( !paletteLocationDir.isAbsolute() )
//    {
//      paletteLocationDir = tdsContext.getConfigFileSource().getFile( paletteLocation );
//      if ( paletteLocationDir == null )
//      {
//        // User configured palette directory not found!!!
//        allow = allowRemote = false;
//        logServerStartup.error( "Palette location [" + paletteLocation + "] not found." +
//                                "\n**** Disabling WMS - check palette configuration: "
//                                + UsageLog.closingMessageNonRequestContext() );
//        return;
//      }
//    }
//
//    if ( paletteLocationDir.exists() && paletteLocationDir.isDirectory() )
//    {
//      ColorPalette.loadPalettes( paletteLocationDir );
//      logServerStartup.debug( "Loaded palettes from " + paletteLocation );
//    }
//    else
//    {
//      // Palette directory doesn't exist or isn't directory!!!
//      allow = allowRemote = false;
//      logServerStartup.error( "Palette location directory [" + paletteLocation + "] doesn't exist or isn't a directory." +
//                              "\n**** Disabling WMS - check palette configuration: "
//                              + UsageLog.closingMessageNonRequestContext() );
//      return;
////        logServerStartup.warn( "Directory of palette files does not exist or is not a directory.  paletteLocation=" + paletteLocation );
////        ColorPalette.loadPalettes( paletteLocationDir );
//    }
//
//    colorRange = new HashMap<String, ColorScaleRange>();
//
//
//
//
//
//
//    // We initialize the ColorPalettes.  We need to do this from here
//    // because we need a way to find out the real path of the
//    // directory containing the palettes.  Therefore we need a way of
//    // getting at the ServletContext object, which isn't available from
//    // the ColorPalette class.
//    tdsContext.getRootDirectory();
//
//    String paletteLocation = this.getWebApplicationContext()
//            .getServletContext().getRealPath( "/WEB-INF/conf/palettes" );
//    File paletteLocationDir = new File( paletteLocation );
//    if ( paletteLocationDir.exists() && paletteLocationDir.isDirectory() )
//    {
//      ColorPalette.loadPalettes( paletteLocationDir );
//    }
//    else
//    {
//      log.info( "Directory of palette files does not exist or is not a directory" );
//    }
//
//  }
//
//*/
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
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public int getMaxImageWidth() {
      // ToDo Fully implement with ThreddsConfig info.
      return 2048;
    }

    @Override
    public int getMaxImageHeight() {
      // ToDo Fully implement with ThreddsConfig info.
      return 2048;
    }

    @Override
    public String getAbstract() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public Set<String> getKeywords() {
      // ToDo Fully implement with ThreddsConfig info.
      return Collections.emptySet();
    }

    @Override
    public String getServiceProviderUrl() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public String getContactName() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public String getContactOrganization() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public String getContactTelephone() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

    @Override
    public String getContactEmail() {
      // ToDo Fully implement with ThreddsConfig info.
      return "Still need to get threddsConfig.xml info.";
    }

}