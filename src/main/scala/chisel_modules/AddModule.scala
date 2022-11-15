package main

import chisel3._

/**
 * Add two chisel inputs
 */
class Add extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(32.W))
    val b     = Input(UInt(32.W))
    val out   = Output(UInt(32.W))
  })

  io.out := io.a + io.b
}
