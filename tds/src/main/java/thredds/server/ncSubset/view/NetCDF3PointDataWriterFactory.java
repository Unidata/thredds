package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.DiskCache2;

class NetCDF3PointDataWriterFactory implements PointDataWriterFactory {

	private static NetCDF3PointDataWriterFactory INSTANCE;

	private static final NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3; 
	
	private NetCDF3PointDataWriterFactory(){
	}
	
	public static NetCDF3PointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new NetCDF3PointDataWriterFactory(); 
		}
		
		return INSTANCE;
	} 
		
	@Override
	public PointDataWriter createPointDataWriter( OutputStream os, DiskCache2 diskCache) {
		return NetCDFPointDataWriter.createNetCDFPointDataWriter(version, os, diskCache);
	}
	
	public NetcdfFileWriter.Version getVersion(){
		return version;
	}
	
}
