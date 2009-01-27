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

package ucar.nc2.util.net;

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
        java.net.URL.setURLStreamHandlerFactory( new URLStreamHandlerFactory() );
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