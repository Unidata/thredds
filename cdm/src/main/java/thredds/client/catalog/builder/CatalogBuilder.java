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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import thredds.client.catalog.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.URLnaming;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Builds client Catalogs using JDOM2
 * Non validating.
 * If you want validation, consider calling saxBuilder.setEntityResolver();
 *
 * @author caron
 * @since 1/8/2015
 */
public class CatalogBuilder {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatalogBuilder.class);

  public interface Callback {
    void setCatalog(Catalog cat);
  }

  //////////////////////////////////////////////////////////////////////////////////

  // protected String location; // read from where?
  private Map<String, Service> serviceMap = new HashMap<>();
  protected Formatter errlog = new Formatter();
  protected boolean fatalError = false;

  public Catalog buildFromLocation(String location, URI baseURI) throws IOException {
    location = StringUtil2.replace(location, "\\", "/");

    if (baseURI == null) {
      try {
        baseURI = new URI(location);
      } catch (URISyntaxException e) {
        errlog.format("Bad location = '%s' err='%s'%n", location, e.getMessage());
        fatalError = true;
        return null;
      }
    }

    this.baseURI = baseURI;
    readXML(location);
    return makeCatalog();
  }

  public Catalog buildFromURI(URI uri) throws IOException {
    this.baseURI = uri;
    readXML(uri);
    return makeCatalog();
  }

  public Catalog buildFromCatref(CatalogRef catref) throws IOException {
    URI catrefURI = catref.getURI();
    if (catrefURI == null) {
      errlog.format("Catref doesnt have valid UrlPath=%s%n", catref.getUrlPath());
      fatalError = true;
      return null;
    }
    this.baseURI = catrefURI;
    Catalog result =  buildFromURI(catrefURI);
    catref.setRead(!fatalError);
    return result;
  }

  public Catalog buildFromString(String catalogAsString, URI docBaseUri) throws IOException {
    this.baseURI = docBaseUri;
    readXMLfromString(catalogAsString);
    return makeCatalog();
  }

  public Catalog buildFromStream(InputStream stream, URI docBaseUri) throws IOException {
    this.baseURI = docBaseUri;
    readXML(stream);
    return makeCatalog();
  }

  public Catalog buildFromJdom(Element root, URI docBaseUri) throws IOException {
    this.baseURI = docBaseUri;
    readCatalog(root);
    return makeCatalog();
  }

  public String getErrorMessage() {
    return errlog.toString();
  }

  public String getValidationMessage() {
    return errlog.toString();
  }

  public boolean hasFatalError() {
    return fatalError;
  }

  ////////////////////////////////////////////////////
  protected String name, version;
  protected CalendarDate expires;
  protected URI baseURI;
  protected List<Property> properties;
  protected List<Service> services;
  protected List<DatasetBuilder> datasetBuilders;

  public void setName(String name) {
    this.name = name;
  }

  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  public void setExpires(CalendarDate expires) {
    this.expires = expires;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void addProperty(Property p) {
    if (p == null) return;
    if (properties == null) properties = new ArrayList<>();
    properties.add(p);
  }

  public void addService(Service s) {
    if (s == null) return;
    if (services == null) services = new ArrayList<>();
    if (!services.contains(s))
      services.add(s);
  }

  public void addDataset(DatasetBuilder d) {
    if (d == null) return;
    if (datasetBuilders == null) datasetBuilders = new ArrayList<>();
    datasetBuilders.add(d);
  }

  public Catalog makeCatalog() {
    Map<String, Object> flds = setFields();
    return new Catalog(baseURI, name, flds, datasetBuilders);
  }

  protected Map<String, Object> setFields() {
    Map<String, Object> flds = new HashMap<>(10);

    if (expires != null) flds.put(Dataset.Expires, expires);
    if (version != null) flds.put(Dataset.Version, version);
    if (services != null) flds.put(Dataset.Services, services);
    if (properties != null) flds.put(Dataset.Properties, properties);

    return flds;
  }

  public DatasetBuilder getTop() {
    if (datasetBuilders == null) return null;
    return datasetBuilders.get(0);
  }

  /////////////////////////////////////////////////////////////////////
  // JDOM

  private void readXML(String location) throws IOException {
     try {
       SAXBuilder saxBuilder = new SAXBuilder();
       org.jdom2.Document jdomDoc = saxBuilder.build(location);
       readCatalog(jdomDoc.getRootElement());

     } catch (Exception e) {
       errlog.format("failed to read catalog at '%s' err='%s'%n", location, e);
       logger.error("failed to read catalog at " + location, e);
       // e.printStackTrace();
       fatalError = true;
     }
   }


  private void readXML(URI uri) throws IOException {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      org.jdom2.Document jdomDoc = saxBuilder.build(uri.toURL());
      readCatalog(jdomDoc.getRootElement());

    } catch (Exception e) {
      errlog.format("failed to read catalog at '%s' err='%s'%n", uri.toString(), e);
      logger.error("failed to read catalog at " + uri.toString(), e);
      // e.printStackTrace();
      fatalError = true;
    }
  }

  private void readXMLfromString(String catalogAsString) throws IOException {
    try {
      StringReader in = new StringReader(catalogAsString);
      SAXBuilder saxBuilder = new SAXBuilder();    // LOOK non-validating
      org.jdom2.Document jdomDoc = saxBuilder.build(in);
      readCatalog(jdomDoc.getRootElement());

    } catch (Exception e) {
      errlog.format("failed to read catalogAsString err='%s'%n", e);
      logger.error("failed to read catalogAsString at" + baseURI.toString(), e);
      e.printStackTrace();
      fatalError = true;
    }
  }

  private void readXML(InputStream stream) throws IOException {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      org.jdom2.Document jdomDoc = saxBuilder.build(stream);
      readCatalog(jdomDoc.getRootElement());

    } catch (Exception e) {
      errlog.format("failed to read catalogAsString err='%s'%n", e);
      logger.error("failed to read catalogAsString at" + baseURI.toString(), e);
      e.printStackTrace();
      fatalError = true;
    }
  }

  /* <xsd:element name="catalog">
     <xsd:complexType>
       <xsd:sequence>
         <xsd:element ref="service" minOccurs="0" maxOccurs="unbounded"/>
         <xsd:element ref="property" minOccurs="0" maxOccurs="unbounded" />
         <xsd:element ref="dataset" minOccurs="1" maxOccurs="unbounded" />
       </xsd:sequence>

       <xsd:attribute name="name" type="xsd:string" />
       <xsd:attribute name="expires" type="dateType"/>
       <xsd:attribute name="version" type="xsd:token" default="1.0.2" />
     </xsd:complexType>
   </xsd:element>
   */
  private void readCatalog(Element catalogElem) {

    String name = catalogElem.getAttributeValue("name");
    String catSpecifiedBaseURL = catalogElem.getAttributeValue("base");   // LOOK what is this ??
    String expiresS = catalogElem.getAttributeValue("expires");
    String version = catalogElem.getAttributeValue("version");

    CalendarDate expires = null;
    if (expiresS != null) {
      try {
        expires = CalendarDateFormatter.isoStringToCalendarDate(null, expiresS);
      } catch (Exception e) {
        errlog.format("bad expires date '%s' err='%s'%n", expiresS, e.getMessage());
      }
    }

    if (catSpecifiedBaseURL != null) {
      try {
        URI userSpecifiedBaseUri = new URI(catSpecifiedBaseURL);
        this.baseURI = userSpecifiedBaseUri;
      } catch (URISyntaxException e) {
        errlog.format("readCatalog(): bad catalog specified base URI='%s' %n", catSpecifiedBaseURL);
      }
    }

    setName(name);
    setExpires(expires);
    setVersion(version);

    // read top-level services
    java.util.List<Element> sList = catalogElem.getChildren("service", Catalog.defNS);
    for (Element e : sList) {
      addService(readService(e));
    }

    // read top-level properties
    java.util.List<Element> pList = catalogElem.getChildren("property", Catalog.defNS);
    for (Element e : pList) {
      addProperty(readProperty(e));
    }

    // look for top-level dataset and catalogRefs elements (keep them in order)
    java.util.List<Element> allChildren = catalogElem.getChildren();
    for (Element e : allChildren) {
      if (e.getName().equals("dataset")) {
        addDataset(readDataset(null, e));

      } else if (e.getName().equals("catalogRef")) {
        addDataset(readCatalogRef(null, e));

      } else {
        addDataset(buildOtherDataset(null, e));
      }
    }
  }

  // for overridding
  protected DatasetBuilder buildOtherDataset(DatasetBuilder parent, Element dsElem) {
    return null;
  }

  /* <xsd:element name="access">
     <xsd:complexType>
       <xsd:sequence>
         <xsd:element ref="dataSize" minOccurs="0"/>   // whyd we do that ?
       </xsd:sequence>
       <xsd:attribute name="urlPath" type="xsd:token" use="required"/>
       <xsd:attribute name="serviceName" type="xsd:string"/>
       <xsd:attribute name="dataFormat" type="dataFormatTypes"/>
     </xsd:complexType>
   </xsd:element >
  */
  protected AccessBuilder readAccess(DatasetBuilder dataset, Element accessElem) {
    String urlPath = accessElem.getAttributeValue("urlPath");
    String serviceName = accessElem.getAttributeValue("serviceName");
    String dataFormat = accessElem.getAttributeValue("dataFormat");

    Service s = serviceMap.get(serviceName);
    if (s == null) {
      errlog.format("Cant find service name='%s'%n", serviceName);
    }
    return new AccessBuilder(dataset, urlPath, s, dataFormat, readDataSize(accessElem));
  }

  protected Property readProperty(Element s) {
    String name = s.getAttributeValue("name");
    String value = s.getAttributeValue("value");
    return new Property(name, value);
  }

  /* <xsd:element name="service">
    <xsd:complexType>
     <xsd:sequence>
       <xsd:element ref="property" minOccurs="0" maxOccurs="unbounded" />
       <xsd:element ref="service" minOccurs="0" maxOccurs="unbounded" />
     </xsd:sequence>

     <xsd:attribute name="name" type="xsd:string" use="required" />
     <xsd:attribute name="base" type="xsd:string" use="required" />
     <xsd:attribute name="serviceType" type="serviceTypes" use="required" />
     <xsd:attribute name="desc" type="xsd:string"/>
     <xsd:attribute name="suffix" type="xsd:string" />
    </xsd:complexType>
   </xsd:element>
   */
  protected Service readService(Element s) {
    String name = s.getAttributeValue("name");
    String typeS = s.getAttributeValue("serviceType");
    String serviceBase = s.getAttributeValue("base");
    String suffix = s.getAttributeValue("suffix");
    String desc = s.getAttributeValue("desc");

    ServiceType type = ServiceType.getServiceTypeIgnoreCase(typeS);
    if (type == null) {
      errlog.format(" non-standard service type = '%s'%n", typeS);
    }

    List<Property> properties = null;
    List<Element> propertyList = s.getChildren("property", Catalog.defNS);
    for (Element e : propertyList) {
      if (properties == null) properties = new ArrayList<>();
      properties.add(readProperty(e));
    }

    // nested services
    List<Service> services = null;
    java.util.List<Element> serviceList = s.getChildren("service", Catalog.defNS);
    for (Element e : serviceList) {
      if (services == null) services = new ArrayList<>();
      services.add(readService(e));
    }

    Service result = new Service(name, serviceBase, typeS, desc, suffix, services, properties);
    serviceMap.put(name, result);
    return result;
  }


  /*
    <xsd:element name="catalogRef" substitutionGroup="dataset">
    <xsd:complexType>
      <xsd:complexContent>
        <xsd:extension base="DatasetType">
          <xsd:attributeGroup ref="XLink"/>
          <xsd:attribute name="useRemoteCatalogService" type="xsd:boolean"/>
        </xsd:extension>
      </xsd:complexContent>
    </xsd:complexType>
  </xsd:element>
   */
  protected DatasetBuilder readCatalogRef(DatasetBuilder parent, Element catRefElem) {
    String title = catRefElem.getAttributeValue("title", Catalog.xlinkNS);
    if (title == null) title = catRefElem.getAttributeValue("name");
    String href = catRefElem.getAttributeValue("href", Catalog.xlinkNS);
    CatalogRefBuilder catRef = new CatalogRefBuilder(parent);
    readDatasetInfo( catRef, catRefElem);
    catRef.setTitle(title);
    catRef.setHref(href);
    return catRef;
  }

  /* <xsd:complexType name="DatasetType">
     <xsd:sequence>
       <xsd:group ref="threddsMetadataGroup" minOccurs="0" maxOccurs="unbounded" />

       <xsd:element ref="access" minOccurs="0" maxOccurs="unbounded"/>
       <xsd:element ref="ncml:netcdf" minOccurs="0"/>
       <xsd:element ref="dataset" minOccurs="0" maxOccurs="unbounded"/>
     </xsd:sequence>

     <xsd:attribute name="name" type="xsd:string" use="required"/>
     <xsd:attribute name="alias" type="xsd:token"/>
     <xsd:attribute name="authority" type="xsd:string"/> <!-- deprecated : use element -->
     <xsd:attribute name="collectionType" type="collectionTypes"/>
     <xsd:attribute name="dataType" type="dataTypes"/> <!-- deprecated : use element -->
     <xsd:attribute name="harvest" type="xsd:boolean"/>
     <xsd:attribute name="ID" type="xsd:token"/>
     <xsd:attribute name="resourceControl" type="xsd:string"/>

     <xsd:attribute name="serviceName" type="xsd:string" /> <!-- deprecated : use element -->
     <xsd:attribute name="urlPath" type="xsd:token" />
   </xsd:complexType> */
  protected DatasetBuilder readDataset(DatasetBuilder parent, Element dsElem) {

    DatasetBuilder dataset = new DatasetBuilder(parent);
    readDatasetInfo(dataset, dsElem);

    // look for access elements
    java.util.List<Element> aList = dsElem.getChildren("access", Catalog.defNS);
    for (Element e : aList) {
      dataset.addAccess(readAccess(dataset, e));
    }

    // look for nested dataset and catalogRefs elements (keep them in order)
    java.util.List<Element> allChildren = dsElem.getChildren();
    for (Element e : allChildren) {
      if (e.getName().equals("dataset")) {
        dataset.addDataset(readDataset(dataset, e));

      } else if (e.getName().equals("catalogRef")) {
        dataset.addDataset(readCatalogRef(dataset, e));

      } else {
        dataset.addDataset( buildOtherDataset(dataset, e));
      }
    }

    return dataset;
  }

  protected void readDatasetInfo(DatasetBuilder dataset, Element dsElem) {
    // read attributes
    String name = dsElem.getAttributeValue("name");
    if (name == null) {
      if (dsElem.getName().equals("catalogRef"))
        dataset.setName("");
      else
        errlog.format(" ** warning: dataset must have a name = '%s'%n", dsElem);
    } else {
      dataset.setName(name);
    }

    dataset.put( Dataset.Alias, dsElem.getAttributeValue("alias"));
    dataset.put( Dataset.Authority, dsElem.getAttributeValue("authority"));
    dataset.put( Dataset.CollectionType, dsElem.getAttributeValue("collectionType"));
    dataset.put( Dataset.Id, dsElem.getAttributeValue("ID"));
    dataset.putInheritedField( Dataset.RestrictAccess, dsElem.getAttributeValue("restrictAccess"));
    dataset.put( Dataset.ServiceName, dsElem.getAttributeValue("serviceName"));
    dataset.put( Dataset.UrlPath, dsElem.getAttributeValue("urlPath"));

    String dataTypeName = dsElem.getAttributeValue("dataType");
    dataset.put( Dataset.FeatureType, dataTypeName);
    if (dataTypeName != null) {
      FeatureType dataType = FeatureType.getType(dataTypeName.toUpperCase());
      if (dataType == null) {
        errlog.format(" ** warning: non-standard data type = '%s'%n", dataTypeName);
      }
    }

    String harvest = dsElem.getAttributeValue("harvest");
    if (harvest != null && harvest.equalsIgnoreCase("true")) dataset.put(Dataset.Harvest, Boolean.TRUE);

    // catalog.addDatasetByID(dataset); // LOOK need to do immed for alias processing

    // read elements
    readThreddsMetadataGroup(dataset.flds, dataset, dsElem);
  }

  /*
   <!-- group of elements can be used in a dataset or in metadata elements -->
  <xsd:group name="threddsMetadataGroup">
    <xsd:choice>
      <xsd:element name="documentation" type="documentationType"/>
      <xsd:element ref="metadata"/>
      <xsd:element ref="property"/>

      <xsd:element ref="contributor"/>
      <xsd:element name="creator" type="sourceType"/>
      <xsd:element name="date" type="dateTypeFormatted"/>
      <xsd:element name="keyword" type="controlledVocabulary"/>
      <xsd:element name="project" type="controlledVocabulary"/>
      <xsd:element name="publisher" type="sourceType"/>

      <xsd:element ref="geospatialCoverage"/>
      <xsd:element name="timeCoverage" type="timeCoverageType"/>
      <xsd:element ref="variables"/>
      <xsd:element ref="variableMap"/>

      <xsd:element name="dataType" type="dataTypes"/>
      <xsd:element name="dataFormat" type="dataFormatTypes"/>
      <xsd:element name="serviceName" type="xsd:string"/>
      <xsd:element name="authority" type="xsd:string"/>
      <xsd:element ref="dataSize"/>
    </xsd:choice>
  </xsd:group>
   */
  protected void readThreddsMetadataGroup(Map<String,Object> flds, DatasetBuilder dataset, Element parent) {
    List<Element> list;

    // look for creators - kind of a Source
    list = parent.getChildren("creator", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Creators, readSource(e));
    }

    // look for contributors
    list = parent.getChildren("contributor", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Contributors, readContributor(e));
    }

    // look for dates
    list = parent.getChildren("date", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Dates, readDate(e));
    }

    // look for documentation
    list = parent.getChildren("documentation", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Documentation, readDocumentation(e));
    }

    // look for keywords - kind of a controlled vocabulary
    list = parent.getChildren("keyword", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Keywords, readControlledVocabulary(e));
    }

    // look for metadata elements
    list = parent.getChildren("metadata", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.MetadataOther, readMetadata(flds, dataset, e));
    }

    // look for projects - kind of a controlled vocabulary
    list = parent.getChildren("project", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Projects, readControlledVocabulary(e));
    }

    // look for properties
    list = parent.getChildren("property", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Properties, readProperty(e));
    }

    // look for publishers - kind of a Source
    list = parent.getChildren("publisher", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Publishers, readSource(e));
    }

    // look for variables
    list = parent.getChildren("variables", Catalog.defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.VariableGroups, readVariables(e));
    }

    // can only be one each of these kinds
    ThreddsMetadata.GeospatialCoverage gc = readGeospatialCoverage(parent.getChild("geospatialCoverage", Catalog.defNS));
    if (gc != null) flds.put(Dataset.GeospatialCoverage, gc);

    DateRange tc = readTimeCoverage(parent.getChild("timeCoverage", Catalog.defNS));
    if (tc != null) flds.put(Dataset.TimeCoverage, tc);

    Element serviceNameElem = parent.getChild("serviceName", Catalog.defNS);
    if (serviceNameElem != null) flds.put(Dataset.ServiceName, serviceNameElem.getText());

    Element authElem = parent.getChild("authority", Catalog.defNS);
    if (authElem != null) flds.put(Dataset.Authority, authElem.getText());

    Element dataTypeElem = parent.getChild("dataType", Catalog.defNS);
    if (dataTypeElem != null) {
      String dataTypeName = dataTypeElem.getText();
      flds.put(Dataset.FeatureType, dataTypeName);
      if ((dataTypeName != null) && (dataTypeName.length() > 0)) {
        FeatureType dataType = FeatureType.getType(dataTypeName.toUpperCase());
        if (dataType == null) {
          errlog.format(" ** warning: non-standard feature type = '%s'%n", dataTypeName);
        }
      }
    }

    Element dataFormatElem = parent.getChild("dataFormat", Catalog.defNS);
    if (dataFormatElem != null) {
      String dataFormatTypeName = dataFormatElem.getText();
      if ((dataFormatTypeName != null) && (dataFormatTypeName.length() > 0)) {
        DataFormatType dataFormatType = DataFormatType.getType(dataFormatTypeName);
        if (dataFormatType == null) {
          errlog.format(" ** warning: non-standard dataFormat type = '%s'%n", dataFormatTypeName);
        }
        flds.put(Dataset.DataFormatType, dataFormatTypeName);
      }
    }

    long size = readDataSize(parent);
    if (size > 0)
      flds.put(Dataset.DataSize, size);

    // LOOK: we seem to have put a variableMap element not contained by <variables>
    ThreddsMetadata.UriResolved mapUri = readUri(parent.getChild("variableMap", Catalog.defNS), "variableMap");
    if (mapUri != null)
      flds.put(Dataset.VariableMapLinkURI, mapUri);
  }


  protected ThreddsMetadata.Contributor readContributor(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Contributor(elem.getText(), elem.getAttributeValue("role"));
  }

  protected long readDataSize(Element parent) {
    Element elem = parent.getChild("dataSize", Catalog.defNS);
    if (elem == null) return -1;

    double size;
    String sizeS = elem.getText();
    try {
      size = Double.parseDouble(sizeS);
    } catch (NumberFormatException e) {
      errlog.format(" ** Parse error: Bad double format in size element = '%s'%n", sizeS);
      return -1;
    }

    String units = elem.getAttributeValue("units");
    char c = Character.toUpperCase(units.charAt(0));
    if (c == 'K') size *= 1000;
    else if (c == 'M') size *= 1000 * 1000;
    else if (c == 'G') size *= 1000 * 1000 * 1000;
    else if (c == 'T') size *= 1000.0 * 1000 * 1000 * 1000;
    else if (c == 'P') size *= 1000.0 * 1000 * 1000 * 1000 * 1000;
    return (long) size;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  protected Documentation readDocumentation(Element s) {
    String href = s.getAttributeValue("href", Catalog.xlinkNS);
    String title = s.getAttributeValue("title", Catalog.xlinkNS);
    String type = s.getAttributeValue("type"); // not XLink type
    String content = s.getTextNormalize();

    URI uri = null;
    if (href != null) {
      try {
        uri = Catalog.resolveUri(baseURI, href);
      } catch (Exception e) {
        errlog.format(" ** Invalid documentation href = '%s' err='%s'%n", href, e.getMessage());
      }
    }

    return new Documentation(href, uri, title, type, content);
  }

  protected double readDouble(Element elem) {
    if (elem == null) return Double.NaN;
    String text = elem.getText();
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      errlog.format(" ** Parse error: Bad double format = '%s'%n", text);
      return Double.NaN;
    }
  }

  /*
    <xsd:element name="geospatialCoverage">
    <xsd:complexType>
      <xsd:sequence>
        <xsd:element name="northsouth" type="spatialRange" minOccurs="0"/>
        <xsd:element name="eastwest" type="spatialRange" minOccurs="0"/>
        <xsd:element name="updown" type="spatialRange" minOccurs="0"/>
        <xsd:element name="name" type="controlledVocabulary" minOccurs="0" maxOccurs="unbounded"/>
      </xsd:sequence>

      <xsd:attribute name="zpositive" type="upOrDown" default="up"/>
    </xsd:complexType>
  </xsd:element>

  <xsd:complexType name="spatialRange">
    <xsd:sequence>
      <xsd:element name="start" type="xsd:double"/>
      <xsd:element name="size" type="xsd:double"/>
      <xsd:element name="resolution" type="xsd:double" minOccurs="0"/>
      <xsd:element name="units" type="xsd:string" minOccurs="0"/>
    </xsd:sequence>
  </xsd:complexType>

  <xsd:simpleType name="upOrDown">
    <xsd:restriction base="xsd:token">
      <xsd:enumeration value="up"/>
      <xsd:enumeration value="down"/>
    </xsd:restriction>
  </xsd:simpleType>
   */
  protected ThreddsMetadata.GeospatialCoverage readGeospatialCoverage(Element gcElem) {
    if (gcElem == null) return null;

    String zpositive = gcElem.getAttributeValue("zpositive");

    ThreddsMetadata.GeospatialRange northsouth = readGeospatialRange(gcElem.getChild("northsouth", Catalog.defNS), CDM.LAT_UNITS);
    ThreddsMetadata.GeospatialRange eastwest = readGeospatialRange(gcElem.getChild("eastwest", Catalog.defNS), CDM.LON_UNITS);
    ThreddsMetadata.GeospatialRange updown = readGeospatialRange(gcElem.getChild("updown", Catalog.defNS), "m");

    // look for names
    List<ThreddsMetadata.Vocab> names = new ArrayList<>();
    java.util.List<Element> list = gcElem.getChildren("name", Catalog.defNS);
    for (Element e : list) {
      ThreddsMetadata.Vocab name = readControlledVocabulary(e);
      names.add(name);
    }

    return new ThreddsMetadata.GeospatialCoverage(eastwest, northsouth, updown, names, zpositive);
  }

  protected ThreddsMetadata.GeospatialRange readGeospatialRange(Element spElem, String defUnits) {
    if (spElem == null) return null;

    double start = readDouble(spElem.getChild("start", Catalog.defNS));
    double size = readDouble(spElem.getChild("size", Catalog.defNS));
    double resolution = readDouble(spElem.getChild("resolution", Catalog.defNS));

    String units = spElem.getChildText("units", Catalog.defNS);
    if (units == null) units = defUnits;

    return new ThreddsMetadata.GeospatialRange(start, size, resolution, units);
  }

  /*
    <xsd:element name="metadata">
    <xsd:complexType>
      <xsd:choice>
        <xsd:group ref="threddsMetadataGroup" minOccurs="0" maxOccurs="unbounded"/>
        <xsd:any namespace="##other" minOccurs="0" maxOccurs="unbounded" processContents="lax"/>
      </xsd:choice>

      <xsd:attribute name="inherited" type="xsd:boolean" default="false"/>
      <xsd:attribute name="metadataType" type="metadataTypeEnum"/>
      <xsd:attributeGroup ref="XLink"/>
    </xsd:complexType>
  </xsd:element>
   */
  protected ThreddsMetadata.MetadataOther readMetadata(Map<String,Object> flds, DatasetBuilder dataset, Element mdataElement) {
    // there are 6 cases to deal with: threddsNamespace vs not & inline vs Xlink & (if thredds) inherited or not
    Namespace namespace;
    List inlineElements = mdataElement.getChildren();
    if (inlineElements.size() > 0) // look at the namespace of the children, if they exist
      namespace = ((Element) inlineElements.get(0)).getNamespace();
    else
      namespace = mdataElement.getNamespace(); // will be thredds

    String mtype = mdataElement.getAttributeValue("metadataType");
    String href = mdataElement.getAttributeValue("href", Catalog.xlinkNS);
    String title = mdataElement.getAttributeValue("title", Catalog.xlinkNS);
    String inheritedS = mdataElement.getAttributeValue("inherited");
    boolean inherited = (inheritedS != null) && inheritedS.equalsIgnoreCase("true");

    boolean isThreddsNamespace = ((mtype == null) || mtype.equalsIgnoreCase("THREDDS")) && namespace.getURI().equals(Catalog.CATALOG_NAMESPACE_10);

    // the case where its not ThreddsMetadata
    if (!isThreddsNamespace) {
      if (inlineElements.size() > 0) {
        // just hold onto the jdom elements as the "content"
         return new ThreddsMetadata.MetadataOther( mtype, namespace.getURI(), namespace.getPrefix(), inherited, mdataElement);

      } else { // otherwise it must be an Xlink
        return new ThreddsMetadata.MetadataOther(href, title, mtype, namespace.getURI(), namespace.getPrefix(), inherited);
      }
    }

    // the case where its ThreddsMetadata
    Map<String,Object> useFlds;
    if (inherited) {
      // the case where its inherited ThreddsMetadata: gonna put stuff in the tmi.
      ThreddsMetadata tmi = (ThreddsMetadata) dataset.get(Dataset.ThreddsMetadataInheritable);
      if (tmi == null) {
        tmi = new ThreddsMetadata();
        dataset.put(Dataset.ThreddsMetadataInheritable, tmi);
      }
      useFlds = tmi.getFlds();

    } else {
      // the case where its non-inherited ThreddsMetadata: gonna put stuff directly into the dataset
      useFlds = flds;
    }
    readThreddsMetadataGroup(useFlds, dataset, mdataElement);

    // also need to capture any XLinks. see http://www.unidata.ucar.edu/software/thredds/v4.6/tds/catalog/InvCatalogSpec.html#metadataElement
    // in this case we just suck it in as if it was inline
    if (href != null) {
      try {
        URI xlinkUri = Catalog.resolveUri(baseURI, href);
        Element remoteMdata = readMetadataFromUrl(xlinkUri);
        return readMetadata(useFlds, dataset, remoteMdata);
      } catch (Exception ioe) {
        errlog.format("Cant read in referenced metadata %s err=%s%n", href, ioe.getMessage());
      }

    }
    return null;  // ThreddsMetadata.MetadataOther was directly added
  }

  private Element readMetadataFromUrl(java.net.URI uri) throws java.io.IOException {
    SAXBuilder saxBuilder = new SAXBuilder();
    Document doc;
    try {
      doc = saxBuilder.build(uri.toURL());
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
    return doc.getRootElement();
  }

  /*
    <xsd:complexType name="sourceType">
    <xsd:sequence>
      <xsd:element name="name" type="controlledVocabulary"/>

      <xsd:element name="contact">
        <xsd:complexType>
          <xsd:attribute name="email" type="xsd:string" use="required"/>
          <xsd:attribute name="url" type="xsd:anyURI"/>
        </xsd:complexType>
      </xsd:element>

    </xsd:sequence>
  </xsd:complexType>
   */
  protected ThreddsMetadata.Source readSource(Element elem) {
    if (elem == null) return null;
    ThreddsMetadata.Vocab name = readControlledVocabulary(elem.getChild("name", Catalog.defNS));
    Element contact = elem.getChild("contact", Catalog.defNS);
    if (contact == null) {
      errlog.format(" ** Parse error: Missing contact element in = '%s'%n", elem.getName());
      return null;
    }
    return new ThreddsMetadata.Source(name, contact.getAttributeValue("url"), contact.getAttributeValue("email"));
  }

  /*
    <xsd:complexType name="timeCoverageType">
    <xsd:sequence>
      <xsd:choice minOccurs="2" maxOccurs="3">
        <xsd:element name="start" type="dateTypeFormatted"/>
        <xsd:element name="end" type="dateTypeFormatted"/>
        <xsd:element name="duration" type="duration"/>
      </xsd:choice>

      <xsd:element name="resolution" type="duration" minOccurs="0"/>
    </xsd:sequence>
  </xsd:complexType>

  <!-- may be a dateType or have a format attribute  -->
  <xsd:complexType name="dateTypeFormatted">
    <xsd:simpleContent>
      <xsd:extension base="dateType">
        <xsd:attribute name="format" type="xsd:string"/>
        <!-- follow java.text.SimpleDateFormat -->
        <xsd:attribute name="type" type="dateEnumTypes"/>
      </xsd:extension>
    </xsd:simpleContent>
  </xsd:complexType>

  <!-- may be a built in date or dateTIme, or a udunit encoded string -->
  <xsd:simpleType name="dateType">
    <xsd:union memberTypes="xsd:date xsd:dateTime udunitDate">
      <xsd:simpleType>
        <xsd:restriction base="xsd:token">
          <xsd:enumeration value="present"/>
        </xsd:restriction>
      </xsd:simpleType>
    </xsd:union>
  </xsd:simpleType>

  <xsd:simpleType name="udunitDate">
    <xsd:restriction base="xsd:string">
      <xsd:annotation>
        <xsd:documentation>Must conform to complete udunits date string, eg "20 days since 1991-01-01"</xsd:documentation>
      </xsd:annotation>
    </xsd:restriction>
  </xsd:simpleType>

  <xsd:simpleType name="duration">
    <xsd:union memberTypes="xsd:duration udunitDuration"/>
  </xsd:simpleType>

  <xsd:simpleType name="udunitDuration">
    <xsd:restriction base="xsd:string">
      <xsd:annotation>
        <xsd:documentation>Must conform to udunits time duration, eg "20.1 hours"</xsd:documentation>
      </xsd:annotation>
    </xsd:restriction>
  </xsd:simpleType>
   */
  protected DateRange readTimeCoverage(Element tElem) {
    if (tElem == null) return null;

    DateType start = readDate(tElem.getChild("start", Catalog.defNS));
    DateType end = readDate(tElem.getChild("end", Catalog.defNS));
    TimeDuration duration = readDuration(tElem.getChild("duration", Catalog.defNS));
    TimeDuration resolution = readDuration(tElem.getChild("resolution", Catalog.defNS));

    try {
      return new DateRange(start, end, duration, resolution);
    } catch (java.lang.IllegalArgumentException e) {
      errlog.format(" ** warning: TimeCoverage error ='%s'%n", e.getMessage());
      return null;
    }
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
      errlog.format(" ** Parse error: Bad date format = '%s'%n", text);
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
      errlog.format(" ** Parse error: Bad duration format = '%s'%n", text);
      return null;
    }
  }

  /*
    <xsd:element name="variables">
    <xsd:complexType>
      <xsd:choice>
        <xsd:element ref="variable" minOccurs="0" maxOccurs="unbounded"/>
        <xsd:element ref="variableMap" minOccurs="0"/>
      </xsd:choice>
      <xsd:attribute name="vocabulary" type="variableNameVocabulary" use="optional"/>
      <xsd:attributeGroup ref="XLink"/>
    </xsd:complexType>
  </xsd:element>

  <xsd:element name="variable">
    <xsd:complexType mixed="true">
      <xsd:attribute name="name" type="xsd:string" use="required"/>
      <xsd:attribute name="vocabulary_name" type="xsd:string" use="optional"/>
      <xsd:attribute name="vocabulary_id" type="xsd:string" use="optional"/>
      <xsd:attribute name="units" type="xsd:string"/>
    </xsd:complexType>
  </xsd:element>

  <xsd:element name="variableMap">
    <xsd:complexType>
      <xsd:attributeGroup ref="XLink"/>
    </xsd:complexType>
  </xsd:element>

  <xsd:simpleType name="variableNameVocabulary">
    <xsd:union memberTypes="xsd:token">
      <xsd:simpleType>
        <xsd:restriction base="xsd:token">
          <xsd:enumeration value="CF-1.0"/>
          <xsd:enumeration value="DIF"/>
          <xsd:enumeration value="GRIB-1"/>
          <xsd:enumeration value="GRIB-2"/>
        </xsd:restriction>
      </xsd:simpleType>
    </xsd:union>
  </xsd:simpleType>
   */
  protected ThreddsMetadata.VariableGroup readVariables( Element varsElem) {
    if (varsElem == null) return null;

    String vocab = varsElem.getAttributeValue("vocabulary");
    ThreddsMetadata.UriResolved variableVocabUri = readUri(varsElem, "Variables vocabulary");

    java.util.List<Element> vlist = varsElem.getChildren("variable", Catalog.defNS);
    ThreddsMetadata.UriResolved variableMap = readUri(varsElem.getChild("variableMap", Catalog.defNS), "Variables Map");
    if ((variableMap != null) && vlist.size() > 0) { // cant do both
      errlog.format(" ** Catalog error: cant have variableMap and variable in same element '%s'%n", varsElem);
    }

    List<ThreddsMetadata.Variable> variables = new ArrayList<>();
    for (Element e : vlist) {
      variables.add(readVariable(e));
    }

    return new ThreddsMetadata.VariableGroup(vocab, variableVocabUri, variableMap, variables);
  }

  static public ThreddsMetadata.Variable readVariable(Element varElem) {
     if (varElem == null) return null;

     String name = varElem.getAttributeValue("name");
     String desc = varElem.getText();
     String vocabulary_name = varElem.getAttributeValue("vocabulary_name");
     String units = varElem.getAttributeValue("units");
     String id = varElem.getAttributeValue("vocabulary_id");

     return new ThreddsMetadata.Variable(name, desc, vocabulary_name, units, id);
   }

  protected ThreddsMetadata.Vocab readControlledVocabulary(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Vocab(elem.getText(), elem.getAttributeValue("vocabulary"));
  }

  private ThreddsMetadata.UriResolved readUri(Element elemWithHref, String what) {
    if (elemWithHref == null) return null;
    String mapHref = elemWithHref.getAttributeValue("href", Catalog.xlinkNS);
    if (mapHref == null) return null;

    try {
      String mapUri = URLnaming.resolve(baseURI.toString(), mapHref);
      return new ThreddsMetadata.UriResolved(mapHref, new URI(mapUri));
    } catch (Exception e) {
      errlog.format(" ** Invalid %s URI= '%s' err='%s'%n", what, mapHref, e.getMessage());
      return null;
    }
  }


}
