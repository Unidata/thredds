package ucar.nc2.grib.grib2.table;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import javax.annotation.Nullable;

/**
 * ECMWF code tables read from resources/grib2/ecmwf/tables/<latest>.
 * EcmwfCodeTableCompare is used to compare with WMO.
 * Notes on important differences, that should get reflected in Grib2Customizer:
 <pre>

 1) Code table 1.0 - GRIB master tables version number
    Ecmwf version 21 discipline 1 category 0 (resources/grib2/ecmwf/tables/21/1.0.table)
      p1=       255 Missing
      p2=       255 Master tables not used. Local table entries and local templates may use the entire range of the table, not just
                    those sections marked Reserved for local used.

 2) Code table 3.1 - Grid definition template number
    Ecmwf version 21 discipline 3 category 1 (resources/grib2/ecmwf/tables/21/3.1.table)
      WMO 3.1 missing EcmwfEntry{codeValue=12, name=Transverse Mercator}
      WMO 3.1 missing EcmwfEntry{codeValue=130, name=Irregular latitude/longitude grid}

 3) Code table 4.0 - Product definition template number
    Ecmwf version 21 discipline 4 category 0 (resources/grib2/ecmwf/tables/21/4.0.table)
     WMO 4.0 missing EcmwfEntry{codeValue=311, name=Satellite product auxiliary information}
     WMO 4.0 missing EcmwfEntry{codeValue=40033, name=Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer at a point in time for simulated}
     WMO 4.0 missing EcmwfEntry{codeValue=40034, name=Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer, in a continuous or non-continuous interval for simulated}
     WMO 4.0 missing EcmwfEntry{codeValue=50001, name=Forecasting Systems with Variable Resolution in a point in time}
     WMO 4.0 missing EcmwfEntry{codeValue=50011, name=Forecasting Systems with Variable Resolution in a continous or non countinous time interval}

 4) Code table 4.230 - Atmospheric chemical constituent type
    Ecmwf version 21 discipline 4 category 230 (resources/grib2/ecmwf/tables/21/4.230.table)
      WMO 4.230 missing EcmwfEntry{codeValue=0, name=Ozone O3}
      ... (all of them?)
    Same with
      Code table 4.233 - Aerosol type

 5) Code table 5.0 - Data representation template number
    Ecmwf version 21 discipline 5 category 0 (resources/grib2/ecmwf/tables/21/5.0.table)
     WMO 5.0 missing EcmwfEntry{codeValue=6, name=Grid point data - simple packing with pre-processing}
     WMO 5.0 missing EcmwfEntry{codeValue=40000, name=JPEG2000 Packing}
     WMO 5.0 missing EcmwfEntry{codeValue=40010, name=PNG pacling}
     WMO 5.0 missing EcmwfEntry{codeValue=50000, name=Sperical harmonics ieee packing}
     WMO 5.0 missing EcmwfEntry{codeValue=50001, name=Second order packing}
     WMO 5.0 missing EcmwfEntry{codeValue=50002, name=Second order packing}

 6) Code table 6.0 - Bit map indicator
    Ecmwf version 21 discipline 6 category 0 (resources/grib2/ecmwf/tables/21/6.0.table)
      WMO 6.0 missing EcmwfEntry{codeValue=1, name=A bit map pre-determined by the originating/generating centre applies to
              this product and is not specified in this Section}

 7)
   No WMO table that matches ECMWF table 4.192
   No WMO table that matches ECMWF table 5.40000
   No WMO table that matches ECMWF table 5.50002
   Unparsed table stepType.table
 </pre>
 */
public class EcmwfCodeTable implements Grib2CodeTableInterface {
  private static final boolean debugOpen = false;
  private static final boolean debug = false;
  private static final String PATH = "resources/grib2/ecmwf/tables";

  private String title;
  private String source;
  private String tableName;

  private final int version, discipline, category;
  private final ImmutableMap<Integer, Entry> paramMap;

  public static EcmwfCodeTable factory(int version, int discipline, int category) {
    try {
      return new EcmwfCodeTable(version, discipline, category);
    } catch (Exception e) {
      return null;
    }
  }

  private EcmwfCodeTable(int version, int discipline, int category) throws IOException {
    this.version = version;
    this.discipline = discipline;
    this.category = category;

    this.paramMap = this.readTable(getTablePath());
  }

  @Override
  public String getName() {
    return String.format("Ecmwf version %d discipline %d category %d (%s)", version, discipline, category, getTablePath());
  }

  @Override
  public String getShortName() {
    return String.format("Ecmwf version %d (%d-%d)", version, discipline, category);
  }

  @Override
  public ImmutableList<Entry> getEntries() {
    return paramMap.values().stream().sorted().collect(ImmutableList.toImmutableList());
  }

  @Override
  @Nullable
  public Entry getEntry(int code) {
    return paramMap.get(code);
  }

  private String getTablePath() {
    return String.format("%s/%d/%d.%d.table", PATH, version, discipline, category);
  }

  /*
  # Code table 4.2 - Parameter number by product discipline and parameter category
  0 0 Estimated precipitation (kg m-2)
  1 1 Instantaneous rain rate (kg m-2 s-1)
  2 2 Cloud top height (m)
  3 3 Cloud top height quality indicator (Code table 4.219)
  4 4 Estimated u-component of wind (m/s)
  5 5 Estimated v-component of wind (m/s)
  6 6 Number of pixel used (Numeric)
  7 7 Solar zenith angle (deg)
  8 8 Relative azimuth angle (deg)
  9 9 Reflectance in 0.6 micron channel (%)
  10 10 Reflectance in 0.8 micron channel (%)
  11 11 Reflectance in 1.6 micron channel (%)
  12 12 Reflectance in 3.9 micron channel (%)
  13 13 Atmospheric divergence (/s)
  14 14 Cloudy brightness temperature (K)
  15 15 Clear-sky brightness temperature (K)
  16 16 Cloudy radiance (with respect to wave number) (W m-1 sr-1)
  17 17 Clear-sky radiance (with respect to wave number) (W m-1 sr-1)
  18 18 Reserved
  19 19 Wind speed (m/s)
  20 20 Aerosol optical thickness at 0.635 um
  21 21 Aerosol optical thickness at 0.810 um
  22 22 Aerosol optical thickness at 1.640 um
  23 23 Angstrom coefficient
  # 24-26 Reserved
  27 27 Bidirectional reflectance factor (numeric)
  28 28 Brightness temperature (K)
  29 29 Scaled radiance (numeric)
  # 30-191 Reserved
  # 192-254 Reserved for local use
  255 255 Missing
   */
  private ImmutableMap<Integer, Entry> readTable(String path) throws IOException {
    ImmutableMap.Builder<Integer, Entry> builder = ImmutableMap.builder();

    if (debugOpen) {
      System.out.printf("readEcmwfTable path= %s%n", path);
    }

    ClassLoader cl = Grib2Table.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Cant find " + path);
      }
      try (BufferedReader dataIS = new BufferedReader(
          new InputStreamReader(is, Charset.forName("UTF8")))) {
        int count = 0;
        while (true) {
          String line = dataIS.readLine();
          if (line == null) {
            break;
          }
          if (line.startsWith("#") || line.trim().length() == 0) {
            continue;
          }
          count++;

          int posBlank1 = line.indexOf(' ');
          int posBlank2 = line.indexOf(' ', posBlank1 + 1);
          int lastParen = line.lastIndexOf('(');

          String num1 = line.substring(0, posBlank1).trim();
          String num2 = line.substring(posBlank1 + 1, posBlank2);
          String desc = (lastParen > 0) ? line.substring(posBlank2 + 1, lastParen).trim()
              : line.substring(posBlank2 + 1).trim();

          if (!num1.equals(num2)) {
            if (debug) {
              System.out.printf("*****num1 != num2 for %s%n", line);
            }
            continue;
          }
          int code = Integer.parseInt(num1);

          EcmwfEntry entry = new EcmwfEntry(code, desc);
          builder.put(entry.getCode(), entry);
          if (debug) {
            System.out.printf(" %s%n", entry);
          }
        }
      }
    }
    return builder.build();
  }

  private class EcmwfEntry implements Entry, Comparable<Entry> {
    private final int codeValue;
    private final String name;

    public EcmwfEntry(int codeValue, String name) {
      this.codeValue = codeValue;
      this.name = name;
    }

    @Override
    public int getCode() {
      return codeValue;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int compareTo(Entry o) {
      return codeValue - o.getCode();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("codeValue", codeValue)
          .add("name", name)
          .toString();
    }
  }

}
