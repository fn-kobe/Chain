#/usr/bin/perl

# Pre-requirement: this script is in upper folder of command
# 1. All configuring files are set in each node, which may be generated b a shell
# 2. java rename to javaSp
#

use 5.14.0;
use strict;
use warnings;
use Cwd qw(cwd);

my @randomRunList;
my @nodeSubActionThreadArray;
my $randomRunListSeparator = ":";
my $checkInterval = 5;#in seconds
my $commandSentNode = "1";# Can be changed or by command
my $currentCommandGroup = "";# used to add to the backup folder

sub generateRandomSequenceForOneTest{
  say "[INFO] Enter generateRandomSequenceForOneTest";
  my ($unCompressedCommandList, $runPeriodInOneTime) = @_;
  
  # init random command list
  for (my $i = 0; $i < $runPeriodInOneTime; ++$i){
    $randomRunList[$i] = "";
  }
  
  #generate the sequence that one command run
  for my $command (@{$unCompressedCommandList}){
    my $random_number = int(rand($runPeriodInOneTime));
    $randomRunList[$random_number] .= ($command . $randomRunListSeparator);
    say "[INFO] " . $command . " is at " . ($random_number + 1);
  }
  say "[INFO] Eixt generateRandomSequenceForOneTest";
}

# return 0 when success, or return 1
sub sendOutCommands{
  say "[INFO] Enter sendOutCommands";
  
  my ($runPeriodInOneTime, $runInterval) = @_;
  for (my $i = 0; $i < $runPeriodInOneTime; ++$i){
    say "[INFO] *** Test " . $i . " interval";
    my $cmdList = $randomRunList[$i];
    if ("" ne $cmdList){
      if (!processOneInterval($cmdList)){
        say "[ERROR] Cannot continue to test due to test error";
        return 0;
       }
    } else {
      # it is OK that no command is arranged in this interval
      say "[INFO] No command to process in this interval. ";
    }
    
    sleep($runInterval);
  }
  
  say "[INFO] Eixt sendOutCommands";
  return 1;
}

# return 0 when success, or return 1
sub processOneInterval(){
  say "[INFO] Enter processOneInterval";
  my $cmdList = shift;
  my @allCmdContent;
  my @cmdArray = split(/$randomRunListSeparator/, $cmdList);
  printRange("[DEBUG] Command to process is :", @cmdArray);
  for my $cmd (@cmdArray){
    my @oneCmdContent = readFile($cmd);
    if (@oneCmdContent){
      push @allCmdContent, @oneCmdContent;
    } else {
      return 0;# No content due to some error configuration
    }
  }
  
  if (@allCmdContent){
    chdir $commandSentNode;
    chdir "command";
    waitAndReplaceFile("command", @allCmdContent);
    chdir "..";
    chdir "..";
  } 
  say "[INFO] Eixt processOneInterval";
  
  return 1;
}

sub waitAndReplaceFile{
  my ($fileName, @content) = @_;
  if (-e $fileName){
    say "[WARN] $fileName exists. Waiting ...";
    sleep(1);
  }
  
  replaceFile($fileName, @content);
}

sub replaceFile{
  my ($fileName, @content) = @_;
  my $file_handle;
  if (!open($file_handle, ">", "$fileName")){
    say "[ERROR] cannot open file $fileName";
    return;
  }
  
  for my $line (@content){
    print $file_handle  $line;
  }
  if (!close($file_handle)){
    say "[WARN] Couldn't close the file $fileName \n";
  }
  say "[INFO] succeed to add the content @content to $fileName";
}

sub readFile{
  my ($fileName) = @_;
 
  if (!open(FH, '<', $fileName)){
    say "[ERROR] cannot open file $fileName to read";
    return;
  }
  
  my @content;
  while(<FH>){
     push @content, $_;
  }
   
  close(FH);
  return @content;
}

sub extractRangeCmdList{
  my $commandList = shift;
  my @commandArray;
  if (-1 != index($commandList, "-")){
    my @rangeArray = split(/-/, $commandList);
    for (my $i = $rangeArray[0]; $i <= $rangeArray[1]; ++$i){
      push @commandArray, $i;
    }
  } else {
    push @commandArray, $commandList;
  }
  return \@commandArray;
}

sub extractSeparationCmdList{
  my $commandList = shift;
  my @commandArray;
  my @tmpArray = split(/:/, $commandList);
  say "[DEBUG] tmpArray " . @tmpArray;
  for my $range (@tmpArray){
    if (-1 != index($commandList, "-")){
      push @commandArray, @{extractRangeCmdList($range)};
    } else {
      push @commandArray, $range;
    }
  }
  return \@commandArray;
}

sub backUpFolder{
  say "[INFO] Enter backUpFolder";
  my $nodeNumber = shift;
  
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime;
  $year += 1900; # $year是从1900开始计数的，所以$year需要加上1900；
  $mon += 1; # $mon是从0开始计数的，所以$mon需要加上1；
  my $datetime = sprintf ("%d-%02d-%02d_%02d_%02d_%02d", $year,$mon,$mday,$hour,$min,$sec);
  my $backupFolderName = $datetime . "_G_" . $currentCommandGroup;
  mkdir $backupFolderName;
  say "[INFO] Backup folder has been created " . $backupFolderName;
  
  for (my $i = 0 ; $i < $nodeNumber; ++$i){
    my $dirname = ($i + 1);
    mkdir $backupFolderName. "\\" . $dirname;
    say "[INFO] Node back up folder has been created " . $dirname;
    my $c = "xcopy $dirname $backupFolderName\\$dirname /s /e /y";
    my $r = `$c`;
    say "[INFO] File backup result " . $r;
  }
  
  say "[INFO] Eixt backUpFolder";
}

sub oneTest{
  my ($nodeNumber, $commandList, $runInterval, $runPeriodInOneTime, $doneWaitTime ) = @_;
  my $childThreadId = 0;
  for (my $i = 0 ; $i < $nodeNumber; ++$i){
    $childThreadId = fork();
    if (0 == $childThreadId){
      startBlockchain($i);
      last;
    } else{
      say "[INFO] Thread " . $childThreadId . " has been started";
      push @nodeSubActionThreadArray, $childThreadId;
    }
  }
  
  if (0 != $childThreadId){
    sleep($checkInterval);
    furtherAction($nodeNumber, $commandList, $runInterval, $runPeriodInOneTime, $doneWaitTime );
    #killChildThread();
    # Node need to wait as it will check and kill child process in furtherAction
  }
}

sub furtherAction{
  my ($nodeNumber, $commandList, $runInterval, $runPeriodInOneTime, $doneWaitTime ) = @_;
  
  waitInitBlockchain($nodeNumber);
  
  if (!@randomRunList){
    say "[INFO] Failed to generate random sequence to run";
    return 0;
  }
  
  if (!sendOutCommands($runPeriodInOneTime, $runInterval)){
    return 0;
  }
  waitFinish($doneWaitTime);
  backUpFolder($nodeNumber);
  killChildThread();
}

sub waitFinish{
  my $doneWaitTime = shift;
  sleep($doneWaitTime);
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

sub waitInitBlockchain{
  my $nodeNumber = shift;
  say "[INFO] Enter waitInitBlockchain \n";
  
  for (my $i = 0 ; $i < $nodeNumber; ++$i){
    my $dirname = ($i + 1) . "\\Blockchain_dump";
    my $isInterBCOk = 0;
    my $isExterBCOk = 0;
    while (1 != $isInterBCOk || 1 != $isExterBCOk){
      sleep($checkInterval);
      my $dh;
      if (!opendir $dh, $dirname) {
        say "[WARN] Can not open folder " . $dirname . " when at " . cwd;
        say "[WARN] Try to enlong the sleep time";
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
          say "[INFO] Internal and external blockchain list has been generated.";
          last;
        }
      }
    } 
  }
  
  say "[INFO] Eixt waitInitBlockchain";
}

sub main{
  if (@ARGV < 6){
    say "[INFO] Missing parameters. test.pl nodeNumber commandList(1-3 or 1:2:3) runInterval runPeriodInOneTime doneWaitTime testTimes";
    return;
  }
  
  my $nodeNumber            = $ARGV[0];
  my $commandList           = $ARGV[1];
  my $runInterval           = $ARGV[2];
  my $runPeriodInOneTime    = $ARGV[3];
  my $doneWaitTime          = $ARGV[4];
  my $testTimes             = $ARGV[5];
   
  
  for (my $i = 0; $i < $testTimes; ++$i){
    say "\n[INFO] Begin to test " . $i . " round";
    my @commandSequenceList = split(/;/, $commandList);
    if (!@commandSequenceList){
      say "[ERROR] command list is not correct " + $commandList;
      return 0;
    }
    # Only generate once and reuse among command group
    my @commandArray = parseOneCommandSequence($commandSequenceList[0], $runPeriodInOneTime);
    if (!@commandArray){
      return 0;
    }

    generateRandomSequenceForOneTest(\@commandArray, $runPeriodInOneTime);
    for my $oneCommandGroup (@commandSequenceList){
      say "[INFO] ** begin to test command group " . $oneCommandGroup;
      $currentCommandGroup = $oneCommandGroup;
      if (!@commandArray){
        @commandArray = parseOneCommandSequence($oneCommandGroup, $runPeriodInOneTime);
      }
      oneTest($nodeNumber, \@commandArray, $runInterval, $runPeriodInOneTime, $doneWaitTime );
      @commandArray = ();
    }
    say "[INFO] End to test " . $i . " round";
  }
  
  return 1;
}

sub parseOneCommandSequence{
  my ($commandList, $runPeriodInOneTime) = @_;
  my @commandArray;
  if (-1 != index($commandList, ":")){
    push @commandArray, @{extractSeparationCmdList($commandList)};
  } elsif (-1 != index($commandList, "-")){
    push @commandArray, @{extractRangeCmdList($commandList)};
  } else {
    push @commandArray, $commandList;
  }
  
  printRange("[INFO] This command group after decompression is : ", @commandArray);
  
  if (@commandArray > $runPeriodInOneTime){
    say "[ERROR] RunPeriodInOneTime $runPeriodInOneTime is not bigger than command list " . @commandArray;
    return ();
  }
  
  return @commandArray;
}

sub killChildThread{
  say "Enter killChildThread";
  kill 9,@nodeSubActionThreadArray;
  # We do not find a good way to kill, just eixt and java jar process exit automatically. 
  my $c = "taskkill /im javaSp.exe /t /f";
  say "[INFO] " . `$c`;
  #exit(0);
  say "[INFO] Eixt killChildThread";
}

sub printRange{
  my $prefix = shift;
  print $prefix;
  my $isPrefix = 0;
  for my $i (@_){
    if (0 == $isPrefix){
      print " $i";
      $isPrefix = 1;
    } else {
      print ", $i"
    }
  }
  # a blank line to separate from other information
  say "";
}

main();