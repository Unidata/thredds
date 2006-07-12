// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.util;

import java.net.*;

/**
 * how do we know if URLStreamHandlerFactory has already been set?
 */

public class URLStreamHandlerFactory implements java.net.URLStreamHandlerFactory {
  static private java.util.HashMap hash = new java.util.HashMap();
  static private boolean installed = false;

  static public void install() {
    try {
      if (!installed) {
        java.net.URL.setURLStreamHandlerFactory( new thredds.util.URLStreamHandlerFactory() );
        installed = true;
      }
    } catch (Error e) {
      System.out.println("Error installing URLStreamHandlerFactory "+e.getMessage());
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

    // return new URL( uri.getScheme(), uri.getHost(), uri.getPort(), uri.getFile(), h);
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
      System.out.println(
        "Unable to set System Property: java.protocol.handler.pkgs");
    }
  }
  */