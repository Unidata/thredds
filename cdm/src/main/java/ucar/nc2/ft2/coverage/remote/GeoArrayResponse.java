/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.remote;

import ucar.ma2.DataType;

import java.nio.ByteOrder;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/16/2015
 */
class GeoArrayResponse {

  String coverageName;
  DataType dataType;

  public ByteOrder byteOrder;
  public boolean deflate;
  public long uncompressedSize;
  public int[] shape;
  public List<String> axisName;
  public String coordSysName;

}
