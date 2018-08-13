原生spring cloud config支持native、git、svn等模式的配置文件读取

1、公共的配置提取到base-base.XXX文件中
    通过覆盖NativeEnvironmentRepository、AbstractScmEnvironmentRepository文件，实现在native、git、svn等模式下读取配置的方式。
在原生A-B.XXX的基础上，实现公共部分的抽取，将base-base.XXX文件中的内容自动添加到A-B.XXX文件中，如果A-B.XXX和base-base.XXX文件中
有相同的配置，A-B.XXX中的内容会覆盖base-base.XXX中的内容。（XXX代表yml/yaml/properties/json）

2、新增db模式的读取配置方式
    1)、新建DbEnvironmentRepository类，继承EnvironmentRepository
    2)、重写findOne方法，实现自己的业务逻辑
    3)、在DB模式相关的类中添加@Profile("db")注解，只在db模式下启动数据库连接
注意：数据库中以字符串的形式存储数据，对于查询到的数据需要进行类型转化，否则返回的数据格式不正确。
   可以通过调用原生的Resolver.resolve来对类型进行转化。
   
3、EnvironmentController进行配置查找的调用过程
    
    1)、通过EnvironmentController进来的请求，最后都会通过Environment labelled(String name, String profiles, String label)去加载，并返回一个Environment对象
    因此，我们可以通过name、profiles和label三个要素去唯一定位一个Environment对象，观察Environment对象我们发现，属性List<PropertySource> propertySources中包含了所有的配置。
    
    2)、 labbelled方法最终调用EnvironmentRepository.findOne(application，profile, label)方法去查找配置信息，EnvironmentRepository接口有几个实现类，其中就包含了NativeEnvironmentRepository、
    JGitEnvironmentRepository、SvnKitEnvironmentRepository、VaultEnvironmentRepository等，DB模式的配置中心，就是通过新增DbEnvironmentRepository来实现。
    
    3)、以NativeEnvironmentRepository.findOne为例,调试源码可知，调用流程如下：
        getEnvironment(config, profile, label)
        -->SpringApplicationBuilder.run(args)
           -->SpringApplication.run(...args)
              -->SpringApplicationRunListeners.environmentPrepared(environment)
                 -->SimpleApplicationEventMulticaster.multicastEvent(event)
                    -->SimpleApplicationEventMulticaster.invokeListener(listener, event)
                       -->ConfigFileApplicationListener.onApplicationEvent(event)
                          -->SpringFactoriesLoader.loadFactories(...)
                             -->PropertiesLoaderUtils.loadProperties(url)
                                -->Properties.load(reader)
        
    4)、 对于返回的Environment对象，不同的请求方式（.yml、.json、.properties）会有不同的后续处理，
        yml:    new Yaml().dumpAsMap(result);
          通过调用snakeyaml-sources.jar中的Serializer类对数据进行序列化，返回字符串yaml.
          serializer.serialize(node);
          会将查找到的数据进行类型转化，Resolver.resolve(...)
        json:   this.objectMapper.writeValueAsString(properties);
        properties:     getPropertiesString(properties);
        