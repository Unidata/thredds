package ucar.nc2.grib;

public interface GribGds {

	public int[] getNptsInLine();
	public int getNpts();
	public GdsHorizCoordSys makeHorizCoordSys();
	
}
