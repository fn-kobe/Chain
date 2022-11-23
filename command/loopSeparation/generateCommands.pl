#/usr/bin/perl

# To generate the separate command files from a big command list file
# Each separated file only contains a command
#

use 5.14.0;
use strict;
use warnings;

sub replaceFile{
  say "[INFO] Enter replaceFile";
  my ($fileName, $content) = @_;
  my $file_handle;
  if (!open($file_handle, ">", "$fileName")){
    say "[ERROR] cannot open file $fileName";
    return 0;
  }
  
  print $file_handle  $content;
  if (!close($file_handle)){
    say "[INFO] Couldn't close the file $fileName \n";
  }
  say "[INFO] Eixt replaceFile";
  return 1;
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

sub main{
  if (@ARGV < 2){
    say "[INFO] Missing parameters. <NameOfThisCommand> bigFileName startNumber";
    return;
  }
  
  my $bigFileName            = $ARGV[0];
  my $startNumber            = $ARGV[1];
  my @bigFileContent  = readFile($bigFileName); 
  if (!@bigFileContent){
    say "[ERROR] No content in big command file " . $bigFileName . ". Abort generation";
    return 0;
  }
  my $count = $startNumber;
  for my $line (@bigFileContent){
    if (!replaceFile($count, $line)){
      say "[ERROR] Failed to create command file " . $count;
      return 0;
    }
    say "[INFO] $count has been created or replaced";
    ++$count;
  }
  
  return 1;
}

main();