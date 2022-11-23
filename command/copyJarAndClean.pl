#! /usr/bin/perl
use 5.14.0;
use strict;
use warnings;

use File::Spec;
use File::Copy;

# change this if want to create more node. No performance thinking here
my $maxSupportNodeNumber = 100;

my $name = "blockchain.jar";
doCopy($name);

my $smartContractFolder = "smartContract";
doCopy($name, $smartContractFolder);

$name = "clean.pl";
doCopy($name);

#*.pub is to package public key for hash-locking test
$name = "*.pub";
doCopy($name);

sub doCopy {
  my $name = shift;
  my $targetSubFolder = shift;

  for (my $i = 1; $i <= $maxSupportNodeNumber; ++$i) {
    # Encounter non-existing folder, return
    if (!(-d $i)) {
      last;
    }

    my $source = File::Spec->catfile(".", "$name");
    my $target = File::Spec->catfile(".", "$i");
    if ($targetSubFolder) {
      $target = File::Spec->catfile($target, $targetSubFolder);
    }

    `cp $source $target`;
  }
}
