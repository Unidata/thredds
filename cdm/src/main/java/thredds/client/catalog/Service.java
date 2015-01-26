/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog;

import net.jcip.annotations.Immutable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *  Client catalog service
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Service {            // (7)
  private final String name;
  private final String base;
  private final String typeS;
  private final String desc;
  private final String suffix;
  private final List<Service> nestedServices;
  private final List<Property> properties;

  public Service(String name, String base, String typeS, String desc, String suffix, List<Service> nestedServices, List<Property> properties) {
    this.name = name;
    this.base = base;
    this.typeS = typeS;
    this.desc = desc;
    this.suffix = suffix;
    this.nestedServices = nestedServices;
    this.properties = properties;
  }

  public String getName() {
    return name;
  }

  public String getBase() {
    return base;
  }

  public String getServiceTypeName() {
    return typeS;
  }
  public ServiceType getType() {
    return ServiceType.getServiceTypeIgnoreCase(typeS);
  }

  public String getDesc() {
    return desc;
  }

  public String getSuffix() {
    return suffix == null ? "" : suffix;
  }

  public List<Service> getNestedServices() {
    return nestedServices == null ? new ArrayList<Service>(0) : nestedServices;
  }

  public List<Property> getProperties() {
    return properties == null ? new ArrayList<Property>(0) : properties;
  }

  /**
   * See if the service Base is reletive
   * @return true if the service Base is reletive
   */
  public boolean isRelativeBase() {
    if (getType() == ServiceType.Compound)
      return true;

    try {
      URI uri = new java.net.URI(base);
      return !uri.isAbsolute();
    } catch (java.net.URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  @Override
  public String toString() {
    return "Service{" +
            "name='" + name + '\'' +
            ", base='" + base + '\'' +
            ", typeS='" + typeS + '\'' +
            ", desc='" + desc + '\'' +
            ", suffix='" + suffix + '\'' +
            ", nestedServices=" + nestedServices +
            ", properties=" + properties +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Service service = (Service) o;

    if (base != null ? !base.equals(service.base) : service.base != null) return false;
    if (desc != null ? !desc.equals(service.desc) : service.desc != null) return false;
    if (name != null ? !name.equals(service.name) : service.name != null) return false;
    if (nestedServices != null ? !nestedServices.equals(service.nestedServices) : service.nestedServices != null) return false;
    if (properties != null ? !properties.equals(service.properties) : service.properties != null) return false;
    if (suffix != null ? !suffix.equals(service.suffix) : service.suffix != null) return false;
    if (typeS != null ? !typeS.equals(service.typeS) : service.typeS != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (base != null ? base.hashCode() : 0);
    result = 31 * result + (typeS != null ? typeS.hashCode() : 0);
    result = 31 * result + (desc != null ? desc.hashCode() : 0);
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    result = 31 * result + (nestedServices != null ? nestedServices.hashCode() : 0);
    result = 31 * result + (properties != null ? properties.hashCode() : 0);
    return result;
  }
}
