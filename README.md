<b><h1> Airsim Control Car </h1></b>

Project in collaboration with Marianna Cossu for HUMAN COMPUTER INTERACTION

<b><h2>Targets</h2></b> 

The aim of this project is to control a car in a simulated environment using a Java application. 

![Class Diagram](Image/Structure.PNG)

A program written in c++ was used to connect the server and the Airsim application.

![Class Diagram](Image/ProgrammaCPP.PNG)

To transmit the information from the Android application to the server, we used the socket threads

![Class Diagram](Image/cell1.PNG) ![Class Diagram](Image/Cell2.PNG) 

The Android application transmits the following information

-   *Tilt*: Tilting the mobile phone to see whether the car should go left or right 
-   *Stopping button*: Allows the car to be stopped
-   *Avanti button*: Allows the car to accelerate

Through the camera, it is possible to tell whether the car is on the right track. In order to be able to recognise the colours, the openCV libraries were used. 

![Class Diagram](Image/final.PNG)
