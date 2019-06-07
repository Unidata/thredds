/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.format;

import thredds.server.ncss.exception.UnsupportedResponseFormatException;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static thredds.server.ncss.format.SupportedFormat.*;

/**
 * An enum of the various operations for netcdf subset service, and what download formats are allowed
 */
@SuppressWarnings("ProblematicWhitespace")
public enum SupportedOperation {
  DATASET_INFO_REQUEST("Dataset info request", XML_FILE),
  DATASET_BOUNDARIES_REQUEST("Dataset grid boundaries request", WKT, JSON),
  GRID_REQUEST("Grid data request", NETCDF3, NETCDF4, NETCDF4EXT,WRF),
  GRID_AS_POINT_REQUEST("Grid as point request", CSV_STREAM, CSV_FILE, XML_STREAM, XML_FILE, NETCDF3, NETCDF4, NETCDF4EXT),
  POINT_REQUEST("Point data request", CSV_STREAM, CSV_FILE, XML_STREAM, XML_FILE, NETCDF3, NETCDF4, NETCDF4EXT),
  STATION_REQUEST("Station data request", CSV_STREAM, CSV_FILE, XML_STREAM, XML_FILE, NETCDF3, NETCDF4, NETCDF4EXT, WATERML2);

  private final String operationName;
  private final List<SupportedFormat> supportedFormats;

  SupportedOperation(String operationName, SupportedFormat... formats) {
    this.operationName = operationName;
    this.supportedFormats = Arrays.asList(formats);
    assert this.supportedFormats.size() > 0;
  }

  public String getName() {
    return operationName;
  }

  public List<SupportedFormat> getSupportedFormats() {
    List<SupportedFormat> result = new ArrayList<>();
    for (SupportedFormat sf : supportedFormats) {
      if (FormatsAvailabilityService.isFormatAvailable(sf))
        result.add(sf);
    }
    return result;
  }

  public SupportedFormat getDefaultFormat() {
    for (SupportedFormat sf : supportedFormats) {
      if (FormatsAvailabilityService.isFormatAvailable(sf))
        return sf;
    }
    return null;
  }

  public @Nonnull
  SupportedFormat getSupportedFormat(String want) throws UnsupportedResponseFormatException {
    if (want == null || want.equals("")) {
      SupportedFormat sf = getDefaultFormat();
      if (sf != null) return sf;
      throw new UnsupportedResponseFormatException("No default Format available");
    }

    for (SupportedFormat f : getSupportedFormats()) {
      if (f.isAlias(want) && FormatsAvailabilityService.isFormatAvailable(f)) {
        return f;
      }
    }

    throw new UnsupportedResponseFormatException("Format " + want + " is not supported for " + getName());
  }
}
