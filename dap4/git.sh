#!/bin/sh
set -x
GIT=github
DST=$GIT/dap4

FILES=`cat Inventory |tr -d '\r' |tr '\r\n' '  '`

rm -fr ${GIT}
mkdir -p ${DST}

for f in ${FILES} ; do
DIR=`dirname $f`
mkdir -p ${DST}/${DIR}
cp $f $DST/$f
done

