#!/bin/bash

Task="3"
C="1.0"
Gamma="0.2"
for numC in {0..6}
do
	if [ $numC == 0 ]; then
		C="0.002"
	elif [ $numC == 1 ]; then
		C="0.02"
	elif [ $numC == 2 ]; then
		C="0.2"
	elif [ $numC == 3 ]; then
		C="2.0"
	elif [ $numC == 4 ]; then
		C="20"
	elif [ $numC == 5 ]; then
		C="200"
	elif [ $numC == 6 ]; then
		C="2000"
	fi

	for numG in {6}
		do
			if [ $numG == 0 ]; then
				Gamma="0.0000002"
			elif [ $numG == 1 ]; then
				Gamma="0.000002"
			elif [ $numG == 2 ]; then
				Gamma="0.00002"
			elif [ $numG == 3 ]; then
				Gamma="0.0002"
			elif [ $numG == 4 ]; then
				Gamma="0.002"
			elif [ $numG == 5 ]; then
				Gamma="0.02"
			elif [ $numG == 6 ]; then
				Gamma="0.2"
			fi
			Cmd="./run.sh data/pa4.signal.train data/pa4.rel.train data/pa4.signal.dev data/pa4.rel.dev $Task $C $Gamma"
			eval $Cmd
			echo "GridSearch: $C $Gamma"
			
		done

done
# echo Done
