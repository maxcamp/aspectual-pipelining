package main

import chisel3._
import chisel3.stage.ChiselStage
import CoolSyntax._


object Main extends App {

    (new ChiselStage).emitVerilog({
        val x = () => new Var
        val add = () => new Add

        val stage1 = add(1, Map("a" -> x, "b" -> x))
        val stage2 = add(2, Map("a" -> stage1, "b" -> x))
        val result = add(3, Map("a" -> stage2, "b" -> x))

        result()
    })

}