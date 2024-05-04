# JComicUtils
Scripts intended for management of a comics collection

## Scripts

+ *repack.[bat|sh]* - will repack any .cbz and .cbr file in `cwd` into a .cbz comic packed with DEFLATE.  This is compatible with most of the older readers I use, like CDisplay.
+ *pdf2cbz.[bat|sh]* - will convert every PDF in `cwd` into a .cbz comic packed with DEFLATE.
+ *unpack.[bat|sh]* - will extract the content of all the comics in `cwd` into a directory with the same name.  Optional *-extension* parameter.
+ *pack.[bat|sh]* - will make a .cbz file out of any child folder under `cwd`.

Every script will remove brackets in the file names and, if present, text files inside the comics.

## Requirements

We need the following software (Linux instructions for Debian-like distros, please look for equivalents with your favorite package manager):

### Java 21

Get it for:
+ Windows: https://adoptopenjdk.net/
+ Linux: `sudo apt-get install openjdk-21-jre-headless`
+ MacOS: `brew install openjdk@21`

### Maven

Find the latest version for:
+ All platforms: https://maven.apache.org/download.cgi

### 7z 16.02

Tested with version:

```
7z -version

7-Zip [64] 16.02 : Copyright (c) 1999-2016 Igor Pavlov : 2016-05-21
```

Get it for:
+ Windows: https://www.7-zip.org/download.html
+ Linux: `sudo apt install p7zip-full`
  (maybe additional packages will be necessary, like `p7zip-rar` in certain distros)
+ MacOS: `brew install p7zip`

## How to install

### Download and compile required libraries

- Command line interaction utils: https://github.com/jorgeb1980/lib-shell-commands
  - `git clone git@github.com:jorgeb1980/lib-shell-commands`
  - `cd lib-shell-commands; mvn install; cd ..`
- Command line application framework: https://github.com/jorgeb1980/lib-cli-base
  - `git clone git@github.com:jorgeb1980/lib-cli-base`
  - `cd lib-cli-base; mvn install; cd ..`

### Download and compile this repository

- https://github.com/jorgeb1980/JComicUtils
  - `git clone git@github.com:jorgeb1980/JComicUtils`
  - `cd JComicUtils; mvn install; cd ..`

As explained in [lib-cli-base/README.md](https://github.com/jorgeb1980/lib-cli-base/blob/master/README.md), this
will create a structure inside `JComicUtils/target/redist` with the necessary scripts and libraries.

## Adittional information

For more information please see:

https://en.wikipedia.org/wiki/DEFLATE

https://www.7-zip.org/faq.html

https://pdfbox.apache.org/index.html