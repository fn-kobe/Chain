#!/bin/bash

# All exchange through one group to simulate a single smart contract
# 1 exchange with 5, 2 exchange with 6, and so on to simulate the accumulate of asset
# key_done is used to ensure that the last tx has been sealed, and then all related txs has been processed
userAllArray=(
";;1-4;key_done"
"3:;send-key_done_789001-789001-789005-0;1-4;key_789008" 
"3;send-P2PSC:all:0:asset#lock#4:asset#lock#8:OP_log_giving_8_assets_for_4_asset_key_789008-789008-789004-0;1-4;key_789007" 
"3;send-P2PSC:all:0:asset#lock#3:asset#lock#7:OP_log_giving_7_assets_for_3_asset_key_789007-789007-789003-0;1-4;key_789006" 
"3;send-P2PSC:all:0:asset#lock#2:asset#lock#6:OP_log_giving_6_assets_for_2_asset_key_789006-789006-789002-0;1-4;key_789005" 
"3;send-P2PSC:all:0:asset#lock#1:asset#lock#5:OP_log_giving_5_assets_for_1_asset_key_789005-789005-789001-0;1-4;key_789004" 
"3;send-P2PSC:all:0:asset#lock#8:asset#lock#4:OP_log_giving_4_assets_for_8_asset_key_789004-789004-789008-0;1-4;key_789003" 
"3;send-P2PSC:all:0:asset#lock#7:asset#lock#3:OP_log_giving_3_assets_for_7_asset_key_789003-789003-789007-0;1-4;key_789002" 
"3;send-P2PSC:all:0:asset#lock#6:asset#lock#2:OP_log_giving_2_assets_for_6_asset_key_789002-789002-789006-0;1-4;key_789001" 
"3;send-P2PSC:all:0:asset#lock#5:asset#lock#1:OP_log_giving_1_assets_for_5_asset_key_789001-789001-789005-0;;" 
)

# each exchange is in a signle group id to simulate separate P2P SC
# 1 exchange with 5, 2 exchange with 6, and so on to simulate the accumulate of asset
userGroupSeparateArray=(
";;1-4;key_done"
"3:;send-key_done_789001-789001-789005-0;1-4;key_789008" 
"3;send-P2PSC:group:104:asset#lock#4:asset#lock#8:OP_log_giving_8_assets_for_4_asset_key_789008-789008-789004-0;1-4;key_789007" 
"3;send-P2PSC:group:103:asset#lock#3:asset#lock#7:OP_log_giving_7_assets_for_3_asset_key_789007-789007-789003-0;1-4;key_789006" 
"3;send-P2PSC:group:102:asset#lock#2:asset#lock#6:OP_log_giving_6_assets_for_2_asset_key_789006-789006-789002-0;1-4;key_789005" 
"3;send-P2PSC:group:101:asset#lock#1:asset#lock#5:OP_log_giving_5_assets_for_1_asset_key_789005-789005-789001-0;1-4;key_789004" 
"3;send-P2PSC:group:104:asset#lock#8:asset#lock#4:OP_log_giving_4_assets_for_8_asset_key_789004-789004-789008-0;1-4;key_789003" 
"3;send-P2PSC:group:103:asset#lock#7:asset#lock#3:OP_log_giving_3_assets_for_7_asset_key_789003-789003-789007-0;1-4;key_789002" 
"3;send-P2PSC:group:102:asset#lock#6:asset#lock#2:OP_log_giving_2_assets_for_6_asset_key_789002-789002-789006-0;1-4;key_789001" 
"3;send-P2PSC:group:101:asset#lock#5:asset#lock#1:OP_log_giving_1_assets_for_5_asset_key_789001-789001-789005-0;;"
)

#simulate out of work
userGroupAllOOWArray=(
"3;send-P2PSC:all:0:asset#lock#4:asset#lock#8:out_of_work_key_789008-789008-789004-0;1-4;key_789007" 
"3;send-P2PSC:all:0:asset#lock#3:asset#lock#7:out_of_work_key_789007-789007-789003-0;1-4;key_789006" 
"3;send-P2PSC:all:0:asset#lock#2:asset#lock#6:out_of_work_key_789006-789006-789002-0;1-4;key_789005" 
"3;send-P2PSC:all:0:asset#lock#1:asset#lock#5:out_of_work_key_789005-789005-789001-0;1-4;key_789004" 
"3;send-P2PSC:all:0:asset#lock#8:asset#lock#4:out_of_work_key_789004-789004-789008-0;1-4;key_789003" 
"3;send-P2PSC:all:0:asset#lock#7:asset#lock#3:out_of_work_key_789003-789003-789007-0;1-4;key_789002" 
"3;send-P2PSC:all:0:asset#lock#6:asset#lock#2:out_of_work_key_789002-789002-789006-0;1-4;key_789001" 
"3;send-P2PSC:all:0:asset#lock#5:asset#lock#1:out_of_work_key_789001-789001-789005-0;;" 
)

# each exchange is in a signle group id to simulate separate P2P SC
# 1 exchange with 5, 2 exchange with 6, and so on to simulate the accumulate of asset
userGroupSeparateOOWArray=(
"3;send-P2PSC:group:104:asset#lock#4:asset#lock#8:out_of_work_key_789008-789008-789004-0;1-4;key_789007" 
"3;send-P2PSC:group:103:asset#lock#3:asset#lock#7:out_of_work_key_789007-789007-789003-0;1-4;key_789006" 
"3;send-P2PSC:group:102:asset#lock#2:asset#lock#6:out_of_work_key_789006-789006-789002-0;1-4;key_789005" 
"3;send-P2PSC:group:101:asset#lock#1:asset#lock#5:out_of_work_key_789005-789005-789001-0;1-4;key_789004" 
"3;send-P2PSC:group:104:asset#lock#8:asset#lock#4:out_of_work_key_789004-789004-789008-0;1-4;key_789003" 
"3;send-P2PSC:group:103:asset#lock#7:asset#lock#3:out_of_work_key_789003-789003-789007-0;1-4;key_789002" 
"3;send-P2PSC:group:102:asset#lock#6:asset#lock#2:out_of_work_key_789002-789002-789006-0;1-4;key_789001" 
"3;send-P2PSC:group:101:asset#lock#5:asset#lock#1:out_of_work_key_789001-789001-789005-0;;"
)

# In each iteration, there will be one mistake to be missing from user 1 to user 8
for ((i=0; i<8; ++i))
do
  pairByPairName="pairByPairSC"
  allInOneSCName="allInOneSC"
  
  if [ ! -d $pairByPairName ]
  then
    mkdir $pairByPairName
  fi
  
  if [ ! -d $allInOneSCName ]
  then
    mkdir $allInOneSCName
  fi

  #exchangePairByPairCommand="perl testSequenceRemoteWithName.pl 4 1 1800 $pairByPairName "
  exchangePairByPairParameter=""
  #exchangeAllInOneSCCommand="perl testSequenceRemoteWithName.pl 4 1 1800 $allInOneSCName "
  exchangeAllInOneSCParameter=""
  # 8 + 2 means two done check txs
  for ((j=0 ; j < $((8 + 2)); ++j))
  do
    if [ $i == $(($j - 2)) ]
    then
      exchangePairByPairParameter+=" ${userGroupSeparateOOWArray[$i]}"
      exchangeAllInOneSCParameter+=" ${userGroupAllOOWArray[$i]}"
    else
      exchangePairByPairParameter+=" ${userGroupSeparateArray[$j]}"
      exchangeAllInOneSCParameter+=" ${userAllArray[$j]}"
    fi
  done
  
  #exchangePairByPairCommand+=" | tee $pairByPairName/${pairByPairName}_$i.log"
  #exchangeAllInOneSCCommand+=" | tee $allInOneSCName/${allInOneSCName}_$i.log"

  echo "Pair by pair parameter: $exchangePairByPairParameter"
  perl testSequenceRemoteWithName.pl 4 1 1800 $pairByPairName "$exchangePairByPairParameter"  | tee $pairByPairName/${pairByPairName}_$i.log
  
  echo "All in one parameter: $exchangeAllInOneSCParameter"
  perl testSequenceRemoteWithName.pl 4 1 1800 $allInOneSCName "$exchangeAllInOneSCParameter" | tee $allInOneSCName/${allInOneSCName}_$i.log
done

