# Catena

CATENA v1.0 README
Updated Jan 9th, 2017 by Flavio Esposito

All feedback appreciated to espositof@slu.edu
 

@copyright 2017 Computer Science Department laboratory, Saint Louis University.  
=================
All rights reserved. Permission to use, copy, modify, and distribute this software and its documentation for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all copies and that both the copyright notice and this permission notice appear in supporting documentation. The laboratory of the Computer Science Department at Saint Louis University makes no representations about the suitability of this software for any purpose.


What is CATENA
================
CATENA is the system implementation of our asynchrnous implementation of the Chain Instantiation Protocol (CIP) for Consensus-based Distributed chain instantiation. 


DISTRIBUTION
================
The distribution tree contains: 

README.TXT
	--this file
build.xml
	--used by ant
Manifest
	--for generating jar file
log4j.properties
	--used for CATENA.log file options
src/ 
	--source files
config/	
	--configuration files (including events.txt)
javadoc/
	--java api reference
classes/
	--compiled code
jar/
	--jar files to run with >java -jar file.jar



COMPILATION AND RUN
============
Compiling this package requires Ant and Java 1.5. These can be downloaded respectively from:  
http://jakarta.apache.org/ant/index.html 
http://java.sun.com/j2se/

- compile
     ant 
- start dns
     ant dns 
- start isd
     ant isd 
- start service provider
     ant sp
- make jar file for command line running
     ant jarcmd
- make jar file for gui running
     ant jargui

     
CONFIGURATION
============
pnode.properties -- configuration for each physical node
events.txt -- events configuration

