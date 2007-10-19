package thredds.wcs.v1_0_0_Plus;

import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

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
public class DescribeCoverage extends WcsRequest
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( DescribeCoverage.class );
  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs" );
  protected static final Namespace gmlNS = Namespace.getNamespace( "gml", "http://www.opengis.net/gml" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  private URI serverURI;
  private List<String> coverages;

  private Document describeCoverageDoc;

  public DescribeCoverage( Operation operation, String version, String datasetPath, GridDataset dataset,
                           URI serverURI, List<String> coverages )
  {
    super( operation, version, datasetPath, dataset );

    this.serverURI = serverURI;
    this.coverages = coverages;
    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Non-null server URI required." );
    if ( this.coverages == null )
      throw new IllegalArgumentException( "Non-null coverage list required." );
    if ( this.coverages.size() < 1 )
      throw new IllegalArgumentException( "Coverage list must contain at least one ID <" + this.coverages.size() + ">." );
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
    XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    xmlOutputter.output( getDescribeCoverageDoc(), pw );
  }

  public Document generateDescribeCoverageDoc()
  {
    // CoverageDescriptions (wcs) [1]
    Element coverageDescriptionsElem = new Element( "CoverageDescriptions", wcsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( gmlNS );
    coverageDescriptionsElem.addNamespaceDeclaration( xlinkNS );

    for ( String curCoverageId : this.coverages )
      coverageDescriptionsElem.addContent( genCovDescrip( curCoverageId ) );

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
    covDescripElem.addContent( new Element( "Identifier", wcsNS ).addContent( covId ) );
    // CoverageDescriptions/CoverageDescription/Metadata (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Domain (wcs) [1]
    // CoverageDescriptions/CoverageDescription/Range (wcs) [1]
    // CoverageDescriptions/CoverageDescription/SupportedCRS (wcs) [1..*] - URI
    // CoverageDescriptions/CoverageDescription/SupportedFormat (wcs) [1..*] - MIME Type (e.g., "application/x-netcdf")

    return covDescripElem;
  }

}
