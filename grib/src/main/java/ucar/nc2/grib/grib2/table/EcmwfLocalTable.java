package ucar.nc2.grib.grib2.table;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.nc2.grib.grib2.table.EcmwfParamTable;

public class EcmwfLocalTable {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(
      EcmwfParamTable.class);
  private static final String MATCH = "4.2.";

  private static final boolean debugOpen = false;
  private static final boolean debug = true;

  private final Map<Integer, EcmwfParamTable> tableMap = new HashMap<>(30);
  private final String resourcePath;

  EcmwfLocalTable(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  @Nullable
  public Grib2Parameter getParameter(int discipline, int category, int number) throws IOException {
    int key = (discipline << 8) + category;
    EcmwfParamTable params = tableMap.get(key);
    if (params == null) {
      params = factory(discipline, category);
      if (params == null)
        return null;
      tableMap.put(key, params);
    }
    return params.getParameter(number);
  }

  @Nullable
  public String getCategory(int discipline, int category) {
    int key = (discipline << 8) + category;
    EcmwfParamTable params = tableMap.get(key);
    return (params == null) ? null : params.getName();
  }

  @Nullable
  private EcmwfParamTable factory(int discipline, int category) throws IOException {
    return EcmwfParamTable.factory(21, discipline, category);
  }

  private String getTablePath(int discipline, int category) {
    return resourcePath + "4.2." + discipline + "." + category + ".table";
  }
}
