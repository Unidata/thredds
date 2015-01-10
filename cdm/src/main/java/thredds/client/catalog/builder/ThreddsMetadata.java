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

import thredds.client.catalog.*;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/9/2015
 */
public class ThreddsMetadata {
  protected boolean inherited;

  protected List<Source> creators = new ArrayList<>();
  protected List<Contributor> contributors = new ArrayList<>();
  protected List<DateType> dates = new ArrayList<>();
  protected List<Documentation> docs = new ArrayList<>();
  protected List<Vocab> keywords = new ArrayList<>(); //
  protected List<MetadataOther> metadata = new ArrayList<>();
  protected List<Vocab> projects = new ArrayList<>();
  protected List<Property> properties = new ArrayList<>(); //
  protected List<Source> publishers = new ArrayList<>(); //
  protected List<Variables> variables = new ArrayList<>();  //
  protected String variableMapLink;

  // singles
  protected GeospatialCoverage gc;
  protected DateRange timeCoverage;
  //protected CalendarDateRange cdateCoverage;
  protected String authorityName, serviceName;
  protected FeatureType dataType;
  protected DataFormatType dataFormat;
  protected double dataSize = 0.0;

  public ThreddsMetadata(boolean inherited) {
    this.inherited = inherited;
  }

  /**
   * Copy Constructor.
   *
   * @param from copy from here
   */
  public ThreddsMetadata(ThreddsMetadata from) {
    this.inherited = from.inherited;
    add(from, true);
  }

  /**
   * Add all the content from another ThreddsMetadata
   *
   * @param tmd              get content from here
   * @param includeInherited if false, dont add inherited Metadata
   */
  public void add(ThreddsMetadata tmd, boolean includeInherited) {
    creators.addAll(tmd.getCreators());
    contributors.addAll(tmd.getContributors());
    dates.addAll(tmd.getDates());
    docs.addAll(tmd.getDocumentation());
    keywords.addAll(tmd.getKeywords());
    projects.addAll(tmd.getProjects());
    properties.addAll(tmd.getProperties());
    publishers.addAll(tmd.getPublishers());
    variables.addAll(tmd.getVariables());

    if (includeInherited)
      metadata.addAll(tmd.getMetadata());
    else {
      for (MetadataOther mdata : tmd.getMetadata() ) {
        if (!mdata.isInherited())
          metadata.add(mdata);
      }
    }

    // LOOK! should be copies ??!!
    if (gc == null) gc = tmd.getGeospatialCoverage();
    if (timeCoverage == null) timeCoverage = tmd.getTimeCoverage();
    if (serviceName == null) serviceName = tmd.getServiceName();
    if (dataType == null) dataType = tmd.getDataType();
    if (dataSize == 0.0) dataSize = tmd.getDataSize();
    if (dataFormat == null) dataFormat = tmd.getDataFormatType();
    if (authorityName == null) authorityName = tmd.getAuthority();
    if (variableMapLink == null) variableMapLink = tmd.getVariableMap();
  }

  /**
   * Add a creator
   * @param c add this
   */
  public void addCreator(Source c) {
    if (c != null) creators.add(c);
  }

  /**
   * @return list of creators (type Source); may be empty, not null.
   */
  public List<ThreddsMetadata.Source> getCreators() {
    return creators;
  }

  /**
   * Set list of creators (type Source); may be empty, not null.
   * @param creators set creators to this list
   */
  public void setCreators(List<Source> creators) {
    this.creators = creators;
  }

  /**
   * Add a contributor
   * @param c add this
   */
  public void addContributor(Contributor c) {
    if (c != null) contributors.add(c);
  }

  /**
   * @return list of contributors (type Contributor); may be empty, not null.
   */
  public List<Contributor> getContributors() {
    return contributors;
  }

  /**
   * Set list of contributors (type Contributor); may be empty, not null.
   * @param contributors set contributers to this
   */
  public void setContributors(List<Contributor> contributors) {
    this.contributors = contributors;
  }

  /**
   * Add a date
   * @param d add this dateType
   */
  public void addDate(DateType d) {
    if (d != null) dates.add(d);
  }

  /**
   * @return list of DateType; may be empty, not null.
   */
  public List<DateType> getDates() {
    return dates;
  }

  /**
   * Add a documentation
   * @param d add this
   */
  public void addDocumentation(Documentation d) {
    if (d != null) docs.add(d);
  }

  /**
   * @return list of Documentation; may be empty, not null.
   */
  public List<Documentation> getDocumentation() {
    return docs;
  }

  /**
   * Add a keyword
   * @param keyword add this
   */
  public void addKeyword(Vocab keyword) {
    if (keyword != null) keywords.add(keyword);
  }

  /**
   * @return list of keywords; may be empty, not null.
   */
  public List<Vocab> getKeywords() {
    return keywords;
  }

  /**
   * Set list of keywords; may be empty, not null.
   * @param keywords set list of keywords to this
   */
  public void setKeywords(List<Vocab> keywords) {
    this.keywords = keywords;
  }

  /**
   * Add Metadata
   * @param m add this
   */
  public void addMetadata(MetadataOther m) {
    if (m != null) metadata.add(m);
  }

  /**
   * remove an Metadata element from list, using equals() to locate it.
   * @param m remove this
   */
  public void removeMetadata(Metadata m) {
    metadata.remove(m);
  }

  /**
   * @return list of Metadata; may be empty, not null.
   */
  public List<MetadataOther> getMetadata() {
    return metadata;
  }

  /**
   * Add a project
   * @param project add this
   */
  public void addProject(Vocab project) {
    if (project != null) projects.add(project);
  }

  /**
   * @return list of projects (type Vocab); may be empty, not null.
   */
  public List<Vocab> getProjects() {
    return projects;
  }

  /**
   * Set list of projects (type Vocab); may be empty, not null.
   * @param projects set list of projects to this
   */
  public void setProjects(List<Vocab> projects) {
    this.projects = projects;
  }

  /**
   * Add a property
   * @param p add this
   */
  public void addProperty(Property p) {
    if (p != null) properties.add(p);
  }

  public void addProperties(List<Attribute> atts) {
    for (Attribute att : atts)
      if (att.isString()) properties.add(new Property(att.getShortName(), att.getStringValue()));
  }

  /**
   * @return list of properties; may be empty, not null.
   */
  public List<Property> getProperties() {
    return properties;
  }

  /**
   * Add a publisher
   * @param p add this
   */
  public void addPublisher(Source p) {
    if (p != null) publishers.add(p);
  }

  /**
   * @return list of publishers (type Source); may be empty, not null.
   */
  public List<Source> getPublishers() {
    return publishers;
  }

  /**
   * Set list of publishers (type Source); may be empty, not null.
   * @param publishers set list of publishers to this
   */
  public void setPublishers(List<Source> publishers) {
    this.publishers = publishers;
  }

  /**
   * Add variables
   * @param vs add this
   */
  public void addVariables(Variables vs) {
    if (vs != null) variables.add(vs);
  }

  /**
   * @return list of Variables; may be empty, not null.
   */
  public List<Variables> getVariables() {
    return variables;
  }

  /**
   * Add a variable Map link
   * @param xlinkHref         : URI of xlink
   * */
  public void addVariableMapLink(String xlinkHref) {
    variableMapLink = xlinkHref;
  }

  public String getVariableMap() {
    return variableMapLink;
  }

  /**
   * set GeospatialCoverage element
   * @param gc set GeospatialCoverage to this
   */
  public void setGeospatialCoverage(GeospatialCoverage gc) {
    this.gc = gc;
  }

  /**
   * @return GeospatialCoverage element
   */
  public GeospatialCoverage getGeospatialCoverage() {
    return gc;
  }

  /**
   * set TimeCoverage element
   * @param tc set TimeCoverage to this
   */
  public void setTimeCoverage(DateRange tc) {
    this.timeCoverage = tc;
  }

  /**
   * set TimeCoverage from a CalendarDateRange
   * @param cdc set CalendarDateRange to this
   */
  public void setTimeCoverage(CalendarDateRange cdc) {
    if (cdc == null) return;
    this.timeCoverage = cdc.toDateRange();
  }

  /**
   * @return TimeCoverage element  as DateRange
   */
  public DateRange getTimeCoverage() {
    return timeCoverage;
  }

  /**
   * @return TimeCoverage element as CalendarDateRange
   */
  public CalendarDateRange getCalendarDateCoverage() {
    return timeCoverage == null ? null : CalendarDateRange.of(timeCoverage);
  }

  /**
   * @return true if this is inherited
   */
  public boolean isInherited() {
    return inherited;
  }

  /**
   * Set inherited
   * @param inherited set true if inherited
   */
  public void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  /**
   * @return serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set serviceName
   * @param serviceName set service name to this, must be valid service
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return dataType
   */
  public FeatureType getDataType() {
    return dataType;
  }

  /**
   * Set dataType
   * @param dataType set to this dataType
   */
  public void setDataType(FeatureType dataType) {
    this.dataType = dataType;
  }

  /**
   * @return dataType
   */
  public DataFormatType getDataFormatType() {
    return dataFormat;
  }

  /**
   * Set dataTypeFormat
   * @param dataFormat set to this dataTypeFormat
   */
  public void setDataFormatType(DataFormatType dataFormat) {
    this.dataFormat = dataFormat;
  }

  /**
   * @return authority
   */
  public String getAuthority() {
    return authorityName;
  }

  /**
   * Set authority
   * @param authorityName set to this
   */
  public void setAuthority(String authorityName) {
    this.authorityName = authorityName;
  }

  /**
   * @return specific type of documentation
   * @param type match on doc.getType()
   */
  public String getDocumentation(String type) {
    for (Documentation doc : getDocumentation()) {
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type)) return doc.getInlineContent();
    }
    return null;
  }

  /* @return Documentation that are xlinks
  public List<Documentation> getDocumentationLinks() {
    List<Documentation> result = new ArrayList<Documentation>();
    for (Documentation doc : getDocumentation()) {
      if (doc.hasXlink())
        result.add(doc);
    }
    return result;
  }

  /* set the list of Documentation that are xlinks
  public void setDocumentationLinks(List newdocs) {
    ArrayList result = new ArrayList(newdocs);
    java.util.List olddocs = getDocumentation();
    for (int i=0; i<olddocs.size(); i++) {
      Documentation doc = (Documentation) olddocs.get(i);
      if (!doc.hasXlink()) // keep the non-links
        result.add(doc);
    }
    docs = result;
  } */

  /**
   * @return specific type of documentation = history
   */
  public String getHistory() {
    return getDocumentation("history");
  }

  /**
   * set specific type of documentation = history
   * @param history add (or replace) the history documentation element to this content
   */
  public void setHistory(String history) {
    addDocumentation("history", history);
  }

  /**
   * @return specific type of documentation = processing_level
   */
  public String getProcessing() {
    return getDocumentation("processing_level");
  }

  /**
   * set specific type of documentation = processing_level
   * @param processing add (or replace) the history documentation element to this content
   */
  public void setProcessing(String processing) {
    addDocumentation("processing_level", processing);
  }

  /**
   * @return specific type of documentation = rights
   */
  public String getRights() {
    return getDocumentation("rights");
  }

  /**
   * set specific type of documentation = rights
   * @param rights add (or replace) the history documentation element to this content
   */
  public void setRights(String rights) {
    addDocumentation("rights", rights);
  }

  /**
   * @return specific type of documentation = summary
   */
  public String getSummary() {
    return getDocumentation("summary");
  }

  /**
   * set specific type of documentation = summary
   * @param summary add (or replace) the history documentation element to this content
   */
  public void setSummary(String summary) {
    addDocumentation("summary", summary);
  }

  /**
   * @return the data size in bytes. A value of 0.0 or NaN means not set.
   */
  public double getDataSize() {
    return dataSize;
  }

  /**
   * Set size (bytes)
   * @param size the data size in bytes. A value of 0.0 or NaN means not set.
   */
  public void setDataSize(double size) {
    this.dataSize = size;
  }

  public boolean hasDataSize() {
    return dataSize != 0.0 && !Double.isNaN(dataSize);
  }

  /**
   * set specified type of documentation
   *
   * @param type    type of documentation
   * @param content documentation contenet
   */
  public void addDocumentation(String type, String content) {
    if (content == null) {
      removeDocumentation(type);
      return;
    }

    content = content.trim();
    for (Documentation doc : getDocumentation()) {
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type)) {
        doc.setInlineContent(content);
        return;
      }
    }
    if (content.length() > 0)
      addDocumentation(new Documentation(null, null, null, type, content));
  }

  /**
   * remove all instances of specified type of documentation
   * @param type type of documentation
   */
  public void removeDocumentation(String type) {
    Iterator iter = docs.iterator();
    while (iter.hasNext()) {
      Documentation doc = (Documentation) iter.next();
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type))
        iter.remove();
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Implements Contributor type.
   */
  static public class Contributor {
    private String name;
    private String role;

    public Contributor() {
    }

    public Contributor(String name, String role) {
      this.name = name;
      this.role = role;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
      hashCode = 0;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
      hashCode = 0;
    }

    // for bean editing
    static public String editableProperties() {
      return "role name";
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Contributor)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        // result = 37*result + dataset.getName().hashCode();
        result = 37 * result + getName().hashCode();
        if (null != getRole())
          result = 37 * result + getRole().hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Implements Source type, used by publisher and creator elements.
   */
  static public class Source {
    private Vocab name, long_name;
    private String url, email;

    // need no-arg constructor for bean handling
    public Source() {
      this.name = new Vocab();
      this.long_name = new Vocab();
    }

    public Source(Vocab name, String url, String email) {
      this.name = name;
      this.url = url;
      this.email = email;
    }

    public Vocab getNameVocab() {
      return this.name;
    }

    /**
     * @return name
     */
    public String getName() {
      return this.name.getText();
    }

    /**
     * Set name
     * @param name to set
     */
    public void setName(String name) {
      this.name.setText(name);
      hashCode = 0;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
      hashCode = 0;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
      hashCode = 0;
    }

    public String getVocabulary() {
      return this.name.getVocabulary();
    }

    public void setVocabulary(String vocabulary) {
      this.name.setVocabulary(vocabulary);
      this.long_name.setVocabulary(vocabulary);
      hashCode = 0;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Source)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        // result = 37*result + dataset.getName().hashCode();
        result = 37 * result + getName().hashCode();
        if (null != getVocabulary())
          result = 37 * result + getVocabulary().hashCode();
        if (null != getUrl())
          result = 37 * result + getUrl().hashCode();
        if (null != getEmail())
          result = 37 * result + getEmail().hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8

    // for bean editing
    static public String hiddenProperties() {
      return "nameVocab";
    }

    static public String editableProperties() {
      return "name email url vocabulary";
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Implements Vocab type, just text with an optional "vocabulary" attribute.
   */
  static public class Vocab {
    private String text;
    private String vocabulary;

    // need no-arg constructor for bean handling
    public Vocab() {
    }

    public Vocab(String text, String vocabulary) {
      this.text = text;
      this.vocabulary = vocabulary;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
      hashCode = 0;
    }

    public String getVocabulary() {
      return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
      this.vocabulary = vocabulary;
      hashCode = 0;
    }

    // for bean editing
    static public String editableProperties() {
      return "text vocabulary";
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Vocab)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        // result = 37*result + dataset.getName().hashCode();
        if (null != getText())
          result = 37 * result + getText().hashCode();
        if (null != getVocabulary())
          result = 37 * result + getVocabulary().hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8

  }

  /* static private java.text.SimpleDateFormat dateFormat;
  static {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  } */

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Implements GeospatialCoverage type.
   */
  static public class GeospatialCoverage {
    static private Range defaultEastwest = new Range(0.0, 0.0, Double.NaN, CDM.LON_UNITS);
    static private Range defaultNorthsouth = new Range(0.0, 0.0, Double.NaN, CDM.LAT_UNITS);
    static private Range defaultUpdown = new Range(0.0, 0.0, Double.NaN, "km");
    static private GeospatialCoverage empty = new GeospatialCoverage();

    private Range eastwest, northsouth, updown;
    private boolean isGlobal = false;
    private String zpositive = "up";
    private List<Vocab> names = new ArrayList<Vocab>(); // Vocab

    // need no-arg constructor for bean handling
    public GeospatialCoverage() {
    }

    public GeospatialCoverage(Range eastwest, Range northsouth, Range updown, List<Vocab> names, String zpositive) {
      this.eastwest = eastwest; //  : new Range(defaultEastwest);
      this.northsouth = northsouth; // : new Range(defaultNorthsouth);
      this.updown = updown; //  : new Range(defaultUpdown);
      if (names != null) this.names = new ArrayList<Vocab>(names);
      if (zpositive != null) this.zpositive = zpositive;

      if (names != null) {
        for (Vocab name : names) {
          String elem = name.getText();
          if (elem.equalsIgnoreCase("global")) isGlobal = true;
        }
      }
    }

    public void extend(GeospatialCoverage gc) {
      this.eastwest.extend(gc.getEastWestRange());
      this.northsouth.extend(gc.getNorthSouthRange());
    }

    public boolean isEmpty() {
      return this.equals(empty);
    }

    public Range getEastWestRange() {
      return eastwest;
    }

    public Range getNorthSouthRange() {
      return northsouth;
    }

    public Range getUpDownRange() {
      return updown;
    }

    public List<ThreddsMetadata.Vocab> getNames() {
      return names;
    }

    /**
     * @return "up" or "down"
     */
    public String getZPositive() {
      return zpositive;
    }

    public void setZPositive(String positive) {
      this.zpositive = positive;
      hashCode = 0;
    }

    /**
     * @return true or false
     */
    public boolean getZPositiveUp() {
      return zpositive.equalsIgnoreCase("up");
    }

    public void setZPositiveUp(boolean positive) {
      this.zpositive = positive ? "up" : "down";
      hashCode = 0;
    }

    public boolean isValid() {
      return isGlobal || ((eastwest != null) && (northsouth != null));
    }

    /**
     * @return true if coverage is Global
     */
    public boolean isGlobal() {
      return isGlobal;
    }

    /**
     * Set isGlobal
     * @param isGlobal true if global coverage
     */
    public void setGlobal(boolean isGlobal) {
      this.isGlobal = isGlobal;
      hashCode = 0;
    }

    /**
     * @return starting latitude, or Double.NaN if not valie
     */
    public double getLatStart() {
      return (northsouth == null) ? Double.NaN : northsouth.start;
    }

    /**
     * Set starting latitude
     * @param start starting latitude, decimal degrees north
     */
    public void setLatStart(double start) {
      if (northsouth == null)
        northsouth = new Range(defaultNorthsouth);
      northsouth.start = start;
      hashCode = 0;
    }

    /**
     * @return latitude extent - may be positive or negetive
     */
    public double getLatExtent() {
      return (northsouth == null) ? Double.NaN : northsouth.size;
    }

    /**
     * Set latitude extent
     * @param size latitude extent, decimal degrees north
     */
    public void setLatExtent(double size) {
      if (northsouth == null)
        northsouth = new Range(defaultNorthsouth);
      northsouth.size = size;
      hashCode = 0;
    }

    /**
     * @return latitude resolution: 0.0 or NaN means not set.
     */
    public double getLatResolution() {
      return (northsouth == null) ? Double.NaN : northsouth.resolution;
    }

    /**
     * Set latitude resolution
     * @param resolution latitude resolution, decimal degrees north
     */
    public void setLatResolution(double resolution) {
      if (northsouth == null)
        northsouth = new Range(defaultNorthsouth);
      northsouth.resolution = resolution;
      hashCode = 0;
    }

    /**
     * @return latitude units
     */
    public String getLatUnits() {
      return (northsouth == null) ? null : northsouth.units;
    }

    /**
     * Set latitude units
     * @param units set units
     */
    public void setLatUnits(String units) {
      if (northsouth == null)
        northsouth = new Range(defaultNorthsouth);
      northsouth.units = units;
      hashCode = 0;
    }

    // LOOK not dealing with units degrees_south
    public double getLatNorth() {
      return Math.max(northsouth.start, northsouth.start + northsouth.size);
    }

    public double getLatSouth() {
      return Math.min(northsouth.start, northsouth.start + northsouth.size);
    }

    /**
     * @return starting longitude
     */
    public double getLonStart() {
      return (eastwest == null) ? Double.NaN : eastwest.start;
    }

    /**
     * Set starting longitude
     * @param start starting longitude
     */
    public void setLonStart(double start) {
      if (eastwest == null)
        eastwest = new Range(defaultEastwest);
      eastwest.start = start;
      hashCode = 0;
    }

    /**
     * @return longitude extent - may be positive or negetive
     */
    public double getLonExtent() {
      return (eastwest == null) ? Double.NaN : eastwest.size;
    }

    /**
     * Set longitude extent
     * @param size longitude extent
     */
    public void setLonExtent(double size) {
      if (eastwest == null)
        eastwest = new Range(defaultEastwest);
      eastwest.size = size;
      hashCode = 0;
    }

    /**
     * @return longitude resolution: 0.0 or NaN means not set.
     */
    public double getLonResolution() {
      return (eastwest == null) ? Double.NaN : eastwest.resolution;
    }

    /**
     * Set longitude resolution
     * @param resolution longitude resolution
     */
    public void setLonResolution(double resolution) {
      if (eastwest == null)
        eastwest = new Range(defaultEastwest);
      eastwest.resolution = resolution;
      hashCode = 0;
    }

    /**
     * @return longitude units
     */
    public String getLonUnits() {
      return (eastwest == null) ? null : eastwest.units;
    }

    /**
     * Set longitude units
     * @param units longitude units
     */
    public void setLonUnits(String units) {
      if (eastwest == null)
        eastwest = new Range(defaultEastwest);
      eastwest.units = units;
      hashCode = 0;
    }

    // LOOK not dealing with units degrees_west
    public double getLonEast() {
      if (eastwest == null) return Double.NaN;
      return Math.max(eastwest.start, eastwest.start + eastwest.size);
    }

    public double getLonWest() {
      if (eastwest == null) return Double.NaN;
      return Math.min(eastwest.start, eastwest.start + eastwest.size);
    }

    /**
     * @return starting height
     */
    public double getHeightStart() {
      return updown == null ? 0.0 : updown.start;
    }

    /**
     * Set starting height
     * @param start starting height
     */
    public void setHeightStart(double start) {
      if (updown == null)
        updown = new Range(defaultUpdown);
      updown.start = start;
      hashCode = 0;
    }

    /**
     * @return height extent - may be positive or negetive
     */
    public double getHeightExtent() {
      return updown == null ? 0.0 : updown.size;
    }

    /**
     * Set height extent
     * @param size height extent
     */
    public void setHeightExtent(double size) {
      if (updown == null)
        updown = new Range(defaultUpdown);
      updown.size = size;
      hashCode = 0;
    }

    /**
     * @return height resolution: 0.0 or NaN means not set.
     */
    public double getHeightResolution() {
      return updown == null ? 0.0 : updown.resolution;
    }

    /**
     * Set height resolution
     * @param resolution height resolution
     */
    public void setHeightResolution(double resolution) {
      if (updown == null)
        updown = new Range(defaultUpdown);
      updown.resolution = resolution;
      hashCode = 0;
    }

    /**
     * @return height units
     */
    public String getHeightUnits() {
      return updown == null ? null : updown.units;
    }

    /**
     * Set height units
     * @param units height units
     */
    public void setHeightUnits(String units) {
      if (updown == null)
        updown = new Range(defaultUpdown);
      updown.units = units;
      hashCode = 0;
    }

    public LatLonRect getBoundingBox() {
      return isGlobal ? new LatLonRect() :
          new LatLonRect(new LatLonPointImpl(getLatStart(), getLonStart()), getLatExtent(), getLonExtent());
    }

    public void setBoundingBox(LatLonRect bb) {
      LatLonPointImpl llpt = bb.getLowerLeftPoint();
      LatLonPointImpl urpt = bb.getUpperRightPoint();
      double height = urpt.getLatitude() - llpt.getLatitude();

      this.eastwest = new Range(llpt.getLongitude(), bb.getWidth(), 0.0, CDM.LON_UNITS);
      this.northsouth = new Range(llpt.getLatitude(), height, 0.0, CDM.LAT_UNITS);

      if ((bb.getWidth() > 358) && (height > 178))
        setGlobal(true); // LOOK ??
    }

    public void setVertical(CoordinateAxis1D vaxis) {  // LOOK WTF
      int n = (int) vaxis.getSize();
      double size = vaxis.getCoordValue(n - 1) - vaxis.getCoordValue(0);
      double resolution = vaxis.getIncrement();
      String units = vaxis.getUnitsString();
      this.updown = new Range(vaxis.getCoordValue(0), size, resolution, units);
      if (units != null) {
        setZPositiveUp(SimpleUnit.isCompatible("m", units));
      }
    }

    public void setVertical(double start, double end, double resolution, String units, boolean isPositiveUp) {
      double size = end - start;
      this.updown = new Range(start, size, resolution, units);
      setZPositiveUp(isPositiveUp);
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof GeospatialCoverage)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        if (null != getEastWestRange())
          result = 37 * result + getEastWestRange().hashCode();
        if (null != getNorthSouthRange())
          result = 37 * result + getNorthSouthRange().hashCode();
        if (null != getUpDownRange())
          result = 37 * result + getUpDownRange().hashCode();
        if (null != getNames())
          result = 37 * result + getNames().hashCode();
        if (null != getZPositive())
          result = 2 * result + getZPositive().hashCode();
        result = 2 * result + (isGlobal() ? 1 : 0);
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8
  }

  /**
   * Implements spatialRange type.
   */
  static public class Range {
    private double start, size, resolution;
    private String units;

    /**
     * Constructor
     *
     * @param start      starting value
     * @param size       ending = start + size
     * @param resolution data resolution, or NaN if unknown
     * @param units      what units are start, size in?
     */
    public Range(double start, double size, double resolution, String units) {
      this.start = start;
      this.size = size;
      this.resolution = resolution;
      this.units = units;
    }

    /**
     * Copy constructor
     * @param from copy this
     */
    public Range(Range from) {
      this.start = from.start;
      this.size = from.size;
      this.resolution = from.resolution;
      this.units = from.units;
    }

    public double getStart() {
      return start;
    }

    public double getSize() {
      return size;
    }

    public double getResolution() {
      return resolution;
    }

    public String getUnits() {
      return units;
    }

    public boolean hasResolution() {
      return (resolution != 0.0) && !Double.isNaN(resolution);
    }

    public void extend(Range r) {
      double stop = Math.max(this.start+this.size, r.start+r.size);
      this.start = Math.min(this.start, r.start);
      this.size = this.start - stop;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Range)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        if (null != getUnits())
          result = 37 * result + getUnits().hashCode();
        result = 37 * result + (int) (getStart() * 1000.0);
        result = 37 * result + (int) (getSize() * 1000.0);
        if (hasResolution())
          result = 37 * result + (int) (getResolution() * 1000.0);
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8

  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Implements TimeCoverage type.
   *
   static public class TimeCoverage {
   static private TimeCoverage empty = new TimeCoverage();

   private DateType start, end;
   private TimeDuration duration, resolution;
   private double durationSecs;
   boolean hasStart, hasEnd, hasDuration, invalid;
   private DateType _start, _end;

   // need no-arg constructor for bean handling
   public TimeCoverage() {
   }

   public TimeCoverage(DateType start, DateType end, TimeDuration duration, TimeDuration resolution) {
   this.start = start;
   this.end = end;
   this.duration = duration;
   this.resolution = resolution;
   recalc();
   }

   private void recalc() {
   hasStart = (start != null) && !start.isBlank();
   hasEnd = (end != null) && !end.isBlank();
   hasDuration = (duration != null) && !duration.isBlank();

   invalid = true;
   if (hasStart && hasEnd) {
   invalid = false;
   } else if (hasStart && hasDuration) {
   invalid = false;
   _end = start.add( duration);
   } else if (hasEnd && hasDuration) {
   invalid = false;
   _start = end.subtract( duration);
   }
   hashCode = 0;
   }

   public boolean isEmpty() { return this.equals( empty); }

   public boolean included( Date d) {
   if (invalid) return false;

   if (hasStart && hasEnd) {
   if (start.after( d)) return false;
   if (end.before( d)) return false;

   } else if (hasStart && hasDuration) {
   if (start.after( d)) return false;
   if (_end.before( d)) return false;

   } else if (hasEnd && hasDuration) {
   if (_start.after( d)) return false;
   if (end.before( d)) return false;

   }

   return true;
   }

   public DateType getStart() { return start; }
   public void setStart(DateType start) {
   this.start = start;
   recalc();
   }

   public DateType getEnd() { return end; }
   public void setEnd(DateType end) {
   this.end = end;
   recalc();
   }

   public TimeDuration getDuration() { return duration; }
   public void setDuration(TimeDuration duration) {
   this.duration = duration;
   recalc();
   }

   public TimeDuration getResolution() { return resolution; }
   public void setResolution(TimeDuration resolution) {
   this.resolution = resolution;
   recalc();
   }

   public String toString() { return "start= "+start +" end= "+end+ " duration= "+ duration
   + " resolution= "+ resolution; }
   public boolean equals(Object o) {
   if (this == o) return true;
   if (!(o instanceof TimeCoverage)) return false;
   return o.hashCode() == this.hashCode();
   }

   public int hashCode() {
   if (hashCode == 0) {
   int result = 17;
   if (hasStart && !start.isBlank())
   result = 37*result + start.hashCode();
   if (hasEnd && !end.isBlank())
   result = 37*result + end.hashCode();
   if (hasDuration && !duration.isBlank())
   result = 37*result + duration.hashCode();
   if (resolution != null && !resolution.isBlank())
   result = 37*result + resolution.hashCode();
   hashCode = result;
   }
   return hashCode;
   }
   private volatile int hashCode = 0; // Bloch, item 8

   } */

  //////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Implements Variable type.
   */
  static public class Variable implements Comparable {
    private String name, desc, vocabulary_name, units, id;

    // no-arg constructor for beans
    public Variable() {
      this.name = "";
      this.desc = "";
      this.vocabulary_name = "";
      this.units = "";
      this.id = "";
    }

    public Variable(String name, String desc, String vocabulary_name, String units, String id) {
      this.name = name;
      this.desc = desc;
      this.vocabulary_name = vocabulary_name;
      this.units = units;
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getVocabularyName() {
      return vocabulary_name;
    }

    public void setVocabularyName(String vocabulary_name) {
      this.vocabulary_name = vocabulary_name;
    }

    // need unique id, cant count on name, eg because of variant GRIB tables
    public String getVocabularyId() {
      return id;
    }

    public void setVocabularyId(String id) {
      this.id = id;
    }

    public void setVocabularyId(Attribute id) {
      if (id == null) return;
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < id.getLength(); i++) {
        if (i > 0) sbuff.append(",");
        sbuff.append(id.getNumericValue(i));
      }
      this.id = sbuff.toString();
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Variable)) return false;
      return o.hashCode() == this.hashCode();
    }

    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        if (null != getName())
          result = 37 * result + getName().hashCode();
        if (null != getDescription())
          result = 37 * result + getDescription().hashCode();
        if (null != getVocabularyName())
          result = 37 * result + getVocabularyName().hashCode();
        if (null != getUnits())
          result = 37 * result + getUnits().hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8

    public String toString()
    {
      return "Variable [" + name + "]";
    }

    public int compareTo(Object o) {
      Variable ov = (Variable) o;
      return name.compareTo(ov.name);
    }

    // for bean editing
    static public String hiddenProperties() {
      return "nameVocab";
    }

    static public String editableProperties() {
      return "name description units vocabularyName";
    }
  }

  /**
   * Implements Variables type.
   */
  static public class Variables {
    private String vocabulary, vocabHref, mapHref;
    private URI vocabUri, mapUri;
    private List<Variable> variables = new ArrayList<Variable>();

    public Variables(String vocab) {
      this.vocabulary = vocab;
    }

    public Variables(String vocab, String vocabHref, URI vocabUri, String mapHref, URI mapUri) {
      this.vocabulary = vocab;
      this.vocabHref = vocabHref;
      this.vocabUri = vocabUri;
      this.mapHref = mapHref;
      this.mapUri = mapUri;
    }

    public String getVocabulary() {
      return vocabulary;
    }

    public String getVocabHref() {
      return vocabHref;
    }

    public URI getVocabUri() {
      return vocabUri;
    }

    public URI getMapUri() {
      return mapUri;
    }

    public String getMapHref() {
      return mapHref;
    }

    public void addVariable(Variable v) {
      variables.add(v);
    }

    public List<Variable> getVariableList() {
      init();
      return variables;
    }

    public void sort() {
      Collections.sort(variables);
    }

    private boolean isInit = false;

    private void init() {
      if (isInit || (mapUri != null)) return;
      isInit = true;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Variables)) return false;
      return o.hashCode() == this.hashCode();
    }

    /**
     * Override Object.hashCode() to implement equals.
     */
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        if (null != getVocabulary())
          result = 37 * result + getVocabulary().hashCode();
        if (null != getVocabUri())
          result = 37 * result + getVocabUri().hashCode();
        if (null != getMapUri())
          result = 37 * result + getMapUri().hashCode();
        if (null != getVariableList())
          result = 37 * result + getVariableList().hashCode();
        hashCode = result;
      }
      return hashCode;
    }

    private volatile int hashCode = 0; // Bloch, item 8

    public String toString()
    {
      StringBuilder sb = new StringBuilder( "Variables" );
      sb.append( " [" )
        .append( getVocabulary() != null ? getVocabulary() : "" )
        .append( "]" );
      if ( mapUri != null )
        sb.append( "map [").append( mapUri.toString()).append("]");
      else
        for ( Variable v : variables )
          sb.append( " " ).append( v.toString() );
      return sb.toString();
    }
  }

  public static class MetadataOther {
      private String title, type;
      private String xlinkHref;
      private java.net.URI xlinkUri = null;
      private String namespaceURI, prefix;
      private boolean isInherited;
      private boolean isThreddsMetadata = true;
      private Object contentObject = null;

      public MetadataOther(String xlinkHref, String title, String type, String namespaceURI, String prefix, boolean inherited) {
        this.xlinkHref = xlinkHref;
        this.title = title;
        this.type = type;
        this.namespaceURI = namespaceURI;
        this.prefix = prefix;
        this.isInherited = inherited;
      }

      public MetadataOther(String mtype, String namespaceURI, String namespacePrefix, boolean inherited, Object contentObject) {
        this.type = mtype;
        this.namespaceURI = namespaceURI;
        this.prefix = namespacePrefix;
        this.isInherited = inherited;
        this.contentObject = contentObject;
      }

    public boolean isInherited() {
      return isInherited;
    }
  }

}
