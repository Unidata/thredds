/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.grib2;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.grib.grib2.table.WmoTemplateTables;
import ucar.nc2.grib.grib2.table.WmoTemplateTables.TemplateTable;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;

import java.util.Formatter;

/**
 * Utilities to show Grib2 records.
 *
 * @author caron
 * @since 12/22/2015.
 */
public class Grib2Show {

  public static void showBytes(Formatter f, byte[] buff, int max) {
    int count = 0;
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      if (b >= 32 && b < 127)
        f.format("%s", (char) ub);
      else
        f.format("(%d)", ub);
      if (max > 0 && count++ > max) break;
    }
  }

  public static void showCompleteGribRecord(Formatter f, String path, Grib2Record gr, Grib2Tables cust) {
    f.format("File=%d %s offset=%d%n", gr.getFile(), path, gr.getIs().getStartPos());
    f.format("Header=\"");
    showBytes(f, gr.getHeader(), 100);
    f.format("\"%n");

    Grib2Variable gv = new Grib2Variable(cust, gr, 0, FeatureCollectionConfig.intvMergeDef, FeatureCollectionConfig.useGenTypeDef);
    f.format("cdmHash=%d%n", gv.hashCode());

    int d = gr.getDiscipline();
    f.format("Grib2IndicatorSection%n");
    f.format(" Discipline = (%d) %s%n", d, cust.getCodeTableValue("0.0", d));
    f.format(" Length     = %d%n", gr.getIs().getMessageLength());

    Grib2SectionIdentification id = gr.getId();
    f.format("%nGrib2IdentificationSection%n");
    f.format(" Center        = (%d) %s%n", id.getCenter_id(), CommonCodeTable.getCenterName(id.getCenter_id(), 2));
    f.format(" SubCenter     = (%d) %s%n", id.getSubcenter_id(), cust.getSubCenterName(id.getCenter_id(), id.getSubcenter_id()));
    f.format(" Master Table  = %d%n", id.getMaster_table_version());
    f.format(" Local Table   = %d%n", id.getLocal_table_version());
    f.format(" RefTimeSignif = %d (%s)%n", id.getSignificanceOfRT(), cust.getCodeTableValue("1.2", id.getSignificanceOfRT()));
    f.format(" RefTime       = %s%n", id.getReferenceDate());
    f.format(" RefTime Fields = %d-%d-%d %d:%d:%d%n", id.getYear(), id.getMonth(), id.getDay(), id.getHour(), id.getMinute(), id.getSecond());
    f.format(" ProductionStatus      = %d (%s)%n", id.getProductionStatus(), cust.getCodeTableValue("1.3", id.getProductionStatus()));
    f.format(" TypeOfProcessedData   = %d (%s)%n", id.getTypeOfProcessedData(), cust.getCodeTableValue("1.4", id.getTypeOfProcessedData()));

    if (gr.hasLocalUseSection()) {
      byte[] lus = gr.getLocalUseSection().getRawBytes();
      f.format("%nLocal Use Section (grib section 2)%n");
      f.format("bytes (len=%d) =", lus.length);
      Misc.showBytes(lus, f);
      f.format("%n");
    }

    Grib2SectionGridDefinition gds = gr.getGDSsection();
    Grib2Gds ggds = gds.getGDS();
    f.format("%nGrib2GridDefinitionSection hash=%d crc=%d%n", ggds.hashCode(), gds.calcCRC());
    f.format(" Length             = %d%n", gds.getLength());
    f.format(" Source  (3.0)      = %d (%s) %n", gds.getSource(), cust.getCodeTableValue("3.0", gds.getSource()));
    f.format(" Npts               = %d%n", gds.getNumberPoints());
    f.format(" Template (3.1)     = %d%n", gds.getGDSTemplateNumber());
    showGdsTemplate(gds, f, cust);

    Grib2SectionProductDefinition pdss = gr.getPDSsection();
    f.format("%nGrib2ProductDefinitionSection%n");
    Grib2Pds pds = gr.getPDS();
    if (pds.isTimeInterval()) {
      TimeCoordIntvDateValue intv = cust.getForecastTimeInterval(gr);
      if (intv != null) f.format(" Interval     = %s%n", intv);
    }
    showPdsTemplate(pdss, f, cust);
    if (pds.getExtraCoordinatesCount() > 0) {
      float[] coords = pds.getExtraCoordinates();
      if (coords != null) {
        f.format("Hybrid Coordinates (%d) %n  ", coords.length);
        for (float fc : coords) f.format("%10.5f ", fc);
      }
      f.format("%n%n");
    }

    Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
    f.format("%nGrib2SectionDataRepresentation%n");
    f.format("  Template           = %d (%s) %n", drs.getDataTemplate(), cust.getCodeTableValue("5.0", drs.getDataTemplate()));
    f.format("  NPoints            = %d%n", drs.getDataPoints());

    Grib2SectionData ds = gr.getDataSection();
    f.format("%nGrib2SectionData%n");
    f.format("  Starting Pos       = %d %n", ds.getStartingPosition());
    f.format("  Data Length        = %d%n", ds.getMsgLength());
  }

  public static void showGdsTemplate(Grib2SectionGridDefinition gds, Formatter f, Grib2Tables cust) {
    int template = gds.getGDSTemplateNumber();
    byte[] raw = gds.getRawBytes();
    showRawWithTemplate("3." + template, raw, f, cust);
  }

  public static void showPdsTemplate(Grib2SectionProductDefinition pdss, Formatter f, Grib2Tables cust) {
    int template = pdss.getPDSTemplateNumber();
    byte[] raw = pdss.getRawBytes();
    showRawWithTemplate("4." + template, raw, f, cust);
  }

  private static void showRawWithTemplate(String key, byte[] raw, Formatter f, Grib2Tables cust) {
    TemplateTable template = WmoTemplateTables.getInstance().getTemplateTable(key);
    if (template == null)
      f.format("Cant find template %s%n", key);
    else
      template.showInfo(cust, raw, f);
  }

  public static void showProcessedPds(Grib2Tables cust, Grib2Pds pds, int discipline, Formatter f) {
    int template = pds.getTemplateNumber();
    f.format(" Product Template %3d = %s%n", template, cust.getCodeTableValue("4.0", template));
    f.format(" Discipline %3d     = %s%n", discipline, cust.getCodeTableValue("0.0", discipline));
    f.format(" Category %3d       = %s%n", pds.getParameterCategory(), cust.getCategory(discipline, pds.getParameterCategory()));
    GribTables.Parameter entry = cust.getParameter(discipline, pds);
    if (entry != null) {
      f.format(" Parameter Name     = %3d %s %n", pds.getParameterNumber(), entry.getName());
      f.format(" Parameter Units    = %s %n", entry.getUnit());
    } else {
      f.format(" Unknown Parameter  = %d-%d-%d %n", discipline, pds.getParameterCategory(), pds.getParameterNumber());
      cust.getParameter(discipline, pds); // debug
    }
    f.format(" Parameter Table  = %s%n", cust.getParamTablePathUsedFor(discipline, pds.getParameterCategory(), pds.getParameterNumber()));

    int tgp = pds.getGenProcessType();
    f.format(" Generating Process Type = %3d %s %n", tgp, cust.getCodeTableValue("4.3", tgp));
    f.format(" Forecast Offset    = %3d %n", pds.getForecastTime());
    f.format(" First Surface Type = %3d %s %n", pds.getLevelType1(), cust.getLevelNameShort(pds.getLevelType1()));
    f.format(" First Surface value= %3f %n", pds.getLevelValue1());
    f.format(" Second Surface Type= %3d %s %n", pds.getLevelType2(), cust.getLevelNameShort(pds.getLevelType2()));
    f.format(" Second Surface val = %3f %n", pds.getLevelValue2());
    f.format("%n Level Name (from table 4.5) = %3s %n", cust.getCodeTableValue("4.5", pds.getLevelType1()));
    f.format(" Gen Process Ttype (from table 4.3) = %3s %n", cust.getCodeTableValue("4.3", pds.getGenProcessType()));
  }

  public static void showProcessedGridRecord(Grib2Tables cust, Grib2Record gr, Formatter f) {
    GribTables.Parameter param = cust.getParameter(gr);
    if (param != null) {
      f.format("  Parameter=%s (%s)%n", param.getName(), param.getAbbrev());
    } else {
      f.format(" Unknown Parameter  = %d-%d-%d %n", gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
    }

    Grib2Pds pds = gr.getPDS();
    VertCoordType levelUnit = cust.getVertUnit(pds.getLevelType1());
    f.format("  Level=%f/%f %s; level name =  (%s)%n", pds.getLevelValue1(), pds.getLevelValue1(), levelUnit.getUnits(), cust.getLevelNameShort(pds.getLevelType1()));

    String intvName = "none";
    if (pds instanceof Grib2Pds.PdsInterval) {
      Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
      Grib2Pds.TimeInterval[] ti = pdsi.getTimeIntervals();
      int statType = ti[0].statProcessType;
      intvName = cust.getStatisticNameShort(statType);
    }

    f.format("  Time Unit=%s; Stat=%s%n", Grib2Utils.getCalendarPeriod( pds.getTimeUnit()), intvName);
    f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
    f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
    TimeCoordIntvDateValue intv = cust.getForecastTimeInterval(gr);
    if (intv != null) f.format("  TimeInterval=%s%n", intv);
    f.format("%n");
    pds.show(f);

    //CFSR malarky
    cust.showSpecialPdsInfo(gr, f);
  }
}
