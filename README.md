# Simple Scala bot #

This code extends the PircBot Java library, http://www.jibble.org/pircbot.php

By default, this project can be built with sbt 0.11 or later.
To get started with sbt: http://www.scala-sbt.org/

## Using sbt 0.7.7 ##

The directory 'project-0.7.7' provides support. In your working copy, rename this directory
to 'project'.

## A note on building ##

This project depends on my fork of Scala-OAuth: https://github.com/qu1j0t3/Scala-OAuth

This needs to be checked out somewhere on your system and a symbolic link created to its src/jm
directory. The symbolic link should be placed in src/main/scala. e.g., assuming the current directory
is the top level of your clone of this project, and you have checked out Scala-OAuth into
~/Documents/git/Scala-OAuth :

$ ln -s ~/Documents/git/Scala-OAuth/Scala\ OAuth/src/jm src/main/scala
