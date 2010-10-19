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

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import thredds.catalog.util.DeepCopyUtils;
import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.UsageLog;
import thredds.server.config.TdsContext;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDataset;
import thredds.util.RequestForwardUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handle all requests for local catalog access and services. Supported
 * services are subsetting of catalogs and HTML views of catalogs (full
 * or subset) and datasets.
 *
 * <p>Can also handle non-catalog requests for XML andHTML files in publicDoc FileSource.
 *
 * <p>Currently, handles the following TDS requests:
 * <ul>
 *   <li>Mapping="/catalog/*" -- e.g., ServletPath="/catalog" and PathInfo="/some/path/*.html"</li>
 *   <li>Mapping="*.xml"      -- ServletPath="/some/path/*.xml" and PathInfo=null</li>
 *   <li>Mapping="*.html"     -- ServletPath="/some/path/*.html" and PathInfo=null</li>
 * </ul>
 * <p>Note: When "/catalog/*" request paths end in "/", a suffix is appended
 * to the path. The suffix default value is "catalog.html" but can be set in
 * {@link LocalCatalogRequestDataBinder}.
 *
 * <p> Uses the following information from an HTTP request:
 * <ul>
 *   <li> The "local catalog path" identifies the local catalog to use. Its
 *     value is either getPathInfo() or getServletPath() depending on how the
 *     requests are mapped to the servlet, see
 *    {@link thredds.util.TdsPathUtils#extractPath(javax.servlet.http.HttpServletRequest)
 *     TdsPathUtils.extractPath()}.</li>
 *   <li>The "local catalog path" extension (which must be either ".xml" or
 *     ".html") determines whether the catalog is displayed with an XML or
 *     an HTML view. [Note: the ".html" ending is replaced with ".xml" when
 *     the path is used to locate the local catalog.]</li>
 *   <li>The "command" parameter must either be empty or have a value of
 *     "SHOW" or "SUBSET", see {@link Command}; </li>
 *   <li>The parameter "dataset" identifies a dataset contained by the
 *     local catalog. </li>
 * </ul>
 *
 * <p>Constraints on the above information:
 * <ul>
 *   <li>The "local catalog path" may end in either ".xml" or ".html" and
 *     must reference an existing local catalog. A ".html" ending is replaced
 *     with ".xml" when the path is used to locate the local catalog. </li>
 *   <li>The "dataset" parameter must either be empty or contain the value
 *     of a dataset ID contained in the catalog.</li>
 *   <li>If the "command" parameter is empty, it will default to "SHOW" if
 *     the "dataset" parameter is empty, otherwise it will default to "SUBSET".</li>
 * </ul>
 *
 * <p>The above information is contained in a {@link LocalCatalogRequest}
 * command object, default values are set during binding by
 * {@link LocalCatalogRequestDataBinder}), and constraints are enforced by
 * {@link LocalCatalogRequestValidator}.
 *
 * @author edavis
 * @since 4.0
 * @see thredds.util.TdsPathUtils#extractPath(javax.servlet.http.HttpServletRequest)
 * @see Command
 * @see LocalCatalogRequest
 * @see LocalCatalogRequestDataBinder
 * @see LocalCatalogRequestValidator
 */
public class LocalCatalogServiceController extends AbstractController
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private HtmlWriter htmlWriter;

  private boolean catalogSupportOnly;
  private boolean htmlView;

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void setHtmlWriter( HtmlWriter htmlWriter ) {
    this.htmlWriter = htmlWriter;
  }

  public boolean isCatalogSupportOnly()
  {
    return catalogSupportOnly;
  }

  public void setCatalogSupportOnly( boolean catalogSupportOnly )
  {
    this.catalogSupportOnly = catalogSupportOnly;
  }

  public boolean isHtmlView()
  {
    return htmlView;
  }

  public void setHtmlView( boolean htmlView )
  {
    this.htmlView = htmlView;
  }

  protected ModelAndView handleRequestInternal( HttpServletRequest request,
                                                HttpServletResponse response )
          throws Exception
  {
    try
    {
      // Gather diagnostics for logging request.
      log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( request ) );

      // Bind HTTP request to a LocalCatalogRequest.
      BindingResult bindingResult = CatalogServiceUtils.bindAndValidateLocalCatalogRequest( request, this.htmlView );

      // If any binding or validation errors, return BAD_REQUEST.
      if ( bindingResult.hasErrors() )
      {
        StringBuilder msg = new StringBuilder( "Bad request" );
        List<ObjectError> oeList = bindingResult.getAllErrors();
        for ( ObjectError e : oeList )
          msg.append( ": " ).append( e.getDefaultMessage() != null ? e.getDefaultMessage() : e.toString() );
        log.info( "handleRequestInternal(): " + msg );
        log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, msg.length() ) );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
        return null;
      }

      // Retrieve the resulting LocalCatalogRequest.
      LocalCatalogRequest catalogServiceRequest = (LocalCatalogRequest) bindingResult.getTarget();

      // Determine path and catalogPath
      String path = catalogServiceRequest.getPath();
      String catalogPath = this.htmlView ? path.replaceAll( ".html$", ".xml" ) : path;

      // Check for matching catalog.
      DataRootHandler drh = DataRootHandler.getInstance();

      InvCatalog catalog = null;
      String baseUriString = request.getRequestURL().toString();
      try
      {
        catalog = drh.getCatalog( catalogPath, new URI( baseUriString ) );
      }
      catch ( URISyntaxException e )
      {
        String msg = "Bad URI syntax [" + baseUriString + "]: " + e.getMessage();
        log.error( "handleRequestInternal(): " + msg );
        log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, msg.length() ) );
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
        return null;
      }

      // If no catalog found, handle as a publicDoc request.
      if ( catalog == null )
        return handlePublicDocumentRequest( request, response, path );

      // Otherwise, handle catalog as indicated by "command".
      if ( catalogServiceRequest.getCommand().equals( Command.SHOW))
      {
        if ( this.htmlView )
        {
          int i = this.htmlWriter.writeCatalog( request, response, (InvCatalogImpl) catalog, true );
          log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );
          return null;
          // return constructModelForCatalogView( catalog, this.htmlView );
        }
        else
          return new ModelAndView( "threddsInvCatXmlView", "catalog", catalog );
      }
      else if ( catalogServiceRequest.getCommand().equals( Command.SUBSET ))
      {
        String datasetId = catalogServiceRequest.getDataset();
        InvDataset dataset = catalog.findDatasetByID( datasetId );
        if ( dataset == null )
        {
          String msg = "Did not find dataset [" + datasetId + "] in catalog [" + baseUriString + "].";
          //log.info( "handleRequestInternal(): " + msg );
          log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, msg.length() ) );
          response.sendError( HttpServletResponse.SC_NOT_FOUND, msg.toString() );
          return null;
        }

        if ( this.htmlView)
        {
          int i = this.htmlWriter.showDataset( baseUriString, (InvDatasetImpl) dataset, request, response, true );
          log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, i ) );
          return null;
        }
        else
        {
          InvCatalog subsetCat = DeepCopyUtils.subsetCatalogOnDataset( catalog, dataset );
          return new ModelAndView( "threddsInvCatXmlView", "catalog", subsetCat );
        }
      }
      else
      {
        String msg = "Unsupported request command [" + catalogServiceRequest.getCommand() + "].";
        log.error( "handleRequestInternal(): " + msg + " -- NOTE: Should have been caught on input validation." );
        log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, msg.length()));
        response.sendError( HttpServletResponse.SC_BAD_REQUEST, msg.toString() );
        return null;
      }
    }
    catch ( IOException e )
    {
      log.error( "handleRequestInternal(): Trouble writing to response.", e);
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      return null;
    }
    catch ( Throwable e )
    {
      log.error( "handleRequestInternal(): Problem handling request.", e );
      log.info( "handleRequestInternal(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, -1 ) );
      if ( ! response.isCommitted() ) response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
      return null;
    }
  }

  private ModelAndView handlePublicDocumentRequest( HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    String path )
          throws IOException, ServletException
  {
    // If not supporting access to public document files, send not found response.
    if ( this.catalogSupportOnly )
    {
      log.info( "handlePublicDocumentRequest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0 ) );
      response.sendError( HttpServletResponse.SC_NOT_FOUND );
      return null;
    }

    // If request doesn't match a known catalog, look for a public document.
    File publicFile = tdsContext.getPublicDocFileSource().getFile( path );
    if ( publicFile != null )
    {
      log.info( "handlePublicDocumentRequest(): " + UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, -1 ) );
      return new ModelAndView( "threddsFileView", "file", publicFile );
    }

    // If request doesn't match a public document, forward to default dispatcher.
    RequestForwardUtils.forwardRequest( path, tdsContext.getDefaultRequestDispatcher(),
                                        request, response );
    return null;
  }
}