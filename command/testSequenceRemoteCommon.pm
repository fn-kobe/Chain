#/usr/bin/perl
package testSequenceRemoteCommon;
require(Exporter);

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

our @ISA = qw(Exporter);
our @EXPORT = qw(processBlockchainCommandOnRemote creatFolderIfNotExist waitAndReplaceFileRemote replaceFile replaceRemote
  checkAllRemoteNode checkOnRemote doesCommandOutputContainFlag doesFileContain getParameter getIpList backUpNodeTestData killChildThread waitInitBlockchain
  runRemoteCommandInChildProcess runRemoteCommand runRemoteCommandWithoutOutput initNodeIp getNodeConfig); #要输出给外部调用的函数或者变量，以空格分隔
our @version = 1.0;

# return: 0 found, -1 not found
sub processBlockchainCommandOnRemote {
  say "[INFO]Enter processBlockchainCommandOnRemote";
  my ($nodeNumberList, $commandToExecute, $nodeInformationMap, $commandFileWaitingTime, $maxCheckingForOneNodeRetries) = @_;
  say "[MAIN][DEBUG] Nodes to tested are '@{$nodeNumberList}'";
  for my $nodeNumber (@{$nodeNumberList}) {
    my ($server, $user, $pass, $remoteWorkingFolder) = @{$nodeInformationMap->{$nodeNumber - 1}};
    my $folder = File::Spec->catfile($remoteWorkingFolder, $nodeNumber, "command");
    # command is put to command file for blockchain to read
    if (!(-d "$folder")) {
      say "Folder $folder does not exist. Try to create it";
      my $cmd = "mkdir -p $folder";
      runRemoteCommand($server, $user, $pass, $cmd);
    }

    if (-1 == waitAndReplaceFileRemote($nodeNumber, "$folder", "command", $nodeInformationMap,
      $commandFileWaitingTime, $maxCheckingForOneNodeRetries, $commandToExecute)) {
      return -1;
    }
  }
  return 0;
}

sub creatFolderIfNotExist {
  my $name = shift;
  if (!(-d $name)) {
    if (!mkdir($name)) {
      say "[MAIN][ERROR] error to create folder $name $_";
    }
  }
}

sub waitAndReplaceFileRemote {
  my ($nodeNumber, $filePath, $fileName, $nodeInformationMap, $commandFileWaitingTime, $maxCheckingForOneNodeRetries, @content) = @_;
  my ($server, $user, $pass) = @{$nodeInformationMap->{$nodeNumber - 1}};
  my $waitTime = 0;
  while (0 == doesCommandOutputContainFlag($server, $user, $pass, "ls $filePath", $fileName)) {
    say "[INFO][WARN] $fileName exists. Waiting ...";
    sleep($commandFileWaitingTime);
    ++$waitTime;
    if ($waitTime > $maxCheckingForOneNodeRetries) {
      say "[ERROR] Max waiting time for command to be processed";
      return -1;
    }
  }

  return replaceRemote($server, $user, $pass, File::Spec->catfile($filePath, $fileName), @content);
}

sub replaceFile {
  my ($filePath, $fileName, @content) = @_;
  my $file_handle;
  if (!open($file_handle, ">", File::Spec->catfile($filePath, $fileName))) {
    say "[ERROR] cannot open file $fileName";
    return -1;
  }

  for my $line (@content) {
    print $file_handle  $line;
  }

  if (!close($file_handle)) {
    say "[WARN] Couldn't close the file $fileName \n";
  }

  say "[INFO] succeed to add the content @content to $fileName";
  return 0;
}

sub replaceRemote {
  say "Enter replaceRemote";
  my ($server, $user, $pass, $filePath, @content) = @_;

  my $ssh2 = Net::SSH2->new();
  $ssh2->connect($server)
    or $ssh2->die_with_error;
  $ssh2->auth_password($user, $pass)
    or $ssh2->die_with_error;

  my $sftp = $ssh2->sftp()
    or $ssh2->die_with_error;;

  #used to avoid read in blockchain.jar when we writting
  my $tempName = $filePath . ".tmp";
  my $file_handle = $sftp->open($tempName, O_CREAT | O_EXCL | O_WRONLY)
    or $sftp->die_with_error;

  for my $line (@content) {
    print $file_handle  $line . "\n";
  }

  close($file_handle);
  $sftp->rename($tempName, $filePath);
  say "Leave replaceRemote";
}

# We assume each remote node is mapped to a folder under a root folder
# and we can check for different groups separated by "|"
sub checkAllRemoteNode {
  my ($checkingNodeGroupList, $condition, $nodeInformationMap) = @_;

  for my $oneGroup (@{$checkingNodeGroupList}) {
    if (0 == checkOneRemoteGroupNode($oneGroup, $condition, $nodeInformationMap)){
      return 0;
    }
  }
  return -1;
}

sub checkOneRemoteGroupNode {
  my ($oneGroup, $condition, $nodeInformationMap) = @_;
  for my $n (@{$oneGroup}) {
    if (-1 == checkOnRemote($n, $condition, $nodeInformationMap)) {
      return -1;
    }
  }
  return 0;
}

# return: 0 found, -1 not found
sub checkOnRemote {
  say "[INFO]Enter checkOnRemote";
  my ($nodeNumber, $condition, $nodeInformationMap) = @_;
  my $nodeFolder = $nodeNumber;
  if (!$condition) {
    say "[INFO] Condition is empty. Return as FOUND";
    say "[INFO]Leave checkOnRemote";
    return 0;
  }
  my ($server, $user, $pass, $remoteWorkingFolder) = @{$nodeInformationMap->{$nodeNumber - 1}};
  my $folderOnCheckingNode = $nodeFolder;
  if (-1 == index($condition, "log")) {
    $folderOnCheckingNode = File::Spec->catfile($remoteWorkingFolder, $nodeFolder, "Blockchain_dump");
  }
  else {
    $condition = substr($condition, 3); # remove log prefix
    $folderOnCheckingNode = File::Spec->catfile($remoteWorkingFolder, $nodeFolder, "autoRun.log");
  }

  # fgrep will fix grep string without regex expression
  my $cmd = "fgrep -nir '$condition' $folderOnCheckingNode";
  if (0 == doesCommandOutputContainFlag($server, $user, $pass, $cmd, $condition)) {
    say "[INFO]Condition '$condition' is FOUND in folder '$folderOnCheckingNode'";
    return 0;
  }

  say "[INFO]Condition '$condition' is NOT found in folder '$folderOnCheckingNode'";
  say "[INFO]Leave checkOnRemote";
  return -1;
}

# 0 exists
sub doesCommandOutputContainFlag {
  my ($server, $user, $pass, $cmd, $flag) = @_;
  my ($stdout, $exit) = runRemoteCommand($server, $user, $pass, $cmd);
  say "[Test] stdout is $stdout";

  if (!$flag || -1 != index($stdout, $flag)) {
    return 0;
  }
  return 1;
}

# return: 0 found, -1 not found
sub doesFileContain {
  my ($fileName, $content) = @_;

  if (!open(FH, '<', $fileName)) {
    say "[INFO][ERROR] cannot open file $fileName to read";
    return -1;
  }

  while (<FH>) {
    if (-1 != index($_, $content)) {
      say "[INFO]Condition '$content' found in '$fileName'";
      close(FH);
      return 0;
    }
  }

  close(FH);
  return -1;
}

sub getParameter {
  my $command = shift;

  my @commandArray = split(/;/, $command);
  if (@commandArray < 2) {
    say "[INFO]Command parameter is not correct " . $command;
    return -1;
  }

  my @executingNodeList = getIpList($commandArray[0]);

  my $commandToExecute = $commandArray[1];
  if (!$commandToExecute) {
    $commandToExecute = "";
  }
  $commandToExecute =~ s/-/ /g;

  my @checkingNodeGroupList = getParallelIpList($commandArray[2]);
  my $condition = $commandArray[3];
  if (!$condition) {
    $condition = "";
  }
  $condition =~ s/-/ /g;

  my $nodeList = "";
  for my $n (@checkingNodeGroupList){
    if ($nodeList){
      $nodeList .= " or ";
    }
    $nodeList .= @{$n};
  }
  say "[INFO]Begin to check on node of '@executingNodeList' to execute '$commandToExecute' no nodes $nodeList when blockchain dump contains keyword '$condition'";

  return(0, \@executingNodeList, $commandToExecute, \@checkingNodeGroupList, $condition);
}

# get a| b format ip list
sub getParallelIpList {
  my $originalList = shift;
  my @originalListArray = split(/\|/, $originalList);
  my @r = ();
  for my $oneGroup (@originalListArray){
    my @ipList = getIpList($oneGroup);
    push(@r, \@ipList);
  }
  return @r;
}

sub getIpList {
  my $originalList = shift;
  if (!$originalList) {
    return();
  }

  my @result;
  if (-1 != index($originalList, "-")) {
    my @originalArray = split(/-/, $originalList);
    if (@originalArray < 2) {
      die "$originalList is not correctly format";
    }
    for (my $i = $originalArray[0]; $i <= $originalArray[1]; ++$i) {
      push @result, $i;
    }
  }
  elsif (-1 != index($originalList, ":")) {
    @result = split(/:/, $originalList);
  }
  else {
    push @result, $originalList;
  }

  return @result;
}

sub backUpNodeTestData {
  say "[Main][INFO] Enter backUpNodeTestData";
  my ($allNodeNumber, $nodeInformationMap, $specificName) = @_;

  my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime;
  $year += 1900; # $year是从1900开始计数的，所以$year需要加上1900；
  $mon += 1;     # $mon是从0开始计数的，所以$mon需要加上1；
  my $datetime = sprintf("%d-%02d-%02d_%02d_%02d_%02d", $year, $mon, $mday, $hour, $min, $sec);
  say "[Main][INFO] Backup folder has been created " . $datetime;

  for (my $i = 0; $i < $allNodeNumber; ++$i) {
    my $dirname = ($i + 1);
    my $backupFolder = File::Spec->catfile("backup", $datetime, $dirname);
    if ($specificName){
      $backupFolder = File::Spec->catfile($backupFolder, $specificName);
    }
    make_path($backupFolder);
    say "[Main][INFO] Node back up folder has been created " . $backupFolder;
    my $c = "";
    if (-1 != index($OSNAME, "MSWin32")) {
      say "MSWin32 system to backUpNodeTestData";
      $c = "xcopy $dirname $backupFolder /s /e /y";;
    }
    elsif (-1 != index($OSNAME, "linux")) {
      say "Linux system to backUpNodeTestData";

      my ($server, $user, $pass, $remoteWorkingFolder) = @{$nodeInformationMap->{$i}};
      my $localDirName = $backupFolder;
      my $remoteDirFullName = File::Spec->catfile($remoteWorkingFolder, $dirname);

      say "[INFO] Begin to backup $remoteDirFullName on $server";
      my $fileName = "$dirname.tar.gz";
      my $cmd = "cd  $remoteDirFullName && tar -czvf $fileName autoRun.log  Blockchain_dump *fig* *Chain*";
      runRemoteCommandWithoutOutput($server, $user, $pass, $cmd);

      $cmd = "cd $localDirName && sshpass -p $pass scp $user\@$server:$remoteDirFullName/$fileName . && tar xzvf $fileName";
      my $r = `$cmd`;
      if (0 != $?) {
        say $r;
      }
    }
    else {
      say "Unkonwn system, no command to backUpNodeTestData";
    }

    # say "[Main][INFO] File backup result " . $r;
  }

  say "[Main][INFO] Eixt backUpNodeTestData";
}

sub killChildThread {
  say "Enter killChildThread";
  my ($nodeInformationClassfiedByAddress, $nodeSubActionThreadArray) = @_;
  # We do not find a good way to kill, just eixt and java jar process exit automatically. 
  my $c = "";
  if (-1 != index($OSNAME, "MSWin32")) {
    say "MSWin32 system to kill process";
    $c = "taskkill /im javaSp.exe /t /f";
  }
  elsif (-1 != index($OSNAME, "linux")) {
    say "Linux system to kill process";
    $c = "killall javaSp";
  }
  else {
    say "Unkown system, no command to kill process";
  }
  my @serverList = values %{$nodeInformationClassfiedByAddress};
  for my $oneServerInfo (@serverList) {
    my ($server, $user, $pass) = @{$oneServerInfo};
    say "[INFO] Try to kill blockchain process on remote node: $server with command '$c'";
    runRemoteCommand($server, $user, $pass, $c);
  }

  for my $id (@{$nodeSubActionThreadArray}) {
    say "[INFO] Try to kill process on control node $id";
    kill $id;
  }
  say "[Main][INFO] Exit killChildThread";
}

sub waitInitBlockchain {
  say "[Main][INFO] Enter waitInitBlockchain \n";
  my ($allNodeNumber, $nodeInformationMap, $checkInterval) = @_;

  for (my $i = 0; $i < $allNodeNumber; ++$i) {
    my ($server, $user, $pass, $remoteWorkingFolder) = @{$nodeInformationMap->{$i}};
    my $dirname = File::Spec->catfile($remoteWorkingFolder, $i + 1, "Blockchain_dump");
    my $isInterBCOk = 0;
    my $isExterBCOk = 0;
    while (1 != $isInterBCOk || 1 != $isExterBCOk) {
      sleep($checkInterval);

      my $cmd = "ls $dirname";
      my ($stdout, $exit) = runRemoteCommandWithoutOutput($server, $user, $pass, $cmd);
      say "[Test][waitInitBlockchain] stdout is $stdout";

      if (!(-f "externalChainProducer") || (-z "externalChainProducer")){
        say "[Test][waitInitBlockchain] No external producer and no need to check external blockchain";
        $isExterBCOk = 1;
      }

      if (-1 != index($stdout, "external")) {
        $isExterBCOk = 1;
      }
      if (-1 != index($stdout, "internal")) {
        $isInterBCOk = 1;
      }

      if (1 == $isInterBCOk && 1 == $isExterBCOk) {
        say "[Main][INFO] Internal and external blockchain list has been generated on node " . ($i + 1) . ".";
        last;
      }
    }
  }

  say "[Main][INFO] Eixt waitInitBlockchain";
}

sub runRemoteCommandInChildProcess {
  my ($server, $user, $pass, $cmd) = @_;
  my $childId = fork();
  if (0 == $childId) {
    runRemoteCommand($server, $user, $pass, $cmd);
    say "[CHILD][$$] Begin to exit child process";
    exit(0);
  }
  else {
    return $childId;
  }
}

sub runRemoteCommandWithoutOutput {
  runRemoteCommand(@_, "noOutput");
}

sub runRemoteCommand {
  my ($server, $user, $pass, $cmd, $noOutput) = @_;

  my $ssh2;
  # if (exists($ipSSHMap{$server})){
  #     $ssh2 = $ipSSHMap{$server};
  # } else {
  $ssh2 = Net::SSH2->new();
  $ssh2->connect($server)
    or $ssh2->die_with_error;
  $ssh2->auth_password($user, $pass)
    or $ssh2->die_with_error;
  #$nodeIP2InformationMap{$server} = $ssh2;
  # }

  my $chan = $ssh2->channel()
    or $ssh2->die_with_error;

  $chan->exec($cmd)
    or $ssh2->die_with_error;

  my $stdout = "";
  my ($out, $err) = ('', '');
  while (!$chan->eof) {
    if (my ($o, $e) = $chan->read2) {
      $out .= $o;
      $err .= $e;
    }
  }
  $stdout = $out . $err;

  my $exit = $chan->exit_status;
  $chan->close;

  if (0 != $exit) {
    say "[WARN] Failed to run command $cmd on server $server with exit code $exit. \
    (Maybe caused by kill command, 143, as to exit test to kill java -jar blockchain.jar or killall javaSp if it does not occur)";
  }
  if (!$noOutput) {
    say "[Debug] --- output:\n $stdout";
    say "[Debug] *** end output";
  }

  return($stdout, $exit);
}

# 1 already exists, 0 not exists
sub initNodeIp {
  #将一个数组或者哈希赋值给一个$变量 是赋值的他的长度
  #    $i -1, \%nodeInformationMap, \%nodeInformationClassfiedByAddress
  my ($nodeIndex, $nodeInformationMap, $nodeInformationClassfiedByAddress) = @_;

  my $nodeNumber = $nodeIndex + 1;

  #hash的引用：hash的引用是hash的“地址”，对引用进行操作会改变原hash的值。
  # 引用创建：使用\符号。$href =\%hash；使用{}表示匿名哈希的引用。
  # 使用{}进行解引用，相当于一个hash名。%hash={“fruit”=>"apple"};$href=\%hash;%{href}等价于%hash。
  # 也可使用“->”进行解引用：${$href}{"fruit"}等价于$hash{"$fruit"},可写为$href->{"fruit"}。
# ${$href}{"fruit"}等价于$hash{"$fruit"},可写为$href->{"fruit"}
  if ($nodeInformationMap->{$nodeIndex}) {
    return(1);
  }
  my @nodeConfig = getNodeConfig($nodeNumber);
  if (4 == @nodeConfig) {

    $nodeInformationMap->{$nodeIndex} = \@nodeConfig;
    my $server = $nodeConfig[0];
    $nodeInformationClassfiedByAddress->{$server} = \@nodeConfig;
  }

  return(0, $nodeInformationMap, $nodeInformationClassfiedByAddress);
}

sub getNodeConfig {
  my $nodeNumber = shift;

  open my $fh, '<', "$nodeNumber/nodeConfig"
    or die "Cannot open file $nodeNumber/nodeConfig";
  my $firstLine = <$fh>;
  close($fh);

  chomp($firstLine);# 去掉结尾的\n
  return split(/ /, $firstLine);
}

1;
__END__
