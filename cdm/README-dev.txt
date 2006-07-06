To run unit tests:

- make sure working directory is netcdf-java-2.2

- set TestAll.datadir to point to wherever /upc/share/testdata is mounted

---------------
Release/branch

make cure everything is checked in - this is a remote tag, working only on the CVS repository, not your local files.

cvs rtag -b REL_2_2_14 grib
cvs rtag -b REL_2_2_14 nj22_all
cvs rtag -b REL_2_2_14 thredds

Now create a local version of the branch, and make bug fixes to it if needed:

cd c:/dev
mkdir nj22.14
cd  nj22.14

cvs co -r REL_2_2_14 grib
cvs co -r REL_2_2_14 nj22_all
cvs co -r REL_2_2_14 thredds