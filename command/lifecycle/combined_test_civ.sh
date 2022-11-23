#!/bin/bash

#putting code, instantiation, and invocation
for ((i=0;i<35;++i))
do
  name=civ
  if [ ! -d $name ]
  then
    mkdir $name
  fi
  perl testSequenceRemoteWithName.pl 3 1 1800 $name ";;1-3;logTest-function-called-successfully"    "1;send-lck:civ:n#TestPuttingCode,c#cHVibGljIGNsYXNzIFRlc3RQdXR0aW5nQ29kZSB7CmludCBpID0gMDsKcHVibGljIHZvaWQgbWV0aG9kMSgpewpTeXN0ZW0ub3V0LnByaW50ZigiW1Rlc3RdW0lORk9dWyoqKioqKioqKioqXSBtZXRob2QxIGluIFRlc3RQdXR0aW5nQ29kZS4gVGVzdCBmdW5jdGlvbiBjYWxsZWQgc3VjY2Vzc2Z1bGx5IVxuIik7Cn0KfQo=,i#instance1,m#method1-789001-789002-0-gas#10000000-gas#10000000;;" | tee $name/${name}_$i.log


#putting code is first and then (instantiate and invoke)
  name=c_iv
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 3 1 1800 $name ";;1-3;logTest-function-called-successfully"    "1;send-lck:iv:n#TestPuttingCode,i#instance2,m#method1-789001-789002-0-gas#10000000;1-3;logSucceed-to-put-code"    "1;send-lck:c:n#TestPuttingCode,c#cHVibGljIGNsYXNzIFRlc3RQdXR0aW5nQ29kZSB7CmludCBpID0gMDsKcHVibGljIHZvaWQgbWV0aG9kMSgpewpTeXN0ZW0ub3V0LnByaW50ZigiW1Rlc3RdW0lORk9dWyoqKioqKioqKioqXSBtZXRob2QxIGluIFRlc3RQdXR0aW5nQ29kZS4gVGVzdCBmdW5jdGlvbiBjYWxsZWQgc3VjY2Vzc2Z1bGx5IVxuIik7Cn0KfQo=-789001-789002-0-gas#10000000;;" | tee $name/${name}_$i.log


#putting code is first and then (instantiate and invoke)
  name=ci_v
  if [ ! -d $name ]
  then
    mkdir $name
  fi

  perl testSequenceRemoteWithName.pl 3 1 1800 $name ";;1-3;logTest-function-called-successfully"    "1;send-lck:v:n#TestPuttingCode,i#instance2,m#method1-789001-789002-0-gas#10000000;1-3;logSucceed-to-instantiate"    "1;send-lck:ci:n#TestPuttingCode,c#cHVibGljIGNsYXNzIFRlc3RQdXR0aW5nQ29kZSB7CmludCBpID0gMDsKcHVibGljIHZvaWQgbWV0aG9kMSgpewpTeXN0ZW0ub3V0LnByaW50ZigiW1Rlc3RdW0lORk9dWyoqKioqKioqKioqXSBtZXRob2QxIGluIFRlc3RQdXR0aW5nQ29kZS4gVGVzdCBmdW5jdGlvbiBjYWxsZWQgc3VjY2Vzc2Z1bGx5IVxuIik7Cn0KfQo=,i#instance2-789001-789002-0-gas#10000000;;" | tee $name/${name}_$i.log


#sparately runs each action
  name=c_i_v
  if [ ! -d $name ]
  then
    mkdir $name
  fi

 perl testSequenceRemoteWithName.pl 3 1 1800 $name ";;1-3;logTest-function-called-successfully"  "1;send-lck:v:n#TestPuttingCode,i#instance2,m#method1-789001-789002-0-gas#10000000;1-3;logSucceed-to-instantiate"    "1;send-lck:i:n#TestPuttingCode,i#instance2-789001-789002-0-gas#10000000;1-3;logSucceed-to-put-code"    "1;send-lck:c:n#TestPuttingCode,c#cHVibGljIGNsYXNzIFRlc3RQdXR0aW5nQ29kZSB7CmludCBpID0gMDsKcHVibGljIHZvaWQgbWV0aG9kMSgpewpTeXN0ZW0ub3V0LnByaW50ZigiW1Rlc3RdW0lORk9dWyoqKioqKioqKioqXSBtZXRob2QxIGluIFRlc3RQdXR0aW5nQ29kZS4gVGVzdCBmdW5jdGlvbiBjYWxsZWQgc3VjY2Vzc2Z1bGx5IVxuIik7Cn0KfQo=-789001-789002-0-gas#10000000;;" | tee $name/${name}_$i.log


done

