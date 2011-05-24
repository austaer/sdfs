package org.opendedup.sdfs.servers;

import java.io.IOException;


import org.opendedup.util.SDFSLogger;
import org.opendedup.util.StringUtils;


import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.AbstractChunkStore;
import org.opendedup.sdfs.filestore.HashChunk;
import org.opendedup.sdfs.filestore.HashStore;
import org.opendedup.sdfs.filestore.gc.ChunkStoreGCScheduler;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

public class HashChunkService {

	private static double kBytesRead;
	private static double kBytesWrite;
	private static final long KBYTE = 1024L;
	private static long chunksRead;
	private static long chunksWritten;
	private static long chunksFetched;
	private static double kBytesFetched;
	private static int unComittedChunks;
	private static int MAX_UNCOMITTEDCHUNKS = 100;
	private static HashStore hs = null;
	private static AbstractChunkStore fileStore = null;
	private static ChunkStoreGCScheduler csGC = null;
	private static int cacheLenth = 10485760 / Main.CHUNK_LENGTH;
	private static ConcurrentLinkedHashMap<String, HashChunk> readBuffers = new Builder<String, HashChunk>().concurrencyLevel(Main.writeThreads).initialCapacity(cacheLenth)
	.maximumWeightedCapacity(cacheLenth).listener(
			new EvictionListener<String, HashChunk>() {
				// This method is called just after a new entry has been
				// added
				public void onEviction(String key, HashChunk writeBuffer) {
				}
			}
	
	).build();

	/**
	 * @return the chunksFetched
	 */
	public static long getChunksFetched() {
		return chunksFetched;
	}

	static {
			try {
				fileStore =(AbstractChunkStore)Class.forName(Main.chunkStoreClass).newInstance();
				fileStore.init(Main.chunkStoreConfig);
			} catch (InstantiationException e) {
				SDFSLogger.getLog().fatal("Unable to initiate ChunkStore. Exiting...",e);
				System.exit(-1);
			} catch (IllegalAccessException e) {
				SDFSLogger.getLog().fatal("Unable to initiate ChunkStore. Exiting...",e);
				System.exit(-1);
			} catch (ClassNotFoundException e) {
				SDFSLogger.getLog().fatal("Unable to initiate ChunkStore. Exiting...",e);
				System.exit(-1);
			} catch (IOException e) {
				SDFSLogger.getLog().fatal("Unable to initiate ChunkStore. Exiting...",e);
				System.exit(-1);
			}
		try {
			hs = new HashStore();
			if(!Main.chunkStoreLocal || Main.enableNetworkChunkStore) {
			csGC = new ChunkStoreGCScheduler();
			}
		} catch (Exception e) {
			SDFSLogger.getLog().fatal( "unable to start hashstore", e);
			System.exit(-1);
		}
	}

	private static long dupsFound;
	
	public static AbstractChunkStore getChuckStore() {
		return fileStore;
	}

	public static boolean writeChunk(byte[] hash, byte[] aContents,
			int position, int len, boolean compressed) throws IOException, HashtableFullException {
		if(aContents.length > Main.chunkStorePageSize)
			throw new IOException("content size out of bounds [" +aContents.length + "] > [" + Main.chunkStorePageSize + "]");
		chunksRead++;
		kBytesRead = kBytesRead + (position / KBYTE);
		boolean written = hs.addHashChunk(new HashChunk(hash, 0, len,
				aContents, compressed));
		if (written) {
			unComittedChunks++;
			chunksWritten++;
			kBytesWrite = kBytesWrite + (position / KBYTE);
			if (unComittedChunks > MAX_UNCOMITTEDCHUNKS) {
				commitChunks();
			}
			return false;
		} else {
			dupsFound++;
			return true;
		}
	}

	public static boolean hashExists(byte[] hash) throws IOException {
		return hs.hashExists(hash);
	}

	public static HashChunk fetchChunk(byte[] hash) throws IOException {
		String hashStr = StringUtils.getHexString(hash);
		HashChunk hashChunk = readBuffers.get(hashStr);
		if(hashChunk == null) {
			hashChunk = hs.getHashChunk(hash);
			readBuffers.put(hashStr, hashChunk);
		}
		byte[] data = hashChunk.getData();
		kBytesFetched = kBytesFetched + (data.length / KBYTE);
		chunksFetched++;
		return hashChunk;
	}

	public static byte getHashRoute(byte[] hash) {
		byte hashRoute = (byte) (hash[1] / (byte) 16);
		if (hashRoute < 0) {
			hashRoute += 1;
			hashRoute *= -1;
		}
		return hashRoute;
	}

	public static void processHashClaims() throws IOException {
		hs.processHashClaims();
	}

	public static void removeStailHashes() throws IOException {
		hs.evictChunks(Main.evictionAge*60*60*1000,false);
	}
	public static long removeStailHashes(int minutes,boolean forceRun) throws IOException {
		return hs.evictChunks(minutes*60*1000,forceRun);
	}

	public static void commitChunks() {
		// H2HashStore.commitTransactions();
		unComittedChunks = 0;
	}
	
	public static long getSize() {
		return hs.getEntries();
	}
	
	public static long getFreeBlocks() {
		return hs.getFreeBlocks();
	}
	
	public static long getMaxSize() {
		return hs.getMaxEntries();
	}
	
	public static int getPageSize() {
		return Main.chunkStorePageSize;
	}

	public static long getChunksRead() {
		return chunksRead;
	}

	public static long getChunksWritten() {
		return chunksWritten;
	}

	public static double getKBytesRead() {
		return kBytesRead;
	}

	public static double getKBytesWrite() {
		return kBytesWrite;
	}

	public static long getDupsFound() {
		return dupsFound;
	}

	public static void close() {
		fileStore.close();
		if(csGC != null)
			csGC.stopSchedules();
		hs.close();

	}

	public static void init() {

	}

}
