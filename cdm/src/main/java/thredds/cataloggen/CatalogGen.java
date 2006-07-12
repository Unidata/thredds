// $Id$

package thredds.cataloggen;

import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.cataloggen.config.CatGenConfigMetadataFactory;
import thredds.cataloggen.config.CatalogGenConfig;
import thredds.cataloggen.config.DatasetSource;
import thredds.datatype.DateType;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//import org.apache.log4j.*;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * CatalogGen crawls dataset sources given in a CatalogGenConfig file
 * to produce THREDDS catalogs.
 *
 * To generate a catalog from a config file:
 * <pre>
 *   String inFileName = "file:/home/edavis/testCatGenConfig.xml";
 *   String outFileName = "/home/edavis/testCatGenConfig-results.xml";
 *   StringBuffer log = new StringBuffer();
 *   CatalogGen catGen = new CatalogGen( inFileName);
 *   if ( catGen.isValid( log))
 *   {
 *     catGen.expand();
 *     catGen.writeCatalog( outFileName);
 *   }
 * </pre>
 *
 *
 * @author Ethan Davis
 * @version $Ver$
 */
public class CatalogGen
{
  //private static Log log = LogFactory.getLog( CatalogGen.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogGen.class);

  /** The catalog: initially as a CatGen config file, until expanded. */
  private InvCatalog catalog = null;

  /** The catalog factory that knows about CatalogGenConfig metadata. */
  protected InvCatalogFactory catFactory = null;

  public List getCatalogRefInfoList()
  {
    return catalogRefInfoList;
  }

  private List catalogRefInfoList = new ArrayList();


  /**
   * Constructs the CatalogGen for the given config document.
   *
   * @param configDocURL - the URL of the configuration document
   */
  public CatalogGen( URL configDocURL)
  {
    // Create a InvCatalogFactory with CATALOG_GEN_CONFIG MetadataType registered.
    log.debug( "CatalogGen(URL): create catalog and CatalogGenConfig converter." );
    this.catFactory = InvCatalogFactory.getDefaultFactory( true );
    this.catFactory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
                                             new CatGenConfigMetadataFactory());

    // Read the given XML config file.
    log.debug( "CatalogGen(URL): reading the config doc <" + configDocURL.toString() + ">.");
    this.catalog = this.catFactory.readXML( configDocURL.toString());
    log.debug( "CatalogGen(URL): done.");
  }

  /**
   * Constructs the CatalogGen for the given config document InputStream.
   *
   * @param configDocInputStream - the InputStream from which to read the config document.
   * @param configDocURL - the URL for the config document.
   */
  public CatalogGen( InputStream configDocInputStream, URL configDocURL )
  {
    // Create a InvCatalogFactory with CATALOG_GEN_CONFIG MetadataType registered.
    log.debug( "CatalogGen(InputStream): create catalog and CatalogGenConfig converter." );
    this.catFactory = new InvCatalogFactory( "default", true );
    this.catFactory.registerMetadataConverter( MetadataType.CATALOG_GEN_CONFIG.toString(),
                                               new CatGenConfigMetadataFactory() );

    // Read the given XML config file.
    log.debug( "CatalogGen(InputStream): reading the config doc <" + configDocURL.toString() + ">." );
    this.catalog = this.catFactory.readXML( configDocInputStream, URI.create( configDocURL.toExternalForm()) );
    log.debug( "CatalogGen(InputStream): CatalogGenConfig doc <" + this.catalog.getName() + "> read.");

  }

  /**
   * Checks the validity of the configuration file.
   * @param out - a StringBuffer with validity error and warning messages.
   * @return - true if no errors, false if errors exist
   */
  public boolean isValid( StringBuffer out)
  {
    log.debug( "isValid(): start");
    return( this.catalog.check( out));
  }

  /**
   * Expand the catalog. Each of the CatalogGenConfig metadata elements
   * is expanded into its constituent datasets.
   */
  public InvCatalog expand()
  {
    CatalogGenConfig tmpCgc = null;
    List cgcList = null;
    DatasetSource dss = null;

    // Find and loop through each CatGenConfigMetadata object.
    List mdataList = findCatGenConfigMdata( this.catalog.getDatasets());
    for ( int i = 0; i < mdataList.size(); i++)
    {
      InvMetadata curMdata = (InvMetadata) mdataList.get( i);
      InvDatasetImpl curParentDataset = ( (InvDatasetImpl) curMdata.getParentDataset());

      // Loop through the CatalogGenConfig objects in current InvMetadata.
      cgcList = (List) curMdata.getContentObject();
      for ( int j = 0; j < cgcList.size(); j++)
      {
        tmpCgc = (CatalogGenConfig) cgcList.get( j);
        log.debug( "expand(): mdata # " + i + " and catGenConfig # " + j + "." );
        dss = tmpCgc.getDatasetSource();
        InvCatalog generatedCat = null;
        try
        {
          generatedCat = dss.fullExpand();
        }
        catch ( IOException e )
        {
          String tmpMsg = "Error: IOException on fullExpand() of DatasetSource <" + dss.getName() + ">: " + e.getMessage();
          log.error( "expand(): " + tmpMsg);
          curParentDataset.addDataset( new InvDatasetImpl( curParentDataset, tmpMsg));
          break;
        }

        catalogRefInfoList.addAll( dss.getCatalogRefInfoList());

        // Always a single top-level dataset in catalog returned by DatasetSource.fullExpand()
        InvDataset genTopDs = (InvDataset) generatedCat.getDatasets().get( 0);

        // Add all services in the generated catalog to the parent catalog.
        for( Iterator it = generatedCat.getServices().iterator(); it.hasNext(); )
        {
          ( (InvCatalogImpl) curParentDataset.getParentCatalog() ).addService( (InvService) it.next() );
        }

        // Add the generated catalog to the parent datasets, i.e., add all the
        // datasets that are children of the generated catalogs' top-level dataset.
        for ( Iterator it = genTopDs.getDatasets().iterator(); it.hasNext(); )
        {
          InvDatasetImpl curGenDataset = (InvDatasetImpl) it.next();
          if ( curGenDataset.hasNestedDatasets())
          {
            // If the dataset has children datasets, add serviceName and make it inherited.
            ThreddsMetadata tm = new ThreddsMetadata( false);
            tm.setServiceName( genTopDs.getServiceDefault().getName());
            InvMetadata md = new InvMetadata( genTopDs, null, XMLEntityResolver.CATALOG_NAMESPACE_10, "", true, true, null, tm);
            curGenDataset.getLocalMetadata().addMetadata( md);
          }
          else
          {
            // Otherwise, add serviceName not inherited.
            curGenDataset.getLocalMetadata().setServiceName(  genTopDs.getServiceDefault().getName());
          }
          curParentDataset.addDataset( curGenDataset);
        }

        curParentDataset.finish();
      }
//      log.debug( "expand(): List datasets that are siblinks of current metadata record CGCM(" + i + ").");
//      List list = curMdata.getParentDataset().getDatasets();
//      Iterator it = list.iterator();
//      while ( it.hasNext())
//      {
//        InvDatasetImpl curDs = (InvDatasetImpl) it.next();
//        log.debug( "Dataset URL is " + curDs.getUrlPath() + ".");
//        log.debug( "Dataset name is " + curDs.getName() + ".");
//      }

      // Remove the current metadata element from its parent dataset.
      log.debug( "expand(): Remove metadata record CGCM(" + i + ")." );
      curParentDataset.removeLocalMetadata( curMdata);
      // *****
    }

    // Finish this catalog now that done building it.
    ((InvCatalogImpl) this.catalog).finish();

    return( this.catalog);
  }

  public void setCatalogExpiresDate( DateType expiresDate )
  {
    ((InvCatalogImpl) this.catalog).setExpires( expiresDate);
  }

  /**
   * Writes the catalog as XML. The catalog is written to the file given
   * in <tt>outFileName</tt>. If <tt>outFileName</tt> is null, the catalog
   * is written to standard out.
   *
   * @param outFileName - the pathname of the output file.
   */
  public boolean writeCatalog( String outFileName)
  {
    log.debug( "writeCatalog(): writing catalog to " + outFileName + ".");

    String invCatDTD = "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd";
    log.debug( "writeCatalog(): set the catalogs DTD (" + invCatDTD + ").");
    // Set the catalogs DTD.
    ( (InvCatalogImpl) catalog).setDTDid( invCatDTD);

    // Print the catalog as an XML document.
    if ( outFileName == null)
    {
      try
      {
        log.debug( "writeCatalog(): write catalog to System.out.");
        this.catFactory.writeXML( (InvCatalogImpl) catalog, System.out);
      }
      catch ( java.io.IOException e)
      {
        log.debug( "writeCatalog(): exception when writing to stdout.\n" +
                e.toString());
        //e.printStackTrace();
        return( false);
      }
      return( true);
    }
    else
    {
      log.debug( "writeCatalog(): try writing catalog to the output file (" + outFileName + ").");
      try
      {

        if ( ! this.catalog.getVersion().equals( "1.0" ) )
        {
          this.catFactory.writeXML( (InvCatalogImpl) catalog, outFileName );
        }
        else
        {
          // Override default output catalog version. (kludge for IDV backward compatibility)
          InvCatalogFactory10 fac10 = (InvCatalogFactory10) this.catFactory.getCatalogConverter( XMLEntityResolver.CATALOG_NAMESPACE_10 );
          fac10.setVersion( this.catalog.getVersion() );
          BufferedOutputStream osCat = new BufferedOutputStream( new FileOutputStream( outFileName ) );
          fac10.writeXML( (InvCatalogImpl) catalog, osCat );
          osCat.close();
        }



      }
      catch ( IOException e )
      {
        log.debug( "writeCatalog(): IOException, catalog not written to " + outFileName + ": " + e.getMessage() );
        return ( false );
      }
      log.debug( "writeCatalog(): catalog written to " + outFileName + "." );
      return ( true );
    }
  }

  InvCatalog getCatalog() { return( this.catalog ); }

  private List findCatGenConfigMdata( List datasets)
  {
    List mdataList = new ArrayList();
    if ( datasets == null) return( mdataList );

    // Iterate through list of datasets.
    Iterator it = datasets.iterator();
    InvDataset curDataset = null;
    while ( it.hasNext() )
    {
      curDataset = (InvDataset) it.next();

      // Get all the local metadata for the given dataset.
      ThreddsMetadata tm = ((InvDatasetImpl) curDataset).getLocalMetadata();

      // Iterate over the local InvMetadata checking for CatalogGenConfig metadata.
      Iterator itMdata = tm.getMetadata().iterator();
      InvMetadata curMetadata = null;
      while ( itMdata.hasNext())
      {
        curMetadata = (InvMetadata) itMdata.next();
        if ( (curMetadata.getMetadataType() != null) &&
             curMetadata.getMetadataType().equals( MetadataType.CATALOG_GEN_CONFIG.toString() ) )
        {
          mdataList.add( curMetadata );
        }
        else if ( (curMetadata.getNamespaceURI() != null) &&
                  curMetadata.getNamespaceURI().equals( CatalogGenConfig.CATALOG_GEN_CONFIG_NAMESPACE_URI_0_5 ))
        {
          mdataList.add( curMetadata );
        }
      }

      // Recurse through nested datasets and find CatalogGenConfig metadata.
      mdataList.addAll( this.findCatGenConfigMdata( curDataset.getDatasets() ) );
    }

    return( mdataList);
  }
}
/*
 * $Log: CatalogGen.java,v $
 * Revision 1.26  2006/06/08 23:08:42  edavis
 * 1) Use catalog "expires" attribute to determine if TDS static catalog cache is stale:
 *     a) Setup CatalogGen tasks to add "expires" attribute to generated catalog.
 *     b) Add checking for expired catalog and re-reading of catalog from disk in DataRootHandler2.getCatalog()
 * 2) Fix DataRootHandler2 singleton init() and getInstance().
 *
 * Revision 1.25  2006/01/23 18:51:05  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.24  2006/01/20 02:08:23  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.23  2005/11/18 23:51:03  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.22  2005/08/23 23:00:51  edavis
 * Allow override of default output catalog version "1.0.1" to "1.0". This allows existing
 * IDV (which reads catalog version as float) to read InvCatalog 1.0.1 catalogs.
 *
 * Revision 1.21  2005/04/29 14:55:57  edavis
 * Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
 * signature. And start on allowing wildcard characters in pathname given
 * to DirectoryScanner.
 *
 * Revision 1.20  2005/04/27 23:05:40  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 * Revision 1.19  2005/04/20 00:05:38  caron
 * *** empty log message ***
 *
 * Revision 1.18  2005/04/05 22:37:02  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.17  2005/03/21 23:04:52  edavis
 * Update CatalogGen.main() so that all the catalogs referenced by catalogRefs
 * in the generated catalogs are in turn generated.
 *
 * Revision 1.16  2005/01/12 22:51:41  edavis
 * 1) Remove all empty collection datasets before returning the catalog from
 * DatasetSource.expand(). 2)Improve how a CatGen config document and the
 * generated datasets are merged, mainly involved the dropping of the top-level
 * generated dataset. 3) Provide for filtering by a group of DatasetFilters: add
 * reject capability, and add the acceptDatasetByFilterGroup() static method.
 *
 * Revision 1.15  2004/12/29 21:53:21  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.14  2004/12/23 23:05:01  edavis
 * Simplify DatasetSource.fullExpand() to always return the generated catalog.
 * The mergeing of the generated catalog into the parent catalog (a
 * CatalogGenConfig document) has been moved into CatalogGen.expand().
 *
 * Revision 1.13  2004/12/22 22:28:59  edavis
 * 1) Fix collection vs atomic dataset filtering includes fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 * 2) Convert DatasetSource subclasses to use isCollection(), getTopLevelDataset(), and expandThisLevel() instead of expandThisType().
 *
 * Revision 1.12  2004/11/30 23:07:49  edavis
 * Update for new DatasetSource API.
 *
 * Revision 1.11  2004/06/03 20:15:54  edavis
 * Modify findCatGenConfigMdata() to work with new catalog object model. Add
 * a constructor that reads from an InputStream.
 *
 * Revision 1.10  2004/05/11 20:13:12  edavis
 * Update for changes to thredds.catalog data model (still InvCat 0.6).
 *
 * Revision 1.9  2004/03/05 06:28:09  edavis
 * Fix the usage message.
 *
 * Revision 1.8  2003/09/24 19:35:17  edavis
 * Fix how output file is handled.
 *
 * Revision 1.7  2003/09/23 21:17:55  edavis
 * Allow configuration document to be specified as a local file path, as a local
 * file URL ("file:"), or as an HTTP URL ("http:").
 *
 * Revision 1.6  2003/09/05 22:01:33  edavis
 * Change some logging from debug() to info().
 *
 * Revision 1.5  2003/08/29 21:41:46  edavis
 * The following changes where made:
 *
 *  1) Added more extensive logging (changed from thredds.util.Log and
 * thredds.util.Debug to using Log4j).
 *
 * 2) Improved existing error handling and added additional error
 * handling where problems could fall through the cracks. Added some
 * catching and throwing of exceptions but also, for problems that aren't
 * fatal, added the inclusion in the resulting catalog of datasets with
 * the error message as its name.
 *
 * 3) Change how the CatGenTimerTask constructor is given the path to the
 * config files and the path to the resulting files so that resulting
 * catalogs are placed in the servlet directory space. Also, add ability
 * for servlet to serve the resulting catalogs.
 *
 * 4) Switch from using java.lang.String to using java.io.File for
 * handling file location information so that path seperators will be
 * correctly handled. Also, switch to java.net.URI rather than
 * java.io.File or java.lang.String where necessary to handle proper
 * URI/URL character encoding.
 *
 * 5) Add handling of requests when no path ("") is given, when the root
 * path ("/") is given, and when the admin path ("/admin") is given.
 *
 * 6) Fix the PUTting of catalogGenConfig files.
 *
 * 7) Start adding GDS DatasetSource capabilities.
 *
 * Revision 1.4  2003/07/03 20:38:28  edavis
 * When made DatasetSource an abstract class, changed resolve() to expand().
 *
 * Revision 1.3  2003/03/18 21:10:02  edavis
 * Updated the usage message.
 *
 * Revision 1.2  2003/03/04 23:02:23  edavis
 * Lots of changes made to support CatGenServlet.
 *
 *
 */
