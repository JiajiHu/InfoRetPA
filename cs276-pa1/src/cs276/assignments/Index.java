package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.nio.*;

public class Index {

	// Term id -> (position in index file, doc frequency) dictionary
//	private static Map<Integer, Pair<Long, Integer>> postingDict 
//		= new TreeMap<Integer, Pair<Long, Integer>>();
  private static Map<Integer, Long> postingDict 
  = new TreeMap<Integer, Long>();

	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new TreeMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 1;
	// Term counter
	private static int wordIdCounter = 1;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	
	private static PostingList mergePosting(PostingList pl1, PostingList pl2){
		assert(pl1 != null && pl2 != null && pl1.getTermId() == pl2.getTermId());
		int tId = pl1.getTermId();
		List<Integer> al1 = pl1.getList();
		List<Integer> al2 = pl2.getList();
		List<Integer> al = new ArrayList<Integer>();
		int ind1 = 0, ind2 = 0, dId1, dId2;
		while(ind1 < al1.size() && ind2 < al2.size()){
			dId1 = al1.get(ind1);
			dId2 = al2.get(ind2);
			if(dId1 <= dId2){
				al.add(dId1);
				ind1 ++;
			}else{
				al.add(dId2);
				ind2 ++;
			}
		}
		while(ind1 < al1.size()){
			dId1 = al1.get(ind1);
			al.add(dId1);
			ind1 ++;
		}
		while(ind2 < al2.size()){
			dId2 = al2.get(ind2);
			al.add(dId2);
			ind2 ++;
		}
		return new PostingList(tId, al);
	}
	private static void printPosting(PostingList pl){
		assert(pl != null);
		System.out.println(pl.getTermId()
				+ "\t" + pl.getList() + "\n");	
	}
	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
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

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();  // block list
		// TODO: one block code test. if only one block -- need to parse, no need to merge -- process in merge !
		
		/* For each block */
		for (File block : dirlist) {
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);

			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();  // file list in each block			
			SortedMap<Integer, ArrayList<Integer> > termIdDocId = new TreeMap<Integer, ArrayList<Integer>> ();
			
			/* For each file */
			for (File file : filelist) {
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				int docId = docIdCounter;
				docDict.put(fileName, docIdCounter++);
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens) {
						/*
						 * Your code here
						 */
						int termId;
						if(termDict.containsKey(token)){
							termId = termDict.get(token);
						}else{
							termId = wordIdCounter;
							termDict.put(token, wordIdCounter++);
						}
						ArrayList<Integer> al;
						if(termIdDocId.containsKey(termId)){
							al = termIdDocId.get(termId); 
							if(al.get(al.size()-1) != docId){
								al.add(docId);
							}
						}else{
							al = new ArrayList<Integer>();
							al.add(docId);
							termIdDocId.put(termId, al);
						}
					}
				}
				reader.close();
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			FileChannel ch = bfc.getChannel();
			/*
			 * Your code here
			 */
			ArrayList<Integer> dIdList;
			PostingList pl;
			for(int tId: termIdDocId.keySet()){
				dIdList = termIdDocId.get(tId);
				pl = new PostingList(tId, dIdList);
				index.writePosting(ch, pl);
			}
			ch.close();
			bfc.close();
			
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		if(dirlist.length == 1){	// only one block in total, no need to merge, but need to populate 'postingDict'
			File b = blockQueue.getFirst();		// shouldn't remove, this file will be renamed to index file
			RandomAccessFile bf = new RandomAccessFile(b, "r");
			FileChannel fc = bf.getChannel();
			long prePos = fc.position();
			PostingList pl = index.readPosting(fc);
			int tId;
			while(pl != null){
				tId = pl.getTermId();
				postingDict.put(tId, prePos);
				prePos = fc.position();
				pl = index.readPosting(fc);
			}
			
		}else{
			while (true) {
				int remain = blockQueue.size();
				if (remain <= 1)
					break;
				
				File b1 = blockQueue.removeFirst();
				File b2 = blockQueue.removeFirst();
				File combfile = new File(output, b1.getName() + "+" + b2.getName());
				if (!combfile.createNewFile()) {
					System.err.println("Create new block failure.");
					return;
				}
				
				RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
				RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
				RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
				 
				/*
				 * Your code here
				 */
				FileChannel fc1 = bf1.getChannel();
				FileChannel fc2 = bf2.getChannel();
				FileChannel mfc = mf.getChannel();
				PostingList pl1 = index.readPosting(fc1);
				PostingList pl2 = index.readPosting(fc2);
				int tId1, tId2;
	      
				while(pl1 != null && pl2 != null){
					tId1 = pl1.getTermId();
					tId2 = pl2.getTermId();
					
			      	if(tId1 < tId2){
			        	if(remain == 2){
			        		postingDict.put(tId1, mfc.position());
			        	}
						index.writePosting(mfc, pl1);
						pl1 = index.readPosting(fc1);
					}else if(tId1 > tId2){
		          		if(remain == 2){
							postingDict.put(tId2, mfc.position() );
						}
						index.writePosting(mfc, pl2);
						pl2 = index.readPosting(fc2);
					}else{
						PostingList mpl = mergePosting(pl1, pl2); // merged posing list
						if(remain == 2){
							postingDict.put(mpl.getTermId(), mfc.position());
						}
						index.writePosting(mfc, mpl);
					    pl1 = index.readPosting(fc1);
					    pl2 = index.readPosting(fc2);
					}
				}
				while(pl1 != null){
					tId1 = pl1.getTermId();
					if(remain == 2){
						postingDict.put(tId1, mfc.position());
					}
					index.writePosting(mfc, pl1);
					pl1 = index.readPosting(fc1);
				}
				while(pl2 != null){
					tId2 = pl2.getTermId();
				  if(remain == 2){
						postingDict.put(tId2, mfc.position());
					}
					index.writePosting(mfc, pl2);
					pl2 = index.readPosting(fc2);
				}
				
				bf1.close();
				bf2.close();
				mf.close();
				mfc.close();
				fc1.close();
				fc2.close();
				b1.delete();
				b2.delete();
				blockQueue.add(combfile);
			}
		}
		
		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId) + "\n");
		}
		postWriter.close();
	}
	

}
class pairComparator implements Comparator<Pair<Integer, Integer>>{
	public int compare(Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2){
		if(pair1.getFirst() == pair2.getFirst()){
			return pair1.getSecond().compareTo(pair2.getSecond());
		}else{
			return pair1.getFirst().compareTo(pair2.getFirst());
		}
	}
} 
