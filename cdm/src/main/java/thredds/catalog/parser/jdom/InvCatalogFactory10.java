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

import thredds.inventory.FeatureCollectionConfig;
import thredds.util.PathAliasReplacement;
import thredds.catalog.*;
import thredds.crawlabledataset.*;
import thredds.crawlabledataset.sorter.LexigraphicByNameSorter;
import thredds.crawlabledataset.filter.*;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.cataloggen.DatasetEnhancer;
import thredds.cataloggen.CatalogRefExpander;
import thredds.cataloggen.datasetenhancer.RegExpAndDurationTimeCoverageEnhancer;
import thredds.cataloggen.inserter.SimpleLatestProxyDsHandler;
import thredds.cataloggen.inserter.LatestCompleteProxyDsHandler;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ucar.unidata.util.Format;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

/**
 * Inventory Catalog parser, version 1.0.
 * Reads InvCatalog.xml files, constructs object representation.
 *
 * @author John Caron
 */

public class InvCatalogFactory10 implements InvCatalogConvertIF, MetadataConverterIF  {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InvCatalogFactory10.class);
  static private final Namespace defNS = Namespace.getNamespace(XMLEntityResolver.CATALOG_NAMESPACE_10);
  static private final Namespace xlinkNS = Namespace.getNamespace("xlink", XMLEntityResolver.XLINK_NAMESPACE);
  static private final Namespace ncmlNS = Namespace.getNamespace("ncml", XMLEntityResolver.NJ22_NAMESPACE);

  static private boolean useBytesForDataSize = false;
  static public void useBytesForDataSize( boolean b) {
    useBytesForDataSize = b;
  }

  private InvCatalogFactory factory = null;
  // private DOMBuilder domBuilder = new DOMBuilder();

  private String version = "1.0.1";
  private boolean debugMetadataRead = false;

  /* public InvCatalogImpl parseXML( InvCatalogFactory fac, org.w3c.dom.Document domDoc, URI uri) {
    this.factory = fac;

    // convert to JDOM document
    Document doc = domBuilder.build(domDoc);

    if (InvCatalogFactory.showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** catalog/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
    }

    InvCatalogImpl catalog = readCatalog( doc.getRootElement(), uri);

    return catalog;
  } */

  private List<PathAliasReplacement> dataRootLocAliasExpanders = Collections.emptyList();
  public void setDataRootLocationAliasExpanders( List<PathAliasReplacement> dataRootLocAliasExpanders)
  {
    if ( dataRootLocAliasExpanders == null )
      this.dataRootLocAliasExpanders = Collections.emptyList();
    else
      this.dataRootLocAliasExpanders = new ArrayList<PathAliasReplacement>( dataRootLocAliasExpanders);
  }
  public List<PathAliasReplacement> getDataRootLocationAliasExpanders()
  {
    return Collections.unmodifiableList( this.dataRootLocAliasExpanders );
  }
  private String expandDataRootLocationAlias( String location )
  {
    for ( PathAliasReplacement par : this.dataRootLocAliasExpanders )
    {
      if ( par.containsPathAlias( location ))
        return par.replacePathAlias( location );
    }
    return location;
  }

  public InvCatalogImpl parseXML( InvCatalogFactory fac, org.jdom.Document jdomDoc, URI uri) {
    this.factory = fac;
    return readCatalog( jdomDoc.getRootElement(), uri);
  }


  private Map<MetadataType, MetadataConverterIF> metadataHash = new HashMap<MetadataType, MetadataConverterIF>(10);
  public void registerMetadataConverter(MetadataType type, MetadataConverterIF converter) {
    metadataHash.put(type, converter);
  }

  public void setVersion( String version) { this.version = version; }

  /////////////////////////////////////////////////////////////////////////////

  protected InvAccessImpl readAccess( InvDatasetImpl dataset, Element accessElem) {
    String urlPath = accessElem.getAttributeValue("urlPath");
    String serviceName = accessElem.getAttributeValue("serviceName");
    String dataFormat = accessElem.getAttributeValue("dataFormat");

    return new InvAccessImpl( dataset, urlPath, serviceName, null, dataFormat, readDataSize( accessElem));
  }

  protected InvCatalogImpl readCatalog( Element catalogElem, URI docBaseURI ) {
    String name = catalogElem.getAttributeValue("name");
    String catSpecifiedBaseURL = catalogElem.getAttributeValue("base");
    String expires = catalogElem.getAttributeValue("expires");
    String version = catalogElem.getAttributeValue("version");

    URI baseURI = docBaseURI;
    if ( catSpecifiedBaseURL != null )
    {
      try
      {
        baseURI = new URI( catSpecifiedBaseURL );
      }
      catch ( URISyntaxException e )
      {
        logger.debug( "readCatalog(): bad catalog specified base URI <" + catSpecifiedBaseURL + ">: " + e.getMessage(), e);
        baseURI = docBaseURI;
      }
    }

    InvCatalogImpl catalog = new InvCatalogImpl( name, version, makeDateType(expires, null, null), baseURI );

    // read top-level services
    java.util.List<Element> sList = catalogElem.getChildren("service", defNS);
    for ( Element e : sList ) {
      InvService s = readService( e, baseURI );
      catalog.addService(s);
    }

    // read top-level properties
    java.util.List<Element> pList = catalogElem.getChildren("property", defNS);
    for ( Element e : pList ) {
      InvProperty s = readProperty( e);
      catalog.addProperty(s);
    }

    // read top-level dataroots
    java.util.List<Element> rootList = catalogElem.getChildren("datasetRoot", defNS);
    for ( Element e : rootList ) {
      DataRootConfig root = readDatasetRoot(e);
      catalog.addDatasetRoot( root);
    }

     // look for top-level dataset and catalogRefs elements (keep them in order)
    java.util.List<Element> allChildren = catalogElem.getChildren();
    for ( Element e : allChildren ) {
      if (e.getName().equals("dataset")) {
        catalog.addDataset( readDataset( catalog, null, e, baseURI ));
      } else if (e.getName().equals("featureCollection")) {
        catalog.addDataset( readFeatureCollection( catalog, null, e, baseURI ));
      } else if (e.getName().equals("datasetFmrc")) {
        catalog.addDataset( readDatasetFmrc( catalog, null, e, baseURI ));
      } else if (e.getName().equals("datasetScan")) {
        catalog.addDataset( readDatasetScan( catalog, null, e, baseURI ));
      } else if (e.getName().equals("catalogRef")) {
        catalog.addDataset( readCatalogRef( catalog, null, e, baseURI ));
      }
    }

    return catalog;
  }

  protected InvCatalogRef readCatalogRef( InvCatalogImpl cat, InvDatasetImpl parent, Element catRefElem, URI baseURI) {
    String title = catRefElem.getAttributeValue("title", xlinkNS);
    if (title == null) title = catRefElem.getAttributeValue("name");
    String href = catRefElem.getAttributeValue("href", xlinkNS);

    InvCatalogRef catRef = new InvCatalogRef( parent, title, href);
    readDatasetInfo( cat, catRef, catRefElem, baseURI);
    return catRef;
  }

  protected ThreddsMetadata.Contributor readContributor(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Contributor( elem.getText(), elem.getAttributeValue("role"));
  }

  protected ThreddsMetadata.Vocab readControlledVocabulary(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Vocab( elem.getText(), elem.getAttributeValue("vocabulary"));
  }

    // read a dataset element
  protected InvDatasetImpl readDataset( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {

      // deal with aliases
    String name = dsElem.getAttributeValue("name");
    String alias = dsElem.getAttributeValue("alias");
    if (alias != null) {
      InvDatasetImpl ds = (InvDatasetImpl) catalog.findDatasetByID( alias);
      if (ds == null)
        factory.appendErr(" ** Parse error: dataset named "+name+" has illegal alias = "+alias+"\n");
      return new InvDatasetImplProxy(name, ds);
    }

    InvDatasetImpl dataset = new InvDatasetImpl( parent, name);
    readDatasetInfo( catalog, dataset, dsElem, base);

    if (InvCatalogFactory.debugXML) System.out.println (" Dataset added: "+ dataset.dump());
    return dataset;
  }

  protected void readDatasetInfo( InvCatalogImpl catalog, InvDatasetImpl dataset, Element dsElem, URI base) {
     // read attributes
    String authority = dsElem.getAttributeValue("authority");
    String collectionTypeName = dsElem.getAttributeValue("collectionType");
    String dataTypeName = dsElem.getAttributeValue("dataType");
    String harvest = dsElem.getAttributeValue("harvest");
    String id = dsElem.getAttributeValue("ID");
    String serviceName = dsElem.getAttributeValue("serviceName");
    String urlPath = dsElem.getAttributeValue("urlPath");
    String restrictAccess = dsElem.getAttributeValue("restrictAccess");

    FeatureType dataType = null;
    if (dataTypeName != null) {
      dataType = FeatureType.getType( dataTypeName.toUpperCase());
      if (dataType == null) {
        factory.appendWarning(" ** warning: non-standard data type = "+dataTypeName+"\n");
      }
    }

    if (dataType != null)
      dataset.setDataType( dataType);
    if (serviceName != null)
      dataset.setServiceName( serviceName);
     if (urlPath != null)
      dataset.setUrlPath( urlPath);

    if (authority != null) dataset.setAuthority( authority);
    if (id != null) dataset.setID( id);
    if (harvest != null) dataset.setHarvest( harvest.equalsIgnoreCase("true"));
    if (restrictAccess != null) dataset.setResourceControl( restrictAccess);

    if (collectionTypeName != null) {
      CollectionType collectionType = CollectionType.findType( collectionTypeName);
      if (collectionType == null) {
        collectionType = CollectionType.getType( collectionTypeName );
        factory.appendWarning(" ** warning: non-standard collection type = "+collectionTypeName+"\n");
      }
      dataset.setCollectionType( collectionType);
    }

    catalog.addDatasetByID( dataset); // need to do immed for alias processing

        // look for services
    java.util.List<Element> serviceList = dsElem.getChildren("service", defNS);
    for ( Element curElem : serviceList )
    {
      InvService s = readService( curElem, base);
      dataset.addService( s);
    }

    // look for direct thredds metadata (not inherited)
    ThreddsMetadata tmg = dataset.getLocalMetadata();
    readThreddsMetadata( catalog, dataset, dsElem, tmg);

      // look for access elements
    java.util.List<Element> aList = dsElem.getChildren("access", defNS);
    for ( Element e : aList ) {
      InvAccessImpl a = readAccess( dataset, e);
      dataset.addAccess( a);
     }

    // look for ncml
    Element ncmlElem = dsElem.getChild( "netcdf", ncmlNS );
    if (ncmlElem != null) {
      ncmlElem.detach();
      dataset.setNcmlElement( ncmlElem);
      // System.out.println(" found ncml= "+ncmlElem);
    }

     // look for nested dataset and catalogRefs elements (keep them in order)
    java.util.List<Element> allChildren = dsElem.getChildren();
    for ( Element e : allChildren) {
      if (e.getName().equals("dataset")) {
        InvDatasetImpl ds = readDataset( catalog, dataset, e, base);
        if (ds != null)
          dataset.addDataset( ds);
      } else if (e.getName().equals("catalogRef")) {
        InvDatasetImpl ds = readCatalogRef( catalog, dataset, e, base);
        dataset.addDataset( ds);
      } else if (e.getName().equals("datasetScan")) {
        dataset.addDataset( readDatasetScan( catalog, dataset, e, base));
      } else if (e.getName().equals("datasetFmrc")) {
        dataset.addDataset( readDatasetFmrc( catalog, dataset, e, base));
      } else if (e.getName().equals("featureCollection")) {
        dataset.addDataset( readFeatureCollection( catalog, dataset, e, base ));
      }
    }
  }

  protected InvDatasetImpl readFeatureCollection( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {
    String name = dsElem.getAttributeValue("name");
    String path = dsElem.getAttributeValue("path");
    String featureType = dsElem.getAttributeValue("featureType");

    Element collElem = dsElem.getChild( "collection", defNS );
    if (collElem == null) {
      logger.error( "featureCollection "+name+" must have a <collection> element." );
      return null;
    }

    String specName = collElem.getAttributeValue("name");
    String spec = collElem.getAttributeValue("spec");
    String olderThan = collElem.getAttributeValue("olderThan");
    String recheckAfter = collElem.getAttributeValue("recheckAfter");
    if (recheckAfter == null)
       recheckAfter = collElem.getAttributeValue("recheckEvery"); // old name
    if (spec == null) {
      logger.error( "featureCollection "+name+" must have a spec attribute." );
      return null;
    }

    String collName = (specName != null) ? specName : name;
    FeatureCollectionConfig.Config config = new FeatureCollectionConfig.Config(collName, spec, olderThan, recheckAfter);

    Element updateElem = dsElem.getChild( "update", defNS );
    if (updateElem != null) {
      String startup = updateElem.getAttributeValue("startup");
      String rescan = updateElem.getAttributeValue("rescan");
      String trigger = updateElem.getAttributeValue("trigger");
      config.updateConfig = new FeatureCollectionConfig.UpdateConfig(startup, rescan, trigger);
    }

    Element protoElem = dsElem.getChild( "protoDataset", defNS );
    if (protoElem != null) {
      String choice = protoElem.getAttributeValue("choice");
      String change = protoElem.getAttributeValue("change");
      Element ncmlElem = protoElem.getChild( "netcdf", ncmlNS );
      config.protoConfig = new FeatureCollectionConfig.ProtoConfig(choice, change, ncmlElem);

    }

    Element fmrcElem = dsElem.getChild( "fmrcConfig", defNS );
    if (fmrcElem != null) {
      String regularize = fmrcElem.getAttributeValue("regularize");
      config.fmrcConfig = new FeatureCollectionConfig.FmrcConfig(regularize);

      String datasetTypes = fmrcElem.getAttributeValue("datasetTypes");
      if (null != datasetTypes)
        config.fmrcConfig.addDatasetType(datasetTypes);

      List<Element> bestElems = fmrcElem.getChildren( "dataset", defNS );
      for (Element best : bestElems) {
        String bestName = best.getAttributeValue("name");
        String offs = best.getAttributeValue("offsetsGreaterEqual");
        double off = Double.parseDouble(offs);
        config.fmrcConfig.addBestDataset(bestName, off);
      }

    }

    InvDatasetFeatureCollection ds = new InvDatasetFeatureCollection( parent, name, path, featureType, config);

    readDatasetInfo( catalog, ds, dsElem, base);
    return ds;
  }

  protected InvDatasetImpl readDatasetFmrc( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {
    String name = dsElem.getAttributeValue("name");
    String path = dsElem.getAttributeValue("path");
    String runsOnly = dsElem.getAttributeValue("runsOnly");
    InvDatasetFmrc dsFmrc = new InvDatasetFmrc( parent, name, path, "true".equals(runsOnly));

    Element fmrcElem = dsElem.getChild( "fmrcInventory", defNS );
    if (fmrcElem != null) {
      String location = expandDataRootLocationAlias( fmrcElem.getAttributeValue("location"));
      String def = fmrcElem.getAttributeValue("fmrcDefinition");
      String suffix = fmrcElem.getAttributeValue("suffix");
      String olderThan = fmrcElem.getAttributeValue("olderThan");
      String subdirs = fmrcElem.getAttributeValue("subdirs");
      dsFmrc.setFmrcInventoryParams( location, def, suffix, olderThan, subdirs);
    }

    readDatasetInfo( catalog, dsFmrc, dsElem, base);
    return dsFmrc;
  }
      // read a dataset scan element
  protected InvDatasetScan readDatasetScan( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {
    InvDatasetScan datasetScan = null;

    if ( dsElem.getAttributeValue( "dirLocation" ) == null )
    {
      if ( dsElem.getAttributeValue( "location" ) == null )
      {
        logger.error( "readDatasetScan(): datasetScan has neither a \"location\" nor a \"dirLocation\" attribute." );
        datasetScan = null;
      }
      else
      {
        return readDatasetScanNew( catalog, parent, dsElem, base );
      }
    }
    else
    {
      String name = dsElem.getAttributeValue( "name" );
      factory.appendWarning("**Warning: Dataset "+name+" using old form of DatasetScan (dirLocation instead of location)\n");

      String path = dsElem.getAttributeValue( "path" );

      String scanDir = expandDataRootLocationAlias( dsElem.getAttributeValue( "dirLocation" ));
      String filter = dsElem.getAttributeValue( "filter" );
      String addDatasetSizeString = dsElem.getAttributeValue( "addDatasetSize" );
      String addLatest = dsElem.getAttributeValue( "addLatest" );
      String sortOrderIncreasingString = dsElem.getAttributeValue( "sortOrderIncreasing" );
      boolean sortOrderIncreasing = false;
      if ( sortOrderIncreasingString != null )
        if ( sortOrderIncreasingString.equalsIgnoreCase( "true" ) )
          sortOrderIncreasing = true;
      boolean addDatasetSize = false;
      if ( addDatasetSizeString != null )
        if ( addDatasetSizeString.equalsIgnoreCase( "true" ) )
          addDatasetSize = true;

      if ( path != null )
      {
        if ( path.charAt( 0 ) == '/' ) path = path.substring( 1 );
        int last = path.length() - 1;
        if ( path.charAt( last ) == '/' ) path = path.substring( 0, last );
      }

      if ( scanDir != null )
      {
        int last = scanDir.length() - 1;
        if ( scanDir.charAt( last ) != '/' ) scanDir = scanDir + '/';
      }

      Element atcElem = dsElem.getChild( "addTimeCoverage", defNS );
      String dsNameMatchPattern = null;
      String startTimeSubstitutionPattern = null;
      String duration = null;
      if ( atcElem != null )
      {
        dsNameMatchPattern = atcElem.getAttributeValue( "datasetNameMatchPattern" );
        startTimeSubstitutionPattern = atcElem.getAttributeValue( "startTimeSubstitutionPattern" );
        duration = atcElem.getAttributeValue( "duration" );
      }

      try
      {
        datasetScan = new InvDatasetScan( catalog, parent, name, path, scanDir, filter, addDatasetSize, addLatest, sortOrderIncreasing,
                                          dsNameMatchPattern, startTimeSubstitutionPattern, duration );
        readDatasetInfo( catalog, datasetScan, dsElem, base );
        if ( InvCatalogFactory.debugXML ) System.out.println( " Dataset added: " + datasetScan.dump() );

      }
      catch ( Exception e )
      {
        logger.error( "Reading DatasetScan", e );
        datasetScan = null;
      }
    }

    return datasetScan;
  }

  protected InvDatasetScan readDatasetScanNew( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base )
  {
    String name = dsElem.getAttributeValue( "name" );
    String path = dsElem.getAttributeValue( "path" );

    String scanDir = expandDataRootLocationAlias( dsElem.getAttributeValue( "location" ));

    // Read datasetConfig element
    String configClassName = null;
    Object configObj = null;
    Element dsConfigElem = dsElem.getChild( "crawlableDatasetImpl", defNS );
    if ( dsConfigElem != null )
    {
      configClassName = dsConfigElem.getAttributeValue( "className");
      List children = dsConfigElem.getChildren();
      if ( children.size() == 1 )
      {
        configObj = children.get( 0);
      }
      else if ( children.size() != 0 )
      {
        logger.warn( "readDatasetScanNew(): content of datasetConfig element not a single element, using first element." );
        configObj = children.get( 0 );
      }
      else
      {
        logger.debug( "readDatasetScanNew(): datasetConfig element has no children.");
        configObj = null;
      }
    }

    // Read filter element
    Element filterElem = dsElem.getChild( "filter", defNS );
    CrawlableDatasetFilter filter = null;
    if ( filterElem != null)
      filter = readDatasetScanFilter( filterElem );

    // Read identifier element
    Element identifierElem = dsElem.getChild( "addID", defNS );
    CrawlableDatasetLabeler identifier = null;
    if ( identifierElem != null )
    {
      identifier = readDatasetScanIdentifier( identifierElem );
    }

    // Read namer element
    Element namerElem = dsElem.getChild( "namer", defNS );
    CrawlableDatasetLabeler namer = null;
    if ( namerElem != null )
    {
      namer = readDatasetScanNamer( namerElem );
    }

    // Read sort element
    Element sorterElem = dsElem.getChild( "sort", defNS );
    // By default, sort in decreasing lexigraphic order.
    CrawlableDatasetSorter sorter = new LexigraphicByNameSorter( false );
    if ( sorterElem != null )
    {
      sorter = readDatasetScanSorter( sorterElem );
    }

    // Read allProxies element (and addLatest element)
    Element addLatestElem = dsElem.getChild( "addLatest", defNS );
    Element addProxiesElem = dsElem.getChild( "addProxies", defNS );
    Map allProxyDsHandlers;
    if ( addLatestElem != null || addProxiesElem != null )
      allProxyDsHandlers = readDatasetScanAddProxies( addProxiesElem, addLatestElem, catalog );
    else
      allProxyDsHandlers = new HashMap();

    // Read addDatasetSize element.
    Element addDsSizeElem = dsElem.getChild( "addDatasetSize", defNS );
    boolean addDatasetSize = false;
    if ( addDsSizeElem != null )
      addDatasetSize = true;

    // Read addTimeCoverage element.
    List childEnhancerList = new ArrayList();
    Element addTimeCovElem = dsElem.getChild( "addTimeCoverage", defNS );
    if ( addTimeCovElem != null )
    {
      DatasetEnhancer addTimeCovEnhancer = readDatasetScanAddTimeCoverage( addTimeCovElem );
      if ( addTimeCovEnhancer != null )
        childEnhancerList.add( addTimeCovEnhancer );
    }

    // Read datasetEnhancerImpl elements (user defined implementations of DatasetEnhancer)
    List dsEnhancerElemList = dsElem.getChildren( "datasetEnhancerImpl", defNS );
    for ( Iterator it = dsEnhancerElemList.iterator(); it.hasNext(); )
    {
      Object o = readDatasetScanUserDefined( (Element) it.next(),
                                             DatasetEnhancer.class );
      if ( o != null )
        childEnhancerList.add( o );
    }

    // Read catalogRefExpander element
//    Element catRefExpanderElem = dsElem.getChild( "catalogRefExpander", defNS );
    CatalogRefExpander catalogRefExpander = null;
//    if ( catRefExpanderElem != null )
//    {
//      catalogRefExpander = readDatasetScanCatRefExpander( catRefExpanderElem );
//    }


    InvDatasetScan datasetScan = null;
    try
    {
      datasetScan = new InvDatasetScan( parent, name, path, scanDir,
                                        configClassName, configObj,
                                        filter, identifier, namer,
                                        addDatasetSize, sorter, allProxyDsHandlers,
                                        childEnhancerList,
                                        catalogRefExpander );
      readDatasetInfo( catalog, datasetScan, dsElem, base );
      if ( InvCatalogFactory.debugXML ) System.out.println( " Dataset added: " + datasetScan.dump() );

    }
    catch ( Exception e )
    {
      logger.error( "readDatasetScanNew(): failed to create DatasetScan", e );
      datasetScan = null;
    }

    return datasetScan;
  }

  CrawlableDatasetFilter readDatasetScanFilter( Element filterElem )
  {
    CrawlableDatasetFilter filter = null;  //lastModifiedLimit

    // Handle LastModifiedLimitFilter CrDsFilters.
    Attribute lastModLimitAtt = filterElem.getAttribute( "lastModifiedLimit");
    if ( lastModLimitAtt != null )
    {
      long lastModLimit;
      try
      {
        lastModLimit = lastModLimitAtt.getLongValue();
      }
      catch ( DataConversionException e )
      {
        String tmpMsg = "readDatasetScanFilter(): bad lastModifedLimit value <" + lastModLimitAtt.getValue() + ">, couldn't parse into long: " + e.getMessage();
        factory.appendErr( tmpMsg );
        logger.warn( tmpMsg );
        return null;
      }
      return new LastModifiedLimitFilter( lastModLimit);
    }

    // Handle LogicalFilterComposer CrDsFilters.
    String compType = filterElem.getAttributeValue( "logicalComp");
    if ( compType != null )
    {
      List filters = filterElem.getChildren( "filter", defNS );
      if ( compType.equalsIgnoreCase( "AND") )
      {
        if ( filters.size() != 2 )
        {
          String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for AND (2 expected).";
          factory.appendErr( tmpMsg );
          logger.warn( tmpMsg );
          return null;
        }
        filter = LogicalFilterComposer.getAndFilter(
                readDatasetScanFilter( (Element) filters.get( 0)),
                readDatasetScanFilter( (Element) filters.get( 1) ) );
      }
      else if ( compType.equalsIgnoreCase( "OR") )
      {
        if ( filters.size() != 2 )
        {
          String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for OR (2 expected).";
          factory.appendErr( tmpMsg );
          logger.warn( tmpMsg );
          return null;
        }
        filter = LogicalFilterComposer.getOrFilter(
                readDatasetScanFilter( (Element) filters.get( 0 ) ),
                readDatasetScanFilter( (Element) filters.get( 1 ) ) );
      }
      else if ( compType.equalsIgnoreCase( "NOT" ) )
      {
        if ( filters.size() != 1 )
        {
          String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for NOT (1 expected).";
          factory.appendErr( tmpMsg );
          logger.warn( tmpMsg );
          return null;
        }
        filter = LogicalFilterComposer.getNotFilter(
                readDatasetScanFilter( (Element) filters.get( 0 ) ) );
      }

      return filter;
    }

    // Handle user defined CrDsFilters.
    Element userDefElem = filterElem.getChild( "crawlableDatasetFilterImpl", defNS);
    if ( userDefElem != null )
    {
      filter = (CrawlableDatasetFilter) readDatasetScanUserDefined( userDefElem, CrawlableDatasetFilter.class );
    }
    
    // Handle MultiSelectorFilter and contained Selectors.
    else
    {
      List selectorList = new ArrayList();
      for ( Iterator it = filterElem.getChildren().iterator(); it.hasNext(); )
      {
        Element curElem = (Element) it.next();

        String regExpAttVal = curElem.getAttributeValue( "regExp");
        String wildcardAttVal = curElem.getAttributeValue( "wildcard");
        String lastModLimitAttVal = curElem.getAttributeValue( "lastModLimitInMillis");
        if ( regExpAttVal == null && wildcardAttVal == null && lastModLimitAttVal == null )
        {
          // If no regExp or wildcard attributes, skip this selector.
          logger.warn( "readDatasetScanFilter(): no regExp, wildcard, or lastModLimitInMillis attribute in filter child <" + curElem.getName() + ">." );
        }
        else
        {
          // Determine if applies to atomic datasets, default true.
          boolean atomic = true;
          String atomicAttVal = curElem.getAttributeValue( "atomic");
          if ( atomicAttVal != null )
          {
            // If not "true", set to false.
            if ( ! atomicAttVal.equalsIgnoreCase( "true"))
              atomic = false;
          }
          // Determine if applies to collection datasets, default false.
          boolean collection = false;
          String collectionAttVal = curElem.getAttributeValue( "collection");
          if ( collectionAttVal != null )
          {
            // If not "false", set to true.
            if ( ! collectionAttVal.equalsIgnoreCase( "false") )
              collection = true;
          }

          // Determine if include or exclude selectors.
          boolean includer = true;
          if ( curElem.getName().equals( "exclude") )
          {
            includer = false;
          }
          else if ( ! curElem.getName().equals( "include") )
          {
            logger.warn( "readDatasetScanFilter(): unhandled filter child <" + curElem.getName() + ">.");
            continue;
          }

          // Determine if regExp or wildcard
          if ( regExpAttVal != null )
          {
            selectorList.add( new MultiSelectorFilter.Selector( new RegExpMatchOnNameFilter( regExpAttVal), includer, atomic, collection ) );
          }
          else if ( wildcardAttVal != null )
          {
            selectorList.add( new MultiSelectorFilter.Selector( new WildcardMatchOnNameFilter( wildcardAttVal), includer, atomic, collection ) );
          }
          else if ( lastModLimitAttVal != null )
          {
            selectorList.add( new MultiSelectorFilter.Selector( new LastModifiedLimitFilter( Long.parseLong( lastModLimitAttVal)), includer, atomic, collection ) );
          }
        }
      }
      filter = new MultiSelectorFilter( selectorList );
    }

    return filter;
  }

  protected CrawlableDatasetLabeler readDatasetScanIdentifier( Element identifierElem )
  {
    CrawlableDatasetLabeler identifier = null;
    Element userDefElem = identifierElem.getChild( "crawlableDatasetLabelerImpl", defNS );
    if ( userDefElem != null )
    {
      identifier = (CrawlableDatasetLabeler) readDatasetScanUserDefined( userDefElem, CrawlableDatasetLabeler.class );
    }
    else
    {
      // Default is to add ID in standard way. Don't have alternates yet.
      return null;
    }

    return identifier;
  }

  protected CrawlableDatasetLabeler readDatasetScanNamer( Element namerElem )
  {
    CrawlableDatasetLabeler namer = null;
//    Element userDefElem = namerElem.getChild( "crawlableDatasetLabelerImpl", defNS );
//    if ( userDefElem != null )
//    {
//      namer = (CrawlableDatasetLabeler) readDatasetScanUserDefined( userDefElem, CrawlableDatasetLabeler.class );
//    }
//    else
//    {
    List labelerList = new ArrayList();
    for ( Iterator it = namerElem.getChildren().iterator(); it.hasNext(); )
    {
      Element curElem = (Element) it.next();
      CrawlableDatasetLabeler curLabeler;

      String regExp = curElem.getAttributeValue( "regExp");
      String replaceString = curElem.getAttributeValue( "replaceString");
      if ( curElem.getName().equals( "regExpOnName") )
      {
        curLabeler = new RegExpAndReplaceOnNameLabeler( regExp, replaceString );
      }
      else if ( curElem.getName().equals( "regExpOnPath" ) )
      {
        curLabeler = new RegExpAndReplaceOnPathLabeler( regExp, replaceString );
      }
      else
      {
        logger.warn( "readDatasetScanNamer(): unhandled namer child <" + curElem.getName() + ">." );
        continue;
      }
      labelerList.add( curLabeler );
    }
    namer = new MultiLabeler( labelerList );
//    }

    return namer;
  }

  protected CrawlableDatasetSorter readDatasetScanSorter( Element sorterElem )
  {
    CrawlableDatasetSorter sorter = null;
    Element userDefElem = sorterElem.getChild( "crawlableDatasetSorterImpl", defNS );
    if ( userDefElem != null )
    {
      sorter = (CrawlableDatasetSorter) readDatasetScanUserDefined( userDefElem, CrawlableDatasetSorter.class );
    }
    else
    {
      Element lexSortElem = sorterElem.getChild( "lexigraphicByName", defNS );
      if ( lexSortElem != null )
      {
        boolean increasing;
        String increasingString = lexSortElem.getAttributeValue( "increasing");
        if ( increasingString.equalsIgnoreCase( "true") )
          increasing = true;
        else
          increasing = false;
        sorter = new LexigraphicByNameSorter( increasing );
      }
    }

    return sorter;
  }

  protected Map readDatasetScanAddProxies( Element addProxiesElem, Element addLatestElem, InvCatalogImpl catalog )
  {
    Map allProxyDsHandlers = new HashMap();

    // Handle old "addLatest" elements.
    if ( addLatestElem != null )
    {
      // Check for simpleLatest element.
      Element simpleLatestElem = addLatestElem.getChild( "simpleLatest", defNS );
      // Get a SimpleLatestDsHandler, use default values if element is null.
      ProxyDatasetHandler pdh = readDatasetScanAddLatest( simpleLatestElem, catalog );
        if ( pdh != null )
          allProxyDsHandlers.put( pdh.getProxyDatasetName(), pdh );
    }

    // Handle all "addProxies" elements.
    if ( addProxiesElem != null )
    {
      for ( Iterator it = addProxiesElem.getChildren().iterator(); it.hasNext(); )
      {
        Element curChildElem = (Element) it.next();
        ProxyDatasetHandler curPdh;

        // Handle "simpleLatest" child elements.
        if ( curChildElem.getName().equals( "simpleLatest")
             && curChildElem.getNamespace().equals( defNS) )
        {
          curPdh = readDatasetScanAddLatest( curChildElem, catalog );
        }

        // Handle "latestComplete" child elements.
        else if ( curChildElem.getName().equals( "latestComplete" )
                  && curChildElem.getNamespace().equals( defNS ) )
        {
          // Get latest name.
          String latestName = curChildElem.getAttributeValue( "name" );
          if ( latestName == null )
          {
            logger.warn( "readDatasetScanAddProxies(): unnamed latestComplete, skipping.");
            continue;
          }

          // Does latest go on top or bottom of list.
          Attribute topAtt = curChildElem.getAttribute( "top" );
          boolean latestOnTop = true;
          if ( topAtt != null )
          {
            try
            {
              latestOnTop = topAtt.getBooleanValue();
            }
            catch ( DataConversionException e )
            {
              latestOnTop = true;
            }
          }

          // Get the latest service name.
          String serviceName = curChildElem.getAttributeValue( "serviceName" );
          if ( serviceName == null )
          {
            logger.warn( "readDatasetScanAddProxies(): no service name given in latestComplete." );
            continue;
          }
          InvService service = catalog.findService( serviceName );
          if ( service == null )
          {
            logger.warn( "readDatasetScanAddProxies(): named service <" + serviceName + "> not found." );
            continue;
          }

          // Get lastModifed limit.
          String lastModLimitVal = curChildElem.getAttributeValue( "lastModifiedLimit" );
          long lastModLimit;
          if ( lastModLimitVal == null )
            lastModLimit = 60; // Default to one hour
          else
            lastModLimit = Long.parseLong( lastModLimitVal);

          // Get isResolver.
          String isResolverString = curChildElem.getAttributeValue( "isResolver");
          boolean isResolver = true;
          if ( isResolverString != null )
            if ( isResolverString.equalsIgnoreCase( "false"))
              isResolver = false;

          // Build the SimpleLatestProxyDsHandler and add to map.
          curPdh = new LatestCompleteProxyDsHandler( latestName, latestOnTop, service, isResolver, lastModLimit );
        }
        else
        {
          curPdh = null;
          // @todo Deal with allowing user defined inserters
          //Element userDefElem = addLatestElem.getChild( "proxyDatasetHandlerImpl", defNS );
        }

        // Add current proxy dataset handler to map if name is not already in map.
        if ( curPdh != null )
        {
          if ( allProxyDsHandlers.containsKey( curPdh.getProxyDatasetName() ) )
          {
            logger.warn( "readDatasetScanAddProxies(): proxy map already contains key <" + curPdh.getProxyDatasetName() + ">, skipping." );
            continue;
          }
          allProxyDsHandlers.put( curPdh.getProxyDatasetName(), curPdh );
        }
      }
    }

    return allProxyDsHandlers;
}

  /**
   * Return a SimpleLatestProxyDsHandler, use default values if element is null.
   *
   * @param simpleLatestElem the simpleLatest element
   * @param catalog the catalog containing the simpleLatest element.
   * @return a SimpleLatestProxyDsHandler
   */
  private ProxyDatasetHandler readDatasetScanAddLatest( Element simpleLatestElem, InvCatalogImpl catalog )
  {
    // Default values is simpleLatestElem is null.
    ProxyDatasetHandler latestAdder = null;
    String latestName = "latest.xml";
    boolean latestOnTop = true;
    String latestServiceName = "latest";
    boolean isResolver = true;

    // If simpleLatestElem exists, read values.
    if ( simpleLatestElem != null )
    {
      // Get latest name.
      String tmpLatestName = simpleLatestElem.getAttributeValue( "name" );
      if ( tmpLatestName != null )
        latestName = tmpLatestName;

      // Does latest go on top or bottom of list.
      Attribute topAtt = simpleLatestElem.getAttribute( "top" );
      if ( topAtt != null )
      {
        try
        {
          latestOnTop = topAtt.getBooleanValue();
        }
        catch ( DataConversionException e )
        {
          latestOnTop = true;
        }
      }

      // Get the latest service name.
      String tmpLatestServiceName = simpleLatestElem.getAttributeValue( "serviceName" );
      if ( tmpLatestServiceName != null )
        latestServiceName = tmpLatestServiceName;

      // Get isResolver.
      String isResolverString = simpleLatestElem.getAttributeValue( "isResolver" );
      if ( isResolverString != null )
        if ( isResolverString.equalsIgnoreCase( "false" ) )
          isResolver = false;
    }

    // Build the SimpleLatestProxyDsHandler
    InvService service = catalog.findService( latestServiceName );
    if ( service == null )
      logger.warn( "readDatasetScanAddLatest(): named service <" + latestServiceName + "> not found." );
    else
      latestAdder = new SimpleLatestProxyDsHandler( latestName, latestOnTop, service, isResolver );

    return latestAdder;
  }

//  protected CatalogRefExpander readDatasetScanCatRefExpander( Element catRefExpanderElem )
//  {
//
//  }

  protected DatasetEnhancer readDatasetScanAddTimeCoverage( Element addTimeCovElem )
  {
    DatasetEnhancer timeCovEnhancer = null;

    String matchName = addTimeCovElem.getAttributeValue( "datasetNameMatchPattern" );
    String matchPath = addTimeCovElem.getAttributeValue( "datasetPathMatchPattern" );
    String subst = addTimeCovElem.getAttributeValue( "startTimeSubstitutionPattern" );
    String duration = addTimeCovElem.getAttributeValue( "duration" );
    if ( matchName != null && subst != null && duration != null )
    {
      timeCovEnhancer = RegExpAndDurationTimeCoverageEnhancer
              .getInstanceToMatchOnDatasetName( matchName, subst, duration );
    }
    else if ( matchPath != null && subst != null && duration != null )
    {
      timeCovEnhancer = RegExpAndDurationTimeCoverageEnhancer
              .getInstanceToMatchOnDatasetPath( matchPath, subst, duration );
    }

    return timeCovEnhancer;
  }

  private Object readDatasetScanUserDefined( Element userDefElem, Class targetClass )
  {
    String className = userDefElem.getAttributeValue( "className");
    Element configElem = null;
    List childrenElemList = userDefElem.getChildren();
    if ( childrenElemList.size() == 1 )
    {
      configElem = (Element) childrenElemList.get( 0 );
    }
    else if ( childrenElemList.size() != 0 )
    {
      logger.warn( "readDatasetScanUserDefined(): config XML not a single element, using first element." );
      configElem = (Element) childrenElemList.get( 0 );
    }
    else
    {
      logger.debug( "readDatasetScanUserDefined(): no config XML elements." );
      configElem = null;
    }

    try
    {
      // Get the Class instance for desired targetClass implementation.
      Class requestedClass = Class.forName( className );

      // Check that the requested Class is a target Class.
      if ( ! targetClass.isAssignableFrom( requestedClass ) )
      {
        throw new IllegalArgumentException( "Requested class <" + className + "> not an implementation of " + targetClass.getName() + "." );
      }

      // Instantiate the desired Object using that classes constructor with a
      // single Object argument.
      Class [] argTypes = {Object.class};
      Object [] args = {configElem};
      Constructor constructor = requestedClass.getConstructor( argTypes );

      return constructor.newInstance( args );
    }
    catch ( ClassNotFoundException e )
    {
      logger.warn( "readDatasetScanUserDefined(): exception creating user defined object <" + className + ">", e );
      return null;
    }
    catch ( NoSuchMethodException e )
    {
      logger.warn( "readDatasetScanUserDefined(): exception creating user defined object <" + className + ">", e );
      return null;
    }
    catch ( InstantiationException e )
    {
      logger.warn( "readDatasetScanUserDefined(): exception creating user defined object <" + className + ">", e );
      return null;
    }
    catch ( IllegalAccessException e )
    {
      logger.warn( "readDatasetScanUserDefined(): exception creating user defined object <" + className + ">", e );
      return null;
    }
    catch ( InvocationTargetException e )
    {
      logger.warn( "readDatasetScanUserDefined(): exception creating user defined object <" + className + ">", e );
      return null;
    }
  }

  protected DataRootConfig readDatasetRoot( Element s) {
    String path = s.getAttributeValue("path");
    String dirLocation = s.getAttributeValue("location");
    if ( dirLocation == null )
      dirLocation = s.getAttributeValue( "dirLocation" );
    dirLocation = expandDataRootLocationAlias( dirLocation );

    if (path != null) {
      if (path.charAt(0) == '/') path = path.substring(1);
      int last = path.length()-1;
      if (path.charAt(last) == '/') path = path.substring(0, last);
    }

    if (dirLocation != null) {
      int last = dirLocation.length()-1;
      if (dirLocation.charAt(last) != '/') dirLocation = dirLocation + '/';
    }

    return new DataRootConfig( path, dirLocation, s.getAttributeValue("cache"));
  }

  protected DateType readDate(Element elem) {
    if (elem == null) return null;
    String format =  elem.getAttributeValue("format");
    String type =  elem.getAttributeValue("type");
    return makeDateType( elem.getText(), format, type);
  }

  protected DateType makeDateType(String text, String format, String type) {
    if (text == null) return null;
    try {
      return new DateType( text, format, type);
    } catch (java.text.ParseException e) {
      factory.appendErr(" ** Parse error: Bad date format = "+text+"\n");
      return null;
    }
  }

  protected TimeDuration readDuration(Element elem) {
    if (elem == null) return null;
    String text = null;
    try {
      text = elem.getText();
      return new TimeDuration( text);
    } catch (java.text.ParseException e) {
      factory.appendErr(" ** Parse error: Bad duration format = "+text+"\n");
      return null;
    }
  }

  protected InvDocumentation readDocumentation( InvCatalog cat, Element s) {
    String href = s.getAttributeValue("href", xlinkNS);
    String title = s.getAttributeValue("title", xlinkNS);
    String type = s.getAttributeValue("type"); // not XLink type
    String content = s.getTextNormalize();

    URI uri = null;
    if (href != null) {
      try {
        uri = cat.resolveUri(href);
      } catch (Exception e) {
        factory.appendErr(" ** Invalid documentation href = "+href+" "+e.getMessage()+"\n");
      }
    }

    InvDocumentation doc = new InvDocumentation( href, uri, title, type, content);

    // LOOK XHTML ?? !!

    if (InvCatalogFactory.debugXML) System.out.println (" Documentation added: "+ doc);
    return doc;
  }

  protected double readDouble(Element elem) {
    if (elem == null) return Double.NaN;
    String text = elem.getText();
    try {
      return Double.parseDouble( text);
    } catch (NumberFormatException e) {
      factory.appendErr(" ** Parse error: Bad double format = "+text+"\n");
      return Double.NaN;
    }
  }

  protected ThreddsMetadata.GeospatialCoverage readGeospatialCoverage( Element gcElem) {
    if (gcElem == null) return null;

    String zpositive = gcElem.getAttributeValue("zpositive");

    ThreddsMetadata.Range northsouth = readGeospatialRange( gcElem.getChild("northsouth", defNS), "degrees_north");
    ThreddsMetadata.Range eastwest = readGeospatialRange( gcElem.getChild("eastwest", defNS), "degrees_east");
    ThreddsMetadata.Range updown = readGeospatialRange( gcElem.getChild("updown", defNS), "m");

    // look for names
    ArrayList names = new ArrayList();
    java.util.List<Element> list = gcElem.getChildren("name", defNS);
    for ( Element e : list) {
      ThreddsMetadata.Vocab name = readControlledVocabulary(e);
      names.add(name);
    }

    ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage(
        eastwest, northsouth, updown, names, zpositive);
    return gc;
  }

  protected ThreddsMetadata.Range readGeospatialRange( Element spElem, String defUnits) {
    if (spElem == null) return null;

    double start = readDouble( spElem.getChild("start", defNS));
    double size = readDouble( spElem.getChild("size", defNS));
    double resolution = readDouble( spElem.getChild("resolution", defNS));

    String units = spElem.getChildText("units", defNS);
    if (units == null) units = defUnits;

    return new ThreddsMetadata.Range( start, size, resolution, units);
  }

  protected InvMetadata readMetadata( InvCatalog catalog, InvDatasetImpl dataset, Element mdataElement) {
    // there are 6 cases to deal with: threddsNamespace vs not & inline vs Xlink & hasConverter or not
    // (the hasConverter only applies when its not threddsNamespace, giving 6 cases)
    // this factory is the converter for threddsNamespace metadata
    //  and also handles non-threddsNamespace when there is no converter, in which case it just
    //   propagates the inline dom elements

    // figure out the namespace
    Namespace namespace;
    List inlineElements = mdataElement.getChildren();
    if (inlineElements.size() > 0) // look at the namespace of the children, if they exist
      namespace = ((Element) inlineElements.get( 0)).getNamespace();
    else
      namespace = mdataElement.getNamespace(); // will be thredds

    String mtype = mdataElement.getAttributeValue("metadataType");
    String href = mdataElement.getAttributeValue("href", xlinkNS);
    String title = mdataElement.getAttributeValue("title", xlinkNS);
    String inheritedS = mdataElement.getAttributeValue("inherited");
    boolean inherited = (inheritedS != null) && inheritedS.equalsIgnoreCase("true");

    boolean isThreddsNamespace = ((mtype == null) || mtype.equalsIgnoreCase("THREDDS")) &&
                                 namespace.getURI().equals(XMLEntityResolver.CATALOG_NAMESPACE_10);

    // see if theres a converter for it.
    MetadataConverterIF metaConverter = factory.getMetadataConverter( namespace.getURI());
    if (metaConverter == null) metaConverter = factory.getMetadataConverter( mtype);
    if (metaConverter != null) {
      if (debugMetadataRead) System.out.println("found factory for metadata type = "+mtype+" namespace = "+
                                                namespace+"="+metaConverter.getClass().getName());

      // see if theres any inline content
      Object contentObj = null;
      if (inlineElements.size() > 0) {
        contentObj = metaConverter.readMetadataContent( dataset, mdataElement);
        return new InvMetadata( dataset, mtype, namespace.getURI(), namespace.getPrefix(),
                                inherited, false, metaConverter, contentObj);

      } else { // otherwise it  must be an Xlink; defer reading
        return new InvMetadata(dataset, href, title, mtype, namespace.getURI(),
                               namespace.getPrefix(), inherited, false, metaConverter);
      }
    }

    // the case where its not ThreddsMetadata, but theres no converter
    if (!isThreddsNamespace) {
      if (inlineElements.size() > 0) {
        // just hold onto the jdom elements as the "content" LOOK should be DOM?
        return new InvMetadata( dataset, mtype, namespace.getURI(), namespace.getPrefix(),
                                inherited, false, this, mdataElement);

      } else { // otherwise it must be an Xlink, never read
        return new InvMetadata(dataset, href, title, mtype, namespace.getURI(),
                               namespace.getPrefix(), inherited, false, null);
      }

    }

    // the case where its ThreddsMetadata
    if (inlineElements.size() > 0) {
      ThreddsMetadata tmg = new ThreddsMetadata(false);
      readThreddsMetadata( catalog, dataset, mdataElement, tmg);
      return new InvMetadata( dataset, mtype, namespace.getURI(), namespace.getPrefix(),
                              inherited, true, this, tmg);

    } else { // otherwise it  must be an Xlink; defer reading
      return new InvMetadata(dataset, href, title, mtype, namespace.getURI(),
                             namespace.getPrefix(), inherited, true, this);
    }

  }

  /* MetadataConverterIF */
  public Object readMetadataContent(InvDataset dataset, org.jdom.Element mdataElement) {
    InvMetadata m = readMetadata(dataset.getParentCatalog(), (InvDatasetImpl) dataset, mdataElement);
    return m.getThreddsMetadata();
  }

  private SAXBuilder saxBuilder;
  private Element readContentFromURL(java.net.URI uri) throws java.io.IOException {
    if (saxBuilder == null) saxBuilder = new SAXBuilder();
    Document doc;
    try {
      doc = saxBuilder.build(uri.toURL());
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    return doc.getRootElement();
  }

     // this is only called for ThredddsMetadata
  public Object readMetadataContentFromURL( InvDataset dataset, java.net.URI uri) throws java.io.IOException {
    Element elem = readContentFromURL(uri);
    Object contentObject = readMetadataContent(dataset, elem);
    if (debugMetadataRead) System.out.println(" convert to " + contentObject.getClass().getName());
    return contentObject;
  }

    /* open and read the referenced catalog XML
    if (debugMetadataRead) System.out.println(" readMetadataContentFromURL = " + uri);
    org.w3c.dom.Element mdataElement = factory.readOtherXML( uri);
    if (mdataElement == null) {
      factory.appendErr(" ** failed to read thredds metadata at = "+uri+" for dataset"+dataset.getName()+"\n");
      return null;
    }

    Object contentObject = readMetadataContent( dataset, mdataElement);
    if (debugMetadataRead) System.out.println(" convert to " + contentObject.getClass().getName());
 return contentObject;  */

  // dummy LOOK
  public boolean validateMetadataContent(Object contentObject, StringBuilder out) { return true; }

  public void addMetadataContent( org.jdom.Element mdataElement, Object contentObject) { }

  protected InvProperty readProperty( Element s) {
    String name = s.getAttributeValue("name");
    String value = s.getAttributeValue("value");
    return new InvProperty( name, value);
  }

  protected ThreddsMetadata.Source readSource(Element elem) {
    if (elem == null) return null;
    ThreddsMetadata.Vocab name = readControlledVocabulary( elem.getChild("name", defNS));
    Element contact = elem.getChild("contact", defNS);
    if (contact == null) {
      factory.appendErr(" ** Parse error: Missing contact element in = "+elem.getName()+"\n");
      return null;
    }
    return new ThreddsMetadata.Source( name, contact.getAttributeValue("url"), contact.getAttributeValue("email"));
  }

  protected InvService readService( Element s, URI baseURI) {
    String name = s.getAttributeValue("name");
    String type = s.getAttributeValue("serviceType");
    String serviceBase = s.getAttributeValue("base");
    String suffix = s.getAttributeValue("suffix");
    String desc = s.getAttributeValue("desc");

    InvService service = new InvService( name, type, serviceBase, suffix, desc);

    java.util.List<Element> propertyList = s.getChildren("property", defNS);
    for ( Element e : propertyList) {
      InvProperty p = readProperty( e);
      service.addProperty( p);
     }

    java.util.List<Element> rootList = s.getChildren("datasetRoot", defNS);
    for ( Element e : rootList ) {
      InvProperty root = readDatasetRoot(e);
      service.addDatasetRoot( root);
    }

    // nested services
    java.util.List<Element> serviceList = s.getChildren("service", defNS);
    for ( Element e : serviceList ) {
      InvService ss = readService( e, baseURI);
      service.addService( ss);
     }

    if (InvCatalogFactory.debugXML) System.out.println (" Service added: "+ service);
    return service;
  }

  protected double readDataSize(Element parent) {
    Element elem = parent.getChild("dataSize", defNS);
    if (elem == null) return Double.NaN;

    double size;
    String sizeS = elem.getText();
    try {
      size = Double.parseDouble( sizeS);
    } catch (NumberFormatException e) {
      factory.appendErr(" ** Parse error: Bad double format in size element = "+sizeS+"\n");
      return Double.NaN;
    }

    String units = elem.getAttributeValue("units");
    char c = Character.toUpperCase(units.charAt(0));
    if (c == 'K') size *= 1000;
    else if (c == 'M') size *= 1000 * 1000;
    else if (c == 'G') size *= 1000 * 1000 * 1000;
    else if (c == 'T') size *= 1000.0 * 1000 * 1000 * 1000;
    else if (c == 'P') size *= 1000.0 * 1000 * 1000 * 1000 * 1000;
    return size;
  }

  protected DateRange readTimeCoverage( Element tElem) {
    if (tElem == null) return null;

    DateType start = readDate( tElem.getChild("start", defNS));
    DateType end = readDate( tElem.getChild("end", defNS));
    TimeDuration duration = readDuration( tElem.getChild("duration", defNS));
    TimeDuration resolution = readDuration( tElem.getChild("resolution", defNS));

    try {
      DateRange tc = new DateRange( start, end, duration, resolution);
      return tc;
    } catch (java.lang.IllegalArgumentException e) {
      factory.appendWarning(" ** warning: TimeCoverage error = "+e.getMessage()+"\n");
      return null;
    }
  }

  protected void readThreddsMetadata( InvCatalog catalog, InvDatasetImpl dataset,
                                      Element parent, ThreddsMetadata tmg) {
    List<Element> list;

     // look for creators - kind of a Source
    list = parent.getChildren("creator", defNS);
    for ( Element e : list) {
      tmg.addCreator( readSource( e));
    }

     // look for contributors
    list = parent.getChildren("contributor", defNS);
    for (Element e : list) {
      tmg.addContributor( readContributor( e));
    }

     // look for dates
    list = parent.getChildren("date", defNS);
    for ( Element e : list) {
      DateType d = readDate( e);
      tmg.addDate( d);
     }

     // look for documentation
    list = parent.getChildren("documentation", defNS);
    for ( Element e : list) {
      InvDocumentation doc = readDocumentation( catalog, e);
      tmg.addDocumentation( doc);
     }

     // look for keywords - kind of a controlled vocabulary
    list = parent.getChildren("keyword", defNS);
    for ( Element e : list) {
      tmg.addKeyword( readControlledVocabulary( e));
    }

    // look for metadata
    java.util.List<Element> mList = parent.getChildren("metadata", defNS);
    for ( Element e : mList) {
      InvMetadata m = readMetadata( catalog, dataset, e);
      if (m != null) {
          tmg.addMetadata(m);
      }
    }

     // look for projects - kind of a controlled vocabulary
    list = parent.getChildren("project", defNS);
    for ( Element e : list) {
      tmg.addProject( readControlledVocabulary( e));
    }

     // look for properties
    list = parent.getChildren("property", defNS);
    for ( Element e : list) {
      InvProperty p = readProperty( e);
      tmg.addProperty( p);
     }

     // look for publishers - kind of a Source
    list = parent.getChildren("publisher", defNS);
    for ( Element e : list) {
      tmg.addPublisher( readSource( e));
    }

     // look for variables
    list = parent.getChildren("variables", defNS);
    for ( Element e : list) {
      ThreddsMetadata.Variables vars = readVariables( catalog, dataset, e);
      tmg.addVariables( vars);
     }

    // can only be one each of these kinds
    ThreddsMetadata.GeospatialCoverage gc = readGeospatialCoverage( parent.getChild("geospatialCoverage", defNS));
    if (gc != null) tmg.setGeospatialCoverage( gc);

    DateRange tc = readTimeCoverage( parent.getChild("timeCoverage", defNS));
    if (tc != null) tmg.setTimeCoverage( tc);

    Element serviceNameElem = parent.getChild("serviceName", defNS);
    if (serviceNameElem != null) tmg.setServiceName( serviceNameElem.getText());

    Element authElem = parent.getChild("authority", defNS);
    if (authElem != null) tmg.setAuthority( authElem.getText());

    Element dataTypeElem = parent.getChild("dataType", defNS);
    if (dataTypeElem != null) {
      String dataTypeName = dataTypeElem.getText();
      if ((dataTypeName != null) && (dataTypeName.length() > 0)) {
        FeatureType dataType = FeatureType.getType( dataTypeName.toUpperCase());
        if (dataType == null) {
          factory.appendWarning(" ** warning: non-standard data type = "+dataTypeName+"\n");
        }
        tmg.setDataType( dataType);
      }
    }

    Element dataFormatElem = parent.getChild("dataFormat", defNS);
    if (dataFormatElem != null) {
      String dataFormatTypeName = dataFormatElem.getText();
      if ((dataFormatTypeName != null) && (dataFormatTypeName.length() > 0)) {
        DataFormatType dataFormatType = DataFormatType.findType( dataFormatTypeName);
        if (dataFormatType == null) {
          dataFormatType = DataFormatType.getType( dataFormatTypeName );
          factory.appendWarning(" ** warning: non-standard dataFormat type = "+dataFormatTypeName+"\n");
        }
        tmg.setDataFormatType( dataFormatType);
      }
    }

    double size = readDataSize(parent);
    if ( !Double.isNaN( size))
      tmg.setDataSize( size);
  }

  protected ThreddsMetadata.Variable readVariable( Element varElem) {
    if (varElem == null) return null;

    String name = varElem.getAttributeValue("name");
    String desc = varElem.getText();
    String vocabulary_name = varElem.getAttributeValue("vocabulary_name");
    String units = varElem.getAttributeValue("units");
    String id = varElem.getAttributeValue("vocabulary_id");

    return new ThreddsMetadata.Variable( name, desc, vocabulary_name, units, id);
  }


  protected ThreddsMetadata.Variables readVariables( InvCatalog cat, InvDataset ds, Element varsElem) {
    if (varsElem == null) return null;

    String vocab = varsElem.getAttributeValue("vocabulary");
    String vocabHref = varsElem.getAttributeValue("href", xlinkNS);

    URI vocabUri = null;
    if (vocabHref != null) {
      try {
        vocabUri = cat.resolveUri(vocabHref);
      } catch (Exception e) {
        factory.appendErr(" ** Invalid Variables vocabulary URI = "+vocabHref+" "+e.getMessage()+"\n");
      }
    }

    java.util.List<Element> vlist = varsElem.getChildren("variable", defNS);

    String mapHref = null;
    URI mapUri = null;
    Element map = varsElem.getChild("variableMap", defNS);
    if (map != null) {
      mapHref = map.getAttributeValue("href", xlinkNS);
      try {
        mapUri = cat.resolveUri(mapHref);
      } catch (Exception e) {
        factory.appendErr(" ** Invalid Variables map URI = "+mapHref+" "+e.getMessage()+"\n");
      }
    }

    if ((mapUri != null) && vlist.size() > 0) { // cant do both
      factory.appendErr(" ** Catalog error: cant have variableMap and variable in same element (dataset = "+
                        ds.getName()+"\n");
      mapUri = null;
    }

    ThreddsMetadata.Variables variables = new ThreddsMetadata.Variables( vocab, vocabHref, vocabUri, mapHref, mapUri);

    for ( Element e : vlist) {
      ThreddsMetadata.Variable v = readVariable( e);
      variables.addVariable( v);
    }

    // read in variable map LOOK: would like to defer
    if (mapUri != null) {
      Element varsElement;
      try {
        varsElement = readContentFromURL(mapUri);
        List<Element> list = varsElement.getChildren("variable", defNS);
        for ( Element e : list) {
          ThreddsMetadata.Variable v = readVariable(e);
          variables.addVariable(v);
        }
      } catch (IOException e) {
        logger.warn("Failure reading vaiable mapUri ", e);
      }

      /*org.w3c.dom.Element domElement = factory.readOtherXML(mapUri);
      if (domElement != null) {
        Element varsElement = toJDOM(domElement);
        List list = varsElement.getChildren("variable", defNS);
        for (int j = 0; j < list.size(); j++) {
          ThreddsMetadata.Variable v = readVariable( (Element) list.get(j));
          variables.addVariable(v);
        }
      } */

    }

    return variables;
  }


  /************************************************************************/
  // Writing XML from objects

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os write to this OutputStream
   * @param raw write raw file if true (for server configuration)
   * @throws IOException
   */
  public void writeXML(InvCatalogImpl catalog, OutputStream os, boolean raw) throws IOException {
    this.raw = raw;
    writeXML( catalog, os);
    this.raw = false;
  }
  private boolean raw = false;

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os write to this OutputStream
   * @throws IOException
   */
  public void writeXML(InvCatalogImpl catalog, OutputStream os) throws IOException {
    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter();
    //fmt.setNewlines(true);
    //fmt.setIndent("  ");
    //fmt.setTrimAllWhite( true);
    XMLOutputter fmt = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());  // LOOK maybe compact ??
    fmt.output(writeCatalog(catalog), os);
  }

  public Document writeCatalog(InvCatalogImpl cat) {
    Element rootElem = new Element("catalog", defNS);
    Document doc = new Document(rootElem);

    // attributes
    if (cat.getName() != null)
      rootElem.setAttribute("name", cat.getName());
    rootElem.setAttribute("version", version);
    rootElem.addNamespaceDeclaration(xlinkNS);
    if (cat.getExpires() != null)
      rootElem.setAttribute("expires", cat.getExpires().toString());

    // services
    Iterator iter = cat.getServices().iterator();
    while ( iter.hasNext()) {
      InvService service = (InvService) iter.next();
      rootElem.addContent( writeService( service));
    }

    // dataset roots
    if (raw) {
      iter = cat.getDatasetRoots().iterator();
      while ( iter.hasNext()) {
        InvProperty p = (InvProperty) iter.next();
        rootElem.addContent( writeDatasetRoot( p));
      }
    }

    // properties
    iter = cat.getProperties().iterator();
    while ( iter.hasNext()) {
      InvProperty p = (InvProperty) iter.next();
      rootElem.addContent( writeProperty( p));
    }

    // datasets
    iter = cat.getDatasets().iterator();
    while ( iter.hasNext()) {
      InvDatasetImpl ds = (InvDatasetImpl) iter.next();
      if (ds instanceof InvDatasetScan)
        rootElem.addContent( writeDatasetScan( (InvDatasetScan) ds));
      else if (ds instanceof InvCatalogRef)
        rootElem.addContent( writeCatalogRef( (InvCatalogRef) ds));
      else
        rootElem.addContent( writeDataset( ds));
    }

    return doc;
  }

  private Element writeAccess( InvAccessImpl access) {
    Element accessElem = new Element("access", defNS);
    accessElem.setAttribute("urlPath", access.getUrlPath());
    if (access.getServiceName() != null)
      accessElem.setAttribute("serviceName", access.getServiceName());
    if (access.getDataFormatType() != null)
      accessElem.setAttribute("dataFormat", access.getDataFormatType().toString());

    if (access.hasDataSize())
      accessElem.addContent( writeDataSize( access.getDataSize()));

    return accessElem;
  }

  private Element writeCatalogRef( InvCatalogRef catRef) {
    Element catrefElem = new Element("catalogRef", defNS);
    catrefElem.setAttribute("href", catRef.getXlinkHref(), xlinkNS);
    String name = catRef.getName() == null ? "" : catRef.getName();
    catrefElem.setAttribute("title", name, xlinkNS);
    if ( catRef.getID() != null )
      catrefElem.setAttribute("ID", catRef.getID() );
    if (catRef.getRestrictAccess() != null)
      catrefElem.setAttribute("restrictAccess", catRef.getRestrictAccess());
    catrefElem.setAttribute("name", "");

    /* List list = catRef.getDocumentation();
    for (int j=0; j< list.size(); j++) {
      InvDocumentation doc = (InvDocumentation) list.get(j);
      catrefElem.addContent( writeDocumentation(doc, "documentation"));
    } */

    return catrefElem;
  }

  protected Element writeContributor(ThreddsMetadata.Contributor c) {
    Element elem = new Element("contributor", defNS);
    if (c.getRole() != null)
      elem.setAttribute("role", c.getRole());
    elem.setText( c.getName());
    return elem;
  }

  private Element writeControlledVocabulary( ThreddsMetadata.Vocab v, String name) {
    Element elem = new Element(name, defNS);
    if (v.getVocabulary() != null)
      elem.setAttribute("vocabulary", v.getVocabulary());
    elem.addContent(v.getText());
    return elem;
  }

  private Element writeDataset( InvDatasetImpl ds) {
    Element dsElem = new Element("dataset", defNS);

    if (ds instanceof InvDatasetImplProxy) {
      dsElem.setAttribute("name", ((InvDatasetImplProxy)ds).getAliasName());
      dsElem.setAttribute("alias", ds.getID());
      return dsElem;
    }

    writeDatasetInfo( ds, dsElem, true, raw);

    return dsElem;
  }

  private Element writeDatasetFmrc( InvDatasetFmrc ds) {
    Element dsElem;

    if ( raw ) {
      dsElem = new Element( "datasetFmrc", defNS );
      dsElem.setAttribute( "name", ds.getName() );
      dsElem.setAttribute( "path", ds.getPath() );
      if (ds.isRunsOnly())
        dsElem.setAttribute( "runsOnly", "true" );
      writeDatasetInfo( ds, dsElem, false, true );

    } else {
      dsElem = writeCatalogRef( ds);
      // dsElem.addContent( writeProperty( new InvProperty( "DatasetFmrc", ds.getPath() ) ) ); /// LOOK security hole - not used anyway, I think
    }

    return dsElem;
  }

  private Element writeDatasetRoot( InvProperty prop) {
    Element drootElem = new Element("datasetRoot", defNS);
    drootElem.setAttribute("path", prop.getName());
    drootElem.setAttribute("location", prop.getValue());
    return drootElem;
  }

  private Element writeDatasetScan( InvDatasetScan ds)
  {
    Element dsElem;

    if ( raw )
    {
      // Setup datasetScan element
      dsElem = new Element( "datasetScan", defNS );
      writeDatasetInfo( ds, dsElem, false, true );
      dsElem.setAttribute( "path", ds.getPath() );
      dsElem.setAttribute( "location", ds.getScanLocation() );

      // Write datasetConfig element
      if ( ds.getCrDsClassName() != null )
      {
        Element configElem = new Element( "crawlableDatasetImpl", defNS );
        configElem.setAttribute( "className", ds.getCrDsClassName() );
        if ( ds.getCrDsConfigObj() != null )
        {
          if ( ds.getCrDsConfigObj() instanceof Element )
          {
            configElem.addContent( (Element) ds.getCrDsConfigObj() );
          }
        }
      }

      // Write filter element
      if ( ds.getFilter() != null )
        dsElem.addContent( writeDatasetScanFilter( ds.getFilter() ) );

      // Write addID element
      //if ( ds.getIdentifier() != null )
      dsElem.addContent( writeDatasetScanIdentifier( ds.getIdentifier()) );

      // Write namer element
      if ( ds.getNamer() != null)
              dsElem.addContent( writeDatasetScanNamer( ds.getNamer() ));

      // Write sort element
      if ( ds.getSorter() != null )
        dsElem.addContent( writeDatasetScanSorter( ds.getSorter() ) );

      // Write addProxy element (and old addLatest element)
      if ( ! ds.getProxyDatasetHandlers().isEmpty() )
        dsElem.addContent( writeDatasetScanAddProxies( ds.getProxyDatasetHandlers() ) );

      // Write addDatasetSize element
      if ( ds.getAddDatasetSize() )
        dsElem.addContent( new Element( "addDatasetSize", defNS ) );

      // Write addTimeCoverage and datasetEnhancerImpl elements
      if ( ds.getChildEnhancerList() != null )
        dsElem.addContent( writeDatasetScanEnhancer( ds.getChildEnhancerList() ) );

      // @todo Write catalogRefExpander elements
//      if ( ds.getCatalogRefExpander() != null )
//        dsElem.addContent( writeDatasetScanCatRefExpander( ds.getCatalogRefExpander()));
    }
    else
    {
      if ( ds.isValid() )
      {
        dsElem = new Element( "catalogRef", defNS );
        writeDatasetInfo( ds, dsElem, false, false );
        dsElem.setAttribute( "href", ds.getXlinkHref(), xlinkNS );
        dsElem.setAttribute( "title", ds.getName(), xlinkNS );
        dsElem.setAttribute( "name", "" );
        dsElem.addContent( writeProperty( new InvProperty( "DatasetScan", "true" ) ) );
      }
      else
      {
        dsElem = new Element( "dataset", defNS );
        dsElem.setAttribute( "name", "** Misconfigured DatasetScan <" + ds.getPath() + "> **" );
        dsElem.addContent( new Comment( ds.getInvalidMessage() ) );
      }
    }

    return dsElem;
  }

  Element writeDatasetScanFilter( CrawlableDatasetFilter filter )
  {
    Element filterElem = new Element( "filter", defNS );
    if ( filter.getClass().isAssignableFrom( MultiSelectorFilter.class ) )
    {
      for ( Iterator it = ((List) filter.getConfigObject()).iterator(); it.hasNext(); )
      {
        MultiSelectorFilter.Selector curSelector = (MultiSelectorFilter.Selector) it.next();
        Element curSelectorElem;
        if ( curSelector.isIncluder() )
          curSelectorElem = new Element( "include", defNS );
        else
          curSelectorElem = new Element( "exclude", defNS );

        CrawlableDatasetFilter curFilter = curSelector.getFilter();
        if ( curFilter instanceof WildcardMatchOnNameFilter )
        {
          curSelectorElem.setAttribute( "wildcard", ((WildcardMatchOnNameFilter) curFilter).getWildcardString() );
          curSelectorElem.setAttribute( "atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false" );
          curSelectorElem.setAttribute( "collection", curSelector.isApplyToCollectionDataset() ? "true" : "false" );
        }
        else if ( curFilter instanceof RegExpMatchOnNameFilter )
        {
          curSelectorElem.setAttribute( "regExp", ((RegExpMatchOnNameFilter) curFilter).getRegExpString() );
          curSelectorElem.setAttribute( "atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false" );
          curSelectorElem.setAttribute( "collection", curSelector.isApplyToCollectionDataset() ? "true" : "false" );
        }
        else if ( curFilter instanceof LastModifiedLimitFilter )
        {
          curSelectorElem.setAttribute( "lastModLimitInMillis", Long.toString( ( (LastModifiedLimitFilter) curFilter ).getLastModifiedLimitInMillis() ) );
          curSelectorElem.setAttribute( "atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false" );
          curSelectorElem.setAttribute( "collection", curSelector.isApplyToCollectionDataset() ? "true" : "false" );
        }
        else
          curSelectorElem.addContent( new Comment( "Unknown selector type <" + curSelector.getClass().getName() + ">.") );

        filterElem.addContent( curSelectorElem );
      }
    }
    else
    {
      filterElem.addContent( writeDatasetScanUserDefined( "crawlableDatasetFilterImpl", filter.getClass().getName(), filter.getConfigObject() ) );
    }

    return filterElem;
  }

  private Element writeDatasetScanNamer( CrawlableDatasetLabeler namer)
  {
    Element namerElem = null;
    if ( namer != null )
    {
      namerElem = new Element( "namer", defNS );
      if ( namer instanceof MultiLabeler )
      {
        for ( Iterator it = ((MultiLabeler) namer).getLabelerList().iterator(); it.hasNext(); )
        {
          CrawlableDatasetLabeler curNamer = (CrawlableDatasetLabeler) it.next();

          Element curNamerElem;
          if ( curNamer instanceof RegExpAndReplaceOnNameLabeler )
          {
            curNamerElem = new Element( "regExpOnName", defNS);
            curNamerElem.setAttribute( "regExp", ((RegExpAndReplaceOnNameLabeler) curNamer).getRegExp() );
            curNamerElem.setAttribute( "replaceString", ((RegExpAndReplaceOnNameLabeler) curNamer).getReplaceString() );
            namerElem.addContent( curNamerElem );
          }
          else if (curNamer instanceof RegExpAndReplaceOnPathLabeler )
          {
            curNamerElem = new Element( "regExpOnPath" , defNS);
            curNamerElem.setAttribute( "regExp", ( (RegExpAndReplaceOnPathLabeler) curNamer ).getRegExp() );
            curNamerElem.setAttribute( "replaceString", ( (RegExpAndReplaceOnPathLabeler) curNamer ).getReplaceString() );
            namerElem.addContent( curNamerElem );
          }
          else
          {
            String tmpMsg = "writeDatasetScanNamer(): unsupported namer <" + curNamer.getClass().getName() + ">.";
            logger.warn( tmpMsg );
            namerElem.addContent( new Comment( tmpMsg ) );
          }
        }
      }
      else
      {
        namerElem.addContent( writeDatasetScanUserDefined( "crawlableDatasetLabelerImpl", namer.getClass().getName(), namer.getConfigObject() ) );
      }
    }

    return namerElem;
  }

  private Element writeDatasetScanIdentifier( CrawlableDatasetLabeler identifier)
  {
    Element identifierElem = new Element( "addID", defNS );
    if ( identifier != null )
    {
      if ( identifier instanceof SimpleLatestProxyDsHandler )
      {
        return identifierElem;
      }
      else
      {
        identifierElem = new Element( "addID", defNS );
        identifierElem.addContent( writeDatasetScanUserDefined( "crawlableDatasetLabelerImpl", identifier.getClass().getName(), identifier.getConfigObject() ) );
      }
    }

    return identifierElem;
  }

  private Element writeDatasetScanAddProxies( Map proxyDsHandlers)
  {
    Element addProxiesElem;

    // Write addLatest element if only proxyDsHandler and named "latest.xml".
    if ( proxyDsHandlers.size() == 1 && proxyDsHandlers.containsKey( "latest.xml") )
    {
      Object o = proxyDsHandlers.get( "latest.xml");
      if ( o instanceof SimpleLatestProxyDsHandler )
      {
        SimpleLatestProxyDsHandler pdh = (SimpleLatestProxyDsHandler) o;
        String name = pdh.getProxyDatasetName();
        boolean top = pdh.isLocateAtTopOrBottom();
        String serviceName = pdh.getProxyDatasetService( null ).getName();

        addProxiesElem = new Element( "addLatest", defNS );
        if ( name.equals( "latest.xml") && top && serviceName.equals( "latest"))
          return addProxiesElem;
        else
        {
          Element simpleLatestElem = new Element( "simpleLatest", defNS );

          simpleLatestElem.setAttribute( "name", name);
          simpleLatestElem.setAttribute( "top", top ? "true" : "false");
          simpleLatestElem.setAttribute( "servicName", serviceName );
          addProxiesElem.addContent( simpleLatestElem );
          return addProxiesElem;
        }
      }
    }

    // Write "addProxies" element
    addProxiesElem = new Element( "addProxies", defNS );
    for ( Iterator it = proxyDsHandlers.keySet().iterator(); it.hasNext(); )
    {
      String curName = (String) it.next();
      ProxyDatasetHandler curPdh = (ProxyDatasetHandler) proxyDsHandlers.get( curName );

      if ( curPdh instanceof SimpleLatestProxyDsHandler )
      {
        SimpleLatestProxyDsHandler sPdh = (SimpleLatestProxyDsHandler) curPdh;

        Element simpleLatestElem = new Element( "simpleLatest", defNS );

        simpleLatestElem.setAttribute( "name", sPdh.getProxyDatasetName() );
        simpleLatestElem.setAttribute( "top", sPdh.isLocateAtTopOrBottom() ? "true" : "false" );
        simpleLatestElem.setAttribute( "servicName", sPdh.getProxyDatasetService( null ).getName() );
        addProxiesElem.addContent( simpleLatestElem );
      }
      else if ( curPdh instanceof LatestCompleteProxyDsHandler )
      {
        LatestCompleteProxyDsHandler lcPdh = (LatestCompleteProxyDsHandler) curPdh;
        Element latestElem = new Element( "latestComplete", defNS );
        latestElem.setAttribute( "name", lcPdh.getProxyDatasetName() );
        latestElem.setAttribute( "top", lcPdh.isLocateAtTopOrBottom() ? "true" : "false" );
        latestElem.setAttribute( "servicName", lcPdh.getProxyDatasetService( null ).getName() );
        latestElem.setAttribute( "lastModifiedLimit", Long.toString( lcPdh.getLastModifiedLimit()));
        addProxiesElem.addContent( latestElem );
      }
      else
      {
        logger.warn( "writeDatasetScanAddProxies(): unknown type of ProxyDatasetHandler <" + curPdh.getProxyDatasetName() + ">.");
        // latestAdderElem.addContent( writeDatasetScanUserDefined( "datasetInserterImpl", latestAdder.getClass().getName(), latestAdder.getConfigObject() ) );
      }

    }
    return addProxiesElem;
  }

  private Element writeDatasetScanSorter( CrawlableDatasetSorter sorter)
  {
    Element sorterElem = new Element( "sort", defNS );
    if ( sorter instanceof LexigraphicByNameSorter )
    {
      Element lexElem = new Element( "lexigraphicByName", defNS );
      lexElem.setAttribute( "increasing", ( (LexigraphicByNameSorter) sorter ).isIncreasing() ? "true" : "false" );
      sorterElem.addContent( lexElem );
    }
    else
    {
      sorterElem.addContent( writeDatasetScanUserDefined( "crawlableDatasetSorterImpl", sorter.getClass().getName(), sorter.getConfigObject() ) );
    }

    return sorterElem;
  }

  private List writeDatasetScanEnhancer( List enhancerList )
  {
    List enhancerElemList = new ArrayList();
    int timeCovCount = 0;
    for ( Iterator it = enhancerList.iterator(); it.hasNext(); )
    {
      DatasetEnhancer curEnhancer = (DatasetEnhancer) it.next();

      if ( curEnhancer instanceof RegExpAndDurationTimeCoverageEnhancer )
      {
        if ( timeCovCount > 0 )
        {
          logger.warn( "writeDatasetScanEnhancer(): More than one addTimeCoverage element, skipping.");
          continue;
        }
        timeCovCount++;
        Element timeCovElem = new Element( "addTimeCoverage", defNS );
        RegExpAndDurationTimeCoverageEnhancer timeCovEnhancer = (RegExpAndDurationTimeCoverageEnhancer) curEnhancer;
        timeCovElem.setAttribute( "datasetNameMatchPattern", timeCovEnhancer.getMatchPattern() );
        timeCovElem.setAttribute( "startTimeSubstitutionPattern", timeCovEnhancer.getSubstitutionPattern() );
        timeCovElem.setAttribute( "duration", timeCovEnhancer.getDuration() );

        enhancerElemList.add( timeCovElem );
      }
      else
      {
        enhancerElemList.add( writeDatasetScanUserDefined( "datasetEnhancerImpl", curEnhancer.getClass().getName(), curEnhancer.getConfigObject() ) );
      }
    }

    return enhancerElemList;
  }

  private Element writeDatasetScanUserDefined( String userDefName, String className, Object configObj )
  {
    Element userDefElem = new Element( userDefName, defNS );
    userDefElem.setAttribute( "className", className );
    if ( configObj != null )
    {
      if ( configObj instanceof Element )
        userDefElem.addContent( (Element) configObj );
      else
        userDefElem.addContent( new Comment( "This class <" + className + "> not yet supported. This XML is missing configuration information (of type " + configObj.getClass().getName() + ")." ) );
    }

    return userDefElem;
  }

  private void writeDatasetInfo( InvDatasetImpl ds, Element dsElem, boolean doNestedDatasets, boolean showNcML) {
    dsElem.setAttribute("name", ds.getName());

    // other attributes, note the others get made into an element
    if ((ds.getCollectionType() != null) && (ds.getCollectionType() != CollectionType.NONE))
      dsElem.setAttribute("collectionType", ds.getCollectionType().toString());
    if (ds.isHarvest())
      dsElem.setAttribute("harvest", "true");
    if (ds.getID() != null)
      dsElem.setAttribute("ID", ds.getID());
    if (ds.getUrlPath() != null)
      dsElem.setAttribute("urlPath", ds.getUrlPath());
    if (ds.getRestrictAccess() != null)
      dsElem.setAttribute("restrictAccess", ds.getRestrictAccess());

    // services (local only)
    Iterator services = ds.getServicesLocal().iterator();
    while ( services.hasNext()) {
      InvService service = (InvService) services.next();
      dsElem.addContent( writeService( service));
    }

    // thredds metadata
    writeThreddsMetadata( dsElem, ds.getLocalMetadata());
    writeInheritedMetadata( dsElem, ds.getLocalMetadataInheritable());
    // writeInheritedMetadata( dsElem, ds.getCat6Metadata()); // LOOK can we get rid of this?

    // access  (local only)
    Iterator access = ds.getAccessLocal().iterator();
    while ( access.hasNext()) {
      InvAccessImpl a = (InvAccessImpl) access.next();
      dsElem.addContent( writeAccess( a));
    }

    if (showNcML && ds.getNcmlElement() != null) {
      org.jdom.Element ncml = (org.jdom.Element) ds.getNcmlElement().clone();
      ncml.detach();
      dsElem.addContent(ncml);
    }

    if (!doNestedDatasets) return;

    // nested datasets
    Iterator datasets = ds.getDatasets().iterator();
    while ( datasets.hasNext()) {
      InvDatasetImpl nested = (InvDatasetImpl) datasets.next();
      if (nested instanceof InvDatasetScan)
        dsElem.addContent( writeDatasetScan( (InvDatasetScan) nested));
      else if (nested instanceof InvDatasetFmrc)
        dsElem.addContent( writeDatasetFmrc( (InvDatasetFmrc) nested));
      else if (nested instanceof InvCatalogRef)
        dsElem.addContent( writeCatalogRef( (InvCatalogRef) nested));
      else
        dsElem.addContent( writeDataset( nested));
    }
  }

  protected Element writeDate(String name, DateType date) {
    Element dateElem = new Element(name, defNS);
    dateElem.addContent(date.getText());
    if (date.getType() != null)
      dateElem.setAttribute("type", date.getType());
    if (date.getFormat() != null)
      dateElem.setAttribute("format", date.getFormat());

    return dateElem;
  }

  private Element writeDocumentation( InvDocumentation doc, String name) {
    Element docElem = new Element(name, defNS);
    if (doc.getType() != null)
      docElem.setAttribute("type", doc.getType());

    if (doc.hasXlink()) {
      docElem.setAttribute("href", doc.getXlinkHref(), xlinkNS);
      if (!doc.getXlinkTitle().equals( doc.getURI().toString()))
        docElem.setAttribute("title", doc.getXlinkTitle(), xlinkNS);
    }

    String inline = doc.getInlineContent();
    if (inline != null)
      docElem.addContent(inline);
    return docElem;
  }

  public Element writeGeospatialCoverage( ThreddsMetadata.GeospatialCoverage gc) {
    Element elem = new Element("geospatialCoverage", defNS);
    if (gc.getZPositive().equals("down"))
      elem.setAttribute( "zpositive", gc.getZPositive());

    if (gc.getNorthSouthRange() != null)
      writeGeospatialRange( elem, new Element("northsouth", defNS), gc.getNorthSouthRange());
    if (gc.getEastWestRange() != null)
      writeGeospatialRange( elem, new Element("eastwest", defNS), gc.getEastWestRange());
    if (gc.getUpDownRange() != null)
      writeGeospatialRange( elem, new Element("updown", defNS), gc.getUpDownRange());

    // serialize isGlobal
    java.util.List<ThreddsMetadata.Vocab> names = gc.getNames();
    ThreddsMetadata.Vocab global = new ThreddsMetadata.Vocab("global", null);
    if (gc.isGlobal() && !names.contains(global)) {
      names.add(global);
    } else if (!gc.isGlobal() && names.contains(global)) {
      names.remove(global);
    }

    for ( ThreddsMetadata.Vocab name : names) {
      elem.addContent(writeControlledVocabulary(name, "name"));
    }

    return elem;
  }

  private void writeGeospatialRange(Element parent, Element elem, ThreddsMetadata.Range r) {
    if (r == null) return;

    elem.addContent( new Element("start", defNS).setText( Double.toString(r.getStart())));
    elem.addContent( new Element("size", defNS).setText( Double.toString(r.getSize())));
    if (r.hasResolution())
      elem.addContent( new Element("resolution", defNS).setText( Double.toString(r.getResolution())));
    if (r.getUnits() != null)
      elem.addContent( new Element("units", defNS).setText( r.getUnits()));

    parent.addContent( elem);
  }

  private Element writeMetadata( InvMetadata mdata) {
    Element mdataElem = new Element("metadata", defNS);
    if (mdata.getMetadataType() != null)
      mdataElem.setAttribute("metadataType", mdata.getMetadataType());
    if (mdata.isInherited())
      mdataElem.setAttribute("inherited", "true");

    String ns = mdata.getNamespaceURI();
    if ((ns != null) && !ns.equals(XMLEntityResolver.CATALOG_NAMESPACE_10)) {
      Namespace mdataNS = Namespace.getNamespace(mdata.getNamespacePrefix(), ns);
      mdataElem.addNamespaceDeclaration(mdataNS);
    }

    if (mdata.hasXlink()) {
      mdataElem.setAttribute("href", mdata.getXlinkHref(), xlinkNS);
      if (mdata.getXlinkTitle() != null)
        mdataElem.setAttribute("title", mdata.getXlinkTitle(), xlinkNS);

    } else if (mdata.getThreddsMetadata() != null) {
      writeThreddsMetadata( mdataElem, mdata.getThreddsMetadata());

    } else {
        // inline non-thredds case
      MetadataConverterIF converter = mdata.getConverter();
      if ((converter != null) && mdata.getContentObject() != null) {
        if (mdata.getContentObject() instanceof Element) { // special case
          Element mdataOrg = (Element) mdata.getContentObject();
          List<Element> children = mdataOrg.getChildren();
          for ( Element child : children ) {
            mdataElem.addContent( (Element) child.clone());
          }
        } else {
          //org.w3c.dom.Element dome = toDOM(mdataElem);
          converter.addMetadataContent(mdataElem, mdata.getContentObject());
          //mdataElem = toJDOM(dome);
          mdataElem.detach();
        }
      }
    }

    return mdataElem;
  }

  private Element writeProperty( InvProperty prop) {
    Element propElem = new Element("property", defNS);
    propElem.setAttribute("name", prop.getName());
    propElem.setAttribute("value", prop.getValue());
    return propElem;
  }

  protected Element writeSource( String elementName, ThreddsMetadata.Source p) {
    Element elem = new Element(elementName, defNS);

    elem.addContent( writeControlledVocabulary( p.getNameVocab() , "name"));

    Element contact = new Element("contact", defNS);
    if (p.getUrl() != null)
      contact.setAttribute("url", p.getUrl());
    if (p.getEmail() != null)
      contact.setAttribute("email", p.getEmail());
    elem.addContent( contact);

    return elem;
  }


  private Element writeService( InvService service) {
    Element serviceElem = new Element("service", defNS);
    serviceElem.setAttribute("name", service.getName());
    serviceElem.setAttribute("serviceType", service.getServiceType().toString());
    serviceElem.setAttribute("base", service.getBase());
    if ((service.getSuffix() != null) && (service.getSuffix().length() > 0))
      serviceElem.setAttribute("suffix", service.getSuffix());

    // properties
    Iterator props = service.getProperties().iterator();
    while ( props.hasNext()) {
      InvProperty p = (InvProperty) props.next();
      serviceElem.addContent( writeProperty( p));
    }

    // services
    Iterator services = service.getServices().iterator();
    while ( services.hasNext()) {
      InvService nested = (InvService) services.next();
      serviceElem.addContent( writeService( nested));
    }

    // dataset roots
    if (raw) {
      Iterator iter = service.getDatasetRoots().iterator();
      while ( iter.hasNext()) {
        InvProperty p = (InvProperty) iter.next();
        serviceElem.addContent( writeDatasetRoot( p));
      }
    }

    return serviceElem;
  }

  private Element writeDataSize( double size) {
    Element sizeElem = new Element("dataSize", defNS);

    // want exactly the number of bytes
    if (useBytesForDataSize) {
      sizeElem.setAttribute("units", "bytes");
      long bytes = (long) size;
      sizeElem.setText( Long.toString(bytes));
      return sizeElem;
    }
    
    // otherwise choose appropriate unit
    String unit;
    if (size > 1.0e15) {
      unit = "Pbytes";
      size *= 1.0e-15;
    } else if (size > 1.0e12) {
      unit = "Tbytes";
      size *= 1.0e-12;
    } else if (size > 1.0e9) {
      unit = "Gbytes";
      size *= 1.0e-9;
    } else if (size > 1.0e6) {
      unit = "Mbytes";
      size *= 1.0e-6;
    } else if (size > 1.0e3) {
      unit = "Kbytes";
      size *= 1.0e-3;
    } else  {
      unit = "bytes";
    }

    sizeElem.setAttribute("units", unit);
    sizeElem.setText( Format.d(size, 4));

    return sizeElem;
  }

  /* protected void writeCat6InheritedMetadata( Element elem, ThreddsMetadata tmi) {
    if ((tmi.getDataType() == null) && (tmi.getServiceName() == null) &&
        (tmi.getAuthority() == null) && ( tmi.getProperties().size() == 0))
      return;

    Element mdataElem = new Element("metadata", defNS);
    mdataElem.setAttribute("inherited", "true");
    writeThreddsMetadata( mdataElem, tmi);
    elem.addContent( mdataElem);
  }  */

  protected void writeInheritedMetadata( Element elem, ThreddsMetadata tmi) {
    Element mdataElem = new Element("metadata", defNS);
    mdataElem.setAttribute("inherited", "true");
    writeThreddsMetadata( mdataElem, tmi);
    if (mdataElem.getChildren().size() > 0)
      elem.addContent( mdataElem);
  }

  protected void writeThreddsMetadata( Element elem, ThreddsMetadata tmg) {

    if (tmg.getServiceName() != null) {
      Element serviceNameElem = new Element("serviceName", defNS);
      serviceNameElem.setText(tmg.getServiceName());
      elem.addContent( serviceNameElem);
    }

    if (tmg.getAuthority() != null) {
      Element authElem = new Element("authority", defNS);
      authElem.setText(tmg.getAuthority());
      elem.addContent( authElem);
    }

    if ((tmg.getDataType() != null) && (tmg.getDataType() != FeatureType.NONE) && (tmg.getDataType() != FeatureType.ANY)) {
      Element dataTypeElem = new Element("dataType", defNS);
      dataTypeElem.setText(tmg.getDataType().toString());
      elem.addContent( dataTypeElem);
    }

    if ((tmg.getDataFormatType() != null) && (tmg.getDataFormatType() != DataFormatType.NONE)) {
      Element dataFormatElem = new Element("dataFormat", defNS);
      dataFormatElem.setText(tmg.getDataFormatType().toString());
      elem.addContent( dataFormatElem);
    }

    if ( tmg.hasDataSize())
      elem.addContent( writeDataSize( tmg.getDataSize()));

    List<InvDocumentation> docList = tmg.getDocumentation();
    for ( InvDocumentation doc : docList ) {
      elem.addContent( writeDocumentation(doc, "documentation"));
    }

    List<ThreddsMetadata.Contributor> contribList = tmg.getContributors();
    for ( ThreddsMetadata.Contributor c : contribList) {
      elem.addContent( writeContributor(c));
    }

    List<ThreddsMetadata.Source> creatorList = tmg.getCreators();
    for ( ThreddsMetadata.Source p : creatorList ) {
      elem.addContent( writeSource("creator", p));
    }

    List<ThreddsMetadata.Vocab> kewordList = tmg.getKeywords();
    for ( ThreddsMetadata.Vocab v : kewordList ) {
      elem.addContent( writeControlledVocabulary(v, "keyword"));
    }

    List<InvMetadata> mdList = tmg.getMetadata();
    for ( InvMetadata m : mdList ) {
      elem.addContent( writeMetadata(m));
    }

    List<ThreddsMetadata.Vocab> projList = tmg.getProjects();
    for ( ThreddsMetadata.Vocab v : projList) {
      elem.addContent( writeControlledVocabulary(v, "project"));
    }

    List<InvProperty> propertyList = tmg.getProperties();
    for ( InvProperty p : propertyList ) {
      elem.addContent( writeProperty(p));
    }

    List<ThreddsMetadata.Source> pubList = tmg.getPublishers();
    for ( ThreddsMetadata.Source p : pubList ) {
      elem.addContent( writeSource("publisher", p));
    }

    List<DateType> dateList = tmg.getDates();
    for ( DateType d : dateList ) {
      elem.addContent( writeDate("date", d));
    }

    ThreddsMetadata.GeospatialCoverage gc = tmg.getGeospatialCoverage();
    if ((gc != null) && !gc.isEmpty())
      elem.addContent( writeGeospatialCoverage( gc));

    DateRange tc = tmg.getTimeCoverage();
    if (tc != null)
      elem.addContent( writeTimeCoverage( tc));

    List<ThreddsMetadata.Variables> varList = tmg.getVariables();
    for ( ThreddsMetadata.Variables v : varList ) {
      elem.addContent( writeVariables(v));
    }
  }

  protected Element writeTimeCoverage( DateRange t) {
    Element elem = new Element("timeCoverage", defNS);

    DateType start = t.getStart();
    DateType end = t.getEnd();
    TimeDuration duration = t.getDuration();
    TimeDuration resolution = t.getResolution();

    if (t.useStart() && (start != null) && !start.isBlank()) {
      Element startElem = new Element("start", defNS);
      startElem.setText(start.toString());
      elem.addContent(startElem);
    }

    if (t.useEnd() && (end != null) && !end.isBlank()) {
      Element telem = new Element("end", defNS);
      telem.setText(end.toString());
      elem.addContent(telem);
    }

    if (t.useDuration() && (duration != null) && !duration.isBlank()) {
      Element telem = new Element("duration", defNS);
      telem.setText(duration.toString());
      elem.addContent(telem);
    }

    if (t.useResolution() && (resolution != null) && !resolution.isBlank()) {
      Element telem = new Element("resolution", defNS);
      telem.setText(t.getResolution().toString());
      elem.addContent(telem);
    }

    return elem;
  }

  protected Element writeVariable( ThreddsMetadata.Variable v) {
    Element elem = new Element("variable", defNS);
    if ( v.getName() != null )
      elem.setAttribute("name", v.getName());
    if (v.getDescription() != null) {
      String desc = v.getDescription().trim();
      if (desc.length() > 0)
        elem.setText(v.getDescription());
    }
    if ( v.getVocabularyName() != null )
      elem.setAttribute("vocabulary_name", v.getVocabularyName());
    if (v.getUnits() != null)
      elem.setAttribute("units", v.getUnits());
    String id = v.getVocabularyId();
    if (id != null)
      elem.setAttribute("vocabulary_id", id);

    return elem;
  }

  protected Element writeVariables( ThreddsMetadata.Variables vs) {
    Element elem = new Element("variables", defNS);
    if (vs.getVocabulary() != null)
      elem.setAttribute("vocabulary", vs.getVocabulary());
    if (vs.getVocabHref() != null)
      elem.setAttribute("href", vs.getVocabHref(), xlinkNS);

    if (vs.getMapHref() != null) { // variable map
      Element mapElem = new Element("variableMap", defNS);
      mapElem.setAttribute("href", vs.getMapHref(), xlinkNS);
      elem.addContent( mapElem);

    } else { // inline variables
      List<ThreddsMetadata.Variable> varList = vs.getVariableList();
      for ( ThreddsMetadata.Variable v : varList ) {
        elem.addContent(writeVariable(v));
      }
    }

    return elem;
  }

  /* public org.w3c.dom.Element toDOM( Element elem) {
    try {
      if (domOut == null) domOut = new DOMOutputter();
      return domOut.output(elem);
    } catch (JDOMException e) {
      System.out.println("InvCatalogFactory6.readMetadata.toDom error " + e);
      return null;
    }
  }

  public Element toJDOM( org.w3c.dom.Element domElement) {
    return builder.build(domElement);
  }   */

}