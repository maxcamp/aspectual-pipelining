package main

import chisel3._

// Can't pass a variable number of by-name parameters in Scala 2, using thunks instead
class Component(module: () => Module, preInputs: () => Module *) extends Module {

    // Call Module on all the de-thunked objects for chisel to work, needs to be done in this context
    val subModule = Module(module())
    val inputs = preInputs.map(a => Module(a()))

    // Put every input into a list
    var ioList: List[(String, Data)] = List()
    for (i <- 0 to inputs.length-1) {
        for ((name, data) <- inputs(i).io.elements) {
            if (name != "out") {
                ioList = (name + i, Input(data.cloneType)) :: ioList
            }
        }
    }
    // Output determind by subModule computation
    ioList = ("out", Output(subModule.io.elements("out").cloneType)) :: ioList

    // Using CustomBundle to be able to create an IO bundle from variables
    val bundle = new CustomBundle(ioList:_*)
    val io = IO(bundle)

    // Pass the IO inputs to this module to the children
    for (i <- 0 to inputs.length-1) {
        for ((name, data) <- inputs(i).io.elements) {
            if (name != "out") {
                inputs(i).io.elements(name) := io.elements(name + i) 
            }
        }
    }

    // Use the outputs from the children as inputs for subModule
    for (i <- 0 to inputs.length-1) {
        subModule.io.getElements.reverse(i) := inputs(i).io.elements("out")
    }

    // Return
    io.elements("out") := subModule.io.elements("out")

}