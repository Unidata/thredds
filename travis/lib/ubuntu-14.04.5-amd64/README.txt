This folder contains shared libraries for NetCDF-4 and its dependencies, built on Ubuntu 14.04.5 LTS.
As of 2017/12/07, that is the OS running on Travis's VMs.

Library versions:
NetCDF: 4.5.0
HDF5: ﻿1.10.1
zlib: 1.2.11

This folder also contains several symbolic links to the libs:

lrwxr-xr-x   1 cwardgar  ustaff    18B Dec  7 20:30 libhdf5.so@ -> libhdf5.so.101.0.0
lrwxr-xr-x   1 cwardgar  ustaff    18B Dec  7 20:30 libhdf5.so.101@ -> libhdf5.so.101.0.0
-rwxr-xr-x   1 cwardgar  ustaff   4.0M Dec  7 20:30 libhdf5.so.101.0.0*
lrwxr-xr-x   1 cwardgar  ustaff    21B Dec  7 20:30 libhdf5_hl.so@ -> libhdf5_hl.so.100.0.1
lrwxr-xr-x   1 cwardgar  ustaff    21B Dec  7 20:30 libhdf5_hl.so.100@ -> libhdf5_hl.so.100.0.1
-rwxr-xr-x   1 cwardgar  ustaff   154K Dec  7 20:30 libhdf5_hl.so.100.0.1*
lrwxr-xr-x   1 cwardgar  ustaff    19B Dec  7 20:30 libnetcdf.so@ -> libnetcdf.so.13.0.0
lrwxr-xr-x   1 cwardgar  ustaff    19B Dec  7 20:30 libnetcdf.so.13@ -> libnetcdf.so.13.0.0
-rwxr-xr-x   1 cwardgar  ustaff   1.1M Dec  7 20:30 libnetcdf.so.13.0.0*
lrwxr-xr-x   1 cwardgar  ustaff    14B Dec  7 20:30 libz.so@ -> libz.so.1.2.11
lrwxr-xr-x   1 cwardgar  ustaff    14B Dec  7 20:30 libz.so.1@ -> libz.so.1.2.11
-rwxr-xr-x   1 cwardgar  ustaff   115K Dec  7 20:30 libz.so.1.2.11*

These are necessary because an application may attempt to load a library using several different names.
They match the symlinks that were created when the software was first built on Ubuntu.

On Windows, the symlinks-–which were created in Linux-–will appear as plain-text files that contain the link text:
http://stackoverflow.com/questions/11662868/what-happens-when-i-clone-a-repository-with-symlinks-on-windows
It's best to only modify them under Linux.
