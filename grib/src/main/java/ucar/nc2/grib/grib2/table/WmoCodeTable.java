package ucar.nc2.grib.grib2.table;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable.WmoEntry;

public class WmoCodeTable implements Grib2CodeTableInterface {
  private final WmoTable wmoTable;
  private final ImmutableMap<Integer, WmoCodeEntry> entryMap;

  WmoCodeTable(WmoTable wmoTable) {
    Preconditions.checkNotNull(wmoTable);
    Preconditions.checkArgument(wmoTable.getType() == TableType.code || wmoTable.getType() == TableType.cat);
    this.wmoTable = wmoTable;
    ImmutableMap.Builder<Integer, WmoCodeEntry> builder = ImmutableMap.builder();
    wmoTable.getEntries().forEach(e -> builder.put(e.getNumber(), new WmoCodeEntry(e)));
    this.entryMap = builder.build();
  }

  @Override
  public String getName() {
    return wmoTable.getName();
  }

  @Override
  public String getShortName() {
    return "WMO " + wmoTable.getId();
  }

  @Override
  public ImmutableList<Entry> getEntries() {
    return ImmutableList.copyOf(entryMap.values());
  }

  @Override
  public Entry getEntry(int codeValue) {
    return entryMap.get(codeValue);
  }

  private static class WmoCodeEntry implements Grib2CodeTableInterface.Entry {
    private final WmoTable.WmoEntry entry;

    public WmoCodeEntry(WmoEntry entry) {
      this.entry = entry;
    }

    @Override
    public int getCode() {
      return entry.getNumber();
    }

    @Override
    public String getName() {
      return entry.getName();
    }
  }
}
