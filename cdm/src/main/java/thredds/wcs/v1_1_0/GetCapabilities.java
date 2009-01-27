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
package thredds.wcs.v1_1_0;

import ucar.nc2.dt.GridDataset;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URI;
import java.io.PrintWriter;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCapabilities
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( GetCapabilities.class );

  protected static final Namespace wcsNS = Namespace.getNamespace( "http://www.opengis.net/wcs/1.1" );
  protected static final Namespace owcsNS = Namespace.getNamespace( "owcs", "http://www.opengis.net/wcs/1.1/ows" );
  protected static final Namespace owsNS = Namespace.getNamespace( "ows", "http://www.opengis.net/ows" );
  protected static final Namespace xlinkNS = Namespace.getNamespace( "xlink", "http://www.w3.org/1999/xlink" );

  public enum Section
  {
    ServiceIdentification, ServiceProvider,
    OperationsMetadata, Contents, All
  }

  private URI serverURI;

  private List<Section> sections;

  private String version = "1.1.0";
  private ServiceId serviceId;
  private ServiceProvider serviceProvider;
  private GridDataset dataset;

  private Document capabilitiesReport;

  public GetCapabilities( URI serverURI, List<Section> sections,
                          ServiceId serviceId, ServiceProvider serviceProvider,
                          GridDataset dataset )
  {
    this.serverURI = serverURI;
    this.sections = sections;
    this.serviceId = serviceId;
    this.serviceProvider = serviceProvider;
    this.dataset = dataset;
    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Non-null server URI required.");
    if ( this.sections == null )
      throw new IllegalArgumentException( "Non-null sections list required (may be empty).");
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required.");
  }

  public Document getCapabilitiesReport()
  {
    if ( this.capabilitiesReport == null )
      capabilitiesReport = generateCapabilities();
    return capabilitiesReport;
  }

  public void writeCapabilitiesReport( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
    xmlOutputter.output( getCapabilitiesReport(), pw );
  }

  public Document generateCapabilities()
  {
    Element capabilitiesElem = new Element( "Capabilities", wcsNS );
    capabilitiesElem.addNamespaceDeclaration( owcsNS );
    capabilitiesElem.addNamespaceDeclaration( owsNS );
    capabilitiesElem.addNamespaceDeclaration( xlinkNS );
    capabilitiesElem.setAttribute( "version", this.version);           // ToDo
    //capabilitiesElem.setAttribute( "updateSequence", "");    // ToDo

    boolean allSections = false;
    if ( sections == null || sections.size() == 0 ||
         ( sections.size() == 1 && sections.get( 0 ).equals( Section.All ) ) )
    {
      allSections = true;
    }

    if ( allSections || sections.contains( Section.ServiceIdentification))
    {
      capabilitiesElem.addContent( generateServiceIdentification( serviceId  ));
    }
    if ( allSections || sections.contains( Section.ServiceProvider))
    {
      capabilitiesElem.addContent( generateServiceProvider( serviceProvider) );
    }
    if ( allSections || sections.contains( Section.OperationsMetadata ))
    {
      capabilitiesElem.addContent( generateOperationsMetadata());
    }
    if ( allSections || sections.contains( Section.Contents))
    {
      capabilitiesElem.addContent( generateContents());
    }

    return new Document( capabilitiesElem );
  }

  public Element generateServiceIdentification( ServiceId serviceId )
  {
    Element serviceIdElem = new Element( "ServiceIdentification", owcsNS );

    if ( serviceId != null  )
    {
      if ( serviceId.getTitle() != null )
      {
        Element titleElem = new Element( "Title", owsNS );
        titleElem.addContent( serviceId.getTitle() );
        serviceIdElem.addContent( titleElem );
      }

      if ( serviceId.getAbstract() != null )
      {
        Element abstractElem = new Element( "Abstract", owsNS );
        abstractElem.addContent( serviceId.getAbstract() );
        serviceIdElem.addContent( abstractElem );
      }

      if ( serviceId.getKeywords() != null &&
           serviceId.getKeywords().size() > 0 )
      {
        Element keywordsElem = new Element( "Keywords", owsNS );
        for ( String curKey : serviceId.getKeywords() )
        {
          Element keywordElem = new Element( "Keyword", owsNS );
          keywordElem.addContent( curKey );
          keywordsElem.addContent( keywordElem );
        }
        serviceIdElem.addContent( keywordsElem );
      }

      if ( serviceId.getServiceType() != null )
      {
        Element serviceTypeElem = new Element( "ServiceType", owcsNS );
        serviceTypeElem.addContent( serviceId.getServiceType() );
        serviceIdElem.addContent( serviceTypeElem );
      }

      if ( serviceId.getServiceTypeVersion() != null &&
           serviceId.getServiceTypeVersion().size() > 0 )
      {
        for ( String curVer : serviceId.getServiceTypeVersion() )
        {
          Element serviceTypeVersionElem = new Element( "ServiceTypeVersion", owcsNS );
          serviceTypeVersionElem.addContent( curVer );
          serviceIdElem.addContent( serviceTypeVersionElem );
        }
      }

      // ToDo When would this be needed? What are Application Profiles? GML profiles?
//    List<URI> appProfileIds;
//    for ( URI curAppProfileId : appProfileIds)
//    {
//      serviceIdElem.addContent( new Element( "Profile", owcsNS).addContent( curAppProfileId.toString()));
//    }
      // 1) WCS spec says that encoding format application profile spec has title of the form:
      //    "WCS 1.1 Application Profile for [Format] [formatVersion] encoding, [profileVersion]"
      //    [[Here's the CF-NetCDF title:
      //          "WCS 1.1 Application Profile for CF-netCDF (1.0-3.0) Coverage Encoding, 1.0" (OGC 06-082r1)]]
      //    but it doesn't say anything about a URI for such an Application Profile.
      // 2) Whereas, the WCS Capabilities doc has a place to identify supported App Profiles by using a URI.

      if ( serviceId.getFees() != null )
      {
        Element feesElem = new Element( "Fees", owcsNS );
        feesElem.addContent( serviceId.getFees() );
        serviceIdElem.addContent( feesElem );
      }

      if ( serviceId.getAccessConstraints() != null &&
           serviceId.getAccessConstraints().size() > 0 )
      {
        for ( String curAC : serviceId.getAccessConstraints() )
        {
          Element accessConstraintsElem = new Element( "AccessConstraints", owcsNS );
          accessConstraintsElem.addContent( curAC );
          serviceIdElem.addContent( accessConstraintsElem );
        }
      }
    }

    return serviceIdElem;
  }

  public Element generateServiceProvider( ServiceProvider serviceProvider )
  {
    // ServiceProvider (ows) [0..1]
    Element servProvElem = new Element( "ServiceProvider", owsNS );

    if ( serviceProvider != null )
    {
      if ( serviceProvider.name != null )
      {
        // ServiceProvider/ProviderName (ows) [0..1]
        servProvElem.addContent(
                new Element( "ProviderName", owsNS ).addContent(
                        serviceProvider.name ) );
      }

      if ( serviceProvider.site != null )
      {
        // ServiceProvider/ProviderSite (ows) [0..1]
        Element provSiteElem = new Element( "ProviderSite", owsNS );
        provSiteElem.setAttribute( "type", "simple" );
        if ( serviceProvider.site.title != null)
          provSiteElem.setAttribute( "title", serviceProvider.site.title, xlinkNS );
        if ( serviceProvider.site.link != null )
          provSiteElem.setAttribute( "href", serviceProvider.site.link.toString(), xlinkNS );
        servProvElem.addContent( provSiteElem );
      }


      if ( serviceProvider.contact != null )
      {
        // ServiceProvider/ServiceContact (ows) [0..1]
        Element servContactElem = new Element( "ServiceContact", owsNS );

        if ( serviceProvider.contact.individualName != null )
        {
          // ServiceProvider/ServiceContact/IndividualName (ows) [0..1]
          Element individualNameElem = new Element( "IndividualName", owsNS);
          individualNameElem.addContent( serviceProvider.contact.individualName);
          servContactElem.addContent( individualNameElem);
        }

        if ( serviceProvider.contact.positionName != null )
        {
          // ServiceProvider/ServiceContact/PositionName (ows) [0..1]
          Element positionNameElem = new Element( "PositionName", owsNS);
          positionNameElem.addContent( serviceProvider.contact.positionName );
          servContactElem.addContent( positionNameElem );
        }

        if ( serviceProvider.contact.contactInfo != null )
        {
          // ServiceProvider/ServiceContact/ContactInfo (ows)[0..1]
          Element contactInfoElem = new Element( "ContactInfo", owsNS);
          if ( serviceProvider.contact.contactInfo.voicePhone != null ||
                  serviceProvider.contact.contactInfo.faxPhone != null )
          {
            // ServiceProvider/ServiceContact/ContactInfo/Phone (ows)[0..1]
            Element phoneElem = new Element( "Phone", owsNS);
            if ( serviceProvider.contact.contactInfo.voicePhone != null )
              for (String curPhone : serviceProvider.contact.contactInfo.voicePhone)
                // ServiceProvider/ServiceContact/ContactInfo/Phone/Voice (ows)[0..*]
                phoneElem.addContent( new Element( "Voice", owsNS ).addContent( curPhone ));
            if ( serviceProvider.contact.contactInfo.faxPhone != null )
              for (String curPhone : serviceProvider.contact.contactInfo.faxPhone)
                // ServiceProvider/ServiceContact/ContactInfo/Phone/Facsimile (ows)[0..*]
                phoneElem.addContent( new Element( "Facsimile", owsNS ).addContent( curPhone ));
            contactInfoElem.addContent( phoneElem);
          }

          if ( serviceProvider.contact.contactInfo.address != null )
          {
            // ServiceProvider/ServiceContact/ContactInfo/Address (ows) [0..1]
            Element addressElem = new Element( "Address", owsNS);
            if ( serviceProvider.contact.contactInfo.address.deliveryPoint != null )
            {
              for ( String curDP : serviceProvider.contact.contactInfo.address.deliveryPoint)
              {
                // ServiceProvider/ServiceContact/ContactInfo/Address/DeliveryPoint (ows) [0..*]
                addressElem.addContent( new Element( "DeliveryPoint", owsNS).addContent( curDP));
              }
            }
            if ( serviceProvider.contact.contactInfo.address.city != null )
            {
              // ServiceProvider/ServiceContact/ContactInfo/Address/City (ows) [0..1]
              addressElem.addContent( new Element( "City", owsNS)
                      .addContent( serviceProvider.contact.contactInfo.address.city));
            }
            if ( serviceProvider.contact.contactInfo.address.adminArea != null )
            {
              // ServiceProvider/ServiceContact/ContactInfo/Address/AdministrativeArea (ows) [0..1]
              addressElem.addContent( new Element( "AdministrativeArea", owsNS )
                      .addContent( serviceProvider.contact.contactInfo.address.adminArea ) );
            }
            if ( serviceProvider.contact.contactInfo.address.postalCode != null )
            {
              // ServiceProvider/ServiceContact/ContactInfo/Address/PostalCode (ows) [0..1]
              addressElem.addContent( new Element( "PostalCode", owsNS )
                      .addContent( serviceProvider.contact.contactInfo.address.postalCode ) );
            }
            if ( serviceProvider.contact.contactInfo.address.country != null)
            {
              // ServiceProvider/ServiceContact/ContactInfo/Address/Country (ows) [0..1]
              addressElem.addContent( new Element( "Country", owsNS )
                      .addContent( serviceProvider.contact.contactInfo.address.country ) );
            }
            if ( serviceProvider.contact.contactInfo.address.email != null )
            {
              for ( String curEmail : serviceProvider.contact.contactInfo.address.email )
              {
                // ServiceProvider/ServiceContact/ContactInfo/Address/ElectronicMailAddress (ows) [0..*]
                addressElem.addContent( new Element( "ElectronicMailAddress", owsNS )
                        .addContent( curEmail ) );
              }
            }

            contactInfoElem.addContent( addressElem);
          }

          if ( serviceProvider.contact.contactInfo.onlineResource != null )
          {
            // ServiceProvider/ServiceContact/ContactInfo/OnlineResource (ows) [0..1]
            Element onlineResourceElem = new Element( "OnlineResource", owsNS);
            onlineResourceElem.setAttribute( "type", "simple");
            if ( serviceProvider.contact.contactInfo.onlineResource.title != null )
              onlineResourceElem.setAttribute( "title", serviceProvider.contact.contactInfo.onlineResource.title, xlinkNS );
            if ( serviceProvider.contact.contactInfo.onlineResource.link != null )
              onlineResourceElem.setAttribute( "href", serviceProvider.contact.contactInfo.onlineResource.link.toString(), xlinkNS );

            contactInfoElem.addContent( onlineResourceElem);
          }

          if ( serviceProvider.contact.contactInfo.hoursOfService != null )
            // ServiceProvider/ServiceContact/ContactInfo/HoursOfService (ows) [0..1]
            contactInfoElem.addContent( new Element( "HoursOfService", owsNS)
                    .addContent( serviceProvider.contact.contactInfo.hoursOfService));

          if ( serviceProvider.contact.contactInfo.contactInstructions != null )
            // ServiceProvider/ServiceContact/ContactInfo/ContactInstructions (ows) [0..1]
            contactInfoElem.addContent( new Element( "ContactInstructions", owsNS )
                    .addContent( serviceProvider.contact.contactInfo.contactInstructions ) );

          servContactElem.addContent( contactInfoElem);
        }

        if ( serviceProvider.contact.role != null )
        {
          // ServiceProvider/ServiceContact/Role (ows) [0..1]
          servContactElem.addContent( new Element( "Role", owsNS )
                  .addContent( serviceProvider.contact.role));
        }

        servProvElem.addContent( servContactElem );
      }
    }
    return servProvElem;
  }

  public Element generateOperationsMetadata()
  {
    // OperationsMetadata (owcs) [0..1]
    Element opsMetadataElem = new Element( "OperationsMetadata", owcsNS );

    // OperationsMetadata/Operation (owcs) [2..*]
    // OperationsMetadata/Operation@name - i.e., "GetCapabilities" or "DescribeCoverage" or "GetCoverage"
    // OperationsMetadata/Operation/...
    opsMetadataElem.addContent( genGetCapOpsElement());
    opsMetadataElem.addContent( genDescCovOpsElement());
    opsMetadataElem.addContent( genGetCovOpsElement());

    // OperationsMetadata/Parameter (owcs) [0..*]
    // OperationsMetadata/Parameter/..(?) (owcs) [0..*]

    // OperationsMetadata/Constraint (owcs) [0..*]
    // OperationsMetadata/Constraint/.. (owcs) [0..*]

    // OperationsMetadata/ExtendedCapabilities (owcs) [0..1]
    // OperationsMetadata/ExtendedCapabilities/.. (owcs) [0..1]
    //
    return opsMetadataElem;
  }

  private Element genGetCapOpsElement()
  {
    // OperationsMetadata/Operation (owcs) @name="GetCapabilities"
    Element getCapOpsElem = new Element( "Operation", owcsNS );
    getCapOpsElem.setAttribute( "name", Request.Operation.GetCapabilities.toString() );

    // Add DCP/HTTP/GET element with xlink to this server.
    // OperationsMetadata/Operation/DCP/HTTP/{GET|POST} (owcs) [1..*]
    //                       -  @type=simple, @xlink:title, @xlink:href
    getCapOpsElem.addContent(
            new Element( "DCP", owcsNS ).addContent(
                    new Element( "HTTP", owcsNS ).addContent(
                            new Element( "GET", owcsNS ).setAttribute(
                                    "href", serverURI.toString(), xlinkNS ) ) ) );

    // Add the "Service", "AcceptVersions", and "Sections" parameters.
    // OperationsMetadata/Operation/Parameter (owcs) [0..*]
    getCapOpsElem.addContent( genParamElement( "service", Collections.singletonList( "WCS" ) ) );
    List<String> allowedValList = new ArrayList<String>();
    allowedValList.add( "1.1.0" );
    allowedValList.add( "1.0.0" );
    getCapOpsElem.addContent( genParamElement( "AcceptVersions", allowedValList ) );

    List<String> sectList = new ArrayList<String>();
    sectList.add( "ServiceIdentification" );
    sectList.add( "ServiceProvider" );
    sectList.add( "OperationsMetadata" );
    sectList.add( "Content" );
    sectList.add( "All" );
    getCapOpsElem.addContent( genParamElement( "Sections", sectList ) );

    // No constraints or metadata for this operation.
    // OperationsMetadata/Operation/Constraint (owcs) [0..*]
    // OperationsMetadata/Operation/Metadata (ows) [0..*]

    return getCapOpsElem;
  }

  private Element genDescCovOpsElement()
  {
    // OperationsMetadata/Operation (owcs) @name="DescribeCoverage"
    Element descCovOpsElem = new Element( "Operation", owcsNS );
    descCovOpsElem.setAttribute( "name", Request.Operation.DescribeCoverage.toString() );

    // Add DCP/HTTP/GET element with xlink to this server.
    // OperationsMetadata/Operation/DCP/HTTP/{GET|POST} (owcs) [1..*]
    //                       -  @type=simple, @xlink:title, @xlink:href
    descCovOpsElem.addContent(
            new Element( "DCP", owcsNS ).addContent(
                    new Element( "HTTP", owcsNS ).addContent(
                            new Element( "GET", owcsNS ).setAttribute(
                                    "href", serverURI.toString(), xlinkNS ) ) ) );

    // Add the "Service", "Version", "AcceptVersions", and "Sections" parameters.
    // OperationsMetadata/Operation/Parameter (owcs) [0..*]
    descCovOpsElem.addContent( genParamElement( "service", Collections.singletonList( "WCS" ) ) );
    descCovOpsElem.addContent( genParamElement( "version", Collections.singletonList( "1.1.0" ) ) );

    List<String> idList = new ArrayList<String>();
    for ( GridDataset.Gridset gs : this.dataset.getGridsets() )
    {
      idList.add( gs.getGeoCoordSystem().getName());
    }
    descCovOpsElem.addContent( genParamElement( "Identifier", idList ) );

    // No constraints or metadata for this operation.
    // OperationsMetadata/Operation/Constraint (owcs) [0..*]
    // OperationsMetadata/Operation/Metadata (ows) [0..*]

    return descCovOpsElem;
  }

  private Element genGetCovOpsElement()
  {
    // OperationsMetadata/Operation (owcs) @name="GetCoverage"
    Element getCovOpsElem = new Element( "Operation", owcsNS );
    getCovOpsElem.setAttribute( "name", Request.Operation.GetCoverage.toString() );

    // Add DCP/HTTP/GET element with xlink to this server.
    // OperationsMetadata/Operation/DCP/HTTP/{GET|POST} (owcs) [1..*]
    //                       -  @type=simple, @xlink:title, @xlink:href
    getCovOpsElem.addContent(
            new Element( "DCP", owcsNS ).addContent(
                    new Element( "HTTP", owcsNS ).addContent(
                            new Element( "GET", owcsNS ).setAttribute(
                                    "href", serverURI.toString(), xlinkNS ) ) ) );

    // Add the "Service", "Version", "AcceptVersions", and "Sections" parameters.
    // OperationsMetadata/Operation/Parameter (owcs) [0..*]
    getCovOpsElem.addContent( genParamElement( "service", Collections.singletonList( "WCS" ) ) );
    getCovOpsElem.addContent( genParamElement( "version", Collections.singletonList( "1.1.0" ) ) );
    getCovOpsElem.addContent( genParamElement( "store", Collections.singletonList( "False" ) ) );

    List<String> idList = new ArrayList<String>();
    for ( GridDataset.Gridset gs : this.dataset.getGridsets() )
    {
      idList.add( gs.getGeoCoordSystem().getName() );
    }
    getCovOpsElem.addContent( genParamElement( "Identifier", idList ) );

    // No constraints or metadata for this operation.
    // OperationsMetadata/Operation/Constraint (owcs) [0..*]
    // OperationsMetadata/Operation/Metadata (ows) [0..*]

    return getCovOpsElem;
  }

  private Element genParamElement( String name, List<String> allowedValues )
  {
    Element paramElem = new Element( "Parameter", owcsNS ).setAttribute( "name", name );
    Element allowedValuesElem = new Element( "AllowedValues", owcsNS );
    for ( String curVal : allowedValues )
      allowedValuesElem.addContent( new Element( "Value", owcsNS).addContent( curVal ) );

    return paramElem.addContent( allowedValuesElem);
  }

  public Element generateContents()
  {
    // Contents (wcs) [0..1]
    Element contentElem = new Element( "Contents", wcsNS );

    for ( GridDataset.Gridset gs : this.dataset.getGridsets())
    {
      // Contents/CoverageSummary (wcs) [0..1]
      //      [[NOTE(1): use unless info can be found in Contents/OtherSources.]]
      Element curCovSum = new Element( "CoverageSummary", wcsNS);

      // Contents/CoverageSummary/Title (ows) [0..1]
      curCovSum.addContent( new Element( "Title", owsNS).addContent( gs.getGeoCoordSystem().getName()));

      // Contents/CoverageSummary/Abstract (ows) [0..1]
      // Contents/CoverageSummary/Keywords (ows) [0..*]
      // Contents/CoverageSummary/Metadata/... (ows) [0..*]
      //     [[NOTE: Either xlink simple type or a concrete AbstractMetaData element.]]
      //     [[NOTE: We are going to support xlink simple type only but probably the TDS won't use this element.]]
      // Contents/CoverageSummary/WGS84BoundingBox/... (ows) [0..*]
      // Contents/CoverageSummary/SupportedCRS (ows) [0..*] - URI
      // Contents/CoverageSummary/SupportedFormats (ows) [0..*] - MIME type
      curCovSum.addContent( new Element( "SupportedFormats", owsNS).addContent("application/x-netcdf"));

      // ----
      //      [[NOTE: This coverage must contain lowerl-level coverages and/or an identifier.]]
      // Contents/CoverageSummary/CoverageSummary/... (wcs) [1..*]
      //      [[NOTE: Indicates that the parent coverage contains lower-level coverages.]]
      // Contents/CoverageSummary/Identifier (wcs) [0..1]
      //      [[NOTE: Indicates that this coverage can be accessed directly by GetCoverage and DescribeCoverage.]]
      //      [[NOTE: this ID must be unique to this WCS server.]]
      curCovSum.addContent( new Element( "Identifier", wcsNS).addContent( gs.getGeoCoordSystem().getName()));
      // ----

      contentElem.addContent( curCovSum);
    }
    // Contents/SupportedCRS (wcs) [0..*] - URI
    //      [[NOTE: union of all SupportedCRS from nested CoverageSummary-s]]
    // Contents/SupportedFormat (wcs) [0..*] - MIME type
    //      [[NOTE: union of all SupportedFormat from nested CoverageSummary-s]]
    // Contents/OtherSource (wcs) [0..*] - @type=simple, @xlink:title, @xlink:href
    //      [[NOTE(1): unless info can be found in sibling CoverageSummary elements.]]


    return contentElem;
  }

  /**
   * Contain the content needed for a GetCapabilities ServiceIdentification section.
   */
   public static class ServiceId
   {
     private String title, _abstract;
     private List<String> keywords;
     private String serviceType;
     private List<String> serviceTypeVersion;
     private String fees;
     private List<String> accessConstraints;

     public ServiceId( String title, String anAbstract, List<String> keywords, String serviceType, List<String> serviceTypeVersion, String fees, List<String> accessConstraints )
     {
       this.title = title;
       this._abstract = anAbstract;
       this.keywords = new ArrayList<String>( keywords);
       this.serviceType = serviceType;
       this.serviceTypeVersion = new ArrayList<String>( serviceTypeVersion );
       this.fees = fees;
       this.accessConstraints = new ArrayList<String>( accessConstraints );
     }

     public String getTitle() { return title; }
     public String getAbstract() { return _abstract; }
     public List<String> getKeywords() { return Collections.unmodifiableList( keywords); }
     public String getServiceType() { return serviceType; }
     public List<String> getServiceTypeVersion() { return Collections.unmodifiableList( serviceTypeVersion); }
     public String getFees() { return fees; }
     public List<String> getAccessConstraints() { return Collections.unmodifiableList( accessConstraints); }
   }

  /**
   * Contain content needed for a GetCapabilities ServiceProvider section.
   */
  public static class ServiceProvider
  {
    public ServiceProvider( String name, OnlineResource site, ServiceContact contact )
    {
      this.name = name;
      this.site = site;
      this.contact = contact;
    }

    public String getName(){ return name; }
    private String name;

    public OnlineResource getSite() { return site; }
    private OnlineResource site;

    public ServiceContact getContact() { return contact; }
    private ServiceContact contact;

    public static class OnlineResource
    {
      public OnlineResource( URI link, String title )
      {
        this.link = link;
        this.title = title;
      }

      public URI getLink() { return link; }
      private URI link;

      public String getTitle() { return title; }
      private String title;
    }
    public static class ServiceContact
    {
      public ServiceContact( String individualName, String positionName, ContactInfo contactInfo, String role )
      {
        this.individualName = individualName;
        this.positionName = positionName;
        this.contactInfo = contactInfo;
        this.role = role;
      }

      public String getIndividualName() { return individualName; }
      private String individualName;

      public String getPositionName() { return positionName; }
      private String positionName;

      public ContactInfo getContactInfo() { return contactInfo; }
      private ContactInfo contactInfo;

      public String getRole() { return role; }
      private String role;
    }
    public static class ContactInfo
    {
      public ContactInfo( List<String> voicePhone, List<String> faxPhone, Address address, OnlineResource onlineResource, String hoursOfService, String contactInstructions )
      {
        this.voicePhone = new ArrayList<String>( voicePhone);
        this.faxPhone = new ArrayList<String>( faxPhone);
        this.address = address;
        this.onlineResource = onlineResource;
        this.hoursOfService = hoursOfService;
        this.contactInstructions = contactInstructions;
      }

      public List<String> getVoicePhone() { return Collections.unmodifiableList( voicePhone); }
      private List<String> voicePhone;

      public List<String> getFaxPhone() { return Collections.unmodifiableList( faxPhone); }
      private List<String> faxPhone;

      public Address getAddress() { return address; }
      private Address address;

      public OnlineResource getOnlineResource() { return onlineResource; }
      private OnlineResource onlineResource;

      public String getHoursOfService() { return hoursOfService; }
      private String hoursOfService;

      public String getContactInstructions() { return contactInstructions; }
      private String contactInstructions;
    }
    public static class Address
    {
      public Address( List<String> deliveryPoint, String city, String adminArea, String postalCode, String country, List<String> email )
      {
        this.deliveryPoint = new ArrayList<String>( deliveryPoint);
        this.city = city;
        this.adminArea = adminArea;
        this.postalCode = postalCode;
        this.country = country;
        this.email = new ArrayList<String>( email);
      }

      public List<String> getDeliveryPoint() { return Collections.unmodifiableList( deliveryPoint); }
      private List<String> deliveryPoint;

      public String getCity() { return city; }
      private String city;

      public String getAdminArea() { return adminArea; }
      private String adminArea;

      public String getPostalCode() { return postalCode; }
      private String postalCode;

      public String getCountry() { return country; }
      private String country;

      public List<String> getEmail() { return Collections.unmodifiableList( email); }
      private List<String> email;
    }
  }

}
