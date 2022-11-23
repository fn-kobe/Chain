#! /usr/bin/perl 
use strict;
use warnings;
use Cwd qw(cwd);


sub main(){
  my $baseDir = cwd;
  my $oneBCNodeNumber = 16;
  my @peerList1;
  my @peerList2;
  my @peerList3;
  for (my $i = 0; $i < $oneBCNodeNumber; ++$i){
    push(@peerList1, "127.0.0.1:" . (9100+$i));
    push(@peerList2, "127.0.0.1:" . (9200+$i));
    push(@peerList3, "127.0.0.1:" . (9300+$i));
  }

  # $bcId $nounce $zero_count $ex_zero_count $ip_port $dirName $minerId @peerList @externalPeerList
  # $zero_count，$ex_zero_count 是什么意思？
  my $bcId = "111", my $nounce = "-1108112160", my $zero_count = "5", my $ex_zero_count = "5";
  my @peerList                = @peerList1;
  my @externalChainConsumer   = @peerList2;
  my @externalChainProducer   = @peerList3;
  # we will put all bc node in one plat folder
  # and then need a global folder naming system starting from bc 1 (will be increased 1 first)
  my $globalNodeFolderNumber = 0;
  for (my $i = 0; $i < @peerList; ++$i) {
    my $interBCNodeNumber = "0" . ($i + 1);# the node number counts from 1 to infinite with '0' as prefix
    $globalNodeFolderNumber += 1;
    processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
        , \@peerList, \@externalChainConsumer, \@externalChainProducer);
  }

  $bcId = "222", $nounce = "1289986143", $zero_count = "5", $ex_zero_count = "5";
  @peerList                = @peerList2;
  @externalChainConsumer   = @peerList3;
  @externalChainProducer   = @peerList1;
  for (my $i = 0; $i < @peerList; ++$i) {
    my $interBCNodeNumber = "0" . ($i + 1);# the node number counts from 1 to infinite with '0' as prefix
    $globalNodeFolderNumber += 1;
    processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
        , \@peerList, \@externalChainConsumer, \@externalChainProducer);
  }

  $bcId = "333", $nounce = "1417598154", $zero_count = "5", $ex_zero_count = "5";
  @peerList                = @peerList3;
  @externalChainConsumer   = @peerList1;
  @externalChainProducer   = @peerList2;
  for (my $i = 0; $i < @peerList; ++$i) {
    my $interBCNodeNumber = "0" . ($i + 1);# the node number counts from 1 to infinite with '0' as prefix
    $globalNodeFolderNumber += 1;
    processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
        , \@peerList, \@externalChainConsumer, \@externalChainProducer);
  }
}

sub creatFolderIfNoeExist{
  my ($folderName) = @_;
  #  -e 文件或目录名存在
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
  #以下代码我们使用 open 函数以只写的方式 > 打开文件 file.txt：
  #> 表示写入方式
  # open(DATA, ">file.txt");
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