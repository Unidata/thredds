#!/bin/bash

# basename is the simple name minus versioning and extension
W="\
./dap4/d4tswar/build/libs/d4tswar \
./opendap/dtswar/build/libs/dtswar \
./tds/build/libs/tds \
"
for w in $W; do
  snap=`ls -1 ${w}-*-SNAPSHOT.war`
  snap=`basename $snap`
  dir=`dirname $w`
  base=`basename $w`
  rm -f "./$base.war"
  cp "$dir/$snap" "./$base.war"
done

