#!/bin/bash --posix

# Case Studies from the paper
# declare -a files=("testcases/probabilistic/Bspline2.scala")

declare -a files=("testcases/probabilistic/Bsplines.scala"
		  "testcases/probabilistic/ClassIDs.scala"
  		  "testcases/probabilistic/Doppler.scala"
  		  "testcases/probabilistic/Example.scala"
		  "testcases/probabilistic/Filters.scala"
		  "testcases/probabilistic/Mul8.scala"
  		  "testcases/probabilistic/RigidBody.scala"
		  "testcases/probabilistic/Sine.scala"
  		  "testcases/probabilistic/SolveCubic.scala"
  		  "testcases/probabilistic/Sqrt.scala"
  		  "testcases/probabilistic/Sum8.scala"
		  "testcases/probabilistic/Traincars.scala"
  		  "testcases/probabilistic/Trid.scala"
  		  "testcases/probabilistic/Turbine.scala")

# Make sure the code is compiled
sbt compile

# generate daisy script
if [ ! -e daisy ]
then
  sbt script
fi


# Run probabilistic error phase of daisy on each testfile
# Runs the non-probabilistic worst case error analysis with setting A: 0 DSI, 100 outer subdivision and uniform float32 inputs
for file in "${files[@]}"
do
  	echo ${file}
	echo "******************** Non-probabilisitic error analysis for uniform float32 inputs ********************"
  	./daisy --probabilisticError $file --precision=Float32 --divLimit=100 --worstCase --thresProb=0.01
  	# Runs the non-probabilistic worst case error analysis with setting B: 0 DSI, 100 outer subdivision and gaussian float32 inputs
  	echo "******************** Non-probabilisitic error analysis for gaussian float32 inputs ********************"
  	echo ${file}
  	./daisy --probabilisticError $file --precision=Float32 --divLimit=100 --worstCase --gaussian --thresProb=0.01
	# Runs the probabilistic error analysis with setting C: 2 DSI, 50 outer subdivision and uniform float32 inputs
	echo "******************** Probabilisitic error analysis for uniform float32 inputs ********************"
 	echo ${file}
  	./daisy --probabilisticError $file --precision=Float32 --dsSubDiv=2 --divLimit=50 --thresProb=0.01
	# Runs the probabilistic error analysis with setting D: 2 DSI, 50 outer subdivision and gaussian float32 inputs
	echo "******************** Probabilisitic error analysis for gaussian float32 inputs ********************"
	echo ${file}
  	./daisy --probabilisticError $file --precision=Float32 --dsSubDiv=2 --divLimit=50 --gaussian --thresProb=0.01
	# Runs the probabilistic error analysis with approximate error specification: 2 DSI, 50 outer subdivision and uniform float32 inputs
	echo "******************** Probabilisitic error analysis for uniform float32 inputs with approximate error specification ********************"
	echo ${file}
	./daisy --probabilisticError $file --precision=Float32 --dsSubDiv=2 --divLimit=50 --thresProb=0.01 --approximate --bigErrorMultiplier=2 --bigErrorProb=0.1
	# Runs the probabilistic error analysis with approximate error specification: 2 DSI, 50 outer subdivision and gaussian float32 inputs
	echo "******************** Probabilisitic error analysis for gaussian float32 inputs with approximate error specification ********************"
	echo ${file}
	./daisy --probabilisticError $file --precision=Float32 --dsSubDiv=2 --divLimit=50 --thresProb=0.01 --gaussian --approximate --bigErrorMultiplier=2 --bigErrorProb=0.1
	# Run probabilistic phase of daisy on each testfile
	# Runs the probabilistic analysis with 4 DSI and 8000 outer subdivision for float32 uniform inputs
	# echo "******************** Probabilisitic Analysis for uniform distribution of inputs ********************"
  	#echo ${file}
  	#./daisy --probabilistic $file --thresholdFile=${thres1} --precision=Float32
	# Runs the probabilistic analysis with 4 DSI and 8000 outer subdivision for float32 normal inputs
	#echo "******************** Probabilisitic Analysis for normal distribution of inputs ********************"
	#echo ${file}
 	#./daisy --probabilistic $file --thresholdFile=${thres2} --precision=Float32 --gaussian
done
