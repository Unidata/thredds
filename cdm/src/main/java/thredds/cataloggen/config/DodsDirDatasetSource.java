// $Id: DodsDirDatasetSource.java,v 1.14 2006/01/20 02:08:23 caron Exp $

package thredds.cataloggen.config;

import thredds.catalog.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;


/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 * @author Ethan Davis
 * @version $Id: DodsDirDatasetSource.java,v 1.14 2006/01/20 02:08:23 caron Exp $
 */

public class DodsDirDatasetSource extends DatasetSource
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DodsDirDatasetSource.class);
  //private static Log log = LogFactory.getLog( DodsDirDatasetSource.class);

  private URI accessPointHeaderUri = null;

  private DodsURLExtractor urlExtractor = null;

  public DodsDirDatasetSource()
  {
    this.type = DatasetSourceType.getType( "DodsDir");
    urlExtractor = new DodsURLExtractor();
  }

  protected InvDataset createDataset( String datasetLocation, String prefixUrlPath ) throws IOException
  {
    URI dsLocUri = null;
    try
    {
      dsLocUri = new URI( datasetLocation);
    }
    catch ( URISyntaxException e )
    {
      throw new IOException( "URISyntaxException for dataset location <" + datasetLocation + ">: " + e.getMessage());
    }
    return( new DodsDirInvDataset( null, dsLocUri));
  }

  /**
   * Build an unnamed InvCatalog for this DatasetSource and return the
   * top-level InvDataset. The ResultService for this DatasetSource is
   * used to create the InvService for the new InvCatalog. Each InvDataset
   * in the catalog is named with the location of the object they represent
   * on the dataset source.
   *
   * @return the top-level dataset of the newly constructed InvCatalog.
   *
   * @throws java.io.IOException if top-level dataset does not exist or is not a collection dataset.
   */
  protected InvCatalog createSkeletonCatalog( String prefixUrlPath ) throws IOException
  {
    String aphString = this.getResultService().getAccessPointHeader();
    String apString = this.getAccessPoint();

    // Check that accessPoint URL starts with accessPointHeader.
    if ( ! apString.startsWith( aphString))
      throw new IOException( "The accessPoint <" + apString + "> must start with the accessPointHeader <" + aphString + ">.");

    // Check that accessPoint URL ends with a slash ("/").
    if ( ! apString.endsWith( "/")) throw new IOException( "The accessPoint URL must end with a \"/\" <" + apString + ">.");

    // Check that accessPoint URL is an OPeNDAP server URL.
    String apVersionString = apString + "version";
    String apVersionResultContent = null;
    try
    {
      apVersionResultContent = urlExtractor.getTextContent( apVersionString);
    }
    catch (java.io.IOException e)
    {
      String tmpMsg = "The accessPoint URL is not an OPeNDAP server URL (no version info) <" + apVersionString + ">";
      log.error( "expandThisType(): " + tmpMsg, e);
      IOException myE = new IOException( tmpMsg + e.getMessage());
      myE.initCause( e);
      throw( myE);
    }
    if ( apVersionResultContent.indexOf( "DODS") == -1 &&
         apVersionResultContent.indexOf( "OPeNDAP") == -1 &&
         apVersionResultContent.indexOf( "DAP") == -1)
    {
      String tmpMsg = "The accessPoint URL version info is not valid <" + apVersionResultContent + ">";
      log.error(  "expandThisType(): " + tmpMsg);
      throw new IOException( tmpMsg);
    }

    // Some setup stuff
    try
    {
      accessPointHeaderUri = new URI( aphString);
    }
    catch ( URISyntaxException e )
    {
      throw new IOException("The accessPointHeader URL failed to map to a URI <" + aphString + ">.");
    }

    // Create catalog.
    InvCatalogImpl catalog = new InvCatalogImpl( null, null, null );
                                                 //this.getName(), null, null);

    // Create service.
    InvService service = new InvService( this.getResultService().getName(),
                                         this.getResultService().getServiceType().toString(),
                                         this.getResultService().getBase(),
                                         this.getResultService().getSuffix(),
                                         this.getResultService().getDescription());
    for ( Iterator it = this.getResultService().getProperties().iterator(); it.hasNext(); )
    {
      service.addProperty( (InvProperty) it.next() );
    }
    for ( Iterator it = this.getResultService().getServices().iterator(); it.hasNext(); )
    {
      service.addService( (InvService) it.next() );
    }

    // Add service to catalog.
    catalog.addService( service);

    // Create top-level dataset.
    DodsDirInvDataset topDs = null;
    try
    {
      topDs = new DodsDirInvDataset( null, new URI( apString));
    }
    catch ( URISyntaxException e )
    {
      throw new IOException("The accessPoint URL failed to map to a URI <" + apString + ">.");
    }

    // Set the serviceName (inherited by all datasets) in top-level dataset.
    ThreddsMetadata tm = new ThreddsMetadata( false);
    tm.setServiceName( service.getName());
    InvMetadata md = new InvMetadata( topDs, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm);
    ThreddsMetadata tm2 = new ThreddsMetadata( false);
    tm2.addMetadata( md);
    topDs.setLocalMetadata(tm2);

    // Add top-level dataset to catalog.
    catalog.addDataset( topDs);

    // Tie up any loose ends in catalog with finish().
    ((InvCatalogImpl) catalog).finish();

    return( catalog);
  }

  /**
   * Return true if the given dataset is a collection dataset, false otherwise.
   *
   * @param dataset - the InvDataset to test for being a collection dataset.
   * @return true if the given dataset is a collection dataset, false otherwise.
   * @throws NullPointerException if the given InvDataset is null.
   * @throws ClassCastException if the given InvDataset is not a DodsDirInvDataset.
   */
  protected boolean isCollection( InvDataset dataset )
  {
    return( ((DodsDirInvDataset) dataset).isDirectory());
  }

  /**
   * Return a list of the InvDatasets contained in the given collection dataset
   * on this DatasetSource.
   *
   * @param dataset - the collection dataset to be expanded.
   * @return A list of the InvDatasets contained in the given collection dataset.
   * @throws IllegalArgumentException when given dataset is not a collection dataset.
   * @throws NullPointerException if given dataset is null.
   * @throws ClassCastException if the given InvDataset is not a DodsDirInvDataset.
   */
  protected List expandThisLevel( InvDataset dataset, String prefixUrlPath )
  {
    // @todo Switch to return type of InvDataset (???) so can return error messages about datasets removed from list.
    if ( dataset == null) throw new NullPointerException( "Given dataset cannot be null.");
    if ( ! isCollection( dataset)) throw new IllegalArgumentException( "Dataset \"" + dataset.getName() + "\" is not a collection dataset.");

    List dsList = new ArrayList();

    // Get list of possible datasets from current URL.
    List possibleDsList = null;
    try
    {
      possibleDsList = urlExtractor.extract( dataset.getName());
    }
    catch (java.io.IOException e)
    {
      log.warn(  "expandThisLevel(): IOException while extracting dataset info from given OPeNDAP directory <" + dataset.getName() + ">, return empty list: " + e.getMessage());
      return( dsList);
    }

    // Handle each link in the current access path.
    String curDsUrlString = null;
    URI curDsUri = null;
    InvDataset curDs = null;
    for ( Iterator it = possibleDsList.iterator(); it.hasNext() ; ) // @todo curDsUrlString = (String) it.next())
    {
      curDsUrlString = (String) it.next();

      // Skip datasets that aren't OPeNDAP datasets (".html") or collection datasets ("/").
      if ( (! curDsUrlString.endsWith( ".html")) && (! curDsUrlString.endsWith( "/")))
      {
        log.warn(  "expandThisLevel(): Dataset isn't an OPeNDAP dataset or collection dataset, skip <" + dataset.getName() + ">.");
        continue;
      }

      // Remove ".html" extension.
      if ( curDsUrlString.endsWith( ".html"))
      {
        curDsUrlString = curDsUrlString.substring( 0, curDsUrlString.length() - 5);
      }

      // Avoid links back down the path hierarchy (i.e., parent directory links).
      if ( ! curDsUrlString.startsWith( this.accessPointHeaderUri.toString()))
      {
        log.debug( "expandThisLevel(): current path <" + curDsUrlString + "> not child of given" +
                " location <" + this.accessPointHeaderUri.toString() + ">, skip.");
        continue;
      }

      // Get URI from URL string.
      try
      {
        curDsUri = new URI( curDsUrlString);
      }
      catch ( URISyntaxException e )
      {
        log.error( "expandThisLevel(): Skipping dataset  <" + curDsUrlString + "> due to URISyntaxException: " + e.getMessage());
        continue;
      }
      log.debug( "expandThisLevel(): handle dataset (" + curDsUrlString + ")");

      try
      {
        curDs = new DodsDirInvDataset( null, curDsUri);
      }
      catch ( IOException e )
      {
        log.warn( "expandThisLevel(): skipping dataset <" + curDsUri.toString() + ">, not under accessPointHeader: " + e.getMessage());
        continue;
      }

      dsList.add( curDs);
    } // END - loop through files in current directory

    return( dsList);
  }

  private class DodsDirInvDataset extends InvDatasetImpl
  {
    private URI uri = null;
    private boolean directory = false;

    DodsDirInvDataset( InvDataset parent, URI uri)
            throws IOException
    {
      super( (InvDatasetImpl) parent, null, null, null, null );
      this.uri = uri;
      this.directory = uri.toString().endsWith("/");

      // Determine the datasets urlPath by removing accessPointHeader from uri.
      // (Use URIs to handle encoding and slash vs backslash issues.)
      String dsAbsolutePath = uri.toString();
      String dsRelativePath = null;
      if ( dsAbsolutePath.startsWith( accessPointHeaderUri.toString()))
      {
        dsRelativePath = dsAbsolutePath.substring( accessPointHeaderUri.toString().length());
      }
      else
      {
        throw new IOException( "URI <" + dsAbsolutePath + "> not under accessPointHeader directory <" + accessPointHeaderUri.toString() + ">.");
      }

      if ( ! this.directory ) this.setUrlPath( dsRelativePath );
      this.setName( dsRelativePath);
    }

    URI getUri() { return( this.uri); }
    void setUri( URI uri ) { this.uri = uri; }
    boolean isDirectory() { return( this.directory); }

  }

}
/*
 * $Log: DodsDirDatasetSource.java,v $
 * Revision 1.14  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.13  2005/11/18 23:51:03  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.12  2005/07/20 22:44:54  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.11  2005/04/20 00:05:38  caron
 * *** empty log message ***
 *
 * Revision 1.10  2005/04/05 22:37:01  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.9  2005/01/20 23:13:30  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.8  2004/12/28 22:49:56  edavis
 * Minor changes to some comments.
 *
 * Revision 1.7  2004/12/22 22:28:59  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.6  2004/11/30 22:49:12  edavis
 * Start changing DatasetSource into a more usable API.
 *
 * Revision 1.5  2004/06/03 20:27:24  edavis
 * Update for thredds.catalog changes for InvCatalog 1.0.
 *
 * Revision 1.4  2004/05/11 20:38:46  edavis
 * Update for changes to thredds.catalog object model (still InvCat 0.6).
 * Start adding some logging statements.
 *
 * Revision 1.3  2003/09/05 22:05:52  edavis
 * Expand logging.
 *
 */
