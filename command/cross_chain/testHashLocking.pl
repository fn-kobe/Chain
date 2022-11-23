#/usr/bin/perl

# This simulates the hash-lock testing, as they are paralled, they nodes should be placed one by one.
# The last one (n), is the precondition for (n-1)
# To check whether files in remote node under specific folder contain target condition string
# This is used mostly to check key word in blockchain dump file.
# And then folder in remote is the blockchain dump folder.
#         nodeip;command;condition
# <cmd> ip_0;command;ipList;condition_0 .. ip_n;command;ipList;condition_n 
#       ipLIst means check condition on several nodes
#       If condition begins with 'log' then it will check in log else check in blockchain dump
# if condition_i in ip_i matches, then send conditional transaction in ip_i-1 
#
# Notice: symbol of '-' will be reaplaced to space in command and condition
#         ipList supports 1-3 or 1:2:3 formats
#
# Scenario: Between two blockchains
#     1) Send asset forzen transaction in parallel. We adopt no condition to simunlate the  
#           bc1 send -> bc2 send
#             21;send hash-locking setting;;     11;send hash-locking setting;;;
#
#     2) Send second commit transaction serially
#               #wait done then node exectue node           #send 2nd tx
#             21;send hask-locking data;11;<have hask-locking data tx>       11;send hask-locking data;<all bc1 and bc2 nodes>;<has hash locking setting tx bc>
#
#   For test:
#     mqZfCHWcuoEBFqrk41R3MY8eYEm/4ZSWcHinPK7b41cggjtIoDXO/AjWz7BDvFPOUhxpukfK0iuspDmFQisk3w== is hashLockingKeyword by RAS_512a.pri
#
#   Then, run as
#       <cmd> ;;<all nodes bc1 and bc2>;<have 2nd tx> 21:11;send 2nd tx;<all bc1 and bc2 nodes>;<has 1nd tx in bc>    21;send 1st tx;<all bc1 node>;<has 1st tx in bc>  11;send 1st tx;;;
#

use 5.14.0;
use strict;
use warnings;
use File::Find;
use Archive::Tar;
use Cwd qw(cwd);

sub checkParameter{
  if (@ARGV < 4){
      say "[INFO][INFO] Missing parameters. <cmd> allNodeNumber runTimes oneTestTimeoutValue ipList;command;ipList:condition ... ipList;command;ipList:condition";
    exit;
  }
}

my $maxCommmandFileWaitingRetries       = 10;
my $commmandFileWaitingTime             = 1;
my $maxCheckingForOneNodeRetries        = 200;# it may be several mining period to wait
my $checkForOneNodeWaitingTime          = 1; # 10
my @nodeSubActionThreadArray;
my $testRound = 0 ;
my $runTimes = 0;
my $allNodeNumber = 0;
my $oneTestTimeoutValue = 0;
my $checkInterval = 5;#in seconds

$SIG{'INT'} = 'int_handler';
sub int_handler{
  say "[Main][WARN] Get INT signal, try to kill all BC processes";
  killChildThread();
  exit 0;
}

$SIG{ALRM}= sub{
  say "[Main][WARN] Timeout try to kill all BC processes";
  killChildThread();
  say "[Main][INFO] backup test data";
  backUpFolder();
  
  # continue to run next round if not in max test round
  ++$testRound;
  if ($testRound >=$runTimes){
    say "[Main][WARN] Max test time reach. Try to exit";
    exit(0);
  }
  say "[Main][INFO] Continue to test as test round " . $testRound . " does not reach the max time " . $runTimes;
  main();
 };

# return 
#   0 success
#   -1 failure
sub processOneCommand{
  say "[INFO]Enter processOneCommand";
  my $command = shift;
  say "[INFO]Processing:\n\t$command";
  # nodeipList;command;checkingNodeip:condition
  my ($returnCode, $nodeipList, $commandToExecute, $checkingNodeList, $condition) = getParameter($command);
  if (-1 == $returnCode){
    return -1;
  }
  
  my $r = 0;
  # we should have condition to check; if no, skip checking
  for (my $i = 0; $condition && $i < $maxCheckingForOneNodeRetries; ++$i){
    if (-1 == checkOnNode($checkingNodeList, $condition)){
      $r = -1;
      if (0 == $i % 10){
        say "[INFO]Sleep $checkForOneNodeWaitingTime to process";
      }
      sleep($checkForOneNodeWaitingTime);
    } else {
      $r = 0;
      last;
    }
  }

  if (-1 == $r){
    say "Condition is not matched. Skip to process";
    return -1;
  }
    
  # when wait done, there is no host and we just return OK
  if (!@{$nodeipList} || 0 == @{$nodeipList}){
    say "No need to execute the command as no ip list";
    return 0;
  }
  
  my $result = -1;
  if (0 == processCommand($nodeipList, $commandToExecute)){
    say "[INFO]Succed to process one command";
    $result = 0;
  } else {
    say "[ERROR]Failed to process one command";
  }
  
  say "[INFO]Leave processOneCommand\n";
  return $result;
}

sub processCommand{
  return processCommandOnLocal(@_);
}

# return: 0 found, -1 not found
sub processCommandOnLocal{
  say "[INFO]Enter processCommandOnLocal";
  my ($nodeipList, $commandToExecute) = @_;
  say "[MAIN][DEBUG] Nodes to tested are '@{$nodeipList}'";
  for my $nodeFolder (@{$nodeipList}){
    my $folder = ".\\$nodeFolder\\command";
    # command is put to command file for blockchain to read
    if (!(-d "$folder")){
      say "Folder $folder does not exist. Try to create it";
      # have tp create twice in perl
      creatFolderIfNotExist($nodeFolder);
      creatFolderIfNotExist($folder);
    }
    if (-1 == waitAndReplaceFile("$folder\\command", $commandToExecute))
    {
      return -1;
    }
  }
  return 0;
}

sub creatFolderIfNotExist{
  my $name = shift;
  if (!(-d $name)){
    if (!mkdir($name)){
      say "[MAIN][ERROR] error to create folder $name $_";
    }
  }
}

sub waitAndReplaceFile{
  my ($fileName, @content) = @_;
  my $waitTime = 0;
  while (-e $fileName){
    say "[INFO][WARN] $fileName exists. Waiting ...";
    sleep($commmandFileWaitingTime);
    ++$waitTime;
    if ($waitTime > $maxCheckingForOneNodeRetries){
      say "[ERROR] Max waiting time for command to be processed";
      last;
      return -1;
    }
  }
  
  return replaceFile($fileName, @content);
}

sub replaceFile{
  my ($fileName, @content) = @_;
  my $file_handle;
  if (!open($file_handle, ">", "$fileName")){
    say "[ERROR] cannot open file $fileName";
    return -1;
  }
  
  for my $line (@content){
    print $file_handle  $line;
  }
  if (!close($file_handle)){
    say "[WARN] Couldn't close the file $fileName \n";
  }
  say "[INFO] succeed to add the content @content to $fileName";
  return 0;
}

# We assume each remote node is mapped to a folder under a root folder
sub checkOnNode{
  my ($checkingNodeList, $condition) = @_;
  for my $n (@{$checkingNodeList}){
    if (-1 == checkOnLocal($n, $condition)){
      return -1;
    }
  }
}

# return: 0 found, -1 not found
sub checkOnLocal{
  say "[INFO]Enter checkOnLocal";
  my ($nodeFolder, $condition) = @_;
  my @files;
  my $folderOnCheckingNode = $nodeFolder;
  if (-1 == index ($condition, "log")){
    $folderOnCheckingNode .= "\\Blockchain_dump";
    @files = glob("$folderOnCheckingNode\\*.dmp");
  } else {# log
    $condition = substr($condition, 3);# remove log prefix
    @files = glob("$nodeFolder\\autoRun.log");
  }
  # block dump file is in dump
  for my $file (@files){
    if (0 == doesFileContain($file, $condition)){
      say "[INFO]Condition '$condition' is FOUND in folder '$folderOnCheckingNode'";
      return 0;
    }
  }
  say "[INFO]Condition '$condition' is NOT found in folder '$folderOnCheckingNode'";
  say "[INFO]Leave checkOnLocal";
  return -1;
}

# return: 0 found, -1 not found
sub doesFileContain{
  my ($fileName, $content) = @_;
 
  if (!open(FH, '<', $fileName)){
    say "[INFO][ERROR] cannot open file $fileName to read";
    return -1;
  }
  
  while(<FH>){
    if (-1 != index($_, $content)){
      say "[INFO]Condition '$content' found in '$fileName'";
      return 0;
    }
  }
   
  close(FH);
  return -1;
}

sub checkOnRemote{
  say "[INFO]Enter checkOnRemote";
  my ($checkingNode, $folderOnCheckingNode, $condition) = @_;
  
  open my $file, '-|', 'ssh -C $checkingNode "find $folderOnCheckingNode -name \*.dmp | xargs grep -l pattern /dev/null | xargs tar c"'
  or die "Can't pipe: $!";

  my $tar = Archive::Tar->new($file);
  foreach my $file ($tar->get_files()) {
      print $file->name, ', ', $file->size, "\n";
  }

  close $file;
  say "[INFO]Leave checkOnRemote";
}

sub getParameter{
  my $command = shift;
  
  my @commandArray = split(/;/, $command);
  if (@commandArray < 2){
    say "[INFO]Command parameter is not correct " + $command;
    return -1;
  }

  my @executingNodeList = getIpList($commandArray[0]);

  my $commandToExecute = $commandArray[1];
    if (!$commandToExecute){
    $commandToExecute = "";
  }
  $commandToExecute =~ s/-/ /g;
  
  my @checkingNodeList = getIpList($commandArray[2]);
  my $condition = $commandArray[3];
  if (!$condition){
    $condition = "";
  }
  $condition =~ s/-/ /g;
  
  say "[INFO]Begin to check on '@executingNodeList' to execute '$commandToExecute' under the condition on node '@checkingNodeList' when blockchain dump contains keyword '$condition'";
  return (0, \@executingNodeList, $commandToExecute, \@checkingNodeList, $condition);
}

sub getIpList{
  my $originalList = shift;
  if (!$originalList){
    return ();
  }
  
  my @result;
  if (-1 != index($originalList, "-")){
    my @originalArray = split(/-/, $originalList);
    if (@originalArray < 2){
      die "$originalList is not correctly format";
    }
    for (my $i = $originalArray[0]; $i <= $originalArray[1]; ++$i){
      push @result, $i;
    }
  } elsif (-1 != index($originalList, ":")){
    @result = split(/:/, $originalList);
  } else {
    push @result, $originalList;
  }
  
  return @result;
}

sub backUpFolder{
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
    # say "[Main][INFO] File backup result " . $r;
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

sub waitInitBlockchain{
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

sub startBlockchain{
  say "[CHILD][INFO] Begin to process in sub thread $$";
  my $id = shift;
  my $folderName = $id;
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

sub startTest{
  my $threadId = 0;
  for (my $i = 0 ; $i < $allNodeNumber; ++$i){
    $threadId = fork();
    if (0 == $threadId){
      startBlockchain($i + 1);
      say "[CHILD][$$][INFO] Finish to start blockchain thread";
      last;# do not fork in child process
    } else{
      say "[Main][INFO] Thread " . $threadId . " has been started";
      push @nodeSubActionThreadArray, $threadId;
    }
  }
  
  if (0 != $threadId){#main thread
    processCommandStep(@_);
  }
}

sub processCommandStep{
  my @commandList = @_;
  my $processItemLength = @commandList;
  waitInitBlockchain();
  my $startEpoc = time;
  say "\n[MAIN][INFO] **** Begin to process test command with $processItemLength commands at epoc $startEpoc";
  for (my $i = $processItemLength; $i > 0; --$i){
    say "Begin to process command at the position of $i";
    say "";# one blank line to separate logs among commands
    if (-1 == processOneCommand($commandList[$i-1])){
      say "[MAIN][ERROR] Error in process one notary command";
      last;
    }
  }
  
  my $endEpoc = time;
  say "*** End at $endEpoc. Total time is " . ($endEpoc - $startEpoc);
  backUpFolder();
  killChildThread();
}

sub oneTest{
  startTest(@_);
}

sub main(){
  $allNodeNumber = shift @ARGV;
  $runTimes = shift @ARGV;
  $oneTestTimeoutValue = shift @ARGV;
  my @commandList = @ARGV;
  
  say "[MAIN] enter main";
  for (; $testRound < $runTimes; ++$testRound)
  {
    say "\n\n ******************* Begin test of the round $testRound at thread $$. Timeout $oneTestTimeoutValue and command list @commandList\n";
    eval{
      say "set timeout value " . $oneTestTimeoutValue;
      alarm $oneTestTimeoutValue;
      oneTest(@commandList);
    };
    say "\n\n ******************* Finish test of the round " . $testRound . " at thread " . $$  . "\n";
    sleep($checkInterval);
  }
  say "[MAIN] eixt main";
}
  
checkParameter();
main();
