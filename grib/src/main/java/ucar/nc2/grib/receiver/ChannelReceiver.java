package ucar.nc2.grib.receiver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import ucar.ma2.Range;
import ucar.nc2.grib.GribGds;

public class ChannelReceiver implements DataReceiverIF {
	
	private DataOutputStream outStream;
	private Range yRange, xRange;

	public ChannelReceiver(WritableByteChannel channel, Range yRange, Range xRange) {
		this.outStream = new DataOutputStream(Channels.newOutputStream(channel));
		this.yRange = yRange;
		this.xRange = xRange;
	}

	public void addData(float[] data, GribGds gds, int resultIndex, int nx) throws IOException {
		for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
			for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
				int dataIdx = y * nx + x;
				outStream.writeFloat(data[dataIdx]);
			}
		}
	}
}