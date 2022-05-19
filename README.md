SSH execute command with password
=================================
No need to confirm fingerprint,
no need to type password in keyboard,
just put password as command-line argument to execute command via SSH,
suit for using in scripts.


Usage as standalone run
=======================
1. Download, unzip from the Releases page.
2. Run in command prompt, arguments are same as the standard ssh command, plus --password option, like
```bat
ssh-execute --password OpenSesame alibaba@192.168.0.1 "ls -l --color /tmp"
```
or
```bat
ssh-execute -l alibaba --password OpenSesame -p 22 192.168.0.1 "ls -l --color /tmp"
```

Specify connection timeout in seconds:
```bat
ssh-execute -o ConnectTimeout=3 --password OpenSesame alibaba@192.168.0.1 "ls -l --color /tmp"
```
or using a new option name to keep the style:
```bat
ssh-execute --connect-timeout 3 --password OpenSesame alibaba@192.168.0.1 "ls -l --color /tmp"
```

Get full command-line usage:
```bat
ssh-execute --help
```


Usage as Java package
=====================
(Under construction...)


How to compile
==============
(Under construction...)


Development Notes
=================
(Dependencies)

sshj
----
examples  
https://github.com/hierynomus/sshj/tree/master/examples/src/main/java/net/schmizz/sshj/examples

com.hierynomus:sshj:0.33.0  
https://search.maven.org/artifact/com.hierynomus/sshj

```xml
<dependency>
  <groupId>com.hierynomus</groupId>
  <artifactId>sshj</artifactId>
  <version>0.33.0</version>
</dependency>
```

Apache Commons CLI
------------------
Using  
https://commons.apache.org/proper/commons-cli/usage.html

```xml
<dependency>
  <groupId>commons-cli</groupId>
  <artifactId>commons-cli</artifactId>
  <version>1.5.0</version>
</dependency>
```

SLF4J
-----
user manual  
https://www.slf4j.org/manual.html

java.util.logging
-----------------
config file  
http://www.javapractices.com/topic/TopicAction.do?Id=143
