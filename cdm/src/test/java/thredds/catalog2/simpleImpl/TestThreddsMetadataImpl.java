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

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog.DataFormatType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import ucar.nc2.constants.FeatureType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestThreddsMetadataImpl
{
  private ThreddsMetadataImpl tmi;
  private ThreddsMetadataBuilder tmb;
  private ThreddsMetadata tm;

  private ThreddsMetadataBuilder.DocumentationBuilder docBldr1;
  private String doc1Ref;
  private URI doc1RefUri;
  private String doc1Title;
  private ThreddsMetadataBuilder.DocumentationBuilder docBldr2;
  private String doc2Content;

  private String kp1, kp2, kp3;

  private String projectTitle;

  private ThreddsMetadata.DatePoint dateCreated;
  private ThreddsMetadata.DatePoint dateModified;
  private ThreddsMetadata.DatePoint dateIssued;
  private ThreddsMetadata.DatePoint dateValid;
  private ThreddsMetadata.DatePoint dateAvailable;
  private ThreddsMetadata.DatePoint dateMetadataCreated;
  private ThreddsMetadata.DatePoint dateMetadataModified;

  private ThreddsMetadata.DateRange temporalCoverage;

  private long dataSizeInBytes;
  private DataFormatType dataFormat;
  private FeatureType dataType;
  private String collectionType;


    @Before
  public void setUp() throws URISyntaxException
  {
    doc1Ref = "http://server/thredds/doc.xml";
    doc1RefUri = new URI( doc1Ref );

    this.doc1Title = "documentation title";
    this.doc2Content = "<x>some content</x>";

    tmi = new ThreddsMetadataImpl();
    tmb = tmi;
  }

    @Test(expected = IllegalArgumentException.class)
    public void checkDatePointWithNullDateThrowsException()
            throws BuilderException
    {
        new ThreddsMetadataImpl.DatePointImpl( null, "", "");
    }

    @Test
    public void checkDatePointEquality()
            throws BuilderException
    {
        String dateString1 = "2009-08-25T12:00";
        String dateFormatString = "yyyy-MM-dd'T'HH:mm";
        String dateTypeCreated = "created";
        ThreddsMetadataImpl.DatePointImpl date1 = new ThreddsMetadataImpl.DatePointImpl( dateString1, dateFormatString, dateTypeCreated );
        ThreddsMetadataImpl.DatePointImpl date2 = new ThreddsMetadataImpl.DatePointImpl( dateString1, dateFormatString, dateTypeCreated );
        ThreddsMetadataImpl.DatePointImpl dateDiff1 = new ThreddsMetadataImpl.DatePointImpl( dateString1, dateFormatString, dateTypeCreated );
        assertEquals( date1, date2 );

        date1.build();
        assertEquals( date1, date2 );

        date2.build();
        assertEquals( date1, date2 );
    }

    @Test
  public void testSimpleSetGet()
  {
    projectTitle = "my project";
    dateCreated = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T12:00", null, ThreddsMetadata.DatePointType.Created.toString());
    dateModified = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T13:00", null, ThreddsMetadata.DatePointType.Modified.toString() );
    dateIssued = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T15:00", null, ThreddsMetadata.DatePointType.Issued.toString() );
    dateValid = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T12:00", null, ThreddsMetadata.DatePointType.Valid.toString() );
    dateAvailable = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T12:00", null, ThreddsMetadata.DatePointType.Available.toString() );
    dateMetadataCreated = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T12:00", null, ThreddsMetadata.DatePointType.MetadataCreated.toString() );
    dateMetadataModified = new ThreddsMetadataImpl.DatePointImpl( "2009-08-25T12:00", null, ThreddsMetadata.DatePointType.MetadataModified.toString() );
    temporalCoverage = new ThreddsMetadataImpl.DateRangeImpl( "2009-08-25T00:00", null, "2009-08-25T12:00", null, null, null);
    dataSizeInBytes = 56000;
    dataFormat = DataFormatType.NETCDF;
    dataType = FeatureType.TRAJECTORY;
    collectionType = "timeSeries";

    tmi.addProjectName( null, this.projectTitle );

    tmi.setCreatedDatePointBuilder( this.dateCreated.getDate(), this.dateCreated.getDateFormat() );
    tmi.setModifiedDatePointBuilder( this.dateModified.getDate(),  this.dateModified.getDateFormat());
    tmi.setIssuedDatePointBuilder( this.dateIssued.getDate(), this.dateIssued.getDateFormat() );
    tmi.setValidDatePointBuilder( this.dateValid.getDate(), this.dateValid.getDateFormat() );
    tmi.setAvailableDatePointBuilder( this.dateAvailable.getDate(), this.dateAvailable.getDateFormat() );
    tmi.setMetadataCreatedDatePointBuilder( this.dateMetadataCreated.getDate(), this.dateMetadataCreated.getDateFormat() );
    tmi.setMetadataModifiedDatePointBuilder( this.dateMetadataModified.getDate(), this.dateMetadataModified.getDateFormat() );

    tmi.setTemporalCoverageBuilder( this.temporalCoverage.getStartDate(), this.temporalCoverage.getStartDateFormat(),
                                    this.temporalCoverage.getEndDate(), this.temporalCoverage.getEndDateFormat(),
                                    this.temporalCoverage.getDuration(), this.temporalCoverage.getResolution() );
    tmi.setDataSizeInBytes( this.dataSizeInBytes );
    tmi.setDataFormat( this.dataFormat );
    tmi.setDataType( this.dataType );
    tmi.setCollectionType( this.collectionType );

    this.buildBuilder();

    assertTrue( tmb.isBuilt() );

    List<ThreddsMetadata.ProjectName> projectNames = tmi.getProjectNames();
    assertNotNull( projectNames);
    assertEquals( 1, projectNames.size());
    ThreddsMetadata.ProjectName projectName = projectNames.get( 0 );
    assertNotNull( projectName );
    assertEquals( this.projectTitle, projectName.getName());

    assertTrue( tmi.getCreatedDate().equals( this.dateCreated ));
    assertTrue( tmi.getModifiedDate().equals( this.dateModified ));
    assertTrue( tmi.getIssuedDate().equals( this.dateIssued ));
    assertTrue( tmi.getValidDate().equals( this.dateValid ));
    assertTrue( tmi.getAvailableDate().equals( this.dateAvailable ));
    assertTrue( tmi.getMetadataCreatedDate().equals( this.dateMetadataCreated ));
    assertTrue( tmi.getMetadataModifiedDate().equals( this.dateMetadataModified ));
    assertTrue( tmi.getTemporalCoverage().equals( this.temporalCoverage ));
    assertTrue( tmi.getDataSizeInBytes() == this.dataSizeInBytes );
    assertTrue( tmi.getDataFormat().equals( this.dataFormat ));
    assertTrue( tmi.getDataType().equals( this.dataType ));
    assertTrue( tmi.getCollectionType().equals( this.collectionType ));
  }

    @Test
  public void testDocumentation()
  {
    assertFalse( tmi.isBuilt() );

    List<ThreddsMetadataBuilder.DocumentationBuilder> docBldrList = tmi.getDocumentationBuilders();
    assertTrue( docBldrList.isEmpty() );

    docBldr1 = tmi.addDocumentation( null, this.doc1Title, this.doc1Ref );
    docBldr2 = tmi.addDocumentation( null, this.doc2Content );

    docBldrList = tmi.getDocumentationBuilders();
    assertTrue( docBldrList.size() == 2);
    assertTrue( docBldrList.get( 0 ) == docBldr1 );
    assertTrue( docBldrList.get( 1) == docBldr2);

    tmi.removeDocumentation( docBldr1 );
    docBldrList = tmi.getDocumentationBuilders();
    assertTrue( docBldrList.size() == 1 );
    assertTrue( docBldrList.get( 0 ) == docBldr2 );

    this.checkBuilderDocumentationIllegalStateGet();
    this.buildBuilder();

    assertTrue( tmb.isBuilt());
    assertTrue( docBldr2.isBuilt());
    List<ThreddsMetadata.Documentation> docList = tm.getDocumentation();
    assertTrue( docList.size() == 1 );
    assertTrue( docList.get( 0 ) == docBldr2 );

    this.checkBuiltDocumentationIllegalStateGet();
  }

  private void buildBuilder()
  {
    // Check if buildable
    BuilderIssues issues = tmb.getIssues();
    if ( ! issues.isValid() )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " ).append( issues.toString() );
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { tm = tmb.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertTrue( tmb.isBuilt() );
  }

  private void checkBuilderDocumentationIllegalStateGet()
  {
    try
    { tmi.getDocumentation(); }
    catch ( IllegalStateException ise )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    fail( "Did not throw expected IllegalStateException.");
  }

  private void checkBuiltDocumentationIllegalStateGet()
  {
    try
    { tmi.getDocumentationBuilders(); }
    catch ( IllegalStateException ise )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    fail( "Did not throw expected IllegalStateException.");
  }
}
