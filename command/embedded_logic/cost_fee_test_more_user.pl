#!/usr/bin/perl
use 5.14.0;
use strict;
use warnings;

use MIME::Base64;

sub generateRandomDataLength{
  my $randomFileSize = 4200; #length from average 211*10 (2110)
  return int(rand($randomFileSize)) + 10;
}

sub doOnetest{
  my $basedFee = 2100;
  my $sizeFee = 1.2;
  my $txMustData = '{"pr":"","d":"","pu":"","h":"481F25F6F0D6A91C8E7D779AD20B7672","exchangeId":01,"miningTime":1615360950543,"blockIndex":"7","t":"","gas":0,"from":"111?789001","id":420546731,"to":"111?789002","value":"0","keyValueParameter":"","txIndex":"7"}';
  my $txCode="if code, then return true";
  doEmbedtest($basedFee, $sizeFee, $txMustData, $txCode);
  doSeparatetest($basedFee, $sizeFee, $txMustData, $txCode);
}

sub doEmbedtest{
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;


  my $numberOfParticipant = 1;# one code for each participant to express this participant's willingness
  my $codeForEachParticipant = $txCode;
  my $user1Tx = generateDataCodeTx($txMustData, $numberOfParticipant, $codeForEachParticipant);
  my $user2Tx = generateDataCodeTx($txMustData, $numberOfParticipant, $codeForEachParticipant);

  my $user1TxTotalLength = length($user1Tx);
  my $user2TxTotalLength = length($user2Tx);

  say "[DEBUG][EMBED] user1Tx's total length is " . $user1TxTotalLength;
  say "[DEBUG][EMBED] user2Tx's total length is " . $user2TxTotalLength;

  my $fee1 = $basedFee + $user1TxTotalLength * $sizeFee;  
  my $fee2 = $basedFee + $user2TxTotalLength * $sizeFee; 
  my $fee = $fee1 + $fee2;  
  say "[INFO][EMBED] Total fee is " . $fee;
}


sub doSeparatetest{
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;


  my $numberOfParticipant = 2;# suppuose two code for those two participants. If more, the codes may not be liner
  my $codeForEachParticipant = $txCode;
  my $codeTx = generateCodeTx($txMustData, $numberOfParticipant, $codeForEachParticipant);
  my $user1Tx = generateDataTx($txMustData);
  my $user2Tx = generateDataTx($txMustData);

  my $codeTxTotalLength = length($codeTx);
  my $user1TxTotalLength = length($user1Tx);
  my $user2TxTotalLength = length($user2Tx);

  say "[DEBUG][SEPARATE] TcodeTx's total length is " . $codeTxTotalLength;
  say "[DEBUG][SEPARATE] user1Tx's total length is " . $user1TxTotalLength;
  say "[DEBUG][SEPARATE] user2Tx's total length is " . $user2TxTotalLength;

  my $feeCode = $basedFee + $codeTxTotalLength * $sizeFee;  
  my $fee1 = $basedFee + $user1TxTotalLength * $sizeFee;  
  my $fee2 = $basedFee + $user2TxTotalLength * $sizeFee; 
  my $fee = $feeCode + $fee1 + $fee2;  
  say "[INFO][SEPARATE] Total fee is " . $fee;
}

sub doOnetestMoreUser{
  my $basedFee = 2100;
  my $sizeFee = 1.2;
  my $txMustData = '{"pr":"","d":"","pu":"","h":"481F25F6F0D6A91C8E7D779AD20B7672","exchangeId":01,"miningTime":1615360950543,"blockIndex":"7","t":"","gas":0,"from":"111?789001","id":420546731,"to":"111?789002","value":"0","keyValueParameter":"","txIndex":"7"}';
  my $txCode="if code, then return true";
  
  my @userNumberArray = (2, 4, 8, 16, 32);
  foreach my $userNumber  (@userNumberArray){
    doEmbedTestMoreUser($basedFee, $sizeFee, $txMustData, $txCode, $userNumber);
    doSeparatetestMoreUser($basedFee, $sizeFee, $txMustData, $txCode, $userNumber);
  }
}

sub doEmbedTestMoreUser{
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;
  my $userNumber = shift;


  my $numberOfParticipant = 1;# one code for each participant to express this participant's willingness
  my $codeForEachParticipant = $txCode;
  
  my @txList;
  my $userTx;
  for (my $i = 0; $i < $userNumber; ++$i){
   $userTx = generateDataCodeTx($txMustData, $numberOfParticipant, $codeForEachParticipant);
    push @txList, $userTx;
  }

  my $fee = 0;
  for (my $i = 0; $i < $userNumber; ++$i){
    say "[DEBUG][EMBED] user " . $i . "'s Tx total length is " . length($txList[$i]);
    $fee += $basedFee + length($txList[$i]) *$sizeFee;
  }

  say "[INFO][EMBED] Total fee is " . $fee;
  say "[INFO][EMBED] Average fee of " . $userNumber . " participants is " . ($fee/$userNumber);
}

sub doSeparatetestMoreUser{
  my $basedFee = shift;
  my $sizeFee = shift;
  my $txMustData = shift;
  my $txCode= shift;
  my $userNumber = shift;

  my $numberOfParticipant = $userNumber;# we simply suppose more user, more code
  my $codeForEachParticipant = $txCode;
  my $codeTx = generateCodeTx($txMustData, $numberOfParticipant, $codeForEachParticipant);
  my @txList;
  my $userTx;
  for (my $i = 0; $i < $userNumber; ++$i){
   $userTx = generateDataTx($txMustData);
    push @txList, $userTx;
  }

  say "[DEBUG][SEPARATE] TcodeTx's total length is " . length($codeTx);

  my $feeCode = $basedFee + length($codeTx) * $sizeFee;  
  my $fee = $feeCode;
  for (my $i = 0; $i < $userNumber; ++$i){
    say "[DEBUG][SEPARATE] user " . $i . "'s Tx total length is " . length($txList[$i]);
    $fee += $basedFee + length($txList[$i]) *$sizeFee;
  }

  say "[INFO][SEPARATE] Total fee is " . $fee;
  say "[INFO][SEPARATE] Average fee of " . $userNumber . " participants is " . ($fee/$userNumber);
}

sub generateCodeTx{
  my $txMustData = shift;
  my $numberOfParticipant = shift;
  my $codeForEachParticipant = shift;
    
  my $codeTx = $txMustData . generateCode($numberOfParticipant, $codeForEachParticipant);
  my $totalLength = length($codeTx);
  say "[INFO][generateCodeTx] Total length is " . $totalLength;
  
  return $codeTx;
}

sub generateCode{
  my $numberOfParticipant = shift;
  my $codeForEachParticipant = shift;
 
  my $txCode = "";
  for (my $i = 0; $i < $numberOfParticipant; ++$i){
    $txCode .= $codeForEachParticipant;
  }
  
  my $txCodeBase64 = encode_base64($txCode);
  say "[DEBUG][EMBED] tx code length is " . length($txCode) . ", and tx base64 code is " . length($txCodeBase64);

  return $txCodeBase64;
}

sub generateDataTx{
  my $txMustData = shift;
  
  my $dataTx = $txMustData . generateData();  
  my $totalLength = length($dataTx);
  say "[INFO][generateCodeTx] Total length is " . $totalLength;
  
  return $dataTx;
}

sub generateData{
  my $randomDataLength = generateRandomDataLength();
  my $txData = "";
  for (my $i= 0; $i < $randomDataLength; ++$i){
    $txData .= "a"; # we use a to fill data, other words can also be used
  }
  return $txData;
}

sub generateDataCodeTx{
  my $txMustData = shift;
  my $numberOfParticipant = shift;
  my $codeForEachParticipant = shift;
    
  my $codeDataTx = $txMustData . generateData(). generateCode($numberOfParticipant, $codeForEachParticipant);
  my $totalLength = length($codeDataTx);
  say "[INFO][generateDataCodeTx] Total length is " . $totalLength;
  
  return $codeDataTx;
}

sub main{
  my $testRound = 200;
  for (my $i = 0; $i < $testRound; ++$i){
    doOnetestMoreUser();
  }
}

main();
