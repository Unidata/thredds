/* Copyright */
package thredds.client.catalog.tools;

import thredds.client.catalog.Dataset;
import thredds.client.catalog.Documentation;
import thredds.client.catalog.ThreddsMetadata;
import thredds.client.catalog.builder.DatasetBuilder;
import ucar.nc2.Attribute;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.util.Map;

/**
 * This uses global attributes (presumably from an netcdf File) and extracts ACDD metadata to add to a dataset's metadata.
 * Will not ovveride metaddata already in the dataset
 *
 * @author caron
 * @since 3/24/2015
 */
public class ThreddsMetadataAcdd {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThreddsMetadataAcdd.class);
  private Map<String,Attribute> ncfile;  // global attributes of the file
  private Dataset ds;             // info from the catalog
  private ThreddsMetadata tmi;           // extract info to here

  public ThreddsMetadataAcdd(Map<String,Attribute> ncfile, DatasetBuilder dsb) {
    this.ncfile = ncfile;
    this.ds = dsb.copyDataset(null);
    this.tmi = dsb.getInheritableMetadata();
  }

  public void extract() {
    extractTimeCoverage();

    if (ds.getGeospatialCoverage() == null) { // thredds metadata takes precedence
      tmi.set(Dataset.GeospatialCoverage, extractGeospatialCoverage());
    }

    Attribute att = ncfile.get(ACDD.keywords);
    if (att != null) {
      String keywordList = att.getStringValue();
      Attribute att2 = ncfile.get(ACDD.keywords_vocabulary);
      String keywords_vocabulary = (att2 == null) ? null : att2.getStringValue();
      addKeywords(keywordList, keywords_vocabulary);
    }

    if (ds.getAuthority() == null) {  // thredds metadata takes precedence
      att = ncfile.get(ACDD.naming_authority);
      if (att != null) {
        tmi.set(Dataset.Authority, att.getStringValue());
      }
    }

    if (ds.getDataFormatName() == null) { // thredds metadata takes precedence
      att = ncfile.get(ACDD.cdm_data_type);
      if (att != null && att.isString()) {
        String val = att.getStringValue();
        FeatureType ft = FeatureType.getType(val);
        if (ft == null) {
          CF.FeatureType cf = CF.FeatureType.getFeatureType(val);
          if (cf != null) ft = CF.FeatureType.convert(cf);
        }
        if (ft != null) tmi.set(Dataset.FeatureType, ft.name());
        else tmi.set(Dataset.FeatureType, val);
      }
    }

    addDocumentation(ACDD.title);
    addDocumentation(ACDD.summary);
    addDocumentation(ACDD.history);
    addDocumentation(ACDD.comment);
    addDocumentation(ACDD.processing_level);
    addDocumentation(ACDD.acknowledgement, "funding");
    addDocumentation(ACDD.license, "rights");

    addDate(ACDD.date_created);
    addDate(ACDD.date_modified);

    addSource(true, ACDD.creator_name, ACDD.creator_url, ACDD.creator_email);
    addSource(false, ACDD.publisher_name, ACDD.publisher_url, ACDD.publisher_email);
  }

  public void extractTimeCoverage() {
    Attribute startTimeAtt = ncfile.get(ACDD.TIME_START);
    Attribute endTimeAtt = ncfile.get(ACDD.TIME_END);
    Attribute durationAtt = ncfile.get(ACDD.TIME_DURATION);
    Attribute resAtt = ncfile.get(ACDD.TIME_RESOLUTION);
    if (startTimeAtt == null && endTimeAtt == null && durationAtt == null) return;

    String dateValue = null;
    DateType start = null;
    if (startTimeAtt != null) {
      try {
        dateValue = startTimeAtt.getStringValue();
        start = new DateType(dateValue, null, null);
      } catch (Exception e) {
        log.warn("MetadataExtractorAcdd Cant Parse start date '{}' for {} message= {}", dateValue, ds.getName(), e.getMessage());
      }
    }

    DateType end = null;
    if (endTimeAtt != null) {
      try {
        dateValue = endTimeAtt.getStringValue();
        end = new DateType(dateValue, null, null);
      } catch (Exception e) {
        log.warn("MetadataExtractorAcdd Cant Parse end date '{}' for {} message= {}", dateValue, ds.getName(), e.getMessage());
      }
    }

    TimeDuration duration = null;
     if (durationAtt != null) {
       try {
         dateValue = durationAtt.getStringValue();
         duration = new TimeDuration(dateValue);
       } catch (Exception e) {
         log.warn("MetadataExtractorAcdd Cant Parse duration '{}' for {} message= {}", dateValue, ds.getName(), e.getMessage());
       }
     }

    TimeDuration resolution = null;
     if (resAtt != null) {
       try {
         dateValue = resAtt.getStringValue();
         resolution = new TimeDuration(dateValue);
       } catch (Exception e) {
         log.warn("MetadataExtractorAcdd Cant Parse resolution '{}' for {} message= {}", dateValue, ds.getName(), e.getMessage());
       }
     }

    try {
      DateRange tc = new DateRange(start, end, duration, resolution);
      tmi.set(Dataset.TimeCoverage, tc);

    } catch (Exception e) {
      log.warn("MetadataExtractorAcdd Cant Calculate DateRange for {} message= {}", ds.getName(), e.getMessage());
    }

  }

  public ThreddsMetadata.GeospatialCoverage extractGeospatialCoverage() {
    ThreddsMetadata.GeospatialRange latRange = makeRange( false, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LAT_RESOLUTION, ACDD.LAT_UNITS);
    if (latRange == null) return null;

    ThreddsMetadata.GeospatialRange lonRange = makeRange( true, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.LON_RESOLUTION, ACDD.LON_UNITS);
    if (lonRange == null) return null;

    ThreddsMetadata.GeospatialRange altRange = makeRange( false, ACDD.VERT_MIN, ACDD.VERT_MAX, ACDD.VERT_RESOLUTION, ACDD.VERT_UNITS);
    Attribute zposAtt = ncfile.get(ACDD.VERT_IS_POSITIVE);
    String zIsPositive = (zposAtt == null) ? null : zposAtt.getStringValue();

    return new ThreddsMetadata.GeospatialCoverage(lonRange, latRange, altRange, null, zIsPositive);
  }

  private ThreddsMetadata.GeospatialRange makeRange(boolean isLon, String minName, String maxName, String resName, String unitsName) {
    Attribute minAtt = ncfile.get(minName);
    Attribute maxAtt = ncfile.get(maxName);
    if (minAtt == null || maxAtt == null) return null;

    Number minN = minAtt.getNumericValue();
    Number maxN = maxAtt.getNumericValue();
    if (minN == null || maxN == null) return null;

    double min = minN.doubleValue();
    double max = maxN.doubleValue();
    double size = max - min;
    if (isLon && max < min) {
      size += 360;
    }

    double res = Double.NaN;
    Attribute resAtt = ncfile.get(resName);
    if (resAtt != null) {
      Number result = resAtt.getNumericValue();
      if (result != null) res = result.doubleValue();
    }

    Attribute unitAtt = ncfile.get(unitsName);
    String units = (unitAtt == null) ? null : unitAtt.getStringValue();

    return new ThreddsMetadata.GeospatialRange(min, size, res, units);
  }

  private void addDocumentation(String docType) {
    Attribute att = ncfile.get(docType);
    if (att != null) {
      String docValue = att.getStringValue();
      String dsValue = ds.getDocumentation(docType);    // metadata/documentation[@type="docType"]
      if (dsValue == null || !dsValue.equals(docValue))
        tmi.addToList(Dataset.Documentation, new Documentation(null, null, null, docType, docValue));
    }
  }

  private void addDocumentation(String attName, String docType) {
    Attribute att = ncfile.get(attName);
    if (att != null) {
      String docValue = att.getStringValue();
      String dsValue = ds.getDocumentation(docType);        // metadata/documentation[@type="docType"]
      if (dsValue == null || !dsValue.equals(docValue))
        tmi.addToList(Dataset.Documentation, new Documentation(null, null, null, docType, docValue));
    }
  }

  private void addKeywords(String keywordList, String vocabulary)  {
    String[] keywords = keywordList.split(",");

    for (String kw : keywords)
      tmi.addToList(Dataset.Keywords, new ThreddsMetadata.Vocab(kw, vocabulary));
  }

  private void addDate(String dateType) {
    Attribute att = ncfile.get(dateType);
    if (att != null) {
      String dateValue = att.getStringValue();

      try {
        tmi.addToList(Dataset.Dates, new DateType(dateValue, null, dateType));
      } catch (Exception e) {
        log.warn("MetadataExtractorAcdd Cant Parse {} date '{}' for {} message= {}", dateType, dateValue, ds.getName(), e.getMessage());
      }
    }
  }

  private void addSource(boolean isCreator, String sourceName, String urlName, String emailName) {
    Attribute att = ncfile.get(sourceName);
    if (att != null) {
      String sourceValue = att.getStringValue();

      Attribute urlAtt = ncfile.get(urlName);
      String url = (urlAtt == null) ? null : urlAtt.getStringValue();

      Attribute emailAtt = ncfile.get(emailName);
      String email = (emailAtt == null) ? null : emailAtt.getStringValue();

      ThreddsMetadata.Vocab name = new ThreddsMetadata.Vocab(sourceValue, null);
      ThreddsMetadata.Source src = new ThreddsMetadata.Source(name, url, email);

      if (isCreator)
        tmi.addToList(Dataset.Creators, src);
      else
        tmi.addToList(Dataset.Publishers, src);
    }
  }
}
