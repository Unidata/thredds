/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package thredds.server.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvService;
import thredds.catalog.ServiceType;
import thredds.servlet.ThreddsConfig;

/**
 * Enum with the services that can be allowed/disallowed in the ThreddsConfig
 * file. Provides static methods that check if a service declared in a catalog
 * is allowed at server level.
 * LOOK: is this implemented ??
 * @author mhermida
 * 
 */
public enum AllowableService {

  // LOOK this assumes that ThreddsConfig has been initialized before this class is instantiated !
	CDMREMOTE(InvService.wms, ThreddsConfig.getBoolean("CdmRemote.allow", true)),
  NCSS(InvService.ncss, ThreddsConfig.getBoolean("NetcdfSubsetService.allow", true)),
	WMS(InvService.wms, ThreddsConfig.getBoolean("WMS.allow", false)),
  WCS(InvService.wcs, ThreddsConfig.getBoolean("WCS.allow", false)),
  ISO(InvService.iso, ThreddsConfig.getBoolean("NCISO.isoAllow", false)),
  UDDC(InvService.uddc, ThreddsConfig.getBoolean("NCISO.uddcAllow", false)),
  NCML(InvService.ncml, ThreddsConfig.getBoolean("NCISO.ncmlAllow", false));

	private Boolean allowed;
	private InvService service;

	private AllowableService(InvService service, boolean allowed) {
		this.allowed = allowed;
		this.service = service;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public InvService getService() {
		return service;
	}

	/**
	 * 
	 * Takes an InvCatalog and checks that the services declared in that catalog
	 * are allowed at server level in the threddsConfig file
	 * 
	 * @param catalog check this catalog
	 * @return A list with the declared services in the catalog that are not allowed at server level
	 */
	public static List<String> checkCatalogServices(InvCatalog catalog) {

		List<String> disallowedServices = new ArrayList<>();

		List<InvService> services = catalog.getServices();
		for (InvService s : services) {
			disallowedServices.addAll(checkService(s));
		}
		return disallowedServices;
	}

	private static List<String> checkService(InvService service) {

		List<String> disallowedServices = new ArrayList<>();

		if (service.getServiceType() == ServiceType.COMPOUND) {

			for (InvService s : service.getServices()) {
				disallowedServices.addAll(checkService(s));
			}

		} else {
			if (!isServiceAllowed(service))
				disallowedServices.add(service.getName());
		}

		return disallowedServices;
	}

	private static boolean isServiceAllowed(InvService service) {

		AllowableService as = findAllowableService(service);

		if (as == null)
			return true;

		return as.isAllowed();
	}

	private static AllowableService findAllowableService(InvService service) {
		boolean found = false;
		List<AllowableService> allServices = Arrays.asList(AllowableService
				.values());
		Iterator<AllowableService> it = allServices.iterator();
		AllowableService as = null;
		while (!found && it.hasNext()) {
			as = it.next();
			if (as.getService().equals(service)) {
				found = true;
			}
		}

		if (found)
			return as;

		return null;
	}
}
