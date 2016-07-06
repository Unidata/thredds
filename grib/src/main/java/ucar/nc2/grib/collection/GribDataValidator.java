package ucar.nc2.grib.collection;

import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grib.GribTables;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * For debugging
 */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, SubsetParams coords) throws IOException;
}
