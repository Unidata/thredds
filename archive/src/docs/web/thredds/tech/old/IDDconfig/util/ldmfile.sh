#!/bin/sh -f

#--------------------------------------------------------------------------
#
# Name:    ldmfile.sh
#
# Purpose: file a LDM product and log the receipt of the product
#
# History: 20030815 - Created for Zlib-compressed GINI image filing
#
#--------------------------------------------------------------------------

SHELL=sh
export SHELL

# Set log file
LOG=/local/ldm/logs/ldm-mcidas.log
exec >>$LOG 2>&1

# Create directory structure
fname=`basename $1`
dirs=`echo $1 | sed s/$fname//`
mkdir -p $dirs

# Write stdin to the designated file and log its filing
echo `date -u +'%b %d %T'` `basename $0`\[$$\]: FILE $1
cat > $1

# Done
exit 0
