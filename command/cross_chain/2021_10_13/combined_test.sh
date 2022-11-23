#!/bin/bash

############ This script is not completed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
############ Command should be tested further
#!!! Notice pub key RSA512a.pub should be prepared

perl testSequenceRemoteSeparateSteps.pl 6 1 1800 $testCaseName lfc:i instantiation

for ((i=0;i<35;++i))
do
  testCaseName=ct # conditional transaction method
  if [ ! -d $testCaseName ]
  then
    mkdir $testCaseName
  fi
  #### <TO DO> repeat run command
  perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c hashLocking  ";;1-6;log[HashLockingProcessor][INFO]-***-Hashlocking-completes 4;send-hashLockingData_mqZfCHWcuoEBFqrk41R3MY8eYEm/4ZSWcHinPK7b41cggjtIoDXO/AjWz7BDvFPOUhxpukfK0iuspDmFQisk3w==-789004-789004-0;1-3;hashLockingData 1;send-hashLockingData_mqZfCHWcuoEBFqrk41R3MY8eYEm/4ZSWcHinPK7b41cggjtIoDXO/AjWz7BDvFPOUhxpukfK0iuspDmFQisk3w==-789004-789004-0;1-6;hashLockingSetting 4;send-hashLockingSetting_RSA512a.pub_hashLockingKeyword-789002-789001-2;; 1;send-hashLockingSetting_RSA512a.pub_hashLockingKeyword-789001-789002-2;;" > hl_2bc_2tx_4_24_1.log

  testCaseName=nortary
  if [ ! -d $testCaseName ]
  then
    mkdir $testCaseName
  fi

  perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c ct ";;1-6;ct003 ;;1-6;ct002 ;;1-6;ct001  1;sendcmt-10001-variable-789003-789001-assetType3-10-222:789001:789002:assetType1:10-ct003;; 4;sendcmt-10001-variable-789002-789003-assetType2-10-111:789003:789001:assetType3:10-ct002;; 1;sendcmt-10001-variable-789001-789002-assetType1-10-222:789002:789003:assetType2:10-ct001;;" > ct_2bc_3tx_4_24_1.log
  
  testCaseName=hasklocking
  if [ ! -d $testCaseName ]
  then
    mkdir $testCaseName
  fi

  perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c nortary ";;1-6;log[NotaryExchangeProcessor][INFO]-***-Two-phase-commit-of 1:1:4;sendcomct-10081-notary_second_tx-789004-789004-0;1-6;notary_first_txonbc3 4;sendcomct-10081-notary_first_txonbc3-789003-789004-2;1-3;notary_first_txonbc2 1;sendcomct-10081-notary_first_txonbc2-789002-789003-2;1-3;notary_first_txonbc1 1;sendcomct-10081-notary_first_txonbc1-789001-789002-2;;" > notary_2bc_3tx_2_24_1.log

done
perl testSequenceRemoteSeparateSteps.pl 6 1 1800 $testCaseName lfc:t termination

################## The following is reference to write the script, remove them when run.

#first setting to negotiate random hash and second to send data
perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c hashLocking  ";;1-6;log[HashLockingProcessor][INFO]-***-Hashlocking-completes 4;send-hashLockingData_mqZfCHWcuoEBFqrk41R3MY8eYEm/4ZSWcHinPK7b41cggjtIoDXO/AjWz7BDvFPOUhxpukfK0iuspDmFQisk3w==-789004-789004-0;1-3;hashLockingData 1;send-hashLockingData_mqZfCHWcuoEBFqrk41R3MY8eYEm/4ZSWcHinPK7b41cggjtIoDXO/AjWz7BDvFPOUhxpukfK0iuspDmFQisk3w==-789004-789004-0;1-6;hashLockingSetting 4;send-hashLockingSetting_RSA512a.pub_hashLockingKeyword-789002-789001-2;; 1;send-hashLockingSetting_RSA512a.pub_hashLockingKeyword-789001-789002-2;;" > hl_2bc_2tx_4_24_1.log

perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c ct ";;1-6;ct003 ;;1-6;ct002 ;;1-6;ct001  1;sendcmt-10001-variable-789003-789001-assetType3-10-222:789001:789002:assetType1:10-ct003;; 4;sendcmt-10001-variable-789002-789003-assetType2-10-111:789003:789001:assetType3:10-ct002;; 1;sendcmt-10001-variable-789001-789002-assetType1-10-222:789002:789003:assetType2:10-ct001;;" > ct_2bc_3tx_4_24_1.log

perl testSequenceRemoteSeparateSteps.pl 6 30 1200 lfc:c nortary ";;1-6;log[NotaryExchangeProcessor][INFO]-***-Two-phase-commit-of 1:1:4;sendcomct-10081-notary_second_tx-789004-789004-0;1-6;notary_first_txonbc3 4;sendcomct-10081-notary_first_txonbc3-789003-789004-2;1-3;notary_first_txonbc2 1;sendcomct-10081-notary_first_txonbc2-789002-789003-2;1-3;notary_first_txonbc1 1;sendcomct-10081-notary_first_txonbc1-789001-789002-2;;" > notary_2bc_3tx_2_24_1.log
