/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.builder;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import thredds.client.catalog.*;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * uses JDOM to parse XML catalogs
 *
 * @author caron
 * @since 1/8/2015
 */
public class JdomCatalogBuilder {
  static public final String CATALOG_NAMESPACE_10 = "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0";
  static public final String NJ22_NAMESPACE = "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";
  static public final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

  static public final Namespace xlinkNS = Namespace.getNamespace("xlink", XLINK_NAMESPACE);
  static public final Namespace defNS = Namespace.getNamespace(CATALOG_NAMESPACE_10);
  static public final Namespace ncmlNS = Namespace.getNamespace("ncml", NJ22_NAMESPACE);

  private Formatter errlog = new Formatter();
  private boolean error = false;
  /**
    * Read an InvCatalog from an a URI.
    * Failures and exceptions are handled
    * by causing validate() to fail. Therefore, be sure to call validate() before trying
    * to use the InvCatalog object.
    *
    * @param uri : the URI of the document, used for resolving reletive references.
    * @return an InvCatalogImpl object
    */
   public boolean readXML(CatalogBuilder catBuilder, URI uri) throws IOException {

     try {
       SAXBuilder saxBuilder = new SAXBuilder();
       org.jdom2.Document jdomDoc = saxBuilder.build(uri.toURL());
       return readCatalog( catBuilder, jdomDoc.getRootElement(), uri);

     } catch (Exception e) {
       throw new IOException("failed to read catalog at "+uri.toString(), e);
     }

   }

   protected boolean readCatalog(CatalogBuilder catBuilder, Element catalogElem, URI docBaseURI) {
     String name = catalogElem.getAttributeValue("name");
     String catSpecifiedBaseURL = catalogElem.getAttributeValue("base");
     String expires = catalogElem.getAttributeValue("expires");
     String version = catalogElem.getAttributeValue("version");

     URI baseURI = docBaseURI;
     if (catSpecifiedBaseURL != null) {
       try {
         baseURI = new URI(catSpecifiedBaseURL);
       } catch (URISyntaxException e) {
         errlog.format("readCatalog(): bad catalog specified base URI=%s %n", catSpecifiedBaseURL);
         baseURI = docBaseURI;
       }
     }

     catBuilder.setName(name);
     catBuilder.setBaseURI(baseURI);
     catBuilder.setExpires(expires);
     catBuilder.setVersion(version);

     // read top-level services
     java.util.List<Element> sList = catalogElem.getChildren("service", defNS);
     for (Element e : sList) {
       catBuilder.addService(readService(e));
     }

     // read top-level properties
     java.util.List<Element> pList = catalogElem.getChildren("property", defNS);
     for (Element e : pList) {
       catBuilder.addProperty(readProperty(e));
     }

     // look for top-level dataset and catalogRefs elements (keep them in order)
     java.util.List<Element> allChildren = catalogElem.getChildren();
     for (Element e : allChildren) {
       if (e.getName().equals("dataset")) {
         catBuilder.addDataset( readDataset(null, e));

       } else if (e.getName().equals("catalogRef")) {
         catBuilder.addDataset( readCatalogRef(null, e));
       }
     }

     return error;
   }

  protected AccessBuilder readAccess(DatasetBuilder dataset, Element accessElem) {
    String urlPath = accessElem.getAttributeValue("urlPath");
    String serviceName = accessElem.getAttributeValue("serviceName");
    String dataFormat = accessElem.getAttributeValue("dataFormat");

    return new AccessBuilder(dataset, urlPath, serviceName, dataFormat, readDataSize(accessElem));
  }

    // read a dataset element
   protected DatasetBuilder readDataset(DatasetBuilder parent, Element dsElem) {

     // deal with aliases
     String name = dsElem.getAttributeValue("name");
     String alias = dsElem.getAttributeValue("alias");
     String authority = dsElem.getAttributeValue("authority");
     String collectionTypeName = dsElem.getAttributeValue("collectionType");
     String dataTypeName = dsElem.getAttributeValue("dataType");
     String harvest = dsElem.getAttributeValue("harvest");
     String id = dsElem.getAttributeValue("ID");
     String serviceName = dsElem.getAttributeValue("serviceName");
     String urlPath = dsElem.getAttributeValue("urlPath");

      FeatureType dataType = null;
      if (dataTypeName != null) {
        dataType = FeatureType.getType(dataTypeName.toUpperCase());
        if (dataType == null) {
          errlog.format(" ** warning: non-standard data type = %s%n", dataTypeName);
        }
      }

      boolean isHarvest = (harvest != null) && harvest.equalsIgnoreCase("true");


     DatasetBuilder builder = new DatasetBuilder( parent, name, collectionTypeName, isHarvest, id, urlPath);

     // look for access elements
     java.util.List<Element> aList = dsElem.getChildren("access", defNS);
     for (Element e : aList) {
       builder.addAccess( readAccess(builder, e));
     }

     // look for top-level dataset and catalogRefs elements (keep them in order)
     java.util.List<Element> allChildren = dsElem.getChildren();
     for (Element e : allChildren) {
       if (e.getName().equals("dataset")) {
         builder.addDataset( readDataset(builder, e));

       } else if (e.getName().equals("catalogRef")) {
         builder.addDataset( readCatalogRef(builder, e));
       }
     }

     return builder;
   }

  protected double readDataSize(Element parent) {
     Element elem = parent.getChild("dataSize", defNS);
     if (elem == null) return Double.NaN;

     double size;
     String sizeS = elem.getText();
     try {
       size = Double.parseDouble(sizeS);
     } catch (NumberFormatException e) {
       errlog.format(" ** Parse error: Bad double format in size element = %s%n", sizeS);
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


  protected Property readProperty(Element s) {
    String name = s.getAttributeValue("name");
    String value = s.getAttributeValue("value");
    return new Property(name, value);
  }

  protected Service readService(Element s) {
    String name = s.getAttributeValue("name");
    String typeS = s.getAttributeValue("serviceType");
    String serviceBase = s.getAttributeValue("base");
    String suffix = s.getAttributeValue("suffix");
    String desc = s.getAttributeValue("desc");

    ServiceType type = null;
    try {
      type = ServiceType.valueOf(typeS);
    } catch (Exception e) {
      errlog.format("bad service type = %s%n", typeS);
      error = true;
    }

    List<Property> properties = null;
    List<Element> propertyList = s.getChildren("property", defNS);
    for (Element e : propertyList) {
      if (properties == null) properties = new ArrayList<>();
      properties.add( readProperty(e));
    }

    // nested services
    List<Service> services = null;
    java.util.List<Element> serviceList = s.getChildren("service", defNS);
    for (Element e : serviceList) {
      if (services == null) services = new ArrayList<>();
      services.add(readService(e));
    }

    return new Service(name, serviceBase, type, suffix, desc, services, properties);
  }


   protected DatasetBuilder readCatalogRef(DatasetBuilder parent, Element catRefElem) {
     return null;

     /*String title = catRefElem.getAttributeValue("title", xlinkNS);
     if (title == null) title = catRefElem.getAttributeValue("name");
     String href = catRefElem.getAttributeValue("href", xlinkNS);
     String useRemCatSerStr = catRefElem.getAttributeValue("useRemoteCatalogService");
     Boolean useRemoteCatalogService = null;
     if (useRemCatSerStr != null) useRemoteCatalogService = Boolean.parseBoolean(useRemCatSerStr);
     CatalogRefBuilder catRef = new CatalogRefBuilder(parent, title, href);
     return catRef;  */
   }

  /*
   protected ThreddsMetadata.Contributor readContributor(Element elem) {
     if (elem == null) return null;
     return new ThreddsMetadata.Contributor(elem.getText(), elem.getAttributeValue("role"));
   }

   protected ThreddsMetadata.Vocab readControlledVocabulary(Element elem) {
     if (elem == null) return null;
     return new ThreddsMetadata.Vocab(elem.getText(), elem.getAttributeValue("vocabulary"));
   }

   protected void readDatasetInfo(InvCatalogImpl catalog, InvDatasetImpl dataset, Element dsElem, URI base) {
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
       dataType = FeatureType.getType(dataTypeName.toUpperCase());
       if (dataType == null) {
         factory.appendWarning(" ** warning: non-standard data type = " + dataTypeName + "\n");
       }
     }

     if (dataType != null)
       dataset.setDataType(dataType);
     if (serviceName != null)
       dataset.setServiceName(serviceName);
     if (urlPath != null)
       dataset.setUrlPath(urlPath);

     if (authority != null) dataset.setAuthority(authority);
     if (id != null) dataset.setID(id);
     if (harvest != null) dataset.setHarvest(harvest.equalsIgnoreCase("true"));
     if (restrictAccess != null) dataset.setResourceControl(restrictAccess);

     if (collectionTypeName != null) {
       CollectionType collectionType = CollectionType.findType(collectionTypeName);
       if (collectionType == null) {
         collectionType = CollectionType.getType(collectionTypeName);
         factory.appendWarning(" ** warning: non-standard collection type = " + collectionTypeName + "\n");
       }
       dataset.setCollectionType(collectionType);
     }

     catalog.addDatasetByID(dataset); // need to do immed for alias processing

     // look for services
     java.util.List<Element> serviceList = dsElem.getChildren("service", defNS);
     for (Element curElem : serviceList) {
       InvService s = readService(curElem, base);
       dataset.addService(s);
     }

     // look for direct thredds metadata (not inherited)
     ThreddsMetadata tmg = dataset.getLocalMetadata();
     readThreddsMetadata(catalog, dataset, dsElem, tmg);

     // look for access elements
     java.util.List<Element> aList = dsElem.getChildren("access", defNS);
     for (Element e : aList) {
       InvAccessImpl a = readAccess(dataset, e);
       dataset.addAccess(a);
     }

     // look for ncml
     Element ncmlElem = dsElem.getChild("netcdf", ncmlNS);
     if (ncmlElem != null) {
       ncmlElem.detach();
       dataset.setNcmlElement(ncmlElem);
       // System.out.println(" found ncml= "+ncmlElem);
     }

     // look for nested dataset and catalogRefs elements (keep them in order)
     java.util.List<Element> allChildren = dsElem.getChildren();
     for (Element e : allChildren) {
       if (e.getName().equals("dataset")) {
         InvDatasetImpl ds = readDataset(catalog, dataset, e, base);
         if (ds != null)
           dataset.addDataset(ds);
       } else if (e.getName().equals("catalogRef")) {
         InvDatasetImpl ds = readCatalogRef(catalog, dataset, e, base);
         dataset.addDataset(ds);
       } else if (e.getName().equals("datasetScan")) {
         dataset.addDataset(readDatasetScan(catalog, dataset, e, base));
       } else if (e.getName().equals("featureCollection")) {
         InvDatasetImpl ds = readFeatureCollection(catalog, dataset, e, base);
         if (ds != null)
           dataset.addDataset(ds);
       }
     }
   }

   protected InvDatasetImpl readFeatureCollection(InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {

     FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(dsElem);
     config.spec = expandAliasForCollectionSpec(config.spec);

     try {
       // InvDatasetFeatureCollection ds = InvDatasetFeatureCollection.factory(parent, config);
       Class c = this.getClass().getClassLoader().loadClass("thredds.catalog.InvDatasetFeatureCollection");
       Method m = c.getMethod("factory", InvDatasetImpl.class, FeatureCollectionConfig.class);
       InvDatasetImpl result = (InvDatasetImpl) m.invoke(null, parent, config);

       if (result == null) {
         logger.error("featureCollection " + config.collectionName + " has fatal error ");
         return null;
       }

       // regular dataset elements
       readDatasetInfo(catalog, result, dsElem, base);
       return result;

     } catch (Exception e) {
       logger.error("featureCollection " + config.collectionName + " has fatal error, skipping ", e);
       return null;
     }

   }

   /* protected InvDatasetImpl readFeatureCollection(InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {

     FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection(dsElem);
     config.spec = expandAliasForCollectionSpec(config.spec);

     try {
       InvDatasetFeatureCollection ds = InvDatasetFeatureCollection.factory(parent, config);
       if (ds == null) {
         logger.error("featureCollection " + config.collectionName + " has fatal error ");
         return null;
       }
       // regular dataset elements
       readDatasetInfo(catalog, ds, dsElem, base);
       return ds;

     } catch (Exception e) {
       logger.error("featureCollection " + config.collectionName + " has fatal error, skipping ", e);
       return null;
     }

   }

   // read a dataset scan element
   protected InvDatasetScan readDatasetScan(InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {
     InvDatasetScan datasetScan;

     if (dsElem.getAttributeValue("dirLocation") == null) {
       if (dsElem.getAttributeValue("location") == null) {
         logger.error("readDatasetScan(): datasetScan has neither a \"location\" nor a \"dirLocation\" attribute.");
         datasetScan = null;
       } else {
         return readDatasetScanNew(catalog, parent, dsElem, base);
       }
     } else {
       String name = dsElem.getAttributeValue("name");
       factory.appendWarning("**Warning: Dataset " + name + " using old form of DatasetScan (dirLocation instead of location)\n");

       String path = dsElem.getAttributeValue("path");

       String scanDir = expandAliasForPath(dsElem.getAttributeValue("dirLocation"));
       String filter = dsElem.getAttributeValue("filter");
       String addDatasetSizeString = dsElem.getAttributeValue("addDatasetSize");
       String addLatest = dsElem.getAttributeValue("addLatest");
       String sortOrderIncreasingString = dsElem.getAttributeValue("sortOrderIncreasing");
       boolean sortOrderIncreasing = false;
       if (sortOrderIncreasingString != null)
         if (sortOrderIncreasingString.equalsIgnoreCase("true"))
           sortOrderIncreasing = true;
       boolean addDatasetSize = true;
       if (addDatasetSizeString != null)
         if (addDatasetSizeString.equalsIgnoreCase("false"))
           addDatasetSize = false;

       if (path != null) {
         if (path.charAt(0) == '/') path = path.substring(1);
         int last = path.length() - 1;
         if (path.charAt(last) == '/') path = path.substring(0, last);
       }

       if (scanDir != null) {
         int last = scanDir.length() - 1;
         if (scanDir.charAt(last) != '/') scanDir = scanDir + '/';
       }

       Element atcElem = dsElem.getChild("addTimeCoverage", defNS);
       String dsNameMatchPattern = null;
       String startTimeSubstitutionPattern = null;
       String duration = null;
       if (atcElem != null) {
         dsNameMatchPattern = atcElem.getAttributeValue("datasetNameMatchPattern");
         startTimeSubstitutionPattern = atcElem.getAttributeValue("startTimeSubstitutionPattern");
         duration = atcElem.getAttributeValue("duration");
       }

       try {
         datasetScan = new InvDatasetScan(catalog, parent, name, path, scanDir, filter, addDatasetSize, addLatest, sortOrderIncreasing,
                 dsNameMatchPattern, startTimeSubstitutionPattern, duration);
         readDatasetInfo(catalog, datasetScan, dsElem, base);
         if (InvCatalogFactory.debugXML) System.out.println(" Dataset added: " + datasetScan.dump());

       } catch (Exception e) {
         logger.error("Reading DatasetScan", e);
         datasetScan = null;
       }
     }

     return datasetScan;
   }

   protected InvDatasetScan readDatasetScanNew(InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {
     String name = dsElem.getAttributeValue("name");
     String path = dsElem.getAttributeValue("path");

     String scanDir = expandAliasForPath(dsElem.getAttributeValue("location"));

     // Read datasetConfig element
     String configClassName = null;
     Object configObj = null;
     Element dsConfigElem = dsElem.getChild("crawlableDatasetImpl", defNS);
     if (dsConfigElem != null) {
       configClassName = dsConfigElem.getAttributeValue("className");
       List children = dsConfigElem.getChildren();
       if (children.size() == 1) {
         configObj = children.get(0);
       } else if (children.size() != 0) {
         logger.warn("readDatasetScanNew(): content of datasetConfig element not a single element, using first element.");
         configObj = children.get(0);
       } else {
         logger.debug("readDatasetScanNew(): datasetConfig element has no children.");
         configObj = null;
       }
     }

     // Read filter element
     Element filterElem = dsElem.getChild("filter", defNS);
     CrawlableDatasetFilter filter = null;
     if (filterElem != null)
       filter = readDatasetScanFilter(filterElem);

     // Read identifier element
     Element identifierElem = dsElem.getChild("addID", defNS);
     CrawlableDatasetLabeler identifier = null;
     if (identifierElem != null) {
       identifier = readDatasetScanIdentifier(identifierElem);
     }

     // Read namer element
     Element namerElem = dsElem.getChild("namer", defNS);
     CrawlableDatasetLabeler namer = null;
     if (namerElem != null) {
       namer = readDatasetScanNamer(namerElem);
     }

     // Read sort element
     Element sorterElem = dsElem.getChild("sort", defNS);
     // By default, sort in decreasing lexigraphic order.
     CrawlableDatasetSorter sorter = new LexigraphicByNameSorter(false);
     if (sorterElem != null) {
       sorter = readDatasetScanSorter(sorterElem);
     }

     // Read allProxies element (and addLatest element)
     Element addLatestElem = dsElem.getChild("addLatest", defNS);
     Element addProxiesElem = dsElem.getChild("addProxies", defNS);
     Map<String, ProxyDatasetHandler> allProxyDsHandlers;
     if (addLatestElem != null || addProxiesElem != null)
       allProxyDsHandlers = readDatasetScanAddProxies(addProxiesElem, addLatestElem, catalog);
     else
       allProxyDsHandlers = new HashMap<>();

     // Read addDatasetSize element.
     Element addDsSizeElem = dsElem.getChild("addDatasetSize", defNS);
     //boolean addDatasetSize = false; old way
     //if ( addDsSizeElem != null )
     //  addDatasetSize = true;
     boolean addDatasetSize = true;
     if (addDsSizeElem != null) {
       if (addDsSizeElem.getTextNormalize().equalsIgnoreCase("false"))
         addDatasetSize = false;
     }

     // Read addTimeCoverage element.
     List<DatasetEnhancer> childEnhancerList = new ArrayList<>();
     Element addTimeCovElem = dsElem.getChild("addTimeCoverage", defNS);
     if (addTimeCovElem != null) {
       DatasetEnhancer addTimeCovEnhancer = readDatasetScanAddTimeCoverage(addTimeCovElem);
       if (addTimeCovEnhancer != null)
         childEnhancerList.add(addTimeCovEnhancer);
     }

     // Read datasetEnhancerImpl elements (user defined implementations of DatasetEnhancer)
     List<Element> dsEnhancerElemList = dsElem.getChildren("datasetEnhancerImpl", defNS);
     for (Element elem : dsEnhancerElemList) {
       DatasetEnhancer o = readDatasetScanUserDefined(elem, DatasetEnhancer.class);
       if (o != null)
         childEnhancerList.add(o);
     }

     // Read catalogRefExpander element
 //    Element catRefExpanderElem = dsElem.getChild( "catalogRefExpander", defNS );
     CatalogRefExpander catalogRefExpander = null;
 //    if ( catRefExpanderElem != null )
 //    {
 //      catalogRefExpander = readDatasetScanCatRefExpander( catRefExpanderElem );
 //    }


     InvDatasetScan datasetScan;
     try {
       datasetScan = new InvDatasetScan(parent, name, path, scanDir,
               configClassName, configObj,
               filter, identifier, namer,
               addDatasetSize, sorter, allProxyDsHandlers,
               childEnhancerList,
               catalogRefExpander);
       readDatasetInfo(catalog, datasetScan, dsElem, base);
       if (InvCatalogFactory.debugXML) System.out.println(" Dataset added: " + datasetScan.dump());

     } catch (Exception e) {
       logger.error("readDatasetScanNew(): failed to create DatasetScan", e);
       datasetScan = null;
     }

     return datasetScan;
   }

   CrawlableDatasetFilter readDatasetScanFilter(Element filterElem) {
     CrawlableDatasetFilter filter = null;  //lastModifiedLimit

     // Handle LastModifiedLimitFilter CrDsFilters.
     Attribute lastModLimitAtt = filterElem.getAttribute("lastModifiedLimit");
     if (lastModLimitAtt != null) {
       long lastModLimit;
       try {
         lastModLimit = lastModLimitAtt.getLongValue();
       } catch (DataConversionException e) {
         String tmpMsg = "readDatasetScanFilter(): bad lastModifedLimit value <" + lastModLimitAtt.getValue() + ">, couldn't parse into long: " + e.getMessage();
         factory.appendErr(tmpMsg);
         logger.warn(tmpMsg);
         return null;
       }
       return new LastModifiedLimitFilter(lastModLimit);
     }

     // Handle LogicalFilterComposer CrDsFilters.
     String compType = filterElem.getAttributeValue("logicalComp");
     if (compType != null) {
       List filters = filterElem.getChildren("filter", defNS);
       if (compType.equalsIgnoreCase("AND")) {
         if (filters.size() != 2) {
           String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for AND (2 expected).";
           factory.appendErr(tmpMsg);
           logger.warn(tmpMsg);
           return null;
         }
         filter = LogicalFilterComposer.getAndFilter(
                 readDatasetScanFilter((Element) filters.get(0)),
                 readDatasetScanFilter((Element) filters.get(1)));
       } else if (compType.equalsIgnoreCase("OR")) {
         if (filters.size() != 2) {
           String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for OR (2 expected).";
           factory.appendErr(tmpMsg);
           logger.warn(tmpMsg);
           return null;
         }
         filter = LogicalFilterComposer.getOrFilter(
                 readDatasetScanFilter((Element) filters.get(0)),
                 readDatasetScanFilter((Element) filters.get(1)));
       } else if (compType.equalsIgnoreCase("NOT")) {
         if (filters.size() != 1) {
           String tmpMsg = "readDatasetScanFilter(): wrong number of filters <" + filters.size() + "> for NOT (1 expected).";
           factory.appendErr(tmpMsg);
           logger.warn(tmpMsg);
           return null;
         }
         filter = LogicalFilterComposer.getNotFilter(
                 readDatasetScanFilter((Element) filters.get(0)));
       }

       return filter;
     }

     // Handle user defined CrDsFilters.
     Element userDefElem = filterElem.getChild("crawlableDatasetFilterImpl", defNS);
     if (userDefElem != null) {
       filter = (CrawlableDatasetFilter) readDatasetScanUserDefined(userDefElem, CrawlableDatasetFilter.class);
     }

     // Handle MultiSelectorFilter and contained Selectors.
     else {
       List<MultiSelectorFilter.Selector> selectorList = new ArrayList<>();
       for (Element curElem : filterElem.getChildren()) {
         String regExpAttVal = curElem.getAttributeValue("regExp");
         String wildcardAttVal = curElem.getAttributeValue("wildcard");
         String lastModLimitAttVal = curElem.getAttributeValue("lastModLimitInMillis");
         if (regExpAttVal == null && wildcardAttVal == null && lastModLimitAttVal == null) {
           // If no regExp or wildcard attributes, skip this selector.
           logger.warn("readDatasetScanFilter(): no regExp, wildcard, or lastModLimitInMillis attribute in filter child <" + curElem.getName() + ">.");
         } else {
           // Determine if applies to atomic datasets, default true.
           boolean atomic = true;
           String atomicAttVal = curElem.getAttributeValue("atomic");
           if (atomicAttVal != null) {
             // If not "true", set to false.
             if (!atomicAttVal.equalsIgnoreCase("true"))
               atomic = false;
           }
           // Determine if applies to collection datasets, default false.
           boolean collection = false;
           String collectionAttVal = curElem.getAttributeValue("collection");
           if (collectionAttVal != null) {
             // If not "false", set to true.
             if (!collectionAttVal.equalsIgnoreCase("false"))
               collection = true;
           }

           // Determine if include or exclude selectors.
           boolean includer = true;
           if (curElem.getName().equals("exclude")) {
             includer = false;
           } else if (!curElem.getName().equals("include")) {
             logger.warn("readDatasetScanFilter(): unhandled filter child <" + curElem.getName() + ">.");
             continue;
           }

           // Determine if regExp or wildcard
           if (regExpAttVal != null) {
             selectorList.add(new MultiSelectorFilter.Selector(new RegExpMatchOnNameFilter(regExpAttVal), includer, atomic, collection));
           } else if (wildcardAttVal != null) {
             selectorList.add(new MultiSelectorFilter.Selector(new WildcardMatchOnNameFilter(wildcardAttVal), includer, atomic, collection));
           } else if (lastModLimitAttVal != null) {
             selectorList.add(new MultiSelectorFilter.Selector(new LastModifiedLimitFilter(Long.parseLong(lastModLimitAttVal)), includer, atomic, collection));
           }
         }
       }
       filter = new MultiSelectorFilter(selectorList);
     }

     return filter;
   }

   protected CrawlableDatasetLabeler readDatasetScanIdentifier(Element identifierElem) {
     CrawlableDatasetLabeler identifier;
     Element userDefElem = identifierElem.getChild("crawlableDatasetLabelerImpl", defNS);
     if (userDefElem != null) {
       identifier = (CrawlableDatasetLabeler) readDatasetScanUserDefined(userDefElem, CrawlableDatasetLabeler.class);
     } else {
       // Default is to add ID in standard way. Don't have alternates yet.
       return null;
     }

     return identifier;
   }

   protected CrawlableDatasetLabeler readDatasetScanNamer(Element namerElem) {
     CrawlableDatasetLabeler namer;
 //    Element userDefElem = namerElem.getChild( "crawlableDatasetLabelerImpl", defNS );
 //    if ( userDefElem != null )
 //    {
 //      namer = (CrawlableDatasetLabeler) readDatasetScanUserDefined( userDefElem, CrawlableDatasetLabeler.class );
 //    }
 //    else
 //    {
     List<CrawlableDatasetLabeler> labelerList = new ArrayList<>();
     for (Element curElem : namerElem.getChildren()) {
       CrawlableDatasetLabeler curLabeler;

       String regExp = curElem.getAttributeValue("regExp");
       String replaceString = curElem.getAttributeValue("replaceString");
       if (curElem.getName().equals("regExpOnName")) {
         curLabeler = new RegExpAndReplaceOnNameLabeler(regExp, replaceString);
       } else if (curElem.getName().equals("regExpOnPath")) {
         curLabeler = new RegExpAndReplaceOnPathLabeler(regExp, replaceString);
       } else {
         logger.warn("readDatasetScanNamer(): unhandled namer child <" + curElem.getName() + ">.");
         continue;
       }
       labelerList.add(curLabeler);
     }
     namer = new MultiLabeler(labelerList);
 //    }

     return namer;
   }

   protected CrawlableDatasetSorter readDatasetScanSorter(Element sorterElem) {
     CrawlableDatasetSorter sorter = null;
     Element userDefElem = sorterElem.getChild("crawlableDatasetSorterImpl", defNS);
     if (userDefElem != null) {
       sorter = (CrawlableDatasetSorter) readDatasetScanUserDefined(userDefElem, CrawlableDatasetSorter.class);
     } else {
       Element lexSortElem = sorterElem.getChild("lexigraphicByName", defNS);
       if (lexSortElem != null) {
         boolean increasing;
         String increasingString = lexSortElem.getAttributeValue("increasing");
         increasing = increasingString.equalsIgnoreCase("true");
         sorter = new LexigraphicByNameSorter(increasing);
       }
     }

     return sorter;
   }

   protected Map<String, ProxyDatasetHandler> readDatasetScanAddProxies(Element addProxiesElem, Element addLatestElem, InvCatalogImpl catalog) {
     Map<String, ProxyDatasetHandler> allProxyDsHandlers = new HashMap<>();

     // Handle old "addLatest" elements.
     if (addLatestElem != null) {
       // Check for simpleLatest element.
       Element simpleLatestElem = addLatestElem.getChild("simpleLatest", defNS);
       // Get a SimpleLatestDsHandler, use default values if element is null.
       ProxyDatasetHandler pdh = readDatasetScanAddLatest(simpleLatestElem, catalog);
       if (pdh != null)
         allProxyDsHandlers.put(pdh.getProxyDatasetName(), pdh);
     }

     // Handle all "addProxies" elements.
     if (addProxiesElem != null) {
       for (Element curChildElem : addProxiesElem.getChildren()) {
         ProxyDatasetHandler curPdh;

         // Handle "simpleLatest" child elements.
         if (curChildElem.getName().equals("simpleLatest")
                 && curChildElem.getNamespace().equals(defNS)) {
           curPdh = readDatasetScanAddLatest(curChildElem, catalog);
         }

         // Handle "latestComplete" child elements.
         else if (curChildElem.getName().equals("latestComplete")
                 && curChildElem.getNamespace().equals(defNS)) {
           // Get latest name.
           String latestName = curChildElem.getAttributeValue("name");
           if (latestName == null) {
             logger.warn("readDatasetScanAddProxies(): unnamed latestComplete, skipping.");
             continue;
           }

           // Does latest go on top or bottom of list.
           Attribute topAtt = curChildElem.getAttribute("top");
           boolean latestOnTop = true;
           if (topAtt != null) {
             try {
               latestOnTop = topAtt.getBooleanValue();
             } catch (DataConversionException e) {
               latestOnTop = true;
             }
           }

           // Get the latest service name.
           String serviceName = curChildElem.getAttributeValue("serviceName");
           if (serviceName == null) {
             logger.warn("readDatasetScanAddProxies(): no service name given in latestComplete.");
             continue;
           }
           InvService service = catalog.findService(serviceName);
           if (service == null) {
             logger.warn("readDatasetScanAddProxies(): named service <" + serviceName + "> not found.");
             continue;
           }

           // Get lastModifed limit.
           String lastModLimitVal = curChildElem.getAttributeValue("lastModifiedLimit");
           long lastModLimit;
           if (lastModLimitVal == null)
             lastModLimit = 60; // Default to one hour
           else
             lastModLimit = Long.parseLong(lastModLimitVal);

           // Get isResolver.
           String isResolverString = curChildElem.getAttributeValue("isResolver");
           boolean isResolver = true;
           if (isResolverString != null)
             if (isResolverString.equalsIgnoreCase("false"))
               isResolver = false;

           // Build the SimpleLatestProxyDsHandler and add to map.
           curPdh = new LatestCompleteProxyDsHandler(latestName, latestOnTop, service, isResolver, lastModLimit);
         } else {
           curPdh = null;
           // @todo Deal with allowing user defined inserters
           //Element userDefElem = addLatestElem.getChild( "proxyDatasetHandlerImpl", defNS );
         }

         // Add current proxy dataset handler to map if name is not already in map.
         if (curPdh != null) {
           if (allProxyDsHandlers.containsKey(curPdh.getProxyDatasetName())) {
             logger.warn("readDatasetScanAddProxies(): proxy map already contains key <" + curPdh.getProxyDatasetName() + ">, skipping.");
             continue;
           }
           allProxyDsHandlers.put(curPdh.getProxyDatasetName(), curPdh);
         }
       }
     }

     return allProxyDsHandlers;
   }

   protected DateType readDate(Element elem) {
     if (elem == null) return null;
     String format = elem.getAttributeValue("format");
     String type = elem.getAttributeValue("type");
     return makeDateType(elem.getText(), format, type);
   }

   protected DateType makeDateType(String text, String format, String type) {
     if (text == null) return null;
     try {
       return new DateType(text, format, type);
     } catch (java.text.ParseException e) {
       factory.appendErr(" ** Parse error: Bad date format = " + text + "\n");
       return null;
     }
   }

   protected TimeDuration readDuration(Element elem) {
     if (elem == null) return null;
     String text = null;
     try {
       text = elem.getText();
       return new TimeDuration(text);
     } catch (java.text.ParseException e) {
       factory.appendErr(" ** Parse error: Bad duration format = " + text + "\n");
       return null;
     }
   }

   protected InvDocumentation readDocumentation(InvCatalog cat, Element s) {
     String href = s.getAttributeValue("href", xlinkNS);
     String title = s.getAttributeValue("title", xlinkNS);
     String type = s.getAttributeValue("type"); // not XLink type
     String content = s.getTextNormalize();

     URI uri = null;
     if (href != null) {
       try {
         uri = cat.resolveUri(href);
       } catch (Exception e) {
         factory.appendErr(" ** Invalid documentation href = " + href + " " + e.getMessage() + "\n");
       }
     }

     InvDocumentation doc = new InvDocumentation(href, uri, title, type, content);

     // LOOK XHTML ?? !!

     if (InvCatalogFactory.debugXML) System.out.println(" Documentation added: " + doc);
     return doc;
   }

   protected double readDouble(Element elem) {
     if (elem == null) return Double.NaN;
     String text = elem.getText();
     try {
       return Double.parseDouble(text);
     } catch (NumberFormatException e) {
       factory.appendErr(" ** Parse error: Bad double format = " + text + "\n");
       return Double.NaN;
     }
   }

   protected ThreddsMetadata.GeospatialCoverage readGeospatialCoverage(Element gcElem) {
     if (gcElem == null) return null;

     String zpositive = gcElem.getAttributeValue("zpositive");

     ThreddsMetadata.Range northsouth = readGeospatialRange(gcElem.getChild("northsouth", defNS), CDM.LAT_UNITS);
     ThreddsMetadata.Range eastwest = readGeospatialRange(gcElem.getChild("eastwest", defNS), CDM.LON_UNITS);
     ThreddsMetadata.Range updown = readGeospatialRange(gcElem.getChild("updown", defNS), "m");

     // look for names
     List<ThreddsMetadata.Vocab> names = new ArrayList<>();
     java.util.List<Element> list = gcElem.getChildren("name", defNS);
     for (Element e : list) {
       ThreddsMetadata.Vocab name = readControlledVocabulary(e);
       names.add(name);
     }

     return new ThreddsMetadata.GeospatialCoverage(eastwest, northsouth, updown, names, zpositive);
   }

   protected ThreddsMetadata.Range readGeospatialRange(Element spElem, String defUnits) {
     if (spElem == null) return null;

     double start = readDouble(spElem.getChild("start", defNS));
     double size = readDouble(spElem.getChild("size", defNS));
     double resolution = readDouble(spElem.getChild("resolution", defNS));

     String units = spElem.getChildText("units", defNS);
     if (units == null) units = defUnits;

     return new ThreddsMetadata.Range(start, size, resolution, units);
   }

   protected InvMetadata readMetadata(InvCatalog catalog, InvDatasetImpl dataset, Element mdataElement) {
     // there are 6 cases to deal with: threddsNamespace vs not & inline vs Xlink & hasConverter or not
     // (the hasConverter only applies when its not threddsNamespace, giving 6 cases)
     // this factory is the converter for threddsNamespace metadata
     //  and also handles non-threddsNamespace when there is no converter, in which case it just
     //   propagates the inline dom elements

     // figure out the namespace
     Namespace namespace;
     List inlineElements = mdataElement.getChildren();
     if (inlineElements.size() > 0) // look at the namespace of the children, if they exist
       namespace = ((Element) inlineElements.get(0)).getNamespace();
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
     MetadataConverterIF metaConverter = factory.getMetadataConverter(namespace.getURI());
     if (metaConverter == null) metaConverter = factory.getMetadataConverter(mtype);
     if (metaConverter != null) {
       if (debugMetadataRead) System.out.println("found factory for metadata type = " + mtype + " namespace = " +
               namespace + "=" + metaConverter.getClass().getName());

       // see if theres any inline content
       Object contentObj;
       if (inlineElements.size() > 0) {
         contentObj = metaConverter.readMetadataContent(dataset, mdataElement);
         return new InvMetadata(dataset, mtype, namespace.getURI(), namespace.getPrefix(),
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
         return new InvMetadata(dataset, mtype, namespace.getURI(), namespace.getPrefix(),
                 inherited, false, this, mdataElement);

       } else { // otherwise it must be an Xlink, never read
         return new InvMetadata(dataset, href, title, mtype, namespace.getURI(),
                 namespace.getPrefix(), inherited, false, null);
       }

     }

     // the case where its ThreddsMetadata
     if (inlineElements.size() > 0) {
       ThreddsMetadata tmg = new ThreddsMetadata(false);
       readThreddsMetadata(catalog, dataset, mdataElement, tmg);
       return new InvMetadata(dataset, mtype, namespace.getURI(), namespace.getPrefix(),
               inherited, true, this, tmg);

     } else { // otherwise it  must be an Xlink; defer reading
       return new InvMetadata(dataset, href, title, mtype, namespace.getURI(),
               namespace.getPrefix(), inherited, true, this);
     }

   }

   public Object readMetadataContent(InvDataset dataset, org.jdom2.Element mdataElement) {
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
   public Object readMetadataContentFromURL(InvDataset dataset, java.net.URI uri) throws java.io.IOException {
     Element elem = readContentFromURL(uri);
     Object contentObject = readMetadataContent(dataset, elem);
     if (debugMetadataRead) System.out.println(" convert to " + contentObject.getClass().getName());
     return contentObject;
   }

     /* open and read the referenced catalog XML
     if (debugMetadataRead) System.out.println(" readMetadataContentFromURL = " + url);
     org.w3c.dom.Element mdataElement = factory.readOtherXML( url);
     if (mdataElement == null) {
       factory.appendErr(" ** failed to read thredds metadata at = "+url+" for dataset"+dataset.getName()+"\n");
       return null;
     }

     Object contentObject = readMetadataContent( dataset, mdataElement);
     if (debugMetadataRead) System.out.println(" convert to " + contentObject.getClass().getName());
  return contentObject;

   // dummy LOOK
   public boolean validateMetadataContent(Object contentObject, StringBuilder out) {
     return true;
   }

   public void addMetadataContent(org.jdom2.Element mdataElement, Object contentObject) {
   }


   protected ThreddsMetadata.Source readSource(Element elem) {
     if (elem == null) return null;
     ThreddsMetadata.Vocab name = readControlledVocabulary(elem.getChild("name", defNS));
     Element contact = elem.getChild("contact", defNS);
     if (contact == null) {
       factory.appendErr(" ** Parse error: Missing contact element in = " + elem.getName() + "\n");
       return null;
     }
     return new ThreddsMetadata.Source(name, contact.getAttributeValue("url"), contact.getAttributeValue("email"));
   }



   protected DateRange readTimeCoverage(Element tElem) {
     if (tElem == null) return null;

     DateType start = readDate(tElem.getChild("start", defNS));
     DateType end = readDate(tElem.getChild("end", defNS));
     TimeDuration duration = readDuration(tElem.getChild("duration", defNS));
     TimeDuration resolution = readDuration(tElem.getChild("resolution", defNS));

     try {
       return new DateRange(start, end, duration, resolution);
     } catch (java.lang.IllegalArgumentException e) {
       factory.appendWarning(" ** warning: TimeCoverage error = " + e.getMessage() + "\n");
       return null;
     }
   }

   protected void readThreddsMetadata(InvCatalog catalog, InvDatasetImpl dataset, Element parent, ThreddsMetadata tmg) {
     List<Element> list;

     // look for creators - kind of a Source
     list = parent.getChildren("creator", defNS);
     for (Element e : list) {
       tmg.addCreator(readSource(e));
     }

     // look for contributors
     list = parent.getChildren("contributor", defNS);
     for (Element e : list) {
       tmg.addContributor(readContributor(e));
     }

     // look for dates
     list = parent.getChildren("date", defNS);
     for (Element e : list) {
       DateType d = readDate(e);
       tmg.addDate(d);
     }

     // look for documentation
     list = parent.getChildren("documentation", defNS);
     for (Element e : list) {
       InvDocumentation doc = readDocumentation(catalog, e);
       tmg.addDocumentation(doc);
     }

     // look for keywords - kind of a controlled vocabulary
     list = parent.getChildren("keyword", defNS);
     for (Element e : list) {
       tmg.addKeyword(readControlledVocabulary(e));
     }

     // look for metadata
     java.util.List<Element> mList = parent.getChildren("metadata", defNS);
     for (Element e : mList) {
       InvMetadata m = readMetadata(catalog, dataset, e);
       if (m != null) {
         tmg.addMetadata(m);
       }
     }

     // look for projects - kind of a controlled vocabulary
     list = parent.getChildren("project", defNS);
     for (Element e : list) {
       tmg.addProject(readControlledVocabulary(e));
     }

     // look for properties
     list = parent.getChildren("property", defNS);
     for (Element e : list) {
       InvProperty p = readProperty(e);
       tmg.addProperty(p);
     }

     // look for publishers - kind of a Source
     list = parent.getChildren("publisher", defNS);
     for (Element e : list) {
       tmg.addPublisher(readSource(e));
     }

     // look for variables
     list = parent.getChildren("variables", defNS);
     for (Element e : list) {
       ThreddsMetadata.Variables vars = readVariables(catalog, dataset, e);
       tmg.addVariables(vars);
     }

     // can only be one each of these kinds
     ThreddsMetadata.GeospatialCoverage gc = readGeospatialCoverage(parent.getChild("geospatialCoverage", defNS));
     if (gc != null) tmg.setGeospatialCoverage(gc);

     DateRange tc = readTimeCoverage(parent.getChild("timeCoverage", defNS));
     if (tc != null) tmg.setTimeCoverage(tc);

     Element serviceNameElem = parent.getChild("serviceName", defNS);
     if (serviceNameElem != null) tmg.setServiceName(serviceNameElem.getText());

     Element authElem = parent.getChild("authority", defNS);
     if (authElem != null) tmg.setAuthority(authElem.getText());

     Element dataTypeElem = parent.getChild("dataType", defNS);
     if (dataTypeElem != null) {
       String dataTypeName = dataTypeElem.getText();
       if ((dataTypeName != null) && (dataTypeName.length() > 0)) {
         FeatureType dataType = FeatureType.getType(dataTypeName.toUpperCase());
         if (dataType == null) {
           factory.appendWarning(" ** warning: non-standard data type = " + dataTypeName + "\n");
         }
         tmg.setDataType(dataType);
       }
     }

     Element dataFormatElem = parent.getChild("dataFormat", defNS);
     if (dataFormatElem != null) {
       String dataFormatTypeName = dataFormatElem.getText();
       if ((dataFormatTypeName != null) && (dataFormatTypeName.length() > 0)) {
         DataFormatType dataFormatType = DataFormatType.findType(dataFormatTypeName);
         if (dataFormatType == null) {
           dataFormatType = DataFormatType.getType(dataFormatTypeName);
           factory.appendWarning(" ** warning: non-standard dataFormat type = " + dataFormatTypeName + "\n");
         }
         tmg.setDataFormatType(dataFormatType);
       }
     }

     double size = readDataSize(parent);
     if (!Double.isNaN(size))
       tmg.setDataSize(size);
   }

   protected ThreddsMetadata.Variable readVariable(Element varElem) {
     if (varElem == null) return null;

     String name = varElem.getAttributeValue("name");
     String desc = varElem.getText();
     String vocabulary_name = varElem.getAttributeValue("vocabulary_name");
     String units = varElem.getAttributeValue("units");
     String id = varElem.getAttributeValue("vocabulary_id");

     return new ThreddsMetadata.Variable(name, desc, vocabulary_name, units, id);
   }


   protected ThreddsMetadata.Variables readVariables(InvCatalog cat, InvDataset ds, Element varsElem) {
     if (varsElem == null) return null;

     String vocab = varsElem.getAttributeValue("vocabulary");
     String vocabHref = varsElem.getAttributeValue("href", xlinkNS);

     URI vocabUri = null;
     if (vocabHref != null) {
       try {
         vocabUri = cat.resolveUri(vocabHref);
       } catch (Exception e) {
         factory.appendErr(" ** Invalid Variables vocabulary URI = " + vocabHref + " " + e.getMessage() + "\n");
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
         factory.appendErr(" ** Invalid Variables map URI = " + mapHref + " " + e.getMessage() + "\n");
       }
     }

     if ((mapUri != null) && vlist.size() > 0) { // cant do both
       factory.appendErr(" ** Catalog error: cant have variableMap and variable in same element (dataset = " +
               ds.getName() + "\n");
       mapUri = null;
     }

     ThreddsMetadata.Variables variables = new ThreddsMetadata.Variables(vocab, vocabHref, vocabUri, mapHref, mapUri);

     for (Element e : vlist) {
       ThreddsMetadata.Variable v = readVariable(e);
       variables.addVariable(v);
     }

     // read in variable map LOOK: would like to defer
     if (mapUri != null) {
       Element varsElement;
       try {
         varsElement = readContentFromURL(mapUri);
         List<Element> list = varsElement.getChildren("variable", defNS);
         for (Element e : list) {
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
       }

     }

     return variables;
   } */
}
