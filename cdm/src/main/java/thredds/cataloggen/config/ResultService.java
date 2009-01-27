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
