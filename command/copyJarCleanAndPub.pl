#! /usr/bin/perl 
use strict;
use warnings;

use File::Spec;
use File::Copy;

# change this if want to create more node. No performance thinking here
my $maxSupportNodeNumber =100;

my $name="blockchain.jar";
doCopy($name);

$name="clean.pl";
doCopy($name);

$name="cleanAndStart.pl";
doCopy($name);

$name="RSA512a.pub";
doCopy($name);

sub doCopy{
  my $name =shift;

  for (my $i = 1; $i <= $maxSupportNodeNumber; ++$i){
    # Encounter non-existing folder, return
    if (!(-d $i)) {
      last;
    }

    my $source = File::Spec->catfile(".","$name");
    my $target = File::Spec->catfile(".","$i");

    copy($source, $target) or die "doCopy failed: $!";;
  }
}
