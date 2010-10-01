package thredds.servlet;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog2.xml.parser.CatalogXmlUtils;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class StaticViewerTest
{
  @Test
  public void checkViewerPropertyWithUrlReplacement() throws URISyntaxException
  {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithUrlReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI( docBaseUriString );

    String catalogAsString = setupCatDsWithViewerProperty( "viewer1", "{url}.info,DS info" );

    InvDataset ds1 = constructCatalogAndAssertAsExpected( docBaseUri, catalogAsString );

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod( "GET" );
    request.setContextPath( contextPath );
    request.setServletPath( servletPath );
    request.setPathInfo( catPathNoExtension + ".html" );
    request.setParameter( "dataset", "ds1" );

    ViewerLinkProvider sv = new ViewServlet.StaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( (InvDatasetImpl) ds1, request );
    assertNotNull( viewerLinks);
    assertEquals( 1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get( 0 );
    assertNotNull( vl);
    assertEquals( "DS info", vl.getTitle());
    assertEquals( host + contextPath + "/dodsC" + "/test/ds1.nc.info", vl.getUrl());
  }

  @Test
  public void checkViewerPropertyWithOpendapReplacement() throws URISyntaxException
  {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithOpendapReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI( docBaseUriString );

    String catalogAsString = setupCatDsWithViewerProperty( "viewer1", "{OPENDAP}.info,ODAP DS info" );

    InvDataset ds1 = constructCatalogAndAssertAsExpected( docBaseUri, catalogAsString );

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod( "GET" );
    request.setContextPath( contextPath );
    request.setServletPath( servletPath );
    request.setPathInfo( catPathNoExtension + ".html" );
    request.setParameter( "dataset", "ds1" );

    ViewerLinkProvider sv = new ViewServlet.StaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( (InvDatasetImpl) ds1, request );
    assertNotNull( viewerLinks);
    assertEquals( 1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get( 0 );
    assertNotNull( vl);
    assertEquals( "ODAP DS info", vl.getTitle());
    assertEquals( host + contextPath + "/dodsC" + "/test/ds1.nc.info", vl.getUrl());
  }

  @Test
  public void checkViewerPropertyWithRemoteWmsReplacement() throws URISyntaxException
  {
    String host = "http://test.thredds.servlet.StaticViewerTest";
    String contextPath = "/thredds";
    String servletPath = "/catalog";
    String catPathNoExtension = "/checkViewerPropertyWithOpendapReplacement";
    String docBaseUriString = host + contextPath + servletPath + catPathNoExtension + ".xml";
    URI docBaseUri = new URI( docBaseUriString );

    String catalogAsString = setupCatDsWithViewerProperty( "viewer1", "{REMOTEWMS}.info,RemoteWms DS info" );

    InvDataset ds1 = constructCatalogAndAssertAsExpected( docBaseUri, catalogAsString );

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod( "GET" );
    request.setContextPath( contextPath );
    request.setServletPath( servletPath );
    request.setPathInfo( catPathNoExtension + ".html" );
    request.setParameter( "dataset", "ds1" );

    ViewerLinkProvider sv = new ViewServlet.StaticView();
    List<ViewerLinkProvider.ViewerLink> viewerLinks = sv.getViewerLinks( (InvDatasetImpl) ds1, request );
    assertNotNull( viewerLinks);
    assertEquals( 1, viewerLinks.size());

    ViewerLinkProvider.ViewerLink vl = viewerLinks.get( 0 );
    assertNotNull( vl);
    assertEquals( "RemoteWms DS info", vl.getTitle());
    assertEquals( "http://server/thredds/wms/test/ds1.nc.info", vl.getUrl());
  }

  private InvDataset constructCatalogAndAssertAsExpected( URI docBaseUri, String catalogAsString )
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true );
    InvCatalogImpl catalog = fac.readXML( catalogAsString, docBaseUri );

    StringBuilder log = new StringBuilder();
    boolean isValid = catalog.check( log );
    assertTrue( "Invalid catalog:\n" + log.toString(), isValid );

    List<InvDataset> datasets = catalog.getDatasets();
    assertNotNull( datasets);
    assertEquals( 1, datasets.size());
    InvDataset ds1 = datasets.get( 0 );
    assertNotNull( ds1);
    assertEquals( "ds 1", ds1.getName());
    return ds1;
  }

  private static String setupCatDsWithViewerProperty( String viewerName, String viewerValue)
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" )
            .append( "         xmlns:xlink='http://www.w3.org/1999/xlink'\n" )
            .append( "         name='Catalog 1'\n" )
            .append( "         version='1.0.3'>\n" )
            .append( "  <service name='all' serviceType='Compound' base=''>\n" )
            .append( "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/' />\n" )
            .append( "    <service name='wms' serviceType='WMS' base='/thredds/wms/' />\n" )
            .append( "    <service name='remoteWMS' serviceType='REMOTEWMS' base='http://server/thredds/wms/' />\n" )
            .append( "  </service>\n" )

            .append( "  <dataset name='ds 1' ID='ds1' urlPath='test/ds1.nc'>\n" )
            .append( "    <metadata inherited='true'>\n" )
            .append( "      <serviceName>all</serviceName>\n" )
            .append( "    </metadata>\n" )
            .append( "    <property name='" ).append( viewerName )
            .append(             "' value='" ).append( viewerValue ).append( "' />\n" )
            .append( "  </dataset>\n" )

    .append( "</catalog>" );

    //return CatalogXmlUtils.wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), "Catalog 1", "1.0.3", null );
    return sb.toString();
  }

}
