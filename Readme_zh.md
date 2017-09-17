                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\  
         \///////////  \/////////    \/////////   \//////////       QQ Group: 467741880

# ZBUS  
zbus核心是一个独立实现的小巧极速的消息队列（MQ），支持持久化与内存队列， 支持单播、广播、组播等多种消息通信模式；在MQ之上 zbus完备地支持了RPC服务，RPC支持独立伺服，基于总线两种模式；同时zbus支持代理服务，基于MQ的HttpProxy实现了类Nginx的HTTP代理服务（支持DMZ网络结构），TcpProxy则支持透明的TCP协议代理，可以代理任何基于TCP的协议，比如代理MySQL数据库。

zbus内建分布式高可用（HA），解决单点问题；Java/.NET/JS/C++/PHP等主流语言接入能力为zbus充当SOA服务总线提供跨平台支持；

在设计上，zbus拥抱KISS准则，所有特性浓缩在一个小小的400K左右的jar包中(非常少的依赖）；轻量，MQ核心，方便二次开发，zbus为微服务架构、系统整合、弹性计算、消息推送等场景提供开箱即用的功能支持。

## 特性
- **高速磁盘/内存MQ**，支持单播，组播，广播多种消息模式
- **RPC开箱即用**，支持同步异步，动态类代理
- **多语言客户端**，Java（服务器）/.NET/JavaScript/PHP/Python/C++/Go(服务器)
- **轻量级**，非常少依赖，整体大小 ~3M
- **无应用故障单点**，分布式高可用的内置支持
- **简洁的协议设计**，类HTTP头部扩展协议，长短连接，WebSocket支持
- **内置监控**，不断丰富的监控指标

## 客户端

[Java](https://gitee.com/rushmore/zbus)
[C#](https://gitee.com/rushmore/zbus-dotnet)
[JavaScript](https://gitee.com/rushmore/zbus-javascript)
[Python](https://gitee.com/rushmore/zbus-python)
[PHP](https://gitee.com/rushmore/zbus-php)
[C++](https://gitee.com/rushmore/zbus-cpp)
[Go](https://gitee.com/rushmore/zbus-go)


## 性能概览

快速AB测试(Apache Benchmark)，基础硬件：Mac i7-4870HQ 2.5GHz SSD

	创建队列: http://localhost:15555/MyTopic?cmd=declare
	
	生产消息 
	ab -k -c 32 -n 1000000 http://localhost:15555/MyTopic?cmd=produce
	消费消息 
	ab -k -c 32 -n 1000000 http://localhost:15555/MyTopic?cmd=consume

	生产:  ~80,000 w/s
	消费:  ~90,000 r/s
	RPC:  ~60,000 /s

	Netty/GoLang网络栈同机比对值： ~150,000/s
	测试数据均未使用批量操作，数据为空，核心耗时在网络栈与处理逻辑栈

## 快速起步

zbus服务器基于Java实现（另有Go版本，推荐使用Java版本），要求JDK6+。

Maven官方下载

	<dependency>
		<groupId>io.zbus</groupId>
		<artifactId>zbus</artifactId>
		<version>0.9.0</version>
	</dependency>
 
 Clone项目到本地后，默认客户端模块不下载，如果对客户端感兴趣，请执行

	git submodule update --init --recursive 

各个语言都包括大量使用例子代码，最好的起步了解方法是直接跑示例。

**监控**

启动起来，监控可以直接访问[http://localhost:15555](http://localhost:15555)
![Monitor](/doc/monitor.png?raw=true "Monitor")

## 消息队列（MQ）

消息队列MQ是zbus的核心功能，消息队列的模型，与同类产品诸如RabbitMQ概念类似，但zbus消息队列没有采用AMQP相对复杂的协议，而是更加简洁的协议。

消息队列组件包括
- Topic， 全局唯一标识消息队列，消息需指定
- ConsumeGroup，仅消费者使用到的分组通道，用于实现多种通信模型
- Producer, 生产消息方
- Consumer, 消费消息方，需指定消费分组通道，带默认值。
- Broker，消息存储服务器（注意：API层面的Broker指的是服务器接入抽象）


		Topic: ||||||||||||||||||||||||||||  <-- Producer写入
		          ^   ^
		          |   |
		          +---+------ConsumeGroup_1 --> Consumer1/Consumer2
		              |
		              +--------ConsumeGroup_n -->  Consumer_3/...Consumer_n


**规则设计**：

1. 队列消息数据单份，分组通道任意多个
2. 分组通道之内的消费者共享分组读指针，读指针改变影响本分组通道上的所有消费者
3. 分组通道之间的消费者互不影响
4. 分组通道的支持消息过滤，基于消息头部Tag

简而言之： *分组之内共享，分组之间独享，分组读指针支持消息标签过滤*

分组通道的设计类似同类产品Kafka，但是在消息过滤以及细节上有些差异，相对更加简单。

**消息模式**：
1. 单播 仅一个分组通道，所有的消费者共享一个分组通道，每条消息只送达其中一个消费者
2. 广播 每个消费一个分组通道，每条消息抵达所有的消费者
3. 组播 单播与广播的混合，多个分组通道，每个分组通道上多个消费者负载均衡
4. 订阅模式 广播的分组上设置消息过滤主题（注意区别队列主题Topic）

基于以上设计，zbus的消息队列能够有效的支持各种业务场景，同时保持底层数据模型的简洁设计。为了便于扩展，消息队列增加MASK标记，可以方便应用在队列上打标签来支持队列个性化，比如支持内存队列与磁盘队列的选择，支持标记队列为RPC作用，支持标记队列使用于代理作用等等。

**API示例**

生产者

	//Broker是对zbus服务器的本地抽象，多地址支持HA
	Broker broker = new Broker("localhost:15555"); 
		  
	Producer p = new Producer(broker);
	p.declareTopic("MyTopic");    //当确定队列不存在需创建
		
	Message msg = new Message();
	msg.setTopic("MyTopic");       //设置消息主题
	//msg.setTag("oo.account.pp"); //可以设置消息标签
	msg.setBody("hello " + System.currentTimeMillis()); 
	
	Message res = p.publish(msg);
	System.out.println(res);   
		
	broker.close();	


消费者

	Broker broker = new Broker("localhost:15555");    
	ConsumerConfig config = new ConsumerConfig(broker);
	config.setTopic("MyTopic");  //指定消息队列主题，同时可以指定分组通道
	config.setMessageHandler(new MessageHandler() { 
		@Override
		public void handle(Message msg, MqClient client) throws IOException {
			System.out.println(msg); //消费处理
		}
	});
	
	Consumer consumer = new Consumer(config);
	consumer.start(); 

详细的控制请参考更多项目示例，其他语言客户端保持几乎相同的命名，减少了理解复杂度。

## 远程方法调用（RPC）

远程方法调用RPC使用广泛，虽然跟MQ本质无必然联系，但是在分布式应用中经常会互相融合。MQ重点解决系统耦合问题，提供丰富的消息模式支持，也因此MQ的消息更偏向于单向与责任链设计。RPC则依然耦合了客户端与服务端时间序列，消息是双向的。

RPC的实现可以非常多选择，也无标准可言，底层通道可以是TCP，也可以基于HTTP，甚至也可以是更底层协议。目标可以简化为：

	封装远程方法[名字+参数] -->--+                 +--->解析出方法+参数调用本地方法
	                           |<==> 网络传输<==> |
	调用结果 <------------------+                 +<---封装调用结果

网络传输协议有各种选择（TCP/HTTP...)，如何封装方法+参数也可以各种选择，比如JSON，XML甚至二进制底层的协议（ProtoBuffer,Thrift等等）。

ZBUS的选择，

	1) 底层通信协议： 基于MQ消息传输（TCP），之上支持HTTP直接访问
	2）RPC请求应答协议： 默认JSON封装， 可替换

ZBUS的RPC的优势在于，继承了MQ消息特性，直接支持了HA高可用特性，减少NameServer等重复开发，天生融合MQ与RPC。
选择JSON是一个折中： 动态能力、通信效率和协议标准化三项指标的平衡。当然也支持个性化选择，需要扩展zbus的RpcCodec来完成。

RPC可讨论的主题众多，诸如高可用，超时，重试，熔断，限流，版本控制等等，zbus并没有全部解决，比如熔断。因为这些主题有不少仍然存在争议，在不同应用场景下会有完全不同的结论，比如自动重试在交易系统中是一个非常危险的选择。


ZBUS的RPC在应用设计上追求了极少依赖的目标，给应用程序提供更换的更大可能，也给zbus本身升级带来平滑。

仍然以Java语言示例，其他语言请参考各自示例。

**RPC服务端**

手动加载服务
	
	public static void main(String[] args) throws Exception {   
		ServiceBootstrap b = new ServiceBootstrap();

		//手动增加模块，使用默认构造，也可以指定模块细节
		b.addModule(InterfaceExampleImpl.class); 
		b.addModule(GenericMethodImpl.class);
		b.addModule(SubService1.class);
		b.addModule(SubService2.class);  
		

		b.serviceName("MyRpc") 
		 .port(15555)  //内部启动了zbus服务器，zbus与rpc服务之间不通过网络协议栈
		 //.serviceAddress("localhost:15555")   //也可以通过网络链接到远程服务器上
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //启用SSL
		 //.serviceToken("myrpc_service")       //启用Token权限验证
		 .start();
	} 

自动加载服务 

	public static void main(String[] args) throws Exception { 
		ServiceBootstrap b = new ServiceBootstrap();  
		b.serviceName("MyRpc")
		 .port(15555) 
		 .autoDiscover(true)
		 .start();
	}

是的，没有任何业务相关的，你已经启动了zbus的RPC服务，而业务相关的代码自动加载进来了。
侵入点：服务需要注释@io.zbus.rpc.Remote

对Spring的支持，zbus可以让应用简单几行配置享用zbus的RPC特性

	<bean class="io.zbus.examples.rpc.biz.InterfaceExampleImpl"></bean>
	<bean class="io.zbus.examples.rpc.biz.generic.GenericMethodImpl"></bean> 
	<bean class="io.zbus.examples.rpc.biz.inheritance.SubService1"></bean> 
	<bean class="io.zbus.examples.rpc.biz.inheritance.SubService2"></bean>  
	
	<bean class="io.zbus.rpc.bootstrap.SpringServiceBootstrap" init-method="start">
		<property name="serviceName" value="MyRpc"/> 
		<property name="serviceToken" value="myrpc_service"/> 
		<property name="autoDiscover" value="true"/>  
		<property name="port" value="15555"/>
	</bean>

除此之外你需要关心的可能就是监控与问题排查的时候了解zbus。


**RPC客户端**

	ClientBootstrap b = new ClientBootstrap(); 
	b.serviceAddress("localhost:15555")
		.serviceName("MyRpc")
		.serviceToken("myrpc_service"); 
	
	RpcInvoker rpc = b.invoker();   //可以通过该RpcInvoker调用底层同步、异步各种API能力
	InterfaceExample api = rpc.createProxy(InterfaceExample.class); //最常用是动态代理出实现类，隔离掉zbus细节


通过Spring进一步完全隔离
		
	<bean id="bootstrap" class="io.zbus.rpc.bootstrap.SpringClientBootstrap">
		<property name="serviceAddress" value="localhost:15555"/>
		<property name="serviceName" value="MyRpc"/> 
		<property name="serviceToken" value="myrpc_service"/> 
	</bean>
	
	<bean factory-bean="bootstrap" factory-method="createProxy">
		<constructor-arg type="java.lang.Class" value="io.zbus.examples.rpc.biz.InterfaceExample"/> 
	</bean> 


另外，高可用HA的配置并没有发生本质变化，serviceAddress地址填写多个完毕，细节参考详细示例。

## HTTP、TCP代理（Proxy）

代理也是zbus提供的经常使用到的功能之一，在这个层面zbus并不是与Nginx，Apache等反向代理竞争产品，而是提供了一个更加简洁的实现选择，同时弥补了在特殊网络要求下的代理模式，比如在金融行业中经常会使用到的安全隔离DMZ中，Apache和Nginx等反向代理均失效，因为目标服务器根本无法连接，而zbus的HttpProxy正是为这种代理而生，当然zbus的弹性能力，使得用zbus部署传统的反向代理一样简单。

**HttpProxy代理配置**

启动类 io.zbus.proxy.http.HttpProxy

	<zbus>   
		<!-- HttpProxy, capable of working in DMZ network enviornment --> 
		<!-- Can be multiple zbus servers, for HA -->
		<!-- Token settings is for zbus authentication -->
		<httpProxy zbus="localhost:15555" connectionCount="1" consumeTimeout="10000" token="admin">
		  <proxy entry="http" token="admin" sendFilter="io.zbus.examples.proxy.MyMessageFilter" recvFilter="">
				<target>http://localhost:15555/?cmd=server</target> <!-- Can be multiple -->
				<target>http://localhost:15556/?cmd=server</target>
		  </proxy>
		<!-- you may add multiple proxies -->
		</httpProxy>  
	</zbus>

zbus的代理能力还体现在其扩展性，sendFilter与recvFilter提供了消息代理的中间逻辑拦截介入，来完成个性化需求，诸如打印日志，截断请求，URL重写等等。

除了HTTP层面的代理，zbus也提供了TCP层的透明代理

**TcpProxy TCP透明代理**

可以方便代理任何TCP协议，示例简单自我解释。

启动类 io.zbus.proxy.tcp.TcpProxy

	<zbus>  
		<tcpProxy>  
			<server>0.0.0.0:80</server>
			<target>localhost:15555</target>
			<connectTimeout>3000</connectTimeout>
			<idleTimeout>60000</idleTimeout> 
		</tcpProxy> 
	</zbus>

本地启动80端口服务，所有访问80的端口的数据转发到15555端口上。

