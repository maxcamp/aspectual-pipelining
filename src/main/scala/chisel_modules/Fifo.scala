package main

import chisel3._
import chisel3.util._

/**
 * Ouput the values in data, one per clock tick
 */
class Fifo(data: Int *) extends Module {
    val io = IO(new Bundle {
        val out = Output(UInt(32.W))
    })

    val queue = Reg(Vec(data.length, UInt(32.W)))
    val pointer = RegInit(0.U(4.W))

    for (i <- 0 to data.length-1) {
        queue(i) := data(i).U
    }

    io.out := queue(pointer)
    pointer := pointer + 1.U




    // printf("output: %d  pointer: %d  queue[2]: %d\n",
    //     io.out, pointer, queue(2))



    // val io = IO(new Bundle {
    //     val out = Decoupled(Output(UInt(32.W)))
    // })

    // io.out.bits := 0.U(32.W)
    // io.out.valid := true.B

    // val q = Module(new Queue(UInt(32.W), data.length, flow = true))
    // q.io.enq.valid := true.B
    // for(x <- data) {
    //     q.io.enq.bits := x.U
    // }
    // // printf("output:  ready %d  valid %d  value %d\n",
    // //      io.out.ready, io.out.valid, io.out.bits)
    // printf("QUEUE:  ready %d  valid %d  value %d\n",
    //      q.io.deq.ready, q.io.deq.valid, q.io.deq.bits)

    // q.io.deq.ready := true.B

    // val o_reg   = RegInit(0.U(32.W))
    // o_reg := q.io.deq.bits
    // io.out.bits := o_reg

}
