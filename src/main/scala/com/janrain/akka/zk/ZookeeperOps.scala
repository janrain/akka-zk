package com.janrain.akka.zk

import concurrent._, duration._
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.{Watcher, ZooKeeper}
import org.apache.zookeeper.AsyncCallback.{ChildrenCallback, DataCallback}
import scala.util.Success
import java.util

object ZookeeperOps {
  case class DataResult(
    path: String,
    rc: Int,
    data: Array[Byte],
    stat: Stat)
}

trait ZookeeperOps { self: Watcher ⇒
  import ZookeeperOps._
  def getData(path: String, watch: Boolean = true)(implicit zk: ZooKeeper): Future[DataResult] = {
    val resultPromise = Promise[DataResult]()
    zk.getData(path, watch, new DataCallback {
      def processResult(rc: Int, path: String, ctx: scala.Any, data: Array[Byte], stat: Stat) {
        resultPromise.complete(Success(DataResult(path, rc, data, stat)))
      }
    }, None)
    resultPromise.future
  }

  def getChildren(path: String, watch: Boolean = true)(implicit zk: ZooKeeper): Future[Seq[String]] = {
    val childrenPromise = Promise[Seq[String]]()
    zk.getChildren(path, watch, new ChildrenCallback {
      def processResult(rc: Int, path: String, ctx: scala.Any, c: util.List[String]) {
        import collection.JavaConversions._
        val children = if (c == null) Nil else c.toList
        childrenPromise.complete(Success(children map { child ⇒ s"$path/$child"} ))
      }
    }, None)
    childrenPromise.future
  }
}
