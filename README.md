BIG is an archive format that was designed to store millions of files.

The format is quite simple. For each archive, two files are used. One file
is where we store all the binary files. The second file contains a list of
all the stored files and their respective location inside the big file.

This format comes out of the need for a simpler flat-file system where we
could read and write more than a million files. Conventional file systems
such as NTFS and EXT3 were not considered as efficient enough to handle 
this number of files, other solutions such as XFS and HDFS were not simple
enough for the triplecheck context.

Current edition focuses on supporting "write-once, read-many".

What is currently NOT supported:
- Editing files after being written
- Avoiding duplicates of the same file(s) to be written
- Sorting of files
- Archive splitting

What is supported:
- writing and reading files in sequential mode
- retrieving the files matching a SHA1 signature or folder location
- file compression using the ZIP algorithm 

What is it good for?
- Simplicity, any tool can read/extract data from a BIG archive
- Millions of files stored inside an archive
- SHA1 signatures for quick finding specific files


License
=======
Unless where specified otherwise, this software is licensed under the EUPL 
without the respective Appendix. If these license terms do not suit your context,
 please get in contact with TripleCheck.

