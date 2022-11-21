package main

import chisel3._

/**
 * Subtract two chisel inputs
 */
class Subtract extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(32.W))
    val b     = Input(UInt(32.W))
    val out   = Output(UInt(32.W))
  })

  io.out := io.b - io.a
}
