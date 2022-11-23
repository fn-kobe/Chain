#/usr/bin/perl

# This simulates the hash-lock testing, as they are paralled, they nodes should be placed one by one.
# The last one (n), is the precondition for (n-1)
# To check whether files in remote node under specific folder contain target condition string
# This is used mostly to check key word in blockchain dump file.
# And then folder in remote is the blockchain dump folder.
#         nodeip;command;condition
# testHaskLocking ip_0;folder:command;ip_1:folder:condition_1 .. ip_n;folder:command;ip_n:folder:condition_n 
# if condition_i in ip_i matches, then send conditional transaction in ip_i-1 
#
# Notice: symbol of '-' will be reaplaced to space

use 5.14.0;
use strict;
use warnings;
use File::Find;
use Archive::Tar;

my $maxCommmandFileWaitingRetries       = 5;
my $commmandFileWaitingTime        = 10;
my $maxCheckingForOneNodeRetries        = 5;
my $checkForOneNodeWaitingTime          = 10;
my @commandList;
my $currentProcessItem = -1;
my $processItemLength = -1;

# return 
#   0 success
#   -1 failure
sub processOneCommand{
  say "[INFO]Enter processOneCommand";
  my $command = shift;
  say "[INFO]Processing:\n\t$command";
  # nodeip;folder:command;ip:condition
  my ($returnCode, $nodeip, $folder, $commandToExecute, $checkingNode, $folderOnCheckingNode, $condition) = getParameter($command);
  if (-1 == $returnCode){
    return -1;
  }
  
  my $result = -1;
  for (my $i = 0; $i < $maxCheckingForOneNodeRetries; ++$i){
    if (-1 == checkOnNode($checkingNode, $folderOnCheckingNode, $condition)){
      say "[INFO]Sleep $checkForOneNodeWaitingTime to process";
      sleep($checkForOneNodeWaitingTime);
    } else {
      $result = 0;
      last;
    }
  }
  
  if (0 == processCommand($nodeip, $folder, $commandToExecute)){
    say "[INFO]Succed to process one command";
    $result = 0;
  } else {
    $result = -1;
    say "[ERROR]Failed to process one command";
  
  }
  
  say "[INFO]Leave processOneCommand\n";
  return $result;
}

sub processCommand{
  my ($nodeip, $folder, $commandToExecute) = @_;
  if ($nodeip eq "self" || $nodeip eq "local"){
      return processCommandOnLocal($folder, $commandToExecute);
  }
  else {
    return processCommandOnRemote($nodeip, $folder, $commandToExecute);
  }
}

# return: 0 found, -1 not found
sub processCommandOnLocal{
  say "[INFO]Enter processCommandOnLocal";
  my ($folder, $commandToExecute) = @_;
  
  # command is put to command file for blockchain to read
  return waitAndReplaceFile("$folder\\command", $commandToExecute);
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

sub processOnRemote{
  say "[INFO]Enter processOnRemote";
  my ($nodeip, $folder, $commandToExecute) = @_;
  
  open my $file, '-|', 'ssh -C $checkingNode "find $folderOnCheckingNode -name \*.dmp | xargs grep -l pattern /dev/null | xargs tar c"'
  or die "Can't pipe: $!";

  my $tar = Archive::Tar->new($file);
  foreach my $file ($tar->get_files()) {
      print $file->name, ', ', $file->size, "\n";
  }

  close $file;
  say "[INFO]Leave processOnRemote";
  return -1;# NOT implemented
}

sub checkOnNode{
  my ($checkingNode, $folderOnCheckingNode, $condition) = @_;
  if ($checkingNode eq "self" || $checkingNode eq "local"){
      checkOnLocal($folderOnCheckingNode, $condition);
  }
  else {
    checkOnRemote($checkingNode, $folderOnCheckingNode, $condition);
  }
}

# return: 0 found, -1 not found
sub checkOnLocal{
  say "[INFO]Enter checkOnLocal";
  my ($folderOnCheckingNode, $condition) = @_;
  
  # block dump file is in dump
  my @files = glob("$folderOnCheckingNode/*.dmp");
  for my $file (@files){
    if (0 == doesFileContain($file, $condition)){
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
    return;
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
  if (@commandArray < 3){
    say "[INFO]Command parameter is not correct";
    return -1;
  }
  
  my $nodeip = $commandArray[0];
  
  my @folderAndCommand = split(/:/, $commandArray[1]);
  if (@folderAndCommand < 2){
    say "[INFO]Executing folder or command is not correct: $commandArray[1]";
    return (-1);
  }
  
  my $folder = $folderAndCommand[0];
  my $commandToExecute = $folderAndCommand[1];
  $commandToExecute =~ s/-/ /g;
  
  my @targetNodeAndCondition = split(/:/, $commandArray[2]);
  if (@targetNodeAndCondition < 3){
    say "[INFO]Target node , folder or condition is not correct: $commandArray[2]";
    return (-1);
  }
  
  my $checkingNode = $targetNodeAndCondition[0];
  my $folderOnCheckingNode = $targetNodeAndCondition[1];
  my $condition = $targetNodeAndCondition[2];
  
  say "[INFO]Begin to check on '$nodeip' at folder '$folder' to execute '$commandToExecute' under the condition on node '$checkingNode' when blockchain dump in folder '$folderOnCheckingNode' contains keyword '$condition'";
  return (0, $nodeip, $folder, $commandToExecute, $checkingNode, $folderOnCheckingNode, $condition);

}

sub main{
  if (@ARGV < 1){
    say "[INFO][INFO] Missing parameters. testHashLocking.pl ip_0;folder:command;ip_1:condition_1 ... ip_n;folder:command;ip_n:condition_n";
    return;
  }
  
  @commandList = @ARGV;
  $processItemLength  = @commandList;
  $currentProcessItem = $processItemLength - 1;
  say "[INFO]Command has $processItemLength nodes to check";
  
  for (my $i = $processItemLength; $i > 0; --$i){
    say "Begin to process command at the position of $i";
    say "";# one blank line to separate logs among commands
    if (-1 == processOneCommand(@commandList[$i-1])){
      return -1;
    }
  }
  
  return 0;
}

main();
