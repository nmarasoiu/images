# images
It works with local files but mounting a NFS like AWS Elastic Filesystem would replicate the files and make them accessible from all the nodes.
When a resize is requested and a file like image_290x239.jpg is not available,
Then a resize is started, max concurrent resizes is the number of physical threads,
A temporary file is created signaling other concurrent requests like refreshes to wait,
After resize, the temporary is renamed to final name.
Memory is a concern by the way of large files and limited memory.
To be studied if a constant memory is possible.
