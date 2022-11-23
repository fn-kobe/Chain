#!/bin/bash

#exchange with ebedded logic
for ((i=0;i<35;++i))
do
  name=embed
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;" | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log

done

