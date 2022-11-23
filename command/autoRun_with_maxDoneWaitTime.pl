#! /usr/bin/perl 
use 5.14.0;
use strict;
use warnings;
use Cwd qw(cwd);


# pre-requirement
# 1. All configuring files are set in each node

# 2. java rename to javaSp

# 3. setacp.txt is in initialConditionTransactionSendingNode node
#   and it should contains "acpparameterSetter" as keyword in it

# 4. setConditionTransaction.txt is configured in ACPParameterSettingNodeList

my $allNodeNumber = $ARGV[0];
my $ACPParameterSettingNodeList = $ARGV[1];
my $initialConditionTransactionSendingNode = $ARGV[2];
my $doneWaitTime = $ARGV[3];
my $runTimes = $ARGV[4];
my @nodeSubActionThreadArray;
my $checkInterval = 5;#in seconds
my $parameterSettingKeyWord = "acpparameterSetter";
my $initialConditionTransactionKeyWord = "sendacpconditiondata";

$SIG{'INT'} = 'int_handler';
sub int_handler{
  say "[Main][WARN] Get INT signal, try to kill all BC processes";
  killChildThread();
  exit 0;
}

sub main(){
  for (my $testRound = 0 ; $testRound < $runTimes; ++$testRound)
  {
    say "\n\n ******************* Begin test of the round " . $testRound . " at thread " . $$ . "\n";
    oneTest();
    say "\n\n ******************* Finish test of the round " . $testRound . " at thread " . $$  . "\n";
    sleep($checkInterval);
  }
}

sub oneTest(){
  # the node number of all node
  if (@ARGV < 5){
    say "[Main][ERROR] Not enough parameter. Format: allNodeNumber ACPParameterSettingNodeList initialConditionTransactionSendingNode  doneWaitTime runTimes";
    exit 1;
  }

  my $threadId = 0;
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    $threadId = fork();
    if (0 == $threadId){
      startBlockchain($i);
      last;
    } else{
      say "[Main][INFO] Thread " . $threadId . " has been started";
      push @nodeSubActionThreadArray, $threadId;
    }
  }
  
  if (0 != $threadId){
    sleep($checkInterval);
    furtherAction();
    #killChildThread();
    # Node need to wait as it will check and kill child process in furtherAction
  }
}

sub startBlockchain(){
  say "[CHILD][INFO] Begin to process in sub thread " . $$;
  my $id = shift;
  my $folderName = $id + 1;
  chdir $folderName or die "Folder " . $folderName . " does not exist";
  # 1. clean the older files
  my $command = "perl clean.pl";
  say "[CHILD][INFO] Clean the environment in sub thread " . $$;
  `$command`;
  # 2. run the command
  $command = "javaSp -jar blockchain.jar 2>&1 >> autoRun.log";
  #system("java", "-jar", "blockchain.jar", "2>&1", ">> autoRun.log");
  say "[CHILD][INFO] Start blockchain in sub thread " . $$;
  system($command);
}

sub furtherAction(){
  waitInitBlockchain();
  setACPParameter();
  waitParameterSettingDone();
  sendInitTrigger();
  waitFinish();
  dumpBC();
  backUpFolder();
  killChildThread();
}

sub waitInitBlockchain(){
  say "[Main][INFO] Enter waitInitBlockchain \n";
  
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    my $dirname = ($i + 1) . "\\Blockchain_dump";
    my $isInterBCOk = 0;
    my $isExterBCOk = 0;
    while (1 != $isInterBCOk || 1 != $isExterBCOk){
      sleep($checkInterval);
      my $dh;
      if (!opendir $dh, $dirname) {
        say "[Main][WARN] Can not open folder " . $dirname . " when at " . cwd;
        say "[Main][WARN] Try to enlong the sleep time";
        next;
      }
      
      my @files = readdir $dh;
      closedir $dh;
      
      foreach my $file (@files){
        if (-d $file){
          next;
        }
        if (-1 != index($file, "external")){
          $isExterBCOk = 1;
        }
        if (-1 != index($file, "internal")){
          $isInterBCOk = 1;
        }
        
        if (1 == $isInterBCOk && 1 == $isExterBCOk){
          say "[Main][INFO] Internal and external blockchain list has been generated.";
          last;
        }
      }
    } 
  }
  
  say "[Main][INFO] Eixt waitInitBlockchain";
}

sub setACPParameter(){
  say "[Main][INFO] Enter setACPParameter";
  
  my @separatedACPParameterSettingNodeList = split(/:/, $ACPParameterSettingNodeList);
  for my $i (@separatedACPParameterSettingNodeList){
    my $dirname = $i;
    my $c = "copy $dirname\\setacp.txt $dirname\\command\\command";
    say "$c" . " at folder " . cwd;
    sleep($checkInterval);
    my $r = `$c`;
    if (0 != $?){
      say $r;
      say "[Main][ERROR] Failed to set acp initial parameters. Try to kill all BC process";
      killChildThread();
    }
    say $r;
  }
  
  say "[Main][INFO] Eixt setACPParameter";
}

sub waitParameterSettingDone(){
  say "[Main][INFO] Enter waitParameterSettingDone";
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    my $dirname = ($i + 1) . "\\Blockchain_dump";
    say "[Main][INFO] Try to check acp parameter setting in BC " . ($i + 1);
    findContentInOneFolder($dirname, $parameterSettingKeyWord);
    say "[Main][INFO] Acp parameter has been set in BC " . ($i + 1);
  }
  say "[Main][INFO] Eixt waitParameterSettingDone";
}

sub sendInitTrigger(){
  say "[Main][INFO] Enter sendInitTrigger";
  
  if (0 == $initialConditionTransactionSendingNode){
    say "[Main][WARN] initialConditionTransactionSendingNode is error value 0";
  }
  my $c = "copy $initialConditionTransactionSendingNode\\setConditionTransaction.txt $initialConditionTransactionSendingNode\\command\\command";
  say "[Main][INFO] " . "$c" . " at folder " . cwd;
  sleep($checkInterval);
  say `$c`;
  
  say "[Main][INFO] Eixt sendInitTrigger";
}

sub waitFinish(){
  say "[Main][INFO] Enter waitFinish";
  
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    my $dirname = ($i + 1) . "\\Blockchain_dump";
    findContentInOneFolder($dirname, $initialConditionTransactionKeyWord);
  }
  # as we do not know which is the done flag, we use timeout mechanism
  sleep($doneWaitTime);
  
  say "[Main][INFO] Eixt waitFinish";
}

sub dumpBC(){
  say "[Main][INFO] Enter dumpBC";
  # TO DO
  say "[Main][INFO] Eixt dumpBC";
}

sub backUpFolder(){
  say "[Main][INFO] Enter backUpFolder";
  
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime;
  $year += 1900; # $year是从1900开始计数的，所以$year需要加上1900；
  $mon += 1; # $mon是从0开始计数的，所以$mon需要加上1；
  my $datetime = sprintf ("%d-%02d-%02d_%02d_%02d_%02d", $year,$mon,$mday,$hour,$min,$sec);
  mkdir $datetime;
  say "[Main][INFO] Backup folder has been created " . $datetime;
  
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    my $dirname = ($i + 1);
    mkdir $datetime. "\\" . $dirname;
    say "[Main][INFO] Node back up folder has been created " . $dirname;
    my $c = "xcopy $dirname $datetime\\$dirname /s /e /y";
    my $r = `$c`;
    say "[Main][INFO] File backup result " . $r;
  }
  
  say "[Main][INFO] Eixt backUpFolder";
}

sub killChildThread{
  say "Enter killChildThread";
  kill 9,@nodeSubActionThreadArray;
  # We do not find a good way to kill, just eixt and java jar process exit automatically. 
  my $c = "taskkill /im javaSp.exe /t /f";
  say "[Main][INFO] " . `$c`;
  #exit(0);
  say "[Main][INFO] Eixt killChildThread";
}

sub findContentInOneFolder{
    my $dirname = shift;
    my $content = shift;
    my $isFound = 0;
    do{
      sleep($checkInterval);
      opendir my($dh), $dirname or die "Couldn't open dir '$dirname': $!";
      my @files = readdir $dh;
      closedir $dh;

      for my $file (@files){
        $file = $dirname . "\\" . $file;
        if (-d $file){
          next;
        }
        if (!open(FILE, $file)){
          say "[Main][WARN] Can not open file " . $file . " at folder " . $dirname . " at current folder of " . cwd;
          next;
        }
        my $oneLine;
        while (my $oneLine = <FILE>) {
           if (-1 != index($oneLine, $content)){
            $isFound = 1;
            say "[Main][INFO] $content is found in file " . $file . " at folder " . $dirname;
            last;
           }
        }
        close(FILE);
        if (1 == $isFound){
          last;
        }
      }
      
    } while (1 != $isFound);
}

main();