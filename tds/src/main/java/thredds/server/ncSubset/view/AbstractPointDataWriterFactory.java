package thredds.server.ncSubset.view;

import thredds.server.ncSubset.format.SupportedFormat;

final class AbstractPointDataWriterFactory {

	private AbstractPointDataWriterFactory(){}
	
	static final PointDataWriterFactory createPointDataWriterFactory(SupportedFormat supportedFormat){
		
		if( supportedFormat.getFormatName().equals("XML") ){
			return XMLPointDataWriterFactory.getInstance() ;
		}
		
		if( supportedFormat.getFormatName().equals("NETCDF3") ){
			return NetCDF3PointDataWriterFactory.getInstance() ;
		}
		
		if( supportedFormat.getFormatName().equals("NETCDF4") ){
			return NetCDF4PointDataWriterFactory.getInstance() ;
		}		
		
		if( supportedFormat.getFormatName().equals("CSV") ){
			return CSVPointDataWriterFactory.getInstance() ;
		}
		
		return null;
		
	}
}
