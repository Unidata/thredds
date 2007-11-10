/*  Copyright (c) 2000-2004 Yale University. All rights reserved.
  *  See full notice at end.
  */

package thredds.servlet.restrict;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import edu.yale.its.tp.cas.client.*;

/**
 * <p>Protects web-accessible resources with CAS.</p>
 * <p/>
 * <p>The following filter initialization parameters are declared in
 * <code>web.xml</code>:</p>
 * <p/>
 * <ul>
 * <li><code>edu.yale.its.tp.cas.client.filter.loginUrl</code>: URL to
 * login page on CAS server.  (Required)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.validateUrl</code>: URL
 * to validation URL on CAS server.  (Required)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.serviceUrl</code>: URL
 * of this service.  (Required if <code>serverName</code> is not
 * specified)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.serverName</code>: full
 * hostname with port number (e.g. <code>www.foo.com:8080</code>).
 * Port number isn't required if it is standard (80 for HTTP, 443 for
 * HTTPS).  (Required if <code>serviceUrl</code> is not specified)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.authorizedProxy</code>:
 * whitespace-delimited list of valid proxies through which authentication
 * may have proceeded.  One one proxy must match.  (Optional.  If nothing
 * is specified, the filter will only accept service tickets &#150; not
 * proxy tickets.)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.proxyCallbackUrl</code>:
 * URL of local proxy callback listener used to acquire PGT/PGTIOU.
 * (Optional.)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.renew</code>: value of
 * CAS "renew" parameter.  Bypasses single sign-on and requires user
 * to provide CAS with his/her credentials again.  (Optional.  If nothing
 * is specified, this defaults to false.)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.gateway</code>: value of
 * CAS "gateway" parameter.  Redirects initial call through CAS and if
 * the user has logged in, validates the ticket on return.  If the user
 * has not logged in, returns to the web application without setting
 * the <code>CAS_FILTER_USER</code> variable.  Note that once a redirect
 * through CAS has occurred, the filter will not automatically try again
 * to log the user in.  You can then either provide an explicit CAS login
 * link (<code>https://cas-server/cas/login?service=http://your-app</code>)
 * or set up two instances of the filter mapped to different paths.  One
 * instance would have gateway=true, the other wouldn't.  When you need
 * the user to be logged in, direct him/her to the path of the other
 * filter.</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.wrapRequest</code>:
 * wrap the <code>HttpServletRequest</code> object, overriding the
 * <code>getRemoteUser()</code> method.  When set to "true",
 * <code>request.getRemoteUser()</code> will return the username of the
 * currently logged-in CAS user.  (Optional.  If nothing is specified,
 * this defaults to false.)</li>
 * </ul>
 * <p/>
 * <p>The logged-in username is set in the session attribute defined by
 * the value of <code>CAS_FILTER_USER</code> and may be accessed from within
 * your application either by setting <code>wrapRequest</code> and calling
 * <code>request.getRemoteUser()</code>, or by calling
 * <code>session.getAttribute(CASFilter.CAS_FILTER_USER)</code>.</p>
 * <p/>
 * <p>If <code>proxyCallbackUrl</code> is set, the URL will be passed to
 * CAS upon validation.  If the callback URL is valid, it will receive a
 * CAS PGT and a PGTIOU.  The PGTIOU will be returned to this filter and
 * will be accessible through the session attribute,
 * <code>CASFilter.CAS_FILTER_PGTIOU</code>.  You may then acquire
 * proxy tickets to other services by calling
 * <code>edu.yale.its.tp.cas.proxy.ProxyTicketReceptor.getProxyTicket(pgtIou, targetService)</code>.
 *
 * @author Shawn Bayern
 * @author Drew Mazurek
 * @author andrew.petro@yale.edu
 *         <p/>
 *         modified for TDS jcaron 1/23/07
 */

public class CASAuthorizer implements Authorizer {

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CASAuthorizer.class);

  // Filter initialization parameters

  /**
   * The name of the filter initialization parameter the value of which should be the https: address
   * of the CAS Login servlet.  Optional parameter, but required for successful redirection of unauthenticated
   * requests to authentication.
   */
  public final static String LOGIN_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.loginUrl";

  /**
   * The name of the filter initialization parameter the value of which must be the https: address
   * of the CAS Validate servlet.  Must be a CAS 2.0 validate servlet (CAS 1.0 non-XML won't suffice).
   * Required parameter.
   */
  public final static String VALIDATE_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.validateUrl";

  /**
   * The name of the filter initialization parameter the value of which must be the address
   * of the service this filter is filtering.  The filter will use this as
   * the service parameter for CAS login and validation. Either this parameter or SERVERNAME_INIT_PARAM must be set.
   */
  public final static String SERVICE_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.serviceUrl";

  /**
   * The name of the filter initialization parameter the vlaue of which must be the server name,
   * e.g. www.yale.edu , of the service this filter is filtering.  The filter will construct from this name
   * and the request the full service parameter for CAS login and validation.
   */
  public final static String SERVERNAME_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.serverName";

  /**
   * The name of the filter initialization parameter the value of which must be the String
   * that should be sent as the "renew" parameter on the request for login and validation.
   * This should either be "true" or not be set.  It is mutually exclusive with GATEWAY.
   */
  public final static String RENEW_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.renew";

  /**
   * The name of the filter initialization parameter the value of which must be a whitespace
   * delimited list of services (ProxyTicketReceptors) authorized to proxy authentication to the
   * service filtered by this Filter.  These must be https: URLs.  This parameter is optional -
   * not setting it results in no proxy tickets being acceptable.
   */
  public final static String AUTHORIZED_PROXY_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.authorizedProxy";

  /**
   * The name of the filter initialization parameter the value of which must be the https: URL
   * to which CAS should send Proxy Granting Tickets when this filter validates tickets.
   */
  public final static String PROXY_CALLBACK_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.proxyCallbackUrl";

  /**
   * The name of the filter initialization parameter the value of which indicates
   * whether this filter should wrap requests to expose the authenticated username.
   */
  public final static String WRAP_REQUESTS_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.wrapRequest";

  /**
   * The name of the filter initialization parameter the value of which is the value the Filter
   * should send for the gateway parameter on the CAS login request.
   */
  public final static String GATEWAY_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.gateway";

  // Session attributes used by this filter

  /**
   * <p>Session attribute in which the username is stored.</p>
   */
  public final static String CAS_FILTER_USER = "edu.yale.its.tp.cas.client.filter.user";

  /**
   * Session attribute in which the CASReceipt is stored.
   */
  public final static String CAS_FILTER_RECEIPT = "edu.yale.its.tp.cas.client.filter.receipt";

  /**
   * Session attribute in which internally used gateway
   * attribute is stored.
   */
  private static final String CAS_FILTER_GATEWAYED = "edu.yale.its.tp.cas.client.filter.didGateway";

  //*********************************************************************
  // Configuration state

  /**
   * Secure URL whereat CAS offers its login service.
   */
  private String casLogin;
  /**
   * Secure URL whereat CAS offers its CAS 2.0 validate service
   */
  private String casValidate;
  /**
   * Filtered service URL for use as service parameter to login and validate
   */
  private String casServiceUrl;
  /**
   * Name of server, for use in assembling service URL for use as service parameter to login and validate.
   */
  private String casServerName;
  /**
   * Secure URL whereto this filter should ask CAS to send Proxy Granting Tickets.
   */
  private String casProxyCallbackUrl;

  /**
   * True if renew parameter should be set on login and validate
   */
  private boolean casRenew;

  /**
   * True if this filter should wrap requests to expose authenticated user as getRemoteUser();
   */
  private boolean wrapRequest;

  /**
   * True if this filter should set gateway=true on login redirect
   */
  private boolean casGateway = false;

  /**
   * List of ProxyTicketReceptor URLs of services authorized to proxy to the path
   * behind this filter.
   */
  private List<String> authorizedProxies = new ArrayList<String>();

  //*********************************************************************
  // Initialization

  public CASAuthorizer() {
  }

  public void init(HttpServlet config) throws ServletException {

    casLogin = config.getInitParameter(LOGIN_INIT_PARAM);
    casValidate = config.getInitParameter(VALIDATE_INIT_PARAM);
    casServiceUrl = config.getInitParameter(SERVICE_INIT_PARAM);
    String casAuthorizedProxy = config.getInitParameter(AUTHORIZED_PROXY_INIT_PARAM);
    casRenew = Boolean.valueOf(config.getInitParameter(RENEW_INIT_PARAM));
    casServerName = config.getInitParameter(SERVERNAME_INIT_PARAM);
    casProxyCallbackUrl = config.getInitParameter(PROXY_CALLBACK_INIT_PARAM);
    wrapRequest = Boolean.valueOf(config.getInitParameter(WRAP_REQUESTS_INIT_PARAM));
    casGateway = Boolean.valueOf(config.getInitParameter(GATEWAY_INIT_PARAM));

    if (casGateway && casRenew) {
      throw new ServletException("gateway and renew cannot both be true in filter configuration");
    }
    if (casServerName != null && casServiceUrl != null) {
      throw new ServletException("serverName and serviceUrl cannot both be set: choose one.");
    }
    if (casServerName == null && casServiceUrl == null) {
      throw new ServletException("one of serverName or serviceUrl must be set.");
    }
    if (casServiceUrl != null) {
      if (!(casServiceUrl.startsWith("https://") || (casServiceUrl.startsWith("http://")))) {
        throw new ServletException("service URL must start with http:// or https://; its current value is [" + casServiceUrl + "]");
      }
    }

    if (casValidate == null) {
      throw new ServletException("validateUrl parameter must be set.");
    }

    if (!casValidate.startsWith("https://")) {
      throw new ServletException("validateUrl must start with https://, its current value is [" + casValidate + "]");
    }

    if (casAuthorizedProxy != null) {

      // parse and remember authorized proxies
      StringTokenizer casProxies = new StringTokenizer(casAuthorizedProxy);
      while (casProxies.hasMoreTokens()) {
        String anAuthorizedProxy = casProxies.nextToken();
        if (!anAuthorizedProxy.startsWith("https://")) {
          throw new ServletException("CASFilter initialization parameter for authorized proxies " +
                  "must be a whitespace delimited list of authorized proxies.  " +
                  "Authorized proxies must be secure (https) addresses.  This one wasn't: [" + anAuthorizedProxy + "]");
        }
        this.authorizedProxies.add(anAuthorizedProxy);
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(("CASFilter initialized as: [" + toString() + "]"));
    }
  }

  private RoleSource db;

  public void setRoleSource(RoleSource db) {
    this.db = db;
  }

  //*********************************************************************

  public boolean authorize(HttpServletRequest request, HttpServletResponse response, String role) throws ServletException, IOException {

    if (log.isDebugEnabled()) {
      log.debug("entering authorize()");
    }

    HttpSession session = request.getSession();

    // if our attribute's already present and valid, pass through the filter chain
    CASReceipt receipt = (CASReceipt) session.getAttribute(CAS_FILTER_RECEIPT);
    if (receipt != null && isReceiptAcceptable(receipt)) {
      log.debug("CAS_FILTER_RECEIPT attribute was present and acceptable..");
      boolean ok = authorize(receipt, role);
      if (!ok) response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return ok;
    }

    // otherwise, we need to authenticate via CAS
    String ticket = request.getParameter("ticket");

    // no ticket?  abort request processing and redirect
    if (ticket == null || ticket.equals("")) {
      log.debug("CAS ticket was not present on request.");
      // did we go through the gateway already?
      boolean didGateway = Boolean.valueOf((String) session.getAttribute(CAS_FILTER_GATEWAYED));

      if (casLogin == null) {
        //TODO: casLogin should probably be ensured to not be null at filter initialization. -awp9
        log.error("casLogin was not set, so filter cannot redirect request for authentication.");
        throw new ServletException(
                "When CASFilter protects pages that do not receive a 'ticket' "
                        + "parameter, it needs a edu.yale.its.tp.cas.client.filter.loginUrl "
                        + "filter parameter");
      }

      if (!didGateway) {
        log.debug("Did not previously gateway.  Setting session attribute to true.");
        session.setAttribute(CAS_FILTER_GATEWAYED, "true");
        redirectToCAS(request, response);
        // abort chain
        return false;
      } else {
        log.debug("Previously gatewayed.");
        // if we should be logged in, make sure validation succeeded
        if (casGateway || session.getAttribute(CAS_FILTER_USER) != null) {
          log.debug("casGateway was true and CAS_FILTER_USER set: ok.");
          boolean ok = authorize(receipt, role);
          if (!ok) response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return ok;

        } else {
          // unknown state... redirect to CAS
          session.setAttribute(CAS_FILTER_GATEWAYED, "true");
          redirectToCAS(request, response);
          // abort chain
          return false;
        }
      }
    }


    try {
      receipt = getAuthenticatedUser(request);
    } catch (CASAuthenticationException e) {
      log.error("failed to authenticate", e);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
      //throw new ServletException(e);
    }

    if (!isReceiptAcceptable(receipt)) {
      log.error("Authentication was technically successful but rejected as a matter of policy. [" + receipt + "]");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
      //throw new ServletException("Authentication was technically successful but rejected as a matter of policy. [" + receipt + "]");
    }

    // Store the authenticated user in the session
    if (session != null) { // probably unnecessary
      session.setAttribute(CAS_FILTER_USER, receipt.getUserName());
      session.setAttribute(CAS_FILTER_RECEIPT, receipt);
      // don't store extra unnecessary session state
      session.removeAttribute(CAS_FILTER_GATEWAYED);
    }
    if (log.isDebugEnabled()) {
      log.debug("validated ticket to get authenticated receipt [" + receipt + "], now passing request along filter chain.");
    }

    boolean ok = authorize(receipt, role);
    if (!ok) response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return ok;
  }

  private boolean authorize(CASReceipt receipt, String role) {
    // see if its authorized for the given role
    if (db == null) return true;
    String username = receipt.getUserName();
    return db.hasRole(username, role);
  }

  // not used
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
  }

  /**
   * Is this receipt acceptable as evidence of authentication by
   * credentials that would have been acceptable to this path?
   * Current implementation checks whether from renew and whether proxy
   * was authorized.
   *
   * @param receipt CASReceipt
   * @return true if acceptable, false otherwise
   */
  private boolean isReceiptAcceptable(CASReceipt receipt) {
    if (receipt == null)
      throw new IllegalArgumentException("Cannot evaluate a null receipt.");
    if (this.casRenew && !receipt.isPrimaryAuthentication()) {
      return false;
    }
    if (receipt.isProxied()) {
      if (!this.authorizedProxies.contains(receipt.getProxyingService())) {
        return false;
      }
    }
    return true;
  }

  //*********************************************************************
  // Utility methods

  /**
   * Converts a ticket parameter to a CASReceipt, taking into account an
   * optionally configured trusted proxy in the tier immediately in front
   * of us.
   *
   * @param request the request
   * @return CASReceipt
   * @throws ServletException           - when unable to get service for request
   * @throws CASAuthenticationException - on authentication failure
   */
  private CASReceipt getAuthenticatedUser(HttpServletRequest request)
          throws ServletException, CASAuthenticationException {
    log.debug("entering getAuthenticatedUser()");
    ProxyTicketValidator pv;

    pv = new ProxyTicketValidator();
    pv.setCasValidateUrl(casValidate);
    pv.setServiceTicket(request.getParameter("ticket"));
    pv.setService(getService(request));
    pv.setRenew(casRenew);
    if (casProxyCallbackUrl != null) {
      pv.setProxyCallbackUrl(casProxyCallbackUrl);
    }
    if (log.isDebugEnabled()) {
      log.debug(
              "about to validate ProxyTicketValidator: [" + pv + "]");
    }

    return CASReceipt.getReceipt(pv);

  }

  /**
   * Returns either the configured service or figures it out for the current
   * request.  The returned service is URL-encoded.
   *
   * @param request the request
   * @return the service
   * @throws javax.servlet.ServletException why nott
   */
  private String getService(HttpServletRequest request)
          throws ServletException {

    log.debug("entering getService()");
    String serviceString;

    // ensure we have a server name or service name
    if (casServerName == null && casServiceUrl == null)
      throw new ServletException(
              "need one of the following configuration "
                      + "parameters: edu.yale.its.tp.cas.client.filter.serviceUrl or "
                      + "edu.yale.its.tp.cas.client.filter.serverName");

    // use the given string if it's provided
    if (casServiceUrl != null)
      serviceString = URLEncoder.encode(casServiceUrl);
    else
      // otherwise, return our best guess at the service
      serviceString = Util.getService(request, casServerName);
    if (log.isDebugEnabled()) {
      log.debug(
              "returning from getService() with service ["
                      + serviceString
                      + "]");
    }
    return serviceString;
  }

  // Redirects the user to CAS, determining the service from the request.
  private void redirectToCAS(
          HttpServletRequest request,
          HttpServletResponse response)
          throws IOException, ServletException {
    if (log.isDebugEnabled()) {
      log.debug("entering redirectToCAS()");
    }

    String casLoginString =
            casLogin
                    + "?service="
                    + getService(request)
                    + ((casRenew)
                    ? "&renew=true"
                    : "")
                    + (casGateway ? "&gateway=true" : "");

    if (log.isDebugEnabled()) {
      log.debug("Redirecting browser to [" + casLoginString + ")");
    }
    response.sendRedirect(casLoginString);

    if (log.isDebugEnabled()) {
      log.debug("returning from redirectToCAS()");
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[CASFilter:");
    sb.append(" casGateway=");
    sb.append(this.casGateway);
    sb.append(" wrapRequest=");
    sb.append(this.wrapRequest);

    sb.append(" casAuthorizedProxies=[");
    sb.append(this.authorizedProxies);
    sb.append("]");

    if (this.casLogin != null) {
      sb.append(" casLogin=[");
      sb.append(this.casLogin);
      sb.append("]");
    } else {
      sb.append(" casLogin=NULL!!!!!");
    }

    if (this.casProxyCallbackUrl != null) {
      sb.append(" casProxyCallbackUrl=[");
      sb.append(casProxyCallbackUrl);
      sb.append("]");
    }

    if (this.casRenew) {
      sb.append(" casRenew=true");
    }

    if (this.casServerName != null) {
      sb.append(" casServerName=[");
      sb.append(casServerName);
      sb.append("]");
    }

    if (this.casServiceUrl != null) {
      sb.append(" casServiceUrl=[");
      sb.append(casServiceUrl);
      sb.append("]");
    }

    if (this.casValidate != null) {
      sb.append(" casValidate=[");
      sb.append(casValidate);
      sb.append("]");
    } else {
      sb.append(" casValidate=NULL!!!");
    }

    return sb.toString();
  }

  /* (non-Javadoc)
  * @see javax.servlet.Filter#destroy()
  */
  public void destroy() {
    // TODO Auto-generated method stub

  }
}

/*
 *  Copyright (c) 2000-2004 Yale University. All rights reserved.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE EXPRESSLY
 *  DISCLAIMED. IN NO EVENT SHALL YALE UNIVERSITY OR ITS EMPLOYEES BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED, THE COSTS OF
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED IN ADVANCE OF THE POSSIBILITY OF SUCH
 *  DAMAGE.
 *
 *  Redistribution and use of this software in source or binary forms,
 *  with or without modification, are permitted, provided that the
 *  following conditions are met:
 *
 *  1. Any redistribution must include the above copyright notice and
 *  disclaimer and this list of conditions in any related documentation
 *  and, if feasible, in the redistributed software.
 *
 *  2. Any redistribution must include the acknowledgment, "This product
 *  includes software developed by Yale University," in any related
 *  documentation and, if feasible, in the redistributed software.
 *
 *  3. The names "Yale" and "Yale University" must not be used to endorse
 *  or promote products derived from this software.
 */

