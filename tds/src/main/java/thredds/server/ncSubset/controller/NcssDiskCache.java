package thredds.server.ncSubset.controller;

import java.io.File;

import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.util.DiskCache2;

public final class NcssDiskCache {

	private DiskCache2 diskCache = null;
	//private long maxFileDownloadSize = -1;
	
	private static NcssDiskCache INSTANCE;
	
	private NcssDiskCache(){
		
			
	    //maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
	    String cache = ThreddsConfig.get("NetcdfSubsetService.dir", ServletUtil.getContentPath() + AbstractNcssController.servletCachePath);
	    File cacheDir = new File(cache);
	    if (!cacheDir.exists())  {
	      if (!cacheDir.mkdirs()) {
	        ServletUtil.logServerStartup.error("Cant make cache directory "+cache);
	        throw new IllegalArgumentException("Cant make cache directory "+cache);
	      }
	    }

	    int scourSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.scour", 60 * 10);
	    int maxAgeSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.maxAge", -1);
	    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
	    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

	    // LOOK: what happens if we are still downloading when the disk scour starts?
	    
	    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);
	    ServletUtil.logServerStartup.info(getClass().getName() + "Ncss.Cache= "+cache+" scour = "+scourSecs+" maxAgeSecs = "+maxAgeSecs);
	}
	
	public DiskCache2 getDiskCache(){
		return this.diskCache;
	}
	
	public static NcssDiskCache getInstance(){
		
		if(INSTANCE == null){
			INSTANCE = new NcssDiskCache();			
		}
		
		return INSTANCE;
	}
	
}
