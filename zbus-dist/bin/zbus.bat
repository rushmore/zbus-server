ECHO OFF

REM SET JAVA_HOME=D:\SDK\jdk6_x64

SET ZBUS_HOME=..
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=io.zbus.mq.server.MqServer

IF "%1" == "" ( 
	SET MAIN_OPTS=-conf ../conf/zbus.xml 
) ELSE ( 
	SET MAIN_OPTS=-conf %1 
) 

SET LIB_OPTS=%ZBUS_HOME%/lib/*;%ZBUS_HOME%/classes;%ZBUS_HOME%/*;%ZBUS_HOME%/conf;
IF NOT EXIST "%JAVA_HOME%" (
    SET JAVA=java
) ELSE (
    SET JAVA="%JAVA_HOME%\bin\java"
)
%JAVA% %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 