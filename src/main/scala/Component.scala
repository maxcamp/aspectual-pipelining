package main

import chisel3._

// Can't pass a variable number of by-name parameters in Scala 2, using thunks instead
class Component(module: () => Module, val stage: Int, preInputs: Map[String, () => Module]) extends Module {

    // Call Module on all the de-thunked objects for chisel to work, needs to be done in this context
    val subModule = Module(module())
    val inputs = preInputs.map({case (key, mod) => (key, Module(mod()))})

    // Put every input to this module into a list with a unique name
    var ioList: List[(String, Data)] = List()
    for ((key, mod) <- inputs) {
        for ((name, data) <- mod.io.elements) {
            if (name != "out") {
                ioList = (key + "_" + name , Input(data.cloneType)) :: ioList
            }
        }
    }
    // This output determind by subModule computation
    ioList = ("out", Output(subModule.io.elements("out").cloneType)) :: ioList
    ioList = ioList.reverse

    // Using CustomBundle to be able to create an IO bundle from a variable
    val bundle = new CustomBundle(ioList:_*)
    val io = IO(bundle)

    // Create however many registers are needed, and group modules by stage
    var registers: Map[String, Data] = Map()
    var stages: Map[Int, Map[String, Module]] = Map()
    for ((key, mod) <- inputs) {
        mod match {
            case c: Component => {
                // One register per stage behind
                for (i <- c.stage to this.stage-1) {
                    registers += (key + "_" + i -> Reg(mod.io.elements("out").cloneType))
                }
                // Add this computation to stages at the right stage
                val updated = stages.getOrElse(c.stage, Map()) + (key -> mod)
                stages += (c.stage -> updated)
            }
            case _ => {
                // Add this computation to stages at the current stage
                val updated = stages.getOrElse(this.stage, Map()) + (key -> mod)
                stages += (this.stage -> updated)
            }
        }
    }

    // Pass the IO inputs to this module to the children one stage at a time
    val order = stages.keySet.toList.sortWith(_<_)
    // Keep track of which values are in which registers
    var previousRegisters: Map[String, Int] = Map()
    for (stage <- order) {
        // Forward old values from previous register to new register
        if (stage != this.stage) {
            for ((key, previousStage) <- previousRegisters) {
                registers(key + "_" + stage) := registers(key + "_" + previousStage)
                previousRegisters += (key -> stage)
            }
        }
        for ((key, mod) <- stages(stage)) {
            for ((name, data) <- mod.io.elements) {
                if (name != "out") {
                    mod.io.elements(name) := io.elements(key + "_" + name)
                }
            }
            // If not the final stage, store the output in a register
            if (stage != this.stage) {
                registers(key + "_" + stage) := mod.io.elements("out")
                previousRegisters += (key -> stage)
            }
        }
    }

    // Use the outputs from the children as inputs for subModule
    // Whether from a register or otherwise
    for ((key, mod) <- inputs) {
        if (previousRegisters.contains(key)) {
            subModule.io.elements(key) := registers(key + "_" + previousRegisters(key))
        }
        else {
            subModule.io.elements(key) := mod.io.elements("out")
        }
    }

    // Return
    io.elements("out") := subModule.io.elements("out")

}