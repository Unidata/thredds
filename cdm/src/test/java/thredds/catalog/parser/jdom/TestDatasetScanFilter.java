package thredds.catalog.parser.jdom;

import junit.framework.*;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.XMLEntityResolver;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.List;
import java.util.Iterator;

/**
 * _more_
 *
 * @author edavis
 * @since Apr 23, 2007 2:15:04 PM
 */
public class TestDatasetScanFilter extends TestCase
{
  public TestDatasetScanFilter( String name )
  {
    super( name );
  }

  /**
   * Test ...
   */
  public void testFilterReadWriteMatch()
  {
    StringBuffer inFilterAsString = new StringBuffer()
            .append( "<filter xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\">\n" )
            .append( "  <include wildcard=\"*.nc\" atomic=\"true\" collection=\"false\" />\n" )
            .append( "  <exclude wildcard=\"*.nc1\" atomic=\"true\" collection=\"false\" />\n" )
            .append( "  <include regExp=\"data\" atomic=\"false\" collection=\"true\" />\n" )
            .append( "  <exclude regExp=\"CVS\" atomic=\"false\" collection=\"true\" />\n" )
            .append( "</filter>" );

    XMLEntityResolver resolver = new XMLEntityResolver( false);
    SAXBuilder builder = resolver.getSAXBuilder();

    Document inDoc;
    try
    {
      inDoc = builder.build( new StringReader( inFilterAsString.toString()));
    }
    catch ( IOException e )
    {
      fail( "I/O error reading XML document: " + e.getMessage());
      return;
    }
    catch ( JDOMException e )
    {
      fail( "Problem parsing XML document: " + e.getMessage());
      return;
    }
    Element inFilterElem = inDoc.getRootElement();

    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false);
    InvCatalogFactory10 factory = (InvCatalogFactory10) fac.getCatalogConverter( thredds.catalog.XMLEntityResolver.CATALOG_NAMESPACE_10 );
    CrawlableDatasetFilter inFilter = factory.readDatasetScanFilter( inFilterElem);
    Element outFilterElem = factory.writeDatasetScanFilter( inFilter );

    // Write the resulting element to string.
    ByteArrayOutputStream inBaos = new ByteArrayOutputStream();
    ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
    XMLOutputter fmt = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    try
    {
      fmt.output( inFilterElem, inBaos );
      fmt.output( outFilterElem, outBaos );
    }
    catch ( IOException e )
    {
      fail( "Failed to write JDOM Elements to byte array: " + e.getMessage());
    }
    String inFilterResultString = inBaos.toString();
    String outFilterResultString = outBaos.toString();

    // Test whether the resulting string is same as test element.
    assertTrue( "Read then write results not as expected.\nInput:\n" + inFilterResultString + "\nOutput:\n" + outFilterResultString,
                inFilterResultString.equals( outFilterResultString));
  }
}
