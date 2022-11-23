#! /usr/bin/perl
use 5.14.0;
use strict;
use warnings;
use Cwd qw(cwd);
use generatedCommon;

if (@ARGV < 2) {
  say "No enough parameters. Usage: <cmd> blockchainNumber nodeNumberInOneBC <workingFolder> <user> <pass>";
  exit 0;
}
# 支持最大的区块链的数量
my $maxSupportNetWorkNumber = 7; # if more needed, please add more zero count here
# Notice, the last one only support 6 zero counts
my @nounceArray = qw(1289986143 1417598154 1149023650 -210709181 -1740289998 -1108112160 -1622010549);
# $zero_count，挖矿的难度
my $zero_count = "6";
# 其他链的难度
my $ex_zero_count = "6";

my @server = ("127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1");
my @serverConsensus = ("PoW", "PoW", "PoS", "PoW", "PoW", "PoW", "PoW");
my @serverDifficult = (6,6,6,6,6,6,6);

# $blockchainNumber 区块链的个数   3
my $blockchainNumber = $ARGV[0];

#  $nodeNumberInOneBC 一个链上的矿工数   3
my $nodeNumberInOneBC = $ARGV[1];
if ($blockchainNumber > $maxSupportNetWorkNumber) {
  say "Only $maxSupportNetWorkNumber networks are supported";
  exit 0;
}

if ($blockchainNumber < 0 || $nodeNumberInOneBC < 0) {
  say "blockchainNumber $blockchainNumber or nodeNumberInOneBC $nodeNumberInOneBC is error";
  exit 0;
}


#  $workingFolder 工作目录
my $workingFolder = "/home/paganini/bc/";

# $user, $pass 账户和密码？
my ($user, $pass) = ("paganini", "paganini");

#如果传进来的参数大于2个  第3个就是工作目录
if (@ARGV > 2) {
  $workingFolder = $ARGV[2];
}
#如果传进来的参数大于3个  第4个就是用户
if (@ARGV > 3) {
  $user = $ARGV[3];
}

#如果传进来的参数大于4个  第5个就是用户密码
if (@ARGV > 4) {
  $pass = $ARGV[4];
}


sub main() {
  # $baseDir 为当前的目录
  my $baseDir = cwd;
  #矿工的邻居节点的地址信息
  my @peerListArray;
  # support for same blockchain on one server,
  # %serverUsedtimeMap 用来干嘛？怎么会统计次数？
  my %serverUsedtimeMap;
  my $serverUsedTimes = 0; # 虚拟机用了3次 ，统计次数
  for (my $j = 0; $j < $blockchainNumber; ++$j) {
    if (exists($serverUsedtimeMap{$server[$j]})){
      $serverUsedTimes = $serverUsedtimeMap{$server[$j]};
      ++$serverUsedTimes;
    }
    $serverUsedtimeMap{$server[$j]} = $serverUsedTimes;
    # @peerList 其他矿工的地址
    my @peerList = ();
    for (my $i = 0; $i < $nodeNumberInOneBC; ++$i) {
      # port starts from 1 (($i + 1)) and each blockchain can occupy 100 ports (($j + 1) * 100). From 9000
      # 将 $server[$j] . ":" . (9000 + 100 * $serverUsedTimes + ($i + 1)) push 到  数组 @peerList
      # ip  为 server的 IP   : 端口号 9000 + 100 * $serverUsedTimes + ($i + 1)
      push(@peerList, $server[$j] . ":" . (9000 + 100 * $serverUsedTimes + ($i + 1)));
    }
    # @peerListArray 里面全是@peerList数组的地址
    push(@peerListArray, \@peerList);
  }

  # we will put all bc node in one plat folder and
  # then need a global folder naming system starting from bc 1 (will be increased 1 first)
  my $globalNodeFolderNumber = 0;
  for (my $j = 0; $j < $blockchainNumber; ++$j) {
    my $nodeNumber = $j + 1;
    # bcid 为111， 222， 333，
    my $bcId = "$nodeNumber$nodeNumber$nodeNumber";
    my $nounce = $nounceArray[$j];
    my @peerList = @{$peerListArray[$j]};

    # @externalChainConsumer Consumer 需要获取我们的信息的其他链
    my @externalChainConsumer = ();
    # Producer 我们需要获取其他链上的信息
    my @externalChainProducer = ();
    if ($blockchainNumber > 1) {

      my $consumerNumber = $j + 1; #  例：如果区块链的数是大于1的那么，顾客链是来自于标号为2的这条区块链
      #$consumerNumber 如果大于了区块链条数
      while ($consumerNumber >= $blockchainNumber) {
        # $consumerNumber 顾客链对区块链总数取余，再赋值给顾客标记链；保证每次执行都会指向下一条区块链
        $consumerNumber %= $blockchainNumber; # from the beginning
      }
      @externalChainConsumer = @{$peerListArray[$consumerNumber]};


      # $producerNumber  生产者标记链(别人需要我们的相关信息),生产者链为本链的前一个链
      my $producerNumber = $j - 1;
      while ($producerNumber < 0) {

        $producerNumber += $blockchainNumber; # from the end
      }
      my @peersArray = @{$peerListArray[$producerNumber]};
      my %oneProducer = (producer=>"producer1",# only one producer for one to one connected topology
        chainId=>"chainId = " . getChainId($producerNumber),
        consensus=>"consensus = " . $serverConsensus[$j],
        difficult=>"difficult = " . $serverDifficult[$j],
        peers => "peers = @peersArray"
      );
      push(@externalChainProducer, \%oneProducer);# only one producer for one to one connected topology
    }

    # 创建所有的相关的文件
    for (my $i = 0; $i < @peerList; ++$i) {
      my $interBCNodeNumber = "0" . ($i + 1); # the node number counts from 1 to infinite with '0' as prefix
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