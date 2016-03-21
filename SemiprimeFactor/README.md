#### [Download the Semiprime Factorization Client (v0.4.4a)](https://github.com/entangledloops/heuristicSearch/blob/master/SemiprimeFactor/SemiprimeClient.jar?raw=true) ####

Build from source if you want the very latest. Use the stable build above if you experience any issues.

## What is this? ##

This software attempts to factor semiprimes into their prime factors via heuristic search. The client is for contributing to the current search being hosted on my personal server or running a local search on your own machine.

You can read about how this works on the [Semiprime Factorization Wiki](https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization).

You can also host your own server by running the latest [Semiprime Factorization Server](https://github.com/entangledloops/heuristicSearch/blob/master/SemiprimeFactor/SemiprimeServer.jar?raw=true) ####

## Screenshots ##

![Search Tab](http://www.entangledloops.com/img/semiprime/search-0.4.4a.png)

![Connect Tab](http://www.entangledloops.com/img/semiprime/connect-0.4.4a.png)

## Run Requirements ##

You need at least [Java 1.8](https://www.java.com/en/download/) installed.

## Build Requirements ##

If you want to build from source, you'll need at least [Java 1.8 JRE](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) to run the client directly, and/or the [Java 1.8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) if you want to build the redistributable jar.

Pull this git repo:

`git clone https://github.com/entangledloops/heuristicSearch/SemiprimeFactor.git`

Open a command prompt/terminal in the SemiprimeFactor subdirectory.
To run the client:

**Linux / OS X:**

`chmod +x gradlew`

`./gradlew run`

**Windows:**

`gradlew run`

**Other tasks available:**

`runServer` - run a local server

`dist` - build the client jar

`distServer` - build the server jar

`createWrapper` - updates gradle wrapper 

Be sure to run `createWrapper` from the parent directory so gradle files can be overwritten. A copy of gradle w/only this task is provided in the `heuristicSearch` dir to make this easier for you.
