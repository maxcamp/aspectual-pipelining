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
            if (!name.startsWith("out") && !name.startsWith("in")) {
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
                    registers += (key + "_" + i -> Reg(mod.io.out.cloneType).suggestName("reg__"+key+"_"+i))
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
        signals += ("inReady_"+stage -> WireInit(false.B).suggestName("inReady_"+stage+"_"+this))             // Write
        signals += ("inValid_"+stage -> WireInit(false.B).suggestName("inValid_"+stage+"_"+this))             // Read
        signals += ("outReady_"+stage -> WireInit(false.B).suggestName("outReady_"+stage+"_"+this))           // Read
        signals += ("outValid_"+stage -> WireInit(false.B).suggestName("outValid_"+stage+"_"+this))           // Write
        signals += ("willBeOutValid_"+stage -> WireInit(false.B).suggestName("willBeOutValid_"+stage+"_"+this))
        // Need to clock valid signal
        // But only in stages followed by registers
        if (i != order.length-1) {
            signals += ("validReg_" + stage -> RegInit(false.B))         
        }
 
        // Tie signals between stages together
        if (i != 0) {
            signals("outReady_"+order(i-1)) := signals("inReady_"+stage)
            signals("inValid_"+stage) := signals("outValid_"+order(i-1))
        }
    }
    // Starting edge case
    // Tests drive io.inValid to be true
    // Otherwise tied to the modules in the first stage we see's inValid signals
    signals("inValid_"+order(0)) := stages(order(0)).values.toList.foldLeft(io.inValid)((x, y) => x
        // y match {
        //     case c: Component2 => {
        //         if(this.stage == 3) {
        //             println("\nHERE  "+c+"\n")
        //             printf("Stage 2 inValid %d\n", c.signals("inValid_"+2))
        //         }
        //         x && c.signals("inValid_"+order(0))
        //     }
        //     case other: DecoupledModule => {
        //         x && other.io.inValid
        //     }
        // }
    )
    // Ending edge case. Tests (or parent Component) drive io.outReady
    signals("outReady_"+order(order.length-1)) := io.outReady

    // Set the signals for each stage
    for (i <- 0 to order.length-1) {
        val stage = order(i)

        // Three things can stall a stage: itself, the next stage not being ready for input, or the previous stage not having valid output

        // To be outValid, all of a stage's modules must be outValid and and the previous stage must be valid
        signals("willBeOutValid_"+stage) := stages(stage).values.toList.foldLeft(true.B)((x, y) => x && y.io.outValid) && signals("inValid_"+stage)

        if (i != order.length-1) {
            // outValid signal is clocked by a register for all but final stage
            signals("validReg_"+stage) := signals("willBeOutValid_"+stage)
            signals("outValid_"+stage) := signals("validReg_"+stage)
        }
        else {
            signals("outValid_"+stage) := signals("willBeOutValid_"+stage)
        }

        // To be inReady, all of a stage's modules must be outValid and the next stage must be ready
        signals("inReady_"+stage) := stages(stage).values.toList.foldLeft(true.B)((x, y) => x && y.io.outValid) && signals("outReady_"+stage)

    }

    // Use previousRegisters to track which values are in which registers
    // The string is the key, the int is the stage for that key's current register
    var previousRegisters: Map[String, Int] = Map()
    for (i <- 0 to order.length-1) {
        val stage = order(i)

        // When ready for output
        when(signals("willBeOutValid_"+stage) && signals("outReady_"+stage)) {
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
            // We need to supply outReady and inValid signals to modules
            mod.io.inValid := true.B    // Not sure what this should be. Not relevant when using queues for input
            mod.io.outReady := (mod match {
                case q: DecoupledFifo => {
                    signals("willBeOutValid_"+stage) && signals("outReady_"+stage)
                }
                case other: DecoupledModule => {
                    signals("outReady_"+stage)
                }
            })
            for ((name, data) <- mod.io.elements) {
                if (!name.startsWith("out") && !name.startsWith("in")) {
                    mod.io.elements(name) := io.elements(key + "_" + name)
                }
            }
            // If not the final stage
            if (stage != this.stage) {
                // When ready for output
                when(signals("willBeOutValid_"+stage) && signals("outReady_"+stage)) {
                    // Store the output in a register
                    registers(key + "_" + stage) := mod.io.out
                    previousRegisters += (key -> stage)
                }
            }
        }
    }

    // subModule signals
    subModule.io.inValid := true.B  // // Not sure what this should be. Not relevant when using queues for input
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

    // Return, setting all outputs
    io.out := subModule.io.out
    io.outValid := signals("outValid_"+this.stage)
    io.inReady := signals("inReady_"+order(0))



    // CHISEL DEBUGGING
    // if(this.stage == 1) {
    //     printf("Adder input a %d\nAdder input b %d\nAdder output  %d\nStage in valid  %d\nStage in ready  %d\nStage will b val %d\nStage out valid %d\nStage out ready %d\n\n", 
    //         subModule.io.elements("a").asUInt, 
    //         subModule.io.elements("b").asUInt, 
    //         subModule.io.out.asUInt, 
    //         signals("inValid_"+this.stage), 
    //         signals("inReady_"+this.stage), 
    //         signals("willBeOutValid_"+stage),
    //         signals("outValid_"+this.stage), 
    //         signals("outReady_"+this.stage))
    // }
    // if(this.stage == 2) {
    //     println("\nTHERE "+this+"\n")
    //     printf("\nAdder input a %d\nAdder input b %d\nAdder output  %d\nStage 2 in valid  %d\nStage 2 in ready  %d\nStage 2 out valid %d\nStage out 2 ready %d\nRegister %d\n", 
    //         subModule.io.elements("a").asUInt, 
    //         subModule.io.elements("b").asUInt, 
    //         subModule.io.out.asUInt, 
    //         signals("inValid_"+this.stage), 
    //         signals("inReady_"+this.stage), 
    //         signals("outValid_"+this.stage), 
    //         signals("outReady_"+this.stage), 
    //         registers("a_1").asUInt)
    // }
    // if(this.stage == 3) {
    //     printf("Adder input a %d\nAdder input b %d\nAdder output  %d\nStage 2 in valid  %d\nStage 2 in ready  %d\nStage 2 out valid %d\nStage 2 out ready %d\nStage 3 in valid  %d\nStage 3 in ready  %d\nStage 3 out valid %d\nStage 3 out ready %d\nRegister %d\n\n", 
    //         subModule.io.elements("a").asUInt, 
    //         subModule.io.elements("b").asUInt, 
    //         subModule.io.out.asUInt,
    //         signals("inValid_"+2), 
    //         signals("inReady_"+2), 
    //         signals("outValid_"+2), 
    //         signals("outReady_"+2),
    //         signals("inValid_"+this.stage), 
    //         signals("inReady_"+this.stage), 
    //         signals("outValid_"+this.stage), 
    //         signals("outReady_"+this.stage), 
    //         registers("a_2").asUInt
    //     )
    // }

}