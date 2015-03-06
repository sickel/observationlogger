See also http://sickel.net/blogg/?p=1841

App for logging field observation

The initial intended use of this app is to log animals behaviour in field. A number of animals can be associated to 
specific behaviour types at specific times. The data are uploaded to a server, but also cached on the device if the 
net connection should be down.

The app may be used in all cases where some classes should be associated to some group at a given time. E.g. the classes could be brands of cars and the groups different colors. When one see a White Ford, one can then drag "Ford" to "White"and then press confirm to have the registration uploaded. The confirm button should be pressed within 20 seconds after the classification is done. Up to 20 seconds after "Confirm" is pressed, the registration may be undone, that is an undo message is sent to the server. 
The observation data and possible undos are also stored locally to avoid data loss if the mobile phone is outside coverage.

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

Using gps to log the position where an observation is done.

V 1.3 March 6 2015

Upload of stored data is working, although it must be easier to see if there are any data that are not uploaded


Bugs:


* Not possible to turn of GPS. 

* Not clear what happens when GPS still have not got a fix or if fixes are too old 


Planned:

User settable data
  
   Turn of uploading, only store data locally
  
   Select to use GPS


Upload settings data from server

  Values for the drag and drop fields could be stored on server and fetched. Use the project name  to select if there are several possible sets.

Locally stored data

   Make a system to make it easier to upload locally stored data that have not been uploaded

   View log of stored data
  
Use a second tab to show more alternatives

   In some cases there are several seldom used alternatives, those can be put on a second tab trigged by a drop on a     designated drop field


Make a text field for ad-hoc alternatives

   There may be a need to register ad-hoc alternatives or more description. 
   

Upload photos

   "A picture speaks more than 1000 words" - sometimes
