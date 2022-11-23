#/usr/bin/perl

use 5.14.0;
use strict;
use warnings;
use File::Find;
use Archive::Tar;
use File::Spec;
use File::Copy;
use Net::SSH2;
use Fcntl qw(O_CREAT O_EXCL O_WRONLY);
use Cwd qw(cwd);
use English qw'-no_match_vars';
use File::Path qw(make_path);

my $keyword = $ARGV[0];
my $filePattern = $ARGV[1];
my $keyStart = $ARGV[2];
my $keyLength = $ARGV[3];
my $valueStart = $ARGV[4];
my $valueLength = $ARGV[5];

my $splitIndicationFlag = "s";

say "[DEBUG] Init command is $keyword $filePattern $keyStart $keyLength";

my $cmd = "grep -nir '" . $keyword . "' " . $filePattern;
say "[DEBUG] grep command is $cmd";
my $result = `$cmd`;
my @resultList = split(/\n/, $result);

my %foundResult = ();

for my $r  (@resultList) {
  my $key = getValue($r, $keyStart, $keyLength);
  if (!exists($foundResult{$key})) {
    my $value = getValue($r, $valueStart, $valueLength);
    say $key . " - " . $value;
    $foundResult{$key} = $value;
  }
}
say "Found result count: " . keys(%foundResult);

sub getValue{
  my ($c, $standard, $length) = @_;
  if (0 == index($standard, $splitIndicationFlag)){
    $standard = substr($standard, length($splitIndicationFlag));#remove
    return getBySplit($c, $standard, $length);
  } else {
    return getByPosition(@_);
  }
}

sub getByPosition{
  my ($c, $start, $length) = @_;
  return substr($c, $start, $length)
}


sub getBySplit{
  my ($c, $splitKey, $number) = @_;
  my @r = split(/$splitKey/, $c);
  return $r[$number];
}




