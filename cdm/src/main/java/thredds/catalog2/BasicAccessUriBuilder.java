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
package thredds.catalog2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicAccessUriBuilder implements AccessUriBuilder
{
  private Logger log = LoggerFactory.getLogger( getClass());

  public URI buildAccessUri( Access access, URI docBaseUri )
  {
    if ( access == null )
      throw new IllegalArgumentException( "Access must not be null.");

    // Determine the base URI of the service.
    URI baseServiceUri = access.getService().getBaseUri();
    if ( ! baseServiceUri.isAbsolute())
    {
      if ( docBaseUri == null )
        throw new IllegalStateException( "Document base URI must not be null if service base URI is not absolute.");
      else
        baseServiceUri = docBaseUri.resolve( baseServiceUri );
    }

    // Build access URI using string concatenation of
    //    service.base + access.urlPath + service.suffix
    // [From "Constructing URLs" section of InvCatalog spec:
    //    http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/v1.0.2/InvCatalogSpec.html#constructingURLs ]
    StringBuilder sb = new StringBuilder( baseServiceUri.toString());
    sb.append( access.getUrlPath() );
    String suffix = access.getService().getSuffix();
    if ( suffix != null && (! suffix.equals( "" )))
      sb.append( suffix );

    try
    {
      return new URI( sb.toString());
    }
    catch ( URISyntaxException e )
    {
      log.error( "buildAccessUri(): URI syntax exception [" + sb.toString() + "].", e );
      return null;
    }
  }
}
