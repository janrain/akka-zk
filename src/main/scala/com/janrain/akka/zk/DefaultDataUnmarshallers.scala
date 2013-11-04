package com.janrain.akka.zk

import scala.xml.{XML, Elem}

trait DefaultDataUnmarshallers {
  import ZkConfigExtension.ValueUnmarshaller
  implicit object StringValueUnmarshaller extends ValueUnmarshaller[String] {
    def unmarshal(data: Array[Byte]): String = new String(data)
  }

  implicit object XmlValueUnmarshaller extends ValueUnmarshaller[Elem] {
    def unmarshal(data: Array[Byte]): Elem = XML.loadString(new String(data))
  }
}

object DefaultDataUnmarshallers extends DefaultDataUnmarshallers
