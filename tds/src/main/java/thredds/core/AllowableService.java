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

import thredds.client.catalog.*;
import thredds.servlet.ThreddsConfig;

import java.util.*;

/**
 * The services that can be allowed/disallowed in the ThreddsConfig.
 * ThreddsConfig is read at the time the object is instantiated.
 * 
 */
public class AllowableService {

  private Map<ServiceType, StandardServices> allowed = new HashMap<>();

  public AllowableService() {  // LOOK not right - need to allow ones we dont know about
    allowed.put(ServiceType.OPENDAP, StandardServices.opendap);
    allowed.put(ServiceType.CdmRemote, StandardServices.cdmRemote);

    if (ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true))
      allowed.put(ServiceType.NetcdfSubset, StandardServices.ncss);

    if (ThreddsConfig.getBoolean("WMS.allow", true))
      allowed.put(ServiceType.WMS, StandardServices.wms);
    if (ThreddsConfig.getBoolean("WCS.allow", false))
      allowed.put(ServiceType.WCS, StandardServices.wcs);
    if (ThreddsConfig.getBoolean("NCISO.isoAllow", false))
      allowed.put(ServiceType.ISO, StandardServices.iso);
    if (ThreddsConfig.getBoolean("NCISO.uddcAllow", false))
      allowed.put(ServiceType.UDDC, StandardServices.uddc);
    if (ThreddsConfig.getBoolean("NCISO.ncmlAllow", false))
      allowed.put(ServiceType.NCML, StandardServices.ncml);
  }

  public boolean isAllowed(ServiceType type) {
    return allowed.get(type) != null;
  }

  public Service getStandardService(ServiceType type) {
    return allowed.get(type).getService();
  }

  /**
   * Takes an InvCatalog and checks that the services declared in that catalog
   * are allowed at server level in the threddsConfig file
   *
   * @param catalog check this catalog
   * @return A list with the declared services in the catalog that are not allowed at server level
   */
  public List<String> getDisallowedServices(Catalog catalog) {

    List<String> disallowedServices = new ArrayList<>();

    for (Service s : catalog.getServices()) {
      checkService(s, disallowedServices);
    }
    
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