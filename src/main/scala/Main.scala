package main

import chisel3._
import chisel3.stage.ChiselStage
import CoolSyntax._


object Main extends App {

    (new ChiselStage).emitVerilog({
        val inputA = () => new Fifo(1, 2, 3)
        val add = () => new Add

        val stage1 = add(1, Map("a" -> inputA, "b" -> inputA))
        val stage2 = add(2, Map("a" -> stage1, "b" -> inputA))
        val result = add(3, Map("a" -> stage2, "b" -> inputA))

        result()
    })

}