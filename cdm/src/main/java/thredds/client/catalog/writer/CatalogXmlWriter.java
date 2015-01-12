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
package thredds.client.catalog.writer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

import thredds.client.catalog.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/11/2015
 */
public class CatalogXmlWriter {
  static private boolean useBytesForDataSize = false;
  static public void useBytesForDataSize(boolean b) {
    useBytesForDataSize = b;
  }

  static private final String version = "1.0.7";

  /************************************************************************/
  // Writing XML from objects

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os      write to this OutputStream
   * @param raw     write raw file if true (for server configuration)
   * @throws java.io.IOException
   */
  public void writeXML(Catalog catalog, OutputStream os, boolean raw) throws IOException {
    this.raw = raw;
    writeXML(catalog, os);
    this.raw = false;
  }

  private boolean raw = false;

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os      write to this OutputStream
   * @throws IOException
   */
  public void writeXML(Catalog catalog, OutputStream os) throws IOException {
    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter();
    //fmt.setNewlines(true);
    //fmt.setIndent("  ");
    //fmt.setTrimAllWhite( true);
    XMLOutputter fmt = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());  // LOOK maybe compact ??
    fmt.output(writeCatalog(catalog), os);
  }

  public Document writeCatalog(Catalog cat) {
    Element rootElem = new Element("catalog", Catalog.defNS);
    Document doc = new Document(rootElem);

    // attributes
    if (cat.getName() != null)
      rootElem.setAttribute("name", cat.getName());
    rootElem.setAttribute("version", version);
    rootElem.addNamespaceDeclaration(Catalog.xlinkNS);
    if (cat.getExpires() != null)
      rootElem.setAttribute("expires", cat.getExpires().toString());

    // services
    Iterator iter = cat.getServices().iterator();
    while (iter.hasNext()) {
      Service service = (Service) iter.next();
      rootElem.addContent(writeService(service));
    }

    /* dataset roots
    if (raw) {
      iter = cat.getDatasetRoots().iterator();
      while (iter.hasNext()) {
        Property p = (Property) iter.next();
        rootElem.addContent(writeDatasetRoot(p));
      }
    } */

    // properties
    iter = cat.getProperties().iterator();
    while (iter.hasNext()) {
      Property p = (Property) iter.next();
      rootElem.addContent(writeProperty(p));
    }

    // datasets
    iter = cat.getDatasets().iterator();
    while (iter.hasNext()) {
      Dataset ds = (Dataset) iter.next();
      //if (ds instanceof DatasetScan)
      //  rootElem.addContent(writeDatasetScan((DatasetScan) ds));
      if (ds instanceof CatalogRef)
        rootElem.addContent(writeCatalogRef((CatalogRef) ds));
      else
        rootElem.addContent(writeDataset(ds));
    }

    return doc;
  }

  private Element writeAccess(Access access) {
    Element accessElem = new Element("access", Catalog.defNS);
    accessElem.setAttribute("urlPath", access.getUrlPath());
    if (access.getService() != null)
      accessElem.setAttribute("serviceName", access.getService().getName());
    if (access.getDataFormatName() != null)
      accessElem.setAttribute("dataFormat", access.getDataFormatName());

    if (access.getDataSize() > 0)
      accessElem.addContent(writeDataSize(access.getDataSize()));

    return accessElem;
  }

  private Element writeCatalogRef(CatalogRef catRef) {
    Element catrefElem = new Element("catalogRef", Catalog.defNS);
    catrefElem.setAttribute("href", catRef.getXlinkHref(), Catalog.xlinkNS);
    String name = catRef.getName() == null ? "" : catRef.getName();
    catrefElem.setAttribute("title", name, Catalog.xlinkNS);
    if (catRef.getId() != null)
      catrefElem.setAttribute("ID", catRef.getId());
    if (catRef.getResourceControl() != null)
      catrefElem.setAttribute("restrictAccess", catRef.getResourceControl());
    catrefElem.setAttribute("name", "");
    return catrefElem;
  }

  protected Element writeContributor(ThreddsMetadata.Contributor c) {
    Element elem = new Element("contributor", Catalog.defNS);
    if (c.getRole() != null)
      elem.setAttribute("role", c.getRole());
    elem.setText(c.getName());
    return elem;
  }

  private Element writeControlledVocabulary(ThreddsMetadata.Vocab v, String name) {
    Element elem = new Element(name, Catalog.defNS);
    if (v.getVocabulary() != null)
      elem.setAttribute("vocabulary", v.getVocabulary());
    elem.addContent(v.getText());
    return elem;
  }

  private Element writeDataset(Dataset ds) {
    Element dsElem = new Element("dataset", Catalog.defNS);

    /* if (ds instanceof DatasetProxy) {
      dsElem.setAttribute("name", ((DatasetProxy) ds).getAliasName());
      dsElem.setAttribute("alias", ds.getID());
      return dsElem;
    } */

    writeDatasetInfo(ds, dsElem, true, raw);

    return dsElem;
  }

  private Element writeDatasetRoot(Property prop) {
    Element drootElem = new Element("datasetRoot", Catalog.defNS);
    drootElem.setAttribute("path", prop.getName());
    drootElem.setAttribute("location", prop.getValue());
    return drootElem;
  }

  /* private Element writeDatasetScan(DatasetScan ds) {
    Element dsElem;

    if (raw) {
      // Setup datasetScan element
      dsElem = new Element("datasetScan", Catalog.defNS);
      writeDatasetInfo(ds, dsElem, false, true);
      dsElem.setAttribute("path", ds.getPath());
      dsElem.setAttribute("location", ds.getScanLocation());

      // Write datasetConfig element
      if (ds.getCrDsClassName() != null) {
        Element configElem = new Element("crawlableDatasetImpl", Catalog.defNS);
        configElem.setAttribute("className", ds.getCrDsClassName());
        if (ds.getCrDsConfigObj() != null) {
          if (ds.getCrDsConfigObj() instanceof Element) {
            configElem.addContent((Element) ds.getCrDsConfigObj());
          }
        }
      }

      // Write filter element
      if (ds.getFilter() != null)
        dsElem.addContent(writeDatasetScanFilter(ds.getFilter()));

      // Write addID element
      //if ( ds.getIdentifier() != null )
      dsElem.addContent(writeDatasetScanIdentifier(ds.getIdentifier()));

      // Write namer element
      if (ds.getNamer() != null)
        dsElem.addContent(writeDatasetScanNamer(ds.getNamer()));

      // Write sort element
      if (ds.getSorter() != null)
        dsElem.addContent(writeDatasetScanSorter(ds.getSorter()));

      // Write addProxy element (and old addLatest element)
      if (!ds.getProxyDatasetHandlers().isEmpty())
        dsElem.addContent(writeDatasetScanAddProxies(ds.getProxyDatasetHandlers()));

      // Write addDatasetSize element
      if (ds.getAddDatasetSize())
        dsElem.addContent(new Element("addDatasetSize", Catalog.defNS));

      // Write addTimeCoverage and datasetEnhancerImpl elements
      if (ds.getChildEnhancerList() != null)
        dsElem.addContent(writeDatasetScanEnhancer(ds.getChildEnhancerList()));

      // @todo Write catalogRefExpander elements
//      if ( ds.getCatalogRefExpander() != null )
//        dsElem.addContent( writeDatasetScanCatRefExpander( ds.getCatalogRefExpander()));
    } else {
      if (ds.isValid()) {
        dsElem = new Element("catalogRef", Catalog.defNS);
        writeDatasetInfo(ds, dsElem, false, false);
        dsElem.setAttribute("href", ds.getXlinkHref(), Catalog.xlinkNS);
        dsElem.setAttribute("title", ds.getName(), Catalog.xlinkNS);
        dsElem.setAttribute("name", "");
        dsElem.addContent(writeProperty(new Property("DatasetScan", "true")));
      } else {
        dsElem = new Element("dataset", Catalog.defNS);
        dsElem.setAttribute("name", "** Misconfigured DatasetScan <" + ds.getPath() + "> **");
        dsElem.addContent(new Comment(ds.getInvalidMessage()));
      }
    }

    return dsElem;
  }

  Element writeDatasetScanFilter(CrawlableDatasetFilter filter) {
    Element filterElem = new Element("filter", Catalog.defNS);
    if (filter.getClass().isAssignableFrom(MultiSelectorFilter.class) && filter.getConfigObject() != null) {
      for (Object o : ((List) filter.getConfigObject())) {
        MultiSelectorFilter.Selector curSelector = (MultiSelectorFilter.Selector) o;
        Element curSelectorElem;
        if (curSelector.isIncluder())
          curSelectorElem = new Element("include", Catalog.defNS);
        else
          curSelectorElem = new Element("exclude", Catalog.defNS);

        CrawlableDatasetFilter curFilter = curSelector.getFilter();
        if (curFilter instanceof WildcardMatchOnNameFilter) {
          curSelectorElem.setAttribute("wildcard", ((WildcardMatchOnNameFilter) curFilter).getWildcardString());
          curSelectorElem.setAttribute("atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false");
          curSelectorElem.setAttribute("collection", curSelector.isApplyToCollectionDataset() ? "true" : "false");
        } else if (curFilter instanceof RegExpMatchOnNameFilter) {
          curSelectorElem.setAttribute("regExp", ((RegExpMatchOnNameFilter) curFilter).getRegExpString());
          curSelectorElem.setAttribute("atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false");
          curSelectorElem.setAttribute("collection", curSelector.isApplyToCollectionDataset() ? "true" : "false");
        } else if (curFilter instanceof LastModifiedLimitFilter) {
          curSelectorElem.setAttribute("lastModLimitInMillis", Long.toString(((LastModifiedLimitFilter) curFilter).getLastModifiedLimitInMillis()));
          curSelectorElem.setAttribute("atomic", curSelector.isApplyToAtomicDataset() ? "true" : "false");
          curSelectorElem.setAttribute("collection", curSelector.isApplyToCollectionDataset() ? "true" : "false");
        } else
          curSelectorElem.addContent(new Comment("Unknown selector type <" + curSelector.getClass().getName() + ">."));

        filterElem.addContent(curSelectorElem);
      }
    } else {
      filterElem.addContent(writeDatasetScanUserDefined("crawlableDatasetFilterImpl", filter.getClass().getName(), filter.getConfigObject()));
    }

    return filterElem;
  }

  private Element writeDatasetScanNamer(CrawlableDatasetLabeler namer) {
    Element namerElem = null;
    if (namer != null) {
      namerElem = new Element("namer", Catalog.defNS);
      if (namer instanceof MultiLabeler) {
        for (CrawlableDatasetLabeler curNamer : ((MultiLabeler) namer).getLabelerList()) {
          Element curNamerElem;
          if (curNamer instanceof RegExpAndReplaceOnNameLabeler) {
            curNamerElem = new Element("regExpOnName", Catalog.defNS);
            curNamerElem.setAttribute("regExp", ((RegExpAndReplaceOnNameLabeler) curNamer).getRegExp());
            curNamerElem.setAttribute("replaceString", ((RegExpAndReplaceOnNameLabeler) curNamer).getReplaceString());
            namerElem.addContent(curNamerElem);
          } else if (curNamer instanceof RegExpAndReplaceOnPathLabeler) {
            curNamerElem = new Element("regExpOnPath", Catalog.defNS);
            curNamerElem.setAttribute("regExp", ((RegExpAndReplaceOnPathLabeler) curNamer).getRegExp());
            curNamerElem.setAttribute("replaceString", ((RegExpAndReplaceOnPathLabeler) curNamer).getReplaceString());
            namerElem.addContent(curNamerElem);
          } else {
            String tmpMsg = "writeDatasetScanNamer(): unsupported namer <" + curNamer.getClass().getName() + ">.";
            logger.warn(tmpMsg);
            namerElem.addContent(new Comment(tmpMsg));
          }
        }
      } else {
        namerElem.addContent(writeDatasetScanUserDefined("crawlableDatasetLabelerImpl", namer.getClass().getName(), namer.getConfigObject()));
      }
    }

    return namerElem;
  }

  private Element writeDatasetScanIdentifier(CrawlableDatasetLabeler identifier) {
    Element identifierElem = new Element("addID", Catalog.defNS);
    if (identifier != null) {
      if (identifier instanceof SimpleLatestProxyDsHandler) {
        return identifierElem;
      } else {
        identifierElem = new Element("addID", Catalog.defNS);
        identifierElem.addContent(writeDatasetScanUserDefined("crawlableDatasetLabelerImpl", identifier.getClass().getName(), identifier.getConfigObject()));
      }
    }

    return identifierElem;
  }

  private Element writeDatasetScanAddProxies(Map<String, ProxyDatasetHandler> proxyDsHandlers) {
    Element addProxiesElem;

    // Write addLatest element if only proxyDsHandler and named "latest.xml".
    if (proxyDsHandlers.size() == 1 && proxyDsHandlers.containsKey("latest.xml")) {
      Object o = proxyDsHandlers.get("latest.xml");
      if (o instanceof SimpleLatestProxyDsHandler) {
        SimpleLatestProxyDsHandler pdh = (SimpleLatestProxyDsHandler) o;
        String name = pdh.getProxyDatasetName();
        boolean top = pdh.isLocateAtTopOrBottom();
        String serviceName = pdh.getProxyDatasetService(null).getName();

        addProxiesElem = new Element("addLatest", Catalog.defNS);
        if (name.equals("latest.xml") && top && serviceName.equals("latest"))
          return addProxiesElem;
        else {
          Element simpleLatestElem = new Element("simpleLatest", Catalog.defNS);

          simpleLatestElem.setAttribute("name", name);
          simpleLatestElem.setAttribute("top", top ? "true" : "false");
          simpleLatestElem.setAttribute("servicName", serviceName);
          addProxiesElem.addContent(simpleLatestElem);
          return addProxiesElem;
        }
      }
    }

    // Write "addProxies" element
    addProxiesElem = new Element("addProxies", Catalog.defNS);
    for (Map.Entry<String, ProxyDatasetHandler> entry : proxyDsHandlers.entrySet()) {
      String curName = entry.getKey();
      ProxyDatasetHandler curPdh = entry.getValue();

      if (curPdh instanceof SimpleLatestProxyDsHandler) {
        SimpleLatestProxyDsHandler sPdh = (SimpleLatestProxyDsHandler) curPdh;

        Element simpleLatestElem = new Element("simpleLatest", Catalog.defNS);

        simpleLatestElem.setAttribute("name", sPdh.getProxyDatasetName());
        simpleLatestElem.setAttribute("top", sPdh.isLocateAtTopOrBottom() ? "true" : "false");
        simpleLatestElem.setAttribute("servicName", sPdh.getProxyDatasetService(null).getName());
        addProxiesElem.addContent(simpleLatestElem);
      } else if (curPdh instanceof LatestCompleteProxyDsHandler) {
        LatestCompleteProxyDsHandler lcPdh = (LatestCompleteProxyDsHandler) curPdh;
        Element latestElem = new Element("latestComplete", Catalog.defNS);
        latestElem.setAttribute("name", lcPdh.getProxyDatasetName());
        latestElem.setAttribute("top", lcPdh.isLocateAtTopOrBottom() ? "true" : "false");
        latestElem.setAttribute("servicName", lcPdh.getProxyDatasetService(null).getName());
        latestElem.setAttribute("lastModifiedLimit", Long.toString(lcPdh.getLastModifiedLimit()));
        addProxiesElem.addContent(latestElem);
      } else {
        logger.warn("writeDatasetScanAddProxies(): unknown type of ProxyDatasetHandler <" + curPdh.getProxyDatasetName() + ">.");
        // latestAdderElem.addContent( writeDatasetScanUserDefined( "datasetInserterImpl", latestAdder.getClass().getName(), latestAdder.getConfigObject() ) );
      }

    }
    return addProxiesElem;
  }

  private Element writeDatasetScanSorter(CrawlableDatasetSorter sorter) {
    Element sorterElem = new Element("sort", Catalog.defNS);
    if (sorter instanceof LexigraphicByNameSorter) {
      Element lexElem = new Element("lexigraphicByName", Catalog.defNS);
      lexElem.setAttribute("increasing", ((LexigraphicByNameSorter) sorter).isIncreasing() ? "true" : "false");
      sorterElem.addContent(lexElem);
    } else {
      sorterElem.addContent(writeDatasetScanUserDefined("crawlableDatasetSorterImpl", sorter.getClass().getName(), sorter.getConfigObject()));
    }

    return sorterElem;
  }

  private List<Element> writeDatasetScanEnhancer(List<DatasetEnhancer> enhancerList) {
    List<Element> enhancerElemList = new ArrayList<>();
    int timeCovCount = 0;
    for (DatasetEnhancer curEnhancer : enhancerList) {
      if (curEnhancer instanceof RegExpAndDurationTimeCoverageEnhancer) {
        if (timeCovCount > 0) {
          logger.warn("writeDatasetScanEnhancer(): More than one addTimeCoverage element, skipping.");
          continue;
        }
        timeCovCount++;
        Element timeCovElem = new Element("addTimeCoverage", Catalog.defNS);
        RegExpAndDurationTimeCoverageEnhancer timeCovEnhancer = (RegExpAndDurationTimeCoverageEnhancer) curEnhancer;
        timeCovElem.setAttribute("datasetNameMatchPattern", timeCovEnhancer.getMatchPattern());
        timeCovElem.setAttribute("startTimeSubstitutionPattern", timeCovEnhancer.getSubstitutionPattern());
        timeCovElem.setAttribute("duration", timeCovEnhancer.getDuration());

        enhancerElemList.add(timeCovElem);
      } else {
        enhancerElemList.add(writeDatasetScanUserDefined("datasetEnhancerImpl", curEnhancer.getClass().getName(), curEnhancer.getConfigObject()));
      }
    }

    return enhancerElemList;
  }

  private Element writeDatasetScanUserDefined(String userDefName, String className, Object configObj) {
    Element userDefElem = new Element(userDefName, Catalog.defNS);
    userDefElem.setAttribute("className", className);
    if (configObj != null) {
      if (configObj instanceof Element)
        userDefElem.addContent((Element) configObj);
      else
        userDefElem.addContent(new Comment("This class <" + className + "> not yet supported. This XML is missing configuration information (of type " + configObj.getClass().getName() + ")."));
    }

    return userDefElem;
  }  */

  private void writeDatasetInfo(Dataset ds, Element dsElem, boolean doNestedDatasets, boolean showNcML) {
    dsElem.setAttribute("name", ds.getName());

    // other attributes, note the others get made into an element
    if (ds.getCollectionType() != null)
      dsElem.setAttribute("collectionType", ds.getCollectionType());
    if (ds.isHarvest())
      dsElem.setAttribute("harvest", "true");
    if (ds.getID() != null)
      dsElem.setAttribute("ID", ds.getID());
    if (ds.getUrlPath() != null)
      dsElem.setAttribute("urlPath", ds.getUrlPath());
    if (ds.getResourceControl() != null)
      dsElem.setAttribute("restrictAccess", ds.getResourceControl());

    /* services (local only)
    for (Service service : ds.getServices()) {
      dsElem.addContent(writeService(service));
    } */

    // thredds metadata
    writeThreddsMetadata(dsElem, ds);
    writeInheritedMetadata(dsElem, ds);
    // writeInheritedMetadata( dsElem, ds.getCat6Metadata()); // LOOK can we get rid of this?

    // access  (local only)
    List<Access> access = (List<Access>) ds.getLocalFieldAsList(Dataset.Access);
    for (Access a : access) {
      dsElem.addContent(writeAccess( a));
    }

    /* if (showNcML && ds.getNcmlElement() != null) {
      org.jdom2.Element ncml = ds.getNcmlElement().clone();
      ncml.detach();
      dsElem.addContent(ncml);
    } */

    if (!doNestedDatasets) return;

    // nested datasets
    for (Dataset nested : ds.getDatasets()) {
     // if (nested instanceof DatasetScan)
     //   dsElem.addContent(writeDatasetScan((DatasetScan) nested));
      if (nested instanceof CatalogRef)
        dsElem.addContent(writeCatalogRef((CatalogRef) nested));
      else
        dsElem.addContent(writeDataset( nested));
    }
  }

  protected Element writeDate(String name, DateType date) {
    Element dateElem = new Element(name, Catalog.defNS);
    dateElem.addContent(date.getText());
    if (date.getType() != null)
      dateElem.setAttribute("type", date.getType());
    if (date.getFormat() != null)
      dateElem.setAttribute("format", date.getFormat());

    return dateElem;
  }

  private Element writeDocumentation(Documentation doc, String name) {
    Element docElem = new Element(name, Catalog.defNS);
    if (doc.getType() != null)
      docElem.setAttribute("type", doc.getType());

    if (doc.hasXlink()) {
      docElem.setAttribute("href", doc.getXlinkHref(), Catalog.xlinkNS);
      if (!doc.getXlinkTitle().equals(doc.getURI().toString()))
        docElem.setAttribute("title", doc.getXlinkTitle(), Catalog.xlinkNS);
    }

    String inline = doc.getInlineContent();
    if (inline != null)
      docElem.addContent(inline);
    return docElem;
  }

  public Element writeGeospatialCoverage(ThreddsMetadata.GeospatialCoverage gc) {
    Element elem = new Element("geospatialCoverage", Catalog.defNS);
    if (gc.getZPositive().equals("down"))
      elem.setAttribute("zpositive", gc.getZPositive());

    if (gc.getNorthSouthRange() != null)
      writeGeospatialRange(elem, new Element("northsouth", Catalog.defNS), gc.getNorthSouthRange());
    if (gc.getEastWestRange() != null)
      writeGeospatialRange(elem, new Element("eastwest", Catalog.defNS), gc.getEastWestRange());
    if (gc.getUpDownRange() != null)
      writeGeospatialRange(elem, new Element("updown", Catalog.defNS), gc.getUpDownRange());

    // serialize isGlobal
    java.util.List<ThreddsMetadata.Vocab> names = gc.getNames();
    ThreddsMetadata.Vocab global = new ThreddsMetadata.Vocab("global", null);
    if (gc.isGlobal() && !names.contains(global)) {
      names.add(global);
    } else if (!gc.isGlobal() && names.contains(global)) {
      names.remove(global);
    }

    for (ThreddsMetadata.Vocab name : names) {
      elem.addContent(writeControlledVocabulary(name, "name"));
    }

    return elem;
  }

  private void writeGeospatialRange(Element parent, Element elem, ThreddsMetadata.GeospatialRange r) {
    if (r == null) return;

    elem.addContent(new Element("start", Catalog.defNS).setText(Double.toString(r.getStart())));
    elem.addContent(new Element("size", Catalog.defNS).setText(Double.toString(r.getSize())));
    if (r.hasResolution())
      elem.addContent(new Element("resolution", Catalog.defNS).setText(Double.toString(r.getResolution())));
    if (r.getUnits() != null)
      elem.addContent(new Element("units", Catalog.defNS).setText(r.getUnits()));

    parent.addContent(elem);
  }

  private Element writeMetadata(ThreddsMetadata.MetadataOther mdata) {
    Element mdataElem = new Element("metadata", Catalog.defNS);
    if (mdata.getType() != null)
      mdataElem.setAttribute("metadataType", mdata.getType());
    if (mdata.isInherited())
      mdataElem.setAttribute("inherited", "true");

    String ns = mdata.getNamespaceURI();
    if ((ns != null) && !ns.equals(Catalog.CATALOG_NAMESPACE_10)) {
      Namespace mdataNS = Namespace.getNamespace(mdata.getPrefix(), ns);
      mdataElem.addNamespaceDeclaration(mdataNS);
    }

    if (mdata.getXlinkHref() != null) {
      mdataElem.setAttribute("href", mdata.getXlinkHref(), Catalog.xlinkNS);
      if (mdata.getTitle() != null)
        mdataElem.setAttribute("title", mdata.getTitle(), Catalog.xlinkNS);

    } else if (mdata.getContentObject() != null && mdata.getContentObject() instanceof Element) {
      Element content = (Element) mdata.getContentObject();
      mdataElem.setContent(content);
    }

    return mdataElem;
  }

  private Element writeProperty(Property prop) {
    Element propElem = new Element("property", Catalog.defNS);
    propElem.setAttribute("name", prop.getName());
    propElem.setAttribute("value", prop.getValue());
    return propElem;
  }

  protected Element writeSource(String elementName, ThreddsMetadata.Source p) {
    Element elem = new Element(elementName, Catalog.defNS);

    elem.addContent(writeControlledVocabulary(p.getNameVocab(), "name"));

    Element contact = new Element("contact", Catalog.defNS);
    if (p.getUrl() != null)
      contact.setAttribute("url", p.getUrl());
    if (p.getEmail() != null)
      contact.setAttribute("email", p.getEmail());
    elem.addContent(contact);

    return elem;
  }


  private Element writeService(Service service) {
    Element serviceElem = new Element("service", Catalog.defNS);
    serviceElem.setAttribute("name", service.getName());
    serviceElem.setAttribute("serviceType", service.getType().toString());
    serviceElem.setAttribute("base", service.getBase());
    if ((service.getSuffix() != null) && (service.getSuffix().length() > 0))
      serviceElem.setAttribute("suffix", service.getSuffix());

    // properties
    for (Property p : service.getProperties()) {
      serviceElem.addContent(writeProperty(p));
    }

    // services
    for (Service nested : service.getNestedServices()) {
      serviceElem.addContent(writeService(nested));
    }

    /* dataset roots
    if (raw) {
      for (Property p : service.getDatasetRoots()) {
        serviceElem.addContent(writeDatasetRoot(p));
      }
    } */

    return serviceElem;
  }

  private Element writeDataSize(double size) {
    Element sizeElem = new Element("dataSize", Catalog.defNS);

    // want exactly the number of bytes
    if (useBytesForDataSize) {
      sizeElem.setAttribute("units", "bytes");
      long bytes = (long) size;
      sizeElem.setText(Long.toString(bytes));
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
    } else {
      unit = "bytes";
    }

    sizeElem.setAttribute("units", unit);
    sizeElem.setText(ucar.unidata.util.Format.d(size, 4));

    return sizeElem;
  }

  /* protected void writeCat6InheritedMetadata( Element elem, ThreddsMetadata tmi) {
    if ((tmi.getDataType() == null) && (tmi.getServiceName() == null) &&
        (tmi.getAuthority() == null) && ( tmi.getProperties().size() == 0))
      return;

    Element mdataElem = new Element("metadata", Catalog.defNS);
    mdataElem.setAttribute("inherited", "true");
    writeThreddsMetadata( mdataElem, tmi);
    elem.addContent( mdataElem);
  }  */

  protected void writeInheritedMetadata(Element elem, Dataset ds) {
    Element mdataElem = new Element("metadata", Catalog.defNS);
    mdataElem.setAttribute("inherited", "true");
    ThreddsMetadata tmi = (ThreddsMetadata) ds.getLocalField(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) return;
    writeThreddsMetadata(mdataElem, tmi);
    if (mdataElem.getChildren().size() > 0)
      elem.addContent(mdataElem);
  }

  protected void writeThreddsMetadata(Element elem, ThreddsMetadataContainer ds) {

    String serviceName = (String) ds.getLocalField(Dataset.ServiceName);
    if (serviceName != null) {
      Element serviceNameElem = new Element("serviceName", Catalog.defNS);
      serviceNameElem.setText(serviceName);
      elem.addContent(serviceNameElem);
    }

    String authority = (String) ds.getLocalField(Dataset.Authority);
    if (authority != null) {
      Element authElem = new Element("authority", Catalog.defNS);
      authElem.setText(authority);
      elem.addContent(authElem);
    }

    String featureTypeName = (String) ds.getLocalField(Dataset.FeatureType);
    if (featureTypeName != null) {
      Element dataTypeElem = new Element("dataType", Catalog.defNS);
      dataTypeElem.setText(featureTypeName);
      elem.addContent(dataTypeElem);
    }

    String dataFormatName = (String) ds.getLocalField(Dataset.DataFormatType);
    if (dataFormatName != null) {
      Element dataFormatElem = new Element("dataFormat", Catalog.defNS);
      dataFormatElem.setText(dataFormatName);
      elem.addContent(dataFormatElem);
    }

    Long dataSize = (Long) ds.getLocalField(Dataset.DataSize);
    if (dataSize != null && dataSize > 0)
      elem.addContent(writeDataSize(dataSize));

    List<Documentation> docList =  (List<Documentation>) ds.getLocalFieldAsList(Dataset.Documentation);
    for (Documentation doc : docList) {
      elem.addContent(writeDocumentation(doc, "documentation"));
    }

    List<ThreddsMetadata.Contributor> contribList = (List<ThreddsMetadata.Contributor>) ds.getLocalFieldAsList(Dataset.Contributors);
    for (ThreddsMetadata.Contributor c : contribList) {
      elem.addContent(writeContributor(c));
    }

    List<ThreddsMetadata.Source> creatorList = (List<ThreddsMetadata.Source>) ds.getLocalFieldAsList(Dataset.Creators);
    for (ThreddsMetadata.Source p : creatorList) {
      elem.addContent(writeSource("creator", p));
    }

    List<ThreddsMetadata.Vocab> kewordList = (List<ThreddsMetadata.Vocab>) ds.getLocalFieldAsList(Dataset.Keywords);
    for (ThreddsMetadata.Vocab v : kewordList) {
      elem.addContent(writeControlledVocabulary(v, "keyword"));
    }

    List<ThreddsMetadata.MetadataOther> mdList = (List<ThreddsMetadata.MetadataOther>) ds.getLocalFieldAsList(Dataset.MetadataOther);
    for (ThreddsMetadata.MetadataOther m : mdList) {
      elem.addContent(writeMetadata(m));
    }

    List<ThreddsMetadata.Vocab> projList = (List<ThreddsMetadata.Vocab>) ds.getLocalFieldAsList(Dataset.Projects);
    for (ThreddsMetadata.Vocab v : projList) {
      elem.addContent(writeControlledVocabulary(v, "project"));
    }

    List<Property> propertyList = (List<Property>) ds.getLocalFieldAsList(Dataset.Properties);
    for (Property p : propertyList) {
      elem.addContent(writeProperty(p));
    }

    List<ThreddsMetadata.Source> pubList = (List<ThreddsMetadata.Source>) ds.getLocalFieldAsList(Dataset.Publishers);
    for (ThreddsMetadata.Source p : pubList) {
      elem.addContent(writeSource("publisher", p));
    }

    List<DateType> dateList = (List<DateType>) ds.getLocalFieldAsList(Dataset.Dates);
    for (DateType d : dateList) {
      elem.addContent(writeDate("date", d));
    }

    ThreddsMetadata.GeospatialCoverage gc = (ThreddsMetadata.GeospatialCoverage) ds.getLocalField(Dataset.GeospatialCoverage);
    if (gc != null)
      elem.addContent(writeGeospatialCoverage(gc));

    DateRange tc = (DateRange) ds.getLocalField(Dataset.TimeCoverage);
    if (tc != null)
      elem.addContent(writeTimeCoverage(tc));

    List<ThreddsMetadata.VariableGroup> varList = (List<ThreddsMetadata.VariableGroup>) ds.getLocalFieldAsList(Dataset.VariableGroups);
    for (ThreddsMetadata.VariableGroup v : varList) {
      elem.addContent(writeVariables(v));
    }

    URI varMapLink = (URI) ds.getLocalField(Dataset.VariableMapLink);
    if (varMapLink != null) {
      Element velem = new Element("variableMap", Catalog.defNS);
      velem.setAttribute("title", "variables", Catalog.xlinkNS);
      velem.setAttribute("href", varMapLink.toString(), Catalog.xlinkNS);
      elem.addContent(velem);
    }
  }

  protected Element writeTimeCoverage(DateRange t) {
    Element elem = new Element("timeCoverage", Catalog.defNS);

    DateType start = t.getStart();
    DateType end = t.getEnd();
    TimeDuration duration = t.getDuration();
    TimeDuration resolution = t.getResolution();

    if (t.useStart() && (start != null) && !start.isBlank()) {
      Element startElem = new Element("start", Catalog.defNS);
      startElem.setText(start.toString());
      elem.addContent(startElem);
    }

    if (t.useEnd() && (end != null) && !end.isBlank()) {
      Element telem = new Element("end", Catalog.defNS);
      telem.setText(end.toString());
      elem.addContent(telem);
    }

    if (t.useDuration() && (duration != null) && !duration.isBlank()) {
      Element telem = new Element("duration", Catalog.defNS);
      telem.setText(duration.toString());
      elem.addContent(telem);
    }

    if (t.useResolution() && (resolution != null) && !resolution.isBlank()) {
      Element telem = new Element("resolution", Catalog.defNS);
      telem.setText(t.getResolution().toString());
      elem.addContent(telem);
    }

    return elem;
  }

  protected Element writeVariable(ThreddsMetadata.Variable v) {
    Element elem = new Element("variable", Catalog.defNS);
    if (v.getName() != null)
      elem.setAttribute("name", v.getName());
    if (v.getDescription() != null) {
      String desc = v.getDescription().trim();
      if (desc.length() > 0)
        elem.setText(v.getDescription());
    }
    if (v.getVocabularyName() != null)
      elem.setAttribute("vocabulary_name", v.getVocabularyName());
    if (v.getUnits() != null)
      elem.setAttribute("units", v.getUnits());
    String id = v.getVocabularyId();
    if (id != null)
      elem.setAttribute("vocabulary_id", id);

    return elem;
  }

  protected Element writeVariables(ThreddsMetadata.VariableGroup vs) {
    Element elem = new Element("variables", Catalog.defNS);
    if (vs.getVocabulary() != null)
      elem.setAttribute("vocabulary", vs.getVocabulary());
    if (vs.getVocabHref() != null)
      elem.setAttribute("href", vs.getVocabHref(), Catalog.xlinkNS);

    if (vs.getMapUri() != null) { // variable map
      Element mapElem = new Element("variableMap", Catalog.defNS);
      mapElem.setAttribute("href", vs.getMapUri().toString(), Catalog.xlinkNS);
      elem.addContent(mapElem);

    } else { // inline variables
      List<ThreddsMetadata.Variable> varList = vs.getVariableList();
      for (ThreddsMetadata.Variable v : varList) {
        elem.addContent(writeVariable(v));
      }
    }

    return elem;
  }
}
