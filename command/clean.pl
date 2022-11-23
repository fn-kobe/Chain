#! /usr/bin/perl 
use 5.14.0;
use strict;
use warnings;

use English qw' -no_match_vars ';

my $folders = "transaction topBlockHash toBeDiscoverAsset toBePublishAsset blocks Blockchain_dump ex_blocks ex_topBlockHash ex_transaction com smartContract";
if (-1 != index($OSNAME, "MSWin32")){
 say "MSWin32 system to clean";
 cleanOnWin($folders);
} elsif (-1 != index($OSNAME, "linux")){
 say "Linux system to clean";
 cleanOnLinux($folders);
} else{
 say "Unkonwn system, no command to clean";
}

sub cleanOnWin{
 my $folders = shift;
 my $command = "";
 $command = "rd /s /q $folders";
 print $command . "\n";
 my $command_output = `$command`;
 print $command_output . "\n";

 $command = "del /s /q *.log *.java";
 print $command . "\n";
 $command_output = `$command`;
 print $command_output . "\n";

 $command = "dir";
 print $command . "\n";
 $command_output =  `$command`;
 print $command_output . "\n";
}

sub cleanOnLinux{
 my $folders = shift;
 my $command = "";

 #remove folder
 $command = "rm -rf $folders *.log *.java";
 print $command . "\n";
 my $command_output = `$command`;
 print $command_output . "\n";

 $command = "ls -l";
 print $command . "\n";
 $command_output =  `$command`;
 print $command_output . "\n";
}
