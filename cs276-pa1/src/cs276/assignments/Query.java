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
	private static void printPosting(PostingList pl){
		assert(pl != null);
		System.out.println(pl.getTermId()
				+ "\t" + pl.getList() + "\n");	
	}
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
		//int tIdMin = Integer.MAX_VALUE;
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

//		System.out.println("term dict size: "+termDict.keySet().size() );
		
		/* Doc dictionary */
		int dIdMin = Integer.MAX_VALUE;
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
			dIdMin = Math.min(dIdMin, Integer.parseInt(tokens[1]) );
		}
		docReader.close();

//		System.out.println("doc dict size: "+docDict.keySet().size() );
//		System.out.println("dId min="+dIdMin);
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

//		System.out.println("posting dict size: "+posDict.keySet().size() );
		
		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		FileChannel fc = indexFile.getChannel();
		while ((line = br.readLine()) != null) {
			/*
			 * Your code here
			 */
			boolean flag = false;
			String[] query = line.split("\\s+");
			List<PostingList> list = new ArrayList<PostingList> ();
			for(String q: query){
//				System.out.println("word: "+q);
				if(!termDict.containsKey(q)){
//					System.out.println("term not found");
					flag = true;
					break;
				}
				int tId = termDict.get(q);
				FileChannel newFc = fc.position(posDict.get(tId));
				PostingList pl = index.readPosting(newFc);
//				printPosting(pl);
				list.add(pl);
			}
			
			if(flag){ // if any query word is not in dict
				System.out.println("no results found");
				continue;
			}
			
			// sort according to index list length
			Collections.sort(list, new postingListComparator());
			
			// intersect:
			
				List<Integer> result = list.get(0).getList();
				PostingList pre = list.get(0), cur;
				
				for(int i=1; i<list.size(); i++){
//					System.out.println("doc freq: "+list.get(i).getList().size() );
					cur = list.get(i);
					if(cur.getTermId() == pre.getTermId()){ // ignore duplicate queries
						continue;
					}else{
						
						result = intersectList(result, cur.getList());
//						System.out.println("after intersect: ");
//						System.out.println(result);
						
						if(result.size() == 0){
							break;
						}
						pre = cur;
					}
				}
				
				if(result.size() == 0){
					System.out.println("no results found");
				}else{
//					System.out.println("number of docs: "+result.size() );
					List<String> sl = new ArrayList<String>();
					for(int dId: result){
//						System.out.println("dId: "+dId);
						String s = docDict.get(dId);
						sl.add(s);
					}
					
					Collections.sort(sl);
//					System.out.println("number of docs after sort: "+result.size() );
					
					for(String s: sl){
						System.out.println(s);
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