package thredds.server.ncSubset.view.gridaspoint;

import thredds.server.ncSubset.format.SupportedFormat;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.DiskCache2;

import java.io.OutputStream;

/**
 * Describe
 *
 * @author caron
 * @since 10/5/13
 */
public class PointDataWriterFactory {

  static public PointDataWriter factory(SupportedFormat supportedFormat, OutputStream outputStream, DiskCache2 diskCache) {

 		if( supportedFormat.getFormatName().equals("XML") ){
 		  return XMLPointDataWriter.factory(outputStream);
 		}

 		if( supportedFormat.getFormatName().equals("NETCDF3") ){
      return NetCDFPointDataWriter.factory(NetcdfFileWriter.Version.netcdf3, outputStream, diskCache);
 		}

 		if( supportedFormat.getFormatName().equals("NETCDF4") ){
      return NetCDFPointDataWriter.factory(NetcdfFileWriter.Version.netcdf4, outputStream, diskCache);
 		}

 		if( supportedFormat.getFormatName().equals("CSV") ){
      return CSVPointDataWriter.factory(outputStream);
 		}

 		return null;

 	}
}
