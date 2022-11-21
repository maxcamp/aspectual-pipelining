package main

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror}
import scala.collection.immutable.ListMap

// Adapted from a Chisel example
final class CustomBundle2[T <: Data](output: T, elts: (String, Data)*) extends DecoupledOutput[T] {
    val out = Decoupled(Output(output))

    val elements = ListMap(elts.map {
        case (field, elt) =>
            field -> elt
        }: _*
    ) + ("out" -> out)
    def apply(elt: String): Data = elements(elt)
    override def cloneType: this.type = {
        val cloned = elts.map { case (n, d) => n -> DataMirror.internal.chiselTypeClone(d) }
        (new CustomBundle2(out.cloneType, cloned: _*)).asInstanceOf[this.type]
    }
}