/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package thredds.server.wms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.wms.config.WmsDetailedConfig;
import thredds.servlet.DatasetHandler;
import thredds.servlet.ServletUtil;
import thredds.servlet.UsageLog;
import thredds.util.TdsPathUtils;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.OperationNotSupportedException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * <p>WmsController for THREDDS</p>
 *
 * @author Jon Blower
 */
public final class ThreddsWmsController extends AbstractWmsController
{
  private static final Logger log = LoggerFactory.getLogger( ThreddsWmsController.class );
  private final Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  private WmsDetailedConfig wmsConfig;

  private static final class ThreddsLayerFactory implements LayerFactory
  {
    private ThreddsDataset ds;

    public ThreddsLayerFactory( ThreddsDataset ds )
    {
      this.ds = ds;
    }

    @Override
    public Layer getLayer( String layerName ) throws LayerNotDefinedException
    {
      ThreddsLayer layer = ds.getLayerById( layerName );
      if ( layer == null ) throw new LayerNotDefinedException( layerName );
      return layer;
    }
  }

  /**
   * Called by Spring to initialize the controller. Loads the WMS configuration
   * from /content/thredds/wmsConfig.xml.
   * @throws Exception if the config file could not be loaded for some reason.
   */
  @Override
  public void init() throws Exception
  {
    super.init();
    ThreddsServerConfig tdsWmsServerConfig = (ThreddsServerConfig) this.serverConfig;
    logServerStartup.error( "WMS:allow= " + tdsWmsServerConfig.isAllow() );
    if ( tdsWmsServerConfig.isAllow() )
    {
      logServerStartup.error( "WMS:allowRemote= " + tdsWmsServerConfig.isAllowRemote() );
      File wmsConfigFile = tdsWmsServerConfig.getTdsContext().getConfigFileSource().getFile( "wmsConfig.xml" );
      if ( wmsConfigFile == null || !wmsConfigFile.exists() || !wmsConfigFile.isFile() )
      {
        tdsWmsServerConfig.setAllow( false );
        logServerStartup.error( "init(): Disabling WMS: Could not find wmsConfig.xml. [Default version available at ${TOMCAT_HOME}/webapps/thredds/WEB-INF/altContent/startup/wmsConfig.xml." );
        return;
      }
      this.wmsConfig = WmsDetailedConfig.fromFile( wmsConfigFile );
      logServerStartup.info( "init(): Loaded WMS configuration from wmsConfig.xml" );
    }
  }

  @Override
  protected ModelAndView dispatchWmsRequest(
          String request,
          RequestParams params,
          HttpServletRequest httpServletRequest,
          HttpServletResponse httpServletResponse,
          UsageLogEntry usageLogEntry ) throws Exception
  {
    log.info( UsageLog.setupRequestContext( httpServletRequest ) );

    ThreddsServerConfig tdsWmsServerConfig = (ThreddsServerConfig) this.serverConfig;
    if ( ! tdsWmsServerConfig.isAllow() )
    {
      log.info( "dispatchWmsRequest(): WMS service not supported." );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, -1 ) );
      httpServletResponse.sendError( HttpServletResponse.SC_FORBIDDEN, "WMS service not supported." );
      return null;
    }

    GridDataset gd = null;
    try
    {
      RequestedDataset reqDataset = new RequestedDataset( httpServletRequest );
      if ( reqDataset.isRemote() && ! tdsWmsServerConfig.isAllowRemote() )
      {
        log.info( "dispatchWmsRequest(): WMS service not supported for remote datasets." );
        throw new WmsException( "WMS service not supported for remote (non-server-resident) datasets.", "LayerNotDefined");
      }

      try {
        gd = reqDataset.openAsGridDataset( httpServletRequest, httpServletResponse );
      }
      catch ( FileNotFoundException e ) {
        // LOOK ToDo Instead could catch FileNotFoundExceptions below and also add to exceptionResolver in wms-servlet.xml 
        log.info( "dispatchWmsRequest(): File not found [" + reqDataset.getPath() + "]:" + e.getMessage() + "." );
        throw new LayerNotDefinedException( reqDataset.getPath());
      }
      if ( gd == null )
      {
        // We have sent an auth challenge to the client, so we send no
        // further information
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_UNAUTHORIZED, -1 ) );
        return null;
      }

      // Extract the metadata from the GridDataset to form a Dataset object
      // TODO: what to use for the dataset ID?
      // TODO: It can be inefficient to create an entire {@link ThreddsDataset} object when
      // all we need is a single layer. This means that a lot of unnecessary objects will be
      // created when only a single layer is needed, e.g. for a GetMap operation. Should create
      //  a means to extract a single layer without creating a whole dataset; however, this
      //  could be tricky when dealing with virtual layers (e.g. velocities).
      ThreddsDataset ds = new ThreddsDataset( reqDataset.getPath(), gd, this.wmsConfig );
      // Create an object that extracts layers from the dataset
      ThreddsLayerFactory layerFactory = new ThreddsLayerFactory( ds );

      ModelAndView modelAndView;
      if ( request.equals( "GetCapabilities" ) )
      {
        // The Capabilities document will contain a single dataset
        Collection<? extends Dataset> datasets = Arrays.asList( ds );
        // In THREDDS we don't know the last update time so we use null
        modelAndView = getCapabilities( datasets, null, params, httpServletRequest, usageLogEntry );
      }
      else if ( request.equals( "GetMap" ) )
      {
        modelAndView = getMap( params, layerFactory, httpServletResponse, usageLogEntry );
      }
      else if ( request.equals( "GetFeatureInfo" ) )
      {
        modelAndView = getFeatureInfo( params, layerFactory, httpServletRequest, httpServletResponse, usageLogEntry );
      }
      // The REQUESTs below are non-standard and could be refactored into
      // a different servlet endpoint
      else if (request.equals("GetMetadata"))
      {
        ThreddsMetadataController tms =
            new ThreddsMetadataController(layerFactory, tdsWmsServerConfig, ds);
        // This is a request for non-standard metadata.  (This will one
        // day be replaced by queries to Capabilities fragments, if possible.)
        // Delegate to the ThreddsMetadataController
        modelAndView = tms.handleRequest( httpServletRequest, httpServletResponse, usageLogEntry );
      }
      else if ( request.equals( "GetLegendGraphic" ) )
      {
        // This is a request for an image that contains the colour scale
        // and range for a given layer
        modelAndView = getLegendGraphic( params, layerFactory, httpServletResponse );
      }
      else if ( request.equals( "GetTransect" ) )
      {
        modelAndView = getTransect( params, layerFactory, httpServletResponse, usageLogEntry );
      }
      else
      {
        throw new OperationNotSupportedException( request );
      }

      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
      return modelAndView;
    }
    catch ( LayerNotDefinedException e ) {
      log.debug( "dispatchWmsRequest(): LayerNotDefinedException: " + e.getMessage());
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, -1 ) );
      throw e;
    }
    catch ( WmsException e ) {
      log.debug( "dispatchWmsRequest(): WmsException: "  + e.getMessage() );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      throw e;
    }
    catch ( java.net.SocketException e ) {
      log.debug( "dispatchWmsRequest(): SocketException: " + e.getMessage());
      log.info( UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_CLIENT_ABORT, -1 ) );
      httpServletResponse.setStatus(ServletUtil.STATUS_CLIENT_ABORT);
      return null;
    }
    catch ( IOException e ) {
      if ( e.getClass().getName().equals( "org.apache.catalina.connector.ClientAbortException")) {
        log.debug( "dispatchWmsRequest(): ClientAbortException: " + e.getMessage() );
        log.info( UsageLog.closingMessageForRequestContext( ServletUtil.STATUS_CLIENT_ABORT, -1 ) );
        return null;
      }
      log.error( "dispatchWmsRequest(): IOException: ", e );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1 ) );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    catch ( Exception e ) {
      log.error( "dispatchWmsRequest(): Exception: ", e );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1 ) );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    catch ( Error e ) {
      log.error( "dispatchWmsRequest(): Error: ", e );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1 ) );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    finally {
      // We ensure that the GridDataset object is closed
      if ( gd != null) gd.close();
    }
  }

  /**
   * Extracts the dataset ID from the HttpServletRequest and determines if it is
   * a local dataset path or a remote dataset URL.
   *
   * <p>The requested dataset can be opened by using the
   * {@link #openAsGridDataset(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
   * method.
   */
  private static class RequestedDataset
  {
    private boolean isRemote = false;
    private String path;

    RequestedDataset( HttpServletRequest request) throws WmsException
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
        throw new WmsException( "Request does not specify a dataset." );
      }
    }

    /**
     * Open the requested dataset as a GridDataset. If the request requires an
     * authentication challenge, a challenge will be sent back to the client using
     * the response object, and this method will return null.  (This is the only
     *  circumstance in which this method will return null.)
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @return the requested dataset as a GridDataset
     * @throws IOException if have trouble opening the dataset
     */
    public GridDataset openAsGridDataset( HttpServletRequest request,
                                          HttpServletResponse response )
            throws IOException
    {
      return isRemote ? ucar.nc2.dt.grid.GridDataset.open( path )
                      : DatasetHandler.openGridDataset( request, response, path );
    }

    public boolean isRemote() {
      return isRemote;
    }

    public String getPath() {
      return path;
    }
  }
}
