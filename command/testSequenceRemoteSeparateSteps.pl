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
#     1) Send first commit transaction by sequence
#           bc1 send -> bc2 send
#             21;send;<all bc1 node>;<has 1st tx in bc>     11;send;;;
#
#     2) Send second commit transaction in parallel
#               #wait done then node exectue node           #send 2nd tx
#             ;;<all nodes bc1 and bc2>;<have 2nd tx>       21:11;send;<all bc1 and bc2 nodes>;<has 2nd tx in bc>
#   Then, run as
#       <cmd> ;;<all nodes bc1 and bc2>;<have 2nd tx> 21:11;send 2nd tx;<all bc1 and bc2 nodes>;<has 1nd tx in bc>    21;send 1st tx;<all bc1 node>;<has 1st tx in bc>  11;send 1st tx;;;
#

use 5.14.0;
use strict;
use warnings;
use File::Find;
use Archive::Tar;
use File::Spec;
use File::Copy;
use Net::SSH2;
use Fcntl qw(O_CREAT O_EXCL O_WRONLY);
use Cwd qw(cwd);
use English qw'-no_match_vars';
use File::Path qw(make_path);
use testSequenceRemoteCommon;

my $commandFileWaitingTime = 1;
my $maxCheckingForOneNodeRetries = 1200; # it may be several mining period to wait
my $checkForOneNodeWaitingTime = 1;      # 10
my @nodeSubActionThreadArray;
my $testRound = 0;
my $runTimes = 0;
my $allNodeNumber = 0;
my $oneTestTimeoutValue = 0;
my $specificName = "";#used to identify different test case, and backup folder will contains this name
my $checkInterval = 5; #in seconds
my @commandList;
my %nodeInformationMap = ();
my %nodeInformationClassfiedByAddress = ();
my $mainId = $$;
my $isInitStepRequired = 1; # -1 means not done this step
my $iProcessCommandRequired = 1; # -1 means not done this step
my $isTerminationRequired = 1; # -1 means not done this step


$SIG{'INT'} = 'int_handler';
sub int_handler {
  if ($$ != $mainId){
    say "[CHILD] Just exit in child process $$ for INT signal";
    exit 0;
  }
  if (-1 != $isTerminationRequired) {
    say "[Main][WARN] Get INT signal, try to kill all BC processes %nodeInformationClassfiedByAddress";
    killChildThread(\%nodeInformationClassfiedByAddress, \@nodeSubActionThreadArray);
  } else {
    say "[Main][WARN] Skip to kill all BC processes as termination is not required";
  }
  exit 0;
}

$SIG{ALRM} = sub {
  if ($$ != $mainId){
    say "[CHILD] Just exit in child process $$ for ALRM signal";
    exit 0;
  }
  if (-1 != $isTerminationRequired) {
    say "\n[Main][WARN] ****** Timeout and try to kill all BC processes\n";
    killChildThread(\%nodeInformationClassfiedByAddress, \@nodeSubActionThreadArray);
    say "[Main][INFO] backup test data";
    backUpNodeTestData();
  } else {
    say "[Main][WARN] ****** Timeout and skip to kill all BC processes as termination is not required";
  }

  # continue to run next round if not in max test round
  ++$testRound;
  if ($testRound >= $runTimes) {
    say "[Main][WARN] Max test time reach. Try to exit";
    exit(0);
  }
  say "[Main][INFO] Continue to test as test round " . $testRound . " does not reach the max time " . $runTimes;
  main();
};

sub checkParameter {
  if (@ARGV < 5) {
    say "[INFO][INFO] Missing parameters. <cmd> allNodeNumber runTimes oneTestTimeoutValue <steps> testCaseName <ipList;command;ipList:condition ... ipList;command;ipList:condition>";
    exit;
  }
}

sub extractParameters {
	$allNodeNumber = shift @ARGV;
	$runTimes = shift @ARGV;
	$oneTestTimeoutValue = shift @ARGV;
  my $requiredSteps = shift @ARGV;
  processLifecycleParameter($requiredSteps);
  $specificName = shift @ARGV;
  processCommandParameter(@ARGV)
}

sub processLifecycleParameter{
  my $requiredStepsParameter = shift;
  my $lifecycle_keyword = "lfc:";
  my $requiredSteps = '';
  if (0 == index($requiredStepsParameter, $lifecycle_keyword)){
    $requiredSteps = substr($requiredStepsParameter, length($lifecycle_keyword));
    $isInitStepRequired = index($requiredSteps, 'i');
    $iProcessCommandRequired = index($requiredSteps, 'c');
    $isTerminationRequired = index($requiredSteps, 't');
  } else {
    say("[INFO][STEPS] Using defaul lifecycle parameters, as no lifecycle is specified in the parameters " . $requiredStepsParameter);
  }

  #debug
  if (-1 != $isInitStepRequired) {say "[INFO][STEPS]**** Initialization steps are required";}
  if (-1 != $iProcessCommandRequired) {say "[INFO][STEPS]**** Steps to process command are required";}
  if (-1 != $isTerminationRequired) {say "[INFO][STEPS]**** Termination steps are required"}
}

sub processCommandParameter{
  if (-1 == $iProcessCommandRequired){
    say "[DEBUG] Command is not required, skip to parse command parameters";
    return;
  }
  if (0 == @_){
    say "[ERROR] **** No command parameter in command processing mode";
    return;
  }
  foreach my $command(@_){
    push @commandList,split(" ", $command);
  }
}

# return 
#   0 success
#   -1 failure
sub processOneCommand {
  say "[INFO]Enter processOneCommand";
  my $command = shift;
  say "[INFO]Processing:\n\t$command";
  # nodeipList;command;checkingNodeip:condition
  my ($returnCode, $nodeipList, $commandToExecute, $checkingNodeGroupList, $condition) = getParameter($command);
  if (-1 == $returnCode) {
    return -1;
  }

  my $r = 0;
  # we should have condition to check; if no, skip checking
  for (my $i = 0; $condition && $i < $maxCheckingForOneNodeRetries; ++$i) {
    if (-1 == checkAllRemoteNode($checkingNodeGroupList, $condition, \%nodeInformationMap)) {
      $r = -1;
      if (0 == $i % 10) {
        say "[INFO]Sleep $checkForOneNodeWaitingTime to process";
      }
      sleep($checkForOneNodeWaitingTime);
    }
    else {
      $r = 0;
      last;
    }
  }

  if (-1 == $r) {
    say "Condition is not matched. Skip to process";
    return -1;
  }

  # when wait done, there is no host and we just return OK
  if (!@{$nodeipList} || 0 == @{$nodeipList}) {
    say "No need to execute the command as no ip list";
    return 0;
  }

  my $result = -1;
  if (0 == processCommand($nodeipList, $commandToExecute, \%nodeInformationMap, $commandFileWaitingTime, $maxCheckingForOneNodeRetries)) {
    say "[INFO]Succed to process one command";
    $result = 0;
  }
  else {
    say "[ERROR]Failed to process one command";
  }

  say "[INFO]Leave processOneCommand\n";
  return $result;
}

sub processCommand {
  return processBlockchainCommandOnRemote(@_);
}

sub startBlockchain {
  say "[MAIN][INFO] Begin to process in sub thread $$";
  my $id = shift;
  my ($server, $user, $pass, $remoteWorkingFolder) = @{$nodeInformationMap{$id}};
  if (!$server){
    say "Cannot find server information";
    return;
  }

  my $nodeFolder = $id + 1;
  my $folderName = File::Spec->catfile($remoteWorkingFolder, $nodeFolder);
  # 1. clean the older files
  my $command = "cd $folderName && perl clean.pl";
  say "[MAIN][INFO] Clean the environment on node $nodeFolder ($server) in sub thread " . $$ . " by command $command";
  runRemoteCommand($server, $user, $pass, $command);

  # 2. run the command
  $command = "cd $folderName && javaSp -jar blockchain.jar 2>&1 >> autoRun.log";
  my $childId = runRemoteCommandInChildProcess($server, $user, $pass, $command);
  if (0 != $childId) {
    push @nodeSubActionThreadArray, $childId;
    say "Thread $childId has been started to run $command";
  }
}

sub startTest {
  say "[INFO] Init node information";
  for (my $i = 0; $i < $allNodeNumber; ++$i) {
    my ($r, $newNodeInformationMap, $newNodeInformationClassfiedByAddress) = initNodeIp($i, \%nodeInformationMap, \%nodeInformationClassfiedByAddress);
    if (0 == $r){
      %nodeInformationMap = %{$newNodeInformationMap};
      %nodeInformationClassfiedByAddress = %{$newNodeInformationClassfiedByAddress};
    }
  }

  if (-1 != $isInitStepRequired){
    initializationStep();
  }

  #only check in main thread
  if (-1 != $iProcessCommandRequired) {
    processCommandStep(@_);
  }

  if (-1 != $isTerminationRequired){
    processTerminationStep()
  }
}

sub initializationStep{
  my $killPreviousBCInstanceCommand = "killall javaSp";
  say "[INFO] Try to killall previous blockchain process by command $killPreviousBCInstanceCommand";
  my @serverList = values %nodeInformationClassfiedByAddress;
  for my $oneServerInfo (@serverList) {
    my ($server, $user, $pass) = @{$oneServerInfo};
    say "[INFO] Try to kill blockchain process on remote node: $server";
    runRemoteCommandWithoutOutput($server, $user, $pass, $killPreviousBCInstanceCommand);
  }

  say "[INFO] Do action on each node";
  for (my $i = 0; $i < $allNodeNumber; ++$i) {
    #only check in main thread
    if ($$ == $mainId) {
      startBlockchain($i);
      say "[MAIN][$$][INFO] Finish to start blockchain thread";
    }
  }
  if ($$ == $mainId) {
    waitInitBlockchain($allNodeNumber, \%nodeInformationMap, $checkInterval);
  }
}

sub processCommandStep {
  if ($$ != $mainId) {
    return;
  }
	my @commandList = @_;
	my $processItemLength = @commandList;

	my $startEpoc = time;
	say "\n[MAIN][INFO] **** Begin to process test command with $processItemLength commands at epoc $startEpoc";
	my $result = 0;
	for (my $i = $processItemLength; $i > 0; --$i) {
		my $cmdStartEpoc = time;
		say "Begin to process command at the position of $i : " . $commandList[$i - 1] . " at time " . $cmdStartEpoc;
		say ""; # one blank line to separate logs among commands
		if (-1 == processOneCommand($commandList[$i - 1])) {
			say "[MAIN][ERROR] Error in process one notary command";
			$result = -1;
			last;
		}
		my $cmdEndEpoc = time;
		say "End to process command at the position of $i : " . $commandList[$i - 1] . " at time " . $cmdEndEpoc;
		say "\n***This command takes " . ($cmdEndEpoc - $cmdStartEpoc);
	}

  my $endEpoc = time;
  if (0 == $result) {
    say "\n****** End at $endEpoc. Total time is " . ($endEpoc - $startEpoc) . " seconds";
    say ""; # a blank line
  }
  else {
    say "\n****** End at $endEpoc [FAIlED]. Total time is " . ($endEpoc - $startEpoc) . " seconds";
    say ""; # a blank line
  }
}

sub processTerminationStep{
  if ($$ != $mainId) {return;}

  backUpNodeTestData($allNodeNumber, \%nodeInformationMap, $specificName);
  killChildThread(\%nodeInformationClassfiedByAddress, \@nodeSubActionThreadArray);
}

sub oneTest {
  startTest(@_);
}

sub main {
  say "[MAIN] enter main";
  for (; ($testRound < $runTimes); ++$testRound) {
    say "\n\n ******************* Begin test of the round $testRound at thread $$. Timeout $oneTestTimeoutValue and command list @commandList\n";
    eval {
      say "set timeout value " . $oneTestTimeoutValue;
      alarm $oneTestTimeoutValue;
      oneTest(@commandList);
    };
    if ($@) {
      say "[MAIN][ERROR] Exception happens: $@";
    }
    my $testSteps = "";
    $testSteps .= (-1 == $isInitStepRequired) ? "":" initialization";
    $testSteps .= (-1 == $iProcessCommandRequired) ? "":" processing command";
    $testSteps .= (-1 == $isTerminationRequired) ? "":" termination";
    say "\n\n ******************* Finish test of " . $testSteps . " step(s) at test round " . $testRound . " at thread " . $$ . "\n";
    sleep($checkInterval);
  }
  say "[MAIN] eixt main";
}

checkParameter();
extractParameters();
main();

