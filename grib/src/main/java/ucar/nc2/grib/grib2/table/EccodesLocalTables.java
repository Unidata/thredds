package ucar.nc2.grib.grib2.table;

import static ucar.nc2.grib.grib2.table.EccodesCodeTable.LATEST_VERSION;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;

/**
 * The results of EcmwfParamTableCompare indicate there are no significant differences of the parameter tables with WMO.
 * The results of EcmwfCodeTableCompare indicate there are some differences of the code tables with WMO.
 * See the comments at in EcmwfCodeTable.java.
 *
 * 1) This change in ECMWF table 1.0 implies that if master table = 255, then all WMO entries may be overridden.
 *       Havent seen this in practice; in particular I wonder if any ECMWF GRIB2 files do this.
 *       LOOK: This is not implemented.
 *
 *     Code table 1.0 - GRIB master tables version number
 *     Ecmwf version 21 discipline 1 category 0 (resources/grib2/ecmwf/tables/21/1.0.table)
 *       WMO=       255 Missing
 *       ECMWF=     255 Master tables not used. Local table entries and local templates may use the entire range of the table, not just
 *                     those sections marked Reserved for local used.
 *
 * 2) Despite missing code entry 3.1.12, there is a WMO 3.12 template. However none for 3.130.
 *    I dont yet see templates defined by ECMWF. So, not implemented.
 *
 *     Code table 3.1 - Grid definition template number
 *     Ecmwf version 21 discipline 3 category 1 (resources/grib2/ecmwf/tables/21/3.1.table)
 *       WMO 3.1 missing EcmwfEntry{codeValue=12, name=Transverse Mercator}
 *       WMO 3.1 missing EcmwfEntry{codeValue=130, name=Irregular latitude/longitude grid}
 *
 * 3)  LOOK: none of these are implemented.
 *     Code table 4.0 - Product definition template number
 *     Ecmwf version 21 discipline 4 category 0 (resources/grib2/ecmwf/tables/21/4.0.table)
 *      WMO 4.0 missing EcmwfEntry{codeValue=311, name=Satellite product auxiliary information}
 *      WMO 4.0 missing EcmwfEntry{codeValue=40033, name=Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer at a point in time for simulated}
 *      WMO 4.0 missing EcmwfEntry{codeValue=40034, name=Individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer, in a continuous or non-continuous interval for simulated}
 *      WMO 4.0 missing EcmwfEntry{codeValue=50001, name=Forecasting Systems with Variable Resolution in a point in time}
 *      WMO 4.0 missing EcmwfEntry{codeValue=50011, name=Forecasting Systems with Variable Resolution in a continous or non countinous time interval}
 *
 *  4) Implemented below.
 *     Code table 4.230 - Atmospheric chemical constituent type
 *     Ecmwf version 21 discipline 4 category 230 (resources/grib2/ecmwf/tables/21/4.230.table)
 *       WMO 4.230 missing EcmwfEntry{codeValue=0, name=Ozone O3}
 *       ... (all of them?)
 *     Same with
 *       Code table 4.233 - Aerosol type
 *
 *  5) LOOK: not implemented.
 *     Code table 5.0 - Data representation template number
 *     Ecmwf version 21 discipline 5 category 0 (resources/grib2/ecmwf/tables/21/5.0.table)
 *      WMO 5.0 missing EcmwfEntry{codeValue=6, name=Grid point data - simple packing with pre-processing}
 *      WMO 5.0 missing EcmwfEntry{codeValue=40000, name=JPEG2000 Packing}
 *      WMO 5.0 missing EcmwfEntry{codeValue=40010, name=PNG pacling}
 *      WMO 5.0 missing EcmwfEntry{codeValue=50000, name=Sperical harmonics ieee packing}
 *      WMO 5.0 missing EcmwfEntry{codeValue=50001, name=Second order packing}
 *      WMO 5.0 missing EcmwfEntry{codeValue=50002, name=Second order packing}
 *
 *  6) LOOK not implemented: So where is it specified?
 *     Code table 6.0 - Bit map indicator
 *     Ecmwf version 21 discipline 6 category 0 (resources/grib2/ecmwf/tables/21/6.0.table)
 *       WMO 6.0 missing EcmwfEntry{codeValue=1, name=A bit map pre-determined by the originating/generating centre applies to
 *               this product and is not specified in this Section}
 *
 *  7) Implemented below.
 *    No WMO table that matches ECMWF table 4.192
 *    No WMO table that matches ECMWF table 5.40000
 *    No WMO table that matches ECMWF table 5.50002
 */
public class EccodesLocalTables extends LocalTables {
  private static final Logger logger = LoggerFactory.getLogger(EccodesLocalTables.class);
  private static final String RESOURCE_DIRECTORY = "resources/grib2/ecmwf/tables/21";
  private static final String MATCH = "4.2.";

  private static final boolean debugOpen = false;
  private static final boolean debug = true;

  private final Map<Integer, EccodesParamTable> tableMap = new HashMap<>(30);

  // Each of the centers using eccodes has a seperate EccodesLocalTables with its own Grib2TableConfig, and center value.
  EccodesLocalTables(Grib2TableConfig config) {
    super(config);
    // LOOK: change to lazy init
    initParams(config.getPath());
  }

  // These are the tables that have been augmented
  private ImmutableSet<String> whitelist = ImmutableSet.of("4.230", "4.233", "4.192", "5.40000", "5.50002");
  private Map<String, EccodesCodeTable> localTables = new HashMap<>();

  @Override
  @Nullable
  public String getCodeTableValue(String tableName, int code) {
    if (!whitelist.contains(tableName)) {
      return super.getCodeTableValue(tableName, code);
    }
    if (localTables.containsKey(tableName)) {
      Grib2CodeTableInterface.Entry entry = localTables.get(tableName).getEntry(code);
      return (entry == null) ? null : entry.getName();
    }

    Iterator<String> tokens = Splitter.on('.')
        .trimResults()
        .omitEmptyStrings()
        .split(tableName)
        .iterator();

    int discipline = Integer.parseInt(tokens.next());
    int category = Integer.parseInt(tokens.next());
    EccodesCodeTable ecmwfCodeTable = EccodesCodeTable.factory(LATEST_VERSION, discipline, category);
    localTables.put(tableName, ecmwfCodeTable);

    Grib2CodeTableInterface.Entry entry = ecmwfCodeTable.getEntry(code);
    return (entry == null) ? null : entry.getName();
  }

  EccodesLocalConcepts localConcepts;
  ImmutableListMultimap<Integer, Grib2Parameter> localConceptMultimap;
  private void initParams(String path) {
    try {
      localConcepts = new EccodesLocalConcepts(path);
      localConceptMultimap = localConcepts.getLocalConceptMultimap();
      for (Map.Entry<Integer, Collection<Grib2Parameter>> entry : localConceptMultimap.asMap().entrySet()) {
        if (isLocal(entry.getKey())) {
          localParams.put(entry.getKey(), entry.getValue().iterator().next());
        }
      }
    } catch (IOException e) {
      logger.warn("EccodesLocalTables failed on %s", path, e);
    }
  }

  @Override
  public ImmutableList<Parameter> getParameters() {
    return localConceptMultimap.values().stream().map(p -> (Parameter) p).sorted(new ParameterSort()).collect(ImmutableList.toImmutableList());
  }

  @Override
  public void showDetails(Formatter f) {
    localConcepts.showDetails(f);
  }

  @Override
  public void showEntryDetails(Formatter f, List<GribTables.Parameter> params) {
    localConcepts.showEntryDetails(f, params);
  }

}
