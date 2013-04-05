<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>  
  <title><spring:message code="global.title"/>: BUFR Validation</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>

   <h3>BUFR Validation</h3>
   <h5>Background</h5>
   <p>Unidata has developed a 100% Java library for reading BUFR records (editions 2,3,4), as part of the <a href="http://www.unidata.ucar.edu/software/netcdf-java/">NetCDF-Java / CDM </a>(Common Data Model) library. For BUFR to be used across institutions or to archive data, we  need  canonical, standardized, machine-readable BUFR tables, shared between producer and client software. A missing table entry makes an entire BUFR message unreadable. An incorrect entry may make the BUFR record unreadable, or  worse, silently give incorrect data values. </p>
   <p>We are publishing our BUFR tables in machine-readable format along with a <a href="validate.html">validation service</a> that allows users to upload BUFR records and test them against our reader. In this way we hope  that centers/users will help verify that our tables and software are correct, validate their BUFR formats, and contribute their own local tables as needed. Ideally a canonical repository of machine-readable BUFR tables will eventually be  hosted by the WMO. </p>
   <p><em>What follows is our current understanding of BUFR. Please send corrections and comments to <a href="mailto:caron@unidata.ucar.edu">John Caron</a> or to the <a href="mailto:bufrtables@unidata.ucar.edu">bufrtables email list</a> for discussion (you must <a href="http://www.unidata.ucar.edu/support/mailinglist/mailing-list-form.html">sign up</a> first).</em></p>
   <h5>Standard WMO BUFR Tables </h5>
   <p>Specific areas of BUFR tables B and D are reserved for <em><strong>local data descriptors</strong></em>, namely categories 48-63 inclusive, and entries 192-255 inclusive (cf p L3-49 of the <a href="http://www.wmo.int/pages/prog/www/WMOCodes/Guides/BUFRCREXPreface_en.html">BUFR Guide</a>). All other entries in tables B and D are  reserved for globally shared descriptors, which we call <strong><em> standard WMO data descriptors</em></strong>. The <a href="http://www.wmo.int/pages/prog/www/WMOCodes/TDCFtables.html">standard WMO tables</a> are available from the WMO in Microsoft Word and PDF format. Extracting   tables from these formats is an error-prone and time-consuming process, that is to say, neither Word nor PDF is  machine readable  in this context. However, there is now an effort at WMO to publish their tables in Excel format, which are easy to extract programatically. </p>
   <p>Past practice at national centers in creating local data descriptors and proposing new standard descriptors has been confusing. After some discussion, we think that the following are the agreed-upon assumptions on how to handle BUFR Tables:</p>
   <ul>
    <li>BUFR tables do not depend on the edition number of the BUFR record</li>
    <li>Local tables cannot  override  the standard WMO data descriptors.</li>
    <li>All table versions up to and including version 13 are  compatible with WMO table version 13. Therefore a reader can use WMO table version 13 for all BUFR messages with version 13 or less.</li>
    <li>WMO table versions 14 and later may have incompatible changes. BUFR messages using version 14 and hight must encode the correct version number in order to be decoded properly.</li>
   </ul>
   <h5>BUFR Message Validator Services</h5>
   <ul>
    <li><a href="validate.html">Unidata BUFR Record Validator</a></li>
    <li><a href="http://www.ecmwf.int/products/data/d/check/">ECMWF BUFR/CREX format checker</a> </li>
   </ul>
   <h5>The bufrtables email list</h5>
   <p>An informal technical working group for developers of software that read and write  BUFR messages. </p>
   <ul>
    <li><a href="mailto:bufrtables@unidata.ucar.edu">Add or remove</a> yourself on the <strong>bufrtables</strong> email list</li>
    <li><a href="http://www.unidata.ucar.edu/mailing_lists/archives/bufrtables/">Archives</a><a href="http://www.unidata.ucar.edu/mailing_lists/archives/bufrtables/"></a></li>
   </ul>
   <hr />
   <p>&nbsp;</p>
   <h3>Using BUFR Validator as a Web Service</h3>
   <p>You can use the BUFR Validator  from a software program that sends URLs and gets back XML. The BUFR Validator is a web service using <a href="http://en.wikipedia.org/wiki/Representational_State_Transfer">REST design principles</a>. </p>
   <h5>Validation</h5>
   <p>The file to be validated is uploaded to the BUFR validation service, and kept on the server for a period of time (currently about an hour). The Forms based validation uses multipart-mime file uploading, which your software can also use if you want. An easier way to use the service is to place the file on your web server, and send the URL. An example:</p>
   <blockquote>
    <pre><strong> http://motherlode.ucar.edu:8080/bufrTables/validate/get?url=http%3A%2F%2Fwww.unidata.ucar.edu%2Fstaff%2Fcaron%2Ftest.bufr&amp;xml=true&amp;username=myName</strong></pre>
   </blockquote>
   <p>The validation service has the following parts in the URL: \</p>
   <ul>
    <li>The server name, in this case, it always starts with <strong>http://motherlode.ucar.edu:8080/bufrTables/</strong></li>
    <li>The service name: <strong>validate/get </strong></li>
    <li>Other information is sent as query parameters:
    <ul>
     <li><strong>url=myFileUrl</strong> is the URL of your file on your server. This must be </li>
     <li><strong>xml=true</strong> so that you get back XML instead of HTML</li>
     <li><strong>username=myName</strong>  makes sure you dont accidentally collide with anybody else using the service. just make myName something unique. </li>
    </ul>
    </li>
   </ul>
   <blockquote>
     <pre><strong> http://motherlode.ucar.edu:8080/bufrTables/validate/get
	   url=myURL
	   xml=true
	   username=myName</strong></pre>
   </blockquote>
   <p>Example result:</p>
   <blockquote>
    <pre>&lt;bufrValidation fileName=&quot;anon/http--www.unidata.ucar.edu-staff-caron-test.bufr&quot; fileSize=&quot;898&quot;&gt;<br />&minus;<br />&lt;bufrMessage status=&quot;ok&quot; record=&quot;0&quot; pos=&quot;30&quot; dds=&quot;ok&quot; size=&quot;fail&quot; nobs=&quot;5&quot;&gt;<br />&lt;ByteCount&gt;countBytes 782 != 780 dataSize&lt;/ByteCount&gt;<br />&lt;BitCount&gt;countBits 6216 != 6208 dataSizeBits&lt;/BitCount&gt;<br />&lt;WMOheader&gt;IUSV51 KWBC&lt;/WMOheader&gt;<br />&lt;center&gt;7.0 (US National Weather Service (NCEP))&lt;/center&gt;<br />&lt;category&gt;Vertical soundings (other than satellite) (2.0.0)&lt;/category&gt;<br />&lt;date&gt;2008-07-08T00:00:00Z&lt;/date&gt;<br />&lt;/bufrMessage&gt;<br />&lt;/bufrValidation&gt;</pre>
   </blockquote>
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
