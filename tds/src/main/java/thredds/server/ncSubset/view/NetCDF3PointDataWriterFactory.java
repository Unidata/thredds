/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncSubset.view;

import java.io.OutputStream;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.DiskCache2;

class NetCDF3PointDataWriterFactory implements PointDataWriterFactory {

	private static NetCDF3PointDataWriterFactory INSTANCE;

	private static final NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3; 
	
	private NetCDF3PointDataWriterFactory(){
	}
	
	public static NetCDF3PointDataWriterFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new NetCDF3PointDataWriterFactory(); 
		}
		
		return INSTANCE;
	} 
		
	@Override
	public PointDataWriter createPointDataWriter( OutputStream os, DiskCache2 diskCache) {
		return NetCDFPointDataWriter.createNetCDFPointDataWriter(version, os, diskCache);
	}
	
	public NetcdfFileWriter.Version getVersion(){
		return version;
	}
	
}
