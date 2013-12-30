# Simple Scala bot #

This code extends the PircBot Java library, http://www.jibble.org/pircbot.php

By default, this project can be built with sbt 0.11 or later.
To get started with sbt: http://www.scala-sbt.org/


## A note on building ##

This project depends on my fork of Scala-OAuth: https://github.com/qu1j0t3/Scala-OAuth

This needs to be checked out somewhere on your system and a symbolic link created to its src/jm
directory. The symbolic link should be placed in src/main/scala. e.g., assuming the current directory
is the top level of your clone of this project, and you have checked out Scala-OAuth into
~/Documents/git/Scala-OAuth :

    $ ln -s ~/Documents/git/Scala-OAuth/Scala\ OAuth/src/jm src/main/scala


## Using sbt 0.7.7 ##

The directory 'project-0.7.7' provides support. In your working copy,
create a symlink to this directory named 'project', e.g.:

    $ ln -s project-0.7.7/ project
    $ sbt
    Getting net.java.dev.jna jna 3.2.3 ...
    :: retrieving :: org.scala-tools.sbt#boot-app
        confs: [default]
        1 artifacts copied, 0 already retrieved (838kB/111ms)
    Getting Scala 2.7.7 ...
    :: retrieving :: org.scala-tools.sbt#boot-scala
        confs: [default]
        2 artifacts copied, 0 already retrieved (9911kB/801ms)
    Getting org.scala-tools.sbt sbt_2.7.7 0.7.7 ...
    :: retrieving :: org.scala-tools.sbt#boot-app
        confs: [default]
        17 artifacts copied, 0 already retrieved (4379kB/523ms)
    [info] Recompiling project definition...
    [info] 	  Source analysis: 1 new/modified, 0 indirectly invalidated, 0 removed.
    Getting Scala 2.9.2 ...
    :: retrieving :: org.scala-tools.sbt#boot-scala
        confs: [default]
        4 artifacts copied, 0 already retrieved (20090kB/975ms)
    [info] Building project SquirtBot 1.0 against Scala 2.9.2
    [info]    using Project with sbt 0.7.7 and Scala 2.9.2
    > run
    [info]
    [info] == copy-resources ==
    [info] == copy-resources ==
    [info]
    [info] == compile ==
    [info]   Source analysis: 2 new/modified, 3 indirectly invalidated, 0 removed.
    [info] Compiling main sources...
    [info] Compilation successful.
    [info]   Post-analysis: 51 classes.
    [info] == compile ==
    [info]
    [info] == run ==
    [info] Running main.scala.squirt.Main
    ...

## Installing an upstart service in Ubuntu ##

    # chown root:root upstart/squirtbot.conf
    # mv upstart/squirtbot.conf /etc/init

Start the bot using
    $ initctl start squirtbot
