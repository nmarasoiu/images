# images
It works with local files but mounting a NFS e.g. a Ceph or Gluster distributed fs container nearby in the same pod,
or AWS EFS Elastic Filesystem, would replicate the files and make them accessible from all the nodes.
Should optimize for rapid access to many small files like typical photos are (for sure small compared to optimal
file sizes for GlusterFs or HDFS).

https://github.com/kofemann/simple-nfs is a Java/JVM based NFS server that can be started to expose
an embedded distributed filesystem and mount it as a simple folder. In case of Docker/K18s, stateful sets & volumes needed.

How resized images cache works. The cached files are not yet deleted, so not fully a cache.
When a resize is requested and a file like image_290x239.jpg is not available;
Then a resize is started, max concurrent resizes is the number of physical threads;
A temporary file like image_290x239.jpg.temporary is created, signaling other concurrent requests, like refreshes, to wait;
After resize, the temporary is renamed to final name.

Memory is a concern by the way of large files and limited memory.
To be studied if a constant memory is possible.
Alternatively, before starting a resize, a memory check can be done to estimate if given the file size on disk and what
 the resize would entail in memory, if we have enough memory to leave also free space, and if not, delay
 and eventually return an Http status explaining to the client that the server is overloaded.

Configs: as OS env vars:
BASE_PATH=the root folder with static images, e.g. /Users/nicu/Downloads
BUFFER_BYTE_SIZE=the amount of bytes in a batch of bytes from disk to the http client e.g. 1024
MAX_SIZE_FOR_RESIZE=the maximum length or height acceptable by the server to engage in a resize operation
MIN_FREE_BYTES_FOR_RESIZE=the minimum amount of free memory in bytes that the server requires in order to engage in resize

Done:
- free memory check before resize
- max w and h to do a resize

To Do:
- distributed/replicated NFS-mountable filesystem
- establish a JSON/XML schema e.g. GraphQL if this is machine to machine communication
- profile the server under load