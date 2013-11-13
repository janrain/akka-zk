package com.janrain.akka.zk

import scala.xml.{XML, Elem}
import scala.xml.parsing.ConstructingParser
import scala.io.Source

trait DefaultDataUnmarshallers {
  import ZkConfigExtension.ValueUnmarshaller
  implicit object StringValueUnmarshaller extends ValueUnmarshaller[String] {
    def unmarshal(data: Array[Byte]): String = new String(data)
  }

  implicit object XmlValueUnmarshaller extends ValueUnmarshaller[Elem] {
    def unmarshal(data: Array[Byte]): Elem = {
      ConstructingParser.fromSource(Source.fromString(new String(data)), preserveWS = true).document().docElem.asInstanceOf[Elem]
    }
  }
}

object DefaultDataUnmarshallers extends DefaultDataUnmarshallers
