package main

import chisel3._
import chisel3.util._

class DecoupledFifoIO extends DecoupledBundle[UInt] {
  val out   = Decoupled(Output(UInt(32.W)))
}

/**
 * Ouput the values in data, one per clock tick
 * Stall for 3 clock ticks before sending out data[stallOn] 
 */
class DecoupledFifo(data: List[Int], stallOn: Int) extends DecoupledModule[UInt] {
    val io = IO(new DecoupledFifoIO)

    val queue = Reg(Vec(data.length, UInt(32.W)))
    val pointer = RegInit(0.U(4.W))
    val counter = RegInit(0.U(2.W))

    for (i <- 0 to data.length-1) {
        queue(i) := data(i).U
    }

    when(pointer === stallOn.U && counter =/= 3.U) {
        io.out.valid := false.B
        io.out.bits := 0.U
        counter := counter + 1.U
    }
    .otherwise {
        io.out.valid := true.B
        io.out.bits := queue(pointer)
        pointer := pointer + 1.U
    }

}
