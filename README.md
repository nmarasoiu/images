# images
It works with local files but mounting a NFS e.g. a Ceph or Gluster distributed fs container nearby in the same pod, or AWS EFS Elastic Filesystem, would replicate the files and make them accessible from all the nodes. Should optimize for rapid access to many small files like typical photos are (for sure small compared to optimal file sizes for GlusterFs or HDFS).

How resized images cache works. The cached files are not yet deleted, so not fully a cache.
When a resize is requested and a file like image_290x239.jpg is not available;
Then a resize is started, max concurrent resizes is the number of physical threads;
A temporary file like image_290x239.jpg.temporary is created, signaling other concurrent requests, like refreshes, to wait;
After resize, the temporary is renamed to final name.

Memory is a concern by the way of large files and limited memory.
To be studied if a constant memory is possible.
Alternatively, before starting a resize, a memory check can be done to estimate if given the file size on disk and what the resize would entail in memory, if we have enough memory to leave also free space, and if not, delay and eventually return an Http status explaining to the client that the server is overloaded.
