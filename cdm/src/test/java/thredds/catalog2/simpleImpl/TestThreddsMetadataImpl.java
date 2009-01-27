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

import junit.framework.*;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog.DataFormatType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestThreddsMetadataImpl extends TestCase
{
  private ThreddsMetadataImpl tmi;
  private ThreddsMetadataBuilder tmb;
  private ThreddsMetadata tm;

  private ThreddsMetadataBuilder.DocumentationBuilder docBldr1;
  private URI doc1RefUri;
  private String doc1Title;
  private ThreddsMetadataBuilder.DocumentationBuilder docBldr2;
  private String doc2Content;

  private String kp1, kp2, kp3;

  private String projectTitle;
  private DateType dateCreated;
  private DateType dateModified;
  private DateType dateIssued;
  private DateRange dateValid;
  private DateRange dateAvailable;
  private DateType dateMetadataCreated;
  private DateType dateMetadataModified;
  private DateRange temporalCoverage;
  private long dataSizeInBytes;
  private DataFormatType dataFormat;
  private FeatureType dataType;
  private String collectionType;


  public TestThreddsMetadataImpl( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    try
    { doc1RefUri = new URI( "http://server/thredds/doc.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); return; }
    this.doc1Title = "documentation title";
    this.doc2Content = "<x>some content</x>";

    tmi = new ThreddsMetadataImpl();
    tmb = tmi;
  }

  public void testSimpleSetGet()
  {
    projectTitle = "my project";
    dateCreated = new DateType( false, Calendar.getInstance().getTime());
    dateModified = dateCreated;
    dateIssued = dateCreated;
    TimeDuration timeDuration2Days = null;
    TimeDuration timeDuration3Days = null;
    try
    {
      timeDuration2Days = new TimeDuration( "P2D" );
      timeDuration3Days = new TimeDuration( "P3D" );
    }
    catch ( ParseException e )
    { fail( "Failed to parse \"P2D\": " + e.getMessage() ); }
    dateValid = new DateRange( dateCreated, null, timeDuration2Days, null);
    dateAvailable = new DateRange( dateCreated, null, timeDuration3Days, null);
    dateMetadataCreated = dateCreated;
    dateMetadataModified = dateCreated;
    temporalCoverage = dateValid;
    dataSizeInBytes = 56000;
    dataFormat = DataFormatType.NETCDF;
    dataType = FeatureType.TRAJECTORY;
    collectionType = "timeSeries";

    tmi.setProjectTitle( this.projectTitle );
    tmi.setDateCreated( this.dateCreated );
    tmi.setDateModified( this.dateModified );
    tmi.setDateIssued( this.dateIssued );
    tmi.setDateValid( this.dateValid );
    tmi.setDateAvailable( this.dateAvailable );
    tmi.setDateMetadataCreated( this.dateMetadataCreated );
    tmi.setDateMetadataModified( this.dateMetadataModified );
    tmi.setTemporalCoverage( this.temporalCoverage );
    tmi.setDataSizeInBytes( this.dataSizeInBytes );
    tmi.setDataFormat( this.dataFormat );
    tmi.setDataType( this.dataType );
    tmi.setCollectionType( this.collectionType );

    this.buildBuilder();

    assertTrue( tmb.isBuilt() );

    assertTrue( tmi.getProjectTitle().equals( this.projectTitle));
    assertTrue( tmi.getDateCreated().equals( this.dateCreated ));
    assertTrue( tmi.getDateModified().equals( this.dateModified ));
    assertTrue( tmi.getDateIssued().equals( this.dateIssued ));
    assertTrue( tmi.getDateValid().equals( this.dateValid ));
    assertTrue( tmi.getDateAvailable().equals( this.dateAvailable ));
    assertTrue( tmi.getDateMetadataCreated().equals( this.dateMetadataCreated ));
    assertTrue( tmi.getDateMetadataModified().equals( this.dateMetadataModified ));
    assertTrue( tmi.getTemporalCoverage().equals( this.temporalCoverage ));
    assertTrue( tmi.getDataSizeInBytes() == this.dataSizeInBytes );
    assertTrue( tmi.getDataFormat().equals( this.dataFormat ));
    assertTrue( tmi.getDataType().equals( this.dataType ));
    assertTrue( tmi.getCollectionType().equals( this.collectionType ));
  }

  public void testDocumentation()
  {
    assertFalse( tmi.isBuilt() );

    List<ThreddsMetadataBuilder.DocumentationBuilder> docBldrList = tmi.getDocumentationBuilders();
    assertTrue( docBldrList.isEmpty() );

    docBldr1 = tmi.addDocumentation( null, this.doc1Title, this.doc1RefUri );
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
    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( ! tmb.isBuildable( issues ) )
    {
      StringBuilder stringBuilder = new StringBuilder( "Not isBuildable(): " );
      for ( BuilderIssue bfi : issues )
        stringBuilder.append( "\n    " ).append( bfi.getMessage() ).append( " [" ).append( bfi.getBuilder().getClass().getName() ).append( "]" );
      fail( stringBuilder.toString() );
    }

    // Build
    try
    { tm = tmb.build(); }
    catch ( BuilderException e )
    { fail( "Build failed: " + e.getMessage() ); }

    assertTrue( tmb.isBuilt() );
  }

  private void checkBuilderSimpleSetIllegalState()
  {
    try
    { tmi.setProjectTitle( this.projectTitle ); }
    catch ( IllegalStateException ise )
    {
      return;
    }
    catch ( Exception e )
    { fail( "Unexpected non-IllegalStateException: " + e.getMessage()); }
    fail( "Did not throw expected IllegalStateException.");


    tmi.setDateCreated( this.dateCreated );
    tmi.setDateModified( this.dateModified );
    tmi.setDateIssued( this.dateIssued );
    tmi.setDateValid( this.dateValid );
    tmi.setDateAvailable( this.dateAvailable );
    tmi.setDateMetadataCreated( this.dateMetadataCreated );
    tmi.setDateMetadataModified( this.dateMetadataModified );
    tmi.setTemporalCoverage( this.temporalCoverage );
    tmi.setDataSizeInBytes( this.dataSizeInBytes );
    tmi.setDataFormat( this.dataFormat );
    tmi.setDataType( this.dataType );
    tmi.setCollectionType( this.collectionType );
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
