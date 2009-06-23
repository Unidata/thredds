/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog;

import thredds.catalog2.Catalog;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TimeLargeCatalogRead
{
  static final String catURL = "http://motherlode.ucar.edu:9080/thredds/radarServer/nexrad/level2/IDD?stn=KARX&time_start=2009-04-07T:00:00:00Z&time_end=2009-05-22T16:44:39Z";
  //static final String catURL = "http://motherlode.ucar.edu:9080/thredds/radarServer/nexrad/level2/IDD?stn=KARX&time_start=2009-04-07T:00:00:00Z&time_end=2009-05-22T16:44:39Z";

  public static void main( String[] args )
          throws URISyntaxException, ThreddsXmlParserException
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    String catAsString = gatCat();
    URI uri = new URI( catURL);
    InvCatalogImpl cat = null;
    long cum = 0;
    long cum2 = 0;
    int numAttempts = 100;
    for ( int i = 0; i < numAttempts; i++ )
    {
      long start = System.currentTimeMillis();
      cat = fac.readXML( catAsString, uri );
      // cat = fac.readXML( catURL );
      long done = System.currentTimeMillis();
      long elapsed = done - start;
      start = System.currentTimeMillis();
      Catalog cat2 = parseCatalog( catAsString, catURL );

      done = System.currentTimeMillis();
      long elapsed2 = done - start;
      cum+=elapsed;
      cum2+=elapsed2;
      System.out.println( "Read catalog ["+i+"]: InvCat=" + elapsed + " stax=" + elapsed2 + "\n" );
    }
    System.out.println( "Cum=" + cum );
    System.out.println( "Avg=" + cum/ numAttempts );
    System.out.println( "CumStax=" + cum2 );
    System.out.println( "AvgStax=" + cum2/ numAttempts );

    StringBuilder sb = new StringBuilder();
    if ( cat.check( sb ) )
      System.out.println( "Failed check:\n" + sb );
    else
      System.out.println( "OK check:\n" + sb );

    System.out.println( "Done" );

  }

  private static Catalog parseCatalog( String docAsString, String docBaseUriString )
          throws URISyntaxException, ThreddsXmlParserException
  {
    URI docBaseUri;
//    try
//    {
      docBaseUri = new URI( docBaseUriString );
//    }
//    catch ( URISyntaxException e )
//    {
//      fail( "Syntax problem with URI [" + docBaseUriString + "]." );
//      return null;
//    }

    Catalog cat;
    ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
//    try
//    {
      cat = cp.parse( new StringReader( docAsString ), docBaseUri );
//    }
//    catch ( ThreddsXmlParserException e )
//    {
//      fail( "Failed to parse catalog: " + e.getMessage() );
//      return null;
//    }

//    assertNotNull( "Result of parse was null catalog [" + docBaseUriString + "].",
//                   cat );
    return cat;
  }

  private static String gatCat()
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<catalog xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" name=\"Radar Level2 datasets in near real time\" version=\"1.0.1\">\n" +
               "\n" +
               "  <service name=\"OPENDAP\" serviceType=\"OPENDAP\" base=\"/thredds/dodsC/nexrad/level2/IDD/\"/>\n" +
               "    <dataset name=\"RadarLevel2 datasets for available stations and times\" collectionType=\"TimeSeries\" ID=\"accept=xml&amp;stn=KARX&amp;time_start=2009-04-07T00:00:00Z&amp;time_end=2009-05-22T16:44:39Z\">\n" +
               "    <metadata inherited=\"true\">\n" +
               "      <dataType>Radial</dataType>\n" +
               "      <dataFormat>NEXRAD2</dataFormat>\n" +
               "      <serviceName>OPENDAP</serviceName>\n" +
               "\n" +
               "    </metadata>\n" +
               "\n" +
               "      <dataset name=\"Level2_KARX_20090522_1518.ar2v\" ID=\"1209322007\"\n" +
               "        urlPath=\"KARX/20090522/Level2_KARX_20090522_1518.ar2v\">\n" +
               "        <date type=\"start of ob\">2009-05-22T15:18:00</date>\n" +
               "      </dataset>");

    for (int i=0; i<9111; i++)
    {
      sb.append( "      <dataset name=\"Level2_KARX_20090522_1518.ar2v\" ID=\"").append( 1209322007 + i).append("\"\n")
              .append(   "        urlPath=\"KARX/20090522/Level2_KARX_20090522_1518.ar2v\">\n")
              .append(   "        <date type=\"start of ob\">2009-05-22T15:18:00</date>\n")
              .append(   "      </dataset>");
    }

    sb.append( "      <dataset name=\"Level2_KARX_20090407_0007.ar2v\" ID=\"1616941921\"\n" +
               "        urlPath=\"KARX/20090407/Level2_KARX_20090407_0007.ar2v\">\n" +
               "        <date type=\"start of ob\">2009-04-07T00:07:00</date>\n" +
               "\n" +
               "      </dataset>\n" +
               "    </dataset>\n" +
               "</catalog>");

    return sb.toString();
  }
}
