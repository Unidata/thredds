package thredds.server.catalogservice;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.server.catalog.CatalogScan;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.HtmlConfigBean;
import thredds.server.config.TdsContext;
import thredds.server.config.TdsServerInfoBean;
import thredds.server.notebook.JupyterNotebookServiceCache;
import thredds.server.viewer.ViewerLinkProvider;
import thredds.server.viewer.ViewerService;
import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CatalogViewContextParser {

  @Autowired
  private ViewerService viewerService;

  @Autowired
  private HtmlConfigBean htmlConfig;

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private TdsServerInfoBean serverInfo;

  private static final String ROOT_CAT_PATTERN = "/thredds/catalog/catalog.(html|xml)";

  public Map<String, Object> getCatalogViewContext(Catalog cat, HttpServletRequest req, boolean isLocalCatalog) {
    Map<String, Object> model = new HashMap<>();
    addBaseContext(model);

    try {
      if (Pattern.matches(CatalogViewContextParser.ROOT_CAT_PATTERN, cat.getBaseURI().getPath())) {
        model.put("rootCatalog", true);
      }
    } catch(Exception e) {
      model.put("rootCatalog", false);
    }

    List<CatalogItemContext> catalogItems = new ArrayList<>();
    addCatalogItems(cat, catalogItems, isLocalCatalog, 0);
    model.put("items", catalogItems);

    return model;
  }

  public  Map<String, Object> getDatasetViewContext(Dataset ds, HttpServletRequest req, boolean isLocalCatalog)
  {
    Map<String, Object> model = new HashMap<>();
    addBaseContext(model);

    DatasetContext context = new DatasetContext(ds, isLocalCatalog, tdsContext, req);

    populateDatasetContext(ds, context, req, isLocalCatalog);

    model.put("dataset", context);

    if (htmlConfig.getGenerateDatasetJsonLD()) {
      String jsonLD = JsonLD.makeDatasetJsonLD(ds, context, tdsContext);
      if (!jsonLD.isEmpty()) {
        model.put("jsonLD", jsonLD);
      }
    }

    return model;
  }

  private void addBaseContext(Map<String, Object> model) {

    String googleTrackingCode = htmlConfig.getGoogleTrackingCode();
    if (googleTrackingCode.isEmpty()) googleTrackingCode = null;
    model.put("googleTracking", googleTrackingCode);

    model.put("serverName", serverInfo.getName());
    model.put("logoUrl", serverInfo.getLogoUrl());
    model.put("logoAlt", serverInfo.getLogoAltText());

    model.put("installName", htmlConfig.getInstallName());
    model.put("installUrl", htmlConfig.getInstallUrl());

    model.put("webappName", htmlConfig.getWebappName());
    model.put("webappUrl", htmlConfig.getWebappUrl());
    model.put("webappVersion", htmlConfig.getWebappVersion());
    model.put("webappBuildTimestamp", htmlConfig.getWebappVersionBuildDate());
    model.put("webappDocsUrl", htmlConfig.getWebappDocsUrl());
    model.put("contextPath", htmlConfig.getWebappContextPath());

    model.put("hostInst", htmlConfig.getHostInstName());
    model.put("hostInstUrl", htmlConfig.getHostInstUrl());

    model.put("standardCSS", htmlConfig.getPageCssUrl());
    model.put("catalogCSS", htmlConfig.getCatalogCssUrl());
    model.put("datasetCSS", htmlConfig.getDatasetCssUrl());
  }

  protected void addCatalogItems(DatasetNode cat, List<CatalogItemContext> catalogItems, boolean isLocalCatalog, int level)
  {
    for (Dataset ds : cat.getDatasets()) {
      populateItemContext(ds, catalogItems, isLocalCatalog, level);
    }
  }

  protected void populateItemContext(Dataset ds, List<CatalogItemContext> catalogItems, boolean isLocalCatalog, int level) {
    CatalogItemContext context = new CatalogItemContext(ds, level);

    // add item href
    context.setHref(getCatalogItemHref(ds, isLocalCatalog));

    // add item icon
    context.setIconSrc(getFolderIconSrc(ds));

    // add item to Catalog
    catalogItems.add(context);

    // recursively add subdirectories
    if (!(ds instanceof CatalogRef)) {
      addCatalogItems(ds, catalogItems, isLocalCatalog, level + 1);
    }
  }

  protected void populateDatasetContext(Dataset ds, DatasetContext context, HttpServletRequest req, boolean isLocalCatalog) {
    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;
      context.addContextItem("href", getCatalogRefHref(catref, isLocalCatalog));
    }

    if (isLocalCatalog) context.setViewers(ds, viewerService.getViewerLinks(ds, req));
  }

  private String getCatalogItemHref(Dataset ds, boolean isLocalCatalog)
  {
    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;
      String href = catref.getXlinkHref();
      if (!isLocalCatalog) {
        URI hrefUri = ds.getParentCatalog().getBaseURI().resolve(href);
        href = hrefUri.toString();
      }
      try {
        URI uri = new URI(href);
        if (uri.isAbsolute()) {
          boolean defaultUseRemoteCatalogService = htmlConfig.getUseRemoteCatalogService(); // read default as set in threddsConfig.xml
          Boolean dsUseRemoteCatalogService = ((CatalogRef) ds).useRemoteCatalogService();  // check to see if catalogRef contains tag that overrides default
          boolean useRemoteCatalogService = defaultUseRemoteCatalogService; // by default, use the option found in threddsConfig.xml
          if (dsUseRemoteCatalogService == null)
            dsUseRemoteCatalogService = defaultUseRemoteCatalogService; // if the dataset does not have the useRemoteDataset option set, opt for the default behavior

          // if the default is not the same as what is defined in the catalog, go with the catalog option
          // as the user has explicitly overridden the default
          if (defaultUseRemoteCatalogService != dsUseRemoteCatalogService) {
            useRemoteCatalogService = dsUseRemoteCatalogService;
          }

          // now, do the right thing with using the remoteCatalogService, or not
          if (useRemoteCatalogService) {
            href = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + href;
          } else {
            int pos = href.lastIndexOf('.');
            href = href.substring(0, pos) + ".html";
          }
        } else {
          int pos = href.lastIndexOf('.');
          href = href.substring(0, pos) + ".html";
        }

      } catch (Exception e) {//(URISyntaxException e) {
        //log.error(href, e);
      }

      return href;

    } else { // Not a CatalogRef
      // Check if dataset has single resolver service.
      if (ds.getAccess().size() == 1 && ServiceType.Resolver == ds.getAccess().get(0).getService().getType()) {
        Access access = ds.getAccess().get(0);
        String accessUrlName = access.getUnresolvedUrlName();
        int pos = accessUrlName.lastIndexOf(".xml");

        if (accessUrlName.equalsIgnoreCase("latest.xml") && !isLocalCatalog) {
          String catBaseUriPath = "";
          String catBaseUri = ds.getParentCatalog().getBaseURI().toString();
          pos = catBaseUri.lastIndexOf("catalog.xml");
          if (pos != -1) {
            catBaseUriPath = catBaseUri.substring(0, pos);
          }
          accessUrlName = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + catBaseUriPath + accessUrlName;
        } else if (pos != -1) {
          accessUrlName = accessUrlName.substring(0, pos) + ".html";
        }
        return accessUrlName;

      } else if (ds.findProperty(Dataset.NotAThreddsDataset) != null) { // Dataset can only be file served
        // Write link to HTML dataset page.
        return makeFileServerUrl(ds);

      } else if (ds.getID() != null) { // Dataset with an ID.    //URI catURI = cat.getBaseURI();
        Catalog cat = ds.getParentCatalog();
        String catHtml;
        if (!isLocalCatalog) {
          // Setup HREF url to link to HTML dataset page (more below).
          catHtml = tdsContext.getContextPath() + "/remoteCatalogService?command=" + RemoteCatalogRequest.Command.SUBSET +  "&catalog=" + cat.getUriString() + "&";
          // Can't be "/catalogServices?..." because subset decides on xml or html by trailing ".html" on URL path
        } else { // replace xml with html
          URI catURI = cat.getBaseURI();
          // Get the catalog name - we want a relative URL
          catHtml = catURI.getPath();
          if (catHtml == null) catHtml = cat.getUriString();  // if URI is a file
          int pos = catHtml.lastIndexOf("/");
          if (pos != -1) catHtml = catHtml.substring(pos + 1);
          // change the ending to "catalog.html?"
          pos = catHtml.lastIndexOf('.');
          if (pos < 0)
            catHtml = catHtml + "catalog.html?";
          else
            catHtml = catHtml.substring(0, pos) + ".html?";
        }

        // Write link to HTML dataset page.
        return catHtml + "dataset=" + StringUtil2.replace(ds.getID(), '+', "%2B");
      } else {
        return null;
      }
    }
  }

  private String getCatalogRefHref(CatalogRef catref, boolean isLocalCatalog) {
    String href = catref.getXlinkHref();
    if (!isLocalCatalog) {
      href = CatalogViewContextParser.makeHrefResolve((Dataset) catref, href);
    }
    return href;
  }

  protected static String makeHrefResolve(Dataset ds, String href) {
    Catalog cat = ds.getParentCatalog();
    if (cat != null) {
      try {
        java.net.URI uri = cat.resolveUri(href);
        href = uri.toString();
      } catch (java.net.URISyntaxException e) {
        return "CatalogViewContextParser: error parsing URL= " + href;
      }
    }
    return href;
  }

  private String getFolderIconSrc(Dataset ds) {
    String iconSrc = null;
    if (ds instanceof CatalogRef) {
      if (ds instanceof CatalogScan || ds.hasProperty("CatalogScan"))
        iconSrc = "cat_folder.png";
      else if (ds instanceof DatasetScan || ds.hasProperty("DatasetScan"))
        iconSrc = "scan_folder.png";
      else if (ds instanceof FeatureCollectionRef)
        iconSrc = "fc_folder.png";
      else
        iconSrc = "folder.png";

    } else { // Not a CatalogRef
      if (ds.hasNestedDatasets())
        iconSrc = "folder.png";
    }

    if (iconSrc != null) iconSrc = htmlConfig.prepareUrlStringForHtml(iconSrc);
    return iconSrc;
  }

  private String makeFileServerUrl(Dataset ds) {
    Access acc = ds.getAccess(ServiceType.HTTPServer);
    assert acc != null;
    return acc.getStandardUrlName();
  }
}

class CatalogItemContext {

  //static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogItemContext.class);

  private String displayName;
  private int level;
  private String dataSize;
  private String lastModified;
  private String iconSrc;
  private String itemhref;

  public CatalogItemContext(Dataset ds, int level)
  {
    // Get display name
    this.displayName = ds.getName();

    // Get data size
    double size = ds.getDataSize();
    if ((size > 0) && !Double.isNaN(size))
      this.dataSize = (Format.formatByteSize(size));


    // Get last modified time.
    DateType lastModDateType = ds.getLastModifiedDate();
    if (lastModDateType != null)
      this.lastModified = lastModDateType.toDateTimeString();

    // Store nesting level
    this.level = level;
  }

  public String getDisplayName() { return this.displayName; }

  public int getLevel() { return this.level; }

  public String getDataSize() { return this.dataSize; }

  public String getLastModified() { return this.lastModified; }

  public String getIconSrc() { return  this.iconSrc; }

  protected void setIconSrc(String iconSrc) { this.iconSrc = iconSrc; }

  public String getHref() { return this.itemhref; }

  protected void setHref(String href) { this.itemhref = href; }

}

class DatasetContext {

  private String contentDir;

  private String name;

  private String catUrl;

  private String catName;

  private Map<String, Object> context;

  private List<Map<String, String>> documentation;

  private List<Map<String, String>> access;

  private List<Map<String, String>> contributors;

  private List<Map<String,String>> keywords;

  private List<Map<String,String>> dates;

  private List<Map<String, String>> projects;

  private List<Map<String, String>> creators;

  private List<Map<String,String>> publishers;

  private List<Map<String, Object>> variables;

  private String variableMapLink;

  private Map<String, Object> geospatialCoverage;

  private Map<String, Object> timeCoverage;

  private List<Map<String, String>> metadata;

  private List<Map<String, String>> properties;

  private List<Map<String, String>> viewerLinks;

  public DatasetContext (Dataset ds, boolean isLocalCatalog, TdsContext tdsContext, HttpServletRequest req) {
    this.contentDir = tdsContext.getContentRootPathProperty();
    // Get display name and catalog url
    this.name = ds.getName();
    String catUrl = ds.getCatalogUrl();
    if (catUrl.indexOf('#') > 0)
      catUrl = catUrl.substring(0, catUrl.lastIndexOf('#'));
    catUrl = catUrl.replace("xml", "html");
    // for direct datasets generated directly off of the root catalog, and maybe others, the base uri is missing
    // the full server path. Try to do what we can.
    if (catUrl.startsWith("/")) {
      String reqUri = req.getRequestURL().toString();
      if (reqUri.contains(req.getContextPath())) {
        String baseUriString = reqUri.split(req.getContextPath())[0];
        catUrl = baseUriString + catUrl;
      }
    }

    this.catUrl =  catUrl;
    this.catName = ds.getParentCatalog().getName();

    setContext(ds);
    setDocumentation(ds);
    setAccess(ds, isLocalCatalog);
    setContributors(ds);
    setKeywords(ds);
    setDates(ds);
    setProjects(ds);
    setCreators(ds, isLocalCatalog);
    setPublishers(ds, isLocalCatalog);
    setVariables(ds, isLocalCatalog);
    setGeospatialCoverage(ds);
    setTimeCoverage(ds);
    setMetadata(ds, isLocalCatalog);
    setProperties(ds, isLocalCatalog);

    this.viewerLinks = new ArrayList<>();
  }

  private void setContext(Dataset ds) {
    this.context = new HashMap<>();

    String dataFormat = ds.getDataFormatName();
    if (dataFormat != null) context.put("dataFormat", dataFormat);

    long dataSize = ds.getDataSize();
    if (dataSize > 0) context.put("dataSize", dataSize);

    String featureType = ds.getFeatureTypeName();
    if (featureType != null) context.put("featureType", featureType);

    String collectionType = ds.getCollectionType();
    if (collectionType != null) context.put("collectionType", collectionType);

    boolean isHarvest = ds.isHarvest();
    if (isHarvest) context.put("isHarvest", true);

    String authority = ds.getAuthority();
    if (authority != null) context.put("authority", authority);

    String id = ds.getId();
    if (id != null) context.put("id", id);

    String restrictAccess = ds.getRestrictAccess();
    if (restrictAccess != null) context.put("restrictAccess", restrictAccess);
  }

  private void setDocumentation(Dataset ds) {
    java.util.List<Documentation> docs = ds.getDocumentation();
    this.documentation = new ArrayList<>(docs.size());

    for (Documentation doc : docs) {
      Map<String, String> docMap = new HashMap<>();
      String inlineContent = doc.getInlineContent();
      if (inlineContent.isEmpty()) inlineContent = null;
      docMap.put("inlineContent", inlineContent);
      String type = doc.getType();
      if (type == null) type = "";
      docMap.put("type", type);
      if (doc.hasXlink()) {
        docMap.put("href", doc.getXlinkHref());
        docMap.put("title", doc.getXlinkTitle());
      }
      this.documentation.add(docMap);
    }
  }

  private void setAccess(Dataset ds, boolean isLocalCatalog) {
    List<Access> access = ds.getAccess();
    this.access = new ArrayList<>(access.size());

    for (Access a : access) {
      Service s = a.getService();
      String urlString = !isLocalCatalog ? a.getStandardUrlName() : a.getUnresolvedUrlName();
      String queryString = null;
      String catalogUrl = null;
      String datasetId = null;

      ServiceType stype = s.getType();
      if (stype != null) {
        switch (stype) {
          case OPENDAP:
          case DODS:
            urlString = urlString + ".html";
            break;

          case DAP4:
            urlString = urlString + ".dmr.xml";
            break;

          case WCS:
            queryString = "service=WCS&version=1.0.0&request=GetCapabilities";
            break;

          case WMS:
            queryString = "service=WMS&version=1.3.0&request=GetCapabilities";
            break;

          case WFS:
        	queryString = "service=WFS&version=2.0.0&request=GetCapabilities";
        	break;
        	 
          case NCML:
          case UDDC:
          case ISO:
            catalogUrl = ds.getCatalogUrl();
            datasetId = ds.getId();
            if (catalogUrl != null && datasetId != null) {
              if (catalogUrl.indexOf('#') > 0)
                catalogUrl = catalogUrl.substring(0, catalogUrl.lastIndexOf('#'));
              queryString = "catalog=" + catalogUrl + "&dataset=" + datasetId;
            }
            break;

          case JupyterNotebook:
            datasetId = ds.getId();
            catalogUrl = ds.getCatalogUrl();
            if (catalogUrl.indexOf('#') > 0)
              catalogUrl = catalogUrl.substring(0, catalogUrl.lastIndexOf('#'));
            if (catalogUrl.indexOf(contentDir) > -1) {
              catalogUrl = catalogUrl.substring(catalogUrl.indexOf(contentDir) + contentDir.length());
            }
            catalogUrl = catalogUrl.substring(catalogUrl.indexOf("/catalog/") + ("/catalog/").length())
                    .replace("html", "xml");
            queryString = "catalog=" + catalogUrl;
            urlString = urlString.substring(0, urlString.indexOf(s.getBase()) + s.getBase().length()) + datasetId;
            break;

          case NetcdfSubset:
            urlString = urlString + "/dataset.html";
            break;

          case CdmRemote:
            queryString = "req=cdl";
            break;
          case CdmrFeature:
            queryString = "req=form";
        }
      }
      Map<String, String> accessMap = new HashMap<>();
      accessMap.put("serviceTypeName", s.getServiceTypeName());
      accessMap.put("serviceDesc", s.getDesc());
      accessMap.put("accessType", s.getAccessType());
      accessMap.put("href", queryString == null ? urlString : urlString + "?" + queryString);
      this.access.add(accessMap);
    }
  }
  private void setContributors(Dataset ds) {
    java.util.List<ThreddsMetadata.Contributor> contributors = ds.getContributors();
    this.contributors = new ArrayList<>(contributors.size());

    for (ThreddsMetadata.Contributor t : contributors) {
      Map<String, String> contribMap = new HashMap<>();
      contribMap.put("role", t.getRole() == null ? "" : t.getRole());
      contribMap.put("name", t.getName());
      this.contributors.add(contribMap);
    }
  }

  private void setKeywords(Dataset ds) {
    java.util.List<ThreddsMetadata.Vocab> keywords = ds.getKeywords();
    this.keywords = new ArrayList<>(keywords.size());

    for (ThreddsMetadata.Vocab t : keywords) {
      Map<String, String> keyMap = new HashMap<>();
      keyMap.put("vocab", t.getVocabulary() == null ? "" : t.getVocabulary());
      keyMap.put("text", t.getText());
      this.keywords.add(keyMap);
    }
  }

  private void setDates(Dataset ds) {
    java.util.List<DateType> dates = ds.getDates();
    this.dates = new ArrayList<>(dates.size());

    for (DateType d : dates) {
      Map<String, String> dateMap = new HashMap<>();
      dateMap.put("type", d.getType() == null ? "" : d.getType());
      dateMap.put("text", d.getText());
      this.dates.add(dateMap);
    }
  }

  private void setProjects(Dataset ds) {
    java.util.List<ThreddsMetadata.Vocab> projects = ds.getProjects();
    this.projects = new ArrayList<>(projects.size());

    for (ThreddsMetadata.Vocab t : projects) {
      Map<String, String> projectMap = new HashMap<>();
      projectMap.put("vocab", t.getVocabulary() == null ? "" : t.getVocabulary());
      projectMap.put("text", t.getText());
      this.projects.add(projectMap);
    }
  }

  private void setCreators(Dataset ds, boolean isLocalCatalog) {
    java.util.List<ThreddsMetadata.Source> creators = ds.getCreators();
    this.creators = new ArrayList<>(creators.size());

    for (ThreddsMetadata.Source t : creators) {
      Map<String, String> creatorMap = new HashMap<>();
      creatorMap.put("name", t.getName());
      creatorMap.put("email", t.getEmail());
      String href = t.getUrl();
      if (!isLocalCatalog) href = CatalogViewContextParser.makeHrefResolve(ds, href);
      creatorMap.put("href", href);
      this.creators.add(creatorMap);
    }
  }

  private void setPublishers(Dataset ds, boolean isLocalCatalog) {
    this.publishers = new ArrayList<>();
    java.util.List<ThreddsMetadata.Source> publishers = ds.getPublishers();

    for (ThreddsMetadata.Source t : publishers) {
      Map<String, String> pubMap = new HashMap<>();
      pubMap.put("name", t.getName());
      pubMap.put("email", t.getEmail());
      String href = t.getUrl();
      if (!isLocalCatalog) href = CatalogViewContextParser.makeHrefResolve(ds, href);
      pubMap.put("href", href);
      this.publishers.add(pubMap);
    }
  }

  private void setVariables(Dataset ds, boolean isLocalCatalog) {
    List<ThreddsMetadata.VariableGroup> vars = ds.getVariables();
    this.variables = new ArrayList<>(vars.size());

    for (ThreddsMetadata.VariableGroup t : vars) {
      Map<String, Object> varGroup = new HashMap<String, Object>();
      String vocab = t.getVocabulary();
      varGroup.put("title", vocab);
      ThreddsMetadata.UriResolved vocabUri = t.getVocabUri();
      if (vocabUri != null) {
        varGroup.put("href", isLocalCatalog ? vocabUri.href : vocabUri.resolved.toString());
      }

      List<Map<String, String>> varList = new ArrayList<>();
      List<ThreddsMetadata.Variable> vlist = t.getVariableList();
      for (ThreddsMetadata.Variable v : vlist) {
        Map<String, String> var= new HashMap<>();
        String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
        var.put("nameAndUnits", v.getName() + units);
        var.put("description", v.getDescription());
        var.put("vocabularyName", v.getVocabularyName());
        varList.add(var);
      }
      varGroup.put("varList", varList);
      this.variables.add(varGroup);
    }

    // LOOK what about VariableMapLink string ??
    ThreddsMetadata.UriResolved uri = ds.getVariableMapLink();
    if (uri != null) {
      this.variableMapLink = uri.resolved.toASCIIString();
    }
  }

  private void setGeospatialCoverage(Dataset ds) {
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    this.geospatialCoverage = new HashMap<>();
    if (gc != null) {
      this.geospatialCoverage.put("eastWestRange", rangeString(gc.getEastWestRange()));
      this.geospatialCoverage.put("northSouthRange", rangeString(gc.getNorthSouthRange()));
      ThreddsMetadata.GeospatialRange r = gc.getUpDownRange();
      if (r != null && r.getSize() > 0) {
        this.geospatialCoverage.put("upDownRange", rangeString(r));
        this.geospatialCoverage.put("zPositive", gc.getZPositive());
      }

      List<String> names = new ArrayList<>();
      List<ThreddsMetadata.Vocab> nlist = gc.getNames();
      if (nlist != null)
        for (ThreddsMetadata.Vocab elem : nlist) {
          names.add(elem.getText());
        }
      this.geospatialCoverage.put("names", names);
    }
  }

  private void setTimeCoverage(Dataset ds) {
    DateRange tc = ds.getTimeCoverage();
    this.timeCoverage = new HashMap<>();
    if (tc != null) {
      DateType start = tc.getStart();
      if (start != null) this.timeCoverage.put("start", start.toString());
      DateType end = tc.getEnd();
      if (end != null) this.timeCoverage.put("end", end.toString());
      TimeDuration duration = tc.getDuration();
      if (duration != null) this.timeCoverage.put("duration", duration.toString());
      TimeDuration resolution = tc.getResolution();
      if (resolution != null) this.timeCoverage.put("resolution", resolution.toString());
    }
  }

  private void setMetadata(Dataset ds, boolean isLocalCatalog) {
    java.util.List<ThreddsMetadata.MetadataOther> metadata = ds.getMetadataOther();
    this.metadata = new ArrayList<>(metadata.size());

    for (ThreddsMetadata.MetadataOther m : metadata) {
      Map<String, String> metadataMap = new HashMap<>();
      if (m.getXlinkHref() != null) {
        String type = (m.getType() == null) ? "" : m.getType();
        metadataMap.put("title", (m.getTitle() == null) ? "Type " + type : m.getTitle());
        String mdLink = m.getXlinkHref();
        if (!isLocalCatalog) mdLink = CatalogViewContextParser.makeHrefResolve(ds, mdLink);
        metadataMap.put("href", mdLink);
      }
    }
  }

  private void setProperties(Dataset ds, boolean isLocalCatalog) {
    java.util.List<Property> propsOrg = ds.getProperties();
    java.util.List<Property> props = new ArrayList<>(ds.getProperties().size());
    for (Property p : propsOrg) {
      if (!p.getName().startsWith("viewer"))  // eliminate the viewer properties from the html view
        props.add(p);
    }

    this.properties = new ArrayList<>(props.size());
    for (Property p : props) {
      Map<String, String> propsMap = new HashMap<>();
      if (p.getName().equals("attachments")) { // LOOK whats this ?
        propsMap.put("href", !isLocalCatalog ? CatalogViewContextParser.makeHrefResolve(ds, p.getValue()) : p.getValue());
      }
      propsMap.put("name", p.getName());
      propsMap.put("value", p.getValue());
      this.properties.add(propsMap);
    }
  }

  private String rangeString(ThreddsMetadata.GeospatialRange r) {
    if (r == null) return "";
    String units = (r.getUnits() == null) ? "" : " " + r.getUnits();
    String resolution = r.hasResolution() ? " Resolution=" + r.getResolution() : "";
    return r.getStart() + " to " + (r.getStart() + r.getSize()) + resolution + units;
  }

  private String makeHrefResolve(Dataset ds, String href) {
    Catalog cat = ds.getParentCatalog();
    if (cat != null) {
      try {
        java.net.URI uri = cat.resolveUri(href);
        href = uri.toString();
      } catch (java.net.URISyntaxException e) {
        return "CatalogViewContextParser: error parsing URL= " + href;
      }
    }
    return href;
  }

  public String getName() { return this.name; }

  public String getCatUrl() { return this.catUrl; }

  public String getCatName() { return this.catName; }

  public List<Map<String, String>> getDocumentation() { return this.documentation; }

  public List<Map<String, String>> getAccess() { return this.access; }

  public List<Map<String, String>> getContributors() { return this.contributors; }

  public List<Map<String, String>> getKeywords() { return this.keywords; }

  public List<Map<String, String>> getDates() { return this.dates; }

  public List<Map<String, String>> getProjects() { return this.projects; }

  public List<Map<String, String>> getCreators() { return this.creators; }

  public List<Map<String, String>> getPublishers() { return this.publishers; }

  public List<Map<String, Object>> getVariables() { return this.variables; }

  public String getVariableMapLink() { return this.variableMapLink; }

  public Map<String, Object> getGeospatialCoverage() { return this.geospatialCoverage;}

  public Map<String, Object> getTimeCoverage() { return this.timeCoverage; }

  public List<Map<String, String>> getMetadata() { return this.metadata; }

  public List<Map<String, String>> getProperties() { return this.properties; }

  public Map<String, Object> getAllContext() { return this.context; }

  public Object getContextItem(String key) { return this.context.get(key); }

  protected void addContextItem(String key, Object value) { this.context.put(key, value); }

  public List<Map<String, String>> getViewerLinks() { return this.viewerLinks; }

  protected void setViewers(Dataset ds, List<ViewerLinkProvider.ViewerLink> viewerLinks) {
    for (ViewerLinkProvider.ViewerLink viewer : viewerLinks) {
      Map<String, String> viewerMap = new HashMap<>();
      viewerMap.put("title", viewer.getTitle());
      viewerMap.put("href", viewer.getUrl());
      this.viewerLinks.add(viewerMap);
    }
  }

}

class JsonLD {

    static String makeDatasetJsonLD(Dataset ds, DatasetContext dsContext, TdsContext tdsContext) {

    JSONObject jo = new JSONObject();

    jo.put("@context", "https://schema.org/");
    jo.put("@type", "Dataset");

    // for the name attribute, try to find a title. If not, use the dataset name.
    Optional<String> name = dsContext.getDocumentation().stream()
            .filter(doc -> doc.containsValue("title"))
            .findFirst()
            .map(found -> found.get("inlineContent"));

    jo.put("name", name.orElse(dsContext.getName()));

    // for the description, find all "summary" attributes and concatenate them.
    List<Map<String, String>> docs = dsContext.getDocumentation();
    List<String> summary = dsContext.getDocumentation().stream()
            .filter(doc -> doc.containsValue("summary"))
            .map(found -> found.get("inlineContent"))
            .collect(Collectors.toList());

    if (!summary.isEmpty()) {
      jo.put("description", StringUtils.join(summary, " "));
    }

    // set url to datasets catalog
    jo.put("url", dsContext.getCatUrl());

    // geocodes ID
    jo.put("@id", dsContext.getCatUrl());

    // keywords
    List<Map<String, String>> kws = dsContext.getKeywords();
    if (kws.size() > 0) {
      JSONArray ja = new JSONArray();
      for (Map<String,String> kw : kws) {
        if(kw.containsKey("text")) {
          ja.put(kw.get("text"));
        }
      }
      jo.put("keywords", ja);
    }

    // set time coverage for dataset
    Map<String, Object> timeCoverage = dsContext.getTimeCoverage();
    String start = timeCoverage.containsKey("start") ? timeCoverage.get("start").toString() : "";
    String end = timeCoverage.containsKey("end") ? timeCoverage.get("end").toString() : "";

    JSONObject temporalCoverage = new JSONObject();
    if (!start.isEmpty() && !end.isEmpty()) {
      temporalCoverage.put("@value", String.format("%s/%s", start, end));
    } else if (!start.isEmpty()) {
      temporalCoverage.put("@value", String.format("%s", start));
    } else if (!end.isEmpty()) {
      temporalCoverage.put("@value", String.format("%s", end));
    }

    if (temporalCoverage.length() > 0) {
        jo.put("temporalCoverage", temporalCoverage);
    }

    // set the spatial coverage
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if (gc != null) {
      LatLonRect bbox = gc.getBoundingBox();
      String box = String.format("%s %s %s %s", bbox.getLowerLeftPoint().getLatitude(),
              bbox.getLowerLeftPoint().getLongitude(),
              bbox.getUpperRightPoint().getLatitude(),
              bbox.getUpperRightPoint().getLongitude());
      JSONObject spatialCoverage = new JSONObject();
      spatialCoverage.put("@type", "Place");
      JSONObject geo = new JSONObject();
      geo.put("@type", "GeoShape");
      geo.put("box", box);
      spatialCoverage.put("geo", geo);
      jo.put("spatialCoverage", spatialCoverage);
    }

    // turn it into a string for use in the html template
    return jo.toString(2);
  }
}
