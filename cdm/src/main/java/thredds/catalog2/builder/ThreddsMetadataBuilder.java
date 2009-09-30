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
package thredds.catalog2.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Iterator;

import ucar.nc2.constants.FeatureType;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog.DataFormatType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsMetadataBuilder extends ThreddsBuilder
{
  public boolean isEmpty();
  
  public DocumentationBuilder addDocumentation( String docType, String title, String externalReference );
  public DocumentationBuilder addDocumentation( String docType, String content );
  public boolean removeDocumentation( DocumentationBuilder docBuilder );
  public List<DocumentationBuilder> getDocumentationBuilders();

  public KeyphraseBuilder addKeyphrase( String authority, String phrase );
  public boolean removeKeyphrase( KeyphraseBuilder keyphraseBuilder );
  public List<KeyphraseBuilder> getKeyphraseBuilders();

  public ProjectNameBuilder addProjectName( String namingAuthority, String name);
  public boolean removeProjectName( ProjectNameBuilder projectNameBuilder);
  public List<ProjectNameBuilder> getProjectNameBuilders();


  public ContributorBuilder addCreator();
  public boolean removeCreator( ContributorBuilder creatorBuilder );
  public List<ContributorBuilder> getCreatorBuilder();

  public ContributorBuilder addContributor();
  public boolean removeContributor( ContributorBuilder contributorBuilder );
  public List<ContributorBuilder> getContributorBuilder();


  public ContributorBuilder addPublisher();
  public boolean removePublisher( ContributorBuilder PublisherBuilder );
  public List<ContributorBuilder> getPublisherBuilder();

  public DatePointBuilder addOtherDatePointBuilder( String date, String format, String type);
  public boolean removeOtherDatePointBuilder( DatePointBuilder builder);
  public List<DatePointBuilder> getOtherDatePointBuilders();


  public DatePointBuilder setCreatedDatePointBuilder( String date, String format );
  public DatePointBuilder getCreatedDatePointBuilder();

  public DatePointBuilder setModifiedDatePointBuilder( String date, String format );
  public DatePointBuilder getModifiedDatePointBuilder();

  public DatePointBuilder setIssuedDatePointBuilder( String date, String format );
  public DatePointBuilder getIssuedDatePointBuilder();

  public DatePointBuilder setValidDatePointBuilder( String date, String format );
  public DatePointBuilder getValidDatePointBuilder();

  public DatePointBuilder setAvailableDatePointBuilder( String date, String format );
  public DatePointBuilder getAvailableDatePointBuilder();

  public DatePointBuilder setMetadataCreatedDatePointBuilder( String date, String format );
  public DatePointBuilder getMetadataCreatedDatePointBuilder();

  public DatePointBuilder setMetadataModifiedDatePointBuilder( String date, String format );
  public DatePointBuilder getMetadataModifiedDatePointBuilder();

  public GeospatialCoverageBuilder setNewGeospatialCoverageBuilder( URI crsUri );
  public void removeGeospatialCoverageBuilder();
  public GeospatialCoverageBuilder getGeospatialCoverageBuilder();

  public DateRangeBuilder setTemporalCoverageBuilder( String startDate, String startDateFormat,
                                                      String endDate, String endDateFormat,
                                                      String duration, String resolution );
  public DateRangeBuilder getTemporalCoverageBuilder();

  public VariableGroupBuilder addVariableGroupBuilder();
  public boolean removeVariableGroupBuilder( VariableGroupBuilder varGroupBldr);
  public List<VariableGroupBuilder> getVariableGroupBuilders();

  public void setDataSizeInBytes( long dataSizeInBytes );
  public long getDataSizeInBytes();

  public void setDataFormat( DataFormatType dataFormat);
  public void setDataFormat( String dataFormat );
  public DataFormatType getDataFormat();

  public void setDataType( FeatureType dataType );
  public void setDataType( String dataType );
  public FeatureType getDataType();

  public void setCollectionType( String collectionType );
  public String getCollectionType();

  ThreddsMetadata build() throws BuilderException;

  public interface DocumentationBuilder extends ThreddsBuilder
  {
    //public void setContainedContent( boolean containedContent );
    public boolean isContainedContent();

    //public void setDocType( String docType );
    public String getDocType();

    //public void setContent( String content );
    public String getContent();

    //public void setTitle( String title );
    public String getTitle();

    //public void setExternalReference( URI externalReference );
    public String getExternalReference();
    public URI getExternalReferenceAsUri() throws URISyntaxException;

    public ThreddsMetadata.Documentation build() throws BuilderException;
  }

  public interface KeyphraseBuilder extends ThreddsBuilder
  {
    public String getAuthority();
    public String getPhrase();

    public ThreddsMetadata.Keyphrase build() throws BuilderException;
  }

  public interface ProjectNameBuilder extends ThreddsBuilder
  {
    public String getNamingAuthority();
    public String getName();

    public ThreddsMetadata.ProjectName build() throws BuilderException;
  }

    public interface DatePointBuilder extends ThreddsBuilder
    {
        public String getDate();
        public String getDateFormat();
        public boolean isTyped();
        public String getType();

        public ThreddsMetadata.DatePoint build() throws BuilderException;
    }

    public interface DateRangeBuilder extends ThreddsBuilder
    {
        public String getStartDateFormat();
        public String getStartDate();
        public String getEndDateFormat();
        public String getEndDate();
        public String getDuration();
        public String getResolution();

        public ThreddsMetadata.DateRange build() throws BuilderException;
    }


    public interface ContributorBuilder extends ThreddsBuilder
  {
    public String getName();
    public void setName( String name );
    public String getNamingAuthority();
    public void setNamingAuthority( String authority );
    public String getRole();
    public void setRole( String role );
    public String getEmail();
    public void setEmail( String email );

    public String getWebPage();
    public void setWebPage( String webPage );

    public ThreddsMetadata.Contributor build() throws BuilderException;
  }

  public interface VariableGroupBuilder extends ThreddsBuilder
  {
    public String getVocabularyAuthorityId();
    public void setVocabularyAuthorityId( String vocabAuthId);

    public String getVocabularyAuthorityUrl();
    public void setVocabularyAuthorityUrl( String vocabAuthUrl);

    public List<VariableBuilder> getVariableBuilders();
    public VariableBuilder addVariableBuilder( String name, String description, String units,
                                               String vocabId, String vocabName );

    public String getVariableMapUrl();
    public void setVariableMapUrl( String variableMapUrl);

    public boolean isEmpty();
  }

  public interface VariableBuilder extends ThreddsBuilder
  {
    public String getName();
    public void setName( String name);

    public String getDescription();
    public void setDescription( String description );

    public String getUnits();
    public void setUnits( String units );

    public String getVocabularyId();
    public void setVocabularyId( String vocabId);

    public String getVocabularyName();
    public void setVocabularyName( String vocabName);

    public String getVocabularyAuthorityId();
    public String getVocabularyAuthorityUrl();

    public ThreddsMetadata.Variable build() throws BuilderException;
  }

  public interface GeospatialCoverageBuilder extends ThreddsBuilder
  {
    public void setCRS( URI crsUri );
    public URI getCRS();

    public void setGlobal( boolean isGlobal );
    public boolean isGlobal();

    public void setZPositiveUp( boolean isZPositiveUp );
    public boolean isZPositiveUp();

    public GeospatialRangeBuilder addExtentBuilder();
    public boolean removeExtentBuilder( GeospatialRangeBuilder geospatialRangeBuilder );
    public List<GeospatialRangeBuilder> getExtentBuilders();

    public ThreddsMetadata.GeospatialCoverage build() throws BuilderException;
  }

  public interface GeospatialRangeBuilder extends ThreddsBuilder
  {
    public void setHorizontal( boolean isHorizontal );
    public boolean isHorizontal();

    public void setStart( double start );
    public double getStart();

    public void setSize( double size );
    public double getSize();

    public void setResolution( double resolution );
    public double getResolution();

    public void setUnits( String units );
    public String getUnits();

    public ThreddsMetadata.GeospatialRange build() throws BuilderException;
  }
}
