package main

import chisel3._
import chisel3.stage.ChiselStage

object Main extends App {

    (new ChiselStage).emitVerilog({
        val x = () => new Var
        val add = () => new Add
        val first = () => new Component(add, x, x)
        val second = () => new Component(add, first, first)

        second()
    })

}