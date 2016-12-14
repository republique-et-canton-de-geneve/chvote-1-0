# CHVote [![Build Status](https://travis-ci.org/lo-enterprise/chvote-1-0.svg?branch=master&style=flat)](https://travis-ci.org/lo-enterprise/chvote-1-0) [![Coverage Status](https://coveralls.io/repos/github/lo-enterprise/chvote-1-0/badge.svg?branch=master)](https://coveralls.io/github/lo-enterprise/chvote-1-0?branch=master)

CHVote aims to be an opensource, publicly owned evoting system. It is the result of the collaboration
between the Geneva State Chancellery and the Geneva IT Department.

CHVote is currently used by 4 cantons in Switzerland: Basel-City, Bern, Geneva and Luzern, either for
votations or elections.

# Released components
The following components are released as opensource software:
* The offline administration application
* Various utility libraries needed by this application

The goals of the offline administration application are to:
* generate the keys responsible for encrypting and decrypting the stored ballots, using passphrases from the 
electoral board;
* test the generated keys and the passphrases;
* decrypt the ballots.

The security objective of having an offline application is to ensure that the private key able to decrypt the ballots
is never known to any system other than the offline application and its offline laptop.

The offline administration application is a key component of the evoting system in that it is the only one responsible
for creating and using the private decryption key.

# System overview
Please read the [system overview documentation](docs/system-overview.md) to learn how the published
 components contribute to the evoting system. You'll get an overview on the following themes:
  * the election process;
  * the system architecture including focuses on the ballot box cryptography and the use of the offline administration
  application;
  * the security concept.

# Compiling and running

## Preconditions
The following software must be installed to compile and run the application:
* Oracle JDK 8
* Maven 3
* [JCE Unlimited Strength Jurisdiction Policy Files for Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
  (see the readme.txt file in the downloaded file for installation instructions into your JDK instance).
  
We do not provide support for the use of OpenJDK/OpenJFX. 

## Compiling
Compile and install the 3 modules in this sequence:

```Shell
cd $PROJECT_ROOT/base-pom
mvn clean install

cd $PROJECT_ROOT/commons-base
mvn clean install

cd $PROJECT_ROOT/admin-offline
mvn clean install
```

Some JDK distributions do not come with the Monocle classes used by the headless GUI tests. If you're running into those cases 
(java.lang.AbstractMethodError: com.sun.glass.ui.monocle.NativePlatform.createInputDeviceRegistry appearing in the test logs),
use the following command to skip the GUI tests:

```Shell
cd $PROJECT_ROOT/admin-offline
mvn -P skipJavaFXTests clean install
```

## Running
Run the application with maven:
```Shell
cd $PROJECT_ROOT/admin-offline
mvn exec:java -Dexec.mainClass="ch.ge.ve.offlineadmin.OfflineAdminApp"
```

# Contributing
CHVote is opensourced with the main purpose of transparency to build confidence in the system.
 
Contributions are also welcomed, either using pull requests or by submitting issues in github. The CHVote community
manager will take care of those inputs, lead exchanges around them, and actions could take place according to their 
relevance, their criticality, and the CHVote development roadmap.

In case of vulnerability discovery, please use the following email address for coordinated disclosure: security-chvote@etat.ge.ch.

# Licence
CHVote components are released under [AGPL 3.0](https://www.gnu.org/licenses/agpl.txt).

# Future
The second generation of CHVote is under active development. It aims to provide end-to-end encryption with individual
and universal verifiabilities. Its source code will be published under AGPL 3.0 as well.
