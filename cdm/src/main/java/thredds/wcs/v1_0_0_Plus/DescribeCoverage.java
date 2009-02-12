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
package thredds.wcs.v1_0_0_Plus;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.output.XMLOutputter;

import java.util.List;
import java.util.Date;
import java.io.PrintWriter;
import java.io.IOException;

import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DescribeCoverage extends WcsRequest
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( DescribeCoverage.class );

  private List<String> coverages;

  private Document describeCoverageDoc;

  public DescribeCoverage( Operation operation, String version, WcsDataset dataset,
                           List<String> coverages )
  {
    super( operation, version, dataset );

    this.coverages = coverages;
    if ( this.coverages == null )
      throw new IllegalArgumentException( "Non-null coverage list required." );
    if ( this.coverages.size() < 1 )
      throw new IllegalArgumentException( "Coverage list must contain at least one ID <" + this.coverages.size() + ">." );
    String badCovIds = "";
    for ( String curCov : coverages )
    {
      if ( ! this.getDataset().isAvailableCoverageName( curCov))
        badCovIds += (badCovIds.length() > 0 ? ", " : "") + curCov;
    }
    if ( badCovIds.length() > 0 )
      throw new IllegalArgumentException("Coverage ID list contains one or more unknown IDs <" + badCovIds + ">." );
  }

  public Document getDescribeCoverageDoc()
  {
    if ( this.describeCoverageDoc == null )
      describeCoverageDoc = generateDescribeCoverageDoc();
    return describeCoverageDoc;
  }

  public void writeDescribeCoverageDoc( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    xmlOutputter.output( getDescribeCoverageDoc(), pw );
  }

  public Document generateDescribeCoverageDoc()
  {
    // CoverageDescription (wcs) [1]
    Element coverageDescriptionsElem = new Element( "CoverageDescription", wcsNS );
    coverageDescriptionsElem.addNamespaceDeclaration( gmlNS );
    coverageDescriptionsElem.addNamespaceDeclaration( xlinkNS );
    coverageDescriptionsElem.setAttribute( "version", this.getVersion() );
    // ToDo Consider dealing with "updateSequence"
    // coverageDescriptionsElem.setAttribute( "updateSequence", this.getCurrentUpdateSequence() );

    for ( String curCoverageId : this.coverages )
      coverageDescriptionsElem.addContent( genCoverageOfferingElem( curCoverageId ) );

    return new Document( coverageDescriptionsElem );
  }

  public Element genCoverageOfferingElem( String covId )
  {
    WcsCoverage coverage = this.getDataset().getAvailableCoverage( covId );
    GridCoordSystem gridCoordSystem = coverage.getCoordinateSystem();

    // CoverageDescription/CoverageOffering (wcs) [1..*]
    Element covDescripElem = genCoverageOfferingBriefElem( "CoverageOffering", covId,
                                                           coverage.getLabel(), coverage.getDescription(),
                                                           gridCoordSystem );

    // CoverageDescription/CoverageOffering/domainSet [1]
    covDescripElem.addContent( genDomainSetElem( coverage));

    // CoverageDescription/CoverageOffering/rangeSet [1]
    covDescripElem.addContent( genRangeSetElem( coverage));

    // CoverageDescription/CoverageOffering/supportedCRSs [1]
    covDescripElem.addContent( genSupportedCRSsElem( coverage));

    // CoverageDescription/CoverageOffering/supportedFormats [1]
    covDescripElem.addContent( genSupportedFormatsElem( coverage) );

    // CoverageDescription/CoverageOffering/supportedInterpolations [0..1]
    covDescripElem.addContent( genSupportedInterpolationsElem() );

    return covDescripElem;
  }

  private Element genDomainSetElem( WcsCoverage coverage)
  {
    // ../domainSet
    Element domainSetElem = new Element( "domainSet", wcsNS);

    // ../domainSet/spatialDomain [0..1] AND/OR temporalDomain [0..1]
    domainSetElem.addContent( genSpatialDomainElem( coverage) );
    if ( coverage.getCoordinateSystem().hasTimeAxis() )
    {
      domainSetElem.addContent( genTemporalDomainElem( coverage.getCoordinateSystem().getTimeAxis1D() ) );
    }

    return domainSetElem;
  }

  private Element genSpatialDomainElem( WcsCoverage coverage )
  {
    // ../domainSet/spatialDomain
    Element spatialDomainElem = new Element( "spatialDomain", wcsNS );
    
    // ../domainSet/spatialDomain/gml:Envelope [1..*]
    spatialDomainElem.addContent( this.genEnvelopeElem( coverage.getCoordinateSystem()));

    // ../domainSet/spatialDomain/gml:RectifiedGrid [0..*]
    spatialDomainElem.addContent( this.genRectifiedGridElem( coverage));

    // ../domainSet/spatialDomain/gml:Polygon [0..*]

    return spatialDomainElem;
  }

  private Element genRectifiedGridElem( WcsCoverage coverage )
  {
    // ../spatialDomain/gml:RectifiedGrid
    Element rectifiedGridElem = new Element( "RectifiedGrid", gmlNS);

    CoordinateAxis1D xaxis = (CoordinateAxis1D) coverage.getCoordinateSystem().getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) coverage.getCoordinateSystem().getYHorizAxis();
    CoordinateAxis1D zaxis = coverage.getCoordinateSystem().getVerticalAxis();

    // ../spatialDomain/gml:RectifiedGrid@srsName [0..1] (URI)
    rectifiedGridElem.setAttribute( "srsName", coverage.getNativeCrs() );

    // ../spatialDomain/gml:RectifiedGrid@dimension [1] (positive integer)
    int ndim = ( zaxis != null ) ? 3 : 2;
    rectifiedGridElem.setAttribute( "dimension", Integer.toString( ndim ) );

    // ../spatialDomain/gml:RectifiedGrid/gml:limits [1]
    int[] minValues = new int[ndim];
    int[] maxValues = new int[ndim];

    maxValues[0] = (int) ( xaxis.getSize() - 1 );
    maxValues[1] = (int) ( yaxis.getSize() - 1 );
    if ( zaxis != null )
      maxValues[2] = (int) ( zaxis.getSize() - 1 );

    Element limitsElem =  new Element( "limits", gmlNS);

    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope [1]
    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:low [1] (integer list)
    // ../spatialDomain/gml:RectifiedGrid/gml:limits/gml:GridEnvelope/gml:high [1] (integer list)
    limitsElem.addContent(
            new Element( "GridEnvelope", gmlNS)
                    .addContent( new Element( "low", gmlNS ).addContent( genIntegerListString( minValues)))
                    .addContent( new Element( "high", gmlNS).addContent( genIntegerListString( maxValues))) );

    rectifiedGridElem.addContent( limitsElem);

    // ../spatialDomain/gml:RectifiedGrid/gml:axisName [1..*] (string)
    rectifiedGridElem.addContent( new Element( "axisName", gmlNS).addContent( "x"));
    rectifiedGridElem.addContent( new Element( "axisName", gmlNS).addContent( "y"));
    if ( zaxis != null )
      rectifiedGridElem.addContent( new Element( "axisName", gmlNS ).addContent( "z" ) );

    // ../spatialDomain/gml:RectifiedGrid/gml:origin [1]
    // ../spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos [1] (space seperated list of double values)
    // ../spatialDomain/gml:RectifiedGrid/gml:origin/gml:pos@dimension [0..1]  (number of entries in list)
    double[] origin = new double[ndim];
    origin[0] = xaxis.getStart();
    origin[1] = yaxis.getStart();
    if ( zaxis != null )
      origin[2] = zaxis.getStart();

    rectifiedGridElem.addContent(
            new Element( "origin", gmlNS).addContent(
                    new Element( "pos", gmlNS).addContent( genDoubleListString( origin))));

    // ../spatialDomain/gml:RectifiedGrid/gml:offsetVector [1..*] (space seperated list of double values)
    // ../spatialDomain/gml:RectifiedGrid/gml:offsetVector@dimension [0..1]  (number of entries in list)
    double[] xoffset = new double[ndim];
    xoffset[0] = xaxis.getIncrement();
    rectifiedGridElem.addContent(
            new Element( "offsetVector", gmlNS)
                    .addContent( genDoubleListString( xoffset)));

    double[] yoffset = new double[ndim];
    yoffset[1] = yaxis.getIncrement();
    rectifiedGridElem.addContent(
            new Element( "offsetVector", gmlNS )
                    .addContent( genDoubleListString( yoffset ) ) );

    if ( zaxis != null )
    {
      double[] zoffset = new double[ndim];
      zoffset[2] = zaxis.getIncrement();
      rectifiedGridElem.addContent(
              new Element( "offsetVector", gmlNS )
                      .addContent( genDoubleListString( zoffset ) ) );
    }

    return rectifiedGridElem;
  }

  private String genIntegerListString( int[] values)
  {
    StringBuffer buf = new StringBuffer();
    for ( int intValue : values )
    {
      if ( buf.length() > 0 )
        buf.append( " ");
      buf.append( intValue);
    }
    return buf.toString();
  }

  private String genDoubleListString( double[] values)
  {
    StringBuffer buf = new StringBuffer();
    for ( double doubleValue : values )
    {
      if ( buf.length() > 0 )
        buf.append( " ");
      buf.append( doubleValue);
    }
    return buf.toString();
  }

  private Element genEnvelopeElem( GridCoordSystem gcs )
  {
    // spatialDomain/Envelope
    Element envelopeElem;
    if ( gcs.hasTimeAxis() )
      envelopeElem = new Element( "EnvelopeWithTimePeriod", wcsNS );
    else
      envelopeElem = new Element( "Envelope", wcsNS );

    // spatialDomain/Envelope@srsName [0..1] (URI)
    envelopeElem.setAttribute( "srsName", "urn:ogc:def:crs:OGC:1.3:CRS84" );

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    double lon = llpt.getLongitude() + llbb.getWidth();
    int posDim = 2;
    String firstPosition = llpt.getLongitude() + " " + llpt.getLatitude();
    String secondPosition = lon + " " + urpt.getLatitude();
    // ToDo WCS 1.0Plus - Deal with conversion to meters. (Yikes!!)
    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
    if ( vertAxis != null )
    {
      posDim++;
      // See verAxis.getUnitsString()
      double zeroIndexValue = vertAxis.getCoordValue( 0 );
      double sizeIndexValue = vertAxis.getCoordValue( ( (int) vertAxis.getSize() ) - 1 );
      if ( vertAxis.getPositive().equals( ucar.nc2.constants.CF.POSITIVE_UP ) )
      {
        firstPosition += " " + zeroIndexValue;
        secondPosition += " " + sizeIndexValue;
      }
      else
      {
        firstPosition += " " + sizeIndexValue;
        secondPosition += " " + zeroIndexValue;
      }
    }

    String posDimString = Integer.toString( posDim );

    // spatialDomain/Envelope/gml:pos [2] (space seperated list of double values)
    // spatialDomain/Envelope/gml:pos@dimension [0..1]  (number of entries in list)
    envelopeElem.addContent(
            new Element( "pos", gmlNS )
                    .addContent( firstPosition )
                    .setAttribute( new Attribute( "dimension", posDimString) ) );
    envelopeElem.addContent(
            new Element( "pos", gmlNS )
                    .addContent( secondPosition )
                    .setAttribute( new Attribute( "dimension", posDimString ) ) );

    // spatialDomain/Envelope/gml:timePostion [2]
    if ( gcs.hasTimeAxis() )
    {
      envelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getStart().toDateTimeStringISO()));
      envelopeElem.addContent(
              new Element( "timePosition", gmlNS).addContent(
                      gcs.getDateRange().getEnd().toDateTimeStringISO()));
    }

    return envelopeElem;
  }

  private Element genTemporalDomainElem( CoordinateAxis1DTime timeAxis )
  {
    // temporalDomain
    Element temporalDomainElem = new Element( "temporalDomain", wcsNS );

    Date[] dates = timeAxis.getTimeDates();
    DateFormatter formatter = new DateFormatter();

    // temporalDomain/timePosition [1..*]
    for ( Date curDate : dates )
    {
      temporalDomainElem.addContent(
              new Element( "timePosition", gmlNS)
                      .addContent( formatter.toDateTimeStringISO( curDate )));
    }

      return temporalDomainElem;
  }

  private Element genRangeSetElem( WcsCoverage coverage )
  {
    // rangeSet
    Element rangeSetElem = new Element( "rangeSet", wcsNS);

    // rangeSet/RangeSet
    // rangeSet/RangeSet@semantic
    // rangeSet/RangeSet@refSys
    // rangeSet/RangeSet@refSysLabel
    //Element innerRangeSetElem = new Element( "RangeSet", wcsNS);
    // ToDo How deal with range fields and thier axes?
    for ( WcsRangeField curField: coverage.getRange() )
    {
      Element fieldElem = new Element( "Field", wcsNS);

      // rangeSet/RangeSet/description [0..1]
      if ( curField.getDescription() != null )
        fieldElem.addContent(
                new Element( "description")
                        .addContent( curField.getDescription() ) );

      // rangeSet/RangeSet/name [1]

      fieldElem.addContent(
              new Element( "name", wcsNS).addContent( curField.getName()));

      // rangeSet/RangeSet/label [1]
      fieldElem.addContent(
              new Element( "label", wcsNS ).addContent( curField.getLabel() ) );
      fieldElem.addContent(
              new Element( "dataType", wcsNS ).addContent( curField.getDatatypeString() ) );
      fieldElem.addContent(
              new Element( "units", wcsNS ).addContent( curField.getUnitsString() ) );

      // .../values/interval
      // .../values/interval/min [0..1]
      // .../values/interval/max [0..1]
      // .../values/interval/res [0..1]

      Element valuesElem = new Element( "AllowedValues", wcsNS );
      Element minElem = new Element( "MinimumValue", wcsNS )
              .addContent( Double.toString(
                      curField.getValidMin()));
      Element maxElem = new Element( "MaximumValue", wcsNS )
              .addContent( Double.toString(
                      curField.getValidMin()));
      valuesElem.addContent( new Element( "Range", wcsNS)
              .addContent( minElem )
              .addContent( maxElem ));

      List<WcsRangeField.Axis> axes = curField.getAxes();
      for ( WcsRangeField.Axis curAxis: axes)
      {
        if ( curAxis != null )
        {
          // rangeSet/RangeSet/axisDescription [0..*]
          //Element axisDescElem = new Element( "axisDescription", wcsNS );
          Element axisDescElem = new Element( "Axis", wcsNS );

          // rangeSet/RangeSet/axisDescription/AxisDescription [1]
          //Element innerAxisDescElem = new Element( "AxisDescription", wcsNS);

          // rangeSet/RangeSet/axisDescription/AxisDescription/name [1]
          // rangeSet/RangeSet/axisDescription/AxisDescription/label [1]
          axisDescElem.addContent( new Element( "name", wcsNS).addContent( curAxis.getName() ));
          axisDescElem.addContent( new Element( "label", wcsNS).addContent( curAxis.getLabel()));

          // rangeSet/RangeSet/axisDescription/AxisDescription/values [1]
          Element axisValuesElem = new Element( "values", wcsNS);

          // rangeSet/RangeSet/axisDescription/AxisDescription/values/singleValue [1..*]
          // ----- interval is alternate for singleValue
          // rangeSet/RangeSet/axisDescription/AxisDescription/values/interval
          // rangeSet/RangeSet/axisDescription/AxisDescription/values/interval/min [0..1]
          // rangeSet/RangeSet/axisDescription/AxisDescription/values/interval/max [0..1]
          // rangeSet/RangeSet/axisDescription/AxisDescription/values/interval/res [0..1]
          // -----
          for ( String curVal : curAxis.getValues())
            axisValuesElem.addContent(
                    new Element( "singleValue", wcsNS)
                            .addContent( curVal));

          // rangeSet/RangeSet/axisDescription/AxisDescription/values/default [0..1]

          axisDescElem.addContent( axisValuesElem);
          fieldElem.addContent( axisDescElem);
        }
      }


      // rangeSet/RangeSet/nullValues [0..1]
      // rangeSet/RangeSet/nullValues/{interval|singleValue} [1..*]
      if ( curField.hasMissingData() )
      {
        fieldElem.addContent(
                new Element( "nullValues", wcsNS).addContent(
                        new Element( "singleValue", wcsNS)
                                // ToDo Is missing always NaN?
                                .addContent( "NaN" ) ) );
      }
      rangeSetElem.addContent( fieldElem);
    }
    return rangeSetElem;
  }

  private Element genSupportedCRSsElem( WcsCoverage coverage )
  {
    // supportedCRSs
    Element supportedCRSsElem = new Element( "supportedCRSs", wcsNS);

    // supportedCRSs/requestCRSs [1..*] (wcs) (space seperated list of strings)
    // supportedCRSs/requestCRSs@codeSpace [0..1] (URI)
    supportedCRSsElem.addContent(
            new Element( "requestCRSs", wcsNS)
                    .addContent( coverage.getDefaultRequestCrs()));

    // supportedCRSs/responseCRSs [1..*] (wcs) (space seperated list of strings)
    // supportedCRSs/responseCRSs@codeSpace [0..1] (URI)
    supportedCRSsElem.addContent(
            new Element( "responseCRSs", wcsNS)
                    .addContent( coverage.getNativeCrs()));

    return supportedCRSsElem;
  }

  private Element genSupportedFormatsElem( WcsCoverage coverage)
  {
    // supportedFormats
    // supportedFormats@nativeFormat [0..1] (string)
    Element supportedFormatsElem = new Element( "supportedFormats", wcsNS );

    // supportedFormats/formats [1..*] (wcs) (space seperated list of strings)
    // supportedFormats/formats@codeSpace [0..1] (URI)
    for ( WcsRequest.Format curFormat : coverage.getSupportedCoverageFormatList() )
    {
      supportedFormatsElem.addContent(
              new Element( "formats", wcsNS )
                      .addContent( curFormat.toString() ) );
    }

    return supportedFormatsElem;
  }

  private Element genSupportedInterpolationsElem()
  {
    // supportedInterpolations
    // supportedInterpolations@default [0..1] ???
    Element supportedInterpolationsElem = new Element( "supportedInterpolations", wcsNS);

    // supportedInterpolations/interpolationMethod [1..*]
    supportedInterpolationsElem.addContent(
            new Element( "interpolationMethod", wcsNS)
                    .addContent( "none"));

    return supportedInterpolationsElem;
  }
}