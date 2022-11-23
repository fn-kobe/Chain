#! /usr/bin/perl 
use strict;
use warnings;
use Cwd qw(cwd);
use generatedCommon;

my @serverList = ("192.168.1.175", "192.168.1.176"); # only use two server
my ($user, $pass, $workingFolder) = ("suhong", "scu123456", "/home/suhong/bc/");

sub main() {
    my $baseDir = cwd;
    my $oneBCNodeNumber = 16;
    my @peerList1;
    my @peerList2;
    for (my $i = 0; $i < $oneBCNodeNumber; ++$i){
        push(@peerList1, $serverList[0] . ":" . (9100+$i));
        push(@peerList2, $serverList[1] . ":" . (9100+$i));
    }
    # $bcId $nounce $zero_count $ex_zero_count $ip_port $dirName $minerId @peerList @externalPeerList
    my $bcId = "111", my $nounce = "-1108112160", my $zero_count = "6", my $ex_zero_count = "6";
    my $server = $serverList[0];
    my @peerList = @peerList1;
    my @externalChainConsumer = @peerList2;
    my @externalChainProducer = @peerList2;
    # we will put all bc node in one plat folder and then need a global folder naming system starting from bc 1 (will be increased 1 first)
    my $globalNodeFolderNumber = 0;
    for (my $i = 0; $i < @peerList; ++$i) {
        my $interBCNodeNumber = "0" . ($i + 1);# the node number counts from 1 to infinite with '0' as prefix
        $globalNodeFolderNumber += 1;
        processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
            , \@peerList, \@externalChainConsumer, \@externalChainProducer, $server, $user, $pass, $workingFolder);
    }

    $bcId = "222", $nounce = "1289986143";# $zero_count = "5", $ex_zero_count = "5";
    $server = $serverList[1];
    @peerList = @peerList2;
    @externalChainConsumer = @peerList1;
    @externalChainProducer = @peerList1;
    for (my $i = 0; $i < @peerList; ++$i) {
        my $interBCNodeNumber = "0" . ($i + 1);# the node number counts from 1 to infinite with '0' as prefix
        $globalNodeFolderNumber += 1;
        processOneBlockchain($baseDir, $bcId, $nounce, $zero_count, $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
            , \@peerList, \@externalChainConsumer, \@externalChainProducer, $server, $user, $pass, $workingFolder);
  }
}

main();