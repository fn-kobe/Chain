#! /usr/bin/perl 
use strict;
use warnings;

use File::Spec;
use File::Copy;

my $name="blockchain.jar";
# change this if want to create more node. No performance thinking here
my $maxSupportNodeNumber =100;

if (@ARGV > 1 && $ARGV[0] ne ""){
  $name = $ARGV[0];
}

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

    copy($source, $target) or die "Copy failed: $!";;
  }

}

#copy /Y  .\blockchain.jar  ..\2
#copy /Y  .\blockchain.jar  ..\3
#copy /Y  .\blockchain.jar  ..\4
#copy /Y  .\blockchain.jar  ..\5
