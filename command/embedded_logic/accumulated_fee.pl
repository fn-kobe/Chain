#!/usr/bin/perl
use 5.14.0;
use strict;
use warnings;

use MIME::Base64;

sub doOnetest{
  my $randomFileSize = 4200;
  my $basedFee = 2100;
  my $sizeFee = 1.2;
  my $txMustData = '{"pr":"","d":"","pu":"","h":"481F25F6F0D6A91C8E7D779AD20B7672","exchangeId":01,"miningTime":1615360950543,"blockIndex":"7","t":"","gas":0,"from":"111?789001","id":420546731,"to":"111?789002","value":"0","keyValueParameter":"","txIndex":"7"}';
  my $txCode="if code, then return true";
  my $randomDataLength = int(rand($randomFileSize)) + 10;#length from average 211*10 (2110)
  doEmbedtest($randomDataLength, $basedFee, $sizeFee, $txMustData, $txCode);
  
  $randomDataLength = int(rand($randomFileSize)) + 10;#length from average 211*10 (2110)
  doSeparatetest($randomDataLength, $basedFee, $sizeFee, $txMustData, $txCode);
}

sub doEmbedtest{
  my $randomDataLength = shift;
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;
  
  say "[DEBUG][EMBED] random data length is " . $randomDataLength;
  say "[DEBUG][EMBED] Tx must data length is " . length($txMustData);

  my $txData = "";
  for (my $i= 0; $i < $randomDataLength; ++$i){
    $txData .= "a"; # we use a to fill data, other words can also be used
  }

  my $txCodeBase64 = encode_base64($txCode);
  say "[DEBUG][EMBED] tx code length is " . length($txCode) . ", and tx base64 code is " . length($txCodeBase64);

  my $txAllData = $txMustData . $txData . $txCodeBase64;

  my $totalLength = length($txAllData);

 # say "[INFO][EMBED] Total Tx is " . $txAllData;
  say "[INFO][EMBED] Total length is " . $totalLength;

  my $fee = $basedFee + $totalLength * $sizeFee;

  say "[INFO][EMBED] Total fee is " . $fee;
}


sub doSeparatetest{
  my $randomDataLength = shift;
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;
  
  say "[DEBUG][EMBED] random data length is " . $randomDataLength;
  say "[DEBUG][EMBED] Tx must data length is " . length($txMustData);

  my $txData = "";
  for (my $i= 0; $i < $randomDataLength; ++$i){
    $txData .= "a"; # we use a to fill data, other words can also be used
  }

  my $txCodeBase64 = encode_base64($txCode);
  say "[DEBUG][EMBED] tx code length is " . length($txCode) . ", and tx base64 code is " . length($txCodeBase64);

  my $tx1AllData = $txMustData . $txData ;
  
  my $tx2AllData = $txMustData . $txCodeBase64;

  my $tx1TotalLength = length($tx1AllData);
  my $tx2TotalLength = length($tx2AllData);

  say "[DEBUG][SEPARATE] Tx1's total length is " . $tx1TotalLength;
  say "[DEBUG][SEPARATE] Tx2's total length is " . $tx2TotalLength;
  say "[INFO][SEPARATE] Total length is " . ($tx1TotalLength + $tx2TotalLength);

  my $fee1 = $basedFee + $tx1TotalLength * $sizeFee;  
  my $fee2 = $basedFee + $tx2TotalLength * $sizeFee; 

  my $fee = $fee1 + $fee2;  

  say "[INFO][SEPARATE] Total fee is " . $fee;
}


sub main{
  my $testRound = 200;
  for (my $i = 0; $i < $testRound; ++$i){
    doOnetest();
  }
}

main();