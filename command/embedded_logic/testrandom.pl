#!/usr/bin/perl
use 5.14.0;
use strict;
use warnings; 


my $totalTime = 10000;
my $sum = 0;
for (my $i= 0; $i < $totalTime; ++$i){
  $sum += int(rand(100))
}

print("Total time is: " . ($sum/$totalTime))