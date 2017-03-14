A simple swing application that provides a local implementation of playlist organization.

Allows you to use an inferior GUI as a trade-off for organizing playlists on your machine with export options.



## Motivation
I couldn't figure out what a song that got deleted in a playlist was.

## Installation
Get JDK 1.8 and maven working.
```mvn package``` should generate a jar and uber jar, ```target/YoutubeLinkManager-1-jar-with-depenencies.jar```.
Move the uber jar to a folder of your choice, rename it if you wish.
If you're keeping the source around, you can run ```mvn clean``` now.
Go get a Google APIs key if you want titles and lengths automatically fetched for you.
Your key will be saved in the serialized data structure, and can be updated or removed at any time.

## Usage
Playlists are saved by serializing the data structures in the same directory as the uber jar.
Output files are generated in the jar's directory.

## Features
* json, csv, and xml output
* automatic title and length fetching
* search across all playlists

## Planned
* GUI improvements
