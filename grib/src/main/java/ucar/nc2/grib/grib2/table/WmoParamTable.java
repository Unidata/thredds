package ucar.nc2.grib.grib2.table;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable.WmoEntry;

public class WmoParamTable implements Grib2ParamTableInterface {

  @Nullable
  public static GribTables.Parameter getParameter(int discipline, int category, int number) {
    // Look rather inefficient.
    WmoParamTable paramTable = WmoCodeFlagTables.getInstance().getParamTable(discipline, category);
    return paramTable == null ? null : paramTable.getParameter(number);
  }

  @Nullable
  public static String getParameterName(int discipline, int category, int number) {
    // Look rather inefficient.
    WmoParamTable paramTable = WmoCodeFlagTables.getInstance().getParamTable(discipline, category);
    GribTables.Parameter param = paramTable == null ? null : paramTable.getParameter(number);
    return (param == null) ? null : param.getName();
  }

  private final WmoTable wmoTable;
  private final ImmutableMap<Integer, GribTables.Parameter> entryMap;

  WmoParamTable(WmoTable wmoTable) {
    Preconditions.checkNotNull(wmoTable);
    Preconditions.checkArgument(wmoTable.getType() == TableType.param);
    this.wmoTable = wmoTable;
    ImmutableMap.Builder<Integer, GribTables.Parameter> builder = ImmutableMap.builder();
    wmoTable.getEntries().forEach(entry -> builder.put(entry.getNumber(), new WmoParamEntry(entry)));
    this.entryMap = builder.build();
  }

  @Override
  public String getName() {
    return wmoTable.getName();
  }

  @Override
  public String getShortName() {
    return wmoTable.getName();
  }

  @Override
  public ImmutableList<GribTables.Parameter> getParameters() {
    return ImmutableList.copyOf(entryMap.values());
  }

  @Nullable
  @Override
  public GribTables.Parameter getParameter(int number) {
    return entryMap.get(number);
  }

  private class WmoParamEntry implements GribTables.Parameter {
    private WmoTable.WmoEntry entry;

    public WmoParamEntry(WmoEntry entry) {
      this.entry = entry;
    }

    @Override
    public int getDiscipline() {
      return entry.getDiscipline();
    }

    @Override
    public int getCategory() {
      return entry.getCategory();
    }

    @Override
    public int getNumber() {
      return entry.getNumber();
    }

    @Override
    public String getName() {
      return entry.getName();
    }

    @Override
    public String getUnit() {
      return entry.getUnit();
    }

    @Nullable
    @Override
    public String getAbbrev() {
      return null;
    }

    @Override
    public String getDescription() {
      return entry.getName();
    }

    @Override
    public String getId() {
      return entry.getId();
    }

    @Nullable
    @Override
    public Float getFill() {
      return null;
    }

    @Override
    public Float getMissing() {
      return Float.NaN;
    }

    @Override
    public String getOperationalStatus() {
      return entry.getStatus();
    }
  }

}
