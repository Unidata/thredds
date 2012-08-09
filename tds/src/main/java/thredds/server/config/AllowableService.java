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
 * 
 * @author mhermida
 * 
 */
public enum AllowableService {

	WMS(InvService.wms, ThreddsConfig.getBoolean("WMS.allow", false)), WCS(
			InvService.wcs, ThreddsConfig.getBoolean("WCS.allow", false)), NCSS(
			InvService.ncss, ThreddsConfig.getBoolean(
					"NetcdfSubsetService.allow", false)), ISO(InvService.iso,
			ThreddsConfig.getBoolean("NCISO.isoAllow", false)), UDDC(
			InvService.uddc, ThreddsConfig.getBoolean("NCISO.uddcAllow", false)), NCML(
			InvService.ncml, ThreddsConfig.getBoolean("NCISO.ncmlAllow", false));

	private Boolean allowed;
	private InvService service;

	AllowableService(InvService service, boolean allowed) {
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
	 * @param catalog
	 * @return A list with the declared services in the catalog that are not allowed at server level
	 */
	public static List<String> checkCatalogServices(InvCatalog catalog) {

		List<String> disallowedServices = new ArrayList<String>();

		List<InvService> services = catalog.getServices();
		for (InvService s : services) {
			disallowedServices.addAll(checkService(s));

		}
		return disallowedServices;
	}

	private static List<String> checkService(InvService service) {

		List<String> disallowedServices = new ArrayList<String>();

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
