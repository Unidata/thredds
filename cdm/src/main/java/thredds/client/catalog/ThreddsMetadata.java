/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.client.catalog;

import net.jcip.annotations.Immutable;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.net.URI;
import java.util.*;

/**
 * Thredds Metadata. Immutable
 *
 * @author John
 * @since 1/10/2015
 */
@Immutable
public class ThreddsMetadata implements ThreddsMetadataContainer {
  private final Map<String, Object> flds;

  public ThreddsMetadata() {
    this.flds = new HashMap<>();
  }

  // do not use after building
  public Map<String, Object> getFlds() {
    return flds;
  }

  @Override
  public Object getLocalField(String fldName) {
    return flds.get(fldName);
  }

  @Override
  public List getLocalFieldAsList(String fldName) {
    Object value = flds.get(fldName);
    if (value != null) {
      if (value instanceof List) return (List) value;
      List result = new ArrayList(1);
      result.add(value);
      return result;
    }
    return new ArrayList(0);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  @Immutable
  static public class Contributor {
    public final String name;
    public final String role;

    public Contributor(String name, String role) {
      this.name = name;
      this.role = role;
    }

    public String getName() {
      return name;
    }

    public String getRole() {
      return role;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Implements Source type, used by publisher and creator elements.
   */
  @Immutable
  static public class Source {
    public final Vocab name;
    public final String url, email;

    public Source(Vocab name, String url, String email) {
      this.name = name;
      this.url = url;
      this.email = email;
    }

    public Vocab getNameVocab() {
      return this.name;
    }

    public String getName() {
      return this.name.getText();
    }

    public String getUrl() {
      return url;
    }

    public String getEmail() {
      return email;
    }

    public String getVocabulary() {
      return this.name.getVocabulary();
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Implements Vocab type, just text with an optional "vocabulary" attribute.
   */
  @Immutable
  static public class Vocab {
    public final String text;
    public final String vocabulary;

    public Vocab(String text, String vocabulary) {
      this.text = text;
      this.vocabulary = vocabulary;
    }

    public String getText() {
      return text;
    }

    public String getVocabulary() {
      return vocabulary;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Implements GeospatialCoverage type.
   */
  @Immutable
  static public class GeospatialCoverage {
    /* static private GeospatialRange defaultEastwest = new GeospatialRange(0.0, 0.0, Double.NaN, CDM.LON_UNITS);
    static private GeospatialRange defaultNorthsouth = new GeospatialRange(0.0, 0.0, Double.NaN, CDM.LAT_UNITS);
    static private GeospatialRange defaultUpdown = new GeospatialRange(0.0, 0.0, Double.NaN, "km");
    static private GeospatialCoverage empty = new GeospatialCoverage();  */

    public final GeospatialRange eastwest, northsouth, updown;
    public final boolean isGlobal;
    public final String zpositive;
    public final List<Vocab> names;

    public GeospatialCoverage(GeospatialRange eastwest, GeospatialRange northsouth, GeospatialRange updown, List<Vocab> names, String zpositive) {
      this.eastwest = eastwest; //  : new Range(defaultEastwest);
      this.northsouth = northsouth; // : new Range(defaultNorthsouth);
      this.updown = updown; //  : new Range(defaultUpdown);
      this.zpositive = (zpositive != null) ? zpositive : "up";

      this.names = names;
      boolean isGlobalCheck = false;
      if (names != null) {
        for (Vocab name : names) {
          String elem = name.getText();
          if (elem.equalsIgnoreCase("global")) isGlobalCheck = true;
        }
      }
      this.isGlobal = isGlobalCheck;
    }

    /* public boolean isEmpty() {
      return this.equals(empty);
    } */

    public GeospatialRange getEastWestRange() {
      return eastwest;
    }

    public GeospatialRange getNorthSouthRange() {
      return northsouth;
    }

    public GeospatialRange getUpDownRange() {
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

    public boolean isZPositiveUp() {
      return zpositive.equalsIgnoreCase("up");
    }

    public boolean isValid() {
      return isGlobal || ((eastwest != null) && (northsouth != null));
    }

    public boolean isGlobal() {
      return isGlobal;
    }

    public double getLatStart() {
      return (northsouth == null) ? Double.NaN : northsouth.start;
    }

    /**
     * @return latitude extent - may be positive or negetive
     */
    public double getLatExtent() {
      return (northsouth == null) ? Double.NaN : northsouth.size;
    }

    /**
     * @return latitude resolution: 0.0 or NaN means not set.
     */
    public double getLatResolution() {
      return (northsouth == null) ? Double.NaN : northsouth.resolution;
    }

    /**
     * @return latitude units
     */
    public String getLatUnits() {
      return (northsouth == null) ? null : northsouth.units;
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
     * @return longitude extent - may be positive or negetive
     */
    public double getLonExtent() {
      return (eastwest == null) ? Double.NaN : eastwest.size;
    }

    /**
     * @return longitude resolution: 0.0 or NaN means not set.
     */
    public double getLonResolution() {
      return (eastwest == null) ? Double.NaN : eastwest.resolution;
    }

    /**
     * @return longitude units
     */
    public String getLonUnits() {
      return (eastwest == null) ? null : eastwest.units;
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
     * @return height extent - may be positive or negetive
     */
    public double getHeightExtent() {
      return updown == null ? 0.0 : updown.size;
    }

    /**
     * @return height resolution: 0.0 or NaN means not set.
     */
    public double getHeightResolution() {
      return updown == null ? 0.0 : updown.resolution;
    }

    /**
     * @return height units
     */
    public String getHeightUnits() {
      return updown == null ? null : updown.units;
    }

    public LatLonRect getBoundingBox() {
      return isGlobal ? new LatLonRect() :
              new LatLonRect(new LatLonPointImpl(getLatStart(), getLonStart()), getLatExtent(), getLonExtent());
    }
  }

  /**
   * Implements spatialRange type.
   */
  @Immutable
  static public class GeospatialRange {
    public final double start, size, resolution;
    public final String units;

    /**
     * Constructor
     *
     * @param start      starting value
     * @param size       ending = start + size
     * @param resolution data resolution, or NaN if unknown
     * @param units      what units are start, size in?
     */
    public GeospatialRange(double start, double size, double resolution, String units) {
      this.start = start;
      this.size = size;
      this.resolution = resolution;
      this.units = units;
    }

    /**
     * Copy constructor
     *
     * @param from copy this
     */
    public GeospatialRange(GeospatialRange from) {
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

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Implements Variable type.
   */
  @Immutable
  static public class Variable implements Comparable<Variable> {
    public final String name, desc, vocabulary_name, units, id;

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

    public String getDescription() {
      return desc;
    }

    public String getVocabularyName() {
      return vocabulary_name;
    }

    // need unique id, cant count on name, eg because of variant GRIB tables
    public String getVocabularyId() {
      return id;
    }

    public String getUnits() {
      return units;
    }

    @Override
    public int compareTo(Variable o) {
      return name.compareTo(o.name);
    }
  }

  @Immutable
  static public class VariableGroup {
    public final String vocabulary, vocabHref;
    public final URI vocabUri, mapUri;
    public final List<Variable> variables;

    public VariableGroup(String vocab, String vocabHref, URI vocabUri, URI mapUri, List<Variable> variables) {
      this.vocabulary = vocab;
      this.vocabHref = vocabHref;
      this.vocabUri = vocabUri;
      this.mapUri = mapUri;
      Collections.sort(variables);
      this.variables = Collections.unmodifiableList(variables);
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

    public List<Variable> getVariableList() {
      return variables;
    }
  }

  @Immutable
  public static class MetadataOther {
    public final String title, type;
    public final String xlinkHref;
    public final String namespaceURI, prefix;
    public final boolean isInherited;
    public final Object contentObject;

    public MetadataOther(String xlinkHref, String title, String type, String namespaceURI, String prefix, boolean inherited) {
      this.xlinkHref = xlinkHref;
      this.title = title;
      this.type = type;
      this.namespaceURI = namespaceURI;
      this.prefix = prefix;
      this.isInherited = inherited;
      this.contentObject = null;
    }

    public MetadataOther(String mtype, String namespaceURI, String namespacePrefix, boolean inherited, Object contentObject) {
      this.xlinkHref = null;
      this.title = null;
      this.type = mtype;
      this.namespaceURI = namespaceURI;
      this.prefix = namespacePrefix;
      this.isInherited = inherited;
      this.contentObject = contentObject;
    }

    public boolean isInherited() {
      return isInherited;
    }

    public String getTitle() {
      return title;
    }

    public String getType() {
      return type;
    }

    public String getXlinkHref() {
      return xlinkHref;
    }

    public String getNamespaceURI() {
      return namespaceURI;
    }

    public String getPrefix() {
      return prefix;
    }

    public Object getContentObject() {
      return contentObject;
    }
  }
}
