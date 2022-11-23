#/usr/bin/perl

use 5.14.0;
use strict;
use warnings;
use File::Find;
use Archive::Tar;
use File::Spec;
use File::Copy;
use Fcntl qw(O_CREAT O_EXCL O_WRONLY);
use Cwd qw(cwd);
use English qw'-no_match_vars';
use File::Path qw(make_path);

my $nodeFolder = "";
if (@ARGV > 0) {
  $nodeFolder = $ARGV[0];
  say "Enter folder $nodeFolder";
}
my $folderName = File::Spec->catfile("$nodeFolder", "command");
my $fileName = File::Spec->catfile($folderName, "command");
my $commandRepeatTimes = 100;

sub main {
  my $dataString = localtime();
  print $dataString . "\n";

  my $r = 0;
  my $errorOutputCount = 0;
  my $maxErrorOutputWaitCount = 20;
  my $timeOutputCount = 0;
  my $maxTimeOutputWaitCount = 20;
  while (-1 != $r) {
    if (-f $fileName) {
      if (0 == $errorOutputCount % $maxErrorOutputWaitCount) {
        say "$fileName exists, waiting";
      }
      ++$errorOutputCount;

      sleep(1);
      next;
    }

    $r = sendCommand($fileName);
    if (0 == $timeOutputCount % $maxTimeOutputWaitCount){
      $dataString = localtime();
      print "Run $timeOutputCount time(s) at $dataString \n";
    }
    ++$timeOutputCount;
  }

  if (-1 == $r){
    say "$fileName does not exits";
  }
}

sub sendCommand {
  my $tempFileName = File::Spec->catfile($folderName, "tmp");
  my $fileName = shift;
  my $file_handle;
  if (!open($file_handle, ">", $tempFileName)) {
    say "[ERROR] cannot open file $tempFileName";
    return -1;
  }

  my $cmd = "sendcomct 10081 notary_second_tx 789004 789004 0";
  for (my $i = 0; $i < $commandRepeatTimes; ++$i) {
    print $file_handle  $cmd . "\n";
  }

  if (!close($file_handle)) {
    say "[WARN] Couldn't close the file $tempFileName \n";
  }

  copy($tempFileName, $fileName);
}

main();
