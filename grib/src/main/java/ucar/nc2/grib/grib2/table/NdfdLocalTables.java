/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
   * Sourced from netcdf-java v5 (https://github.com/Unidata/netcdf-java/pull/601)
   * Up-to-date with degrib Version 2.25 09/21/2020
   */
  private void init() {
    add(0, 0, 193, "ApparentT", "Apparent Temperature", "K");
    add(0, 0, 205, "WBGT", "Wet Bulb Globe Temperature", "K");
    add(0, 1, 192, "Wx", "Weather string", "");
    add(0, 1, 193, "PPI", "Precipitation Potential Index", "%");
    add(0, 1, 227, "IceAccum", "Ice Accumulation", "kg/m^2");

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
    add(0, 19, 235, "TCWind", "Tropical Cyclone Wind Threat", "0=none; 4=low; 6=moderate; 8=high; 10=extreme");
    add(0, 19, 236, "TCSurge", "Tropical Cyclone Storm Surge Threat", "0=none; 4=low; 6=moderate; 8=high; 10=extreme");
    add(0, 19, 238, "TCRain", "Tropical Cyclone Flooding Rain Threat", "0=none; 4=low; 6=moderate; 8=high; 10=extreme");
    add(0, 19, 239, "TCTornado", "Tropical Cyclone Tornado Threat", "0=none; 4=low; 6=moderate; 8=high; 10=extreme");
    add(0, 19, 246, "SNOWLVL", "Snow Level", "m");

    /* Leaving next two lines in for grandfathering sake. 9/19/2007... Probably can remove in future. */
    add(0, 19, 203, "TotalSvrProb", "Total Probability of Severe Thunderstorms", "%");
    add(0, 19, 204, "TotalXtrmProb", "Total Probability of Extreme Severe Thunderstorms", "%");
    add(0, 192, 192, "FireWx", "Critical Fire Weather", "%");
    add(0, 192, 194, "DryLightning", "Dry Lightning", "%");

    /* Mike added 1/13 */
    add(2, 1, 192, "CANL", "Cold Advisory for Newborn Livestock",
        "0=none; 2=slight; 4=mild; 6=moderate; 8=severe; 10=extreme");

    /* Arthur Added this to both NDFD and NCEP local tables. (5/1/2006) */
    add(10, 3, 192, "Surge", "Hurricane Storm Surge", "m");
    add(10, 3, 193, "ETSurge", "Extra Tropical Storm Surge", "m");
    add(10, 3, 250, "StormTide", "Storm Surge and Tide", "m");
    add(10, 3, 251, "Tide", "Tide", "m");
    /* Mike added 2/2012 */
    add(0, 1, 198, "MinRH", "Minimum Relative Humidity", "%");
  }

  private void add(int discipline, int category, int number, String abbrev, String name, String unit) {
    local.put(makeParamId(discipline, category, number), new Grib2Parameter(discipline, category, number, name, unit, abbrev, null));
  }

}
