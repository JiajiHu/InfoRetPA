#!/bin/bash

if [[ ! $# -eq 6 && ! $# -eq 7 ]]; then
  echo "Usage: l2r.sh <train_signal_file> <train_rel_file> <test_signal_file> <task> <C> <gamma>[out_file]"
  echo "\tout_file (optional): specify where to write output results to. If not specified, write to stdout"
  exit
fi

train_signal_file=$1
train_rel_file=$2
test_signal_file=$3
task=$4
c=$5
gamma=$6
out_file=""
if [ $# -ge 7 ]; then
  out_file=$7
fi

#ant

#echo ""
#echo "# Executing: java -cp bin:lib/weka.jar cs276.pa4.Rank $train_signal_file $train_rel_file $test_signal_file $task $out_file"
java -cp bin:lib/weka.jar cs276.pa4.Learning2Rank $train_signal_file $train_rel_file $test_signal_file $task $c $gamma $out_file

