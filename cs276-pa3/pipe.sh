#!/usr/bin/env sh
# ./rank.sh <queryDocTrainData path> taskType
# ./Ndcg.sh <out_path> <relevance_file_path>
java -Xmx1024m -cp bin/ edu.stanford.cs276.Rank $1 $2 > temp.txt
java -Xmx1024m -cp bin/ edu.stanford.cs276.NdcgMain temp.txt $3
