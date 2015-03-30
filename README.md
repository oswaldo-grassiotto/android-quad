Summary

This project is about creating an android based quadcopter, it's a work in progress and not functional yet. The idea is to have a smartphone in the quad providing it with sensors, gps, camera and multi-threaded processing(the controller) and another smartphone acting as the remote control (the remote) providing a video stream from the quad and user commands to the quad through a regular wi-fi connection using the remote controller smartphone as the hotspot. The body of the quadcopter will be made with a 3D printer and the files will be available for download. Currently the smartphone model being used to develop the controller is an HTC One M8 (2014) and a Motorola Moto X (2014) for the remote.

The controller phone is connected to a IOIO board in order to control the brushless motors, it uses its camera to create a fake video stream which is then sent to the remote. Fake video because we simply send preview frames to the remote to be displayed in sequence as pictures (for now). It uses its orientation sensor and accelerometer for stabilization and flight control. It also receives commands from the remote such as take a high res picture, record video (both stored locally) and the flight commands (go forward, turn left, etc). 

The remote phone creates a wireless hotspot for the controller to connect to (not part of the app itself). That allows the remote receive a stream of frames which it then displays as a video, and sends commands to the quad. It has two virtual joysticks used for flight controls and two extra buttons used to take high res pictures and record videos. 

Current Status

Controller

Completed

    Streams video to the client
    Receives commands from the client (movement and take picture)
    Connects to the IOIO and uses received commands to change motor output 

Missing

    Use orientation and accelerometer data to stabilize the quad
    Use orientation and accelerometer along with user commands to fly
    Automatic ip detection between controller and remote
    Code cleanup
    Create proper icons 


Remote

Completed

    Receives and displays video stream from the controller
    Reads and sends input from both virtual joysticks to the controller
    Sends take high res picture command to controller
    Settings menu to change stream resolution on the controller 

Missing

    Send record video command to the controller
    Full screen video
    Automatic ip detection between controller and remote
    Code cleanup
    Create proper icons 

Other

    Body is currently being designed. The 3D printer is up and running again (yay). 
