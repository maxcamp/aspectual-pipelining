package main

import chisel3._

/**
 * Chisel module that returns its input
 */ 
class Var extends Module {
  val io = IO(new Bundle {
    val x     = Input(UInt(32.W))
    val out   = Output(UInt(32.W))
  })

  io.out := io.x
}
