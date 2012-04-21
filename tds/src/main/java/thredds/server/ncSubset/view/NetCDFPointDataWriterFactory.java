package thredds.server.ncSubset.view;

import java.io.OutputStream;

class NetCDFPointDataWriterFactory implements PointDataWriterFactory {

	private static NetCDFPointDataWriterFactory INSTANCE;
	
	private NetCDFPointDataWriterFactory(){
		
	}
	
	public static NetCDFPointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new NetCDFPointDataWriterFactory(); 
		}
		
		return INSTANCE;
	} 
		
	@Override
	public PointDataWriter createPointDataWriter(OutputStream os) {
		return NetCDFPointDataWriter.createNetCDFPointDataWriter(os);
	}
	
	
}
