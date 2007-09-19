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

  public enum Section
  {
    ServiceIdentification, ServiceProvider,
    OperationsMetadata, Contents, All
  }

  private URI serverURI;

  private List<Section> sections;
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
    capabilitiesElem.setAttribute( "version", "");           // ToDo
    capabilitiesElem.setAttribute( "updateSequence", "");    // ToDo

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
        Element provNameElem = new Element( "ProviderName", owsNS );
        provNameElem.addContent( serviceProvider.name );
        servProvElem.addContent( provNameElem );
      }

      if ( serviceProvider.site != null )
      {
        // ServiceProvider/ProviderSite (ows) [0..1]
        Element provSiteElem = new Element( "ProviderSite", owsNS );
        provSiteElem.setAttribute( "type", "simple" );
        if ( serviceProvider.site.title != null)
          provSiteElem.setAttribute( "xlink:title", serviceProvider.site.title );
        if ( serviceProvider.site.link != null )
          provSiteElem.setAttribute( "xlink:href", serviceProvider.site.link.toString() );
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
              onlineResourceElem.setAttribute( "xlink:title", serviceProvider.contact.contactInfo.onlineResource.title );
            if ( serviceProvider.contact.contactInfo.onlineResource.link != null )
              onlineResourceElem.setAttribute( "xlink:href", serviceProvider.contact.contactInfo.onlineResource.link.toString() );

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

    return opsMetadataElem;
  }

  public Element generateContents()
  {
    // Contents (wcs) [0..1]
    Element contentElem = new Element( "Contents", wcsNS );

    for ( GridDataset.Gridset gs : this.dataset.getGridsets())
    {
      Element curCovSum = new Element( "CoverageSummary", wcsNS);
      // Contents/CoverageSummary (wcs) [0..1]
      //      [[NOTE(1): use unless info can be found in Contents/OtherSources.]]
      // Contents/CoverageSummary/Title (ows) [0..1]
      // Contents/CoverageSummary/Abstract (ows) [0..1]
      // Contents/CoverageSummary/Keywords (ows) [0..*]
      // Contents/CoverageSummary/Metadata/... (ows) [0..*]
      //     [[NOTE: Either xlink simple type or a concrete AbstractMetaData element.]]
      //     [[NOTE: We are going to support xlink simple type only but probably the TDS won't use this element.]]
      // Contents/CoverageSummary/WGS84BoundingBox/... (ows) [0..*]
      // Contents/CoverageSummary/SupportedCRS (ows) [0..*] - URI
      // Contents/CoverageSummary/SupportedFormatS (ows) [0..*] - MIME type
      // ----
      //      [[NOTE: This coverage must contain lowerl-level coverages and/or an identifier.]]
      // Contents/CoverageSummary/CoverageSummary/... (wcs) [1..*]
      //      [[NOTE: Indicates that the parent coverage contains lower-level coverages.]]
      // Contents/CoverageSummary/Identifier (wcs) [0..1]
      //      [[NOTE: Indicates that this coverage can be accessed directly by GetCoverage and DescribeCoverage.]]
      //      [[NOTE: this ID must be unique to this WCS server.]]
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
       this.keywords = new ArrayList<String>(keywords.size());
       Collections.copy( this.keywords, keywords);
       this.serviceType = serviceType;
       this.serviceTypeVersion = new ArrayList<String>( serviceTypeVersion.size() );
       Collections.copy( this.serviceTypeVersion, serviceTypeVersion);
       this.fees = fees;
       this.accessConstraints = new ArrayList<String>( accessConstraints.size() );
       Collections.copy(this.accessConstraints, accessConstraints);
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
    public String name;
    public OnlineResource site;
    public ServiceContact contact;

    public class OnlineResource
    {
      public URI link;
      public String title;
    }
    public class ServiceContact
    {
      public String individualName;
      public String positionName;
      public ContactInfo contactInfo;
      public String role;
    }
    public class ContactInfo
    {
      public List<String> voicePhone;
      public List<String> faxPhone;
      public Address address;
      public OnlineResource onlineResource;
      public String hoursOfService;
      public String contactInstructions;
    }
    public class Address
    {
      public List<String> deliveryPoint;
      public String city;
      public String adminArea;
      public String postalCode;
      public String country;
      public List<String> email;
    }
  }

}
