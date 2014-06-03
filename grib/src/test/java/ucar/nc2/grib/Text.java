package ucar.nc2.grib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import ucar.nc2.grib.receiver.DataReceiver;

public class Text {
	
	
	private static int LENGTH_PER_LINE = 40*3;
	
	RandomAccessFile raf;
	FileChannel fc;
	ByteBuffer buf;

	Text(int width, int height, String filename, int nPts) throws IOException {
		raf = new RandomAccessFile(filename, "rw");
		fc = raf.getChannel();
		buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, nPts * LENGTH_PER_LINE);
	}
	
	public synchronized void set(double lat, double lon, double value) throws IOException {
	    buf.put((lat+";"+lon+";"+value+"\n").getBytes());
	}
	
	public void close() throws IOException {
		fc.close();
		raf.close();
	}
	
	
	public static void generateText(DataReceiver data, String filename, boolean reduced) throws IOException {
		GribGds gds = data.getGds(0);		
		int[] nPtsInLine = gds.getNptsInLine();
		int widthTotal, heightTotal;
		
		if (reduced) {
			widthTotal = getMaxFromArray(nPtsInLine);
			heightTotal = nPtsInLine.length;
		} else {
			widthTotal = gds.makeHorizCoordSys().nx;
			heightTotal = gds.makeHorizCoordSys().ny;
		}
		
		double[] doubles = (double[]) data.getArray().get1DJavaArray(double.class);
		Text text = new Text(widthTotal, heightTotal, filename, gds.getNpts());
		GdsHorizCoordSys coordSys = gds.makeHorizCoordSys();
		int cnt = 0;
		for (int y = 0; y < heightTotal; y++) {
			int widthLine = reduced ? nPtsInLine[y] : widthTotal;
			double lat = reduced ? coordSys.gaussLats.getDouble(y) :
				coordSys.getStartY() + ((coordSys.getEndY() - coordSys.getStartY()) / heightTotal) * y;
			for (int x = 0; x < widthLine; x++) {
				double value = doubles[cnt++];
				if (value != Double.NaN) {
					double lon =
							coordSys.getStartX() + ((coordSys.getEndX() - coordSys.getStartX()) / widthLine) * x;
					text.set(lat, lon, value);
				}
			}
		}
		text.close();
	}
	
	private static int getMaxFromArray(int[] array) {
		int max = Integer.MIN_VALUE;
		for (int i : array) {
			max = i > max ? i : max;
		}
		return max;
	}
	
	static class MinMax {
		public double min, max;
		public MinMax(double min, double max) {
			this.min = min;
			this.max = max;
		}
	}
	
	private static MinMax getMinMaxFromArray(double[] array) {
		MinMax ret = new MinMax(Double.MAX_VALUE, Double.MIN_VALUE);
		for (double i : array) {
			if (i != Double.NaN) {
				ret.max = i > ret.max ? i : ret.max;
				ret.min = i < ret.min ? i : ret.min;
			}
		}
		return ret;
	}
	
}
