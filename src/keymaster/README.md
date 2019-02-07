# keymaster

FIXME: description

## Installation

Download from http://example.com/FIXME.

## How to build proto files

    $ lein pom && mvn generate-sources

## Creating a snakeoil cert

    $ openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout snakeoil.key -out snakeoil.pem -subj "/C=US/ST=Illinois/L=Chicago/O=Snake Oil/OU=InfoSec/CN=snakeoil.com"

    $ openssl pkcs12 -export -out snakeoil.p12 -inkey snakeoil.key -in snakeoil.pem -passout pass:""

## Converting certificate serial numbers into subject id & counter

OpenSSL output

```
        Serial Number:
            04:91:fe:e1:3b:cb:c5:15:12:59:9c:8e:cc:ad:64:39:0b:7a:37:0e
```

```
        Subject: CN=c44aedee-fd9a-4da1-ad7e-95d48538c317
```

Python script

```
>>> padding = 100000000
>>> s = '04:91:fe:e1:3b:cb:c5:15:12:59:9c:8e:cc:ad:64:39:0b:7a:37:0e'
>>> s.replace(':', '')
'0491fee13bcbc51512599c8eccad64390b7a370e'
>>> int(s.replace(':', ''), 16)
26091774300724008084012778977114618344700000014L
>>> base = int(s.replace(':', ''), 16)
>>> uuidbase = base / padding
>>> counter = base - uuidbase * padding
>>> hex(uuidbase)
'0xc44aedeefd9a4da1ad7e95d48538c317L'
>>> f = lambda x: '-'.join([x[2:10], x[10:14], x[14:18], x[18:22], x[22:34]])
>>> f(hex(uuidbase)), counter
('c44aedee-fd9a-4da1-ad7e-95d48538c317', 14L)
```

## TLS with grpc-java

Keymaster needs a private key and certificate chain so it can receive traffic over TLS. 
This will be handled differently in production, but for dev purposes we'll use the default key materials from xio.

```
$ cp xio-default-server-private-key-pkcs8.pem keymaster-private-key.pem
$ cat xio-default-server-certificate-x509.pem xio-default-snakeoil-intermediate-x509.pem xio-default-snakeoil-ca-x509.pem > keymaster-certificate-chain.pem
```

## Usage

FIXME: explanation

    $ java -jar keymaster-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
