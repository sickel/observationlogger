See also http://sickel.net/blogg/?p=1841

App for logging field observation

The initial intended use of this app is to log animals behaviour in field. A number of animals can be associated to 
specific behaviour types at specific times. The data are uploaded to a server, but also cached on the device if the 
net connection should be down.

The app may be used in all cases where some classes should be associated to some group at a given time. E.g. the classes could be brands of cars and the groups different colors. When one see a White Ford, one can then drag "Ford" to "White"and then press confirm to have the registration uploaded. The confirm button should be pressed within 20 seconds after the classification is done. Up to 20 seconds after "Confirm" is pressed, the registration may be undone, that is an undo message is sent to the server. 
The observation data and possible undos are also stored locally to avoid data loss if the mobile phone is outside coverage.


v 1.6 March 27 2016 (not yet on google play)

* Fixed some bugs

* Can reenable Confirm-button

* Ad hoc drop or freetext comment

* Count down timer with (selectable) vibration and visual display

* Reminder to fill in user name and project

* Working default upload url. data can be viwed at http://sickel.net/obslog/show.php (no guarantee for real work, just for usability testing)

* Various clean up.


Bugs:

* Not clear what happens when GPS still have not got a fix or if fixes are too old 


Planned:

User settable data
  
* Turn of uploading, only store data locally

* Send data by email

* Export data to kml
  
* Select to use GPS (in case only time and observation of interest)

* turn off GPS.

* In case of periodical logging. Turn off GPS (e.g.) two minutes after last observation and turn it on again two minutes before next planned observation.

 
Upload settings data from server

*  Values for the drag and drop and time out values fields could be stored on server and fetched. Use the project name to select which if there are several possible sets.


Upload photos

*   "A picture speaks more than 1000 words" - sometimes
  	Can just select a picture to upload


Misc

* RESTcompatible. use POST to upload, GET when fetching data.


Development history


V 1.0 February 27 2015

Can register behaviour for animals

User Interface made to minimise the risk of unintended registrations

Possible to undo a registration



V 1.1 March 1 2015

User settable upload url

User settable drag and drop names

Uuid for installation

User settable user name (used in upload)

User settable timeout for Confirm or Undo

User settable project name (used in upload)


V 1.2 March 3 2015

* Using gps to log the position where an observation is done.



V 1.3 March 6 2015

* Upload of stored data is working, although it must be easier to see if there are any data that are not uploaded



V 1.4 March 7 2015

* Displaying if there are points that are not uploaded

* Code cleaning


v 1.5 March 8 2015 

* Two level selection.

* New icon

* Code cleaning

