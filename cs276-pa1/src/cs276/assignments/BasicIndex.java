package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class BasicIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) throws IOException{
		/*
		 * Your code here
		 */
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.clear();
		int flag = fc.read(buf);
		if(flag < 1)
			return null;

		buf.rewind();
		int len = buf.getInt();
		assert(len > 0);
		buf.clear();
		fc.read(buf);
		buf.rewind();
		int tId = buf.getInt();
		ArrayList<Integer> al = new ArrayList<Integer>();
		int dId;
		int count = len;
		while(count > 0){
			buf.clear();
			fc.read(buf);
			buf.rewind();
			dId = buf.getInt();
			al.add(dId);
			count --;
		}
		assert(al.size() == len);
		PostingList pl = new PostingList(tId, al);
		return pl;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) throws IOException {
		/*
		 * Your code here
		 */
		ByteBuffer buf = ByteBuffer.allocate(p.getList().size()*4+8);
		buf.clear();
		buf.putInt(p.getList().size());
		buf.putInt(p.getTermId());
		for(int i: p.getList()){
			buf.putInt(i);
		}
		buf.flip();

		while(buf.hasRemaining()) {
		    fc.write(buf);
		}
	}	
}
