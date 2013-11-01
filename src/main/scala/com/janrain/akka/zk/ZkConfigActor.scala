package com.janrain.akka.zk

import concurrent._, duration._
import akka.actor._
import akka.event.LoggingReceive
import org.apache.zookeeper.{WatchedEvent, Watcher, ZooKeeper}
import org.apache.zookeeper.Watcher.Event.{KeeperState, EventType}

object ZkConfigActor {
  // PROTOCOL
  case class Subscribe(subscriber: ActorRef, path: String, andChildren: Boolean)
  case class SubscribeAck(subscribe: Subscribe)
  case class Fetch(path: String, andChildren: Boolean)
  case object Reconnect
  case object Ready

  case class Subscription(
    actor: ActorRef,
    andChildren: Boolean
  )

  // INTERNAL STATE
  case class ZkConfigState(zookeeper: ZooKeeper, subscriptions: Map[String, Seq[Subscription]] = Map.empty) {
    def withSubscription(actorRef: ActorRef, path: String, andChildren: Boolean) = copy(subscriptions = {
      subscriptions + {
        path → (subscriptions.getOrElse(path, Nil) :+ Subscription(actorRef, andChildren))
      }
    })

    def withoutSubscriber(actorRef: ActorRef) = copy(subscriptions = {
      subscriptions map {
        case (path, subscribers) ⇒ path → (subscribers filterNot { _.actor == actorRef })
      } filter {
        case (_, subscribers) ⇒ !subscribers.isEmpty
      }
    })
  }

  def props = Props(classOf[ZkConfigActor])
}

class ZkConfigActor extends Actor with ActorLogging with Watcher with ZookeeperOps with Stash {
  import ZkConfigActor._
  import ZkConfigExtension._
  import akka.pattern.pipe

  val config = context.system.settings.config.getConfig("janrain.akka.zk")
  val zkTimeout = FiniteDuration(config.getMilliseconds("timeout"), MILLISECONDS).toMillis.toInt
  val zkConnection = config.getString("connection")
  def zk() = new ZooKeeper(zkConnection, zkTimeout, this)

  def connecting(state: ZkConfigState): Receive = LoggingReceive {
    case Ready ⇒ {
      unstashAll()
      context become running(state)
    }
    case _ ⇒ stash()
  }

  def running(state: ZkConfigState): Receive = LoggingReceive {
    case Ready ⇒ {}

    case subscribe@Subscribe(subscriber, path, andChildren) ⇒
      log.debug(s"$sender subscribing to $path")
      sender ! SubscribeAck(subscribe)
      self ! Fetch(path, andChildren)
      context watch subscriber
      context become running(state.withSubscription(subscriber, path, andChildren))

    case Fetch(path, andChildren) ⇒
      import context.dispatcher
      getData(path, watch = true)(state.zookeeper) map { dataResult ⇒
        ConfigValue(dataResult.path, Option(dataResult.data))
      } pipeTo self

      if (andChildren) {
        getChildren(path, watch = true)(state.zookeeper) map { children ⇒
          children map { child ⇒ self ! Fetch(child, andChildren) }
        }
      }

    case configValue: ConfigValue ⇒
      (state.subscriptions collect {
        case (path, subscribers) if configValue.path startsWith path ⇒ subscribers
      }).flatten map { subscriber ⇒
        subscriber.actor ! configValue
      }

    case Reconnect ⇒
      context become connecting(state.copy(zookeeper = zk()))
      for (subs <- state.subscriptions) {
        for (s <- subs._2) {
          log.debug(s"Resubscribing: ${subs._1}  ${s.actor} ${s.andChildren}")
          self ! Fetch(subs._1, s.andChildren)
          context watch s.actor
        }
      }
      self ! Ready

    case Terminated(actorRef) ⇒
      context become running(state.withoutSubscriber(actorRef))
  }

  def receive = connecting(ZkConfigState(zk()))

  def process(event: WatchedEvent) {
    import EventType._

    event.getState match {
      case KeeperState.SyncConnected ⇒ self ! Ready
      case KeeperState.Disconnected | KeeperState.Expired ⇒ self ! Reconnect
      case _ ⇒ {}
    }

    event.getType match {
      case NodeDataChanged | NodeDeleted if event.getPath != null ⇒ {
        self ! Fetch(event.getPath, andChildren = false)
      }
      case NodeChildrenChanged if event.getPath != null ⇒ self ! Fetch(event.getPath, andChildren = true)
      case t ⇒ log.debug(s"unhandled event type $t, ${event.getPath}")
    }
  }
}
