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

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.server.config.ThreddsConfig;

import java.util.*;

/**
 * These are the services that the TDS can do.
 * May be allowed/disallowed in ThreddsConfig.
 * ThreddsConfig is read at the time the object is instantiated.
 *
 * @author caron
 * @since 1/23/2015
 */
//@Component
//@DependsOn("TdsContext")  // which initializes ThreddsConfig
public class AllowedServices {

  private Map<ServiceType, AllowedService> allowed = new HashMap<>();

  private static class AllowedService {
    StandardServices ss;
    boolean allowed;

    private AllowedService(StandardServices ss, boolean allowed) {
      this.ss = ss;
      this.allowed = allowed;
    }
  }

  public AllowedServices() {
    allowed.put(ServiceType.CdmRemote, new AllowedService(StandardServices.cdmRemote, true));
    allowed.put(ServiceType.DAP4, new AllowedService(StandardServices.dap4, true));
    allowed.put(ServiceType.OPENDAP, new AllowedService(StandardServices.opendap, true));
    allowed.put(ServiceType.Resolver, new AllowedService(StandardServices.latest, true));

    allowed.put(ServiceType.NetcdfSubset, new AllowedService(StandardServices.ncss, ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true)));
    allowed.put(ServiceType.WMS, new AllowedService(StandardServices.wms, ThreddsConfig.getBoolean("WMS.allow", true)));
    allowed.put(ServiceType.WCS, new AllowedService(StandardServices.wcs, ThreddsConfig.getBoolean("WCS.allow", true)));
    allowed.put(ServiceType.ISO, new AllowedService(StandardServices.iso, ThreddsConfig.getBoolean("NCISO.isoAllow", true)));
    allowed.put(ServiceType.UDDC, new AllowedService(StandardServices.uddc, ThreddsConfig.getBoolean("NCISO.uddcAllow", true)));
    allowed.put(ServiceType.NCML, new AllowedService(StandardServices.ncml, ThreddsConfig.getBoolean("NCISO.ncmlAllow", true)));
  }

  public boolean isAllowed(ServiceType type) {
    AllowedService s = allowed.get(type);
    return s == null || s.allowed;
  }

  public Service getStandardService(ServiceType type) {
    AllowedService s = allowed.get(type);
    return s == null ? null : s.ss.getService();
  }

  public void addIfAllowed(ServiceType type, List<Service> result) {
    AllowedService s = allowed.get(type);
    if (s == null)
      return ;
    if (s.allowed) result.add( s.ss.getService());
  }

  /**
   * TChecks that the services declared in the catalog
   * are allowed at server level in the threddsConfig file
   *
   * @param catalog check this catalog
   * @return A list with the declared services in the catalog that are not allowed at server level
   */
  public List<String> getDisallowedServices(Catalog catalog) {
    List<String> disallowedServices = new ArrayList<>();
    for (Service s : catalog.getServices())
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

}