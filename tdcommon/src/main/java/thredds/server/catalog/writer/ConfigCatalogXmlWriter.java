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
package thredds.server.catalog.writer;

import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Property;
import thredds.client.catalog.tools.CatalogXmlWriter;

/**
 * Describe
 *
 * @author caron
 * @since 1/16/2015
 */
public class ConfigCatalogXmlWriter extends CatalogXmlWriter {

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
}
