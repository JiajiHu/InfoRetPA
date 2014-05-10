#!/usr/bin/env sh
# ./Ndcg.sh <rank_output_path> <relevence_doc_path>
java -Xmx1024m -cp bin/ edu.stanford.cs276.NdcgMain $1 $2
