package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;

public interface Grib2FlagTableInterface {
  String getName();
  String getShortName();
  ImmutableList<Entry> getEntries();

  /**
   * Find the Entry in this table with the given code.
   * @param code unsigned short.
   */
  Grib2FlagTableInterface.Entry getEntry(int code);

  interface Entry {
    /** Unsigned short */
    int getCode();

    ImmutableList<Integer> getValues();

    String getName(int value);
  }
}
