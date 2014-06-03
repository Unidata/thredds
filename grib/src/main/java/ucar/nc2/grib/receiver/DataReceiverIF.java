package ucar.nc2.grib.receiver;

import java.io.IOException;

import ucar.nc2.grib.GribGds;

public interface DataReceiverIF {
	
	void addData(float[] data, GribGds gds, int resultIndex, int nx) throws IOException;

}
