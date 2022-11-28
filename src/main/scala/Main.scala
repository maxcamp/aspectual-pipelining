package main

import chisel3._
import chisel3.stage.ChiselStage
import CoolSyntax._


object Main extends App {

    (new ChiselStage).emitVerilog({
        val inputA = () => new DecoupledFifo(List(1, 2, 3), 5)
        val inputB = () => new DecoupledFifo(List(1, 2, 3), 5)
        val inputC = () => new DecoupledFifo(List(1, 2, 3), 5)
        val add = () => new DecoupledAdd

        val stage1 = add(1, Map("a" -> inputA, "b" -> inputA))
        val stage2 = add(2, Map("a" -> stage1, "b" -> inputB))
        val result = add(3, Map("a" -> stage2, "b" -> inputC))

        // OUTPUT:
        // 4  on clock cycle 3, 
        // 8  on clock cycle 4, 
        // 12 on clock cycle 5
        result()
    })

}