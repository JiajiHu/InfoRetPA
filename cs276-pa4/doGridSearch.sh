#!/bin/bash
Cmd="./gridsearch.sh &>GSresult.txt"
eval $Cmd
Cmd2="cat GSresult.txt | grep GridSearch > results.txt"
eval $Cmd2
