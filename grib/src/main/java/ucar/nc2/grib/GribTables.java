/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import javax.annotation.Nullable;
import ucar.nc2.grib.coord.VertCoordType;

/**
 * Abstraction of GribTable for Grib Collections.
 * Allows Grin1 and Grib2 to be handles through common interface.
 *
 * @author John
 * @since 9/5/11
 */
public interface GribTables {

  @Nullable
  String getSubCenterName(int center, int subcenter);

  String getLevelNameShort(int code);

  @Nullable
  GribStatType getStatType(int intvType);

  VertCoordType getVertUnit(int code);

  @Nullable
  String getGeneratingProcessName(int code);

  @Nullable
  String getGeneratingProcessTypeName(int code);

  interface Parameter {
    /** Unsigned byte */
    int getDiscipline();

    /** Unsigned byte */
    int getCategory();

    /** Unsigned byte */
    int getNumber();

    String getName();

    String getUnit();

    @Nullable
    String getAbbrev();

    String getDescription();

    /** Unique across all Parameter tables */
    String getId();

    @Nullable
    Float getFill();

    Float getMissing();

    @Nullable
    String getOperationalStatus();
  }

}
