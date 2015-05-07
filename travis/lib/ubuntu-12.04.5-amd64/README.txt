This folder contains shared libraries for NetCDF-4 and its dependencies, built on Ubuntu 12.04.5 LTS.
As of 2015/04/16, that is the OS running on Travis's VMs.

Library versions:
NetCDF: 4.3.3.1
HDF5: 1.8.13
zlib: 1.2.8

This folder also contains several symbolic links to the libs:

lrwxrwxrwx 1 cwardgar cwardgar      19 Apr 17 20:56 libhdf5_hl.so -> libhdf5_hl.so.8.0.2
lrwxrwxrwx 1 cwardgar cwardgar      19 Apr 17 20:56 libhdf5_hl.so.8 -> libhdf5_hl.so.8.0.2
-rwxr-xr-x 1 cwardgar cwardgar  143606 Apr 17 20:51 libhdf5_hl.so.8.0.2
lrwxrwxrwx 1 cwardgar cwardgar      16 Apr 17 20:57 libhdf5.so -> libhdf5.so.8.0.2
lrwxrwxrwx 1 cwardgar cwardgar      16 Apr 17 20:57 libhdf5.so.8 -> libhdf5.so.8.0.2
-rwxr-xr-x 1 cwardgar cwardgar 3173196 Apr 17 20:51 libhdf5.so.8.0.2
lrwxrwxrwx 1 cwardgar cwardgar      18 Apr 17 20:57 libnetcdf.so -> libnetcdf.so.7.2.0
lrwxrwxrwx 1 cwardgar cwardgar      18 Apr 17 20:57 libnetcdf.so.7 -> libnetcdf.so.7.2.0
-rwxr-xr-x 1 cwardgar cwardgar 2453594 Apr 17 20:51 libnetcdf.so.7.2.0
lrwxrwxrwx 1 cwardgar cwardgar      13 Apr 17 20:58 libz.so -> libz.so.1.2.8
lrwxrwxrwx 1 cwardgar cwardgar      13 Apr 17 20:58 libz.so.1 -> libz.so.1.2.8
-rwxr-xr-x 1 cwardgar cwardgar  101138 Apr 17 20:51 libz.so.1.2.8

These are necessary because an application may attempt to load a library using several different names.
They match the symlinks that were created when the software was first built on Ubuntu.

On Windows, the symlinks—which were created in Linux—will appear as plain-text files that contain the link text:
http://stackoverflow.com/questions/11662868/what-happens-when-i-clone-a-repository-with-symlinks-on-windows
It's best to only modify them under Linux.
