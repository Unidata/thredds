package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.NetcdfFileWriter;

interface PointDataWriterFactory {

	public PointDataWriter createPointDataWriter(OutputStream os);
}
