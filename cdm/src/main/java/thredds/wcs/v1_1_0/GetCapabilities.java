package thredds.wcs.v1_1_0;

import ucar.nc2.dt.GridDataset;

import java.util.List;
import java.util.ArrayList;
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

  private String serviceIdTitle;
  private String serviceIdAbstract;
  private List<String> serviceIdKeywords;
  private String serviceType;
  private List<String> serviceTypeVersion;
  private String fees;
  private List<String> accessConstraints;

  public GetCapabilities()
  {
    serviceIdTitle = "need a title";
    serviceIdAbstract = "need an abstract";
    serviceIdKeywords = new ArrayList<String>();
    serviceIdKeywords.add( "need keywords");
    serviceType = "OGC WCS";
    serviceTypeVersion = new ArrayList<String>();
    serviceTypeVersion.add("1.1.0");
    fees = "NONE";
    accessConstraints = new ArrayList<String>();
    accessConstraints.add( "NONE");
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

  public enum Section
  {
    ServiceIdentification, ServiceProvider, OperationsMetadata, Contents,
    All
  }

  public String junk()
  {
    return Section.valueOf( "ServiceIdentification").toString();
  }

  public Element generateServiceIdentification( String title, String abs,
                                                List<String> keywords,
                                                String serviceType,
                                                List<String> serviceTypeVersion,
                                                String fees,
                                                List<String> accessConstraints )
  {
    Element serviceIdElem = new Element( "ServiceIdentification", owcsNS );

    if ( title != null )
    {
      Element titleElem = new Element( "Title", owsNS );
      titleElem.addContent( title );
      serviceIdElem.addContent( titleElem );
    }

    if ( abs != null )
    {
      Element abstractElem = new Element( "Abstract", owsNS );
      abstractElem.addContent( abs );
      serviceIdElem.addContent( abstractElem );
    }

    if ( keywords != null && keywords.size() > 0 )
    {
      Element keywordsElem = new Element( "Keywords", owsNS );
      for ( String curKey : keywords )
      {
        Element keywordElem = new Element( "Keyword", owsNS );
        keywordElem.addContent( curKey );
        keywordsElem.addContent( keywordElem );
      }
      serviceIdElem.addContent( keywordsElem );
    }

    if ( serviceType != null )
    {
      Element serviceTypeElem = new Element( "ServiceType", owcsNS );
      serviceTypeElem.addContent( serviceType );
      serviceIdElem.addContent( serviceTypeElem );
    }

    if ( serviceTypeVersion != null && serviceTypeVersion.size() > 0 )
    {
      for ( String curVer : serviceTypeVersion )
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

    if ( fees != null )
    {
      Element feesElem = new Element( "Fees", owcsNS );
      feesElem.addContent( fees );
      serviceIdElem.addContent( feesElem );
    }

    if ( accessConstraints != null && accessConstraints.size() > 0 )
    {
      for ( String curAC : accessConstraints )
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

   public class ServiceId
   {
     private String title, _abstract;
     private List<String> keywords;
     private String serviceType;
     private List<String> serviceTypeVersion;
     private String fees;
     private List<String> accessConstraints;

     public ServiceId( String title, String abst, List<String> keywords,
                       String serviceType, List<String> serviceTypeVersion,
                       String fees, List<String> accessConstraints )
     {

     }
   }
}
