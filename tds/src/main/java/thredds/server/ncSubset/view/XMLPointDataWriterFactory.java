package thredds.server.ncSubset.view;

import java.io.OutputStream;

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
	public PointDataWriter createPointDataWriter(OutputStream os) {
		
		return XMLPointDataWriter.createXMLPointDataWriter(os);
	}

}
