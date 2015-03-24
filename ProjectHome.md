# When Androids Fly... #
## Summary ##
This project is about creating an android based quadcopter, **it's a work in progress and not functional yet**. The idea is to have a smartphone in the quad providing it with sensors, gps, camera and multi-threaded processing(the **controller**) and another smartphone acting as the _remote_ _control_ providing connectivity to the quad via wi-fi hotspot and user commands (the **remote**). The body of the quadcopter will be made with a 3D printer and the files will be available for download. Currently the smartphone model being used to develop the controller is an Xperia P (lt22i) and a Galaxy S3 (gt i9300) for the remote.

  * The controller phone is connected to a [IOIO board](http://github.com/ytai/ioio/wiki) in order to control the brushless motors, it uses its camera to create a fake video stream which is then sent to the remote. Fake video because we simply send preview frames to the remote to be displayed in sequence as pictures. It uses its orientation sensor and accelerometer for stabilization and flight control. It also receives commands from the remote such as take a high res picture, record video (both stored locally) and the flight commands (go forward, turn left, etc).

  * The remote phone creates a wireless hotspot for the controller to connect to (not part of the app itself). Through it the remote receives a stream of frames which it displays as a video, and sends commands to the quad. It has two virtual joysticks used for flight controls and two extra buttons used to take high res pictures and record videos.

---

## Current Status ##

### Controller ###

**_Completed_**
  * Streams video to the client
  * Receives commands from the client (movement and take picture)
  * Connects to the IOIO and uses received commands to change motor output

**_Missing_**
  * Use orientation and accelerometer data to stabilize the quad
  * Use orientation and accelerometer along with user commands to fly
  * Automatic ip detection between controller and remote
  * Code cleanup
  * Create proper icons

### Remote ###

**_Completed_**
  * Receives and displays video stream from the controller
  * Reads and sends input from both virtual joysticks to the controller
  * Sends take high res picture command to controller
  * Settings menu to change video resolution on the controller

**_Missing_**
  * Send record video command to the controller
  * Full screen video
  * Automatic ip detection between controller and remote
  * Code cleanup
  * Create proper icons

### Other ###
  * Body is currently being designed. The 3D printer is up and running again (yay).