#!/usr/bin/perl

use 5.14.0;
use strict;
use warnings;
use Math::Random qw(random_uniform_integer);

my $lowestAttack = 4096;
my @times = (4);
my @knownFactors = (2, 4, 8, 16, 32, -1);#1/2, 1/4, 1/8, and so on, -1 means attack weaker system with 1 possibility

my $testCount = 32;
my $pattackMinFactor = 0.8;
my $pattackMaxFactor = 2 - $pattackMinFactor;

sub oneAttack{
  my ($u1, $u2, $knownFactor) = @_;
  
  my $u1Loop = 0;
  my $u2Loop = 0;
  while ($u1Loop < $u1 && $u2Loop < $u2){
    my $choose = -1; # to allow the pchoose is 1
    if (-1 != $knownFactor){
      $choose = random_uniform_integer(1, 1, $knownFactor);
    }
    if (1 == $choose){# generate 1/n
      $u2Loop++;
    } else{
      $u1Loop++;
    }
    
  }
  say "[DEBUG] Test result ( $u1Loop $u2Loop ) and total loop " . ($u1Loop + $u2Loop);
  return $u1Loop + $u2Loop;
}

sub oneCombination{
  my ($u1, $u2, $knownFactor) = @_;
  say "[INFO] Begin the test combination for $u1 $u2";
  for (my $count = 0; $count < $testCount; ++$count){
    # generate around the average of $u1 and $u2
    my $randomU1 = random_uniform_integer(1, $u1 * $pattackMinFactor, $u1 * $pattackMaxFactor);
    my $randomU2 = random_uniform_integer(1, $u2 * $pattackMinFactor, $u2 * $pattackMaxFactor);
    
    #say "[INFO] The real attmep for $randomU1 $randomU2";
    my $r = oneAttack($randomU1, $randomU2, $knownFactor); # current we only test one factor
  }
  say "[INFO] End the test combination for $u1 $u2";
}

sub allCombination{
  foreach my $time (@times){
    say "[INFO] Begin to test stronger system with pattack time $time";
    foreach my $knownFactor (@knownFactors){
      say "[INFO] Begin to test with known factor $knownFactor";
      oneCombination($lowestAttack, $lowestAttack * $time, $knownFactor);
    }
  }
}

allCombination();