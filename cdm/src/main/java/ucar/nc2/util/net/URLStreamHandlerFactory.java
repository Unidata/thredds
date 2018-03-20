/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import java.net.*;

/**
 * how do we know if URLStreamHandlerFactory has already been set?
 */

public class URLStreamHandlerFactory implements java.net.URLStreamHandlerFactory {

  static public org.slf4j.Logger log = ucar.httpservices.HTTPSession.log;

  //////////////////////////////////////////////////////////////////////////
  static private java.util.HashMap hash = new java.util.HashMap();
  static private boolean installed = false;

  static public void install() {
    try {
      if (!installed) {
        java.net.URL.setURLStreamHandlerFactory( new URLStreamHandlerFactory() );
        installed = true;
      }
    } catch (Error e) {
      log.error("Error installing URLStreamHandlerFactory "+e.getMessage());
      // Log.errorG("Error installing URLStreamHandlerFactory "+e.getMessage());
    }
  }

  static public void register(String protocol, URLStreamHandler sh) {
    hash.put( protocol.toLowerCase(), sh);
  }

  static public URL makeURL( String urlString) throws MalformedURLException {
    return installed ? new URL( urlString) : makeURL( null, urlString);
  }

  static public URL makeURL( URL parent, String urlString) throws MalformedURLException {
    if (installed)
      return new URL( parent, urlString);

    // install failed, use alternate form of URL constructor
    try {
      URI uri = new URI( urlString);
      URLStreamHandler h = (URLStreamHandler) hash.get( uri.getScheme().toLowerCase());;
      return new URL( parent, urlString, h);
    } catch (URISyntaxException e) {
      throw new MalformedURLException(e.getMessage());
    }

    // return new URL( url.getScheme(), url.getHost(), url.getPort(), url.getFile(), h);
  }

  public URLStreamHandler createURLStreamHandler(String protocol) {
    return (URLStreamHandler) hash.get( protocol.toLowerCase());
  }

}

/*
  // load protocol for ADDE URLs
  // See java.net.URL for explanation of URL handling
  static
  {
    try
    {
      String handlers = System.getProperty("java.protocol.handler.pkgs");
      String newProperty = null;
      if (handlers == null)
        newProperty = "edu.wisc.ssec.mcidas";
      else if (handlers.indexOf("edu.wisc.ssec.mcidas") < 0)
        newProperty = "edu.wisc.ssec.mcidas | " + handlers;
      if (newProperty != null)  // was set above
        System.setProperty("java.protocol.handler.pkgs", newProperty);
    }
    catch (Exception e)
    {
      LogStream.out.println(
        "Unable to set System Property: java.protocol.handler.pkgs");
    }
  }
  */
