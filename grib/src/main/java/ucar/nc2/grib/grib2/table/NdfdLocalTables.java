/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.grib2.Grib2Parameter;

/**
 * Ndfd local tables
 * @see "http://graphical.weather.gov/docs/grib_design.html"
 */

public class NdfdLocalTables extends LocalTables {
  private static NdfdLocalTables single;

  public static Grib2Customizer getCust(Grib2Table table) {
    if (single == null) single = new NdfdLocalTables(table);
    return single;
  }

  private NdfdLocalTables(Grib2Table grib2Table) {
    super(grib2Table);
    if (grib2Table.getPath() == null)
      grib2Table.setPath(this.getClass().getName());
    init();
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return this.getClass().getName();
  }

  /*
  take from degrib repository:http://slosh.nws.noaa.gov/pubview/degrib/src/degrib/metaname.c?root=degrib&view=markup
  Apparently for center 8, subcenter 0 and subcenter 255 (!)
  */
  private void init() {
    add(0, 0, 193, "ApparentT", "Apparent Temperature", "K");
    add(0, 1, 192, "Wx", "Weather string", "");

    /* LOOK ignored : grandfather'ed in a NDFD choice for POP. */
    add(0, 10, 8, "PoP12", "Prob of 0.01 In. of Precip", "%");

    add(0, 13, 194, "smokes", "Surface level smoke from fires", "log10(g/m^3)");
    add(0, 13, 195, "smokec", "Average vertical column smoke from fires", "log10(g/m^3)");
    add(0, 14, 192, "O3MR", "Ozone Mixing Ratio", "kg/kg");
    add(0, 14, 193, "OZCON", "Ozone Concentration", "PPB");

    /* Arthur adopted NCEP ozone values from NCEP local table to NDFD local tables. (11/14/2009) */
    add(0, 14, 200, "OZMAX1", "Ozone Daily Max from 1-hour Average", "ppbV");
    add(0, 14, 201, "OZMAX8", "Ozone Daily Max from 8-hour Average", "ppbV");

    /* Added 1/23/2007 in preparation for SPC NDFD Grids */
    add(0, 19, 194, "ConvOutlook", "Convective Hazard Outlook", "0=none; 2=tstm; 4=slight; 6=moderate; 8=high");
    add(0, 19, 197, "TornadoProb", "Tornado Probability", "%");
    add(0, 19, 198, "HailProb", "Hail Probability", "%");
    add(0, 19, 199, "WindProb", "Damaging Thunderstorm Wind Probability", "%");
    add(0, 19, 200, "XtrmTornProb", "Extreme Tornado Probability", "%");
    add(0, 19, 201, "XtrmHailProb", "Extreme Hail Probability", "%");
    add(0, 19, 202, "XtrmWindProb", "Extreme Thunderstorm Wind Probability", "%");
    add(0, 19, 215, "TotalSvrProb", "Total Probability of Severe Thunderstorms", "%");
    add(0, 19, 216, "TotalXtrmProb", "Total Probability of Extreme Severe Thunderstorms", "%");
    add(0, 19, 217, "WWA", "Watch Warning Advisory", "");

    /* Leaving next two lines in for grandfathering sake. 9/19/2007... Probably can remove in future. */
    add(0, 19, 203, "TotalSvrProb", "Total Probability of Severe Thunderstorms", "%");
    add(0, 19, 204, "TotalXtrmProb", "Total Probability of Extreme Severe Thunderstorms", "%");
    add(0, 192, 192, "FireWx", "Critical Fire Weather", "%");
    add(0, 192, 194, "DryLightning", "Dry Lightning", "%");

    /* Arthur Added this to both NDFD and NCEP local tables. (5/1/2006) */
    add(10, 3, 192, "Surge", "Hurricane Storm Surge", "m");
    add(10, 3, 193, "ETSurge", "Extra Tropical Storm Surge", "m");
  }

  private void add(int discipline, int category, int number, String abbrev, String name, String unit) {
    local.put(makeParamId(discipline, category, number), new Grib2Parameter(discipline, category, number, name, unit, abbrev, null));
  }

}
