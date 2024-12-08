# JComicUtils
Scripts intended for management of a comics collection

## Scripts

+ *repack.[bat|sh]* - will repack any .cbz and .cbr file in `cwd` into a .cbz comic.  The chosen format is compatible with most of the older readers I use, like CDisplay.
+ *pdf2cbz.[bat|sh]* - will convert every PDF in `cwd` into a .cbz comic.
+ *unpack.[bat|sh]* - will extract the content of all the comics in `cwd` into a directory with the same name.  Optional *-extension* parameter.
+ *pack.[bat|sh]* - will make a .cbz file out of any child folder under `cwd`.

Every script will remove brackets in the file names and, if present, text files inside the comics.

## Requirements

We need the following software (Linux instructions for Debian-like distros, please look for equivalents with your favorite package manager):

### Java 21

Get it for:
+ Windows: https://adoptopenjdk.net/
+ Linux: `sudo apt-get install openjdk-21-jdk`
+ MacOS: `brew install openjdk@21`

### Maven

Find the latest version for:
+ All platforms: https://maven.apache.org/download.cgi

## How to install

### Download and compile required libraries


- Test extensions and utils: https://github.com/jorgeb1980/lib-test-utils
  - `git clone git@github.com:jorgeb1980/lib-test-utils`
  - `cd lib-test-utils; mvn install; cd ..`
- Command line application framework: https://github.com/jorgeb1980/lib-cli-base
  - `git clone git@github.com:jorgeb1980/lib-cli-base`
  - `cd lib-cli-base; mvn install; cd ..`

### Download and compile this repository

- https://github.com/jorgeb1980/JComicUtils
  - `git clone git@github.com:jorgeb1980/JComicUtils`
  - `cd JComicUtils; mvn install; cd ..`

As explained in [lib-cli-base/README.md](https://github.com/jorgeb1980/lib-cli-base/blob/master/README.md), this
will create a structure inside `JComicUtils/target/redist` with the necessary scripts and libraries.
