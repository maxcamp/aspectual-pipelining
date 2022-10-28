package main

import chisel3._
import chisel3.stage.ChiselStage
import CoolSyntax._


object Main extends App {

    (new ChiselStage).emitVerilog({
        val x = () => new Var
        val add = () => new Add

        val result = add(add(x, x), add(x, x))

        result()
    })

}