#!/bin/bash

#putting code, instantiation, and invocation
for ((i=0;i<35;++i))
do
  name=embed
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embed" "1;send-embed-789001-789002-0;;" | tee $name/${name}_$i.log


#putting code is first and then (instantiate and invoke)
  name=separate
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateEnd" "1;send-separateEnd-789001-789002-0;1-8;separateStart" "1;send-separateStart-789001-789002-0;;" | tee $name/${name}_$i.log

done

