# P2P-File-Sharer
A file sharer using peer-to-peer architecture and a ring topology of uploading and downloading peers, over TCP.

The software consists of two scripts-- tracker and peer. The tracker breaks a given file into 10KB chunks and stores them in individual binary files. Five peers can then be spun up and connected to the tracker simultaneously on different threads in order to be connectd into the network. The tracker randomly distributes the chunks evenly among the peers. Each peer then creates a summary file detailing which chunks they have received.

Once all the peers have downloaded their allotted chunks, they each create an upload and a download thread and connect to each other in ring topology to simultaneously upload requested chunks to their upload neighbor and download chunks they still need from their download neighbor. After each download, a peer updates its summary file. Once a peer has received all chunks, it reassembles them into a single file-- a replica of the original file stored by the tracker.

This file sharer can be simulated on a single device using the local host 127.0.0.1 or can be run across multiple devices, with each peer entering the IP address of the peer they will download from in the command line.
