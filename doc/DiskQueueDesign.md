##Message Queue Design
zbus file system targets to support Unicast, Multicast and Broadcast messaging model based on the following 5 components.

* Index -- manages the block files
* Block -- stores the real data
* DiskMessage -- message format in disk, the read and write unit for message queue
* QueueWriter -- writes message to indexed blocks, a thin layer on Index
* QueueReader -- reads message from indexed blocks



						   Index MappedFile
			   +------------------------------------------------+
			   | Version                                       4|
			   +------------------------------------------------+
			   | BlockCount(n)                                 4|
			   +------------------------------------------------+
			   | BlockStart                                    8| -----+
			   +------------------------------------------------+      |
			   | MessageCount                                  8|      |
			   +------------------------------------------------+      |
			   |               Extension                 to 1024|      |
			   +------------------------------------------------+      |
		       |0_createdTs | baseOffset | endOffset | updatedTs|<-----+
		       +------------------------------------------------+
		  +--->|1_createdTs | baseOffset | endOffset | updatedTs|        BlockFile 00000000000000000000.zbus
		  |    +------------------------------------------------+      +------->+---------------+                   DiskMessage Format  
		  |    |                    ...                         |      |        | DiskMessage_0 |                   +----------------+ 
		  |    +------------------------------------------------+      |        | DiskMessage_1 |                   | Offset        8| 
		  |    |n_createdTs | baseOffset | endOffset | updatedTs|      |        | DiskMessage_2 |<---+              +----------------+ 
		  |    +------------------------------------------------+      |        |       .       |    |              | Timestamp     8| 
		  |           |            | [QueueWriter]                     |        |       .       |    |              +----------------+ 
		  |           +----------- | ----------------------------------+        |       .       |    |              | Id           40| 
		  |                        |                                            | DiskMessage_n |    |              +----------------+ 
		  |                        +------------------------------------------->+---------------+    |              | CorrOffset    8| 
		  |                                                                                          |              +----------------+ 
		  |                                                                                          |              | MessageNumber 8| 
		  | [QueueReader1] MappedFile                                                                |              +----------------+ 
		  |     +---------------+                                                                    |              | Tag         128| 
		  +-----| BlockNumber  8|                                                                    |              +----------------+ 
		        +---------------+                                                                    |              | Length        4| 
		        | Offset       4|--------------------------------------------------------------------+              +----------------+ 
		        +---------------+                                                                                   | Body          ?| 
		        | Tag        128|                                                                                   +----------------+ 
		        +---------------+ 
		  ^ 
	      | [QueueReader2] MappedFile                                                                ^ 
	      |     +---------------+                                                                    | 
	      +-----| BlockNumber  8|                                                                    | 
	            +---------------+                                                                    | 
	            | Offset       4|--------------------------------------------------------------------+ 
	            +---------------+ 
	            | Filter     128| 
	            +---------------+  
            
**Index**

Index stores an infinite(long sized) array of block metadata, each block metadata item identifies 
* **base offset**, the block file name, which is also the first message'offset in current block file, e.g. 00000000000000000000.zbus is the first block name. 
* **end offset**, the offset of the block to write if not full, the end of block if full.
* **created time stamp**, initialized when block created
* **updated time stamp**, updated when any writes occurs.

BlockCount is the number of block available to read in the index.

BlockStart is the start block number which is valid to read/write, initialized as 0, incremented if any history blocks deleted.   The real slot in the index file is mapped by **blockNumber%MaxBlockCount**

MessageCount is total message counter since Index created.

Flag is used by the application to set special meaning

Extension is managed as key-value pairs, such as storing index's creator information.

**Block**

Block reads and writes DiskMessage.

DiskMessage's tag is employed to filter on reading message, which is useful for subscriber to filter out uninterested messages.

**QueueReader**

* **BlockNumber**, the slot number in Index
* **Offset**, the next read offset in the block
* **Filter**, filter on message's tag to read, default to null

**QueueWriter**

A very thin layer on Index, only the last block **blockStart+blockCount-1** is writable.

**Performance Boost**

Index and QueueReader are loaded via MappedByteBuffer(MappedFile), the operations on Index itself can be assumed as the speed of memory operation.