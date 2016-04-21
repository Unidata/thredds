/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.Service;
import thredds.client.catalog.ServiceType;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.AllowedServicesIF;
import thredds.server.config.TdsContext;
import ucar.nc2.constants.FeatureType;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Manage Lists of Services
 *
 * @author caron
 * @see "src/main/webapp/WEB-INF/tdsGlobalConfig.xml"
 * @since 1/23/2015
 */
@Component
public class AllowedServices implements AllowedServicesIF {
  static private final Logger logServerStartup = LoggerFactory.getLogger("serverStartup");
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger("catalogInit");

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

  @Autowired
  private DebugCommands debugCommands;

  private Map<StandardService, AllowedService> allowed = new HashMap<>();
  private List<String> allowedGridServiceNames, allowedPointServiceNames, allowedPointCollectionServiceNames, allowedRadialServiceNames;
  private List<Service> allowedGridServices = new ArrayList<>();
  private List<Service> allowedPointServices = new ArrayList<>();
  private List<Service> allowedPointCollectionServices = new ArrayList<>();
  private List<Service> allowedRadialServices = new ArrayList<>();
  private Map<String, Service> globalServices = new HashMap<>();

  // see WEB-INF/tdsGlobalConfig.xml
  public void setAllow(Map<String, Boolean> map) {
    for (Map.Entry<String, Boolean> s : map.entrySet()) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s.getKey());
      if (service == null)
        logServerStartup.error("No service named " + s.getKey());
      else
        allowed.put(service, new AllowedService(service, s.getValue()));
    }
  }

  // allows users to turn off services in ThreddsConfig
  public void setAllowService(StandardService service, Boolean allow) {
    if (allow == null) return;
    AllowedService as = allowed.get(service);
    if (as == null)
      logServerStartup.error("AllowedService cant find StandardService " + service);
    else
      as.allowed = allow;
  }

  public void setGridServices(List<String> list) {
    this.allowedGridServiceNames = list;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  public void setPointServices(List<String> list) {
    this.allowedPointServiceNames = list;
  }

  public void setPointCollectionServices(List<String> list) {
    this.allowedPointCollectionServiceNames = list;
  }

  public void setRadialServices(List<String> list) {
    this.allowedRadialServiceNames = list;
  }

  public void finish() {
    List<Service> result = new ArrayList<>();
    for (String s : allowedGridServiceNames) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s);
      if (service == null)
        logServerStartup.error("No service named " + s);
      else {
        AllowedService as = allowed.get(service);
        if (as != null && as.allowed)
          result.add(makeService(as.ss));
      }
    }
    allowedGridServices = Collections.unmodifiableList(result);

    result = new ArrayList<>();
    for (String s : allowedPointCollectionServiceNames) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s);
      if (service == null)
        logServerStartup.error("No service named " + s);
      else {
        AllowedService as = allowed.get(service);
        if (as != null && as.allowed)
          result.add(makeService(as.ss));
      }
    }
    allowedPointCollectionServices = Collections.unmodifiableList(result);

    result = new ArrayList<>();
    for (String s : allowedPointServiceNames) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s);
      if (service == null)
        logServerStartup.error("No service named " + s);
      else {
        AllowedService as = allowed.get(service);
        if (as != null && as.allowed)
          result.add(makeService(as.ss));
      }
    }
    allowedPointServices = Collections.unmodifiableList(result);

    result = new ArrayList<>();
    for (String s : allowedRadialServiceNames) {
      StandardService service = StandardService.getStandardServiceIgnoreCase(s);
      if (service == null)
        logServerStartup.error("No service named " + s);
      else {
        AllowedService as = allowed.get(service);
        if (as != null && as.allowed)
          result.add(makeService(as.ss));
      }
    }
    allowedRadialServices = Collections.unmodifiableList(result);
  }

  private Service makeService(StandardService ss) {
    String path = ss.base.startsWith("/") ? tdsContext.getContextPath() + ss.base : ss.base;
    // (String name, String base, String typeS, String desc, String suffix, List<Service> nestedServices, List<Property> properties
    return new Service(ss.type.toString(), path, ss.type.toString(), null, null, null, null);
  }

  ////////////////////////////////////////////////
  // public API

  public Service getStandardServices(String featTypeName) {
    FeatureType ft = FeatureType.getType(featTypeName);
    return (ft == null) ? null : getStandardServices(ft);
  }

  public Service getStandardServices(FeatureType featType) {
    if (featType.isCoverageFeatureType()) {
      return new Service("GridServices", "", ServiceType.Compound.toString(), null, null, allowedGridServices, null);
    }

    if (featType.isPointFeatureType()) {
      return new Service("PointServices", "", ServiceType.Compound.toString(), null, null, allowedPointServices, null);
    }

    if (featType == FeatureType.RADIAL) {
      return new Service("RadialServices", "", ServiceType.Compound.toString(), null, null, allowedRadialServices, null);
    }

    return null;
  }

  public Service getStandardCollectionServices(FeatureType featType) {
    if (featType.isCoverageFeatureType()) {
      return new Service("GridCollectionServices", "", ServiceType.Compound.toString(), null, null, allowedGridServices, null);
    }

    if (featType.isPointFeatureType()) {
      return new Service("PointCollectionServices", "", ServiceType.Compound.toString(), null, null, allowedPointCollectionServices, null);
    }

    return null;
  }

  public boolean isAllowed(StandardService type) {
    AllowedService s = allowed.get(type);
    return s != null && s.allowed;
  }

  @Nullable
  public Service getStandardService(StandardService type) {
    AllowedService s = allowed.get(type);
    if (s == null) return null;
    return !s.allowed ? null : makeService(s.ss);
  }

  ///////////////////////////////////////////////////////
  // manage global services - in the top catalog

  public void clearGlobalServices() {
    globalServices = new HashMap<>();
  }

  public void addGlobalServices(List<Service> services) {
    for (Service s : services) {
      Service got = globalServices.get(s.getName());
      if (got != null) {
        logCatalogInit.error("Already have a global service {} trying to add {}", got, s);
      } else {
        globalServices.put(s.getName(), s);
        logCatalogInit.info("Added global service {}", s);
      }
    }
  }

  public Service findGlobalService(String name) {
    if (name == null) return null;
    return globalServices.get(name);
  }

  public Collection<Service> getGlobalServices() {
    return globalServices.values();
  }

  /////////////////////////////////////////////
  // used by old-style config catalogs for error checking and messages

  /**
   * Checks if a list of services are allowed.
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
      AllowedService as = findByService(service);
      if (as != null && !as.allowed)              // must be explicitly disallowed
        disallowedServices.add(service.getName());
    }
  }

  private AllowedService findByService(Service service) {
    for (AllowedService entry : allowed.values()) {
      if (entry.ss.type == service.getType()) {
        if (entry.ss.type == ServiceType.NetcdfSubset) { // have to special case this
          if (!service.getBase().startsWith(entry.ss.base)) continue; // keep going
        }
        return entry; // otherwise we found it
      }
    }
    return null;
  }

  public void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showServices", "Show standard and global services") {
      public void doAction(DebugCommands.Event e) {
        e.pw.printf("%n<h3>Global Services</h3>%n");
        for (Service s : getGlobalServices()) {
          if (s.getType() == ServiceType.Compound) {
            e.pw.printf(" <b>%s </b>(Compound):%n", s.getName());
            for (Service sn : s.getNestedServices())
              e.pw.printf("   <b>%s:</b> %s%n", sn.getName(), sn.toString());
            e.pw.printf("%n");

          } else {
            e.pw.printf(" <b>%s:</b> %s%n", s.getName(), s.toString());
          }
        }
        e.pw.printf("%n<h3>Grid Services</h3>%n");
        for (Service s : allowedGridServices) {
          e.pw.printf(" <b>%s:</b> %s%n", s.getName(), s.toString());
        }
        e.pw.printf("%n<h3>Point Services</h3>%n");
        for (Service s : allowedPointServices) {
          e.pw.printf(" <b>%s:</b> %s%n", s.getName(), s.toString());
        }
        e.pw.printf("%n<h3>Point Collection Services</h3>%n");
        for (Service s : allowedPointCollectionServices) {
          e.pw.printf(" <b>%s:</b> %s%n", s.getName(), s.toString());
        }
        e.pw.printf("%n<h3>Radial Services</h3>%n");
        for (Service s : allowedRadialServices) {
          e.pw.printf(" <b>%s:</b> %s%n", s.getName(), s.toString());
        }
      }
    };
    debugHandler.addAction(act);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private List<String> isAThreddsDataset;
  public void setIsAThreddsDataset(List<String> list) {
    this.isAThreddsDataset = list;
  }

  private List<String> isNotAThreddsDataset;
  public void setIsNotAThreddsDataset(List<String> list) {
    this.isNotAThreddsDataset = list;
  }

  public boolean isAThreddsDataset(String filename) {
    if (isAThreddsDataset != null) {
      for (String suffix : isAThreddsDataset)
        if (filename.endsWith(suffix)) return true;
    }
    if (isNotAThreddsDataset != null) {
      for (String suffix : isNotAThreddsDataset)
        if (filename.endsWith(suffix)) return false;
    }

    return true;
  }

}