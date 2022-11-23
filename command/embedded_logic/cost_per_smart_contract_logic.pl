#!/usr/bin/perl
use 5.14.0;
use strict;
use warnings;

sub main{
  my $testRound = 30;
  my $eachValue = 0;
  my $totalValue = 0;
  for (my $i = 0; $i < $testRound; ++$i){
    $eachValue = int(rand(1000)) + 10;#length from 10 to 110
    $totalValue += $eachValue;
    say "Value in this turn " . $eachValue . " , and total value is " . $totalValue;
  }
}

main();
