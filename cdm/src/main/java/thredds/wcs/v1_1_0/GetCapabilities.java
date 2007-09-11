package thredds.wcs.v1_1_0;

import ucar.nc2.dt.GridDataset;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URI;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

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

  private ServiceId serviceId;
  private GridDataset dataset;

  private Document capabilitiesReport;

  public GetCapabilities( ServiceId serviceId, GridDataset dataset )
  {
    this.serviceId = serviceId;
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

  public Document generateCapabilities( URI serverURI, GridDataset gds, List<Section> sections )
  {
    Element capabilitiesElem = new Element( "Capabilities", wcsNS );
    capabilitiesElem.addNamespaceDeclaration( owcsNS );
    capabilitiesElem.addNamespaceDeclaration( owsNS );

    if ( sections == null || sections.size() == 0 ||
         ( sections.size() == 1 && sections.get( 0 ).equals( Section.All ) ) )
    {

    }
    else
    {
//      if ( sections.contains( Section.ServiceIdentification))
//      {
//        capabilitiesElem.addContent( generateServiceIdentification( "need a title", "need an abstract",  ));
//      }
      if ( sections.contains( Section.ServiceProvider))
      {
        capabilitiesElem.addContent( generateServiceProvider("") );
      }
      if ( sections.contains( Section.OperationsMetadata ))
      {
        capabilitiesElem.addContent( generateOperationsMetadata());
      }
      if (sections.contains( Section.Contents))
      {
        capabilitiesElem.addContent( generateContent());
      }
    }

    return new Document( capabilitiesElem );
  }

  public Element generateServiceIdentification( ServiceId serviceId )
  {
    Element serviceIdElem = new Element( "ServiceIdentification", owcsNS );

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

    return serviceIdElem;
  }

  public Element generateServiceProvider( String providerName )
  {
    Element servProvElem = new Element( "ServiceProvider", owsNS );

    Element provNameElem = new Element( "ProviderName", owsNS );
    provNameElem.addContent( providerName );
    servProvElem.addContent( provNameElem );

    Element provSiteElem = new Element( "ProviderSite", owsNS );
    provSiteElem.setAttribute( "type", "simple" );
    provSiteElem.setAttribute( "xlink:title", "" );
    provSiteElem.setAttribute( "xlink:href", "" );
    servProvElem.addContent( provSiteElem );

    Element servContactElem = new Element( "ServiceContact", owsNS );
    // ...
    // IndividualName [0..1]
    // PositionName [0..1]
    // ContactInfo {0..1]
    //   Phone [0..1]
    //   Address [0..1]
    //   OnlineResource [0..1]
    //   HoursOfService [0..1]
    //   ContactInstructions [0..1]
    // Role [0..1]
    // ...
    servProvElem.addContent( servContactElem );

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
}
