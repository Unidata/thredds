FROM unidata/tds:v4.6.4-SNAPSHOT
MAINTAINER Sean Arms <sarms@ucar.edu>

ENV THREDDS_DIR=/usr/local/tomcat/content/thredds
USER root
RUN mkdir -p $THREDDS_DIR && chown -R tomcat:tomcat $THREDDS_DIR

#
# Copy over TDS config files and unzip
#

USER tomcat

ONBUILD COPY config.zip $THREDDS_DIR/config.zip
ONBUILD RUN unzip $THREDDS_DIR/config.zip -d $THREDDS_DIR

