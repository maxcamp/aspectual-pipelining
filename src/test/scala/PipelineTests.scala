package main

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class PipelineTests(c: Component) extends PeekPokeTester(c) {
  val outputs = List(13, 17, 21)

  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt()
    expect(intCast, outputs(i))
    step(1)
    i += 1
  } while (i < 3)
}

class PipelineTester extends ChiselFlatSpec {
  behavior of "Add Single-Stage Pipeline"

  val inputA = () => new Fifo(1, 2, 3)
  val inputB = () => new Fifo(4, 5, 6)
  val inputC = () => new Fifo(7, 8, 9)
  val add = () => new Add

  val stage1 = () => new Component(add, 1, Map("a" -> inputA, "b" -> inputA))
  val stage2 = () => new Component(add, 1, Map("a" -> stage1, "b" -> inputB))
  val result = () => new Component(add, 1, Map("a" -> stage2, "b" -> inputC))

  backends foreach {backend =>
    it should s"test the basic add circuit" in {
      Driver(result, backend)((c) => new PipelineTests(c)) should be (true)
    }
  }
}
