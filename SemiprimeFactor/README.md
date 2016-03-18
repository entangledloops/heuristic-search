**[Download](https://github.com/entangledloops/heuristicSearch/blob/master/SemiprimeFactor/SemiprimeFactor.jar?raw=true) the latest [Semiprime Factorization](https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization) Client (currently v0.3a).**

## Run Requirements ##

You need at least [Java 1.8](https://www.java.com/en/download/) installed.

## Build Requirements ##

If you want to build from source, you'll need at least the [Java 1.8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Pull this git repo:

`git clone https://github.com/entangledloops/heuristicSearch/SemiprimeFactor.git`

Open a command prompt/terminal in the SemiprimeFactor subdirectory.
To run the client:

Linux / OS X:

`chmod +x gradlew`
`./gradlew run`

Windows:

`gradlew run`

Other gradle tasks available:

`runServer` - run a local server
`dist` - build the client jar
`distServer` - build the server jar
`createWrapper` - updates gradle wrapper 

Note: Be sure to run `createWrapper` from the parent directory so gradle files can be overwritten. A copy of gradle w/only this task is provided in the `heuristicSearch` dir to make this easier for you.

## Wiki ##

[Semiprime Factorization Wiki](https://github.com/entangledloops/heuristicSearch/wiki/Semiprime-Factorization)
