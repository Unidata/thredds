package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.DiskCache2;

interface PointDataWriterFactory {

	public PointDataWriter createPointDataWriter(OutputStream os, DiskCache2 diskCache);
}
