use strict;
use warnings;

use Crypt::OpenSSL::Bignum;
use Crypt::OpenSSL::Random;
use Crypt::OpenSSL::RSA;

my $rsa = Crypt::OpenSSL::RSA->generate_key(2048);
my ($n, $e, $d, $p, $q) = $rsa->get_key_parameters();

printf "private key is: %s\n", $rsa->get_private_key_string();
printf "public key (in PKCS1 format) is: %s\n", $rsa->get_public_key_string();