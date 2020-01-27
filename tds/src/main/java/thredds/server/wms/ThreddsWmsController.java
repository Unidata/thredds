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
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.dataset.TdsRequestedDataset;
import thredds.server.wms.config.WmsDetailedConfig;
import thredds.servlet.ServletUtil;
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
   * from /content/thredds/wmsConfig.xml. Also tells GeoToolkit that we are running
   * in a server environment.
   * @throws Exception if the config file could not be loaded for some reason.
   */
  @Override
  public void init() throws Exception
  {
    super.init();
    // tell geotoolkit that we are running on a server
    // and do not try to use systemPreferences
    // from https://github.com/opengeospatial/teamengine-offline/blob/6b81fc3fc4647e8f68ba433701199b0e68fc36d2/src/test-suite-dependencies/geotk-utility-3.21-sources/src/main/java/org/geotoolkit/lang/Setup.java#L99
    //
    // In current implementation, invoking {@link #initialize(Properties)}
    // with a property entry {@code platform=server} also disable the usage of
    // {@linkplain java.util.prefs.Preferences#systemRoot() system preferences}. This is a temporary
    // workaround for the JDK 6 behavior on Unix system, which display "<cite>WARNING: Couldn't flush
    // system prefs</cite>" if the {@code etc/.java} directory has not been created during the Java
    // installation process.
    //
    Properties geoToolkitProperties = new Properties();
    geoToolkitProperties.setProperty("platform", "server");
    org.geotoolkit.lang.Setup.initialize(geoToolkitProperties);
    ThreddsServerConfig tdsWmsServerConfig = (ThreddsServerConfig) this.serverConfig;
    logServerStartup.info( "WMS:allow= " + tdsWmsServerConfig.isAllow() );
    if ( tdsWmsServerConfig.isAllow() )
    {
      logServerStartup.info( "WMS:allowRemote= " + tdsWmsServerConfig.isAllowRemote() );
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
    ThreddsServerConfig tdsWmsServerConfig = (ThreddsServerConfig) this.serverConfig;
    if ( ! tdsWmsServerConfig.isAllow() )
    {
      log.debug( "dispatchWmsRequest(): WMS service not supported." );
      httpServletResponse.sendError( HttpServletResponse.SC_FORBIDDEN, "WMS service not supported." );
      return null;
    }

    GridDataset gd = null;
    try
    {
      TdsRequestedDataset reqDataset = new TdsRequestedDataset( httpServletRequest );
      if ( reqDataset.isRemote() && ! tdsWmsServerConfig.isAllowRemote() )
      {
        log.debug( "dispatchWmsRequest(): WMS service not supported for remote datasets." );
        throw new WmsException( "WMS service not supported for remote (non-server-resident) datasets.", "LayerNotDefined");
      }

      try {
        gd = reqDataset.openAsGridDataset( httpServletRequest, httpServletResponse );
      }
      catch ( FileNotFoundException e ) {
        // LOOK ToDo Instead could catch FileNotFoundExceptions below and also add to exceptionResolver in wms-servlet.xml 
        log.debug( "dispatchWmsRequest(): File not found [{}]:{}.", reqDataset.getPath(), e.getMessage());
        throw new LayerNotDefinedException( reqDataset.getPath());
      }
      catch ( Exception e ) {
        log.error( "dispatchWmsRequest()on [" + reqDataset.getPath() + "]:", e);
        httpServletResponse.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return null;
      }
      if ( gd == null )
      {
        // We have sent an auth challenge to the client, so we send no
        // further information
        return null;
      }

      // Extract the metadata from the GridDataset to form a Dataset object
      // TODO: what to use for the dataset ID?
      // TODO: It can be inefficient to create an entire {@link ThreddsDataset} object when
      // all we need is a single layer. This means that a lot of unnecessary objects will be
      // created when only a single layer is needed, e.g. for a GetMap operation. Should create
      //  a means to extract a single layer without creating a whole dataset; however, this
      //  could be tricky when dealing with virtual layers (e.g. velocities).      
      //ThreddsDataset ds = new ThreddsDataset( reqDataset.getPath(), gd, this.wmsConfig );
      
      ThreddsDataset ds = ThreddsDataset.getThreddsDatasetForRequest(request, gd, reqDataset, this.wmsConfig, params );
      // Create an object that extracts layers from the dataset
      ThreddsLayerFactory layerFactory = new ThreddsLayerFactory( ds );

      ModelAndView modelAndView;
      switch (request) {
        case "GetCapabilities":
          // The Capabilities document will contain a single dataset
          Collection<? extends Dataset> datasets = Arrays.asList(ds);
          // In THREDDS we don't know the last update time so we use null
          modelAndView = getCapabilities(datasets, null, params, httpServletRequest, usageLogEntry);
          //httpServletResponse.setContentType(ContentType.xml.toString());
          break;
        case "GetMap":
          modelAndView = getMap(params, layerFactory, httpServletResponse, usageLogEntry);
          break;
        case "GetFeatureInfo":
          modelAndView = getFeatureInfo(params, layerFactory, httpServletRequest, httpServletResponse, usageLogEntry);
          break;
        // The REQUESTs below are non-standard and could be refactored into
        // a different servlet endpoint
        case "GetMetadata":
          ThreddsMetadataController tms =
                  new ThreddsMetadataController(layerFactory, tdsWmsServerConfig, ds);
          // This is a request for non-standard metadata.  (This will one
          // day be replaced by queries to Capabilities fragments, if possible.)
          // Delegate to the ThreddsMetadataController
          modelAndView = tms.handleRequest(httpServletRequest, httpServletResponse, usageLogEntry);
          break;
        case "GetLegendGraphic":
          // This is a request for an image that contains the colour scale
          // and range for a given layer
          modelAndView = getLegendGraphic(params, layerFactory, httpServletResponse);
          break;
        case "GetTransect":
          modelAndView = getTransect(params, layerFactory, httpServletResponse, usageLogEntry);
          break;
        case "GetVerticalProfile":
          modelAndView = getVerticalProfile(params, layerFactory, httpServletResponse, usageLogEntry);
          break;
        case "GetVerticalSection":
          modelAndView = getVerticalSection(params, layerFactory, httpServletResponse, usageLogEntry);
          break;
        default:
          throw new OperationNotSupportedException(request);
      }

      return modelAndView;
    }
    catch ( LayerNotDefinedException e ) {
      log.debug( "dispatchWmsRequest(): LayerNotDefinedException: " + e.getMessage());
      throw e;
    }
    catch ( WmsException e ) {
      log.debug( "dispatchWmsRequest(): WmsException: "  , e );
      throw e;
    }
    catch ( thredds.server.dataset.DatasetException e ) {
      log.error( "dispatchWmsRequest(): DatasetException: " + e.getMessage() );
      throw new WmsException( e.getMessage() );
    }
    catch ( java.net.SocketException e ) {
      log.debug( "dispatchWmsRequest(): SocketException: " + e.getMessage());
      httpServletResponse.setStatus(ServletUtil.STATUS_CLIENT_ABORT);
      return null;
    }
    catch ( IOException e ) {
      if ( e.getClass().getName().equals( "org.apache.catalina.connector.ClientAbortException")) {
        log.debug("dispatchWmsRequest(): ClientAbortException: " + e.getMessage());
        return null;
      }
      log.error( "dispatchWmsRequest(): IOException: ", e );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    catch ( Exception e ) {
      log.error( "dispatchWmsRequest(): Exception: ", e );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    catch ( Error e ) {
      log.error( "dispatchWmsRequest(): Error: ", e );
      if ( httpServletResponse.isCommitted() )
        return null;
      throw e;
    }
    finally {
      // We ensure that the GridDataset object is closed
      if ( gd != null) gd.close();
    }
  }

}
