---
title: Block Non-Essential Port Access At The Firewall
last_updated: 2018-10-13
sidebar: tdsTutorial_sidebar
toc: false
permalink: block_nonessential_ports.html
---

### Rationale
* It is easy to issue commands to Tomcat if you know:
  1. the correct port number; and
  2. the command expected on that port.
* Unless you are on a private network, you need a firewall to restrict who is allowed to access network ports.
* We recommend working with your systems/network administrator to block access to all non-essential ports at the firewall.

### For running the TDS, keep in mind the following:
* Port `8080` should have unrestricted access unless you plan to proxy requests to Tomcat from and HTTP server.
* If you are using any of the TDS monitoring and debugging tools, or the Tomcat `manager` application, you must also open up port `8443`.

### Resources
* Your local systems/network administrator:

  {% include image.html file="tds/tutorial/production_servers/super.png" alt="super sys admin" caption="" %}
