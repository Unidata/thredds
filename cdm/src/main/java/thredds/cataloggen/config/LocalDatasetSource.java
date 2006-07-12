// $Id$

package thredds.cataloggen.config;

import thredds.catalog.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Concrete implementation of DatasetSource for local disk sources.
 *
 * @author Ethan Davis
 * @version $Id$
 */

public class LocalDatasetSource extends DatasetSource
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LocalDatasetSource.class);
  // private static Log log = LogFactory.getLog( LocalDatasetSource.class);

  private File accessPointHeaderFile = null;
  private File accessPointFile = null;

  LocalDatasetSource()
  {
      this.type = DatasetSourceType.getType( "Local");
  }

  protected InvDataset createDataset( String datasetLocation, String prefixUrlPath )
          throws IOException
  {
    if ( datasetLocation == null) throw new NullPointerException( "Dataset location cannot be null.");
    this.checkAccessPoint();
    return( new LocalInvDataset( null, new File( datasetLocation), prefixUrlPath));
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
   * @throws IOException if top-level dataset does not exist or is not a collection dataset.
   */
  protected InvCatalog createSkeletonCatalog( String prefixUrlPath ) throws IOException
  {
    this.checkAccessPoint();

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
    File apFile = new File( this.getAccessPoint() );
    InvDatasetImpl topDs = new LocalInvDataset( null, apFile, prefixUrlPath);

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

  private void checkAccessPoint()
          throws IOException
  {
    if ( accessPointHeaderFile == null )
    {
      // Check that the accessPointHeader local file exists.
      File aphFile = new File( this.getResultService().getAccessPointHeader() );
      if ( !aphFile.exists() ) throw new IOException( "The accessPointHeader local file does not exist <" + aphFile.getPath() + ">." );

      // Check that the accessPoint local file exists.
      File apFile = new File( this.getAccessPoint() );
      if ( !apFile.exists() ) throw new IOException( "The accessPoint local file does not exist <" + apFile.getPath() + ">." );
      if ( !apFile.isDirectory() ) throw new IOException( "The accessPoint local file is not a directory <" + apFile.getPath() + ">." );

      // Check that accessPoint file starts with accessPointHeader.
      if ( ! apFile.getPath().startsWith( aphFile.getPath() )
           && ! apFile.getCanonicalPath().startsWith( aphFile.getCanonicalPath() ) )
      {
        String tmpMsg = "The accessPoint <" + apFile.getPath() + " or " + apFile.getCanonicalPath() + "> must start with the accessPointHeader <" + aphFile.getPath() + " or " + aphFile.getCanonicalPath() + ">.";
        logger.debug( "checkAccessPoint(): {}", tmpMsg );
        throw new IOException( tmpMsg );
      }

      // Save the accessPointHeader as a URI so encodings and "/" vs "\" issues are handled.
      accessPointHeaderFile = aphFile;
      accessPointFile = apFile;
    }
  }

  /**
   * Return true if the given dataset is a collection dataset, false otherwise.
   *
   * @param dataset - the InvDataset to test for being a collection dataset.
   * @return true if the given dataset is a collection dataset, false otherwise.
   * @throws NullPointerException if the given InvDataset is null.
   * @throws ClassCastException if the given InvDataset is not a LocalInvDataset.
   */
  protected boolean isCollection( InvDataset dataset)
  {
    return( ((LocalInvDataset) dataset).isDirectory());
  }

  /**
   * Return a list of the InvDatasets contained in the given collection dataset
   * on this DatasetSource.
   *
   * @param dataset - the collection dataset to be expanded.
   * @return A list of the InvDatasets contained in the given collection dataset.
   * @throws IllegalArgumentException when given dataset is not a collection dataset.
   * @throws NullPointerException if given dataset is null.
   * @throws ClassCastException if the given InvDataset is not a LocalInvDataset.
   */
  protected List expandThisLevel( InvDataset dataset, String prefixUrlPath )
  {
    if ( dataset == null) throw new NullPointerException( "Given dataset cannot be null.");
    if ( ! isCollection( dataset)) throw new IllegalArgumentException( "Dataset \"" + dataset.getName() + "\" is not a collection dataset.");

    // Deal with all files in this directory.
    File theDir = new File( ((LocalInvDataset) dataset).getLocalPath() );
    File[] allFiles = theDir.listFiles();
    InvDataset curDs = null;
    ArrayList list = new ArrayList();
    for ( int i = 0; i < allFiles.length; i++)
    {
      try
      {
        curDs = new LocalInvDataset( dataset, allFiles[ i], prefixUrlPath );
      }
      catch ( IOException e )
      {
        // Given file doesn't exist or is not under the accessPointHeader directory, skip.
        continue;
      }

      list.add( curDs);
    }

    return( list);
  }

  /**
   * An InvDatasetImpl subclass to deal with datasets on local disk.
   */
  private class LocalInvDataset extends InvDatasetImpl
  {
    private String relativePath;
    private String catRelativePath;
    private String localPath;
    private boolean directory;

    LocalInvDataset( InvDataset parent, File file, String prefixUrlPath) throws IOException
    {
      super( (InvDatasetImpl) parent, null, null, null, null );
      if (logger.isDebugEnabled())
        logger.debug( "LocalInvDataset(): parent=" + ( parent == null ? "" : parent.getName() ) + "; file=" + file.getPath());

      if ( ! file.exists()) throw new IOException( "File <" + file.getPath() + "> does not exist.");
      this.directory = file.isDirectory();

      // Determine the datasets urlPath by removing accessPointHeader from localPath.
      // (Use URIs to handle encoding and slash vs backslash issues.)
      String uriStringFromFile = file.toURI().toString();
      String uriStringFromCanonicalFile = file.getCanonicalFile().toURI().toString();
      String aphUriStringFromFile = accessPointHeaderFile.toURI().toString();
      String aphUriStringFromCanonicalFile = accessPointHeaderFile.getCanonicalFile().toURI().toString();
      String apUriStringFromFile = accessPointFile.toURI().toString();
      String apUriStringFromCanonicalFile = accessPointFile.getCanonicalFile().toURI().toString();
      relativePath = null;

      if (logger.isDebugEnabled()) {
        logger.debug( "LocalInvDataset():               file URI=" + uriStringFromFile );
        logger.debug( "LocalInvDataset():     canonical file URI=" + uriStringFromCanonicalFile );
        logger.debug( "LocalInvDataset():           aph file URI=" + aphUriStringFromFile );
        logger.debug( "LocalInvDataset(): aph canonical file URI=" + aphUriStringFromCanonicalFile );
      }

      // NOTE: Started using File.getCanonicalFile() to deal with comparisons containing this (".") or parent ("..") directories.
      //       But that caused caused comparison problems for symbolic links (e.g., on motherlode "/usr/local/apache/htdocs/motherlode/dods/model"
      //       does not start with "/usr/local/apache/htdocs/motherlode" because "model" is a link to "/data/ldm/pub/decoded/netcdf/GRIB").
      if ( ! uriStringFromFile.startsWith( aphUriStringFromFile ) )
      {
        if ( ! uriStringFromCanonicalFile.startsWith( aphUriStringFromCanonicalFile ) )
        {
          String tmpMsg = "File <" + uriStringFromFile + " or " + uriStringFromCanonicalFile + "> must start with accessPointHeader <" + aphUriStringFromFile + " or " + aphUriStringFromCanonicalFile + ">.";
          throw new IOException( tmpMsg );
        }
        else
        {
          relativePath = uriStringFromCanonicalFile.substring( aphUriStringFromCanonicalFile.length() );
          catRelativePath = uriStringFromCanonicalFile.substring( apUriStringFromCanonicalFile.length());
          localPath = file.getCanonicalPath();
        }
      }
      else
      {
        relativePath = uriStringFromFile.substring( aphUriStringFromFile.length() );
        catRelativePath = uriStringFromFile.substring( apUriStringFromFile.length() );
        localPath = file.getAbsolutePath();
      }

      if (logger.isDebugEnabled()) {
        logger.debug( "LocalInvDataset():           relativePath=" + relativePath );
        logger.debug( "LocalInvDataset():              localPath=" + localPath );
      }

      if ( ! this.directory )
      {
        // Check if service base is relative to the catalog URL the accessPointHeader.
        if ( LocalDatasetSource.this.getResultService().getBase().equals( "" )
             && ! LocalDatasetSource.this.getResultService().getServiceType().equals( ServiceType.COMPOUND) )
        {
//          String tmpUrlPath = prefixUrlPath == null || prefixUrlPath.equals( "")
//                              ? catRelativePath
//                              : prefixUrlPath + "/" + catRelativePath;
          this.setUrlPath( catRelativePath  );
        }
        else
        {
          String tmpUrlPath = prefixUrlPath == null || prefixUrlPath.equals( "" )
                              ? relativePath
                              : prefixUrlPath + "/" + relativePath;
          this.setUrlPath( tmpUrlPath );
        }

        // @todo Should make this optional, maybe with DatasetEnhancer1 (but by then only have dataset, not file).
        if ( LocalDatasetSource.this.isAddDatasetSize() )
        {
          this.setDataSize( file.length());
        }
      }
      String dsName = relativePath.endsWith( "/")
                      ? relativePath.substring( 0, relativePath.length() - 1)
                      : relativePath;
      int index = dsName.lastIndexOf( "/");
      if ( index != -1 ) dsName = dsName.substring( index + 1);
      this.setName( dsName);
    }

    String getLocalPath() { return( this.localPath); }
    void setLocalPath( String localPath ) { this.localPath = localPath; }

    public boolean isDirectory() { return( this.directory); }
  }
}
/*
 * $Log: LocalDatasetSource.java,v $
 * Revision 1.27  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.26  2005/12/06 19:39:20  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.25  2005/07/22 16:19:50  edavis
 * Allow DatasetSource and InvDatasetScan to add dataset size metadata.
 *
 * Revision 1.24  2005/07/21 17:19:17  edavis
 * Fix for InvDatasetScan and catalog relative services.
 *
 * Revision 1.23  2005/07/20 22:44:54  edavis
 * Allow InvDatasetScan to work with a service that is not catalog relative.
 * (DatasetSource can now add a prefix path name to resulting urlPaths.)
 *
 * Revision 1.22  2005/07/08 18:34:59  edavis
 * Fix problem dealing with service URLs that are relative
 * to the catalog (base="") and those that are relative to
 * the collection (base URL is not empty).
 *
 * Revision 1.21  2005/06/30 14:42:00  edavis
 * Change how LocalDatasetSource compares datasets to the accessPointHeader.
 * Using File.getPath() has problems with this (".") and parent ("..") directories.
 * Using File.getCanonicalPath() has problems if a symbolic link is in the dataset
 * path above the accessPointHeader path. So, do both if necessary.
 *
 * Revision 1.20  2005/06/11 19:03:56  caron
 * no message
 *
 * Revision 1.19  2005/06/08 21:20:15  edavis
 * Fixed naming of top dataset in InvDatasetScan produced catalogs
 * (removed "/" from end of name). Added to TestInvDatasetScan.
 *
 * Revision 1.18  2005/06/06 18:25:52  edavis
 * Update DirectoryScanner to allow all directories even if name
 * doesn't send with "/".
 *
 * Revision 1.17  2005/06/03 19:12:41  edavis
 * Start adding wildcard handling in DirectoryScanner. Change
 * how DatasetSource names datasets and how catalogRefs are
 * constructed in DatasetSource.expand().
 *
 * Revision 1.16  2005/04/20 00:05:38  caron
 * *** empty log message ***
 *
 * Revision 1.15  2005/04/05 22:37:01  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.14  2005/03/31 23:12:20  edavis
 * Some fixes for CatalogGen tests.
 *
 * Revision 1.13  2005/01/20 23:13:30  edavis
 * Extend DirectoryScanner to handle catalog generation for a list of top-level
 * data directories:
 * 1) add getMainCatalog(List):void to DirectoryScanner;
 * 2) add expand(List):void to DatasetSource, and
 * 3) two changes to the abstract methods in DatasetSource:
 *   a) add createDataset(String):InvDataset and
 *   b) rename getTopLevelDataset():InvDataset to
 *      createSkeletonCatalog():InvDataset.
 *
 * Revision 1.12  2004/12/28 22:49:56  edavis
 * Minor changes to some comments.
 *
 * Revision 1.11  2004/12/22 22:28:59  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.10  2004/11/30 22:49:12  edavis
 * Start changing DatasetSource into a more usable API.
 *
 * Revision 1.9  2004/06/03 20:27:24  edavis
 * Update for thredds.catalog changes for InvCatalog 1.0.
 *
 * Revision 1.8  2004/05/11 20:38:46  edavis
 * Update for changes to thredds.catalog object model (still InvCat 0.6).
 * Start adding some logging statements.
 *
 * Revision 1.7  2003/09/24 19:36:31  edavis
 * Fix how "givenLocation" is handled in expandThisType().
 *
 * Revision 1.6  2003/09/12 20:57:26  edavis
 * Deal with possibility that the "givenLocation" string handed to
 * expandThisType() might be either a local file name or a "file:" URI.
 *
 * Revision 1.5  2003/09/05 22:06:24  edavis
 * Add more logging.
 *
 */
