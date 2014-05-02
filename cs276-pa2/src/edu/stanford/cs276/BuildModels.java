package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.List;


public class BuildModels {

	public static LanguageModel languageModel;
	public static NoisyChannelModel noisyChannelModel;

	public static void main(String[] args) throws Exception {

		String trainingCorpus = null;
		String editsFile = null;
		String extra = null;
		if (args.length == 2 || args.length == 3) {
			trainingCorpus = args[0];
			editsFile = args[1];
			if (args.length == 3) extra = args[2];
		} else {
			System.err.println(
					"Invalid arguments.  Argument count must 2 or 3" + 
							"./buildmodels <training corpus dir> <training edit1s file> \n" + 
							"./buildmodels <training corpus dir> <training edit1s file> <extra> \n" + 
							"SAMPLE: ./buildmodels data/corpus data/edit1s.txt \n" +
							"SAMPLE: ./buildmodels data/corpus data/edit1s.txt extra \n"
					);
			return;
		}
		System.out.println("training corpus: " + args[0]);

		languageModel =  LanguageModel.create(trainingCorpus);
		noisyChannelModel = NoisyChannelModel.create(editsFile);
		if ("extra".equals(extra)) {
		  // add a blacklist of popular misspellings -> should include more, but this will do for now
		  List<String> blacklist = new ArrayList<String>();;
      blacklist.add("standford");
      blacklist.add("tressider");
      blacklist.add("accomodate");
      languageModel.unaryVals.purgeDict(blacklist);
    }
		// Save the models to disk
		noisyChannelModel.save();
		languageModel.save();
		
		
	}
}
