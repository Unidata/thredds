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
package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.List;

/**
 * Represents a data access service and allows basic data access information
 * to be factored out of dataset Access objects.
 *
 * @author edavis
 * @since 4.0
 */
public interface Service
{
  /**
   * Returns the name of this Service, may not be null.
   *
   * @return the name of this Service, never null.
   */
  public String getName();

  /**
   * Returns a human-readable description of this Service.
   *
   * @return a human-readable description of this service.
   */
  public String getDescription();

  /**
   * Return the ServiceType for this Service, may not be null.
   *
   * @return the ServiceType for this Service, never null.
   */
  public ServiceType getType();

  /**
   * Return the base URI for this Service, may not be null.
   *
   * @return the base URI for this Service, never null.
   */
  public URI getBaseUri();

  /**
   * Return the suffix for this Service. The suffix will not be null
   * but may be an empty string.
   *
   * @return the suffix for this Service.
   */
  public String getSuffix();

  /**
   * Return the List of Property objects associated with this Service.
   *
   * @return the List of Property objects associated with this Service, may be an empty list but not null.
   */
  public List<Property> getProperties();

  public Property getPropertyByName( String name );

  /**
   * Return the List of Service Objects nested in this service. Nested
   * services are only allowed when this service has a "Compound" ServiceType.
   *
   * @return the List of Service Objects nested in this service, may be an empty list but not null.
   */
  public List<Service> getServices();

  public Service getServiceByName( String name );
  public Service findServiceByNameGlobally( String name );
}
