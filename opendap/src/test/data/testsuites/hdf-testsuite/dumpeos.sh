#!/bin/sh

for i in PointFile GridFile SwathFile
do
echo ${i}
../../../bin/geturl -a "http://manta/cgi-bin/nph-hdf/${i}.hdf" > ${i}.das
../../../bin/geturl -d "http://manta/cgi-bin/nph-hdf/${i}.hdf" > ${i}.dds
../../../bin/geturl -D "http://manta/cgi-bin/nph-hdf/${i}.hdf?" > ${i}.data
done
