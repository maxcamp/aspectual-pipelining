package main

import chisel3._
import chisel3.experimental.{DataMirror}
import scala.collection.immutable.ListMap

// Taken from Chisel example
final class CustomBundle(elts: (String, Data)*) extends Record {
    val elements = ListMap(elts.map {
        case (field, elt) =>
            field -> elt
        }: _*
    )
    def apply(elt: String): Data = elements(elt)
    override def cloneType: this.type = {
        val cloned = elts.map { case (n, d) => n -> DataMirror.internal.chiselTypeClone(d) }
        (new CustomBundle(cloned: _*)).asInstanceOf[this.type]
    }
}