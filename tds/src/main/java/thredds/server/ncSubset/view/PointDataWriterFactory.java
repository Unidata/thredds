package thredds.server.ncSubset.view;

import java.io.OutputStream;

interface PointDataWriterFactory {

	public PointDataWriter createPointDataWriter(OutputStream os);
}
