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

import java.util.List;

/**
 * <xsd:element name="service">
  <xsd:complexType>
   <xsd:sequence>
     <xsd:element ref="property" minOccurs="0" maxOccurs="unbounded" />
     <xsd:element ref="service" minOccurs="0" maxOccurs="unbounded" />
   </xsd:sequence>

   <xsd:attribute name="name" type="xsd:string" use="required" />
   <xsd:attribute name="base" type="xsd:string" use="required" />
   <xsd:attribute name="serviceType" type="serviceTypes" use="required" />
   <xsd:attribute name="desc" type="xsd:string"/>
   <xsd:attribute name="suffix" type="xsd:string" />
  </xsd:complexType>
 </xsd:element>
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Service {            // (7)
  private final String name;
  private final String base;
  private final ServiceType type;
  private final String desc;
  private final String suffix;
  private final List<Service> nestedServices;
  private final List<Property> properties;

  public Service(String name, String base, ServiceType type, String desc, String suffix, List<Service> nestedServices, List<Property> properties) {
    this.name = name;
    this.base = base;
    this.type = type;
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

  public ServiceType getType() {
    return type;
  }

  public String getDesc() {
    return desc;
  }

  public String getSuffix() {
    return suffix;
  }

  public List<Service> getNestedServices() {
    return nestedServices;
  }

  public List<Property> getProperties() {
    return properties;
  }
}
