#! /usr/bin/perl 
use strict;
use warnings;

my $command = "rd /s /q transaction topBlockHash toBeDiscoverAsset toBePublishAsset blocks Blockchain_dump ex_blocks ex_topBlockHash ex_transaction DiagramDump";
print $command . "\n";
my $command_output = `$command`;
print $command_output . "\n";

$command = "del /s /q *.log";
print $command . "\n";
$command_output = `$command`;
print $command_output . "\n";

$command = "dir";
print $command . "\n";
$command_output =  `$command`; 
 print $command_output . "\n";