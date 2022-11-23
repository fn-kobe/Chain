#/usr/bin/perl

# To generate the separate command files from a big command list file
# Each separated file only contains a command
#

use 5.14.0;
use strict;
use warnings;


sub main{
  if (@ARGV < 3){
    say "[INFO] Missing parameters. sourceGroupList totalNumber targetFileStart";
    return;
  }
  
  my $sourceGroupList        = $ARGV[0];
  my $totalNumber            = $ARGV[1];
  my $targetFileStart        = $ARGV[2];
  
  my @sourceGroupArray  = split(/:/, $sourceGroupList);
  my @combinedContent   = ();
  for (my $i = 0; $i < $totalNumber; ++$i){
    @combinedContent = ();
    for my $source (@sourceGroupArray){
      
      push @combinedContent, readFileWithOutLineChnager($source + $i);
    }
    
    replaceFile($targetFileStart + $i, @combinedContent);
  }
  
}

sub replaceFile{
  say "[INFO] Enter replaceFile";
  my ($fileName, @content) = @_;
  my $file_handle;
  if (!open($file_handle, ">", "$fileName")){
    say "[ERROR] cannot open file $fileName";
    return 0;
  }
  
  for my $line (@content){
    print $file_handle  $line . "\n";
  }
  if (!close($file_handle)){
    say "[INFO] Couldn't close the file $fileName \n";
  }
  say "succeed to add content @content to $fileName";
  say "[INFO] Eixt replaceFile";
  return 1;
}

sub readFileWithOutLineChnager{
  my ($fileName) = @_; 
  my @c = readFile($fileName);
  my @nc = ();
  for my $l (@c){
    chomp($l);
    push @nc, $l;
  }
  
  return @nc;
}

sub readFile{
  say "[INFO] Enter readFile";
  
  my ($fileName) = @_; 
  if (!open(FH, '<', $fileName)){
    say "[ERROR] cannot open file $fileName to read";
    return;
  }
  
  my @content;
  while(<FH>){
     push @content, $_;
  }
  close(FH);
  say "[INFO] Eixt readFile";
  
  return @content;
}

main();