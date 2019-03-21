# zip4j_mini4stream
A stripped version of zip4j (zips only, single file, ZipOutputStream only).

This fork is in no way better than zip4j, just smaller and simpler if all you care about is zipping a single file in a password protected zip.

My goal was to streamline the use I make of zip4j (i.e. single import, default parameters, less calls). At the same time, I wanted to strip away anything I don't use (i.e. unzip, standard encryption, multiple files). While I was hacking away, I've merged some classes together (i.e. the output stream hierarchy) to reduce the final binary even more. I also resolved warnings from -Xlint and Eclipse.
