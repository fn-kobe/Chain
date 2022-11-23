#!/usr/bin/perl

use 5.14.0;
use strict;
use warnings;
use Math::Random qw(random_uniform_integer);

my $testRound = 8 * 60;# 8 hours
my $sleepInterval = 60;# one minute

sub outputNetWork{
  my $testNumber = shift;
  if (0 == @_){
    say "[ERROR] No process name is specified";
    exit(0);
  }
  
  my $topResultString = `sudo nethogs -b -c 1`;
  my @topResult = split(/\n/, $topResultString);
  for my $process (@_){
    my @processInfo = grep /$process/, @topResult;
    my $content = "$testNumber: \n";
    for my $eachLine (@processInfo){
      $content .= $eachLine . "\n";
    }
    writeToFile(getNetName($process), $content)
  }
}

sub getNetName{
  return shift . "Net";
}

sub outputCPU{
  my $testNumber = shift;
  if (0 == @_){
    say "[ERROR] No process name is specified";
    exit(0);
  }
  
  my $topResultString = `top -b -n 1`;
  my @topResult = split(/\n/, $topResultString);
  for my $process (@_){
    my @processInfo = grep /$process/, @topResult;
    my $content = "$testNumber: \n";
    for my $eachLine (@processInfo){
      $content .= $eachLine . "\n";
    }
    writeToFile($process, $content)
  }
}

sub writeToFile{
  my $fileName = shift;
  my $content = shift;
  
  open FH, ">>$fileName" or die "Can not open file $fileName, $!";
  print FH $content;
  close FH;
}

sub emptyFile{
  my $fileName = shift;
  unlink $fileName;
}

sub main{
  #prepare
  
  for my $f(@_){
    say "[INFO] Begin to empty file $f";
    emptyFile($f);
    
    say "[INFO] Begin to empty file " . getNetName($f);
    emptyFile(getNetName($f));
  }
  
  #test
  for (my $i = 0; $i < $testRound; ++$i){
    say "[INFO] Begin to test $i round";
    outputCPU($i, @_);
    outputNetWork($i, @_);
    say "[INFO] sleep $sleepInterval seconds for next test round";
    sleep($sleepInterval);
  }
}

main(@ARGV);