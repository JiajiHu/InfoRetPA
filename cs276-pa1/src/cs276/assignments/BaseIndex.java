package cs276.assignments;

import java.nio.channels.FileChannel;
import java.io.IOException;

public interface BaseIndex {
	
	public PostingList readPosting (FileChannel fc) throws IOException ;
	
	public void writePosting (FileChannel fc, PostingList p) throws IOException ;
}
