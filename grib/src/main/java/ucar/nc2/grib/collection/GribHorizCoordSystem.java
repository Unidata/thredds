/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import ucar.nc2.grib.GdsHorizCoordSys;

import javax.annotation.concurrent.Immutable;

/**
 * Encapsolates the GdsHorizCoordSys; shared by the GroupHcs
 *
 * @author caron
 * @since 11/10/2014
 */
@Immutable
public class GribHorizCoordSystem {
  private final GdsHorizCoordSys hcs;
  private final byte[] rawGds;         // raw gds: Grib1SectionGridDefinition or Grib2SectionGridDefinition
  private final Object gdsHash;
  private final String id, description;
  private final int predefinedGridDefinition;  // grib1

  GribHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, Object gdsHash, String id,
      String description, int predefinedGridDefinition) {
    this.hcs = hcs;
    this.rawGds = rawGds;
    this.gdsHash = gdsHash;
    this.predefinedGridDefinition = predefinedGridDefinition;

    this.id = id;
    this.description = description;
  }

  public GdsHorizCoordSys getHcs() {
    return hcs;
  }

  public byte[] getRawGds() {
    return rawGds;
  }

  // use this object for hashmaps
  public Object getGdsHash() {
    return gdsHash;
  }

  // unique name for Group
  public String getId() {
    return id;
  }

  // human readable
  public String getDescription() {
    return description;
  }

  public int getPredefinedGridDefinition() {
    return predefinedGridDefinition;
  }

}
