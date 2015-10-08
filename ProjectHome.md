# onsie #

## Description ##
An Uno! card game clone. A network server and client program. Will eventually include chat features.

Test server and client programs can be [downloaded](http://code.google.com/p/onsie/downloads/list). They are simple command line programs which demonstrate the current abilities of the programs. They work but are not robust.

## Using ##
Requirements to run:
  1. Java Runtime Environment (JRE) 1.6.0 or higher (may work with other versions, untested)

Requirements to build:
  1. Java Development Kit (JDK) 1.6.0\_14 (may work other versions, untested)
  1. Ant 1.7.1 (to use the included build.xml Could be built by hand without)

To run the server: `java -jar Server-rXX.jar [NUM]` where `[NUM]` is the number of players (defaults to 4).

To run the client: `java -jar Client-rXX.jar [HOST]` where `[HOST]` is the IP address or name of the server (defaults to `localhost`).

## News ##
  * Most recent working version is [r11](https://code.google.com/p/onsie/source/detail?r=11).
  * Client GUI is a work in progress. Images have been created for the cards. Initial loading of hand works. Revision [r28](https://code.google.com/p/onsie/source/detail?r=28) works up to a point.