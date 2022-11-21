package main

import chisel3._
import chisel3.util._

abstract class DecoupledBundle[T <: Data] extends Bundle with DecoupledOutput[T]

trait DecoupledOutput[T <: Data] extends Record {
    val out: DecoupledIO[T]
}

trait DecoupledModule[T <: Data] extends Module {

    val io: DecoupledOutput[T]


}
