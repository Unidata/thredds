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
