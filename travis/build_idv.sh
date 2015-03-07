#!/bin/bash
# Checkout source if not already cached
if [[ ! -d "$HOME/IDV/.git" ]]; then
  git clone --depth 1 https://www.github.com/unidata/IDV.git ${HOME}/IDV
fi

cd ${HOME}/IDV

# Update ncIdv.jar with our new one, then build IDV
#cp ${TRAVIS_BUILD_DIR}/ncIdv/build/libs/ncIdv-*.jar lib/ncIdv.jar
sed -i s/value=\"ignore\"/value=\"last\"/ build.xml
unzip -o libsrc/j3d-linux.zip -d extlib
ant -lib ${HOME}/IDV/extlib idv
