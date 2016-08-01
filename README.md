# zbus project

Rushmore, new start from Aug-2016

zbus project once started in 2009, mainly served as service bus in Guosen Securities and QianHai Equity Exchange, and was implemented based zeromq. Since 2013, zbus evolved to java platform, and gain much attention in Chinese open source community: http://git.oschina.net/rushmore/zbus. With these experience and many great feedbacks from open source and enterprise I plan to write a totally new version of zbus project, which will improve many features, such as Message Queue, High Availability, Monitor and Admin, and Version compatible control.

Functionalities of zbus 
* Brilliant fast MQ (design borrowed from kafka project)
* Remote Procedure Call
* Service Proxy for HTTP/TCP

Features of zbus 
* Small footprint:  netty.jar + zk.jar + zbus.jar
* High Availability for Finance related applications
* Java/C#/Python/C official clients support
* Easy way to monitor and admin
 
Road Map
* zbus.io started from zbus-0.8, history reason zbus-7.0.0 => zbus-0.7