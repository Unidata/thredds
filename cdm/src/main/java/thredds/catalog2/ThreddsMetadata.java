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
package thredds.catalog2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Iterator;

import ucar.nc2.constants.FeatureType;
import thredds.catalog.DataFormatType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsMetadata
{
  public List<Documentation> getDocumentation();

  public List<Keyphrase> getKeyphrases();
  public List<ProjectName> getProjectNames();
  public List<Contributor> getCreator();
  public List<Contributor> getContributor();
  public List<Contributor> getPublisher();

  public List<DatePoint> getOtherDates();
  public DatePoint getCreatedDate();
  public DatePoint getModifiedDate();
  public DatePoint getIssuedDate();

  public DatePoint getValidDate();  // Should really be some kind of DateRange.
  public DatePoint getAvailableDate();  // Should really be some kind of DateRange.

  public DatePoint getMetadataCreatedDate();
  public DatePoint getMetadataModifiedDate();

  public GeospatialCoverage getGeospatialCoverage();
  public DateRange getTemporalCoverage();  
  public List<VariableGroup> getVariableGroups();

  public long getDataSizeInBytes();
  public DataFormatType getDataFormat();
  public FeatureType getDataType();
  public String getCollectionType(); // ?????

    public interface Documentation
  {
    public boolean isContainedContent();

    public String getDocType(); //"summary" ("abstract"?), "history", "processing_level", "funding", "rights"

    public String getContent();

    public String getTitle();
    public String getExternalReference();
    public URI getExternalReferenceAsUri() throws URISyntaxException;
  }

  public interface Keyphrase
  {
    public String getAuthority();
    public String getPhrase();
  }

  public interface ProjectName
  {
    public String getNamingAuthority();
    public String getName();
  }

    public enum DatePointType
    {

        Created( "created"),
        Modified( "modified"),
        Valid( "valid"),
        Issued( "issued"),
        Available( "available"),
        MetadataCreated( "metadataCreated"),
        MetadataModified( "metadataModified"),

        Other( ""),
        Untyped( "");

        private final String label;

        DatePointType( String label) {
            this.label = label;
        }

        public static DatePointType getTypeForLabel( String label) {
            if ( label == null || label.equals( "" ))
                return Untyped;
            for ( DatePointType dpt : DatePointType.values())
                if ( dpt.label.equalsIgnoreCase( label ))
                    return dpt;
            return Other;
        }

        public String toString() {
            return this.label;
        }
    }

    public interface DatePoint
    {
        public String getDate();
        public String getDateFormat();
        public boolean isTyped();
        public String getType();
    }

    public interface DateRange
    {
        public String getStartDateFormat();
        public String getStartDate();
        public String getEndDateFormat();
        public String getEndDate();
        public String getDuration();
        public String getResolution();
    }

  public interface Contributor
  {
    public String getName();
    public String getNamingAuthority();
    public String getRole();
    public String getEmail();
    public String getWebPage();
  }

  public interface VariableGroup
  {
    public String getVocabularyAuthorityId();
    public String getVocabularyAuthorityUrl();

    public List<Variable> getVariables();
    public String getVariableMapUrl();
    public boolean isEmpty();
  }

  public interface Variable
  {
    public String getName();
    public String getDescription();
    public String getUnits();

    public String getVocabularyId();
    public String getVocabularyName();

    public String getVocabularyAuthorityId();
    public String getVocabularyAuthorityUrl();
  }

  public interface GeospatialCoverage
  {
    public URI getCRS();
    public boolean isGlobal();
    public boolean isZPositiveUp();   // Is this needed since have CRS?
    public List<GeospatialRange> getExtent(); 
  }

  public interface GeospatialRange
  {
    public boolean isHorizontal();
    public double getStart();
    public double getSize();
    public double getResolution();
    public String getUnits();
  }
}