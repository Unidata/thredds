% Example script to read CF-compliant structured grid NetCDF 
% data into Matlab using the NetCDF-Java library

% Get the NetCDF-Java library from: 
% http://www.unidata.ucar.edu/software/netcdf-java/
%
% I'm using "toolsUI.jar" which is advertised as "a nice fat jar
% file containing everything in a single jar"

% Rich Signell  rsignell@usgs.gov

% For Matlab 7+:
javaaddpath('/home/rsignell/java/jar/toolsUI-2.2.16.jar','-end');

% For Matlab 6 or 6.5:
%  1. "edit classpath.txt" and add the toolsUI jar file to the list
%  2.  Restart Matlab

% NOTE: For all versions of Matlab, edit "classpath.txt" and remove 
% (or comment) the line that contains
%  "mwucarunits.jar" and restart MATLAB.  The "mwucarunits.jar" file is only
% used by the Mathworks "Model-Based Calibration Toolbox" and contains an 
% old implementation of the Unidata udunits package that conflicts with
% the more recent version that NetCDF-Java uses.

% import the methods we need for this example
import ucar.nc2.dataset.grid.*
import ucar.nc2.dataset.grid.GridDataset.*

% open a CF-compliant NetCDF File

dataset = GridDataset.open('http://stellwagen.er.usgs.gov/cgi-bin/nph-dods/models/adria/roms_sed/sed038/adria03_sed038_his_0034.nc');

% get the grid associated with the variable name "temp" (temperature)

tgrid = dataset.findGridByName('temp');

% get the coordinate system for this grid:
gcs=tgrid.getCoordinateSystem();

latAxis=gcs.getYHorizAxis();
lonAxis=gcs.getXHorizAxis();
timeAxis=gcs.getTimeAxis();
zAxis=gcs.getVerticalAxis();

tim=gcs.getTimeDates();
lonj=lonAxis.read();
latj=latAxis.read();

% read times from file as gregorian dates
tj=getTimeDates(gcs);

% read data associated with this grid:
% at vertical_level=19 (the level bounded by the sea surface) 
% at time_step = 0
itime=0;
zlev=19;
tempj = tgrid.readDataSlice(itime,zlev,-1,-1);

% copy into Matlab arrays 
lat=squeeze(copyToNDJavaArray(latj));
lon=squeeze(copyToNDJavaArray(lonj));
temp=squeeze(copyToNDJavaArray(tempj));

% plot the data
pcolor(lon,lat,double(temp));shading flat;colorbar

% use date string for title
title(char(tj(itime+1)))

% the Matlab "methods" command is very helpful in seeing what
% methods are available for specific classes.  For example, try:
% >> methods(gcs)
