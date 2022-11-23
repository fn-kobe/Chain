#!/bin/bash
#for and



#for or
for ((i=0;i<35;++i))
do

:<<!
    4bc
        or
      1 ->2 -> 4
        ->3 -> 4
!
  name=or_1
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemote.pl 15 1 1800 ";;10;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-randomTrigger1in2Outll1-600-2-1-sendacptriggerdata-sender1;;" ";;4-12;ACPActionParameterSetTransaction"   "4;setacp-sender4-default-randomTrigger1in2Outll1-sender1-111-triggerTo444-600-0-0;;" "7;setacp-sender7-default-randomTrigger1in2Outll1-sender1-111-triggerTo444-600-0-0;;" "10;setacp-sender10-default-triggerTo444-null-null-triggerDone444-600-0-0;;"| tee $name/${name}_$i.log


done


