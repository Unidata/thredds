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
/**
 * User: rkambic
 * Date: Oct 13, 2010
 * Time: 11:19:08 AM
 */

package thredds.server.radarServer;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetScan;
import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class CatalogRadarServerController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private HtmlWriter htmlWriter;

  private boolean htmlView;

  /**
    * The view to forward to in case an dataset needs to be created.
  */
  private static final String CREATE_VIEW = "forward:createdataset.htm";

  /**
  * The model key used to retrieve the message from the model.
  */
  private static final String MODEL_KEY = "message";

  /**
  * The unique key for retrieving the text associated with this message.
  */
  private static final String MSG_CODE = "message.create.dataset";


  public  CatalogRadarServerController() {}
  

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void setHtmlWriter( HtmlWriter htmlWriter ) {
    this.htmlWriter = htmlWriter;
  }

  public boolean isHtmlView()
  {
    return htmlView;
  }

  public void setHtmlView( boolean htmlView )
  {
    this.htmlView = htmlView;
  }

  @Override
  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    try
    {
      // Gather diagnostics for logging request.
      log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );
      // setup
      String pathInfo = request.getPathInfo();
      if (pathInfo == null) pathInfo = "";

      // default is the complete radarCollection catalog
      InvCatalogImpl catalog = DatasetRepository.cat;
      // level2 and level3 catalog/dataset
      if (pathInfo.contains("level2") || pathInfo.contains("level3") ) {
        catalog = level2level3catalog(catalog, pathInfo );
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));
      }
      if ( catalog == null  ) {
         ModelAndView mav = new ModelAndView(CREATE_VIEW);
         mav.addObject(MODEL_KEY, MSG_CODE);
         return mav;
      } else {
        if ( this.htmlView )
        {
          int i = HtmlWriter.getInstance().writeCatalog(request, response, catalog, true);
          log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );
          return null;
        }
        else
         return new ModelAndView( "threddsInvCatXmlView", "catalog", catalog );
      }

    }
    catch ( RadarServerException e )
    {
      throw e; // pass it onto Spring exceptionResolver
    }
    catch ( Throwable e )
    {
      log.error( "handleRequestInternal(): Problem handling request.", e );
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      if ( ! response.isCommitted() ) response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
      return null;
    }
  }
  private InvCatalogImpl level2level3catalog(InvCatalogImpl cat, String pathInfo )
            throws RadarServerException, IOException {
    InvCatalogImpl tCat = null;
    try {
      StringBuilder aDs = new StringBuilder( pathInfo.substring(1) );
      int i = aDs.indexOf( "/catalog");
      if ( i > 0)
          aDs.delete( i, aDs.length());
      i = aDs.indexOf( "/dataset");
      if ( i > 0)
          aDs.delete( i, aDs.length());

      ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(false);
      factory.writeXML(cat, os, true);
      tCat = factory.readXML(
          new ByteArrayInputStream(os.toByteArray()), DatasetRepository.catURI);

      Iterator parents = tCat.getDatasets().iterator();
      while (parents.hasNext()) {
        ArrayList<InvDatasetImpl> delete = new ArrayList<InvDatasetImpl>();
        InvDatasetImpl top = (InvDatasetImpl) parents.next();
        Iterator tDatasets = top.getDatasets().iterator();
        while (tDatasets.hasNext()) {
          InvDatasetImpl ds = (InvDatasetImpl) tDatasets.next();
          if (ds instanceof InvDatasetScan) {
            InvDatasetScan ids = (InvDatasetScan) ds;
            if (ids.getPath() == null)
              continue;
            if (ids.getPath().contains(aDs)) {
              ids.setXlinkHref(ids.getPath() + "/dataset.xml");
            } else {
              delete.add(ds);
            }
          }
        }
        // remove datasets
        for (InvDatasetImpl idi : delete) {
          top.removeDataset(idi);
        }
      }
    } catch (Throwable e) {
      throw new RadarServerException( "CatalogRadarServerController.level2level3catalog" );  
    }
    return tCat;
  }
}
