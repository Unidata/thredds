/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.constants;

/**
 * Attribute Conventions for Dataset Discovery
 *
 * @author caron
 * @since 9/14/13
 * @see "http://wiki.esipfed.org/index.php?title=Category:Attribute_Conventions_Dataset_Discovery"
 */
public class ACDD {
  // highly recommended
  public static final String title = "title";             // dataset@name
  public static final String summary = "summary";         // metadata/documentation[@type="summary"]
  public static final String keywords = "keywords";       // metadata/keyword
                                                          // A comma separated list of key words and phrases.
  // recommended
  public static final String id = "id";                   // dataset@id
  public static final String naming_authority = "naming_authority"; // dataset@authority

  public static final String keywords_vocabulary = "keywords_vocabulary";  // metadata/keyword@vocabulary
                                                          // If you are using a controlled vocabulary for the words/phrases in your
                                                          // "keywords" attribute, the unique name or identifier of the vocabulary from which keywords
                                                          // are taken. If more than one keyword vocabulary is used, each may be presented with a prefix
                                                          // (e.g., "CF:NetCDF COARDS Climate and Forecast Standard Names") and a following comma, so that
                                                          // keywords may optionally be prefixed with the controlled vocabulary key.

  public static final String cdm_data_type = "cdm_data_type"; // metadata/dataType
                                                          // The organization of the data, as understood by THREDDS (a [1]THREDDS "dataType"]).
                                                          // One of Grid, Image, Station, Swath, and Trajectory. For points, profiles, and time series
                                                          // (described in this NODC guidance, use Station; for Trajectory Time Series, use Trajectory.

  public static final String history = "history";         //  metadata/documentation[@type="history"]
  public static final String comment = "comment";         // metadata/documentation
  public static final String date_modified = "date_modified"; // metadata/date[@type="modified"]

  public static final String creator_name = "creator_name";         //  metadata/creator
  public static final String creator_email = "creator_email";
  public static final String creator_url = "creator_url";

  public static final String publisher_name = "publisher_name";     //  metadata/publisher
  public static final String publisher_email = "publisher_email";
  public static final String publisher_url = "publisher_url";

  public static final String processing_level = "processing_level"; // metadata/documentation[@type="processing_level"]
  public static final String acknowledgement = "acknowledgement";  // metadata/documentation[@type="funding"]

  public static final String standard_name_vocabulary = "standard_name_vocabulary";  // The unique name or identifier of the controlled vocabulary from which
                                                           // variable standard names are taken. If more than one controlled vocabulary is used, each may
                                                           // be presented with a prefix (e.g., "CF:NetCDF COARDS Climate and Forecast Standard Names")
                                                           // and a following comma, so that standard names may optionally be prefixed with the controlled
                                                           // vocabulary key.   LOOK

  public static final String license = "license";       // metadata/documentation[@type="rights"]

  // coverage
  public static final String LAT_MIN = "geospatial_lat_min";
  public static final String LAT_MAX = "geospatial_lat_max";
  public static final String LAT_RESOLUTION = "geospatial_lat_resolution";
  public static final String LAT_UNITS = "geospatial_lat_units";

  public static final String LON_MIN = "geospatial_lon_min";
  public static final String LON_MAX = "geospatial_lon_max";
  public static final String LON_RESOLUTION = "geospatial_lon_resolution";
  public static final String LON_UNITS = "geospatial_lon_units";

  public static final String VERT_MIN = "geospatial_vertical_min";
  public static final String VERT_MAX = "geospatial_vertical_max";
  public static final String VERT_RESOLUTION = "geospatial_vertical_resolution";
  public static final String VERT_UNITS = "geospatial_vertical_units";
  public static final String VERT_IS_POSITIVE = "geospatial_vertical_positive";

  public static final String TIME_START = "time_coverage_start";
  public static final String TIME_END = "time_coverage_end";
  public static final String TIME_DURATION = "time_coverage_duration";
  public static final String TIME_RESOLUTION = "time_coverage_resolution";


  // suggested
  public static final String contributor_info = "contributor_info"; //   The name and role of any individuals or institutions that contributed to the
                                                        // creation of this data. May be presented as free text, or in a format compatible with ISO 19139.

  public static final String date_created = "date_created";  // metadata/date[@type="created"]

  public static final String coverage_content_type = "coverage_content_type";   // Information about the content of the file, valid values are image,
                                                        // thematicClassification, physicalMeasurement, auxiliaryInformation, qualityInformation,
                                                        // referenceInformation, modelResult, coordinate.

  public static final String creator_institution_info = "creator_institution_info"; // Additional information for the institution that produced the data;
                                                        // can include any information as ISO 19139 or free text.

  public static final String creator_project = "creator_project"; // The scientific project that produced the data; should uniquely identify the project.

  public static final String creator_project_info = "creator_project_info"; // Additional information for the institution that produced the data; can include any information as ISO 19139 or free text.

  public static final String publisher_institution = "publisher_institution";  // The institution that published the data file; should uniquely identify the institution.

  public static final String publisher_institution_info = "publisher_institution_info"; // Additional information for the institution that published the data; can include any information as ISO 19139 or free text.

  public static final String publisher_project = "publisher_project";   // The scientific project that published the data; should uniquely identify the project.

  public static final String publisher_project_info = "publisher_project_info";  //  dditional information for the institution that published the data; can include any information as ISO 19139 or free text.

    // class not interface, per Bloch edition 2 item 19
  private ACDD() {} // disable instantiation
}
