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
use testSequenceRemoteCommon;

# $maxSupportNodeNumber 是最大支持的矿工数量 100
my $maxSupportNodeNumber =100;

# %nodeInformationClassfiedByAddress 通过地址来区分矿工信息
my %nodeInformationClassfiedByAddress = ();
# %nodeInformationMap Map表
my %nodeInformationMap = ();

sub main() {
  say "[MAIN] Begin to process";
  getFromNetwrokFolder();
  doSend();
  say "[MAIN] End to process";
}

# copy 简单的复制的功能
sub getFromNetwrokFolder{
  # 复制之前先删除以前的所有的信息
  say "[INFO] Begin to clean existing folder";
  for (my $i = 1; $i <= $maxSupportNodeNumber; ++$i) {
    # Encounter non-existing folder, exit loop
    if (!(-d $i)) {
      last;
    }
    say `rm -rf $i`;
  }

  say "[INFO] Begin to copy folder from network folder";
  for (my $i = 1; $i <= $maxSupportNodeNumber; ++$i) {
    my $networkFolderName = "$i$i$i";
    if (!(-d $networkFolderName)) {
      last;
    }
    #   如果 $i =11   则$networkFolderName 111111
    say "[INFO] Process network $networkFolderName";
    # 进入这个目录并拷贝所有的东西
    `cd $networkFolderName && cp -rf * ..`;
  }
}

sub doSend{
  my $folderList = "";
  for (my $i = 1; $i <= $maxSupportNodeNumber; ++$i) {
    # Encounter non-existing folder, exit loop
    if (!(-d $i)) {
      last;
    }

    $folderList .= $i . " ";

    #  1 already exists, 0 not exists
    my ($r, $newNodeInformationMap, $newNodeInformationClassfiedByAddress) = initNodeIp($i -1, \%nodeInformationMap, \%nodeInformationClassfiedByAddress);

    if (0 == $r){
      %nodeInformationClassfiedByAddress = %{$newNodeInformationClassfiedByAddress};
    }
  }

  my $tarFileName = "sendToRemote.tar.gz";
  #*.pub is to package public key for hash-locking test
  #将 $folderList 以及 blockchain.jar *.pl *.pub  打包成sendToRemote.tar.gz
  my $cmd = "rm -f $tarFileName && tar czvf $tarFileName $folderList blockchain.jar *.pl *.pub";
  say `$cmd`;

  # -e 文件或目录名存在
  if (!(-e $tarFileName)){
    say "$tarFileName does not exists. Skip";
    exit(0);
  }

  my @serverList = values %nodeInformationClassfiedByAddress;
  if (!@serverList){
    say "[WARN] Empty server list";
  }

  for my $oneServerInfo (@serverList) {
    my ($server, $user, $pass, $workingFolder) = @{$oneServerInfo};
    $cmd = "mkdir -p $workingFolder && cd $workingFolder && rm -rf *";
    say "[INFO] Try to clean remote node: $server with command $cmd";
    runRemoteCommand($server, $user, $pass, $cmd);

    say "[INFO] Try to send $tarFileName to remote node: $server with scp command";
    $cmd = "sshpass -p $pass scp $tarFileName $user\@$server:$workingFolder";
    say 'sshpass -p $pass scp $tarFileName $user\@$server:$workingFolder';
    say `$cmd`;

    $cmd = "cd $workingFolder && tar xzvf $tarFileName && perl copyJarAndClean.pl";
    say "[INFO] Try to process further (unzip and copy jar and clean.pl) on $server";
    runRemoteCommand($server, $user, $pass, $cmd);

    my $javaFullPath = "/usr/bin/java";
    my $javaSpFullPath = "/usr/bin/javaSp";
    $cmd = "if [ ! -f $javaSpFullPath ]; then echo $pass | sudo -S cp -s $javaFullPath $javaSpFullPath && echo '$javaSpFullPath copied'; fi";
    say "[INFO] Try to check javaSp on $server";
    runRemoteCommand($server, $user, $pass, $cmd);
  }
}

main();

