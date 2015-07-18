FROM tomcat:8
MAINTAINER Sean Arms <sarms@ucar.edu>
#
# Expose ports 8080 (tomcat) and 8443 (tomcat ssl)
#
EXPOSE 8080 8443

#
# Install netcdf and some basic command line tools
#
RUN apt-get update && apt-get install -y \
  less \
  libnetcdf-dev \
  vim 

#
# Create the user and group tomcat and change ownershiup of the tomcat
#   directory to user and group tomcat
#
RUN groupadd -r tomcat -g 1000 && \
useradd -u 1000 -r -g tomcat -d /usr/local/tomcat -s /bin/bash -c "User tomcat for Docker image" tomcat

#
# Create a data directory and the tdm directory
#
RUN mkdir /data && mkdir -p /usr/local/tomcat/content/tdm  

#
# Copy over modified tomcat files
#
COPY tomcat-files/bin/setenv.sh /usr/local/tomcat/bin/setenv.sh
COPY tomcat-files/conf/server.xml /usr/local/tomcat/conf/server.xml
COPY tomcat-files/conf/tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml
COPY tomcat-files/conf/keystore /usr/local/tomcat/conf/keystore

#
# Copy over TDM files
#
COPY tdm-files/runTdm.sh /usr/local/tomcat/content/tdm/runTdm.sh

#
# Copy over script that starts up tomcat and the TDM
#
COPY tomcat-files/startItAll.sh /usr/local/tomcat/startItAll.sh

#
# Copy over bash_profile file that sets correct umask for sharing
#
# Copy over bash_logout file that nicely closes java processes for
#  shutting down
#
COPY tomcat-files/bash_profile /usr/local/tomcat/.bash_profile
COPY tomcat-files/bash_logout /usr/local/tomcat/.bash_logout


#
# Change owner of tomcat directory to user and group tomcat
#
RUN chown -R tomcat:tomcat /usr/local/tomcat 

#
# Switch to user tomcat
#
USER tomcat

#
# get the latest stable THREDDS Data Server (TDS)
#
#RUN wget -P /usr/local/tomcat/webapps ftp://ftp.unidata.ucar.edu/pub/thredds/4.6/current/thredds.war
#
# Need the latest build of the TDS for the workshop
#
COPY tds-files/tds-4.6.3-SNAPSHOT.war /usr/local/tomcat/webapps/thredds##04.06.03-SNAPSHOT.war

#
# get the latest THREDDS Data Monitor (TDM)
#
#RUN wget -P /usr/local/tomcat/content/tdm ftp://ftp.unidata.ucar.edu/pub/thredds/4.6/current/tdm-4.6.jar
#
# Need the latest build of the TDM for the workshop
#
COPY tdm-files/tdmFat-4.6.3-SNAPSHOT.jar /usr/local/tomcat/content/tdm/tdmFat-4.6.3-SNAPSHOT.jar

RUN chmod u+rwx /usr/local/tomcat/startItAll.sh && \ 
    chmod u+rwx /usr/local/tomcat/content/tdm/runTdm.sh 
#
# Start the TDM
#
CMD  /usr/local/tomcat/startItAll.sh
