**[Download the latest stable Semiprime Factorization Client](https://github.com/entangledloops/heuristicSearch/blob/master/SemiprimeFactor/SemiprimeClient.jar?raw=true).**

Build from source if you want the very latest. Use the stable build above if you experience any issues.

## Wiki ##

[Semiprime Factorization Wiki](https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization)

## Run Requirements ##

You need at least [Java 1.8](https://www.java.com/en/download/) installed.

## Gui Screenshots ##

![Search Tab](http://www.entangledloops.com/img/semiprime/search-0.4.4a.png)

![Connect Tab](http://www.entangledloops.com/img/semiprime/connect-0.4.4a.png)

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

Other gradle tasks available:

`runServer` - run a local server
`dist` - build the client jar
`distServer` - build the server jar
`createWrapper` - updates gradle wrapper 

Note: Be sure to run `createWrapper` from the parent directory so gradle files can be overwritten. A copy of gradle w/only this task is provided in the `heuristicSearch` dir to make this easier for you.
