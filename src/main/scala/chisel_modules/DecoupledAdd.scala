package main

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap

class DecoupledAddIO extends DecoupledBundle {
  val a     = Input(UInt(32.W))
  val b     = Input(UInt(32.W))
  val out   = Output(UInt(32.W))
}

/**
 * Add two chisel inputs
 * Implements Decoupled but purely combinational
 */
class DecoupledAdd extends DecoupledModule {
  val io = IO(new DecoupledAddIO)

  // Computation only takes one clock tick, therefore we are always ready for input
  io.inReady := true.B

  // Always signal that we have an output
  io.out := io.a + io.b
  io.outValid := true.B
}
