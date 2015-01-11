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
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.io.IOException;
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

  private Formatter errlog;
  private URI docBaseURI;
  private boolean error = false;
  private Map<String, Service> serviceMap = new HashMap<>();

  public JdomCatalogBuilder(Formatter errlog) {
    this.errlog = errlog;
  }

  public boolean readXML(CatalogBuilder catBuilder, URI uri) throws IOException {

    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      org.jdom2.Document jdomDoc = saxBuilder.build(uri.toURL());
      return readCatalog(catBuilder, jdomDoc.getRootElement(), uri);

    } catch (Exception e) {
      errlog.format("failed to read catalog at %s err=%s%n", uri.toString(), e);
      return false;
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
  protected boolean readCatalog(CatalogBuilder catBuilder, Element catalogElem, URI docBaseURI) {
    this.docBaseURI = docBaseURI;

    String name = catalogElem.getAttributeValue("name");
    String catSpecifiedBaseURL = catalogElem.getAttributeValue("base");
    String expiresS = catalogElem.getAttributeValue("expires");
    String version = catalogElem.getAttributeValue("version");

    CalendarDate expires = null;
    if (expiresS != null) {
      try {
        expires = CalendarDateFormatter.isoStringToCalendarDate(null, expiresS);
      } catch (Exception e) {
        errlog.format("bad expires date %s err=%s%n", expiresS, e.getMessage());
      }
    }

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
        catBuilder.addDataset(readDataset(null, e));

      } else if (e.getName().equals("catalogRef")) {
        catBuilder.addDataset(readCatalogRef(null, e));
      }
    }

    return error;
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

    ServiceType type = null;
    try {
      type = ServiceType.getServiceTypeIgnoreCase(typeS);
    } catch (Exception e) {
      errlog.format("bad service type = %s%n", typeS);
      error = true;
    }

    List<Property> properties = null;
    List<Element> propertyList = s.getChildren("property", defNS);
    for (Element e : propertyList) {
      if (properties == null) properties = new ArrayList<>();
      properties.add(readProperty(e));
    }

    // nested services
    List<Service> services = null;
    java.util.List<Element> serviceList = s.getChildren("service", defNS);
    for (Element e : serviceList) {
      if (services == null) services = new ArrayList<>();
      services.add(readService(e));
    }

    Service result = new Service(name, serviceBase, type, suffix, desc, services, properties);
    serviceMap.put(name, result);
    return result;
  }
  
  
  protected DatasetBuilder readCatalogRef(DatasetBuilder parent, Element catRefElem) {
    String title = catRefElem.getAttributeValue("title", xlinkNS);
    if (title == null) title = catRefElem.getAttributeValue("name");
    String href = catRefElem.getAttributeValue("href", xlinkNS);
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
    java.util.List<Element> aList = dsElem.getChildren("access", defNS);
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
      }
    }

    return dataset;
  }
  protected void readDatasetInfo(DatasetBuilder dataset, Element dsElem) {
    // read attributes
    String name = dsElem.getAttributeValue("name");
    if (name == null) 
      errlog.format(" ** warning: dataset must have a name = %s%n", dsElem);
    else
      dataset.setName(name);
    
    dataset.put( Dataset.Alias, dsElem.getAttributeValue("alias"));
    dataset.put( Dataset.Authority, dsElem.getAttributeValue("authority"));
    dataset.put( Dataset.CollectionType, dsElem.getAttributeValue("collectionType"));
    dataset.put( Dataset.Id, dsElem.getAttributeValue("ID"));
    dataset.put( Dataset.ResourceControl, dsElem.getAttributeValue("resourceControl"));          
    dataset.put( Dataset.ServiceName, dsElem.getAttributeValue("serviceName"));          
    dataset.put( Dataset.UrlPath, dsElem.getAttributeValue("urlPath"));

    String dataTypeName = dsElem.getAttributeValue("dataType");
    if (dataTypeName != null) {
      FeatureType dataType = FeatureType.getType(dataTypeName.toUpperCase());
      if (dataType == null) {
        errlog.format(" ** warning: non-standard data type = %s%n", dataTypeName);
      } else {
        dataset.put( Dataset.FeatureType, dataType);
      }
    }

    String harvest = dsElem.getAttributeValue("harvest");
    if (harvest != null && harvest.equalsIgnoreCase("true")) dataset.put(Dataset.Harvest, Boolean.TRUE);

    // catalog.addDatasetByID(dataset); // need to do immed for alias processing

    // read attributes
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
    list = parent.getChildren("creator", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Creators, readSource(e));
    }

    // look for contributors
    list = parent.getChildren("contributor", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Contributors, readContributor(e));
    }

    // look for dates
    list = parent.getChildren("date", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Dates, readDate(e));
    }

    // look for documentation
    list = parent.getChildren("documentation", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Documentation, readDocumentation(e));
    }

    // look for keywords - kind of a controlled vocabulary
    list = parent.getChildren("keyword", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Keywords, readControlledVocabulary(e));
    }

    // look for metadata elements
    list = parent.getChildren("metadata", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.MetadataOther, readMetadata(flds, dataset, e));
    }

    // look for projects - kind of a controlled vocabulary
    list = parent.getChildren("project", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Projects, readControlledVocabulary(e));
    }

    // look for properties
    list = parent.getChildren("property", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Properties, readProperty(e));
    }

    // look for publishers - kind of a Source
    list = parent.getChildren("publisher", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.Publishers, readSource(e));
    }

    // look for variables
    list = parent.getChildren("variables", defNS);
    for (Element e : list) {
      DatasetBuilder.addToList(flds, Dataset.VariableGroups, readVariables(e));
    }

    // can only be one each of these kinds
    ThreddsMetadata.GeospatialCoverage gc = readGeospatialCoverage(parent.getChild("geospatialCoverage", defNS));
    if (gc != null) flds.put(Dataset.GeospatialCoverage, gc);

    DateRange tc = readTimeCoverage(parent.getChild("timeCoverage", defNS));
    if (tc != null) flds.put(Dataset.TimeCoverage, tc);

    Element serviceNameElem = parent.getChild("serviceName", defNS);
    if (serviceNameElem != null) flds.put(Dataset.ServiceName, serviceNameElem.getText());

    Element authElem = parent.getChild("authority", defNS);
    if (authElem != null) flds.put(Dataset.Authority, authElem.getText());

    Element dataTypeElem = parent.getChild("dataType", defNS);
    if (dataTypeElem != null) {
      String dataTypeName = dataTypeElem.getText();
      if ((dataTypeName != null) && (dataTypeName.length() > 0)) {
        FeatureType dataType = FeatureType.getType(dataTypeName.toUpperCase());
        if (dataType == null) {
          errlog.format(" ** warning: non-standard feature type = %s%n", dataTypeName);
        }
        flds.put(Dataset.FeatureType, dataTypeName);
      }
    }

    Element dataFormatElem = parent.getChild("dataFormat", defNS);
    if (dataFormatElem != null) {
      String dataFormatTypeName = dataFormatElem.getText();
      if ((dataFormatTypeName != null) && (dataFormatTypeName.length() > 0)) {
        DataFormatType dataFormatType = DataFormatType.getType(dataFormatTypeName);
        if (dataFormatType == null) {
          errlog.format(" ** warning: non-standard dataFormat type = %s%n", dataFormatTypeName);
        }
        flds.put(Dataset.DataFormatType, dataFormatTypeName);
      }
    }

    long size = readDataSize(parent);
    if (size > 0)
      flds.put(Dataset.DataSize, size);

    URI mapUri = readVariableMap(parent.getChild("variableMap", defNS));
    if (mapUri != null)
      flds.put(Dataset.VariableMapLink, mapUri);
  }

  protected long readDataSize(Element parent) {
    Element elem = parent.getChild("dataSize", defNS);
    if (elem == null) return -1;

    double size;
    String sizeS = elem.getText();
    try {
      size = Double.parseDouble(sizeS);
    } catch (NumberFormatException e) {
      errlog.format(" ** Parse error: Bad double format in size element = %s%n", sizeS);
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
      errlog.format(" ** Parse error: Bad date format = %s%n", text);
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
      errlog.format(" ** Parse error: Bad duration format = %s%n", text);
      return null;
    }
  }

  protected Documentation readDocumentation(Element s) {
    String href = s.getAttributeValue("href", xlinkNS);
    String title = s.getAttributeValue("title", xlinkNS);
    String type = s.getAttributeValue("type"); // not XLink type
    String content = s.getTextNormalize();

    URI uri = null;
    if (href != null) {
      try {
        uri =  docBaseURI.resolve(href);
      } catch (Exception e) {
        errlog.format(" ** Invalid documentation href = %s err=%s%n", href, e.getMessage());
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
      errlog.format(" ** Parse error: Bad double format = %s%n", text);
      return Double.NaN;
    }
  }

  protected ThreddsMetadata.GeospatialCoverage readGeospatialCoverage(Element gcElem) {
    if (gcElem == null) return null;

    String zpositive = gcElem.getAttributeValue("zpositive");

    ThreddsMetadata.GeospatialRange northsouth = readGeospatialRange(gcElem.getChild("northsouth", defNS), CDM.LAT_UNITS);
    ThreddsMetadata.GeospatialRange eastwest = readGeospatialRange(gcElem.getChild("eastwest", defNS), CDM.LON_UNITS);
    ThreddsMetadata.GeospatialRange updown = readGeospatialRange(gcElem.getChild("updown", defNS), "m");

    // look for names
    List<ThreddsMetadata.Vocab> names = new ArrayList<>();
    java.util.List<Element> list = gcElem.getChildren("name", defNS);
    for (Element e : list) {
      ThreddsMetadata.Vocab name = readControlledVocabulary(e);
      names.add(name);
    }

    return new ThreddsMetadata.GeospatialCoverage(eastwest, northsouth, updown, names, zpositive);
  }

  protected ThreddsMetadata.GeospatialRange readGeospatialRange(Element spElem, String defUnits) {
    if (spElem == null) return null;

    double start = readDouble(spElem.getChild("start", defNS));
    double size = readDouble(spElem.getChild("size", defNS));
    double resolution = readDouble(spElem.getChild("resolution", defNS));

    String units = spElem.getChildText("units", defNS);
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
    // there are 4 cases to deal with: threddsNamespace vs not & inline vs Xlink
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

    boolean isThreddsNamespace = ((mtype == null) || mtype.equalsIgnoreCase("THREDDS")) && namespace.getURI().equals(CATALOG_NAMESPACE_10);

    // the case where its not ThreddsMetadata
    if (!isThreddsNamespace) {
      ThreddsMetadata.MetadataOther mo;
      if (inlineElements.size() > 0) {
        // just hold onto the jdom elements as the "content"
         mo = new ThreddsMetadata.MetadataOther( mtype, namespace.getURI(), namespace.getPrefix(), inherited, mdataElement);

      } else { // otherwise it must be an Xlink
        mo = new ThreddsMetadata.MetadataOther(href, title, mtype, namespace.getURI(), namespace.getPrefix(), inherited);
      }
      return mo;
    }

    // the case where its ThreddsMetadata:
    if (inherited) {  // gonna put stuff in the tmi.
      ThreddsMetadata tmi = (ThreddsMetadata) dataset.get(Dataset.ThreddsMetadataInheritable);
      if (tmi == null) {
        tmi = new ThreddsMetadata();
        dataset.put(Dataset.ThreddsMetadataInheritable, tmi);
      }
      readThreddsMetadataGroup(tmi.getFlds(), dataset, mdataElement);

    } else { // not inherited - stick it directly into the dataset
      readThreddsMetadataGroup(flds, dataset, mdataElement);
    }

    return null;
  }

  private Element readContentFromURL(java.net.URI uri) throws java.io.IOException {
    SAXBuilder saxBuilder = new SAXBuilder();
    Document doc;
    try {
      doc = saxBuilder.build(uri.toURL());
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    return doc.getRootElement();
  }

  protected ThreddsMetadata.Source readSource(Element elem) {
    if (elem == null) return null;
    ThreddsMetadata.Vocab name = readControlledVocabulary(elem.getChild("name", defNS));
    Element contact = elem.getChild("contact", defNS);
    if (contact == null) {
      errlog.format(" ** Parse error: Missing contact element in = %s%n", elem.getName());
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
      errlog.format(" ** warning: TimeCoverage error =%s%n", e.getMessage());
      return null;
    }
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


  protected ThreddsMetadata.VariableGroup readVariables( Element varsElem) {
    if (varsElem == null) return null;

    String vocab = varsElem.getAttributeValue("vocabulary");
    String vocabHref = varsElem.getAttributeValue("href", xlinkNS);

    URI vocabUri = null;
    if (vocabHref != null) {
      try {
        vocabUri =  docBaseURI.resolve(vocabHref);
      } catch (Exception e) {
        errlog.format(" ** Invalid Variables vocabulary URI= %s err=%s%n",vocabHref,e.getMessage());
      }
    }

    java.util.List<Element> vlist = varsElem.getChildren("variable", defNS);

    URI mapUri = readVariableMap(varsElem.getChild("variableMap", defNS));
    if ((mapUri != null) && vlist.size() > 0) { // cant do both
      errlog.format(" ** Catalog error: cant have variableMap and variable in same element%n", varsElem);
      mapUri = null;
    }

    List<ThreddsMetadata.Variable> variables = null;
    if (vlist.size() > 0) {
      variables = new ArrayList<>();
      for (Element e : vlist) {
        variables.add(readVariable(e));
      }
    }

    // read in variable map LOOK: would like to defer
    if (mapUri != null) {
      try {
        Element varsElement = readContentFromURL(mapUri);
        List<Element> list = varsElement.getChildren("variable", defNS);
        if (vlist.size() > 0) {
          variables = new ArrayList<>();
          for (Element e : list) {
            variables.add(readVariable(e));
          }
        }
      } catch (IOException e) {
        errlog.format("Failure reading variable %s mapUri err=%s%n", vocab, e.getMessage());
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

    return new ThreddsMetadata.VariableGroup(vocab, vocabHref, vocabUri, mapUri, variables);
  }
  
  protected ThreddsMetadata.Vocab readControlledVocabulary(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Vocab(elem.getText(), elem.getAttributeValue("vocabulary"));
  }

  protected ThreddsMetadata.Contributor readContributor(Element elem) {
    if (elem == null) return null;
    return new ThreddsMetadata.Contributor(elem.getText(), elem.getAttributeValue("role"));
  }

  private URI readVariableMap(Element variableMapElem) {
    if (variableMapElem == null) return null;
    String mapHref = null;
    mapHref = variableMapElem.getAttributeValue("href", xlinkNS);
    URI mapUri = null;
    try {
      mapUri =  docBaseURI.resolve(mapHref);
    } catch (Exception e) {
      errlog.format(" ** Invalid Variables map URI= %s err=%s%n", mapHref, e.getMessage());
    }
    return mapUri;
  }

}
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  /*



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
       errlog.format(" ** Parse error: Bad date format = " + text + "\n");
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
       errlog.format(" ** Parse error: Bad duration format = " + text + "\n");
       return null;
     }
   }

   protected ThreddsMetadataE.Documentation readDocumentation(Element s) {
     String href = s.getAttributeValue("href", xlinkNS);
     String title = s.getAttributeValue("title", xlinkNS);
     String type = s.getAttributeValue("type"); // not XLink type
     String content = s.getTextNormalize();

     if (content == null) {
       return new ThreddsMetadataE.Documentation(type, title, href);
     } else {
       return new ThreddsMetadataE.Documentation(type, content);
     }
   }

   protected double readDouble(Element elem) {
     if (elem == null) return Double.NaN;
     String text = elem.getText();
     try {
       return Double.parseDouble(text);
     } catch (NumberFormatException e) {
       errlog.format(" ** Parse error: Bad double format = " + text + "\n");
       return Double.NaN;
     }
   }

   protected ThreddsMetadataE.GeospatialCoverage readGeospatialCoverage(Element gcElem) {
     if (gcElem == null) return null;

     String zpositive = gcElem.getAttributeValue("zpositive");

     ThreddsMetadataE.GeospatialRange northsouth = readGeospatialRange(gcElem.getChild("northsouth", defNS), CDM.LAT_UNITS).setHorizontal(true);
     ThreddsMetadataE.GeospatialRange eastwest = readGeospatialRange(gcElem.getChild("eastwest", defNS), CDM.LON_UNITS).setHorizontal(true);
     ThreddsMetadataE.GeospatialRange updown = readGeospatialRange(gcElem.getChild("updown", defNS), "m").setHorizontal(false);

     // look for names
     List<ThreddsMetadataE.Vocab> names = new ArrayList<>();
     java.util.List<Element> list = gcElem.getChildren("name", defNS);
     for (Element e : list) {
       ThreddsMetadataE.Vocab name = readControlledVocabulary(e);
       names.add(name);
     }

     ThreddsMetadataE.GeospatialCoverage result = new ThreddsMetadataE.GeospatialCoverage(eastwest, northsouth, updown, names, zpositive);
     result.addRange(northsouth);
     result.addRange(eastwest);
     result.addRange(updown);

     return result;
   }

   protected ThreddsMetadataE.GeospatialRange readGeospatialRange(Element spElem, String defUnits) {
     if (spElem == null) return null;

     double start = readDouble(spElem.getChild("start", defNS));
     double size = readDouble(spElem.getChild("size", defNS));
     double resolution = readDouble(spElem.getChild("resolution", defNS));

     String units = spElem.getChildText("units", defNS);
     if (units == null) units = defUnits;

     ThreddsMetadataE.GeospatialRange result = new ThreddsMetadataE.GeospatialRange();
     result.setStart(start);
     result.setSize(size);
     result.setResolution(resolution);
     result.setUnits(units);

     return result;
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
       ThreddsMetadataE tmg = new ThreddsMetadataE(false);
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
       errlog.format(" ** failed to read thredds metadata at = "+url+" for dataset"+dataset.getName()+"\n");
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


   protected ThreddsMetadataE.Source readSource(Element elem) {
     if (elem == null) return null;
     ThreddsMetadataE.Vocab name = readControlledVocabulary(elem.getChild("name", defNS));
     Element contact = elem.getChild("contact", defNS);
     if (contact == null) {
       errlog.format(" ** Parse error: Missing contact element in = " + elem.getName() + "\n");
       return null;
     }
     return new ThreddsMetadataE.Source(name, contact.getAttributeValue("url"), contact.getAttributeValue("email"));
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
       errlog.format(" ** warning: TimeCoverage error = " + e.getMessage() + "\n");
       return null;
     }
   }

   protected void readThreddsMetadata(InvCatalog catalog, InvDatasetImpl dataset, Element parent, ThreddsMetadataE tmg) {
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
       ThreddsMetadataE.Variables vars = readVariables(catalog, dataset, e);
       tmg.addVariables(vars);
     }

     // can only be one each of these kinds
     ThreddsMetadataE.GeospatialCoverage gc = readGeospatialCoverage(parent.getChild("geospatialCoverage", defNS));
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
           errlog.format(" ** warning: non-standard data type = " + dataTypeName + "\n");
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
           errlog.format(" ** warning: non-standard dataFormat type = " + dataFormatTypeName + "\n");
         }
         tmg.setDataFormatType(dataFormatType);
       }
     }

     double size = readDataSize(parent);
     if (!Double.isNaN(size))
       tmg.setDataSize(size);
   }

   protected ThreddsMetadataE.VariableGroup readVariableGroup(Element varsElem) {
     if (varsElem == null) return null;

     String vocab = varsElem.getAttributeValue("vocabulary");
     String vocabHref = varsElem.getAttributeValue("href", xlinkNS);

     URI vocabUri = null;
     if (vocabHref != null) {
       try {
         vocabUri = cat.resolveUri(vocabHref);
       } catch (Exception e) {
         errlog.format(" ** Invalid Variables vocabulary URI = " + vocabHref + " " + e.getMessage() + "\n");
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
         errlog.format(" ** Invalid Variables map URI = " + mapHref + " " + e.getMessage() + "\n");
       }
     }

     if ((mapUri != null) && vlist.size() > 0) { // cant do both
       errlog.format(" ** Catalog error: cant have variableMap and variable in same element (dataset = " +
               ds.getName() + "\n");
       mapUri = null;
     }

     ThreddsMetadataE.VariableGroup variableGroup = new ThreddsMetadataE.VariableGroup(vocab, vocabHref, vocabUri, mapHref, mapUri);
     variableGroup.setVariableMapUrl(vocabUri);
     variableGroup.setVocabularyAuthorityId(vocabUri);

     for (Element e : vlist) {
       ThreddsMetadataE.ThreddsVariable v = readVariable(e, variableGroup);
       variableGroup.addVariable(v);
     }

     // read in variable map LOOK: would like to defer
     if (mapUri != null) {
       Element varsElement;
       try {
         varsElement = readContentFromURL(mapUri);
         List<Element> list = varsElement.getChildren("variable", defNS);
         for (Element e : list) {
           ThreddsMetadataE.ThreddsVariable v = readVariable(e, variableGroup);
           variableGroup.addVariable(v);
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

     return variableGroup;
   }

  protected ThreddsMetadataE.ThreddsVariable readVariable(Element varElem, ThreddsMetadataE.VariableGroup parent) {
     if (varElem == null) return null;

     String name = varElem.getAttributeValue("name");
     String desc = varElem.getText();
     String vocabulary_name = varElem.getAttributeValue("vocabulary_name");
     String units = varElem.getAttributeValue("units");
     String id = varElem.getAttributeValue("vocabulary_id");
     return new ThreddsMetadataE.ThreddsVariable(name, desc, units, id, vocabulary_name, parent);
   }

}        */
