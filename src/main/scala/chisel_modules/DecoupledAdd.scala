package main

import chisel3._
import chisel3.util._

/**
 * Add two chisel inputs, decoupled output
 */
class DecoupledAdd extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(32.W))
    val b     = Input(UInt(32.W))
    val out   = Decoupled(Output(UInt(32.W)))
  })

  io.out.bits := io.a + io.b
  io.out.valid := true.B
}
