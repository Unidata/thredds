---
title: Installation Checklist
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: installation_checklist.html
---

##  Bare-bones Instructions for Standing Up a TDS Production Server

#### Prior to Installation
1. Purchase a certificate from a [certificate authority](https://en.wikipedia.org/wiki/Certificate_authority){:target="_blank"} for your TDS domain/host. 
2. Create a [dedicated user/group](tomcat_permissions.html#dedicated){:target="_blank"} for running the Tomcat server.

#### Java

1. [Install the JDK](install_java_tomcat.html#installing-java-jdk){:target="_blank"}

#### Tomcat

1. [Install Tomcat](install_java_tomcat.html#installing-the-tomcat-servlet-container){:target="_blank"}
2. Create a [`setenv.sh` file](running_tomcat.html#setting-java_home-java_opts-catalina_base-and-content_root){:target="_blank"} in `$TOMCAT_HOME/bin` to set JVM options and the TDS `$CONTENT_ROOT`.
3. Make the following modifications to `${tomcat_home}/conf/server.xml`:
 * Enable [digested password support](digested_passwords.html#configure-tomcat-to-use-digested-passwords){:target="_blank"} by modifying the `UserDatabaseRealm`.
 * Enable [TLS/SSL in tomcat](enable_tls_encryption.html#enabling-tlsssl-in-tomcat){:target="_blank"} using you CA certificate.
 * Enable [Compression](performance_tips.html#compression){:target="_blank"} in the Tomcat connectors.
 * Modify the Tomcat [AccessLogValve](tomcat_access_log.html)){:target="_blank"} log format and changed the prefix and suffix and pattern attributes for the access log file.
4. Create a [digested password](digested_passwords.html#digest.sh){:target="_blank"} using the algorithm specified in the `UserDatabaseRealm` of the `${tomcat_home}/conf/server.xml` file.
5. Make the following modifications to `${tomcat_home}/conf/tomcat-users.xml`:
 * Create roles for [manager-gui](tomcat_manager_app.html#granting-access-to-the-manager-application){:target="_blank"}, [`tdsConfig`](digested_passwords.html#configure-tomcat-to-use-digested-passwords){:target="_blank"} and [tdsMonitor](digested_passwords.html#configure-tomcat-to-use-digested-passwords){:target="_blank"}.
 * Create a [user](tomcat_manager_app.html#granting-access-to-the-manager-application){:target="_blank"} with the [digested password](digested_passwords.html#configure-tomcat-to-use-digested-passwords){:target="_blank"} with access to the `manager-gui`, `tdsConfig`, and `tdsMonitor` roles.
6. If you choose to use the Tomcat `manager` application, modify the [deployment descriptor](secure_manager_app.html){:target="_blank"} (`${tomcat_home}/webapps/manager/WEB-INF/web.xml`) to force access to occur only via HTTPS.
7. Remove all [unused web applications](remove_unused_webapps.html){:target="_blank"} from the `${tomcat_home}/webapps` directory.
8. Modify the [permissions of `${tomcat_home}`](tomcat_permissions.html){:target="_blank"} to restrict access.

#### TDS
1. [Download the TDS WAR](https://www.unidata.ucar.edu/downloads/thredds/index.jsp){:target="_blank"} file.
2. If needed, [rename the WAR file](deploying_the_tds.html){:target="_blank"} to `thredds.war`.
3. [Deploy the `thredds.war`](deploying_the_tds.html){:target="_blank"} file to the `${tomcat_home}/webapps` directory.
4. Start/restart Tomcat so that it has a chance to create initial files in [`${tomcat_home}/content/thredds`](tds_content_directory.html){:target="_blank"}.
5. [Modify `${tomcat_home}/content/thredds/catalog.xml`](default_config_catalog.html#default-tds-root-catalog){:target="_blank"} for your site.
6. Modify `${tomcat_home}/content/thredds/threddsConfig.xml` for your site in the following manner:
 * Add the needed information to the [`ServerInformation` element](basic_tds_configuration.html#server-information){:target="_blank"}.
 * [Enable any other optional services](adding_ogc_iso_services.html){:target="_blank"}  like WMS or WCS.
 
#### Continued Maintenance
1. Be sure to periodically check to make sure you are running the [latest versions of Java, Tomcat and the TDS](keep_software_uptodate.html){:target="_blank"}.
2. If you have enabled [access logging](tomcat_access_log.html){:target="_blank"} (and you should), zip up the unused access logs in `${tomcat_home}/logs/` and archive them off to another directory.
3. Likewise, zip up the unused [TDS servlet logs](tds_logs.html){:target="_blank"} in `${tomcat_home}/content/thredds/logs` and archive them as well.
4. Manually rotate Tomcat's [`catalina.out`](tomcat_log_files.html#things-to-know-about-catalinaout){:target="_blank"} log file when it grows too large.

#### Upgrading the TDS

{%include important.html content="
When installing a new `thredds.war`, everything in `${tomcat_home}/webapps/thredds` is overwritten. However, nothing in `${tomcat_home}/content/` is overwritten.
" %}

#### Upgrading Tomcat
{%include important.html content="
If you are using the Tomcat `manager` application, you will need to <a href=\"secure_manager_app.html#enabling-tlsssl-for-the-tomcat-manager-application\" target=\"_blank\">modify the deployment descriptor</a> to enable access via HTTPS only.
" %}