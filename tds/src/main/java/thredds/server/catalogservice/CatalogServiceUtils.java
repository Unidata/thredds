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
package thredds.server.catalogservice;

import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvDatasetImpl;
import thredds.servlet.UsageLog;
import thredds.server.config.HtmlConfig;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceUtils
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( CatalogServiceUtils.class );

  private CatalogServiceUtils() {}

  public static BindingResult bindAndValidateRemoteCatalogRequest( HttpServletRequest request )
  {
    // Bind and validate the request to a RemoteCatalogRequest.
    RemoteCatalogRequest rcr = new RemoteCatalogRequest();
    RemoteCatalogRequestDataBinder db = new RemoteCatalogRequestDataBinder( rcr, "request" );
    db.setAllowedFields( new String[]{"catalogUri", "command", "dataset", "verbose", "htmlView"} );
    db.bind( request );
    
    BindingResult bindingResult = db.getBindingResult();
    ValidationUtils.invokeValidator( new RemoteCatalogRequestValidator(), bindingResult.getTarget(), bindingResult );

    return bindingResult;
  }

  public static BindingResult bindAndValidateLocalCatalogRequest( HttpServletRequest request,
                                                                  boolean htmlView )
  {
    // Bind and validate the request to a LocalCatalogRequest.
    LocalCatalogRequest rcr = new LocalCatalogRequest();
    LocalCatalogRequestDataBinder db = new LocalCatalogRequestDataBinder( rcr, "request" );
    db.setAllowedFields( new String[]{"path", "command", "dataset"} );
    db.bind( request );

    BindingResult bindingResult = db.getBindingResult();
    LocalCatalogRequestValidator validator = new LocalCatalogRequestValidator();
    validator.setHtmlView( htmlView );
    ValidationUtils.invokeValidator( validator, bindingResult.getTarget(), bindingResult );

    return bindingResult;
  }

  public static ModelAndView constructModelForCatalogView( InvCatalog cat, HtmlConfig htmlConfig )
  {
    // Hand to catalog view.
    String catName = cat.getName();
    String catUri = cat.getUriString();
    if ( catName == null )
    {
      List childrenDs = cat.getDatasets();
      if ( childrenDs.size() == 1 )
      {
        InvDatasetImpl onlyChild = (InvDatasetImpl) childrenDs.get( 0 );
        catName = onlyChild.getName();
      }
      else
        catName = "";
    }

    Map<String, Object> model = new HashMap<String, Object>();
    model.put( "catalog", cat );
    model.put( "catalogName", HtmlUtils.htmlEscape( catName ) );
    model.put( "catalogUri", HtmlUtils.htmlEscape( catUri ) );

    htmlConfig.addHtmlConfigInfoToModel( model );

    return new ModelAndView( "thredds/server/catalog/catalog", model );
  }

  public static ModelAndView constructValidationMessageModelAndView( URI uri,
                                                                     String validationMessage,
                                                                     HtmlConfig htmlConfig )
  {
    Map<String, Object> model = new HashMap<String, Object>();
    model.put( "catalogUrl", uri );
    model.put( "message", validationMessage );

    htmlConfig.addHtmlConfigInfoToModel( model );

    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
    return new ModelAndView( "/thredds/server/catalogservice/validationMessage", model );
  }

  public static ModelAndView constructValidationErrorModelAndView( URI uri,
                                                                   String validationMessage,
                                                                   HtmlConfig htmlConfig )
  {
    Map<String, Object> model = new HashMap<String, Object>();
    model.put( "catalogUrl", uri );
    model.put( "message", validationMessage );

    htmlConfig.addHtmlConfigInfoToModel( model );

    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
    return new ModelAndView( "/thredds/server/catalogservice/validationError", model );
  }
}
