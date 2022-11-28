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
 * Add two chisel inputs, decoupled output
 */
class DecoupledAdd extends DecoupledModule {
  val io = IO(new DecoupledAddIO)

  // Register to hold on to output in case it is not ready
  val o_reg = RegInit(0.U(32.W))

  // Computation only takes one clock tick, therefore we are always ready for input
  io.inReady := true.B

  // When input is sent to us (and when we are ready which is always), load input
  when(io.inValid) {
    o_reg := io.a + io.b
  }

  // Always signal that we have an output (computed from most recent input)
  io.out := o_reg
  io.outValid := true.B
}
