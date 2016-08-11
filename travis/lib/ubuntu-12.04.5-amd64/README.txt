This folder contains shared libraries for NetCDF-4 and its dependencies, built on Ubuntu 12.04.5 LTS.
As of 2016/07/14, that is the OS running on Travis's VMs.

Library versions:
NetCDF: 4.4.1
HDF5: ﻿1.8.17
zlib: 1.2.8

This folder also contains several symbolic links to the libs:

lrwxr-xr-x   1 cwardgar  ustaff    17B Jul 14 17:10 libhdf5.so -> libhdf5.so.10.2.0
lrwxr-xr-x   1 cwardgar  ustaff    17B Jul 14 17:10 libhdf5.so.10 -> libhdf5.so.10.2.0
-rwxr-xr-x   1 cwardgar  ustaff   3.1M Jul 14 17:06 libhdf5.so.10.2.0
lrwxr-xr-x   1 cwardgar  ustaff    20B Jul 14 17:11 libhdf5_hl.so -> libhdf5_hl.so.10.1.0
lrwxr-xr-x   1 cwardgar  ustaff    20B Jul 14 17:11 libhdf5_hl.so.10 -> libhdf5_hl.so.10.1.0
-rwxr-xr-x   1 cwardgar  ustaff   141K Jul 14 17:06 libhdf5_hl.so.10.1.0
lrwxr-xr-x   1 cwardgar  ustaff    19B Jul 14 17:13 libnetcdf.so -> libnetcdf.so.11.0.3
lrwxr-xr-x   1 cwardgar  ustaff    19B Jul 14 17:12 libnetcdf.so.11 -> libnetcdf.so.11.0.3
-rwxr-xr-x   1 cwardgar  ustaff   1.3M Jul 14 17:06 libnetcdf.so.11.0.3
lrwxr-xr-x   1 cwardgar  ustaff    13B Jul 14 17:08 libz.so -> libz.so.1.2.8
lrwxr-xr-x   1 cwardgar  ustaff    13B Jul 14 17:08 libz.so.1 -> libz.so.1.2.8
-rwxr-xr-x   1 cwardgar  ustaff    99K Jul 14 17:06 libz.so.1.2.8

These are necessary because an application may attempt to load a library using several different names.
They match the symlinks that were created when the software was first built on Ubuntu.

On Windows, the symlinks-–which were created in Linux–will appear as plain-text files that contain the link text:
http://stackoverflow.com/questions/11662868/what-happens-when-i-clone-a-repository-with-symlinks-on-windows
It's best to only modify them under Linux.
