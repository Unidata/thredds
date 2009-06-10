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
package thredds.wcs.v1_0_0_1;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.net.URI;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;

import thredds.wcs.Request;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class GetCapabilities extends WcsRequest
{
//    private static org.slf4j.Logger log =
//            org.slf4j.LoggerFactory.getLogger( GetCapabilities.class );

  public enum Section
  {
    All( ""),
    Service( "WCS_Capabilities/Service"),
    Capability( "WCS_Capabilities/Capability" ),
    ContentMetadata( "WCS_Capabilities/ContentMetadata" );

    private final String altId;
    Section( String altId) { this.altId = altId; }
    public String toString() { return altId; }

    public static Section getSection( String altId)
    {
      for ( Section curSection : Section.values())
      {
        if ( curSection.altId.equals( altId))
          return curSection;
      }
      throw new IllegalArgumentException( "No such instance [" + altId + "].");
    }
  }

  private URI serverURI;

  private Section section;

  private ServiceInfo serviceInfo;

  private String updateSequence;

  private Document capabilitiesReport;

  public GetCapabilities( Request.Operation operation, String version, WcsDataset dataset,
                          URI serverURI, Section section, String updateSequence,
                          ServiceInfo serviceInfo )
  {
    super( operation, version, dataset);
    this.serverURI = serverURI;
    this.section = section;
    this.serviceInfo = serviceInfo;

    this.updateSequence = updateSequence;

    if ( this.serverURI == null )
      throw new IllegalArgumentException( "Null server URI not allowed.");
    if ( this.section == null )
      throw new IllegalArgumentException( "Null section not allowed.");
  }

  String getCurrentUpdateSequence()
  {
    // ToDo If decide to support updateSequence, need to
    // ToDo     1) update getCurrentUpdateSequence() and
    // ToDo     2) update logic to handle exceptions appropriately.
    if (updateSequence == null)
      return null;
    return null;
  }

  public Document getCapabilitiesReport()
          throws WcsException
  {
    if ( this.capabilitiesReport == null )
      capabilitiesReport = generateCapabilities();
    return capabilitiesReport;
  }

  public void writeCapabilitiesReport( PrintWriter pw )
          throws WcsException, IOException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    xmlOutputter.output( getCapabilitiesReport(), pw );
  }

  public String writeCapabilitiesReportAsString()
          throws WcsException
  {
    XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
    return xmlOutputter.outputString( getCapabilitiesReport() );
  }

  Document generateCapabilities()
          throws WcsException
  {
    Element rootElem;

    if ( section.equals( Section.All))
    {
      rootElem = new Element( "WCS_Capabilities", wcsNS );

      rootElem.addContent( generateServiceSection( this.serviceInfo ) );
      rootElem.addContent( generateCapabilitySection() );
      rootElem.addContent( generateContentMetadataSection() );
    }
    else if ( section.equals( Section.Service))
    {
      rootElem = generateServiceSection( this.serviceInfo );
    }
    else if ( section.equals( Section.Capability))
    {
      rootElem = generateCapabilitySection();
    }
    else if ( section.equals( Section.ContentMetadata))
    {
      rootElem = generateContentMetadataSection();
    }
    else
    {
      throw new WcsException();
    }

    rootElem.addNamespaceDeclaration( gmlNS );
    rootElem.addNamespaceDeclaration( xlinkNS );
    rootElem.setAttribute( "version", this.getVersion() );

    // ToDo If decide to support updateSequence, need to
    // ToDo     1) update getCurrentUpdateSequence() and
    // ToDo     2) update logic to handle exceptions appropriately.
    if ( this.getCurrentUpdateSequence() != null )
      rootElem.setAttribute( "updateSequence", this.getCurrentUpdateSequence());

    return new Document( rootElem );
  }

  public Element generateServiceSection( ServiceInfo serviceInfo )
  {
    // WCS_Capabilities/Service
    Element serviceElem = new Element( "Service", wcsNS );

    if ( serviceInfo != null )
    {
      // WCS_Capabilities/Service/gml:metaDataProperty [0..*]
      // WCS_Capabilities/Service/gml:description [0..1]
      // WCS_Capabilities/Service/gml:name [0..*]
      // WCS_Capabilities/Service/metadataLink [0..*]
      // WCS_Capabilities/Service/description [0..1]
      if ( serviceInfo.getDescription() != null )
        serviceElem.addContent( new Element( "description", wcsNS ).addContent( serviceInfo.getDescription() ) );

      // WCS_Capabilities/Service/name
      if ( serviceInfo.getName() != null )
        serviceElem.addContent( new Element( "name", wcsNS ).addContent( serviceInfo.getName() ) );

      // WCS_Capabilities/Service/label (string)
      if ( serviceInfo.getLabel() != null )
        serviceElem.addContent( new Element( "label", wcsNS ).addContent( serviceInfo.getLabel() ) );

      // WCS_Capabilities/Service/keywords [0..*](string)
      // WCS_Capabilities/Service/keywords/keyword [1..*](string)
      // WCS_Capabilities/Service/keywords/type [0..1](string)
      // WCS_Capabilities/Service/keywords/type@codeSpace [0..1](URI)
      if ( serviceInfo.getKeywords() != null &&
           serviceInfo.getKeywords().size() > 0 )
      {
        Element keywordsElem = new Element( "keywords", wcsNS );
        for ( String curKey : serviceInfo.getKeywords() )
        {
          keywordsElem.addContent( new Element( "keyword", wcsNS ).addContent( curKey ) );
        }
        serviceElem.addContent( keywordsElem );
      }

      ResponsibleParty respParty = serviceInfo.getResponsibleParty();
      if ( respParty != null )
      {
        // WCS_Capabilities/Service/responsibleParty [0..1](string)
        Element respPartyElem = new Element( "responsibleParty", wcsNS );

        //-----
        // WCS_Capabilities/Service/responsibleParty/individualName [1](string)
        //   AND/OR
        // WCS_Capabilities/Service/responsibleParty/organisationName [1](string)
        //-----
        if (respParty.getIndividualName() != null )
          respPartyElem.addContent( new Element( "individualName", wcsNS).addContent( respParty.getIndividualName()));
        if (respParty.getOrganizationName() != null )
          respPartyElem.addContent( new Element( "organisationName", wcsNS).addContent( respParty.getOrganizationName()));

        // WCS_Capabilities/Service/responsibleParty/positionName [0..1](string)
        if (respParty.getPositionName() != null )
          respPartyElem.addContent( new Element( "positionName", wcsNS).addContent( respParty.getPositionName()));

        // WCS_Capabilities/Service/responsibleParty/contactInfo [0..1]
        if ( respParty.getContact() != null )
        {
          Element contactElem = new Element( "contactInfo", wcsNS);

          // WCS_Capabilities/Service/responsibleParty/contactInfo/phone/{voice|facsimile} [0..1] (string)
          Element phoneElem = new Element( "phone", wcsNS );
          if ( respParty.getContact().getVoicePhone() != null )
            for ( String curVoicePhone : respParty.getContact().getVoicePhone() )
              phoneElem.addContent( new Element( "voice", wcsNS ).addContent( curVoicePhone ) );
          if ( respParty.getContact().getFaxPhone() != null )
            for ( String curFaxPhone : respParty.getContact().getFaxPhone() )
              phoneElem.addContent( new Element( "facsimile", wcsNS ).addContent( curFaxPhone ) );

          if ( phoneElem.getContentSize() > 0 )
            contactElem.addContent( phoneElem);

          // WCS_Capabilities/Service/responsibleParty/contactInfo/address [0..1]
          ResponsibleParty.Address contactAddress = respParty.getContact().getAddress();
          if ( contactAddress != null )
          {
            Element addressElem = new Element( "address", wcsNS );
            if ( contactAddress.getDeliveryPoint() != null )
            {
              for ( String curDP : contactAddress.getDeliveryPoint() )
              {
                // WCS_Capabilities/Service/responsibleParty/contactInfo/address/deliveryPoint [0..*]
                addressElem.addContent( new Element( "deliveryPoint", wcsNS ).addContent( curDP ) );
              }
            }
            if ( contactAddress.getCity() != null )
            {
              // WCS_Capabilities/Service/responsibleParty/contactInfo/address/city [0..1]
              addressElem.addContent( new Element( "city", wcsNS ).addContent( contactAddress.getCity() ) );
            }
            if ( contactAddress.getAdminArea() != null )
            {
              // WCS_Capabilities/Service/responsibleParty/contactInfo/address/administrativeArea [0..1]
              addressElem.addContent( new Element( "administrativeArea", wcsNS )
                      .addContent( contactAddress.getAdminArea() ) );
            }
            if ( contactAddress.getPostalCode() != null )
            {
              // WCS_Capabilities/Service/responsibleParty/contactInfo/address/postalCode [0..1]
              addressElem.addContent( new Element( "postalCode", wcsNS )
                      .addContent( contactAddress.getPostalCode() ) );
            }
            if ( contactAddress.getCountry() != null )
            {
              // WCS_Capabilities/Service/responsibleParty/contactInfo/address/country [0..1]
              addressElem.addContent( new Element( "country", wcsNS )
                      .addContent( contactAddress.getCountry() ) );
            }
            if ( contactAddress.getEmail() != null )
            {
              for ( String curEmail : contactAddress.getEmail() )
              {
                // WCS_Capabilities/Service/responsibleParty/contactInfo/address/electronicMailAddress [0..*]
                addressElem.addContent( new Element( "electronicMailAddress", wcsNS )
                        .addContent( curEmail ) );
              }
            }

            contactElem.addContent( addressElem );
          }

          // WCS_Capabilities/Service/responsibleParty/contactInfo/onlineResource@{xlink:href|xlink:title} [0..1]
          ResponsibleParty.OnlineResource onlineRes = respParty.getContact().getOnlineResource();
          if ( onlineRes != null )
          {
            Element onlineResElem = new Element( "onlineResource", wcsNS);
            onlineResElem.setAttribute( "type", "simple" );
            if ( onlineRes.getTitle() != null )
              onlineResElem.setAttribute( "title", onlineRes.getTitle(), xlinkNS );
            if ( onlineRes.getLink() != null )
              onlineResElem.setAttribute( "href", onlineRes.getLink().toString(), xlinkNS );

            contactElem.addContent( onlineResElem);
          }

          respPartyElem.addContent( contactElem);
        }

        serviceElem.addContent( respPartyElem );
      }
    }

    // WCS_Capabilities/Service/fees [1] ("NONE")
    serviceElem.addContent( new Element( "fees", wcsNS ).addContent( "NONE"));

    // WCS_Capabilities/Service/accessConstraints [1..*] ("NONE")
    serviceElem.addContent( new Element( "accessConstraints", wcsNS ).addContent( "NONE" ) );

    return serviceElem;
  }

  public Element generateCapabilitySection()
  {
    // WCS_Capabilities/Capability
    Element capElem = new Element( "Capability", wcsNS );

    // WCS_Capabilities/Capability/
    Element requestElem = new Element( "Request", wcsNS );

    requestElem.addContent( genCapabilityOperationElem( Request.Operation.GetCapabilities.toString() ));
    requestElem.addContent( genCapabilityOperationElem( Request.Operation.DescribeCoverage.toString() ));
    requestElem.addContent( genCapabilityOperationElem( Request.Operation.GetCoverage.toString() ));

    capElem.addContent( requestElem);

    capElem.addContent(
            new Element( "Exception", wcsNS ).addContent(
                    new Element( "Format", wcsNS).addContent( "application/vnd.ogc.se_xml")));

    return capElem;
  }

  private Element genCapabilityOperationElem( String operationAsString )
  {
    Element getCapOpsElem;
    getCapOpsElem= new Element( operationAsString, wcsNS );
    getCapOpsElem.addContent(
            new Element( "DCPType", wcsNS ).addContent(
                    new Element( "HTTP", wcsNS ).addContent(
                            new Element( "Get", wcsNS ).addContent(
                                    new Element( "OnlineResource", wcsNS ).setAttribute(
                                            "href", serverURI.toString(), xlinkNS ) ) ) ) );
    return getCapOpsElem;
  }

  public Element generateContentMetadataSection()
  {
    // WCS_Capabilities/ContentMetadata
    Element contMdElem = new Element( "ContentMetadata", wcsNS );

    // ToDo WCS 1.0Plus - change GridDatatype to GridDataset.Gridset

    for ( WcsCoverage curCoverage : this.getDataset().getAvailableCoverageCollection())
      // WCS_Capabilities/ContentMetadata/CoverageOfferingBrief
      // WCS_Capabilities/ContentMetadata/CoverageOfferingBrief
      contMdElem.addContent(
              genCoverageOfferingBriefElem( "CoverageOfferingBrief",
                                            curCoverage.getName(),
                                            curCoverage.getLabel(),
                                            curCoverage.getDescription(),
                                            curCoverage.getCoordinateSystem() ) );

    return contMdElem;
  }

  /**
   * Contain the content needed for a WCS_Capabilities/Service section.
   */
   public static class ServiceInfo
  {
     private String name, label, description;
     private List<String> keywords;
     private ResponsibleParty responsibleParty;
     private String fees;
     private List<String> accessConstraints;

     public ServiceInfo( String name, String label, String description,
                       List<String> keywords, ResponsibleParty responsibleParty,
                       String fees, List<String> accessConstraints )
     {
       this.name = name;
       this.label = label;
       this.description = description;
       this.keywords = new ArrayList<String>( keywords);
       this.responsibleParty = responsibleParty;

       this.fees = fees;
       this.accessConstraints = new ArrayList<String>( accessConstraints );
     }

     public String getName() { return name; }
     public String getLabel() { return label; }
     public String getDescription() { return description; }
     public List<String> getKeywords() { return Collections.unmodifiableList( keywords); }
     public ResponsibleParty getResponsibleParty() { return responsibleParty; }

     public String getFees() { return fees; }
     public List<String> getAccessConstraints() { return Collections.unmodifiableList( accessConstraints); }
   }

  /**
   * Contain content needed for a GetCapabilities ServiceProvider section.
   */
  public static class ResponsibleParty
  {
    public ResponsibleParty( String individualName, String organizationName, String positionName,
                             ContactInfo contactInfo )
    {
      this.individualName = individualName;
      this.organizationName = organizationName;
      this.positionName = positionName;
      this.contactInfo = contactInfo;
    }

    public String getIndividualName() { return individualName; }
    private String individualName;

    public String getOrganizationName() { return organizationName; }
    private String organizationName;

    public String getPositionName() { return positionName; }
    private String positionName;

    public ContactInfo getContact() { return contactInfo; }
    private ContactInfo contactInfo;

    public static class ContactInfo
    {
      public ContactInfo( List<String> voicePhone, List<String> faxPhone,
                          Address address, OnlineResource onlineResource )
      {
        this.voicePhone = new ArrayList<String>( voicePhone);
        this.faxPhone = new ArrayList<String>( faxPhone);
        this.address = address;
        this.onlineResource = onlineResource;
      }

      public List<String> getVoicePhone() { return Collections.unmodifiableList( voicePhone); }
      private List<String> voicePhone;

      public List<String> getFaxPhone() { return Collections.unmodifiableList( faxPhone); }
      private List<String> faxPhone;

      public Address getAddress() { return address; }
      private Address address;

      public OnlineResource getOnlineResource() { return onlineResource; }
      private OnlineResource onlineResource;
    }
    public static class Address
    {
      public Address( List<String> deliveryPoint, String city,
                      String adminArea, String postalCode, String country,
                      List<String> email )
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
  }

}
