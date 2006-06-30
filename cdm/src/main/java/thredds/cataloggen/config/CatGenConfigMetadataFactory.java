// $Id

package thredds.cataloggen.config;

import thredds.catalog.MetadataConverterIF;
import thredds.catalog.InvDataset;
import thredds.catalog.ServiceType;

import org.jdom.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.*;
import org.jdom.output.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.net.URI;
import java.io.IOException;

/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 * @author Ethan Davis
 * @version 1.0
 */

public class CatGenConfigMetadataFactory
        implements MetadataConverterIF
{
  //private static Log log = LogFactory.getLog( CatGenConfigMetadataFactory.class );
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatGenConfigMetadataFactory.class);

  private static boolean showParsedXML = false, debug = false;
  private static final Namespace CATALOG_GEN_CONFIG_NAMESPACE_0_5 =
          Namespace.getNamespace( CatalogGenConfig.CATALOG_GEN_CONFIG_NAMESPACE_URI_0_5 );
  //private DOMBuilder builder = new DOMBuilder();

  public CatGenConfigMetadataFactory()
  {
    log.debug( "CatGenConfigMetadataFactory(): .");
  }

 /**
  * Create an InvMetadata content object from an XML document at a named URL.
  * The content object is an ArrayList of CatalogGenConfig instances.
  *
  * @param dataset - the containing dataset
  * @param urlString - the URL where the XML doc is located.
  * @return
  * @throws java.net.MalformedURLException if given URL is malformed.
  * @throws java.io.IOException if problems reading from given URL
  */
  private Object readMetadataContentFromURL( InvDataset dataset, String urlString)
    throws java.net.MalformedURLException, java.io.IOException
  {
    // @todo This isn't used anywhere. Remove?
    Document doc;
    try
    {
      SAXBuilder builder = new SAXBuilder( true);
      doc = builder.build( urlString);
    } catch ( JDOMException e)
    {
      log.error( "CatGenConfigMetadataFactory parsing error= \n" + e.getMessage());
      throw new java.io.IOException( "CatGenConfigMetadataFactory parsing error= " + e.getMessage());
    }

    if (showParsedXML)
    {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** catalog/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    return( readMetadataContentJdom( dataset, doc.getRootElement()));
  }

  /**
   * Create an InvMetadata content object from an org.w3c.dom.Element.
   * The content object is an ArrayList of CatalogGenConfig instances.
   *
   * @param dataset - the containing dataset
   * @param mdataElement - the metadata element as an org.w3c.dom.Element
   * @return an object representing the metadata which is an ArrayList of CatalogGenConfig instances.
   */
  public Object readMetadataContent( InvDataset dataset, org.jdom.Element mdataElement )
  {
    log.debug( "readMetadataContent(): ." );

    // convert to JDOM element
    //Element mdataElement = builder.build( mdataDomElement );
    return readMetadataContentJdom( dataset, mdataElement );
  }

  public Object readMetadataContentFromURL( InvDataset dataset, URI uri ) throws IOException
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Serialize the InvMetadata content object to a org.w3c.dom.Element
   *
   * @param mdataJdomElement - add content to this org.w3c.dom.Element
   * @param contentObject - the content model
   */
  public void addMetadataContent( org.jdom.Element mdataJdomElement, Object contentObject )
  {
    // convert to JDOM element
    //Element mdataJdomElement = builder.build( mdataElement );

    ArrayList catGenConfigList = (ArrayList) contentObject;
    Iterator iter = catGenConfigList.iterator();
    while ( iter.hasNext())
    {
      CatalogGenConfig cgc = (CatalogGenConfig) iter.next();
      mdataJdomElement.addContent( createCatGenConfigElement( cgc));
    }
  }

  /** Validate the content object. */
  public boolean validateMetadataContent( Object contentObject, StringBuffer out)
  {
    boolean ok = true;
    ArrayList catGenConfigList = (ArrayList) contentObject;
    Iterator iter = catGenConfigList.iterator();
    while ( iter.hasNext()) {
      CatalogGenConfig catGenConf = (CatalogGenConfig) iter.next();
      ok &= catGenConf.validate( out);
    }
    return ok;

  }

  //---------------------------------------------------------------------------
  // Private methods to support readMetadataContent( InvDataset, org.w3c.dom.Element).

  /**
   * Given a metadata JDOM element, return metadata content object
   * (ArrayList of CatalogGenConfig).
   * <p/>
   * From the given metadata JDOM element, build (and return) an
   * ArrayList of CatalogGenConfig instances.
   */
  private Object readMetadataContentJdom( InvDataset dataset, Element mdataElement )
  {
    Namespace catGenConfigNamespace = null;
    ArrayList catGenConfigList = new ArrayList();
    // Get the "catalogGenConfig" children elements with
    // CatalogGenConfig namespace first and then with THREDDS namespace.
   Iterator iter = mdataElement.getChildren( "catalogGenConfig", CATALOG_GEN_CONFIG_NAMESPACE_0_5 ).iterator();
    if ( ! iter.hasNext())
      iter = mdataElement.getChildren( "catalogGenConfig", mdataElement.getNamespace() ).iterator();

    while ( iter.hasNext() )
    {
      Element catGenConfigElement = (Element) iter.next();
      if ( debug )
      {
        log.debug( "readMetadataContent=" + catGenConfigElement);
      }
      catGenConfigList.add( readCatGenConfigElement( dataset, catGenConfigElement ) );
    }
    return ( catGenConfigList );
  }

  /** Return a CatalogGenConfig when given a 'catalogGenConfig' JDOM element. */
  private CatalogGenConfig readCatGenConfigElement(
    InvDataset parentDataset, Element catGenConfElement)
  {

    String type = catGenConfElement.getAttributeValue("type");

    CatalogGenConfig catGenConf = new CatalogGenConfig( parentDataset, type);

    // get any datasetSource elements
    java.util.List list = catGenConfElement.getChildren( "datasetSource", catGenConfElement.getNamespace() );
    for (int i=0; i< list.size(); i++)
    {
      Element dsSourceElement = (Element) list.get(i);

      catGenConf.setDatasetSource( readDatasetSourceElement( parentDataset,
                                                             dsSourceElement));
    }

    // @todo Start only allowing datasetSource elements in catalogGenConfig elements.
//    // get any datasetNamer elements
//    list = catGenConfElement.getChildren( "datasetNamer", catGenConfElement.getNamespace() );
//    for (int i=0; i< list.size(); i++)
//    {
//      Element dsNamerElement = (Element) list.get(i);
//
//      catGenConf.addDatasetNamer( readDatasetNamerElement( parentDataset,
//                                                           dsNamerElement));
//    }

    return( catGenConf);
  }

  /** Return a DatasetSource when given a 'datasetSource' JDOM element. */
  private DatasetSource readDatasetSourceElement( InvDataset parentDataset,
                                                  Element dsSourceElement)
  {
    String name = dsSourceElement.getAttributeValue( "name");
    String type = dsSourceElement.getAttributeValue( "type");
    String structure = dsSourceElement.getAttributeValue( "structure");
    String accessPoint = dsSourceElement.getAttributeValue( "accessPoint");
    String createCatalogRefs = dsSourceElement.getAttributeValue( "createCatalogRefs");

    // get the resultService element
    Element resultServiceElement = dsSourceElement.getChild( "resultService", dsSourceElement.getNamespace() );
    ResultService resultService = readResultServiceElement( parentDataset,
                                                         resultServiceElement);

    DatasetSource dsSource = DatasetSource.newDatasetSource( name,
                                                    DatasetSourceType.getType( type),
                                                    DatasetSourceStructure.getStructure( structure),
                                                    accessPoint, resultService);
    if ( createCatalogRefs != null)
    {
      dsSource.setCreateCatalogRefs( Boolean.valueOf( createCatalogRefs).booleanValue());
    }

    // get any datasetNamer elements
    java.util.List list = dsSourceElement.getChildren( "datasetNamer", dsSourceElement.getNamespace() );
    for (int i=0; i< list.size(); i++)
    {
      Element dsNamerElement = (Element) list.get(i);

      dsSource.addDatasetNamer( readDatasetNamerElement( parentDataset,
                                                         dsNamerElement));
    }

    // get any datasetFilter elements
    list = dsSourceElement.getChildren( "datasetFilter", dsSourceElement.getNamespace() );
    for (int i=0; i< list.size(); i++)
    {
      Element dsFilterElement = (Element) list.get(i);

      dsSource.addDatasetFilter( readDatasetFilterElement( dsSource,
                                                           dsFilterElement));
    }

    return( dsSource);
  }

  /** Return a DatasetNamer when given a 'datasetNamer' JDOM element. */
  private DatasetNamer readDatasetNamerElement( InvDataset parentDataset,
                                                 Element dsNamerElement)
  {
    String name = dsNamerElement.getAttributeValue( "name");
    String addLevel = dsNamerElement.getAttributeValue( "addLevel");
    String type = dsNamerElement.getAttributeValue( "type");
    String matchPattern = dsNamerElement.getAttributeValue( "matchPattern");
    String substitutePattern = dsNamerElement.getAttributeValue( "substitutePattern");
    String attribContainer = dsNamerElement.getAttributeValue( "attribContainer");
    String attribName = dsNamerElement.getAttributeValue( "attribName");

    DatasetNamer dsNamer = new DatasetNamer( parentDataset,
                                             name, addLevel, type,
                                             matchPattern, substitutePattern,
                                             attribContainer, attribName);

    return( dsNamer);
  }

  /** Return a DatasetFilter when given a 'datasetFilter' JDOM element. */
  private DatasetFilter readDatasetFilterElement( DatasetSource parentDatasetSource,
                                                  Element dsFilterElement)
  {
    String name = dsFilterElement.getAttributeValue( "name");
    String type = dsFilterElement.getAttributeValue( "type");
    String matchPattern = dsFilterElement.getAttributeValue( "matchPattern");

    DatasetFilter dsFilter = new DatasetFilter( parentDatasetSource,
                                                name, DatasetFilter.Type.getType( type), matchPattern);
    String matchPatternTarget = dsFilterElement.getAttributeValue( "matchPatternTarget");
    dsFilter.setMatchPatternTarget( matchPatternTarget);

    if ( dsFilterElement.getAttributeValue( "applyToCollectionDatasets") != null)
    {
      boolean applyToCollectionDatasets = Boolean.valueOf( dsFilterElement.getAttributeValue( "applyToCollectionDatasets")).booleanValue();
      dsFilter.setApplyToCollectionDatasets( applyToCollectionDatasets);
    }
    if ( dsFilterElement.getAttributeValue( "applyToAtomicDatasets") != null)
    {
      boolean applyToAtomicDatasets = Boolean.valueOf( dsFilterElement.getAttributeValue( "applyToAtomicDatasets")).booleanValue();
      dsFilter.setApplyToAtomicDatasets( applyToAtomicDatasets);
    }
    if ( dsFilterElement.getAttributeValue( "rejectMatchingDatasets") != null)
    {
      boolean rejectMatchingDatasets = Boolean.valueOf( dsFilterElement.getAttributeValue( "rejectMatchingDatasets")).booleanValue();
      dsFilter.setRejectMatchingDatasets( rejectMatchingDatasets);
    }

    return( dsFilter);
  }

  /** Return a ResultService when given a 'resultService' JDOM element. */
  private ResultService readResultServiceElement( InvDataset parentDataset,
                                                  Element resultServiceElement)
  {
    String name = resultServiceElement.getAttributeValue( "name");
    String serviceType = resultServiceElement.getAttributeValue( "serviceType");
    String base = resultServiceElement.getAttributeValue( "base");
    String suffix = resultServiceElement.getAttributeValue( "suffix");
    String accessPointHeader =
      resultServiceElement.getAttributeValue( "accessPointHeader");

    return( new ResultService( name, ServiceType.getType( serviceType), base, suffix,
                               accessPointHeader));
  }

  //---------------------------------------------------------------------------
  // Private methods to support addMetadataContent()

  /** Create a 'catalogGenConfig' JDOM element */
  private org.jdom.Element createCatGenConfigElement( CatalogGenConfig cgc)
  {
    // @todo Need to deal with the 0.6 and 1.0 namespaces.
    Element cgcElem = new Element("catalogGenConfig", CATALOG_GEN_CONFIG_NAMESPACE_0_5);
    if ( cgc != null)
    {
      if ( cgc.getType() != null)
      {
        cgcElem.setAttribute( "type", cgc.getType().toString());
      }

      // Add 'datasetSource' element
      DatasetSource dsSource = cgc.getDatasetSource();
      cgcElem.addContent( createDatasetSourceElement( dsSource));

    }

    return( cgcElem);
  }

  /** Create a 'DatasetSource' JDOM element */
  private org.jdom.Element createDatasetSourceElement( DatasetSource dsSource)
  {
    Element dssElem = new Element("datasetSource", CATALOG_GEN_CONFIG_NAMESPACE_0_5);
    if ( dsSource != null)
    {
      // Add 'name' attribute.
      if ( dsSource.getName() != null)
      {
        dssElem.setAttribute( "name", dsSource.getName());
      }

      // Add 'type' attribute.
      if ( dsSource.getType() != null)
      {
        dssElem.setAttribute( "type", dsSource.getType().toString());
      }

      // Add 'structure' attribute.
      if ( dsSource.getStructure() != null)
      {
        dssElem.setAttribute( "structure", dsSource.getStructure().toString());
      }

      // Add 'accessPoint' attribute.
      if ( dsSource.getAccessPoint() != null)
      {
        dssElem.setAttribute( "accessPoint", dsSource.getAccessPoint());
      }

      // Add 'createCatalogRefs' attribute.
      dssElem.setAttribute( "createCatalogRefs", Boolean.toString(  dsSource.isCreateCatalogRefs()));

      // Add 'resultService' element
      ResultService rs = dsSource.getResultService();
      dssElem.addContent( createResultServiceElement( rs));

      // Add 'datasetNamer' elements
      java.util.List list = dsSource.getDatasetNamerList();
      for ( int j=0; j < list.size(); j++)
      {
        DatasetNamer dsNamer = (DatasetNamer) list.get(j);
        dssElem.addContent( createDatasetNamerElement( dsNamer));
      }

      // Add 'datasetFilter' elements
      list = dsSource.getDatasetFilterList();
      for ( int j=0; j < list.size(); j++)
      {
        DatasetFilter dsFilter = (DatasetFilter) list.get(j);
        dssElem.addContent( createDatasetFilterElement( dsFilter));
      }
    }

    return( dssElem);
  }

  /** Create a 'DatasetNamer' JDOM element */
  private org.jdom.Element createDatasetNamerElement( DatasetNamer dsNamer)
  {
    Element dsnElem = new Element("datasetNamer", CATALOG_GEN_CONFIG_NAMESPACE_0_5);
    if ( dsNamer != null)
    {
      // Add 'name' attribute.
      if ( dsNamer.getName() != null)
      {
        dsnElem.setAttribute( "name", dsNamer.getName());
      }

      // Add 'addLevel' attribute.
      dsnElem.setAttribute( "addLevel", Boolean.toString( dsNamer.getAddLevel()));

      // Add 'type' attribute.
      if ( dsNamer.getType() != null)
      {
        dsnElem.setAttribute( "type", dsNamer.getType().toString());
      }

      // Add 'matchPattern' attribute.
      if ( dsNamer.getMatchPattern() != null)
      {
        dsnElem.setAttribute( "matchPattern", dsNamer.getMatchPattern());
      }

      // Add 'subsitutePattern' attribute.
      if ( dsNamer.getSubstitutePattern() != null)
      {
        dsnElem.setAttribute( "substitutePattern", dsNamer.getSubstitutePattern());
      }

      // Add 'attribContainer' attribute.
      if ( dsNamer.getAttribContainer() != null)
      {
        dsnElem.setAttribute( "attribContainer", dsNamer.getAttribContainer());
      }

      // Add 'attribName' attribute.
      if ( dsNamer.getAttribName() != null)
      {
        dsnElem.setAttribute( "attribName", dsNamer.getAttribName());
      }

    }

    return( dsnElem);
  }

  /** Create a 'DatasetFilter' JDOM element */
  private org.jdom.Element createDatasetFilterElement( DatasetFilter dsFilter)
  {
    Element dsfElem = new Element("datasetFilter", CATALOG_GEN_CONFIG_NAMESPACE_0_5);
    if ( dsFilter != null)
    {
      // Add 'name' attribute.
      if ( dsFilter.getName() != null)
      {
        dsfElem.setAttribute( "name", dsFilter.getName());
      }

      // Add 'type' attribute.
      if ( dsFilter.getType() != null)
      {
        dsfElem.setAttribute( "type", dsFilter.getType().toString());
      }

      // Add 'matchPattern' attribute.
      if ( dsFilter.getMatchPattern() != null)
      {
        dsfElem.setAttribute( "matchPattern", dsFilter.getMatchPattern());
      }

      // Add 'matchPatternTarget' attribute.
      if ( dsFilter.getMatchPatternTarget() != null)
      {
        dsfElem.setAttribute( "matchPatternTarget", dsFilter.getMatchPatternTarget());
      }

      // Add 'applyToCollectionDatasets' attribute.
      dsfElem.setAttribute( "applyToCollectionDatasets", String.valueOf( dsFilter.isApplyToCollectionDatasets()));

      // Add 'applyToAtomicDatasets' attribute.
      dsfElem.setAttribute( "applyToAtomicDatasets", String.valueOf( dsFilter.isApplyToAtomicDatasets()));

      // Add 'rejectMatchingDatasets' attribute.
      dsfElem.setAttribute( "rejectMatchingDatasets", String.valueOf( dsFilter.isRejectMatchingDatasets()));

    }

    return( dsfElem);
  }

  /** Create a 'ResultService' JDOM element */
  private org.jdom.Element createResultServiceElement( ResultService resultService)
  {
    Element rsElem = new Element("resultService", CATALOG_GEN_CONFIG_NAMESPACE_0_5);
    if ( resultService != null)
    {
      // Add 'name' attribute.
      if ( resultService.getName() != null)
      {
        rsElem.setAttribute( "name", resultService.getName());
      }

      // Add 'serviceType' attribute.
      if ( resultService.getServiceType() != null)
      {
        rsElem.setAttribute( "serviceType", resultService.getServiceType().toString());
      }

      // Add 'base' attribute.
      if ( resultService.getBase() != null)
      {
        rsElem.setAttribute( "base", resultService.getBase());
      }

      // Add 'suffix' attribute.
      if ( resultService.getSuffix() != null)
      {
        rsElem.setAttribute( "suffix", resultService.getSuffix());
      }

      // Add 'accessPointHeader' attribute.
      if ( resultService.getAccessPointHeader() != null)
      {
        rsElem.setAttribute( "accessPointHeader", resultService.getAccessPointHeader());
      }
    }

    return( rsElem);
  }
}

/*
 * $Log: CatGenConfigMetadataFactory.java,v $
 * Revision 1.16  2006/01/20 02:08:22  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.15  2006/01/04 00:16:35  caron
 * jdom 1.0
 * use nj22.12, visadNoDods
 *
 * Revision 1.14  2005/11/18 23:51:03  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.13  2005/04/05 22:37:01  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.12  2005/01/20 21:54:37  edavis
 * Change some System.out.println() calls to log4j calls.
 *
 * Revision 1.11  2005/01/14 21:24:33  edavis
 * Add handling of datasetSource@createCatalogRefs to DTD/XSD and
 * CatGenConfigMetadataFactory and testing.
 *
 * Revision 1.10  2005/01/12 22:51:40  edavis
 * 1) Remove all empty collection datasets before returning the catalog from
 * DatasetSource.expand(). 2)Improve how a CatGen config document and the
 * generated datasets are merged, mainly involved the dropping of the top-level
 * generated dataset. 3) Provide for filtering by a group of DatasetFilters: add
 * reject capability, and add the acceptDatasetByFilterGroup() static method.
 *
 * Revision 1.9  2004/12/29 21:53:21  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.8  2004/12/22 22:06:17  edavis
 * Fix so that default values are handled properly for the DatasetFilter attributes applyToCollectionDataset, applyToAtomicDataset, and invertMatchMeaning.
 *
 * Revision 1.7  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.6  2004/11/30 22:45:35  edavis
 * Update for changes in CatGenConfig 0.5 DTD and XSD.
 *
 * Revision 1.5  2004/09/24 03:26:31  caron
 * merge nj22
 *
 * Revision 1.4  2004/06/03 20:17:47  edavis
 * Modify to handle both 0.6 and 1.0 catalogs. Mostly this means dealing with
 * both namespace and metadataType differences in the CatalogGenConfig
 * metadata.
 *
 * Revision 1.3  2004/05/11 20:38:45  edavis
 * Update for changes to thredds.catalog object model (still InvCat 0.6).
 * Start adding some logging statements.
 *
 * Revision 1.2  2003/07/03 20:02:27  edavis
 * Made DatasetSource an abstract class and each type of DatasetSource
 * its own subclass.
 *
 * Revision 1.1.1.1  2002/12/11 22:27:54  edavis
 * CatGen into reorged thredds CVS repository.
 *
 */