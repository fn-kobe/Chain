#! /usr/bin/perl 

use 5.14.0;

use strict;
use warnings;

sub processOneTest{
    my ($txNumber, $randomRange, $testRound, $outputAllTxs) = @_;

    my @types = ("civ", "ci_v", "c_i_v"); # txNumber is 0 for civ, 1 for ci_v, and 2 for c_i_v
    my $lifecycleType = $types[$txNumber-1];
    say "[DEBUG] lifecycle type: $lifecycleType, randomRange: $randomRange, at $testRound test round";

    # randomly simulate the disacrd situation
    my @txState;# 0 discard, 1 exists
    for (my $i = 0; $i < $txNumber; ++$i){
        my $randomChoice = int(rand($randomRange));
        # 1/$randomChoice, we only choice 0
        if (0 == $randomChoice){ # discard at 1/n
            @txState[$i] = 0;
        } else{
            @txState[$i] = 1;
        }
    }

    #porcess the results
        #check discarded tx
    my $disacrdTxNumber = 0;
    for (my $i = 0; $i < $txNumber; ++$i){
        if (0 == $txState[$i]){
            ++$disacrdTxNumber;
        }
    }

    if ($outputAllTxs eq "all" || 0 != $disacrdTxNumber){
        say "[MISSING TXS][INFO] Test round $testRound of $lifecycleType type misses  $disacrdTxNumber txs at 1/$randomRange" ;
    }

    my $errorTxNumber = 0;
    for (my $i = 0; $i < $txNumber; ++$i){

        for (my $j = $i + 1; $j < $txNumber; ++$j){
            if (0 == $txState[$i] && 1 == $txState[$j]){ # previous tx does not exist and post tx exist
                ++$errorTxNumber;
                say "[ERROR TXS][DEBUG] $i does not exist, while $j eixts"
            }
        }
    }

    if ($outputAllTxs eq "all" || 0 != $errorTxNumber){
        say "[ERROR TXS][INFO] Test round $testRound of $lifecycleType type errors  $errorTxNumber txs at 1/$randomRange";
    }
}

sub main{
    my $testTimes = 1;
    my $startRandom = 2;
    my $randomFactor = 2;
    my $randomTimes = 2;
    my $outputAllTxs = "";

    if (@ARGV > 3){
        $testTimes = $ARGV[0];
        $startRandom = $ARGV[1];
        $randomFactor = $ARGV[2];
        $randomTimes = $ARGV[3];
    } else{
        say "Usage: testTimes, startRandom, randomFactor, randomTimes";
        exit(0);
    }

    if (@ARGV > 4){
        $outputAllTxs = $ARGV[4];
    }

    my $randomRange = $startRandom;
    for (my $j = 0; $j < $randomTimes; ++$j){

        for (my $i = 0; $i < $testTimes; ++$i){
            processOneTest(1, $randomRange, $i, $outputAllTxs);
            processOneTest(2, $randomRange, $i, $outputAllTxs);
            processOneTest(3, $randomRange, $i, $outputAllTxs);
        }

        $randomRange *= $randomFactor;
    }
}

main();