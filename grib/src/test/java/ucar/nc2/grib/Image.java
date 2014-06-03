package ucar.nc2.grib;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ucar.nc2.grib.receiver.DataReceiver;

public class Image {
		
	BufferedImage image;
	Graphics2D g;
	
	MinMax from;
	MinMax to = new MinMax(0, 255);

	Image(int width, int height, MinMax from) {
		this.from = from;
		this. image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		this.g = image.createGraphics();
		this.g.setBackground(Color.BLUE);
	}
	
	public synchronized void setPixel(int w, int h, double value) {
		int c = (int) scale(value, from, to);
		g.setColor(new Color (c, c, c));
		g.drawLine(w, h, w, h);
	}
	
	public void save(File file) throws IOException {
		ImageIO.write(image, "png", file);
	}
	
	private double scale(double value, MinMax from, MinMax to) {
		return to.min + ((value - from.min) / (from.max - from.min)) * (to.max - to.min);
	}
	
	
	public static void generateImage(DataReceiver data, String filename, boolean reduced) throws IOException {
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
		Image img = new Image(widthTotal, heightTotal, getMinMaxFromArray(doubles));
		int cnt = 0;
		for (int y = 0; y < heightTotal; y++) {
			int widthLine = reduced ? nPtsInLine[y] : widthTotal;
			for (int x = 0; x < widthLine; x++) {
				double value = doubles[cnt++];
				if (value != Double.NaN) {
					int xPx = (widthTotal / 2) - (widthLine / 2) + x;
					img.setPixel(xPx, y, value);
				}
			}
		}
		img.save(new File(filename));
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
