package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.util.DiskCache2;

class XMLPointDataWriterFactory implements PointDataWriterFactory {	

	private static XMLPointDataWriterFactory INSTANCE;
	
	private XMLPointDataWriterFactory(){
		
	} 
	
	public static XMLPointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new XMLPointDataWriterFactory();
		}
		
		return INSTANCE;
	}
	
	@Override
	public PointDataWriter createPointDataWriter(OutputStream os, DiskCache2 diskCache ) {
		
		return XMLPointDataWriter.createXMLPointDataWriter(os);
	}

}
