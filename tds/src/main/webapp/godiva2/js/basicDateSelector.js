/* 
 * A very basic date selector consisting of drop-down boxes for year, month
 * and day.  Will work independent of the calendar system, hence is useful
 * for 360-day calendars
 * @author Jon Blower
 * @date 24th March 2010
 */

// Uses the Prototype library
var BasicDateSelector = Class.create
({
    initialize: function(obj)
    {
        this.callback = obj.callback;

        this.yearSel  = document.createElement("select");
        this.monthSel = document.createElement("select");
        this.daySel   = document.createElement("select");

        this.container = document.createElement('span');
        this.container.appendChild(this.yearSel);
        this.container.appendChild(this.monthSel);
        this.container.appendChild(this.daySel);

        // Every time we change one of the year, month or day fields, we'll call
        // onDateChange().  Note that the .bind(this) part is very important to
        // ensure that the onDateChange() event knows that "this" refers to
        // this BasicDateSelector.
        this.yearSel .observe('change', this.onYearChange.bind(this));
        this.monthSel.observe('change', this.onMonthChange.bind(this));
        this.daySel  .observe('change', this.onDateChange.bind(this));
    },

    // dateTime = ISO representation of the date/time we wish to set this to
    // after initialization
    setup: function(datesWithData, dateTime)
    {
        this.datesWithData = datesWithData;
        // Extract the year, month and day from the ISO string
        var dateTimeEls = dateTime.split('-');
        var yearToSet = parseInt(dateTimeEls[0]);
        var monthToSet = parseInt(dateTimeEls[1]);
        var dayToSet = parseInt(dateTimeEls[2]);

        this.setupYears(yearToSet, monthToSet, dayToSet);
    },

    setupYears: function(yearToSet, monthToSet, dayToSet)
    {
        this.yearSel.options.length = 0;
        var i = 0;
        for (var year in this.datesWithData) {
            this.yearSel.options[i] = new Option(year, year);
            if (year == yearToSet) {
                this.yearSel.options[i].selected = true;
            }
            i++;
        }
        this.setupMonths(monthToSet, dayToSet);
    },

    setupMonths: function(monthToSet, dayToSet)
    {
        this.monthSel.options.length = 0;
        var i = 0;
        var monthsWithData = this.datesWithData[this.yearSel.value];
        for (var month in monthsWithData) {
            // We must add 1 to the month number as datesWithData uses a
            // zero-based month for compatibility with Javascript dates
            month = parseInt(month) + 1;
            this.monthSel.options[i] = new Option(month, month);
            if (month == monthToSet) {
                this.monthSel.options[i].selected = true;
            }
            i++;
        }
        this.setupDays(dayToSet);
    },

    setupDays: function(dayToSet)
    {
        this.daySel.options.length = 0;
        var daysWithData = this.datesWithData[this.yearSel.value][this.monthSel.value - 1];
        // Day numbers are stored in an array
        for (var i = 0; i < daysWithData.length; i++) {
            var day = daysWithData[i];
            this.daySel.options[i] = new Option(day, day);
            if (day == dayToSet) {
                this.daySel.options[i].selected = true;
            }
        }
    },

    // Called when the user changes the year field
    // Automatically updates the months field
    onYearChange: function(event)
    {
        // We'll try to keep the same month selected if we can
        this.setupMonths(this.monthSel.value, this.daySel.value);
        this.onDateChange();
    },

    // Called when the user changes the year field
    // Automatically updates the months field
    onMonthChange: function(event)
    {
        // We'll try to keep the same day selected if we can
        this.setupDays(this.daySel.value);
        this.onDateChange();
    },

    // Called when the user changes any of the fields.
    onDateChange: function(event)
    {
        this.callback(this.yearSel.value, this.monthSel.value, this.daySel.value);
    },

    show: function(el)
    {
        this.el = el;
        el.appendChild(this.container);
        this.container.style.visibility = 'visible';
    }
});

// Creates a BasicDateSelector at the given element in the DOM
function BasicDateSelector2(el)
{

    var callback = null; // Function that will be called with (year, month, day)
                          // when the selected date changes

    this.dateChange = function()
    {
        alert('dateChange: callback = ' + callback);
        if (callback != null) {
            callback(yearSel.value, monthSel.value, daySel.value);
        }
    }

    yearSel.onchange  = this.dateChange;
    monthSel.onchange = this.dateChange;
    daySel.onchange   = this.dateChange;


    this.setup = function(datesWithData)
    {
        var i = 0;
        for (var year in datesWithData) {
            yearSel.options[i] = new Option(year, year);
            i++;
        }
        var yr = yearSel.options[0].value;
        this.setupMonth(datesWithData[yr]);
    }

    this.setupMonth = function(monthsWithData)
    {
        var i = 0;
        for (var month in monthsWithData) {
            monthSel.options[i] = new Option(month, month);
            i++;
        }
        var mnth = monthSel.options[0].value;
        this.setupDay(monthsWithData[mnth]);
    }

    this.setupDay = function(daysWithData)
    {
        // Day numbers are stored in an array
        for (var i = 0; i < daysWithData.length; i++) {
            daySel.options[i] = new Option(daysWithData[i], daysWithData[i]);
        }
    }
}

