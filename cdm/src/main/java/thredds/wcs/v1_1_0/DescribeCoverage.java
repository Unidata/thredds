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
package thredds.wcs.v1_1_0;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.net.URI;
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;

import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DescribeCoverage
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( DescribeCoverage.class );

  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1" );
  protected static final Namespace owcsNS = Namespace.getNamespace( "owcs", "http://www.opengis.net/wcs/1.1/ows" );
  protected static final Namespace owsNS = Namespace.getNamespace( "ows", "http://www.opengis.net/ows" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  private URI serverURI;
  private List<String> identifiers;

  private String version = "1.1.0";
  private GridDataset dataset;

  private Document describeCoverageDoc;

  public DescribeCoverage( URI serverURI, List<String> identifiers,
                           GridDataset dataset )
  {
    this.serverURI = serverURI;
    this.identifiers = identifiers;
    this.dataset = dataset;
    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Non-null server URI required.");
    if ( this.identifiers == null )
      throw new IllegalArgumentException( "Non-null identifier list required." );
    if ( this.identifiers.size() < 1 )
      throw new IllegalArgumentException( "Identifier list must contain at least one ID <" + this.identifiers.size() + ">." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required.");
  }

  public Document getDescribeCoverageDoc()
  {
    if ( this.describeCoverageDoc == null )
      describeCoverageDoc = generateDescribeCoverageDoc();
    return describeCoverageDoc;
  }

  public void writeDescribeCoverageDoc( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
    xmlOutputter.output( getDescribeCoverageDoc(), pw );
  }

  public Document generateDescribeCoverageDoc()
  {
    // CoverageDescriptions (wcs) [1]
    Element coverageDescriptionsElem = new Element( "CoverageDescriptions", wcsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( owcsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( owsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( xlinkNS );

    for ( String curId : this.identifiers)
      coverageDescriptionsElem.addContent( genCovDescrip( curId) );

    return new Document( coverageDescriptionsElem );
  }

  public Element genCovDescrip( String covId )
  {
    // CoverageDescriptions/CoverageDescription (wcs) [1..*]
    Element covDescripElem = new Element( "CoverageDescription", wcsNS );

    // CoverageDescriptions/CoverageDescription/Title (ows) [0..1]
    // CoverageDescriptions/CoverageDescription/Abstract (ows) [0..1]
    // CoverageDescriptions/CoverageDescription/Keywords (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Keywords/Keyword (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Identifier (wcs) [1]
    covDescripElem.addContent( new Element( "Identifier", wcsNS).addContent( covId));
    // CoverageDescriptions/CoverageDescription/Metadata (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Domain (wcs) [1]
    // CoverageDescriptions/CoverageDescription/Range (wcs) [1]
    // CoverageDescriptions/CoverageDescription/SupportedCRS (wcs) [1..*] - URI
    // CoverageDescriptions/CoverageDescription/SupportedFormat (wcs) [1..*] - MIME Type (e.g., "application/x-netcdf")

    return covDescripElem;
  }
}
