package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;

public interface Grib2CodeTableInterface {
  String getName();
  String getShortName();
  ImmutableList<Entry> getEntries();

  /**
   * Find the Entry in this table with the given code.
   * @param code unsigned short.
   */
  Entry getEntry(int code);

  interface Entry {
    /** Unsigned short */
    int getCode();
    String getName();
  }
}
