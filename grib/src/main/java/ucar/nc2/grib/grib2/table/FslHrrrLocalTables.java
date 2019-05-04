/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * FSL/GSD (center 59)
 * genProcessId 125 = HRRR
 *
 * @author caron
 * @see "http://ruc.noaa.gov/hrrr/GRIB2Table.txt"
 * @since 2/1/12
 */
public class FslHrrrLocalTables extends NcepLocalTables {
  public static final int center_id = 59;

  FslHrrrLocalTables(Grib2TableConfig config) {
    super(config);   // default resource path
    initLocalTable(null);
  }

  @Override
  public String getParamTablePathUsedFor(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getParamTablePathUsedFor(discipline, category, number);
    return config.getPath();
  }

  @Override
  public ImmutableList<Parameter> getParameters() {
    return getLocalParameters();
  }
  
  @Override
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
    return localParams.get(makeParamId(discipline, category, number));
   }

  // LOOK  maybe combine grib1, grib2 and bufr ??
  @Override
  public String getSubCenterName(int center, int subcenter) {

    switch (subcenter) {
      case 0:
        return null;
      case 1:
        return "FSL/FRD Regional Analysis and Prediction Branch";
      case 2:
        return "FSL/FRD Local Analysis and Prediction Branch";
    }
    return super.getSubCenterName(center, subcenter);
  }

  @Override
  public String getGeneratingProcessName(int genProcess) {
    switch (genProcess) {
      case 103:
        return "ExREF";
      case 105:
        return "RAP/ RUC";
      case 106:
        return "Developmental Testbed Center Winter Field Experiment, WRF-ARW";
      case 112:
        return "Developmental Testbed Center Winter Field Experiment, WRF-NMM";
      case 116:
        return "Flow-following Finite-volume Icosahedral Model (FIM)";
      case 125:
        return "High-Resolution Rapid Refresh";
      default:
        return null;
    }
  }

  @Override
  public String getLevelNameShort(int id) {
    if (id == 200) {
      return "Entire_atmosphere";
    }
    return super.getLevelNameShort(id);
  }

  @Override

  public String getLevelName(int id) {
    if (id == 200) {
      return "Entire atmosphere layer";
    }
    return super.getLevelName(id);
  }

  public String getStatisticNameShort(int id) {
    if (id == 255) {
      return "Interval";
    }
    return super.getStatisticNameShort(id);
  }

  private void initLocalTable(Formatter f) {
    localParams = readCsv(config.getPath(), f);
  }

  // debugging
  @Override
  public void lookForProblems(Formatter f) {
    initLocalTable(f);
  }

  private Map<Integer, Grib2Parameter> readCsv(String resourcePath, Formatter f) {
    boolean header = true;
    Map<Integer, Grib2Parameter> result = new HashMap<>(100);

    ClassLoader cl = getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is == null) throw new IllegalStateException("Cant find " + resourcePath);
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset))) {
        HashMap<String, Grib2Parameter> names = new HashMap<>(200);

        while (true) {
          String line = br.readLine();
          if (line == null) {
            break;
          }
          if (line.startsWith("Record")) {
            break;
          }
        }

        while (true) {
          String line = br.readLine();
          if (line == null) {
            break;
          }
          if ((line.length() == 0) || line.startsWith("#")) {
            continue;
          }
          String[] flds = line.split(",");

          //RecordNumber,	TableNumber,	DisciplineNumber,	CategoryNumber,	ParameterNumber,	WGrib2Name,	NCLName,				FieldType,			Description,													Units,
          String recordNumber = flds[0].trim();
          int tableNumber = Integer.parseInt(flds[1].trim());
          int disciplineNumber = Integer.parseInt(flds[2].trim());
          int categoryNumber = Integer.parseInt(flds[3].trim());
          int parameterNumber = Integer.parseInt(flds[4].trim());

          String WGrib2Name = flds[5].trim();
          String NCLName = flds[6].trim();
          String FieldType = flds[7].trim();
          String Description = flds[8].trim();
          String Units = flds[9].trim();
          if (f != null) {
            f.format("%3s %3d %3d %3d %3d %-10s %-25s %-30s %-100s %-20s%n", recordNumber,
                tableNumber, disciplineNumber, categoryNumber, parameterNumber,
                WGrib2Name, NCLName, FieldType, Description, Units);
          }

          String name = !WGrib2Name.equals("var") ? WGrib2Name : FieldType;
          Grib2Parameter s = new Grib2Parameter(disciplineNumber, categoryNumber, parameterNumber,
              name, Units, null, Description);
          // s.desc = Description;
          result.put(makeParamId(disciplineNumber, categoryNumber, parameterNumber), s);
          if (f != null) {
            f.format(" %s%n", s);
          }
          if (categoryNumber > 191 || parameterNumber > 191) {
            Grib2Parameter dup = names.get(s.getName());
            if (dup != null && f != null) {
              if (header) {
                f.format("Problems in table %s %n", resourcePath);
              }
              header = false;
              f.format(" DUPLICATE NAME %s and %s (%s)%n", s.getId(), dup.getId(), s.getName());
            }
          }
          names.put(s.getName(), s);
        }
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return result;
  }
}
