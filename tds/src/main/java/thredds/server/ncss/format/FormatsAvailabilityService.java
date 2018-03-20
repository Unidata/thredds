/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.format;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public final class FormatsAvailabilityService {

  private static Map<SupportedFormat, Boolean> formatsAvailability = new HashMap<>();

  static {
    //Default availabiliy
    formatsAvailability.put(SupportedFormat.XML_FILE, true);
    formatsAvailability.put(SupportedFormat.XML_STREAM, true);
    formatsAvailability.put(SupportedFormat.CSV_FILE, true);
    formatsAvailability.put(SupportedFormat.CSV_STREAM, true);
    formatsAvailability.put(SupportedFormat.NETCDF3, true);
    formatsAvailability.put(SupportedFormat.NETCDF4, false);              // must be turned on
    formatsAvailability.put(SupportedFormat.NETCDF4EXT, false);           // extended model
    formatsAvailability.put(SupportedFormat.WKT, true);
    formatsAvailability.put(SupportedFormat.JSON, true);
    formatsAvailability.put(SupportedFormat.WATERML2, true);
  }

  static public boolean isFormatAvailable(SupportedFormat format) {
    Boolean available = formatsAvailability.get(format);
    return (available == null) ? false : available;
  }

  static public void setFormatAvailability(SupportedFormat format, boolean available) {
    formatsAvailability.put(format, available);
  }
}
