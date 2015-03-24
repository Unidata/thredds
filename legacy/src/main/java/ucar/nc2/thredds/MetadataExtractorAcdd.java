package ucar.nc2.thredds;

import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDocumentation;
import thredds.catalog.ThreddsMetadata;
import ucar.nc2.Attribute;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import java.util.Map;

/**
 * Extract ACDD metadata from dataset and promote into the catalog objects
 *
 * @author caron
 * @since 9/14/13
 */
public class MetadataExtractorAcdd {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataExtractorAcdd.class);
  private Map<String,Attribute> ncfile;
  private InvDatasetImpl ds;
  private ThreddsMetadata tmi;

  public MetadataExtractorAcdd(Map<String,Attribute> ncfile, InvDatasetImpl ds, ThreddsMetadata tmi) {
    this.ncfile = ncfile;
    this.tmi = tmi;
    this.ds = ds;
  }

  public void extract() {
    extractTimeCoverage();

    if (ds.getGeospatialCoverage() == null) { // thredds metadata takes precedence
      tmi.setGeospatialCoverage( extractGeospatialCoverage());
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
        tmi.setAuthority(att.getStringValue());
      }
    }

    if (ds.getDataType() == null) { // thredds metadata takes precedence
      att = ncfile.get(ACDD.cdm_data_type);
      if (att != null && att.isString()) {
        String val = att.getStringValue();
        FeatureType ft = FeatureType.getType(val);
        if (ft == null) {
          CF.FeatureType cf = CF.FeatureType.getFeatureType(val);
          if (cf != null) ft = CF.FeatureType.convert(cf);
        }
        if (ft != null) tmi.setDataType( ft);
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
        log.warn("MetadataExtractorAcdd Cant Parse start date '{}' for {} message= {}", dateValue, ds.getFullName(), e.getMessage());
      }
    }

    DateType end = null;
    if (endTimeAtt != null) {
      try {
        dateValue = endTimeAtt.getStringValue();
        end = new DateType(dateValue, null, null);
      } catch (Exception e) {
        log.warn("MetadataExtractorAcdd Cant Parse end date '{}' for {} message= {}", dateValue, ds.getFullName(), e.getMessage());
      }
    }

    TimeDuration duration = null;
     if (durationAtt != null) {
       try {
         dateValue = durationAtt.getStringValue();
         duration = new TimeDuration(dateValue);
       } catch (Exception e) {
         log.warn("MetadataExtractorAcdd Cant Parse duration '{}' for {} message= {}", dateValue, ds.getFullName(), e.getMessage());
       }
     }

    TimeDuration resolution = null;
     if (resAtt != null) {
       try {
         dateValue = resAtt.getStringValue();
         resolution = new TimeDuration(dateValue);
       } catch (Exception e) {
         log.warn("MetadataExtractorAcdd Cant Parse resolution '{}' for {} message= {}", dateValue, ds.getFullName(), e.getMessage());
       }
     }

    try {
      DateRange tc = new DateRange(start, end, duration, resolution);
      tmi.setTimeCoverage(tc);

    } catch (Exception e) {
      log.warn("MetadataExtractorAcdd Cant Calculate DateRange for {} message= {}", ds.getFullName(), e.getMessage());
    }

  }

  public ThreddsMetadata.GeospatialCoverage extractGeospatialCoverage() {
    ThreddsMetadata.Range latRange = makeRange( false, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LAT_RESOLUTION, ACDD.LAT_UNITS);
    if (latRange == null) return null;

    ThreddsMetadata.Range lonRange = makeRange( true, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.LON_RESOLUTION, ACDD.LON_UNITS);
    if (lonRange == null) return null;

    ThreddsMetadata.Range altRange = makeRange( false, ACDD.VERT_MIN, ACDD.VERT_MAX, ACDD.VERT_RESOLUTION, ACDD.VERT_UNITS);
    Attribute zposAtt = ncfile.get(ACDD.VERT_IS_POSITIVE);
    String zIsPositive = (zposAtt == null) ? null : zposAtt.getStringValue();

    return new ThreddsMetadata.GeospatialCoverage(lonRange, latRange, altRange, null, zIsPositive);
  }

  private ThreddsMetadata.Range makeRange(boolean isLon, String minName, String maxName, String resName, String unitsName) {
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

    return new ThreddsMetadata.Range(min, size, res, units);
  }

  private void addDocumentation(String docType) {
    Attribute att = ncfile.get(docType);
    if (att != null) {
      String docValue = att.getStringValue();
      String dsValue = ds.getDocumentation(docType);    // metadata/documentation[@type="docType"]
      if (dsValue == null || !dsValue.equals(docValue))
        tmi.addDocumentation(new InvDocumentation(null, null, null, docType, docValue));
    }
  }

  private void addDocumentation(String attName, String docType) {
    Attribute att = ncfile.get(attName);
    if (att != null) {
      String docValue = att.getStringValue();
      String dsValue = ds.getDocumentation(docType);        // metadata/documentation[@type="docType"]
      if (dsValue == null || !dsValue.equals(docValue))
        tmi.addDocumentation(new InvDocumentation(null, null, null, docType, docValue));
    }
  }

  private void addKeywords(String keywordList, String vocabulary)  {
    String[] keywords = keywordList.split(",");

    for (String kw : keywords)
      tmi.addKeyword(new ThreddsMetadata.Vocab(kw, vocabulary));
  }

  private void addDate(String dateType) {
    Attribute att = ncfile.get(dateType);
    if (att != null) {
      String dateValue = att.getStringValue();

      try {
        tmi.addDate(new DateType(dateValue, null, dateType));
      } catch (Exception e) {
        log.warn("MetadataExtractorAcdd Cant Parse {} date '{}' for {} message= {}", dateType, dateValue, ds.getFullName(), e.getMessage());
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
        tmi.addCreator(src);
      else
        tmi.addPublisher(src);
    }
  }

}
