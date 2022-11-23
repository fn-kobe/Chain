#! /usr/bin/perl
use 5.14.0;
use strict;
use warnings;
use Cwd qw(cwd);
use generatedCommon;



if (@ARGV < 3) {
  say "No enough parameters. Usage: <cmd> blockchainNumber nodeNumberInOneBC centerBCIndex <workingFolder> <user> <pass>";
  exit 0;
}

my $maxSupportNetWorkNumber = 7; # if more needed, please add more zero count here
# Notice, the last one only support 6 zero counts
my @nounceArray = qw(1289986143 1417598154 1149023650 -210709181 -1740289998 -1108112160 -1622010549);
my $zero_count = "6";
my $ex_zero_count = "6";

my @server = ("192.168.1.185", "192.168.1.185", "192.168.1.175", "192.168.1.176", "192.168.1.163", "192.168.1.178", "192.168.1.187");
my @serverConsensus = ("PoW", "PoW", "PoS", "PoW", "PoW", "PoW", "PoW");
my @serverDifficult = (6,6,6,6,6,6,6);

my $blockchainNumber = $ARGV[0];
my $nodeNumberInOneBC = $ARGV[1];
if ($blockchainNumber > $maxSupportNetWorkNumber) {
  say "Only $maxSupportNetWorkNumber networks are supported";
  exit 0;
}

if ($blockchainNumber < 0 || $nodeNumberInOneBC < 0) {
  say "blockchainNumber $blockchainNumber or nodeNumberInOneBC $nodeNumberInOneBC is error";
  exit 0;
}

my $centerBCIndex = $ARGV[2];

my $workingFolder = "/home/suhong/bc/";
my ($user, $pass) = ("suhong", "scu123456");
if (@ARGV > 3) {
  $workingFolder = $ARGV[3];
}
if (@ARGV > 4) {
  $user = $ARGV[4];
}
if (@ARGV > 5) {
  $pass = $ARGV[5];
}


sub main() {
  my $baseDir = cwd;
  my @peerListArray;
  # support for same blockchain on one server,
  my %serverUsedtimeMap;
  my $serverUsedTimes = 0;
  for (my $j = 0; $j < $blockchainNumber; ++$j) {
    if (exists($serverUsedtimeMap{$server[$j]})){
      $serverUsedTimes = $serverUsedtimeMap{$server[$j]};
      ++$serverUsedTimes;
    }
    $serverUsedtimeMap{$server[$j]} = $serverUsedTimes;
    my @peerList = ();
    for (my $i = 0; $i < $nodeNumberInOneBC; ++$i) {
      # port starts from 1 (($i + 1)) and each blockchain can occupy 100 ports (($j + 1) * 100). From 9000
      push(@peerList, $server[$j] . ":" . (9000 + 100 * $serverUsedTimes + ($i + 1)));
    }
    push(@peerListArray, \@peerList);
  }

  # we will put all bc node in one plat folder and then need a global folder naming system starting from bc 1 (will be increased 1 first)
  my $globalNodeFolderNumber = 0;
  my @centerBCPeerAsConsumer = ();
  my @centerBCPeerAsProducer = ();
  my @satelliteBCsAsConsumer = ();
  my @satelliteBCsAsProducer = ();
  my $satelliteIndex = 1;
  for (my $j = 0; $j < $blockchainNumber; ++$j) {
    my @peersArray = @{$peerListArray[$j]};
    if ($j != $centerBCIndex){
      my %oneProducer = (producer=>"producer$satelliteIndex",
        chainId=>"chainId = " . getChainId($j),
        consensus=>"consensus = " . $serverConsensus[$j],
        difficult=>"difficult = " . $serverDifficult[$j],
        peers => "peers = @peersArray"
      );
      push(@satelliteBCsAsProducer, \%oneProducer);
      ++$satelliteIndex;
      push(@satelliteBCsAsConsumer, @{$peerListArray[$j]});
    } else {
      my %oneProducer = (producer=>"producer1",
        chainId=>"chainId = " . getChainId($j),
        consensus=>"consensus = " . $serverConsensus[$j],
        difficult=>"difficult = " . $serverDifficult[$j],
        peers => "peers = @peersArray"
      );
      push(@centerBCPeerAsProducer, \%oneProducer);
      push(@centerBCPeerAsConsumer, @{$peerListArray[$j]});
    }
  }

  for (my $j = 0; $j < $blockchainNumber; ++$j) {
    my $bcId = getChainId($j);
    my $nounce = $nounceArray[$j];
    my @peerList = @{$peerListArray[$j]};

    my @externalChainConsumer = ();
    my @externalChainProducer = ();

    for (my $i = 0; $i < @peerList; ++$i) {
      if ($j == $centerBCIndex){
        @externalChainConsumer = @satelliteBCsAsConsumer;
        @externalChainProducer = @satelliteBCsAsProducer;
      }else{
        @externalChainConsumer = @centerBCPeerAsConsumer;
        @externalChainProducer = @centerBCPeerAsProducer;
      }
      my $interBCNodeNumber = $i + 1; # the node number counts from 1 to infinite with '0' as prefix
      $globalNodeFolderNumber += 1;
      my $consensusType = $serverConsensus[$j];
      processOneBlockchain($baseDir, $bcId, $consensusType, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
        , \@peerList, \@externalChainConsumer, \@externalChainProducer, $server[$j], $user, $pass, $workingFolder);
    }
  }
}

sub getChainId{
  my $index = shift;
  my $nodeNumber = $index + 1;
  return "$nodeNumber$nodeNumber$nodeNumber";
}

main();