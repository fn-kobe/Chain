#! /usr/bin/perl 
use strict;
use warnings;
use Cwd qw(cwd);


sub main(){
  my $baseDir = cwd;
  my @peerList1   = qw(127.0.0.1:9601 127.0.0.1:9602 127.0.0.1:9603);
  my @peerList2   = qw(127.0.0.1:9604 127.0.0.1:9605 127.0.0.1:9606);
  # $bcId $nounce $zero_count $ex_zero_count $ip_port $dirName $minerId @peerList @externalPeerList
  my $bcId = "111", my $nounce = "-1108112160", my $zero_count = "5", my $ex_zero_count = "5";
  my @peerList                = @peerList1;
  my @externalChainConsumer   = @peerList2;
  my @externalChainProducer   = @peerList2;
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9601", "1", "01", \@peerList, \@externalChainConsumer, \@externalChainProducer);
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9602", "2", "02", \@peerList, \@externalChainConsumer, \@externalChainProducer);
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9603", "3", "03", \@peerList, \@externalChainConsumer, \@externalChainProducer);
  
  $bcId = "222", $nounce = "1289986143", $zero_count = "5", $ex_zero_count = "5";
  @peerList                = @peerList2;
  @externalChainConsumer   = @peerList1;
  @externalChainProducer   = @peerList1;
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9604", "4", "04", \@peerList, \@externalChainConsumer, \@externalChainProducer);
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9605", "5", "05", \@peerList, \@externalChainConsumer, \@externalChainProducer);
  processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, "127.0.0.1:9606", "6", "06", \@peerList, \@externalChainConsumer, \@externalChainProducer);
}

sub creatFolderIfNoeExist{
  my ($folderName) = @_;
  if (!( -e $folderName )){
      my $command = "mkdir $folderName";
      `$command`;
    }
}

sub processOneBlockchain{
  (my $baseDir, my $bcId, my $nounce, my $zero_count, my $ex_zero_count, my $ip_port, my $dirName,
      my $minerId, my $peerList, my $externalChainConsumer, my $externalChainProducer) = @_;
  
  printf "Base dir is $baseDir\n";
  chdir $baseDir;
  creatFolderIfNoeExist($bcId);
  # Change to network folder
  chdir $bcId;
  creatFolderIfNoeExist($dirName);
  # Change to node folder
  chdir $dirName;
  my $currentDir = cwd;
  printf "Current node dir is $currentDir\n";

  my $fileName = "ExternalMiningConfiguration";
  my $file_handle;
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  print $file_handle  "[difficulty]\n";
  print $file_handle  "zero_count=$ex_zero_count\n";
  close($file_handle) or die "Couldn't close the file $fileName \n";

  $fileName = "peerAddressConfiguration";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  foreach my $peer (@{$peerList}){
    print $file_handle  "$peer\n";
  }
  close($file_handle) or die "Couldn't close the file $fileName \n";

  $fileName = "externalChainConsumer";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  foreach my $exPeer (@{$externalChainConsumer}){
    print $file_handle  "$exPeer\n";
  }
  close($file_handle) or die "Couldn't close the file $fileName \n";

  $fileName = "externalChainProducer";
  open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
  foreach my $exPeer (@{$externalChainProducer}){
    print $file_handle  "$exPeer\n";
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
  print $file_handle  "account=$bcId?45678_$minerId\n";
  print $file_handle  "[network]\n";
  print $file_handle  "self_address_port=$ip_port\n";
  close($file_handle) or die "Couldn't close the file $fileName \n";
}

main();