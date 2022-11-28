package main

import chisel3._
import chisel3.util._

trait DecoupledBundle extends Bundle with DecoupledOutput

trait DecoupledOutput extends Record {
    val out: Data       
    val outValid = Output(Bool())           // We say if the output is valid
    val outReady = Input(Bool())            // They tell us if they are ready for output
    val inReady = Output(Bool())            // We say if we are ready for input
    val inValid = Input(Bool())             // They tell us if the input is valid
}

trait DecoupledModule extends Module {

    val io: DecoupledOutput


}
