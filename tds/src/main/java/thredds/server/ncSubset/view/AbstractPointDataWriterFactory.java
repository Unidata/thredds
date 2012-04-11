package thredds.server.ncSubset.view;

import thredds.server.ncSubset.controller.SupportedFormat;

final class AbstractPointDataWriterFactory {

	private AbstractPointDataWriterFactory(){}
	
	static final PointDataWriterFactory createPointDataWriterFactory(SupportedFormat supportedFormat){
		
		if( supportedFormat.getFormatName().equals("XML") ){
			return XMLPointDataWriterFactory.getInstance() ;
		}
		
		if( supportedFormat.getFormatName().equals("NETCDF") ){
			return NetCDFPointDataWriterFactory.getInstance() ;
		}
		
		if( supportedFormat.getFormatName().equals("CSV") ){
			return CSVPointDataWriterFactory.getInstance() ;
		}
		
		return null;
		
	}
}
