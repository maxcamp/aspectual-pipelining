package main

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap

class DecoupledAddIO extends DecoupledBundle[UInt] {
  val a     = Input(UInt(32.W))
  val b     = Input(UInt(32.W))
  val out   = Decoupled(Output(UInt(32.W)))
}

/**
 * Add two chisel inputs, decoupled output
 */
class DecoupledAdd extends DecoupledModule[UInt] {
  val io = IO(new DecoupledAddIO)

  io.out.bits := io.a + io.b
  io.out.valid := true.B
}
