package main

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class PipelineTests(c: Component2, outputs: List[Int], delay: Int) extends PeekPokeTester(c) {
  
  // Clock cycles to wait before first output
  step(delay)

  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt()
    expect(intCast, outputs(i))
    step(1)
    i += 1
  } while (i < outputs.length)

}

class PipelineTester extends ChiselFlatSpec {
  behavior of "Add/Subtract Pipeline"

  val inputA = () => new Fifo(1, 2, 3)
  val inputB = () => new Fifo(0, 4, 5, 6)
  val inputC = () => new Fifo(0, 0, 7, 8, 9)
  val add = () => new Add
  val subtract = () => new Subtract

  val stage1 = () => new Component2(add, 1, Map("a" -> inputA, "b" -> inputA))
  val stage2 = () => new Component2(add, 2, Map("a" -> stage1, "b" -> inputB))
  val result = () => new Component2(add, 3, Map("a" -> stage2, "b" -> inputC))

  backends foreach {backend =>
    it should s"test the one-stage add circuit" in {
      Driver(stage1, backend)((c) => new PipelineTests(c, List(2, 4, 6), 0)) should be (true)
    }
    it should s"test the two-stage add circuit" in {
      Driver(stage2, backend)((c) => new PipelineTests(c, List(6, 9, 12), 1)) should be (true)
    }
    it should s"test the three-stage add circuit" in {
      Driver(result, backend)((c) => new PipelineTests(c, List(13, 17, 21), 2)) should be (true)
    }
  }
}
