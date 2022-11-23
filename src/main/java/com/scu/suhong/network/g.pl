#!/usr/bin/perl
use 5.14.0;
use strict;
use warnings;

my $char = 'A';
my $r = "";
for (my $i = 0; $i < 3; ++$i){
  $r = "all!" . chr($char) . "2+";
  $char += 3;
  $r .= "all!" . chr($char) . "2+";
  $char += 3;
  $r .= "all!" . chr($char) . "2";
  say $r;
  $char += 3;
}


