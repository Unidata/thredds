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
//    serviceIdTitle = "need a title";
//    serviceIdAbstract = "need an abstract";
//    serviceIdKeywords = new ArrayList<String>();
//    serviceIdKeywords.add( "need keywords");
//    serviceType = "OGC WCS";
//    serviceTypeVersion = new ArrayList<String>();
//    serviceTypeVersion.add("1.1.0");
//    fees = "NONE";
//    accessConstraints = new ArrayList<String>();
//    accessConstraints.add( "NONE");
  }

  public Document getCapabilitiesReport() { return capabilitiesReport; }

  public void writeCapabilitiesReport( PrintWriter pw )
          throws IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( Format.getPrettyFormat() );
    xmlOutputter.output( capabilitiesReport, pw );
  }

  public Document generateCapabilities()
  {
    Element capabilitiesElem = new Element( "Capabilities", wcsNS );
    capabilitiesElem.addNamespaceDeclaration( owcsNS );
    capabilitiesElem.addNamespaceDeclaration( owsNS );

    boolean allSections = false;
    if ( sections == null || sections.size() == 0 ||
         ( sections.size() == 1 && sections.get( 0 ).equals( Section.All ) ) )
    {
      allSections = true;
    }

    if ( sections.contains( Section.ServiceIdentification))
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
      capabilitiesElem.addContent( generateContent());
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
//      Element profileElem = new Element( "Profile", owcsNS);
//      profileElem.addContent( curAppProfileId.toString());
//      serviceIdElem.addContent( profileElem);
//    }
      // "WCS 1.1 Application Profile for [Format] [formatVersion] encoding, [profileVersion]"
      // "WCS 1.1 Application Profile for CF/1.0 netCDF 3 encoding, [profileVersion]"
      // "WCS 1.1 Application Profile for CF-netCDF 1.0/3 encoding, [profileVersion]"

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
    // ServiceProvider [0..1]
    Element servProvElem = new Element( "ServiceProvider", owsNS );

    if ( serviceProvider != null )
    {
      if ( serviceProvider.name != null )
      {
        // ServiceProvider/ProviderName [0..1]
        Element provNameElem = new Element( "ProviderName", owsNS );
        provNameElem.addContent( serviceProvider.name );
        servProvElem.addContent( provNameElem );
      }

      if ( serviceProvider.site != null )
      {
        // ServiceProvider/ProviderSite [0..1]
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
        // ServiceProvider/ServiceContact [0..1]
        Element servContactElem = new Element( "ServiceContact", owsNS );

        if ( serviceProvider.contact.individualName != null )
        {
          // ServiceProvider/ServiceContact/IndividualName [0..1]
          Element individualNameElem = new Element( "IndividualName", owsNS);
          individualNameElem.addContent( serviceProvider.contact.individualName);
          servContactElem.addContent( individualNameElem);
        }

        if ( serviceProvider.contact.positionName != null )
        {
          // ServiceProvider/ServiceContact/PositionName [0..1]
          Element positionNameElem = new Element( "PositionName", owsNS);
          positionNameElem.addContent( serviceProvider.contact.positionName );
          servContactElem.addContent( positionNameElem );
        }

        if ( serviceProvider.contact.contactInfo != null )
        {
          // ServiceProvider/ServiceContact/ContactInfo [0..1]
          Element contactInfoElem = new Element( "ContactInfo", owsNS);
          if ( serviceProvider.contact.contactInfo.voicePhone != null ||
                  serviceProvider.contact.contactInfo.faxPhone != null )
          {
            // ServiceProvider/ServiceContact/ContactInfo/Phone [0..1]
            Element phoneElem = new Element( "Phone", owsNS);
            if ( serviceProvider.contact.contactInfo.voicePhone != null )
              for (String curPhone : serviceProvider.contact.contactInfo.voicePhone)
                // ServiceProvider/ServiceContact/ContactInfo/Phone/Voice [0..*]
                phoneElem.addContent( new Element( "Voice", owsNS ).addContent( curPhone ));
            if ( serviceProvider.contact.contactInfo.faxPhone != null )
              for (String curPhone : serviceProvider.contact.contactInfo.faxPhone)
                // ServiceProvider/ServiceContact/ContactInfo/Phone/Facsimile [0..*]
                phoneElem.addContent( new Element( "Facsimile", owsNS ).addContent( curPhone ));
            contactInfoElem.addContent( phoneElem);
          }

          if ( serviceProvider.contact.contactInfo.address != null )
          {
            // ServiceProvider/ServiceContact/ContactInfo/Address/DeliveryPoint [0..1]
            // ServiceProvider/ServiceContact/ContactInfo/Address/City [0..1]
            // ServiceProvider/ServiceContact/ContactInfo/Address/AdministrativeArea [0..1]
            // ServiceProvider/ServiceContact/ContactInfo/Address/PostalCode [0..1]
            // ServiceProvider/ServiceContact/ContactInfo/Address/Country [0..1]
            // ServiceProvider/ServiceContact/ContactInfo/Address/ElectronicMailAddress [0..*]

          }

          // ServiceProvider/ServiceContact/ContactInfo/OnlineResource [0..1]
          // ServiceProvider/ServiceContact/ContactInfo/HoursOfService [0..1]
          // ServiceProvider/ServiceContact/ContactInfo/ContactInstructions [0..1]
          servContactElem.addContent( contactInfoElem);
        }

        if ( serviceProvider.contact.role != null )
        {
          // ServiceProvider/ServiceContact/Role [0..1]
          Element roleElem = new Element( "Role", owsNS );
          roleElem.addContent( serviceProvider.contact.role);
          servContactElem.addContent( roleElem);
        }

        servProvElem.addContent( servContactElem );
      }
    }
    return servProvElem;
  }

  public Element generateOperationsMetadata()
  {
    Element opsMetadataElem = new Element( "OperationsMetadata", owsNS );

    return opsMetadataElem;
  }

  public Element generateContent()
  {
    Element contentElem = new Element( "Content", owsNS );

    return contentElem;
  }

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
      public String deliveryPoint;
      public String city;
      public String AdminArea;
      public String postalCode;
      public String country;
      public List<String> email;
    }
  }

}
