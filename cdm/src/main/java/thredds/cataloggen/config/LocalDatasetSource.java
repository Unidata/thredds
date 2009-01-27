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
// $Id: LocalDatasetSource.java 63 2006-07-12 21:50:51Z edavis $

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
 * @version $Id: LocalDatasetSource.java 63 2006-07-12 21:50:51Z edavis $
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
