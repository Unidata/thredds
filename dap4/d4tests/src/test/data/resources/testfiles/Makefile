NC=\
test_one_var \
test_one_vararray \
test_atomic_types \
test_atomic_array \
test_enum \
test_enum_2 \
test_enum_array \
test_groups1 \
test_struct_type \
test_struct_array \
test_misc1 \
test_vlen1 \
test_vlen2 \
test_opaque \
test_opaque_array \
test_fill \
test_utf8

HDF5=\
test_struct_nested \
test_struct_nested3 \
test_vlen3 \
test_vlen4 \
test_vlen5

C=tst_fills.c

BUGS=

all::
	which ncgen ; which ncdump
	for x in ${C} ; do ${MAKE} C1=$$x c1; done
	for x in ${NC} ${HDF5} ; do ncgen -k3 $${x}.cdl ; done
	for x in ${HDF5} ; do cp $${x}.nc $${x}.hdf5 ; done 

c1:
	gcc -g -O0 -o ${C1:.c=.exe} ${C1} -L/usr/local/lib -lnetcdf -lcurl -lhdf5 -lhdf5_hl
	./${C1:.c=.exe}		

clean::
	for x in ${NC} ; do rm -f $${x}.nc ; done
	for x in ${HDF5} ; do rm -f $${x}.hdf5 ; done
	rm -fr dump

verify:
	rm -fr dump
	mkdir dump
	for x in ${NC} ; do ncdump $${x}.nc > dump/$${x}.cdl ; done
	for x in ${HDF5} ; do ncdump $${x}.hdf5 > dump/$${x}.cdl ; done
	for x in ${NC} ${HDF5} ; do \
	echo $${x}.cdl ; \
	diff -wBb $${x}.cdl dump/$${x}.cdl ; \
	done

UI=f:/git/thredds/ui/target/toolsUI-4.4.0-SNAPSHOT.jar
F=test_test

# note -vall fails
x::
	rm -f ${F}.hdf5 ${F}.nc
	ncgen -k3 -o ${F}.nc ${F}.cdl
	cp ${F}.nc ${F}.hdf5
	java -cp ${UI} ucar.nc2.NCdumpW ${F}.hdf5

#	ncdump ${F}.hdf5

t::
	ncgen -k3 -o ${F}.nc ${F}.cdl
	ncdump ${F}.nc
