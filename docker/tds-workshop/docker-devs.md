Before trying to build, make sure the following TDS related files are
in the following locations:

tds gradle artifact: tds-files/tds-M.m.r-SNAPSHOT.war  
tdm gradle artifact: tdm-files/tdmFat-M.m.r-SNAPSHOT.jar

As the artifact versions change, the Dockerfile will need to be updated to
reflect the new version information.

The Dockerfile has a few commented lines that use wget to get the
latest stable versions from the Unidata ftp site...we may use this
again at some point, but for now, build and put the latest SNAPSHOT
artifacts in the proper directories.

Sean's Dockerhub workflow...YMMV
================================

 * Tweak the Dockerfile
 * Rebuild image:
   * `docker build -t tds-workshop .`
 * Start image: 
   * `docker run -i -t tds-workshop /bin/bash`
 * If all looks ok, tag the image:
   * docker tag -f tds-workshop unidata/tds-workshop:latest
 * Commit container:
   * `docker commit <id as seen on docker run command line above>  unidata/tds-workshop:latest`
 * Push to docker hub:
   * `docker push unidata/tds-workshop`

If this works, you will see new stuff being uploaded to dockerhub. 
