#! /usr/bin/perl 
use strict;
use warnings;

my $name="blockchain.jar";

if (@ARGV > 1 && $ARGV[0] ne ""){
  $name = $ARGV[0];
}

for (my $i = 2; $i <= 6; ++$i){
  my $command = "copy /Y $name ..\\$i";
  print $command . "\n";
  `$command`;
}

#copy /Y  .\blockchain.jar  ..\2
#copy /Y  .\blockchain.jar  ..\3
#copy /Y  .\blockchain.jar  ..\4
#copy /Y  .\blockchain.jar  ..\5
