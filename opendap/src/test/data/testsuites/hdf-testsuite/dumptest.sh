#!/bin/sh

#for i in an1 dfp1 dfr1 gr1 gr2 gr3 gr4 gr5 sds1 sds2 sds3 sds4 sds5 sds6 sds7 vs1 vs2 vs3 vs4 vs5
for i in gr4
do
echo test${i}
../../../bin/geturl -a "http://manta/cgi-bin/nph-hdf/test${i}.hdf" > test${i}.das
../../../bin/geturl -d "http://manta/cgi-bin/nph-hdf/test${i}.hdf" > test${i}.dds
../../../bin/geturl -D "http://manta/cgi-bin/nph-hdf/test${i}.hdf?" > test${i}.data
done
