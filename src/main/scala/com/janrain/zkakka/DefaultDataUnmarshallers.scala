package com.janrain.zkakka

trait DefaultDataUnmarshallers {
  import ZkConfigExtension.ValueUnmarshaller
  implicit object StringValueUnmarshaller extends ValueUnmarshaller[String] {
    def unmarshal(data: Array[Byte]): String = new String(data)
  }
}

object DefaultDataUnmarshallers extends DefaultDataUnmarshallers
