<?xml version="1.0" encoding="UTF-8"?>
${comments.log4jconfiguration!}
<Configuration status="${statusLevel}">
    <Appenders>
      <#list appenders as appender>
        ${comments[appender.name]!}
        <#list appender.param as param>
          <#if param.name == "File">
            <#assign fileName=param.value>
          </#if>
          <#if param.name == "Append">
            <#assign append=param.value>
          </#if>
        </#list>
        <#if appender.clazz == "org.apache.log4j.RollingFileAppender">
        <RollingFile name="${appender.name}" fileName="${fileName}" filePattern="${fileName?replace('.log', '-%i.log')}">
            <PatternLayout pattern="${appender.layout.param?first.value}"/>
            <@policies appender/>
            <@maxbackupindex appender/>
        </RollingFile>
        <#elseif appender.clazz == "org.apache.log4j.DailyRollingFileAppender">
        <RollingFile name="${appender.name}" fileName="${fileName}" filePattern="${fileName?replace('.log', '%d{' + appender.param?filter(p -> p.name == 'DatePattern')?first.value + '}.log')}">
            <PatternLayout pattern="${appender.layout.param?first.value}"/>
            <@policies appender/>
            <@maxbackupindex appender/>
        </RollingFile>
        <#elseif appender.clazz == "org.apache.log4j.FileAppender">
        <File name="${appender.name}" fileName="${fileName}" append="${append}">
            <PatternLayout pattern="${appender.layout.param?first.value}"/>
            <#list appender.param as p>
                <#if p.name == "Threshold">
            <ThresholdFilter level="${p.value}"/>
                </#if>
            </#list>
        </File>
        <#elseif appender.clazz == "org.apache.log4j.ConsoleAppender">
        <Console name="${appender.name}"<@consoletarget appender/>>
            <PatternLayout pattern="${appender.layout.param?first.value}"/>
            <#list appender.param as p>
                <#if p.name == "Threshold">
            <Filter type="ThresholdFilter" level="${p.value}"/>
                </#if>
            </#list>
        </Console>
        <#elseif appender.clazz == "org.apache.log4j.varia.NullAppender">
        <Null name="${appender.name}" />
        <#elseif appender.clazz == "org.apache.log4j.AsyncAppender">
        <Async name="${appender.name}"<@consoletarget appender/>>
            <#list appender.appenderRef as appenderRef>
            <AppenderRef ref="${appenderRef.ref}"/>
            </#list>
        </Async>
        <#else>
        <${appender.clazz?keep_after_last(".")} name="${appender.name}">
          <#list appender.param as p>
            <Param name="${p.name}" value="${p.value}"/>
          </#list>
        </${appender.clazz?keep_after_last(".")}>
        </#if>
      </#list>

    </Appenders>
    
    <Loggers>
      <#list loggers as logger>
        ${comments[logger.name]!}
        <Logger name="${logger.name}"<#if (logger.priorityOrLevel?size > 0)> level="${logger.priorityOrLevel?first.value}"</#if><#if !logger.additivity?boolean> additivity="${logger.additivity}"</#if><#if (logger.appenderRef?size == 0)>/</#if>>
          <#if (logger.appenderRef?size > 0)>
            <#list logger.appenderRef as appender>
            <AppenderRef ref="${appender.ref}"/>
            </#list>
        </Logger>
          </#if>
      </#list>
        ${comments.root!}
        <Root level="${root.priorityOrLevel?first.value}">
          <#list root.appenderRef as appender>
            <AppenderRef ref="${appender.ref}"/>
          </#list>
        </Root>

    </Loggers>
</Configuration>
<#macro consoletarget appender>
  <#assign target = "">
  <#list appender.param as p>
    <#if p.name == "Target">
      <#assign target = p.value>
    </#if>
  </#list>
  <#if target == "System.out">
    <#assign target = "">
  </#if>
  <#if target == "System.err" || target == "STDERR">
    <#assign target = " target=\"SYSTEM_ERR\"">
  </#if>
${target}</#macro>
<#macro policies appender>
  <#if appender.clazz == 'org.apache.log4j.DailyRollingFileAppender' || (appender.param?filter(p -> p.name == 'MaxFileSize')?size > 0)>
            <Policies>
      <#list appender.param as p>
        <#if p.name == "MaxFileSize">
                <SizeBasedTriggeringPolicy size="${p.value}"/>
        </#if>
      </#list>
      <#if appender.clazz == 'org.apache.log4j.DailyRollingFileAppender'>
                <TimeBasedTriggeringPolicy filePattern="${fileName?replace('.log', '%d{' + appender.param?filter(p -> p.name == 'DatePattern')?first.value + '}.log')}"/>
      </#if>
            </Policies>
  </#if>
</#macro>
<#macro maxbackupindex appender>
  <#list appender.param as p>
    <#if p.name == "MaxBackupIndex">
            <DefaultRolloverStrategy max="${p.value}"/>    
    </#if>
  </#list>
</#macro>
