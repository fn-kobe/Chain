#!/bin/bash
#for and

echo "Begin to generate 5 blockchains"
perl generatedMultipleAnyBCSerialization.pl 5 3
perl sendToRemote.pl

for ((i=0;i<35;++i))
do
:<<! 
  3 bc 
    1 -> 2 -> 3
!
  name=and_1
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemote.pl 15 1 1800 ";;7-9;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-triggerOut111-600-0-0-sendacptriggerdata-sender1;;" ";;4-9;ACPActionParameterSetTransaction"        "4;setacp-sender4-default-triggerOut111-sender1-111-triggerOut222-600-0-0;;" "7;setacp-sender7-default-triggerOut222-null-222-triggerDone333-600-0-0;;"  | tee $name/${name}_$i.log

:<<! 
  4 bc and 4*3 nodes 
    1 -> 2 -> 4
      -> 3 -> 4
!
  name=and_2
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemote.pl 15 1 1800 ";;10-12;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-triggerOutll1-600-0-0-sendacptriggerdata-sender1;;" ";;4-12;ACPActionParameterSetTransaction"        "4;setacp-sender4-default-triggerOutll1-sender1-111-triggerTo444-600-0-0;;" "7;setacp-sender7-default-triggerOutll1-sender1-111-triggerTo444-600-0-0;;"       "10;setacp-sender10-default-triggerTo444-null-null-triggerDone555-600-0-0;;" | tee $name/${name}_$i.log

:<<!  
      5 bc and 5*3 nodes 
      1 ->2 -> 5
        ->3 -> 5
        ->4 -> 5
!
  name=and_3
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemote.pl 15 1 1800 ";;13-15;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-triggerOutll1-600-0-0-sendacptriggerdata-sender1;;"  ";;4-15;ACPActionParameterSetTransaction"       "4;setacp-sender4-default-triggerOutll1-sender1-111-triggerTo555-600-0-0;;" "7;setacp-sender7-default-triggerOutll1-sender1-111-triggerTo555-600-0-0;;"       "10;setacp-sender10-default-triggerOutll1-sender1-111-triggerTo555-600-0-0;;"       "13;setacp-sender13-default-triggerTo555-null-null-triggerDone555-600-0-0;;" | tee $name/${name}_$i.log
done

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

:<<!
    5bc
        or
      1 ->2 -> 5
        ->3 -> 5
        ->4 -> 5
!
  name=or_2
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemote.pl 15 1 1800 ";;13;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-randomTrigger1in3Outll1-600-3-1-sendacptriggerdata-sender1;;" ";;4-15;ACPActionParameterSetTransaction"   "4;setacp-sender4-default-randomTrigger1in3Outll1-sender1-111-triggerTo555-600-0-0;;" "7;setacp-sender7-default-randomTrigger1in3Outll1-sender1-111-triggerTo555-600-0-0;;" "10;setacp-sender10-default-randomTrigger1in3Outll1-sender1-111-triggerTo555-600-0-0;;" "13;setacp-sender10-default-triggerTo555-null-null-triggerDone555-600-0-0;;" | tee $name/${name}_$i.log

:<<!
    5bc
      // may missing one
          or
      1 ->2 -> 5
        ->3 -> 4 
!
  name=or_3
  if [ ! -d $name ]
  then
    mkdir $name
  fi    
  perl testSequenceRemote.pl 15 1 1800 ";;6|7;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-randomTrigger1in2Outll1-600-2-1-sendacptriggerdata-sender1;;" ";;4-15;ACPActionParameterSetTransaction"   "4;setacp-sender4-default-randomTrigger1in2Outll1-sender1-111-triggerOut222-600-0-0;;" "7;setacp-sender7-default-randomTrigger1in2Outll1-sender1-111-triggerOut333-600-0-0;;" "10;setacp-sender10-default-triggerOut333-sender7-333-triggerDone444-600-0-0;;" "13;setacp-sender13-default-triggerOut222-sender4-222-triggerDone555-600-0-0;;" | tee $name/${name}_$i.log

done



echo "Begin to generate 7 blockchains"
perl generatedMultipleAnyBCSerialization.pl 7 3
perl sendToRemote.pl

#for or
for ((i=0;i<35;++i))
do


:<<!
    7bc
        or
      1 -> 2 ->   ->7
             or
        -> 3 -> 4 -> 7
             -> 5 -> 6
!
  name=or_4
  if [ ! -d $name ]
  then
    mkdir $name
  fi     
  perl testSequenceRemote.pl 21 1 1800 ";;6|7;log***-Succeed-to-execute-ACP-action"    "1;sendacptrigger-randomTrigger1in2Outll1-600-2-1-sendacptriggerdata-sender1;;" ";;4-21;ACPActionParameterSetTransaction"   "4;setacp-sender4-default-randomTrigger1in2Outll1-sender1-111-triggerTo777-600-0-0;;" "7;setacp-sender7-default-randomTrigger1in2Outll1-sender1-111-randomTrigger1in2Out333-600-2-1;;" "10;setacp-sender10-default-randomTrigger1in2Out333-null-null-triggerTo777-600-0-0;;" "13;setacp-sender13-default-randomTrigger1in2Out333-null-null-triggerOut555-600-0-0;;" "16;setacp-sender16-default-triggerOut555-null-null-triggerDone666-600-0-0;;" "19;setacp-sender19-default-triggerTo777-null-null-triggerDone777-600-0-0;;" | tee $name/${name}_$i.log

done


