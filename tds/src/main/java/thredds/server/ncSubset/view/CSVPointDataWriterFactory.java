package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.util.DiskCache2;

class CSVPointDataWriterFactory implements PointDataWriterFactory {

	private static CSVPointDataWriterFactory INSTANCE;
	
	private CSVPointDataWriterFactory(){
		
	}
	
	public static CSVPointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new CSVPointDataWriterFactory(); 
		}
		
		return INSTANCE;
	}
	
	@Override
	public PointDataWriter createPointDataWriter(OutputStream os, DiskCache2 diskCache) {
		
		return CSVPointDataWriter.createCSVPointDataWriter(os);
	}

}
