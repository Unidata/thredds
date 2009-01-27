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
// $Id: LatestModel.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server;

import thredds.dqc.server.latest.LatestDqcHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

public class LatestModel extends DqcHandler
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( LatestModel.class);

  protected LatestDqcHandler proxy;

  /** Default constructor. */
  public LatestModel()
  {
    log.info( "LatestModel(): Constructing LatestDqcHandler proxy." );
    proxy = new LatestDqcHandler();
  }

  // Implement DqcHandler.handleRequest().
  public void handleRequest(HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException
  {
    log.info( "handleRequest(): Calling LatestDqcHandler proxy.");
    proxy.handleRequest( req, res );
  }

  // Implement DqcHandler.initWithHandlerConfigDoc().
  public void initWithHandlerConfigDoc( URL configDocURL)
    throws IOException
  {
    log.info( "initWithHandlerConfigDoc(): Initializing LatestDqcHandler proxy." );
    proxy.initWithHandlerConfigDoc( configDocURL );
    proxy.setHandlerInfo( this.getHandlerInfo() );
  }

}
/*
 * $Log: LatestModel.java,v $
 * Revision 1.15  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.14  2005/10/03 22:35:41  edavis
 * Minor fixes for LatestDqcHandler.
 *
 * Revision 1.13  2005/09/30 21:51:38  edavis
 * Improve "Latest" DqcHandler so it can deal with new IDD naming conventions:
 * new configuration file format; add LatestDqcHandler which handles new and old
 * config file formats; use LatestDqcHandler as a proxy for LatestModel.
 *
 * Revision 1.12  2005/08/23 23:00:51  edavis
 * Allow override of default output catalog version "1.0.1" to "1.0". This allows existing
 * IDV (which reads catalog version as float) to read InvCatalog 1.0.1 catalogs.
 *
 * Revision 1.11  2005/07/13 22:48:07  edavis
 * Improve server logging, includes adding a final log message
 * containing the response time for each request.
 *
 * Revision 1.10  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.9  2004/08/24 23:48:44  edavis
 * Bug fixes dealing with 0.6 vs 1.0 catalog creation.
 *
 * Revision 1.8  2004/08/23 16:45:20  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.7  2004/03/05 06:32:45  edavis
 * Update the DqcHandler interface.
 *
 * Revision 1.6  2004/01/15 19:45:31  edavis
 * Changes made while adding support for the JPL QuikSCAT
 * DODS File Server catalog.
 *
 * Revision 1.5  2003/12/24 06:29:10  edavis
 * Update from get/setHandlerConfig() to initWithHandlerConfigDoc() using
 * InputStream instead of File to get config document.
 *
 * Revision 1.4  2003/12/11 01:33:25  edavis
 * Minor changes and added logging.
 *
 * Revision 1.3  2003/05/28 22:18:23  edavis
 * Added run date/time to returned dataset name.
 *
 * Revision 1.2  2003/05/06 22:18:33  edavis
 * Add lots of error messages and reorganize some code.
 *
 * Revision 1.1  2003/04/28 17:57:45  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 *
 */