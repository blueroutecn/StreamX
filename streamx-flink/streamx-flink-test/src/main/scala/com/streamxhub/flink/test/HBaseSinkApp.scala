package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.{HBaseOutputFormat, HBaseSink}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.hadoop.hbase.client.{Mutation, Put}
import org.apache.hadoop.hbase.util.Bytes
import java.util.{Collections, Random}

import org.apache.flink.streaming.api.scala._

import scala.collection.JavaConverters._
import com.streamxhub.common.util.ConfigUtils

object HBaseSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    val source = context.addSource(new TestSource)
    val random = new Random()

    //定义转换规则...
    implicit def entry2Put(entity: TestEntity): java.lang.Iterable[Mutation] = {
      val put = new Put(Bytes.toBytes(System.nanoTime() + random.nextInt(1000000)), entity.timestamp)
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("cid"), Bytes.toBytes(entity.cityId))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("oid"), Bytes.toBytes(entity.orderId))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("os"), Bytes.toBytes(entity.orderStatus))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("oq"), Bytes.toBytes(entity.quantity))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("sid"), Bytes.toBytes(entity.siteId))
      Collections.singleton(put)
    }

    //1）插入方式1
    HBaseSink(context).sink[TestEntity](source, "order")

    //2) 插入方式2
    //1.指定HBase 配置文件
    implicit val prop = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    //2.插入...
    source.writeUsingOutputFormat(new HBaseOutputFormat[TestEntity]("order", entry2Put))


  }

}
