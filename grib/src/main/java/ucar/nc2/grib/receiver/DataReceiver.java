package ucar.nc2.grib.receiver;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.grib.GribGds;

public class DataReceiver implements DataReceiverIF {
	
	private GribGds[] gds;
	private Array dataArray;
	private Range yRange, xRange;
	private int size;
	private int horizSize;
	
	public DataReceiver(Section section, Range yRange, Range xRange) {
		
		dataArray = 
				Array.factory(DataType.FLOAT, section.getShape());
		this.yRange = yRange;
		this.xRange = xRange;
		this.horizSize = yRange.length() * xRange.length();

		// prefill with NaNs, to deal with missing data
		IndexIterator iter = dataArray.getIndexIterator();
		while (iter.hasNext()) {
			iter.setFloatNext(Float.NaN);
		}
		
		size = (int) (dataArray.getSize() / horizSize);
		gds = new GribGds[size];
		
	}
	
	public void addData(float[] data, GribGds gds, int resultIndex, int nx) throws IOException {
		
		int start = resultIndex * horizSize;
		int count = 0;
		for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
			for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
				int dataIdx = y * nx + x;
				dataArray.setFloat(start + count, data[dataIdx]);
				count++;
			}
		}
		
		this.gds[resultIndex] = gds;
	
	}
	
	public Array getArray() {
		return dataArray;
	}
	
	public GribGds getGds(int idx) {
		return gds[idx];
	}

}
