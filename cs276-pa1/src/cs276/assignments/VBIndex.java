package cs276.assignments;
import java.awt.Button;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class VBIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) throws IOException{
		/*
		 * Your code here
		 */
		Byte b = readByte(fc);
 		if(b == null)		// else can't be null during this retrieval
			return null;
		
 		// len
 		int len = 0;
 		len |= b.byteValue() & 0x7f;
 		while( (b.byteValue() >> 7) == 0){
			len <<= 7;
 			b = readByte(fc);
 			len |= b.byteValue() & 0x7f;
		}
		
 		// termId
 		b = readByte(fc);
 		int tId = 0;
 		tId |= b.byteValue() & 0x7f;
 		while( (b.byteValue() >> 7) == 0){
			tId <<= 7;
 			b = readByte(fc);
 			tId |= b.byteValue() & 0x7f;
		}
 		
 		// gaps
 		List<Integer> list = new ArrayList<Integer>();
 		int count = len;
// 		System.out.println("len:"+len);
 		while(count > 0){
 			b = readByte(fc);
 			int gap = 0;
 	 		gap |= b.byteValue() & 0x7f;
 	 		while( (b.byteValue() >> 7) == 0){
 				gap <<= 7;
 	 			b = readByte(fc);
 	 			gap |= b.byteValue() & 0x7f;
 			}
 	 		list.add(gap);
 	 		gap = 0;
 			count--;
 		}
		assert(list.size() == len);
		for(int i=1; i<list.size(); i++){
			list.set(i, list.get(i-1)+list.get(i));
		}
//		cSystem.out.println("len: "+len+" list lenght: "+list.size() );
		PostingList pl = new PostingList(tId, list);
		return pl;
	}
	
	private Byte readByte(FileChannel fc) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.clear();
		int flag = fc.read(buf);
		if(flag < 1)
			return null;
		buf.rewind();
		Byte b = buf.get();
		return b;
	}
	
	@Override
	public void writePosting(FileChannel fc, PostingList p) throws IOException {
		/*
		 * Your code here
		 */
		List<Integer> l = p.getList();
		int gap;
		byte[] bt;
		ByteBuffer buf;
		
		int len = p.getList().size();
		bt = VBEncodeInteger(len);
		writeByte(fc, bt);
		
		int tId = p.getTermId();
		bt = VBEncodeInteger(tId);
		writeByte(fc, bt);
		
		for(int i=0; i<l.size(); i++){
			if(i == 0){
				gap = l.get(0);
			}else{
				gap = l.get(i) - l.get(i-1);
			}
			bt = VBEncodeInteger(gap);
			writeByte(fc, bt);
		}
	}	
	private void writeByte(FileChannel fc, byte[] bt) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(bt.length);
		buf.clear();
		buf.put(bt);
		buf.flip();
		while(buf.hasRemaining()){
			fc.write(buf);
		}
	}
	private byte[] VBEncodeInteger(int gap){
		byte[] result;
		if(gap == 0){
			result = new byte[1];
			result[0] = (byte) 0x80;
			return result;
		}
	    Stack<Byte> bs = new Stack<Byte>(); 
		int numBytes = 0;
	    while (gap > 0) {
            byte low_order = (byte) (gap & 0x7f);
            gap = gap >> 7;
            bs.push(low_order);
            numBytes ++; 
        }
        
        int i = 0;
        result = new byte[numBytes];
        while (!bs.empty()){
            result[i] = bs.pop(); 
            if (bs.empty()){
                result[i] |= (0x80); 
            } else {
                i ++;
            }
        }
	    return result;
	}

}
