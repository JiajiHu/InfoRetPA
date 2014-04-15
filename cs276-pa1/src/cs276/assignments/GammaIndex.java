package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class GammaIndex implements BaseIndex {

	@Override
	public PostingList readPosting(FileChannel fc) throws IOException {
		/*
		 * Your code here
		 */
	  Byte newin[] = {readByte(fc),null};
	  byte bytes[] = {(byte)0,(byte)0};
	  int bitPosition[] = {0,0};
	  if (newin[0]==null) {
	      return null;
	    }
	  else {
	      bytes[0] = newin[0].byteValue();
	  }
//    System.out.println("read byte: "+Byte.toString(bytes[0]));
	  // len
	  int len = getNextNumber(fc, bytes, bitPosition);
//    System.out.println("LEN: "+len);
	  
	  //termID
	  int tId = getNextNumber(fc, bytes, bitPosition);
//	  System.out.println("TID: "+tId);
    
	  //gaps
	  List<Integer> list = new ArrayList<Integer>();
    int count = len;
    int gap;
    while(count > 0){
      gap = getNextNumber(fc,bytes,bitPosition);
//      System.out.println("GAP: "+gap);
      list.add(gap);
      count--;
    }
	  
	  //put it all together
    assert(list.size() == len);
    for(int i=1; i<list.size(); i++){
      list.set(i, list.get(i-1)+list.get(i));
    }
    PostingList pl = new PostingList(tId, list);
    return pl;
  }

	@Override
	public void writePosting(FileChannel fc, PostingList p) throws IOException {
		/*
		 * Your code here
		 */
    int len = p.getList().size();
	  int tId = p.getTermId();
    List<Integer> l = p.getList();
    int gap;
    
    byte bytes[] = {(byte)0,(byte)0};
    int bitPosition[] = {0,0};

//    System.out.println("NEW POSTING: ");
//    System.out.println("LEN: "+len);
    writeNextNumber(fc, bytes, bitPosition, len);
//    System.out.println("byte: " + Byte.toString(bytes[0]));
//    System.out.println("TID: "+tId);
    writeNextNumber(fc, bytes, bitPosition, tId);
//    System.out.println("byte: " + Byte.toString(bytes[0]));
    
    for(int i=0; i<l.size(); i++){
      if(i == 0){
        gap = l.get(0);
      }else{
        gap = l.get(i) - l.get(i-1);
      }
//      System.out.println("GAP: "+gap);
      
      writeNextNumber(fc, bytes, bitPosition, gap);
    }
    
    finishWrite(fc, bytes[0]);
	}
	
	private int getNextNumber(FileChannel fc, byte[] bytes, int[] bitPosition) throws IOException {
	  int ret = 0;
	  int count=0;
	  while ( true )
	  {  
	      if ((bytes[0] & (1 << (7 - bitPosition[0]))) > 0)  
	      {
	        count++;	        
	        bitPosition[0]++;
	        if (bitPosition[0] > 7) {
	          bytes[0] = readByte(fc).byteValue();
//	          System.out.println("read byte: "+Byte.toString(bytes[0]));
	          bitPosition[0] = 0;
	        }
	      }
	      else {
	        bitPosition[0]++;
	        if (bitPosition[0] > 7){
	          bytes[0] = readByte(fc).byteValue();
//	          System.out.println("read byte: "+Byte.toString(bytes[0]));
            bitPosition[0] = 0;  
	        }
	        break;
	      }
	  }
	  ret = (1 << count);
	  while(count > 0){
	    count--;
	    if ((bytes[0] & (1 << (7 - bitPosition[0]))) > 0)  
      {
	      ret = ret + (1 << count);
      }
	    bitPosition[0]++;
      if (bitPosition[0] > 7) {
        bytes[0] = readByte(fc).byteValue();
//        System.out.println("read byte: "+Byte.toString(bytes[0]));
        bitPosition[0] = 0;
      }
	  }
	  
	  return ret;
	}
	
	private void writeNextNumber(FileChannel fc, byte[] bytes, int[] bitPosition, int num) throws IOException {
	  String numString = Integer.toBinaryString(num);
	  
	  int count = numString.length()-1;
	  //System.out.println("count: " + count);
	  
	  while(count > 0){
      count--;
      bytes[0] = (byte) ((int) bytes[0] + (int) (1 << (7 - bitPosition[0])));
      bitPosition[0]++;
      if (bitPosition[0] > 7) {
        writeByte(fc,bytes[0]);
//        System.out.println("byte: " + Byte.toString(bytes[0]));
        bitPosition[0]=0;
        bytes[0]=(byte)0;
      }
    }
	  
	  bitPosition[0]++;
	  if (bitPosition[0] > 7) {
	    writeByte(fc,bytes[0]);
//	    System.out.println("byte: " + Byte.toString(bytes[0]));
      bitPosition[0]=0;
      bytes[0]=(byte)0;
    }
    count = 1;
	  String[] nums = numString.split("");
	  while(count < numString.length()){
	    if (nums[count+1].equals("1")){
	      bytes[0] = (byte) ((int) bytes[0] + (int) (1 << (7 - bitPosition[0])));
	    }
	    bitPosition[0]++;
      if (bitPosition[0] > 7) {
//        System.out.println("byte: " + Byte.toString(bytes[0]));
        writeByte(fc,bytes[0]);
        bitPosition[0]=0;
        bytes[0]=(byte)0;
      }
      count++;
	  }	  
	}
	
	private void finishWrite(FileChannel fc, byte b) throws IOException {
//	  System.out.println("byte: " + Byte.toString(b));
    writeByte(fc, b);
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
	
	private void writeByte(FileChannel fc, byte bt) throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(1);
    buf.clear();
    buf.put(bt);
    buf.flip();
    while(buf.hasRemaining()){
      fc.write(buf);
    }
  }
	
}
