package thredds.server.ncSubset.view;

import java.io.OutputStream;

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
	public PointDataWriter createPointDataWriter(OutputStream os) {
		
		return CSVPointDataWriter.createCSVPointDataWriter(os);
	}

}
