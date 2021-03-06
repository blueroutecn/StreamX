package com.streamxhub.flink.core.source

import java.util.Properties

import com.mongodb.MongoClient
import org.bson.Document
import com.mongodb.client.{FindIterable, MongoCursor, MongoDatabase}
import com.streamxhub.common.util.{Logger, MongoConfig}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.Map


object MongoSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): MongoSource = new MongoSource(ctx, overrideParams)

}

class MongoSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param queryFun
   * @param resultFun
   * @param interval
   * @param config
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](queryFun: MongoDatabase => FindIterable[Document], resultFun: MongoCursor[Document] => List[R], interval: Long)(implicit config: Properties): DataStream[R] = {
    overrideParams.foreach(x => config.setProperty(x._1, x._2))
    val mongoFun = new MongoSourceFunction[R](queryFun, resultFun, interval)
    ctx.addSource(mongoFun)
  }

}

/**
 *
 * @param queryFun
 * @param resultFun
 * @param interval
 * @param typeInformation$R$0
 * @param config
 * @tparam R
 */
private[this] class MongoSourceFunction[R: TypeInformation](queryFun: MongoDatabase => FindIterable[Document], resultFun: MongoCursor[Document] => List[R], interval: Long)(implicit config: Properties) extends RichSourceFunction[R] with Logger {

  private[this] var isRunning = true

  var client: MongoClient = _

  var database: MongoDatabase = _

  override def cancel(): Unit = this.isRunning = false

  override def open(parameters: Configuration): Unit = {
    client = MongoConfig.getClient(config)
    val db = MongoConfig.getProperty(config, MongoConfig.database)
    database = client.getDatabase(db)
  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (isRunning) {
      val find = queryFun(database)
      resultFun(find.iterator).foreach(ctx.collect)
      Thread.sleep(interval)
    }
  }

  override def close(): Unit = {
    client.close()
  }

}