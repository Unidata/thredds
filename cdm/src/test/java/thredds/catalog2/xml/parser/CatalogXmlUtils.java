package thredds.catalog2.xml.parser;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;
import thredds.catalog2.xml.writer.ThreddsXmlWriter;
import thredds.catalog2.xml.writer.ThreddsXmlWriterFactory;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.Catalog;
import thredds.catalog.ServiceType;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import ucar.nc2.units.DateType;

/**
 * Utility methods for generating catalog XML. 
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogXmlUtils
{
  private static final String catName = "thredds.catalog2.xml.parser.CatalogXmlUtils";
  private static final String catVersion = "1.0.2";

  private CatalogXmlUtils(){}

    public static CatalogBuilder parseCatalogIntoBuilder( URI docBaseUri, String catalogXml )
            throws ThreddsXmlParserException
    {
      ThreddsXmlParserFactory fac = ThreddsXmlParserFactory.newFactory();
      ThreddsXmlParser cp = fac.getCatalogParser();
      return cp.parseIntoBuilder( new StringReader( catalogXml ), docBaseUri );
    }

    public static void writeCatalogXml( Catalog cat )
    {
        ThreddsXmlWriter txw = ThreddsXmlWriterFactory.newInstance().createThreddsXmlWriter();
        try {
            txw.writeCatalog( cat, System.out );
        }
        catch ( ThreddsXmlWriterException e )
        {
            e.printStackTrace();
            fail( "Failed writing catalog to sout: " + e.getMessage() );
        }
    }

    public static String getCatalog( DateType expires ) {
        return wrapThreddsXmlInCatalog( "", expires );
    }

  public static String wrapThreddsXmlInCatalog( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" )
            .append( "         xmlns:xlink='http://www.w3.org/1999/xlink'\n" );
    if ( expires != null )
      sb.append( "         expires='" ).append( expires.toString() ).append( "'\n" );
    sb      .append( "         name='").append( catName).append( "'\n" )
            .append( "         version='").append( catVersion).append( "'>\n" )
            .append(    threddsXml )
            .append( "</catalog>" );

    return sb.toString();
  }

  public static void assertCatalogAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires )
  {
      assertNotNull( "DocBase URI is null.", docBaseUri) ;
      assertNotNull( "CatalogBuilder [" + docBaseUri + "] is null.", catBuilder) ;

    assertEquals( catBuilder.getDocBaseUri(), docBaseUri);
    assertEquals( catBuilder.getName(), catName);
    if ( expires != null )
      assertEquals( catBuilder.getExpires().toString(), expires.toString());
  }

  public static String getCatalogWithService( DateType expires ) {
    return wrapThreddsXmlInCatalogWithService( "", expires );
  }

  public static String wrapThreddsXmlInCatalogWithService( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append(    threddsXml );

    return wrapThreddsXmlInCatalog(  sb.toString(), expires );
  }

  public static void assertCatalogWithServiceAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires)
  {
    assertCatalogAsExpected( catBuilder, docBaseUri, expires );

    List<ServiceBuilder> serviceBldrs = catBuilder.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty() );
    assertTrue( serviceBldrs.size() == 1 );
    ServiceBuilder serviceBldr = serviceBldrs.get( 0 );
    assertEquals( serviceBldr.getName(), "odap" );
    assertEquals( serviceBldr.getType(), ServiceType.OPENDAP );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/dodsC/" );
  }

  public static String getCatalogWithCompoundService( DateType expires ) {
    return wrapThreddsXmlInCatalogWithCompoundService( "", expires );
  }

  public static String wrapThreddsXmlInCatalogWithCompoundService( String threddsXml, DateType expires )
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <service name='all' serviceType='Compound' base=''>\n" )
            .append( "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/' />\n" )
            .append( "    <service name='wms' serviceType='WMS' base='/thredds/wms/' />\n" )
            .append( "  </service>\n" )
            .append(    threddsXml );

    return wrapThreddsXmlInCatalog(  sb.toString(), expires );
  }

  public static void assertCatalogWithCompoundServiceAsExpected( CatalogBuilder catBuilder, URI docBaseUri, DateType expires )
  {
    assertCatalogAsExpected( catBuilder, docBaseUri, expires );

    List<ServiceBuilder> serviceBldrs = catBuilder.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty() );
    assertTrue( serviceBldrs.size() == 1 );
    ServiceBuilder serviceBldr = serviceBldrs.get( 0 );
    assertEquals( serviceBldr.getName(), "all" );
    assertEquals( serviceBldr.getType(), ServiceType.COMPOUND );
    assertEquals( serviceBldr.getBaseUri().toString(), "" );

    serviceBldrs = serviceBldr.getServiceBuilders();
    assertFalse( serviceBldrs.isEmpty());
    assertEquals( serviceBldrs.size(), 3 );

    serviceBldr = serviceBldrs.get( 0);
    assertEquals( serviceBldr.getName(), "odap" );
    assertEquals( serviceBldr.getType(), ServiceType.OPENDAP );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/dodsC/" );

    serviceBldr = serviceBldrs.get( 1);
    assertEquals( serviceBldr.getName(), "wcs" );
    assertEquals( serviceBldr.getType(), ServiceType.WCS );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/wcs/" );

    serviceBldr = serviceBldrs.get( 2);
    assertEquals( serviceBldr.getName(), "wms" );
    assertEquals( serviceBldr.getType(), ServiceType.WMS );
    assertEquals( serviceBldr.getBaseUri().toString(), "/thredds/wms/" );
  }

  public static String getCatalogWithSingleAccessDatasetWithRawServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <serviceName>odap</serviceName>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetWithMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <metadata>\n" )
            .append( "      <serviceName>odap</serviceName>\n" )
            .append( "    </metadata>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetWithInheritedMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='Test1' urlPath='test/test1.nc'>\n" )
            .append( "    <metadata inherited='true'>\n" )
            .append( "      <serviceName>odap</serviceName>\n" )
            .append( "    </metadata>\n" )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getCatalogWithSingleAccessDatasetOldStyle()
  {
    String sb = "<dataset name='Test1' urlPath='test/test1.nc' serviceName='odap' />\n";

    return wrapThreddsXmlInCatalogWithCompoundService( sb, null );
  }

  public static void assertCatalogHasSingleAccessDataset( CatalogBuilder catBuilder,
                                                                            URI docBaseUri )
  {
    assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );

    List<DatasetNodeBuilder> dsBuilders = catBuilder.getDatasetNodeBuilders();
    assertTrue( dsBuilders.size() == 1 );
    DatasetNodeBuilder dsnBuilder = dsBuilders.get( 0 );
    if ( !( dsnBuilder instanceof DatasetBuilder ) )
    {
      fail( "DatasetNode [" + dsnBuilder.getName() + "] not a Dataset." );
      return;
    }
    DatasetBuilder dsBldr = (DatasetBuilder) dsnBuilder;
    List<AccessBuilder> accesses = dsBldr.getAccessBuilders();
    assertFalse( "Dataset [" + dsBldr.getName() + "] not accessible.", accesses.isEmpty() );
    assertTrue( accesses.size() == 1 );
    AccessBuilder access = accesses.get( 0 );
    assertEquals( access.getUrlPath(), "test/test1.nc" );
    assertEquals( access.getServiceBuilder().getType(), ServiceType.OPENDAP );
    assertEquals( access.getServiceBuilder().getBaseUri().toString(), "/thredds/dodsC/" );
  }

  public static String wrapThreddsXmlInContainerDataset( String threddsXml )
  {

    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='container dataset' ID='containerDs'>\n" )
            .append(      threddsXml )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

    public static DatasetBuilder assertCatalogWithContainerDatasetAsExpected( CatalogBuilder catBuilder,
                                                                              URI docBaseUri)
    {
        assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );

        List<DatasetNodeBuilder> dsBuilders = catBuilder.getDatasetNodeBuilders();
        assertTrue( dsBuilders.size() == 1 );
        DatasetNodeBuilder dsnBuilder = dsBuilders.get( 0 );
        assertNotNull( dsnBuilder);
        if ( !( dsnBuilder instanceof DatasetBuilder ) )
        {
            fail( "DatasetNode [" + dsnBuilder.getName() + "] not a Dataset." );
            return null;
        }
        DatasetBuilder dsBldr = (DatasetBuilder) dsnBuilder;

        // Check that the container dataset isn't accessible, two methods:
        // 1)
        assertFalse( dsBldr.isAccessible() );
        // 2)
        List<AccessBuilder> accesses = dsBldr.getAccessBuilders();
        assertTrue( accesses.isEmpty());

        assertEquals( dsBldr.getName(), "container dataset" );
        assertEquals( dsBldr.getId(), "containerDs" );

        assertFalse( dsBldr.isCollection() );
        assertFalse( dsBldr.isBuilt() );

        return dsBldr;
    }

  public static String wrapThreddsXmlInCatalogDatasetMetadata( String threddsXml )
  {
    return wrapThreddsXmlInContainerDataset( "<metadata>" + threddsXml + "</metadata>\n" );
  }

  public static String wrapThreddsXmlInCatalogDatasetMetadataInherited( String threddsXml )
  {
    return wrapThreddsXmlInContainerDataset( "<metadata inherited='true'>" + threddsXml + "</metadata>\n" );
  }

  public static String getNestedDatasetWithRawServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds1'>\n")
            .append( "    <serviceName>odap</serviceName>\n")
            .append( "    <dataset name='ds2' urlPath='test/test1.nc' />\n")
            .append( "  </dataset>\n");

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getNestedDatasetWithMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds1'>\n")
            .append( "    <metadata><serviceName>odap</serviceName></metadata>\n")
            .append( "    <dataset name='ds2' urlPath='test/test1.nc' />\n")
            .append( "  </dataset>\n");

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getNestedDatasetWithUninheritedMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds1'>\n")
            .append( "    <metadata inherited='false'><serviceName>odap</serviceName></metadata>\n")
            .append( "    <dataset name='ds2' urlPath='test/test1.nc' />\n")
            .append( "  </dataset>\n");

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static String getNestedDatasetWithInheritedMetadataServiceName()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds1'>\n")
            .append( "    <metadata inherited='true'><serviceName>odap</serviceName></metadata>\n")
            .append( "    <dataset name='ds2' urlPath='test/test1.nc' />\n")
            .append( "  </dataset>\n");

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }

  public static void assertNestedDatasetIsAccessible( CatalogBuilder catBuilder,
                                                      URI docBaseUri )
  {
    DatasetBuilder dsBldr = getAndAssertNestedDataset( catBuilder, docBaseUri );

    List<AccessBuilder> accesses = dsBldr.getAccessBuilders();
    assertFalse( accesses.isEmpty() );
    assertTrue( accesses.size() == 1 );
    AccessBuilder access = accesses.get( 0 );
    assertEquals( access.getUrlPath(), "test/test1.nc" );
    assertEquals( access.getServiceBuilder().getType(), ServiceType.OPENDAP );
    assertEquals( access.getServiceBuilder().getBaseUri().toString(), "/thredds/dodsC/" );
  }

  public static void assertNestedDatasetIsNotAccessible( CatalogBuilder catBuilder,
                                                         URI docBaseUri )
  {
    DatasetBuilder dsBldr = getAndAssertNestedDataset( catBuilder, docBaseUri );

    List<AccessBuilder> accesses = dsBldr.getAccessBuilders();
    assertTrue( accesses.isEmpty() );
  }

  public static DatasetBuilder getAndAssertNestedDataset( CatalogBuilder catBuilder,
                                                          URI docBaseUri )
  {
    assertCatalogWithCompoundServiceAsExpected( catBuilder, docBaseUri, null );

    // Get first dataset.
    List<DatasetNodeBuilder> dsBuilders = catBuilder.getDatasetNodeBuilders();
    assertTrue( dsBuilders.size() == 1 );
    DatasetNodeBuilder dsnBuilder = dsBuilders.get( 0 );

    // Get nested dataset.
    dsBuilders = dsnBuilder.getDatasetNodeBuilders();
    assertTrue( dsBuilders.size() == 1 );
    dsnBuilder = dsBuilders.get( 0 );

    if ( !( dsnBuilder instanceof DatasetBuilder ) )
    {
      fail( "DatasetNode [" + dsnBuilder.getName() + "] not a Dataset." );
      return null;
    }

    return (DatasetBuilder) dsnBuilder;
  }

  public static String getCatalogWithNestedDatasetInheritedMetadata()
  {
    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='ds1'>\n")
            .append( "    <metadata inherited='true'><serviceName>odap</serviceName></metadata>\n")
            .append( "    <dataset name='ds2'>\n")
            .append( "      <serviceName>wcs</serviceName>\n")
            .append( "      <dataset name='Test1' urlPath='test/test1.nc' />\n" )
            .append( "      <dataset name='Test2' urlPath='test/test2.nc'>\n" )
            .append( "        <serviceName>wms</serviceName>\n" )
            .append( "      </dataset>\n" )
            .append( "    </dataset>")
            .append( "  </dataset>");

    return wrapThreddsXmlInCatalogWithCompoundService( sb.toString(), null );
  }


}
