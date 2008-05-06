// $Id: ResultService.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

import thredds.catalog.*;

import java.util.Iterator;

/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 * @author Ethan Davis
 * @version 1.0
 */

public class ResultService extends InvService
{
  //
  private String accessPointHeader = null;

  private boolean isValid = true;
  private StringBuffer log = new StringBuffer();

  /**
   * Constructor
   *
   * @param name
   * @param serviceType
   * @param base
   * @param suffix
   * @param accessPointHeader
   */
  public ResultService( String name, ServiceType serviceType,
                        String base, String suffix,
                        String accessPointHeader)
  {
    super( name, serviceType.toString(), base, suffix, null);
    this.accessPointHeader = accessPointHeader;
  }

  /**
   * Copy constructor.
   * @param service
   */
  protected ResultService( ResultService service)
  {
    this( service, service.getAccessPointHeader());
  }

  public ResultService( InvService service, String accessPointHeader)
  {
    super( service.getName(), service.getServiceType().toString(), service.getBase(),
           service.getSuffix(), service.getDescription());

    for ( Iterator it = service.getProperties().iterator(); it.hasNext(); )
    {
      InvProperty prop = (InvProperty) it.next();
      addProperty( new InvProperty( prop.getName(), prop.getValue())); // LOOK JC changed
    }

    if ( service.getServiceType() == ServiceType.COMPOUND)
    {
      for ( Iterator it = service.getServices().iterator(); it.hasNext(); )
      {
        addService( (InvService) it.next() ); // LOOK JC changed
      }
    }
    this.accessPointHeader = accessPointHeader;

  }

  /**
   * Return a String containing the accessPointHeader attribute text
   *
   * @return String accessPointHeader attribute text
   */
  public String getAccessPointHeader() { return( this.accessPointHeader); }

  public void setAccessPointHeader( String accessPointHeader)
  {
    this.accessPointHeader = accessPointHeader;
  }

  /**
   * Validate this ResultService object. Return true if valid, false if invalid.
   *
   * @param out StringBuffer with validation messages.
   * @return boolean true if valid, false if invalid
   */
  protected boolean validate( StringBuilder out)
  {
    this.isValid = true;

    // If log from construction has content, append to validation output msg.
    if (this.log.length() > 0) {
      out.append( this.log);
    }

    // Check that 'accessPointHeader' attribute is not null.
    if ( this.getAccessPointHeader() == null)
    {
      this.isValid = false;
      out.append( " ** ResultService (1): a null 'accessPointHeader' is invalid.");
    }

    return( this.isValid);
  }

  /** string representation */
  public String toString()
  {
    return( "ResultService[" + super.toString() +
            " accessPointHeader:<" + this.getAccessPointHeader() + ">]");
  }
}
