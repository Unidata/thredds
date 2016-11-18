/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

package thredds.server.ncss.format;

import thredds.server.config.FormatsAvailabilityService;
import thredds.server.ncss.exception.UnsupportedResponseFormatException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static thredds.server.ncss.format.SupportedFormat.*;

/**
 * the various operations for netcdf subset service
 *
 * @author mhermida
 */
@SuppressWarnings("ProblematicWhitespace")
public enum SupportedOperation {
    DATASET_INFO_REQUEST("Dataset info request", XML_FILE),
    DATASET_BOUNDARIES_REQUEST("Dataset grid boundaries request", WKT, JSON),
    GRID_REQUEST("Grid data request", NETCDF3, NETCDF4 /*, NETCDF4EXT*/),
    POINT_REQUEST("Point data request", XML_STREAM, XML_FILE, CSV_STREAM, CSV_FILE, GEOCSV_FILE, GEOCSV_STREAM, NETCDF3, NETCDF4 /*, NETCDF4EXT */),
    STATION_REQUEST("Station data request",
            XML_STREAM, XML_FILE, CSV_STREAM, CSV_FILE, GEOCSV_FILE, GEOCSV_STREAM, NETCDF3, NETCDF4, /*NETCDF4EXT,*/ WATERML2);

    private final String operationName;
    private final List<SupportedFormat> supportedFormats;

    private SupportedOperation(String operationName, SupportedFormat... formats) {
        this.operationName = operationName;
        this.supportedFormats = Collections.unmodifiableList(Arrays.asList(formats));
        assert this.supportedFormats.size() > 0;
    }

    public String getName() {
        return operationName;
    }

    public List<SupportedFormat> getSupportedFormats() {
        return supportedFormats;
    }

    public SupportedFormat getDefaultFormat() {
        return supportedFormats.get(0);
    }

    public SupportedFormat getSupportedFormat(String want) throws UnsupportedResponseFormatException {
        if (want == null || want.equals("")) {
            return getDefaultFormat();
        }

        for (SupportedFormat f : getSupportedFormats()) {
            if (f.isAlias(want) && FormatsAvailabilityService.isFormatAvailable(f)) {
                return f;
            }
        }

		/*
		List<SupportedFormat> supportedFormats = operation.getSupportedFormats();

		int len = supportedFormats.size();
		int cont =0;
		boolean found=false;

		while (!found && cont < len) {
			//if( supportedFormats.get(cont).getAliases().contains(format) && supportedFormats.get(cont).isAvailable()
			  )  found = true;
			if( supportedFormats.get(cont).getAliases().contains(format) &&  FormatsAvailabilityService
			.isFormatAvailable(supportedFormats.get(cont))) found = true;
			cont++;
		} 
 	
		if( found ) return supportedFormats.get(cont-1);
		*/

        throw new UnsupportedResponseFormatException("Format " + want + " is not supported for " + getName());
    }
}
