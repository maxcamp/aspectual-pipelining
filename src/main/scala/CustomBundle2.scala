package main

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror}
import scala.collection.immutable.ListMap

// Adapted from a Chisel example
final class CustomBundle2(output: Data, elts: (String, Data)*) extends DecoupledOutput {
    val out = Output(output)

    val elements = ListMap(elts.map {
        case (field, elt) =>
            field -> elt
        }: _*
    ) + 
    ("out" -> out) + 
    ("inValid" -> this.inValid) +
    ("inReady" -> this.inReady) +
    ("outValid" -> this.outValid) +
    ("outReady" -> this.outReady)
    def apply(elt: String): Data = elements(elt)
    override def cloneType: this.type = {
        val cloned = elts.map { case (n, d) => n -> DataMirror.internal.chiselTypeClone(d) }
        (new CustomBundle2(out.cloneType, cloned: _*)).asInstanceOf[this.type]
    }
}