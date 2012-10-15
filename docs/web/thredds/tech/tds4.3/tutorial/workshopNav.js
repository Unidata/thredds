 $(document).ready(function(){
   setWorkshopNav();
   
   function setWorkshopNav() {
      var pageTitle = $("title").html();
      var workshopNav = '<ul id="workshopNav">' + 
                     '<li><a href="/projects/THREDDS/tech/tutorial/workshop2012.html">TDS Workshop Home</a></li>' +
                     '<li>' + pageTitle + '</li>' +
                     '</ul>';

      $(workshopNav).prependTo('#container');
      $(workshopNav).appendTo('#container');
   }

});
