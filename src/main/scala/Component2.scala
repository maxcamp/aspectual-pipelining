package main

import chisel3._

/***
 * Set previous stage's output to ready when we are inReady
 * When output from previous stage is valid
 *      Take the output bits and feed it to our submodule
 * Set our output to valid when the submodule flags as valid
 */

// Can't pass a variable number of by-name parameters in Scala 2, using thunks instead
class Component2(module: () => DecoupledModule, val stage: Int, preInputs: Map[String, () => DecoupledModule]) extends DecoupledModule {

    // Call Module on all the de-thunked objects for chisel to work, needs to be done in this context
    val subModule = Module(module())
    val inputs = preInputs.map({case (key, mod) => (key, Module(mod()))})

    // Put every input to this module into a list with a unique name
    // * Not used if the inputs are all fed from pre-set queues *
    var ioList: List[(String, Data)] = List()
    for ((key, mod) <- inputs) {
        for ((name, data) <- mod.io.elements) {
            if (name != "out") {
                ioList = (key + "_" + name , Input(data.cloneType)) :: ioList
            }
        }
    }
    // This output determind by subModule computation
    val outputType = subModule.io.out.cloneType

    // Using CustomBundle to be able to create an IO bundle from a variable
    val bundle = new CustomBundle2(outputType, ioList:_*)
    val io = IO(bundle)

    // Create however many registers are needed, and group modules by stage
    var registers: Map[String, Data] = Map()
    var stages: Map[Int, Map[String, DecoupledModule]] = Map()
    for ((key, mod) <- inputs) {
        mod match {
            case c: Component2 => {
                // One register per stage behind
                for (i <- c.stage to this.stage-1) {
                    registers += (key + "_" + i -> Reg(mod.io.out.cloneType))
                }
                // Add this computation to stages at the approrpriate stage
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
    // Create the signals needed for each stage
    val order = stages.keySet.toList.sortWith(_<_)
    var signals: Map[String, Bool] = Map()
    for (i <- 0 to order.length-1) {
        val stage = order(i)
        signals += ("inReady_" + stage -> RegInit(true.B))              // Write
        signals += ("inValid_" + stage -> RegInit(false.B))             // Read
        signals += ("outReady_" + stage -> RegInit(false.B))            // Read
        signals += ("outValid_" + stage -> RegInit(false.B))            // Write
        if (i == 0) {
            signals("inValid_"+stage) := true.B
            // Set overall inReady
            io.inReady := signals("inReady_"+stage)
        }
        // Tie signals between stages together
        else {
            signals("inReady_"+stage) <> signals("outReady_"+order(i-1))
            signals("outValid_"+order(i-1)) <> signals("inValid_"+stage)
        }
        if (i == order.length-1) {
            signals("outReady_"+stage) := true.B
        }
    }

    // Set the signals for each stage
    for (i <- 0 to order.length-1) {
        val stage = order(i)

        // Three things can stall a stage: itself, the next stage not being ready for input, or the previous stage not having valid output

        // Before flagging outValid as true, a stage has to wait on: all of its modules to be outValid
        signals("outValid_"+stage) := stages(stage).values.toList.foldLeft(true.B)((x, y) => x && y.io.outValid)

        // Before flagging inReady as true, a stage has to wait on: all of its modules to be inReady
        signals("inReady_"+stage) := stages(stage).values.toList.foldLeft(true.B)((x, y) => x && y.io.inReady)
    }

    // Use previousRegisters to track which values are in which registers
    // The string is the key, the int is the stage for its current registers
    var previousRegisters: Map[String, Int] = Map()
    for (i <- 0 to order.length-1) {
        val stage = order(i)

        when(signals("outValid_"+stage) && signals("outReady_"+stage)) {
            // Forward old values from previous register to new register
            if (stage != this.stage) {
                for ((key, previousStage) <- previousRegisters) {
                    registers(key + "_" + stage) := registers(key + "_" + previousStage)
                    previousRegisters += (key -> stage)
                }
            }
        }
        // Pass the IO inputs to this module to the children one stage at a time
        for ((key, mod) <- stages(stage)) {
            // We need to supply outReady and inValid signals to submodules
            mod.io.inValid := signals("inValid_"+stage)
            mod.io.outReady := signals("outReady_"+stage)
            for ((name, data) <- mod.io.elements) {
                if (name != "out") {
                    mod.io.elements(name) := io.elements(key + "_" + name)
                }
            }
            // If not the final stage
            if (stage != this.stage) {
                // If ready for output
                when(signals("outValid_"+stage) && signals("outReady_"+stage)) {
                    // Store the output in a register
                    registers(key + "_" + stage) := mod.io.out
                    previousRegisters += (key -> stage)
                }
            }
        }
    }

    // subModule signals
    subModule.io.inValid := signals("inValid_"+this.stage)
    subModule.io.outReady := signals("outReady_"+this.stage)
    // Use the outputs from the children as inputs for subModule
    for ((key, mod) <- inputs) {
        // Whether from a register or otherwise
        if (previousRegisters.contains(key)) {
            subModule.io.elements(key) := registers(key + "_" + previousRegisters(key))
        }
        else {
            subModule.io.elements(key) := mod.io.out
        }
    }

    // Return, setting both output bits and valid signal
    io.out := subModule.io.out
    io.outValid := subModule.io.outValid && signals("outValid_"+this.stage)

}