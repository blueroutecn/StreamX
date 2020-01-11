package com.streamxhub.flink.core

import com.streamxhub.flink.core.conf.ConfigConst._
import com.streamxhub.flink.core.util.{Logger, PropertiesUtils, SystemPropertyUtils}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._

import scala.collection.JavaConversions._
import scala.annotation.meta.getter


class DataSetContext(val parameter: ParameterTool, val env: ExecutionEnvironment) extends ExecutionEnvironment(env.getJavaEnv)

trait FlinkDataSet extends Logger {

  @(transient@getter)
  private var env: ExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: DataSetContext = _

  def handler(context: DataSetContext): Unit

  private def initialize(args: Array[String]): Unit = {
    //read config and merge config......
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(APP_CONF, null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--flink.conf $path \" in main arguments")
      case file => file
    }
    val configFile = new java.io.File(config)
    require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
    val configArgs = config.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
      case "yml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
    }

    this.parameter = ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties())

    env = ExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(parameter)
  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: ExecutionEnvironment): Unit = {}

  private def createContext(): Unit = {
    context = new DataSetContext(parameter, env)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    doStart()
    handler(context)
  }

  def doStart(): Unit = {
    val appName = parameter.get(KEY_APP_NAME, "")
    logger.info(
      s"""
         |
         |   ____   __   _          __          ___         __          ____       __
         |  / __/  / /  (_)  ___   / /__       / _ \\ ___ _ / /_ ___ _  / __/ ___  / /_
         | / _/   / /  / /  / _ \\ /  '_/      / // // _ `// __// _ `/ _\\ \\  / -_)/ __/
         |/_/    /_/  /_/  /_//_//_/\\_\\      /____/ \\_,_/ \\__/ \\_,_/ /___/  \\__/ \\__/
         |
         |
         |$appName Starting....
         |
         |""".stripMargin)
  }

}


