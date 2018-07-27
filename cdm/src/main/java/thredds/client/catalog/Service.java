/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import ucar.nc2.util.Indent;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Formatter;
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
  private final String accessType;
  private final String suffix;
  private final List<Service> nestedServices;
  private final List<Property> properties;

  public Service(String name, String base, String typeS, String desc, String suffix, List<Service> nestedServices, List<Property> properties, String accessType) {
    this.name = name;
    this.base = base; // (base.length() == 0 || base.endsWith("/")) ? base : base + "/";
    this.typeS = typeS;
    this.desc = desc;
    this.accessType = accessType;
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

  public String getAccessType() { return accessType; }

  public String getSuffix() {
    return suffix == null ? "" : suffix;
  }

  public List<Service> getNestedServices() {
    return nestedServices == null ? new ArrayList<>(0) : nestedServices;
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

  protected String toString(Indent indent) {
    Formatter f = new Formatter();
    f.format("%sService{ name=%s, base=%s, typeS=%s", indent, name, base, typeS);
    if (desc != null) f.format(",desc=%s", desc);
    if (suffix != null) f.format(",suffix=%s", suffix);

    if (properties != null) {
      f.format("%n");
      indent.incr();
      for (Property p : properties)
        f.format("%s%s%n", indent, p.toString());
      indent.decr();
      f.format("%n");
    }

    if (nestedServices != null) {
      f.format("%n");
      indent.incr();
      for (Service nested : nestedServices)
        f.format("%s%n", nested.toString(indent));
      indent.decr();
      f.format("}%n");
    } else
      f.format("}");

    return f.toString();
  }

  @Override
  public String toString() {
    return toString(new Indent(2));
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
