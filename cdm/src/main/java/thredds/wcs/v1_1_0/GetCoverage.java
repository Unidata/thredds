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
import java.util.Collections;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.util.DiskCache2;
import ucar.ma2.InvalidRangeException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCoverage
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( GetCoverage.class );

  protected static final Namespace owcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1/ows" );
  protected static final Namespace owsNS = Namespace.getNamespace( "ows", "http://www.opengis.net/ows" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  private URI serverURI;
  private String identifier;

  private String version = "1.1.0";
  private String datasetPath;
  private GridDataset dataset;

  private Document getCoverageDoc;

  public GetCoverage( URI serverURI, String identifier,
                      String datasetPath, GridDataset dataset )
  {
    this.serverURI = serverURI;
    this.identifier = identifier;
    this.datasetPath = datasetPath;
    this.dataset = dataset;
    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Non-null server URI required." );
    if ( this.identifier == null )
      throw new IllegalArgumentException( "Non-null coverage identifier required." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  private boolean dataOnlyRequest = true;
  public boolean isDataOnlyRequest() { return dataOnlyRequest; }

  //public NetcdfFile getCoverageData() {}

  static private DiskCache2 diskCache = null;

  static public void setDiskCache( DiskCache2 _diskCache )
  {
    diskCache = _diskCache;
  }

  static private DiskCache2 getDiskCache()
  {
    if ( diskCache == null )
    {
      log.error( "getDiskCache(): Disk cache has not been set.");
      throw new IllegalStateException( "Disk cache must be set before calling GetCoverage.getDiskCache().");
      //diskCache = new DiskCache2( "/wcsCache/", true, -1, -1 );
    }
    return diskCache;
  }


  public File writeCoverageDataToFile()
          throws WcsException
  {
    try
    {
      File dir = new File( getDiskCache().getRootDirectory() );
      File ncFile = File.createTempFile( "WCS", ".nc", dir );

      NetcdfCFWriter writer = new NetcdfCFWriter();
      writer.makeFile( ncFile.getPath(), dataset,
                       Collections.singletonList( identifier ), null, null,
  //                     Collections.singletonList( req.getCoverage() ),
  //                     req.getBoundingBox(), dateRange,
                       true, 1, 1, 1 );
      return ncFile;
    }
    catch ( InvalidRangeException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to subset coverage <" + identifier + ">: " + e.getMessage());
      throw new WcsException( WcsException.Code.UnsupportedCombination, "", "Failed to subset coverage <" + identifier + ">.");
    }
    catch ( IOException e )
    {
      log.error( "writeCoverageDataToFile(): Failed to write file for requested coverage <" + identifier + ">: " + e.getMessage());
      throw new WcsException( WcsException.Code.NoApplicableCode, "", "Problem creating coverage <" + identifier + ">." );
    }
  }

  public Document getGetCoverageDoc()
  {
    if ( this.getCoverageDoc == null )
      getCoverageDoc = generateGetCoverageDoc();
    return getCoverageDoc;
  }

  public void writeGetCoverageDoc( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
    xmlOutputter.output( getGetCoverageDoc(), pw );
  }

  public Document generateGetCoverageDoc()
  {
    // Coverages (owcs) [1]
    Element coveragesElem = new Element( "Coverages", owcsNS );
    coveragesElem.addNamespaceDeclaration( owsNS );
    coveragesElem.addNamespaceDeclaration( xlinkNS );

    coveragesElem.addContent( genCoverage( this.identifier ) );

    return new Document( coveragesElem );
  }

  public Element genCoverage( String covId )
  {
    // Coverages/Coverage (owcs) [1..*]
    Element covDescripElem = new Element( "Coverage", owcsNS );

    // CoverageDescriptions/CoverageDescription/Abstract (ows) [0..1]
    // CoverageDescriptions/CoverageDescription/Keywords (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Keywords/Keyword (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Identifier (wcs) [1]
    //covDescripElem.addContent( new Element( "Identifier", wcsNS ).addContent( covId ) );
    // CoverageDescriptions/CoverageDescription/Metadata (ows) [0..*]
    // CoverageDescriptions/CoverageDescription/Domain (wcs) [1]
    // CoverageDescriptions/CoverageDescription/Range (wcs) [1]
    // CoverageDescriptions/CoverageDescription/SupportedCRS (wcs) [1..*] - URI
    // CoverageDescriptions/CoverageDescription/SupportedFormat (wcs) [1..*] - MIME Type (e.g., "application/x-netcdf")

    return covDescripElem;
  }
}
