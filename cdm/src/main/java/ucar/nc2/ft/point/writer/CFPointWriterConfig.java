/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;

/**
 * Configuration for CFPointWriter
 *
 * @author caron
 * @since 6/23/2014
 */
public class CFPointWriterConfig {
  public NetcdfFileWriter.Version version;        // netcdf file version
  public Nc4Chunking chunking;                    // for netcdf-4
  public boolean noTimeCoverage = false;          // does not have a time dimension
  public int recDimensionLength = -1;             // do use unlimited dimension (for netcdf3), use fixed dimension of this length NOT USED

  public CFPointWriterConfig(NetcdfFileWriter.Version version) {
    this(version, new Nc4ChunkingDefault());  // The default chunker used in Nc4Iosp.
  }

  public CFPointWriterConfig(NetcdfFileWriter.Version version, Nc4Chunking chunking) {
    this.version = version;
    this.chunking = chunking;
  }

  public CFPointWriterConfig setNoTimeCoverage(boolean noTimeCoverage)  {
    this.noTimeCoverage = noTimeCoverage;
    return this;
  }
}
