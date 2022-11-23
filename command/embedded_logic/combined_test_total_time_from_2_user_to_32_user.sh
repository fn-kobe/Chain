#!/bin/bash

#exchange with ebedded logic
for ((i=0;i<200;++i))
do
#2 user
  name=embed2
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;" | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate2
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log

#4 user
 name=embed4
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser4" "1;send-embedUser4-789002-789001-0;1-8;embedUser3" "1;send-embedUser3-789002-789001-0;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;"  | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate4
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser4"  "1;send-separateUser4-789002-789001-0;1-8;separateUser3" "1;send-separateUser3-789002-789001-0;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log
  
  #8 user
 name=embed8
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser8" "1;send-embedUser8-789002-789001-0;1-8;embedUser7" "1;send-embedUser7-789002-789001-0;1-8;embedUser6" "1;send-embedUser6-789002-789001-0;1-8;embedUser5" "1;send-embedUser5-789002-789001-0;1-8;embedUser4" "1;send-embedUser4-789002-789001-0;1-8;embedUser3" "1;send-embedUser3-789002-789001-0;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;"  | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate8
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser8"  "1;send-separateUser8-789002-789001-0;1-8;separateUser7" "1;send-separateUser7-789002-789001-0;1-8;separateUser6" "1;send-separateUser6-789002-789001-0;1-8;separateUser5" "1;send-separateUser5-789002-789001-0;1-8;separateUser4" "1;send-separateUser4-789002-789001-0;1-8;separateUser3" "1;send-separateUser3-789002-789001-0;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log
  
#16 user
  name=embed16
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser16" "1;send-embedUser16-789002-789001-0;1-8;embedUser15" "1;send-embedUser15-789002-789001-0;1-8;embedUser14" "1;send-embedUser14-789002-789001-0;1-8;embedUser13" "1;send-embedUser13-789002-789001-0;1-8;embedUser12" "1;send-embedUser12-789002-789001-0;1-8;embedUser11" "1;send-embedUser11-789002-789001-0;1-8;embedUser10" "1;send-embedUser10-789002-789001-0;1-8;embedUser9" "1;send-embedUser9-789002-789001-0;1-8;embedUser8" "1;send-embedUser8-789002-789001-0;1-8;embedUser7" "1;send-embedUser7-789002-789001-0;1-8;embedUser6" "1;send-embedUser6-789002-789001-0;1-8;embedUser5" "1;send-embedUser5-789002-789001-0;1-8;embedUser4" "1;send-embedUser4-789002-789001-0;1-8;embedUser3" "1;send-embedUser3-789002-789001-0;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;"  | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate16
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser16"  "1;send-separateUser16-789002-789001-0;1-8;separateUser15" "1;send-separateUser15-789002-789001-0;1-8;separateUser14" "1;send-separateUser14-789002-789001-0;1-8;separateUser13" "1;send-separateUser13-789002-789001-0;1-8;separateUser12" "1;send-separateUser12-789002-789001-0;1-8;separateUser11" "1;send-separateUser11-789002-789001-0;1-8;separateUser10" "1;send-separateUser10-789002-789001-0;1-8;separateUser9" "1;send-separateUser9-789002-789001-0;1-8;separateUser8" "1;send-separateUser8-789002-789001-0;1-8;separateUser7" "1;send-separateUser7-789002-789001-0;1-8;separateUser6" "1;send-separateUser6-789002-789001-0;1-8;separateUser5" "1;send-separateUser5-789002-789001-0;1-8;separateUser4" "1;send-separateUser4-789002-789001-0;1-8;separateUser3" "1;send-separateUser3-789002-789001-0;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log

  
#32 user
  name=embed32
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;embedUser32" "1;send-embedUser32-789002-789001-0;1-8;embedUser31" "1;send-embedUser31-789002-789001-0;1-8;embedUser30" "1;send-embedUser30-789002-789001-0;1-8;embedUser29" "1;send-embedUser29-789002-789001-0;1-8;embedUser28" "1;send-embedUser28-789002-789001-0;1-8;embedUser27" "1;send-embedUser27-789002-789001-0;1-8;embedUser26" "1;send-embedUser26-789002-789001-0;1-8;embedUser25" "1;send-embedUser25-789002-789001-0;1-8;embedUser24" "1;send-embedUser24-789002-789001-0;1-8;embedUser23" "1;send-embedUser23-789002-789001-0;1-8;embedUser22" "1;send-embedUser22-789002-789001-0;1-8;embedUser21" "1;send-embedUser21-789002-789001-0;1-8;embedUser20" "1;send-embedUser20-789002-789001-0;1-8;embedUser19" "1;send-embedUser19-789002-789001-0;1-8;embedUser18" "1;send-embedUser18-789002-789001-0;1-8;embedUser17" "1;send-embedUser17-789002-789001-0;1-8;embedUser16" "1;send-embedUser16-789002-789001-0;1-8;embedUser15" "1;send-embedUser15-789002-789001-0;1-8;embedUser14" "1;send-embedUser14-789002-789001-0;1-8;embedUser13" "1;send-embedUser13-789002-789001-0;1-8;embedUser12" "1;send-embedUser12-789002-789001-0;1-8;embedUser11" "1;send-embedUser11-789002-789001-0;1-8;embedUser10" "1;send-embedUser10-789002-789001-0;1-8;embedUser9" "1;send-embedUser9-789002-789001-0;1-8;embedUser8" "1;send-embedUser8-789002-789001-0;1-8;embedUser7" "1;send-embedUser7-789002-789001-0;1-8;embedUser6" "1;send-embedUser6-789002-789001-0;1-8;embedUser5" "1;send-embedUser5-789002-789001-0;1-8;embedUser4" "1;send-embedUser4-789002-789001-0;1-8;embedUser3" "1;send-embedUser3-789002-789001-0;1-8;embedUser2" "1;send-embedUser2-789002-789001-0;1-8;embedUser1" "1;send-embedUser1-789001-789002-0;;"  | tee $name/${name}_$i.log


#putting code is first and then exchange
  name=separate32
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 8 1 1800 $name ";;1-8;separateUser32" "1;send-separateUser32-789002-789001-0;1-8;separateUser31" "1;send-separateUser31-789002-789001-0;1-8;separateUser30" "1;send-separateUser30-789002-789001-0;1-8;separateUser29" "1;send-separateUser29-789002-789001-0;1-8;separateUser28" "1;send-separateUser28-789002-789001-0;1-8;separateUser27" "1;send-separateUser27-789002-789001-0;1-8;separateUser26" "1;send-separateUser26-789002-789001-0;1-8;separateUser25" "1;send-separateUser25-789002-789001-0;1-8;separateUser24" "1;send-separateUser24-789002-789001-0;1-8;separateUser23" "1;send-separateUser23-789002-789001-0;1-8;separateUser22" "1;send-separateUser22-789002-789001-0;1-8;separateUser21" "1;send-separateUser21-789002-789001-0;1-8;separateUser20" "1;send-separateUser20-789002-789001-0;1-8;separateUser19" "1;send-separateUser19-789002-789001-0;1-8;separateUser18" "1;send-separateUser18-789002-789001-0;1-8;separateUser17" "1;send-separateUser17-789002-789001-0;1-8;separateUser16" "1;send-separateUser16-789002-789001-0;1-8;separateUser15" "1;send-separateUser15-789002-789001-0;1-8;separateUser14" "1;send-separateUser14-789002-789001-0;1-8;separateUser13" "1;send-separateUser13-789002-789001-0;1-8;separateUser12" "1;send-separateUser12-789002-789001-0;1-8;separateUser11" "1;send-separateUser11-789002-789001-0;1-8;separateUser10" "1;send-separateUser10-789002-789001-0;1-8;separateUser9" "1;send-separateUser9-789002-789001-0;1-8;separateUser8" "1;send-separateUser8-789002-789001-0;1-8;separateUser7" "1;send-separateUser7-789002-789001-0;1-8;separateUser6" "1;send-separateUser6-789002-789001-0;1-8;separateUser5" "1;send-separateUser5-789002-789001-0;1-8;separateUser4" "1;send-separateUser4-789002-789001-0;1-8;separateUser3" "1;send-separateUser3-789002-789001-0;1-8;separateUser2" "1;send-separateUser2-789002-789001-0;1-8;separateUser1" "1;send-separateUser1-789001-789002-0;1-8;puttingCode" "1;send-puttingCode-789001-789002-0;;" | tee $name/${name}_$i.log

done

