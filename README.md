App for logging field observation

The initial intended use of this app is to log animals behaviour in field. A number of animals can be associated to 
specific behaviour types at specific times. The data are uploaded to a server, but also cached on the device if the 
net connection should be down.

The app may be used in all cases where some classes should be associated to some group at a given time. E.g. the classes 
could be brands of cars and the groups different colors. When one see a White Ford, one can then drag "Ford" to "White" 
and then press confirm to have the registration uploaded. The confirm button should be pressed within 20 seconds after 
the classification is done. Up to 20 seconds after "Confirm" is pressed, the registration may be undone - that is an undo
message is sent to the server. 

V 1.0 February 27 2015

Can register behaviour for animals
User Interface made to minimise the risk of unintended registrations
Possible to undo a registration

V 1.1 March 1 2015
User settable upload url
User settable drag and drop names
Uuid for installation
User settable user name (used in the uploads)
User settable timeout for Confirm or Undo

Planned:

User settable data
- Turn of uploading, only store data locally


GPS
Make it possible to log the position where an observation is done. (this should be user-selectable)

- Upload settings data from server
  Values for the drag and drop fields could be stored on server and fetched. Use the project name
  to select if there are several possible sets.

