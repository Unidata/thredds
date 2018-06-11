/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.constants;

import java.nio.charset.Charset;

/**
 * CDM constants.
 *
 * @author caron
 * @since 12/20/11
 */
public class CDM {
  public static final String UTF8 = "UTF-8";
  public static final Charset utf8Charset = Charset.forName("UTF-8");

  // structural
  public static final String CHUNK_SIZES = "_ChunkSizes";
  public static final String COMPRESS = "_Compress";
  public static final String COMPRESS_DEFLATE = "deflate";
  public static final String FIELD_ATTS = "_field_atts";    // netcdf4 compound atts

  // from the Netcdf Users Guide
  // http://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#Attribute-Conventions
  public static final String ABBREV = "abbreviation";
  public static final String ADD_OFFSET = "add_offset";
  public static final String CONVENTIONS = "Conventions";
  public static final String DESCRIPTION = "description";
  public static final String FILL_VALUE = "_FillValue";
  public static final String HISTORY = "history";
  public static final String LONG_NAME = "long_name";
  public static final String MISSING_VALUE = "missing_value";
  public static final String SCALE_FACTOR = "scale_factor";
  public static final String TITLE = "title";
  public static final String UNITS = "units";
  public static final String UDUNITS = "udunits";
  public static final String UNSIGNED = "_Unsigned";
  public static final String VALID_RANGE = "valid_range";
  public static final String VALID_MIN = "valid_min";
  public static final String VALID_MAX = "valid_max";

  // staggering for _Coordinate.Stagger
  public static final String ARAKAWA_E = "Arakawa-E";
  public static final String CurvilinearOrthogonal = "Curvilinear_Orthogonal";
  public static final String StaggerType = "stagger_type";

  // misc
  public static final String CF_EXTENDED = "CDM-Extended-CF";
  public static final String FILE_FORMAT = "file_format";
  public static final String GAUSSIAN = "gaussian_lats";
  public static final String LAT_UNITS = "degrees_north";
  public static final String LON_UNITS = "degrees_east";
  public static final String RLATLON_UNITS = "degrees";
  public static final String TIME_OFFSET = "time offset from runtime";

  // Special Attribute Names
  public static final String NCPROPERTIES = "_NCProperties";
  public static final String ISNETCDF4 = "_IsNetcdf4";
  public static final String SUPERBLOCKVERSION = "_SuperblockVersion";
  public static final String DAP4_LITTLE_ENDIAN = "_DAP4_Little_Endian";
  public static final String EDU_UCAR_PREFIX = "_edu.ucar";

  // class not interface, per Bloch edition 2 item 19
  private CDM() {} // disable instantiation
}
