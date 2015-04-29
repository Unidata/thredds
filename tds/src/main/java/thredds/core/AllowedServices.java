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

package thredds.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.server.config.TdsContext;

import java.util.*;

/**
 * These are the services that the TDS can do.
 *
 * @author caron
 * @since 1/23/2015
 */
@Component
public class AllowedServices {
  static private final Logger logServerStartup = LoggerFactory.getLogger("serverStartup");

  // These are the services that the TDS can do.*
  private enum StandardService {
    cdmRemote(ServiceType.CdmRemote, "/cdmremote/"),
    cdmrFeature(ServiceType.CdmrFeature, "/cdmrfeature/"),
    dap4(ServiceType.DAP4, "/dap4/"),
    httpServer(ServiceType.HTTPServer, "/fileServer/"),
    resolver(ServiceType.Resolver, ""),
    netcdfSubset(ServiceType.NetcdfSubset, "/ncss/"),
    opendap(ServiceType.OPENDAP, "/dodsC/"),
    wms(ServiceType.WMS, "/wms/"),
    wcs(ServiceType.WCS, "/wcs/"),

    iso(ServiceType.ISO, "/iso/"),
    ncml(ServiceType.NCML, "/ncml/"),
    uddc(ServiceType.UDDC, "/uddc/");

    static public StandardService getStandardServiceIgnoreCase(String typeS) {
      for (StandardService s : values())
        if (s.toString().equalsIgnoreCase(typeS)) return s;
      return null;
    }

    final ServiceType type;
    final String base;

    StandardService(ServiceType type, String base) {
      this.type = type;
      this.base = base;
    }
  }

  private static class AllowedService {
    StandardService ss;
    boolean allowed;

    private AllowedService(StandardService ss, boolean allowed) {
      this.ss = ss;
      this.allowed = allowed;
    }
  }

  //////////////////////////////////////////////////////

  @Autowired
  private TdsContext tdsContext;

  private Map<ServiceType, AllowedService> allowed = new HashMap<>();
  private List<Service> allowedGrid = new ArrayList<>();

  public void setAllow(Map<String, Boolean> map) {
    for (Map.Entry<String, Boolean> s : map.entrySet()) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s.getKey());
      if (service == null)
        logServerStartup.error("No service named " + s.getKey());
      else
        allowed.put(service.type, new AllowedService(service, s.getValue()));
    }
  }

  public void setGridServices(List<String> list) {
    for (String s : list) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s);
      if (service == null)
        logServerStartup.error("No service named " + s);
      else {
        AllowedService as = allowed.get(service.type);
        if (as.allowed)
          allowedGrid.add( getService(as.ss));
      }
    }
  }

  private Service getService(StandardService ss) {
    String path = ss.base.startsWith("/") ? tdsContext.getContextPath() + ss.base : ss.base;
    // (String name, String base, String typeS, String desc, String suffix, List<Service> nestedServices, List<Property> properties
    return new Service(ss.type.toString(), path, ss.type.toString(), null, null, null, null);
  }

  public List<Service> getGridServices() {
    return allowedGrid;
  }

  /**
   * Checks is a list of  services are allowed.
   *
   * @param services check this list of services
   * @return A list of disallowed services, may be empty not null
   */
  public List<String> getDisallowedServices(List<Service> services) {
    List<String> disallowedServices = new ArrayList<>();
    for (Service s : services)
      checkService(s, disallowedServices);
    return disallowedServices;
  }

  private void checkService(Service service, List<String> disallowedServices) {
    if (service.getType() == ServiceType.Compound) {
      for (Service nested : service.getNestedServices())
        checkService(nested, disallowedServices);

    } else {
      if (!isAllowed(service.getType()))
        disallowedServices.add(service.getName());
    }
  }

  public boolean isAllowed(ServiceType type) {
    AllowedService s = allowed.get(type);
    return s == null || s.allowed;
  }

  // may return null
  public Service getStandardService(ServiceType type) {
    AllowedService s = allowed.get(type);
    if (s == null) return null;
    return !s.allowed? null : getService(s.ss);
  }

}