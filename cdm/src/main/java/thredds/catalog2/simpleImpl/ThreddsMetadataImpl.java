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
package thredds.catalog2.simpleImpl;

import thredds.catalog2.ThreddsMetadata;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog.DataFormatType;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.nc2.constants.FeatureType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataImpl
        implements ThreddsMetadata, ThreddsMetadataBuilder
{
  private boolean isBuilt;

  private List<DocumentationImpl> docs;
  private List<KeyphraseImpl> keyphrases;
  private List<ContributorImpl> creators;
  private List<ContributorImpl> contributors;
  private List<ContributorImpl> publishers;

  private String projectTitle;

  private List<DatePointImpl> otherDates;
  private DatePointImpl createdDate;
  private DatePointImpl modifiedDate;
  private DatePointImpl issuedDate;
  private DatePointImpl validDate;
  private DatePointImpl availableDate;
  private DatePointImpl metadataCreatedDate;
  private DatePointImpl metadataModifiedDate;

  private GeospatialCoverageImpl geospatialCoverage;
  private DateRangeImpl temporalCoverage;

  private List<VariableImpl> variables;
  private long dataSizeInBytes;
  private DataFormatType dataFormat;
  private FeatureType dataType;
  private String collectionType;

    public ThreddsMetadataImpl()
  {
    this.isBuilt = false;
    this.dataSizeInBytes = -1;
  }

  public boolean isEmpty()
  {
    if ( this.docs != null && ! this.docs.isEmpty() )
      return false;
    if ( this.keyphrases != null && ! this.keyphrases.isEmpty() )
      return false;
    if ( this.creators != null && ! this.creators.isEmpty() )
      return false;
    if ( this.contributors != null && ! this.contributors.isEmpty() )
      return false;
    if ( this.publishers != null && ! this.publishers.isEmpty() )
      return false;

    if ( this.projectTitle != null || this.createdDate != null || this.modifiedDate != null
         || this.issuedDate != null || this.validDate != null || this.availableDate != null
         || this.metadataCreatedDate != null || this.metadataModifiedDate != null
         || this.geospatialCoverage != null || this.temporalCoverage != null )
      return false;

    if ( this.variables != null && ! this.variables.isEmpty() )
      return false;

    if ( this.dataSizeInBytes != -1 || this.dataFormat != null || this.dataType != null || this.collectionType != null )
      return false;

    return true;
  }

  public DocumentationBuilder addDocumentation( String docType, String title, URI externalReference )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built.");
    if ( title == null ) throw new IllegalArgumentException( "Title may not be null.");
    if ( externalReference == null ) throw new IllegalArgumentException( "External reference may not be null.");
    if ( this.docs == null )
      this.docs = new ArrayList<DocumentationImpl>();
    DocumentationImpl doc = new DocumentationImpl( docType, title, externalReference );
    this.docs.add( doc );
    return doc;
  }

  public DocumentationBuilder addDocumentation( String docType, String content )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( content == null ) throw new IllegalArgumentException( "Content may not be null." );
    if ( this.docs == null )
      this.docs = new ArrayList<DocumentationImpl>();
    DocumentationImpl doc = new DocumentationImpl( docType, content );
    this.docs.add( doc );
    return doc;
  }

  public boolean removeDocumentation( DocumentationBuilder docBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( docBuilder == null )
      return false;
    if ( this.docs == null )
      return false;
    return this.docs.remove( (DocumentationImpl) docBuilder );
  }

  public List<DocumentationBuilder> getDocumentationBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.docs == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DocumentationBuilder>( this.docs) );
  }

  public List<Documentation> getDocumentation()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.docs == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Documentation>( this.docs ) );
  }

  public KeyphraseBuilder addKeyphrase( String authority, String phrase )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( phrase == null )
      throw new IllegalArgumentException( "Phrase may not be null.");
    if ( this.keyphrases == null )
      this.keyphrases = new ArrayList<KeyphraseImpl>();
    KeyphraseImpl keyphrase = new KeyphraseImpl( authority, phrase);
    this.keyphrases.add( keyphrase );
    return keyphrase;
  }

  public boolean removeKeyphrase( KeyphraseBuilder keyphraseBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( keyphraseBuilder == null )
      return false;
    if ( this.keyphrases == null )
      return false;
    return this.keyphrases.remove( (KeyphraseImpl) keyphraseBuilder );
  }

  public List<KeyphraseBuilder> getKeyphraseBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.keyphrases == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<KeyphraseBuilder>( this.keyphrases ) );
  }

  public List<Keyphrase> getKeyphrases()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.keyphrases == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Keyphrase>( this.keyphrases ) );
  }

  public ContributorBuilder addCreator()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.creators == null )
      this.creators = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.creators.add( contributor );
    return contributor;
  }

  public boolean removeCreator( ContributorBuilder creatorBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( creatorBuilder == null )
      return false;
    if ( this.creators == null )
      return false;
    return this.creators.remove( (ContributorImpl) creatorBuilder );
  }

  public List<ContributorBuilder> getCreatorBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.creators == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.creators ) );
  }

  public List<Contributor> getCreator()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.creators == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.creators ) );
  }

  public ContributorBuilder addContributor()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.contributors == null )
      this.contributors = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.contributors.add( contributor );
    return contributor;
  }

  public boolean removeContributor( ContributorBuilder contributorBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( contributorBuilder == null )
      return false;
    if ( this.contributors == null )
      return false;
    return this.contributors.remove( (ContributorImpl) contributorBuilder );
  }

  public List<ContributorBuilder> getContributorBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.contributors == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.contributors ) );
  }

  public List<Contributor> getContributor()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.contributors == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.contributors ) );
  }

  public ContributorBuilder addPublisher()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.publishers == null )
      this.publishers = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.publishers.add( contributor );
    return contributor;
  }

  public boolean removePublisher( ContributorBuilder publisherBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( publisherBuilder == null )
      return false;
    if ( this.publishers == null )
      return false;
    return this.publishers.remove( (ContributorImpl) publisherBuilder );
  }

  public List<ContributorBuilder> getPublisherBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.publishers == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.publishers ) );
  }

  public List<Contributor> getPublisher()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.publishers == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.publishers ) );
  }

  public void setProjectTitle( String projectTitle )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.projectTitle = projectTitle;
  }

  public String getProjectTitle()
  {
    return this.projectTitle;
  }

    public DatePointBuilder addOtherDatePointBuilder( String date, String format, String type )
    {
        if ( this.isBuilt )
            throw new IllegalStateException( "This Builder has been built." );
        DatePointType datePointType = DatePointType.getTypeForLabel( type );
        if ( datePointType != DatePointType.Other
             && datePointType != DatePointType.Untyped )
            throw new IllegalArgumentException( "Must use explicit setter method for given type [" + type + "]." );
        if ( this.otherDates == null )
            this.otherDates = new ArrayList<DatePointImpl>();
        DatePointImpl dp = new DatePointImpl( date, format, type);
        this.otherDates.add( dp );
        return dp;
    }

    public boolean removeOtherDatePointBuilder( DatePointBuilder builder )
    {
        if ( this.isBuilt )
            throw new IllegalStateException( "This Builder has been built." );
        if ( builder == null )
            return false;
        if ( this.otherDates == null )
            return false;
        return this.otherDates.remove( (DatePointImpl) builder );
    }

    public List<DatePointBuilder> getOtherDatePointBuilders()
    {
        if ( this.isBuilt )
            throw new IllegalStateException( "This Builder has been built." );
        if ( this.otherDates == null )
            return Collections.emptyList();
        return Collections.unmodifiableList( new ArrayList<DatePointBuilder>( this.otherDates) );
    }

    public List<DatePoint> getOtherDates()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        if ( this.otherDates == null )
            return Collections.emptyList();
        return Collections.unmodifiableList( new ArrayList<DatePoint>( this.otherDates ) );
    }

    public DatePointBuilder setCreatedDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.createdDate = new DatePointImpl( date, format, DatePointType.Created.toString());
    return this.createdDate;
  }

  public DatePointBuilder getCreatedDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.createdDate;
  }

    public DatePoint getCreatedDate()
    {
        if ( ! this.isBuilt)
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.createdDate;
    }

  public DatePointBuilder setModifiedDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.modifiedDate = new DatePointImpl( date, format, DatePointType.Modified.toString() );
    return this.modifiedDate;
  }

  public DatePointBuilder getModifiedDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.modifiedDate;
  }

    public DatePoint getModifiedDate() {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.modifiedDate;
    }

  public DatePointBuilder setIssuedDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.issuedDate = new DatePointImpl( date, format, DatePointType.Issued.toString() );
    return this.issuedDate;
  }

  public DatePointBuilder getIssuedDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.issuedDate;
  }

    public DatePoint getIssuedDate()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.issuedDate;
    }

    public DatePointBuilder setValidDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.validDate = new DatePointImpl( date, format, DatePointType.Valid.toString() );
    return this.validDate;
  }

  public DatePointBuilder getValidDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.validDate;
  }

    public DatePoint getValidDate()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.validDate;
    }

  public DatePointBuilder setAvailableDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.availableDate = new DatePointImpl( date, format, DatePointType.Available.toString() );
    return this.availableDate;
  }

  public DatePointBuilder getAvailableDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.availableDate;
  }

    public DatePoint getAvailableDate()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.availableDate;
    }

  public DatePointBuilder setMetadataCreatedDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.metadataCreatedDate = new DatePointImpl( date, format, DatePointType.MetadataCreated.toString() );
    return this.metadataCreatedDate;
  }

  public DatePointBuilder getMetadataCreatedDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.metadataCreatedDate;
  }

    public DatePoint getMetadataCreatedDate()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.metadataCreatedDate;
    }

  public DatePointBuilder setMetadataModifiedDatePointBuilder( String date, String format )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.metadataModifiedDate = new DatePointImpl( date, format, DatePointType.MetadataModified.toString() );
    return this.metadataModifiedDate;
  }

  public DatePointBuilder getMetadataModifiedDatePointBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.metadataModifiedDate;
  }

    public DatePoint getMetadataModifiedDate()
    {
        if ( !this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
        return this.metadataModifiedDate;
    }

    public GeospatialCoverageBuilder setNewGeospatialCoverageBuilder( URI crsUri )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    GeospatialCoverageImpl gci = new GeospatialCoverageImpl();
    gci.setCRS( crsUri );
    this.geospatialCoverage = gci;
    return null;
  }

  public void removeGeospatialCoverageBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.geospatialCoverage = null;
  }

  public GeospatialCoverageBuilder getGeospatialCoverageBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    return this.geospatialCoverage;
  }

  public GeospatialCoverage getGeospatialCoverage()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    return this.geospatialCoverage;
  }

  public DateRangeBuilder setTemporalCoverageBuilder( String startDate, String startDateFormat,
                                                      String endDate, String endDateFormat, String duration )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.temporalCoverage = new DateRangeImpl( startDate, startDateFormat, endDate, endDateFormat, duration );
    return this.temporalCoverage;
  }

  public DateRangeBuilder getTemporalCoverageBuilder()
  {
      if ( this.isBuilt )
          throw new IllegalStateException( "This Builder has been built." );
      return this.temporalCoverage;
  }

    public DateRange getTemporalCoverage() {
        if ( ! this.isBuilt )
            throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built.");
        return this.temporalCoverage;
    }

  public VariableBuilder addVariableBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.variables == null )
      this.variables = new ArrayList<VariableImpl>();
    VariableImpl var = new VariableImpl();
    this.variables.add( var);
    return var;
  }

  public boolean removeVariableBuilder( VariableBuilder variableBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( variableBuilder == null )
      return false;
    if ( this.variables == null )
      return false;
    return this.variables.remove( (VariableImpl) variableBuilder );
  }

  public List<VariableBuilder> getVariableBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.variables == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<VariableBuilder>( this.variables ) );
  }

  public List<Variable> getVariables()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.variables == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Variable>( this.variables ) );
  }

  public void setDataSizeInBytes( long dataSizeInBytes )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataSizeInBytes = dataSizeInBytes;
  }

  public long getDataSizeInBytes()
  {
    return this.dataSizeInBytes;
  }

  public void setDataFormat( DataFormatType dataFormat )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataFormat = dataFormat;
  }
  public void setDataFormat( String dataFormat )
  {
    this.setDataFormat( DataFormatType.getType( dataFormat));
  }

  public DataFormatType getDataFormat()
  {
    return this.dataFormat;
  }

  public void setDataType( FeatureType dataType )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataType = dataType;
  }
    public void setDataType( String dataType)
    {
        this.setDataType( FeatureType.getType( dataType ));
    }

  public FeatureType getDataType()
  {
    return this.dataType;
  }

  public void setCollectionType( String collectionType )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.collectionType = collectionType;
  }

  public String getCollectionType() // ?????
  {
    return this.collectionType;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public boolean isBuildable( BuilderIssues issues )
  {
    if ( this.isBuilt )
      return true;

    BuilderIssues localIssues = new BuilderIssues();

    // Check subordinates.
    if ( this.docs != null )
      for ( DocumentationImpl doc : this.docs )
        doc.isBuildable( localIssues );
    // ToDo keywords
    // ToDo ...


    if ( localIssues.isEmpty() )
      return true;

    issues.addAllIssues( localIssues );
    return false;
  }

  public ThreddsMetadata build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    BuilderIssues issues = new BuilderIssues();
    if ( ! isBuildable( issues ) )
      throw new BuilderException( issues );

    // Check subordinates.
    if ( this.docs != null )
      for ( DocumentationImpl doc : this.docs )
        doc.build();
    // ToDo keywords
    // ToDo ...

    this.isBuilt = true;
    return this;
  }

    public static class DocumentationImpl
          implements Documentation, DocumentationBuilder
  {
    private boolean isBuilt = false;

    private final boolean isContainedContent;

    public final String docType;
    public final String title;
    public final URI externalReference;
    public final String content;

    public DocumentationImpl( String docType, String title, URI externalReference )
    {
      if ( title == null ) throw new IllegalArgumentException( "Title may not be null.");
      if ( externalReference == null ) throw new IllegalArgumentException( "External reference may not be null.");
      this.isContainedContent = false;
      this.docType = docType;
      this.title = title;
      this.externalReference = externalReference;
      this.content = null;
    }

    public DocumentationImpl( String docType, String content )
    {
      if ( content == null ) throw new IllegalArgumentException( "Content may not be null." );
      this.isContainedContent = true;
      this.docType = docType;
      this.title = null;
      this.externalReference = null;
      this.content = content;
    }

    public boolean isContainedContent()
    {
      return this.isContainedContent;
    }

    public String getDocType()
    {
      return this.docType;
    }

    public String getContent()
    {
      if ( ! this.isContainedContent )
        throw new IllegalStateException( "No contained content, use externally reference to access documentation content." );
      return this.content;
    }

    public String getTitle()
    {
      if ( this.isContainedContent )
        throw new IllegalStateException( "Documentation with contained content has no title." );
      return this.title;
    }

    public URI getExternalReference()
    {
      if ( this.isContainedContent )
        throw new IllegalStateException( "Documentation with contained content has no external reference.");
      return this.externalReference;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues )
    {
      return true;
    }

    public Documentation build() throws BuilderException
    {
        this.isBuilt = true;
        return this;
    }
  }

  public static class KeyphraseImpl
          implements Keyphrase, KeyphraseBuilder
  {
    private boolean isBuilt;
    private final String authority;
    private final String phrase;

    public KeyphraseImpl( String authority, String phrase)
    {
        if ( phrase == null || phrase.equals( "" ))
            throw new IllegalArgumentException( "Phrase may not be null.");
        this.authority = authority;
        this.phrase = phrase;
        this.isBuilt = false;
    }

    public String getAuthority()
    {
      return this.authority;
    }

    public String getPhrase()
    {
      return this.phrase;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues ) {
        return true;
    }

    public Keyphrase build() throws BuilderException {
        this.isBuilt = true;
        return this;
    }
  }

    public static class DatePointImpl
            implements DatePoint, DatePointBuilder
    {
        private boolean isBuilt = false;

        private final String date;
        private final String format;
        private final String type;

        public DatePointImpl( String date, String format, String type) {
            if ( date == null )
                throw new IllegalArgumentException( "Date may not be null.");

            this.date = date;
            this.format = format;
            this.type = type;
        }

        public String getDate() {
            return this.date;
        }

        public String getDateFormat() {
            return this.format;
        }

        public boolean isTyped() {
            return this.type != null || this.type.equals(  "" );
        }

        public String getType() {
            return this.type;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj ) return true;
            if ( ! ( obj instanceof DatePointImpl )) return false;
            return obj.hashCode() == this.hashCode();
        }

        @Override
        public int hashCode()
        {
            int result = 17;
            if ( this.date != null )
                result = 37*result + this.date.hashCode();
            if ( this.format != null )
                result = 37*result + this.format.hashCode();
            if ( this.type != null )
                result = 37*result + this.type.hashCode();
            return result;
        }

        public boolean isBuilt() {
            return this.isBuilt;
        }

        public boolean isBuildable( BuilderIssues issues ) {
            return true;
        }

        public DatePoint build() throws BuilderException {
            this.isBuilt = true;
            return this;
        }
    }

    public static class DateRangeImpl
            implements DateRange, DateRangeBuilder
    {
        private boolean isBuilt = false;

        private final String startDateFormat;
        private final String startDate;
        private final String endDateFormat;
        private final String endDate;
        private final String duration;

        public DateRangeImpl( String startDate, String startDateFormat,
                              String endDate, String endDateFormat,
                              String duration )
        {
            this.startDateFormat = startDateFormat;
            this.startDate = startDate;
            this.endDateFormat = endDateFormat;
            this.endDate = endDate;
            this.duration = duration;
        }

        public String getStartDateFormat()
        {
            return this.startDateFormat;
        }

        public String getStartDate()
        {
            return this.startDate;
        }

        public String getEndDateFormat()
        {
            return this.endDateFormat;
        }

        public String getEndDate()
        {
            return this.endDate;
        }

        public String getDuration()
        {
            return this.duration;
        }

        public String toString()
        {
            return (this.isBuilt ? "DateRange" : "DateRangeBuilder") +
                   " [" + this.startDate + " <-- " + this.duration + " --> " + this.endDate + "]";
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj ) return true;
            if ( !( obj instanceof DateRangeImpl ) ) return false;
            return obj.hashCode() == this.hashCode();
        }

        @Override
        public int hashCode()
        {
            int result = 17;
            if ( this.startDate != null )
                result = 37 * result + this.startDate.hashCode();
            if ( this.startDateFormat != null )
                result = 37 * result + this.startDateFormat.hashCode();
            if ( this.endDate != null )
                result = 37 * result + this.endDate.hashCode();
            if ( this.endDateFormat != null )
                result = 37 * result + this.endDateFormat.hashCode();
            if ( this.duration != null )
                result = 37 * result + this.duration.hashCode();
            return result;
        }

        public boolean isBuilt() {
            return this.isBuilt;
        }

        public boolean isBuildable( BuilderIssues issues )
        {
            if (this.isBuilt) return true;

            int specified = 3;
            if ( this.startDate == null || this.startDate.equals( "" ) )
                specified--;
            if ( this.endDate == null || this.endDate.equals( "" ) )
                specified--;
            if ( this.duration == null || this.duration.equals( "" ) )
                specified--;

            if ( specified == 2 )
                return true;
            else if ( specified < 2)
              issues.addIssue( "Underspecified " + this.toString(), this);
            else // if (specified > 2)
              issues.addIssue( "Overspecified " + this.toString(), this);
            
            return false;
        }

        public DateRange build() throws BuilderException {
            this.isBuilt = true;
            return this;
        }
    }

  public static class ContributorImpl
          implements Contributor, ContributorBuilder
  {
    private boolean isBuilt;

    private String authority;
    private String name;
    private String email;
    private URI webPage;

    public String getAuthority()
    {
      return this.authority;
    }

    public void setAuthority( String authority )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.authority = authority;
    }

    public String getName()
    {
      return this.name;
    }

    public void setName( String name )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( name == null )
        throw new IllegalArgumentException( "Name may not be null.");
      this.name = name;
    }

    public String getEmail()
    {
      return this.email;
    }

    public void setEmail( String email )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.email = email;
    }

    public URI getWebPage()
    {
      return this.webPage;
    }

    public void setWebPage( URI webPage )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.webPage = webPage;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues )
    {
      return true;
    }

    public Contributor build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }

  public static class VariableImpl
          implements Variable, VariableBuilder
  {
    private boolean isBuilt;

    private String authority = "";
    private String id;
    private String title = "";
    private String description = "";
    private String units = "";

    public VariableImpl() {}

    public String getAuthority()
    {
      return this.authority;
    }

    public void setAuthority( String authority )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.authority = authority == null ? "" : authority;
    }

    public String getId()
    {
      return this.id;
    }

    public void setId( String id )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( id == null )
        throw new IllegalArgumentException( "Id may not be null.");
      this.id = id;
    }

    public String getTitle()
    {
      return this.title;
    }

    public void setTitle( String title )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.title = title == null ? "" : title;
    }

    public String getDescription()
    {
      return this.description;
    }

    public void setDescription( String description )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.description = description == null ? "" : description;
    }

    public String getUnits()
    {
      return this.units;
    }

    public void setUnits( String units )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.units = units == null ? "" : units;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues )
    {
      return true;
    }

    public Variable build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }

  public static class GeospatialCoverageImpl
          implements GeospatialCoverage,
                     GeospatialCoverageBuilder
  {
    private boolean isBuilt;

    private URI defaultCrsUri;

    private URI crsUri;
    private boolean is3D;
    private boolean isZPositiveUp;

    private boolean isGlobal;
    private List<GeospatialRangeImpl> extent;

    GeospatialCoverageImpl()
    {
      this.isBuilt = false;
      String defaultCrsUriString = "urn:x-mycrs:2D-WGS84-ellipsoid";
      try
      { this.defaultCrsUri = new URI( defaultCrsUriString ); }
      catch ( URISyntaxException e )
      { throw new IllegalStateException( "Bad URI syntax for default CRS URI ["+defaultCrsUriString+"]: " + e.getMessage()); }
      this.crsUri = this.defaultCrsUri;
    }

    public void setCRS( URI crsUri )
    {
      if ( this.isBuilt)
        throw new IllegalStateException( "This Builder has been built.");
      if ( crsUri == null )
        this.crsUri = this.defaultCrsUri;
      this.crsUri = crsUri;
    }

    public URI getCRS()
    {
      return this.crsUri;
    }

    public void setGlobal( boolean isGlobal )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.isGlobal = isGlobal;
    }

    public boolean isGlobal()
    {
      return this.isGlobal;
    }

    public void setZPositiveUp( boolean isZPositiveUp )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.isZPositiveUp = isZPositiveUp;
    }

    public boolean isZPositiveUp()   // Is this needed since have CRS?
    {
      return this.isZPositiveUp;
    }

    public GeospatialRangeBuilder addExtentBuilder()
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( this.extent == null )
        this.extent = new ArrayList<GeospatialRangeImpl>();
      GeospatialRangeImpl gri = new GeospatialRangeImpl();
      this.extent.add( gri );
      return gri;
    }

    public boolean removeExtentBuilder( GeospatialRangeBuilder geospatialRangeBuilder )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( geospatialRangeBuilder == null )
        return true;
      if ( this.extent == null )
        return false;
      return this.extent.remove( (GeospatialRangeImpl) geospatialRangeBuilder );
    }

    public List<GeospatialRangeBuilder> getExtentBuilders()
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( this.extent == null )
        return Collections.emptyList();
      return Collections.unmodifiableList( new ArrayList<GeospatialRangeBuilder>( this.extent) );
    }

    public List<GeospatialRange> getExtent()
    {
      if ( ! this.isBuilt )
        throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
      if ( this.extent == null )
        return Collections.emptyList();
      return Collections.unmodifiableList( new ArrayList<GeospatialRange>( this.extent ) );
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues )
    {
      return false;
    }

    public GeospatialCoverage build() throws BuilderException
    {
      return this;
    }
  }

  public static class GeospatialRangeImpl
          implements GeospatialRange,
                     GeospatialRangeBuilder
  {
    private boolean isBuilt;

    private boolean isHorizontal;
    private double start;
    private double size;
    private double resolution;
    private String units;

    public GeospatialRangeImpl()
    {
      this.isBuilt = false;
      
      this.isHorizontal = false;
      this.start = 0.0;
      this.size = 0.0;
      this.resolution = 0.0;
      this.units = "";
    }

    public void setHorizontal( boolean isHorizontal )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built.");
      this.isHorizontal = isHorizontal;
    }

    public boolean isHorizontal()
    {
      return this.isHorizontal;
    }

    public void setStart( double start )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.start = start;
    }

    public double getStart()
    {
      return this.start;
    }

    public void setSize( double size )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.size = size;
    }

    public double getSize()
    {
      return this.size;
    }

    public void setResolution( double resolution )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.resolution = resolution;
    }

    public double getResolution()
    {
      return this.resolution;
    }

    public void setUnits( String units )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.units = units == null ? "" : units;
    }

    public String getUnits()
    {
      return this.units;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( BuilderIssues issues )
    {
      return true;
    }

    public GeospatialRange build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }
}
