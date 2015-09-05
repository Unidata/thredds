package ucar.nc2.grib.collection;

import ucar.nc2.grib.GribTables;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Map;

/**
 * Created by snake on 9/3/2015.
 */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, Map<String, Object> coords) throws IOException;
}
