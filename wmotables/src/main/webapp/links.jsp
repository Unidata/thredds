<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>
   <title><spring:message code="global.title"/>: Links and Resources</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>

  <h3>Links and Resources</h3>



   <h5>Web Services</h5>
   <p><b>BUFR Message Validator Services</b></p>
   <ul>
    <li>Unidata BUFR Record Validator (coming soon)</li>
    <li><a href="http://www.ecmwf.int/products/data/d/check/">ECMWF BUFR/CREX format checker</a></li>
   </ul>
   <p><b>bufrtables email list</b></p>
   <p>An informal technical working group for developers of software that read and write  BUFR messages. </p>
   <ul>
    <li><a href="mailto:bufrtables@unidata.ucar.edu">Add or remove</a> yourself to the <strong>bufrtables</strong> email list</li>
    <li><a href="http://www.unidata.ucar.edu/mailing_lists/archives/bufrtables/">Archives</a></li>
   </ul>


   <h5>Documentation</h5>
   <p><b>WMO</b></p>
   <ul>
    <li><a href="http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html">Latest WMO Tables</a> - <a href="http://www.wmo.int/pages/disclaimer/copyright_en.html">WMO copyright</a> applies to these files</li>
    <li><a href="http://www.wmo.int/pages/prog/www/WMOCodes.html">WMO Codes</a></li>
    <li><a href="http://www.wmo.int/pages/prog/www/WMOCodes/Guides/BUFRCREXPreface_en.html">Guide to WMO Table-Driven      Code Forms</a></li>
    <li><a href="http://www.wmo.ch/pages/prog/www/WDM/Guides/Guide-binary-1B.html">GUIDE TO THE CODE FORM FM-94 BUFR</a></li>
   </ul>
   <p><b>NCEP</b></p>
   <ul>
    <li><a href="http://www.emc.ncep.noaa.gov/mmb/data_processing/data_processing/">Observational  Data  Processing  at NCEP</a></li>
    <li><a href="http://www.emc.ncep.noaa.gov/mmb/data_processing/">Data Processing Pages</a></li>
    <li><a href="http://www.nco.ncep.noaa.gov/sib/decoders/BUFRLIB/">BUFRLIB decoder</a></li>
     <ul>
      <li><a href="http://www.nco.ncep.noaa.gov/pmb/codes/nwprod/fix/">NCEP internal tables</a></li>
      <li><a href="http://www.nco.ncep.noaa.gov/sib/decoders/BUFRLIB/toc/dfbftab/">Description and Format of NCEP internal format BUFR Tables</a></li>
     </ul>
    <li><a href="http://www.nco.ncep.noaa.gov/pmb/docs/on388/">NCEP GRIB1 Documentation / Tables</a>  </li>
    <li><a href="http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml">NCEP GRIB2 Documentation / Tables</a></li>
    <li>GRIB Decoders</li>
     <ul>
      <li><a href="http://www.cpc.ncep.noaa.gov/products/wesley/wgrib.html">wgrib</a> (GRIB1)</li>
      <li><a href="http://www.cpc.ncep.noaa.gov/products/wesley/wgrib2/">wgrib2</a> (GRIB2</li>
      <li><a href="http://www.nws.noaa.gov/mdl/degrib/">degrib</a> National Digital Forecast Database (NDFD) GRIB2 decoder <a href="http://www.nws.noaa.gov/mdl/degrib/"></a></li>
     </ul>
   </ul>
   <p><b>ECMWF</b></p>
   <ul>
    <li><a href="http://www.ecmwf.int/products/data/software/bufr.html">BUFRDC encoding/decoding   software</a></li>
    <li><a href="https://software.ecmwf.int/wiki/display/GRIB/Home">GRIB-API library</a></li>
   </ul>
   <p><b>Environment Canada </b></p>
   <ul>
    <li><a href="https://launchpad.net/libecbufr">BUFR Library</a></li>
   </ul>
   <p><b>INOE / CPTEC (Brazil)</b></p>
   <ul>
    <li><a href="http://downloads.cptec.inpe.br/publicacoes/distribuicao.jsp">MBUFR Tools</a></li>
   </ul>
   <p><b>Northern Lighthouse</b></p>
   <ul>
    <li><a href="http://www.northern-lighthouse.com/cipher/bufrtool.html">Cipher BUFRtool</a></li>
    <li><a href="http://www.northern-lighthouse.com/tables/bufr_tables.html">BUFR Tables</a></li>
    <li>BUFR: A METEOROLOGICAL CODE FOR THE 21ST CENTURY (<a href="http://www.northern-lighthouse.com/Karhila_2010.pdf">paper</a>)</li>
   </ul>
   <p><b><a href="http://www.knmi.nl/opera/">EUMETNET OPERA</a></b></p>
   <ul>
    <li><a href="http://www.knmi.nl/opera/bufr.html">BUFR Software</a> for radar data (No new releases of this software are foreseen, and development is stopped.)</li>
   </ul>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
