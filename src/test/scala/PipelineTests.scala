package main

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class PipelineTests(c: Component) extends PeekPokeTester(c) {
  val outputs = List(0, 4, 8)

  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt()
    expect(intCast, outputs(i))
    step(1)
    i += 1
  } while (i < 3)
}

class PipelineTester extends ChiselFlatSpec {
  behavior of "Single-Stage Add/Subtract Pipeline"

  val inputA = () => new Fifo(1, 2, 3)
  val inputB = () => new Fifo(4, 5, 6)
  val inputC = () => new Fifo(6, 5, 4)
  val add = () => new Add
  val subtract = () => new Subtract

  val stage1 = () => new Component(add, 1, Map("a" -> inputA, "b" -> inputA))
  val stage2 = () => new Component(add, 1, Map("a" -> stage1, "b" -> inputB))
  val result = () => new Component(subtract, 1, Map("b" -> stage2, "a" -> inputC))

  backends foreach {backend =>
    it should s"test the basic add circuit" in {
      Driver(result, backend)((c) => new PipelineTests(c)) should be (true)
    }
  }
}
