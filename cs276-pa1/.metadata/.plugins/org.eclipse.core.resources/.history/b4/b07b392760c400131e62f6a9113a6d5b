package cs276.assignments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;
import java.lang.StringBuilder;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */

	private static List<Integer> intersectList(List<Integer> l1, List<Integer> l2){
		List<Integer> result = new ArrayList<Integer>();
		int p1 = 0, p2 = 0, dId1, dId2;
		while(p1 < l1.size() && p2 < l2.size()){
			dId1 = l1.get(p1);
			dId2 = l2.get(p2);
			if(dId1 == dId2){
				result.add(dId1);
				p1++;
				p2++;
			}else if(dId1 < dId2){
				p1++;
			}else{
				p2++;
			}
		}
		
		return result;

	}
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		FileChannel fc = indexFile.getChannel();
		while ((line = br.readLine()) != null) {
			/*
			 * Your code here
			 */
//			System.out.println("query: "+line);
			String[] query = line.split("\\s+");
			List<PostingList> list = new ArrayList<PostingList> ();
			for(String q: query){
				if(!termDict.containsKey(q)){
					break;
				}
				int tId = termDict.get(q);
				FileChannel newFc = fc.position(posDict.get(tId));
				PostingList pl = index.readPosting(newFc);
				list.add(pl);
			}
			Collections.sort(list, new postingListComparator());
			
			// debug:
//			for(PostingList pl: list){
//				Index.printPosting(pl);
//			}
			
			// intersect:
			if(list.size() == 0){
				System.out.println("no results found");
			}else{
				List<Integer> result = list.get(0).getList();
				PostingList pre = list.get(0), cur;
				
				for(int i=1; i<list.size(); i++){
					cur = list.get(i);
					if(cur.getTermId() == pre.getTermId()){ // ignore duplicate queries
						continue;
					}else{
						result = intersectList(result, cur.getList());
						if(result.size() == 0){
							break;
						}
						pre = cur;
					}
				}
				
				if(result.size() == 0){
					System.out.println("no results found");
				}else{
					List<String> sl = new ArrayList<String>();
					for(int dId: result){
						String s = docDict.get(dId);
						sl.add(s);
					}
					Collections.sort(sl);
					for(String s: sl){
						System.out.println(s);
					}
				}
			}	
		}
		br.close();
		indexFile.close();
		
	}
}
class postingListComparator implements Comparator<PostingList>{
	public int compare(PostingList pl1, PostingList pl2){
		Integer len1 = pl1.getList().size(), len2 = pl2.getList().size();
		return len1.compareTo(len2);
	}
}