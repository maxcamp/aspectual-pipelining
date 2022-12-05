package main

import chisel3._
import chisel3.util._

class DecoupledFifoIO extends DecoupledBundle {
  val out   = Output(UInt(32.W))
}

/**
 * Ouput the values in data, one per clock tick
 * Stall for 3 clock ticks before sending out data[stallOn] 
 */
class DecoupledFifo(data: List[Int], stallOn: Int) extends DecoupledModule {
    val io = IO(new DecoupledFifoIO)

    val queue = Reg(Vec(data.length, UInt(32.W)))
    val pointer = RegInit(0.U(4.W))
    val counter = RegInit(0.U(2.W))

    // Does not affect anything because we don't accept input during runtime
    io.inReady := DontCare

    // Permanently store values in queue
    for (i <- 0 to data.length-1) {
        queue(i) := data(i).U
    }

    // val gogogo = RegInit(false.B)
    // gogogo := io.outReady

    // Simulate queue stalling
    when(pointer === stallOn.U && counter =/= 3.U) {
        io.outValid := false.B
        io.out := 0.U
        counter := counter + 1.U
    }
    .otherwise {
        io.outValid := true.B
        // Not ready for output, but our output is valid
        when(!io.outReady) {
            io.out := queue(pointer)
        }
        // Ready to send
        .otherwise {
            io.out := queue(pointer)
            pointer := pointer + 1.U
        }
    }
    

}
