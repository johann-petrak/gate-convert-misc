# Tools to convert the sentence polarity dataset v1.0 to GATE

This is for converting the files from https://www.cs.cornell.edu/people/pabo/movie-review-data/rt-polaritydata.tar.gz to annotated GATE documents. 

See https://www.cs.cornell.edu/people/pabo/movie-review-data/

Corpus introduced in Pang/Lee ACL 2005. Released July 2005.

To convert the original files, make sure the rt-polarity.pos and rt-polarity.neg are the 
only files in some directory `origdir`, and the destination directory `destdir` is empty, 
then run

   `./convert.sh origdir destdir`
