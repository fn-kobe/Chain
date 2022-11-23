#! /usr/bin/perl 
use strict;
use warnings;

use Cwd qw(cwd);

# We have only support 2 group. 
# If more group, to use "generatedMultiple.pl"
my $totalDirNumber = 6;
my $oneGroupNumber =3;
my $startPort = 9601;
my $group1StartPort = $startPort;
my $group2StartPort = $startPort + $oneGroupNumber;
my $ip = "127.0.0.1";
my $zero_count = 5;
my $ex_zero_count = 5;
my $port = 0;
my $dirName;
my $command;
my $baseDir = cwd;
printf "Base dir is $baseDir \n";
for (my $i = 0; $i < $totalDirNumber; ++$i)
{
  $dirName = $i + 1;
  chdir $baseDir;
  
  
  if (!( -e $dirName )){
    $command = "mkdir $dirName";
    `$command`;
  }
  chdir $dirName;
  
  my $currentDir = cwd;
  printf "New dir is $currentDir \n";

  my $fileName = "ExternalMiningConfiguration";
  my $file_handle;
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  print $file_handle  "[difficulty]\n";
  print $file_handle  "zero_count=$ex_zero_count\n";
  close($file_handle) or die "Couldn't close the file $fileName \n";

  my $bcId = 111;
  my $nounce = -1108112160;
  if ($i >= $oneGroupNumber) 
  {
    $group1StartPort = $startPort + $oneGroupNumber;
    $group2StartPort = $startPort;
    $nounce = 1289986143;
    $bcId=222;
  }

  $fileName = "peerAddressConfiguration";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  my $fileCreateSymbol = ">";
  for (my $j= 0; $j < $oneGroupNumber; ++$j){
    $port = $group1StartPort + $j;
    print $file_handle  "$ip:$port\n";
  }
  close($file_handle) or die "Couldn't close the file $fileName \n";


  $fileName = "externalPeerAddressConfiguration";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  $fileCreateSymbol = ">";
  for (my $j= 0; $j < $oneGroupNumber; ++$j){
    $port = $group2StartPort + $j;
    print $file_handle  "$ip:$port\n";
    $fileCreateSymbol = ">>";
  }
  close($file_handle) or die "Couldn't close the file $fileName \n";


  $fileName = "MiningConfiguration";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  
  print $file_handle  "[difficulty]\n";
  print $file_handle  "zero_count=$zero_count\n";
  print $file_handle  "[blockchain]\n";
  print $file_handle  "genesis_nouce=$nounce\n";
  print $file_handle  "blockchainId=$bcId\n";
  print $file_handle  "blockchain_sync_interval=1000\n";
  print $file_handle  "[mining]\n";
  my $minerId = $i + 1;
  print $file_handle  "account=$bcId?45678_0$minerId\n";
  print $file_handle  "[network]\n";
  my $self_port = $startPort + $i;
  print $file_handle  "self_address_port=$ip:$self_port\n";
  close($file_handle) or die "Couldn't close the file $fileName \n";
}

