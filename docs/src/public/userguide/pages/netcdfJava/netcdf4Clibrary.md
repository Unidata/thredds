---
title: netCDF C Library
last_updated: 2018-10-24
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: netcdf4_c_library.html
---

In order to write NetCDF-4 files, you must have the [NetCDF-4 C library](https://www.unidata.ucar.edu/software/netcdf/){:target="_blank"} (libnetcdf) - version 4.3.1 or above - available on your system, along with all supporting libraries (libhdf5, libz, etc).
The details of this differ for each operating system, and our experiences (so far) are documented below.

## Installation

For all platforms, we strongly recommend 64-bit Java, if you can run it.
Also, be sure to use the latest version, as security improvements are constantly being made.

### Thread safety

Any pre-built version of libnetcdf that you install - whether from a package manager or from a download page - is likely to not be thread-safe.
The same is also true for versions you build from source.
Therefore, netCDF-Java will only allow one thread to write via the netCDF-C libary at a time.
This may result in a performance hit.

When might you do concurrent writes of NetCDF-4 files?
For TDS administrators, this can often happen in the NetCDF Subset Service (NCSS). 

### Linux

#### Pre-built

The easiest way to get libnetcdf is through a package management program, such as rpm, yum, adept, and others.
Details will vary with each program but "netcdf" is usually the package name you want.

#### Build from source

Instructions for how to build libnetcdf from source can be [found here](https://www.unidata.ucar.edu/software/netcdf/docs/getting_and_building_netcdf.html){:target="_blank"}.

### Mac

#### Pre-built

As with Linux, a package manager is usually the easiest option.
libnetcdf is known to be available both from [Homebrew](https://brew.sh/){:target="_blank"} and [MacPorts](https://www.macports.org/){:target="_blank"}.
\"netcdf\" is usually the package name you want.

#### Build from source

This is identical to the [Linux build](https://www.unidata.ucar.edu/software/netcdf/docs/getting_and_building_netcdf.html){:target="_blank"}.

### Windows

#### Pre-built

Pre-built binaries are [available here](https://www.unidata.ucar.edu/software/netcdf/docs/winbin.html){:target="_blank"}.

#### Build from source

Instructions for how to build libnetcdf from source can be [found here](https://www.unidata.ucar.edu/software/netcdf/docs/getting_and_building_netcdf.html#netCDF-CMake){:target="_blank"}.

## Loading

In order to use libnetcdf, the CDM must know its location, as well as the location(s) of its dependencies.
These binaries will have different extensions depending on your platform:

* On Linux, they will be .SO files.
* On Mac, they will be .DYLIB files.
* On Windows, they will be .DLL files.

There are several ways to specify their location(s).

### Preferred method (requires NetCDF-Java 4.5.4 or later)

Set the system library path.
This is the path that the operating system will search whenever it needs to find a shared library that it doesn\'t already know the location of.
It is not Java-, NetCDF-, or CDM-specific.
As usual, details will vary with each platform.

#### Linux

The system library path maps to the `LD_LIBRARY_PATH` environment variable.
If you built from source and used the default installation directory, `libnetcdf` and its dependencies will all be in `/usr/local/lib`.
If you got `libnetcdf` from a package manager, it might\'ve been installed elsewhere.

Note that `/usr/local/lib` is included in the default shared library search path of many flavors of Linux.
Therefore, it may not be necessary to set `LD_LIBRARY_PATH` at all.
Notable exceptions include some RedHat-derived distributions.
Read [this](https://www.tldp.org/HOWTO/Program-Library-HOWTO/shared-libraries.html#AEN62){:target="_blank"} for more info.

#### Mac

The system library path maps to the `DYLD_LIBRARY_PATH` environment variable.
If you built from source and used the default installation directory, `libnetcdf` and its dependencies will all be in `/usr/local/lib`.
They will also be installed there if you obtained them using Homebrew.
MacPorts, on the other hand, installs binaries to `/opt/local/lib`.

Note that `/usr/local/lib` is part of the default library search path on Mac.
Therefore, it may not be necessary to set `DYLD_LIBRARY_PATH` at all.

#### Windows

The system library path maps to the `PATH` environment variable.
To find libnetcdf and its dependencies, you’ll want to add `$NC4_INSTALL_DIR/bin`, `$NC4_INSTALL_DIR/deps/$ARCH/bin`, and `$NC4_INSTALL_DIR/deps/$ARCH/lib` to the `PATH` variable.
`NC4_INSTALL_DIR` is the location where you installed libnetcdf and `ARCH` is its architecture (either \"w32\" or \"x64\").

### Alternate methods

The following alternatives are Java- and/or CDM-specific.
To use these, it’s required that libnetcdf and all of its dependencies live in the same directory.
So, if that is not the case in your current configuration, you must manually copy them all to the same place.
This is a particular issue on Windows, because the libraries are installed in separate locations by default.

In addition to the library path, the CDM also needs to know the library name.
This is almost always \"netcdf\", unless you’ve renamed it.

For TDS users, we recommend setting the library path and name in the [THREDDS configuration file](http://localhost:4005/tds_config_ref.html#netcdf-4-c-library-loading).

For standalone CDM library use, you can:

* create a system environment variable: `JNA_PATH=/path/to/library`
* set a Java property on the command line: `-Djna.library.path=/path/to/library`
* set the library path and name in the runtime configuration file
* directly call `Nc4Iosp.setLibraryAndPath()` from your Java program

In all cases, we recommended that you use an absolute path to specify the library location.

### Troubleshooting

If you get a message like this:

~~~bash
Warning! ***HDF5 library version mismatched error***
The HDF5 header files used to compile this application do not match
the version used by the HDF5 library to which this application is linked.
Data corruption or segmentation faults may occur if the application continues.
This can happen when an application was compiled by one version of HDF5 but
linked with a different version of static or shared HDF5 library.
You should recompile the application or check your shared library related
settings such as 'LD_LIBRARY_PATH'.
You can, at your own risk, disable this warning by setting the environment
variable 'HDF5_DISABLE_VERSION_CHECK' to a value of '1'.
Setting it to 2 or higher will suppress the warning messages totally.
Headers are 1.8.10, library is 1.8.5
Bye...
~~~

Make sure that you don’t have an old version of `libhdf5` in your system library path.

## Writing NetCDF-4 files

* From the command line: see [nccopy man page](cdm_utility_programs.html#nccopy
)
* From ToolsUI: in Viewer tab, bring up the file to copy, then click \"Write netCDF file\" button to get dialog.
* From TDS NetCDF Subset Service: choose netcdf-4 output type.
* From a Java program (see `ucar.nc2.dataset.NetcdfDataset.main()` for complete example):
  ~~~java
  Nc4Chunking chunker = Nc4Chunking factory(Strategy type, int deflateLevel, boolean shuffle);
  NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf4;

  FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, filenameOut, version, chunker);
  ...
  NetcdfFile ncfileOut = writer.write();
  ncfileIn.close();
  ncfileOut.close();
  ~~

### Chunking Strategy (version 4.5)

When writing NetCDF-4 files, one must decide on how the variables are to be chunked.
In the NetCDF-Java library this is done through the use of a `Nc4Chunking` strategy.
The possibilities currently are:

* `standard` : this strategy is used by default (see below for description)
* `grib`: this strategy matches how GRIB files are stored: the chunking uses only the two rightmost dimensions, which for GRIB is the x and y dimension. 
  Use this strategy when converting GRIB files to NetCDF-4, in order to optimize conversion writing time, and to get similar access speed.
* `custom`: you may write your own implementation of `ucar.nc2.write.Nc4Chunking` and pass it into `FileWriter2` or `NetcdfFileWriter`. 
  This gives you complete control over chunking.

Both `standard` and `grib` strategies allow you to override individual variable chunking if you want by setting the variable\'s `_ChunkSizes` attribute.

**By default, the Java library will write chunked and compressed NetCDF-4 files**, using the default chunking algorithm.
You may pass in a null for the chunking parameter to use the default.

### Default chunking strategy

For each Variable:

* Look for a variable attribute named `_ChunkSizes`, whose value is a vector of integer chunk sizes, one for each dimension. 
  If it exists, use it.
* If the variable does not have an unlimited dimension:
  * it will be chunked if the total size in bytes > `Nc4ChunkingDefault.minVariableSize`
  * chunk size will be `fillFastest( variable.shape, Nc4ChunkingDefault.defaultChunkSize)`
* If the variable has one or more unlimited dimensions, it will be chunked, and the chunk size will be calculated as:
  * set unlimited dimensions to length one, then compute `fillFastest( variable.shape, Nc4ChunkingDefault.defaultChunkSize)`
  * if the resulting chunk size is greater than `Nc4ChunkingDefault.minChunksize`, use it
  * if not, set the unlimited dimension chunk sizes so that the resulting chunksize is close to `Nc4ChunkingDefault.minChunksize`.
    If there are N unlimited dimensions, take the Nth root, ie evenly divide the chunk size among the unlimited dimensions.

The `fillFastest( int[] shape, maxSize)` algorithm fills the fastest varying (rightmost) dimensions first, until the `chunkSize` is as close to `maxSize` as possible without exceeding.
The net effect is that the chunkSizes will be close to `Nc4ChunkingDefault.defaultChunkSize`, with a minimum of Nc4ChunkingDefault.minChunksize, and favoring read access along the fast dimensions.
Any variable with an unlimited dimension will use at least `Nc4ChunkingDefault.minChunksize` bytes (approx, but if compressing, unused space should be mostly eliminated).

Current default values (these can be overridden by the user):

* `minVariableSize` = 65K
* `defaultChunkSize` = 256K
* `minChunksize` = 8K

By default, compression (deflate level = 5) and the shuffle filter will be used.

The user can override these by:

~~~java
// set deflate > 0 to compress
// set shuffle to true for the shuffle filter 
Nc4Chunking chunker = Nc4Chunking factory(Nc4Chunking.Strategy.standard, int deflateLevel, boolean shuffle);
~~~