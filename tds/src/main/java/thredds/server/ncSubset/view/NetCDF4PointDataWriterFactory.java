package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.NetcdfFileWriter;

public class NetCDF4PointDataWriterFactory implements PointDataWriterFactory {

	private static NetCDF4PointDataWriterFactory INSTANCE;

	private static final NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf4; 
	
	private NetCDF4PointDataWriterFactory(){
	}
	
	public static NetCDF4PointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new NetCDF4PointDataWriterFactory(); 
		}
		
		return INSTANCE;
	} 
		
	@Override
	public PointDataWriter createPointDataWriter( OutputStream os) {
		return NetCDFPointDataWriter.createNetCDFPointDataWriter(version, os);
	}
	
	public NetcdfFileWriter.Version getVersion(){
		return version;
	}
	
}
