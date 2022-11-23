#! /usr/bin/perl 
use strict;
use warnings;

my $name="blockchain.jar";
for (my $i = 2; $i <= 6; ++$i){
  my $command = "copy /Y $name ..\\$i";
  print $command . "\n";
  `$command`;
}

$name="clean.pl";
for (my $i = 2; $i <= 6; ++$i){
  my $command = "copy /Y $name ..\\$i";
  print $command . "\n";
  `$command`;
}

$name="setacp.txt";
for (my $i = 2; $i <= 6; ++$i){
  my $command = "copy /Y $name ..\\$i";
  print $command . "\n";
  `$command`;
}

#copy /Y  .\blockchain.jar  ..\2
#copy /Y  .\blockchain.jar  ..\3
#copy /Y  .\blockchain.jar  ..\4
#copy /Y  .\blockchain.jar  ..\5
