package main

import chisel3._

// Can't pass a variable number of by-name parameters in Scala 2, using thunks instead
class Component(module: () => Module, val stage: Int, preInputs: Map[String, () => Module]) extends Module {

    // Call Module on all the de-thunked objects for chisel to work, needs to be done in this context
    val subModule = Module(module())
    val inputs = preInputs.map({case (key, mod) => (key, Module(mod()))})

    // Put every input into a list
    var ioList: List[(String, Data)] = List()
    for ((key, mod) <- inputs) {
        for ((name, data) <- mod.io.elements) {
            if (name != "out") {
                ioList = (key + "_" + name , Input(data.cloneType)) :: ioList
            }
        }
    }
    // Output determind by subModule computation
    ioList = ("out", Output(subModule.io.elements("out").cloneType)) :: ioList
    ioList = ioList.reverse

    // Using CustomBundle to be able to create an IO bundle from a variable
    val bundle = new CustomBundle(ioList:_*)
    val io = IO(bundle)

    // Determine if a register is needed for each input
    var registerNeeded: List[Boolean] = List()
    for ((_, mod) <- inputs) {
        mod match {
            case c: Component => {registerNeeded = (c.stage < this.stage) :: registerNeeded}
            case _ => {registerNeeded = false :: registerNeeded}
        }
    }
    registerNeeded = registerNeeded.reverse

    // Pass the IO inputs to this module to the children
    for ((key, mod) <- inputs) {
        for ((name, data) <- mod.io.elements) {
            if (name != "out") {
                mod.io.elements(name) := io.elements(key + "_" + name) 
            }
        }
    }

    // Use the outputs from the children as inputs for subModule
    for ((key, mod) <- inputs) {
        subModule.io.elements(key) := mod.io.elements("out")
    }

    // Return
    io.elements("out") := subModule.io.elements("out")

}