#! /usr/bin/perl
package generatedCommon;
require(Exporter);

use strict;
use warnings;
use File::Spec;
use Cwd qw(cwd);

our @ISA     = qw(Exporter);
our @EXPORT  = qw(processOneBlockchain ); #要输出给外部调用的函数或者变量，以空格分隔
our @version = 1.0;

sub processOneBlockchain {
    #processOneBlockchain($baseDir, $bcId, $consensusType, $nounce, $zero_count,
    # $ex_zero_count, $peerList[$i], $globalNodeFolderNumber, $interBCNodeNumber
    #, \@peerList, \@externalChainConsumer, \@externalChainProducer, $server[$j], $user, $pass, $workingFolder)
    my ($baseDir, $bcId, $consensusType, $nounce, $zero_count, $ex_zero_count, $ip_port, $dirName,
        $nodeId, $peerList, $externalChainConsumer, $externalChainProducer, $remoteServer, $user, $pass, $workingFolder) = @_;

    printf "Base dir is $baseDir\n";
    chdir $baseDir;
    creatFolderIfNoeExist($bcId);
    # Change to network folder
    chdir $bcId;
    creatFolderIfNoeExist($dirName);
    # Change to node folder
    chdir $dirName;
    my $currentDir = cwd;
    printf "Current node dir is $currentDir\n";

    my $fileName = "ExternalMiningConfiguration";
    my $file_handle;
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    print $file_handle  "[difficulty]\n";
    print $file_handle  "zero_count=$ex_zero_count\n";
    close($file_handle) or die "Couldn't close the file $fileName \n";

    $fileName = "peerAddressConfiguration";
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    foreach my $peer (@{$peerList}) {
        print $file_handle  "$peer\n";
    }
    close($file_handle) or die "Couldn't close the file $fileName \n";

    $fileName = "externalChainConsumer";
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    foreach my $exPeer (@{$externalChainConsumer}) {
        print $file_handle  "$exPeer\n";
    }
    close($file_handle) or die "Couldn't close the file $fileName \n";

    $fileName = "externalChainProducer";
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    foreach my $p (@{$externalChainProducer}) {
        print $file_handle  "[". $p->{producer} ."]\n";
        print $file_handle  $p->{chainId} . "\n";
        print $file_handle  $p->{consensus} . "\n";
        print $file_handle  $p->{difficult} . "\n";
        print $file_handle  $p->{peers} . "\n";
    }

    close($file_handle) or die "Couldn't close the file $fileName \n";

    $fileName = "MiningConfiguration";
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    print $file_handle  "[difficulty]\n";
    print $file_handle  "zero_count=$zero_count\n";
    print $file_handle  "[blockchain]\n";
    print $file_handle  "genesis_nouce=$nounce\n";
    print $file_handle  "blockchainId=$bcId\n";
    print $file_handle  "blockchain_sync_interval=1000\n";
    print $file_handle  "[mining]\n";
    print $file_handle  "account=$bcId?78900$nodeId\n";
    print $file_handle  "consensus=$consensusType\n";
    print $file_handle  "nodeId=0$nodeId\n";
    print $file_handle  "[network]\n";
    print $file_handle  "self_address_port=$ip_port\n";
    close($file_handle) or die "Couldn't close the file $fileName \n";

    $fileName = "nodeConfig";
    open($file_handle, ">", "$fileName") or die "cannot open file $fileName";
    print $file_handle  "$remoteServer $user $pass $workingFolder\n";
    close($file_handle) or die "Couldn't close the file $fileName \n";

    generateSmartContractConfig();
}


sub creatFolderIfNoeExist {
    my ($folderName) = @_;
    if (!(-e $folderName)) {
        my $command = "mkdir $folderName";
        `$command`;
    }
}

sub generateSmartContractConfig {
    my $nativeJar = "blockchain.jar";
    my $smartContractName = "NotCallingOther";
    my ($isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost) =
      ("no", "", "javaSp -cp", $nativeJar, "com.scu.suhong.instantiationOptimization.embed." . $smartContractName);
    doGenerateSmartContractConfig($smartContractName, $isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost);

    $smartContractName = "NotCallingOtherDelay";
    ($isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost) =
      ("yes", "writeStateToBlockchain", "javaSp -cp", $nativeJar, "com.scu.suhong.instantiationOptimization.embed." . $smartContractName);
    doGenerateSmartContractConfig($smartContractName, $isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost);

    $smartContractName = "CallingOneDeeper";
    ($isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost) =
      ("no", "", "javaSp -cp", $nativeJar, "com.scu.suhong.instantiationOptimization.embed." . $smartContractName);
    doGenerateSmartContractConfig($smartContractName, $isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost);
}

sub doGenerateSmartContractConfig{
    my ($smartContractName, $isDelayedInstance, $instanceMustMethods, $Launcher, $runCommand, $ClosePost) = @_;
    my $file_handle;
    my $smartContractFolder = "smartContract";
    mkdir($smartContractFolder);
    my $smartContractConfigFullPath = File::Spec->catfile($smartContractFolder, "$smartContractName.scc");
    open($file_handle, ">", $smartContractConfigFullPath) or die "cannot open file $smartContractConfigFullPath";
    print $file_handle  "[instantiation]\n";
    print $file_handle  "isDelayedInstance = $isDelayedInstance\n";
    print $file_handle  "instanceMustMethods = $instanceMustMethods\n";
    print $file_handle  "[command]\n";
    print $file_handle  "Launcher=$Launcher\n";
    print $file_handle  "runCommand = $runCommand\n";
    print $file_handle  "ClosePost = $ClosePost\n";
    close($file_handle) or die "Couldn't close the file $smartContractName \n";
}

1;
__END__