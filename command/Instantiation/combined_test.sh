#!/bin/bash
#for and
mkdir -p testLog
for ((i=0;i<35;++i))
do
:<<! 
  Rquest and new
!
  perl testSequenceRemote.pl 4 1 1800 ";;1-4;log[SleepAndCallTwoExternalMethod][INFO]-***-smart-contract-end-at" "1;send-InstantiationTx:innerSmartContract:SleepAndCallTwoExternalMethod:10:10:10:noDelay:NoDisposable-789001-789002-0;;" | tee testLog/Rquestandnew_$i.log 

:<<! 
  Delay instantiation
!
    perl testSequenceRemote.pl 4 1 1800 ";;1-4;log[SleepAndCallTwoExternalMethod][INFO]-***-smart-contract-end-at" "1;send-InstantiationTx:innerSmartContract:SleepAndCallTwoExternalMethod:10:10:10:Delay:NoDisposable-789001-789002-0;;"  | tee  testLog/Delay_$i.log 


:<<! 
  Disposable instantiation
!
    perl testSequenceRemote.pl 4 1 1800 ";;1-4;log[SleepAndCallTwoExternalMethod][INFO]-***-smart-contract-end-at" "1;send-InstantiationTx:innerSmartContract:SleepAndCallTwoExternalMethod:10:10:10:noDelay:Disposable-789001-789002-0;;"  | tee  testLog/Disposable$i.log 
    
    
:<<! 
  Delay and Disposable instantiation
!
    perl testSequenceRemote.pl 4 1 1800 ";;1-4;log[SleepAndCallTwoExternalMethod][INFO]-***-smart-contract-end-at" "1;send-InstantiationTx:innerSmartContract:SleepAndCallTwoExternalMethod:10:10:10:Delay:Disposable-789001-789002-0;;"  | tee  testLog/DelayAndDisposable$i.log 

done